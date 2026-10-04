package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.configurator.ConfiguratorPage
import com.onlyfield.assetmanager.configurator.map.*
import com.onlyfield.assetmanager.core.forms.QuickAdd
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.ui.*
import com.onlyfield.assetmanager.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloorHomeScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val context = LocalContext.current
    val bu = project.businessUnits.find { it.id == vm.selectedBuId }
    val area = bu?.let { ObjectMap.areas(it).find { it.id == vm.selectedAreaId } }
    var addingStructure by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<Pair<ObjectRef?, MapPoint?>?>(null) }
    var editor by remember { mutableStateOf<MapObjectDraft?>(null) }
    // Object to select after "Go to"; cleared when the user picks a floor by hand.
    var focus by remember { mutableStateOf<ObjectRef?>(null) }
    var editorPage by remember { mutableStateOf(ConfiguratorPage.ESSENTIALS) }
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
    AppScaffold(project.name, subtitle = listOfNotNull(bu?.name, area?.let { ObjectMap.areaLabel(bu, it) }).joinToString(" / "), onBack = ::back, snackbarHost = snackbar, busy = vm.busy) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (area == null) {
                Text(if (bu == null) i18n.text("text.26aad2e3cb26") else i18n.text("text.363156736748"), style = MaterialTheme.typography.titleLarge)
                Button(onClick = { addingStructure = true }) { Text(if (bu == null) i18n.text("text.4e90901d9fa2") else i18n.text("text.3575ad226840")) }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bu == null) items(project.businessUnits.sortedForDisplay(i18n) { it.name }, key = { it.id }) { b ->
                        ItemCard(b.name, listOf(i18n.text("text.e326a5ebe3a5", ObjectMap.areas(b).size)), onClick = { vm.selectedBuId = b.id; vm.selectedAreaId = null })
                    } else items(ObjectMap.areas(bu).sortedForDisplay(i18n) { it.name }, key = { it.id }) { a ->
                        ItemCard(ObjectMap.areaLabel(bu, a), listOf(i18n.plural("text.76188f884c68", ObjectMap.nodes(project, a.id).size + ObjectMap.routes(project, a.id).size)), onClick = { focus = null; vm.selectedAreaId = a.id })
                    }
                }
            } else {
                imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                MapWorkspace(project, area.id, image, i18n, MapActions(
                    update = vm::editMap,
                    goTo = { target, ref -> vm.selectedBuId = ObjectMap.floorBusinessUnit(project, target); vm.selectedAreaId = target; focus = ref },
                    edit = { draft, page -> editorPage = page; editor = draft },
                    add = { parent, point -> if (vm.busy == null) adding = parent to point },
                ), Modifier.weight(1f), tools = listOf(
                    com.onlyfield.assetmanager.configurator.PaneAction(i18n.text("map.scan")) { scanning = true },
                    com.onlyfield.assetmanager.configurator.PaneAction(i18n.text("text.68f86d09412c")) { if (vm.busy == null) selectingPlan = true },
                ), focus = focus, media = { ref ->
                    val target = if (ref.type == PlacementTargetType.RACK) AttachmentTargetType.RACK else AttachmentTargetType.DEVICE
                    // Thumbnails wrap so the pane never scrolls sideways.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        project.attachments.filter { it.targetId == ref.id && it.targetType == target }.forEach { a ->
                            Column(Modifier.width(120.dp)) {
                                MediaThumbnail(vm.attachmentFile(a), a.fileType == AttachmentType.PDF)
                                Text(a.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }
                })
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
    adding?.let { (parent, point) -> if (area != null) MapObjectPicker(project, i18n, area.id, parent, point, onClose = { adding = null },
        onAdd = { draft -> adding = null; vm.edit(i18n.text("quick.added", QuickAdd.name(draft))) { draft.apply(it, i18n) } },
        onEdit = { draft -> adding = null; editorPage = ConfiguratorPage.ESSENTIALS; editor = draft }) }
    editor?.let { draft -> key(draft.id) { FloorObjectEditor(vm, project, draft, editorPage) { editor = null } } }
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
