package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.model.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceModelsSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showAddEditModelDialog by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<DeviceModel?>(null) }

    var showApplyModelDialog by remember { mutableStateOf(false) }
    var applyTargetModel by remember { mutableStateOf<DeviceModel?>(null) }

    val allDevices = remember(project) {
        project.businessUnits.flatMap { it.devices }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Catalogo Modelli Apparati (${project.deviceModels.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(onClick = {
                editingModel = null
                showAddEditModelDialog = true
            }) {
                Text("+ Nuovo Modello")
            }
        }

        if (project.deviceModels.isEmpty()) {
            EmptyStateCard(
                message = "Nessun modello di apparato definito nel progetto.",
                actionLabel = "+ Nuovo Modello",
                onAction = {
                    editingModel = null
                    showAddEditModelDialog = true
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.deviceModels) { model ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Marca: ${model.brand ?: "—"} | Codice Modello: ${model.modelNumber ?: "—"}")
                                Text("Categoria: ${model.category.name} | Altezza default: ${model.defaultHeightU}U")

                                if (model.portTemplates.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Template Porte:", style = MaterialTheme.typography.labelMedium)
                                    model.portTemplates.forEach { tmpl ->
                                        Text("• Prefix: '${tmpl.namePrefix}' | N. Porte: ${tmpl.portCount} (Da ${tmpl.startNumber})", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = {
                                    applyTargetModel = model
                                    showApplyModelDialog = true
                                }) {
                                    Text("Applica ad Apparato", fontSize = 11.sp)
                                }

                                OutlinedButton(onClick = {
                                    editingModel = model
                                    showAddEditModelDialog = true
                                }) {
                                    Text("Modifica", fontSize = 11.sp)
                                }

                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.deleteDeviceModel(project, model.id)
                                        onProjectUpdated(updated, "Modello '${model.name}' rimosso.")
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

    // Add/Edit Model Dialog
    if (showAddEditModelDialog) {
        val m = editingModel
        var name by remember { mutableStateOf(m?.name ?: "") }
        var brand by remember { mutableStateOf(m?.brand ?: "") }
        var modelNumber by remember { mutableStateOf(m?.modelNumber ?: "") }
        var category by remember { mutableStateOf(m?.category ?: DeviceCategory.NETWORK_SWITCH) }
        var defaultHeightUText by remember { mutableStateOf(m?.defaultHeightU?.toString() ?: "1") }

        // Port Template fields
        var portTemplates by remember { mutableStateOf(m?.portTemplates ?: emptyList()) }
        var prefixInput by remember { mutableStateOf("Gi1/0/") }
        var countInput by remember { mutableStateOf("24") }

        AlertDialog(
            onDismissRequest = { showAddEditModelDialog = false },
            title = { Text(if (m == null) "Nuovo Modello Apparato" else "Modifica Modello: ${m.name}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Modello *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Marca / Produttore") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = modelNumber,
                            onValueChange = { modelNumber = it },
                            label = { Text("Codice Modello") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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

                        OutlinedTextField(
                            value = defaultHeightUText,
                            onValueChange = { defaultHeightUText = it },
                            label = { Text("Altezza U Default") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true
                        )
                    }

                    Divider()
                    Text("Template Generazione Porte:", fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = prefixInput,
                            onValueChange = { prefixInput = it },
                            label = { Text("Prefisso (es. Gi1/0/)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = countInput,
                            onValueChange = { countInput = it },
                            label = { Text("N. Porte") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true
                        )
                        Button(
                            enabled = prefixInput.isNotBlank() && countInput.toIntOrNull() != null,
                            onClick = {
                                val cnt = countInput.toIntOrNull() ?: 1
                                portTemplates = portTemplates + PortTemplate(namePrefix = prefixInput.trim(), portCount = cnt)
                            }
                        ) {
                            Text("+ Aggiungi")
                        }
                    }

                    portTemplates.forEachIndexed { idx, tmpl ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ${tmpl.portCount} porte con prefisso '${tmpl.namePrefix}'")
                                TextButton(onClick = { portTemplates = portTemplates.filterIndexed { i, _ -> i != idx } }) {
                                    Text("Rimuovi", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val hU = defaultHeightUText.toIntOrNull() ?: 1
                        val newModel = DeviceModel(
                            id = m?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            brand = brand.ifBlank { null },
                            modelNumber = modelNumber.ifBlank { null },
                            category = category,
                            defaultHeightU = hU,
                            portTemplates = portTemplates
                        )

                        val updated = if (m == null) {
                            com.onlyfield.assetmanager.pc.DesktopDomainLogic.addDeviceModel(project, newModel)
                        } else {
                            com.onlyfield.assetmanager.pc.DesktopDomainLogic.updateDeviceModel(project, newModel)
                        }

                        showAddEditModelDialog = false
                        onProjectUpdated(updated, "Modello '${newModel.name}' salvato.")
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddEditModelDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Apply Model to Device Dialog
    if (showApplyModelDialog) {
        val tmplModel = applyTargetModel
        if (tmplModel != null) {
            var selectedDeviceId by remember { mutableStateOf(allDevices.firstOrNull()?.id ?: "") }

            AlertDialog(
                onDismissRequest = { showApplyModelDialog = false },
                title = { Text("Applica Modello: ${tmplModel.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Seleziona l'apparato a cui applicare categoria, altezza U e porte dal modello:")

                        var devExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { devExp = true }, modifier = Modifier.fillMaxWidth()) {
                                val dName = allDevices.find { it.id == selectedDeviceId }?.technicalName ?: "Seleziona Apparato"
                                Text(dName)
                            }
                            DropdownMenu(expanded = devExp, onDismissRequest = { devExp = false }) {
                                allDevices.forEach { dev ->
                                    DropdownMenuItem(text = { Text("${dev.technicalName} (${dev.category.name})") }, onClick = { selectedDeviceId = dev.id; devExp = false })
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = selectedDeviceId.isNotBlank(),
                        onClick = {
                            val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.applyModelToDevice(project, selectedDeviceId, tmplModel.id)
                            showApplyModelDialog = false
                            val targetDev = allDevices.find { it.id == selectedDeviceId }
                            onProjectUpdated(updated, "Modello '${tmplModel.name}' applicato ad apparato '${targetDev?.technicalName}'.")
                        }
                    ) {
                        Text("Applica Modello")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showApplyModelDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}
