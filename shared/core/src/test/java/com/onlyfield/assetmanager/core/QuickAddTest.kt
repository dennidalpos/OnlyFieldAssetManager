package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class QuickAddTest {
    private val area = Area(name = "Terra")
    private val site = Site(name = "BU", areas = listOf(area))
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(site))
    private fun type(id: String) = ObjectCatalog.builtins.first { it.id == id }

    @Test fun switchFromPresetIsSavedWithPortsNameAndMapPlacement() {
        val preset = DevicePresets.forType("switch")!!
        val base = MapObjectDraft(type = type("switch"), siteId = site.id, areaId = area.id)
        val draft = QuickAdd.draft(base, " SW-01 ", preset.result(preset.defaults()))
        assertTrue(draft.errors(project).isEmpty())
        val saved = draft.apply(project)
        val device = saved.sites.single().devices.single()
        assertEquals("SW-01", device.technicalName)
        assertTrue(device.ports.isNotEmpty())
        assertTrue(ObjectMap.nodes(saved, area.id).any { it.id == device.id })
    }

    @Test fun rackStartsEmptyWithChosenHeight() {
        val draft = QuickAdd.draft(MapObjectDraft.forRack(project, null), "R1", rackHeightU = 24)
        val rack = draft.apply(project).racks.single()
        assertEquals(24, rack.heightU)
    }

    @Test fun siteIsRequiredUntilChosen() {
        val base = MapObjectDraft(type = type("modem"), siteId = "", areaId = "", device = DeviceForm(siteId = null, objectTypeId = "modem"))
        assertTrue(QuickAdd.draft(base, "MD-01").errors(project).isNotEmpty())
        val chosen = QuickAdd.draft(base, "MD-01", siteId = site.id)
        assertTrue(chosen.errors(project).isEmpty())
        assertEquals("MD-01", chosen.apply(project).sites.single().devices.single().technicalName)
    }
}
