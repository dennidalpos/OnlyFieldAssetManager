package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.configurator.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.QuickAdd
import com.onlyfield.assetmanager.core.forms.PortLogic
import com.onlyfield.assetmanager.configurator.map.ObjectPickerDialog
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.Screen
import com.onlyfield.assetmanager.ui.components.*

@Composable
fun InventoryScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    var query by rememberSaveableString()
    var areaFilter by remember { mutableStateOf<Area?>(null) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var creating by remember { mutableStateOf(false) }
    var editingNew by remember { mutableStateOf<MapObjectDraft?>(null) }
    var batch by remember { mutableStateOf(false) }
    val confirm = LocalConfirm.current

    val devices = index.devices.filter {
        matchesQuery(query, it.technicalName, it.physicalLabel, it.alias, it.ipAddress, it.macAddress, it.serialNumber) && (areaFilter == null || it.areaId == areaFilter?.id)
    }.sortedForDisplay(i18n) { it.technicalName }

    AppScaffold(
        title = i18n.text("ux.nav.devices"),
        subtitle = i18n.text("text.e9f37f3828d1", devices.size, index.devices.size),
        onBack = { vm.back() },
        snackbarHost = snackbar,
        actions = {
            if (selecting) {
                TextButton(enabled = selected.isNotEmpty(), onClick = { batch = true }) { Text(i18n.text("text.19c9647425aa", selected.size)) }
                TextButton(onClick = { selecting = false; selected = emptySet() }) { Text(i18n.text("text.fa67303aecda")) }
            } else if (index.devices.isNotEmpty()) {
                TextButton(onClick = { selecting = true }) { Text(i18n.text("text.5b62ee34518b")) }
            }
        },
        floatingActionButton = {
            if (!selecting) ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.cf301d95d32c")) })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(query, { query = it }, i18n.text("text.ddf4aaf4dd0a"))
            if (index.areas.isNotEmpty()) {
                OptionPicker(i18n.text("text.024dc204d7ba"), index.areas, areaFilter, { it.name }, { areaFilter = it }, noneLabel = i18n.text("text.4c852ffc6db0"))
            }
            when {
                index.devices.isEmpty() -> EmptyState(i18n.text("text.8d6015db495b"))
                devices.isEmpty() -> EmptyState(i18n.text("ux.noResults"), actionLabel = i18n.text("text.c4483e052140"), onAction = { query = ""; areaFilter = null })
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices, key = { it.id }) { d ->
                        ItemCard(
                            title = d.technicalName + (d.alias?.let { " ($it)" } ?: ""),
                            badge = d.category.toDisplayString(i18n = i18n),
                            details = listOf(
                                listOfNotNull(d.areaId?.let { index.areaName(it) }, d.rackId?.let { i18n.text("text.f5ba7982ad75", index.rackName(it)) + (d.positionU?.let { u -> i18n.text("text.bf28e779d560", u) } ?: "") })
                                    .joinToString(" › "),
                                listOfNotNull(d.ipAddress, i18n.plural("text.53a2e3b94696", d.ports.size)).joinToString(" · ")
                            ),
                            leading = if (selecting) {
                                { Checkbox(checked = d.id in selected, onCheckedChange = { selected = if (it) selected + d.id else selected - d.id }) }
                            } else null,
                            menu = if (selecting) emptyList() else listOf(MenuAction(i18n.text("text.dd41b3275173"), destructive = true) {
                                confirm(ConfirmRequest(i18n.text("text.87fc0efddabf", d.technicalName), i18n.text("text.2548407c6a6b"), i18n.text("text.dd41b3275173")) {
                                    vm.moveToTrash("DEVICE", d.id, d.technicalName)
                                })
                            }),
                            onClick = {
                                if (selecting) selected = if (d.id in selected) selected - d.id else selected + d.id
                                else vm.navigate(Screen.DeviceDetail(d.id))
                            }
                        )
                    }
                }
            }
        }
    }

    if (creating) ObjectPickerDialog(project, i18n, null, { creating = false }, base = { inventoryDeviceDraft(project, null).withType(it) },
        onAdd = { draft -> creating = false; vm.edit(i18n.text("quick.added", QuickAdd.name(draft))) { draft.apply(it, i18n) } },
        onEdit = { draft -> creating = false; editingNew = draft }, filter = { it.kind == ObjectKind.DEVICE })
    editingNew?.let { draft -> DeviceDialog(vm, project, null, initial = draft) { editingNew = null } }
    if (batch) BatchDialog(vm, project, index, selected) { batch = false; selecting = false; selected = emptySet() }
}

@Composable
private fun rememberSaveableString() = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

@Composable
fun DeviceDetailScreen(vm: ProjectViewModel, project: Project, deviceId: String, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current
    val device = index.device(deviceId)
    if (device == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    var editing by remember { mutableStateOf(false) }
    var editingPorts by remember { mutableStateOf(false) }
    var replacing by remember { mutableStateOf(false) }
    var merging by remember { mutableStateOf(false) }
    val takePhoto = rememberPhotoCapture(vm)

    AppScaffold(
        title = device.technicalName,
        subtitle = device.category.toDisplayString(i18n = i18n),
        onBack = { vm.back() },
        snackbarHost = snackbar,
        actions = {
            TextButton(onClick = { takePhoto(AttachmentTargetType.DEVICE, device.id) }) { Text(i18n.text("text.494e0843d958")) }
            TextButton(onClick = { editing = true }) { Text(i18n.text("text.49e493ba9d9c")) }
            OverflowMenu(
                listOf(
                    MenuAction(i18n.text("text.475df7909caf")) { replacing = true },
                    MenuAction(i18n.text("text.99f9d71d6d8e")) { merging = true },
                    MenuAction(i18n.text("text.dd41b3275173"), destructive = true) {
                        confirm(ConfirmRequest(i18n.text("text.87fc0efddabf", device.technicalName), i18n.text("text.2548407c6a6b"), i18n.text("text.dd41b3275173")) {
                            vm.back()
                            vm.moveToTrash("DEVICE", device.id, device.technicalName)
                        })
                    }
                )
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow(i18n.text("text.e4de7d26b141"), index.businessUnitOf(device.id)?.name)
                        InfoRow(i18n.text("text.024dc204d7ba"), device.areaId?.let { index.areaName(it) })
                        InfoRow(i18n.text("text.4cd265c2b8c6"), device.rackId?.let { "${index.rackName(it)}" + (device.positionU?.let { u -> i18n.text("text.60c93506e0bf", u) } ?: "") + i18n.text("text.a5a90940b98f", device.heightU) })
                        InfoRow(i18n.text("text.9fe5b72aa900"), device.physicalLabel)
                        InfoRow(i18n.text("text.b19e02e9502b"), device.alias)
                        InfoRow("IP", device.ipAddress)
                        InfoRow("MAC", device.macAddress)
                        InfoRow(i18n.text("text.488b09e885d9"), device.serialNumber)
                        InfoRow(i18n.text("text.90c2d339a9d5"), project.deviceModels.find { it.id == device.deviceModelId }?.name)
                        InfoRow(i18n.text("text.9ac631f3dde4"), device.observation?.status?.toDisplayString(i18n = i18n))
                        device.observation?.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            val feeds = project.powerFeeds.filter { it.deviceId == device.id }
            val interfaces = project.logicalInterfaces.filter { it.deviceId == device.id }
            if (feeds.isNotEmpty() || interfaces.isNotEmpty()) {
                item {
                    Text(
                        listOfNotNull(
                            feeds.takeIf { it.isNotEmpty() }?.let { i18n.text("text.c75735e0ae06") + it.joinToString { f -> f.feedName } },
                            interfaces.takeIf { it.isNotEmpty() }?.let { i18n.text("text.209a42c686ba") + it.joinToString { i -> i.name } }
                        ).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            val photos = index.attachmentsOf(device.id)
            if (photos.isNotEmpty()) {
                item { SectionTitle(i18n.text("text.0db131a944fa", photos.size)) }
                items(photos, key = { it.id }) { a ->
                    ItemCard(title = a.name, details = listOf("${a.fileType.toDisplayString(i18n = i18n)} · ${a.originalFileName}"))
                }
            }
            // Ports as the panel drawing; tapping one opens the editor on Ports.
            if (device.ports.isNotEmpty()) {
                item { SectionTitle(i18n.text("text.625d94dac5fc", device.ports.size)) }
                item {
                    val cells = remember(project, device.id) { PortLogic.panel(project, device, index = index) }
                    PortPanel(cells, i18n, onClick = { editingPorts = true })
                }
            }
        }
    }

    if (editing) DeviceDialog(vm, project, device) { editing = false }
    if (editingPorts) DeviceDialog(vm, project, device, initialSection = ConfiguratorPage.PORTS) { editingPorts = false }
    if (replacing) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(device.category) }
        EditScreen(i18n.text("text.29f6b1f39967"), { replacing = false }, {
            replacing = false
            vm.back()
            vm.replaceDevice(device.id, name.trim(), category)
        }, confirmEnabled = name.isNotBlank(), confirmLabel = i18n.text("text.3260dc474cbc")) {
            Text(i18n.text("text.a6c13e0dbb08"), style = MaterialTheme.typography.bodySmall)
            FormField(name, { name = it }, i18n.text("text.9d9ce7aec419"))
            EnumPicker(i18n.text("text.54276aa0307f"), DeviceCategory.entries, category, { it.toDisplayString(i18n = i18n) }, { category = it })
        }
    }
    if (merging) {
        var duplicate by remember { mutableStateOf<Device?>(null) }
        var choices by remember { mutableStateOf(MergeDataChoices()) }
        EditScreen(i18n.text("text.e546e4f9c2ba"), { merging = false }, {
            merging = false
            vm.mergeDevices(device.id, duplicate!!.id, choices)
        }, confirmEnabled = duplicate != null, confirmLabel = i18n.text("text.c442a0f989e0")) {
            Text(i18n.text("text.85c628019422", device.technicalName), style = MaterialTheme.typography.bodySmall)
            OptionPicker(i18n.text("text.505c57b1f96f"), index.devices.filter { it.id != device.id }, duplicate, { it.technicalName }, { duplicate = it },
                optionDetail = { it.ipAddress })
            duplicate?.let { dup ->
                SectionTitle(i18n.text("text.b8446f35e08e"))
                LabeledCheckbox(choices.useTechnicalNameFromDuplicate, { choices = choices.copy(useTechnicalNameFromDuplicate = it) }, i18n.text("text.ddd991c1f573", dup.technicalName))
                LabeledCheckbox(choices.useIpFromDuplicate, { choices = choices.copy(useIpFromDuplicate = it) }, i18n.text("text.ab03ca58781a", dup.ipAddress ?: "vuoto"))
                LabeledCheckbox(choices.useMacFromDuplicate, { choices = choices.copy(useMacFromDuplicate = it) }, i18n.text("text.204c390b7016", dup.macAddress ?: "vuoto"))
                LabeledCheckbox(choices.usePhysicalLabelFromDuplicate, { choices = choices.copy(usePhysicalLabelFromDuplicate = it) }, i18n.text("text.9fe5b72aa900"))
                LabeledCheckbox(choices.useLocationFromDuplicate, { choices = choices.copy(useLocationFromDuplicate = it) }, i18n.text("text.d7effa1c65a8"))
                LabeledCheckbox(choices.mergePorts, { choices = choices.copy(mergePorts = it) }, i18n.text("text.f059285a3fb1", dup.ports.size))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun DeviceDialog(vm: ProjectViewModel, project: Project, device: Device?, initialSerial: String? = null, initial: MapObjectDraft? = null,
                          initialSection: ConfiguratorPage = ConfiguratorPage.ESSENTIALS, onClose: () -> Unit) {
    val i18n = LocalMessages.current
    var draft by remember(device) { mutableStateOf(initial ?: inventoryDeviceDraft(project, device).let { d -> initialSerial?.let { d.copy(device = d.device.copy(serialNumber = it)) } ?: d }) }
    var scanningSerial by remember { mutableStateOf(false) }
    EditScreen(configuratorTitle(project, draft, i18n), onClose, {
        onClose(); vm.edit(configuratorTitle(project, draft, i18n)) { draft.apply(it, i18n) }
    }, validationMessage = configuratorValidation(project, draft, i18n), confirmEnabled = draft.errors(project, i18n).isEmpty(), confirmLabel = configuratorAction(project, draft, i18n)) {
        ObjectFields(project, draft, initialSection) { draft = it }
        ConfiguratorSection(i18n.text("ux.scanSerial"), i18n = i18n) {
            OutlinedButton(onClick = { scanningSerial = true }) { Text(i18n.text("ux.scanSerial")) }
        }
    }
    if (scanningSerial) BarcodeScanner(onCode = { code -> scanningSerial = false; draft = draft.copy(device = draft.device.copy(serialNumber = code)) }, onClose = { scanningSerial = false }, hint = i18n.text("text.02b33d3895e8"))
}

@Composable
private fun BatchDialog(vm: ProjectViewModel, project: Project, index: ProjectIndex, ids: Set<String>, onClose: () -> Unit) {
    val i18n = LocalMessages.current

    var changes by remember { mutableStateOf(BatchDeviceChanges(category = DeviceCategory.NETWORK_SWITCH)) }
    val any = changes.updateCategory || changes.updateAreaId || changes.updateRackId || changes.updateObservationNotes
    EditScreen(i18n.text("text.667a7f57a15e", ids.size), onClose, {
        onClose()
        vm.edit(i18n.text("text.9a5c72e77350", ids.size)) { ProjectEdits.batchEditDevices(it, ids.toList(), changes, i18n = i18n) }
    }, confirmEnabled = any, confirmLabel = i18n.text("text.7a1ff7ffd286")) {
        Text(ids.mapNotNull { index.device(it)?.technicalName }.joinToString(", "), style = MaterialTheme.typography.bodySmall)
        LabeledCheckbox(changes.updateCategory, { changes = changes.copy(updateCategory = it) }, i18n.text("text.e87af9d65308"))
        if (changes.updateCategory) EnumPicker(i18n.text("text.54276aa0307f"), DeviceCategory.entries, changes.category ?: DeviceCategory.CUSTOM, { it.toDisplayString(i18n = i18n) }, { changes = changes.copy(category = it) })
        LabeledCheckbox(changes.updateAreaId, { changes = changes.copy(updateAreaId = it) }, i18n.text("text.4b1d7d720dd9"))
        if (changes.updateAreaId) OptionPicker(i18n.text("text.024dc204d7ba"), index.areas, index.area(changes.areaId), { it.name }, { changes = changes.copy(areaId = it?.id) }, noneLabel = i18n.text("text.0eb949b8ab9b"))
        LabeledCheckbox(changes.updateRackId, { changes = changes.copy(updateRackId = it) }, i18n.text("text.5b6890ba529a"))
        if (changes.updateRackId) OptionPicker(i18n.text("text.4cd265c2b8c6"), project.racks, index.rack(changes.rackId), { it.name }, { changes = changes.copy(rackId = it?.id) }, noneLabel = i18n.text("text.da968f7d518f"))
        LabeledCheckbox(changes.updateObservationNotes, { changes = changes.copy(updateObservationNotes = it) }, i18n.text("text.d0210741d000"))
        if (changes.updateObservationNotes) FormField(changes.observationNotes.orEmpty(), { changes = changes.copy(observationNotes = it) }, i18n.text("text.d8da2c49df39"), singleLine = false)
    }
}
