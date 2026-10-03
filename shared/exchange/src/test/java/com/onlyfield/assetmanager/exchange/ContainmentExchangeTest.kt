package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ContainmentExchangeTest {
    private val area = Area(name = "Terra")
    private val rack = Rack(name = "R1", areaId = area.id)
    private val other = Rack(name = "R2", areaId = area.id)
    private val device = Device(technicalName = "SW", rackId = rack.id, positionU = 4)
    private val child = ObjectRef(PlacementTargetType.DEVICE, device.id)
    private val initial = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, racks = listOf(rack, other),
        businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area), devices = listOf(device))))

    @Test fun encryptedHierarchyRoundTripsAndLegacyRackIsConverted() {
        val p = ObjectHierarchy.normalize(initial)
        val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(p, password = "dummy-password"), "dummy-password")
        assertTrue(result.validationResult.isValid)
        assertEquals("1.10", result.pkg!!.manifest.formatVersion)
        assertEquals(p.objectContainments, result.pkg!!.project.objectContainments)
        assertEquals(4, result.pkg!!.project.businessUnits.single().devices.single().positionU)
        val legacy = PackageSerializer.importPackage(PackageSerializer.exportPackage(initial))
        assertEquals(ObjectRef(PlacementTargetType.RACK, rack.id), ObjectHierarchy.parent(legacy.pkg!!.project, child))
    }

    @Test fun competingParentsConflictOnTheChildIdentity() {
        val third = Rack(name = "R3", areaId = area.id)
        val base = ObjectHierarchy.normalize(initial.copy(racks = initial.racks + third))
        val local = ObjectHierarchy.assign(base, child, ObjectRef(PlacementTargetType.RACK, other.id))
        val remote = ObjectHierarchy.assign(base, child, ObjectRef(PlacementTargetType.RACK, third.id))
        val result = ProjectMerger.merge(base, local, remote)
        val conflict = result.conflicts.single { it.key.kind == "objectContainments" }
        assertEquals(device.id, conflict.key.id)
        val resolved = result.resolve(result.conflicts.associate { it.key to MergeSide.INCOMING })
        assertEquals(ObjectRef(PlacementTargetType.RACK, third.id), ObjectHierarchy.parent(resolved, child))
    }

    @Test fun cyclicPackageIsRejectedInsteadOfFlatteningItsHierarchy() {
        val type = ObjectType(name = "Contenitore", canContainObjects = true)
        val d = device.copy(objectTypeId = type.id)
        val p = initial.copy(objectTypes = listOf(type), businessUnits = listOf(initial.businessUnits.single().copy(devices = listOf(d))),
            objectContainments = listOf(ObjectContainment(child, ObjectRef(PlacementTargetType.RACK, rack.id)), ObjectContainment(ObjectRef(PlacementTargetType.RACK, rack.id), child)))
        val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(p))
        assertNull(result.pkg)
        assertTrue(result.validationResult.issues.any { it.code == "INVALID_OBJECT_CONTAINMENT" })
    }
}
