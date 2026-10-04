package com.onlyfield.assetmanager.core.model

import kotlin.math.ceil
import kotlin.math.sqrt

enum class ObjectFamily { NETWORK, SECURITY, SERVER, POWER, PASSIVE, STRUCTURE, ENDPOINT, OTHER }

enum class LinkMedium { COPPER, FIBER, POWER, OTHER }

/** [typeId] is set only for built-in types, so UIs can pick a drawn icon without matching codes. */
data class Glyph(val code: String, val family: ObjectFamily, val typeId: String? = null)

/** Short, unique map symbols: never derived from translated names for built-in types. */
object ObjectGlyph {
    val RACK = Glyph("R", ObjectFamily.STRUCTURE, "rack")
    private val builtins = mapOf(
        "switch" to Glyph("SW", ObjectFamily.NETWORK), "router" to Glyph("RT", ObjectFamily.NETWORK),
        "modem" to Glyph("MD", ObjectFamily.NETWORK), "ont" to Glyph("ONT", ObjectFamily.NETWORK),
        "access-point" to Glyph("AP", ObjectFamily.NETWORK), "wifi-controller" to Glyph("WLC", ObjectFamily.NETWORK),
        "firewall" to Glyph("FW", ObjectFamily.SECURITY), "camera" to Glyph("CAM", ObjectFamily.SECURITY),
        "nvr" to Glyph("NVR", ObjectFamily.SECURITY), "access-control" to Glyph("ACC", ObjectFamily.SECURITY),
        "server" to Glyph("SRV", ObjectFamily.SERVER), "nas" to Glyph("NAS", ObjectFamily.SERVER), "san" to Glyph("SAN", ObjectFamily.SERVER),
        "workstation" to Glyph("PC", ObjectFamily.ENDPOINT), "ip-phone" to Glyph("TEL", ObjectFamily.ENDPOINT),
        "pbx" to Glyph("PBX", ObjectFamily.ENDPOINT), "sensor" to Glyph("SEN", ObjectFamily.ENDPOINT),
        "ups" to Glyph("UPS", ObjectFamily.POWER), "pdu" to Glyph("PDU", ObjectFamily.POWER), "power-supply" to Glyph("PSU", ObjectFamily.POWER),
        "patch-panel" to Glyph("PP", ObjectFamily.PASSIVE), "outlet" to Glyph("PR", ObjectFamily.PASSIVE), "blank-panel" to Glyph("BP", ObjectFamily.PASSIVE),
        "rack" to RACK, "shelf" to Glyph("SH", ObjectFamily.STRUCTURE),
        "cabinet" to Glyph("ARM", ObjectFamily.STRUCTURE), "enclosure" to Glyph("BOX", ObjectFamily.STRUCTURE),
    )

    fun family(category: DeviceCategory): ObjectFamily = when (category) {
        DeviceCategory.NETWORK_SWITCH -> ObjectFamily.NETWORK
        DeviceCategory.PATCH_PANEL, DeviceCategory.BLANK_PANEL -> ObjectFamily.PASSIVE
        DeviceCategory.UPS_PDU -> ObjectFamily.POWER
        DeviceCategory.SERVER_STORAGE -> ObjectFamily.SERVER
        DeviceCategory.CAMERA_NVR -> ObjectFamily.SECURITY
        DeviceCategory.SHELF -> ObjectFamily.STRUCTURE
        DeviceCategory.CUSTOM -> ObjectFamily.OTHER
    }

    fun of(type: ObjectType): Glyph {
        val builtin = ObjectCatalog.builtins.any { it.id == type.id }
        val glyph = builtins[type.id]?.takeIf { builtin }
            ?: Glyph(initials(type.name), if (type.kind == ObjectKind.RACK) ObjectFamily.STRUCTURE else family(type.category))
        return if (builtin) glyph.copy(typeId = type.id) else glyph
    }

    fun of(project: Project, device: Device?): Glyph {
        val type = ObjectCatalog.type(project, device?.objectTypeId)
        return type?.let(::of) ?: Glyph(initials(device?.technicalName.orEmpty()), family(device?.category ?: DeviceCategory.CUSTOM))
    }

    /** "Gateway LoRa" → "GL", "Gateway" → "GAT". */
    fun initials(name: String): String {
        val words = name.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        val code = if (words.size > 1) words.take(3).joinToString("") { it.take(1) } else words.firstOrNull()?.take(3).orEmpty()
        return code.uppercase().ifBlank { "?" }
    }
}

/** Resolves cable ends to devices with one lookup table. */
class CableEnds(project: Project) {
    private val devices = project.businessUnits.flatMap { it.devices }
    private val byId = devices.associateBy { it.id }
    private val byPort = devices.flatMap { d -> d.ports.map { it.id to d } }.toMap()
    fun device(cable: Cable, first: Boolean): Device? {
        val port = if (first) cable.portAId else cable.portBId
        return if (port != null) byPort[port] else byId[if (first) cable.deviceAId else cable.deviceBId]
    }
    fun ref(cable: Cable, first: Boolean): ObjectRef? = device(cable, first)?.let { ObjectRef(PlacementTargetType.DEVICE, it.id) }

    companion object {
        fun medium(cable: Cable): LinkMedium = when {
            cable.objectTypeId == "power-cable" -> LinkMedium.POWER
            cable.medium == CableMedium.FIBER_OVERALL || cable.medium == CableMedium.AOC -> LinkMedium.FIBER
            cable.medium == CableMedium.ETHERNET_COPPER || cable.medium == CableMedium.DAC -> LinkMedium.COPPER
            else -> LinkMedium.OTHER
        }
    }
}

/** Cable end outside the current view, with its floor and business unit for labels and navigation. */
data class RemoteEnd(
    val device: Device,
    val port: Port?,
    /** Object to select on [areaId]: the device itself, opened through its containers. */
    val ref: ObjectRef,
    val areaId: String?,
    val areaName: String?,
    val buId: String?,
    val buName: String?,
) {
    /** "SW-05 · Primo · BU Nord"; floor and BU only when they differ from the viewer's. */
    fun label(fromAreaId: String? = null, fromBuId: String? = null, name: String = device.technicalName): String =
        listOfNotNull(name, areaName?.takeIf { areaId != fromAreaId }, buName?.takeIf { buId != fromBuId }).joinToString(" · ")
}

data class SceneNode(
    val ref: ObjectRef,
    val name: String,
    val glyph: Glyph,
    val point: MapPoint,
    val isContainer: Boolean,
    val childCount: Int,
    val portsUsed: Int,
    val portsTotal: Int,
    /** Cables whose both ends are inside this node: shown only after opening it. */
    val internalCables: List<String>,
)

/**
 * One drawn line per visible pair of nodes. A null end lies outside this view
 * (other floor, outside the container, or unknown) and is drawn at [start]/[end].
 * [routeCableId] owns the editable route; [reversed] when its A end is [b].
 */
data class SceneLink(
    val a: ObjectRef?,
    val b: ObjectRef?,
    val cableIds: List<String>,
    val media: Set<LinkMedium>,
    val routeCableId: String,
    val reversed: Boolean = false,
    val bends: List<MapPoint> = emptyList(),
    val start: MapPoint? = null,
    val end: MapPoint? = null,
    /** Outside end of each cable when [b] is null, keyed by cable id. */
    val remotes: Map<String, RemoteEnd> = emptyMap(),
)

/** What a map view shows: a floor or the inside of a container, with simplified links. */
data class MapScene(val areaId: String, val container: ObjectRef?, val nodes: List<SceneNode>, val links: List<SceneLink>, val buId: String? = null) {
    private val byRef = nodes.associateBy { it.ref }
    val editable get() = container == null

    fun node(ref: ObjectRef?): SceneNode? = ref?.let(byRef::get)
    fun points(link: SceneLink): List<MapPoint> =
        listOfNotNull(node(link.a)?.point ?: link.start) + link.bends + listOfNotNull(node(link.b)?.point ?: link.end)
    fun moved(ref: ObjectRef, point: MapPoint) = copy(nodes = nodes.map { if (it.ref == ref) it.copy(point = point) else it })
    fun linkOf(cableId: String): SceneLink? = links.find { cableId in it.cableIds }

    /** Route to persist after editing [points] of [link] on a floor view. */
    fun route(project: Project, link: SceneLink, points: List<MapPoint>): CableRoute? {
        if (!editable || points.size < 2) return null
        val ordered = if (link.reversed) points.reversed() else points
        val stored = project.cableRoutes.find { it.cableId == link.routeCableId && it.areaId == areaId }
        return (stored ?: CableRoute(cableId = link.routeCableId, areaId = areaId)).copy(points = ordered.map { MapPoint(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) })
    }

    companion object {
        fun area(project: Project, areaId: String): MapScene {
            val hierarchy = HierarchyIndex(project)
            val nodes = ObjectMap.nodes(project, areaId, hierarchy).map { ObjectRef(it.type, it.id) to it.point }
            val refs = nodes.map { it.first }.toSet()
            return build(project, hierarchy, areaId, null, nodes) { ref -> hierarchy.root(ref).takeIf { it in refs } }
        }

        fun container(project: Project, container: ObjectRef): MapScene {
            val hierarchy = HierarchyIndex(project)
            val children = sorted(project, hierarchy.children[container].orEmpty())
            val columns = if (container.type == PlacementTargetType.RACK) ceil(children.size / 12.0).toInt() else ceil(sqrt(children.size.toDouble())).toInt()
            val cols = columns.coerceAtLeast(1)
            val rows = ceil(children.size.toDouble() / cols).toInt().coerceAtLeast(1)
            // Racks fill columns top-down so the list follows the U order.
            val nodes = children.mapIndexed { i, ref ->
                val (col, row) = if (container.type == PlacementTargetType.RACK) i / rows to i % rows else i % cols to i / cols
                ref to MapPoint((col + .5f) / cols, (row + .5f) / rows)
            }
            val refs = children.toSet()
            return build(project, hierarchy, hierarchy.areaId(container).orEmpty(), container, nodes) { ref ->
                (listOf(ref) + hierarchy.ancestors(ref)).firstOrNull { it in refs }
            }
        }

        private fun sorted(project: Project, refs: List<ObjectRef>): List<ObjectRef> {
            val lookup = Lookup(project)
            return refs.map { it to lookup.name(it).lowercase() }
                .sortedWith(compareByDescending<Pair<ObjectRef, String>> { lookup.devices[it.first.id]?.positionU ?: -1 }.thenBy { it.second })
                .map { it.first }
        }

        /** Id tables for one scene build, so names and container roles are not scanned per node. */
        private class Lookup(private val project: Project) {
            val devices = project.businessUnits.flatMap { it.devices }.associateBy { it.id }
            private val racks = project.racks.associateBy { it.id }
            private val types = ObjectCatalog.types(project).reversed().associateBy { it.id } // first match wins, as in ObjectCatalog.type
            fun name(ref: ObjectRef): String = when (ref.type) {
                PlacementTargetType.RACK -> racks[ref.id]?.name
                PlacementTargetType.DEVICE -> devices[ref.id]?.technicalName
            } ?: ObjectHierarchy.name(project, ref)
            fun canContain(ref: ObjectRef): Boolean = when (ref.type) {
                PlacementTargetType.RACK -> ref.id in racks
                PlacementTargetType.DEVICE -> devices[ref.id]?.let { types[it.objectTypeId] }?.let { it.kind == ObjectKind.DEVICE && it.canContainObjects } == true
            }
        }

        private fun build(project: Project, hierarchy: HierarchyIndex, areaId: String, container: ObjectRef?,
                          placed: List<Pair<ObjectRef, MapPoint>>, visible: (ObjectRef) -> ObjectRef?): MapScene {
            val ends = CableEnds(project)
            val remote = RemoteEnds(project, hierarchy)
            val graph = ConnectionGraph(project)
            val lookup = Lookup(project)
            val devices = lookup.devices
            val stored = if (container == null) project.cableRoutes.filter { it.areaId == areaId }.associateBy { it.cableId } else emptyMap()
            val internal = mutableMapOf<ObjectRef, MutableList<String>>()
            val groups = linkedMapOf<String, MutableList<Triple<Cable, ObjectRef?, ObjectRef?>>>()
            for (cable in project.cables.sortedBy { it.id }) {
                val a = ends.ref(cable, true)?.let(visible)
                val b = ends.ref(cable, false)?.let(visible)
                val key = when {
                    a != null && a == b -> { internal.getOrPut(a) { mutableListOf() } += cable.id; continue }
                    a != null && b != null -> listOf(a, b).map { "${it.type}:${it.id}" }.sorted().joinToString("|")
                    a != null || b != null -> "out:${(a ?: b)!!.id}"
                    cable.id in stored -> "free:${cable.id}"
                    else -> continue
                }
                groups.getOrPut(key) { mutableListOf() } += Triple(cable, a, b)
            }
            val points = placed.toMap()
            val links = groups.values.map { group ->
                val (cable, a, b) = group.firstOrNull { it.first.id in stored } ?: group.first()
                val route = stored[cable.id]
                // Outside ends stay on the visible node's A side so the stub always starts at a node.
                val reversed = a == null && b != null
                val near = (if (reversed) b else a)?.let(points::get)
                val far = if (reversed) route?.points?.first() else route?.points?.last()
                SceneLink(
                    a = if (reversed) b else a, b = if (reversed) a else b,
                    cableIds = group.map { it.first.id }, media = group.map { CableEnds.medium(it.first) }.toSet(),
                    routeCableId = cable.id, reversed = reversed,
                    bends = route?.bends?.let { if (reversed) it.reversed() else it }.orEmpty(),
                    start = if (a == null && b == null) route?.points?.first() else null,
                    end = if (a == null && b == null) route?.points?.last() else if (a == null || b == null) far ?: near?.let(::edge) else null,
                    remotes = if ((a == null) != (b == null)) group.mapNotNull { (c, ca, _) -> remote.of(c, first = ca == null)?.let { c.id to it } }.toMap() else emptyMap(),
                )
            }
            val nodes = placed.map { (ref, point) ->
                val subtree = listOf(ref) + hierarchy.descendants(ref)
                val ports = subtree.filter { it.type == PlacementTargetType.DEVICE }.flatMap { devices[it.id]?.ports.orEmpty() }
                val glyph = if (ref.type == PlacementTargetType.RACK) ObjectGlyph.RACK else ObjectGlyph.of(project, devices[ref.id])
                val children = hierarchy.children[ref].orEmpty()
                SceneNode(ref, lookup.name(ref), glyph, point,
                    isContainer = children.isNotEmpty() || lookup.canContain(ref), childCount = children.size,
                    portsUsed = ports.count { graph.occupied(it.id) }, portsTotal = ports.size, internalCables = internal[ref].orEmpty())
            }
            return MapScene(areaId, container, nodes, links, ObjectMap.floorBusinessUnit(project, areaId).ifBlank { null })
        }

        /** Nearest border point, slightly inside the page so the stub stays visible. */
        fun edge(p: MapPoint): MapPoint {
            val inset = .03f
            val options = listOf(p.x to MapPoint(inset, p.y), 1 - p.x to MapPoint(1 - inset, p.y), p.y to MapPoint(p.x, inset), 1 - p.y to MapPoint(p.x, 1 - inset))
            return options.minBy { it.first }.second
        }
    }
}

/** Resolves cable ends to [RemoteEnd] with one set of lookup tables. */
class RemoteEnds(private val project: Project, private val hierarchy: HierarchyIndex = HierarchyIndex(project)) {
    private val ends = CableEnds(project)
    private val areas = project.businessUnits.flatMap { bu -> ObjectMap.areas(bu).map { it.id to it } }.toMap()
    private val owners = project.businessUnits.flatMap { bu -> bu.devices.map { it.id to bu } }.toMap()

    fun of(cable: Cable, first: Boolean): RemoteEnd? {
        val device = ends.device(cable, first) ?: return null
        val portId = if (first) cable.portAId else cable.portBId
        val ref = ObjectRef(PlacementTargetType.DEVICE, device.id)
        val areaId = hierarchy.areaId(ref)
        val bu = owners[device.id]
        return RemoteEnd(device, device.ports.find { it.id == portId }, ref, areaId, areaId?.let(areas::get)?.name, bu?.id, bu?.name)
    }
}

/** Backbone segments a cable runs through, in the cable's order. */
fun Project.backbones(cable: Cable): List<SharedPathSegment> = cable.sharedPathSegmentIds.mapNotNull { id -> sharedPathSegments.find { it.id == id } }
