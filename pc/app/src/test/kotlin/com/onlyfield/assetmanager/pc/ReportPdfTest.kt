package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream

class ReportPdfTest {

    private val project = Project(
        name = "Sede di Prova",
        createdEpochMs = 0,
        updatedEpochMs = 0,
        sites = listOf(
            Site(
                name = "IT",
                areas = listOf(Area(id = "a1", name = "Sala server")),
                devices = (1..120).map { Device(technicalName = "SW-%03d".format(it), areaId = "a1", ipAddress = "10.0.0.$it") }
            )
        ),
        credentials = listOf(Credential(username = "admin", secret = "SuperSegreta!")),
        attachments = listOf(
            Attachment(name = "Foto pubblica", originalFileName = "a.jpg", relativePath = ""),
            Attachment(name = "Schema riservato", originalFileName = "b.pdf", relativePath = "", classification = AttachmentClassification.CONFIDENTIAL)
        )
    )

    private fun bytes(filter: ExportFilterConfig = ExportFilterConfig(authorName = "Tecnico")): ByteArray =
        ByteArrayOutputStream().also { DesktopDocumentManager.exportCompositePdf(project, filter, ReportSelection(), it) }.toByteArray()

    private fun pdf(filter: ExportFilterConfig = ExportFilterConfig(authorName = "Tecnico")): String =
        Loader.loadPDF(bytes(filter)).use { PDFTextStripper().getText(it) }

    @Test
    fun producesStructurallyValidMultiPagePdf() {
        Loader.loadPDF(bytes()).use { doc -> assertTrue("120 devices need more than one page", doc.numberOfPages > 1) }
        val text = pdf()
        assertTrue(text.contains("SW-120"))
        assertTrue(text.contains("Tecnico"))
    }

    @Test
    fun neverLeaksSecretsAndHonoursConfidentiality() {
        val normal = pdf()
        assertFalse(normal.contains("SuperSegreta"))
        assertTrue(normal.contains("Foto pubblica"))
        assertFalse(normal.contains("Schema riservato"))
        assertTrue(pdf(ExportFilterConfig(includeConfidential = true)).contains("Schema riservato"))
    }
}
