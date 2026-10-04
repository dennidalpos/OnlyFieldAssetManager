package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.ConfiguratorPage
import com.onlyfield.assetmanager.configurator.PortPanel
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.PortLogic
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/** Host operations; the map never opens editors or dialogs on its own. */
class MapActions(
    val update: (Project, String) -> Unit,
    val edit: (MapObjectDraft, ConfiguratorPage) -> Unit,
    /** New object inside [parent] (or on the floor at [point]). */
    val add: (parent: ObjectRef?, point: MapPoint?) -> Unit,
)

/** Business unit that owns the floor, used for new or legacy objects without one. */
fun floorBusinessUnit(project: Project, areaId: String): String =
    project.businessUnits.firstOrNull { bu -> ObjectMap.areas(bu).any { it.id == areaId } }?.id ?: project.businessUnits.firstOrNull()?.id.orEmpty()

fun editDraft(project: Project, ref: ObjectRef, areaId: String, i18n: Messages): MapObjectDraft {
    val bu = ProjectIndex(project).businessUnitOf(ref.id)?.id ?: floorBusinessUnit(project, areaId)
    return if (ref.type == PlacementTargetType.RACK) MapObjectDraft.rack(project, bu, areaId, ref.id) else MapObjectDraft.device(project, bu, areaId, ref.id, i18n)
}

/**
 * Floor map with drill-down into containers and an adaptive, non-modal detail pane:
 * side panel from 840 dp (expanded window class), bottom panel below.
 */
@Composable
fun MapWorkspace(
    project: Project,
    areaId: String,
    image: ImageBitmap?,
    i18n: Messages,
    actions: MapActions,
    modifier: Modifier = Modifier,
    toolbar: @Composable RowScope.() -> Unit = {},
    media: @Composable (ObjectRef) -> Unit = {},
) {
    var path by remember(areaId) { mutableStateOf(emptyList<ObjectRef>()) }
    val hierarchy = remember(project) { HierarchyIndex(project) }
    // Keep levels only while each is still a child of the previous one on this floor (deleted or moved levels are dropped).
    val validPath = path.withIndex().takeWhile { (i, ref) ->
        hierarchy.parents[ref] == path.getOrNull(i - 1) && (i > 0 || hierarchy.areaId(ref) == areaId)
    }.map { it.value }
    if (validPath != path) SideEffect { path = validPath }
    val container = validPath.lastOrNull()
    var selection by remember(areaId, container) { mutableStateOf<MapSelection?>(null) }
    val scene = remember(project, areaId, container) { if (container == null) MapScene.area(project, areaId) else MapScene.container(project, container) }
    if (selection is MapSelection.Node && scene.node((selection as MapSelection.Node).ref) == null) selection = null
    // Narrow windows hide the pane until something is selected; "List" opens it on demand.
    var listOpen by remember(areaId) { mutableStateOf(false) }

    fun open(ref: ObjectRef) { path = validPath + ref }
    val canvas: @Composable (Modifier) -> Unit = { m ->
        MapCanvas(scene, if (container == null) image else null, selection, i18n,
            onSelect = { selection = it }, onOpen = { open(it.ref) },
            onMove = { ref, p -> actions.update(ObjectMap.place(project, areaId, ref.type, ref.id, p), i18n.text("text.6f590fa5345d")) },
            onRoute = { link, pts -> scene.route(project, link, pts)?.let { actions.update(ObjectMap.saveRoute(project, it), i18n.text("text.e020099624b5")) } },
            onLongPress = { actions.add(null, it) }, modifier = m)
    }
    val pane: @Composable (Modifier) -> Unit = { m ->
        MapDetailPane(project, scene, selection, i18n, actions, onSelect = { selection = it }, onOpen = ::open, media = media, modifier = m, hierarchy = hierarchy)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val trail: @Composable RowScope.() -> Unit = {
            if (validPath.isNotEmpty()) TextButton(onClick = { path = validPath.dropLast(1) }) { Text("‹ " + i18n.text("map.back")) }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { path = emptyList() }, enabled = validPath.isNotEmpty()) { Text(i18n.text("map.floor")) }
                validPath.forEachIndexed { i, ref ->
                    Text("›")
                    TextButton(onClick = { path = validPath.take(i + 1) }, enabled = i < validPath.lastIndex) { Text(MapStyle.shortName(ObjectHierarchy.name(project, ref, i18n), 18)) }
                }
            }
        }
        val commands: @Composable RowScope.(Boolean) -> Unit = { narrow ->
            Button(onClick = { actions.add(container, null) }) { Text(i18n.text("text.84cbef7b19b8")) }
            if (narrow) FilterChip(selected = listOpen, onClick = { listOpen = !listOpen }, label = { Text(i18n.text("map.list")) })
            toolbar()
        }
        // Phones get breadcrumb and commands on two rows so neither is clipped.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow = maxWidth < 840.dp
            if (maxWidth >= 600.dp) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { trail(); commands(narrow) }
            else Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { trail() }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { commands(true) }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val showPane = selection != null || container != null || listOpen || maxWidth >= 840.dp
            val paneMax = maxHeight * .45f
            if (maxWidth >= 840.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                canvas(Modifier.weight(1f).fillMaxHeight())
                pane(Modifier.width(360.dp).fillMaxHeight())
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                canvas(Modifier.fillMaxWidth().weight(1f))
                if (showPane) pane(Modifier.fillMaxWidth().heightIn(max = paneMax))
            }
        }
    }
}

@Composable
fun MapDetailPane(project: Project, scene: MapScene, selection: MapSelection?, i18n: Messages, actions: MapActions,
                  onSelect: (MapSelection?) -> Unit, onOpen: (ObjectRef) -> Unit, media: @Composable (ObjectRef) -> Unit, modifier: Modifier = Modifier,
                  hierarchy: HierarchyIndex = remember(project) { HierarchyIndex(project) }) {
    val index = remember(project) { ProjectIndex(project) }
    Surface(modifier.testTag("map-detail"), tonalElevation = 2.dp, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (selection) {
                is MapSelection.Node -> scene.node(selection.ref)?.let { node ->
                    NodeDetails(project, index, hierarchy, scene, node, i18n, actions, onSelect, onOpen, media, current = false)
                }
                is MapSelection.Link -> selection.cableIds.firstNotNullOfOrNull { scene.linkOf(it) }?.let { link ->
                    LinkDetails(project, index, scene, link, selection.cableId, i18n, actions, onSelect)
                }
                null -> {
                    scene.container?.let { ref ->
                        val node = remember(project, ref) { containerNode(project, scene.areaId, ref) }
                        NodeDetails(project, index, hierarchy, scene, node, i18n, actions, onSelect, onOpen, media, current = true)
                    }
                    SceneObjects(scene, i18n, onSelect)
                    if (scene.container == null) FloorHint(i18n, expanded = scene.nodes.isEmpty())
                }
            }
        }
    }
}

/** Summary of the open container, taken from the view that contains it. */
private fun containerNode(project: Project, areaId: String, ref: ObjectRef): SceneNode {
    val parent = ObjectHierarchy.parent(project, ref)
    val outer = if (parent == null) MapScene.area(project, areaId) else MapScene.container(project, parent)
    return outer.node(ref) ?: SceneNode(ref, ObjectHierarchy.name(project, ref), ObjectGlyph.RACK, MapPoint(.5f, .5f), true, 0, 0, 0, emptyList())
}

@Composable
private fun GlyphBadge(glyph: Glyph) {
    Box(Modifier.size(40.dp).background(MapStyle.family(glyph.family), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Text(glyph.code, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

/** Same objects as the canvas, as a list: the non-touch way (screen reader, keyboard) to select them. */
@Composable
private fun SceneObjects(scene: MapScene, i18n: Messages, onSelect: (MapSelection?) -> Unit) {
    Text(i18n.text("map.objects", scene.nodes.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    if (scene.nodes.isEmpty()) Text(i18n.text("map.noObjects"), style = MaterialTheme.typography.bodySmall)
    scene.nodes.sortedBy { it.name.lowercase() }.forEach { node ->
        TextButton(onClick = { onSelect(MapSelection.Node(node.ref)) }, modifier = Modifier.fillMaxWidth()) {
            Text(node.glyph.code, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
            Text(node.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (node.isContainer && node.childCount > 0) Text(i18n.text("map.children", node.childCount), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FloorHint(i18n: Messages, expanded: Boolean) {
    var open by remember(expanded) { mutableStateOf(expanded) }
    Text(i18n.text("map.hint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = { open = !open }) { Text(i18n.text("map.legend") + if (open) " ▴" else " ▾") }
    if (!open) return
    LinkMedium.entries.forEach { m ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val color = MapStyle.medium(setOf(m), MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.Canvas(Modifier.width(32.dp).height(8.dp)) {
                drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2), 6f, pathEffect = MapStyle.dash(setOf(m), 4f))
            }
            Text(i18n.text("map.medium.${m.name}"), style = MaterialTheme.typography.bodySmall)
        }
    }
    Text(i18n.text("map.legendNodes"), style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeDetails(project: Project, index: ProjectIndex, hierarchy: HierarchyIndex, scene: MapScene, node: SceneNode, i18n: Messages, actions: MapActions,
                        onSelect: (MapSelection?) -> Unit, onOpen: (ObjectRef) -> Unit, media: @Composable (ObjectRef) -> Unit, current: Boolean) {
    val device = index.device(node.ref.id).takeIf { node.ref.type == PlacementTargetType.DEVICE }
    val type = ObjectCatalog.type(project, device?.objectTypeId)
    var assigning by remember(node.ref) { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlyphBadge(node.glyph)
        Column(Modifier.weight(1f)) {
            Text(node.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(if (node.ref.type == PlacementTargetType.RACK) i18n.text("text.4cd265c2b8c6") else type?.let { ObjectCatalog.displayName(it, i18n) },
                familyLabel(node.glyph.family, i18n)).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        }
        if (!current) TextButton(onClick = { onSelect(null) }) { Text("✕") }
    }
    val facts = listOfNotNull(
        device?.ipAddress?.let { "IP $it" }, device?.physicalLabel, device?.serialNumber?.let { "S/N $it" },
        node.childCount.takeIf { node.isContainer }?.let { i18n.text("map.children", it) },
        node.portsTotal.takeIf { it > 0 }?.let { i18n.text("map.portsUsage", node.portsUsed, it) },
        node.internalCables.size.takeIf { it > 0 }?.let { i18n.text("map.internal", it) },
        device?.hardware?.poeBudgetWatts?.let { i18n.text("map.poeLoad", PortLogic.poeLoad(project, device).toInt(), it.toInt()) },
    )
    if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
    if (device != null && device.ports.isNotEmpty()) {
        val cells = remember(project, device.id) { PortLogic.panel(project, device, index = index) }
        PortPanel(cells, i18n, compact = true, onPanelClick = { actions.edit(editDraft(project, node.ref, scene.areaId, i18n), ConfiguratorPage.PORTS) })
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (node.isContainer && !current) Button(onClick = { onOpen(node.ref) }) { Text(i18n.text("map.open")) }
        OutlinedButton(onClick = { actions.edit(editDraft(project, node.ref, scene.areaId, i18n), ConfiguratorPage.ESSENTIALS) }) { Text(i18n.text("map.edit")) }
        if (device != null) OutlinedButton(onClick = { actions.edit(editDraft(project, node.ref, scene.areaId, i18n), ConfiguratorPage.PORTS) }) { Text(i18n.text("ux.ports")) }
        if (current) {
            OutlinedButton(onClick = { actions.add(node.ref, null) }) { Text(i18n.text("map.addHere")) }
            TextButton(onClick = { assigning = !assigning }) { Text(i18n.text("text.d6d7a4656267")) }
        }
        if (!current && scene.container != null) TextButton(onClick = {
            actions.update(ObjectHierarchy.assign(project, node.ref, null, i18n), i18n.text("text.30541a9c6cee")); onSelect(null)
        }) { Text(i18n.text("text.38cdc675224c")) }
    }
    if (assigning) {
        val candidates = remember(project, node.ref) {
            val ancestors = hierarchy.ancestors(node.ref).toSet()
            ObjectHierarchy.refs(project).filter { it != node.ref && it !in ancestors && hierarchy.parents[it] != node.ref && hierarchy.areaId(it) == scene.areaId }
                .map { it to ObjectHierarchy.name(project, it, i18n) }.sortedBy { it.second.lowercase() }
        }
        if (candidates.isEmpty()) Text(i18n.text("text.032265877af8"), style = MaterialTheme.typography.bodySmall)
        candidates.forEach { (child, name) ->
            TextButton(onClick = { actions.update(ObjectHierarchy.assign(project, child, node.ref, i18n), i18n.text("text.30541a9c6cee")); assigning = false }, modifier = Modifier.fillMaxWidth()) {
                Text(name, modifier = Modifier.weight(1f))
            }
        }
    }
    val links = scene.links.filter { it.a == node.ref || it.b == node.ref }
    if (links.isNotEmpty()) {
        Text(i18n.text("map.cables"), style = MaterialTheme.typography.titleSmall)
        links.forEach { link ->
            val other = (if (link.a == node.ref) link.b else link.a)?.let { scene.node(it)?.name } ?: i18n.text("map.outside")
            TextButton(onClick = { onSelect(MapSelection.Link(link.cableIds)) }, modifier = Modifier.fillMaxWidth()) {
                Text("→ $other", modifier = Modifier.weight(1f)); Text(i18n.text("map.cableCount", link.cableIds.size))
            }
        }
    }
    media(node.ref)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkDetails(project: Project, index: ProjectIndex, scene: MapScene, link: SceneLink, cableId: String?, i18n: Messages, actions: MapActions, onSelect: (MapSelection?) -> Unit) {
    val ends = remember(project) { CableEnds(project) }
    fun endLabel(cable: Cable, first: Boolean): String {
        val port = if (first) cable.portAId else cable.portBId
        val device = ends.device(cable, first) ?: return i18n.text("map.unknown")
        val area = ObjectMap.areaId(project, device)
        val where = if (area != null && area != scene.areaId) " (${index.areaName(area)})" else ""
        return (port?.let(index::portLabel) ?: device.technicalName) + where
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${scene.node(link.a)?.name ?: i18n.text("map.outside")} ⟷ ${scene.node(link.b)?.name ?: i18n.text("map.outside")}",
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
        TextButton(onClick = { onSelect(null) }) { Text("✕") }
    }
    Text(listOf(i18n.text("map.cableCount", link.cableIds.size)) .plus(link.media.map { i18n.text("map.medium.${it.name}") }).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
    link.cableIds.mapNotNull { id -> project.cables.find { it.id == id } }.forEach { cable ->
        val selected = cable.id == cableId
        Surface(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().clickable { onSelect(MapSelection.Link(link.cableIds, cable.id)) }) {
            Column(Modifier.padding(8.dp)) {
                Text(cable.codeOrLabel ?: i18n.text("text.89dbe18e8407"), style = MaterialTheme.typography.labelLarge)
                Text("${endLabel(cable, true)} → ${endLabel(cable, false)}", style = MaterialTheme.typography.bodySmall)
                Text(cable.medium.toDisplayString(i18n), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (selected) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { actions.edit(MapObjectDraft.cable(project, floorBusinessUnit(project, scene.areaId), scene.areaId, cable.id, i18n), ConfiguratorPage.ESSENTIALS) }) { Text(i18n.text("text.478f91db1996")) }
        }
    }
    if (scene.editable) Text(i18n.text("map.routeHint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** "SW-03": glyph code plus the first free two-digit number among existing names. */
fun suggestName(project: Project, type: ObjectType): String {
    val used = (project.businessUnits.flatMap { it.devices }.map { it.technicalName } + project.racks.map { it.name }).toSet()
    val code = ObjectGlyph.of(type).code
    return generateSequence(1) { it + 1 }.map { "$code-${it.toString().padStart(2, '0')}" }.first { it !in used }
}

/** Draft for an object added from the map: inside [parent], or on the floor at [point]. */
fun newObjectDraft(project: Project, type: ObjectType, preset: com.onlyfield.assetmanager.core.forms.PresetResult?, areaId: String, parent: ObjectRef?, point: MapPoint?): MapObjectDraft {
    val bu = floorBusinessUnit(project, areaId)
    val base = if (parent != null) MapObjectDraft.newObject(project, type, bu, areaId, parent) else MapObjectDraft(type = type, buId = bu, areaId = areaId, mapPoint = point)
    val named = when (type.kind) {
        ObjectKind.DEVICE -> base.copy(device = base.device.copy(technicalName = suggestName(project, type)))
        ObjectKind.RACK -> base.copy(rack = base.rack.copy(name = suggestName(project, type)))
        ObjectKind.CABLE -> base
    }
    return preset?.let { com.onlyfield.assetmanager.core.forms.DevicePresets.apply(named, it) } ?: named
}
