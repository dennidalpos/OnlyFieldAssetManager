package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeviceMergeDataTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun powerRestoreRefusalRetainsCopyBaseTrashAndMediaThenAllowsRetry() {
        for (password in listOf(null, "local-password")) for (branched in listOf(false, true)) {
            val storage = DesktopStorageManager(folder.newFolder())
            val deleted = Device(technicalName = "Deleted"); val other = Device(technicalName = "Other"); val downstream = Device(technicalName = "Downstream")
            val firstSource = Device(technicalName = "First source"); val secondSource = Device(technicalName = "Second source")
            val originalFeeds = (if (branched) listOf(PowerFeed(deviceId = downstream.id, feedName = "B", sourceDeviceId = secondSource.id)) else emptyList()) +
                listOf(PowerFeed(deviceId = deleted.id, feedName = "A", sourceDeviceId = other.id), PowerFeed(deviceId = downstream.id, feedName = "A", sourceDeviceId = deleted.id))
            val att = Attachment(name = "Photo", originalFileName = "photo.bin", relativePath = "", targetType = AttachmentTargetType.DEVICE, targetId = deleted.id)
            val before = Project(name = "Restore power", createdEpochMs = 0, updatedEpochMs = 0, isPasswordProtected = password != null,
                sites = listOf(Site(name = "Site", devices = listOf(deleted, other, downstream, firstSource, secondSource))), powerFeeds = originalFeeds, attachments = listOf(att))
            storage.storeAttachmentBytes(before.id, att, "keep bytes".toByteArray())
            val main = storage.saveProjectLocally(before, password, syncBase = before)
            val state = DesktopAppState(storage)
            try {
                state.importFile(main, password, compare = false)
                val (removed, item) = ProjectEdits.deleteDeviceToTrash(state.project!!, deleted.id)
                val changed = removed.copy(powerFeeds = removed.powerFeeds +
                    (if (branched) listOf(PowerFeed(deviceId = other.id, feedName = "B", sourceDeviceId = firstSource.id)) else emptyList()) +
                    PowerFeed(deviceId = other.id, feedName = "A", sourceDeviceId = downstream.id))
                state.addToTrash(item!!); state.update(changed, "Change remaining feeds"); assertNull(state.error)
                val bytes = main.readBytes(); val base = storage.loadSyncBase(before.id, password); val trash = state.trash.toList()
                state.restoreTrash(item)
                assertNotNull(state.error); assertEquals(changed, state.project); assertEquals(trash, state.trash)
                assertArrayEquals(bytes, main.readBytes()); assertEquals(base, storage.loadSyncBase(before.id, password))
                assertEquals("keep bytes", String(storage.attachmentBytes(before.id, att)!!))
                state.update(changed.copy(powerFeeds = changed.powerFeeds.filterNot { it.deviceId == other.id && it.sourceDeviceId == downstream.id }), "Correct remaining feeds")
                state.restoreTrash(state.trash.single()); assertNull(state.error); assertTrue(state.trash.isEmpty())
                state.closeProject(); state.importFile(main, password, compare = false); assertNull(state.error)
                assertTrue(state.project!!.powerFeeds.containsAll(originalFeeds))
                assertEquals("keep bytes", String(storage.attachmentBytes(before.id, att)!!))
            } finally { state.shutdown() }
        }
    }

    @Test fun exportedTransferredConfigurationKeepsItsPortPayload() {
        for (copyPorts in listOf(false, true)) {
            val storage = DesktopStorageManager(folder.newFolder())
            val survivor = Device(technicalName = "Survivor"); val original = Device(technicalName = "Duplicate")
            val port = Port(deviceId = original.id, name = "1"); val duplicate = original.copy(ports = listOf(port))
            val att = Attachment(name = "Config", originalFileName = "config.txt", relativePath = "", targetType = AttachmentTargetType.PORT,
                targetId = port.id, classification = AttachmentClassification.CONFIDENTIAL)
            val before = Project(name = "Port config", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate))), attachments = listOf(att),
                deviceConfigurations = listOf(DeviceConfiguration(deviceId = duplicate.id, title = "Running", attachmentId = att.id)))
            storage.storeAttachmentBytes(before.id, att, "configuration bytes".toByteArray())
            val main = storage.saveProjectLocally(before)
            val state = DesktopAppState(storage)
            try {
                state.importFile(main, null, compare = false)
                assertTrue(state.mergeDevices(survivor.id, duplicate.id, MergeDataChoices(mergePorts = copyPorts)))
                val exported = folder.newFile()
                storage.exportPackageToFile(state.project!!, exported)
                PackageSerializer.importPackage(exported.readBytes()).pkg!!.use {
                    assertEquals(AttachmentClassification.CONFIDENTIAL, it.project.attachments.single().classification)
                    assertEquals("configuration bytes", String(it.attachments.getValue(AttachmentFiles.entryName(att))))
                }
            } finally { state.shutdown() }
        }
    }

    @Test fun allChoicesPersistReopenAndRestoreForPlainAndProtectedCopies() {
        for (password in listOf(null, "local-password")) for (mask in 0..15) {
            val root = folder.newFolder(); val storage = DesktopStorageManager(root)
            val survivor = Device(technicalName = "Survivor"); val duplicate = Device(technicalName = "Duplicate")
            val att = Attachment(name = "Configuration", originalFileName = "config.txt", relativePath = "", targetType = AttachmentTargetType.DEVICE, targetId = duplicate.id)
            val before = Project(name = "Merge", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate))), attachments = listOf(att), isPasswordProtected = password != null,
                credentials = listOf(Credential(deviceId = duplicate.id, username = "admin", secret = "dummy-secret")),
                deviceConfigurations = listOf(DeviceConfiguration(deviceId = duplicate.id, title = "Config", attachmentId = att.id)),
                powerFeeds = listOf(PowerFeed(deviceId = duplicate.id, feedName = "A")),
                customExtraFields = listOf(CustomExtraField(targetType = "DEVICE", targetId = duplicate.id, fieldKey = "private", fieldValue = "dummy-private", classification = AttachmentClassification.CONFIDENTIAL)))
            storage.storeAttachmentBytes(before.id, att, "configuration bytes".toByteArray())
            val main = storage.saveProjectLocally(before, password)
            val state = DesktopAppState(storage)
            try {
                state.importFile(main, password, compare = false)
                val choices = MergeDataChoices(mergePorts = false, mergeCredentials = mask and 1 != 0, mergeConfigurations = mask and 2 != 0, mergePowerFeeds = mask and 4 != 0, mergeExtraFields = mask and 8 != 0)
                val expectedMerge = ProjectEdits.mergeDevices(state.project!!, survivor.id, duplicate.id, choices).first
                assertTrue(state.mergeDevices(survivor.id, duplicate.id, choices)); assertNull(state.error)
                val merged = state.project!!; val trash = state.trash.single()
                assertEquals(expectedMerge.copy(updatedEpochMs = merged.updatedEpochMs), merged)
                state.closeProject(); state.importFile(main, password, compare = false)
                assertNull(state.error); assertEquals(merged, state.project)
                assertEquals(trash, state.trash.single())
                val exported = folder.newFile()
                storage.exportPackageToFile(state.project!!, exported, password)
                PackageSerializer.importPackage(exported.readBytes(), password).pkg!!.use {
                    assertEquals(merged.credentials, it.project.credentials)
                    assertEquals(choices.mergeConfigurations, it.attachments.containsKey(AttachmentFiles.entryName(att)))
                }
                state.restoreTrash(state.trash.single()); assertNull(state.error)
                val expected = ProjectEdits.restoreFromTrash(merged, trash)
                assertEquals(expected.credentials, state.project!!.credentials); assertEquals(expected.deviceConfigurations, state.project!!.deviceConfigurations)
                assertEquals(expected.powerFeeds, state.project!!.powerFeeds); assertEquals(expected.customExtraFields, state.project!!.customExtraFields)
                assertEquals("configuration bytes", String(storage.attachmentBytes(before.id, att)!!)); assertTrue(state.trash.isEmpty())
            } finally { state.shutdown() }
        }
    }

    @Test fun cycleRefusalPreservesWorkingCopyBaseTrashAndPasswordThenAllowsRetry() {
        for (password in listOf(null, "local-password")) {
            val storage = DesktopStorageManager(folder.newFolder())
            val survivor = Device(technicalName = "Survivor"); val duplicate = Device(technicalName = "Duplicate")
            val feed = PowerFeed(deviceId = duplicate.id, feedName = "A", sourceDeviceId = survivor.id)
            val before = Project(name = "Cycle", createdEpochMs = 0, updatedEpochMs = 0, isPasswordProtected = password != null,
                sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate))), powerFeeds = listOf(feed))
            val main = storage.saveProjectLocally(before, password, syncBase = before)
            val state = DesktopAppState(storage)
            try {
                state.importFile(main, password, compare = false)
                val bytes = main.readBytes(); val base = storage.loadSyncBase(before.id, password)
                assertFalse(state.mergeDevices(survivor.id, duplicate.id, MergeDataChoices()))
                assertNotNull(state.error); assertEquals(before, state.project); assertTrue(state.trash.isEmpty())
                assertArrayEquals(bytes, main.readBytes()); assertEquals(base, storage.loadSyncBase(before.id, password))
                state.update(before.copy(powerFeeds = listOf(feed.copy(sourceDeviceId = null))), "Correct source")
                assertTrue(state.mergeDevices(survivor.id, duplicate.id, MergeDataChoices()))
                assertNull(state.error); assertEquals(survivor.id, state.project!!.powerFeeds.single().deviceId)
            } finally { state.shutdown() }
        }
    }
}
