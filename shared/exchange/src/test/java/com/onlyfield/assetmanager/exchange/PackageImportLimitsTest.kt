package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Project
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class PackageImportLimitsTest {
    private val project = Project(name = "Bounded import", createdEpochMs = 0, updatedEpochMs = 0)
    private fun zip(entries: Map<String, ByteArray>) = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip -> entries.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() } }
    }.toByteArray()
    private fun entries(bytes: ByteArray): Map<String, ByteArray> = buildMap {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) { put(entry.name, zip.readBytes()); entry = zip.nextEntry }
        }
    }
    private fun reject(bytes: ByteArray, limits: PackageImportLimits, code: String) {
        val result = PackageSerializer.importPackage(ByteArrayInputStream(bytes), null, Messages(), limits)
        assertNull(result.pkg)
        assertEquals(listOf(code), result.validationResult.issues.map { it.code })
    }

    @Test fun enforcesCompressedExpandedEntryAndCountLimitsDuringReading() {
        val bytes = zip(mapOf("a" to ByteArray(1024), "b" to ByteArray(1024)))
        reject(bytes, PackageImportLimits(archiveBytes = bytes.size.toLong() - 1), "PACKAGE_SIZE_LIMIT_EXCEEDED")
        reject(bytes, PackageImportLimits(expandedBytes = 2047), "PACKAGE_CONTENT_LIMIT_EXCEEDED")
        reject(bytes, PackageImportLimits(fileBytes = 512), "ENTRY_SIZE_LIMIT_EXCEEDED")
        reject(bytes, PackageImportLimits(entries = 1), "PACKAGE_ENTRY_LIMIT_EXCEEDED")
    }

    @Test fun directoriesAndTrailingBytesCannotBypassLimits() {
        val dirs = zip((0..2).associate { "dir$it/" to byteArrayOf() })
        reject(dirs, PackageImportLimits(entries = 2), "PACKAGE_ENTRY_LIMIT_EXCEEDED")
        val valid = PackageSerializer.exportPackage(project)
        reject(valid + ByteArray(1000), PackageImportLimits(archiveBytes = valid.size.toLong() + 500), "PACKAGE_SIZE_LIMIT_EXCEEDED")
    }

    @Test fun malformedUtf8EntryNameIsRejectedAsStructuralError() {
        val bytes = zip(mapOf("a" to byteArrayOf(1)))
        bytes[30] = 0xff.toByte()
        reject(bytes, PackageImportLimits(), "INVALID_ZIP_ARCHIVE")
    }

    @Test fun realPolicyRejectsHighlyCompressedFileAbove32MiB() {
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("attachments/bomb"))
                val chunk = ByteArray(8192)
                repeat(32 * 1024 * 1024 / chunk.size + 1) { zip.write(chunk) }
            }
        }.toByteArray()
        reject(bytes, PackageImportLimits(), "ENTRY_SIZE_LIMIT_EXCEEDED")
    }

    @Test fun exactValidBoundariesAndEncryptedPayloadsRoundTrip() {
        for (password in listOf(null, "dummy-password")) {
            val bytes = PackageSerializer.exportPackage(project, mapOf("attachments/file" to ByteArray(512)), password)
            val expanded = entries(bytes).values.sumOf { it.size.toLong() }
            val result = PackageSerializer.importPackage(ByteArrayInputStream(bytes), password, Messages(),
                PackageImportLimits(archiveBytes = bytes.size.toLong(), fileBytes = 4096, expandedBytes = expanded, entries = entries(bytes).size))
            assertNotNull(result.pkg)
            assertArrayEquals(ByteArray(512), result.pkg!!.attachments["attachments/file"])
        }
    }

    @Test fun rejectsKdfAndMalformedMetadataBeforeRequestingOrDerivingPassword() {
        val original = entries(PackageSerializer.exportPackage(project, password = "dummy-password"))
        val manifest = PackageSerializer.jsonConfig.decodeFromString(PackageManifest.serializer(), original.getValue("manifest.json").toString(Charsets.UTF_8))
        val variants = listOf(manifest.copy(kdfIterations = 0), manifest.copy(kdfIterations = -1),
            manifest.copy(kdfIterations = Int.MAX_VALUE), manifest.copy(kdfSaltHex = "00"), manifest.copy(cipherIvHex = "z".repeat(24)))
        for (variant in variants) {
            val changed = zip(original + ("manifest.json" to PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), variant).toByteArray()))
            val result = PackageSerializer.importPackage(changed, "wrong-password")
            assertNull(result.pkg)
            assertEquals(if (variant.kdfIterations !in 1..PackageImportLimits.MAX_KDF_ITERATIONS) "INVALID_KDF_PARAMETERS" else "CORRUPTED_ENCRYPTION_METADATA", result.validationResult.issues.single().code)
        }
    }

    @Test fun compressedStreamStopsAtFirstExcessByte() {
        val stream = object : InputStream() {
            var consumed = 0
            override fun read(): Int { consumed++; return 0 }
            override fun read(b: ByteArray, off: Int, len: Int): Int { consumed += len; b.fill(0, off, off + len); return len }
        }
        val counted = PackageInput(stream, 16)
        val result = runCatching { while (counted.read(ByteArray(8192)) >= 0) { } }
        assertEquals("PACKAGE_SIZE_LIMIT_EXCEEDED", (result.exceptionOrNull() as PackageLimitExceeded).code)
        assertEquals(17, stream.consumed)
    }
}
