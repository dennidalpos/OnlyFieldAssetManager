package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.onboarding.*
import org.junit.Assert.*
import org.junit.Test

class NewSiteWizardTest {
    @Test fun requiredListsBlockNavigation() {
        val start = NewSiteWizard()
        assertSame(start, start.next())
        assertSame(start, start.skip())
        val bu = start.update { it.copy(projectName = "Ospedale") }.next()
        assertEquals(setOf("businessUnits"), bu.errors().keys)
        assertSame(bu, bu.next())
        val floors = bu.addBusinessUnit("Padiglione A").next()
        assertEquals(NewSiteStep.AREA, floors.step)
        assertSame(floors, floors.next())
    }
    @Test fun multipleBusinessUnitsAndFloorsKeepTheirIds() {
        val bus = NewSiteWizard().update { it.copy(projectName = " Ospedale ", customer = "ASL") }
            .addBusinessUnit("Padiglione A").addBusinessUnit("Padiglione B")
        val a = bus.draft.businessUnits.first().id
        val b = bus.draft.businessUnits.last().id
        val ready = bus.addArea(a, "Terra").addArea(a, "Primo").addArea(b, "CED")
        val p = ready.buildProject(42)
        assertEquals(listOf(a, b), p.businessUnits.map { it.id })
        assertEquals(3, p.businessUnits.sumOf { it.areas.size })
        assertTrue(p.businessUnits.all { it.devices.isEmpty() })
        assertEquals("Ospedale", p.name)
        assertEquals(42L, p.updatedEpochMs)
    }
    @Test fun emptyAdditionalBusinessUnitAndOptionalPassword() {
        val bus = NewSiteWizard().update { it.copy(projectName = "Sito") }.addBusinessUnit("BU1").addBusinessUnit("BU vuota")
        val w = bus.addArea(bus.draft.businessUnits.first().id, "CED").copy(step = NewSiteStep.PASSWORD)
        assertNull(w.password)
        assertEquals(2, w.buildProject().businessUnits.size)
        val mismatch = w.update { it.copy(password = "abc", passwordConfirm = "abd") }
        assertFalse(mismatch.canProceed)
        assertTrue(mismatch.skip().canProceed)
        assertNull(mismatch.skip().password)
    }
    @Test(expected = IllegalArgumentException::class) fun cannotBuildWithoutFloor() {
        NewSiteWizard().update { it.copy(projectName = "Sito") }.addBusinessUnit("BU").buildProject()
    }
}
