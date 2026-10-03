package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.data.repository.mappers.*
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
import kotlinx.coroutines.withContext

data class SearchResult(
    val device: Device,
    val businessUnitName: String,
    val siteName: String?,
    val areaName: String?,
    val matchedField: String,
)

data class PackageImportEvaluation(
    val importResult: PackageImportResult,
    val comparison: ProjectComparison?,
)

/**
 * Entry point of the Android data layer. Each area lives in its own class
 * (ProjectStore, DocumentExports, PackageExchange, InventorySearch, TrashOperations, CableTracer).
 */
class ProjectRepository(
    private val db: AppDatabase,
    /** Folder holding attachment files (`<root>/<projectId>/<attachmentId>/<name>`); null in tests without storage. */
    private val attachmentsRoot: java.io.File? = null,
) {
    private val projectDao = db.projectDao()
    private val inventoryDao = db.inventoryDao()
    private val store = ProjectStore(db)
    private val documents = DocumentExports(store::load)
    private val exchange = PackageExchange(attachmentsRoot, store::load, store::save)
    private val search = InventorySearch(db)
    private val trash = TrashOperations(db, store::load)

    // --- Projects ---

    fun getAllProjects(): Flow<List<ProjectEntity>> {
        return projectDao.getAllProjects()
    }

    suspend fun getProjectById(projectId: String): Project? = store.load(projectId)

    suspend fun saveProject(project: Project) = store.save(project)

    /** Deletes the project and, through cascading foreign keys, everything it contains. */
    suspend fun deleteProject(projectId: String) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun renameProject(projectId: String, newName: String) {
        val existing = projectDao.getProjectById(projectId) ?: return
        val updated = existing.copy(
            name = newName,
            updatedEpochMs = System.currentTimeMillis()
        )
        projectDao.updateProject(updated)
    }

    suspend fun verifyProjectPassword(projectId: String, password: String): Boolean {
        val projEntity = projectDao.getProjectById(projectId) ?: return false
        if (!projEntity.isPasswordProtected) return true
        val storedHash = projEntity.passwordHash ?: return false
        // PBKDF2 is CPU-bound: keep it off the main thread.
        val ok = withContext(Dispatchers.Default) { PasswordHasher.verify(password, storedHash) }
        if (ok && PasswordHasher.needsRehash(storedHash)) {
            val upgraded = withContext(Dispatchers.Default) { PasswordHasher.hash(password) }
            projectDao.updateProject(projEntity.copy(passwordHash = upgraded))
        }
        return ok
    }

    suspend fun setProjectPassword(projectId: String, currentPassword: String?, newPassword: String): Boolean {
        val projEntity = projectDao.getProjectById(projectId) ?: return false
        if (projEntity.isPasswordProtected) {
            if ((currentPassword == null) || (!verifyProjectPassword(projectId, currentPassword))) {
                return false
            }
        }
        val updated = projEntity.copy(
            isPasswordProtected = true,
            passwordHash = withContext(Dispatchers.Default) { PasswordHasher.hash(newPassword) },
            updatedEpochMs = System.currentTimeMillis()
        )
        projectDao.updateProject(updated)
        return true
    }

    suspend fun removeProjectPassword(projectId: String, currentPassword: String): Boolean {
        val projEntity = projectDao.getProjectById(projectId) ?: return false
        if (!projEntity.isPasswordProtected) return true
        if (!verifyProjectPassword(projectId, currentPassword)) return false

        val updated = projEntity.copy(
            isPasswordProtected = false,
            passwordHash = null,
            updatedEpochMs = System.currentTimeMillis()
        )
        projectDao.updateProject(updated)
        return true
    }

    // --- Documents ---

    suspend fun exportRackPdfToStream(projectId: String, rackId: String, outputStream: OutputStream) =
        documents.exportRackPdfToStream(projectId, rackId, outputStream)

    suspend fun exportXlsxToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream) =
        documents.exportXlsxToStream(projectId, filterConfig, outputStream)

    suspend fun exportMarkdownToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream) =
        documents.exportMarkdownToStream(projectId, filterConfig, outputStream)

    suspend fun exportCompositePdfToStream(projectId: String, filterConfig: ExportFilterConfig, selection: ReportSelection, outputStream: OutputStream) =
        documents.exportCompositePdfToStream(projectId, filterConfig, selection, outputStream)

    suspend fun printProjectDocument(context: Context, projectId: String, filterConfig: ExportFilterConfig, selection: ReportSelection) =
        documents.printProjectDocument(context, projectId, filterConfig, selection)

    // --- Packages and attachments ---

    suspend fun exportProjectPackage(projectId: String, password: String? = null) = exchange.exportProjectPackage(projectId, password)

    suspend fun exportProjectPackageToStream(projectId: String, outputStream: OutputStream, password: String? = null) =
        exchange.exportProjectPackageToStream(projectId, outputStream, password)

    suspend fun evaluateImportPackage(inputStream: InputStream, password: String? = null, currentProjectId: String? = null) =
        exchange.evaluateImportPackage(inputStream, password, currentProjectId)

    suspend fun importProjectPackage(pkg: ProjectPackage) = exchange.importProjectPackage(pkg)

    fun attachmentFile(projectId: String, attachment: com.onlyfield.assetmanager.core.model.Attachment) = exchange.attachmentFile(projectId, attachment)

    fun attachmentsRoot(): java.io.File? = attachmentsRoot

    fun missingAttachments(project: Project) = exchange.missingAttachments(project)

    suspend fun saveAttachment(projectId: String, attachment: com.onlyfield.assetmanager.core.model.Attachment) {
        inventoryDao.insertAttachments(listOf(toAttachmentEntity(projectId, attachment)))
    }

    suspend fun updateAreaFloorplan(projectId: String, areaId: String, attachmentId: String?, pageIndex: Int = 0) {
        val project = getProjectById(projectId) ?: return
        val updatedBus = project.businessUnits.map { bu ->
            val updatedSites = bu.sites.map { site ->
                val updatedAreas = site.areas.map { area ->
                    if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = pageIndex)
                    else area
                }
                site.copy(areas = updatedAreas)
            }
            val updatedDirectAreas = bu.areas.map { area ->
                if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = pageIndex)
                else area
            }
            bu.copy(sites = updatedSites, areas = updatedDirectAreas)
        }
        val updatedProject = project.copy(
            businessUnits = updatedBus,
            updatedEpochMs = System.currentTimeMillis()
        )
        saveProject(updatedProject)
    }

    suspend fun saveSharedPathSegment(projectId: String, segment: com.onlyfield.assetmanager.core.model.SharedPathSegment) {
        inventoryDao.insertSharedPathSegments(listOf(toSharedPathSegmentEntity(projectId, segment)))
    }

    // --- Search and cabling ---

    suspend fun searchInventory(projectId: String, query: String) = search.searchInventory(projectId, query)

    fun traceCableChain(project: Project, startPortId: String) = CableTracer.trace(project, startPortId)

    // --- Trash and device operations ---

    suspend fun getTrashItems(projectId: String) = trash.getTrashItems(projectId)

    suspend fun emptyTrash(projectId: String) = trash.emptyTrash(projectId)

    suspend fun deleteTrashItemPermanently(trashId: String) = trash.deleteTrashItemPermanently(trashId)

    suspend fun moveToTrash(projectId: String, itemType: String, itemId: String) = trash.moveToTrash(projectId, itemType, itemId)

    suspend fun restoreFromTrash(projectId: String, trashId: String) = trash.restoreFromTrash(projectId, trashId)

    suspend fun replaceDevice(projectId: String, oldDeviceId: String, newTechnicalName: String, newCategory: com.onlyfield.assetmanager.core.model.DeviceCategory) =
        trash.replaceDevice(projectId, oldDeviceId, newTechnicalName, newCategory)

    suspend fun mergeDevices(projectId: String, survivingDeviceId: String, duplicateDeviceId: String, choices: com.onlyfield.assetmanager.core.model.MergeDataChoices) =
        trash.mergeDevices(projectId, survivingDeviceId, duplicateDeviceId, choices)

    suspend fun batchEditDevices(projectId: String, deviceIds: List<String>, changes: com.onlyfield.assetmanager.core.model.BatchDeviceChanges) =
        trash.batchEditDevices(projectId, deviceIds, changes)
}
