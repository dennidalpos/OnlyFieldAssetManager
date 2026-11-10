package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.onboarding.*
import org.junit.Assert.*
import org.junit.Test

class NewSiteWizardTest {
    @Test fun requiredListsBlockNavigation() {
        val start = NewSiteWizard()
        assertSame(start, start.next())
        assertSame(start, start.skip())
        val site = start.update { it.copy(projectName = "Ospedale") }.next()
        assertEquals(setOf("sites"), site.errors().keys)
        assertSame(site, site.next())
        val floors = site.addSite("Padiglione A").next()
        assertEquals(NewSiteStep.AREA, floors.step)
        assertSame(floors, floors.next())
    }
    @Test fun multipleSitesAndFloorsKeepTheirIds() {
        val sites = NewSiteWizard().update { it.copy(projectName = " Ospedale ", customer = "ASL") }
            .addSite("Padiglione A").addSite("Padiglione B")
        val a = sites.draft.sites.first().id
        val b = sites.draft.sites.last().id
        val ready = sites.addArea(a, "Terra").addArea(a, "Primo").addArea(b, "CED")
        val p = ready.buildProject(42)
        assertEquals(listOf(a, b), p.sites.map { it.id })
        assertEquals(3, p.sites.sumOf { it.areas.size })
        assertTrue(p.sites.all { it.devices.isEmpty() })
        assertEquals("Ospedale", p.name)
        assertEquals(42L, p.updatedEpochMs)
    }
    @Test fun emptyAdditionalSiteAndOptionalPassword() {
        val sites = NewSiteWizard().update { it.copy(projectName = "Sito") }.addSite("BU1").addSite("BU vuota")
        val w = sites.addArea(sites.draft.sites.first().id, "CED").copy(step = NewSiteStep.PASSWORD)
        assertNull(w.password)
        assertEquals(2, w.buildProject().sites.size)
        val mismatch = w.update { it.copy(password = "abc", passwordConfirm = "abd") }
        assertFalse(mismatch.canProceed)
        assertTrue(mismatch.skip().canProceed)
        assertNull(mismatch.skip().password)
    }
    @Test(expected = IllegalArgumentException::class) fun cannotBuildWithoutFloor() {
        NewSiteWizard().update { it.copy(projectName = "Sito") }.addSite("BU").buildProject()
    }
}
