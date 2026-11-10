package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

class ProtectedMediaTest {
    @get:Rule val folder = TemporaryFolder()
    private val image = Attachment(name = "Photo", originalFileName = "photo.png", relativePath = "", fileType = AttachmentType.IMAGE)
    private val plan = Attachment(name = "Plan", originalFileName = "plan.pdf", relativePath = "", fileType = AttachmentType.PDF)
    private val project = Project(name = "Protected", createdEpochMs = 0, updatedEpochMs = 0, isPasswordProtected = true, attachments = listOf(image, plan))
    private val imageBytes = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()
    private val pdfBytes = ByteArrayOutputStream().also { out -> PDDocument().use { it.addPage(PDPage()); it.save(out) } }.toByteArray()
    private val media = mapOf(AttachmentFiles.entryName(image) to imageBytes, AttachmentFiles.entryName(plan) to pdfBytes)

    private fun incoming(): File = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(project, media, "old-password")) }

    private fun noPlainCopies(storage: DesktopStorageManager) {
        assertEquals(0, storage.getMediaFolder().takeIf { it.exists() }?.walkTopDown()?.count { it.isFile } ?: 0)
        assertEquals(0, storage.getTempFolder().walkTopDown().count { it.isFile })
    }

    @Test fun protectedImportRendersFromMemoryAndReopensWithoutPlainFiles() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        try {
            state.importFile(incoming(), "old-password")
            assertNull(state.error)
            assertEquals(project.id, state.project?.id)
            assertArrayEquals(imageBytes, state.attachmentBytes(image))
            assertEquals(1, PlanMedia.pageCount(state.attachmentBytes(plan)!!))
            assertEquals(3, PlanMedia.bufferedImage(state.attachmentBytes(image)!!, false, 0)!!.width)
            noPlainCopies(storage)
            val local = state.storedProjects.single().file
            state.closeProject()
            assertNull(storage.attachmentBytes(project.id, image))
            noPlainCopies(storage)
            state.importFile(local, "wrong-password", compare = false)
            assertNull(state.project)
            assertNull(storage.attachmentBytes(project.id, image))
            state.importFile(local, "old-password", compare = false)
            assertNull(state.error)
            assertArrayEquals(pdfBytes, state.attachmentBytes(plan))
            noPlainCopies(storage)
        } finally { state.shutdown() }
        assertNull(storage.attachmentBytes(project.id, image))
    }

    @Test fun passwordChangesAndRemovalPreserveAllPayloads() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        try {
            state.importFile(incoming(), "old-password")
            assertNull(state.changePassword("old-password", "new-password", "new-password"))
            noPlainCopies(storage)
            val local = state.storedProjects.single().file
            state.closeProject()
            assertNull(PackageSerializer.importPackage(local.readBytes(), "old-password").pkg)
            state.importFile(local, "new-password", compare = false)
            assertArrayEquals(imageBytes, state.attachmentBytes(image))
            assertArrayEquals(pdfBytes, state.attachmentBytes(plan))
            assertNull(state.changePassword("new-password", "", ""))
            state.closeProject()
            state.openStored(local)
            assertFalse(state.hasPassword)
            assertArrayEquals(imageBytes, state.attachmentBytes(image))
            assertArrayEquals(pdfBytes, state.attachmentBytes(plan))
        } finally { state.shutdown() }
    }

    @Test fun enablingProtectionMovesExistingPlainMediaIntoEncryptedPackage() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val plainProject = project.copy(isPasswordProtected = false)
        val file = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(plainProject, media)) }
        try {
            state.importFile(file)
            assertTrue(storage.attachmentFile(project.id, image).isFile)
            assertNull(state.changePassword("", "password", "password"))
            noPlainCopies(storage)
            val local = state.storedProjects.single().file
            state.closeProject()
            assertNull(storage.attachmentBytes(project.id, image))
            state.importFile(local, "password", compare = false)
            assertArrayEquals(imageBytes, state.attachmentBytes(image))
            assertArrayEquals(pdfBytes, state.attachmentBytes(plan))
            noPlainCopies(storage)
        } finally { state.shutdown() }
    }

    @Test fun legacyProtectedCopyIsUpgradedOnlyAfterCorrectUnlock() {
        val storage = DesktopStorageManager(folder.newFolder())
        val local = File(storage.getProjectsFolder(), "${project.id}.ofam").apply { writeBytes(PackageSerializer.exportPackage(project, password = "password")) }
        val plain = storage.attachmentFile(project.id, image).apply { parentFile.mkdirs(); writeBytes(imageBytes) }
        val state = DesktopAppState(storage)
        try {
            val before = local.readBytes()
            state.importFile(local, "wrong", compare = false)
            assertArrayEquals(before, local.readBytes())
            assertTrue(plain.isFile)
            state.importFile(local, "password", compare = false)
            assertNull(state.error)
            noPlainCopies(storage)
            state.closeProject()
            val upgraded = PackageSerializer.importPackage(local.readBytes(), "password").pkg!!
            assertArrayEquals(imageBytes, AttachmentFiles.bytesIn(upgraded, image))
            assertNull(AttachmentFiles.bytesIn(upgraded, plan))
        } finally { state.shutdown() }
    }

    @Test fun editsAndNewMediaPersistInEncryptedCopy() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        try {
            state.importFile(incoming(), "old-password")
            val extra = Attachment(name = "Extra", originalFileName = "extra.txt", relativePath = "")
            val bytes = "PRIVATE-MEDIA-MARKER".toByteArray()
            storage.storeAttachmentBytes(project.id, extra, bytes)
            state.update(state.project!!.copy(name = "Edited", attachments = project.attachments + extra), "edited")
            noPlainCopies(storage)
            val local = state.storedProjects.single().file
            state.closeProject()
            assertNull(storage.attachmentBytes(project.id, extra))
            assertFalse(local.readBytes().toString(Charsets.ISO_8859_1).contains("PRIVATE-MEDIA-MARKER"))
            state.importFile(local, "old-password", compare = false)
            assertEquals("Edited", state.project?.name)
            assertArrayEquals(bytes, state.attachmentBytes(extra))
            assertArrayEquals(pdfBytes, state.attachmentBytes(plan))
            noPlainCopies(storage)
        } finally { state.shutdown() }
    }
}
