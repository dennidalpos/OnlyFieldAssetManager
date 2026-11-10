package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MissingPayloadTest {
    private val first = Attachment(name = "First photo", originalFileName = "first.jpg", relativePath = "attachments/legacy.jpg")
    private val second = Attachment(name = "Second photo", originalFileName = "second.jpg", relativePath = "")
    private val project = Project(name = "Photos", createdEpochMs = 0, updatedEpochMs = 0, attachments = listOf(first, second))
    private val payload = "DUMMY-PHOTO".toByteArray()

    private fun withoutEntry(zip: ByteArray, path: String): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { target ->
            ZipInputStream(ByteArrayInputStream(zip)).use { source ->
                var entry = source.nextEntry
                while (entry != null) {
                    if (entry.name != path) {
                        target.putNextEntry(ZipEntry(entry.name))
                        source.copyTo(target)
                        target.closeEntry()
                    }
                    entry = source.nextEntry
                }
            }
        }
        return output.toByteArray()
    }

    @Test fun everyAbsentCatalogPayloadIsReportedWithoutBlockingImport() {
        val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(project))
        assertNotNull(result.pkg)
        val warnings = result.validationResult.issues.filter { it.code == "MISSING_ATTACHMENT_PAYLOAD" }
        assertEquals(setOf(first.id, second.id), warnings.map { it.targetEntityId }.toSet())
        assertTrue(warnings.all { it.severity == ValidationSeverity.DOCUMENTARY_WARNING })
        val pkg = requireNotNull(result.pkg)
        assertTrue(pkg.attachments.isEmpty())
        assertEquals(project.attachments, pkg.project.attachments)
    }

    @Test fun removedPayloadIsReportedOnceAndPresentPayloadRemainsAvailable() {
        val zip = PackageSerializer.exportPackage(project, project.attachments.associate { AttachmentFiles.entryName(it) to payload })
        val result = PackageSerializer.importPackage(withoutEntry(zip, AttachmentFiles.entryName(first)))
        assertTrue(result.validationResult.isValid)
        val warnings = result.validationResult.issues.filter { it.code.startsWith("MISSING_") }
        assertEquals(listOf(first.id), warnings.map { it.targetEntityId })
        val pkg = requireNotNull(result.pkg)
        assertNull(AttachmentFiles.bytesIn(pkg, first))
        assertArrayEquals(payload, AttachmentFiles.bytesIn(pkg, second))
    }

    @Test fun completeAndLegacyPayloadsProduceNoMissingWarnings() {
        for (firstPath in listOf(AttachmentFiles.entryName(first), first.relativePath)) {
            val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(project, mapOf(firstPath to payload, AttachmentFiles.entryName(second) to payload)))
            assertTrue(result.validationResult.isValid)
            assertFalse(result.validationResult.issues.any { it.code.startsWith("MISSING_") })
        }
    }

    @Test fun encryptedMissingPayloadIsReportedAfterUnlock() {
        val zip = PackageSerializer.exportPackage(project, project.attachments.associate { AttachmentFiles.entryName(it) to payload }, password = "password")
        val result = PackageSerializer.importPackage(withoutEntry(zip, AttachmentFiles.entryName(first)), password = "password")
        assertTrue(result.validationResult.isValid)
        assertEquals(listOf(first.id), result.validationResult.issues.filter { it.code == "MISSING_ATTACHMENT_PAYLOAD" }.map { it.targetEntityId })
        assertArrayEquals(payload, AttachmentFiles.bytesIn(result.pkg!!, second))
    }

    @Test fun missingManifestPayloadOutsideCatalogIsReported() {
        val path = "attachments/orphan.txt"
        val plainProject = project.copy(attachments = emptyList())
        val zip = PackageSerializer.exportPackage(plainProject, mapOf(path to payload))
        val result = PackageSerializer.importPackage(withoutEntry(zip, path))
        assertTrue(result.validationResult.isValid)
        assertEquals(listOf(path), result.validationResult.issues.filter { it.code == "MISSING_PACKAGE_ENTRY" }.map { it.targetEntityId })
    }
}
