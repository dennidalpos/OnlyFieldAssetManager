package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class MapSceneTest {
    private val area = Area(name = "Terra")
    private val other = Area(name = "Primo")
    private val rack = Rack(name = "R1", areaId = area.id)
    private val boxType = ObjectType(name = "Scatola", canContainObjects = true)
    private val box = Device(technicalName = "BOX", areaId = area.id, rackId = rack.id, positionU = 10, objectTypeId = boxType.id)
    private val sw = Device(technicalName = "SW", areaId = area.id, rackId = rack.id, positionU = 3, objectTypeId = "switch")
    private val ap = Device(technicalName = "AP", areaId = area.id, objectTypeId = "access-point")
    private val remote = Device(technicalName = "REMOTE", areaId = other.id)
    private fun project(vararg cables: Cable) = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1,
        sites = listOf(Site(name = "BU", areas = listOf(area, other), devices = listOf(box, sw, ap, remote))),
        racks = listOf(rack), objectTypes = listOf(boxType), cables = cables.toList())
    private val rackRef = ObjectRef(PlacementTargetType.RACK, rack.id)
    private val apRef = ObjectRef(PlacementTargetType.DEVICE, ap.id)

    @Test fun floorShowsRootsWithOneLinePerPairAndHidesInternalCables() {
        val a = Cable(deviceAId = sw.id, deviceBId = ap.id)
        val b = Cable(deviceAId = ap.id, deviceBId = box.id, medium = CableMedium.FIBER_OVERALL)
        val internal = Cable(deviceAId = sw.id, deviceBId = box.id)
        val scene = MapScene.area(project(a, b, internal), area.id)
        assertEquals(setOf(rackRef, apRef), scene.nodes.map { it.ref }.toSet())
        val link = scene.links.single()
        assertEquals(setOf(a.id, b.id), link.cableIds.toSet())
        assertEquals(setOf(LinkMedium.COPPER, LinkMedium.FIBER), link.media)
        val rackNode = scene.node(rackRef)!!
        assertTrue(rackNode.isContainer)
        assertEquals(listOf(internal.id), rackNode.internalCables)
        assertEquals(2, rackNode.childCount)
    }

    @Test fun switchedOffDeviceIsFaded() {
        val p = project().let { it.copy(sites = it.sites.map { s -> s.copy(devices = s.devices.map { d -> if (d.id == ap.id) d.copy(operationalStatus = OperationalStatus.DECOMMISSIONED) else d }) }) }
        val nodes = MapScene.area(p, area.id).nodes.associateBy { it.ref }
        assertTrue(nodes.getValue(apRef).inactive)
        assertFalse(nodes.getValue(rackRef).inactive)
    }

    @Test fun defaultRouteIsStraightAndExplicitCentreBendSurvivesMovingEndpoints() {
        val c = Cable(deviceAId = sw.id, deviceBId = ap.id)
        listOf(project(c), project(c).copy(cableRoutes = listOf(CableRoute(cableId = c.id, areaId = area.id)))).forEach { p ->
            val scene = MapScene.area(p, area.id)
            val link = scene.links.single()
            assertEquals(listOf(scene.node(link.a)!!.point, scene.node(link.b)!!.point), scene.points(link))
        }
        val points = listOf(MapPoint(.2f, .5f), MapPoint(.5f, .5f), MapPoint(.8f, .5f))
        val saved = project(c).copy(cableRoutes = listOf(CableRoute(cableId = c.id, areaId = area.id, points = points)))
        val moved = ObjectMap.place(ObjectMap.place(saved, area.id, rackRef.type, rackRef.id, MapPoint(.1f, .2f)),
            area.id, apRef.type, apRef.id, MapPoint(.9f, .8f))
        val scene = MapScene.area(moved, area.id)
        val link = scene.links.single()
        assertEquals(listOf(MapPoint(.1f, .2f), points[1], MapPoint(.9f, .8f)), scene.points(link))
        assertEquals(points, moved.cableRoutes.single().points)
        assertEquals(listOf(points[1]), scene.route(moved, link, scene.points(link))!!.bends)
    }

    @Test fun crossFloorCableBecomesAStubAtTheBorder() {
        val c = Cable(deviceAId = remote.id, deviceBId = ap.id)
        val scene = MapScene.area(project(c), area.id)
        val link = scene.links.single()
        assertEquals(apRef, link.a); assertNull(link.b); assertTrue(link.reversed)
        val end = scene.points(link).last()
        assertTrue(end.x <= .03f || end.x >= .97f || end.y <= .03f || end.y >= .97f)
        // Saved geometry keeps the cable's own A→B orientation.
        val route = scene.route(project(c), link, scene.points(link))!!
        assertEquals(scene.node(apRef)!!.point, route.points.last())
    }

    @Test fun stubKnowsRemoteDeviceFloorAndSite() {
        val north = Area(name = "Nord 1")
        val northRack = Rack(name = "RN", areaId = north.id)
        val sw5 = Device(technicalName = "SW-05", rackId = northRack.id, positionU = 1)
        val c = Cable(deviceAId = ap.id, deviceBId = sw5.id)
        val p = project(c).let { it.copy(sites = it.sites + Site(name = "BU Nord", areas = listOf(north), devices = listOf(sw5)),
            racks = it.racks + northRack) }
        val scene = MapScene.area(p, area.id)
        val end = scene.links.single().remotes.getValue(c.id)
        assertEquals(ObjectRef(PlacementTargetType.DEVICE, sw5.id), end.ref)
        assertEquals(north.id, end.areaId)
        assertEquals("SW-05 · Nord 1 · BU Nord", end.label(scene.areaId, scene.siteId))
        assertEquals("SW-05", end.label(north.id, end.siteId))
        // The far floor shows the same cable as a stub towards AP.
        assertEquals("AP · Terra · BU", MapScene.area(p, north.id).links.single().remotes.getValue(c.id).let { it.label(north.id, end.siteId) })
    }

    @Test fun containerShowsChildrenInUnitOrderAndOutsideLinks() {
        val toAp = Cable(deviceAId = sw.id, deviceBId = ap.id)
        val inside = Cable(deviceAId = sw.id, deviceBId = box.id)
        val scene = MapScene.container(project(toAp, inside), rackRef)
        assertEquals(listOf(box.id, sw.id), scene.nodes.map { it.ref.id })
        assertFalse(scene.editable)
        assertEquals(2, scene.links.size)
        assertNull(scene.links.single { toAp.id in it.cableIds }.b)
        assertNull(scene.route(project(toAp), scene.links.first(), listOf(MapPoint(0f, 0f), MapPoint(1f, 1f))))
    }

    @Test fun builtInGlyphsAreUniqueAndCustomTypesUseInitials() {
        val codes = ObjectCatalog.builtins.filter { it.kind != ObjectKind.CABLE }.map { ObjectGlyph.of(it).code }
        assertEquals(codes.size, codes.distinct().size)
        assertEquals("GL", ObjectGlyph.of(ObjectType(name = "Gateway LoRa")).code)
        assertEquals(ObjectFamily.SECURITY, ObjectGlyph.of(ObjectType(name = "Lettore", category = DeviceCategory.CAMERA_NVR)).family)
    }

    @Test fun portUsageCountsTheWholeSubtree() {
        val swWithPorts = sw.copy(ports = listOf(Port(deviceId = sw.id, name = "1"), Port(deviceId = sw.id, name = "2")))
        val p = project().let { it.copy(sites = it.sites.map { site -> site.copy(devices = site.devices.map { d -> if (d.id == sw.id) swWithPorts else d }) }) }
        val used = p.copy(cables = listOf(Cable(portAId = swWithPorts.ports.first().id, deviceBId = ap.id)))
        val node = MapScene.area(used, area.id).node(rackRef)!!
        assertEquals(1, node.portsUsed); assertEquals(2, node.portsTotal)
    }
}
