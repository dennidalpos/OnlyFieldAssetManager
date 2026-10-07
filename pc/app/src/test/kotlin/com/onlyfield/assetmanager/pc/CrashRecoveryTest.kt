package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.*
import com.sun.nio.file.ExtendedOpenOption
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit

class CrashRecoveryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun restartRecoversProjectBaseTrashAndMediaWithLocalPasswordBeforeOpening() {
        for (password in listOf(null, "dummy-password")) for (phase in listOf("staged", "partial", "applied", "committed")) {
            val root = folder.newFolder()
            val storage = DesktopStorageManager(root)
            val att = Attachment(name = "Photo", originalFileName = "photo.bin", relativePath = "")
            val project = Project(name = "Previous", createdEpochMs = 0, updatedEpochMs = 0, isPasswordProtected = password != null, attachments = listOf(att))
            val trash = listOf(TrashItem(projectId = project.id, itemType = "DEVICE", itemId = java.util.UUID.randomUUID().toString(), displayName = "Recoverable", serializedJson = "{}"))
            storage.storeAttachmentBytes(project.id, att, "old media".toByteArray())
            val main = storage.saveProjectLocally(project, password, trashItems = trash, syncBase = project)
            val base = File(root, "sync/${project.id}.ofam")
            val mainBefore = main.readBytes()
            val baseBefore = base.readBytes()
            val java = File(System.getProperty("java.home"), "bin/java.exe").takeIf { it.isFile }
                ?: File(System.getProperty("java.home"), "bin/java")
            val process = ProcessBuilder(java.absolutePath, "-cp", System.getProperty("ofam.test.classpath"),
                DesktopRecoveryCrashProcess::class.java.name, root.path, project.id, password.orEmpty(), phase)
                .redirectErrorStream(true).start()
            assertTrue(process.waitFor(20, TimeUnit.SECONDS))
            assertEquals(process.inputStream.bufferedReader().readText(), 42, process.exitValue())
            val restarted = DesktopStorageManager(root)
            val state = DesktopAppState(restarted)
            try {
                if (password != null) {
                    assertNotNull(state.storedProjects.single().readError)
                    state.openStored(main)
                    assertTrue(state.dialog is AppDialog.ImportPassword)
                    assertNull(state.project)
                    state.importFile(main, "wrong", compare = false)
                    assertNull(state.project)
                    assertEquals(listOf(project.id), FileRecovery.owners(root))
                }
                state.importFile(main, password, compare = false)
                assertNull(state.error)
                assertEquals(if (phase == "committed") "Incoming" else "Previous", state.project?.name)
                assertEquals(state.project?.name, restarted.loadSyncBase(project.id, password)?.name)
                assertEquals(trash, restarted.loadTrash(project.id, password))
                assertEquals("old media", String(restarted.attachmentBytes(project.id, att)!!))
                if (password != null) assertFalse(restarted.attachmentFile(project.id, att).exists())
                assertTrue(FileRecovery.owners(root).isEmpty())
                if (phase != "committed") {
                    // Opening writes a fresh package; the recovery itself retained the original base bytes.
                    assertArrayEquals(baseBefore, base.readBytes())
                    assertEquals(project, PackageSerializer.importPackage(mainBefore, password).pkg!!.use { it.project })
                }
            } finally { state.shutdown() }
        }
    }

    @Test fun failedRecoveryBlocksWritesAndRetryPreservesExternalChanges() {
        val root = folder.newFolder()
        val storage = DesktopStorageManager(root)
        val project = Project(name = "Previous", createdEpochMs = 0, updatedEpochMs = 0)
        val main = storage.saveProjectLocally(project)
        val files = ReversibleFiles(recovery = FileRecovery.start(root, project.id, null))
        files.replace(main) { it.write(PackageSerializer.exportPackage(project.copy(name = "Incoming"))) }
        files.apply()
        val external = PackageSerializer.exportPackage(project.copy(name = "External"))
        main.writeBytes(external)
        val restarted = DesktopStorageManager(root)
        assertNotNull(restarted.listStoredProjects().single().readError)
        assertNotNull(runCatching { restarted.saveProjectLocally(project) }.exceptionOrNull())
        assertArrayEquals(external, main.readBytes())
        assertEquals(listOf(project.id), FileRecovery.owners(root))
    }
}

/** The child only interrupts its own isolated storage transaction. */
object DesktopRecoveryCrashProcess {
    @JvmStatic fun main(args: Array<String>) {
        val root = File(args[0]); val id = args[1]; val password = args[2].ifEmpty { null }
        val main = File(root, "projects/$id.ofam"); val base = File(root, "sync/$id.ofam")
        PackageSerializer.importPackage(main.readBytes(), password).pkg!!.use { local ->
            val incoming = local.project.copy(name = "Incoming")
            val files = ReversibleFiles(recovery = FileRecovery.start(root, id, password))
            files.replace(main) { PackageSerializer.exportPackageToStream(it, incoming, local.attachments, password) }
            files.replace(base) { PackageSerializer.exportPackageToStream(it, incoming, password = password) }
            when (args[3]) {
                "partial" -> Files.newByteChannel(base.toPath(), setOf(StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE)).use {
                    check(runCatching { files.apply() }.exceptionOrNull() is java.io.IOException)
                }
                "applied" -> files.apply()
                "committed" -> {
                    files.apply()
                    val backup = File(root, ".recovery/$id").walkTopDown().single { it.name == "0-old.enc" }
                    Files.newByteChannel(backup.toPath(), setOf(StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE)).use {
                        check(files.commit().isNotEmpty())
                    }
                }
            }
            Runtime.getRuntime().halt(42)
        }
    }
}
