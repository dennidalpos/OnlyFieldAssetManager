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
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class PackageImportResult(
    val pkg: ProjectPackage?,
    val validationResult: ValidationResult
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

    fun exportPackage(
        project: Project,
        attachments: Map<String, ByteArray> = emptyMap(),
        exportedEpochMs: Long = System.currentTimeMillis()
    ): ByteArray {
        val projectJsonBytes = jsonConfig.encodeToString(Project.serializer(), project).toByteArray(Charsets.UTF_8)
        val projectSha256 = calculateSha256(projectJsonBytes)

        val checksumsMap = mutableMapOf<String, String>()
        checksumsMap[PackageManifest.PROJECT_FILE_NAME] = projectSha256

        val processedAttachments = mutableMapOf<String, ByteArray>()
        for ((path, bytes) in attachments) {
            val normalizedPath = if (path.startsWith(PackageManifest.ATTACHMENTS_DIR)) path else "${PackageManifest.ATTACHMENTS_DIR}$path"
            checksumsMap[normalizedPath] = calculateSha256(bytes)
            processedAttachments[normalizedPath] = bytes
        }

        val manifest = PackageManifest(
            formatVersion = PackageManifest.CURRENT_FORMAT_VERSION,
            exportId = UUID.randomUUID().toString(),
            exportedEpochMs = exportedEpochMs,
            projectId = project.id,
            projectName = project.name,
            checksums = checksumsMap
        )

        val manifestJsonBytes = jsonConfig.encodeToString(PackageManifest.serializer(), manifest).toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Write manifest
            zos.putNextEntry(ZipEntry(PackageManifest.MANIFEST_FILE_NAME))
            zos.write(manifestJsonBytes)
            zos.closeEntry()

            // Write project JSON
            zos.putNextEntry(ZipEntry(PackageManifest.PROJECT_FILE_NAME))
            zos.write(projectJsonBytes)
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

    fun importPackage(zipBytes: ByteArray): PackageImportResult {
        val issues = mutableListOf<ValidationIssue>()

        if (zipBytes.isEmpty()) {
            issues.add(
                ValidationIssue(
                    code = "EMPTY_PACKAGE",
                    message = "Package ZIP file is empty",
                    severity = ValidationSeverity.STRUCTURAL_ERROR
                )
            )
            return PackageImportResult(null, ValidationResult(issues))
        }

        var manifestBytes: ByteArray? = null
        var projectBytes: ByteArray? = null
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
                            PackageManifest.PROJECT_FILE_NAME -> projectBytes = content
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

        if (projectBytes == null) {
            issues.add(
                ValidationIssue(
                    code = "MISSING_PROJECT_DATA",
                    message = "Package missing project.json",
                    severity = ValidationSeverity.STRUCTURAL_ERROR
                )
            )
            return PackageImportResult(null, ValidationResult(issues))
        }

        val manifest = try {
            jsonConfig.decodeFromString(PackageManifest.serializer(), String(manifestBytes!!, Charsets.UTF_8))
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

        // Validate SHA-256 Checksums
        val actualProjectChecksum = calculateSha256(projectBytes!!)
        val expectedProjectChecksum = manifest.checksums[PackageManifest.PROJECT_FILE_NAME]
        if (expectedProjectChecksum != null && expectedProjectChecksum != actualProjectChecksum) {
            issues.add(
                ValidationIssue(
                    code = "PROJECT_CHECKSUM_MISMATCH",
                    message = "SHA-256 mismatch for project.json. Expected: $expectedProjectChecksum, Actual: $actualProjectChecksum",
                    severity = ValidationSeverity.STRUCTURAL_ERROR
                )
            )
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
            jsonConfig.decodeFromString(Project.serializer(), String(projectBytes!!, Charsets.UTF_8))
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
