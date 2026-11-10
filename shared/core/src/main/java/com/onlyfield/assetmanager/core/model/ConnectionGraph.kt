package com.onlyfield.assetmanager.core.model

import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.i18n.Messages

enum class ConnectionState { AVAILABLE, COMPLETE, INCOMPLETE, CONFLICT }

data class ChainStep(
    val stepIndex: Int,
    val currentPort: Port?,
    val currentDevice: Device?,
    val cable: Cable? = null,
    val panelMapping: PanelMapping? = null,
    val isUnknownPassage: Boolean = false,
    val description: String,
)

/** Types whose ports come in front/rear pairs joined by an internal passage. */
val PASS_THROUGH_TYPES = setOf("outlet", "junction-box")

/** Passive objects let a path continue (panels, outlets, junction boxes); active ones terminate it. */
fun Device.isPassive() = hardware.passive || category == DeviceCategory.PATCH_PANEL || objectTypeId in PASS_THROUGH_TYPES

/** Physical continuity only: active devices terminate a path. */
class ConnectionGraph(val project: Project) {
    private val devices = project.sites.flatMap { it.devices }.associateBy { it.id }
    private val ports = devices.values.flatMap { it.ports }.associateBy { it.id }
    private val comboPeers = ports.values.filter { it.hardware.comboKey != null }.groupBy { it.deviceId to it.hardware.comboKey }
    private data class Edge(val id: String, val a: String, val b: String?, val cable: Cable? = null, val mapping: PanelMapping? = null)
    private val edges = buildList {
        project.cables.forEach { c ->
            val a = c.portAId ?: c.deviceAId?.let { "device:$it" }
            val b = c.portBId ?: c.deviceBId?.let { "device:$it" }
            if (a != null) add(Edge("c:${c.id}", a, b, cable = c))
            else if (b != null) add(Edge("c:${c.id}", b, null, cable = c))
        }
        project.panelMappings.forEach { m -> add(Edge("m:${m.id}", m.portAId, m.portBId, mapping = m)) }
    }
    private val adjacency = buildMap<String, MutableList<Edge>> {
        edges.forEach { e -> getOrPut(e.a) { mutableListOf() }.add(e); e.b?.let { getOrPut(it) { mutableListOf() }.add(e) } }
    }
    private val states = mutableMapOf<String, ConnectionState>()
    private fun device(node: String): Device? = if (node.startsWith("device:")) devices[node.removePrefix("device:")] else ports[node]?.deviceId?.let(devices::get)
    private fun exists(node: String) = if (node.startsWith("device:")) device(node) != null else node in ports
    private fun passive(d: Device?) = d?.isPassive() == true
    private fun terminal(node: String) = exists(node) && !passive(device(node))
    private fun external(node: String) = adjacency[node].orEmpty().filter { it.mapping == null }

    fun occupied(portId: String, excludingCableId: String? = null): Boolean {
        val p = ports[portId] ?: return false
        val peers = if (p.hardware.comboKey == null) listOf(p) else comboPeers[p.deviceId to p.hardware.comboKey].orEmpty()
        return peers.any { port -> external(port.id).any { it.cable?.id != excludingCableId || it.cable == null } }
    }

    fun state(portId: String): ConnectionState {
        states[portId]?.let { return it }
        if (portId !in ports) return ConnectionState.CONFLICT
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<String, String?>>()
        queue.add(portId to null)
        var conflict = false
        var incomplete = false
        var terminals = 0
        while (queue.isNotEmpty()) {
            val (node, incoming) = queue.removeFirst()
            if (!visited.add(node)) { conflict = true; continue }
            if (!exists(node)) { conflict = true; continue }
            val links = adjacency[node].orEmpty()
            if (external(node).size > 1 || links.count { it.mapping != null } > 1) conflict = true
            val p = ports[node]
            if (p?.observation?.status == ObservationStatus.CONFLICT) conflict = true
            if (p?.endpointStatus == EndpointStatus.DETACHED_TO_VERIFY || p?.observation?.status == ObservationStatus.TO_VERIFY) incomplete = true
            if (p?.hardware?.comboKey != null) {
                val siblings = comboPeers[p.deviceId to p.hardware.comboKey].orEmpty()
                if (siblings.sumOf { external(it.id).size } > 1) conflict = true
            }
            if (terminal(node)) {
                terminals++
                if (links.size > 1) conflict = true
            } else if (links.size < 2) incomplete = true
            for (e in links.filter { it.id != incoming }) {
                if (e.mapping?.isUnknownPassage == true || e.cable?.observation?.status == ObservationStatus.TO_VERIFY) incomplete = true
                if (e.cable?.observation?.status == ObservationStatus.CONFLICT) conflict = true
                val next = if (node == e.a) e.b else e.a
                if (next == null) incomplete = true else queue.add(next to e.id)
            }
        }
        val result = when {
            conflict -> ConnectionState.CONFLICT
            !incomplete && terminals == 2 -> ConnectionState.COMPLETE
            else -> ConnectionState.INCOMPLETE
        }
        visited.filter { it in ports }.forEach { id ->
            val port = ports.getValue(id)
            val detached = port.endpointStatus == EndpointStatus.DETACHED_TO_VERIFY
            states[id] = if (!occupied(id) && !detached && result != ConnectionState.CONFLICT) ConnectionState.AVAILABLE else result
        }
        return states[portId] ?: result
    }

    fun trace(startPortId: String, i18n: Messages = Messages()): List<ChainStep> {
        val steps = mutableListOf<ChainStep>()
        val visited = mutableSetOf<String>()
        var node = startPortId
        var incoming: String? = null
        while (visited.add(node)) {
            val candidates = adjacency[node].orEmpty().filter { it.id != incoming }
            if (candidates.isEmpty()) break
            if (candidates.count { it.mapping == null } > 1 || candidates.count { it.mapping != null } > 1 || (incoming != null && candidates.size > 1)) {
                steps.add(ChainStep(steps.size + 1, ports[node], device(node), isUnknownPassage = true, description = i18n.text("config.conflict")))
                break
            }
            val edge = candidates.firstOrNull { it.mapping == null } ?: candidates.first()
            val next = if (node == edge.a) edge.b else edge.a
            val unknown = next == null || !exists(next) || edge.mapping?.isUnknownPassage == true
            val destination = next?.let { n -> listOfNotNull(device(n)?.technicalName, ports[n]?.name).joinToString(" › ") }.orEmpty()
            steps.add(ChainStep(steps.size + 1, ports[node], device(node), edge.cable, edge.mapping, unknown,
                listOfNotNull(edge.cable?.codeOrLabel ?: edge.cable?.medium?.toDisplayString(i18n), edge.mapping?.let { i18n.text("mapping.internal") }, destination.ifBlank { i18n.text("config.undefined") }).joinToString(" → ")))
            if (unknown) break
            if (next in visited) {
                steps.add(ChainStep(steps.size + 1, ports[next], device(next), isUnknownPassage = true, description = i18n.text("config.cycle")))
                break
            }
            node = requireNotNull(next)
            incoming = edge.id
        }
        return steps
    }
}
