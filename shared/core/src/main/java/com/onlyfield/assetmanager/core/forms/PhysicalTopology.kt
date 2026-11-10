package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.*

/**
 * Active device in the topology. [row] and [column] place it in the layered layout (rows wrap long
 * levels); [outside] marks the far end of a link that falls outside the site/floor filter.
 */
data class TopologyNode(val device: Device, val level: Int, val row: Int, val column: Int, val outside: Boolean, val areaId: String?)

/** Complete paths between two active devices, passives collapsed. */
data class TopologyLink(val a: String, val b: String, val paths: Int, val media: Set<CableMedium>, val passives: Int)

data class Topology(
    val nodes: List<TopologyNode>,
    val links: List<TopologyLink>,
    /** Paths leaving a device without reaching another active one, per device id. */
    val stubs: Map<String, Int>,
    /** Nodes per row, top to bottom. */
    val rowSizes: List<Int>,
    /** End devices folded into the device they hang from (see [PhysicalTopology.build]). */
    val folded: Map<String, List<Device>> = emptyMap(),
)

/** Physical topology of the active devices, built from [PathSchematics]. */
object PhysicalTopology {
    /**
     * [foldEndpoints] hides end devices (PCs, phones, cameras, APs…) with a single link to an infrastructure
     * device and counts them on it, so the drawing shows the network structure.
     */
    fun build(project: Project, siteId: String? = null, areaId: String? = null, perRow: Int = 8, foldEndpoints: Boolean = false): Topology {
        val index = ProjectIndex(project)
        val graph = ConnectionGraph(project)
        val active = index.devices.filter { !it.isPassive() }
        val siteOf = project.sites.flatMap { s -> s.devices.map { it.id to s.id } }.toMap()
        val areaOf = active.associate { it.id to ObjectMap.areaId(project, it) }
        fun inside(d: Device) = (siteId == null || siteOf[d.id] == siteId) && (areaId == null || areaOf[d.id] == areaId)

        class Acc(val paths: MutableSet<Pair<String, String>> = mutableSetOf(), val media: MutableSet<CableMedium> = mutableSetOf(), var passives: Int = 0)
        val links = linkedMapOf<Pair<String, String>, Acc>()
        val stubs = mutableMapOf<String, Int>()
        for (d in active.filter(::inside)) for (port in d.ports) {
            if (!graph.occupied(port.id)) continue
            val path = PathSchematics.of(project, port.id, graph, index) ?: continue
            val ends = listOf(path.stations.first(), path.stations.last())
            val far = ends.firstOrNull { it.device?.id != d.id }?.device
            if (path.stations.size < 2 || ends.any { it.passive } || far == null) { stubs.merge(d.id, 1, Int::plus); continue }
            val key = listOf(d.id, far.id).sorted().let { it[0] to it[1] }
            val acc = links.getOrPut(key) { Acc() }
            // Each path is met from both of its ends: identify it by its end ports.
            val id = listOf(ends[0].ports.lastOrNull()?.id.orEmpty(), ends[1].ports.firstOrNull()?.id.orEmpty()).sorted().let { it[0] to it[1] }
            if (acc.paths.add(id)) {
                acc.media += path.segments.map { it.cable.medium }
                acc.passives = maxOf(acc.passives, path.stations.count { it.passive })
            }
        }
        val ids = (active.filter(::inside).map { it.id } + links.keys.flatMap { listOf(it.first, it.second) }).toSet()
        val all = links.keys.flatMap { (a, b) -> listOf(a to b, b to a) }.groupBy({ it.first }, { it.second })
        val folded = if (!foldEndpoints) emptyMap() else active.filter { d ->
            val only = all[d.id]?.singleOrNull()
            d.id in ids && rank(d) == 3 && only != null && index.device(only)?.let(::rank)?.let { it < 3 } == true
        }.groupBy { all.getValue(it.id).single() }
        val hidden = folded.values.flatten().map { it.id }.toSet()
        val devices = active.filter { it.id in ids && it.id !in hidden }
        links.keys.removeAll { it.first in hidden || it.second in hidden }
        val neighbours = links.keys.flatMap { (a, b) -> listOf(a to b, b to a) }.groupBy({ it.first }, { it.second })

        // Each connected group is a band of rows, breadth-first from its most upstream device
        // (ONT, router, then the busiest switch); devices without links share the last band.
        val level = mutableMapOf<String, Int>()
        val groups = mutableListOf<List<String>>()
        val pending = devices.sortedWith(compareBy({ rank(it) }, { -(neighbours[it.id]?.size ?: 0) }, { it.technicalName.lowercase() }))
        for (root in pending.filter { it.id in neighbours }) {
            if (root.id in level) continue
            val group = mutableListOf(root.id)
            val queue = ArrayDeque(listOf(root.id)); level[root.id] = 0
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                neighbours[id].orEmpty().filter { it !in level }.sortedBy { index.device(it)?.technicalName }.forEach { level[it] = level.getValue(id) + 1; queue += it; group += it }
            }
            groups += group
        }
        pending.filter { it.id !in level }.map { it.id }.takeIf { it.isNotEmpty() }?.let { alone -> alone.forEach { level[it] = 0 }; groups += alone }
        // Order within a level by the position of the parents (fewer crossings), then by name; long levels wrap.
        val byId = devices.associateBy { it.id }
        val position = mutableMapOf<String, Double>()
        val rows = mutableListOf<Int>()
        val placed = mutableListOf<TopologyNode>()
        groups.forEach { group ->
            group.mapNotNull(byId::get).groupBy { level.getValue(it.id) }.toSortedMap().forEach { (lvl, members) ->
                val ordered = members.sortedWith(compareBy({ d -> neighbours[d.id].orEmpty().mapNotNull(position::get).average().takeIf { !it.isNaN() } ?: Double.MAX_VALUE }, { it.technicalName.lowercase() }))
                ordered.chunked(perRow.coerceAtLeast(1)).forEach { chunk ->
                    val row = rows.size
                    rows += chunk.size
                    chunk.forEachIndexed { col, d ->
                        position[d.id] = (col + .5) / chunk.size
                        placed += TopologyNode(d, lvl, row, col, !inside(d), areaOf[d.id])
                    }
                }
            }
        }
        return Topology(placed, links.map { (k, acc) -> TopologyLink(k.first, k.second, acc.paths.size, acc.media, acc.passives) }, stubs - hidden, rows, folded)
    }

    private fun rank(d: Device) = when (d.objectTypeId) {
        "ont", "modem" -> 0
        "router", "firewall" -> 1
        "switch", "wifi-controller" -> 2
        else -> 3
    }
}
