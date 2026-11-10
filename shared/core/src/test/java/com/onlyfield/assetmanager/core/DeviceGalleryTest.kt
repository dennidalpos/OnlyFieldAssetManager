package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.DeviceGalleries
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceGalleryTest {
    private fun device(id: String, type: String, area: String?, rack: String? = null, unit: Int? = null) =
        Device(id = id, technicalName = id.uppercase(), objectTypeId = type, areaId = area, rackId = rack, positionU = unit)
    private val project = Project(name = "Rete", createdEpochMs = 1, updatedEpochMs = 1, racks = listOf(Rack(id = "k", name = "RACK-A", areaId = "t")),
        sites = listOf(
            Site(id = "a", name = "Municipio", areas = listOf(Area(id = "t", name = "Terra"), Area(id = "u", name = "Primo")),
                devices = listOf(device("sw", "switch", "t", "k", 10), device("rt", "router", "t", "k", 20), device("fw", "firewall", "t"), device("pc", "workstation", "t"), device("ap", "switch", "u"))),
            Site(id = "b", name = "Biblioteca", devices = listOf(device("x", "switch", null)))))

    @Test fun racksComeFirstTopUnitFirstAndEndDevicesAreOptional() {
        val floor = DeviceGalleries.build(project, "a", "t")
        assertEquals(listOf("RACK-A" to listOf("RT", "SW"), null to listOf("FW")), floor.map { it.rack?.name to it.devices.map { d -> d.technicalName } })
        assertEquals(listOf("FW", "PC"), DeviceGalleries.build(project, "a", "t", endpoints = true).last().devices.map { it.technicalName })
    }

    @Test fun scopeFollowsSiteAndContainer() {
        assertEquals(listOf("X"), DeviceGalleries.build(project, "b").flatMap { it.devices }.map { it.technicalName })
        assertEquals(setOf("RT", "SW", "FW", "AP"), DeviceGalleries.build(project, "a").flatMap { it.devices }.map { it.technicalName }.toSet())
        assertEquals(listOf("RT", "SW"), DeviceGalleries.build(project, "a", "t", ObjectRef(PlacementTargetType.RACK, "k")).flatMap { it.devices }.map { it.technicalName })
    }
}
