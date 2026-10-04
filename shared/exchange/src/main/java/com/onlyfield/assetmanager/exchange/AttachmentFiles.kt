package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import java.io.File

/** Shared attachment paths for packages and local storage. */
object AttachmentFiles {

    fun safeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifBlank { "file" }

    fun entryName(attachment: Attachment): String =
        "${PackageManifest.ATTACHMENTS_DIR}${attachment.id}/${safeName(attachment.originalFileName)}"

    fun localFile(root: File, projectId: String, attachment: Attachment): File =
        File(root, "$projectId/${attachment.id}/${safeName(attachment.originalFileName)}")

    /** Existing attachment bytes by package path. */
    fun collect(project: Project, locate: (Attachment) -> File?): Map<String, ByteArray> =
        project.attachments.mapNotNull { att ->
            locate(att)?.takeIf { it.isFile }?.let { entryName(att) to it.readBytes() }
        }.toMap()

    /** Attachments whose local file is missing. */
    fun missing(project: Project, locate: (Attachment) -> File?): List<Attachment> =
        project.attachments.filter { locate(it)?.isFile != true }

    /** Imported attachment bytes, including legacy paths. */
    fun bytesIn(pkg: ProjectPackage, attachment: Attachment): ByteArray? =
        pkg.attachments[entryName(attachment)]
            ?: pkg.attachments[attachment.relativePath]
            ?: pkg.attachments["${PackageManifest.ATTACHMENTS_DIR}${attachment.relativePath.removePrefix("/")}"]

    /** Writes imported files under [root]. */
    fun extract(pkg: ProjectPackage, root: File): Int {
        var written = 0
        for (att in pkg.project.attachments) {
            val bytes = bytesIn(pkg, att) ?: continue
            val target = localFile(root, pkg.project.id, att)
            target.parentFile?.mkdirs()
            target.writeBytes(bytes)
            written++
        }
        return written
    }
}
