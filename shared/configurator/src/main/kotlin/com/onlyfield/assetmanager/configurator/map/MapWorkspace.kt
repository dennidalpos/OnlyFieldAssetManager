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
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    /** Shows [focus] on floor [areaId] (any business unit); null hides "Go to". */
    val goTo: ((areaId: String, focus: ObjectRef) -> Unit)? = null,
)

/** Business unit that owns the floor, used for new or legacy objects without one. */
fun floorBusinessUnit(project: Project, areaId: String): String = ObjectMap.floorBusinessUnit(project, areaId)

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
    /** Object to open and select on arrival, e.g. after "Go to" from another floor. */
    focus: ObjectRef? = null,
) {
    val hierarchy = remember(project) { HierarchyIndex(project) }
    val arrival = focus?.takeIf { hierarchy.areaId(it) == areaId }
    var path by remember(areaId, arrival) { mutableStateOf(arrival?.let { hierarchy.ancestors(it).reversed() }.orEmpty()) }
    // Keep levels only while each is still a child of the previous one on this floor (deleted or moved levels are dropped).
    val validPath = path.withIndex().takeWhile { (i, ref) ->
        hierarchy.parents[ref] == path.getOrNull(i - 1) && (i > 0 || hierarchy.areaId(ref) == areaId)
    }.map { it.value }
    if (validPath != path) SideEffect { path = validPath }
    val container = validPath.lastOrNull()
    var selection by remember(areaId, arrival) { mutableStateOf<MapSelection?>(arrival?.let { MapSelection.Node(it) }) }
    val scene = remember(project, areaId, container) { if (container == null) MapScene.area(project, areaId) else MapScene.container(project, container) }
    // Selections that the current view no longer shows are dropped.
    when (val s = selection) {
        is MapSelection.Node -> if (scene.node(s.ref) == null) selection = null
        is MapSelection.Link -> if (s.cableIds.none { scene.linkOf(it) != null }) selection = null
        null -> Unit
    }
    // Narrow windows hide the pane until something is selected; "List" opens it on demand.
    var listOpen by remember(areaId) { mutableStateOf(false) }

    val floorName = remember(project, areaId) { ProjectIndex(project).area(areaId)?.name } ?: i18n.text("map.floor")
    fun go(levels: List<ObjectRef>) { path = levels; selection = null }
    fun open(ref: ObjectRef) = go(validPath + ref)
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
            if (validPath.isNotEmpty()) TextButton(onClick = { go(validPath.dropLast(1)) }) { Text("‹ " + i18n.text("map.back")) }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { go(emptyList()) }, enabled = validPath.isNotEmpty()) { Text(MapStyle.shortName(floorName, 24)) }
                validPath.forEachIndexed { i, ref ->
                    Text("›", Modifier.clearAndSetSemantics {})
                    TextButton(onClick = { go(validPath.take(i + 1)) }, enabled = i < validPath.lastIndex) { Text(MapStyle.shortName(ObjectHierarchy.name(project, ref, i18n), 18)) }
                }
            }
        }
        val commands: @Composable RowScope.(Boolean) -> Unit = { narrow ->
            Button(onClick = { actions.add(container, null) }) { Text(i18n.text(if (container == null) "map.addObject" else "map.addHere")) }
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
