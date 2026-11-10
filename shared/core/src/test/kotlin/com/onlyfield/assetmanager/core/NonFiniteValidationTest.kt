package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ModelValidator
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class NonFiniteValidationTest {
    @Test fun formsRejectSpecialValuesAndOverflowInEveryLanguage() {
        for (language in listOf("it", "en", "es")) {
            val i18n = Messages(Locale.forLanguageTag(language))
            for (value in listOf("NaN", "Infinity", "-Infinity", "1e999", "-1e999")) {
                assertNull(FieldValidators.parseDecimal(value))
                assertNotNull(FieldValidators.decimal(value, i18n = i18n))
                assertTrue(CableForm(lengthValue = value).errors(i18n).containsKey("lengthValue"))
                assertTrue(PowerFeedForm(deviceId = "device", feedName = "Feed", loadWatts = value, loadVa = value)
                    .errors(i18n).keys.containsAll(listOf("loadWatts", "loadVa")))
                assertTrue(PoeForm(portId = "port", watts = value).errors(i18n).containsKey("watts"))
            }
            assertEquals(12.5, FieldValidators.parseDecimal("12,5")!!, 0.0)
            assertNull(FieldValidators.decimal("12.5", min = 0.0, i18n = i18n))
            assertNull(FieldValidators.decimal("", i18n = i18n))
        }
    }

    @Test fun modelRejectsEverySerializedNonFiniteField() {
        val area = Area(name = "Area")
        val deviceId = java.util.UUID.randomUUID().toString()
        val port = Port(deviceId = deviceId, name = "Port")
        val device = Device(id = deviceId, technicalName = "Device", mountingType = MountingType.OUT_OF_RACK,
            areaId = area.id, ports = listOf(port))
        val cable = Cable()
        val initial = Project(name = "Finite values", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Site", areas = listOf(area), devices = listOf(device))), cables = listOf(cable))
        for (number in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val hardware = HardwareSpec(poeBudgetWatts = number)
            val variants = listOf(
                initial.copy(powerFeeds = listOf(PowerFeed(deviceId = deviceId, feedName = "Feed", loadWatts = number))),
                initial.copy(powerFeeds = listOf(PowerFeed(deviceId = deviceId, feedName = "Feed", loadVa = number))),
                initial.copy(poeMappings = listOf(PoeMapping(portId = port.id, allocatedPowerWatts = number))),
                initial.copy(cables = listOf(Cable(lengthValue = number))),
                initial.copy(sites = listOf(initial.sites.single().copy(devices = listOf(device.copy(hardware = hardware))))),
                initial.copy(deviceModels = listOf(DeviceModel(name = "Model", hardware = hardware))),
                initial.copy(floorplanPlacements = listOf(FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE,
                    targetId = deviceId, xRatio = number.toFloat(), yRatio = .5f))),
                initial.copy(annotations = listOf(Annotation(areaId = area.id, x1Ratio = .5f, y1Ratio = number.toFloat()))),
                initial.copy(cableRoutes = listOf(CableRoute(cableId = cable.id, areaId = area.id,
                    points = listOf(MapPoint(number.toFloat(), .5f), MapPoint(.2f, .2f)))))
            )
            variants.forEachIndexed { index, project ->
                assertTrue("Non-finite field $index accepted", !ModelValidator.validateProject(project).isValid)
            }
        }
        assertTrue(ModelValidator.validateProject(initial.copy(powerFeeds = listOf(PowerFeed(deviceId = deviceId, feedName = "Feed",
            loadWatts = 12.5, loadVa = 20.0)), poeMappings = listOf(PoeMapping(portId = port.id, allocatedPowerWatts = 5.5)),
            cables = listOf(Cable(lengthValue = 0.0)))).isValid)
    }
}
