package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceOperationsTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = ProjectRepository(db)
    }

    @After fun tearDown() = db.close()

    private fun fixture(): Project {
        val area = Area(id = "area", name = "Floor")
        val rack = Rack(id = "rack", name = "Rack", areaId = area.id)
        val type = ObjectType(id = "box-type", name = "Box", canContainObjects = true)
        val old = Device(id = "old", technicalName = "Old", areaId = area.id, rackId = rack.id,
            positionU = 12, heightU = 3, mountingType = MountingType.SHELF_MOUNT, rackSide = RackSide.REAR,
            serialNumber = "hidden-serial", ipAddress = "10.0.0.1", objectTypeId = type.id,
            observation = Observation("Survey", 1, ObservationStatus.CONFLICT),
            ports = listOf(Port(id = "old-port", deviceId = "old", name = "P1", label = "Rear")))
        val survivor = Device(id = "survivor", technicalName = "Survivor", areaId = area.id, rackId = rack.id,
            positionU = 4, heightU = 2, mountingType = MountingType.RACK_MOUNT, serialNumber = "keep-hidden",
            ports = listOf(Port(id = "survivor-port", deviceId = "survivor", name = "P2")))
        val child = Device(id = "child", technicalName = "Child", areaId = area.id, rackId = rack.id)
        return ObjectHierarchy.assign(Project(id = "project", name = "Device operations", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(id = "site", name = "Site", areas = listOf(area), devices = listOf(old, survivor, child))),
            racks = listOf(rack), objectTypes = listOf(type),
            cables = listOf(Cable(id = "cable", portAId = "old-port", portBId = "survivor-port"))),
            ObjectRef(PlacementTargetType.DEVICE, child.id), ObjectRef(PlacementTargetType.DEVICE, old.id))
    }

    @Test fun replacementMatchesCoreAndRestoresPreviousHierarchy() = runBlocking {
        repository.saveProject(fixture())
        val before = repository.getProjectById("project")!!
        val (expected, expectedTrash) = ProjectEdits.replaceDevice(before, "old", "New", DeviceCategory.NETWORK_SWITCH)
        val (trash, replacement) = repository.replaceDevice("project", "old", "New", DeviceCategory.NETWORK_SWITCH)
        val actual = repository.getProjectById("project")!!
        val expectedDevice = expected.sites.single().devices.single { it.technicalName == "New" }
        assertEquals(expectedDevice, replacement.copy(id = expectedDevice.id))
        assertEquals(replacement, actual.sites.single().devices.single { it.id == replacement.id })
        assertEquals(expected.sites.single().devices.filterNot { it.id == expectedDevice.id },
            actual.sites.single().devices.filterNot { it.id == replacement.id })
        assertEquals(ObjectHierarchy.parent(expected, ObjectRef(PlacementTargetType.DEVICE, expectedDevice.id)),
            ObjectHierarchy.parent(actual, ObjectRef(PlacementTargetType.DEVICE, replacement.id)))
        assertEquals(expectedTrash!!.serializedJson, trash!!.serializedJson)
        assertEquals(expectedTrash.containments, trash.containments)
        assertEquals(expectedTrash.mountSnapshots, trash.mountSnapshots)
        assertEquals(expected.cables.single().copy(observation = actual.cables.single().observation), actual.cables.single())
        assertEquals(ObservationStatus.TO_VERIFY, actual.cables.single().observation!!.status)
        assertEquals(ObjectRef(PlacementTargetType.RACK, "rack"),
            ObjectHierarchy.parent(actual, ObjectRef(PlacementTargetType.DEVICE, "child")))
        assertTrue(repository.restoreFromTrash("project", trash.id))
        val restored = repository.getProjectById("project")!!
        assertEquals(before.sites.single().devices.first { it.id == "old" }, restored.sites.single().devices.first { it.id == "old" })
        assertEquals(ObjectRef(PlacementTargetType.DEVICE, "old"),
            ObjectHierarchy.parent(restored, ObjectRef(PlacementTargetType.DEVICE, "child")))
    }

    @Test fun mergeMatchesCoreForFieldsPortsConnectionsAndContainment() = runBlocking {
        for (mergePorts in listOf(false, true)) {
            repository.saveProject(fixture())
            repository.emptyTrash("project")
            val before = repository.getProjectById("project")!!
            val choices = MergeDataChoices(useTechnicalNameFromDuplicate = true, useIpFromDuplicate = true, mergePorts = mergePorts)
            val (expected, expectedTrash) = ProjectEdits.mergeDevices(before, "survivor", "old", choices)
            val merged = repository.mergeDevices("project", "survivor", "old", choices)!!
            val actual = repository.getProjectById("project")!!
            val expectedDevice = expected.sites.single().devices.first { it.id == "survivor" }
            assertEquals(expectedDevice, merged.copy(ports = merged.ports.zip(expectedDevice.ports).map { (port, reference) -> port.copy(id = reference.id) }))
            assertEquals(expectedDevice.ports.size, merged.ports.size)
            assertEquals(merged, actual.sites.single().devices.first { it.id == "survivor" })
            assertEquals(expected.objectContainments, actual.objectContainments)
            assertEquals(expectedTrash!!.serializedJson, repository.getTrashItems("project").single().serializedJson)
            assertEquals(expected.cables.single().copy(observation = actual.cables.single().observation), actual.cables.single())
            assertNull(actual.cables.single().portAId)
            assertEquals("survivor-port", actual.cables.single().portBId)
            assertEquals(ObservationStatus.TO_VERIFY, actual.cables.single().observation!!.status)
        }
    }

    @Test fun failedReplacementOrMergePreservesWholeProjectAndTrashThenAllowsRetry() = runBlocking {
        for (merge in listOf(false, true)) for (table in listOf("devices", "trash_items")) {
            repository.saveProject(fixture())
            repository.emptyTrash("project")
            repository.setProjectPassword("project", null, "local-password")
            val before = repository.getProjectById("project")!!
            val condition = if (table == "devices") "WHEN NEW.technicalName = '${if (merge) "Old" else "New"}'" else ""
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_edit BEFORE INSERT ON $table $condition BEGIN SELECT RAISE(ABORT, 'injected device edit failure'); END")
            suspend fun edit() {
                if (merge) repository.mergeDevices("project", "survivor", "old", MergeDataChoices(useTechnicalNameFromDuplicate = true))
                else repository.replaceDevice("project", "old", "New", DeviceCategory.NETWORK_SWITCH)
            }
            val failure = runCatching { edit() }.exceptionOrNull()
            assertNotNull(failure)
            assertTrue(failure!!.message.orEmpty().contains("injected device edit failure"))
            assertEquals(before, repository.getProjectById("project"))
            assertTrue(repository.getTrashItems("project").isEmpty())
            assertTrue(repository.verifyProjectPassword("project", "local-password"))
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_edit")
            edit()
            assertFalse(repository.getProjectById("project")!!.sites.single().devices.any { it.id == "old" })
            assertEquals(1, repository.getTrashItems("project").size)
        }
    }
}
