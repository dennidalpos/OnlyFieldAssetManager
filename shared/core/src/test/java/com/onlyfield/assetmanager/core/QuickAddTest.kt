package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class QuickAddTest {
    private val area = Area(name = "Terra")
    private val bu = BusinessUnit(name = "BU", areas = listOf(area))
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(bu))
    private fun type(id: String) = ObjectCatalog.builtins.first { it.id == id }

    @Test fun switchFromPresetIsSavedWithPortsNameAndMapPlacement() {
        val preset = DevicePresets.forType("switch")!!
        val base = MapObjectDraft(type = type("switch"), buId = bu.id, areaId = area.id)
        val draft = QuickAdd.draft(base, " SW-01 ", preset.result(preset.defaults()))
        assertTrue(draft.errors(project).isEmpty())
        val saved = draft.apply(project)
        val device = saved.businessUnits.single().devices.single()
        assertEquals("SW-01", device.technicalName)
        assertTrue(device.ports.isNotEmpty())
        assertTrue(ObjectMap.nodes(saved, area.id).any { it.id == device.id })
    }

    @Test fun rackStartsEmptyWithChosenHeight() {
        val draft = QuickAdd.draft(MapObjectDraft.forRack(project, null), "R1", rackHeightU = 24)
        val rack = draft.apply(project).racks.single()
        assertEquals(24, rack.heightU)
        assertTrue(QuickAdd.needsDetails(draft, hasPreset = false, hasErrors = false))
    }

    @Test fun businessUnitIsRequiredUntilChosen() {
        val base = MapObjectDraft(type = type("modem"), buId = "", areaId = "", device = DeviceForm(businessUnitId = null, objectTypeId = "modem"))
        assertTrue(QuickAdd.draft(base, "MD-01").errors(project).isNotEmpty())
        val chosen = QuickAdd.draft(base, "MD-01", buId = bu.id)
        assertTrue(chosen.errors(project).isEmpty())
        assertEquals("MD-01", chosen.apply(project).businessUnits.single().devices.single().technicalName)
        assertFalse(QuickAdd.needsDetails(chosen, hasPreset = false, hasErrors = false))
    }
}
