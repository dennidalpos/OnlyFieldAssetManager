package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceMergeDataTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun powerRestoreRefusalRetainsRowsVerifierBaseTrashAndMediaThenAllowsRetry() = runBlocking {
        for (branched in listOf(false, true)) {
            val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
            try {
                val root = folder.newFolder(); val repo = ProjectRepository(db, root, "dummy-recovery-key")
                val deleted = Device(technicalName = "Deleted"); val other = Device(technicalName = "Other"); val downstream = Device(technicalName = "Downstream")
                val firstSource = Device(technicalName = "First source"); val secondSource = Device(technicalName = "Second source")
                val originalFeeds = (if (branched) listOf(PowerFeed(deviceId = downstream.id, feedName = "B", sourceDeviceId = secondSource.id)) else emptyList()) +
                    listOf(PowerFeed(deviceId = deleted.id, feedName = "A", sourceDeviceId = other.id), PowerFeed(deviceId = downstream.id, feedName = "A", sourceDeviceId = deleted.id))
                val att = Attachment(name = "Photo", originalFileName = "photo.bin", relativePath = "", targetType = AttachmentTargetType.DEVICE, targetId = deleted.id)
                val project = Project(name = "Restore power", createdEpochMs = 0, updatedEpochMs = 0,
                    sites = listOf(Site(name = "Site", devices = listOf(deleted, other, downstream, firstSource, secondSource))), powerFeeds = originalFeeds, attachments = listOf(att))
                repo.saveProject(project); repo.setProjectPassword(project.id, null, "local-password")
                val file = AttachmentFiles.localFile(root, project.id, att).apply { parentFile!!.mkdirs(); writeText("keep bytes") }
                repo.moveToTrash(project.id, "DEVICE", deleted.id)
                val removed = repo.getProjectById(project.id)!!
                val changed = removed.copy(powerFeeds = removed.powerFeeds +
                    (if (branched) listOf(PowerFeed(deviceId = other.id, feedName = "B", sourceDeviceId = firstSource.id)) else emptyList()) +
                    PowerFeed(deviceId = other.id, feedName = "A", sourceDeviceId = downstream.id))
                repo.saveProject(changed); repo.exportProjectPackage(project.id, "local-password")
                val base = repo.getSyncBase(project.id); val trash = repo.getTrashItems(project.id)
                assertTrue(runCatching { repo.restoreFromTrash(project.id, trash.single().id) }.exceptionOrNull() is IllegalStateException)
                assertEquals(changed, repo.getProjectById(project.id)); assertEquals(base, repo.getSyncBase(project.id)); assertEquals(trash, repo.getTrashItems(project.id))
                assertTrue(repo.verifyProjectPassword(project.id, "local-password")); assertEquals("keep bytes", file.readText())
                repo.saveProject(changed.copy(powerFeeds = changed.powerFeeds.filterNot { it.deviceId == other.id && it.sourceDeviceId == downstream.id }))
                assertTrue(repo.restoreFromTrash(project.id, trash.single().id))
                val reopened = ProjectRepository(db, root, "dummy-recovery-key")
                assertTrue(reopened.getProjectById(project.id)!!.powerFeeds.containsAll(originalFeeds)); assertTrue(reopened.getTrashItems(project.id).isEmpty())
                assertTrue(reopened.verifyProjectPassword(project.id, "local-password")); assertEquals("keep bytes", file.readText())
            } finally { db.close() }
        }
    }

    @Test fun allChoicesPersistReopenExportAndRestoreWithoutLosingSecretsOrMedia() = runBlocking {
        for (mask in 0..15) {
            val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
            try {
                val root = folder.newFolder()
                val repo = ProjectRepository(db, root, "dummy-recovery-key")
                val survivor = Device(technicalName = "Survivor"); val duplicate = Device(technicalName = "Duplicate")
                val att = Attachment(name = "Configuration", originalFileName = "config.txt", relativePath = "", targetType = AttachmentTargetType.DEVICE, targetId = duplicate.id)
                val before = Project(name = "Merge", createdEpochMs = 0, updatedEpochMs = 0,
                    sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate))), attachments = listOf(att),
                    credentials = listOf(Credential(deviceId = duplicate.id, username = "admin", secret = "dummy-secret")),
                    deviceConfigurations = listOf(DeviceConfiguration(deviceId = duplicate.id, title = "Config", attachmentId = att.id)),
                    powerFeeds = listOf(PowerFeed(deviceId = duplicate.id, feedName = "A")),
                    customExtraFields = listOf(CustomExtraField(targetType = "DEVICE", targetId = duplicate.id, fieldKey = "private", fieldValue = "dummy-private", classification = AttachmentClassification.CONFIDENTIAL)))
                repo.saveProject(before); repo.setProjectPassword(before.id, null, "local-password")
                val local = repo.getProjectById(before.id)!!
                val file = AttachmentFiles.localFile(root, before.id, att).apply { requireNotNull(parentFile).mkdirs(); writeText("configuration bytes") }
                val choices = MergeDataChoices(mergePorts = false, mergeCredentials = mask and 1 != 0, mergeConfigurations = mask and 2 != 0, mergePowerFeeds = mask and 4 != 0, mergeExtraFields = mask and 8 != 0)
                val expected = ProjectEdits.mergeDevices(local, survivor.id, duplicate.id, choices).first
                repo.mergeDevices(before.id, survivor.id, duplicate.id, choices)
                repo.collectMedia(before.id)
                val reopened = ProjectRepository(db, root, "dummy-recovery-key")
                val actual = reopened.getProjectById(before.id)!!
                assertEquals(expected.credentials, actual.credentials); assertEquals(expected.deviceConfigurations, actual.deviceConfigurations)
                assertEquals(expected.powerFeeds, actual.powerFeeds); assertEquals(expected.customExtraFields, actual.customExtraFields)
                val trash = reopened.getTrashItems(before.id).single()
                assertTrue(reopened.verifyProjectPassword(before.id, "local-password"))
                PackageSerializer.importPackage(reopened.exportProjectPackage(before.id, "local-password")!!, "local-password").pkg!!.use { exported ->
                    assertEquals(expected.credentials, exported.project.credentials)
                    assertEquals(choices.mergeConfigurations, exported.attachments.containsKey(AttachmentFiles.entryName(att)))
                }
                assertTrue(reopened.restoreFromTrash(before.id, trash.id))
                val restored = reopened.getProjectById(before.id)!!
                val expectedRestored = ProjectEdits.restoreFromTrash(actual, trash)
                assertEquals(expectedRestored.credentials, restored.credentials); assertEquals(expectedRestored.deviceConfigurations, restored.deviceConfigurations)
                assertEquals(expectedRestored.powerFeeds, restored.powerFeeds); assertEquals(expectedRestored.customExtraFields, restored.customExtraFields)
                assertEquals("configuration bytes", file.readText()); assertTrue(reopened.getTrashItems(before.id).isEmpty())
            } finally { db.close() }
        }
    }

    @Test fun cycleRefusalPreservesRowsTrashVerifierBaseAndMediaThenAllowsRetry() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val root = folder.newFolder(); val repo = ProjectRepository(db, root, "dummy-recovery-key")
            val survivor = Device(technicalName = "Survivor"); val duplicate = Device(technicalName = "Duplicate")
            val att = Attachment(name = "Photo", originalFileName = "photo.bin", relativePath = "")
            val feed = PowerFeed(deviceId = duplicate.id, feedName = "A", sourceDeviceId = survivor.id)
            val project = Project(name = "Cycle", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate))), powerFeeds = listOf(feed), attachments = listOf(att))
            repo.saveProject(project); repo.setProjectPassword(project.id, null, "local-password")
            val before = repo.getProjectById(project.id)!!
            val file = AttachmentFiles.localFile(root, project.id, att).apply { requireNotNull(parentFile).mkdirs(); writeText("keep bytes") }
            repo.exportProjectPackage(project.id, "local-password"); val base = repo.getSyncBase(project.id)
            assertNotNull(runCatching { repo.mergeDevices(project.id, survivor.id, duplicate.id, MergeDataChoices()) }.exceptionOrNull())
            assertEquals(before, repo.getProjectById(project.id)); assertEquals(base, repo.getSyncBase(project.id))
            assertTrue(repo.getTrashItems(project.id).isEmpty()); assertTrue(repo.verifyProjectPassword(project.id, "local-password"))
            assertEquals("keep bytes", file.readText())
            repo.saveProject(before.copy(powerFeeds = listOf(feed.copy(sourceDeviceId = null))))
            assertNotNull(repo.mergeDevices(project.id, survivor.id, duplicate.id, MergeDataChoices()))
            assertEquals(survivor.id, repo.getProjectById(project.id)!!.powerFeeds.single().deviceId)
        } finally { db.close() }
    }
}
