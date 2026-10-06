package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.TrashItem
import kotlinx.serialization.builtins.ListSerializer
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageImportResult
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.ProjectPackage
import com.onlyfield.assetmanager.exchange.PackagePayloads
import com.onlyfield.assetmanager.exchange.PayloadViews
import com.onlyfield.assetmanager.exchange.FilePayloadMap
import com.onlyfield.assetmanager.exchange.ReversibleFiles
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class LocalPasswordRequired : IllegalStateException()
internal data class LocalProjectState(val trash: List<TrashItem>, val media: Map<String, ByteArray>, val project: Project? = null) : AutoCloseable {
    override fun close() { (media as? AutoCloseable)?.close() }
}

data class DataDirStatus(
    val path: File,
    val exists: Boolean,
    val isWritable: Boolean,
    val freeSpaceBytes: Long,
)

data class StoredProjectInfo(
    val id: String,
    val name: String,
    val file: File,
    val isEncrypted: Boolean,
    val lastModifiedEpochMs: Long,
    val readError: String? = null,
)

class DesktopStorageManager(
    initialDataDir: File = File(System.getProperty("user.home"), ".onlyfield_asset_manager")
) {

    var dataDir: File = initialDataDir
        private set

    private val activeLocks = mutableMapOf<String, Pair<RandomAccessFile, FileLock>>()
    private val mediaPayloads = java.util.concurrent.ConcurrentHashMap<String, PackagePayloads>()
    private fun stagedMedia(source: Map<String, ByteArray>, include: (String) -> Boolean = { true }): PackagePayloads = PackagePayloads(getTempFolder()).also { store ->
        try { store.copyFrom(source, include) } catch (e: Exception) { store.close(); throw e }
    }
    private val protectedMediaIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    var saveCleanupErrors: List<Exception> = emptyList()
        private set

    init {
        ensureDataDirStructure()
    }

    fun setDataDirectory(dir: File): DataDirStatus {
        dataDir = dir
        val status = checkDataDirectoryStatus()
        if (status.exists && status.isWritable) {
            ensureDataDirStructure()
        }
        return status
    }

    fun checkDataDirectoryStatus(): DataDirStatus {
        val exists = dataDir.exists() || try { dataDir.mkdirs() } catch (_: Exception) { false }
        val isWritable = if (exists) {
            try {
                val testFile = File(dataDir, ".write_test_${System.currentTimeMillis()}")
                val created = testFile.createNewFile()
                if (created) testFile.delete()
                created
            } catch (_: Exception) {
                false
            }
        } else false

        val freeSpace = if (exists) dataDir.freeSpace else 0L
        return DataDirStatus(
            path = dataDir,
            exists = exists,
            isWritable = isWritable,
            freeSpaceBytes = freeSpace
        )
    }

    private fun ensureDataDirStructure() {
        val projectsDir = File(dataDir, "projects")
        val tmpDir = File(dataDir, "tmp")
        if (!projectsDir.exists()) projectsDir.mkdirs()
        if (!tmpDir.exists()) tmpDir.mkdirs()
    }

    fun getProjectsFolder(): File = File(dataDir, "projects")
    fun getTempFolder(): File = File(dataDir, "tmp")


    private fun syncBaseFile(projectId: String) = File(dataDir, "sync/$projectId.ofam")

    /** Password-protected merge snapshot. */
    fun saveSyncBase(project: Project, password: String?) = withProjectLock(project.id) {
        atomicWrite(syncBaseFile(project.id), PackageSerializer.exportPackage(project, password = password, i18n = i18n))
    }

    fun loadSyncBase(projectId: String, password: String?): Project? {
        val file = syncBaseFile(projectId)
        if (!file.isFile) return null
        return file.inputStream().use { PackageSerializer.importPackage(it, password, i18n = i18n, stagingDirectory = getTempFolder()) }.pkg?.use { it.project }
            ?: throw IllegalStateException(i18n.text("text.61c13d777659"))
    }

    private fun prepareWrite(target: File, bytes: ByteArray): File {
        Files.createDirectories(target.parentFile.toPath())
        val temp = Files.createTempFile(target.parentFile.toPath(), target.name, ".tmp").toFile()
        try { temp.writeBytes(bytes); return temp } catch (e: Exception) { Files.deleteIfExists(temp.toPath()); throw e }
    }

    private fun replaceFile(temp: File, target: File) {
        try {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun atomicWrite(target: File, bytes: ByteArray) {
        val temp = prepareWrite(target, bytes)
        try { replaceFile(temp, target) } finally { Files.deleteIfExists(temp.toPath()) }
    }

    private fun preparePackage(target: File, project: Project, media: Map<String, ByteArray>, password: String?): File {
        Files.createDirectories(target.parentFile.toPath())
        val temp = Files.createTempFile(target.parentFile.toPath(), target.name, ".tmp").toFile()
        try {
            temp.outputStream().buffered().use { PackageSerializer.exportPackageToStream(it, project, media, password, i18n = i18n) }
            return temp
        } catch (e: Exception) { Files.deleteIfExists(temp.toPath()); throw e }
    }

    private fun writePackage(target: File, project: Project, media: Map<String, ByteArray>, password: String?) {
        val temp = preparePackage(target, project, media, password)
        try { replaceFile(temp, target) } finally { Files.deleteIfExists(temp.toPath()) }
    }

    /** Prepares project and merge snapshot for password changes. */
    fun changeProjectPassword(project: Project, oldPassword: String?, newPassword: String?, trashItems: List<TrashItem>) = withProjectLock(project.id) {
        val target = File(getProjectsFolder(), "${project.id}.ofam")
        val base = loadSyncBase(project.id, oldPassword)
        val local = target.inputStream().use { PackageSerializer.importPackage(it, oldPassword, i18n = i18n) }.pkg
            ?: throw IllegalStateException(i18n.text("text.42e4ee10f12f"))
        local.use {
            val retained = project.copy(attachments = project.attachments + com.onlyfield.assetmanager.core.edit.ProjectEdits.trashAttachments(trashItems))
            val media = AttachmentFiles.activePayloads(retained, PayloadViews.merge(local.attachments, localMedia(project.id)))
            saveProjectLocally(project, newPassword, attachments = media, trashItems = trashItems,
                syncBase = base?.copy(isPasswordProtected = newPassword != null))
        }
    }

    /** Per-project attachment folder. */
    fun getMediaFolder(): File = File(dataDir, "media")

    fun attachmentFile(projectId: String, attachment: Attachment): File =
        AttachmentFiles.localFile(getMediaFolder(), projectId, attachment)

    fun attachmentBytes(projectId: String, attachment: Attachment): ByteArray? =
        mediaPayloads[projectId]?.get(AttachmentFiles.entryName(attachment))
            ?: attachmentFile(projectId, attachment).takeIf { it.isFile }?.readBytes()

    fun hasAttachment(projectId: String, attachment: Attachment): Boolean =
        mediaPayloads[projectId]?.containsKey(AttachmentFiles.entryName(attachment)) == true ||
            (projectId !in protectedMediaIds && attachmentFile(projectId, attachment).isFile)

    fun mediaSnapshot(projectId: String): Map<String, ByteArray>? = mediaPayloads[projectId]
    internal fun mediaProtected(projectId: String) = projectId in protectedMediaIds

    fun restoreMedia(projectId: String, snapshot: Map<String, ByteArray>?, protected: Boolean = false) {
        if (protected) protectedMediaIds += projectId else protectedMediaIds -= projectId
        val restored = snapshot?.let { it as? PackagePayloads ?: stagedMedia(it) }
        val replaced = if (restored == null) mediaPayloads.remove(projectId) else mediaPayloads.put(projectId, restored)
        if (replaced !== restored) replaced?.close()
    }

    /** Protected media use bounded RAM and encrypted staging; the package is durable. */
    fun prepareMedia(project: Project, pkg: ProjectPackage?, protected: Boolean, local: Boolean, retainedMedia: Map<String, ByteArray> = emptyMap()) {
        val media = PackagePayloads(getTempFolder())
        try {
            if (local || pkg == null) media.copyFrom(localMedia(project.id))
            media.copyFrom(retainedMedia)
            pkg?.let {
                for (att in project.attachments) {
                    if (att !in it.project.attachments) continue
                    val path = AttachmentFiles.pathIn(it, att) ?: continue
                    val canonical = AttachmentFiles.entryName(att)
                    media.write(canonical) { output -> it.writePayload(path, output) }
                }
            }
            mediaPayloads[project.id] = media
            if (protected) protectedMediaIds += project.id else protectedMediaIds -= project.id
        } catch (e: Exception) { media.close(); throw e }
    }

    private fun plainMediaFiles(projectId: String): List<File> {
        return AttachmentFiles.ownedFiles(getMediaFolder(), projectId)
    }

    private fun localMedia(projectId: String): Map<String, ByteArray> =
        mediaPayloads[projectId] ?: FilePayloadMap(plainMediaFiles(projectId).associate { file ->
            "attachments/${file.relativeTo(File(getMediaFolder(), projectId)).invariantSeparatorsPath}" to file
        })

    fun hasRecoveryMedia(project: Project, trashItems: List<TrashItem>): Boolean {
        val media = localMedia(project.id)
        val retained = project.copy(attachments = project.attachments + com.onlyfield.assetmanager.core.edit.ProjectEdits.trashAttachments(trashItems))
        return media.keys != AttachmentFiles.activePayloads(retained, media).keys
    }

    fun storeAttachmentBytes(projectId: String, attachment: Attachment, bytes: ByteArray) {
        AttachmentFiles.validateSize(bytes.size.toLong(), i18n)
        val memory = mediaPayloads[projectId]
        if (memory != null) memory.putBytes(AttachmentFiles.entryName(attachment), bytes)
        if (projectId !in protectedMediaIds) {
            val target = attachmentFile(projectId, attachment)
            Files.createDirectories(target.parentFile.toPath())
            Files.write(target.toPath(), bytes)
        }
    }

    /** Copies [source] as [attachment]. */
    fun storeAttachmentFile(projectId: String, attachment: Attachment, source: File): File {
        AttachmentFiles.validateSize(source.length(), i18n)
        val memory = mediaPayloads[projectId]
        if (memory != null) {
            memory.write(AttachmentFiles.entryName(attachment)) { output ->
                source.inputStream().use { AttachmentFiles.copyBounded(it, output, i18n) }
            }
        }
        val target = attachmentFile(projectId, attachment)
        if (projectId !in protectedMediaIds) {
            Files.createDirectories(target.parentFile.toPath())
            source.inputStream().use { input -> target.outputStream().use { AttachmentFiles.copyBounded(input, it, i18n) } }
        }
        return target
    }

    /** Saves imported attachment files. */
    fun extractAttachments(pkg: ProjectPackage): Int {
        if (!mediaPayloads.containsKey(pkg.project.id)) return AttachmentFiles.extract(pkg, getMediaFolder())
        var count = 0
        pkg.project.attachments.forEach { attachment ->
            AttachmentFiles.bytesIn(pkg, attachment)?.let { storeAttachmentBytes(pkg.project.id, attachment, it); count++ }
        }
        return count
    }

    private val trashJson = Json { ignoreUnknownKeys = true }

    private fun trashFile(projectId: String) = File(dataDir, "trash/$projectId.json")

    var i18n: Messages = Messages()

    companion object { const val LOCAL_TRASH_ENTRY = "attachments/local/trash.json" }

    /** Reads local state without replacing corrupt data. */
    fun loadTrash(projectId: String, password: String? = null): List<TrashItem> = loadLocalState(projectId, password).use { it.trash }

    internal fun loadLocalState(projectId: String, password: String?): LocalProjectState {
        val local = File(getProjectsFolder(), "$projectId.ofam")
        var media = emptyMap<String, ByteArray>()
        var localProject: Project? = null
        val bytes = if (local.isFile) {
            val result = local.inputStream().use { PackageSerializer.importPackage(it, password, i18n = i18n, stagingDirectory = getTempFolder()) }
            if (result.validationResult.issues.any { it.code == "PASSWORD_REQUIRED" || it.code == "INVALID_PACKAGE_PASSWORD" }) throw LocalPasswordRequired()
            val pkg = result.pkg ?: throw IllegalStateException(i18n.text("text.0f81b75705c7"))
            pkg.use {
                localProject = it.project
                check(result.validationResult.issues.none { issue -> issue.targetEntityId == LOCAL_TRASH_ENTRY }) { i18n.text("text.72ea3e1800e5") }
                val trash = it.attachments[LOCAL_TRASH_ENTRY]
                media = stagedMedia(it.attachments) { path -> path != LOCAL_TRASH_ENTRY }
                trash
            }
        } else null
        try {
            val text = bytes?.toString(Charsets.UTF_8) ?: trashFile(projectId).takeIf { it.isFile }?.readText(Charsets.UTF_8) ?: return LocalProjectState(emptyList(), media, localProject)
            val items = trashJson.decodeFromString(ListSerializer(TrashItem.serializer()), text)
            require(items.all { it.projectId == projectId }) { i18n.text("text.7e60756fd2ba") }
            return LocalProjectState(items, media, localProject)
        } catch (e: Exception) { (media as? AutoCloseable)?.close(); throw e }
    }

    private fun trashBytes(items: List<TrashItem>) = trashJson.encodeToString(ListSerializer(TrashItem.serializer()), items).toByteArray(Charsets.UTF_8)

    fun missingAttachments(project: Project): List<Attachment> =
        project.attachments.filter { attachmentBytes(project.id, it) == null }

    fun acquireProjectLock(projectId: String) {
        if (activeLocks.containsKey(projectId)) return
        val lockFile = File(getProjectsFolder(), "$projectId.lock")
        val raf = RandomAccessFile(lockFile, "rw")
        val channel: FileChannel = raf.channel
        val lock = try {
            channel.tryLock()
        } catch (_: java.nio.channels.OverlappingFileLockException) {
            null
        } catch (e: java.io.IOException) {
            raf.close()
            throw e
        } ?: run {
            raf.close()
            throw IllegalStateException(i18n.text("text.8bbb3adbe88a", projectId))
        }
        activeLocks[projectId] = Pair(raf, lock)
    }

    fun releaseProjectLock(projectId: String) {
        mediaPayloads.remove(projectId)?.close()
        protectedMediaIds.remove(projectId)
        val pair = activeLocks.remove(projectId) ?: return
        try {
            pair.second.release()
        } finally {
            pair.first.close()
        }
    }

    fun ownsProjectLock(projectId: String): Boolean = activeLocks.containsKey(projectId)

    private inline fun <T> withProjectLock(projectId: String, action: () -> T): T {
        val alreadyOwned = ownsProjectLock(projectId)
        acquireProjectLock(projectId)
        try { return action() } finally { if (!alreadyOwned) releaseProjectLock(projectId) }
    }

    fun isLocalProjectFile(file: File): Boolean = file.canonicalFile.parentFile == getProjectsFolder().canonicalFile

    fun releaseAllLocks() {
        val keys = activeLocks.keys.toList()
        for (key in keys) {
            releaseProjectLock(key)
        }
    }

    fun listStoredProjects(): List<StoredProjectInfo> {
        val projectsDir = getProjectsFolder()
        if (!projectsDir.exists() || !projectsDir.canRead()) return emptyList()

        val files = projectsDir.listFiles { _, name -> name.endsWith(".ofam") } ?: return emptyList()
        val result = mutableListOf<StoredProjectInfo>()

        for (file in files) {
            try {
                val manifest = PackageSerializer.readManifest(file, i18n)
                result += StoredProjectInfo(manifest.projectId, manifest.projectName, file, manifest.isEncrypted, file.lastModified())
            } catch (e: java.io.IOException) {
                result += unreadableProject(file, e)
            } catch (e: kotlinx.serialization.SerializationException) {
                result += unreadableProject(file, e)
            } catch (e: IllegalArgumentException) {
                result += unreadableProject(file, e)
            }
        }
        return result.sortedByDescending { it.lastModifiedEpochMs }
    }

    private fun unreadableProject(file: File, error: Exception) = StoredProjectInfo(
        file.nameWithoutExtension, file.nameWithoutExtension, file, false, file.lastModified(),
        readError = i18n.text("text.953ce2808a1f", file.name) + error.message.orEmpty(),
    )

    fun saveProjectLocally(
        project: Project,
        password: String? = null,
        attachments: Map<String, ByteArray> = emptyMap(),
        trashItems: List<TrashItem>? = null,
        syncBase: Project? = null,
        recoverableAttachments: List<Attachment> = emptyList(),
    ): File = withProjectLock(project.id) {
        val status = checkDataDirectoryStatus()
        if (!status.isWritable) {
            throw IllegalStateException(i18n.text("text.a28d4f63178f", dataDir.absolutePath))
        }

        val items = trashItems ?: loadTrash(project.id, password)
        require(items.all { it.projectId == project.id }) { i18n.text("text.7e60756fd2ba") }
        val targetFile = File(getProjectsFolder(), "${project.id}.ofam")
        val recovery = recoverableAttachments + com.onlyfield.assetmanager.core.edit.ProjectEdits.trashAttachments(items)
        val retained = project.copy(attachments = (project.attachments + recovery).distinctBy { it.id })
        val media = AttachmentFiles.activePayloads(retained, PayloadViews.merge(localMedia(project.id), attachments))
        val cached = mediaPayloads[project.id]
        val next = if (password != null && (attachments.isNotEmpty() || cached == null || cached.keys != media.keys)) stagedMedia(media) else null
        var committed = false
        try {
            ReversibleFiles(getTempFolder()).use { files ->
                files.replace(targetFile) { PackageSerializer.exportPackageToStream(it, project, PayloadViews.merge(media, mapOf(LOCAL_TRASH_ENTRY to trashBytes(items))), password, i18n = i18n) }
                syncBase?.let { base ->
                    require(base.id == project.id)
                    files.replace(syncBaseFile(project.id)) { PackageSerializer.exportPackageToStream(it, base, password = password, i18n = i18n) }
                }
                if (password != null) plainMediaFiles(project.id).forEach(files::remove)
                else for (att in retained.attachments) {
                    val path = AttachmentFiles.entryName(att)
                    if (media.containsKey(path)) files.replace(attachmentFile(project.id, att)) { output ->
                        if (media is PackagePayloads) media.writeTo(path, output) else output.write(media.getValue(path))
                    }
                }
                if (password == null) AttachmentFiles.stageCollection(getMediaFolder(), project, recovery, files)
                files.remove(trashFile(project.id))
                files.apply()
                saveCleanupErrors = files.commit()
                committed = true
            }
        } finally { if (!committed) next?.close() }
        if (password != null) protectedMediaIds += project.id else protectedMediaIds -= project.id
        if (next != null) {
            val previous = mediaPayloads.put(project.id, next)
            try { previous?.close() } catch (e: Exception) { saveCleanupErrors = saveCleanupErrors + e }
        }
        if (password == null) cached?.keys?.toList()?.filterNot { it in media.keys }?.forEach { name ->
            try { cached.remove(name) } catch (e: Exception) { saveCleanupErrors = saveCleanupErrors + e }
        }
        try { AttachmentFiles.pruneEmptyDirectories(getMediaFolder(), project.id) }
        catch (e: java.io.IOException) { saveCleanupErrors = saveCleanupErrors + e }
        targetFile
    }

    fun loadLocalProject(projectId: String, password: String? = null): PackageImportResult {
        val file = File(getProjectsFolder(), "$projectId.ofam")
        if (!file.exists()) {
            throw IllegalArgumentException(i18n.text("text.c5806cf5c477", file.absolutePath))
        }
        val alreadyOwned = ownsProjectLock(projectId)
        acquireProjectLock(projectId)
        try {
            val result = file.inputStream().use { PackageSerializer.importPackage(it, password = password, i18n = i18n, stagingDirectory = getTempFolder()) }
            if (result.pkg?.project?.id != null && result.pkg?.project?.id != projectId) {
                result.pkg?.close()
                throw IllegalArgumentException(i18n.text("text.6c9d85161290"))
            }
            if (result.pkg == null && !alreadyOwned) releaseProjectLock(projectId)
            return result
        } catch (e: Exception) {
            if (!alreadyOwned) releaseProjectLock(projectId)
            throw e
        }
    }

    fun importPackageFromFile(file: File, password: String? = null): PackageImportResult {
        if (!file.exists() || !file.canRead()) {
            throw IllegalArgumentException(i18n.text("text.d60173daedb8", file.absolutePath))
        }
        return file.inputStream().use { PackageSerializer.importPackage(it, password = password, i18n = i18n, stagingDirectory = getTempFolder()) }
    }

    fun exportPackageToFile(
        project: Project,
        targetFile: File,
        password: String? = null,
        attachments: Map<String, ByteArray> = localMedia(project.id)
    ) {
        val parent = targetFile.parentFile ?: File(".")
        if (parent.exists() && !parent.canWrite()) {
            throw IllegalStateException(i18n.text("text.d4de3136debe", parent.absolutePath))
        }

        val shared = PayloadViews.select(attachments, attachments.keys - LOCAL_TRASH_ENTRY)
        val exported = com.onlyfield.assetmanager.core.edit.ProjectEdits.purgeTrashAttachments(project, loadTrash(project.id, password))
            .copy(updatedEpochMs = project.updatedEpochMs)
        writePackage(targetFile, exported, AttachmentFiles.activePayloads(exported, shared), password)
    }

    fun loadAndroidFixtureFile(): Project {
        val candidatePaths = listOf(
            File("fixtures/v1_sample_project.json"),
            File("../fixtures/v1_sample_project.json"),
            File("../../fixtures/v1_sample_project.json")
        )
        val file = candidatePaths.firstOrNull { it.exists() }
            ?: throw IllegalStateException(i18n.text("text.25fbf87f38fe"))

        val jsonText = file.readText(Charsets.UTF_8)
        return PackageSerializer.jsonConfig.decodeFromString(Project.serializer(), jsonText)
    }

    fun cleanTempFolder() {
        val tmpDir = getTempFolder()
        if (tmpDir.exists()) {
            tmpDir.listFiles()?.forEach {
                try { it.delete() } catch (_: Exception) {}
            }
        }
    }
}
