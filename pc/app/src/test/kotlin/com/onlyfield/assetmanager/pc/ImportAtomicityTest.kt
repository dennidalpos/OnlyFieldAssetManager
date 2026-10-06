package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import com.sun.nio.file.ExtendedOpenOption

class ImportAtomicityTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun cleanupFailureAfterCommitKeepsCommittedBytesAndReportsWarning() {
        val old = folder.newFile("payload").apply { writeBytes(ByteArray(2 * 1024 * 1024) { 42 }) }
        com.onlyfield.assetmanager.exchange.ReversibleFiles(folder.root).use { files ->
            files.replace(old) { it.write("incoming".toByteArray()) }
            files.apply()
            val backup = folder.root.walkTopDown().first { it.extension == "enc" }
            assertFalse(backup.readBytes().take(32).all { it == 42.toByte() })
            Files.newByteChannel(backup.toPath(), setOf(StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE)).use {
                assertTrue(files.commit().isNotEmpty())
            }
            assertEquals("incoming", old.readText())
        }
    }

    @Test fun lockedProjectOrBasePreservesOpenAndClosedCopiesAndTheirPayloads() {
        for (password in listOf(null, "test-password")) for (opened in listOf(false, true)) for (merge in listOf(false, true)) for (lockedBase in listOf(false, true)) {
            val storage = DesktopStorageManager(folder.newFolder())
            val att = Attachment(name = "Photo", originalFileName = "photo.png", relativePath = "")
            val local = Project(name = "Previous", createdEpochMs = 0, updatedEpochMs = 0, attachments = listOf(att), isPasswordProtected = password != null)
            val trash = listOf(TrashItem(projectId = local.id, itemType = "DEVICE", itemId = java.util.UUID.randomUUID().toString(), displayName = "Recoverable", serializedJson = "{}"))
            storage.storeAttachmentBytes(local.id, att, "old bytes".toByteArray())
            val main = storage.saveProjectLocally(local, password, trashItems = trash)
            storage.saveSyncBase(local, password)
            val base = File(storage.dataDir, "sync/${local.id}.ofam")
            val state = DesktopAppState(storage)
            try {
                if (opened) state.importFile(main, password, compare = false)
                val before = main.readBytes()
                val baseBefore = base.readBytes()
                val payloadBefore = storage.attachmentFile(local.id, att).takeIf { it.isFile }?.readBytes()
                val incoming = local.copy(name = "Incoming", updatedEpochMs = 100)
                val file = File(folder.newFolder(), "incoming.ofam").apply { writeBytes(PackageSerializer.exportPackage(incoming,
                    attachments = mapOf(AttachmentFiles.entryName(att) to "incoming bytes".toByteArray()), password = password)) }
                state.importFile(file, password)
                val review = state.dialog as AppDialog.Compare
                Files.newByteChannel((if (lockedBase) base else main).toPath(), setOf(StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE)).use {
                    if (merge) state.startMerge(review.pkg) else state.acceptIncoming(review.pkg, password)
                }
                assertNotNull(state.error)
                assertEquals(if (opened) local else null, state.project)
                assertArrayEquals(before, main.readBytes())
                assertArrayEquals(baseBefore, base.readBytes())
                if (payloadBefore != null) assertArrayEquals(payloadBefore, storage.attachmentFile(local.id, att).readBytes())
                else assertFalse(storage.attachmentFile(local.id, att).exists())
                storage.importPackageFromFile(main, password).pkg!!.use { assertEquals("old bytes", String(it.attachments.getValue(AttachmentFiles.entryName(att)))) }
                assertEquals(trash, storage.loadTrash(local.id, password))
                assertEquals("Previous", storage.loadSyncBase(local.id, password)?.name)
                state.importFile(file, password)
                val retry = state.dialog as AppDialog.Compare
                if (merge) state.startMerge(retry.pkg) else state.acceptIncoming(retry.pkg, password)
                assertNull(state.error)
                assertEquals("Incoming", state.project?.name)
                assertEquals("Incoming", storage.loadSyncBase(local.id, password)?.name)
                assertEquals(trash, storage.loadTrash(local.id, password))
                assertEquals("incoming bytes", String(storage.attachmentBytes(local.id, att)!!))
            } finally { state.shutdown() }
        }
    }
}
