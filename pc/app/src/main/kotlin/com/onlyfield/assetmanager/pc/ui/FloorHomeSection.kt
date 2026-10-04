package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.configurator.ConfiguratorPage
import com.onlyfield.assetmanager.configurator.map.*
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
    val bu = project.businessUnits.find { it.id == state.selectedBuId }
    val area = bu?.let { ObjectMap.areas(it).find { it.id == state.selectedAreaId } }
    var addingStructure by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<Pair<ObjectRef?, MapPoint?>?>(null) }
    var editor by remember { mutableStateOf<MapObjectDraft?>(null) }
    var editorPage by remember { mutableStateOf(ConfiguratorPage.ESSENTIALS) }
    var selectingPlan by remember { mutableStateOf(false) }
    var newPlanId by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val attachment = project.attachments.find { it.id == area?.floorplanAttachmentId }
    LaunchedEffect(attachment?.id, area?.floorplanPageIndex) {
        image = null; imageError = null
        if (attachment != null) {
            try { image = withContext(Dispatchers.IO) { PlanMedia.image(state.attachmentFile(attachment) ?: error(i18n.text("text.b514c5e8cde0")), attachment.fileType == AttachmentType.PDF, area?.floorplanPageIndex ?: 0, i18n = i18n) } }
            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; imageError = i18n.text("text.04e6695ec9e8", e.message) }
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { state.selectedBuId = null; state.selectedAreaId = null }) { Text(project.name) }
            bu?.let { TextButton(onClick = { state.selectedAreaId = null }) { Text("› ${it.name}") } }
            area?.let { Text("› ${ObjectMap.areaLabel(bu, it)}", Modifier.padding(top = 12.dp)) }
        }
        if (area == null) {
            Text(if (bu == null) i18n.text("text.26aad2e3cb26") else i18n.text("text.363156736748"), style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { addingStructure = true }) { Text(if (bu == null) i18n.text("text.4e90901d9fa2") else i18n.text("text.3575ad226840")) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bu == null) items(project.businessUnits.sortedForDisplay(i18n) { it.name }, key = { it.id }) { b ->
                    Card(Modifier.fillMaxWidth()) { TextButton(onClick = { state.selectedBuId = b.id; state.selectedAreaId = null }, modifier = Modifier.fillMaxWidth()) { Text(i18n.text("text.edc54eb6f93e", b.name, ObjectMap.areas(b).size)) } }
                } else items(ObjectMap.areas(bu).sortedForDisplay(i18n) { it.name }, key = { it.id }) { a ->
                    Card(Modifier.fillMaxWidth()) { TextButton(onClick = { state.selectedAreaId = a.id }, modifier = Modifier.fillMaxWidth()) { Text(i18n.text("text.0b16578ff793", ObjectMap.areaLabel(bu, a), ObjectMap.nodes(project, a.id).size + ObjectMap.routes(project, a.id).size)) } }
                }
            }
        } else {
            imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            MapWorkspace(project, area.id, image, i18n, MapActions(
                update = state::update,
                edit = { draft, page -> editorPage = page; editor = draft },
                add = { parent, point -> adding = parent to point },
            ), Modifier.weight(1f), toolbar = {
                OutlinedButton(onClick = { scanning = true }) { Text(i18n.text("map.scan")) }
                OutlinedButton(onClick = { selectingPlan = true }) { Text(i18n.text("text.68f86d09412c")) }
            }, media = { ref ->
                val target = if (ref.type == PlacementTargetType.RACK) AttachmentTargetType.RACK else AttachmentTargetType.DEVICE
                // The pane draws the section title; thumbnails scroll sideways to keep it compact.
                Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    project.attachments.filter { it.targetId == ref.id && it.targetType == target }.forEach { a ->
                        Column(Modifier.width(120.dp)) {
                            MediaThumbnail(state.attachmentFile(a), a.fileType == AttachmentType.PDF)
                            Text(a.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            })
        }
    }
    if (addingStructure) {
        var name by remember { mutableStateOf("") }
        FormDialog(if (bu == null) i18n.text("text.5beecc355a96") else i18n.text("text.91e5e6cad9c8", bu.name), { addingStructure = false }, {
            state.update(if (bu == null) ProjectEdits.addBusinessUnit(project, name.trim()) else ProjectEdits.addArea(project, bu.id, Area(name = name.trim())), i18n.text("text.ad31ce615e92"))
            addingStructure = false
        }, confirmEnabled = name.isNotBlank()) { FormField(name, { name = it }, i18n.text("text.2e245546ff59")) }
    }
    adding?.let { (parent, point) -> if (area != null) ObjectPickerDialog(project, i18n, area.id, parent, onClose = { adding = null }, onPick = { type, preset ->
        editorPage = ConfiguratorPage.ESSENTIALS; editor = newObjectDraft(project, type, preset, area.id, parent, point); adding = null
    }, onCustom = { type ->
        val updated = project.copy(objectTypes = project.objectTypes + type)
        state.update(updated, i18n.text("text.a4d3de9d1b61"))
        editorPage = ConfiguratorPage.ESSENTIALS; editor = newObjectDraft(updated, type, null, area.id, parent, point); adding = null
    }) }
    editor?.let { draft -> key(draft.id) { FloorObjectEditor(state, project, draft, editorPage) { editor = null } } }
    if (selectingPlan && area != null) PlanChooser(project, area, newPlanId, state::attachmentFile, {
        DesktopStorageHelper.pickOpenFile(i18n.text("text.04458b820c0e"), i18n.text("text.0c7a70a251fc"), "pdf", "png", "jpg", "jpeg", "webp", "bmp", i18n = i18n)?.let { file ->
            state.importFloorplan(file, area.id)?.let { a ->
                if (a.fileType == AttachmentType.IMAGE) { state.update(ProjectEdits.setAreaFloorplan(state.project!!, area.id, a.id), i18n.text("text.fcd1cc58f46b")); selectingPlan = false }
                else newPlanId = a.id
            }
        }
    }, { id, page, pages -> state.update(ProjectEdits.setAreaFloorplan(state.project!!, area.id, id, page, pages), i18n.text("text.fcd1cc58f46b")); selectingPlan = false; newPlanId = null }, { selectingPlan = false; newPlanId = null })
    if (scanning && area != null) {
        var code by remember { mutableStateOf("") }
        FormDialog(i18n.text("text.bafdb2fe7ef0"), { scanning = false }, {
            scanning = false
            val index = ProjectIndex(project)
            when (val match = CodeLookup.find(index, code, i18n = i18n)) {
                is CodeMatch.DeviceMatch -> editor = MapObjectDraft.device(project, index.businessUnitOf(match.device.id)!!.id, match.device.areaId ?: area.id, match.device.id, i18n = i18n)
                is CodeMatch.PortMatch -> editor = MapObjectDraft.device(project, index.businessUnitOf(match.port.device.id)!!.id, match.port.device.areaId ?: area.id, match.port.device.id, i18n = i18n)
                is CodeMatch.RackMatch -> editor = MapObjectDraft.rack(project, bu.id, match.rack.areaId ?: area.id, match.rack.id)
                is CodeMatch.CableMatch -> editor = MapObjectDraft.cable(project, bu.id, area.id, match.cable.id, i18n = i18n)
                is CodeMatch.OtherProject -> state.error = i18n.text("text.b18a8bb53b19")
                is CodeMatch.NotFound -> { val draft = MapObjectDraft(type = ObjectCatalog.builtins.first(), buId = bu.id, areaId = area.id); editor = draft.copy(device = draft.device.copy(serialNumber = match.code)) }
            }
        }, confirmEnabled = code.isNotBlank(), confirmLabel = i18n.text("text.12abcf9ee7d6")) { FormField(code, { code = it }, i18n.text("text.dabfdc4bb56c"), hint = i18n.text("text.dcf70239e881")) }
    }
}
