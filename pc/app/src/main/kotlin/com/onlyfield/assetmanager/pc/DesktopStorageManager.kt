package com.onlyfield.assetmanager.pc

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

    /** Folder holding the files of the attachments, one sub-folder per project. */
    fun getMediaFolder(): File = File(dataDir, "media")

    fun attachmentFile(projectId: String, attachment: Attachment): File =
        AttachmentFiles.localFile(getMediaFolder(), projectId, attachment)

    /** Copies [source] into the media folder as the file of [attachment]. */
    fun storeAttachmentFile(projectId: String, attachment: Attachment, source: File): File {
        val target = attachmentFile(projectId, attachment)
        target.parentFile?.mkdirs()
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return target
    }

    /** Saves the attachment files contained in an imported package; returns how many were written. */
    fun extractAttachments(pkg: ProjectPackage): Int = AttachmentFiles.extract(pkg, getMediaFolder())

    private val trashJson = Json { ignoreUnknownKeys = true }

    private fun trashFile(projectId: String) = File(dataDir, "trash/$projectId.json")

    /** Trash of a project, persisted between sessions (only for projects without a password). */
    fun loadTrash(projectId: String): List<TrashItem> = try {
        trashFile(projectId).takeIf { it.isFile }
            ?.let { trashJson.decodeFromString(ListSerializer(TrashItem.serializer()), it.readText(Charsets.UTF_8)) }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    fun saveTrash(projectId: String, items: List<TrashItem>) {
        val file = trashFile(projectId)
        if (items.isEmpty()) {
            file.delete()
            return
        }
        file.parentFile?.mkdirs()
        file.writeText(trashJson.encodeToString(ListSerializer(TrashItem.serializer()), items), Charsets.UTF_8)
    }

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
        } catch (_: Exception) {
            null
        } ?: run {
            raf.close()
            throw IllegalStateException("Impossibile acquisire il blocco esclusivo sul progetto $projectId: aperto da un'altra istanza.")
        }
        activeLocks[projectId] = Pair(raf, lock)
    }

    fun releaseProjectLock(projectId: String) {
        val pair = activeLocks.remove(projectId) ?: return
        try {
            pair.second.release()
            pair.first.close()
            val lockFile = File(getProjectsFolder(), "$projectId.lock")
            if (lockFile.exists()) lockFile.delete()
        } catch (_: Exception) {}
    }

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
                val importRes = PackageSerializer.importPackage(zipBytes = bytes)
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
        attachments: Map<String, ByteArray> = emptyMap()
    ): File {
        val status = checkDataDirectoryStatus()
        if (!status.isWritable) {
            throw IllegalStateException("La cartella dati '${dataDir.absolutePath}' non è scrivibile o il supporto è stato rimosso.")
        }

        val projectsDir = getProjectsFolder()
        val tmpDir = getTempFolder()

        val bytes = PackageSerializer.exportPackage(
            project = project,
            attachments = attachments,
            password = password
        )

        val tmpFile = File(tmpDir, "${project.id}_${System.currentTimeMillis()}.tmp")
        val targetFile = File(projectsDir, "${project.id}.ofam")

        try {
            tmpFile.writeBytes(bytes)
            Files.move(
                tmpFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            // Fallback if atomic move across filesystems fails
            Files.move(
                tmpFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        } finally {
            if (tmpFile.exists()) tmpFile.delete()
        }

        return targetFile
    }

    fun loadLocalProject(projectId: String, password: String? = null): PackageImportResult {
        val file = File(getProjectsFolder(), "$projectId.ofam")
        if (!file.exists()) {
            throw IllegalArgumentException("Progetto locale non trovato: ${file.absolutePath}")
        }
        acquireProjectLock(projectId)
        val bytes = file.readBytes()
        return PackageSerializer.importPackage(zipBytes = bytes, password = password)
    }

    fun importPackageFromFile(file: File, password: String? = null): PackageImportResult {
        if (!file.exists() || !file.canRead()) {
            throw IllegalArgumentException("Impossibile leggere il file specificato: ${file.absolutePath}")
        }
        val bytes = file.readBytes()
        return PackageSerializer.importPackage(zipBytes = bytes, password = password)
    }

    fun exportPackageToFile(
        project: Project,
        targetFile: File,
        password: String? = null,
        attachments: Map<String, ByteArray> = AttachmentFiles.collect(project) { attachmentFile(project.id, it) }
    ) {
        val parent = targetFile.parentFile ?: File(".")
        if (parent.exists() && !parent.canWrite()) {
            throw IllegalStateException("La cartella di destinazione non è scrivibile: ${parent.absolutePath}")
        }

        val bytes = PackageSerializer.exportPackage(
            project = project,
            attachments = attachments,
            password = password
        )

        val tmpFile = File(parent, "${targetFile.name}.tmp_${System.currentTimeMillis()}")
        try {
            tmpFile.writeBytes(bytes)
            Files.move(
                tmpFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(
                tmpFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        } finally {
            if (tmpFile.exists()) tmpFile.delete()
        }
    }

    fun loadAndroidFixtureFile(): Project {
        val candidatePaths = listOf(
            File("fixtures/v1_sample_project.json"),
            File("../fixtures/v1_sample_project.json"),
            File("../../fixtures/v1_sample_project.json")
        )
        val file = candidatePaths.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Fixture 'v1_sample_project.json' non trovata nei percorsi di ricerca.")

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
