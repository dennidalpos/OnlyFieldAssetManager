package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.report.ReportContent
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Locale

class SurveyReportTest {
    @Test fun rackOnlyReportPreservesExplicitAndMissingSurveyStates() {
        val rack = Rack(name = "Rack")
        val devices = listOf(null, ObservationStatus.VERIFIED, ObservationStatus.CONFLICT, ObservationStatus.NOT_DETECTED).mapIndexed { i, state ->
            Device(technicalName = "RACK-SURVEY-$i", rackId = rack.id, positionU = i + 1, observation = state?.let { Observation("Survey", 1, it) })
        }
        val project = Project(name = "Rack", createdEpochMs = 0, updatedEpochMs = 0, racks = listOf(rack), sites = listOf(Site(name = "Site", devices = devices)))
        val selection = ReportSelection(includeInventoryTable = false, includeNotesAndAttachments = false,
            includeCablingAndPorts = false, includeLogicalNetwork = false, includePowerAndBadges = false,
            includeFloorPlans = false, includePaths = false, includeTopology = false)
        for (language in listOf("it", "en", "es")) {
            val i18n = Messages(Locale.forLanguageTag(language))
            val rows = ReportContent.build(project, ExportFilterConfig(), selection, i18n = i18n)
            devices.forEach { device -> assertTrue(rows.any { it.text.contains("${device.technicalName} · ${(device.observation?.status ?: ObservationStatus.TO_VERIFY).toDisplayString(i18n)}") }) }
        }
    }

    @Test fun pdfPreservesNullAndExplicitSurveyStatesNotesAndFilteredWarnings() {
        val floor = Area(name = "Floor")
        val states = listOf(null, ObservationStatus.VERIFIED, ObservationStatus.CONFLICT, ObservationStatus.NOT_DETECTED)
        val devices = states.mapIndexed { i, state -> Device(technicalName = "SURVEY-$i", areaId = floor.id,
            observation = state?.let { Observation("Survey", 1, it, "NOTE-$i") }) }
        val port = Port(deviceId = devices.first().id, name = "LOOSE-PORT", endpointStatus = EndpointStatus.DETACHED_TO_VERIFY)
        val selected = devices.mapIndexed { i, d -> if (i == 0) d.copy(ports = listOf(port)) else d }
        val excluded = Device(technicalName = "EXCLUDED", observation = Observation("Survey", 1, ObservationStatus.CONFLICT, "PRIVATE-NOTE"))
        val project = Project(name = "Survey", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "Site", areas = listOf(floor), devices = selected), Site(name = "Other", devices = listOf(excluded))),
            credentials = listOf(Credential(username = "SECRET-USER", secret = "SECRET-VALUE")))
        for (language in listOf("it", "en", "es")) {
            val i18n = Messages(Locale.forLanguageTag(language))
            val bytes = ByteArrayOutputStream().also {
                DesktopDocumentManager.exportCompositePdf(project, ExportFilterConfig(selectedAreaId = floor.id),
                    ReportSelection(includeFloorPlans = false, includePaths = false, includeTopology = false), it, i18n)
            }.toByteArray()
            val text = Loader.loadPDF(bytes).use { PDFTextStripper().getText(it) }
            devices.forEachIndexed { i, d ->
                val row = text.lineSequence().first { it.contains(d.technicalName) }
                assertTrue("$language $row", row.contains((states[i] ?: ObservationStatus.TO_VERIFY).toDisplayString(i18n)))
            }
            listOf("NOTE-1", "NOTE-2", "NOTE-3").forEach { assertTrue(text.contains(it)) }
            assertTrue(text.contains(i18n.text("text.6d8683663196", port.name, devices.first().technicalName)))
            listOf("EXCLUDED", "PRIVATE-NOTE", "SECRET-USER", "SECRET-VALUE").forEach { assertFalse(text.contains(it)) }
        }
    }
}
