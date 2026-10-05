package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.exchange.PackageManifest
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class StoredMetadataTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun listReadsNamesAndProtectionWithoutLoadingProjectPayloads() {
        val storage = DesktopStorageManager(folder.newFolder())
        for (encrypted in listOf(false, true)) {
            val id = UUID.randomUUID().toString()
            val manifest = PackageManifest(exportId = UUID.randomUUID().toString(), exportedEpochMs = 0,
                projectId = id, projectName = "Project $encrypted", isEncrypted = encrypted)
            val file = File(storage.getProjectsFolder(), "$id.ofam")
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest).toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("project.json")); zip.write("invalid project payload".toByteArray()); zip.closeEntry()
            }
            val item = storage.listStoredProjects().single { it.id == id }
            assertEquals(manifest.projectName, item.name)
            assertEquals(encrypted, item.isEncrypted)
            assertNull(item.readError)
            assertNull(storage.importPackageFromFile(file).pkg)
        }
    }

    @Test fun unreadableArchivesRemainVisibleWithTheirError() {
        val storage = DesktopStorageManager(folder.newFolder())
        val file = File(storage.getProjectsFolder(), "broken.ofam").apply { writeText("broken") }
        val item = storage.listStoredProjects().single()
        assertEquals(file, item.file)
        assertNotNull(item.readError)
    }
}
