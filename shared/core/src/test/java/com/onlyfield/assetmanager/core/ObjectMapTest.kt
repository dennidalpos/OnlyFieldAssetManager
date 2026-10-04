package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import org.junit.Assert.*
import org.junit.Test

class ObjectMapTest {
    private val area = Area(name = "Terra")
    private val other = Area(name = "Primo")
    private val bu = BusinessUnit(name = "BU", areas = listOf(area, other))
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(bu))

    @Test fun fittedCoordinatesRoundTripThroughMarginsZoomAndPan() {
        val view = MapViewport(800f, 600f, 1600f, 400f, 2f, 40f, -30f)
        val original = MapPoint(.3f, .7f)
        val screen = view.screen(original)
        val actual = view.relative(screen.x, screen.y)
        assertEquals(original.x, actual.x, .00001f); assertEquals(original.y, actual.y, .00001f)
        assertEquals(MapPoint(0f, 0f), view.relative(-10000f, -10000f))
    }
    @Test fun panKeepsThePageOnScreen() {
        assertEquals(0f, MapViewport(800f, 600f, 1200f, 900f, 1f, 500f, -500f).clamped().panX)
        val zoomed = MapViewport(800f, 600f, 1200f, 900f, 2f, 1000f, -1000f).clamped()
        assertEquals(0f, zoomed.left); assertEquals(600f, zoomed.top + zoomed.pageHeight)
        val small = MapViewport(800f, 600f, 1200f, 900f, .5f, 500f, 0f).clamped()
        assertEquals(800f, small.left + small.pageWidth)
    }
    @Test fun newObjectsAppearAndStayOnTheirOwnFloor() {
        val d = MapObjectDraft(type = ObjectCatalog.builtins.first(), buId = bu.id, areaId = area.id)
        val saved = d.copy(device = d.device.copy(technicalName = "SW-01")).apply(project)
        assertEquals(listOf("SW-01"), ObjectMap.nodes(saved, area.id).map { it.name })
        assertTrue(ObjectMap.nodes(saved, other.id).isEmpty())
        val placed = ObjectMap.place(saved, area.id, PlacementTargetType.DEVICE, d.id, MapPoint(.8f, .2f))
        assertEquals(MapPoint(.8f, .2f), ObjectMap.nodes(placed, area.id).single().point)
        assertEquals(1, placed.floorplanPlacements.size)
    }
    @Test fun objectAddedWithoutPointStartsAwayFromTheEdges() {
        val d = MapObjectDraft(type = ObjectCatalog.builtins.first(), buId = bu.id, areaId = area.id)
        val saved = d.copy(device = d.device.copy(technicalName = "RK-01")).apply(project)
        val point = ObjectMap.nodes(saved, area.id).single().point
        assertTrue(point.toString(), point.x in .3f..(.7f) && point.y in .3f..(.7f))
    }
    @Test fun legacyLayoutHasNoRepeatedPositionsForOneHundredDevices() {
        val p = project.copy(businessUnits = listOf(bu.copy(devices = (1..100).map { Device(technicalName = "D$it", areaId = area.id) })))
        val nodes = ObjectMap.nodes(p, area.id)
        assertEquals(100, nodes.map { it.point }.distinct().size)
        assertEquals(nodes, ObjectMap.nodes(p, area.id))
    }
    @Test fun editsPreservePortsAndFieldsNotShownByTheMap() {
        val device = Device(technicalName = "SW", areaId = area.id, siteId = "retained", ports = listOf(Port(deviceId = "device", name = "P1")))
        val p = project.copy(businessUnits = listOf(bu.copy(devices = listOf(device))))
        val draft = MapObjectDraft.device(p, bu.id, area.id, device.id)
        val saved = draft.copy(device = draft.device.copy(alias = "Core")).apply(p).businessUnits.single().devices.single()
        assertEquals(device.ports, saved.ports); assertEquals(device.siteId, saved.siteId)
        assertEquals("Core", saved.alias)
    }
    @Test fun cableGeometryFollowsDevicesAndSurvivesDisconnection() {
        val device = Device(technicalName = "SW", areaId = area.id)
        val cable = Cable(codeOrLabel = "C1", deviceAId = device.id)
        var p = project.copy(businessUnits = listOf(bu.copy(devices = listOf(device))), cables = listOf(cable))
        p = ObjectMap.place(p, area.id, PlacementTargetType.DEVICE, device.id, MapPoint(.7f, .2f))
        assertEquals(MapPoint(.7f, .2f), ObjectMap.routePoints(p, ObjectMap.routes(p, area.id).single(), ObjectMap.nodes(p, area.id)).first())
        val deleted = ProjectEdits.deleteDeviceToTrash(p, device.id).first
        assertNull(deleted.cables.single().deviceAId)
        assertEquals(MapPoint(.7f, .2f), deleted.cableRoutes.single().points.first())
        assertTrue(deleted.floorplanPlacements.isEmpty())
        assertTrue(ProjectEdits.deleteCable(deleted, cable.id).cableRoutes.isEmpty())
    }
    @Test fun crossFloorCableAppearsOnBothRelevantMaps() {
        val a = Device(technicalName = "A", areaId = area.id)
        val b = Device(technicalName = "B", areaId = other.id)
        val p = project.copy(businessUnits = listOf(bu.copy(devices = listOf(a, b))), cables = listOf(Cable(deviceAId = a.id, deviceBId = b.id)))
        assertEquals(1, ObjectMap.routes(p, area.id).size); assertEquals(1, ObjectMap.routes(p, other.id).size)
        val issues = com.onlyfield.assetmanager.core.validation.ModelValidator.validateProject(p).issues
        assertFalse(issues.any { it.code == "DETACHED_CABLE_ENDPOINT" })
    }
    @Test fun removingRackKeepsUnassignedDevicesOnTheFloor() {
        val rack = Rack(name = "R1", areaId = area.id)
        val device = Device(technicalName = "SW", rackId = rack.id)
        val cable = Cable(deviceAId = device.id)
        val p = ObjectMap.place(project.copy(businessUnits = listOf(bu.copy(devices = listOf(device))), racks = listOf(rack), cables = listOf(cable)), area.id, PlacementTargetType.RACK, rack.id, MapPoint(.5f, .5f))
        assertEquals(1, ObjectMap.routes(p, area.id).size)
        val deleted = ProjectEdits.deleteRackToTrash(p, rack.id).first
        assertEquals(area.id, deleted.businessUnits.single().devices.single().areaId)
        assertEquals(listOf(device.id), ObjectMap.nodes(deleted, area.id).map { it.id })
        assertTrue(deleted.floorplanPlacements.isEmpty())
    }

    @Test fun removingPortKeepsCableConnectedToItsDevice() {
        val id = java.util.UUID.randomUUID().toString()
        val port = Port(deviceId = id, name = "P1")
        val device = Device(id = id, technicalName = "SW", areaId = area.id, ports = listOf(port))
        val cable = Cable(portAId = port.id)
        val p = project.copy(businessUnits = listOf(bu.copy(devices = listOf(device))), cables = listOf(cable))
        val saved = ProjectEdits.deletePortFromDevice(p, id, port.id)
        assertNull(saved.cables.single().portAId)
        assertEquals(id, saved.cables.single().deviceAId)
        assertEquals(1, ObjectMap.routes(saved, area.id).size)
    }

    @Test(expected = IllegalArgumentException::class) fun invalidRouteIsRejected() {
        ObjectMap.saveRoute(project, CableRoute(cableId = "missing", areaId = area.id, points = listOf(MapPoint(Float.NaN, 0f), MapPoint(1f, 1f))))
    }
}
