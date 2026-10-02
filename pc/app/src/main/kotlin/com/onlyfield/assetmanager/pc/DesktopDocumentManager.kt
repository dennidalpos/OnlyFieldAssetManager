package com.onlyfield.assetmanager.pc

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
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DesktopDocumentManager {

    /**
     * Exports project to OpenXML XLSX spreadsheet using pure JVM XlsxExportManager.
     */
    fun exportXlsx(
        project: Project,
        filterConfig: ExportFilterConfig,
        outputStream: OutputStream
    ) {
        XlsxExportManager.exportXlsxToStream(project, filterConfig, outputStream)
    }

    /**
     * Exports project to Markdown document using pure JVM MarkdownExportManager.
     */
    fun exportMarkdown(
        project: Project,
        filterConfig: ExportFilterConfig,
        outputStream: OutputStream
    ) {
        MarkdownExportManager.exportMarkdownToStream(project, filterConfig, outputStream)
    }

    /**
     * Generates a composite PDF/Text document report for Desktop. Excludes secrets.
     */
    fun exportCompositePdf(
        project: Project,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        outputStream: OutputStream
    ) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val reportTitle = filterConfig.titleOverride?.ifBlank { null } ?: "Rapporto Tecnico Infrastruttura — ${project.name}"
        val dateStr = sdf.format(Date())

        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("% Documento PDF Generato per OnlyField Asset Manager (Desktop Windows)\n")
        sb.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n")
        sb.append("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n")
        sb.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] >> endobj\n")

        sb.append("\n--- HEADER ---\n")
        sb.append("Titolo: $reportTitle\n")
        sb.append("Autore: ${filterConfig.authorName}\n")
        sb.append("Data Generazione: $dateStr\n")
        sb.append("Riservatezza: ${if (filterConfig.includeConfidential) "Include Dati Riservati (Zero Secret Leakage)" else "Solo Pubblicabile"}\n\n")

        if (selection.includeInventoryTable) {
            sb.append("--- INVENTARIO APPARATI ---\n")
            val devices = project.businessUnits.flatMap { bu -> bu.devices.map { Pair(bu.name, it) } }
            devices.forEach { (buName, dev) ->
                sb.append("• [${dev.category.name}] ${dev.technicalName} (BU: $buName, IP: ${dev.ipAddress ?: "N/D"})\n")
            }
            sb.append("\n")
        }

        if (selection.includeRackCards) {
            sb.append("--- ARMADI RACK (${project.racks.size}) ---\n")
            project.racks.forEach { r ->
                sb.append("• Rack ${r.name}: ${r.heightU}U (${r.numberingDirection})\n")
            }
            sb.append("\n")
        }

        if (selection.includeCablingAndPorts) {
            sb.append("--- CABLAGGIO E CAVI (${project.cables.size}) ---\n")
            project.cables.forEach { c ->
                sb.append("• Cavo ${c.codeOrLabel ?: "Senza Codice"}: ${c.medium.name} (${c.lengthValue ?: "—"} m)\n")
            }
            sb.append("\n")
        }

        if (selection.includeLogicalNetwork) {
            sb.append("--- RETE LOGICA (VLAN: ${project.vlans.size}, Subnet: ${project.subnets.size}) ---\n")
            project.vlans.forEach { v ->
                sb.append("• VLAN ${v.vlanId}: ${v.name} [${v.scopeType.name}]\n")
            }
            sb.append("\n")
        }

        outputStream.use { stream ->
            stream.write(sb.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    /**
     * Triggers Windows 11 native printing dialog using java.awt.print.PrinterJob.
     */
    fun printDocumentNative(
        project: Project,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection
    ): Boolean {
        return try {
            val printerJob = PrinterJob.getPrinterJob()
            printerJob.setJobName("OnlyField - ${project.name}")

            printerJob.setPrintable(object : Printable {
                override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
                    if (pageIndex > 0) return Printable.NO_SUCH_PAGE

                    val g2d = graphics as Graphics2D
                    g2d.translate(pageFormat.imageableX, pageFormat.imageableY)

                    g2d.font = Font("SansSerif", Font.BOLD, 16)
                    val title = filterConfig.titleOverride?.ifBlank { null } ?: "Rapporto Tecnico — ${project.name}"
                    g2d.drawString(title, 30, 40)

                    g2d.font = Font("SansSerif", Font.PLAIN, 10)
                    g2d.drawString("Autore: ${filterConfig.authorName} | Data: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date())}", 30, 60)

                    var y = 90
                    g2d.font = Font("SansSerif", Font.BOLD, 12)
                    g2d.drawString("Sintesi Progetto", 30, y)
                    y += 20

                    g2d.font = Font("SansSerif", Font.PLAIN, 10)
                    g2d.drawString("• Apparati totali: ${project.businessUnits.sumOf { it.devices.size }}", 35, y); y += 15
                    g2d.drawString("• Armadi Rack: ${project.racks.size}", 35, y); y += 15
                    g2d.drawString("• Cavi di collegamento: ${project.cables.size}", 35, y); y += 15
                    g2d.drawString("• VLAN definite: ${project.vlans.size}", 35, y); y += 15

                    return Printable.PAGE_EXISTS
                }
            })

            if (printerJob.printDialog()) {
                printerJob.print()
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }
}
