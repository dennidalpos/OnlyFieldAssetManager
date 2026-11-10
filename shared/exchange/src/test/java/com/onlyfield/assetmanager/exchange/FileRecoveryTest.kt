package com.onlyfield.assetmanager.exchange

import com.sun.nio.file.ExtendedOpenOption
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.concurrent.TimeUnit

class FileRecoveryTest {
    @get:Rule val folder = TemporaryFolder()
    private val owner = UUID.randomUUID().toString()
    private fun target(name: String) = File(folder.root, "$owner/$name").apply { parentFile.mkdirs() }

    private fun crash(phase: String) {
        val java = File(System.getProperty("java.home"), "bin/java.exe")
            .takeIf { it.isFile } ?: File(System.getProperty("java.home"), "bin/java")
        val process = ProcessBuilder(java.absolutePath, "-cp", System.getProperty("ofam.test.classpath"),
            RecoveryCrashProcess::class.java.name, folder.root.absolutePath, owner, phase).redirectErrorStream(true).start()
        assertTrue("Recovery child did not finish", process.waitFor(20, TimeUnit.SECONDS))
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(output, 42, process.exitValue())
    }

    @Test fun processStopAfterApplyRollsBackAddedReplacedAndRemovedFiles() {
        target("old").writeText("old bytes")
        target("removed").writeText("removed bytes")
        crash("applied")
        assertEquals("new bytes", target("old").readText())
        assertFalse(target("removed").exists())
        assertTrue(target("added").exists())
        assertThrows(RecoveryPasswordRequired::class.java) { FileRecovery.recover(folder.root, owner, null) }
        assertThrows(RecoveryPasswordRequired::class.java) { FileRecovery.recover(folder.root, owner, "wrong") }
        FileRecovery.recover(folder.root, owner, "dummy-password")
        assertEquals("old bytes", target("old").readText())
        assertEquals("removed bytes", target("removed").readText())
        assertFalse(target("added").exists())
        assertTrue(FileRecovery.owners(folder.root).isEmpty())
    }

    @Test fun processStopDuringStagingLeavesTheOriginalFiles() {
        target("old").writeText("old bytes")
        target("removed").writeText("removed bytes")
        crash("staged")
        FileRecovery.recover(folder.root, owner, "dummy-password")
        assertEquals("old bytes", target("old").readText())
        assertEquals("removed bytes", target("removed").readText())
        assertFalse(target("added").exists())
    }

    @Test fun databaseCommitBeforeJournalCommitFinishesTheFileChanges() {
        target("old").writeText("old bytes")
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, "dummy-password", beforeState = "old-state"))
        files.replace(target("old")) { it.write("new bytes".toByteArray()) }
        files.expectState("new-state")
        files.apply()
        FileRecovery.recover(folder.root, owner, "dummy-password", currentState = "new-state")
        assertEquals("new bytes", target("old").readText())
        assertTrue(FileRecovery.owners(folder.root).isEmpty())
    }

    @Test fun rolledBackDatabaseRestoresFilesAndChangedDatabaseBlocksRecovery() {
        target("old").writeText("old bytes")
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, "dummy-password", beforeState = "old-state"))
        files.replace(target("old")) { it.write("new bytes".toByteArray()) }
        files.expectState("new-state")
        files.apply()
        assertThrows(IllegalStateException::class.java) { FileRecovery.recover(folder.root, owner, "dummy-password", "unrelated-state") }
        assertEquals("new bytes", target("old").readText())
        FileRecovery.recover(folder.root, owner, "dummy-password", "old-state")
        assertEquals("old bytes", target("old").readText())
    }

    @Test fun secondRollbackFailureKeepsBackupsAndRetrySucceeds() {
        val old = target("old").apply { writeText("old bytes") }
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, "dummy-password"))
        files.replace(old) { it.write("new bytes".toByteArray()) }
        files.apply()
        Files.newByteChannel(old.toPath(), setOf(StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE)).use {
            assertNotNull(runCatching { files.close() }.exceptionOrNull())
            assertEquals("new bytes", old.readText())
            assertEquals(listOf(owner), FileRecovery.owners(folder.root))
        }
        FileRecovery.recover(folder.root, owner, "dummy-password")
        assertEquals("old bytes", old.readText())
    }

    @Test fun corruptBackupNeverReplacesTheDestinationAndIsPreserved() {
        val old = target("old").apply { writeText("old bytes") }
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, "dummy-password"))
        files.replace(old) { it.write("new bytes".toByteArray()) }
        files.apply()
        val backup = File(folder.root, ".recovery/$owner").walkTopDown().single { it.name == "0-old.enc" }
        val bytes = backup.readBytes()
        backup.writeBytes(bytes.copyOf(20))
        assertNotNull(runCatching { FileRecovery.recover(folder.root, owner, "dummy-password") }.exceptionOrNull())
        assertEquals("new bytes", old.readText())
        assertTrue(backup.exists())
        backup.writeBytes(bytes)
        FileRecovery.recover(folder.root, owner, "dummy-password")
        assertEquals("old bytes", old.readText())
    }

    @Test fun unknownFilesAndTargetsOutsideTheProjectArePreserved() {
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, null))
        val outside = folder.newFile("user-source").apply { writeText("user bytes") }
        assertThrows(IllegalArgumentException::class.java) { files.replace(outside) { it.write(byteArrayOf(1)) } }
        val dir = File(folder.root, ".recovery/$owner").listFiles()!!.single()
        val unknown = File(dir, "user-backup").apply { writeText("keep") }
        assertNotNull(runCatching { files.close() }.exceptionOrNull())
        assertEquals("keep", unknown.readText())
        assertEquals("user bytes", outside.readText())
    }

    @Test fun largeBackupsAreChunkedEncryptedAndRestoreTheSameHash() {
        val old = target("old")
        val bytes = ByteArray(10 * 1024 * 1024) { (it % 251).toByte() }
        old.writeBytes(bytes)
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, "dummy-password"))
        files.replace(old) { it.write("new bytes".toByteArray()) }
        files.apply()
        val backup = File(folder.root, ".recovery/$owner").walkTopDown().single { it.name == "0-old.enc" }
        java.util.zip.ZipFile(backup).use { assertTrue(it.size() > 1) }
        assertFalse(backup.readBytes().toString(Charsets.ISO_8859_1).contains("new bytes"))
        FileRecovery.recover(folder.root, owner, "dummy-password")
        assertEquals(PackageSerializer.calculateSha256(bytes), PackageSerializer.calculateSha256(old.readBytes()))
    }

    @Test fun aFileChangedOutsideTheAppBlocksRecoveryWithoutOverwritingIt() {
        val old = target("old").apply { writeText("old bytes") }
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, null))
        files.replace(old) { it.write("new bytes".toByteArray()) }
        files.apply()
        old.writeText("external edit")
        assertThrows(IllegalStateException::class.java) { FileRecovery.recover(folder.root, owner, null) }
        assertEquals("external edit", old.readText())
    }

    @Test fun missingJournalPreservesEveryBackupAndBlocksRecovery() {
        val old = target("old").apply { writeText("old bytes") }
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, null))
        files.replace(old) { it.write("new bytes".toByteArray()) }; files.apply()
        val journal = File(folder.root, ".recovery/$owner").walkTopDown().single { it.name == "journal.enc" }
        val backup = File(journal.parentFile, "0-old.enc")
        val saved = journal.readBytes(); Files.delete(journal.toPath())
        assertThrows(IllegalStateException::class.java) { FileRecovery.recover(folder.root, owner, null) }
        assertEquals("new bytes", old.readText()); assertTrue(backup.isFile)
        journal.writeBytes(saved); FileRecovery.recover(folder.root, owner, null)
        assertEquals("old bytes", old.readText())
    }

    @Test fun databaseCommitIsNeverRolledBackWhenCommitMarkerWriteFails() {
        val old = target("old").apply { writeText("old bytes") }
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, "dummy-password", "before"))
        files.replace(old) { it.write("new bytes".toByteArray()) }; files.expectState("after"); files.apply(); files.databaseCommitted()
        val journal = File(folder.root, ".recovery/$owner").walkTopDown().single { it.name == "journal.enc" }
        Files.newByteChannel(journal.toPath(), setOf(StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE)).use {
            assertTrue(files.commit().isNotEmpty()); files.close()
            assertEquals("new bytes", old.readText())
        }
        FileRecovery.recover(folder.root, owner, "dummy-password", "after")
        assertEquals("new bytes", old.readText()); assertTrue(FileRecovery.owners(folder.root).isEmpty())
    }

    @Test fun interruptedHeaderCreationRemovesOnlyItsOwnUnappliedTemp() {
        val dir = File(folder.root, ".recovery/$owner/txn-${UUID.randomUUID()}").apply { mkdirs() }
        File(dir, "header.tmp").writeText("partial header")
        assertFalse(FileRecovery.requiresPassword(folder.root, owner))
        FileRecovery.recover(folder.root, owner, null)
        assertFalse(dir.exists()); assertTrue(FileRecovery.owners(folder.root).isEmpty())
    }

    @Test fun occupiedRecoveryTempBlocksWithoutTouchingTheDestinationOrBackup() {
        val old = target("old").apply { writeText("old bytes") }
        val files = ReversibleFiles(recovery = FileRecovery.start(folder.root, owner, null))
        files.replace(old) { it.write("new bytes".toByteArray()) }; files.apply()
        val transaction = File(folder.root, ".recovery/$owner").listFiles()!!.single()
        val temp = File(old.parentFile, ".ofam-recover-${transaction.name}-0.tmp")
        Files.createDirectory(temp.toPath())
        assertThrows(IllegalStateException::class.java) { FileRecovery.recover(folder.root, owner, null) }
        assertEquals("new bytes", old.readText()); assertTrue(File(transaction, "0-old.enc").isFile)
        Files.delete(temp.toPath()); FileRecovery.recover(folder.root, owner, null)
        assertEquals("old bytes", old.readText())
    }
}

/** Only this test child exits without closing its own transaction. */
object RecoveryCrashProcess {
    @JvmStatic fun main(args: Array<String>) {
        val root = File(args[0])
        val owner = args[1]
        val files = ReversibleFiles(recovery = FileRecovery.start(root, owner, "dummy-password"))
        files.replace(File(root, "$owner/old")) { it.write("new bytes".toByteArray()) }
        files.remove(File(root, "$owner/removed"))
        files.replace(File(root, "$owner/added")) { it.write("added bytes".toByteArray()) }
        if (args[2] == "applied") files.apply()
        Runtime.getRuntime().halt(42)
    }
}
