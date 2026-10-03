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
    val isEncrypted: Boolean = false,
    val kdfSaltHex: String? = null,
    val kdfIterations: Int? = null,
    val cipherIvHex: String? = null,
    val checksums: Map<String, String> = emptyMap(), // relativePath -> SHA-256 Hex
    /** When true every attachment entry is stored as IV (12 bytes) + AES-256-GCM ciphertext. */
    val attachmentsEncrypted: Boolean = false,
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = "1.9"
        const val MANIFEST_FILE_NAME = "manifest.json"
        const val PROJECT_FILE_NAME = "project.json"
        const val PROJECT_ENC_FILE_NAME = "project.json.enc"
        const val ATTACHMENTS_DIR = "attachments/"
        const val DEFAULT_KDF_ITERATIONS = 100000
    }
}

data class ProjectPackage(
    val manifest: PackageManifest,
    val project: Project,
    val attachments: Map<String, ByteArray> = emptyMap(), // relativePath -> bytes
)
