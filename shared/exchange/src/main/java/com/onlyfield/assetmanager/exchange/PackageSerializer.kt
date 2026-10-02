package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.core.validation.ValidationIssue
import com.onlyfield.assetmanager.core.validation.ValidationResult
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class PackageImportResult(
    val pkg: ProjectPackage?,
    val validationResult: ValidationResult,
)

object PackageSerializer {

    val jsonConfig = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun calculateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int = PackageManifest.DEFAULT_KDF_ITERATIONS,
    ): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    private fun encryptAesGcm(plainTextBytes: ByteArray, key: SecretKeySpec, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        return cipher.doFinal(plainTextBytes)
    }

    private fun decryptAesGcm(cipherTextBytes: ByteArray, key: SecretKeySpec, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        return cipher.doFinal(cipherTextBytes)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        check((length % 2) == 0)
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun exportPackage(
        project: Project,
        attachments: Map<String, ByteArray> = emptyMap(),
        password: String? = null,
        exportedEpochMs: Long = System.currentTimeMillis(),
    ): ByteArray {
        val projectJsonBytes = jsonConfig.encodeToString(Project.serializer(), project).toByteArray(Charsets.UTF_8)

        val isEncrypted = !password.isNullOrBlank() || project.isPasswordProtected
        val effectivePassword = password?.takeIf { it.isNotBlank() }

        if (isEncrypted && (effectivePassword == null)) {
            throw IllegalArgumentException("Password must be supplied for password-protected project export")
        }

        val checksumsMap = mutableMapOf<String, String>()
        val processedAttachments = mutableMapOf<String, ByteArray>()
        for ((path, bytes) in attachments) {
            val normalizedPath = if (path.startsWith(PackageManifest.ATTACHMENTS_DIR)) path else "${PackageManifest.ATTACHMENTS_DIR}$path"
            checksumsMap[normalizedPath] = calculateSha256(bytes)
            processedAttachments[normalizedPath] = bytes
        }

        var kdfSaltHex: String? = null
        var kdfIterations: Int? = null
        var cipherIvHex: String? = null
        val payloadBytes: ByteArray
        val payloadEntryName: String

        if (isEncrypted && (effectivePassword != null)) {
            val random = SecureRandom()
            val salt = ByteArray(16).also { random.nextBytes(it) }
            val iv = ByteArray(12).also { random.nextBytes(it) }
            val iterations = PackageManifest.DEFAULT_KDF_ITERATIONS

            val key = deriveKey(effectivePassword, salt, iterations)
            payloadBytes = encryptAesGcm(projectJsonBytes, key, iv)
            payloadEntryName = PackageManifest.PROJECT_ENC_FILE_NAME

            kdfSaltHex = salt.toHex()
            kdfIterations = iterations
            cipherIvHex = iv.toHex()

            checksumsMap[PackageManifest.PROJECT_ENC_FILE_NAME] = calculateSha256(payloadBytes)
        } else {
            payloadBytes = projectJsonBytes
            payloadEntryName = PackageManifest.PROJECT_FILE_NAME
            checksumsMap[PackageManifest.PROJECT_FILE_NAME] = calculateSha256(payloadBytes)
        }

        val manifest = PackageManifest(
            formatVersion = PackageManifest.CURRENT_FORMAT_VERSION,
            exportId = UUID.randomUUID().toString(),
            exportedEpochMs = exportedEpochMs,
            projectId = project.id,
            projectName = project.name,
            isEncrypted = isEncrypted,
            kdfSaltHex = kdfSaltHex,
            kdfIterations = kdfIterations,
            cipherIvHex = cipherIvHex,
            checksums = checksumsMap,
        )

        val manifestJsonBytes = jsonConfig.encodeToString(PackageManifest.serializer(), manifest).toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Write manifest
            zos.putNextEntry(ZipEntry(PackageManifest.MANIFEST_FILE_NAME))
            zos.write(manifestJsonBytes)
            zos.closeEntry()

            // Write project JSON (or project.json.enc)
            zos.putNextEntry(ZipEntry(payloadEntryName))
            zos.write(payloadBytes)
            zos.closeEntry()

            // Write attachments
            for ((path, bytes) in processedAttachments) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(bytes)
                zos.closeEntry()
            }
        }

        return baos.toByteArray()
    }

    fun importPackage(zipBytes: ByteArray, password: String? = null): PackageImportResult {
        val issues = mutableListOf<ValidationIssue>()

        if (zipBytes.isEmpty()) {
            issues.add(
                ValidationIssue(
                    code = "EMPTY_PACKAGE",
                    message = "Package ZIP file is empty",
                    severity = ValidationSeverity.STRUCTURAL_ERROR,
                )
            )
            return PackageImportResult(null, ValidationResult(issues))
        }

        var manifestBytes: ByteArray? = null
        var projectPlainBytes: ByteArray? = null
        var projectEncBytes: ByteArray? = null
        val attachments = mutableMapOf<String, ByteArray>()

        try {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name
                        val content = zis.readBytes()
                        when (name) {
                            PackageManifest.MANIFEST_FILE_NAME -> manifestBytes = content
                            PackageManifest.PROJECT_FILE_NAME -> projectPlainBytes = content
                            PackageManifest.PROJECT_ENC_FILE_NAME -> projectEncBytes = content
                            else -> attachments[name] = content
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            issues.add(
                ValidationIssue(
                    code = "INVALID_ZIP_ARCHIVE",
                    message = "Failed to parse ZIP archive: ${e.message}",
                    severity = ValidationSeverity.STRUCTURAL_ERROR
                )
            )
            return PackageImportResult(null, ValidationResult(issues))
        }

        if (manifestBytes == null) {
            issues.add(
                ValidationIssue(
                    code = "MISSING_MANIFEST",
                    message = "Package missing manifest.json",
                    severity = ValidationSeverity.STRUCTURAL_ERROR
                )
            )
            return PackageImportResult(null, ValidationResult(issues))
        }

        val manifest = try {
            jsonConfig.decodeFromString(PackageManifest.serializer(), String(manifestBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            issues.add(
                ValidationIssue(
                    code = "INVALID_MANIFEST_JSON",
                    message = "Failed to parse manifest.json: ${e.message}",
                    severity = ValidationSeverity.STRUCTURAL_ERROR
                )
            )
            return PackageImportResult(null, ValidationResult(issues))
        }

        val projectBytes: ByteArray
        if (manifest.isEncrypted) {
            if (projectEncBytes == null) {
                issues.add(
                    ValidationIssue(
                        code = "MISSING_ENCRYPTED_PROJECT_DATA",
                        message = "Encrypted package missing project.json.enc",
                        severity = ValidationSeverity.STRUCTURAL_ERROR
                    )
                )
                return PackageImportResult(null, ValidationResult(issues))
            }

            // Verify SHA-256 Checksum of project.json.enc
            val actualEncChecksum = calculateSha256(projectEncBytes)
            val expectedEncChecksum = manifest.checksums[PackageManifest.PROJECT_ENC_FILE_NAME]
            if ((expectedEncChecksum != null) && (expectedEncChecksum != actualEncChecksum)) {
                issues.add(
                    ValidationIssue(
                        code = "PROJECT_CHECKSUM_MISMATCH",
                        message = "SHA-256 mismatch for project.json.enc. Expected: $expectedEncChecksum, Actual: $actualEncChecksum",
                        severity = ValidationSeverity.STRUCTURAL_ERROR
                    )
                )
                return PackageImportResult(null, ValidationResult(issues))
            }

            if (password.isNullOrBlank()) {
                issues.add(
                    ValidationIssue(
                        code = "PASSWORD_REQUIRED",
                        message = "Il pacchetto è protetto da password. Inserire la password per importare.",
                        severity = ValidationSeverity.STRUCTURAL_ERROR
                    )
                )
                return PackageImportResult(null, ValidationResult(issues))
            }

            val saltHex = manifest.kdfSaltHex
            val ivHex = manifest.cipherIvHex
            val iterations = manifest.kdfIterations ?: PackageManifest.DEFAULT_KDF_ITERATIONS

            if ((saltHex == null) || (ivHex == null)) {
                issues.add(
                    ValidationIssue(
                        code = "CORRUPTED_ENCRYPTION_METADATA",
                        message = "Encrypted package manifest missing salt or IV",
                        severity = ValidationSeverity.STRUCTURAL_ERROR
                    )
                )
                return PackageImportResult(null, ValidationResult(issues))
            }

            try {
                val key = deriveKey(password, saltHex.hexToBytes(), iterations)
                projectBytes = decryptAesGcm(projectEncBytes, key, ivHex.hexToBytes())
            } catch (_: Exception) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_PACKAGE_PASSWORD",
                        message = "Password errata o pacchetto protetto manomesso/troncato.",
                        severity = ValidationSeverity.STRUCTURAL_ERROR
                    )
                )
                return PackageImportResult(null, ValidationResult(issues))
            }
        } else {
            if (projectPlainBytes == null) {
                issues.add(
                    ValidationIssue(
                        code = "MISSING_PROJECT_DATA",
                        message = "Package missing project.json",
                        severity = ValidationSeverity.STRUCTURAL_ERROR
                    )
                )
                return PackageImportResult(null, ValidationResult(issues))
            }

            // Verify SHA-256 Checksum of project.json
            val actualProjectChecksum = calculateSha256(projectPlainBytes)
            val expectedProjectChecksum = manifest.checksums[PackageManifest.PROJECT_FILE_NAME]
            if ((expectedProjectChecksum != null) && (expectedProjectChecksum != actualProjectChecksum)) {
                issues.add(
                    ValidationIssue(
                        code = "PROJECT_CHECKSUM_MISMATCH",
                        message = "SHA-256 mismatch for project.json. Expected: $expectedProjectChecksum, Actual: $actualProjectChecksum",
                        severity = ValidationSeverity.STRUCTURAL_ERROR
                    )
                )
            }
            projectBytes = projectPlainBytes
        }

        for ((attPath, attBytes) in attachments) {
            val expectedChecksum = manifest.checksums[attPath]
            if (expectedChecksum != null) {
                val actualChecksum = calculateSha256(attBytes)
                if (actualChecksum != expectedChecksum) {
                    issues.add(
                        ValidationIssue(
                            code = "ATTACHMENT_CHECKSUM_MISMATCH",
                            message = "SHA-256 mismatch for attachment $attPath. Expected: $expectedChecksum, Actual: $actualChecksum",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = attPath
                        )
                    )
                }
            }
        }

        val project = try {
            jsonConfig.decodeFromString(Project.serializer(), String(projectBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            issues.add(
                ValidationIssue(
                    code = "INVALID_PROJECT_JSON",
                    message = "Failed to parse project.json: ${e.message}",
                    severity = ValidationSeverity.STRUCTURAL_ERROR
                )
            )
            return PackageImportResult(null, ValidationResult(issues))
        }

        // Validate model constraints and relationships
        val modelValidation = ModelValidator.validateProject(project)
        issues.addAll(modelValidation.issues)

        val finalValidation = ValidationResult(issues)
        val pkg = if (finalValidation.isValid) {
            ProjectPackage(manifest = manifest, project = project, attachments = attachments)
        } else {
            null
        }

        return PackageImportResult(pkg, finalValidation)
    }
}
