package com.onlyfield.assetmanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.ui.*
import com.onlyfield.assetmanager.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FloorHomeScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val bu = project.businessUnits.find { it.id == vm.selectedBuId }
    val area = bu?.let { ObjectMap.areas(it).find { it.id == vm.selectedAreaId } }
    var addingStructure by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<MapObjectDraft?>(null) }
    var selectingPlan by remember { mutableStateOf(false) }
    var newPlanId by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val attachment = project.attachments.find { it.id == area?.floorplanAttachmentId }
    fun back() { when { area != null -> vm.selectedAreaId = null; bu != null -> vm.selectedBuId = null; else -> vm.back() } }
    BackHandler(enabled = bu != null) { back() }
    LaunchedEffect(attachment?.id, area?.floorplanPageIndex) {
        image = null; imageError = null
        if (attachment != null) {
            try {
                image = withContext(Dispatchers.IO) { PlanMedia.image(vm.attachmentFile(attachment) ?: error("File della planimetria non disponibile"), attachment.fileType == AttachmentType.PDF, area?.floorplanPageIndex ?: 0) }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; imageError = "Planimetria non leggibile: ${e.message}. Puoi usare lo schema." }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && area != null) vm.importFloorplan(context, uri, area.id) { a ->
            if (a.fileType == AttachmentType.IMAGE) {
                vm.edit("Planimetria impostata.") { ProjectEdits.setAreaFloorplan(it, area.id, a.id) }; selectingPlan = false
            } else newPlanId = a.id
        }
    }
    AppScaffold(project.name, subtitle = listOfNotNull(bu?.name, area?.let { ObjectMap.areaLabel(bu, it) }).joinToString(" / "), onBack = ::back, snackbarHost = snackbar, busy = vm.busy,
        actions = { TextButton(onClick = { vm.navigate(Screen.ProjectTools) }) { Text("Strumenti") } }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (area == null) {
                Text(if (bu == null) "Seleziona BU" else "Seleziona piano", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { addingStructure = true }) { Text(if (bu == null) "Aggiungi BU" else "Aggiungi piano") }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bu == null) items(project.businessUnits, key = { it.id }) { b ->
                        ItemCard(b.name, listOf("${ObjectMap.areas(b).size} piani / zone"), onClick = { vm.selectedBuId = b.id; vm.selectedAreaId = null })
                    } else items(ObjectMap.areas(bu), key = { it.id }) { a ->
                        ItemCard(ObjectMap.areaLabel(bu, a), listOf("${ObjectMap.nodes(project, a.id).size + ObjectMap.routes(project, a.id).size} oggetti"), onClick = { vm.selectedAreaId = a.id })
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(enabled = vm.busy == null, onClick = { catalog = true }) { Text("Aggiungi") }
                    OutlinedButton(onClick = { scanning = true }) { Text("QR") }
                    OutlinedButton(enabled = vm.busy == null, onClick = { selectingPlan = true }) { Text("Planimetria") }
                }
                imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                FloorCanvas(project, area.id, image, vm::editMap, { n ->
                    editor = if (n.type == PlacementTargetType.RACK) MapObjectDraft.rack(project, bu.id, area.id, n.id) else MapObjectDraft.device(project, bu.id, area.id, n.id)
                }, { editor = MapObjectDraft.cable(project, bu.id, area.id, it) }, Modifier.weight(1f))
            }
        }
    }
    if (addingStructure) {
        var name by remember { mutableStateOf("") }
        EditScreen(if (bu == null) "Nuova BU" else "Nuovo piano in ${bu.name}", { addingStructure = false }, {
            vm.edit("Struttura aggiornata.") { if (bu == null) ProjectEdits.addBusinessUnit(it, name.trim()) else ProjectEdits.addArea(it, bu.id, Area(name = name.trim())) }
            addingStructure = false
        }, confirmEnabled = name.isNotBlank()) { FormField(name, { name = it }, "Nome *") }
    }
    if (catalog && area != null) ObjectCatalogDialog(project, { catalog = false }, { type ->
        editor = MapObjectDraft(type = type, buId = bu.id, areaId = area.id); catalog = false
    }, { type ->
        vm.edit("Tipologia aggiunta.") { it.copy(objectTypes = it.objectTypes + type) }
        editor = MapObjectDraft(type = type, buId = bu.id, areaId = area.id); catalog = false
    })
    editor?.let { draft -> key(draft.id) { FloorObjectEditor(vm, project, draft) { editor = null } } }
    if (selectingPlan && area != null) PlanChooser(project, area, newPlanId, vm::attachmentFile, { picker.launch(arrayOf("image/*", "application/pdf")) }, { id, page, pages ->
        vm.edit("Planimetria impostata.") { ProjectEdits.setAreaFloorplan(it, area.id, id, page, pages) }; selectingPlan = false; newPlanId = null
    }, { selectingPlan = false; newPlanId = null })
    if (scanning) BarcodeScanner(onCode = { code ->
        scanning = false
        val unknown = vm.openScannedCode(code)
        if (unknown != null && area != null) {
            val draft = MapObjectDraft(type = ObjectCatalog.builtins.first(), buId = bu.id, areaId = area.id)
            editor = draft.copy(device = draft.device.copy(serialNumber = unknown))
        }
    }, onClose = { scanning = false })
}
