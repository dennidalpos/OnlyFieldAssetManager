package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.*

/** A port reached through a cable along the physical path; internal passages are implied. */
data class PathHop(val device: Device, val port: Port?, val cable: Cable)

/** What the operator needs to label and connect a port; VLAN, PoE and hardware stay secondary. */
data class PortSummary(
    val port: Port,
    val device: Device,
    val state: ConnectionState,
    val cable: Cable?,
    val hops: List<PathHop>,
    /** On a pass-through: the path leaving from the other side (front ↔ rear). */
    val backHops: List<PathHop>,
    /** Active devices at the ends of a complete path. */
    val terminals: List<PathHop>,
    val photos: Int,
)

object PortSummaries {
    fun of(project: Project, portId: String, graph: ConnectionGraph = ConnectionGraph(project), index: ProjectIndex = ProjectIndex(project)): PortSummary? {
        val ref = index.port(portId) ?: return null
        val state = graph.state(portId)
        val partner = if (ref.device.isPassive()) project.panelMappings.firstNotNullOfOrNull { m ->
            when (portId) { m.portAId -> m.portBId; m.portBId -> m.portAId; else -> null }
        } else null
        val hops = hops(graph, index, portId)
        val back = partner?.let { hops(graph, index, it) }.orEmpty()
        return PortSummary(ref.port, ref.device, state, project.cables.firstOrNull { it.portAId == portId || it.portBId == portId }, hops, back,
            listOfNotNull(back.lastOrNull(), hops.lastOrNull()).filter { state == ConnectionState.COMPLETE && !it.device.isPassive() },
            project.attachments.count { it.targetType == AttachmentTargetType.PORT && it.targetId == portId })
    }

    private fun hops(graph: ConnectionGraph, index: ProjectIndex, portId: String): List<PathHop> =
        graph.trace(portId).mapNotNull { step ->
            val cable = step.cable ?: return@mapNotNull null
            val from = step.currentPort?.id
            val far = if (cable.portAId == from) cable.portBId else cable.portAId
            val farDevice = far?.let(index::port)?.device ?: index.device(if (cable.portAId == from) cable.deviceBId else cable.deviceAId) ?: return@mapNotNull null
            PathHop(farDevice, far?.let(index::port)?.port, cable)
        }
}

object CableLabels {
    /** Text to write on the cable: both ends as `DEVICE/PORT`, `?` when unknown; Windows-1252 safe for PDF labels. */
    fun suggest(project: Project, cable: Cable, index: ProjectIndex = ProjectIndex(project)): String {
        fun end(portId: String?, deviceId: String?) = index.port(portId)?.let { "${it.device.technicalName}/${it.port.name}" }
            ?: index.device(deviceId)?.technicalName ?: "?"
        return "${end(cable.portAId, cable.deviceAId)} – ${end(cable.portBId, cable.deviceBId)}"
    }
}
