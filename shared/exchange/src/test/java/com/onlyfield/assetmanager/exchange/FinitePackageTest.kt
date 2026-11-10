package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Locale

class FinitePackageTest {
    @Test fun finiteValuesRoundTripAndInvalidValuesLeaveOutputEmpty() {
        val device = Device(technicalName = "Device", mountingType = MountingType.OUT_OF_RACK, hardware = HardwareSpec(poeBudgetWatts = 45.5))
        val project = Project(name = "Finite package", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Site", devices = listOf(device))), cables = listOf(Cable(lengthValue = .5)),
            powerFeeds = listOf(PowerFeed(deviceId = device.id, feedName = "Custom", loadWatts = 12.5, loadVa = 20.5)))
        val imported = PackageSerializer.importPackage(PackageSerializer.exportPackage(project))
        assertNotNull(imported.pkg)
        imported.pkg!!.use { assertEquals(project, it.project) }
        for (language in listOf("it", "en", "es")) for (number in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val i18n = Messages(Locale.forLanguageTag(language))
            val output = ByteArrayOutputStream()
            try {
                PackageSerializer.exportPackageToStream(output, project.copy(powerFeeds = listOf(project.powerFeeds.single().copy(loadWatts = number))), i18n = i18n)
                fail("Non-finite value exported")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.contains(i18n.text("text.eb98296d7970")))
                assertEquals(0, output.size())
            }
        }
    }
}
