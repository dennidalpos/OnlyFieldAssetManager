package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/** Delivery PDF of the demo: every drawn section, no credentials. Pages are kept in build/reports for review. */
class DeliveryPdfTest {
    private val demo = PackageSerializer.importPackage(File("../../fixtures/demo/onlyfield-demo.ofam").readBytes()).pkg!!.project
    private val project = demo.copy(credentials = listOf(Credential(username = "admin-demo", secret = "Segreto-Demo-42")))

    @Test fun demoReportHasAllDrawnSectionsAndNoSecrets() {
        val out = ByteArrayOutputStream()
        // A plain image stands in for the floor plan files, which the package does not need here.
        val plan = BufferedImage(800, 500, BufferedImage.TYPE_INT_RGB).apply { createGraphics().apply { color = Color(0xF4, 0xF1, 0xEA); fillRect(0, 0, 800, 500); dispose() } }
        DesktopDocumentManager.exportCompositePdf(project, ExportFilterConfig(authorName = "Tecnico"), ReportSelection(), out, planImage = { plan })
        val bytes = out.toByteArray()
        File("build/reports/delivery").apply { mkdirs() }.resolve("demo.pdf").writeBytes(bytes)
        Loader.loadPDF(bytes).use { doc ->
            val text = PDFTextStripper().getText(doc)
            listOf("Planimetrie", "Rack", "Percorsi", "Topologia fisica", "Da", "Passaggi", "SW-COM-CORE", "PR-COM-MED", "GB-TEA-PT-01", "Fronte", "Retro")
                .forEach { assertTrue(it, text.contains(it)) }
            assertFalse(text.contains("Segreto-Demo-42"))
            assertFalse(text.contains("admin-demo"))
            // Drawn pages: a floor plan has images, rack and topology pages are vector drawings.
            assertTrue(doc.pages.any { page -> page.resources.xObjectNames.any() })
            val renderer = PDFRenderer(doc)
            (0 until doc.numberOfPages).forEach { i -> ImageIO.write(renderer.renderImageWithDPI(i, 60f), "png", File("build/reports/delivery/page-%02d.png".format(i + 1))) }
        }
    }
}
