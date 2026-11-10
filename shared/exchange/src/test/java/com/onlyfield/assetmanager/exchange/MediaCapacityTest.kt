package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile

class MediaCapacityTest {
    @get:Rule val folder = TemporaryFolder()
    private val attachment = Attachment(name = "Media", originalFileName = "photo.bin", relativePath = "")
    private val project = Project(name = "Città protetta", createdEpochMs = 0, updatedEpochMs = 0, isPasswordProtected = true, attachments = listOf(attachment))
    private fun file(bytes: Long) = folder.newFile().apply { RandomAccessFile(this, "rw").use { it.setLength(bytes) } }

    @Test fun oversizedFilesAndUnknownLengthStreamsStopBeforeRetention() {
        val source = file(33L * 1024 * 1024)
        assertTrue(runCatching { AttachmentFiles.validateCapacity(project, { source }) }.exceptionOrNull() is IllegalArgumentException)
        val output = ByteArrayOutputStream()
        val failure = runCatching { source.inputStream().use { AttachmentFiles.copyBounded(it, output) } }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(output.size() <= 32 * 1024 * 1024)
        assertTrue(runCatching { FilePayloadMap(mapOf("file" to source)).getValue("file") }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun protectedCapacityRejectsAggregateEntryAndArchiveLimitsWithoutAPassword() {
        val source = file(2048)
        for (limits in listOf(PackageImportLimits(entries = 2), PackageImportLimits(expandedBytes = 2000), PackageImportLimits(archiveBytes = 2000))) {
            assertTrue(runCatching { MediaCapacity.validate(project, { source }, Messages(), limits) }.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test fun acceptedProtectedAndPlainMediaExportAndReimportWithinTheSamePolicy() {
        val source = file(1024 * 1024)
        for (password in listOf(null, "dummy-password")) {
            val current = project.copy(isPasswordProtected = password != null)
            AttachmentFiles.validateCapacity(current, { source })
            val bytes = PackageSerializer.exportPackage(current, AttachmentFiles.collect(current) { source }, password)
            val result = PackageSerializer.importPackage(bytes, password)
            assertTrue(result.validationResult.isValid)
            result.pkg!!.use { assertEquals(1024 * 1024L, it.payloadSize(AttachmentFiles.entryName(attachment))) }
        }
    }

    @Test fun protectedPreflightKeepsAConservativeMarginNearTheArchiveLimit() {
        val source = file(1024 * 1024)
        val exported = PackageSerializer.exportPackage(project, AttachmentFiles.collect(project) { source }, "dummy-password")
        val limit = PackageImportLimits(archiveBytes = exported.size + 1L)
        assertTrue(exported.size < limit.archiveBytes)
        assertTrue(runCatching { MediaCapacity.validate(project, { source }, Messages(), limit) }.exceptionOrNull() is IllegalArgumentException)
    }
}
