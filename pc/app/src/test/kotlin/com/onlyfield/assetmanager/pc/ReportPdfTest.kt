package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
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

    private fun pdf(filter: ExportFilterConfig = ExportFilterConfig(authorName = "Tecnico")): String {
        val out = ByteArrayOutputStream()
        DesktopDocumentManager.exportCompositePdf(project, filter, ReportSelection(), out)
        return out.toString(Charsets.ISO_8859_1.name())
    }

    @Test
    fun producesStructurallyValidMultiPagePdf() {
        val text = pdf()
        assertTrue(text.startsWith("%PDF-1.4"))
        assertTrue(text.trimEnd().endsWith("%%EOF"))
        val pageCount = Regex("/Type /Pages /Kids \\[[^]]*] /Count (\\d+)").find(text)!!.groupValues[1].toInt()
        assertTrue("120 devices need more than one page", pageCount > 1)

        // Every xref offset must point exactly at the start of its object.
        val xrefStart = Regex("startxref\\n(\\d+)").find(text)!!.groupValues[1].toInt()
        val entries = text.substring(xrefStart).lines().drop(3).takeWhile { it.endsWith(" n ") }
        entries.forEachIndexed { i, e ->
            val offset = e.substring(0, 10).toInt()
            assertTrue("object ${i + 1} offset", text.startsWith("${i + 1} 0 obj", offset))
        }
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
