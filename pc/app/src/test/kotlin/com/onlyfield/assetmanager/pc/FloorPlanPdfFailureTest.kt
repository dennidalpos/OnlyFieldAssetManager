package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

class FloorPlanPdfFailureTest {
    @get:Rule val folder = TemporaryFolder()
    private fun export(project: Project, plan: (Area) -> BufferedImage?, filter: ExportFilterConfig = ExportFilterConfig(), selection: ReportSelection = ReportSelection()): ByteArray =
        ByteArrayOutputStream().also { DesktopDocumentManager.exportCompositePdf(project, filter, selection, it, planImage = plan) }.toByteArray()

    @Test fun unreadableImagePdfAndMissingPayloadCannotBecomeSuccessfulDocuments() {
        for (type in listOf(AttachmentType.IMAGE, AttachmentType.PDF)) {
            val storage = DesktopStorageManager(folder.newFolder())
            val state = DesktopAppState(storage)
            val attachment = Attachment(name = "Plan", originalFileName = "plan.bin", relativePath = "plan.bin", fileType = type)
            val area = Area(name = "Floor", floorplanAttachmentId = attachment.id)
            val project = Project(name = "Plans", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Building", areas = listOf(area))), attachments = listOf(attachment))
            try {
                val incoming = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(project, mapOf("plan.bin" to byteArrayOf(1, 2, 3)))) }
                state.importFile(incoming, compare = false)
                assertNotNull(state.project)
                assertThrows(IOException::class.java) { export(state.project!!, state::planImage) }
                assertThrows(IOException::class.java) { export(project, { null }) }
                assertThrows(IOException::class.java) { export(project, { throw IOException("isolated loading failure") }) }
            } finally { state.closeProject() }
        }
    }
    @Test fun validAbsentExcludedAndUnselectedBackgroundsHaveDistinctOutcomes() {
        val attachment = Attachment(name = "Plan", originalFileName = "plan.png", relativePath = "plan.png", classification = AttachmentClassification.CONFIDENTIAL)
        val area = Area(name = "Floor", floorplanAttachmentId = attachment.id)
        val project = Project(name = "Plans", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "Building", areas = listOf(area))), attachments = listOf(attachment))
        val image = BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB)
        val valid = export(project, { image }, ExportFilterConfig(includeConfidential = true))
        Loader.loadPDF(valid).use { assertTrue(it.pages.any { page -> page.resources.xObjectNames.any() }) }
        val failIfRead: (Area) -> BufferedImage? = { error("Excluded backgrounds must not be read") }
        for (bytes in listOf(export(project, failIfRead), export(project, failIfRead, ExportFilterConfig(includeConfidential = true), ReportSelection(includeFloorPlans = false)),
            export(project.copy(sites = project.sites.map { it.copy(areas = listOf(area.copy(floorplanAttachmentId = null))) }), failIfRead))) {
            Loader.loadPDF(bytes).use { assertTrue(it.numberOfPages > 0) }
        }
        val png = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        assertNotNull(PlanMedia.bufferedImage(png, false, 0))
    }

    @Test fun storedPdfBackgroundIsRenderedAndMissingPayloadFails() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val attachment = Attachment(name = "Plan", originalFileName = "plan.pdf", relativePath = "plan.pdf", fileType = AttachmentType.PDF)
        val area = Area(name = "Floor", floorplanAttachmentId = attachment.id)
        val project = Project(name = "PDF plan", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "Building", areas = listOf(area))), attachments = listOf(attachment))
        val pdf = ByteArrayOutputStream().also { out -> PDDocument().use { it.addPage(PDPage()); it.save(out) } }.toByteArray()
        try {
            val incoming = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(project, mapOf("plan.pdf" to pdf))) }
            state.importFile(incoming, compare = false)
            Loader.loadPDF(export(state.project!!, state::planImage)).use {
                assertTrue(it.pages.any { page -> page.resources.xObjectNames.any() })
            }
            assertTrue(storage.attachmentFile(project.id, attachment).delete())
            storage.restoreMedia(project.id, null)
            assertThrows(IOException::class.java) { export(state.project!!, state::planImage) }
        } finally { state.closeProject() }
    }
}
