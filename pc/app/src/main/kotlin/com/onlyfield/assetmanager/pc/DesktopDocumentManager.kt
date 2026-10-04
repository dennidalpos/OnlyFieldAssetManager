package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.exchange.MarkdownExportManager
import com.onlyfield.assetmanager.exchange.XlsxExportManager
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.PrinterJob
import com.onlyfield.assetmanager.pc.report.ReportContent
import com.onlyfield.assetmanager.pc.report.ReportLine
import com.onlyfield.assetmanager.pc.report.SimplePdfWriter
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

    /** Writes a PDF without credentials. */
    fun exportCompositePdf(
        project: Project,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        outputStream: OutputStream,
        i18n: Messages = Messages()
    ) {
        val lines = ReportContent.build(project, filterConfig, selection, i18n = i18n)
        outputStream.use { SimplePdfWriter.write(lines, lines.first().text, it, i18n) }
    }

    /** Opens the Windows print dialog. */
    fun printDocumentNative(
        project: Project,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        i18n: Messages = Messages()
    ): Boolean {
        val lines = ReportContent.build(project, filterConfig, selection, i18n = i18n)
        return try {
            val job = PrinterJob.getPrinterJob()
            job.setJobName("OnlyField - ${project.name}")
            job.setPrintable(ReportPrintable(lines, i18n))
            if (job.printDialog()) {
                job.print()
                true
            } else false
        } catch (_: Throwable) {
            false
        }
    }

    /** Paginates [lines] with Java2D. */
    private class ReportPrintable(private val lines: List<ReportLine>, private val i18n: Messages) : Printable {
        private var pages: List<List<Pair<ReportLine, List<String>>>>? = null

        private fun fontOf(line: ReportLine) = when (line) {
            is ReportLine.Title -> Font("SansSerif", Font.BOLD, 16)
            is ReportLine.Heading -> Font("SansSerif", Font.BOLD, 13)
            is ReportLine.SubHeading -> Font("SansSerif", Font.BOLD, 11)
            is ReportLine.Meta -> Font("SansSerif", Font.PLAIN, 9)
            else -> Font("SansSerif", Font.PLAIN, 10)
        }

        private fun indentOf(line: ReportLine) = if (line is ReportLine.Item) 12 + line.indent * 14 else 0

        private fun layout(g: Graphics2D, format: PageFormat): List<List<Pair<ReportLine, List<String>>>> {
            val result = mutableListOf<MutableList<Pair<ReportLine, List<String>>>>(mutableListOf())
            var used = 0.0
            for (line in lines) {
                val metrics = g.getFontMetrics(fontOf(line))
                val width = format.imageableWidth - indentOf(line)
                val text = (if (line is ReportLine.Item) "• " else "") + line.text
                val wrapped = mutableListOf<String>()
                var current = ""
                for (word in text.split(' ')) {
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (metrics.stringWidth(candidate) > width && current.isNotEmpty()) { wrapped += current; current = "  $word" } else current = candidate
                }
                wrapped += current
                val height = wrapped.size * metrics.height + if (line is ReportLine.Heading) 8 else 0
                if (used + height > format.imageableHeight - 20 && result.last().isNotEmpty()) { result.add(mutableListOf()); used = 0.0 }
                result.last().add(line to wrapped)
                used += height
            }
            return result
        }

        override fun print(graphics: Graphics, format: PageFormat, pageIndex: Int): Int {
            val g = graphics as Graphics2D
            val laidOut = pages ?: layout(g, format).also { pages = it }
            if (pageIndex >= laidOut.size) return Printable.NO_SUCH_PAGE
            g.translate(format.imageableX, format.imageableY)
            var y = 0
            for ((line, wrapped) in laidOut[pageIndex]) {
                g.font = fontOf(line)
                val metrics = g.fontMetrics
                if (line is ReportLine.Heading) y += 8
                for (text in wrapped) {
                    y += metrics.height
                    g.drawString(text, indentOf(line), y - metrics.descent)
                }
            }
            g.font = Font("SansSerif", Font.PLAIN, 8)
            g.drawString(i18n.text("text.5c159d205c21", pageIndex + 1, laidOut.size), 0, format.imageableHeight.toInt() - 4)
            return Printable.PAGE_EXISTS
        }
    }
}
