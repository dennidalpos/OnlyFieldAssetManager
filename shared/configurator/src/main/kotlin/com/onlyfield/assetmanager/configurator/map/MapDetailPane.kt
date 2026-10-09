package com.onlyfield.assetmanager.configurator.map

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.*
import com.onlyfield.assetmanager.core.display.ObjectSummary
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.CableLabels
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.PortLogic
import com.onlyfield.assetmanager.core.forms.RackLayout
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/**
 * Non-modal detail pane. Fixed order for an object: identity, actions (photo always there),
 * ports, cables, photos, then the other details collapsed. Without a selection it lists the view's objects.
 */
@Composable
fun MapDetailPane(project: Project, scene: MapScene, selection: MapSelection?, i18n: Messages, actions: MapActions,
                  onSelect: (MapSelection?) -> Unit, onOpen: (ObjectRef) -> Unit, media: @Composable (ObjectRef) -> Unit, modifier: Modifier = Modifier,
                  hierarchy: HierarchyIndex = remember(project) { HierarchyIndex(project) }) {
    val index = remember(project) { ProjectIndex(project) }
    var assigning by remember(scene.container) { mutableStateOf(false) }
    Surface(modifier.testTag("map-detail"), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.medium) {
        // A new selection starts at the top of the pane.
        Column(Modifier.then(if (selection is MapSelection.Node) Modifier else Modifier.verticalScroll(remember(selection, scene.container) { ScrollState(0) })).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    SceneObjects(scene, i18n, onSelect, onAssign = scene.container?.let { { assigning = true } })
                    if (scene.container == null) FloorHint(i18n)
                }
            }
        }
    }
    val container = scene.container
    if (assigning && container != null) AssignObjectDialog(project, index, hierarchy, scene.areaId, container, i18n, onClose = { assigning = false }) { child ->
        actions.update(ObjectHierarchy.assign(project, child, container, i18n), i18n.text("text.30541a9c6cee")); assigning = false
    }
}

/** Summary of the open container, taken from the view that contains it. */
private fun containerNode(project: Project, areaId: String, ref: ObjectRef): SceneNode {
    val parent = ObjectHierarchy.parent(project, ref)
    val outer = if (parent == null) MapScene.area(project, areaId) else MapScene.container(project, parent)
    return outer.node(ref) ?: SceneNode(ref, ObjectHierarchy.name(project, ref), ObjectGlyph.RACK, MapPoint(.5f, .5f), true, 0, 0, 0, emptyList())
}

/** Same objects as the canvas, as a list: the non-touch way (screen reader, keyboard) to select them. */
@Composable
private fun SceneObjects(scene: MapScene, i18n: Messages, onSelect: (MapSelection?) -> Unit, onAssign: (() -> Unit)?) {
    var query by remember(scene.areaId, scene.container) { mutableStateOf("") }
    val title = i18n.text(if (scene.container != null) "map.contents" else "map.objectsTitle")
    SectionTitle(title, scene.nodes.size) {
        onAssign?.let { TextButton(onClick = it) { Text(i18n.text("map.assignExisting")) } }
    }
    if (scene.nodes.isEmpty()) { Text(i18n.text("map.noObjects"), style = MaterialTheme.typography.bodySmall); return }
    if (scene.nodes.size > 10) OutlinedTextField(query, { query = it }, label = { Text(i18n.text("map.filter")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    val shown = scene.nodes.filter { query.isBlank() || it.name.contains(query.trim(), true) }
    val groups = shown.groupBy { it.glyph.family }.toSortedMap(compareBy { it.ordinal })
    groups.forEach { (family, nodes) ->
        if (groups.size > 1) Text(familyLabel(family, i18n), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        nodes.sortedBy { it.name.lowercase() }.forEach { node ->
            TextButton(onClick = { onSelect(MapSelection.Node(node.ref)) }, modifier = Modifier.fillMaxWidth()) {
                GlyphBadge(node.glyph, 28.dp)
                Spacer(Modifier.width(10.dp))
                Text(node.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (node.isContainer && node.childCount > 0) Text(i18n.plural("map.objectCount", node.childCount), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (shown.isEmpty()) Text(i18n.text("ux.noResults"), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun FloorHint(i18n: Messages) {
    var open by remember { mutableStateOf(false) }
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

@Composable
private fun ColumnScope.NodeDetails(project: Project, index: ProjectIndex, hierarchy: HierarchyIndex, scene: MapScene, node: SceneNode, i18n: Messages, actions: MapActions,
                        onSelect: (MapSelection?) -> Unit, onOpen: (ObjectRef) -> Unit, media: @Composable (ObjectRef) -> Unit, current: Boolean) {
    val device = index.device(node.ref.id).takeIf { node.ref.type == PlacementTargetType.DEVICE }
    val type = ObjectCatalog.type(project, device?.objectTypeId)
    val summary = remember(project, node.ref) { ObjectSummary.of(project, node.ref, i18n, index, hierarchy) }
    fun edit(page: ConfiguratorPage) = actions.edit(editDraft(project, node.ref, scene.areaId, i18n), page)
    val typeLabel = if (node.ref.type == PlacementTargetType.RACK) i18n.text("text.4cd265c2b8c6")
        else type?.let { ObjectCatalog.displayName(it, i18n) } ?: familyLabel(node.glyph.family, i18n)
    PaneHeader(node.glyph, node.name, typeLabel, i18n, overline = if (current) i18n.text("map.openContainer") else null,
        onClose = if (current) null else ({ onSelect(null) }))

    val editAction = PaneAction(i18n.text("map.edit")) { edit(ConfiguratorPage.ESSENTIALS) }
    val portsAction = device?.let { PaneAction(i18n.text("ux.ports")) { edit(ConfiguratorPage.PORTS) } }
    val opens = node.isContainer && !current
    val parentName = scene.container?.let { ObjectHierarchy.name(project, it, i18n) }
    val removeAction = if (!current && parentName != null) PaneAction(i18n.text("map.removeFromContainer")) {
        actions.update(ObjectHierarchy.assign(project, node.ref, null, i18n), i18n.text("map.removedFrom", parentName)); onSelect(null)
    } else null
    val trash = actions.trash
    val delete = if (current || trash == null) null else DeleteRequest(i18n.text("text.dd41b3275173"), i18n.text("text.87fc0efddabf", node.name),
        i18n.text(if (node.ref.type == PlacementTargetType.RACK) "text.309921cb8d51" else "text.2548407c6a6b")) { onSelect(null); trash(node.ref) }
    val target = if (node.ref.type == PlacementTargetType.RACK) AttachmentTargetType.RACK else AttachmentTargetType.DEVICE
    val photoAction = actions.photo?.let { photo -> PaneAction(i18n.text("quick.photo")) { photo(target, node.ref.id) } }
    ActionRow(i18n,
        primary = if (opens) PaneAction(i18n.text("map.open")) { onOpen(node.ref) } else if (current) null else editAction,
        photo = photoAction,
        overflow = listOfNotNull(editAction.takeIf { opens || current }, portsAction, removeAction),
        delete = delete)

    val rack = index.rack(node.ref.id).takeIf { node.ref.type == PlacementTargetType.RACK }
    // Primary: what is needed to find, label and connect the object.
    val primary = listOfNotNull(
        rack?.let { ObjectSummary.Fact(i18n.text("map.fact.rackUnits"), i18n.text("map.rackUnitsValue", RackLayout.usedUnits(it, index.devices), it.heightU)) },
        node.childCount.takeIf { node.isContainer && !current }?.let { ObjectSummary.Fact(i18n.text("map.contents"), i18n.plural("map.objectCount", it)) },
        summary.mount?.let { ObjectSummary.Fact(i18n.text("map.fact.mount"), it) },
        device?.physicalLabel?.takeIf { it.isNotBlank() }?.let { ObjectSummary.Fact(i18n.text("map.fact.label"), it) },
        device?.operationalStatus?.takeIf { it != OperationalStatus.IN_SERVICE }?.let { ObjectSummary.Fact(i18n.text("device.status"), it.toDisplayString(i18n)) },
    )
    Column(Modifier.then(if (current) Modifier else Modifier.weight(1f).verticalScroll(remember(node.ref) { ScrollState(0) })), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    FactRows(primary)
    var quickPort by remember(node.ref) { mutableStateOf<String?>(null) }
    device?.let { d ->
        SectionTitle(i18n.text("ux.ports")) { Text("${node.portsUsed}/${node.portsTotal}", style = MaterialTheme.typography.labelMedium) }
        val cells = remember(project, d.id) { PortLogic.panel(project, d, index = index) }
        DeviceDrawing(project, d, i18n, showName = false, onPort = { quickPort = it.port.id })
        // Field documentation loop: open the next cabled port still without a photo.
        val missing = cells.filter { it.photoMissing }
        if (missing.isNotEmpty() && actions.photo != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(i18n.plural("photo.missing", missing.size), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { quickPort = missing.first().port.id }) { Text(i18n.text("photo.nextMissing")) }
        }
    }
    quickPort?.let { id ->
        PortQuickDialog(project, id, i18n, PortQuickActions(update = actions.update, photo = actions.photo,
            details = { port -> actions.edit(editDraft(project, node.ref, scene.areaId, i18n).copy(focusPortId = port.id), ConfiguratorPage.PORTS) },
            openDevice = { far -> quickPort = null; ObjectMap.areaId(project, far)?.let { area -> actions.goTo?.invoke(area, ObjectRef(PlacementTargetType.DEVICE, far.id)) } }),
            onClose = { quickPort = null })
    }

    val links = scene.links.filter { it.a == node.ref || it.b == node.ref }
    if (links.isNotEmpty() && !current) {
        SectionTitle(i18n.text("map.cables"), links.size)
        links.forEach { link ->
            val other = (if (link.a == node.ref) link.b else link.a)?.let { scene.node(it)?.name } ?: stubLabel(scene, link, i18n) ?: i18n.text("map.outside")
            TextButton(onClick = { onSelect(MapSelection.Link(link.cableIds)) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(other, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(link.media.joinToString(" · ") { i18n.text("map.medium.${it.name}") }, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(i18n.plural("map.cableCount", link.cableIds.size), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    // Photos are a primary section: always shown, even before the first shot.
    if (!current) {
        val attachments = project.attachments.count { it.targetId == node.ref.id && it.targetType == target }
        SectionTitle(i18n.text("ux.attachments"), attachments.takeIf { it > 0 }) {
            photoAction?.let { a -> TextButton(onClick = a.onClick) { Text(i18n.text("map.addPhoto")) } }
        }
        if (attachments > 0) media(node.ref)
        else Text(i18n.text("map.noPhotos"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // Secondary: technical details, collapsed.
    val secondary = listOfNotNull(
        node.internalCables.size.takeIf { it > 0 }?.let { ObjectSummary.Fact(i18n.text("map.fact.internal"), it.toString()) },
        device?.hardware?.poeBudgetWatts?.let { ObjectSummary.Fact(i18n.text("map.fact.poe"), "${PortLogic.poeLoad(project, device).toInt()}/${it.toInt()} W") },
    ) + (if (device != null && !current) summary.identity.filterNot { it.value == device.physicalLabel } else emptyList())
    if (secondary.isNotEmpty() || (device != null && !current)) {
        var open by remember(node.ref) { mutableStateOf(false) }
        TextButton(onClick = { open = !open }) { Text(i18n.text("map.moreDetails") + if (open) " ▴" else " ▾") }
        if (open) {
            FactRows(secondary)
            if (device != null && !current) LogicalLinksSection(project, hierarchy, device, i18n, actions)
        }
    }
    }
}

/** WAN/VPN links of a device, with creation and editing in a dialog. */
@Composable
private fun LogicalLinksSection(project: Project, hierarchy: HierarchyIndex, device: Device, i18n: Messages, actions: MapActions) {
    val links = remember(project, device.id) { LogicalLinks.of(project, device.id) }
    var editing by remember(device.id) { mutableStateOf<WanVpnConnection?>(null) }
    var open by remember(device.id) { mutableStateOf(false) }
    SectionTitle(i18n.text("map.logicalLinks"), links.size.takeIf { it > 0 }) {
        TextButton(onClick = { editing = null; open = true }) { Text(i18n.text("map.newLogicalLink")) }
    }
    links.forEach { link ->
        val farRef = LogicalLinks.far(link, device.id).first?.let { ObjectRef(PlacementTargetType.DEVICE, it) }
        val farArea = farRef?.let(hierarchy::areaId)
        val towards = LogicalLinks.farLabel(project, link, device.id) ?: i18n.text("text.65389ba5d2fd")
        TextButton(onClick = { editing = link; open = true }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("${link.type.toDisplayString(i18n)} · ${link.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(link.providerOrCarrier, link.bandwidth, i18n.text("map.towards", towards)).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val goTo = actions.goTo
        if (goTo != null && farRef != null && farArea != null) TextButton(onClick = { goTo(farArea, farRef) }) {
            Text(i18n.text("map.goTo", ObjectHierarchy.name(project, farRef, i18n)))
        }
    }
    if (open) LogicalLinkDialog(project, device.id, editing, i18n, onClose = { open = false }) { saved ->
        val updated = if (editing == null) ProjectEdits.addWanVpnConnection(project, saved) else ProjectEdits.updateWanVpnConnection(project, saved)
        actions.update(updated, i18n.text("text.ec170e822ebb")); open = false
    }
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
    val far = scene.node(link.b)?.name ?: stubLabel(scene, link, i18n)?.removePrefix("→ ") ?: i18n.text("map.outside")
    PaneHeader(null, "${scene.node(link.a)?.name ?: i18n.text("map.outside")} ⟷ $far",
        (listOf(i18n.plural("map.cableCount", link.cableIds.size)) + link.media.map { i18n.text("map.medium.${it.name}") }).joinToString(" · "),
        i18n, onClose = { onSelect(null) })
    SectionTitle(i18n.text("map.cablesTitle"), link.cableIds.size)
    var passageFrom by remember(link) { mutableStateOf<String?>(null) }
    passageFrom?.let { end ->
        PortQuickDialog(project, end, i18n, PortQuickActions(update = actions.update, photo = actions.photo), onClose = { passageFrom = null }, insertPassage = true)
    }
    link.cableIds.mapNotNull { id -> project.cables.find { it.id == id } }.forEach { cable ->
        val selected = cable.id == cableId
        Surface(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().semantics { this.selected = selected }
                .clickable(role = Role.Button) { onSelect(MapSelection.Link(link.cableIds, cable.id)) }) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cable.codeOrLabel ?: CableLabels.suggest(project, cable, index), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text(cable.medium.toDisplayString(i18n), style = MaterialTheme.typography.labelSmall)
                }
                Text("A: ${endLabel(cable, true)}", style = MaterialTheme.typography.bodySmall)
                Text("B: ${endLabel(cable, false)}", style = MaterialTheme.typography.bodySmall)
                val remote = link.remotes[cable.id]
                remote?.let { r ->
                    Text(i18n.text("map.remoteEnd", r.label(name = r.port?.let { index.portLabel(it.id) } ?: r.device.technicalName)), style = MaterialTheme.typography.bodySmall)
                }
                if (selected) FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    actions.photo?.let { photo -> FilledTonalButton(onClick = { photo(AttachmentTargetType.CABLE, cable.id) }) {
                        Text(i18n.text("quick.photo") + (project.attachments.count { it.targetType == AttachmentTargetType.CABLE && it.targetId == cable.id }.takeIf { it > 0 }?.let { " ($it)" } ?: ""))
                    } }
                    Button(onClick = { actions.edit(MapObjectDraft.cable(project, floorSite(project, scene.areaId), scene.areaId, cable.id, i18n), ConfiguratorPage.ESSENTIALS) }) {
                        Text(i18n.text("map.editCable"))
                    }
                    (cable.portAId ?: cable.portBId)?.let { end -> OutlinedButton(onClick = { passageFrom = end }) { Text(i18n.text("quick.insertPassage")) } }
                    val goTo = actions.goTo
                    val target = remote?.areaId
                    if (goTo != null && remote != null && target != null) OutlinedButton(onClick = { goTo(target, remote.ref) }) {
                        Text(i18n.text("map.goTo", remote.device.technicalName))
                    }
                }
                if (selected) DeleteAction(DeleteRequest(i18n.text("text.7efe336bd548"), i18n.text("text.aca453245e79"), i18n.text("text.4d6a1c85c84a")) {
                    val rest = link.cableIds - cable.id
                    onSelect(if (rest.isEmpty()) null else MapSelection.Link(rest))
                    actions.update(ProjectEdits.deleteCable(project, cable.id), i18n.text("text.20c7dd63256f"))
                }, i18n)
            }
        }
    }
    if (scene.editable) Text(i18n.text("map.routeHint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Existing objects of this floor that can move into [container]: searchable, with type and current place. */
@Composable
private fun AssignObjectDialog(project: Project, index: ProjectIndex, hierarchy: HierarchyIndex, areaId: String, container: ObjectRef, i18n: Messages,
                               onClose: () -> Unit, onPick: (ObjectRef) -> Unit) {
    var query by remember { mutableStateOf("") }
    val candidates = remember(project, container) {
        val ancestors = hierarchy.ancestors(container).toSet()
        ObjectHierarchy.refs(project).filter { it != container && it !in ancestors && hierarchy.parents[it] != container && hierarchy.areaId(it) == areaId }
            .map { it to ObjectHierarchy.name(project, it, i18n) }.sortedBy { it.second.lowercase() }
    }
    val shown = candidates.filter { query.isBlank() || it.second.contains(query.trim(), true) }
    AlertDialog(onDismissRequest = onClose, title = { Text(i18n.text("map.assignTitle", ObjectHierarchy.name(project, container, i18n))) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (candidates.size > 7) OutlinedTextField(query, { query = it }, label = { Text(i18n.text("map.filter")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (candidates.isEmpty()) Text(i18n.text("text.032265877af8"), style = MaterialTheme.typography.bodySmall)
            else if (shown.isEmpty()) Text(i18n.text("ux.noResults"), style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(shown, key = { it.first.id }) { (ref, name) ->
                    val device = index.device(ref.id).takeIf { ref.type == PlacementTargetType.DEVICE }
                    val glyph = if (ref.type == PlacementTargetType.RACK) ObjectGlyph.RACK else ObjectGlyph.of(project, device)
                    val type = if (ref.type == PlacementTargetType.RACK) i18n.text("text.4cd265c2b8c6")
                        else ObjectCatalog.type(project, device?.objectTypeId)?.let { ObjectCatalog.displayName(it, i18n) }
                    val place = hierarchy.parents[ref]?.let { i18n.text("map.inContainer", ObjectHierarchy.name(project, it, i18n)) } ?: i18n.text("map.onFloor")
                    TextButton(onClick = { onPick(ref) }, modifier = Modifier.fillMaxWidth()) {
                        GlyphBadge(glyph, 28.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(type, place).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onClose) { Text(i18n.text("text.18c9d912a210")) } })
}
