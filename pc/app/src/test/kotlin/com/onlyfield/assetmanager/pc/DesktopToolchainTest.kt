package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.*
import com.onlyfield.assetmanager.exchange.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class DesktopToolchainTest {

    @Test
    fun testDesktopSharedCoreAndExchangeIntegration() {
        val now = System.currentTimeMillis()
        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Test Project Windows Desktop",
            createdEpochMs = now,
            updatedEpochMs = now,
            sites = listOf(
                Site(
                    id = UUID.randomUUID().toString(),
                    name = "BU Windows",
                    areas = listOf(
                        Area(
                            id = UUID.randomUUID().toString(),
                            name = "Area Desktop"
                        )
                    ),
                    devices = listOf(
                        Device(
                            id = UUID.randomUUID().toString(),
                            areaId = "area-1",
                            technicalName = "SW-CORE-01",
                            category = DeviceCategory.NETWORK_SWITCH
                        )
                    )
                )
            )
        )

        val result = ModelValidator.validateProject(project)
        assertNotNull("Expected validation result to be non-null", result)

        val bytes = PackageSerializer.exportPackage(project = project)
        assertNotNull("Serialized bytes should not be null", bytes)
        assertTrue("Serialized package should not be empty", bytes.isNotEmpty())

        val importResult = PackageSerializer.importPackage(zipBytes = bytes)
        val restored = importResult.pkg?.project
        assertNotNull("Imported project should not be null", restored)
        assertEquals(project.id, restored?.id)
        assertEquals(project.name, restored?.name)
        assertEquals(1, restored?.sites?.size)
    }

    @Test
    fun testDesktopStorageHelperPrintCheck() {
        // Must execute cleanly without unhandled exceptions on JVM/Windows environment
        val canCheckPrinter = try {
            DesktopStorageHelper.isPrinterAvailable()
            true
        } catch (_: Throwable) {
            false
        }
        assertTrue("Printer status check should complete cleanly", canCheckPrinter)
    }

    @Test
    fun testDesktopPackageExportAndImportOnFile() {
        val tempFile = File.createTempFile("pc_test_", ".ofam")
        try {
            val now = System.currentTimeMillis()
            val projId = UUID.randomUUID().toString()
            val project = Project(
                id = projId,
                name = "Project File Storage Test",
                createdEpochMs = now,
                updatedEpochMs = now
            )

            val bytes = PackageSerializer.exportPackage(project = project)
            tempFile.writeBytes(bytes)

            assertTrue(tempFile.exists())
            assertTrue(tempFile.length() > 0)

            val readBytes = tempFile.readBytes()
            val importResult = PackageSerializer.importPackage(zipBytes = readBytes)
            val restored = importResult.pkg?.project

            assertNotNull("Restored project should not be null", restored)
            assertEquals(projId, restored?.id)
            assertEquals("Project File Storage Test", restored?.name)
        } finally {
            tempFile.delete()
        }
    }
}
