package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.*

/** Front labels select the actual rear endpoints of fixed cabling. */
object PassiveCabling {
    fun nextFront(project: Project, frontId: String): String? {
        val ref = ProjectIndex(project).port(frontId) ?: return null
        val graph = ConnectionGraph(project)
        return ref.device.ports.dropWhile { it.id != frontId }.drop(1)
            .filter { it.hardware.side == PortSide.FRONT && it.hardware.group == ref.port.hardware.group }
            .firstOrNull { front -> rear(project, front.id)?.let { !graph.occupied(it.id) } == true }?.id
    }

    fun supported(device: Device): Boolean = device.objectTypeId in setOf("patch-panel", "outlet", "junction-box") || device.category == DeviceCategory.PATCH_PANEL

    fun rear(project: Project, frontId: String): Port? {
        val index = ProjectIndex(project)
        val front = index.port(frontId) ?: return null
        if (!supported(front.device) || front.port.hardware.side != PortSide.FRONT) return null
        val mappings = project.panelMappings.filter { it.portAId == frontId || it.portBId == frontId }
        val mapping = mappings.singleOrNull()?.takeUnless { it.isUnknownPassage } ?: return null
        val partner = if (mapping.portAId == frontId) mapping.portBId else mapping.portAId
        if (partner == null || project.panelMappings.count { it.portAId == partner || it.portBId == partner } != 1) return null
        return partner.let(index::port)?.takeIf { it.device.id == front.device.id && it.port.hardware.side == PortSide.REAR }?.port
    }

    fun pairs(project: Project, from: String, to: String, count: Int): List<Pair<Port, Port>> {
        val index = ProjectIndex(project)
        val graph = ConnectionGraph(project)
        fun run(id: String): List<Port> {
            val ref = index.port(id) ?: return emptyList()
            val start = ref.device.ports.indexOfFirst { it.id == id }
            return ref.device.ports.drop(start).filter { it.hardware.side == PortSide.FRONT && it.hardware.group == ref.port.hardware.group }
                .mapNotNull { rear(project, it.id) }.filterNot { graph.occupied(it.id) }
        }
        if (rear(project, from)?.let { graph.occupied(it.id) } != false || rear(project, to)?.let { graph.occupied(it.id) } != false) return emptyList()
        return run(from).zip(run(to)).take(count.coerceAtLeast(0))
    }
}
