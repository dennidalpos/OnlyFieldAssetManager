package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.onlyfield.assetmanager.configurator.RackElevation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.configurator.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.RackLayout
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.QuickAdd
import com.onlyfield.assetmanager.configurator.map.ObjectPickerDialog
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.Screen
import com.onlyfield.assetmanager.ui.components.*

// --- Sites and areas ----------------------------------------------------------------------------

@Composable
fun StructureScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val confirm = LocalConfirm.current
    var editBu by remember { mutableStateOf<BusinessUnit?>(null) }
    var newBu by remember { mutableStateOf(false) }
    var areaTarget by remember { mutableStateOf<Pair<BusinessUnit, Area?>?>(null) }
    val takePhoto = rememberPhotoCapture(vm)
    val index = remember(project) { ProjectIndex(project) }

    AppScaffold(
        i18n.text("text.0ce8316b807d"), onBack = { vm.back() }, snackbarHost = snackbar,
        subtitle = i18n.text("text.3fd68ef551e3"),
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { newBu = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.e4de7d26b141")) }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            project.businessUnits.forEach { bu ->
                val areas = bu.areas + bu.sites.flatMap { it.areas }
                item(key = bu.id) {
                    ItemCard(
                        title = bu.name,
                        details = listOf(i18n.text("text.6c039d300c29", bu.devices.size, areas.size)),
                        menu = listOf(
                            MenuAction(i18n.text("text.aeeb0ed4eaa8")) { areaTarget = bu to null },
                            MenuAction(i18n.text("text.98b79b084f23")) { editBu = bu },
                            MenuAction(i18n.text("text.7efe336bd548"), destructive = true) {
                                confirm(ConfirmRequest(i18n.text("text.e36c23dfb086", bu.name), i18n.text("text.256ea58c5c85")) {
                                    vm.edit(i18n.text("text.d4cbe1b8af3d")) { ProjectEdits.deleteBusinessUnit(it, bu.id) ?: error(i18n.text("text.1fcaf8b3ea81")) }
                                })
                            }
                        ),
                        onClick = { areaTarget = bu to null }
                    )
                }
                items(areas.sortedForDisplay(i18n) { it.name }, key = { it.id }) { area ->
                    ItemCard(
                        title = area.name,
                        details = listOfNotNull(area.floor?.let { i18n.text("text.6e61502a3560", it) }, area.description, index.attachmentsOf(area.id).size.takeIf { it > 0 }?.let { i18n.text("text.5b587bc5bd9e", it) }),
                        modifier = Modifier.padding(start = 24.dp),
                        onClick = { areaTarget = bu to area },
                        menu = listOf(MenuAction(i18n.text("text.d88211a9e4b9")) { takePhoto(AttachmentTargetType.AREA, area.id) }, MenuAction(i18n.text("text.7efe336bd548"), destructive = true) {
                            confirm(ConfirmRequest(i18n.text("text.653fc942318e", area.name), i18n.text("text.67328da70967")) {
                                vm.edit(i18n.text("text.1d50eb1afff7")) { ProjectEdits.deleteArea(it, area.id) ?: error(i18n.text("text.4969d887c7f4")) }
                            })
                        })
                    )
                }
            }
        }
    }

    if (newBu || editBu != null) {
        val bu = editBu
        var name by remember(bu) { mutableStateOf(bu?.name.orEmpty()) }
        EditScreen(if (bu == null) i18n.text("text.9058af538683") else i18n.text("text.98b79b084f23"), { newBu = false; editBu = null }, {
            newBu = false; editBu = null
            vm.edit(i18n.text("text.2f92bdd5ba64")) { if (bu == null) ProjectEdits.addBusinessUnit(it, name.trim()) else ProjectEdits.renameBusinessUnit(it, bu.id, name.trim()) }
        }, confirmEnabled = name.isNotBlank()) { FormField(name, { name = it }, i18n.text("text.2e245546ff59")) }
    }

    areaTarget?.let { (bu, area) ->
        var name by remember(area, bu) { mutableStateOf(area?.name.orEmpty()) }
        var floor by remember(area, bu) { mutableStateOf(area?.floor.orEmpty()) }
        var description by remember(area, bu) { mutableStateOf(area?.description.orEmpty()) }
        EditScreen(if (area == null) i18n.text("text.884e872b782f", bu.name) else i18n.text("text.e325f13a6dee"), { areaTarget = null }, {
            areaTarget = null
            val edited = (area ?: Area(name = name.trim())).copy(name = name.trim(), floor = floor.trim().ifBlank { null }, description = description.trim().ifBlank { null })
            vm.edit(i18n.text("text.b0e1d3b2c943", edited.name)) { if (area == null) ProjectEdits.addArea(it, bu.id, edited) else ProjectEdits.updateArea(it, edited) }
        }, confirmEnabled = name.isNotBlank()) {
            FormField(name, { name = it }, i18n.text("text.2e245546ff59"), hint = i18n.text("text.306610be3be7"))
            FormField(floor, { floor = it }, i18n.text("text.fa2bd181d8ba"))
            FormField(description, { description = it }, i18n.text("text.6fb818621896"))
        }
    }
}

// --- Racks --------------------------------------------------------------------------------------

@Composable
fun RacksScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    var creating by remember { mutableStateOf(false) }
    var editingNew by remember { mutableStateOf<com.onlyfield.assetmanager.core.forms.MapObjectDraft?>(null) }
    AppScaffold(
        i18n.text("text.4cd265c2b8c6"), onBack = { vm.back() }, snackbarHost = snackbar, subtitle = i18n.plural("text.2c2d174aee1f", project.racks.size),
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.4cd265c2b8c6")) }) }
    ) { padding ->
        if (project.racks.isEmpty()) EmptyState(i18n.text("text.cc75d410beeb"), Modifier.padding(padding), i18n.text("text.3f21051ccc3f")) { creating = true }
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.racks.sortedForDisplay(i18n) { it.name }, key = { it.id }) { r ->
                ItemCard(
                    title = r.name,
                    details = listOf(i18n.text("text.4abc2837dcad", index.areaName(r.areaId, i18n.text("text.0eb949b8ab9b")), RackLayout.usedUnits(r, index.devices), r.heightU)),
                    onClick = { vm.navigate(Screen.RackDetail(r.id)) }
                )
            }
        }
    }
    if (creating) ObjectPickerDialog(project, i18n, null, { creating = false }, base = { com.onlyfield.assetmanager.core.forms.MapObjectDraft.forRack(project, null) },
        onAdd = { draft -> creating = false; vm.edit(i18n.text("quick.added", QuickAdd.name(draft))) { draft.apply(it, i18n) } },
        onEdit = { draft -> creating = false; editingNew = draft }, filter = { it.kind == ObjectKind.RACK })
    editingNew?.let { draft -> RackDialog(vm, index, null, initial = draft) { editingNew = null } }
}

@Composable
fun RackDetailScreen(vm: ProjectViewModel, project: Project, rackId: String, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val context = LocalContext.current
    val confirm = LocalConfirm.current
    val index = remember(project) { ProjectIndex(project) }
    val rack = index.rack(rackId)
    if (rack == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    var editing by remember { mutableStateOf(false) }
    var placing by remember { mutableStateOf(false) }
    var side by remember { mutableStateOf(RackSide.FRONT) }
    var addingAt by remember { mutableStateOf<Int?>(null) }
    var editingNew by remember { mutableStateOf<com.onlyfield.assetmanager.core.forms.MapObjectDraft?>(null) }
    val takePhoto = rememberPhotoCapture(vm)
    val inRack = index.devices.filter { it.rackId == rack.id }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { vm.exportRackPdf(context.contentResolver, it, rack.id) }
    }

    AppScaffold(
        rack.name, onBack = { vm.back() }, snackbarHost = snackbar, busy = vm.busy,
        subtitle = i18n.text("text.494764b80774", rack.heightU, index.areaName(rack.areaId, i18n.text("text.1abc7243c3dd")), index.attachmentsOf(rack.id).size),
        actions = {
            TextButton(onClick = { takePhoto(AttachmentTargetType.RACK, rack.id) }) { Text(i18n.text("text.494e0843d958")) }
            TextButton(onClick = { editing = true }) { Text(i18n.text("text.49e493ba9d9c")) }
            OverflowMenu(listOf(
                MenuAction(i18n.text("text.550dee4c04e7")) { pdfLauncher.launch("${safeFileName(rack.name)}.pdf") },
                MenuAction(i18n.text("text.dd41b3275173"), destructive = true) {
                    confirm(ConfirmRequest(i18n.text("text.87fc0efddabf", rack.name), i18n.text("text.309921cb8d51"), i18n.text("text.dd41b3275173")) {
                        vm.back()
                        vm.moveToTrash("RACK", rack.id, rack.name)
                    })
                }
            ))
        },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { placing = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.d6616679ad89")) }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(RackSide.FRONT, RackSide.REAR).forEachIndexed { i, s ->
                    SegmentedButton(selected = side == s, onClick = { side = s }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(s.toDisplayString(i18n = i18n)) }
                }
            }
            // Full-width elevation; tapping a free unit places a device there.
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RackElevation(index.project, rack, side, i18n, onDevice = { vm.navigate(Screen.DeviceDetail(it.id)) }, onAddAt = { addingAt = it })
                val unplaced = inRack.filter { it.positionU == null }
                if (unplaced.isNotEmpty()) {
                    SectionTitle(i18n.text("text.d75d6ae3881b"))
                    unplaced.sortedForDisplay(i18n) { it.technicalName }.forEach { d -> ItemCard(d.technicalName, listOf(i18n.text("text.a03528f849dc")), onClick = { vm.navigate(Screen.DeviceDetail(d.id)) }) }
                }
            }
        }
    }

    if (editing) RackDialog(vm, index, rack) { editing = false }
    addingAt?.let { u -> RackUnitPicker(index.project, rack, u, side, i18n, onClose = { addingAt = null },
        onAdd = { draft -> addingAt = null; vm.edit(i18n.text("quick.added", QuickAdd.name(draft))) { draft.apply(it, i18n) } },
        onEdit = { draft -> addingAt = null; editingNew = draft }) }
    editingNew?.let { draft -> DeviceDialog(vm, index.project, null, initial = draft) { editingNew = null } }
    if (placing) {
        val candidates = index.devices.filter { it.rackId != rack.id || it.positionU == null }
        var device by remember { mutableStateOf<Device?>(null) }
        var placeSide by remember { mutableStateOf(side) }
        val free = device?.let { RackLayout.freeStartPositions(rack, index.devices, it.heightU, placeSide, it.id) } ?: emptyList()
        var start by remember(device, placeSide) { mutableStateOf(free.firstOrNull()) }
        EditScreen(i18n.text("text.a96a0f4b4c96", rack.name), { placing = false }, {
            placing = false
            val d = device!!
            val placed = d.copy(rackId = rack.id, positionU = start, rackSide = placeSide,
                mountingType = if (d.mountingType == MountingType.OUT_OF_RACK) MountingType.RACK_MOUNT else d.mountingType)
            vm.edit(i18n.text("text.19beb0981143", d.technicalName, start)) { ProjectEdits.updateDevice(it, placed, i18n = i18n) }
        }, confirmEnabled = device != null && start != null, confirmLabel = i18n.text("text.ee84d8c1e721")) {
            OptionPicker(i18n.text("text.e7f2c0e68768"), candidates, device, { it.technicalName }, { device = it }, optionDetail = { "${it.heightU}U" })
            EnumPicker(i18n.text("text.22ab4cafea0c"), RackSide.entries, placeSide, { it.toDisplayString(i18n = i18n) }, { placeSide = it })
            if (device != null) {
                if (free.isEmpty()) Text(i18n.text("text.6bf6a823593b", device!!.heightU), color = MaterialTheme.colorScheme.error)
                else OptionPicker(i18n.text("text.f45aad4dc280"), free, start, { "U$it" }, { start = it }, supportingText = i18n.text("text.9067efd57c75"))
            }
        }
    }
}

@Composable
private fun RackDialog(vm: ProjectViewModel, index: ProjectIndex, rack: Rack?, initial: com.onlyfield.assetmanager.core.forms.MapObjectDraft? = null, onClose: () -> Unit) {
    val i18n = LocalMessages.current
    var draft by remember(rack) { mutableStateOf(initial ?: com.onlyfield.assetmanager.core.forms.MapObjectDraft.forRack(index.project, rack)) }
    EditScreen(configuratorTitle(index.project, draft, i18n), onClose, {
        onClose(); vm.edit(configuratorTitle(index.project, draft, i18n)) { draft.apply(it, i18n) }
    }, validationMessage = configuratorValidation(index.project, draft, i18n), confirmEnabled = draft.errors(index.project, i18n).isEmpty(), confirmLabel = configuratorAction(index.project, draft, i18n)) {
        ObjectFields(index.project, draft) { draft = it }
    }
}


// --- Models -------------------------------------------------------------------------------------

@Composable
fun ModelsScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val confirm = LocalConfirm.current
    val index = remember(project) { ProjectIndex(project) }
    var editing by remember { mutableStateOf<DeviceModel?>(null) }
    var creating by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf<DeviceModel?>(null) }

    AppScaffold(
        i18n.text("text.19b5ea1a9124"), onBack = { vm.back() }, snackbarHost = snackbar,
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.90c2d339a9d5")) }) }
    ) { padding ->
        if (project.deviceModels.isEmpty()) EmptyState(i18n.text("text.ee7a9420f94d"), Modifier.padding(padding))
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.deviceModels.sortedForDisplay(i18n) { it.name }, key = { it.id }) { m ->
                ItemCard(
                    title = m.name, badge = m.category.toDisplayString(i18n = i18n),
                    details = listOf(listOfNotNull(m.brand, m.modelNumber, "${m.defaultHeightU}U").joinToString(" · "),
                        m.portTemplates.joinToString { "${it.portCount}× ${it.namePrefix}" }),
                    onClick = { editing = m },
                    menu = listOf(
                        MenuAction(i18n.text("text.b3313b6d6747")) { if (m.kind == ObjectKind.DEVICE) applying = m else editing = m },
                        MenuAction(i18n.text("text.7efe336bd548"), destructive = true) {
                            confirm(ConfirmRequest(i18n.text("text.e36c23dfb086", m.name), i18n.text("text.748a2419cc1a")) {
                                vm.edit(i18n.text("text.a6dd92b641a8")) { ProjectEdits.deleteDeviceModel(it, m.id) }
                            })
                        }
                    )
                )
            }
        }
    }

    if (creating || editing != null) {
        val original = editing
        var edited by remember(original) { mutableStateOf(original ?: DeviceModel(name = "", category = DeviceCategory.NETWORK_SWITCH)) }
        EditScreen(if (original == null) i18n.text("ux.add.model") else i18n.text("ux.edit.model", original.name), { creating = false; editing = null }, {
            vm.edit(i18n.text("config.model")) { if (original == null) ProjectEdits.addDeviceModel(it, edited) else ProjectEdits.updateDeviceModel(it, edited) }
            creating = false; editing = null
        }, confirmLabel = i18n.text(if (original == null) "ux.add" else "ux.saveChanges"), confirmEnabled = edited.name.isNotBlank() && com.onlyfield.assetmanager.core.forms.HardwareConfigurator.validGroups(edited.portTemplates) && edited.defaultHeightU in 1..60) {
            val markDirty = LocalMarkDirty.current
            com.onlyfield.assetmanager.configurator.ModelConfigurator(project, edited, i18n) { markDirty(); edited = it }
        }
    }

    applying?.let { model ->
        var deviceId by remember(model) { mutableStateOf<String?>(null) }
        var draft by remember(model) { mutableStateOf<com.onlyfield.assetmanager.core.forms.MapObjectDraft?>(null) }
        EditScreen(i18n.text("ux.applyModel", model.name), { applying = null }, {
            vm.edit(i18n.text("config.model")) { requireNotNull(draft).apply(it, i18n) }; applying = null
        }, confirmLabel = i18n.text("ux.apply"), validationMessage = draft?.let { configuratorValidation(project, it, i18n) }, confirmEnabled = draft?.errors(project, i18n)?.isEmpty() == true) {
            DevicePicker(i18n.text("config.device"), index, deviceId, { id ->
                deviceId = id
                draft = index.device(id)?.let { com.onlyfield.assetmanager.core.forms.HardwareConfigurator.applyModel(com.onlyfield.assetmanager.core.forms.MapObjectDraft.forDevice(project, it), model) }
            })
            draft?.let { d -> ObjectFields(project, d) { draft = it } }
        }
    }
}
