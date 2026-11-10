package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.forms.PathSchematics
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathSchematicTest {
    private val sw = Device(id = "sw", technicalName = "SW-01", ports = listOf(Port(id = "sw-1", deviceId = "sw", name = "P1")))
    private val outlet = Device(id = "pr", technicalName = "PR-01", objectTypeId = "outlet", ports = listOf(
        Port(id = "pr-f", deviceId = "pr", name = "P1", hardware = PortHardware(side = PortSide.FRONT)),
        Port(id = "pr-r", deviceId = "pr", name = "P1", hardware = PortHardware(side = PortSide.REAR))))
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "Sede", devices = listOf(sw, outlet))),
        panelMappings = listOf(PanelMapping(portAId = "pr-f", portBId = "pr-r")))
        .let { HardwareConfigurator.connect(it, "pr-r", "sw-1", CableMedium.ETHERNET_COPPER) }

    @Test fun anOpenRunReadsFromTheActiveEnd() {
        // Queried from the free front of the outlet: the drawing starts at the switch and ends open.
        val path = PathSchematics.of(project, "pr-f")!!
        assertEquals(listOf("SW-01", "PR-01"), path.stations.map { it.device?.technicalName })
        assertEquals(listOf("pr-r", "pr-f"), path.stations[1].ports.map { it.id })
        assertEquals(1, path.focus)
        assertTrue(path.openEnd(1))
        assertEquals("PR-01/P1 – SW-01/P1", path.segments.single().label)
    }

    @Test fun aFreeActivePortIsASingleStation() {
        val free = project.copy(cables = emptyList())
        assertEquals(0, PathSchematics.of(free, "sw-1")!!.segments.size)
    }
}
