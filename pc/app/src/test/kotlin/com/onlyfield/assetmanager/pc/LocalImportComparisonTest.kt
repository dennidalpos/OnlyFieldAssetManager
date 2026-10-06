package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.exchange.ComparisonStatus
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalImportComparisonTest {
    @get:Rule val folder = TemporaryFolder()
    private val local = Project(name = "Local B", createdEpochMs = 100, updatedEpochMs = 200)
    private val other = Project(name = "Open A", createdEpochMs = 100, updatedEpochMs = 100)

    private fun incoming(project: Project = local.copy(name = "Older B", updatedEpochMs = 100), password: String? = null) =
        File(folder.newFolder(), "incoming.ofam").apply { writeBytes(PackageSerializer.exportPackage(project, password = password)) }

    @Test fun olderPackageReviewsItsLocalCopyWithNoneOtherOrSameProjectOpen() {
        for (open in listOf(null, other, local)) {
            val storage = DesktopStorageManager(folder.newFolder())
            val file = storage.saveProjectLocally(local)
            val state = DesktopAppState(storage)
            try {
                open?.let { state.openStored(storage.saveProjectLocally(it)) }
                val before = file.readBytes()
                state.importFile(incoming())
                val review = state.dialog as AppDialog.Compare
                assertEquals(local.id, review.comparison.currentProjectId)
                assertEquals(ComparisonStatus.OLDER_REVISION, review.comparison.status)
                assertNotNull(review.comparison.warningMessage)
                assertEquals(open, state.project)
                state.dismissDialog()
                assertEquals(open, state.project)
                assertArrayEquals(before, file.readBytes())
                state.importFile(incoming())
                val confirmed = state.dialog as AppDialog.Compare
                state.acceptIncoming(confirmed.pkg, confirmed.password)
                assertEquals("Older B", state.project?.name)
                storage.importPackageFromFile(file).pkg!!.use { assertEquals("Older B", it.project.name) }
            } finally { state.shutdown() }
        }
    }

    @Test fun differentProjectDoesNotOfferTheOpenCopyAsAMergeTarget() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        try {
            state.openStored(storage.saveProjectLocally(other))
            state.importFile(incoming())
            val review = state.dialog as AppDialog.Compare
            assertNull(review.comparison.currentProjectId)
            state.startMerge(review.pkg)
            assertSame(review, state.dialog)
            assertEquals(other, state.project)
            assertFalse(File(storage.getProjectsFolder(), "${local.id}.ofam").exists())
        } finally { state.shutdown() }
    }

    @Test fun closedProtectedCopyIsUnlockedForReviewBeforeAnyReplacement() {
        val storage = DesktopStorageManager(folder.newFolder())
        val file = storage.saveProjectLocally(local.copy(isPasswordProtected = true), "local-password")
        val before = file.readBytes()
        val state = DesktopAppState(storage)
        try {
            state.importFile(incoming())
            val prompt = state.dialog as AppDialog.LocalReplacementPassword
            assertTrue(prompt.compareBeforeReplace)
            state.acceptIncomingWithLocalPassword(prompt.pkg, prompt.incomingPassword, "wrong")
            assertTrue((state.dialog as AppDialog.LocalReplacementPassword).wrongPassword)
            state.acceptIncomingWithLocalPassword(prompt.pkg, prompt.incomingPassword, "local-password")
            val review = state.dialog as AppDialog.Compare
            assertEquals(ComparisonStatus.OLDER_REVISION, review.comparison.status)
            assertNull(state.project)
            assertArrayEquals(before, file.readBytes())
            state.dismissDialog()
            assertArrayEquals(before, file.readBytes())
            assertFalse(storage.ownsProjectLock(local.id))
        } finally { state.shutdown() }
    }

    @Test fun closedCopyMergePreservesLocalPasswordAndDoesNotChangeTheOtherProject() {
        val storage = DesktopStorageManager(folder.newFolder())
        val protected = local.copy(isPasswordProtected = true)
        val localFile = storage.saveProjectLocally(protected, "local-password")
        storage.saveSyncBase(protected, "local-password")
        val otherFile = storage.saveProjectLocally(other)
        val state = DesktopAppState(storage)
        try {
            state.openStored(otherFile)
            val otherBytes = otherFile.readBytes()
            state.importFile(incoming(local.copy(name = "Merged B", updatedEpochMs = 300)))
            val prompt = state.dialog as AppDialog.LocalReplacementPassword
            state.acceptIncomingWithLocalPassword(prompt.pkg, null, "local-password")
            val review = state.dialog as AppDialog.Compare
            state.startMerge(review.pkg)
            assertNull(state.dialog)
            assertNull(state.error)
            assertEquals("Merged B", state.project?.name)
            assertTrue(state.hasPassword)
            assertArrayEquals(otherBytes, otherFile.readBytes())
            storage.importPackageFromFile(localFile, "local-password").pkg!!.use { assertEquals("Merged B", it.project.name) }
            assertNull(storage.importPackageFromFile(localFile).pkg)
        } finally { state.shutdown() }
    }
}
