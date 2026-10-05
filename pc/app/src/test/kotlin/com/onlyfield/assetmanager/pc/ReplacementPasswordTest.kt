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

class ReplacementPasswordTest {
    @get:Rule val folder = TemporaryFolder()
    private val project = Project(name = "Local", createdEpochMs = 0, updatedEpochMs = 0, isPasswordProtected = true)
    private val deleted = Attachment(name = "Deleted photo", originalFileName = "photo.jpg", relativePath = "")
    private val payload = "DELETED-PHOTO".toByteArray()
    private val trash = TrashItem(projectId = project.id, itemType = "ATTACHMENT", itemId = deleted.id, displayName = deleted.name,
        serializedJson = PackageSerializer.jsonConfig.encodeToString(Attachment.serializer(), deleted))
    private fun stored(storage: DesktopStorageManager): File = storage.saveProjectLocally(project, "local-password",
        attachments = mapOf(AttachmentFiles.entryName(deleted) to payload), trashItems = listOf(trash))
    private fun incoming(password: String? = "incoming-password") = PackageSerializer.importPackage(
        PackageSerializer.exportPackage(project.copy(name = "Incoming", isPasswordProtected = password != null), password = password), password).pkg!!

    @Test fun closedCopyPromptsAndPreservesTrashMediaThroughReopen() {
        for (password in listOf("incoming-password", null)) {
            val storage = DesktopStorageManager(folder.newFolder())
            val file = stored(storage)
            val before = file.readBytes()
            val state = DesktopAppState(storage)
            try {
                val pkg = incoming(password)
                state.acceptIncoming(pkg, password)
                assertTrue(state.dialog is AppDialog.LocalReplacementPassword)
                assertNull(state.project)
                assertArrayEquals(before, file.readBytes())
                state.acceptIncomingWithLocalPassword(pkg, password, "wrong")
                assertTrue((state.dialog as AppDialog.LocalReplacementPassword).wrongPassword)
                assertArrayEquals(before, file.readBytes())
                state.dialog = null
                assertArrayEquals(before, file.readBytes())
                state.acceptIncomingWithLocalPassword(pkg, password, "local-password")
                assertNull(state.error)
                assertEquals("Incoming", state.project?.name)
                assertEquals(listOf(trash), state.trash)
                assertEquals(password != null, state.hasPassword)
                assertArrayEquals(payload, storage.attachmentBytes(project.id, deleted))
                state.closeProject()
                state.importFile(file, password, compare = false)
                assertEquals("Incoming", state.project?.name)
                assertEquals(listOf(trash), state.trash)
                assertArrayEquals(payload, storage.attachmentBytes(project.id, deleted))
                val exported = PackageSerializer.importPackage(file.readBytes(), password).pkg!!
                assertArrayEquals(payload, exported.attachments[AttachmentFiles.entryName(deleted)])
                if (password != null) assertNull(PackageSerializer.importPackage(file.readBytes(), "local-password").pkg)
            } finally { state.shutdown() }
        }
    }

    @Test fun openCopyUsesItsUnlockedTrashWithoutAnotherPrompt() {
        val storage = DesktopStorageManager(folder.newFolder())
        val file = stored(storage)
        val state = DesktopAppState(storage)
        try {
            state.importFile(file, "local-password", compare = false)
            state.acceptIncoming(incoming(), "incoming-password")
            assertNull(state.dialog)
            assertNull(state.error)
            assertEquals("Incoming", state.project?.name)
            assertEquals(listOf(trash), state.trash)
            assertArrayEquals(payload, storage.attachmentBytes(project.id, deleted))
        } finally { state.shutdown() }
    }

    @Test fun corruptLocalTrashPreventsReplacementWithoutLosingOldCopy() {
        val storage = DesktopStorageManager(folder.newFolder())
        val file = File(storage.getProjectsFolder(), "${project.id}.ofam").apply {
            writeBytes(PackageSerializer.exportPackage(project, mapOf(DesktopStorageManager.LOCAL_TRASH_ENTRY to "broken".toByteArray()), "local-password"))
        }
        val before = file.readBytes()
        val state = DesktopAppState(storage)
        try {
            state.acceptIncomingWithLocalPassword(incoming(), "incoming-password", "local-password")
            assertNull(state.project)
            assertNotNull(state.error)
            assertArrayEquals(before, file.readBytes())
        } finally { state.shutdown() }
    }

    @Test fun failedAtomicReplacementRetainsLocalCopyAndReleasesLock() {
        val storage = DesktopStorageManager(folder.newFolder())
        val file = stored(storage)
        val before = file.readBytes()
        val state = DesktopAppState(storage)
        try {
            Files.newByteChannel(file.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
                state.acceptIncomingWithLocalPassword(incoming(), "incoming-password", "local-password")
                assertNull(state.project)
                assertNotNull(state.error)
                assertArrayEquals(before, file.readBytes())
                assertFalse(storage.ownsProjectLock(project.id))
            }
            state.importFile(file, "local-password", compare = false)
            assertEquals(project.name, state.project?.name)
            assertEquals(listOf(trash), state.trash)
            assertArrayEquals(payload, storage.attachmentBytes(project.id, deleted))
            assertFalse(storage.dataDir.walkTopDown().any { it.extension == "tmp" })
        } finally { state.shutdown() }
    }
}
