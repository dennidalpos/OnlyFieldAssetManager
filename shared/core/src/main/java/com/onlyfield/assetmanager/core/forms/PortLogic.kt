package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*

/** One cell of the port panel: free/occupied first, problems as a secondary flag. */
data class PortCell(
    val port: Port,
    val occupied: Boolean,
    val warning: Boolean,
    val poeCapable: PoeStandard?,
    val poe: PoeMapping?,
    val vlan: PortVlanMembership?,
    val peer: String?,
)

/** Bulk logical edits on ports, stored in the existing PoE and VLAN entities. */
object PortLogic {
    fun panel(project: Project, device: Device, graph: ConnectionGraph = ConnectionGraph(project), index: ProjectIndex = ProjectIndex(project)): List<PortCell> {
        val poe = project.poeMappings.associateBy { it.portId }
        val vlans = project.portVlanMemberships.associateBy { it.portId }
        return device.ports.map { port ->
            val state = graph.state(port.id)
            val cable = project.cables.firstOrNull { it.portAId == port.id || it.portBId == port.id }
            val peer = cable?.let { c -> if (c.portAId == port.id) c.portBId?.let(index::portLabel) ?: index.device(c.deviceBId)?.technicalName
                else c.portAId?.let(index::portLabel) ?: index.device(c.deviceAId)?.technicalName }
            PortCell(port, graph.occupied(port.id), state == ConnectionState.CONFLICT || state == ConnectionState.INCOMPLETE,
                port.hardware.poeStandard, poe[port.id], vlans[port.id], peer)
        }
    }

    /** Ids of ports that exist in [project]; stale UI selections must never create orphan rows. */
    private fun existing(project: Project, portIds: Collection<String>): List<String> {
        val ids = project.sites.flatMap { site -> site.devices.flatMap { d -> d.ports.map { it.id } } }.toSet()
        return portIds.filter { it in ids }
    }

    /** [standard] null removes PoE from the ports. */
    fun setPoe(project: Project, portIds: Collection<String>, standard: PoeStandard?, role: PoeRole = PoeRole.PSE_SOURCE): Project =
        existing(project, portIds).fold(project) { p, id ->
            val existing = p.poeMappings.find { it.portId == id }
            when {
                standard == null -> existing?.let { ProjectEdits.deletePoeMapping(p, it.id) } ?: p
                else -> ProjectEdits.addOrUpdatePoeMapping(p, (existing ?: PoeMapping(portId = id)).copy(standard = standard, role = role))
            }
        }

    /** Access: [untagged] only. Trunk: [tagged] plus optional native [untagged]. Missing VLANs are created at project scope. */
    fun setVlan(project: Project, portIds: Collection<String>, mode: PortVlanMode, untagged: Int?, tagged: List<Int> = emptyList()): Project {
        val numbers = (listOfNotNull(untagged) + tagged).distinct()
        require(numbers.all { it in 1..4094 }) { "VLAN outside 1..4094" }
        val ports = existing(project, portIds).ifEmpty { return project }
        val withVlans = numbers.filter { n -> project.vlans.none { it.vlanId == n } }
            .fold(project) { p, n -> ProjectEdits.addVlan(p, Vlan(vlanId = n, name = "VLAN $n")) }
        return ports.fold(withVlans) { p, id ->
            val existing = p.portVlanMemberships.find { it.portId == id }
            ProjectEdits.addOrUpdatePortVlanMembership(p, (existing ?: PortVlanMembership(portId = id)).copy(
                mode = mode, untaggedVlanId = untagged, nativeVlanId = if (mode == PortVlanMode.TRUNK) untagged else null,
                taggedVlanIds = if (mode == PortVlanMode.TRUNK) tagged.distinct().sorted() else emptyList()))
        }
    }

    fun clearVlan(project: Project, portIds: Collection<String>): Project =
        project.portVlanMemberships.filter { it.portId in portIds }.fold(project) { p, m -> ProjectEdits.deletePortVlanMembership(p, m.id) }

    /** Subnets linked to the VLAN number through [Subnet.vlanId] (a VLAN entity id). */
    fun subnets(project: Project, vlanNumber: Int?): List<Subnet> {
        val ids = project.vlans.filter { it.vlanId == vlanNumber }.map { it.id }.toSet()
        return project.subnets.filter { it.vlanId in ids }
    }

    /** Allocated PoE watts on the device: explicit allocation or the standard's maximum. */
    fun poeLoad(project: Project, device: Device): Double {
        val ports = device.ports.map { it.id }.toSet()
        return project.poeMappings.filter { it.portId in ports && it.role == PoeRole.PSE_SOURCE }
            .sumOf { it.allocatedPowerWatts ?: DevicePresets.watts(it.standard) }
    }
}
