package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.*
import com.onlyfield.assetmanager.exchange.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class CrashRecoveryTest {
    @get:Rule val folder = TemporaryFolder()
    private fun database(path: String): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val isolated = object : android.content.ContextWrapper(context) {
            override fun getApplicationContext(): android.content.Context = this
            override fun getDatabasePath(name: String): java.io.File = java.io.File(path)
        }
        return Room.databaseBuilder(isolated, AppDatabase::class.java, "project.db").allowMainThreadQueries().build()
    }

    @Test fun coldRepositoryRecoversTheRealRoomCommitOutcomeVerifierBaseTrashAndMedia() = runBlocking {
        for (commit in listOf(false, true)) for (deletion in listOf(false, true)) {
            val name = folder.newFolder().resolve("project.db").absolutePath
            val root = folder.newFolder()
            var db = database(name)
            val att = Attachment(name = "Photo", originalFileName = "photo.bin", relativePath = "")
            val device = Device(technicalName = "Recoverable")
            val project = Project(name = "Previous", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(Site(name = "Site", devices = listOf(device))), attachments = listOf(att))
            val repository = ProjectRepository(db, root, "dummy-recovery-key")
            repository.saveProject(project)
            repository.setProjectPassword(project.id, null, "old-password")
            repository.moveToTrash(project.id, "DEVICE", device.id)
            val file = AttachmentFiles.localFile(root, project.id, att).apply { requireNotNull(parentFile).mkdirs(); writeText("old bytes") }
            repository.exportProjectPackage(project.id, "old-password")
            val before = repository.getProjectById(project.id)!!
            val beforeBase = repository.getSyncBase(project.id)
            val beforeTrash = repository.getTrashItems(project.id)
            val storage = ProjectStore(db)
            val recovery = ProjectRecovery(db, root, "dummy-recovery-key")
            val files = recovery.files(project.id)
            if (deletion) files.remove(file) else files.replace(file) { it.write("incoming bytes".toByteArray()) }
            val incoming = before.copy(name = "Incoming", isPasswordProtected = false)
            val outcome = runCatching {
                db.withTransaction {
                    if (deletion) { db.projectDao().deleteSyncSnapshot(project.id); db.projectDao().deleteProjectById(project.id) }
                    else { storage.saveImported(incoming, null); storage.saveBase(incoming) }
                    recovery.expectState(files, project.id)
                    files.apply()
                    if (!commit) throw IOException("Interrupt before Room commit")
                }
            }
            assertEquals(commit, outcome.isSuccess)
            // Leave the journal open as a process interruption would, then reopen persistent Room.
            db.close()
            db = database(name)
            try {
                val restarted = ProjectRepository(db, root, "dummy-recovery-key")
                assertTrue(restarted.recoverAll().isEmpty())
                if (commit && deletion) {
                    assertNull(restarted.getProjectById(project.id)); assertNull(restarted.getSyncBase(project.id)); assertFalse(file.exists())
                } else if (commit) {
                    assertEquals(incoming, restarted.getProjectById(project.id))
                    assertEquals(incoming, restarted.getSyncBase(project.id))
                    assertEquals(beforeTrash, restarted.getTrashItems(project.id))
                    assertEquals("incoming bytes", file.readText())
                } else {
                    assertEquals(before, restarted.getProjectById(project.id)); assertEquals(beforeBase, restarted.getSyncBase(project.id))
                    assertEquals(beforeTrash, restarted.getTrashItems(project.id)); assertEquals("old bytes", file.readText())
                    assertTrue(restarted.verifyProjectPassword(project.id, "old-password"))
                }
                assertTrue(FileRecovery.owners(root).isEmpty())
                assertTrue(restarted.recoverAll().isEmpty())
            } finally { db.close() }
        }
    }

    @Test fun secondRollbackFailureIsVisibleBlocksProjectAndCanBeRetried() = runBlocking {
        val db = database(folder.newFolder().resolve("project.db").absolutePath)
        try {
            val root = folder.newFolder()
            val project = Project(name = "Previous", createdEpochMs = 0, updatedEpochMs = 0)
            val repository = ProjectRepository(db, root, "dummy-recovery-key")
            repository.saveProject(project)
            val file = java.io.File(root, "${project.id}/photo").apply { requireNotNull(parentFile).mkdirs(); writeText("old bytes") }
            val files = ProjectRecovery(db, root, "dummy-recovery-key").files(project.id)
            files.replace(file) { it.write("incoming bytes".toByteArray()) }
            files.apply()
            file.writeText("external edit")
            assertNotNull(runCatching { files.close() }.exceptionOrNull())
            val restarted = ProjectRepository(db, root, "dummy-recovery-key")
            assertEquals(1, restarted.recoverAll().size)
            assertNotNull(runCatching { restarted.getProjectById(project.id) }.exceptionOrNull())
            assertNotNull(runCatching { restarted.saveProject(project.copy(name = "Overwrite")) }.exceptionOrNull())
            assertEquals("external edit", file.readText())
            assertEquals(project, ProjectStore(db).load(project.id))
            file.writeText("incoming bytes")
            assertTrue(restarted.recoverAll().isEmpty())
            assertEquals("old bytes", file.readText())
        } finally { db.close() }
    }

    @Test fun identicalCatalogAndBaseStillDistinguishCommittedImportFromRollback() = runBlocking {
        for (commit in listOf(false, true)) {
            val path = folder.newFolder().resolve("project.db").absolutePath
            var db = database(path)
            val root = folder.newFolder()
            val project = Project(name = "Identical", createdEpochMs = 0, updatedEpochMs = 0)
            val store = ProjectStore(db)
            store.save(project); store.saveBase(project)
            val base = db.projectDao().getSyncSnapshot(project.id)!!
            val file = java.io.File(root, "${project.id}/photo").apply { requireNotNull(parentFile).mkdirs(); writeText("old bytes") }
            val recovery = ProjectRecovery(db, root, "dummy-recovery-key")
            val before = recovery.fingerprint(project.id)
            val files = recovery.files(project.id)
            files.replace(file) { it.write("incoming bytes".toByteArray()) }
            runCatching {
                db.withTransaction {
                    store.save(project); store.saveBase(project)
                    assertEquals(base.projectJson, db.projectDao().getSyncSnapshot(project.id)!!.projectJson)
                    assertTrue(db.projectDao().getSyncSnapshot(project.id)!!.savedEpochMs > base.savedEpochMs)
                    assertNotEquals(before, recovery.fingerprint(project.id))
                    recovery.expectState(files, project.id); files.apply()
                    if (!commit) throw IOException("Interrupt identical import")
                }
            }.also { assertEquals(commit, it.isSuccess) }
            db.close(); db = database(path)
            try {
                assertTrue(ProjectRepository(db, root, "dummy-recovery-key").recoverAll().isEmpty())
                assertEquals(if (commit) "incoming bytes" else "old bytes", file.readText())
            } finally { db.close() }
        }
    }
}
