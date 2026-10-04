package com.onlyfield.assetmanager.core.model

import com.onlyfield.assetmanager.core.i18n.Messages

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
    val canContainObjects: Boolean = false,
)

@Serializable
data class MapPoint(val x: Float, val y: Float)

@Serializable
data class CableRoute(
    val id: String = UUID.randomUUID().toString(),
    val cableId: String,
    val areaId: String,
    val points: List<MapPoint> = listOf(MapPoint(.4f, .5f), MapPoint(.6f, .5f)),
) {
    /** Routes saved before 2026-10 used a shared centre bend; treat it as "no bend". */
    val bends: List<MapPoint> get() = if (points == LEGACY_DEFAULT) emptyList() else points.drop(1).dropLast(1)

    companion object {
        val LEGACY_DEFAULT = listOf(MapPoint(.2f, .5f), MapPoint(.5f, .5f), MapPoint(.8f, .5f))
    }
}

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
        device("blank-panel", "Pannello cieco", DeviceCategory.BLANK_PANEL),
        // Built-in containers: their children open as a nested map.
        ObjectType("shelf", "Mensola", DeviceCategory.SHELF, canContainObjects = true),
        ObjectType("cabinet", "Armadio", DeviceCategory.SHELF, canContainObjects = true),
        ObjectType("enclosure", "Cassetta", DeviceCategory.SHELF, canContainObjects = true),
        ObjectType("copper-cable", "Cavo rame", kind = ObjectKind.CABLE),
        ObjectType("fiber-cable", "Cavo fibra", kind = ObjectKind.CABLE, cableMedium = CableMedium.FIBER_OVERALL),
        ObjectType("coax-cable", "Cavo coassiale", kind = ObjectKind.CABLE, cableMedium = CableMedium.OTHER),
        ObjectType("power-cable", "Cavo alimentazione", kind = ObjectKind.CABLE, cableMedium = CableMedium.OTHER),
    )
    fun displayName(type: ObjectType, i18n: Messages = Messages()): String {
        if (builtins.none { it.id == type.id && it.name == type.name }) return type.name
        return when (type.id) {
            "switch" -> i18n.text("text.39921a740bf2")
            "router" -> i18n.text("text.065d8e6c0bf7")
            "modem" -> i18n.text("text.9e5b909af171")
            "ont" -> i18n.text("text.5812b68a9926")
            "firewall" -> i18n.text("text.f12efc4df249")
            "access-point" -> i18n.text("text.68eddefa8c1e")
            "wifi-controller" -> i18n.text("text.6ed43883b6e9")
            "server" -> i18n.text("text.aef7de28d529")
            "workstation" -> i18n.text("text.b9d0a58e17a6")
            "nas" -> i18n.text("text.ef0d1931f1fe")
            "san" -> i18n.text("text.8217143646e5")
            "ip-phone" -> i18n.text("text.708463735cef")
            "pbx" -> i18n.text("text.21d1d681e4e1")
            "camera" -> i18n.text("text.d578291b8358")
            "nvr" -> i18n.text("text.6324095cf2b2")
            "access-control" -> i18n.text("text.eed2bcc354fc")
            "sensor" -> i18n.text("text.b1e79f3cf9bb")
            "ups" -> i18n.text("text.356479227b43")
            "pdu" -> i18n.text("text.06128351b86c")
            "power-supply" -> i18n.text("text.4060ef581992")
            "rack" -> i18n.text("text.4cd265c2b8c6")
            "patch-panel" -> i18n.text("text.e97fc26f3676")
            "outlet" -> i18n.text("text.4803b51f3912")
            "shelf" -> i18n.text("text.ae286ff299bb")
            "cabinet" -> i18n.text("type.cabinet")
            "enclosure" -> i18n.text("type.enclosure")
            "blank-panel" -> i18n.text("text.3f0dca5e90e5")
            "copper-cable" -> i18n.text("text.b688578fe650")
            "fiber-cable" -> i18n.text("text.e6858e841f57")
            "coax-cable" -> i18n.text("text.12754c12b186")
            "power-cable" -> i18n.text("text.04e7f6b64d01")
            else -> type.name
        }
    }
    fun types(project: Project) = builtins + project.objectTypes
    fun type(project: Project, id: String?) = types(project).find { it.id == id }
}

data class MapNode(val type: PlacementTargetType, val id: String, val name: String, val point: MapPoint)

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
    fun cableLabel(project: Project, cable: Cable, i18n: Messages = Messages()): String {
        fun label(first: Boolean, i18n: Messages = Messages()): String {
            val device = endpoint(project, cable, first) ?: return i18n.text("text.c5d0e28cb66f")
            val portId = if (first) cable.portAId else cable.portBId
            val port = device.ports.find { it.id == portId }
            val floor = areaId(project, device)
            val area = project.businessUnits.flatMap { areas(it) }.find { it.id == floor }
            return device.technicalName + (port?.let { " / ${it.name}" } ?: "") + " (${area?.name ?: i18n.text("text.37a8636584b2")})"
        }
        return "${cable.codeOrLabel ?: i18n.text("text.89dbe18e8407")}: ${label(true, i18n = i18n)} → ${label(false, i18n = i18n)}"
    }
    fun areas(bu: BusinessUnit) = bu.areas + bu.sites.flatMap { it.areas }
    /** Business unit that owns the floor; falls back to the first one for legacy data. */
    fun floorBusinessUnit(project: Project, areaId: String): String =
        project.businessUnits.firstOrNull { bu -> areas(bu).any { it.id == areaId } }?.id ?: project.businessUnits.firstOrNull()?.id.orEmpty()
    fun areaLabel(bu: BusinessUnit, area: Area): String = bu.sites.find { s -> s.areas.any { it.id == area.id } }?.let { "${it.name} / ${area.name}" } ?: area.name

    fun nodes(project: Project, areaId: String, hierarchy: HierarchyIndex = HierarchyIndex(project)): List<MapNode> {
        val devices = project.businessUnits.flatMap { it.devices }.filter { d -> d.areaId == areaId && ObjectRef(PlacementTargetType.DEVICE, d.id) !in hierarchy.parents }
        val racks = project.racks.filter { it.areaId == areaId && ObjectRef(PlacementTargetType.RACK, it.id) !in hierarchy.parents }
        val items = racks.map { Triple(PlacementTargetType.RACK, it.id, it.name) } + devices.map { Triple(PlacementTargetType.DEVICE, it.id, it.technicalName) }
        val placements = project.floorplanPlacements.filter { it.areaId == areaId }.associateBy { it.targetType to it.targetId }
        return items.mapIndexed { i, (type, id, name) ->
            val stored = placements[type to id]
            MapNode(type, id, name, stored?.let { MapPoint(it.xRatio, it.yRatio) } ?: freePoint(i, items.size.coerceAtLeast(36)))
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
        // Free slot nearest the centre: edge slots would clip the node and its label.
        val point = (0 until slots).map { freePoint(it, slots) }.sortedBy { hypot(it.x - .5f, it.y - .5f) }
            .first { p -> occupied.none { hypot(it.x - p.x, it.y - p.y) < gap } }
        return place(project, areaId, type, id, point)
    }

    fun endpoint(project: Project, cable: Cable, first: Boolean): Device? {
        val devices = project.businessUnits.flatMap { it.devices }
        val port = if (first) cable.portAId else cable.portBId
        val id = if (first) cable.deviceAId else cable.deviceBId
        return if (port != null) devices.find { d -> d.ports.any { it.id == port } } else devices.find { it.id == id }
    }

    fun areaId(project: Project, device: Device?): String? = device?.let { ObjectHierarchy.areaId(project, ObjectRef(PlacementTargetType.DEVICE, it.id)) }

    fun routes(project: Project, areaId: String, hierarchy: HierarchyIndex = HierarchyIndex(project)): List<CableRoute> {
        val stored = project.cableRoutes.filter { it.areaId == areaId }.associateBy { it.cableId }
        val ends = CableEnds(project)
        return project.cables.mapNotNull { cable ->
            stored[cable.id] ?: if (listOf(true, false).any { first -> ends.device(cable, first)?.let { hierarchy.areaId(ObjectRef(PlacementTargetType.DEVICE, it.id)) } == areaId })
                CableRoute(id = UUID.nameUUIDFromBytes("${cable.id}:$areaId".toByteArray(Charsets.UTF_8)).toString(), cableId = cable.id, areaId = areaId) else null
        }
    }

    fun routePoints(project: Project, route: CableRoute, nodes: List<MapNode>): List<MapPoint> {
        val cable = project.cables.find { it.id == route.cableId } ?: return route.points
        val points = listOf(route.points.first()) + route.bends + route.points.last()
        return points.mapIndexed { i, point ->
            val endpoint = when (i) { 0 -> endpoint(project, cable, true); points.lastIndex -> endpoint(project, cable, false); else -> null }
            endpoint?.let { ObjectHierarchy.root(project, ObjectRef(PlacementTargetType.DEVICE, it.id)) }?.let { ref -> nodes.find { it.id == ref.id && it.type == ref.type }?.point } ?: point
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
