package com.onlyfield.assetmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.DeviceForm
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.Screen
import com.onlyfield.assetmanager.ui.components.*

private const val OBSERVATION_SOURCE = "App Android"

@Composable
fun InventoryScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val index = remember(project) { ProjectIndex(project) }
    var query by rememberSaveableString()
    var areaFilter by remember { mutableStateOf<Area?>(null) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var creating by remember { mutableStateOf(false) }
    var batch by remember { mutableStateOf(false) }

    val devices = index.devices.filter {
        matchesQuery(query, it.technicalName, it.physicalLabel, it.alias, it.ipAddress, it.macAddress) && (areaFilter == null || it.areaId == areaFilter?.id)
    }

    AppScaffold(
        title = "Inventario",
        subtitle = "${devices.size} di ${index.devices.size} apparati",
        onBack = { vm.back() },
        snackbarHost = snackbar,
        actions = {
            if (selecting) {
                TextButton(enabled = selected.isNotEmpty(), onClick = { batch = true }) { Text("Modifica (${selected.size})") }
                TextButton(onClick = { selecting = false; selected = emptySet() }) { Text("Fine") }
            } else if (index.devices.isNotEmpty()) {
                TextButton(onClick = { selecting = true }) { Text("Seleziona") }
            }
        },
        floatingActionButton = {
            if (!selecting) ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Apparato") })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(query, { query = it }, "Cerca nome, etichetta, IP, MAC…")
            if (index.areas.isNotEmpty()) {
                OptionPicker("Area", index.areas, areaFilter, { it.name }, { areaFilter = it }, noneLabel = "Tutte le aree")
            }
            when {
                index.devices.isEmpty() -> EmptyState("Nessun apparato. Aggiungi il primo con il pulsante in basso.")
                devices.isEmpty() -> EmptyState("Nessun apparato corrisponde alla ricerca.")
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices, key = { it.id }) { d ->
                        ItemCard(
                            title = d.technicalName + (d.alias?.let { " ($it)" } ?: ""),
                            badge = d.category.toDisplayString(),
                            details = listOf(
                                listOfNotNull(d.areaId?.let { index.areaName(it) }, d.rackId?.let { "Rack ${index.rackName(it)}" + (d.positionU?.let { u -> " U$u" } ?: "") })
                                    .joinToString(" › "),
                                listOfNotNull(d.ipAddress, "${d.ports.size} porte").joinToString(" · ")
                            ),
                            leading = if (selecting) {
                                { Checkbox(checked = d.id in selected, onCheckedChange = { selected = if (it) selected + d.id else selected - d.id }) }
                            } else null,
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

    if (creating) DeviceDialog(vm, project, index, null) { creating = false }
    if (batch) BatchDialog(vm, project, index, selected) { batch = false; selecting = false; selected = emptySet() }
}

@Composable
private fun rememberSaveableString() = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

@Composable
fun DeviceDetailScreen(vm: ProjectViewModel, project: Project, deviceId: String, snackbar: SnackbarHostState) {
    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current
    val device = index.device(deviceId)
    if (device == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    var editing by remember { mutableStateOf(false) }
    var addingPort by remember { mutableStateOf(false) }
    var replacing by remember { mutableStateOf(false) }
    var merging by remember { mutableStateOf(false) }
    val takePhoto = rememberPhotoCapture(vm)

    AppScaffold(
        title = device.technicalName,
        subtitle = device.category.toDisplayString(),
        onBack = { vm.back() },
        snackbarHost = snackbar,
        actions = {
            TextButton(onClick = { takePhoto(AttachmentTargetType.DEVICE, device.id) }) { Text("Foto") }
            TextButton(onClick = { editing = true }) { Text("Modifica") }
            OverflowMenu(
                listOf(
                    MenuAction("Sostituisci con nuovo apparato…") { replacing = true },
                    MenuAction("Unisci un duplicato…") { merging = true },
                    MenuAction("Sposta nel cestino", destructive = true) {
                        confirm(ConfirmRequest("Spostare «${device.technicalName}» nel cestino?", "I cavi collegati alle sue porte verranno scollegati. Potrai ripristinarlo dal Cestino.", "Sposta nel cestino") {
                            vm.back()
                            vm.moveToTrash("DEVICE", device.id, device.technicalName)
                        })
                    }
                )
            )
        },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { addingPort = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Porta") }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow("Business unit", index.businessUnitOf(device.id)?.name)
                        InfoRow("Area", device.areaId?.let { index.areaName(it) })
                        InfoRow("Rack", device.rackId?.let { "${index.rackName(it)}" + (device.positionU?.let { u -> " · U$u" } ?: "") + " · ${device.heightU}U" })
                        InfoRow("Etichetta", device.physicalLabel)
                        InfoRow("Alias", device.alias)
                        InfoRow("IP", device.ipAddress)
                        InfoRow("MAC", device.macAddress)
                        InfoRow("Modello", project.deviceModels.find { it.id == device.deviceModelId }?.name)
                        InfoRow("Rilievo", device.observation?.status?.toDisplayString() ?: "Da verificare")
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
                            feeds.takeIf { it.isNotEmpty() }?.let { "Alimentazioni: " + it.joinToString { f -> f.feedName } },
                            interfaces.takeIf { it.isNotEmpty() }?.let { "Interfacce: " + it.joinToString { i -> i.name } }
                        ).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            val photos = index.attachmentsOf(device.id)
            if (photos.isNotEmpty()) {
                item { SectionTitle("Foto e allegati (${photos.size})") }
                items(photos, key = { it.id }) { a ->
                    ItemCard(title = a.name, details = listOf("${a.fileType.toDisplayString()} · ${a.originalFileName}"))
                }
            }
            item { SectionTitle("Porte (${device.ports.size})") }
            if (device.ports.isEmpty()) item { Text("Nessuna porta. Aggiungile o applica un modello con template porte.", style = MaterialTheme.typography.bodySmall) }
            items(device.ports, key = { it.id }) { port ->
                val cable = project.cables.find { it.portAId == port.id || it.portBId == port.id }
                val peer = cable?.let { if (it.portAId == port.id) it.portBId else it.portAId }
                ItemCard(
                    title = port.name,
                    details = listOf(port.label.orEmpty(), if (cable != null) "Collegata a ${index.portLabel(peer, "estremità libera")}" else "Libera"),
                    menu = listOf(MenuAction("Elimina porta", destructive = true) {
                        confirm(ConfirmRequest("Eliminare la porta ${port.name}?", if (cable != null) "Il cavo collegato resterà con un'estremità libera." else "La porta verrà eliminata.") {
                            vm.edit("Porta ${port.name} eliminata.") { ProjectEdits.deletePortFromDevice(it, device.id, port.id) }
                        })
                    })
                )
            }
        }
    }

    if (editing) DeviceDialog(vm, project, index, device) { editing = false }
    if (addingPort) {
        var name by remember { mutableStateOf("") }
        var label by remember { mutableStateOf("") }
        val duplicate = device.ports.any { it.name.equals(name.trim(), ignoreCase = true) }
        EditScreen("Nuova porta", { addingPort = false }, {
            addingPort = false
            vm.edit("Porta ${name.trim()} aggiunta.") { ProjectEdits.addPortToDevice(it, device.id, name.trim(), label.trim().ifBlank { null }) }
        }, confirmEnabled = name.isNotBlank() && !duplicate) {
            FormField(name, { name = it }, "Nome porta *", error = if (duplicate) "Porta già presente" else null, hint = "Es. Gi1/0/1")
            FormField(label, { label = it }, "Etichetta")
        }
    }
    if (replacing) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(device.category) }
        EditScreen("Sostituisci apparato", { replacing = false }, {
            replacing = false
            vm.back()
            vm.replaceDevice(device.id, name.trim(), category)
        }, confirmEnabled = name.isNotBlank(), confirmLabel = "Sostituisci") {
            Text("Il nuovo apparato eredita area e rack dell'attuale, che viene spostato nel cestino.", style = MaterialTheme.typography.bodySmall)
            FormField(name, { name = it }, "Nome del nuovo apparato *")
            EnumPicker("Categoria", DeviceCategory.entries, category, { it.toDisplayString() }, { category = it })
        }
    }
    if (merging) {
        var duplicate by remember { mutableStateOf<Device?>(null) }
        var choices by remember { mutableStateOf(MergeDataChoices()) }
        EditScreen("Unisci un duplicato", { merging = false }, {
            merging = false
            vm.mergeDevices(device.id, duplicate!!.id, choices)
        }, confirmEnabled = duplicate != null, confirmLabel = "Unisci") {
            Text("Il duplicato viene spostato nel cestino dopo aver copiato i dati scelti in «${device.technicalName}».", style = MaterialTheme.typography.bodySmall)
            OptionPicker("Apparato duplicato *", index.devices.filter { it.id != device.id }, duplicate, { it.technicalName }, { duplicate = it },
                optionDetail = { it.ipAddress })
            duplicate?.let { dup ->
                SectionTitle("Usa dal duplicato")
                LabeledCheckbox(choices.useTechnicalNameFromDuplicate, { choices = choices.copy(useTechnicalNameFromDuplicate = it) }, "Nome (${dup.technicalName})")
                LabeledCheckbox(choices.useIpFromDuplicate, { choices = choices.copy(useIpFromDuplicate = it) }, "IP (${dup.ipAddress ?: "vuoto"})")
                LabeledCheckbox(choices.useMacFromDuplicate, { choices = choices.copy(useMacFromDuplicate = it) }, "MAC (${dup.macAddress ?: "vuoto"})")
                LabeledCheckbox(choices.usePhysicalLabelFromDuplicate, { choices = choices.copy(usePhysicalLabelFromDuplicate = it) }, "Etichetta")
                LabeledCheckbox(choices.useLocationFromDuplicate, { choices = choices.copy(useLocationFromDuplicate = it) }, "Posizione")
                LabeledCheckbox(choices.mergePorts, { choices = choices.copy(mergePorts = it) }, "Trasferisci le porte (${dup.ports.size})")
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
internal fun DeviceDialog(vm: ProjectViewModel, project: Project, index: ProjectIndex, device: Device?, onClose: () -> Unit) {
    val initialBu = device?.let { index.businessUnitOf(it.id)?.id } ?: project.businessUnits.firstOrNull()?.id
    var form by remember(device) { mutableStateOf(DeviceForm.from(device, initialBu)) }
    val rack = index.rack(form.rackId)
    val errors = form.errors(rack?.heightU)
    val buAreas = project.businessUnits.find { it.id == form.businessUnitId }?.let { bu -> bu.areas + bu.sites.flatMap { it.areas } } ?: index.areas

    EditScreen(
        title = if (device == null) "Nuovo apparato" else "Modifica apparato",
        onDismiss = onClose,
        confirmEnabled = errors.isEmpty(),
        onConfirm = {
            val saved = form.toDevice(device, OBSERVATION_SOURCE)
            onClose()
            if (device == null) vm.edit("Apparato «${saved.technicalName}» creato.") { ProjectEdits.addDevice(it, form.businessUnitId!!, saved) }
            else vm.edit("Apparato «${saved.technicalName}» aggiornato.") { ProjectEdits.updateDevice(it, saved) }
        }
    ) {
        FormField(form.technicalName, { form = form.copy(technicalName = it) }, "Nome tecnico *", error = errors["technicalName"])
        EnumPicker("Categoria", DeviceCategory.entries, form.category, { it.toDisplayString() }, { form = form.copy(category = it) })
        FormField(form.physicalLabel, { form = form.copy(physicalLabel = it) }, "Etichetta fisica")
        FormField(form.alias, { form = form.copy(alias = it) }, "Alias")
        FormField(form.ipAddress, { form = form.copy(ipAddress = it) }, "Indirizzo IP", error = errors["ipAddress"], kind = FieldKind.IP)
        FormField(form.macAddress, { form = form.copy(macAddress = it) }, "Indirizzo MAC", error = errors["macAddress"])
        SectionTitle("Posizione")
        if (project.businessUnits.size > 1 || device == null) {
            OptionPicker("Business unit *", project.businessUnits, project.businessUnits.find { it.id == form.businessUnitId }, { it.name },
                { form = form.copy(businessUnitId = it?.id, areaId = null) }, enabled = device == null,
                supportingText = if (device != null) "Non modificabile dopo la creazione" else null)
        }
        OptionPicker("Area", buAreas, index.area(form.areaId), { it.name }, { form = form.copy(areaId = it?.id) }, noneLabel = "Nessuna area")
        OptionPicker("Rack", project.racks, rack, { it.name }, {
            form = form.copy(rackId = it?.id, mountingType = if (it == null) MountingType.OUT_OF_RACK else if (form.mountingType == MountingType.OUT_OF_RACK) MountingType.RACK_MOUNT else form.mountingType)
        }, noneLabel = "Fuori rack", optionDetail = { "${it.heightU}U" })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (form.rackId != null) FormField(form.positionU, { form = form.copy(positionU = it) }, "Posizione U", Modifier.weight(1f), errors["positionU"], kind = FieldKind.NUMBER, hint = rack?.let { "1–${it.heightU}" })
            FormField(form.heightU, { form = form.copy(heightU = it) }, "Altezza U *", Modifier.weight(1f), errors["heightU"], kind = FieldKind.NUMBER)
        }
        if (form.rackId != null) EnumPicker("Lato", RackSide.entries, form.rackSide, { it.toDisplayString() }, { form = form.copy(rackSide = it) })
        if (project.deviceModels.isNotEmpty()) {
            OptionPicker("Modello", project.deviceModels, project.deviceModels.find { it.id == form.deviceModelId }, { it.name }, { form = form.copy(deviceModelId = it?.id) }, noneLabel = "Nessun modello")
        }
        SectionTitle("Rilievo")
        EnumPicker("Stato", ObservationStatus.entries, form.observationStatus, { it.toDisplayString() }, { form = form.copy(observationStatus = it) })
        FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false, minLines = 2)
    }
}

@Composable
private fun BatchDialog(vm: ProjectViewModel, project: Project, index: ProjectIndex, ids: Set<String>, onClose: () -> Unit) {
    var changes by remember { mutableStateOf(BatchDeviceChanges(category = DeviceCategory.NETWORK_SWITCH)) }
    val any = changes.updateCategory || changes.updateAreaId || changes.updateRackId || changes.updateObservationNotes
    EditScreen("Modifica ${ids.size} apparati", onClose, {
        onClose()
        vm.edit("Modificati ${ids.size} apparati.") { ProjectEdits.batchEditDevices(it, ids.toList(), changes) }
    }, confirmEnabled = any, confirmLabel = "Applica") {
        Text(ids.mapNotNull { index.device(it)?.technicalName }.joinToString(", "), style = MaterialTheme.typography.bodySmall)
        LabeledCheckbox(changes.updateCategory, { changes = changes.copy(updateCategory = it) }, "Cambia categoria")
        if (changes.updateCategory) EnumPicker("Categoria", DeviceCategory.entries, changes.category ?: DeviceCategory.CUSTOM, { it.toDisplayString() }, { changes = changes.copy(category = it) })
        LabeledCheckbox(changes.updateAreaId, { changes = changes.copy(updateAreaId = it) }, "Cambia area")
        if (changes.updateAreaId) OptionPicker("Area", index.areas, index.area(changes.areaId), { it.name }, { changes = changes.copy(areaId = it?.id) }, noneLabel = "Nessuna area")
        LabeledCheckbox(changes.updateRackId, { changes = changes.copy(updateRackId = it) }, "Cambia rack")
        if (changes.updateRackId) OptionPicker("Rack", project.racks, index.rack(changes.rackId), { it.name }, { changes = changes.copy(rackId = it?.id) }, noneLabel = "Fuori rack")
        LabeledCheckbox(changes.updateObservationNotes, { changes = changes.copy(updateObservationNotes = it) }, "Sostituisci le note di rilievo")
        if (changes.updateObservationNotes) FormField(changes.observationNotes.orEmpty(), { changes = changes.copy(observationNotes = it) }, "Note", singleLine = false)
    }
}
