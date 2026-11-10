package com.onlyfield.assetmanager.pc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.ui.FloorplanMediaSection
import com.onlyfield.assetmanager.pc.ui.MediaThumbnail
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO

class MediaUiDispatchTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun attachmentListUsesAvailabilityWithoutRequestingPayloadBytes() {
        val present = Attachment(name = "Available", originalFileName = "large.bin", relativePath = "")
        val missing = Attachment(name = "Missing", originalFileName = "missing.bin", relativePath = "")
        val project = Project(name = "Media", createdEpochMs = 0, updatedEpochMs = 0, attachments = listOf(present, missing))
        compose.setContent {
            FloorplanMediaSection(project, { _, _ -> }, { _, _, _ -> },
                { fail("Listing must not load or decrypt payloads"); null }, { it.id == present.id }, {}, true, { _, _ -> false })
        }
        compose.onNodeWithText("Available").assertIsDisplayed()
        compose.onNodeWithText("Missing").assertIsDisplayed()
    }

    @Test fun thumbnailReadsOnAWorkerInsteadOfTheCompositionThread() {
        val bytes = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()
        val compositionThread = AtomicReference<Thread>()
        val workerRead = AtomicBoolean()
        compose.setContent {
            compositionThread.set(Thread.currentThread())
            MediaThumbnail("photo", {
                assertNotSame(compositionThread.get(), Thread.currentThread())
                workerRead.set(true)
                bytes
            })
        }
        compose.waitUntil(5000) { workerRead.get() }
    }

    @Test fun protectedAvailabilityDoesNotDecryptOrReadThePayload() {
        val storage = DesktopStorageManager(folder.newFolder())
        val attachment = Attachment(name = "Encrypted", originalFileName = "encrypted.bin", relativePath = "")
        val project = Project(name = "Protected", createdEpochMs = 0, updatedEpochMs = 0, isPasswordProtected = true)
        val file = storage.saveProjectLocally(project, "dummy-password")
        val state = DesktopAppState(storage)
        try {
            state.importFile(file, "dummy-password", compare = false)
            val source = File(folder.newFolder(), "encrypted.bin").apply { writeBytes(ByteArray(2 * 1024 * 1024)) }
            state.addAttachment(source, attachment.name, AttachmentClassification.SHAREABLE)
            assertNull(state.error)
            val saved = state.project!!.attachments.single()
            val staged = storage.getTempFolder().walkTopDown().single { it.isFile }
            staged.writeBytes(byteArrayOf(1, 2, 3))
            assertTrue(state.hasAttachment(saved))
            assertFalse(state.hasAttachment(attachment))
            assertTrue(runCatching { state.attachmentBytes(saved) }.isFailure)
        } finally { state.shutdown() }
    }
}
