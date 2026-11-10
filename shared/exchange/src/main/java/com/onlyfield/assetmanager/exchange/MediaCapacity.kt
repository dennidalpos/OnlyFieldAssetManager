package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import java.io.File
import java.io.OutputStream
import java.util.UUID

/** Capacity check without retaining a local project password. */
internal object MediaCapacity {
    fun validate(project: Project, locate: (Attachment) -> File?, i18n: Messages,
        limits: PackageImportLimits = PackageImportLimits()) {
        val files = project.attachments.mapNotNull { att -> locate(att)?.takeIf { it.isFile }?.let { AttachmentFiles.entryName(att) to it } }.toMap()
        require(files.size + 2 <= limits.entries && files.values.all { it.length() in 0..limits.fileBytes }) { i18n.text("package.importLimit") }
        val json = PackageSerializer.jsonConfig.encodeToString(Project.serializer(), project).toByteArray(Charsets.UTF_8)
        require(json.size <= limits.fileBytes && json.size + files.values.sumOf { it.length() } <= limits.expandedBytes) { i18n.text("package.importLimit") }
        if (!project.isPasswordProtected) {
            // The native exporter checks JSON, expanded content and the actual compressed archive.
            PackageSerializer.exportPackageToStream(OutputStream.nullOutputStream(), project, FilePayloadMap(files), i18n = i18n)
            return
        }
        val projectEntry = PackageManifest.PROJECT_ENC_FILE_NAME
        // Fixed-width fields budget the manifest; this metadata is never exported or used for encryption.
        val manifest = PackageManifest(exportId = UUID.randomUUID().toString(), exportedEpochMs = System.currentTimeMillis(),
            projectId = project.id, projectName = project.name, isEncrypted = true,
            kdfSaltHex = "0".repeat(32), kdfIterations = PackageManifest.DEFAULT_KDF_ITERATIONS, cipherIvHex = "0".repeat(24),
            checksums = (files.keys + projectEntry).associateWith { "0".repeat(64) }, attachmentsEncrypted = files.isNotEmpty())
        val manifestSize = PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest).toByteArray(Charsets.UTF_8).size.toLong()
        val sizes = files.mapValues { it.value.length() + PackageImportLimits.GCM_OVERHEAD } +
            mapOf(projectEntry to json.size + 16L, PackageManifest.MANIFEST_FILE_NAME to manifestSize)
        require(manifestSize <= limits.fileBytes && sizes.values.sum() <= limits.expandedBytes) { i18n.text("package.importLimit") }
        // zlib compressBound plus ZIP headers/descriptors; encrypted data cannot rely on plaintext compressibility.
        val archiveBound = 22L + sizes.entries.sumOf { (name, size) ->
            size + (size shr 12) + (size shr 14) + (size shr 25) + 13 + 92 + 2L * name.toByteArray(Charsets.UTF_8).size
        }
        require(archiveBound <= limits.archiveBytes) { i18n.text("package.importLimit") }
    }
}
