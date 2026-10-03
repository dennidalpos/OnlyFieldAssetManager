package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.scan.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.*
import com.onlyfield.assetmanager.pc.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FloorHomeSection(state: DesktopAppState) {
    val project = state.project ?: return
    val bu = project.businessUnits.find { it.id == state.selectedBuId }
    val area = bu?.let { ObjectMap.areas(it).find { it.id == state.selectedAreaId } }
    var addingStructure by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<MapObjectDraft?>(null) }
    var container by remember { mutableStateOf<ObjectRef?>(null) }
    var selectingPlan by remember { mutableStateOf(false) }
    var newPlanId by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val attachment = project.attachments.find { it.id == area?.floorplanAttachmentId }
    LaunchedEffect(attachment?.id, area?.floorplanPageIndex) {
        image = null; imageError = null
        if (attachment != null) {
            try { image = withContext(Dispatchers.IO) { PlanMedia.image(state.attachmentFile(attachment) ?: error("File della planimetria non disponibile"), attachment.fileType == AttachmentType.PDF, area?.floorplanPageIndex ?: 0) } }
            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; imageError = "Planimetria non leggibile: ${e.message}. Puoi usare lo schema." }
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { state.selectedBuId = null; state.selectedAreaId = null }) { Text(project.name) }
            bu?.let { TextButton(onClick = { state.selectedAreaId = null }) { Text("› ${it.name}") } }
            area?.let { Text("› ${ObjectMap.areaLabel(bu, it)}", Modifier.padding(top = 12.dp)) }
        }
        if (area == null) {
            Text(if (bu == null) "Seleziona BU" else "Seleziona piano", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { addingStructure = true }) { Text(if (bu == null) "Aggiungi BU" else "Aggiungi piano") }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bu == null) items(project.businessUnits, key = { it.id }) { b ->
                    Card(Modifier.fillMaxWidth()) { TextButton(onClick = { state.selectedBuId = b.id; state.selectedAreaId = null }, modifier = Modifier.fillMaxWidth()) { Text("${b.name} · ${ObjectMap.areas(b).size} piani / zone") } }
                } else items(ObjectMap.areas(bu), key = { it.id }) { a ->
                    Card(Modifier.fillMaxWidth()) { TextButton(onClick = { state.selectedAreaId = a.id }, modifier = Modifier.fillMaxWidth()) { Text("${ObjectMap.areaLabel(bu, a)} · ${ObjectMap.nodes(project, a.id).size + ObjectMap.routes(project, a.id).size} oggetti") } }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { catalog = true }) { Text("Aggiungi") }
                OutlinedButton(onClick = { scanning = true }) { Text("QR") }
                OutlinedButton(onClick = { selectingPlan = true }) { Text("Planimetria") }
            }
            imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            FloorCanvas(project, area.id, image, state::update, { n ->
                val ref = ObjectRef(n.type, n.id)
                if (ObjectHierarchy.canContain(project, ref)) container = ref
                else editor = MapObjectDraft.device(project, project.businessUnits.find { b -> b.devices.any { it.id == n.id } }?.id ?: bu.id, area.id, n.id)
            }, { editor = MapObjectDraft.cable(project, bu.id, area.id, it) }, Modifier.weight(1f))
        }
    }
    if (addingStructure) {
        var name by remember { mutableStateOf("") }
        FormDialog(if (bu == null) "Nuova BU" else "Nuovo piano in ${bu.name}", { addingStructure = false }, {
            state.update(if (bu == null) ProjectEdits.addBusinessUnit(project, name.trim()) else ProjectEdits.addArea(project, bu.id, Area(name = name.trim())), "Struttura aggiornata.")
            addingStructure = false
        }, confirmEnabled = name.isNotBlank()) { FormField(name, { name = it }, "Nome *") }
    }
    if (catalog && area != null) ObjectCatalogDialog(project, { catalog = false }, { type ->
        editor = MapObjectDraft(type = type, buId = bu.id, areaId = area.id); catalog = false
    }, { type ->
        state.update(project.copy(objectTypes = project.objectTypes + type), "Tipologia aggiunta.")
        editor = MapObjectDraft(type = type, buId = bu.id, areaId = area.id); catalog = false
    })
    editor?.let { draft -> key(draft.id) { FloorObjectEditor(state, project, draft) { editor = null } } }
    container?.let { ref -> ContainerBrowser(state, ref, bu!!.id, area!!.id) { container = null } }
    if (selectingPlan && area != null) PlanChooser(project, area, newPlanId, state::attachmentFile, {
        DesktopStorageHelper.pickOpenFile("Scegli planimetria", "Immagini e PDF", "pdf", "png", "jpg", "jpeg", "webp", "bmp")?.let { file ->
            state.importFloorplan(file, area.id)?.let { a ->
                if (a.fileType == AttachmentType.IMAGE) { state.update(ProjectEdits.setAreaFloorplan(state.project!!, area.id, a.id), "Planimetria impostata."); selectingPlan = false }
                else newPlanId = a.id
            }
        }
    }, { id, page, pages -> state.update(ProjectEdits.setAreaFloorplan(state.project!!, area.id, id, page, pages), "Planimetria impostata."); selectingPlan = false; newPlanId = null }, { selectingPlan = false; newPlanId = null })
    if (scanning && area != null) {
        var code by remember { mutableStateOf("") }
        FormDialog("QR · lettore USB", { scanning = false }, {
            scanning = false
            val index = ProjectIndex(project)
            when (val match = CodeLookup.find(index, code)) {
                is CodeMatch.DeviceMatch -> editor = MapObjectDraft.device(project, index.businessUnitOf(match.device.id)!!.id, match.device.areaId ?: area.id, match.device.id)
                is CodeMatch.PortMatch -> editor = MapObjectDraft.device(project, index.businessUnitOf(match.port.device.id)!!.id, match.port.device.areaId ?: area.id, match.port.device.id)
                is CodeMatch.RackMatch -> editor = MapObjectDraft.rack(project, bu.id, match.rack.areaId ?: area.id, match.rack.id)
                is CodeMatch.CableMatch -> editor = MapObjectDraft.cable(project, bu.id, area.id, match.cable.id)
                is CodeMatch.OtherProject -> state.error = "L'etichetta appartiene a un altro progetto."
                is CodeMatch.NotFound -> { val draft = MapObjectDraft(type = ObjectCatalog.builtins.first(), buId = bu.id, areaId = area.id); editor = draft.copy(device = draft.device.copy(serialNumber = match.code)) }
            }
        }, confirmEnabled = code.isNotBlank(), confirmLabel = "Apri") { FormField(code, { code = it }, "Codice", hint = "Leggi il QR o barcode con il lettore USB") }
    }
}
