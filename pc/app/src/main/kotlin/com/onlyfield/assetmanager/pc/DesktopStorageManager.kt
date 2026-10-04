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
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
)

class DesktopStorageManager(
    initialDataDir: File = File(System.getProperty("user.home"), ".onlyfield_asset_manager")
) {

    var dataDir: File = initialDataDir
        private set

    private val activeLocks = mutableMapOf<String, Pair<RandomAccessFile, FileLock>>()

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
        return PackageSerializer.importPackage(file.readBytes(), password, i18n = i18n).pkg?.project
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

    /** Prepares project and merge snapshot for password changes. */
    fun changeProjectPassword(project: Project, oldPassword: String?, newPassword: String?, trashItems: List<TrashItem>) = withProjectLock(project.id) {
        val target = File(getProjectsFolder(), "${project.id}.ofam")
        val syncFile = syncBaseFile(project.id)
        val oldSyncBytes = syncFile.takeIf { it.isFile }?.readBytes()
        val base = loadSyncBase(project.id, oldPassword)
        val local = PackageSerializer.importPackage(target.readBytes(), oldPassword, i18n = i18n).pkg
            ?: throw IllegalStateException(i18n.text("text.42e4ee10f12f"))
        val mainTemp = prepareWrite(target, PackageSerializer.exportPackage(project, local.attachments + (LOCAL_TRASH_ENTRY to trashBytes(trashItems)), newPassword, i18n = i18n))
        var syncTemp: File? = null
        var syncReplaced = false
        try {
            if (base != null) {
                syncTemp = prepareWrite(syncFile, PackageSerializer.exportPackage(base.copy(isPasswordProtected = newPassword != null), password = newPassword, i18n = i18n))
                replaceFile(syncTemp, syncFile)
                syncReplaced = true
            }
            replaceFile(mainTemp, target)
            Files.deleteIfExists(trashFile(project.id).toPath())
        } catch (e: Exception) {
            if (syncReplaced && oldSyncBytes != null) {
                try { atomicWrite(syncFile, oldSyncBytes) } catch (rollback: Exception) { e.addSuppressed(rollback) }
            }
            throw e
        } finally {
            Files.deleteIfExists(mainTemp.toPath())
            syncTemp?.let { Files.deleteIfExists(it.toPath()) }
        }
    }

    /** Per-project attachment folder. */
    fun getMediaFolder(): File = File(dataDir, "media")

    fun attachmentFile(projectId: String, attachment: Attachment): File =
        AttachmentFiles.localFile(getMediaFolder(), projectId, attachment)

    /** Copies [source] as [attachment]. */
    fun storeAttachmentFile(projectId: String, attachment: Attachment, source: File): File {
        val target = attachmentFile(projectId, attachment)
        target.parentFile?.mkdirs()
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return target
    }

    /** Saves imported attachment files. */
    fun extractAttachments(pkg: ProjectPackage): Int = AttachmentFiles.extract(pkg, getMediaFolder())

    private val trashJson = Json { ignoreUnknownKeys = true }

    private fun trashFile(projectId: String) = File(dataDir, "trash/$projectId.json")

    var i18n: Messages = Messages()

    companion object { const val LOCAL_TRASH_ENTRY = "attachments/local/trash.json" }

    /** Reads local state without replacing corrupt data. */
    fun loadTrash(projectId: String, password: String? = null): List<TrashItem> {
        val local = File(getProjectsFolder(), "$projectId.ofam")
        val bytes = if (local.isFile) {
            val result = PackageSerializer.importPackage(local.readBytes(), password, i18n = i18n)
            check(result.validationResult.issues.none { it.targetEntityId == LOCAL_TRASH_ENTRY }) { i18n.text("text.72ea3e1800e5") }
            val pkg = result.pkg ?: throw IllegalStateException(i18n.text("text.0f81b75705c7"))
            pkg.attachments[LOCAL_TRASH_ENTRY]
        } else null
        val text = bytes?.toString(Charsets.UTF_8) ?: trashFile(projectId).takeIf { it.isFile }?.readText(Charsets.UTF_8) ?: return emptyList()
        val items = trashJson.decodeFromString(ListSerializer(TrashItem.serializer()), text)
        require(items.all { it.projectId == projectId }) { i18n.text("text.7e60756fd2ba") }
        return items
    }

    private fun trashBytes(items: List<TrashItem>) = trashJson.encodeToString(ListSerializer(TrashItem.serializer()), items).toByteArray(Charsets.UTF_8)

    fun missingAttachments(project: Project): List<Attachment> =
        AttachmentFiles.missing(project) { attachmentFile(project.id, it) }

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
                val bytes = file.readBytes()
                val importRes = PackageSerializer.importPackage(zipBytes = bytes, i18n = i18n)
                val manifest = importRes.pkg?.manifest
                if (manifest != null) {
                    result.add(
                        StoredProjectInfo(
                            id = manifest.projectId,
                            name = manifest.projectName,
                            file = file,
                            isEncrypted = manifest.isEncrypted,
                            lastModifiedEpochMs = file.lastModified()
                        )
                    )
                } else if (importRes.validationResult.issues.any { it.code == "PASSWORD_REQUIRED" }) {
                    val projId = file.nameWithoutExtension
                    result.add(
                        StoredProjectInfo(
                            id = projId,
                            name = file.nameWithoutExtension,
                            file = file,
                            isEncrypted = true,
                            lastModifiedEpochMs = file.lastModified()
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        return result.sortedByDescending { it.lastModifiedEpochMs }
    }

    fun saveProjectLocally(
        project: Project,
        password: String? = null,
        attachments: Map<String, ByteArray> = emptyMap(),
        trashItems: List<TrashItem>? = null
    ): File = withProjectLock(project.id) {
        val status = checkDataDirectoryStatus()
        if (!status.isWritable) {
            throw IllegalStateException(i18n.text("text.a28d4f63178f", dataDir.absolutePath))
        }

        val items = trashItems ?: loadTrash(project.id, password)
        require(items.all { it.projectId == project.id }) { i18n.text("text.7e60756fd2ba") }
        val targetFile = File(getProjectsFolder(), "${project.id}.ofam")
        atomicWrite(targetFile, PackageSerializer.exportPackage(project, attachments + (LOCAL_TRASH_ENTRY to trashBytes(items)), password, i18n = i18n))
        Files.deleteIfExists(trashFile(project.id).toPath())
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
            val result = PackageSerializer.importPackage(zipBytes = file.readBytes(), password = password, i18n = i18n)
            require(result.pkg?.project?.id == null || result.pkg?.project?.id == projectId) { i18n.text("text.6c9d85161290") }
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
        val bytes = file.readBytes()
        return PackageSerializer.importPackage(zipBytes = bytes, password = password, i18n = i18n)
    }

    fun exportPackageToFile(
        project: Project,
        targetFile: File,
        password: String? = null,
        attachments: Map<String, ByteArray> = AttachmentFiles.collect(project) { attachmentFile(project.id, it) }
    ) {
        val parent = targetFile.parentFile ?: File(".")
        if (parent.exists() && !parent.canWrite()) {
            throw IllegalStateException(i18n.text("text.d4de3136debe", parent.absolutePath))
        }

        val bytes = PackageSerializer.exportPackage(
            project = project,
            attachments = attachments,
            password = password,
            i18n = i18n)

        atomicWrite(targetFile, bytes)
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
