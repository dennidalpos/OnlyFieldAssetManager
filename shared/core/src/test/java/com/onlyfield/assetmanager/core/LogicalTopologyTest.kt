package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.forms.LogicalTopologies
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicalTopologyTest {
    private fun device(id: String, type: String, ip: String? = null) =
        Device(id = id, technicalName = id, objectTypeId = type, ipAddress = ip, ports = listOf(Port(id = "$id-1", deviceId = id, name = "P1"), Port(id = "$id-2", deviceId = id, name = "P2")))

    private val voice = Vlan(id = "v20", vlanId = 20, name = "Voce")
    private val data = Vlan(id = "v10", vlanId = 10, name = "Dati")
    private val project = Project(name = "P", createdEpochMs = 1, updatedEpochMs = 1,
        sites = listOf(
            Site(id = "a", name = "A", devices = listOf(device("fw", "firewall"), device("sw", "switch", "192.168.10.2"), device("pc", "workstation", "192.168.10.50"), device("tel", "ip-phone"), device("ups", "ups"))),
            Site(id = "b", name = "B", devices = listOf(device("nas", "nas", "192.168.10.9")))),
        vlans = listOf(voice, data, Vlan(id = "v30", vlanId = 30, name = "Ospiti")),
        subnets = listOf(Subnet(id = "lan", cidrBlock = "192.168.10.0/24", gatewayIp = "192.168.10.1", vlanId = "v10"), Subnet(id = "mgmt", cidrBlock = "10.0.0.0/29", scopeType = VlanScopeType.SITE, scopeTargetId = "b")),
        logicalInterfaces = listOf(
            LogicalInterface(deviceId = "fw", name = "lan", ipAddress = "192.168.10.1"),
            LogicalInterface(deviceId = "fw", name = "voice", vlanId = 20)),
        portVlanMemberships = listOf(PortVlanMembership(portId = "sw-1", mode = PortVlanMode.ACCESS, untaggedVlanId = 20)),
        wanVpnConnections = listOf(WanVpnConnection(name = "Fibra", localEndpointDeviceId = "fw")))
        .let { HardwareConfigurator.connect(it, "sw-1", "tel-1", CableMedium.ETHERNET_COPPER) }

    @Test fun devicesFallIntoSubnetsAndVlans() {
        val logical = LogicalTopologies.build(project)
        // VLAN order, subnets without VLAN last; the empty VLAN and the empty subnet are still drawn.
        assertEquals(listOf("subnet:lan", "vlan:20", "vlan:30", "subnet:mgmt"), logical.groups.map { it.id })
        val lan = logical.groups.first()
        assertEquals(listOf("fw", "nas", "pc", "sw"), lan.members.map { it.device.id })
        assertTrue(lan.members.first().gateway)
        assertEquals(2, lan.members.first().groups)
        assertEquals("Dati", lan.vlanName)
        // The phone has no address: the access port it is wired to puts it in the voice VLAN.
        assertEquals(listOf("fw", "tel"), logical.groups[1].members.map { it.device.id })
        assertEquals(listOf("ups"), logical.unassigned.map { it.id })
        assertEquals(listOf("Fibra"), logical.connections.map { it.name })
    }

    @Test fun siteScopeKeepsOnlyItsDevicesAndItsOwnEmptyBoxes() {
        val b = LogicalTopologies.build(project, "b")
        assertEquals(listOf("subnet:lan", "subnet:mgmt"), b.groups.map { it.id })
        assertEquals(listOf("nas"), b.groups.first().members.map { it.device.id })
        assertFalse(b.groups.first().members.single().gateway)
        assertTrue(b.unassigned.isEmpty() && b.connections.isEmpty())
    }
}
