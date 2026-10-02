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
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.DesktopDomainLogic
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CablingSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var subTab by remember { mutableStateOf(0) } // 0 = Cavi Fisici, 1 = Percorsi Condivisi, 2 = Permutazioni / Pannelli

    // All available ports across project devices for dropdowns
    val allPorts = remember(project) {
        project.businessUnits.flatMap { bu ->
            bu.devices.flatMap { dev ->
                dev.ports.map { port ->
                    Triple(port, dev, bu)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-tabs
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }) {
                Text("🔌 Cavi Fisici (${project.cables.size})", modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 1, onClick = { subTab = 1 }) {
                Text("🛣️ Percorsi Condivisi (${project.sharedPathSegments.size})", modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 2, onClick = { subTab = 2 }) {
                Text("🔀 Mapping Pannelli (${project.panelMappings.size})", modifier = Modifier.padding(10.dp))
            }
        }

        when (subTab) {
            0 -> CablesSubSection(project, allPorts, onProjectUpdated)
            1 -> SharedPathsSubSection(project, onProjectUpdated)
            2 -> PanelMappingsSubSection(project, allPorts, onProjectUpdated)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CablesSubSection(
    project: Project,
    allPorts: List<Triple<Port, Device, BusinessUnit>>,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingCable by remember { mutableStateOf<Cable?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Elenco Cavi di Collegamento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                editingCable = null
                showDialog = true
            }) {
                Text("+ Nuovo Cavo")
            }
        }

        if (project.cables.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun cavo censito nel progetto.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.cables) { cable ->
                    val portAInfo = allPorts.find { it.first.id == cable.portAId }
                    val portBInfo = allPorts.find { it.first.id == cable.portBId }

                    val textA = if (portAInfo != null) "${portAInfo.second.technicalName}:${portAInfo.first.name}" else "— Non collegato —"
                    val textB = if (portBInfo != null) "${portBInfo.second.technicalName}:${portBInfo.first.name}" else "— Non collegato —"

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
                                Text(
                                    text = "Cavo: ${(cable.codeOrLabel ?: "Senza Codice").ifBlank { "Senza Codice" }} [ID: ${cable.id.take(8)}]",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text("Da: $textA  ➡️  A: $textB")
                                Text("Mezzo: ${cable.medium.name} | Connettori: ${cable.connectorA ?: "—"} / ${cable.connectorB ?: "—"} | Lunghezza: ${cable.lengthValue ?: "—"} ${cable.lengthUnit ?: "m"}")
                                if (!cable.color.isNullOrBlank()) Text("Colore: ${cable.color}")
                                if (!cable.notes.isNullOrBlank()) Text("Note: ${cable.notes}", style = MaterialTheme.typography.bodySmall)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    editingCable = cable
                                    showDialog = true
                                }) {
                                    Text("Modifica", fontSize = 11.sp)
                                }

                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        val updated = DesktopDomainLogic.deleteCable(project, cable.id)
                                        onProjectUpdated(updated, "Cavo rimosso.")
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

    if (showDialog) {
        val c = editingCable
        var codeOrLabel by remember { mutableStateOf(c?.codeOrLabel ?: "") }
        var selectedPortAId by remember { mutableStateOf(c?.portAId ?: "") }
        var selectedPortBId by remember { mutableStateOf(c?.portBId ?: "") }
        var medium by remember { mutableStateOf(c?.medium ?: CableMedium.ETHERNET_COPPER) }
        var connectorA by remember { mutableStateOf(c?.connectorA ?: "") }
        var connectorB by remember { mutableStateOf(c?.connectorB ?: "") }
        var color by remember { mutableStateOf(c?.color ?: "") }
        var lengthValueText by remember { mutableStateOf(c?.lengthValue?.toString() ?: "") }
        var notes by remember { mutableStateOf(c?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (c == null) "Nuovo Cavo" else "Modifica Cavo") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = codeOrLabel,
                        onValueChange = { codeOrLabel = it },
                        label = { Text("Codice / Etichetta Cavo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Port A Dropdown / Selector
                    OutlinedTextField(
                        value = selectedPortAId,
                        onValueChange = { selectedPortAId = it },
                        label = { Text("ID Porta A (opzionale)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("Puoi incollare l'ID porta o inserirlo dall'inventario") }
                    )

                    // Port B Dropdown / Selector
                    OutlinedTextField(
                        value = selectedPortBId,
                        onValueChange = { selectedPortBId = it },
                        label = { Text("ID Porta B (opzionale)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Mezzo Trasmissivo:")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CableMedium.entries.take(4).forEach { m ->
                            FilterChip(
                                selected = medium == m,
                                onClick = { medium = m },
                                label = { Text(m.name, fontSize = 10.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = connectorA,
                            onValueChange = { connectorA = it },
                            label = { Text("Connettore A") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = connectorB,
                            onValueChange = { connectorB = it },
                            label = { Text("Connettore B") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text("Colore Cavo") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lengthValueText,
                            onValueChange = { lengthValueText = it },
                            label = { Text("Lunghezza (m)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Note Cavo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val len = lengthValueText.toDoubleOrNull()
                    val newCable = Cable(
                        id = c?.id ?: UUID.randomUUID().toString(),
                        codeOrLabel = codeOrLabel.ifBlank { null },
                        portAId = selectedPortAId.ifBlank { null },
                        portBId = selectedPortBId.ifBlank { null },
                        medium = medium,
                        connectorA = connectorA.ifBlank { null },
                        connectorB = connectorB.ifBlank { null },
                        color = color.ifBlank { null },
                        lengthValue = len,
                        notes = notes.ifBlank { null }
                    )

                    val updated = if (c == null) {
                        DesktopDomainLogic.addCable(project, newCable)
                    } else {
                        DesktopDomainLogic.updateCable(project, newCable)
                    }
                    onProjectUpdated(updated, "Cavo salvato.")
                    showDialog = false
                }) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annulla") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharedPathsSubSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingSegment by remember { mutableStateOf<SharedPathSegment?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Percorsi Condivisi e Canalizzazioni", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                editingSegment = null
                showDialog = true
            }) {
                Text("+ Nuovo Percorso")
            }
        }

        if (project.sharedPathSegments.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessuna tratta / percorso condiviso definito.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.sharedPathSegments) { seg ->
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
                                Text(seg.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Descrizione: ${(seg.description ?: "—").ifBlank { "—" }}")
                                Text("Capacità Massima Cavi: ${seg.capacityMaxCables ?: "Illimitata"}")
                                if (!seg.notes.isNullOrBlank()) Text("Note: ${seg.notes}", style = MaterialTheme.typography.bodySmall)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    editingSegment = seg
                                    showDialog = true
                                }) {
                                    Text("Modifica", fontSize = 11.sp)
                                }

                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        val updated = DesktopDomainLogic.deleteSharedPathSegment(project, seg.id)
                                        onProjectUpdated(updated, "Percorso rimosso.")
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

    if (showDialog) {
        val s = editingSegment
        var name by remember { mutableStateOf(s?.name ?: "") }
        var description by remember { mutableStateOf(s?.description ?: "") }
        var maxCablesText by remember { mutableStateOf(s?.capacityMaxCables?.toString() ?: "") }
        var notes by remember { mutableStateOf(s?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (s == null) "Nuovo Percorso Condiviso" else "Modifica Percorso") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Tratta / Canalina") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrizione Percorso") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxCablesText,
                        onValueChange = { maxCablesText = it },
                        label = { Text("Capacità Massima Cavi (opzionale)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

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
                        val cap = maxCablesText.toIntOrNull()
                        val newSeg = SharedPathSegment(
                            id = s?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            description = description.ifBlank { null },
                            capacityMaxCables = cap,
                            notes = notes.ifBlank { null }
                        )

                        val updated = if (s == null) {
                            DesktopDomainLogic.addSharedPathSegment(project, newSeg)
                        } else {
                            DesktopDomainLogic.updateSharedPathSegment(project, newSeg)
                        }
                        onProjectUpdated(updated, "Percorso condiviso salvato.")
                        showDialog = false
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annulla") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelMappingsSubSection(
    project: Project,
    allPorts: List<Triple<Port, Device, BusinessUnit>>,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingMapping by remember { mutableStateOf<PanelMapping?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mapping Mappature Permutatori e Pannelli", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                editingMapping = null
                showDialog = true
            }) {
                Text("+ Nuova Permutazione")
            }
        }

        if (project.panelMappings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessuna permutazione / mapping pannello definito.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.panelMappings) { map ->
                    val portA = allPorts.find { it.first.id == map.portAId }
                    val portB = allPorts.find { it.first.id == map.portBId }

                    val labelA = portA?.let { "${it.second.technicalName}:${it.first.name}" } ?: map.portAId
                    val labelB = portB?.let { "${it.second.technicalName}:${it.first.name}" } ?: (map.portBId ?: "— Nessuna —")

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
                                Text("Permutazione Tipo: ${map.mappingType}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Porta A: $labelA  ↔️  Porta B: $labelB")
                                if (map.isUnknownPassage) Text("⚠️ Passaggio sconosciuto / Sotto-traccia", color = MaterialTheme.colorScheme.error)
                                if (!map.notes.isNullOrBlank()) Text("Note: ${map.notes}", style = MaterialTheme.typography.bodySmall)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    editingMapping = map
                                    showDialog = true
                                }) {
                                    Text("Modifica", fontSize = 11.sp)
                                }

                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        val updated = DesktopDomainLogic.deletePanelMapping(project, map.id)
                                        onProjectUpdated(updated, "Permutazione rimossa.")
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

    if (showDialog) {
        val m = editingMapping
        var portAId by remember { mutableStateOf(m?.portAId ?: "") }
        var portBId by remember { mutableStateOf(m?.portBId ?: "") }
        var mappingType by remember { mutableStateOf(m?.mappingType ?: "CROSS_CONNECT") }
        var isUnknownPassage by remember { mutableStateOf(m?.isUnknownPassage ?: false) }
        var notes by remember { mutableStateOf(m?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (m == null) "Nuova Permutazione" else "Modifica Permutazione") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = portAId,
                        onValueChange = { portAId = it },
                        label = { Text("ID Porta A") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = portBId,
                        onValueChange = { portBId = it },
                        label = { Text("ID Porta B (opzionale)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = mappingType,
                        onValueChange = { mappingType = it },
                        label = { Text("Tipo Mappatura (es. CROSS_CONNECT, PATCH_PANEL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isUnknownPassage,
                            onCheckedChange = { isUnknownPassage = it }
                        )
                        Text("Passaggio sconosciuto / Tratta non ispezionabile")
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
                    enabled = portAId.isNotBlank(),
                    onClick = {
                        val newMapping = PanelMapping(
                            id = m?.id ?: UUID.randomUUID().toString(),
                            portAId = portAId,
                            portBId = portBId.ifBlank { null },
                            mappingType = mappingType,
                            isUnknownPassage = isUnknownPassage,
                            notes = notes.ifBlank { null }
                        )

                        val updated = if (m == null) {
                            DesktopDomainLogic.addPanelMapping(project, newMapping)
                        } else {
                            DesktopDomainLogic.updatePanelMapping(project, newMapping)
                        }
                        onProjectUpdated(updated, "Mapping pannello salvato.")
                        showDialog = false
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annulla") }
            }
        )
    }
}
