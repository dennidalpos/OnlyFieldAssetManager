package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.display.GlobalSearch
import com.onlyfield.assetmanager.core.display.HitKind
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSearchTest {
    private val floor = Area(name = "Terra")
    private val rack = Rack(name = "RK-A", areaId = floor.id)
    private val sw = Device(technicalName = "SW-01", ipAddress = "10.0.0.11", rackId = rack.id, positionU = 1)
        .let { it.copy(ports = (1..8).map { n -> Port(deviceId = it.id, name = "P$n") }) }
    private val sw10 = Device(technicalName = "SW-010", areaId = floor.id)
    private val loose = Device(technicalName = "AP-99", serialNumber = "SN123")
    private val project = HardwareConfigurator.connect(
        Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, racks = listOf(rack),
            sites = listOf(Site(name = "Municipio", areas = listOf(floor), devices = listOf(sw, sw10, loose)))),
        sw.ports[4].id, null, CableMedium.ETHERNET_COPPER)
        .let { p -> p.copy(cables = p.cables.map { it.copy(codeOrLabel = "C-042") }) }
    private val search = GlobalSearch(project)

    @Test fun exactMatchComesFirstAndMapTargetIsTheFloor() {
        val hits = search.search("SW-01")
        assertEquals("SW-01", hits.first().title)
        assertEquals(floor.id, hits.first().areaId)
        assertEquals("Municipio › Terra › RK-A", hits.first().place)
        assertTrue(hits.any { it.title == "SW-010" })
    }

    @Test fun findsByIpSerialPortAndCable() {
        assertEquals(sw.id, search.search("10.0.0.11").single().id)
        val bySerial = search.search("sn123").single()
        assertEquals(loose.id, bySerial.id)
        // No floor: the host opens the editor instead of the map.
        assertNull(bySerial.areaId)
        val port = search.search("SW-01 P5").first()
        assertEquals(HitKind.PORT, port.kind)
        assertEquals(sw.ports[4].id, port.id)
        assertEquals(sw.id, port.focus.id)
        assertEquals(HitKind.CABLE, search.search("C-042").single().kind)
    }

    @Test fun barePortNamesDoNotFloodResults() {
        assertTrue(search.search("P1").none { it.kind == HitKind.PORT })
    }

    @Test fun recentKeepsOrderAndDropsDeletedItems() {
        assertEquals(listOf(loose.id, sw.id), search.recent(listOf(loose.id, "gone", sw.id)).map { it.id })
    }
}
