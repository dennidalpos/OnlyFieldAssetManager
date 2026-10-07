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

    @Test fun deviceRestoreRejectsMissingOrRelocatedFloor() {
        val area = Area(name = "Floor")
        val located = device.copy(areaId = area.id)
        val original = project.copy(sites = listOf(site.copy(areas = listOf(area), devices = listOf(located))))
        val (deleted, item) = ProjectEdits.deleteDeviceToTrash(original, device.id)
        for (changed in listOf(
            deleted.copy(sites = listOf(site.copy(devices = emptyList()))),
            deleted.copy(sites = listOf(site.copy(devices = emptyList()), Site(name = "Other", areas = listOf(area))))
        )) {
            assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(changed, item!!) }
        }
        assertEquals(located, ProjectEdits.restoreFromTrash(deleted, item!!).sites.single().devices.single())
    }

    @Test fun deviceRestoreRejectsActiveAndRepeatedPortIds() {
        val port = Port(deviceId = device.id, name = "P1")
        val original = project.copy(sites = listOf(site.copy(devices = listOf(device.copy(ports = listOf(port))))))
        val (deleted, item) = ProjectEdits.deleteDeviceToTrash(original, device.id)
        val other = Device(technicalName = "Active")
        val collision = deleted.copy(sites = listOf(site.copy(devices = listOf(other.copy(ports = listOf(port.copy(deviceId = other.id)))))))
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(collision, item!!) }
        val repeated = item!!.copy(serializedJson = Json.encodeToString(Device.serializer(), device.copy(ports = listOf(port, port))))
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(deleted, repeated) }
        assertEquals(listOf(port), ProjectEdits.restoreFromTrash(deleted, item).sites.single().devices.single().ports)
    }

    @Test fun placementRestoreRejectsMissingFloorAndReusedIds() {
        val area = Area(name = "Floor")
        val placement = FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE,
            targetId = device.id, xRatio = .2f, yRatio = .3f)
        val original = project.copy(sites = listOf(site.copy(areas = listOf(area))), floorplanPlacements = listOf(placement))
        val (deleted, item) = ProjectEdits.deleteDeviceToTrash(original, device.id)
        assertThrows(IllegalStateException::class.java) {
            ProjectEdits.restoreFromTrash(deleted.copy(sites = listOf(site.copy(devices = emptyList()))), item!!)
        }
        val collision = deleted.copy(floorplanPlacements = listOf(placement.copy(targetId = "other")))
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(collision, item!!) }
        assertEquals(listOf(placement), ProjectEdits.restoreFromTrash(deleted, item!!).floorplanPlacements)
    }

    @Test fun hierarchyRestoreRejectsMissingContainerOrChild() {
        val rack = Rack(name = "Rack")
        val mounted = device.copy(rackId = rack.id)
        val original = project.copy(sites = listOf(site.copy(devices = listOf(mounted))), racks = listOf(rack))
        val (deleted, item) = ProjectEdits.deleteDeviceToTrash(original, device.id)
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(deleted.copy(racks = emptyList()), item!!) }
        val (withoutRack, rackItem) = ProjectEdits.deleteRackToTrash(original, rack.id)
        val missingChild = withoutRack.copy(sites = listOf(site.copy(devices = emptyList())))
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(missingChild, rackItem!!) }
        assertEquals(rack.id, ProjectEdits.restoreFromTrash(deleted, item!!).sites.single().devices.single().rackId)
    }

    @Test fun rackRestoreRejectsFloorMovedToAnotherSiteEvenIfOriginalSiteRemains() {
        val area = Area(name = "Floor")
        val rack = Rack(name = "Rack", areaId = area.id)
        val original = project.copy(sites = listOf(site.copy(areas = listOf(area))), racks = listOf(rack))
        val (deleted, item) = ProjectEdits.deleteRackToTrash(original, rack.id)
        val moved = deleted.copy(sites = listOf(site, Site(name = "Other", areas = listOf(area))))
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(moved, item!!) }
    }

    @Test fun containerRestoreDoesNotUndoAnExplicitMoveOfItsReleasedChild() {
        val rack = Rack(name = "Original rack")
        val otherRack = Rack(name = "Other rack")
        val original = project.copy(sites = listOf(site.copy(devices = listOf(device.copy(rackId = rack.id)))), racks = listOf(rack, otherRack))
        val (deleted, item) = ProjectEdits.deleteRackToTrash(original, rack.id)
        val moved = ObjectHierarchy.assign(deleted, ObjectRef(PlacementTargetType.DEVICE, device.id), ObjectRef(PlacementTargetType.RACK, otherRack.id))
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(moved, item!!) }
        assertEquals(otherRack.id, moved.sites.single().devices.single().rackId)
    }

    @Test fun restoreDoesNotFollowItsContainerToAnotherFloor() {
        val area = Area(name = "Original floor")
        val otherArea = Area(name = "Other floor")
        val rack = Rack(name = "Rack", areaId = area.id)
        val original = project.copy(sites = listOf(site.copy(areas = listOf(area, otherArea),
            devices = listOf(device.copy(areaId = area.id, rackId = rack.id)))), racks = listOf(rack))
        val (deleted, item) = ProjectEdits.deleteDeviceToTrash(original, device.id)
        val moved = ProjectEdits.updateRack(deleted, rack.copy(areaId = otherArea.id))
        assertThrows(IllegalStateException::class.java) { ProjectEdits.restoreFromTrash(moved, item!!) }
    }

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
