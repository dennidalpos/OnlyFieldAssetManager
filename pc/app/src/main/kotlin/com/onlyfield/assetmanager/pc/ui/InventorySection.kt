package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.model.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit,
    onTrashItemCreated: (TrashItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<DeviceCategory?>(null) }
    var selectedAreaFilter by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showAddEditDeviceDialog by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<Device?>(null) }

    var showPortsDialog by remember { mutableStateOf(false) }
    var portTargetDevice by remember { mutableStateOf<Device?>(null) }

    var showReplaceDialog by remember { mutableStateOf(false) }
    var replaceTargetDevice by remember { mutableStateOf<Device?>(null) }

    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeSurvivingDevice by remember { mutableStateOf<Device?>(null) }

    // Multi-select for Batch Edit
    var selectedDeviceIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchEditDialog by remember { mutableStateOf(false) }

    val allAreas = remember(project) {
        project.businessUnits.flatMap { bu ->
            bu.areas + bu.sites.flatMap { it.areas }
        }.distinctBy { it.id }
    }

    val allDevicesWithBu = remember(project) {
        project.businessUnits.flatMap { bu ->
            bu.devices.map { dev -> Pair(bu, dev) }
        }
    }

    val filteredDevices = remember(allDevicesWithBu, searchQuery, selectedCategoryFilter, selectedAreaFilter) {
        allDevicesWithBu.filter { (_, dev) ->
            val matchQuery = searchQuery.isBlank() ||
                    dev.technicalName.contains(searchQuery, ignoreCase = true) ||
                    (dev.physicalLabel ?: "").contains(searchQuery, ignoreCase = true) ||
                    (dev.alias ?: "").contains(searchQuery, ignoreCase = true) ||
                    (dev.ipAddress ?: "").contains(searchQuery, ignoreCase = true) ||
                    (dev.macAddress ?: "").contains(searchQuery, ignoreCase = true)

            val matchCategory = selectedCategoryFilter == null || dev.category == selectedCategoryFilter
            val matchArea = selectedAreaFilter == null || dev.areaId == selectedAreaFilter

            matchQuery && matchCategory && matchArea
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filters & Search Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cerca apparato (Nome, IP, MAC, Label...)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Category Filter Dropdown
                var catDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { catDropdownExpanded = true }) {
                        Text(selectedCategoryFilter?.name ?: "Tutte le Categorie")
                    }
                    DropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tutte le Categorie") },
                            onClick = {
                                selectedCategoryFilter = null
                                catDropdownExpanded = false
                            }
                        )
                        DeviceCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryFilter = cat
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Area Filter Dropdown
                var areaDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { areaDropdownExpanded = true }) {
                        val areaName = allAreas.find { it.id == selectedAreaFilter }?.name ?: "Tutte le Aree"
                        Text(areaName)
                    }
                    DropdownMenu(
                        expanded = areaDropdownExpanded,
                        onDismissRequest = { areaDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tutte le Aree") },
                            onClick = {
                                selectedAreaFilter = null
                                areaDropdownExpanded = false
                            }
                        )
                        allAreas.forEach { area ->
                            DropdownMenuItem(
                                text = { Text(area.name) },
                                onClick = {
                                    selectedAreaFilter = area.id
                                    areaDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(onClick = {
                    editingDevice = null
                    showAddEditDeviceDialog = true
                }) {
                    Text("+ Nuovo Apparato")
                }

                if (selectedDeviceIds.isNotEmpty()) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        onClick = { showBatchEditDialog = true }
                    ) {
                        Text("Modifica in Blocco (${selectedDeviceIds.size})")
                    }
                }
            }
        }

        // Devices List Table
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Apparati Trovati (${filteredDevices.size} di ${allDevicesWithBu.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDeviceIds.isNotEmpty()) {
                        TextButton(onClick = { selectedDeviceIds = emptySet() }) {
                            Text("Deseleziona tutti")
                        }
                    }
                }

                if (filteredDevices.isEmpty()) {
                    EmptyStateCard(
                        message = "Nessun apparato corrisponde ai filtri selezionati.",
                        actionLabel = "+ Nuovo Apparato",
                        onAction = {
                            editingDevice = null
                            showAddEditDeviceDialog = true
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredDevices) { (bu, dev) ->
                            val isSelected = selectedDeviceIds.contains(dev.id)
                            val areaName = allAreas.find { it.id == dev.areaId }?.name ?: "Non assegnata"
                            val rackName = project.racks.find { it.id == dev.rackId }?.name ?: "Fuori Rack"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedDeviceIds = if (checked) selectedDeviceIds + dev.id else selectedDeviceIds - dev.id
                                        }
                                    )

                                    Column(modifier = Modifier.weight(1.5f).padding(horizontal = 8.dp)) {
                                        Text(
                                            text = dev.technicalName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Label: ${dev.physicalLabel ?: "—"} | Alias: ${dev.alias ?: "—"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "IP: ${dev.ipAddress ?: "—"} | MAC: ${dev.macAddress ?: "—"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1.2f).padding(horizontal = 8.dp)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = dev.category.toDisplayString(),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("BU: ${bu.name} | Area: $areaName", style = MaterialTheme.typography.bodySmall)
                                        Text("Rack: $rackName ${dev.positionU?.let { "(U$it, ${dev.heightU}U)" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                                    }

                                    Column(modifier = Modifier.weight(0.8f)) {
                                        Text("Porte: ${dev.ports.size}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                        Text("Stato: ${dev.observation?.status?.name ?: "N/D"}", style = MaterialTheme.typography.bodySmall)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = {
                                            portTargetDevice = dev
                                            showPortsDialog = true
                                        }) {
                                            Text("Porte (${dev.ports.size})", fontSize = 11.sp)
                                        }

                                        OutlinedButton(onClick = {
                                            editingDevice = dev
                                            showAddEditDeviceDialog = true
                                        }) {
                                            Text("Modifica", fontSize = 11.sp)
                                        }

                                        OutlinedButton(onClick = {
                                            replaceTargetDevice = dev
                                            showReplaceDialog = true
                                        }) {
                                            Text("Sostituisci", fontSize = 11.sp)
                                        }

                                        OutlinedButton(onClick = {
                                            mergeSurvivingDevice = dev
                                            showMergeDialog = true
                                        }) {
                                            Text("Unisci", fontSize = 11.sp)
                                        }

                                        Button(
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            onClick = {
                                                val (updated, trashItem) = com.onlyfield.assetmanager.pc.DesktopDomainLogic.deleteDeviceToTrash(project, dev.id)
                                                if (trashItem != null) onTrashItemCreated(trashItem)
                                                onProjectUpdated(updated, "Apparato '${dev.technicalName}' spostato nel cestino.")
                                            }
                                        ) {
                                            Text("Elimina", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Device Dialog
    if (showAddEditDeviceDialog) {
        val dev = editingDevice
        val buList = project.businessUnits
        var selectedBuId by remember { mutableStateOf(buList.find { bu -> bu.devices.any { it.id == dev?.id } }?.id ?: buList.firstOrNull()?.id ?: "") }
        var technicalName by remember { mutableStateOf(dev?.technicalName ?: "") }
        var physicalLabel by remember { mutableStateOf(dev?.physicalLabel ?: "") }
        var alias by remember { mutableStateOf(dev?.alias ?: "") }
        var ipAddress by remember { mutableStateOf(dev?.ipAddress ?: "") }
        var macAddress by remember { mutableStateOf(dev?.macAddress ?: "") }
        var category by remember { mutableStateOf(dev?.category ?: DeviceCategory.NETWORK_SWITCH) }
        var selectedAreaId by remember { mutableStateOf(dev?.areaId ?: allAreas.firstOrNull()?.id) }
        var selectedRackId by remember { mutableStateOf(dev?.rackId) }
        var positionUText by remember { mutableStateOf(dev?.positionU?.toString() ?: "") }
        var heightUText by remember { mutableStateOf(dev?.heightU?.toString() ?: "1") }
        var mountingType by remember { mutableStateOf(dev?.mountingType ?: MountingType.RACK_MOUNT) }
        var notesText by remember { mutableStateOf(dev?.observation?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showAddEditDeviceDialog = false },
            title = { Text(if (dev == null) "Nuovo Apparato" else "Modifica Apparato: ${dev.technicalName}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = technicalName,
                            onValueChange = { technicalName = it },
                            label = { Text("Nome Tecnico *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = physicalLabel,
                            onValueChange = { physicalLabel = it },
                            label = { Text("Etichetta Fisica") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            label = { Text("Alias") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("Indirizzo IP") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = macAddress,
                            onValueChange = { macAddress = it },
                            label = { Text("Indirizzo MAC") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Category Dropdown
                        var catExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { catExp = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Categoria: ${category.name}")
                            }
                            DropdownMenu(expanded = catExp, onDismissRequest = { catExp = false }) {
                                DeviceCategory.entries.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.name) }, onClick = { category = c; catExp = false })
                                }
                            }
                        }

                        // Area Dropdown
                        var areaExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { areaExp = true }, modifier = Modifier.fillMaxWidth()) {
                                val aName = allAreas.find { it.id == selectedAreaId }?.name ?: "Seleziona Area"
                                Text("Area: $aName")
                            }
                            DropdownMenu(expanded = areaExp, onDismissRequest = { areaExp = false }) {
                                allAreas.forEach { a ->
                                    DropdownMenuItem(text = { Text(a.name) }, onClick = { selectedAreaId = a.id; areaExp = false })
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Rack Dropdown
                        var rackExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { rackExp = true }, modifier = Modifier.fillMaxWidth()) {
                                val rName = project.racks.find { it.id == selectedRackId }?.name ?: "Fuori Rack"
                                Text("Rack: $rName")
                            }
                            DropdownMenu(expanded = rackExp, onDismissRequest = { rackExp = false }) {
                                DropdownMenuItem(text = { Text("Fuori Rack") }, onClick = { selectedRackId = null; rackExp = false })
                                project.racks.forEach { r ->
                                    DropdownMenuItem(text = { Text(r.name) }, onClick = { selectedRackId = r.id; rackExp = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = positionUText,
                            onValueChange = { positionUText = it },
                            label = { Text("Posizione U") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = heightUText,
                            onValueChange = { heightUText = it },
                            label = { Text("Altezza U") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Note di Osservazione") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = technicalName.isNotBlank(),
                    onClick = {
                        val posU = positionUText.toIntOrNull()
                        val hU = heightUText.toIntOrNull() ?: 1

                        val newDevice = Device(
                            id = dev?.id ?: UUID.randomUUID().toString(),
                            technicalName = technicalName.trim(),
                            physicalLabel = physicalLabel.ifBlank { null },
                            alias = alias.ifBlank { null },
                            ipAddress = ipAddress.ifBlank { null },
                            macAddress = macAddress.ifBlank { null },
                            category = category,
                            areaId = selectedAreaId,
                            rackId = selectedRackId,
                            positionU = posU,
                            heightU = hU,
                            mountingType = mountingType,
                            ports = dev?.ports ?: emptyList(),
                            observation = Observation("DesktopUI", System.currentTimeMillis(), notes = notesText.ifBlank { null })
                        )

                        val updatedProj = if (dev == null) {
                            com.onlyfield.assetmanager.pc.DesktopDomainLogic.addDevice(project, selectedBuId, newDevice)
                        } else {
                            com.onlyfield.assetmanager.pc.DesktopDomainLogic.updateDevice(project, newDevice)
                        }

                        showAddEditDeviceDialog = false
                        onProjectUpdated(updatedProj, if (dev == null) "Aggiunto apparato '${newDevice.technicalName}'." else "Aggiornato apparato '${newDevice.technicalName}'.")
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddEditDeviceDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Ports Dialog
    if (showPortsDialog) {
        val targetDev = portTargetDevice
        if (targetDev != null) {
            var newPortName by remember { mutableStateOf("") }
            var newPortLabel by remember { mutableStateOf("") }

            val currentDevState = project.businessUnits.flatMap { it.devices }.find { it.id == targetDev.id } ?: targetDev

            AlertDialog(
                onDismissRequest = { showPortsDialog = false },
                title = { Text("Gestione Porte: ${currentDevState.technicalName}") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPortName,
                                onValueChange = { newPortName = it },
                                label = { Text("Nome Porta (es. Gi1/0/1)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newPortLabel,
                                onValueChange = { newPortLabel = it },
                                label = { Text("Etichetta") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                enabled = newPortName.isNotBlank(),
                                onClick = {
                                    val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.addPortToDevice(
                                        project, currentDevState.id, newPortName.trim(), newPortLabel.ifBlank { null }
                                    )
                                    newPortName = ""
                                    newPortLabel = ""
                                    onProjectUpdated(updated, "Porta aggiunta a '${currentDevState.technicalName}'.")
                                }
                            ) {
                                Text("+ Aggiungi")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Porte Esistenti (${currentDevState.ports.size}):", fontWeight = FontWeight.Bold)

                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(currentDevState.ports) { port ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(port.name, fontWeight = FontWeight.Bold)
                                            port.label?.let { Text("Etichetta: $it", style = MaterialTheme.typography.bodySmall) }
                                        }
                                        Button(
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            onClick = {
                                                val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.deletePortFromDevice(
                                                    project, currentDevState.id, port.id
                                                )
                                                onProjectUpdated(updated, "Porta '${port.name}' rimossa.")
                                            }
                                        ) {
                                            Text("Elimina", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showPortsDialog = false }) {
                        Text("Chiudi")
                    }
                }
            )
        }
    }

    // Replace Device Dialog
    if (showReplaceDialog) {
        val targetDev = replaceTargetDevice
        if (targetDev != null) {
            var newTechName by remember { mutableStateOf("${targetDev.technicalName}-REPLACEMENT") }
            var newCat by remember { mutableStateOf(targetDev.category) }

            AlertDialog(
                onDismissRequest = { showReplaceDialog = false },
                title = { Text("Sostituisci Apparato: ${targetDev.technicalName}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("L'apparato attuale verrà spostato nel cestino. Un nuovo apparato verrà creato ereditando posizione e collocazione.")
                        OutlinedTextField(
                            value = newTechName,
                            onValueChange = { newTechName = it },
                            label = { Text("Nuovo Nome Tecnico *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = newTechName.isNotBlank(),
                        onClick = {
                            val (updated, trashItem) = com.onlyfield.assetmanager.pc.DesktopDomainLogic.replaceDevice(
                                project, targetDev.id, newTechName.trim(), newCat
                            )
                            showReplaceDialog = false
                            if (trashItem != null) onTrashItemCreated(trashItem)
                            onProjectUpdated(updated, "Sostituito '${targetDev.technicalName}' con '$newTechName'.")
                        }
                    ) {
                        Text("Esegui Sostituzione")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showReplaceDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }

    // Merge Devices Dialog
    if (showMergeDialog) {
        val survDev = mergeSurvivingDevice
        if (survDev != null) {
            val candidateDuplicates = allDevicesWithBu.map { it.second }.filter { it.id != survDev.id }
            var selectedDuplicateId by remember { mutableStateOf(candidateDuplicates.firstOrNull()?.id ?: "") }
            var useNameFromDup by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showMergeDialog = false },
                title = { Text("Unisci Duplicato in: ${survDev.technicalName}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Seleziona l'apparato duplicato da unire e rimuovere:")

                        var dupExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { dupExp = true }, modifier = Modifier.fillMaxWidth()) {
                                val dName = candidateDuplicates.find { it.id == selectedDuplicateId }?.technicalName ?: "Seleziona Duplicato"
                                Text(dName)
                            }
                            DropdownMenu(expanded = dupExp, onDismissRequest = { dupExp = false }) {
                                candidateDuplicates.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.technicalName) }, onClick = { selectedDuplicateId = c.id; dupExp = false })
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = useNameFromDup, onCheckedChange = { useNameFromDup = it })
                            Text("Usa il nome dell'apparato duplicato per il superstite")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = selectedDuplicateId.isNotBlank(),
                        onClick = {
                            val choices = MergeDataChoices(useTechnicalNameFromDuplicate = useNameFromDup, mergePorts = true)
                            val (updated, trashItem) = com.onlyfield.assetmanager.pc.DesktopDomainLogic.mergeDevices(
                                project, survDev.id, selectedDuplicateId, choices
                            )
                            showMergeDialog = false
                            if (trashItem != null) onTrashItemCreated(trashItem)
                            onProjectUpdated(updated, "Apparati uniti con successo in '${survDev.technicalName}'.")
                        }
                    ) {
                        Text("Esegui Fusione")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showMergeDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }

    // Batch Edit Dialog
    if (showBatchEditDialog) {
        var updateCat by remember { mutableStateOf(false) }
        var batchCat by remember { mutableStateOf(DeviceCategory.NETWORK_SWITCH) }
        var updateArea by remember { mutableStateOf(false) }
        var batchAreaId by remember { mutableStateOf<String?>(allAreas.firstOrNull()?.id) }
        var updateRack by remember { mutableStateOf(false) }
        var batchRackId by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showBatchEditDialog = false },
            title = { Text("Modifica in Blocco (${selectedDeviceIds.size} Apparati)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = updateCat, onCheckedChange = { updateCat = it })
                        Text("Aggiorna Categoria")
                    }
                    if (updateCat) {
                        var catExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { catExp = true }) { Text(batchCat.name) }
                            DropdownMenu(expanded = catExp, onDismissRequest = { catExp = false }) {
                                DeviceCategory.entries.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.name) }, onClick = { batchCat = c; catExp = false })
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = updateArea, onCheckedChange = { updateArea = it })
                        Text("Aggiorna Area")
                    }
                    if (updateArea) {
                        var areaExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { areaExp = true }) {
                                val aName = allAreas.find { it.id == batchAreaId }?.name ?: "Seleziona Area"
                                Text(aName)
                            }
                            DropdownMenu(expanded = areaExp, onDismissRequest = { areaExp = false }) {
                                allAreas.forEach { a ->
                                    DropdownMenuItem(text = { Text(a.name) }, onClick = { batchAreaId = a.id; areaExp = false })
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = updateRack, onCheckedChange = { updateRack = it })
                        Text("Aggiorna Rack")
                    }
                    if (updateRack) {
                        var rackExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { rackExp = true }) {
                                val rName = project.racks.find { it.id == batchRackId }?.name ?: "Fuori Rack"
                                Text(rName)
                            }
                            DropdownMenu(expanded = rackExp, onDismissRequest = { rackExp = false }) {
                                DropdownMenuItem(text = { Text("Fuori Rack") }, onClick = { batchRackId = null; rackExp = false })
                                project.racks.forEach { r ->
                                    DropdownMenuItem(text = { Text(r.name) }, onClick = { batchRackId = r.id; rackExp = false })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val changes = BatchDeviceChanges(
                        updateCategory = updateCat,
                        category = if (updateCat) batchCat else null,
                        updateAreaId = updateArea,
                        areaId = if (updateArea) batchAreaId else null,
                        updateRackId = updateRack,
                        rackId = if (updateRack) batchRackId else null
                    )

                    val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.batchEditDevices(project, selectedDeviceIds.toList(), changes)
                    showBatchEditDialog = false
                    val count = selectedDeviceIds.size
                    selectedDeviceIds = emptySet()
                    onProjectUpdated(updated, "Applicate modifiche in blocco a $count apparati.")
                }) {
                    Text("Applica Modifiche")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBatchEditDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}
