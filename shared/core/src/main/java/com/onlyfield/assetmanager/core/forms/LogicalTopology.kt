package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.*

/** A device inside a subnet or VLAN box; [groups] > 1 means it joins several boxes (router, firewall, L3 switch). */
data class LogicalMember(val device: Device, val address: String?, val gateway: Boolean, val groups: Int)

/** A subnet (with its VLAN, when linked) or a VLAN without subnet, and the devices that live in it. */
data class LogicalGroup(val id: String, val subnet: Subnet?, val vlanNumber: Int?, val vlanName: String?, val members: List<LogicalMember>)

data class LogicalTopology(
    val groups: List<LogicalGroup>,
    /** Active devices no subnet or VLAN claims. */
    val unassigned: List<Device>,
    /** WAN/VPN connections ending on a device in scope. */
    val connections: List<WanVpnConnection>,
)

/**
 * Logical view built only from what was surveyed: a device belongs to a subnet through its IP or a
 * logical interface, and to a VLAN through an interface or the access port it is wired to.
 */
object LogicalTopologies {
    fun build(project: Project, siteId: String? = null): LogicalTopology {
        val index = ProjectIndex(project)
        val scope = (if (siteId == null) index.devices else project.sites.find { it.id == siteId }?.devices.orEmpty())
        val inScope = scope.map { it.id }.toSet()
        val vlanOf = project.vlans.associateBy { it.id }
        val nets = project.subnets.mapNotNull { s -> net(s.cidrBlock)?.let { s to it } }

        class Hit(val device: Device, val address: String?)
        val hits = linkedMapOf<String, MutableList<Hit>>()
        fun groupsFor(address: String?, cidr: String?, vlan: Int?): List<String> {
            val ip = ip(address)
            val bySubnet = when {
                // The most specific subnet wins when ranges nest.
                ip != null -> nets.filter { it.second.contains(ip) }.maxByOrNull { it.second.prefix }?.let { listOf(it.first) }
                else -> net(cidr)?.let { wanted -> nets.filter { it.second.base == wanted.base && it.second.prefix == wanted.prefix }.map { it.first } }
            }.orEmpty().ifEmpty { vlan?.let { n -> project.subnets.filter { vlanOf[it.vlanId]?.vlanId == n } }.orEmpty() }
            return bySubnet.map { "subnet:${it.id}" }.ifEmpty { listOfNotNull(vlan?.let { "vlan:$it" }) }
        }
        fun add(device: Device?, address: String?, cidr: String?, vlan: Int?) {
            if (device == null || device.id !in inScope) return
            groupsFor(address, cidr, vlan).forEach { hits.getOrPut(it) { mutableListOf() } += Hit(device, address?.trim()?.substringBefore('/')?.takeIf(String::isNotEmpty)) }
        }
        scope.forEach { add(it, it.ipAddress, null, null) }
        project.logicalInterfaces.forEach { add(index.device(it.deviceId), it.ipAddress, it.subnetCidr, it.vlanId) }
        // An access port puts whatever is wired to it in its VLAN.
        val graph = ConnectionGraph(project)
        for (m in project.portVlanMemberships) {
            val vlan = m.untaggedVlanId?.takeIf { m.mode == PortVlanMode.ACCESS || m.mode == PortVlanMode.UNTAGGED } ?: continue
            val own = index.port(m.portId)?.device ?: continue
            if (!graph.occupied(m.portId)) continue
            val path = PathSchematics.of(project, m.portId, graph, index) ?: continue
            val far = listOf(path.stations.first(), path.stations.last()).firstOrNull { it.device?.id != own.id && !it.passive }?.device
            add(far, null, null, vlan)
        }

        // Boxes per device: more than one marks what routes between them.
        val memberships = mutableMapOf<String, Int>()
        hits.values.forEach { list -> list.map { it.device.id }.distinct().forEach { memberships.merge(it, 1, Int::plus) } }
        fun members(id: String, gateway: String?) = hits[id].orEmpty().groupBy { it.device.id }.values.map { same ->
            val address = same.firstNotNullOfOrNull { it.address }
            LogicalMember(same.first().device, address, gateway != null && same.any { it.address == gateway }, memberships.getValue(same.first().device.id))
        }.sortedWith(compareBy({ !it.gateway }, { it.groups <= 1 }, { it.device.technicalName.lowercase() }))

        val subnetGroups = project.subnets.map { s ->
            val vlan = vlanOf[s.vlanId]
            LogicalGroup("subnet:${s.id}", s, vlan?.vlanId, vlan?.name, members("subnet:${s.id}", s.gatewayIp?.trim()?.takeIf(String::isNotEmpty)))
        }
        val covered = subnetGroups.mapNotNull { it.vlanNumber }.toSet()
        val vlanNumbers = (project.vlans.map { it.vlanId } + hits.keys.filter { it.startsWith("vlan:") }.map { it.removePrefix("vlan:").toInt() }).distinct().filter { it !in covered }
        val vlanGroups = vlanNumbers.map { n -> LogicalGroup("vlan:$n", null, n, project.vlans.find { it.vlanId == n }?.name, members("vlan:$n", null)) }
        // Inside a site, empty boxes are shown only when they are scoped to it.
        val visible = (subnetGroups + vlanGroups).filter { g ->
            siteId == null || g.members.isNotEmpty() || g.subnet?.let { it.scopeType == VlanScopeType.SITE && it.scopeTargetId == siteId } == true
        }.sortedWith(compareBy({ it.vlanNumber ?: Int.MAX_VALUE }, { net(it.subnet?.cidrBlock)?.base ?: Long.MAX_VALUE }, { it.id }))
        return LogicalTopology(
            visible,
            scope.filter { !it.isPassive() && it.id !in memberships }.sortedBy { it.technicalName.lowercase() },
            project.wanVpnConnections.filter { it.localEndpointDeviceId in inScope || it.remoteEndpointDeviceId in inScope },
        )
    }

    private class Net(val base: Long, val prefix: Int) {
        fun contains(address: Long) = prefix == 0 || (address shr (32 - prefix)) == (base shr (32 - prefix))
    }

    private fun ip(text: String?): Long? {
        val parts = text?.trim()?.substringBefore('/')?.split('.')?.takeIf { it.size == 4 } ?: return null
        return parts.fold(0L) { acc, part -> acc * 256 + (part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null) }
    }

    private fun net(cidr: String?): Net? {
        val parts = cidr?.trim()?.split('/')?.takeIf { it.size == 2 } ?: return null
        val prefix = parts[1].toIntOrNull()?.takeIf { it in 0..32 } ?: return null
        val address = ip(parts[0]) ?: return null
        return Net(if (prefix == 0) 0 else (address shr (32 - prefix)) shl (32 - prefix), prefix)
    }
}
