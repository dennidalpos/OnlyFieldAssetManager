package com.onlyfield.assetmanager

import android.content.Context
import android.content.ContextWrapper
import android.os.Process
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.EncryptedDatabase
import com.onlyfield.assetmanager.data.repository.*
import com.onlyfield.assetmanager.exchange.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EncryptedRecoveryNativeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scenarios = listOf("rollback-update", "commit-update", "rollback-delete", "commit-delete", "blocked")

    /** Optional prepare/recover invocations verify a new process; the default cleans up in one run. */
    @Test fun encryptedRecoveryRetainsTheRoomOutcomeAndRetriesBlockedRollback() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("recoveryPhase", "roundtrip")
        require(phase in listOf("prepare", "recover", "roundtrip"))
        val token = UUID.fromString(args.getString("recoveryRun", UUID.randomUUID().toString())).toString()
        val root = File(context.cacheDir, "native-recovery-$token")
        check(root.canonicalFile.parentFile == context.cacheDir.canonicalFile)
        var prepared = false
        try {
            if (phase != "recover") {
                check(root.mkdir())
                prepare(root)
                File(root, "pid.txt").writeText(Process.myPid().toString())
                prepared = true
            }
            if (phase != "prepare") {
                check(root.isDirectory)
                if (phase == "recover") assertNotEquals(File(root, "pid.txt").readText().toInt(), Process.myPid())
                recover(root)
            }
        } finally {
            if (phase != "prepare" || !prepared) {
                check(root.canonicalFile.parentFile == context.cacheDir.canonicalFile)
                if (root.exists()) check(root.deleteRecursively())
            }
        }
    }

    private fun isolated(root: File, scenario: String): Context {
        val folder = File(root, scenario).apply { check(isDirectory || mkdir()) }
        val keys = File(root, "keys").apply { check(isDirectory || mkdir()) }
        return object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = File(folder, "isolated.db")
            override fun getNoBackupFilesDir(): File = keys
        }
    }

    private fun projectId(root: File, scenario: String): String = UUID.nameUUIDFromBytes("${root.name}/$scenario".toByteArray()).toString()

    private suspend fun prepare(root: File) {
        for (scenario in scenarios) {
            val isolated = isolated(root, scenario)
            val media = File(root, "$scenario/media").apply { check(mkdir()) }
            val db = EncryptedDatabase.open(isolated)
            try {
                val password = EncryptedDatabase.recoveryPassword(isolated)
                val repository = ProjectRepository(db, media, password)
                val device = Device(technicalName = "Synthetic recoverable device")
                val attachment = Attachment(name = "Synthetic recovery media", originalFileName = "photo.bin", relativePath = "")
                val project = Project(id = projectId(root, scenario), name = "Previous isolated state", createdEpochMs = 1, updatedEpochMs = 1,
                    sites = listOf(Site(name = "Synthetic site", devices = listOf(device))), attachments = listOf(attachment))
                repository.saveProject(project)
                assertTrue(repository.setProjectPassword(project.id, null, "dummy-project-password"))
                repository.moveToTrash(project.id, "DEVICE", device.id)
                val file = AttachmentFiles.localFile(media, project.id, attachment).apply {
                    check(parentFile!!.mkdirs()); writeText("old synthetic bytes")
                }
                assertNotNull(repository.exportProjectPackage(project.id, "dummy-project-password"))
                val before = requireNotNull(repository.getProjectById(project.id))
                val storage = ProjectStore(db)
                val recovery = ProjectRecovery(db, media, password)
                val beforeState = recovery.fingerprint(project.id)
                val files = recovery.files(project.id)
                val deletion = scenario.endsWith("delete")
                val commit = scenario.startsWith("commit")
                if (deletion) files.remove(file) else files.replace(file) { it.write("incoming synthetic bytes".toByteArray()) }
                if (scenario == "blocked") {
                    files.apply()
                    file.writeText("external synthetic edit")
                } else {
                    val interruption = IOException("Synthetic interruption before commit")
                    val result = runCatching {
                        db.withTransaction {
                            if (deletion) {
                                db.projectDao().deleteSyncSnapshot(project.id)
                                db.projectDao().deleteProjectById(project.id)
                            } else {
                                val incoming = before.copy(name = "Incoming isolated state", isPasswordProtected = false)
                                storage.saveImported(incoming, null)
                                storage.saveBase(incoming)
                            }
                            recovery.expectState(files, project.id)
                            files.apply()
                            if (!commit) throw interruption
                        }
                    }
                    if (commit) result.getOrThrow() else assertSame(interruption, result.exceptionOrNull())
                }
                File(root, "$scenario/expected.txt").writeText(if (commit) recovery.fingerprint(project.id) else beforeState)
                assertEquals(listOf(project.id), FileRecovery.owners(media))
                // Leave the durable journal pending; the next invocation owns recovery.
            } finally { db.close() }
            val header = File(root, "$scenario/isolated.db").inputStream().use { it.readNBytes(16) }
            assertFalse(String(header, Charsets.US_ASCII).startsWith("SQLite format 3"))
        }
        val wrapped = File(root, "keys/recovery_key.bin").readBytes()
        assertEquals(60, wrapped.size)
        File(root, "wrapped.sha256").writeText(PackageSerializer.calculateSha256(wrapped))
    }

    private suspend fun recover(root: File) {
        assertEquals(File(root, "wrapped.sha256").readText(), PackageSerializer.calculateSha256(File(root, "keys/recovery_key.bin").readBytes()))
        for (scenario in scenarios) {
            val isolated = isolated(root, scenario)
            val media = File(root, "$scenario/media")
            val id = projectId(root, scenario)
            val db = EncryptedDatabase.open(isolated)
            try {
                val password = EncryptedDatabase.recoveryPassword(isolated)
                val repository = ProjectRepository(db, media, password)
                if (scenario == "blocked") {
                    val journal = File(media, ".recovery/$id").walkTopDown().filter { it.isFile }.associate {
                        it.relativeTo(media).path to PackageSerializer.calculateSha256(it.readBytes())
                    }
                    assertEquals(1, repository.recoverAll().size)
                    assertTrue(runCatching { repository.getProjectById(id) }.exceptionOrNull() is IOException)
                    assertTrue(runCatching {
                        repository.saveProject(Project(id = id, name = "Rejected overwrite", createdEpochMs = 1, updatedEpochMs = 1))
                    }.exceptionOrNull() is IOException)
                    journal.forEach { (path, hash) -> assertEquals(hash, PackageSerializer.calculateSha256(File(media, path).readBytes())) }
                    val project = requireNotNull(ProjectStore(db).load(id))
                    val file = AttachmentFiles.localFile(media, id, project.attachments.single())
                    assertEquals("external synthetic edit", file.readText())
                    file.writeText("incoming synthetic bytes")
                }
                assertTrue(repository.recoverAll().isEmpty())
                assertEquals(File(root, "$scenario/expected.txt").readText(), ProjectRecovery(db, media, password).fingerprint(id))
                val project = repository.getProjectById(id)
                if (scenario == "commit-delete") {
                    assertNull(project)
                    assertNull(repository.getSyncBase(id))
                    assertTrue(AttachmentFiles.ownedFiles(media, id).isEmpty())
                } else {
                    requireNotNull(project)
                    assertEquals(if (scenario == "commit-update") "Incoming isolated state" else "Previous isolated state", project.name)
                    val file = AttachmentFiles.localFile(media, id, project.attachments.single())
                    assertEquals(if (scenario == "commit-update") "incoming synthetic bytes" else "old synthetic bytes", file.readText())
                    assertEquals(1, repository.getTrashItems(id).size)
                    if (scenario != "commit-update") assertTrue(repository.verifyProjectPassword(id, "dummy-project-password"))
                }
                assertTrue(FileRecovery.owners(media).isEmpty())
                assertTrue(repository.recoverAll().isEmpty())
            } finally { db.close() }
        }
    }
}
