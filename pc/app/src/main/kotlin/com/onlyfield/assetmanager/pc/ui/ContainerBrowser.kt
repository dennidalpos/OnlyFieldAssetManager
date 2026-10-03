package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.pc.DesktopAppState
import com.onlyfield.assetmanager.pc.ui.components.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft

@Composable
internal fun ContainerBrowser(state: DesktopAppState, root: ObjectRef, buId: String, areaId: String, close: () -> Unit) {
    val project = state.project ?: return
    var path by remember(root) { mutableStateOf(listOf(root)) }
    var editing by remember { mutableStateOf<MapObjectDraft?>(null) }
    var adding by remember { mutableStateOf(false) }
    var assigning by remember { mutableStateOf(false) }
    var side by remember { mutableStateOf(RackSide.FRONT) }
    val current = path.last()
    val exists = current in ObjectHierarchy.refs(project)
    if (!exists) { LaunchedEffect(current) { close() }; return }
    fun back() { if (assigning) assigning = false else if (path.size > 1) path = path.dropLast(1) else close() }
    fun mutate(action: (Project) -> Project) { state.project?.let { state.update(action(it), "Contenitore aggiornato.") } }
    val slot = LocalDetailSlot.current
    val latestClose by rememberUpdatedState(close)
    val dismiss = remember { { latestClose() } }
    DisposableEffect(slot, editing) {
        if (editing == null) slot?.let { it.dismiss = dismiss; it.dirty = false }
        onDispose { if (slot?.dismiss === dismiss) { slot.dismiss = null; slot.dirty = false } }
    }
    editing?.let { draft -> key(draft.id) { FloorObjectEditor(state, project, draft) { editing = null } }; return }
    if (adding) {
        ObjectCatalogDialog(project, { adding = false }, { type ->
            if (type.kind != ObjectKind.CABLE) {
                editing = MapObjectDraft.newObject(project, type, buId, areaId, current)
                adding = false
            }
        }, { type ->
            mutate { it.copy(objectTypes = it.objectTypes + type) }
            editing = MapObjectDraft.newObject(project, type, buId, areaId, current)
            adding = false
        }, allowCables = false)
        return
    }
    val rack = project.racks.find { current.type == PlacementTargetType.RACK && it.id == current.id }
    val device = project.businessUnits.flatMap { it.devices }.find { current.type == PlacementTargetType.DEVICE && it.id == current.id }
    val children = ObjectHierarchy.children(project, current)
    AlertDialog(onDismissRequest = ::back, title = { Text(path.joinToString(" › ") { ObjectHierarchy.name(project, it) }) }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (assigning) {
                Text("Scegli un oggetto esistente")
                val candidates = ObjectHierarchy.refs(project).filter { it != current && it !in children && it !in ObjectHierarchy.ancestors(project, current) && ObjectHierarchy.areaId(project, it) == areaId }
                if (candidates.isEmpty()) Text("Nessun oggetto disponibile sul piano")
                candidates.forEach { child -> TextButton(onClick = { mutate { ObjectHierarchy.assign(it, child, current) }; assigning = false }) { Text(ObjectHierarchy.name(project, child)) } }
            } else {
                device?.let { d ->
                    Text(listOfNotNull(d.physicalLabel, d.alias, d.ipAddress, d.serialNumber).joinToString(" · "))
                    d.ports.forEach { Text("Porta: ${it.name}") }
                }
                rack?.let { r ->
                    Row { TextButton(onClick = { side = RackSide.FRONT }) { Text("Fronte") }; TextButton(onClick = { side = RackSide.REAR }) { Text("Retro") } }
                    RackElevation(r, project.businessUnits.flatMap { it.devices }.filter { it.rackId == r.id }, side, Modifier.fillMaxWidth().height(220.dp))
                }
                project.attachments.filter { it.targetId == current.id && it.targetType == if (current.type == PlacementTargetType.RACK) AttachmentTargetType.RACK else AttachmentTargetType.DEVICE }.forEach { a ->
                    Text(a.name); MediaThumbnail(state.attachmentFile(a), a.fileType == AttachmentType.PDF)
                }
                TextButton(onClick = {
                    val owner = project.businessUnits.find { b -> b.devices.any { it.id == current.id } }?.id ?: buId
                    editing = if (current.type == PlacementTargetType.RACK) MapObjectDraft.rack(project, owner, areaId, current.id) else MapObjectDraft.device(project, owner, areaId, current.id)
                }) { Text("Modifica oggetto") }
                if (ObjectHierarchy.canContain(project, current)) {
                    Text("Contenuto (${children.size})", style = MaterialTheme.typography.titleMedium)
                    if (children.isEmpty()) Text("Contenitore vuoto")
                    children.forEach { child ->
                        Row {
                            TextButton(onClick = { path = path + child }, modifier = Modifier.weight(1f)) { Text(ObjectHierarchy.name(project, child)) }
                            TextButton(onClick = { mutate { ObjectHierarchy.assign(it, child, null) } }) { Text("Rimuovi dal contenitore") }
                        }
                    }
                    Row { OutlinedButton(onClick = { adding = true }) { Text("Aggiungi oggetto") }; TextButton(onClick = { assigning = true }) { Text("Assegna esistente") } }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = ::back) { Text(if (path.size > 1 || assigning) "Indietro" else "Chiudi") } })
}
