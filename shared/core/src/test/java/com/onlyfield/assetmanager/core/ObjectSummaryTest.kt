package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.display.ObjectSummary
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ObjectSummaryTest {
    private val area = Area(name = "Terra")
    private val rack = Rack(name = "R1", areaId = area.id)
    private val boxType = ObjectType(name = "Scatola", canContainObjects = true)
    private val box = Device(technicalName = "BOX", areaId = area.id, objectTypeId = boxType.id)
    private val sw = Device(technicalName = "SW", areaId = area.id, rackId = rack.id, positionU = 12, heightU = 2, rackSide = RackSide.FRONT,
        physicalLabel = "  ", ipAddress = "10.0.0.2", serialNumber = "SN1")
    private val ap = Device(technicalName = "AP", areaId = area.id)
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1,
        businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area), devices = listOf(box, sw, ap))), racks = listOf(rack), objectTypes = listOf(boxType))

    @Test fun identityKeepsOnlyFilledValuesWithLabels() {
        val summary = ObjectSummary.of(project, ObjectRef(PlacementTargetType.DEVICE, sw.id))
        assertEquals(listOf("Indirizzo IP" to "10.0.0.2", "Numero di serie" to "SN1"), summary.identity.map { it.label to it.value })
    }

    @Test fun locationListsFloorAndContainersOutermostFirstWithMount() {
        val nested = ObjectHierarchy.assign(project, ObjectRef(PlacementTargetType.DEVICE, ap.id), ObjectRef(PlacementTargetType.DEVICE, box.id))
        val inRack = ObjectHierarchy.assign(nested, ObjectRef(PlacementTargetType.DEVICE, box.id), ObjectRef(PlacementTargetType.RACK, rack.id))
        assertEquals(listOf("Terra", "R1", "BOX"), ObjectSummary.of(inRack, ObjectRef(PlacementTargetType.DEVICE, ap.id)).location)
        val sw = ObjectSummary.of(project, ObjectRef(PlacementTargetType.DEVICE, sw.id))
        assertEquals("U12–13 · Fronte", sw.mount)
        assertEquals("Terra › R1 › U12–13 · Fronte", sw.place())
    }

    @Test fun racksAndLooseObjectsHaveNoMountAndNoIdentity() {
        val rackSummary = ObjectSummary.of(project, ObjectRef(PlacementTargetType.RACK, rack.id))
        assertTrue(rackSummary.identity.isEmpty())
        assertNull(rackSummary.mount)
        assertEquals("Terra", rackSummary.place())
    }
}
