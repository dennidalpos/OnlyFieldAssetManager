package com.onlyfield.assetmanager

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.export.PdfExportManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class SurveyPdfTest {
    private val floor = Area(name = "Selected floor")
    private val rack = Rack(name = "Survey rack", areaId = floor.id)
    private val states = listOf(null, ObservationStatus.TO_VERIFY, ObservationStatus.VERIFIED, ObservationStatus.CONFLICT, ObservationStatus.NOT_DETECTED)
    private val devices = states.mapIndexed { i, state -> Device(technicalName = "SURVEY-$i", areaId = floor.id, rackId = rack.id,
        positionU = i + 1, observation = state?.let { Observation("Survey", 1, it, "NOTE-$i") }) }
    private val port = Port(deviceId = devices.first().id, name = "LOOSE-PORT", endpointStatus = EndpointStatus.DETACHED_TO_VERIFY,
        observation = Observation("Survey", 1, ObservationStatus.CONFLICT, "PORT-NOTE"))
    private val chosen = devices.mapIndexed { i, d -> if (i == 0) d.copy(ports = listOf(port)) else d }
    private val cable = Cable(codeOrLabel = "LOOSE-CABLE", portAId = port.id,
        observation = Observation("Survey", 1, ObservationStatus.NOT_DETECTED, "CABLE-NOTE"))
    private val project = Project(name = "Survey", createdEpochMs = 0, updatedEpochMs = 0, racks = listOf(rack),
        sites = listOf(Site(name = "Selected", areas = listOf(floor), devices = chosen), Site(name = "Excluded", devices = listOf(
            Device(technicalName = "EXCLUDED-DEVICE", observation = Observation("Survey", 1, ObservationStatus.CONFLICT, "EXCLUDED-NOTE"))))),
        cables = listOf(cable), credentials = listOf(Credential(username = "SECRET-USER", secret = "SECRET-VALUE")),
        attachments = listOf(Attachment(name = "PRIVATE-ATTACHMENT", originalFileName = "private.png", relativePath = "private.png", classification = AttachmentClassification.CONFIDENTIAL)))

    private fun compact(text: String) = text.filterNot(Char::isWhitespace)
    private fun text(write: (java.io.OutputStream) -> Unit): String {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File.createTempFile("survey-test-", ".pdf", context.cacheDir)
        try {
            file.outputStream().use(write)
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    assertTrue(renderer.pageCount > 0)
                    (0 until renderer.pageCount).joinToString("\n") { n ->
                        renderer.openPage(n).use { page -> page.textContents.joinToString("") { it.text } }
                    }
                }
            }
        } finally { check(file.delete()) }
    }

    @Test fun compositeAndRackPdfsPreserveSurveyStatesNotesAndSelectedWarningsInThreeLanguages() {
        for (language in listOf("it", "en", "es")) {
            val i18n = Messages(Locale.forLanguageTag(language))
            val composite = text { PdfExportManager.exportCompositeReportPdfToStream(project,
                ExportFilterConfig(selectedAreaId = floor.id), ReportSelection(), it, i18n) }
            val rackText = text { PdfExportManager.exportRackPdfToStream(project, rack, chosen, emptyList(), it, i18n) }
            for (result in listOf(composite, rackText)) {
                val content = compact(result)
                chosen.forEachIndexed { i, d ->
                    val row = compact("${d.technicalName} · ${d.category.toDisplayString(i18n)} · ${d.operationalStatus.toDisplayString(i18n)} · ${(states[i] ?: ObservationStatus.TO_VERIFY).toDisplayString(i18n)}")
                    assertTrue("$language missing survey row $row", content.contains(row))
                }
                listOf("NOTE-1", "NOTE-2", "NOTE-3", "NOTE-4").forEach { assertTrue(content.contains(it)) }
                assertTrue(content.contains(compact(i18n.text("text.9c2ca030d9f5", chosen[3].technicalName, ObservationStatus.CONFLICT.toDisplayString(i18n).lowercase(i18n.locale)))))
                listOf("EXCLUDED-DEVICE", "EXCLUDED-NOTE", "PRIVATE-ATTACHMENT", "SECRET-USER", "SECRET-VALUE").forEach { assertFalse("$language leaked $it", content.contains(it)) }
            }
            val content = compact(composite)
            listOf("PORT-NOTE", "CABLE-NOTE").forEach { assertTrue(content.contains(it)) }
            assertTrue(content.contains(compact(i18n.text("text.6d8683663196", port.name, chosen.first().technicalName))))
        }
    }

    @Test fun disabledSectionsExcludeSurveyNotesAndUnrelatedWarnings() {
        val selection = ReportSelection(includeInventoryTable = false, includeRackCards = false,
            includeCablingAndPorts = false, includePowerAndBadges = false, includeNotesAndAttachments = false,
            includeFloorPlans = false, includePaths = false, includeTopology = false)
        val result = compact(text { PdfExportManager.exportCompositeReportPdfToStream(project, ExportFilterConfig(), selection, it) })
        listOf("SURVEY-", "NOTE-", "LOOSE-PORT", "LOOSE-CABLE", "EXCLUDED-DEVICE").forEach { assertFalse(result.contains(it)) }
    }
}
