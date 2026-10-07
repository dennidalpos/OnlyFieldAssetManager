package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.QuickAdd
import com.onlyfield.assetmanager.configurator.map.ObjectPickerDialog
import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.configurator.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.scan.CodeLookup
import com.onlyfield.assetmanager.core.scan.CodeMatch

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun InventorySection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit,
    onTrashItemCreated: (TrashItem) -> Unit,
    onMergeDevices: (String, String, MergeDataChoices) -> Boolean,
    saveError: () -> String? = { null },
) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current

    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<DeviceCategory?>(null) }
    var areaFilter by remember { mutableStateOf<Area?>(null) }
    var statusFilter by remember { mutableStateOf<OperationalStatus?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    var editing by remember { mutableStateOf<Device?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }
    var editingNew by remember { mutableStateOf<MapObjectDraft?>(null) }
    var portsOf by remember { mutableStateOf<String?>(null) }
    var replaceTarget by remember { mutableStateOf<Device?>(null) }
    var mergeTarget by remember { mutableStateOf<Device?>(null) }
    var showBatch by remember { mutableStateOf(false) }

    val filtered = remember(index, query, categoryFilter, areaFilter, statusFilter, i18n.locale) {
        index.devices.filter { d ->
            matchesQuery(query, d.technicalName, d.physicalLabel, d.alias, d.ipAddress, d.macAddress, d.serialNumber) &&
                (categoryFilter == null || d.category == categoryFilter) &&
                (areaFilter == null || d.areaId == areaFilter?.id) &&
                (statusFilter == null || d.operationalStatus == statusFilter)
        }.sortedForDisplay(i18n) { it.technicalName }
    }
    // Drop selections of devices that no longer exist.
    LaunchedEffect(index) { selectedIds = selectedIds.filter { index.device(it) != null }.toSet() }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = i18n.text("ux.nav.devices"),
            subtitle = i18n.text("text.e9f37f3828d1", filtered.size, index.devices.size),
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = i18n.text("text.ddf4aaf4dd0a"),
            onSearchSubmit = {
                // Exact code (e.g. from a USB reader): open the device it identifies.
                when (val match = CodeLookup.find(index, query, i18n = i18n)) {
                    is CodeMatch.DeviceMatch -> changeDetail { editing = match.device }
                    is CodeMatch.PortMatch -> changeDetail { portsOf = match.port.device.id }
                    else -> Unit
                }
            }
        ) {
            Button(onClick = { changeDetail { creating = true } }, enabled = project.sites.isNotEmpty()) { Text(i18n.text("text.8650e4573818")) }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionPicker(
                label = i18n.text("text.54276aa0307f"),
                options = DeviceCategory.entries,
                selected = categoryFilter,
                optionLabel = { it.toDisplayString(i18n = i18n) },
                onSelected = { categoryFilter = it },
                noneLabel = i18n.text("text.d67bee20ea14"),
                modifier = Modifier.widthIn(max = 240.dp)
            )
            OptionPicker(
                label = i18n.text("text.024dc204d7ba"),
                options = index.areas,
                selected = areaFilter,
                optionLabel = { it.name },
                onSelected = { areaFilter = it },
                noneLabel = i18n.text("text.4c852ffc6db0"),
                modifier = Modifier.widthIn(max = 240.dp)
            )
            OptionPicker(
                label = i18n.text("device.status"),
                options = OperationalStatus.entries,
                selected = statusFilter,
                optionLabel = { it.toDisplayString(i18n = i18n) },
                onSelected = { statusFilter = it },
                noneLabel = i18n.text("status.filterAll"),
                modifier = Modifier.widthIn(max = 240.dp)
            )
            if (selectedIds.isNotEmpty()) {
                Text(i18n.text("text.a3de97a5039a", selectedIds.size), fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { changeDetail { showBatch = true } }) { Text(i18n.text("text.12fea36b47a9")) }
                TextButton(onClick = { selectedIds = emptySet() }) { Text(i18n.text("text.a4918611156f")) }
            } else if (filtered.isNotEmpty()) {
                TextButton(onClick = { selectedIds = filtered.map { it.id }.toSet() }) { Text(i18n.text("text.0e5b81f76ba6")) }
            }
        }

        when {
            project.sites.isEmpty() -> EmptyState(i18n.text("text.d07b43aa8cb1"))
            index.devices.isEmpty() -> EmptyState(i18n.text("text.acc21f707eb1"), actionLabel = i18n.text("text.8650e4573818"), onAction = { creating = true })
            filtered.isEmpty() -> EmptyState(i18n.text("text.4e750f66126c"), actionLabel = i18n.text("text.c4483e052140"), onAction = {
                query = ""; categoryFilter = null; areaFilter = null; statusFilter = null
            })
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.id }) { dev ->
                    val site = index.siteOf(dev.id)
                    val location = listOfNotNull(
                        site?.name,
                        dev.areaId?.let { index.areaName(it, i18n.text("text.8b721ed0312b")) },
                        dev.rackId?.let { i18n.text("text.f5ba7982ad75", index.rackName(it)) + (dev.positionU?.let { u -> i18n.text("text.60c93506e0bf", u) } ?: "") }
                    ).joinToString(" › ")
                    val network = listOfNotNull(dev.ipAddress?.let { i18n.text("text.ef7dea7beb29", it) }, dev.macAddress?.let { i18n.text("text.98503a17d766", it) }, dev.physicalLabel?.let { i18n.text("text.7ef850a6b73f", it) })
                        .joinToString(" · ")
                    var menuOpen by remember { mutableStateOf(false) }
                    ItemCard(
                        title = dev.technicalName + (dev.alias?.let { " ($it)" } ?: ""),
                        badge = dev.category.toDisplayString(i18n = i18n),
                        details = listOfNotNull(location, listOf(network, i18n.plural("text.53a2e3b94696", dev.ports.size)).filter { it.isNotBlank() }.joinToString(" · "),
                            dev.operationalStatus.takeIf { it != OperationalStatus.IN_SERVICE }?.let { i18n.text("device.status") + ": " + it.toDisplayString(i18n) }),
                        selected = editing?.id == dev.id || portsOf == dev.id,
                        onClick = { changeDetail { editing = dev } },
                        leading = {
                            Checkbox(
                                checked = dev.id in selectedIds,
                                onCheckedChange = { selectedIds = if (it) selectedIds + dev.id else selectedIds - dev.id }
                            )
                        }
                    ) {
                        TextButton(onClick = { changeDetail { portsOf = dev.id } }) { Text(i18n.text("text.2edfc95a3c46")) }
                        EditButton { editing = dev }
                        Box {
                            TextButton(onClick = { menuOpen = true }) { Text(i18n.text("text.9cc22637b4a3")) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(i18n.text("text.475df7909caf")) }, onClick = { menuOpen = false; changeDetail { replaceTarget = dev } })
                                DropdownMenuItem(
                                    text = { Text(i18n.text("text.99f9d71d6d8e")) },
                                    enabled = index.devices.size > 1,
                                    onClick = { menuOpen = false; changeDetail { mergeTarget = dev } }
                                )
                            }
                        }
                        DeleteButton(dev.technicalName, label = i18n.text("text.dd41b3275173"), message = i18n.text("text.f509b29b1f58"), onDelete = {
                            val (updated, trashItem) = ProjectEdits.deleteDeviceToTrash(project, dev.id, i18n = i18n)
                            trashItem?.let(onTrashItemCreated)
                            onProjectUpdated(updated, i18n.text("text.4e2629d50c9b", dev.technicalName))
                        })
                    }
                }
            }
        }
    }

    if (creating) ObjectPickerDialog(project, i18n, null, { creating = false }, base = { inventoryDeviceDraft(project, null).withType(it) },
        onAdd = { draft ->
            onProjectUpdated(draft.apply(project, i18n), i18n.text("quick.added", QuickAdd.name(draft)))
            if (saveError() == null) creating = false
        },
        onEdit = { draft -> creating = false; editingNew = draft }, filter = { it.kind == ObjectKind.DEVICE }, error = saveError())
    if (editingNew != null || editing != null) {
        DeviceDialog(
            project = project,
            device = editing,
            onDismiss = { editingNew = null; editing = null },
            onSave = { updated, message -> editingNew = null; editing = null; onProjectUpdated(updated, message) },
            initial = editingNew,
        )
    }

    portsOf?.let { id ->
        index.device(id)?.let { dev -> DeviceDialog(project, dev, { portsOf = null }, onProjectUpdated, ConfiguratorPage.PORTS) }
            ?: run { portsOf = null }
    }

    replaceTarget?.let { dev ->
        ReplaceDialog(dev, onDismiss = { replaceTarget = null }) { name, category ->
            val (updated, trashItem) = ProjectEdits.replaceDevice(project, dev.id, name, category, i18n = i18n)
            replaceTarget = null
            trashItem?.let(onTrashItemCreated)
            onProjectUpdated(updated, i18n.text("text.6dafb91fca82", dev.technicalName, name))
        }
    }

    mergeTarget?.let { survivor ->
        MergeDialog(index, survivor, onDismiss = { mergeTarget = null }) { duplicateId, choices ->
            if (onMergeDevices(survivor.id, duplicateId, choices)) mergeTarget = null
        }
    }

    if (showBatch) {
        BatchEditDialog(project, index, selectedIds, onDismiss = { showBatch = false }) { changes ->
            val count = selectedIds.size
            val updated = ProjectEdits.batchEditDevices(project, selectedIds.toList(), changes, i18n = i18n)
            showBatch = false
            selectedIds = emptySet()
            onProjectUpdated(updated, i18n.text("text.d57904b4d677", count))
        }
    }
}

@Composable
private fun DeviceDialog(
    project: Project,
    device: Device?,
    onDismiss: () -> Unit,
    onSave: (Project, String) -> Unit,
    initialSection: ConfiguratorPage = ConfiguratorPage.ESSENTIALS,
    initial: MapObjectDraft? = null,
) {
    val i18n = LocalMessages.current
    var draft by remember(LocalDetailSlot.current?.editorVersion, device) { mutableStateOf(initial ?: inventoryDeviceDraft(project, device)) }
    EditPanel(title = configuratorTitle(project, draft, i18n, initialSection), confirmLabel = configuratorAction(project, draft, i18n), onDismiss = onDismiss,
        validationMessage = configuratorValidation(project, draft, i18n), confirmEnabled = draft.errors(project, i18n).isEmpty(), width = 800.dp,
        onConfirm = { onSave(draft.apply(project, i18n), configuratorTitle(project, draft, i18n)) }) {
        ObjectFields(project, draft, initialSection) { draft = it }
    }
}

@Composable
private fun ReplaceDialog(device: Device, onDismiss: () -> Unit, onConfirm: (String, DeviceCategory) -> Unit) {
    val i18n = LocalMessages.current

    var name by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf("") }
    var category by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(device.category) }
    EditPanel(
        title = i18n.text("text.db1d35f100ab", device.technicalName),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(name.trim(), category) },
        confirmEnabled = name.isNotBlank(),
        confirmLabel = i18n.text("text.3260dc474cbc"),
        width = 520.dp
    ) {
        Text(i18n.text("text.dc43498aef85"))
        FormField(name, { name = it }, i18n.text("text.9d9ce7aec419"))
        EnumPicker(i18n.text("text.54276aa0307f"), DeviceCategory.entries, category, { it.toDisplayString(i18n = i18n) }, { category = it })
    }
}

@Composable
private fun MergeDialog(index: ProjectIndex, survivor: Device, onDismiss: () -> Unit, onConfirm: (String, MergeDataChoices) -> Unit) {
    val i18n = LocalMessages.current

    val candidates = index.devices.filter { it.id != survivor.id }
    var duplicate by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf<Device?>(null) }
    var choices by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(MergeDataChoices()) }

    EditPanel(
        title = i18n.text("text.8622418bc21e", survivor.technicalName),
        onDismiss = onDismiss,
        onConfirm = { duplicate?.let { onConfirm(it.id, choices) } },
        confirmEnabled = duplicate != null,
        confirmLabel = i18n.text("text.c442a0f989e0"),
        width = 600.dp
    ) {
        Text(i18n.text("text.7f27c2f83658"))
        OptionPicker(
            label = i18n.text("text.505c57b1f96f"),
            options = candidates,
            selected = duplicate,
            optionLabel = { it.technicalName },
            optionDetail = { listOfNotNull(it.ipAddress, index.areaName(it.areaId, "")).joinToString(" · ") },
            onSelected = { duplicate = it }
        )
        duplicate?.let { dup ->
            Text(i18n.text("text.ba17b5a14990"), fontWeight = FontWeight.SemiBold)
            LabeledCheckbox(choices.useAliasFromDuplicate, { choices = choices.copy(useAliasFromDuplicate = it) }, i18n.text("text.2feff34b895f", dup.alias ?: "vuoto"))
            LabeledCheckbox(choices.useTechnicalNameFromDuplicate, { choices = choices.copy(useTechnicalNameFromDuplicate = it) }, i18n.text("text.f056df92d232", dup.technicalName))
            LabeledCheckbox(choices.usePhysicalLabelFromDuplicate, { choices = choices.copy(usePhysicalLabelFromDuplicate = it) }, i18n.text("text.6c6b0e8c59a2", dup.physicalLabel ?: "vuota"))
            LabeledCheckbox(choices.useIpFromDuplicate, { choices = choices.copy(useIpFromDuplicate = it) }, i18n.text("text.e22da0229a30", dup.ipAddress ?: "vuoto"))
            LabeledCheckbox(choices.useMacFromDuplicate, { choices = choices.copy(useMacFromDuplicate = it) }, i18n.text("text.c893c36f0a38", dup.macAddress ?: "vuoto"))
            LabeledCheckbox(choices.useLocationFromDuplicate, { choices = choices.copy(useLocationFromDuplicate = it) }, i18n.text("text.6d44f00413d7"))
            Text("Trasferisci:", fontWeight = FontWeight.SemiBold)
            LabeledCheckbox(choices.mergePorts, { choices = choices.copy(mergePorts = it) }, i18n.text("text.625d94dac5fc", dup.ports.size))
            LabeledCheckbox(choices.mergeCredentials, { choices = choices.copy(mergeCredentials = it) }, i18n.text("merge.credentials"))
            LabeledCheckbox(choices.mergeConfigurations, { choices = choices.copy(mergeConfigurations = it) }, i18n.text("merge.configurations"))
            LabeledCheckbox(choices.mergePowerFeeds, { choices = choices.copy(mergePowerFeeds = it) }, i18n.text("merge.powerFeeds"))
            LabeledCheckbox(choices.mergeExtraFields, { choices = choices.copy(mergeExtraFields = it) }, i18n.text("merge.extraFields"))
            Text(i18n.text("merge.associatedHint"), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BatchEditDialog(
    project: Project,
    index: ProjectIndex,
    selectedIds: Set<String>,
    onDismiss: () -> Unit,
    onApply: (BatchDeviceChanges) -> Unit,
) {
    val i18n = LocalMessages.current

    var changes by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(BatchDeviceChanges(category = DeviceCategory.NETWORK_SWITCH, mountingType = MountingType.RACK_MOUNT)) }
    val any = changes.updateCategory || changes.updateAreaId || changes.updateRackId || changes.updateMountingType || changes.updateObservationNotes

    EditPanel(
        title = i18n.text("text.48263b4ad8ed"),
        onDismiss = onDismiss,
        onConfirm = { onApply(changes) },
        confirmEnabled = any,
        confirmLabel = i18n.text("text.635382b11942", selectedIds.size),
        width = 600.dp
    ) {
        Text(
            selectedIds.mapNotNull { index.device(it)?.technicalName }.sorted().joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3
        )
        Text(i18n.text("text.4b100c535572"), fontWeight = FontWeight.SemiBold)
        LabeledCheckbox(changes.updateCategory, { changes = changes.copy(updateCategory = it) }, i18n.text("text.54276aa0307f"))
        if (changes.updateCategory) EnumPicker(i18n.text("text.54276aa0307f"), DeviceCategory.entries, changes.category ?: DeviceCategory.CUSTOM, { it.toDisplayString(i18n = i18n) }, { changes = changes.copy(category = it) })
        LabeledCheckbox(changes.updateAreaId, { changes = changes.copy(updateAreaId = it) }, i18n.text("text.024dc204d7ba"))
        if (changes.updateAreaId) OptionPicker(i18n.text("text.024dc204d7ba"), index.areas, index.area(changes.areaId), { it.name }, { changes = changes.copy(areaId = it?.id) }, noneLabel = i18n.text("text.0eb949b8ab9b"))
        LabeledCheckbox(changes.updateRackId, { changes = changes.copy(updateRackId = it) }, i18n.text("text.4cd265c2b8c6"))
        if (changes.updateRackId) OptionPicker(i18n.text("text.4cd265c2b8c6"), project.racks, index.rack(changes.rackId), { it.name }, { changes = changes.copy(rackId = it?.id) }, noneLabel = i18n.text("text.da968f7d518f"))
        LabeledCheckbox(changes.updateMountingType, { changes = changes.copy(updateMountingType = it) }, i18n.text("text.6a0a9db35cde"))
        if (changes.updateMountingType) EnumPicker(i18n.text("text.d3a2e8299422"), MountingType.entries, changes.mountingType ?: MountingType.RACK_MOUNT, { it.toDisplayString(i18n = i18n) }, { changes = changes.copy(mountingType = it) })
        LabeledCheckbox(changes.updateObservationNotes, { changes = changes.copy(updateObservationNotes = it) }, i18n.text("text.b8320c85f325"))
        if (changes.updateObservationNotes) FormField(changes.observationNotes.orEmpty(), { changes = changes.copy(observationNotes = it) }, i18n.text("text.d8da2c49df39"), singleLine = false)
    }
}
