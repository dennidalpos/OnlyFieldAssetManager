package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.i18n.Messages
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.UUID

/** Shared attachment paths for packages and local storage. */
object AttachmentFiles {

    /** Enumerate only this project's owned files; never follow links out of storage. */
    fun ownedFiles(root: File, projectId: String): List<File> {
        require(UUID.fromString(projectId).toString() == projectId) { "Invalid project ID" }
        val base = root.toPath().toAbsolutePath().normalize()
        val project = base.resolve(projectId)
        require(project.startsWith(base) && project != base)
        require(!Files.isSymbolicLink(base) && !Files.isSymbolicLink(project)) { "Linked media storage is unsupported" }
        if (!Files.exists(project, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        return Files.walk(project).use { paths -> paths.map { path ->
            require(!Files.isSymbolicLink(path)) { "Linked media storage is unsupported" }
            path
        }.filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }.map { it.toFile() }.toList() }
    }

    /** Keep active catalog and undo files; removed bytes are backed up encrypted until commit. */
    fun stageCollection(root: File, project: Project, recoverable: List<Attachment>, files: ReversibleFiles) {
        val retained = (project.attachments + recoverable).map { localFile(root, project.id, it).toPath().toAbsolutePath().normalize() }.toSet()
        ownedFiles(root, project.id).filterNot { it.toPath() in retained }.forEach(files::remove)
    }

    fun pruneEmptyDirectories(root: File, projectId: String) {
        ownedFiles(root, projectId)
        val project = root.toPath().toAbsolutePath().normalize().resolve(projectId)
        if (!Files.exists(project, LinkOption.NOFOLLOW_LINKS)) return
        Files.walk(project).use { paths -> paths.filter { Files.isDirectory(it, LinkOption.NOFOLLOW_LINKS) }
            .sorted(Comparator.reverseOrder()).toList() }.forEach { path ->
            if (Files.newDirectoryStream(path).use { !it.iterator().hasNext() }) Files.delete(path)
        }
    }

    fun validateSize(bytes: Long, i18n: Messages = Messages()) {
        require(bytes in 0..PackageImportLimits().fileBytes) { i18n.text("package.importLimit") }
    }

    /** Enforces the file limit even when the provider omits or changes its size. */
    fun copyBounded(input: java.io.InputStream, output: java.io.OutputStream, i18n: Messages = Messages()) {
        try { PackageInput(input, PackageImportLimits().fileBytes).copyTo(output) }
        catch (e: PackageLimitExceeded) { throw IllegalArgumentException(i18n.text("package.importLimit"), e) }
    }

    fun validateCapacity(project: Project, locate: (Attachment) -> File?, i18n: Messages = Messages()) =
        MediaCapacity.validate(project, locate, i18n)

    fun safeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifBlank { "file" }

    fun entryName(attachment: Attachment): String =
        "${PackageManifest.ATTACHMENTS_DIR}${attachment.id}/${safeName(attachment.originalFileName)}"

    fun localFile(root: File, projectId: String, attachment: Attachment): File =
        File(root, "$projectId/${attachment.id}/${safeName(attachment.originalFileName)}")

    /** Existing attachment bytes by package path. */
    fun collect(project: Project, locate: (Attachment) -> File?): Map<String, ByteArray> =
        FilePayloadMap(project.attachments.mapNotNull { att ->
            locate(att)?.takeIf { it.isFile }?.let { entryName(att) to it }
        }.toMap())

    /** Attachments whose local file is missing. */
    fun missing(project: Project, locate: (Attachment) -> File?): List<Attachment> =
        project.attachments.filter { locate(it)?.isFile != true }

    /** Imported attachment bytes, including legacy paths. */
    fun bytesIn(pkg: ProjectPackage, attachment: Attachment): ByteArray? =
        pathIn(pkg, attachment)?.let { pkg.attachments[it] }

    /** Resolve aliases without loading payload bytes. */
    fun pathIn(pkg: ProjectPackage, attachment: Attachment): String? =
        pathIn(pkg.attachments, attachment)

    /** Only catalogued payloads travel; local recovery bytes stay local. */
    fun activePayloads(project: Project, payloads: Map<String, ByteArray>): Map<String, ByteArray> =
        PayloadViews.select(payloads, project.attachments.mapNotNull { pathIn(payloads, it) }.toSet())

    private fun pathIn(payloads: Map<String, ByteArray>, attachment: Attachment): String? =
        listOf(entryName(attachment), attachment.relativePath,
            "${PackageManifest.ATTACHMENTS_DIR}${attachment.relativePath.removePrefix("/")}")
            .firstOrNull { payloads.containsKey(it) }

    /** Writes imported files under [root]. */
    fun extract(pkg: ProjectPackage, root: File): Int {
        ReversibleFiles().use { files ->
            val count = stage(pkg, root, pkg.project, files)
            files.apply()
            val errors = files.commit()
            if (errors.isNotEmpty()) throw errors.first()
            return count
        }
    }

    fun stage(pkg: ProjectPackage, root: File, selected: Project, files: ReversibleFiles): Int {
        var count = 0
        for (att in selected.attachments) {
            if (att !in pkg.project.attachments) continue
            val path = pathIn(pkg, att) ?: continue
            files.replace(localFile(root, selected.id, att)) { pkg.writePayload(path, it) }
            count++
        }
        return count
    }
}
