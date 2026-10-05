package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.BulkCabling
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BulkCablingTest {
    private fun ports(deviceId: String, side: PortSide?, n: Int, prefix: String = "P") =
        (1..n).map { Port(id = "$deviceId-${side ?: "X"}-$it", deviceId = deviceId, name = "$prefix$it", hardware = PortHardware(side = side)) }

    private val sw = Device(id = "sw", technicalName = "SW-01", ports = ports("sw", null, 6))
    private val pp = Device(id = "pp", technicalName = "PP-01", ports = ports("pp", PortSide.FRONT, 4) + ports("pp", PortSide.REAR, 4, "R"))
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "Sede", devices = listOf(sw, pp))))

    @Test fun seriesSkipsOccupiedPortsAndStaysOnOneSide() {
        // P2 is already used: the switch run continues with P3.
        val busy = HardwareConfigurator.connect(project, "sw-X-2", null, CableMedium.ETHERNET_COPPER)
        assertEquals(4, BulkCabling.maxCount(busy, "sw-X-1", "pp-FRONT-1"))
        val pairs = BulkCabling.pairs(busy, "sw-X-1", "pp-FRONT-1", 10)
        assertEquals(listOf("P1" to "P1", "P3" to "P2", "P4" to "P3", "P5" to "P4"), pairs.map { it.first.name to it.second.name })

        val cabled = BulkCabling.connect(busy, pairs, CableMedium.ETHERNET_COPPER)
        assertEquals(5, cabled.cables.size)
        assertEquals("SW-01/P5 – PP-01/P4", cabled.cables.first { it.portAId == "sw-X-5" }.codeOrLabel)
        // Rear ports were never touched.
        assertEquals(0, cabled.cables.count { it.portBId?.contains("REAR") == true })
    }

    @Test fun nextFreeMovesAlongTheSameSide() {
        val cabled = BulkCabling.connect(project, BulkCabling.pairs(project, "pp-FRONT-1", "sw-X-1", 1), CableMedium.ETHERNET_COPPER)
        assertEquals("pp-FRONT-2", BulkCabling.nextFree(cabled, "pp-FRONT-1"))
        assertNull(BulkCabling.nextFree(cabled, "pp-FRONT-4"))
    }
}
