package com.onlyfield.assetmanager.core.model

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.ceil
import kotlin.math.sqrt

@Serializable
enum class ObjectKind { DEVICE, RACK, CABLE }

@Serializable
data class ObjectType(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: DeviceCategory = DeviceCategory.CUSTOM,
    val kind: ObjectKind = ObjectKind.DEVICE,
    val cableMedium: CableMedium = CableMedium.ETHERNET_COPPER,
)

@Serializable
data class MapPoint(val x: Float, val y: Float)

@Serializable
data class CableRoute(
    val id: String = UUID.randomUUID().toString(),
    val cableId: String,
    val areaId: String,
    val points: List<MapPoint> = listOf(MapPoint(.2f, .5f), MapPoint(.5f, .5f), MapPoint(.8f, .5f)),
)

object ObjectCatalog {
    private fun device(id: String, name: String, category: DeviceCategory = DeviceCategory.CUSTOM) = ObjectType(id, name, category)
    val builtins = listOf(
        device("switch", "Switch", DeviceCategory.NETWORK_SWITCH), device("router", "Router"),
        device("modem", "Modem"), device("ont", "ONT"), device("firewall", "Firewall"),
        device("access-point", "Access point"), device("wifi-controller", "Controller Wi-Fi"),
        device("server", "Server", DeviceCategory.SERVER_STORAGE), device("workstation", "Workstation"),
        device("nas", "NAS", DeviceCategory.SERVER_STORAGE), device("san", "SAN", DeviceCategory.SERVER_STORAGE),
        device("ip-phone", "Telefono IP"), device("pbx", "Centralino"),
        device("camera", "Telecamera", DeviceCategory.CAMERA_NVR), device("nvr", "NVR/DVR", DeviceCategory.CAMERA_NVR),
        device("access-control", "Controllo accessi"), device("sensor", "Sensore"),
        device("ups", "UPS", DeviceCategory.UPS_PDU), device("pdu", "PDU", DeviceCategory.UPS_PDU),
        device("power-supply", "Alimentatore", DeviceCategory.UPS_PDU),
        ObjectType("rack", "Rack", kind = ObjectKind.RACK),
        device("patch-panel", "Patch panel", DeviceCategory.PATCH_PANEL), device("outlet", "Presa dati"),
        device("shelf", "Mensola", DeviceCategory.SHELF), device("blank-panel", "Pannello cieco", DeviceCategory.BLANK_PANEL),
        ObjectType("copper-cable", "Cavo rame", kind = ObjectKind.CABLE),
        ObjectType("fiber-cable", "Cavo fibra", kind = ObjectKind.CABLE, cableMedium = CableMedium.FIBER_OVERALL),
        ObjectType("coax-cable", "Cavo coassiale", kind = ObjectKind.CABLE, cableMedium = CableMedium.OTHER),
        ObjectType("power-cable", "Cavo alimentazione", kind = ObjectKind.CABLE, cableMedium = CableMedium.OTHER),
    )
    fun types(project: Project) = builtins + project.objectTypes
    fun type(project: Project, id: String?) = types(project).find { it.id == id }
}

data class MapNode(val type: PlacementTargetType, val id: String, val name: String, val point: MapPoint, val symbol: String)

/** Coordinates are relative to the fitted page, before viewport zoom and pan. */
data class MapViewport(val width: Float, val height: Float, val contentWidth: Float, val contentHeight: Float, val zoom: Float = 1f, val panX: Float = 0f, val panY: Float = 0f) {
    private val fit = min(width / contentWidth, height / contentHeight)
    val pageWidth get() = contentWidth * fit * zoom
    val pageHeight get() = contentHeight * fit * zoom
    val left get() = (width - pageWidth) / 2 + panX
    val top get() = (height - pageHeight) / 2 + panY
    fun screen(point: MapPoint) = MapPoint(left + point.x * pageWidth, top + point.y * pageHeight)
    fun relative(x: Float, y: Float) = MapPoint(((x - left) / pageWidth).coerceIn(0f, 1f), ((y - top) / pageHeight).coerceIn(0f, 1f))
}

object ObjectMap {
    fun areas(bu: BusinessUnit) = bu.areas + bu.sites.flatMap { it.areas }
    fun areaLabel(bu: BusinessUnit, area: Area): String = bu.sites.find { s -> s.areas.any { it.id == area.id } }?.let { "${it.name} / ${area.name}" } ?: area.name

    fun nodes(project: Project, areaId: String): List<MapNode> {
        val devices = project.businessUnits.flatMap { it.devices }.filter { d -> d.areaId == areaId || (d.areaId == null && project.racks.any { it.id == d.rackId && it.areaId == areaId }) }
        val racks = project.racks.filter { it.areaId == areaId }
        val items = racks.map { Triple(PlacementTargetType.RACK, it.id, it.name) } + devices.map { Triple(PlacementTargetType.DEVICE, it.id, it.technicalName) }
        return items.mapIndexed { i, (type, id, name) ->
            val stored = project.floorplanPlacements.find { it.areaId == areaId && it.targetType == type && it.targetId == id }
            val device = devices.find { it.id == id }
            MapNode(type, id, name, stored?.let { MapPoint(it.xRatio, it.yRatio) } ?: freePoint(i, items.size.coerceAtLeast(36)),
                if (type == PlacementTargetType.RACK) "R" else ObjectCatalog.type(project, device?.objectTypeId)?.name?.take(2)?.uppercase() ?: "D")
        }
    }

    private fun freePoint(i: Int, slots: Int): MapPoint {
        val columns = ceil(sqrt(slots.toDouble())).toInt().coerceAtLeast(1)
        val rows = ceil(slots.toDouble() / columns).toInt().coerceAtLeast(1)
        return MapPoint((i % columns + .5f) / columns, (i / columns + .5f) / rows)
    }

    fun place(project: Project, areaId: String, type: PlacementTargetType, id: String, point: MapPoint): Project {
        require(point.x.isFinite() && point.y.isFinite())
        val previous = project.floorplanPlacements.find { it.areaId == areaId && it.targetType == type && it.targetId == id }
        val placement = previous?.copy(xRatio = point.x.coerceIn(0f, 1f), yRatio = point.y.coerceIn(0f, 1f))
            ?: FloorplanPlacement(areaId = areaId, targetType = type, targetId = id, xRatio = point.x.coerceIn(0f, 1f), yRatio = point.y.coerceIn(0f, 1f))
        return project.copy(floorplanPlacements = project.floorplanPlacements.filterNot { it.id == placement.id } + placement)
    }

    fun placeNew(project: Project, areaId: String, type: PlacementTargetType, id: String): Project {
        val occupied = nodes(project, areaId).filterNot { it.id == id }.map { it.point }
        val slots = (occupied.size + 1).coerceAtLeast(36) * 4
        val gap = .25f / sqrt(slots.toFloat())
        val point = (0 until slots).map { freePoint(it, slots) }.first { p -> occupied.none { hypot(it.x - p.x, it.y - p.y) < gap } }
        return place(project, areaId, type, id, point)
    }

    fun endpoint(project: Project, cable: Cable, first: Boolean): Device? {
        val devices = project.businessUnits.flatMap { it.devices }
        val port = if (first) cable.portAId else cable.portBId
        val id = if (first) cable.deviceAId else cable.deviceBId
        return if (port != null) devices.find { d -> d.ports.any { it.id == port } } else devices.find { it.id == id }
    }

    fun areaId(project: Project, device: Device?): String? = device?.areaId ?: project.racks.find { it.id == device?.rackId }?.areaId

    fun routes(project: Project, areaId: String): List<CableRoute> = project.cables.mapNotNull { cable ->
        project.cableRoutes.find { it.cableId == cable.id && it.areaId == areaId }
            ?: if (areaId(project, endpoint(project, cable, true)) == areaId || areaId(project, endpoint(project, cable, false)) == areaId) CableRoute(id = UUID.nameUUIDFromBytes("${cable.id}:$areaId".toByteArray(Charsets.UTF_8)).toString(), cableId = cable.id, areaId = areaId) else null
    }

    fun routePoints(project: Project, route: CableRoute, nodes: List<MapNode>): List<MapPoint> {
        val cable = project.cables.find { it.id == route.cableId } ?: return route.points
        return route.points.mapIndexed { i, point ->
            val endpoint = when (i) { 0 -> endpoint(project, cable, true); route.points.lastIndex -> endpoint(project, cable, false); else -> null }
            nodes.find { it.id == endpoint?.id }?.point ?: point
        }
    }

    fun saveRoute(project: Project, route: CableRoute): Project {
        require(route.points.size >= 2 && route.points.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f })
        return project.copy(cableRoutes = project.cableRoutes.filterNot { it.cableId == route.cableId && it.areaId == route.areaId } + route)
    }

    fun segmentDistance(point: MapPoint, a: MapPoint, b: MapPoint): Float {
        val dx = b.x - a.x; val dy = b.y - a.y
        val length = dx * dx + dy * dy
        val t = if (length == 0f) 0f else (((point.x - a.x) * dx + (point.y - a.y) * dy) / length).coerceIn(0f, 1f)
        return hypot(point.x - a.x - t * dx, point.y - a.y - t * dy)
    }
}
