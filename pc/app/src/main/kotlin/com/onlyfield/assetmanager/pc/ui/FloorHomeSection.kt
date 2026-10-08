package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.display.sitesForDisplay
import com.onlyfield.assetmanager.core.display.displayName
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.configurator.ConfiguratorPage
import com.onlyfield.assetmanager.configurator.map.*
import com.onlyfield.assetmanager.core.forms.QuickAdd
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.scan.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.*
import com.onlyfield.assetmanager.pc.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FloorHomeSection(state: DesktopAppState) {
    val i18n = LocalMessages.current

    val project = state.project ?: return
    val site = project.sites.find { it.id == state.selectedSiteId }
    val area = site?.let { it.areas.find { it.id == state.selectedAreaId } }
    var addingStructure by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<Pair<ObjectRef?, MapPoint?>?>(null) }
    var editor by remember { mutableStateOf<MapObjectDraft?>(null) }
    // Object to select after "Go to"; cleared when the user picks a floor by hand.
    var focus by remember { mutableStateOf<ObjectRef?>(null) }
    var editorPage by remember { mutableStateOf(ConfiguratorPage.ESSENTIALS) }
    var selectingPlan by remember { mutableStateOf(false) }
    var newPlanId by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var topology by remember { mutableStateOf(false) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val attachment = project.attachments.find { it.id == area?.floorplanAttachmentId }
    LaunchedEffect(attachment?.id, area?.floorplanPageIndex) {
        image = null; imageError = null
        if (attachment != null) {
            try { image = withContext(Dispatchers.IO) { PlanMedia.image(state.attachmentBytes(attachment) ?: error(i18n.text("text.b514c5e8cde0")), attachment.fileType == AttachmentType.PDF, area?.floorplanPageIndex ?: 0, i18n = i18n) } }
            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; imageError = i18n.text("text.04e6695ec9e8", e.message) }
        }
    }
    fun openHit(hit: com.onlyfield.assetmanager.core.display.SearchHit) {
        searching = false
        state.recentSearch = withRecent(state.recentSearch, hit.id)
        val target = hit.areaId
        if (target == null) { editorPage = ConfiguratorPage.ESSENTIALS; editor = searchEditDraft(project, hit) }
        else { state.selectedSiteId = ObjectMap.floorSite(project, target); state.selectedAreaId = target; focus = hit.focus }
    }
    fun openDevice(d: Device) {
        topology = false
        val target = ObjectMap.areaId(project, d)
        if (target == null) { editorPage = ConfiguratorPage.ESSENTIALS; editor = MapObjectDraft.forDevice(project, d) }
        else { state.selectedSiteId = ObjectMap.floorSite(project, target); state.selectedAreaId = target; focus = ObjectRef(PlacementTargetType.DEVICE, d.id) }
    }
    Column(Modifier.fillMaxSize().onPreviewKeyEvent { e ->
        // Ctrl+F opens the project search from the map.
        if (e.type == KeyEventType.KeyDown && e.isCtrlPressed && e.key == Key.F) { searching = true; true } else false
    }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (area == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { state.selectedSiteId = null; state.selectedAreaId = null }) { Text(project.name) }
            site?.let { TextButton(onClick = { state.selectedAreaId = null }) { Text("› ${it.name}") } }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { topology = true }) { Text(i18n.text("topology.title")) }
            OutlinedButton(onClick = { searching = true }) { Text(i18n.text("search.action")) }
        }
        if (area == null) {
            Text(if (site == null) i18n.text("text.26aad2e3cb26") else i18n.text("text.363156736748"), style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { addingStructure = true }) { Text(if (site == null) i18n.text("text.4e90901d9fa2") else i18n.text("text.3575ad226840")) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (site == null) items(project.sites.sitesForDisplay(i18n), key = { it.id }) { b ->
                    Card(Modifier.fillMaxWidth()) { TextButton(onClick = { state.selectedSiteId = b.id; state.selectedAreaId = null }, modifier = Modifier.fillMaxWidth()) { Text(i18n.text("text.edc54eb6f93e", b.displayName(), b.areas.size)) } }
                } else items(site.areas.sortedForDisplay(i18n) { it.name }, key = { it.id }) { a ->
                    Card(Modifier.fillMaxWidth()) { TextButton(onClick = { focus = null; state.selectedAreaId = a.id }, modifier = Modifier.fillMaxWidth()) { Text(i18n.text("text.0b16578ff793", a.name, ObjectMap.nodes(project, a.id).size + ObjectMap.routes(project, a.id).size)) } }
                }
            }
        } else {
            imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            MapWorkspace(project, area.id, image, i18n, MapActions(
                update = state::update,
                goTo = { target, ref -> state.selectedSiteId = ObjectMap.floorSite(project, target); state.selectedAreaId = target; focus = ref },
                edit = { draft, page -> editorPage = page; editor = draft },
                add = { parent, point -> adding = parent to point },
                trash = { ref ->
                    val name = ObjectHierarchy.name(project, ref, i18n)
                    val (updated, item) = if (ref.type == PlacementTargetType.RACK) ProjectEdits.deleteRackToTrash(project, ref.id, i18n)
                        else ProjectEdits.deleteDeviceToTrash(project, ref.id, i18n)
                    item?.let(state::addToTrash)
                    state.update(updated, i18n.text("text.4e2629d50c9b", name))
                },
                photo = com.onlyfield.assetmanager.configurator.LocalPhotoAction.current,
            ), Modifier.weight(1f), tools = listOf(
                    com.onlyfield.assetmanager.configurator.PaneAction(i18n.text("topology.title")) { topology = true },
                    com.onlyfield.assetmanager.configurator.PaneAction(i18n.text("map.scan")) { scanning = true },
                    com.onlyfield.assetmanager.configurator.PaneAction(i18n.text("text.68f86d09412c")) { selectingPlan = true },
                ), state = state.mapUiState.floor(project, area.id), focus = focus, onFocusHandled = { focus = null },
                onSearch = { searching = true }, onLeaveFloor = { state.selectedAreaId = null }, media = { ref ->
                val target = if (ref.type == PlacementTargetType.RACK) AttachmentTargetType.RACK else AttachmentTargetType.DEVICE
                // Thumbnails wrap so the pane never scrolls sideways.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    project.attachments.filter { it.targetId == ref.id && it.targetType == target }.forEach { a ->
                        Column(Modifier.width(120.dp)) {
                            MediaThumbnail(a.id, { state.attachmentBytes(a) }, a.fileType == AttachmentType.PDF)
                            Text(a.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            })
        }
    }
    if (addingStructure) {
        var name by remember { mutableStateOf("") }
        FormDialog(if (site == null) i18n.text("text.5beecc355a96") else i18n.text("text.91e5e6cad9c8", site.name), { addingStructure = false }, {
            state.update(if (site == null) ProjectEdits.addSite(project, name.trim()) else ProjectEdits.addArea(project, site.id, Area(name = name.trim())), i18n.text("text.ad31ce615e92"))
            if (state.error == null) addingStructure = false
        }, confirmEnabled = name.isNotBlank()) {
            FormField(name, { name = it }, i18n.text("text.2e245546ff59"))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    adding?.let { (parent, point) -> if (area != null) MapObjectPicker(project, i18n, area.id, parent, point, onClose = { adding = null },
        onAdd = { draft ->
            state.update(draft.apply(project, i18n), i18n.text("quick.added", QuickAdd.name(draft)))
            if (state.error == null) adding = null
        },
        onEdit = { draft -> adding = null; editorPage = ConfiguratorPage.ESSENTIALS; editor = draft }, error = state.error) }
    editor?.let { draft -> key(draft.id) { FloorObjectEditor(state, project, draft, editorPage) { editor = null } } }
    if (searching) GlobalSearchDialog(project, i18n, state.recentSearch, ::openHit) { searching = false }
    if (topology) TopologyDialog(project, i18n, site?.id, area?.id, ::openDevice) { topology = false }
    if (selectingPlan && area != null) PlanChooser(project, area, newPlanId, state::attachmentBytes, {
        DesktopStorageHelper.pickOpenFile(i18n.text("text.04458b820c0e"), i18n.text("text.0c7a70a251fc"), "pdf", "png", "jpg", "jpeg", "webp", "bmp", i18n = i18n)?.let { file ->
            state.importFloorplan(file, area.id)?.let { a ->
                newPlanId = a.id
                if (a.fileType == AttachmentType.IMAGE) {
                    state.update(ProjectEdits.setAreaFloorplan(state.project!!, area.id, a.id), i18n.text("text.fcd1cc58f46b"))
                    if (state.error == null) { selectingPlan = false; newPlanId = null }
                }
            }
        }
    }, { id, page, pages ->
        state.update(ProjectEdits.setAreaFloorplan(state.project!!, area.id, id, page, pages), i18n.text("text.fcd1cc58f46b"))
        if (state.error == null) { selectingPlan = false; newPlanId = null }
    }, { selectingPlan = false; newPlanId = null }, error = state.error)
    if (scanning && area != null) {
        var code by remember { mutableStateOf("") }
        FormDialog(i18n.text("text.bafdb2fe7ef0"), { scanning = false }, {
            scanning = false
            val index = ProjectIndex(project)
            when (val match = CodeLookup.find(index, code, i18n = i18n)) {
                is CodeMatch.DeviceMatch -> editor = MapObjectDraft.device(project, index.siteOf(match.device.id)!!.id, match.device.areaId ?: area.id, match.device.id, i18n = i18n)
                is CodeMatch.PortMatch -> editor = MapObjectDraft.device(project, index.siteOf(match.port.device.id)!!.id, match.port.device.areaId ?: area.id, match.port.device.id, i18n = i18n)
                is CodeMatch.RackMatch -> editor = MapObjectDraft.rack(project, site.id, match.rack.areaId ?: area.id, match.rack.id)
                is CodeMatch.CableMatch -> editor = MapObjectDraft.cable(project, site.id, area.id, match.cable.id, i18n = i18n)
                is CodeMatch.OtherProject -> state.error = i18n.text("text.b18a8bb53b19")
                is CodeMatch.NotFound -> { val draft = MapObjectDraft(type = ObjectCatalog.builtins.first(), siteId = site.id, areaId = area.id); editor = draft.copy(device = draft.device.copy(serialNumber = match.code)) }
            }
        }, confirmEnabled = code.isNotBlank(), confirmLabel = i18n.text("text.12abcf9ee7d6")) { FormField(code, { code = it }, i18n.text("text.dabfdc4bb56c"), hint = i18n.text("text.dcf70239e881")) }
    }
}
