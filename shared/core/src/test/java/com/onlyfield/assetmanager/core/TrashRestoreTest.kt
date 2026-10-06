package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TrashRestoreTest {
    private val device = Device(technicalName = "Recoverable")
    private val site = Site(name = "Original", devices = listOf(device))
    private val project = Project(name = "Restore", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(site))

    @Test fun missingOriginalSiteNeverRelocatesTheDeviceToAnotherSite() {
        val (deleted, item) = ProjectEdits.deleteDeviceToTrash(project, device.id)
        val changed = deleted.copy(sites = listOf(Site(name = "Other")))
        assertTrue(runCatching { ProjectEdits.restoreFromTrash(changed, item!!) }.exceptionOrNull() is IllegalStateException)
        assertTrue(changed.sites.single().devices.isEmpty())
        val restored = ProjectEdits.restoreFromTrash(deleted, item!!)
        assertEquals(device, restored.sites.single().devices.single())
    }

    @Test fun unsupportedTrashTypesFailInsteadOfReturningTheSameProject() {
        for (type in listOf("ATTACHMENT", "UNKNOWN")) {
            val item = TrashItem(projectId = project.id, itemType = type, itemId = "entry", displayName = "Entry", serializedJson = "{}")
            assertTrue(runCatching { ProjectEdits.restoreFromTrash(project, item) }.exceptionOrNull() is IllegalStateException)
        }
    }

    @Test fun credentialRestorePreservesSecretAndRejectsAnExistingId() {
        val credential = Credential(username = "Dummy", secret = "dummy-test-secret")
        val item = TrashItem(projectId = project.id, itemType = "CREDENTIAL", itemId = credential.id, displayName = "Dummy",
            serializedJson = Json.encodeToString(Credential.serializer(), credential))
        val restored = ProjectEdits.restoreFromTrash(project, item)
        assertEquals(credential, restored.credentials.single())
        val existing = project.copy(credentials = listOf(credential.copy(secret = "other-dummy-test-secret")))
        assertTrue(runCatching { ProjectEdits.restoreFromTrash(existing, item) }.exceptionOrNull() is IllegalStateException)
        assertEquals("other-dummy-test-secret", existing.credentials.single().secret)
    }

    @Test fun existingDeviceOrRackIdsCannotBeDuplicatedByRestore() {
        val (_, deviceItem) = ProjectEdits.deleteDeviceToTrash(project, device.id)
        assertTrue(runCatching { ProjectEdits.restoreFromTrash(project, deviceItem!!) }.exceptionOrNull() is IllegalStateException)
        val rack = Rack(name = "Rack")
        val withRack = project.copy(racks = listOf(rack))
        val (_, rackItem) = ProjectEdits.deleteRackToTrash(withRack, rack.id)
        assertTrue(runCatching { ProjectEdits.restoreFromTrash(withRack, rackItem!!) }.exceptionOrNull() is IllegalStateException)
    }

    @Test fun mismatchedProjectOrSerializedIdCannotBeRestored() {
        val (deleted, item) = ProjectEdits.deleteDeviceToTrash(project, device.id)
        for (invalid in listOf(item!!.copy(projectId = "different-project"), item.copy(itemId = "different-object"))) {
            assertTrue(runCatching { ProjectEdits.restoreFromTrash(deleted, invalid) }.exceptionOrNull() is IllegalStateException)
        }
    }

    @Test fun rackRestoreRequiresItsOriginalSiteAndAreaButUnplacedRacksRemainValid() {
        val area = Area(name = "Floor")
        val rack = Rack(name = "Rack", areaId = area.id)
        val original = project.copy(sites = listOf(site.copy(areas = listOf(area))), racks = listOf(rack))
        val (deleted, item) = ProjectEdits.deleteRackToTrash(original, rack.id)
        assertEquals(site.id, item!!.originalSiteId)
        val movedArea = deleted.copy(sites = listOf(Site(name = "Other", areas = listOf(area))))
        assertTrue(runCatching { ProjectEdits.restoreFromTrash(movedArea, item) }.exceptionOrNull() is IllegalStateException)
        val missingArea = deleted.copy(sites = listOf(site.copy(areas = emptyList())))
        assertTrue(runCatching { ProjectEdits.restoreFromTrash(missingArea, item) }.exceptionOrNull() is IllegalStateException)
        assertEquals(rack, ProjectEdits.restoreFromTrash(deleted, item).racks.single())
        val unplaced = rack.copy(areaId = null)
        val (empty, unplacedItem) = ProjectEdits.deleteRackToTrash(project.copy(racks = listOf(unplaced)), rack.id)
        assertEquals(unplaced, ProjectEdits.restoreFromTrash(empty, unplacedItem!!).racks.single())
    }
}
