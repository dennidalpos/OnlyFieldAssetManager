package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.SiteTopology
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteTopologyTest {
    private fun device(id: String) = Device(id = id, technicalName = id, objectTypeId = "switch", ports = listOf(Port(id = "$id-1", deviceId = id, name = "P1")))

    @Test fun sitesAreJoinedByCrossingCablesAndWanVpn() {
        val project = Project(name = "P", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(id = "a", name = "A", devices = listOf(device("a1"), device("a2"))), Site(id = "b", name = "B", devices = listOf(device("b1"))), Site(id = "c", name = "C")),
            cables = listOf(Cable(portAId = "a1-1", portBId = "b1-1", medium = CableMedium.RADIO), Cable(portAId = "a1-1", portBId = "a2-1")),
            wanVpnConnections = listOf(
                WanVpnConnection(name = "S2S", type = WanVpnType.VPN, localEndpointDeviceId = "a1", remoteEndpointDeviceId = "b1"),
                WanVpnConnection(name = "Fibra A", providerOrCarrier = "TIM", localEndpointDeviceId = "a1"),
                WanVpnConnection(name = "Fibra B", providerOrCarrier = "tim", localEndpointDeviceId = "b1"),
                WanVpnConnection(name = "Interna", localEndpointDeviceId = "a1", remoteEndpointDeviceId = "a2"),
                WanVpnConnection(name = "Senza apparati", remoteEndpointSiteDescription = "Roma")))
        val overview = SiteTopology.build(project, perRow = 2)
        // Surveyed sites first, then one external node per provider; unplaceable connections are left out.
        assertEquals(listOf("a", "b", "c", "external:tim"), overview.nodes.map { it.id })
        assertEquals(listOf(2, 2), overview.rowSizes)
        val ab = overview.links.single { it.a == "a" && it.b == "b" }
        assertEquals(1, ab.cables)
        assertTrue(ab.radio)
        assertEquals(listOf("S2S"), ab.connections.map { it.name })
        assertEquals(setOf("a", "b"), overview.links.filter { it.b == "external:tim" }.map { it.a }.toSet())
        assertEquals(3, overview.links.size)
    }
}
