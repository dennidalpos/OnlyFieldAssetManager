package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class ImportAtomicityTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun projectOrBaseFailureRollsBackRowsVerifierTrashAndExistingPayload() = runBlocking {
        for (table in listOf("projects", "sync_snapshots")) {
            val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
            try {
                val root = folder.newFolder()
                val repository = ProjectRepository(db, root, recoveryPassword = "dummy-recovery-key")
                val att = Attachment(name = "Photo", originalFileName = "photo.png", relativePath = "")
                val device = Device(technicalName = "Recoverable")
                val before = Project(name = "Previous", createdEpochMs = 0, updatedEpochMs = 0,
                    sites = listOf(Site(name = "Site", devices = listOf(device))), attachments = listOf(att))
                repository.saveProject(before)
                repository.setProjectPassword(before.id, null, "previous-password")
                repository.moveToTrash(before.id, "DEVICE", device.id)
                val local = repository.getProjectById(before.id)!!
                val trash = repository.getTrashItems(before.id)
                val file = AttachmentFiles.localFile(root, before.id, att).apply { requireNotNull(parentFile).mkdirs(); writeText("old bytes") }
                repository.exportProjectPackage(before.id, "previous-password")
                val base = repository.getSyncBase(before.id)
                val incoming = before.copy(name = "Incoming", isPasswordProtected = false)
                val result = repository.evaluateImportPackage(ByteArrayInputStream(PackageSerializer.exportPackage(incoming,
                    attachments = mapOf(AttachmentFiles.entryName(att) to "incoming bytes".toByteArray()))))
                db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_import BEFORE ${if (table == "projects") "UPDATE" else "INSERT"} ON $table BEGIN SELECT RAISE(ABORT, 'injected commit failure'); END")
                result.importResult.pkg!!.use { pkg ->
                    assertNotNull(runCatching { repository.importProjectPackage(pkg) }.exceptionOrNull())
                    assertEquals(local, repository.getProjectById(before.id))
                    assertEquals(base, repository.getSyncBase(before.id))
                    assertEquals(trash, repository.getTrashItems(before.id))
                    assertEquals("old bytes", file.readText())
                    assertTrue(repository.verifyProjectPassword(before.id, "previous-password"))
                    assertNotNull(runCatching { repository.importMergedPackage(pkg, incoming) }.exceptionOrNull())
                    assertEquals(local, repository.getProjectById(before.id))
                    assertEquals(base, repository.getSyncBase(before.id))
                    assertEquals("old bytes", file.readText())
                }
                db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_import")
                val retry = repository.evaluateImportPackage(ByteArrayInputStream(PackageSerializer.exportPackage(incoming,
                    attachments = mapOf(AttachmentFiles.entryName(att) to "incoming bytes".toByteArray()))))
                retry.importResult.pkg!!.use { assertTrue(repository.importProjectPackage(it)) }
                assertEquals(incoming, repository.getProjectById(before.id))
                assertEquals(incoming, repository.getSyncBase(before.id))
                assertEquals("incoming bytes", file.readText())
                assertEquals(trash, repository.getTrashItems(before.id))
            } finally { db.close() }
        }
    }
}
