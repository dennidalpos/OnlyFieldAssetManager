package com.onlyfield.assetmanager

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.export.PdfExportManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CompositePdfTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val area = Area(name = "Piano verificato")
    private val rack = Rack(name = "RACK-COMPLETO", areaId = area.id, heightU = 60)
    private val devices = (1..100).map { n ->
        Device(id = "device-$n", technicalName = "DEVICE-${n.toString().padStart(3, '0')}",
            rackId = rack.id, areaId = area.id, positionU = n.takeIf { it <= 60 },
            ports = listOf(Port(id = "port-$n", deviceId = "device-$n", name = "PORT-$n")))
    }
    private val site = Site(name = "Sede verificata", areas = listOf(area), devices = devices)
    private val project = Project(name = "Report completo", createdEpochMs = 0, updatedEpochMs = 0,
        sites = listOf(site), racks = listOf(rack),
        cables = listOf(Cable(codeOrLabel = "CABLE-CHECK", portAId = "port-1", portBId = "port-2")),
        vlans = listOf(Vlan(vlanId = 10, name = "VLAN-CHECK")),
        subnets = listOf(Subnet(cidrBlock = "192.0.2.0/24")),
        logicalInterfaces = listOf(LogicalInterface(deviceId = "device-1", name = "INTERFACE-CHECK")),
        wanVpnConnections = listOf(WanVpnConnection(name = "WAN-CHECK")),
        powerFeeds = listOf(PowerFeed(deviceId = "device-1", feedName = "FEED-CHECK")),
        poeMappings = listOf(PoeMapping(portId = "port-1")),
        documentBadges = listOf(DocumentBadge(targetType = "DEVICE", targetId = "device-1", label = "BADGE-CHECK")),
        attachments = (1..75).map { n -> Attachment(name = "ATTACHMENT-${n.toString().padStart(3, '0')}",
            originalFileName = "file-$n.jpg", relativePath = "attachments/file-$n.jpg", attributionText = "AUTHOR-$n") } +
            Attachment(name = "CONFIDENTIAL-CHECK", originalFileName = "private.jpg", relativePath = "private.jpg", classification = AttachmentClassification.CONFIDENTIAL),
        customExtraFields = listOf(CustomExtraField(targetType = "PROJECT", targetId = "pdf-project", fieldKey = "CUSTOM-CHECK", fieldValue = "Long note ".repeat(900) + "END-OF-NOTE")),
        credentials = listOf(Credential(username = "SECRET-USER", secret = "SECRET-VALUE")), id = "pdf-project")

    private fun pdf(name: String, selection: ReportSelection = ReportSelection(), filter: ExportFilterConfig = ExportFilterConfig(), language: String = "it"): File {
        val file = File(context.getExternalFilesDir(null), "$name.pdf")
        file.outputStream().use { PdfExportManager.exportCompositeReportPdfToStream(project, filter, selection, it, Messages(Locale.forLanguageTag(language))) }
        return file
    }

    private fun text(file: File, minimumPages: Int = 1): String =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                assertTrue("Expected at least $minimumPages pages, got ${renderer.pageCount}", renderer.pageCount >= minimumPages)
                (0 until renderer.pageCount).joinToString("\n") { index ->
                    // Extraction may split glyphs and insert spaces within identifiers.
                    renderer.openPage(index).use { page -> page.textContents.joinToString("") { it.text }.filterNot(Char::isWhitespace) }
                }
            }
        }

    @Test fun allDevicesAttachmentsAndSectionsSurvivePagination() {
        val result = text(pdf("composite-complete"), minimumPages = 6)
        devices.forEach { assertTrue("Missing ${it.technicalName}: ${result.take(300)}", result.contains(it.technicalName)) }
        (1..75).forEach { assertTrue("Missing attachment $it", result.contains("ATTACHMENT-${it.toString().padStart(3, '0')}")) }
        listOf("CABLE-CHECK", "VLAN-CHECK", "INTERFACE-CHECK", "WAN-CHECK", "FEED-CHECK", "BADGE-CHECK", "CUSTOM-CHECK", "END-OF-NOTE", "AUTHOR-75").forEach {
            assertTrue("Missing $it", result.contains(it))
        }
        listOf("CONFIDENTIAL-CHECK", "SECRET-USER", "SECRET-VALUE").forEach { assertFalse(result.contains(it)) }
    }

    @Test fun attachmentsAndLongNotesDoNotDependOnInventory() {
        val selection = ReportSelection(includeInventoryTable = false, includeRackCards = false,
            includeCablingAndPorts = false, includeLogicalNetwork = false, includePowerAndBadges = false,
            includeFloorPlans = false, includePaths = false, includeTopology = false)
        val result = text(pdf("composite-notes", selection), minimumPages = 3)
        assertTrue(result.contains("ATTACHMENT-075"))
        assertTrue(result.contains("END-OF-NOTE"))
        assertFalse(result.contains("DEVICE-100"))
        assertFalse(result.contains("CABLE-CHECK"))
        assertFalse(result.contains("VLAN-CHECK"))
        assertFalse(result.contains("FEED-CHECK"))
    }

    @Test fun filtersAndSectionSwitchesAreAppliedInThreeLanguages() {
        val selection = ReportSelection(includeInventoryTable = false, includeRackCards = false,
            includeCablingAndPorts = false, includePowerAndBadges = false, includeNotesAndAttachments = false,
            includeFloorPlans = false, includePaths = false, includeTopology = false)
        listOf("it", "en", "es").forEach { language ->
            val result = text(pdf("composite-network-$language", selection, language = language))
            assertTrue(result.filterNot(Char::isWhitespace).contains(Messages(Locale.forLanguageTag(language)).text("text.7070d68f65b5").filterNot(Char::isWhitespace)))
            assertTrue(result.contains("VLAN-CHECK"))
            assertFalse(result.contains("ATTACHMENT-001"))
            assertFalse(result.contains("DEVICE-100"))
        }
        val empty = text(pdf("composite-filtered", filter = ExportFilterConfig(selectedSiteId = "excluded")))
        assertFalse(empty.contains("DEVICE-001"))
        assertFalse(empty.contains("RACK-COMPLETO"))
        assertFalse(empty.contains("CABLE-CHECK"))
    }

    @Test fun rackExportIncludesEveryMountedAndUnmountedDevice() {
        val file = File(context.getExternalFilesDir(null), "rack-complete.pdf")
        val unmounted = listOf(Device(technicalName = "UNMOUNTED-CHECK", areaId = area.id))
        file.outputStream().use { PdfExportManager.exportRackPdfToStream(project, rack, devices, unmounted, it) }
        val result = text(file, minimumPages = 3)
        devices.forEach { assertTrue("Missing ${it.technicalName}", result.contains(it.technicalName)) }
        assertTrue(result.contains("UNMOUNTED-CHECK"))
    }

}
