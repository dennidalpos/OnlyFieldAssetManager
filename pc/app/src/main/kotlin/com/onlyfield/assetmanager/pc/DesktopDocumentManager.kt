package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.exchange.MarkdownExportManager
import com.onlyfield.assetmanager.exchange.XlsxExportManager
import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.pc.report.PdfReportWriter
import com.onlyfield.assetmanager.pc.report.ReportContent
import org.apache.pdfbox.printing.PDFPageable
import java.awt.image.BufferedImage
import java.awt.print.PrinterJob
import java.io.OutputStream

object DesktopDocumentManager {

    /** Exports XLSX. */
    fun exportXlsx(
        project: Project,
        filterConfig: ExportFilterConfig,
        outputStream: OutputStream,
        i18n: Messages = Messages()
    ) {
        XlsxExportManager.exportXlsxToStream(project, filterConfig, outputStream, i18n = i18n)
    }

    /** Exports Markdown. */
    fun exportMarkdown(
        project: Project,
        filterConfig: ExportFilterConfig,
        outputStream: OutputStream,
        i18n: Messages = Messages()
    ) {
        MarkdownExportManager.exportMarkdownToStream(project, filterConfig, outputStream, i18n = i18n)
    }

    /** Writes a PDF without credentials; [planImage] gives the floor plan background of an area. */
    fun exportCompositePdf(
        project: Project,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        outputStream: OutputStream,
        i18n: Messages = Messages(),
        planImage: (Area) -> BufferedImage? = { null },
    ) {
        val lines = ReportContent.build(project, filterConfig, selection, i18n = i18n)
        outputStream.use { PdfReportWriter(project, i18n, planImage).write(lines, lines.first().text, it) }
    }

    /** Opens the Windows print dialog with the same document as the PDF. */
    fun printDocumentNative(
        project: Project,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        i18n: Messages = Messages(),
        planImage: (Area) -> BufferedImage? = { null },
    ): Boolean {
        val lines = ReportContent.build(project, filterConfig, selection, i18n = i18n)
        return try {
            PdfReportWriter(project, i18n, planImage).build(lines, lines.first().text).use { document ->
                val job = PrinterJob.getPrinterJob()
                job.setJobName("OnlyField - ${project.name}")
                job.setPageable(PDFPageable(document))
                if (job.printDialog()) { job.print(); true } else false
            }
        } catch (_: Throwable) {
            false
        }
    }
}
