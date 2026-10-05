package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import org.junit.Assert.*
import org.junit.Test

class ObjectHierarchyTest {
    private val area = Area(name = "Terra")
    private val rack = Rack(name = "R1", areaId = area.id)
    private val boxType = ObjectType(name = "Scatola", canContainObjects = true)
    private val box = Device(technicalName = "BOX", areaId = area.id, rackId = rack.id, positionU = 10, objectTypeId = boxType.id)
    private val sw = Device(technicalName = "SW", areaId = area.id, rackId = rack.id, positionU = 3, ports = listOf(Port(deviceId = "unused", name = "p1")))
    private val outside = Device(technicalName = "AP", areaId = area.id)
    private val initial = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1,
        sites = listOf(Site(name = "BU", areas = listOf(area), devices = listOf(box, sw, outside))), racks = listOf(rack), objectTypes = listOf(boxType))
    private val rackRef = ObjectRef(PlacementTargetType.RACK, rack.id)
    private val boxRef = ObjectRef(PlacementTargetType.DEVICE, box.id)
    private val swRef = ObjectRef(PlacementTargetType.DEVICE, sw.id)

    @Test fun legacyRackIsCanonicalAndChildrenDisappearFromFloor() {
        val p = ObjectHierarchy.normalize(initial)
        assertEquals(2, p.objectContainments.size)
        assertEquals(setOf(rack.id, outside.id), ObjectMap.nodes(p, area.id).map { it.id }.toSet())
        assertEquals(3, p.sites.single().devices.first { it.id == sw.id }.positionU)
    }

    @Test fun nestedParentPreservesMountingAndMovingRackMovesTheWholeTree() {
        val p = ObjectHierarchy.assign(initial, swRef, boxRef)
        assertEquals(rackRef, ObjectHierarchy.root(p, swRef))
        assertEquals(3, p.sites.single().devices.first { it.id == sw.id }.positionU)
        val other = Area(name = "Primo")
        val moved = p.copy(racks = listOf(rack.copy(areaId = other.id)))
        assertEquals(other.id, ObjectHierarchy.areaId(moved, swRef))
        assertFalse(ObjectMap.nodes(moved, area.id).any { it.id == rack.id })
    }

    @Test fun selfCyclesAndNonContainersAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { ObjectHierarchy.assign(initial, rackRef, rackRef) }
        assertThrows(IllegalArgumentException::class.java) { ObjectHierarchy.assign(initial, rackRef, boxRef) }
        assertThrows(IllegalArgumentException::class.java) { ObjectHierarchy.assign(initial, boxRef, ObjectRef(PlacementTargetType.DEVICE, outside.id)) }
    }

    @Test fun detachRevealsTheDeviceWithoutDeletingCablesOrPorts() {
        val legacy = initial.copy(sites = initial.sites.map { site -> site.copy(devices = site.devices.map { if (it.id == sw.id) it.copy(areaId = null) else it }) })
        val p = ObjectHierarchy.assign(legacy, swRef, null)
        assertTrue(ObjectMap.nodes(p, area.id).any { it.id == sw.id })
        val d = p.sites.single().devices.first { it.id == sw.id }
        assertNull(d.rackId)
        assertEquals(sw.ports, d.ports)
    }

    @Test fun deletionLiftsChildrenAndTrashRestoresHierarchyAndUnits() {
        val p = ObjectHierarchy.assign(initial, swRef, boxRef)
        val (deleted, trash) = ProjectEdits.deleteDeviceToTrash(p, box.id)
        assertEquals(rackRef, ObjectHierarchy.parent(deleted, swRef))
        val restored = ProjectEdits.restoreFromTrash(deleted, trash!!)
        assertEquals(boxRef, ObjectHierarchy.parent(restored, swRef))
        assertEquals(10, restored.sites.single().devices.first { it.id == box.id }.positionU)
        val (withoutRack, rackTrash) = ProjectEdits.deleteRackToTrash(p, rack.id)
        assertNull(ObjectHierarchy.parent(withoutRack, boxRef))
        assertEquals(boxRef, ObjectHierarchy.parent(withoutRack, swRef))
        assertEquals(rackRef, ObjectHierarchy.root(ProjectEdits.restoreFromTrash(withoutRack, rackTrash!!), swRef))
    }
}
