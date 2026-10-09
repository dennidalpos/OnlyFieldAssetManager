package com.onlyfield.assetmanager

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.export.PdfExportManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PrintPdfNativeTest {
    @Test fun requestedPagesHaveMatchingContentGeometryAndRanges() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val area = Area(name = "Print floor")
        val rack = Rack(name = "Print rack", areaId = area.id)
        val devices = (0 until 160).map { Device(technicalName = "PRINT-DEVICE-$it", areaId = area.id,
            rackId = rack.id, positionU = if (it < 42) it + 1 else null) }
        val project = Project(name = "Print fixture", createdEpochMs = 0, updatedEpochMs = 0,
            racks = listOf(rack), sites = listOf(Site(name = "Site", areas = listOf(area), devices = devices)))
        val file = File.createTempFile("print-native-", ".pdf", context.cacheDir)
        try {
            for (media in listOf(PrintAttributes.MediaSize.ISO_A4, PrintAttributes.MediaSize.ISO_A5.asLandscape())) {
                val attributes = PrintAttributes.Builder().setMediaSize(media)
                    .setResolution(PrintAttributes.Resolution("test", "Test", 300, 300))
                    .setMinMargins(PrintAttributes.Margins(300, 400, 500, 600)).setColorMode(PrintAttributes.COLOR_MODE_COLOR).build()
                val geometry = PrintedPdfDocument(context, attributes)
                val width = geometry.pageWidth; val height = geometry.pageHeight; val bounds = geometry.pageContentRect
                geometry.close()
                fun write(ranges: Array<PageRange>): Pair<List<String>, Array<PageRange>> {
                    val written = file.outputStream().use { PdfExportManager.writePrint(context, attributes, ranges,
                        project, ExportFilterConfig(), ReportSelection(), it, Messages()) {} }
                    val texts = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer -> (0 until renderer.pageCount).map { number ->
                            renderer.openPage(number).use { page ->
                                assertEquals(width, page.width); assertEquals(height, page.height)
                                page.textContents.forEach { content -> content.bounds.forEach { rect ->
                                    assertTrue("Text outside margins: $rect / $bounds", rect.left >= bounds.left - 1 &&
                                        rect.top >= bounds.top - 1 && rect.right <= bounds.right + 1 && rect.bottom <= bounds.bottom + 3)
                                } }
                                page.textContents.joinToString(" ") { it.text }.filterNot(Char::isWhitespace)
                            }
                        } }
                    }
                    return texts to written
                }
                val (all, _) = write(arrayOf(PageRange.ALL_PAGES))
                assertTrue(all.size > 3)
                val count = PdfExportManager.layoutPrint(context, attributes, project, ExportFilterConfig(), ReportSelection(), Messages()) {}
                assertEquals(all.size, count)
                devices.forEach { assertTrue("Missing ${it.technicalName}", all.joinToString("").contains(it.technicalName)) }
                for (ranges in listOf(arrayOf(PageRange(1, 1)), arrayOf(PageRange(1, 2)),
                    arrayOf(PageRange(0, 0), PageRange(2, 2)), arrayOf(PageRange(0, 1), PageRange(1, 2)))) {
                    val expected = all.indices.filter { n -> ranges.any { n in it.start..it.end } }
                    val (texts, written) = write(ranges)
                    assertEquals(expected.map { all[it] }, texts)
                    assertEquals(expected, written.flatMap { (it.start..it.end).toList() })
                }
            }
        } finally { check(file.delete()) }
    }
}
