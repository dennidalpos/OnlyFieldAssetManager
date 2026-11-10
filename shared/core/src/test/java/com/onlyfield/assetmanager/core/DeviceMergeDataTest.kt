package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ModelValidator
import org.junit.Assert.*
import org.junit.Test

class DeviceMergeDataTest {
    @Test fun restoringPowerFeedsRejectsNewCyclesWithMultipleSourcesAndAllowsRetry() {
        for (branched in listOf(false, true)) {
            val deleted = Device(technicalName = "Deleted"); val other = Device(technicalName = "Other"); val downstream = Device(technicalName = "Downstream")
            val firstSource = Device(technicalName = "First source"); val secondSource = Device(technicalName = "Second source")
            val originalFeeds = (if (branched) listOf(PowerFeed(deviceId = downstream.id, feedName = "B", sourceDeviceId = secondSource.id)) else emptyList()) +
                listOf(PowerFeed(deviceId = deleted.id, feedName = "A", sourceDeviceId = other.id), PowerFeed(deviceId = downstream.id, feedName = "A", sourceDeviceId = deleted.id))
            val before = Project(name = "Restore power", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Site", devices = listOf(deleted, other, downstream, firstSource, secondSource))), powerFeeds = originalFeeds)
            assertTrue(ModelValidator.validateProject(before).isValid)
            val (removed, trash) = ProjectEdits.deleteDeviceToTrash(before, deleted.id)
            val changed = removed.copy(powerFeeds = removed.powerFeeds +
                (if (branched) listOf(PowerFeed(deviceId = other.id, feedName = "B", sourceDeviceId = firstSource.id)) else emptyList()) +
                PowerFeed(deviceId = other.id, feedName = "A", sourceDeviceId = downstream.id))
            assertTrue(ModelValidator.validateProject(changed).isValid)
            assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(changed, trash!!) }
            val corrected = changed.copy(powerFeeds = changed.powerFeeds.filterNot { it.deviceId == other.id && it.sourceDeviceId == downstream.id })
            val restored = ProjectEdits.restoreFromTrash(corrected, trash!!)
            assertTrue(ModelValidator.validateProject(restored).isValid)
            assertTrue(restored.powerFeeds.containsAll(originalFeeds))
        }
    }

    @Test fun transferredConfigurationRetainsItsPortAttachmentWithOrWithoutPortCopy() {
        val survivor = Device(technicalName = "Survivor")
        val original = Device(technicalName = "Duplicate")
        val port = Port(deviceId = original.id, name = "1")
        val duplicate = original.copy(ports = listOf(port))
        val att = Attachment(name = "Config", originalFileName = "config.txt", relativePath = "", targetType = AttachmentTargetType.PORT,
            targetId = port.id, classification = AttachmentClassification.CONFIDENTIAL)
        val config = DeviceConfiguration(deviceId = duplicate.id, title = "Running", attachmentId = att.id)
        val before = Project(name = "Port config", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate))), attachments = listOf(att), deviceConfigurations = listOf(config))
        for (copyPorts in listOf(false, true)) {
            val (merged, trash) = ProjectEdits.mergeDevices(before, survivor.id, duplicate.id, MergeDataChoices(mergePorts = copyPorts))
            val device = merged.sites.single().devices.single()
            assertEquals(config.copy(deviceId = survivor.id), merged.deviceConfigurations.single())
            assertEquals(att.copy(targetType = if (copyPorts) AttachmentTargetType.PORT else AttachmentTargetType.DEVICE,
                targetId = if (copyPorts) device.ports.single().id else survivor.id), merged.attachments.single())
            assertTrue(ModelValidator.validateProject(merged).isValid)
            assertNotNull(trash)
        }
    }

    @Test fun powerMergeRejectsSelfIndirectAndBranchedCyclesThenAllowsRetry() {
        for (shape in listOf("self", "indirect", "branched")) {
            val survivor = Device(technicalName = "Survivor"); val duplicate = Device(technicalName = "Duplicate")
            val middle = Device(technicalName = "Middle"); val upstream = Device(technicalName = "Upstream")
            val feeds = if (shape == "self") listOf(PowerFeed(deviceId = duplicate.id, feedName = "A", sourceDeviceId = survivor.id))
                else listOf(PowerFeed(deviceId = duplicate.id, feedName = "A", sourceDeviceId = middle.id)) +
                    (if (shape == "branched") listOf(PowerFeed(deviceId = middle.id, feedName = "B", sourceDeviceId = upstream.id)) else emptyList()) +
                    listOf(PowerFeed(deviceId = middle.id, feedName = "A", sourceDeviceId = survivor.id))
            val before = Project(name = "Power cycle", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate, middle, upstream))), powerFeeds = feeds)
            assertTrue(ModelValidator.validateProject(before).isValid)
            assertThrows(IllegalStateException::class.java) { ProjectEdits.mergeDevices(before, survivor.id, duplicate.id, MergeDataChoices()) }
            assertEquals(feeds, before.powerFeeds)
            val corrected = before.copy(powerFeeds = feeds.map { it.copy(sourceDeviceId = null) })
            val (merged, trash) = ProjectEdits.mergeDevices(corrected, survivor.id, duplicate.id, MergeDataChoices())
            assertNotNull(trash); assertTrue(ModelValidator.validateProject(merged).isValid)
        }
    }

    @Test fun duplicateAsPowerSourceTransfersOrStaysWithItsRecoverableFeed() {
        val survivor = Device(technicalName = "Survivor")
        val duplicate = Device(technicalName = "Duplicate")
        val other = Device(technicalName = "Other")
        val feed = PowerFeed(deviceId = other.id, feedName = "A", sourceDeviceId = duplicate.id)
        val before = Project(name = "Power", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate, other))), powerFeeds = listOf(feed))
        for (transfer in listOf(false, true)) {
            val (merged, trash) = ProjectEdits.mergeDevices(before, survivor.id, duplicate.id, MergeDataChoices(mergePowerFeeds = transfer))
            assertEquals(if (transfer) listOf(feed.copy(sourceDeviceId = survivor.id)) else emptyList(), merged.powerFeeds)
            assertTrue(ModelValidator.validateProject(merged).isValid)
            val restored = ProjectEdits.restoreFromTrash(merged, trash!!)
            assertEquals(if (transfer) listOf(feed.copy(sourceDeviceId = survivor.id)) else listOf(feed), restored.powerFeeds)
        }
    }

    @Test fun restoringAssociatedDataNeverOverwritesAReusedIdOrMissingContext() {
        val device = Device(technicalName = "Device")
        val att = Attachment(name = "Config", originalFileName = "config.txt", relativePath = "")
        val credential = Credential(deviceId = device.id, username = "admin", secret = "dummy-secret")
        val config = DeviceConfiguration(deviceId = device.id, title = "Running", attachmentId = att.id)
        val before = Project(name = "Restore", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(Site(name = "Site", devices = listOf(device))),
            attachments = listOf(att), credentials = listOf(credential), deviceConfigurations = listOf(config))
        val (deleted, trash) = ProjectEdits.deleteDeviceToTrash(before, device.id)
        val reused = deleted.copy(credentials = listOf(credential.copy(deviceId = null, secret = "other-dummy-secret")))
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(reused, trash!!) }
        assertEquals("other-dummy-secret", reused.credentials.single().secret)
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(deleted.copy(attachments = emptyList()), trash!!) }
        assertEquals(before.credentials, ProjectEdits.restoreFromTrash(deleted, trash!!).credentials)
    }

    @Test fun eachChoicePreservesIdsConflictsClassificationAndRestorableData() {
        for (mask in 0..15) {
            val survivor = Device(technicalName = "Survivor")
            val duplicate = Device(technicalName = "Duplicate")
            val credential = Credential(deviceId = duplicate.id, username = "admin", secret = "dummy-duplicate-secret", type = CredentialType.PASSWORD, notes = "Keep")
            val config = DeviceConfiguration(deviceId = duplicate.id, title = "Running", configText = "dummy-private-config", capturedEpochMs = 1, notes = "Keep")
            val feed = PowerFeed(deviceId = duplicate.id, feedName = "A", sourceDeviceId = null, loadWatts = 10.0, notes = "Keep")
            val extra = CustomExtraField(targetType = "DEVICE", targetId = duplicate.id, fieldKey = "private", fieldValue = "dummy-value", classification = AttachmentClassification.CONFIDENTIAL, notes = "Keep")
            val survivorCredential = credential.copy(id = java.util.UUID.randomUUID().toString(), deviceId = survivor.id, secret = "dummy-survivor-secret")
            val survivorConfig = config.copy(id = java.util.UUID.randomUUID().toString(), deviceId = survivor.id, configText = "survivor config")
            val survivorFeed = feed.copy(id = java.util.UUID.randomUUID().toString(), deviceId = survivor.id, loadWatts = 20.0)
            val survivorExtra = extra.copy(id = java.util.UUID.randomUUID().toString(), targetId = survivor.id, fieldValue = "survivor value")
            val rackField = extra.copy(id = java.util.UUID.randomUUID().toString(), targetType = "PROJECT", targetId = "other")
            val before = Project(name = "Merge", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate))),
                credentials = listOf(survivorCredential, credential), deviceConfigurations = listOf(survivorConfig, config),
                powerFeeds = listOf(survivorFeed, feed), customExtraFields = listOf(survivorExtra, extra, rackField))
            val choices = MergeDataChoices(mergePorts = false, mergeCredentials = mask and 1 != 0, mergeConfigurations = mask and 2 != 0,
                mergePowerFeeds = mask and 4 != 0, mergeExtraFields = mask and 8 != 0)
            val (merged, trash) = ProjectEdits.mergeDevices(before, survivor.id, duplicate.id, choices)
            assertNotNull(trash)
            assertEquals(listOf(survivorCredential) + if (choices.mergeCredentials) listOf(credential.copy(deviceId = survivor.id)) else emptyList(), merged.credentials)
            assertEquals(listOf(survivorConfig) + if (choices.mergeConfigurations) listOf(config.copy(deviceId = survivor.id)) else emptyList(), merged.deviceConfigurations)
            assertEquals(listOf(survivorFeed) + if (choices.mergePowerFeeds) listOf(feed.copy(deviceId = survivor.id)) else emptyList(), merged.powerFeeds)
            assertEquals(listOf(survivorExtra) + if (choices.mergeExtraFields) listOf(extra.copy(targetId = survivor.id), rackField) else listOf(rackField), merged.customExtraFields)
            assertTrue(ModelValidator.validateProject(merged).isValid)
            val restored = ProjectEdits.restoreFromTrash(merged, trash!!)
            assertEquals(before.credentials.map { if (choices.mergeCredentials && it.id == credential.id) it.copy(deviceId = survivor.id) else it }.toSet(), restored.credentials.toSet())
            assertEquals(before.deviceConfigurations.map { if (choices.mergeConfigurations && it.id == config.id) it.copy(deviceId = survivor.id) else it }.toSet(), restored.deviceConfigurations.toSet())
            assertEquals(before.powerFeeds.map { if (choices.mergePowerFeeds && it.id == feed.id) it.copy(deviceId = survivor.id) else it }.toSet(), restored.powerFeeds.toSet())
            assertEquals(before.customExtraFields.map { if (choices.mergeExtraFields && it.id == extra.id) it.copy(targetId = survivor.id) else it }.toSet(), restored.customExtraFields.toSet())
        }
    }
}
