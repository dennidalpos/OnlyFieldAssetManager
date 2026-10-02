package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun RackSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit,
    onTrashItemCreated: (TrashItem) -> Unit
) {
    var selectedRackId by remember { mutableStateOf(project.racks.firstOrNull()?.id) }
    var rackSideView by remember { mutableStateOf(RackSide.FRONT) }

    // Dialogs
    var showAddEditRackDialog by remember { mutableStateOf(false) }
    var editingRack by remember { mutableStateOf<Rack?>(null) }

    val selectedRack = remember(project, selectedRackId) {
        project.racks.find { it.id == selectedRackId } ?: project.racks.firstOrNull()
    }

    val allDevicesInProject = remember(project) {
        project.businessUnits.flatMap { it.devices }
    }

    val devicesInSelectedRack = remember(allDevicesInProject, selectedRack) {
        if (selectedRack == null) emptyList()
        else allDevicesInProject.filter { it.rackId == selectedRack.id }
    }

    val unassignedDevices = remember(allDevicesInProject) {
        allDevicesInProject.filter { it.rackId == null }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Racks List Panel
            Card(
                modifier = Modifier.width(320.dp).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Armadi Rack (${project.racks.size})", fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            editingRack = null
                            showAddEditRackDialog = true
                        }) {
                            Text("+ Nuovo")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (project.racks.isEmpty()) {
                        EmptyStateCard(
                            message = "Nessun armadio rack presente.",
                            actionLabel = "+ Nuovo",
                            onAction = {
                                editingRack = null
                                showAddEditRackDialog = true
                            }
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(project.racks) { rack ->
                                val isSelected = rack.id == selectedRack?.id
                                val devCount = allDevicesInProject.count { it.rackId == rack.id }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedRackId = rack.id },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(rack.name, fontWeight = FontWeight.Bold)
                                        Text("Altezza: ${rack.heightU}U | Apparati: $devCount", style = MaterialTheme.typography.bodySmall)
                                        Text("Direzione: ${rack.numberingDirection}", style = MaterialTheme.typography.labelSmall)

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = {
                                                editingRack = rack
                                                showAddEditRackDialog = true
                                            }) {
                                                Text("Modifica", fontSize = 11.sp)
                                            }
                                            TextButton(onClick = {
                                                val (updated, trashItem) = com.onlyfield.assetmanager.pc.DesktopDomainLogic.deleteRackToTrash(project, rack.id)
                                                if (trashItem != null) onTrashItemCreated(trashItem)
                                                onProjectUpdated(updated, "Rack '${rack.name}' spostato nel cestino.")
                                            }) {
                                                Text("Elimina", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Visual Rack Elevation Layout Preview
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (selectedRack == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Seleziona o crea un armadio rack per visualizzare il layout U.")
                    }
                } else {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        // Rack Header Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Vista Prospetto: ${selectedRack.name} (${selectedRack.heightU}U)",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Occupazione: ${devicesInSelectedRack.sumOf { it.heightU }}U su ${selectedRack.heightU}U")
                            }

                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = rackSideView == RackSide.FRONT,
                                    onClick = { rackSideView = RackSide.FRONT },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Text("Fronte (FRONT)")
                                }
                                SegmentedButton(
                                    selected = rackSideView == RackSide.REAR,
                                    onClick = { rackSideView = RackSide.REAR },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text("Retro (REAR)")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual Slots Column
                        val heightU = selectedRack.heightU
                        val slots = remember(heightU, selectedRack.numberingDirection) {
                            if (selectedRack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) {
                                (heightU downTo 1).toList()
                            } else {
                                (1..heightU).toList()
                            }
                        }

                        Row(modifier = Modifier.fillMaxSize()) {
                            // 2D Elevation View
                            Card(
                                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                            ) {
                                LazyColumn(
                                    modifier = Modifier.padding(12.dp).fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(slots) { uNum ->
                                        // Find device starting or occupying uNum
                                        val mountedDev = devicesInSelectedRack.find { dev ->
                                            val pos = dev.positionU ?: return@find false
                                            val sideMatch = dev.rackSide == RackSide.BOTH || dev.rackSide == rackSideView
                                            sideMatch && uNum >= pos && uNum < (pos + dev.heightU)
                                        }

                                        if (mountedDev != null) {
                                            val isStartU = mountedDev.positionU == uNum
                                            Surface(
                                                modifier = Modifier.fillMaxWidth().height(28.dp),
                                                color = getCategoryColor(mountedDev.category),
                                                shape = RoundedCornerShape(2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "U$uNum",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = if (isStartU) "${mountedDev.technicalName} [${mountedDev.category.name}]" else "↑ (${mountedDev.technicalName})",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        } else {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth().height(24.dp),
                                                color = Color(0xFF2B2B2B),
                                                shape = RoundedCornerShape(2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("U$uNum", fontSize = 10.sp, color = Color.Gray)
                                                    Text("[ Libero ]", fontSize = 10.sp, color = Color.DarkGray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Rack Devices Management Side Panel
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                Text("Apparati In Questo Rack (${devicesInSelectedRack.size})", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(devicesInSelectedRack) { dev ->
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
                                                    Text(dev.technicalName, fontWeight = FontWeight.Bold)
                                                    Text("Posizione: U${dev.positionU ?: "N/D"} (${dev.heightU}U)", style = MaterialTheme.typography.bodySmall)
                                                }
                                                OutlinedButton(onClick = {
                                                    val unassignDev = dev.copy(rackId = null, positionU = null)
                                                    val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.updateDevice(project, unassignDev)
                                                    onProjectUpdated(updated, "Dislocato '${dev.technicalName}' dal rack.")
                                                }) {
                                                    Text("Rimuovi", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Assegna Apparato Non Collocato:", fontWeight = FontWeight.Bold)

                                if (unassignedDevices.isEmpty()) {
                                    Text("Tutti gli apparati sono già collocati.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    var assignExp by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(onClick = { assignExp = true }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Seleziona apparato da inserire...")
                                        }
                                        DropdownMenu(expanded = assignExp, onDismissRequest = { assignExp = false }) {
                                            unassignedDevices.forEach { dev ->
                                                DropdownMenuItem(
                                                    text = { Text("${dev.technicalName} (${dev.heightU}U)") },
                                                    onClick = {
                                                        assignExp = false
                                                        val assigned = dev.copy(rackId = selectedRack.id, positionU = 1)
                                                        val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.updateDevice(project, assigned)
                                                        onProjectUpdated(updated, "Apparato '${dev.technicalName}' collocato in '${selectedRack.name}'.")
                                                    }
                                                )
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
    }

    // Add/Edit Rack Dialog
    if (showAddEditRackDialog) {
        val r = editingRack
        var name by remember { mutableStateOf(r?.name ?: "") }
        var heightUText by remember { mutableStateOf(r?.heightU?.toString() ?: "42") }
        var numDir by remember { mutableStateOf(r?.numberingDirection ?: NumberingDirection.BOTTOM_TO_TOP) }
        var notes by remember { mutableStateOf(r?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showAddEditRackDialog = false },
            title = { Text(if (r == null) "Nuovo Armadio Rack" else "Modifica Rack: ${r.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Armadio Rack *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = heightUText,
                        onValueChange = { heightUText = it },
                        label = { Text("Altezza totale (Unità U) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Numerazione U:")
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(
                            selected = numDir == NumberingDirection.BOTTOM_TO_TOP,
                            onClick = { numDir = NumberingDirection.BOTTOM_TO_TOP }
                        )
                        Text("Dal Basso in Alto")
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(
                            selected = numDir == NumberingDirection.TOP_TO_BOTTOM,
                            onClick = { numDir = NumberingDirection.TOP_TO_BOTTOM }
                        )
                        Text("Dall'Alto in Basso")
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val hU = heightUText.toIntOrNull() ?: 42
                        val newRack = Rack(
                            id = r?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            heightU = hU,
                            numberingDirection = numDir,
                            notes = notes.ifBlank { null }
                        )

                        val updated = if (r == null) {
                            com.onlyfield.assetmanager.pc.DesktopDomainLogic.addRack(project, newRack)
                        } else {
                            com.onlyfield.assetmanager.pc.DesktopDomainLogic.updateRack(project, newRack)
                        }

                        showAddEditRackDialog = false
                        onProjectUpdated(updated, "Armadio rack '${newRack.name}' salvato.")
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddEditRackDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

private fun getCategoryColor(category: DeviceCategory): Color {
    return when (category) {
        DeviceCategory.NETWORK_SWITCH -> Color(0xFF1565C0)
        DeviceCategory.PATCH_PANEL -> Color(0xFF2E7D32)
        DeviceCategory.UPS_PDU -> Color(0xFFD84315)
        DeviceCategory.SERVER_STORAGE -> Color(0xFF6A1B9A)
        DeviceCategory.CAMERA_NVR -> Color(0xFF00838F)
        DeviceCategory.SHELF -> Color(0xFF424242)
        DeviceCategory.BLANK_PANEL -> Color(0xFF37474F)
        DeviceCategory.CUSTOM -> Color(0xFF5D4037)
    }
}
