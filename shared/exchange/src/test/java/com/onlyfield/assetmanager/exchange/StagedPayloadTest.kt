package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.AttachmentType
import com.onlyfield.assetmanager.core.model.Project
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.InterruptedIOException
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class StagedPayloadTest {
    @get:Rule val folder = TemporaryFolder()
    private val attachment = Attachment(name = "Large document", originalFileName = "sample.bin",
        fileType = AttachmentType.OTHER, relativePath = "sample.bin")
    private val project = Project(name = "Staging", createdEpochMs = 0, updatedEpochMs = 0, attachments = listOf(attachment))
    private val payload = ByteArray(2 * 1024 * 1024).also { java.util.Random(42).nextBytes(it) }
    private val path = AttachmentFiles.entryName(attachment)
    private fun archive(password: String? = null) = PackageSerializer.exportPackage(project, mapOf(path to payload), password)

    private fun rewrite(bytes: ByteArray, update: (MutableMap<String, ByteArray>) -> Unit): ByteArray {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) { entries[entry.name] = zip.readBytes(); entry = zip.nextEntry }
        }
        update(entries)
        return ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip -> entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(content); zip.closeEntry()
            } }
        }.toByteArray()
    }

    @Test fun clearAndProtectedPayloadsUseEncryptedStagingAndReleaseAfterConsumption() {
        for (password in listOf(null, "dummy-password")) {
            val staging = folder.newFolder()
            val result = PackageSerializer.importPackage(archive(password).inputStream(), password, stagingDirectory = staging)
            val pkg = checkNotNull(result.pkg)
            assertTrue(result.validationResult.issues.isEmpty())
            assertEquals(payload.size.toLong(), pkg.payloadSize(path))
            val scratch = staging.walkTopDown().filter { it.isFile }.toList()
            assertEquals(1, scratch.size)
            assertFalse(scratch.single().readBytes().take(1024) == payload.take(1024))
            val extracted = folder.newFolder()
            pkg.use {
                assertArrayEquals(payload, AttachmentFiles.bytesIn(it, attachment))
                assertEquals(1, AttachmentFiles.extract(it, extracted))
                assertArrayEquals(payload, AttachmentFiles.localFile(extracted, project.id, attachment).readBytes())
            }
            assertTrue(staging.listFiles()!!.isEmpty())
            assertThrows(IllegalStateException::class.java) { pkg.attachments[path] }
        }
    }

    @Test fun invalidManifestReleasesPreviouslyStagedPayloads() {
        val changed = rewrite(archive()) { entries ->
            val manifest = PackageSerializer.jsonConfig.decodeFromString(PackageManifest.serializer(), entries.getValue("manifest.json").toString(Charsets.UTF_8))
            entries["manifest.json"] = PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest.copy(formatVersion = "unsupported")).toByteArray()
        }
        val staging = folder.newFolder()
        val result = PackageSerializer.importPackage(changed.inputStream(), stagingDirectory = staging)
        assertNull(result.pkg)
        assertEquals("UNSUPPORTED_FORMAT_VERSION", result.validationResult.issues.single().code)
        assertTrue(staging.listFiles()!!.isEmpty())
    }

    @Test fun tamperedGcmTagIsReportedAndDoesNotLeavePayloadFiles() {
        val changed = rewrite(archive("dummy-password")) { entries ->
            val encrypted = entries.getValue(path)
            encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
            val manifest = PackageSerializer.jsonConfig.decodeFromString(PackageManifest.serializer(), entries.getValue("manifest.json").toString(Charsets.UTF_8))
            entries["manifest.json"] = PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(),
                manifest.copy(checksums = manifest.checksums + (path to PackageSerializer.calculateSha256(encrypted)))).toByteArray()
        }
        val staging = folder.newFolder()
        val result = PackageSerializer.importPackage(changed.inputStream(), "dummy-password", stagingDirectory = staging)
        checkNotNull(result.pkg).use {
            assertTrue(result.validationResult.issues.any { issue -> issue.code == "ATTACHMENT_DECRYPTION_FAILED" })
            assertNull(AttachmentFiles.pathIn(it, attachment))
            assertTrue(staging.walkTopDown().none { file -> file.isFile })
        }
        assertTrue(staging.listFiles()!!.isEmpty())
    }

    @Test fun interruptionPropagatesAndReleasesStaging() {
        val staging = folder.newFolder()
        val input = object : java.io.ByteArrayInputStream(archive()) {
            var consumed = 0
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                if (consumed > 1536 * 1024) throw InterruptedIOException("Cancellation during payload staging")
                return super.read(bytes, offset, length).also { consumed += maxOf(0, it) }
            }
        }
        assertThrows(InterruptedIOException::class.java) { PackageSerializer.importPackage(input, stagingDirectory = staging) }
        assertTrue(staging.listFiles()!!.isEmpty())
    }

    @Test fun corruptScratchDoesNotReplaceExistingAttachmentOrHideAuthenticationFailure() {
        val staging = folder.newFolder()
        checkNotNull(PackageSerializer.importPackage(archive().inputStream(), stagingDirectory = staging).pkg).use { pkg ->
            val scratch = staging.walkTopDown().single { it.isFile }
            RandomAccessFile(scratch, "rw").use { file ->
                file.seek(file.length() - 1)
                val last = file.readByte()
                file.seek(file.length() - 1)
                file.writeByte(last.toInt() xor 1)
            }
            val root = folder.newFolder()
            val target = AttachmentFiles.localFile(root, project.id, attachment)
            assertTrue(target.parentFile.mkdirs())
            target.writeText("existing")
            assertThrows(javax.crypto.AEADBadTagException::class.java) { AttachmentFiles.extract(pkg, root) }
            assertEquals("existing", target.readText())
            assertTrue(root.walkTopDown().none { it.extension == "tmp" })
        }
        assertTrue(staging.listFiles()!!.isEmpty())
    }
}
