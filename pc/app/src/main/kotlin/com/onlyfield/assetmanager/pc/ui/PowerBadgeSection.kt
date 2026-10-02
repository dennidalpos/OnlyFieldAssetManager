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
fun PowerBadgeSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var subTab by remember { mutableStateOf(0) } // 0 = Alimentazione A/B, 1 = Mappature PoE, 2 = Badge Documentali

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }) {
                Text("⚡ Feeds Alimentazione A/B (${project.powerFeeds.size})", modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 1, onClick = { subTab = 1 }) {
                Text("🔌 Mapping PoE (${project.poeMappings.size})", modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 2, onClick = { subTab = 2 }) {
                Text("🏷️ Badge Documentali (${project.documentBadges.size})", modifier = Modifier.padding(10.dp))
            }
        }

        when (subTab) {
            0 -> PowerFeedsSubSection(project, onProjectUpdated)
            1 -> PoeMappingsSubSection(project, onProjectUpdated)
            2 -> DocumentBadgesSubSection(project, onProjectUpdated)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PowerFeedsSubSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingFeed by remember { mutableStateOf<PowerFeed?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Feeds Alimentazione e Linee PDU/UPS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                editingFeed = null
                showDialog = true
            }) {
                Text("+ Nuova Alimentazione")
            }
        }

        if (project.powerFeeds.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessuna linea di alimentazione registrata.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.powerFeeds) { feed ->
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
                                Text("${feed.feedName} (${feed.feedType.name})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Apparato ID: ${feed.deviceId}")
                                Text("Tensione: ${feed.voltageVolts ?: "230"}V | Carico: ${feed.loadWatts ?: "—"} W / ${feed.loadVa ?: "—"} VA")
                                Text("Sorgente/Presa: ${feed.sourceOutletDescription ?: "—"} | Autonomia UPS: ${feed.observedRuntimeMinutes ?: "—"} min")
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    editingFeed = feed
                                    showDialog = true
                                }) {
                                    Text("Modifica", fontSize = 11.sp)
                                }

                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        val updated = DesktopDomainLogic.deletePowerFeed(project, feed.id)
                                        onProjectUpdated(updated, "Alimentazione rimossa.")
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
        val f = editingFeed
        var feedName by remember { mutableStateOf(f?.feedName ?: "Linea A - UPS 1") }
        var deviceId by remember { mutableStateOf(f?.deviceId ?: "") }
        var feedType by remember { mutableStateOf(f?.feedType ?: PowerFeedType.PRIMARY_A) }
        var voltageText by remember { mutableStateOf(f?.voltageVolts?.toString() ?: "230") }
        var wattsText by remember { mutableStateOf(f?.loadWatts?.toString() ?: "") }
        var runtimeText by remember { mutableStateOf(f?.observedRuntimeMinutes?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (f == null) "Nuova Alimentazione" else "Modifica Alimentazione") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = feedName, onValueChange = { feedName = it }, label = { Text("Nome Feed Alimentazione") })
                    OutlinedTextField(value = deviceId, onValueChange = { deviceId = it }, label = { Text("ID Apparato Destinazione") })

                    Text("Tipo Alimentazione:")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PowerFeedType.entries.take(4).forEach { t ->
                            FilterChip(
                                selected = feedType == t,
                                onClick = { feedType = t },
                                label = { Text(t.name, fontSize = 10.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = voltageText, onValueChange = { voltageText = it }, label = { Text("Tensione (V)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = wattsText, onValueChange = { wattsText = it }, label = { Text("Carico Watts") }, modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(value = runtimeText, onValueChange = { runtimeText = it }, label = { Text("Autonomia stimata (minuti)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newFeed = PowerFeed(
                        id = f?.id ?: UUID.randomUUID().toString(),
                        deviceId = deviceId.ifBlank { "DEV_UNASSIGNED" },
                        feedName = feedName,
                        feedType = feedType,
                        voltageVolts = voltageText.toIntOrNull(),
                        loadWatts = wattsText.toDoubleOrNull(),
                        observedRuntimeMinutes = runtimeText.toIntOrNull()
                    )

                    val updated = if (f == null) DesktopDomainLogic.addPowerFeed(project, newFeed) else DesktopDomainLogic.updatePowerFeed(project, newFeed)
                    onProjectUpdated(updated, "Alimentazione salvata.")
                    showDialog = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Annulla") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoeMappingsSubSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Power over Ethernet (PoE) Mappings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showDialog = true }) {
                Text("+ Nuovo Mapping PoE")
            }
        }

        if (project.poeMappings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessuna mappatura PoE definita nel progetto.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.poeMappings) { poe ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Porta ID: ${poe.portId}", fontWeight = FontWeight.Bold)
                                Text("Ruolo: ${poe.role.name} | Standard: ${poe.standard.name} | Potenza: ${poe.allocatedPowerWatts ?: "—"} W")
                            }

                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                onClick = {
                                    val updated = DesktopDomainLogic.deletePoeMapping(project, poe.id)
                                    onProjectUpdated(updated, "Mapping PoE rimosso.")
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

    if (showDialog) {
        var portId by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(PoeRole.PSE_SOURCE) }
        var standard by remember { mutableStateOf(PoeStandard.IEEE_802_3AT) }
        var wattsText by remember { mutableStateOf("30.0") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Aggiungi Mapping PoE") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = portId, onValueChange = { portId = it }, label = { Text("ID Porta") })

                    Text("Ruolo PoE:")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PoeRole.entries.forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.name, fontSize = 10.sp) })
                        }
                    }

                    OutlinedTextField(value = wattsText, onValueChange = { wattsText = it }, label = { Text("Potenza Allocata (W)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val poe = PoeMapping(
                        id = UUID.randomUUID().toString(),
                        portId = portId,
                        role = role,
                        standard = standard,
                        allocatedPowerWatts = wattsText.toDoubleOrNull()
                    )
                    onProjectUpdated(DesktopDomainLogic.addOrUpdatePoeMapping(project, poe), "Mapping PoE salvato.")
                    showDialog = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Annulla") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentBadgesSubSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Badge Documentali di Progetto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showDialog = true }) {
                Text("+ Nuovo Badge")
            }
        }

        if (project.documentBadges.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun badge documentale presente.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.documentBadges) { badge ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("🏷️ ${badge.label}", fontWeight = FontWeight.Bold)
                                Text("Categoria: ${badge.category.name} | Target: ${badge.targetType} [${badge.targetId.take(8)}]")
                                if (badge.isDerived) Text("⚙️ Badge derivato automaticamente", style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                onClick = {
                                    val updated = DesktopDomainLogic.deleteDocumentBadge(project, badge.id)
                                    onProjectUpdated(updated, "Badge rimosso.")
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

    if (showDialog) {
        var label by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(BadgeCategory.FREE_LABEL) }
        var targetType by remember { mutableStateOf("DEVICE") }
        var targetId by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nuovo Badge Documentale") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Etichetta Badge") })
                    OutlinedTextField(value = targetType, onValueChange = { targetType = it }, label = { Text("Target Type (DEVICE, RACK, PROJECT)") })
                    OutlinedTextField(value = targetId, onValueChange = { targetId = it }, label = { Text("ID Target") })

                    Text("Categoria:")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BadgeCategory.entries.take(4).forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.name, fontSize = 10.sp) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val badge = DocumentBadge(
                        id = UUID.randomUUID().toString(),
                        targetType = targetType,
                        targetId = targetId.ifBlank { "PROJ_GLOBAL" },
                        label = label,
                        category = category
                    )
                    onProjectUpdated(DesktopDomainLogic.addDocumentBadge(project, badge), "Badge creato.")
                    showDialog = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Annulla") } }
        )
    }
}
