package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.onboarding.NewSiteStep
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class NewSiteWizardTest {
    @Test
    fun requiredStepsBlockNextUntilFilled() {
        val start = NewSiteWizard()
        assertSame(start, start.next())
        assertSame(start, start.skip()) // project step is not skippable
        val area = start.update { it.copy(projectName = "Ospedale") }.next().next()
        assertEquals(NewSiteStep.AREA, area.step)
        assertEquals(setOf("area"), area.errors().keys)
    }

    @Test
    fun buildsProjectWithAreaAndDevice() {
        val w = NewSiteWizard()
            .update { it.copy(projectName = " Ospedale ", customer = "ASL 1") }.next()
            .update { it.copy(businessUnit = "Padiglione A") }.next()
            .update { it.copy(area = "Sala server") }.next()
            .update { it.copy(deviceName = "SW-CORE", deviceIp = "10.0.0.1") }.next()
        assertEquals(NewSiteStep.PASSWORD, w.step)
        val p = w.skip().buildProject(now = 42L)
        assertEquals("Ospedale", p.name)
        assertEquals("ASL 1", p.description)
        val bu = p.businessUnits.single()
        assertEquals("Padiglione A", bu.name)
        val device = bu.devices.single()
        assertEquals(bu.areas.single().id, device.areaId)
        assertEquals("10.0.0.1", device.ipAddress)
        assertEquals(42L, p.updatedEpochMs)
        assertNull(w.skip().password)
    }

    @Test
    fun skippedDeviceAndMismatchedPassword() {
        val w = NewSiteWizard(step = NewSiteStep.DEVICE).update { it.copy(projectName = "X", area = "A", deviceIp = "999.1.1.1") }
        assertEquals(setOf("deviceName", "deviceIp"), w.errors().keys)
        val pw = w.skip().update { it.copy(password = "abc", passwordConfirm = "abd") }
        assertEquals(NewSiteStep.PASSWORD, pw.step)
        assertEquals(setOf("passwordConfirm"), pw.errors().keys)
        assertEquals(0, pw.buildProject().businessUnits.single().devices.size)
        assertEquals("abc", pw.password)
    }
}
