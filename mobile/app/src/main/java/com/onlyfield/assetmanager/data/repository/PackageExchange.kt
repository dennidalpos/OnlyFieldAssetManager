package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.ProjectComparisonEvaluator
import com.onlyfield.assetmanager.exchange.ProjectPackage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.InputStream
import java.io.OutputStream

/** .ofam export/import and the attachment files that travel with the package. */
internal class PackageExchange(
    private val attachmentsRoot: java.io.File?,
    private val load: suspend (String) -> Project?,
    private val save: suspend (Project) -> Unit,
    private val saveBase: suspend (Project) -> Unit,
    private val saveImported: suspend (Project, String?) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val transaction: suspend (suspend () -> Unit) -> Unit = { it() },
    private val loadTrash: suspend (String) -> List<com.onlyfield.assetmanager.core.model.TrashItem> = { emptyList() },
    private val recovery: ProjectRecovery? = null,
    private val evaluateLocal: suspend (String) -> Project? = load,
) {
    init { require(attachmentsRoot == null || recovery != null) { "File storage requires durable recovery" } }
    private suspend fun getProjectById(projectId: String) = load(projectId)
    private suspend fun saveProject(project: Project) = save(project)

    suspend fun exportProjectPackage(projectId: String, password: String? = null, i18n: Messages = Messages()): ByteArray? = withContext(ioDispatcher) {
        val stored = getProjectById(projectId) ?: return@withContext null
        val project = com.onlyfield.assetmanager.core.edit.ProjectEdits.purgeTrashAttachments(stored, loadTrash(projectId))
            .copy(updatedEpochMs = stored.updatedEpochMs)
        val files = AttachmentFiles.collect(project) { attachmentFile(project.id, it) }
        return@withContext PackageSerializer.exportPackage(project, attachments = files, password = password, i18n = i18n).also { saveBase(project) }
    }

    /** Resolve only the project-scoped file; imported paths never address local storage. */
    fun attachmentFile(projectId: String, attachment: com.onlyfield.assetmanager.core.model.Attachment): java.io.File? {
        val root = attachmentsRoot ?: return null
        return AttachmentFiles.localFile(root, projectId, attachment).takeIf { it.isFile }
    }

    fun attachmentsRoot(): java.io.File? = attachmentsRoot

    fun missingAttachments(project: Project) = AttachmentFiles.missing(project) { attachmentFile(project.id, it) }

    suspend fun exportProjectPackageToStream(
        projectId: String,
        outputStream: OutputStream,
        password: String? = null,
        i18n: Messages = Messages()): Boolean = withContext(ioDispatcher) {
        val stored = getProjectById(projectId) ?: return@withContext false
        val project = com.onlyfield.assetmanager.core.edit.ProjectEdits.purgeTrashAttachments(stored, loadTrash(projectId))
            .copy(updatedEpochMs = stored.updatedEpochMs)
        val files = AttachmentFiles.collect(project) { attachmentFile(project.id, it) }
        outputStream.use { stream ->
            PackageSerializer.exportPackageToStream(stream, project, files, password, i18n = i18n)
        }
        saveBase(project)
        return@withContext true
    }

    suspend fun evaluateImportPackage(
        inputStream: InputStream,
        password: String? = null,
        i18n: Messages = Messages()): PackageImportEvaluation {
        var imported: com.onlyfield.assetmanager.exchange.PackageImportResult? = null
        try {
            return withContext(ioDispatcher) {
                val importResult = runInterruptible {
                    inputStream.use { PackageSerializer.importPackage(it, password = password, i18n = i18n) }.also { imported = it }
                }
                currentCoroutineContext().ensureActive()
                val pkg = importResult.pkg
                if (pkg == null || !importResult.validationResult.isValid) {
                    return@withContext PackageImportEvaluation(importResult = importResult, comparison = null)
                }
                val comparison = ProjectComparisonEvaluator.evaluate(
                    currentProject = evaluateLocal(pkg.project.id),
                    currentManifest = null,
                    incomingPackage = pkg,
                    i18n = i18n,
                )
                PackageImportEvaluation(importResult = importResult, comparison = comparison)
            }
        } catch (e: Exception) {
            imported?.pkg?.close()
            throw e
        }
    }

    suspend fun importProjectPackage(pkg: ProjectPackage, password: String?): Boolean = withContext(ioDispatcher) {
        require(!pkg.project.isPasswordProtected || !password.isNullOrBlank()) { "A protected import requires its package password" }
        commitImport(pkg, pkg.project) { saveImported(it, password) }
        return@withContext true
    }

    /** Saves the result of a merge; the base becomes the package, i.e. what the other device has. */
    suspend fun importMerged(pkg: ProjectPackage, merged: Project) = withContext(ioDispatcher) {
        val local = requireNotNull(getProjectById(merged.id)) { "A merge requires a local project" }
        val selected = merged.copy(isPasswordProtected = local.isPasswordProtected)
        commitImport(pkg, selected) { saveProject(it) }
    }

    private suspend fun commitImport(pkg: ProjectPackage, incoming: Project, persist: suspend (Project) -> Unit) {
        val local = getProjectById(incoming.id)
        val selected = if (local == null) incoming else com.onlyfield.assetmanager.core.edit.ProjectEdits.retainTrashAttachments(incoming, local, loadTrash(incoming.id))
        require(selected.id == pkg.project.id)
        (recovery?.files(selected.id) ?: com.onlyfield.assetmanager.exchange.ReversibleFiles()).use { files ->
            attachmentsRoot?.let { AttachmentFiles.stage(pkg, it, selected, files) }
            currentCoroutineContext().ensureActive()
            // The commit outcome must survive cancellation at the Room return boundary.
            withContext(kotlinx.coroutines.NonCancellable) {
                transaction { persist(selected); saveBase(pkg.project); recovery?.expectState(files, selected.id); files.apply() }
                files.databaseCommitted()
                val cleanupErrors = files.commit()
                if (cleanupErrors.isNotEmpty()) android.util.Log.w("PackageExchange", "Import committed; staging cleanup failed", cleanupErrors.first())
            }
        }
    }
}
