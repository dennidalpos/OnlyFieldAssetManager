package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Project
import kotlinx.serialization.Serializable

@Serializable
data class PackageManifest(
    val formatVersion: String = CURRENT_FORMAT_VERSION,
    val exportId: String,
    val exportedEpochMs: Long,
    val projectId: String,
    val projectName: String,
    val checksums: Map<String, String> = emptyMap() // relativePath -> SHA-256 Hex
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = "1.0"
        const val MANIFEST_FILE_NAME = "manifest.json"
        const val PROJECT_FILE_NAME = "project.json"
        const val ATTACHMENTS_DIR = "attachments/"
    }
}

data class ProjectPackage(
    val manifest: PackageManifest,
    val project: Project,
    val attachments: Map<String, ByteArray> = emptyMap() // relativePath -> bytes
)
