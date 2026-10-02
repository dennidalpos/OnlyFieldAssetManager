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
fun NetworkLogicalSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var subTab by remember { mutableStateOf(0) }
    // 0 = VLAN & Subnet, 1 = Interfacce Logiche & Port VLAN, 2 = Gruppi LAG & WAN/VPN, 3 = Videosorveglianza & Config, 4 = Campi Extra

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScrollableTabRow(selectedTabIndex = subTab, edgePadding = 0.dp) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }) {
                Text("🌐 VLAN & Subnet (${project.vlans.size}/${project.subnets.size})", modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 1, onClick = { subTab = 1 }) {
                Text("🔀 Interfacce & Port-VLAN (${project.logicalInterfaces.size}/${project.portVlanMemberships.size})", modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 2, onClick = { subTab = 2 }) {
                Text("🔗 LAG & WAN/VPN (${project.lagGroups.size}/${project.wanVpnConnections.size})", modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 3, onClick = { subTab = 3 }) {
                Text("📹 Videosorveglianza & Config (${project.videoSurveillanceMappings.size}/${project.deviceConfigurations.size})", modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 4, onClick = { subTab = 4 }) {
                Text("🏷️ Campi Extra (${project.customExtraFields.size})", modifier = Modifier.padding(10.dp))
            }
        }

        when (subTab) {
            0 -> VlanSubnetSubSection(project, onProjectUpdated)
            1 -> InterfacesVlanSubSection(project, onProjectUpdated)
            2 -> LagWanSubSection(project, onProjectUpdated)
            3 -> VideoConfigSubSection(project, onProjectUpdated)
            4 -> CustomFieldsSubSection(project, onProjectUpdated)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VlanSubnetSubSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showVlanDialog by remember { mutableStateOf(false) }
    var editingVlan by remember { mutableStateOf<Vlan?>(null) }

    var showSubnetDialog by remember { mutableStateOf(false) }
    var editingSubnet by remember { mutableStateOf<Subnet?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // VLAN Section
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("VLAN Definite (${project.vlans.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Button(onClick = {
                        editingVlan = null
                        showVlanDialog = true
                    }) {
                        Text("+ Nuova VLAN")
                    }
                }

                if (project.vlans.isEmpty()) {
                    Text("Nessuna VLAN configurata.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(project.vlans) { vlan ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("VLAN ${vlan.vlanId}: ${vlan.name}", fontWeight = FontWeight.Bold)
                                        Text("Ambito: ${vlan.scopeType.name} | Descrizione: ${(vlan.description ?: "—").ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = { editingVlan = vlan; showVlanDialog = true }) { Text("Modifica", fontSize = 10.sp) }
                                        Button(
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            onClick = { onProjectUpdated(DesktopDomainLogic.deleteVlan(project, vlan.id), "VLAN rimossa.") }
                                        ) { Text("Elimina", fontSize = 10.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subnet Section
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subnet CIDR (${project.subnets.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Button(onClick = {
                        editingSubnet = null
                        showSubnetDialog = true
                    }) {
                        Text("+ Nuova Subnet")
                    }
                }

                if (project.subnets.isEmpty()) {
                    Text("Nessuna Subnet CIDR configurata.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(project.subnets) { sub ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${sub.cidrBlock} ${(sub.name?.let { "($it)" } ?: "")}", fontWeight = FontWeight.Bold)
                                        Text("Gateway: ${sub.gatewayIp ?: "—"} | VLAN ID: ${sub.vlanId ?: "—"}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = { editingSubnet = sub; showSubnetDialog = true }) { Text("Modifica", fontSize = 10.sp) }
                                        Button(
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            onClick = { onProjectUpdated(DesktopDomainLogic.deleteSubnet(project, sub.id), "Subnet rimossa.") }
                                        ) { Text("Elimina", fontSize = 10.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Vlan Dialog
    if (showVlanDialog) {
        val v = editingVlan
        var vlanIdText by remember { mutableStateOf(v?.vlanId?.toString() ?: "10") }
        var name by remember { mutableStateOf(v?.name ?: "") }
        var desc by remember { mutableStateOf(v?.description ?: "") }

        AlertDialog(
            onDismissRequest = { showVlanDialog = false },
            title = { Text(if (v == null) "Nuova VLAN" else "Modifica VLAN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vlanIdText, onValueChange = { vlanIdText = it }, label = { Text("VLAN ID (1-4094)") })
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome VLAN") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descrizione") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val vid = vlanIdText.toIntOrNull() ?: 10
                    val newV = Vlan(id = v?.id ?: UUID.randomUUID().toString(), vlanId = vid, name = name.ifBlank { "VLAN_$vid" }, description = desc.ifBlank { null })
                    val updated = if (v == null) DesktopDomainLogic.addVlan(project, newV) else DesktopDomainLogic.updateVlan(project, newV)
                    onProjectUpdated(updated, "VLAN salvata.")
                    showVlanDialog = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showVlanDialog = false }) { Text("Annulla") } }
        )
    }

    // Subnet Dialog
    if (showSubnetDialog) {
        val s = editingSubnet
        var cidrBlock by remember { mutableStateOf(s?.cidrBlock ?: "192.168.1.0/24") }
        var gatewayIp by remember { mutableStateOf(s?.gatewayIp ?: "192.168.1.1") }
        var name by remember { mutableStateOf(s?.name ?: "") }

        AlertDialog(
            onDismissRequest = { showSubnetDialog = false },
            title = { Text(if (s == null) "Nuova Subnet CIDR" else "Modifica Subnet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cidrBlock, onValueChange = { cidrBlock = it }, label = { Text("Blocco CIDR (es. 10.0.0.0/24)") })
                    OutlinedTextField(value = gatewayIp, onValueChange = { gatewayIp = it }, label = { Text("IP Gateway (opzionale)") })
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome Rete (opzionale)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newS = Subnet(id = s?.id ?: UUID.randomUUID().toString(), cidrBlock = cidrBlock, gatewayIp = gatewayIp.ifBlank { null }, name = name.ifBlank { null })
                    val updated = if (s == null) DesktopDomainLogic.addSubnet(project, newS) else DesktopDomainLogic.updateSubnet(project, newS)
                    onProjectUpdated(updated, "Subnet salvata.")
                    showSubnetDialog = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showSubnetDialog = false }) { Text("Annulla") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterfacesVlanSubSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showIntDialog by remember { mutableStateOf(false) }
    var editingInt by remember { mutableStateOf<LogicalInterface?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Interfacce Logiche / SVI L3", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = { editingInt = null; showIntDialog = true }) { Text("+ Nuova Interfaccia") }
        }

        if (project.logicalInterfaces.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessuna interfaccia logica / SVI creata.") }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(project.logicalInterfaces) { logInt ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(logInt.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("IP/Subnet: ${logInt.ipAddress ?: "—"} / ${logInt.subnetCidr ?: "—"} | VLAN: ${logInt.vlanId ?: "—"}")
                                Text("MAC: ${logInt.macAddress ?: "—"} | L3: ${if (logInt.isL3) "Sì" else "No"}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { editingInt = logInt; showIntDialog = true }) { Text("Modifica", fontSize = 11.sp) }
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = { onProjectUpdated(DesktopDomainLogic.deleteLogicalInterface(project, logInt.id), "Interfaccia rimossa.") }
                                ) { Text("Elimina", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIntDialog) {
        val i = editingInt
        var devId by remember { mutableStateOf(i?.deviceId ?: "") }
        var name by remember { mutableStateOf(i?.name ?: "Vlan10") }
        var ip by remember { mutableStateOf(i?.ipAddress ?: "") }
        var subnet by remember { mutableStateOf(i?.subnetCidr ?: "") }

        AlertDialog(
            onDismissRequest = { showIntDialog = false },
            title = { Text(if (i == null) "Nuova Interfaccia Logica" else "Modifica Interfaccia") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = devId, onValueChange = { devId = it }, label = { Text("ID Apparato") })
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome Interfaccia (es. Vlan10)") })
                    OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("Indirizzo IP") })
                    OutlinedTextField(value = subnet, onValueChange = { subnet = it }, label = { Text("Subnet CIDR") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newI = LogicalInterface(
                        id = i?.id ?: UUID.randomUUID().toString(),
                        deviceId = devId.ifBlank { "DEV_UNASSIGNED" },
                        name = name,
                        ipAddress = ip.ifBlank { null },
                        subnetCidr = subnet.ifBlank { null }
                    )
                    val updated = if (i == null) DesktopDomainLogic.addLogicalInterface(project, newI) else DesktopDomainLogic.updateLogicalInterface(project, newI)
                    onProjectUpdated(updated, "Interfaccia logica salvata.")
                    showIntDialog = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showIntDialog = false }) { Text("Annulla") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LagWanSubSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showWanDialog by remember { mutableStateOf(false) }
    var editingWan by remember { mutableStateOf<WanVpnConnection?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Connessioni WAN e VPN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = { editingWan = null; showWanDialog = true }) { Text("+ Nuova Connessione WAN/VPN") }
        }

        if (project.wanVpnConnections.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessuna connessione WAN/VPN definita.") }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(project.wanVpnConnections) { conn ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${conn.name} (${conn.type.name})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Carrier: ${conn.providerOrCarrier ?: "—"} | Banda: ${conn.bandwidth ?: "—"}")
                                Text("Endpoint locale: ${conn.localEndpointSiteDescription ?: "—"}  ➡️  Remoto: ${conn.remoteEndpointSiteDescription ?: "—"}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { editingWan = conn; showWanDialog = true }) { Text("Modifica", fontSize = 11.sp) }
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = { onProjectUpdated(DesktopDomainLogic.deleteWanVpnConnection(project, conn.id), "Connessione rimossa.") }
                                ) { Text("Elimina", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWanDialog) {
        val w = editingWan
        var name by remember { mutableStateOf(w?.name ?: "") }
        var provider by remember { mutableStateOf(w?.providerOrCarrier ?: "") }
        var bandwidth by remember { mutableStateOf(w?.bandwidth ?: "") }

        AlertDialog(
            onDismissRequest = { showWanDialog = false },
            title = { Text(if (w == null) "Nuova Connessione WAN/VPN" else "Modifica Connessione") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome Connessione / Circuito") })
                    OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Provider / Carrier") })
                    OutlinedTextField(value = bandwidth, onValueChange = { bandwidth = it }, label = { Text("Banda (es. 1 Gbps)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newW = WanVpnConnection(
                        id = w?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        providerOrCarrier = provider.ifBlank { null },
                        bandwidth = bandwidth.ifBlank { null }
                    )
                    val updated = if (w == null) DesktopDomainLogic.addWanVpnConnection(project, newW) else DesktopDomainLogic.updateWanVpnConnection(project, newW)
                    onProjectUpdated(updated, "Connessione WAN/VPN salvata.")
                    showWanDialog = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showWanDialog = false }) { Text("Annulla") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoConfigSubSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showConfigDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Configurazioni Apparati / Script", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showConfigDialog = true }) { Text("+ Nuova Configurazione") }
        }

        if (project.deviceConfigurations.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessuna configurazione CLI / testo memorizzata.") }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(project.deviceConfigurations) { cfg ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cfg.title, fontWeight = FontWeight.Bold)
                                Text("Apparato ID: ${cfg.deviceId}")
                                if (!cfg.configText.isNullOrBlank()) Text("Testo Config: ${cfg.configText?.take(100)}...", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                onClick = { onProjectUpdated(DesktopDomainLogic.deleteDeviceConfiguration(project, cfg.id), "Configurazione rimossa.") }
                            ) { Text("Elimina", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }

    if (showConfigDialog) {
        var devId by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("Running Config") }
        var configText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Aggiungi Configurazione Apparato") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = devId, onValueChange = { devId = it }, label = { Text("ID Apparato") })
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titolo Configurazione") })
                    OutlinedTextField(value = configText, onValueChange = { configText = it }, label = { Text("Testo CLI / Running-Config") }, modifier = Modifier.height(150.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newCfg = DeviceConfiguration(
                        id = UUID.randomUUID().toString(),
                        deviceId = devId.ifBlank { "DEV_UNASSIGNED" },
                        title = title,
                        configText = configText.ifBlank { null }
                    )
                    onProjectUpdated(DesktopDomainLogic.addDeviceConfiguration(project, newCfg), "Configurazione aggiunta.")
                    showConfigDialog = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showConfigDialog = false }) { Text("Annulla") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomFieldsSubSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Campi Extra e Personalizzati", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showDialog = true }) { Text("+ Nuovo Campo Extra") }
        }

        if (project.customExtraFields.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessun campo extra/personalizzato registrato.") }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(project.customExtraFields) { field ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${field.fieldKey}: ${field.fieldValue}", fontWeight = FontWeight.Bold)
                                Text("Target: ${field.targetType} [${field.targetId.take(8)}] | Tipo: ${field.fieldType.name}")
                            }
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                onClick = { onProjectUpdated(DesktopDomainLogic.deleteCustomExtraField(project, field.id), "Campo extra rimosso.") }
                            ) { Text("Elimina", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var key by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        var targetType by remember { mutableStateOf("DEVICE") }
        var targetId by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nuovo Campo Extra") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Chiave Campo (es. NumeroSerialeHW)") })
                    OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Valore Campo") })
                    OutlinedTextField(value = targetType, onValueChange = { targetType = it }, label = { Text("Tipo Target (DEVICE, RACK, PROJECT)") })
                    OutlinedTextField(value = targetId, onValueChange = { targetId = it }, label = { Text("ID Target") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val field = CustomExtraField(
                        id = UUID.randomUUID().toString(),
                        targetType = targetType,
                        targetId = targetId.ifBlank { "PROJ_GLOBAL" },
                        fieldKey = key,
                        fieldValue = value
                    )
                    onProjectUpdated(DesktopDomainLogic.addCustomExtraField(project, field), "Campo extra salvato.")
                    showDialog = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Annulla") } }
        )
    }
}
