package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.*

/** What the operator needs to label and connect a port; the path itself is [PathSchematics]. */
data class PortSummary(
    val port: Port,
    val device: Device,
    val state: ConnectionState,
    val cable: Cable?,
    val photos: Int,
    /** Photos of [cable]; zero without a cable. */
    val cablePhotos: Int = 0,
)

object PortSummaries {
    fun of(project: Project, portId: String, graph: ConnectionGraph = ConnectionGraph(project), index: ProjectIndex = ProjectIndex(project)): PortSummary? {
        val ref = index.port(portId) ?: return null
        val cable = project.cables.firstOrNull { it.portAId == portId || it.portBId == portId }
        return PortSummary(ref.port, ref.device, graph.state(portId), cable,
            project.attachments.count { it.targetType == AttachmentTargetType.PORT && it.targetId == portId },
            cable?.let { c -> project.attachments.count { it.targetType == AttachmentTargetType.CABLE && it.targetId == c.id } } ?: 0)
    }
}

/** A cabled port is documented by a photo of the port or of its cable. */
object PhotoCoverage {
    /** Ids of ports and cables with at least one photo. */
    fun photographed(project: Project): Set<String> = project.attachments
        .filter { it.targetType == AttachmentTargetType.PORT || it.targetType == AttachmentTargetType.CABLE }
        .mapNotNullTo(HashSet()) { it.targetId }

    /** Cabled ports of [device] without a photo of the port or of its cable, in device order. */
    fun missing(project: Project, device: Device, photographed: Set<String> = photographed(project)): List<Port> {
        val cableOf = cablesByPort(project)
        return device.ports.filter { p -> cableOf[p.id]?.let { c -> p.id !in photographed && c.id !in photographed } ?: false }
    }

    internal fun cablesByPort(project: Project): Map<String, Cable> = buildMap {
        // First cable wins, as in [PortSummaries.of] (a second one is a conflict).
        project.cables.forEach { c -> listOfNotNull(c.portAId, c.portBId).forEach { getOrPut(it) { c } } }
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
