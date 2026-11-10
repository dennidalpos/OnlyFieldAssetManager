package com.onlyfield.assetmanager.pc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.pc.ui.dialogs.ProjectDialogs
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ImportPayloadReviewTest {
    @get:Rule val compose = createComposeRule()
    private val directory = Files.createTempDirectory("ofam-review").toFile()
    private val storage = DesktopStorageManager(File(directory, "data"))
    private val state = DesktopAppState(storage)
    private val attachment = Attachment(name = "Missing photo", originalFileName = "photo.jpg", relativePath = "")
    private val project = Project(name = "Incoming", createdEpochMs = 0, updatedEpochMs = 0, attachments = listOf(attachment))

    @After fun cleanUp() {
        storage.releaseAllLocks()
        directory.deleteRecursively()
    }

    @Test fun newIncompleteProjectNeedsConfirmationAndDisplaysWarning() {
        val file = File(directory, "incoming.ofam").apply { writeBytes(PackageSerializer.exportPackage(project)) }
        state.importFile(file)
        assertNull(state.project)
        val review = state.dialog as AppDialog.Compare
        assertEquals(listOf(attachment.id), review.warnings.map { it.targetEntityId })
        compose.setContent { ProjectDialogs(state) }
        compose.onNodeWithText(review.warnings.single().message).assertIsDisplayed()
        state.acceptIncoming(review.pkg, review.password)
        assertEquals(project.id, state.project?.id)
        assertEquals(listOf(attachment.id), storage.missingAttachments(state.project!!).map { it.id })
    }

    @Test fun completePackageOpensWithoutMissingWarnings() {
        val file = File(directory, "incoming.ofam").apply {
            writeBytes(PackageSerializer.exportPackage(project, mapOf(AttachmentFiles.entryName(attachment) to "PHOTO".toByteArray())))
        }
        state.importFile(file)
        assertNull(state.dialog)
        assertEquals(project.id, state.project?.id)
        assertTrue(storage.missingAttachments(state.project!!).isEmpty())
    }
}
