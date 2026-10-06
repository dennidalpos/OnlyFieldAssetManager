package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.data.repository.mappers.*
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.data.local.AppDatabase
import android.content.Context
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.exchange.PackageImportResult
import com.onlyfield.assetmanager.exchange.PasswordHasher
import com.onlyfield.assetmanager.exchange.ProjectComparison
import com.onlyfield.assetmanager.exchange.ProjectPackage
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import androidx.room.withTransaction

data class SearchResult(
    val device: Device,
    val siteName: String,
    val areaName: String?,
    val matchedField: String,
)

data class PackageImportEvaluation(
    val importResult: PackageImportResult,
    val comparison: ProjectComparison?,
)

/** Android data facade over focused repository services. */
class ProjectRepository(
    db: AppDatabase,
    /** Attachment root; null in storage-free tests. */
    private val attachmentsRoot: java.io.File? = null,
) {
    private val projectDao = db.projectDao()
    private val inventoryDao = db.inventoryDao()
    private val store = ProjectStore(db)
    private val documents = DocumentExports(store::load)
    private val exchange = PackageExchange(attachmentsRoot, store::load, { store.save(it) }, store::saveBase, store::saveImported,
        transaction = { db.withTransaction { it() } })
    private val search = InventorySearch(db)
    private val trash = TrashOperations(db, store::load, { store.save(it) })

    // --- Projects ---

    fun getAllProjects(): Flow<List<ProjectEntity>> {
        return projectDao.getAllProjects()
    }

    suspend fun getProjectById(projectId: String): Project? = store.load(projectId)

    suspend fun saveProject(project: Project) = store.save(project)

    /** Media additions must remain exportable before committing their catalog. */
    suspend fun saveMediaProject(project: Project, i18n: Messages = Messages(), onCommitted: () -> Unit = {}) = withContext(Dispatchers.IO) {
        com.onlyfield.assetmanager.exchange.AttachmentFiles.validateCapacity(project, { exchange.attachmentFile(project.id, it) }, i18n)
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        // A cancelled dispatcher return must not trigger deletion after a durable commit.
        withContext(kotlinx.coroutines.NonCancellable) { store.save(project); onCommitted() }
    }

    /** Deletes the project and, through cascading foreign keys, everything it contains. */
    suspend fun deleteProject(projectId: String) {
        projectDao.deleteSyncSnapshot(projectId)
        projectDao.deleteProjectById(projectId)
    }

    suspend fun renameProject(projectId: String, newName: String) {
        val existing = projectDao.getProjectById(projectId) ?: return
        val updated = existing.copy(
            name = newName,
            updatedEpochMs = System.currentTimeMillis(),
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
            updatedEpochMs = System.currentTimeMillis(),
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
            updatedEpochMs = System.currentTimeMillis(),
        )
        projectDao.updateProject(updated)
        return true
    }

    // --- Documents ---

    suspend fun exportRackPdfToStream(projectId: String, rackId: String, outputStream: OutputStream, i18n: Messages = Messages()) =
        documents.exportRackPdfToStream(projectId, rackId, outputStream, i18n = i18n)

    suspend fun exportXlsxToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream, i18n: Messages = Messages()) =
        documents.exportXlsxToStream(projectId, filterConfig, outputStream, i18n = i18n)

    suspend fun exportMarkdownToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream, i18n: Messages = Messages()) =
        documents.exportMarkdownToStream(projectId, filterConfig, outputStream, i18n = i18n)

    suspend fun exportCompositePdfToStream(projectId: String, filterConfig: ExportFilterConfig, selection: ReportSelection, outputStream: OutputStream, i18n: Messages = Messages()) =
        documents.exportCompositePdfToStream(projectId, filterConfig, selection, outputStream, i18n = i18n)

    suspend fun printProjectDocument(context: Context, projectId: String, filterConfig: ExportFilterConfig, selection: ReportSelection, i18n: Messages = Messages()) =
        documents.printProjectDocument(context, projectId, filterConfig, selection, i18n = i18n)

    // --- Packages and attachments ---

    suspend fun exportProjectPackage(projectId: String, password: String? = null, i18n: Messages = Messages()) = exchange.exportProjectPackage(projectId, password, i18n = i18n)

    suspend fun exportProjectPackageToStream(projectId: String, outputStream: OutputStream, password: String? = null, i18n: Messages = Messages()) =
        exchange.exportProjectPackageToStream(projectId, outputStream, password, i18n = i18n)

    suspend fun evaluateImportPackage(inputStream: InputStream, password: String? = null, i18n: Messages = Messages()) =
        exchange.evaluateImportPackage(inputStream, password, i18n = i18n)

    suspend fun importProjectPackage(pkg: ProjectPackage, password: String? = null) = exchange.importProjectPackage(pkg, password)

    suspend fun importMergedPackage(pkg: ProjectPackage, merged: Project) = exchange.importMerged(pkg, merged)

    /** Last synced snapshot of the project, or null if it was never exported/imported. */
    suspend fun getSyncBase(projectId: String): Project? = store.loadBase(projectId)

    fun attachmentFile(projectId: String, attachment: com.onlyfield.assetmanager.core.model.Attachment) = exchange.attachmentFile(projectId, attachment)

    fun attachmentsRoot(): java.io.File? = attachmentsRoot

    fun missingAttachments(project: Project) = exchange.missingAttachments(project)

    suspend fun saveAttachment(projectId: String, attachment: com.onlyfield.assetmanager.core.model.Attachment) {
        inventoryDao.insertAttachments(listOf(toAttachmentEntity(projectId, attachment)))
    }

    suspend fun updateAreaFloorplan(projectId: String, areaId: String, attachmentId: String?, pageIndex: Int = 0) {
        val project = getProjectById(projectId) ?: return
        val updatedSites = project.sites.map { site ->
            val updatedDirectAreas = site.areas.map { area ->
                if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = pageIndex)
                else area
            }
            site.copy(areas = updatedDirectAreas)
        }
        val updatedProject = project.copy(
            sites = updatedSites,
            updatedEpochMs = System.currentTimeMillis(),
        )
        saveProject(updatedProject)
    }

    // --- Search and cabling ---

    suspend fun searchInventory(projectId: String, query: String, i18n: Messages = Messages()) = search.searchInventory(projectId, query, i18n = i18n)

    fun traceCableChain(project: Project, startPortId: String, i18n: Messages = Messages()) = com.onlyfield.assetmanager.core.model.ConnectionGraph(project).trace(startPortId, i18n = i18n)

    // --- Trash and device operations ---

    suspend fun getTrashItems(projectId: String) = trash.getTrashItems(projectId)

    suspend fun emptyTrash(projectId: String) = trash.emptyTrash(projectId)

    suspend fun deleteTrashItemPermanently(trashId: String) = trash.deleteTrashItemPermanently(trashId)

    suspend fun moveToTrash(projectId: String, itemType: String, itemId: String, i18n: Messages = Messages()) = trash.moveToTrash(projectId, itemType, itemId, i18n = i18n)

    suspend fun restoreFromTrash(projectId: String, trashId: String, i18n: Messages = Messages()) = trash.restoreFromTrash(projectId, trashId, i18n = i18n)

    suspend fun replaceDevice(projectId: String, oldDeviceId: String, newTechnicalName: String, newCategory: com.onlyfield.assetmanager.core.model.DeviceCategory, i18n: Messages = Messages()) =
        trash.replaceDevice(projectId, oldDeviceId, newTechnicalName, newCategory, i18n = i18n)

    suspend fun mergeDevices(projectId: String, survivingDeviceId: String, duplicateDeviceId: String, choices: com.onlyfield.assetmanager.core.model.MergeDataChoices, i18n: Messages = Messages()) =
        trash.mergeDevices(projectId, survivingDeviceId, duplicateDeviceId, choices, i18n = i18n)

    suspend fun batchEditDevices(projectId: String, deviceIds: List<String>, changes: com.onlyfield.assetmanager.core.model.BatchDeviceChanges, i18n: Messages = Messages()) =
        trash.batchEditDevices(projectId, deviceIds, changes, i18n = i18n)
}
