package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageSerializerTest {

    @Test
    fun testExportAndImportRoundTrip() {
        val siteId = UUID.randomUUID().toString()
        val dev = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "sw-core-01",
            ipAddress = "10.0.1.1",
            siteId = siteId
        )
        val site = Site(id = siteId, name = "Data Center 1")
        val bu = BusinessUnit(id = UUID.randomUUID().toString(), name = "HQ BU", sites = listOf(site), devices = listOf(dev))

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Export Test Project",
            createdEpochMs = 1700000000000L,
            updatedEpochMs = 1700000000000L,
            businessUnits = listOf(bu)
        )

        val attachmentBytes = "Sample Rack Diagram Content".toByteArray(Charsets.UTF_8)
        val attachments = mapOf("rack_diagram.png" to attachmentBytes)

        // 1. Export package
        val zipBytes = PackageSerializer.exportPackage(project, attachments)
        assertTrue(zipBytes.isNotEmpty())

        // 2. Import package
        val importResult = PackageSerializer.importPackage(zipBytes)
        assertTrue(importResult.validationResult.isValid)

        val importedPkg = importResult.pkg
        assertNotNull(importedPkg)
        assertEquals(project.id, importedPkg!!.project.id)
        assertEquals(project.name, importedPkg.project.name)
        assertEquals(1, importedPkg.project.businessUnits.size)

        // Verify attachment
        val importedAttachment = importedPkg.attachments["attachments/rack_diagram.png"]
        assertNotNull(importedAttachment)
        assertArrayEquals(attachmentBytes, importedAttachment)
    }

    @Test
    fun testCorruptedChecksumRejection() {
        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Checksum Test Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L
        )

        val projectJsonBytes = PackageSerializer.jsonConfig.encodeToString(Project.serializer(), project).toByteArray(Charsets.UTF_8)
        
        // Create manifest with a deliberately wrong checksum for project.json
        val manifest = PackageManifest(
            formatVersion = PackageManifest.CURRENT_FORMAT_VERSION,
            exportId = UUID.randomUUID().toString(),
            exportedEpochMs = 1000L,
            projectId = project.id,
            projectName = project.name,
            checksums = mapOf(PackageManifest.PROJECT_FILE_NAME to "0000000000000000000000000000000000000000000000000000000000000000")
        )
        val manifestJsonBytes = PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest).toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry(PackageManifest.MANIFEST_FILE_NAME))
            zos.write(manifestJsonBytes)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(PackageManifest.PROJECT_FILE_NAME))
            zos.write(projectJsonBytes)
            zos.closeEntry()
        }

        val corruptedZipBytes = baos.toByteArray()

        val importResult = PackageSerializer.importPackage(corruptedZipBytes)
        assertFalse(importResult.validationResult.isValid)
        assertNull(importResult.pkg)
        assertTrue(importResult.validationResult.issues.any {
            it.code == "PROJECT_CHECKSUM_MISMATCH" && it.severity == ValidationSeverity.STRUCTURAL_ERROR
        })
    }

    @Test
    fun testInvalidZipHandledGracefully() {
        val badBytes = "Not a zip file".toByteArray(Charsets.UTF_8)
        val importResult = PackageSerializer.importPackage(badBytes)

        assertFalse(importResult.validationResult.isValid)
        assertNull(importResult.pkg)
        assertTrue(importResult.validationResult.issues.any { 
            it.code == "INVALID_ZIP_ARCHIVE" || it.code == "MISSING_MANIFEST" 
        })
    }

    @Test
    fun testProjectComparisonEvaluator() {
        val projId = UUID.randomUUID().toString()
        val p1 = Project(
            id = projId,
            name = "Project Alpha",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L
        )

        val pkg1 = ProjectPackage(
            manifest = PackageManifest(exportId = "e1", exportedEpochMs = 1000L, projectId = projId, projectName = "Project Alpha"),
            project = p1
        )

        // 1. Comparison with no local project -> NEWER_REVISION
        val compNew = ProjectComparisonEvaluator.evaluate(null, null, pkg1)
        assertEquals(ComparisonStatus.NEWER_REVISION, compNew.status)

        // 2. Comparison with identical local project -> IDENTICAL
        val compIdentical = ProjectComparisonEvaluator.evaluate(p1, pkg1.manifest, pkg1)
        assertEquals(ComparisonStatus.IDENTICAL, compIdentical.status)

        // 3. Comparison with older local project -> NEWER_REVISION
        val p1Updated = p1.copy(updatedEpochMs = 2000L)
        val pkg1Newer = ProjectPackage(
            manifest = PackageManifest(exportId = "e2", exportedEpochMs = 2000L, projectId = projId, projectName = "Project Alpha"),
            project = p1Updated
        )
        val compNewer = ProjectComparisonEvaluator.evaluate(p1, pkg1.manifest, pkg1Newer)
        assertEquals(ComparisonStatus.NEWER_REVISION, compNewer.status)

        // 4. Comparison with newer local project -> OLDER_REVISION
        val compOlder = ProjectComparisonEvaluator.evaluate(p1Updated, pkg1Newer.manifest, pkg1)
        assertEquals(ComparisonStatus.OLDER_REVISION, compOlder.status)
        assertNotNull(compOlder.warningMessage)

        // 5. Comparison with different project ID -> DIFFERENT_PROJECT
        val p2 = Project(id = UUID.randomUUID().toString(), name = "Project Beta", createdEpochMs = 1000L, updatedEpochMs = 1000L)
        val pkg2 = ProjectPackage(
            manifest = PackageManifest(exportId = "e3", exportedEpochMs = 1000L, projectId = p2.id, projectName = p2.name),
            project = p2
        )
        val compDiff = ProjectComparisonEvaluator.evaluate(p1, pkg1.manifest, pkg2)
        assertEquals(ComparisonStatus.DIFFERENT_PROJECT, compDiff.status)
    }
}
