package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.i18n.Messages
import java.io.File

/** Shared attachment paths for packages and local storage. */
object AttachmentFiles {

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
        listOf(entryName(attachment), attachment.relativePath,
            "${PackageManifest.ATTACHMENTS_DIR}${attachment.relativePath.removePrefix("/")}")
            .firstOrNull { pkg.attachments.containsKey(it) }

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
