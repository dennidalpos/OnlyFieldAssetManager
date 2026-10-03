package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.DeviceForm
import com.onlyfield.assetmanager.core.scan.CodeLookup
import com.onlyfield.assetmanager.core.scan.CodeMatch

@Composable
fun InventorySection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit,
    onTrashItemCreated: (TrashItem) -> Unit
) {
    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current

    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<DeviceCategory?>(null) }
    var areaFilter by remember { mutableStateOf<Area?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    var editing by remember { mutableStateOf<Device?>(null) }
    var creating by remember { mutableStateOf(false) }
    var portsOf by remember { mutableStateOf<String?>(null) }
    var replaceTarget by remember { mutableStateOf<Device?>(null) }
    var mergeTarget by remember { mutableStateOf<Device?>(null) }
    var showBatch by remember { mutableStateOf(false) }

    val filtered = remember(index, query, categoryFilter, areaFilter) {
        index.devices.filter { d ->
            matchesQuery(query, d.technicalName, d.physicalLabel, d.alias, d.ipAddress, d.macAddress, d.serialNumber) &&
                (categoryFilter == null || d.category == categoryFilter) &&
                (areaFilter == null || d.areaId == areaFilter?.id)
        }
    }
    // Drop selections of devices that no longer exist.
    LaunchedEffect(index) { selectedIds = selectedIds.filter { index.device(it) != null }.toSet() }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Inventario apparati",
            subtitle = "${filtered.size} di ${index.devices.size} apparati",
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = "Cerca nome, etichetta, IP, MAC, seriale…",
            onSearchSubmit = {
                // Exact code (e.g. from a USB reader): open the device it identifies.
                when (val match = CodeLookup.find(index, query)) {
                    is CodeMatch.DeviceMatch -> editing = match.device
                    is CodeMatch.PortMatch -> portsOf = match.port.device.id
                    else -> Unit
                }
            }
        ) {
            Button(onClick = { creating = true }, enabled = project.businessUnits.isNotEmpty()) { Text("+ Nuovo apparato") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OptionPicker(
                label = "Categoria",
                options = DeviceCategory.entries,
                selected = categoryFilter,
                optionLabel = { it.toDisplayString() },
                onSelected = { categoryFilter = it },
                noneLabel = "Tutte le categorie",
                modifier = Modifier.width(240.dp)
            )
            OptionPicker(
                label = "Area",
                options = index.areas,
                selected = areaFilter,
                optionLabel = { it.name },
                onSelected = { areaFilter = it },
                noneLabel = "Tutte le aree",
                modifier = Modifier.width(240.dp)
            )
            Spacer(Modifier.weight(1f))
            if (selectedIds.isNotEmpty()) {
                Text("${selectedIds.size} selezionati", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { showBatch = true }) { Text("Modifica in blocco…") }
                TextButton(onClick = { selectedIds = emptySet() }) { Text("Deseleziona") }
            } else if (filtered.isNotEmpty()) {
                TextButton(onClick = { selectedIds = filtered.map { it.id }.toSet() }) { Text("Seleziona tutti") }
            }
        }

        when {
            project.businessUnits.isEmpty() -> EmptyState("Crea prima una business unit nella sezione Progetto.")
            index.devices.isEmpty() -> EmptyState("Nessun apparato nel progetto.", actionLabel = "+ Nuovo apparato", onAction = { creating = true })
            filtered.isEmpty() -> EmptyState("Nessun apparato corrisponde ai filtri.", actionLabel = "Azzera filtri", onAction = {
                query = ""; categoryFilter = null; areaFilter = null
            })
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.id }) { dev ->
                    val bu = index.businessUnitOf(dev.id)
                    val location = listOfNotNull(
                        bu?.name,
                        dev.areaId?.let { index.areaName(it, "area mancante") },
                        dev.rackId?.let { "Rack ${index.rackName(it)}" + (dev.positionU?.let { u -> " · U$u" } ?: "") }
                    ).joinToString(" › ")
                    val network = listOfNotNull(dev.ipAddress?.let { "IP $it" }, dev.macAddress?.let { "MAC $it" }, dev.physicalLabel?.let { "Etichetta $it" })
                        .joinToString(" · ")
                    var menuOpen by remember { mutableStateOf(false) }
                    ItemCard(
                        title = dev.technicalName + (dev.alias?.let { " ($it)" } ?: ""),
                        badge = dev.category.toDisplayString(),
                        details = listOf(location, network, "${dev.ports.size} porte · ${dev.observation?.status?.toDisplayString() ?: "Da verificare"}"),
                        leading = {
                            Checkbox(
                                checked = dev.id in selectedIds,
                                onCheckedChange = { selectedIds = if (it) selectedIds + dev.id else selectedIds - dev.id }
                            )
                        }
                    ) {
                        TextButton(onClick = { portsOf = dev.id }) { Text("Porte") }
                        EditButton { editing = dev }
                        Box {
                            TextButton(onClick = { menuOpen = true }) { Text("Altro ▾") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("Sostituisci con nuovo apparato…") }, onClick = { menuOpen = false; replaceTarget = dev })
                                DropdownMenuItem(
                                    text = { Text("Unisci un duplicato…") },
                                    enabled = index.devices.size > 1,
                                    onClick = { menuOpen = false; mergeTarget = dev }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Sposta nel cestino", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuOpen = false
                                        confirm(
                                            ConfirmRequest(
                                                title = "Spostare «${dev.technicalName}» nel cestino?",
                                                message = "I cavi collegati alle sue porte verranno scollegati. Potrai ripristinarlo dal Cestino finché l'applicazione resta aperta.",
                                                confirmLabel = "Sposta nel cestino"
                                            ) {
                                                val (updated, trashItem) = ProjectEdits.deleteDeviceToTrash(project, dev.id)
                                                trashItem?.let(onTrashItemCreated)
                                                onProjectUpdated(updated, "«${dev.technicalName}» spostato nel cestino.")
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        DeviceDialog(
            project = project,
            index = index,
            device = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { updated, message -> creating = false; editing = null; onProjectUpdated(updated, message) }
        )
    }

    portsOf?.let { id ->
        index.device(id)?.let { dev -> PortsDialog(project, index, dev, onProjectUpdated) { portsOf = null } }
            ?: run { portsOf = null }
    }

    replaceTarget?.let { dev ->
        ReplaceDialog(dev, onDismiss = { replaceTarget = null }) { name, category ->
            val (updated, trashItem) = ProjectEdits.replaceDevice(project, dev.id, name, category)
            replaceTarget = null
            trashItem?.let(onTrashItemCreated)
            onProjectUpdated(updated, "«${dev.technicalName}» sostituito da «$name».")
        }
    }

    mergeTarget?.let { survivor ->
        MergeDialog(index, survivor, onDismiss = { mergeTarget = null }) { duplicateId, choices ->
            val duplicateName = index.deviceName(duplicateId)
            val (updated, trashItem) = ProjectEdits.mergeDevices(project, survivor.id, duplicateId, choices)
            mergeTarget = null
            trashItem?.let(onTrashItemCreated)
            onProjectUpdated(updated, "«$duplicateName» unito in «${survivor.technicalName}».")
        }
    }

    if (showBatch) {
        BatchEditDialog(project, index, selectedIds, onDismiss = { showBatch = false }) { changes ->
            val count = selectedIds.size
            val updated = ProjectEdits.batchEditDevices(project, selectedIds.toList(), changes)
            showBatch = false
            selectedIds = emptySet()
            onProjectUpdated(updated, "Modifica applicata a $count apparati.")
        }
    }
}

@Composable
private fun DeviceDialog(
    project: Project,
    index: ProjectIndex,
    device: Device?,
    onDismiss: () -> Unit,
    onSave: (Project, String) -> Unit,
) {
    val initialBu = device?.let { index.businessUnitOf(it.id)?.id } ?: project.businessUnits.singleOrNull()?.id
    var form by remember(device) { mutableStateOf(DeviceForm.from(device, initialBu)) }
    val rack = index.rack(form.rackId)
    val errors = form.errors(rack?.heightU)
    val buAreas = project.businessUnits.find { it.id == form.businessUnitId }
        ?.let { bu -> bu.areas + bu.sites.flatMap { it.areas } } ?: index.areas

    EditPanel(
        title = if (device == null) "Nuovo apparato" else "Modifica «${device.technicalName}»",
        onDismiss = onDismiss,
        confirmEnabled = errors.isEmpty(),
        onConfirm = {
            val saved = form.toDevice(device, source = "Editor Windows")
            val updated = if (device == null) ProjectEdits.addDevice(project, requireNotNull(form.businessUnitId), saved)
            else ProjectEdits.updateDevice(project, saved)
            onSave(updated, if (device == null) "Apparato «${saved.technicalName}» creato." else "Apparato «${saved.technicalName}» aggiornato.")
        },
        width = 680.dp
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(form.technicalName, { form = form.copy(technicalName = it) }, "Nome tecnico *", Modifier.weight(1f), errors["technicalName"])
            EnumPicker("Categoria", DeviceCategory.entries, form.category, { it.toDisplayString() }, { form = form.copy(category = it) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(form.physicalLabel, { form = form.copy(physicalLabel = it) }, "Etichetta fisica", Modifier.weight(1f))
            FormField(form.alias, { form = form.copy(alias = it) }, "Alias", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(form.ipAddress, { form = form.copy(ipAddress = it) }, "Indirizzo IP", Modifier.weight(1f), errors["ipAddress"])
            FormField(form.macAddress, { form = form.copy(macAddress = it) }, "Indirizzo MAC", Modifier.weight(1f), errors["macAddress"])
        }
        FormField(form.serialNumber, { form = form.copy(serialNumber = it) }, "Numero di serie", hint = "Con il lettore USB: clic nel campo e leggi il codice")

        Text("Posizione", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionPicker(
                label = "Business unit *",
                options = project.businessUnits,
                selected = project.businessUnits.find { it.id == form.businessUnitId },
                optionLabel = { it.name },
                onSelected = { form = form.copy(businessUnitId = it?.id, areaId = null) },
                enabled = device == null,
                supportingText = if (device != null) "Non modificabile dopo la creazione" else errors["businessUnitId"],
                isError = errors.containsKey("businessUnitId"),
                modifier = Modifier.weight(1f)
            )
            OptionPicker(
                label = "Area",
                options = buAreas,
                selected = index.area(form.areaId),
                optionLabel = { it.name },
                optionDetail = { it.floor?.let { f -> "Piano $f" } },
                onSelected = { form = form.copy(areaId = it?.id) },
                noneLabel = "Nessuna area",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionPicker(
                label = "Rack",
                options = project.racks,
                selected = rack,
                optionLabel = { it.name },
                optionDetail = { "${it.heightU}U · ${index.areaName(it.areaId, "nessuna area")}" },
                onSelected = {
                    form = form.copy(
                        rackId = it?.id,
                        mountingType = if (it == null) MountingType.OUT_OF_RACK else if (form.mountingType == MountingType.OUT_OF_RACK) MountingType.RACK_MOUNT else form.mountingType
                    )
                },
                noneLabel = "Fuori rack",
                modifier = Modifier.weight(1.4f)
            )
            FormField(
                form.positionU, { form = form.copy(positionU = it) }, "Posizione U", Modifier.weight(0.8f), errors["positionU"],
                hint = rack?.let { "1–${it.heightU}" }
            )
            FormField(form.heightU, { form = form.copy(heightU = it) }, "Altezza (U) *", Modifier.weight(0.8f), errors["heightU"])
        }
        if (form.rackId != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnumPicker("Lato rack", RackSide.entries, form.rackSide, { it.toDisplayString() }, { form = form.copy(rackSide = it) }, Modifier.weight(1f))
                EnumPicker("Montaggio", MountingType.entries, form.mountingType, { it.toDisplayString() }, { form = form.copy(mountingType = it) }, Modifier.weight(1f))
            }
        }
        if (project.deviceModels.isNotEmpty()) {
            OptionPicker(
                label = "Modello",
                options = project.deviceModels,
                selected = project.deviceModels.find { it.id == form.deviceModelId },
                optionLabel = { it.name },
                optionDetail = { listOfNotNull(it.brand, it.modelNumber).joinToString(" ") },
                onSelected = { form = form.copy(deviceModelId = it?.id) },
                noneLabel = "Nessun modello"
            )
        }

        Text("Rilievo", fontWeight = FontWeight.SemiBold)
        EnumPicker("Stato del rilievo", ObservationStatus.entries, form.observationStatus, { it.toDisplayString() }, { form = form.copy(observationStatus = it) })
        FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false, minLines = 2)
    }
}

@Composable
private fun PortsDialog(
    project: Project,
    index: ProjectIndex,
    device: Device,
    onProjectUpdated: (Project, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val duplicate = device.ports.any { it.name.equals(name.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(620.dp),
        title = { Text("Porte di «${device.technicalName}»") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    FormField(name, { name = it }, "Nome porta", Modifier.weight(1f), error = if (duplicate) "Porta già presente" else null, hint = "Es. Gi1/0/1")
                    FormField(label, { label = it }, "Etichetta", Modifier.weight(1f))
                    Button(
                        enabled = name.isNotBlank() && !duplicate,
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            onProjectUpdated(ProjectEdits.addPortToDevice(project, device.id, name.trim(), label.trim().ifBlank { null }), "Porta «${name.trim()}» aggiunta.")
                            name = ""; label = ""
                        }
                    ) { Text("Aggiungi") }
                }
                if (device.ports.isEmpty()) {
                    EmptyState("Nessuna porta. Aggiungile qui o applica un modello con template porte.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(device.ports, key = { it.id }) { port ->
                            val cable = project.cables.find { it.portAId == port.id || it.portBId == port.id }
                            val peer = cable?.let { if (it.portAId == port.id) it.portBId else it.portAId }
                            ItemCard(
                                title = port.name,
                                details = listOf(
                                    port.label?.let { "Etichetta $it" }.orEmpty(),
                                    if (cable != null) "Collegata a ${index.portLabel(peer, "estremità libera")}" else "Libera"
                                )
                            ) {
                                DeleteButton(port.name, onDelete = {
                                    onProjectUpdated(ProjectEdits.deletePortFromDevice(project, device.id, port.id), "Porta «${port.name}» eliminata.")
                                }, message = if (cable != null) "La porta è collegata a un cavo, che resterà con un'estremità libera." else null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Fine") } }
    )
}

@Composable
private fun ReplaceDialog(device: Device, onDismiss: () -> Unit, onConfirm: (String, DeviceCategory) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(device.category) }
    EditPanel(
        title = "Sostituisci «${device.technicalName}»",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(name.trim(), category) },
        confirmEnabled = name.isNotBlank(),
        confirmLabel = "Sostituisci",
        width = 520.dp
    ) {
        Text("Il nuovo apparato eredita area, rack e posizione U dell'attuale, che viene spostato nel cestino. I cavi collegati alle vecchie porte restano con un'estremità libera.")
        FormField(name, { name = it }, "Nome del nuovo apparato *")
        EnumPicker("Categoria", DeviceCategory.entries, category, { it.toDisplayString() }, { category = it })
    }
}

@Composable
private fun MergeDialog(index: ProjectIndex, survivor: Device, onDismiss: () -> Unit, onConfirm: (String, MergeDataChoices) -> Unit) {
    val candidates = index.devices.filter { it.id != survivor.id }
    var duplicate by remember { mutableStateOf<Device?>(null) }
    var choices by remember { mutableStateOf(MergeDataChoices()) }

    EditPanel(
        title = "Unisci un duplicato in «${survivor.technicalName}»",
        onDismiss = onDismiss,
        onConfirm = { duplicate?.let { onConfirm(it.id, choices) } },
        confirmEnabled = duplicate != null,
        confirmLabel = "Unisci",
        width = 600.dp
    ) {
        Text("L'apparato duplicato viene spostato nel cestino dopo aver trasferito i dati scelti.")
        OptionPicker(
            label = "Apparato duplicato *",
            options = candidates,
            selected = duplicate,
            optionLabel = { it.technicalName },
            optionDetail = { listOfNotNull(it.ipAddress, index.areaName(it.areaId, "")).joinToString(" · ") },
            onSelected = { duplicate = it }
        )
        duplicate?.let { dup ->
            Text("Usa dal duplicato:", fontWeight = FontWeight.SemiBold)
            LabeledCheckbox(choices.useAliasFromDuplicate, { choices = choices.copy(useAliasFromDuplicate = it) }, "Alias (${dup.alias ?: "vuoto"})")
            LabeledCheckbox(choices.useTechnicalNameFromDuplicate, { choices = choices.copy(useTechnicalNameFromDuplicate = it) }, "Nome tecnico («${dup.technicalName}»)")
            LabeledCheckbox(choices.usePhysicalLabelFromDuplicate, { choices = choices.copy(usePhysicalLabelFromDuplicate = it) }, "Etichetta fisica (${dup.physicalLabel ?: "vuota"})")
            LabeledCheckbox(choices.useIpFromDuplicate, { choices = choices.copy(useIpFromDuplicate = it) }, "Indirizzo IP (${dup.ipAddress ?: "vuoto"})")
            LabeledCheckbox(choices.useMacFromDuplicate, { choices = choices.copy(useMacFromDuplicate = it) }, "Indirizzo MAC (${dup.macAddress ?: "vuoto"})")
            LabeledCheckbox(choices.useLocationFromDuplicate, { choices = choices.copy(useLocationFromDuplicate = it) }, "Posizione (sede e area)")
            Text("Trasferisci:", fontWeight = FontWeight.SemiBold)
            LabeledCheckbox(choices.mergePorts, { choices = choices.copy(mergePorts = it) }, "Porte (${dup.ports.size})")
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
    var changes by remember { mutableStateOf(BatchDeviceChanges(category = DeviceCategory.NETWORK_SWITCH, mountingType = MountingType.RACK_MOUNT)) }
    val any = changes.updateCategory || changes.updateAreaId || changes.updateRackId || changes.updateMountingType || changes.updateObservationNotes

    EditPanel(
        title = "Modifica in blocco",
        onDismiss = onDismiss,
        onConfirm = { onApply(changes) },
        confirmEnabled = any,
        confirmLabel = "Applica a ${selectedIds.size} apparati",
        width = 600.dp
    ) {
        Text(
            selectedIds.mapNotNull { index.device(it)?.technicalName }.sorted().joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3
        )
        Text("Spunta i campi da sovrascrivere:", fontWeight = FontWeight.SemiBold)
        LabeledCheckbox(changes.updateCategory, { changes = changes.copy(updateCategory = it) }, "Categoria")
        if (changes.updateCategory) EnumPicker("Categoria", DeviceCategory.entries, changes.category ?: DeviceCategory.CUSTOM, { it.toDisplayString() }, { changes = changes.copy(category = it) })
        LabeledCheckbox(changes.updateAreaId, { changes = changes.copy(updateAreaId = it) }, "Area")
        if (changes.updateAreaId) OptionPicker("Area", index.areas, index.area(changes.areaId), { it.name }, { changes = changes.copy(areaId = it?.id) }, noneLabel = "Nessuna area")
        LabeledCheckbox(changes.updateRackId, { changes = changes.copy(updateRackId = it) }, "Rack")
        if (changes.updateRackId) OptionPicker("Rack", project.racks, index.rack(changes.rackId), { it.name }, { changes = changes.copy(rackId = it?.id) }, noneLabel = "Fuori rack")
        LabeledCheckbox(changes.updateMountingType, { changes = changes.copy(updateMountingType = it) }, "Tipo di montaggio")
        if (changes.updateMountingType) EnumPicker("Montaggio", MountingType.entries, changes.mountingType ?: MountingType.RACK_MOUNT, { it.toDisplayString() }, { changes = changes.copy(mountingType = it) })
        LabeledCheckbox(changes.updateObservationNotes, { changes = changes.copy(updateObservationNotes = it) }, "Note di rilievo")
        if (changes.updateObservationNotes) FormField(changes.observationNotes.orEmpty(), { changes = changes.copy(observationNotes = it) }, "Note", singleLine = false)
    }
}
