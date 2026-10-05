package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.Project
import android.content.Context
import android.print.PrintManager
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.exchange.MarkdownExportManager
import com.onlyfield.assetmanager.exchange.XlsxExportManager
import com.onlyfield.assetmanager.export.PdfExportManager
import com.onlyfield.assetmanager.export.ProjectPrintDocumentAdapter
import java.io.OutputStream

/** PDF, Excel, Markdown and print output of a stored project. */
internal class DocumentExports(private val load: suspend (String) -> Project?) {
    private suspend fun getProjectById(projectId: String) = load(projectId)

    suspend fun exportRackPdfToStream(projectId: String, rackId: String, outputStream: OutputStream, i18n: Messages = Messages()): Boolean {
        val project = getProjectById(projectId) ?: return false
        val rack = project.racks.find { it.id == rackId } ?: return false

        val allDevices = project.sites.flatMap { it.devices }
        val devicesInRack = allDevices.filter { it.rackId == rackId }
        val unmountedDevices = allDevices.filter { (it.rackId == null) && (it.areaId == rack.areaId) }

        PdfExportManager.exportRackPdfToStream(
            project = project,
            rack = rack,
            devicesInRack = devicesInRack,
            unmountedDevices = unmountedDevices,
            outputStream = outputStream,
            i18n = i18n)
        return true
    }

    suspend fun exportXlsxToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream, i18n: Messages = Messages()): Boolean {
        val project = getProjectById(projectId) ?: return false
        XlsxExportManager.exportXlsxToStream(project, filterConfig, outputStream, i18n = i18n)
        return true
    }

    suspend fun exportMarkdownToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream, i18n: Messages = Messages()): Boolean {
        val project = getProjectById(projectId) ?: return false
        MarkdownExportManager.exportMarkdownToStream(project, filterConfig, outputStream, i18n = i18n)
        return true
    }

    suspend fun exportCompositePdfToStream(
        projectId: String,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        outputStream: OutputStream,
        i18n: Messages = Messages()): Boolean {
        val project = getProjectById(projectId) ?: return false
        PdfExportManager.exportCompositeReportPdfToStream(project, filterConfig, selection, outputStream, i18n = i18n)
        return true
    }

    suspend fun printProjectDocument(
        context: android.content.Context,
        projectId: String,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        i18n: Messages = Messages()): Boolean {
        val project = getProjectById(projectId) ?: return false
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager ?: return false
        val jobName = "Report_${project.name}"
        val adapter = ProjectPrintDocumentAdapter(project, filterConfig, selection, i18n)
        printManager.print(jobName, adapter, null)
        return true
    }
}
