package com.onlyfield.assetmanager.data.repository

import androidx.room.withTransaction
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.DeviceModel
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.local.AreaEntity
import com.onlyfield.assetmanager.data.local.BusinessUnitEntity
import com.onlyfield.assetmanager.data.local.CredentialEntity
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.data.local.DeviceEntity
import com.onlyfield.assetmanager.data.local.DeviceModelEntity
import com.onlyfield.assetmanager.data.local.PortEntity
import android.content.Context
import android.print.PrintManager
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.local.RackEntity
import com.onlyfield.assetmanager.data.local.SiteEntity
import com.onlyfield.assetmanager.exchange.DeviceModelSerializer
import com.onlyfield.assetmanager.exchange.MarkdownExportManager
import com.onlyfield.assetmanager.exchange.PackageImportResult
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.PasswordHasher
import com.onlyfield.assetmanager.exchange.ProjectComparison
import com.onlyfield.assetmanager.exchange.ProjectComparisonEvaluator
import com.onlyfield.assetmanager.exchange.ProjectPackage
import com.onlyfield.assetmanager.exchange.XlsxExportManager
import com.onlyfield.assetmanager.export.PdfExportManager
import com.onlyfield.assetmanager.export.ProjectPrintDocumentAdapter
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers

/** PDF, Excel, Markdown and print output of a stored project. */
internal class DocumentExports(private val load: suspend (String) -> Project?) {
    private suspend fun getProjectById(projectId: String) = load(projectId)

    suspend fun exportRackPdfToStream(projectId: String, rackId: String, outputStream: OutputStream): Boolean {
        val project = getProjectById(projectId) ?: return false
        val rack = project.racks.find { it.id == rackId } ?: return false

        val allDevices = project.businessUnits.flatMap { it.devices }
        val devicesInRack = allDevices.filter { it.rackId == rackId }
        val unmountedDevices = allDevices.filter { (it.rackId == null) && (it.areaId == rack.areaId) }

        PdfExportManager.exportRackPdfToStream(
            project = project,
            rack = rack,
            devicesInRack = devicesInRack,
            unmountedDevices = unmountedDevices,
            outputStream = outputStream
        )
        return true
    }

    suspend fun exportXlsxToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream): Boolean {
        val project = getProjectById(projectId) ?: return false
        XlsxExportManager.exportXlsxToStream(project, filterConfig, outputStream)
        return true
    }

    suspend fun exportMarkdownToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream): Boolean {
        val project = getProjectById(projectId) ?: return false
        MarkdownExportManager.exportMarkdownToStream(project, filterConfig, outputStream)
        return true
    }

    suspend fun exportCompositePdfToStream(
        projectId: String,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        outputStream: OutputStream
    ): Boolean {
        val project = getProjectById(projectId) ?: return false
        PdfExportManager.exportCompositeReportPdfToStream(project, filterConfig, selection, outputStream)
        return true
    }

    suspend fun printProjectDocument(
        context: android.content.Context,
        projectId: String,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection
    ): Boolean {
        val project = getProjectById(projectId) ?: return false
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager ?: return false
        val jobName = "Report_${project.name}"
        val adapter = ProjectPrintDocumentAdapter(project, filterConfig, selection)
        printManager.print(jobName, adapter, null)
        return true
    }
}
