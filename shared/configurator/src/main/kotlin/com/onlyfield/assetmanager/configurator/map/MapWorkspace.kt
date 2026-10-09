package com.onlyfield.assetmanager.configurator.map

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.configurator.OverflowActions
import com.onlyfield.assetmanager.configurator.PaneAction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.onlyfield.assetmanager.configurator.SymbolIcons
import com.onlyfield.assetmanager.configurator.SymbolButton
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.ConfiguratorPage
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/** Host operations; the map never opens editors or dialogs on its own. */
class MapActions(
    val update: (Project, String) -> Unit,
    val edit: (MapObjectDraft, ConfiguratorPage) -> Unit,
    /** New object inside [parent] (or on the floor at [point]). */
    val add: (parent: ObjectRef?, point: MapPoint?) -> Unit,
    /** Shows [focus] on floor [areaId] (any site); null hides "Go to". */
    val goTo: ((areaId: String, focus: ObjectRef) -> Unit)? = null,
    /** Moves a device or rack to the trash (host storage); null hides "Move to trash". */
    val trash: ((ObjectRef) -> Unit)? = null,
    /** Takes or picks a photo for a device, rack, port or cable; null hides the photo buttons. */
    val photo: ((AttachmentTargetType, String) -> Unit)? = null,
)

/** site that owns the floor, used for new or legacy objects without one. */
fun floorSite(project: Project, areaId: String): String = ObjectMap.floorSite(project, areaId)

fun editDraft(project: Project, ref: ObjectRef, areaId: String, i18n: Messages): MapObjectDraft {
    val site = ProjectIndex(project).siteOf(ref.id)?.id ?: floorSite(project, areaId)
    return if (ref.type == PlacementTargetType.RACK) MapObjectDraft.rack(project, site, areaId, ref.id) else MapObjectDraft.device(project, site, areaId, ref.id, i18n)
}

/**
 * Floor map with drill-down into containers and an adaptive, non-modal detail pane:
 * side panel from 840 dp; expandable selection card below.
 */
@Composable
fun MapWorkspace(
    project: Project,
    areaId: String,
    image: ImageBitmap?,
    i18n: Messages,
    actions: MapActions,
    modifier: Modifier = Modifier,
    /** Secondary map tools. */
    tools: List<PaneAction> = emptyList(),
    media: @Composable (ObjectRef) -> Unit = {},
    /** Object to open and select on arrival, e.g. after "Go to" from another floor. */
    focus: ObjectRef? = null,
    state: FloorUiState = remember(project.id, areaId) { FloorUiState() },
    onFocusHandled: () -> Unit = {},
    onSearch: (() -> Unit)? = null,
    onLeaveFloor: (() -> Unit)? = null,

) {
    val hierarchy = remember(project) { HierarchyIndex(project) }
    val arrival = focus?.takeIf { hierarchy.areaId(it) == areaId }
    var path by state::path
    LaunchedEffect(arrival) {
        if (arrival != null) {
            path = hierarchy.ancestors(arrival).reversed()
            state.selection = MapSelection.Node(arrival)
            onFocusHandled()
        }
    }
    // Keep levels only while each is still a child of the previous one on this floor (deleted or moved levels are dropped).
    val validPath = path.withIndex().takeWhile { (i, ref) ->
        hierarchy.parents[ref] == path.getOrNull(i - 1) && (i > 0 || hierarchy.areaId(ref) == areaId)
    }.map { it.value }
    if (validPath != path) SideEffect { path = validPath }
    val container = validPath.lastOrNull()
    var selection by state::selection
    val scene = remember(project, areaId, container) { if (container == null) MapScene.area(project, areaId) else MapScene.container(project, container) }
    // Selections that the current view no longer shows are dropped.
    when (val s = selection) {
        is MapSelection.Node -> if (scene.node(s.ref) == null) selection = null
        is MapSelection.Link -> if (s.cableIds.none { scene.linkOf(it) != null }) selection = null
        null -> Unit
    }
    // Narrow windows hide the pane until something is selected; "List" opens it on demand.
    var listOpen by state::listOpen

    val siteName = project.sites.firstOrNull { site -> site.areas.any { it.id == areaId } }?.name.orEmpty()
    val floorName = remember(project, areaId) { ProjectIndex(project).area(areaId)?.name } ?: i18n.text("map.floor")
    fun go(levels: List<ObjectRef>) { path = levels; selection = null; state.expanded = false; listOpen = false }
    fun open(ref: ObjectRef) = go(validPath + ref)
    val canvas: @Composable (Modifier) -> Unit = { m ->
        MapCanvas(scene, if (container == null) image else null, selection, i18n,
            onSelect = { selection = it; state.expanded = false }, onOpen = { open(it.ref) },
            onMove = { ref, p -> actions.update(ObjectMap.place(project, areaId, ref.type, ref.id, p), i18n.text("text.6f590fa5345d")) },
            onRoute = { link, pts -> scene.route(project, link, pts)?.let { actions.update(ObjectMap.saveRoute(project, it), i18n.text("text.e020099624b5")) } },
            onLongPress = { actions.add(container, it) }, modifier = m.clip(MaterialTheme.shapes.medium).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium), camera = state.camera(container))
    }
    val latestPane by rememberUpdatedState<@Composable (Modifier) -> Unit>({ m ->
        MapDetailPane(project, scene, selection, i18n, actions, onSelect = { selection = it }, onOpen = ::open, media = media, modifier = m, hierarchy = hierarchy)
    })
    val pane = remember { movableContentOf<Modifier> { m -> latestPane(m) } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val levels = listOf(floorName) + validPath.map { ObjectHierarchy.name(project, it, i18n) }
        // Back plus the current level; the levels above sit in a menu instead of a scrolling trail.
        val trail: @Composable RowScope.() -> Unit = {
            if (validPath.isNotEmpty()) TextButton(onClick = { go(validPath.dropLast(1)) }) { Icon(SymbolIcons.back, null); Text(i18n.text("map.back")) }
            Box(Modifier.weight(1f)) {
                var open by remember { mutableStateOf(false) }
                TextButton(onClick = { open = true }) {
                    Text((listOf(siteName) + levels).filter { it.isNotBlank() }.joinToString(" / "), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(SymbolIcons.expand, null)
                }
                DropdownMenu(open, { open = false }) {
                    onLeaveFloor?.let { leave -> DropdownMenuItem(text = { Text(siteName) }, onClick = { open = false; leave() }) }
                    levels.dropLast(1).forEachIndexed { i, name ->
                        DropdownMenuItem(text = { Text("  ".repeat(i) + MapStyle.shortName(name, 32)) }, onClick = { open = false; go(validPath.take(i)) })
                    }
                }
            }
        }
        val addLabel = i18n.text(if (container == null) "map.addObject" else "map.addHere")
        BoxWithConstraints(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)) {
            val narrow = maxWidth < 840.dp
            if (maxWidth >= 600.dp) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                trail()
                onSearch?.let { search -> SymbolButton(SymbolIcons.search, i18n.text("search.action"), search) }
                Button(onClick = { actions.add(container, null) }) { Text(addLabel) }
                if (narrow) FilterChip(selected = listOpen, onClick = { listOpen = !listOpen; state.expanded = listOpen }, label = { Text(i18n.text("map.list")) })
                OverflowActions(i18n, tools)
            } else Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { trail(); onSearch?.let { search -> SymbolButton(SymbolIcons.search, i18n.text("search.action"), search) }; OverflowActions(i18n, tools) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { actions.add(container, null) }, modifier = Modifier.weight(1f)) { Text(addLabel) }
                    FilterChip(selected = listOpen, onClick = { listOpen = !listOpen; state.expanded = listOpen }, label = { Text(i18n.text("map.list")) })
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            SideEffect { state.wideDetails = maxWidth >= 840.dp }
            val showPane = selection != null || container != null || listOpen || maxWidth >= 840.dp
            if (maxWidth >= 840.dp && (selection != null || container != null || listOpen)) SideEffect { state.expanded = true }
            if (maxWidth >= 840.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                canvas(Modifier.weight(1f).fillMaxHeight())
                pane(Modifier.width(360.dp).fillMaxHeight())
            } else Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!state.expanded || !showPane) canvas(Modifier.fillMaxWidth().weight(1f))
                    if (showPane && !state.expanded) {
                        Column(Modifier.fillMaxWidth().heightIn(max = this@BoxWithConstraints.maxHeight * .4f).verticalScroll(rememberScrollState())) {
                            MapSummary(project, scene, selection, i18n, actions, { selection = null; listOpen = false }, { state.expanded = true })
                        }
                    }
                }
                if (showPane && state.expanded) Surface(Modifier.fillMaxSize(), shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxSize()) {
                        TextButton(onClick = { state.expanded = false }, modifier = Modifier.align(Alignment.End)) { Text(i18n.text("ux.reduce")) }
                        pane(Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
        }
    }
}

/** "SW-03": glyph code plus the first free two-digit number among existing names. */
fun suggestName(project: Project, type: ObjectType): String {
    val used = (project.sites.flatMap { it.devices }.map { it.technicalName } + project.racks.map { it.name }).toSet()
    val code = ObjectGlyph.of(type).code
    return generateSequence(1) { it + 1 }.map { "$code-${it.toString().padStart(2, '0')}" }.first { it !in used }
}

/** Draft for an object added from the map: inside [parent], or on the floor at [point]. */
fun newObjectDraft(project: Project, type: ObjectType, preset: com.onlyfield.assetmanager.core.forms.PresetResult?, areaId: String, parent: ObjectRef?, point: MapPoint?): MapObjectDraft {
    val site = floorSite(project, areaId)
    val base = if (parent != null) MapObjectDraft.newObject(project, type, site, areaId, parent) else MapObjectDraft(type = type, siteId = site, areaId = areaId, mapPoint = point)
    val named = when (type.kind) {
        ObjectKind.DEVICE -> base.copy(device = base.device.copy(technicalName = suggestName(project, type)))
        ObjectKind.RACK -> base.copy(rack = base.rack.copy(name = suggestName(project, type)))
        ObjectKind.CABLE -> base
    }
    return preset?.let { com.onlyfield.assetmanager.core.forms.DevicePresets.apply(named, it) } ?: named
}
