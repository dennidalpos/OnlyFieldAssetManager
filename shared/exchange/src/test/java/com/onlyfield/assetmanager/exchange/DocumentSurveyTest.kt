package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class DocumentSurveyTest {
    @Test fun warningsFollowPrintedSectionsAndKeepOriginalExternalReferences() {
        val floor = Area(name = "Floor")
        val device = Device(technicalName = "Selected", areaId = floor.id)
        val port = Port(deviceId = device.id, name = "Selected port")
        val other = Device(technicalName = "External")
        val otherPort = Port(deviceId = other.id, name = "External port")
        val site = Site(name = "Selected", areas = listOf(floor), devices = listOf(device.copy(ports = listOf(port))))
        val project = Project(name = "Warnings", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(site, Site(name = "Other", devices = listOf(other.copy(ports = listOf(otherPort))))),
            cables = listOf(Cable(portAId = port.id, portBId = otherPort.id, observation = Observation("Survey", 1, ObservationStatus.CONFLICT))),
            powerFeeds = listOf(PowerFeed(deviceId = device.id, feedName = "A")))
        val scope = DocumentSelection(project, ExportFilterConfig(selectedSiteId = site.id))
        val inventory = ReportSelection(includeRackCards = false, includeCablingAndPorts = false, includeLogicalNetwork = false,
            includePowerAndBadges = false, includeNotesAndAttachments = false, includeFloorPlans = false, includePaths = false, includeTopology = false)
        val i18n = Messages()
        assertEquals(listOf("UNVERIFIED_DEVICE_OBSERVATION"), scope.warnings(i18n, inventory).map { it.code })
        val withPaths = scope.warnings(i18n, inventory.copy(includePaths = true))
        assertTrue(withPaths.any { it.code == "UNVERIFIED_CABLE" })
        assertFalse(withPaths.any { it.code == "DETACHED_CABLE_ENDPOINT" })
        val power = scope.warnings(i18n, inventory.copy(includeInventoryTable = false, includePowerAndBadges = true))
        assertTrue(power.any { it.code == "SINGLE_FEED_PARTIAL_COVERAGE_WARNING" })
        assertFalse(power.any { it.code == "UNVERIFIED_DEVICE_OBSERVATION" })
    }

    @Test fun markdownAndXlsxPreserveSurveyStatesNotesAndSelectedWarningsInThreeLanguages() {
        val floor = Area(name = "Selected floor")
        val states = listOf(null, ObservationStatus.TO_VERIFY, ObservationStatus.VERIFIED, ObservationStatus.CONFLICT, ObservationStatus.NOT_DETECTED)
        val devices = states.mapIndexed { index, status -> Device(technicalName = "SURVEY-$index", areaId = floor.id,
            observation = status?.let { Observation("Survey", 1, it, "NOTE-$index") }) }
        val selectedPort = Port(deviceId = devices.first().id, name = "LOOSE-PORT", endpointStatus = EndpointStatus.DETACHED_TO_VERIFY,
            observation = Observation("Survey", 1, ObservationStatus.CONFLICT, "PORT-NOTE"))
        val excluded = Device(technicalName = "EXCLUDED-DEVICE", observation = Observation("Survey", 1, ObservationStatus.CONFLICT, "EXCLUDED-NOTE"))
        val cable = Cable(codeOrLabel = "LOOSE-CABLE", portAId = selectedPort.id,
            observation = Observation("Survey", 1, ObservationStatus.NOT_DETECTED, "CABLE-NOTE"))
        val project = Project(name = "Survey", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "Selected", areas = listOf(floor), devices = devices.mapIndexed { i, d -> if (i == 0) d.copy(ports = listOf(selectedPort)) else d }),
                Site(name = "Excluded", devices = listOf(excluded))), cables = listOf(cable),
            credentials = listOf(Credential(username = "SECRET-USER", secret = "SECRET-VALUE")),
            attachments = listOf(Attachment(name = "EXCLUDED-REVIEW", originalFileName = "review.png", relativePath = "review.png", classification = AttachmentClassification.REVIEW_REQUIRED)))
        for (language in listOf("it", "en", "es")) {
            val i18n = Messages(Locale.forLanguageTag(language))
            val filter = ExportFilterConfig(selectedAreaId = floor.id, reviewRequiredConfirmed = false)
            val markdown = ByteArrayOutputStream().also { MarkdownExportManager.exportMarkdownToStream(project, filter, it, i18n) }.toString(Charsets.UTF_8)
            devices.forEachIndexed { index, device ->
                val row = markdown.lineSequence().first { it.startsWith("| **${device.technicalName}**") }
                assertTrue("$language $row", row.contains((states[index] ?: ObservationStatus.TO_VERIFY).toDisplayString(i18n)))
            }
            assertTrue(markdown.contains(i18n.text("text.46d91c6ab242", 3)))
            val xlsx = ByteArrayOutputStream().also { XlsxExportManager.exportXlsxToStream(project, filter, it, i18n) }.toByteArray()
            val sheets = mutableMapOf<String, String>()
            ZipInputStream(ByteArrayInputStream(xlsx)).use { zip ->
                generateSequence { zip.nextEntry }.forEach { sheets[it.name] = zip.readBytes().toString(Charsets.UTF_8) }
            }
            val inventory = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(ByteArrayInputStream(sheets.getValue("xl/worksheets/sheet1.xml").toByteArray()))
            val rows = inventory.getElementsByTagName("row")
            devices.forEachIndexed { index, _ ->
                val cells = rows.item(index + 1).childNodes
                val status = (0 until cells.length).map { cells.item(it) }.first { it.attributes?.getNamedItem("r")?.nodeValue == "M${index + 2}" }.textContent
                assertEquals(states[index]?.toDisplayString(i18n) ?: ObservationStatus.TO_VERIFY.toDisplayString(i18n), status)
            }
            val texts = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(ByteArrayInputStream(sheets.getValue("xl/worksheets/sheet5.xml").toByteArray())).documentElement.textContent
            assertTrue(DocumentSelection(project, filter.copy(reviewRequiredConfirmed = true)).warnings(i18n).any { it.code == "ATTACHMENT_NEEDS_REVIEW" })
            for (text in listOf(markdown, texts)) {
                listOf("NOTE-1", "NOTE-2", "NOTE-3", "NOTE-4", "PORT-NOTE", "CABLE-NOTE").forEach { assertTrue("$language missing $it", text.contains(it)) }
                assertTrue(text.contains(i18n.text("text.6d8683663196", selectedPort.name, devices.first().technicalName)))
                listOf("EXCLUDED-DEVICE", "EXCLUDED-NOTE", "EXCLUDED-REVIEW", "SECRET-USER", "SECRET-VALUE").forEach { assertFalse("$language leaked $it", text.contains(it)) }
            }
        }
    }
}
