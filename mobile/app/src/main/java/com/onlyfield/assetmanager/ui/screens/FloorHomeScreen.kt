package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.ui.LocalMessages

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
    val i18n = LocalMessages.current

    val context = LocalContext.current
    val bu = project.businessUnits.find { it.id == vm.selectedBuId }
    val area = bu?.let { ObjectMap.areas(it).find { it.id == vm.selectedAreaId } }
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
    fun back() { when { area != null -> vm.selectedAreaId = null; bu != null -> vm.selectedBuId = null; else -> vm.back() } }
    BackHandler(enabled = bu != null) { back() }
    LaunchedEffect(attachment?.id, area?.floorplanPageIndex) {
        image = null; imageError = null
        if (attachment != null) {
            try {
                image = withContext(Dispatchers.IO) { PlanMedia.image(vm.attachmentFile(attachment) ?: error(i18n.text("text.b514c5e8cde0")), attachment.fileType == AttachmentType.PDF, area?.floorplanPageIndex ?: 0, i18n = i18n) }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; imageError = i18n.text("text.04e6695ec9e8", e.message) }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && area != null) vm.importFloorplan(context, uri, area.id) { a ->
            if (a.fileType == AttachmentType.IMAGE) {
                vm.edit(i18n.text("text.fcd1cc58f46b")) { ProjectEdits.setAreaFloorplan(it, area.id, a.id) }; selectingPlan = false
            } else newPlanId = a.id
        }
    }
    AppScaffold(project.name, subtitle = listOfNotNull(bu?.name, area?.let { ObjectMap.areaLabel(bu, it) }).joinToString(" / "), onBack = ::back, snackbarHost = snackbar, busy = vm.busy,
        actions = { TextButton(onClick = { vm.navigate(Screen.ProjectTools) }) { Text(i18n.text("text.bb1ca9a0ad66")) } }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (area == null) {
                Text(if (bu == null) i18n.text("text.26aad2e3cb26") else i18n.text("text.363156736748"), style = MaterialTheme.typography.titleLarge)
                Button(onClick = { addingStructure = true }) { Text(if (bu == null) i18n.text("text.4e90901d9fa2") else i18n.text("text.3575ad226840")) }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bu == null) items(project.businessUnits, key = { it.id }) { b ->
                        ItemCard(b.name, listOf(i18n.text("text.e326a5ebe3a5", ObjectMap.areas(b).size)), onClick = { vm.selectedBuId = b.id; vm.selectedAreaId = null })
                    } else items(ObjectMap.areas(bu), key = { it.id }) { a ->
                        ItemCard(ObjectMap.areaLabel(bu, a), listOf(i18n.text("text.76188f884c68", ObjectMap.nodes(project, a.id).size + ObjectMap.routes(project, a.id).size)), onClick = { vm.selectedAreaId = a.id })
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(enabled = vm.busy == null, onClick = { catalog = true }) { Text(i18n.text("text.84cbef7b19b8")) }
                    OutlinedButton(onClick = { scanning = true }) { Text("QR") }
                    OutlinedButton(enabled = vm.busy == null, onClick = { selectingPlan = true }) { Text(i18n.text("text.68f86d09412c")) }
                }
                imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                FloorCanvas(project, area.id, image, vm::editMap, { n ->
                    val ref = ObjectRef(n.type, n.id)
                if (ObjectHierarchy.canContain(project, ref)) container = ref
                else editor = MapObjectDraft.device(project, project.businessUnits.find { b -> b.devices.any { it.id == n.id } }?.id ?: bu.id, area.id, n.id, i18n = i18n)
                }, { editor = MapObjectDraft.cable(project, bu.id, area.id, it, i18n = i18n) }, Modifier.weight(1f))
            }
        }
    }
    if (addingStructure) {
        var name by remember { mutableStateOf("") }
        EditScreen(if (bu == null) i18n.text("text.5beecc355a96") else i18n.text("text.91e5e6cad9c8", bu.name), { addingStructure = false }, {
            vm.edit(i18n.text("text.ad31ce615e92")) { if (bu == null) ProjectEdits.addBusinessUnit(it, name.trim()) else ProjectEdits.addArea(it, bu.id, Area(name = name.trim())) }
            addingStructure = false
        }, confirmEnabled = name.isNotBlank()) { FormField(name, { name = it }, i18n.text("text.2e245546ff59")) }
    }
    if (catalog && area != null) ObjectCatalogDialog(project, { catalog = false }, { type ->
        editor = MapObjectDraft(type = type, buId = bu.id, areaId = area.id); catalog = false
    }, { type ->
        vm.edit(i18n.text("text.a4d3de9d1b61")) { it.copy(objectTypes = it.objectTypes + type) }
        editor = MapObjectDraft(type = type, buId = bu.id, areaId = area.id); catalog = false
    })
    editor?.let { draft -> key(draft.id) { FloorObjectEditor(vm, project, draft) { editor = null } } }
    container?.let { ref -> ContainerBrowser(vm, project, ref, bu!!.id, area!!.id) { container = null } }
    if (selectingPlan && area != null) PlanChooser(project, area, newPlanId, vm::attachmentFile, { picker.launch(arrayOf("image/*", "application/pdf")) }, { id, page, pages ->
        vm.edit(i18n.text("text.fcd1cc58f46b")) { ProjectEdits.setAreaFloorplan(it, area.id, id, page, pages) }; selectingPlan = false; newPlanId = null
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
