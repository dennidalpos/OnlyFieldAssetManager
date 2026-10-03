package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import java.io.File

/**
 * Shared convention for attachment files, used by both apps.
 *
 * - Inside a `.ofam` package the file of an attachment is stored at [entryName].
 * - On disk each app keeps it at `<root>/<projectId>/<attachmentId>/<file name>` ([localFile]),
 *   where `root` is the app's attachment folder.
 */
object AttachmentFiles {

    fun safeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifBlank { "file" }

    fun entryName(attachment: Attachment): String =
        "${PackageManifest.ATTACHMENTS_DIR}${attachment.id}/${safeName(attachment.originalFileName)}"

    fun localFile(root: File, projectId: String, attachment: Attachment): File =
        File(root, "$projectId/${attachment.id}/${safeName(attachment.originalFileName)}")

    /** Bytes of every attachment whose file exists, keyed by package entry name. */
    fun collect(project: Project, locate: (Attachment) -> File?): Map<String, ByteArray> =
        project.attachments.mapNotNull { att ->
            locate(att)?.takeIf { it.isFile }?.let { entryName(att) to it.readBytes() }
        }.toMap()

    /** Attachments of [project] whose file could not be found, e.g. to warn before exporting. */
    fun missing(project: Project, locate: (Attachment) -> File?): List<Attachment> =
        project.attachments.filter { locate(it)?.isFile != true }

    /** File content of [attachment] inside an imported package, also accepting older path conventions. */
    fun bytesIn(pkg: ProjectPackage, attachment: Attachment): ByteArray? =
        pkg.attachments[entryName(attachment)]
            ?: pkg.attachments[attachment.relativePath]
            ?: pkg.attachments["${PackageManifest.ATTACHMENTS_DIR}${attachment.relativePath.removePrefix("/")}"]

    /** Writes the files of an imported package under [root]; returns how many were written. */
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
