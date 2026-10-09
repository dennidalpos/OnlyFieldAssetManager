package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.util.zip.*

class PackageMetadataTest {
    @get:Rule val folder = TemporaryFolder()
    private val project = Project(name = "Synthetic site", createdEpochMs = 0, updatedEpochMs = 0)
    private val json = PackageSerializer.jsonConfig
    private fun entries(bytes: ByteArray): LinkedHashMap<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) { entries[entry.name] = zip.readBytes(); entry = zip.nextEntry }
        }
        return entries
    }
    private fun zip(entries: Map<String, ByteArray>) = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip -> entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
        } }
    }.toByteArray()
    private fun reject(bytes: ByteArray, code: String, password: String? = null) {
        val staging = folder.newFolder()
        val result = PackageSerializer.importPackage(bytes.inputStream(), password, stagingDirectory = staging)
        assertNull(result.pkg)
        assertEquals(code, result.validationResult.issues.single().code)
        assertEquals(ValidationSeverity.STRUCTURAL_ERROR, result.validationResult.issues.single().severity)
        assertTrue(staging.listFiles()!!.isEmpty())
    }
    private fun archive(password: String? = null) = PackageSerializer.exportPackage(project, mapOf("staged.bin" to byteArrayOf(1, 2)), password)

    @Test fun duplicateManifestProjectAndMediaEntriesReleaseStaging() {
        for (name in listOf("manifest.json", "project.json", "attachments/staged.bin")) {
            val entries = entries(archive())
            val alias = name.dropLast(1) + "x"
            entries[alias] = entries.getValue(name)
            val duplicate = zip(entries).toString(Charsets.ISO_8859_1).replace(alias, name).toByteArray(Charsets.ISO_8859_1)
            reject(duplicate, "DUPLICATE_PACKAGE_ENTRY")
        }
    }
    @Test fun incoherentManifestFlagsAndIdentityAreRejected() {
        for (case in listOf("id", "attachments", "cipher")) {
            val entries = entries(archive())
            val manifest = json.decodeFromString(PackageManifest.serializer(), entries.getValue("manifest.json").toString(Charsets.UTF_8))
            val changed = when (case) {
                "id" -> manifest.copy(projectId = "foreign-project")
                "attachments" -> manifest.copy(attachmentsEncrypted = true)
                else -> manifest.copy(cipherIvHex = "000000000000000000000000")
            }
            entries["manifest.json"] = json.encodeToString(PackageManifest.serializer(), changed).toByteArray()
            reject(zip(entries), if (case == "id") "PACKAGE_PROJECT_ID_MISMATCH" else "INCONSISTENT_PACKAGE_PROTECTION")
        }
    }
    @Test fun plainProtectedProjectAndConflictingPayloadsAreRejected() {
        val plain = entries(archive())
        plain["project.json"] = json.encodeToString(Project.serializer(), project.copy(isPasswordProtected = true)).toByteArray()
        val manifest = json.decodeFromString(PackageManifest.serializer(), plain.getValue("manifest.json").toString(Charsets.UTF_8))
        plain["manifest.json"] = json.encodeToString(PackageManifest.serializer(), manifest.copy(checksums = manifest.checksums +
            ("project.json" to PackageSerializer.calculateSha256(plain.getValue("project.json"))))).toByteArray()
        reject(zip(plain), "INCONSISTENT_PACKAGE_PROTECTION")
        for (password in listOf(null, "dummy-password")) {
            val conflict = entries(archive(password))
            conflict[if (password == null) "project.json.enc" else "project.json"] = byteArrayOf(1)
            reject(zip(conflict), "INCONSISTENT_PACKAGE_PROTECTION", password)
        }
    }
    @Test fun plainAndProtectedValidPackagesStillOpen() {
        for (password in listOf(null, "dummy-password")) {
            val result = PackageSerializer.importPackage(archive(password), password)
            assertTrue(result.validationResult.isValid)
            result.pkg!!.use { assertEquals(project, it.project); assertEquals(1, it.attachments.size) }
        }
    }
}
