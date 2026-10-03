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

/** .ofam export/import and the attachment files that travel with the package. */
internal class PackageExchange(
    private val attachmentsRoot: java.io.File?,
    private val load: suspend (String) -> Project?,
    private val save: suspend (Project) -> Unit,
    private val saveBase: suspend (Project) -> Unit,
) {
    private suspend fun getProjectById(projectId: String) = load(projectId)
    private suspend fun saveProject(project: Project) = save(project)

    suspend fun exportProjectPackage(projectId: String, password: String? = null): ByteArray? {
        val project = getProjectById(projectId) ?: return null
        val files = AttachmentFiles.collect(project) { attachmentFile(project.id, it) }
        return PackageSerializer.exportPackage(project, attachments = files, password = password).also { saveBase(project) }
    }

    /** Local file of an attachment, also accepting the older `filesDir`-relative path. */
    fun attachmentFile(projectId: String, attachment: com.onlyfield.assetmanager.core.model.Attachment): java.io.File? {
        val root = attachmentsRoot ?: return null
        val canonical = AttachmentFiles.localFile(root, projectId, attachment)
        if (canonical.isFile) return canonical
        return root.parentFile?.let { java.io.File(it, attachment.relativePath) }?.takeIf { attachment.relativePath.isNotBlank() && it.isFile }
    }

    fun attachmentsRoot(): java.io.File? = attachmentsRoot

    fun missingAttachments(project: Project) = AttachmentFiles.missing(project) { attachmentFile(project.id, it) }

    suspend fun exportProjectPackageToStream(
        projectId: String,
        outputStream: OutputStream,
        password: String? = null
    ): Boolean {
        val zipBytes = exportProjectPackage(projectId, password = password) ?: return false
        outputStream.use { stream ->
            stream.write(zipBytes)
            stream.flush()
        }
        return true
    }

    suspend fun evaluateImportPackage(
        inputStream: InputStream,
        password: String? = null,
        currentProjectId: String? = null
    ): PackageImportEvaluation {
        val bytes = inputStream.use { it.readBytes() }
        val importResult = PackageSerializer.importPackage(bytes, password = password)

        val pkg = importResult.pkg
        if ((pkg == null) || (!importResult.validationResult.isValid)) {
            return PackageImportEvaluation(importResult = importResult, comparison = null)
        }

        val localProjectId = currentProjectId ?: pkg.project.id
        val localProject = getProjectById(localProjectId)

        val comparison = ProjectComparisonEvaluator.evaluate(
            currentProject = localProject,
            currentManifest = null,
            incomingPackage = pkg
        )

        return PackageImportEvaluation(importResult = importResult, comparison = comparison)
    }

    suspend fun importProjectPackage(pkg: ProjectPackage): Boolean {
        attachmentsRoot?.let { AttachmentFiles.extract(pkg, it) }
        saveProject(pkg.project)
        saveBase(pkg.project)
        return true
    }

    /** Saves the result of a merge; the base becomes the package, i.e. what the other device has. */
    suspend fun importMerged(pkg: ProjectPackage, merged: Project) {
        attachmentsRoot?.let { AttachmentFiles.extract(pkg, it) }
        saveProject(merged)
        saveBase(pkg.project)
    }
}
