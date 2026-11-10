package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.PhysicalTopology
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoTopologyTest {
    private val project = DemoSeed.build()
    private val devices = project.sites.flatMap { it.devices }
    private fun id(name: String) = devices.single { it.technicalName == name }.id
    private fun linked(t: com.onlyfield.assetmanager.core.forms.Topology, a: String, b: String) =
        t.links.single { setOf(it.a, it.b) == setOf(id(a), id(b)) }

    @Test fun activeDevicesOnlyWithPassivesCollapsed() {
        val topology = PhysicalTopology.build(project)
        assertEquals(devices.count { !it.isPassive() }, topology.nodes.size)
        assertTrue(topology.nodes.none { it.device.isPassive() || it.outside })
        // Access point → outlet → junction box → panel → switch: one link, three passives in between.
        assertEquals(3, linked(topology, "AP-TEA-PT-01", "SW-TEA-PT-A").passives)
        val radio = linked(topology, "SW-COM-P1-A", "SW-MED-PT-A")
        assertTrue(CableMedium.RADIO in radio.media)
        assertEquals(1, radio.paths)
        // Each network starts from its most upstream device, in its own band of rows.
        val ont = topology.nodes.single { it.device.technicalName == "ONT-COM-01" }
        assertEquals(0, ont.level)
        assertEquals(0, ont.row)
        val theatre = topology.nodes.single { it.device.technicalName == "ONT-TEA-01" }
        assertEquals(0, theatre.level)
        assertTrue(topology.nodes.filter { it.row == theatre.row }.all { it.device.technicalName.contains("TEA") })
    }

    @Test fun siteFilterKeepsFarEndsOutsideAndRowsWrap() {
        val med = project.sites.single { it.code == "MED" }
        val topology = PhysicalTopology.build(project, siteId = med.id, perRow = 3)
        val outside = topology.nodes.filter { it.outside }.map { it.device.technicalName }
        assertTrue(outside.toString(), "SW-COM-P1-A" in outside)
        assertTrue(topology.nodes.filter { !it.outside }.all { n -> med.devices.any { it.id == n.device.id } })
        assertTrue(topology.rowSizes.all { it in 1..3 })
        assertEquals(topology.nodes.size, topology.rowSizes.sum())
    }

    @Test fun endDevicesFoldIntoTheirSwitch() {
        val full = PhysicalTopology.build(project)
        val folded = PhysicalTopology.build(project, foldEndpoints = true)
        assertTrue(folded.nodes.none { it.device.technicalName == "AP-TEA-PT-01" })
        assertTrue(folded.folded.getValue(id("SW-TEA-PT-A")).any { it.technicalName == "AP-TEA-PT-01" })
        assertEquals(full.nodes.size, folded.nodes.size + folded.folded.values.sumOf { it.size })
        // Infrastructure stays: the radio link between the two switches is still drawn.
        linked(folded, "SW-COM-P1-A", "SW-MED-PT-A")
    }
}
