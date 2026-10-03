package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.*

@Composable
fun NetworkLogicalSection(project: Project, onProjectUpdated: (Project, String) -> Unit) {
    val index = remember(project) { ProjectIndex(project) }
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubTabs(
            listOf(
                "VLAN (${project.vlans.size})",
                "Subnet (${project.subnets.size})",
                "Interfacce logiche (${project.logicalInterfaces.size})",
                "WAN / VPN (${project.wanVpnConnections.size})",
                "Configurazioni (${project.deviceConfigurations.size})",
                "Campi extra (${project.customExtraFields.size})"
            ),
            tab
        ) { tab = it }
        when (tab) {
            0 -> VlanTab(project, onProjectUpdated)
            1 -> SubnetTab(project, onProjectUpdated)
            2 -> InterfacesTab(project, index, onProjectUpdated)
            3 -> WanTab(project, index, onProjectUpdated)
            4 -> ConfigsTab(project, index, onProjectUpdated)
            5 -> ExtraFieldsTab(project, index, onProjectUpdated)
        }
    }
}

private fun vlanLabel(project: Project, vlanNumber: Int?): String =
    vlanNumber?.let { n -> project.vlans.find { it.vlanId == n }?.let { "VLAN $n · ${it.name}" } ?: "VLAN $n" } ?: "—"

@Composable
private fun VlanTab(project: Project, onProjectUpdated: (Project, String) -> Unit) {
    val index = remember(project) { ProjectIndex(project) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Vlan?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }
    val vlans = project.vlans.filter { matchesQuery(query, it.vlanId.toString(), it.name, it.description) }.sortedBy { it.vlanId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("VLAN", searchQuery = query, onSearchChange = { query = it }, searchPlaceholder = "Cerca numero o nome…") {
            Button(onClick = { changeDetail { creating = true } }) { Text("+ Nuova VLAN") }
        }
        if (vlans.isEmpty()) EmptyState("Nessuna VLAN.", actionLabel = "+ Nuova VLAN", onAction = { creating = true })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vlans, key = { it.id }) { v ->
                val scope = if (v.scopeType == VlanScopeType.PROJECT) "Tutto il progetto"
                else "${v.scopeType.toDisplayString()}: ${index.entityName(v.scopeTargetId) ?: "non indicato"}"
                ItemCard(title = "VLAN ${v.vlanId} · ${v.name}", details = listOf(scope, v.description.orEmpty())) {
                    EditButton { editing = v }
                    DeleteButton("VLAN ${v.vlanId}", onDelete = { onProjectUpdated(ProjectEdits.deleteVlan(project, v.id), "VLAN ${v.vlanId} eliminata.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val v = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, v) { mutableStateOf(VlanForm.from(v)) }
        val taken = project.vlans.filter { it.id != v?.id && it.scopeType == VlanScopeType.PROJECT }.map { it.vlanId }.toSet()
        val errors = form.errors(taken)
        EditPanel(
            title = if (v == null) "Nuova VLAN" else "Modifica VLAN",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toVlan(v)
                creating = false; editing = null
                onProjectUpdated(if (v == null) ProjectEdits.addVlan(project, saved) else ProjectEdits.updateVlan(project, saved), "VLAN ${saved.vlanId} salvata.")
            },
            width = 520.dp
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.vlanId, { form = form.copy(vlanId = it) }, "Numero VLAN *", Modifier.weight(0.6f), errors["vlanId"], hint = "1–4094")
                FormField(form.name, { form = form.copy(name = it) }, "Nome *", Modifier.weight(1f), errors["name"])
            }
            EnumPicker("Ambito", VlanScopeType.entries, form.scopeType, { it.toDisplayString() }, { form = form.copy(scopeType = it, scopeTargetId = null) })
            when (form.scopeType) {
                VlanScopeType.BUSINESS_UNIT -> OptionPicker("Business unit", project.businessUnits, project.businessUnits.find { it.id == form.scopeTargetId }, { it.name }, { form = form.copy(scopeTargetId = it?.id) })
                VlanScopeType.SITE -> OptionPicker("Sede", index.sites, index.sites.find { it.id == form.scopeTargetId }, { it.name }, { form = form.copy(scopeTargetId = it?.id) })
                VlanScopeType.DEVICE -> DevicePicker("Apparato", index, form.scopeTargetId, { form = form.copy(scopeTargetId = it) })
                VlanScopeType.PROJECT -> Unit
            }
            FormField(form.description, { form = form.copy(description = it) }, "Descrizione")
        }
    }
}

@Composable
private fun SubnetTab(project: Project, onProjectUpdated: (Project, String) -> Unit) {
    var editing by remember { mutableStateOf<Subnet?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Subnet") { Button(onClick = { changeDetail { creating = true } }) { Text("+ Nuova subnet") } }
        if (project.subnets.isEmpty()) EmptyState("Nessuna subnet.", actionLabel = "+ Nuova subnet", onAction = { creating = true })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.subnets, key = { it.id }) { s ->
                val vlan = project.vlans.find { it.id == s.vlanId }
                ItemCard(
                    title = s.cidrBlock + (s.name?.let { " · $it" } ?: ""),
                    details = listOf(
                        listOfNotNull(s.gatewayIp?.let { "Gateway $it" }, vlan?.let { "VLAN ${it.vlanId} · ${it.name}" }).joinToString(" · "),
                        s.description.orEmpty()
                    )
                ) {
                    EditButton { editing = s }
                    DeleteButton(s.cidrBlock, onDelete = { onProjectUpdated(ProjectEdits.deleteSubnet(project, s.id), "Subnet ${s.cidrBlock} eliminata.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val s = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, s) { mutableStateOf(SubnetForm.from(s)) }
        val errors = form.errors()
        EditPanel(
            title = if (s == null) "Nuova subnet" else "Modifica subnet",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toSubnet(s)
                creating = false; editing = null
                onProjectUpdated(if (s == null) ProjectEdits.addSubnet(project, saved) else ProjectEdits.updateSubnet(project, saved), "Subnet ${saved.cidrBlock} salvata.")
            },
            width = 520.dp
        ) {
            FormField(form.cidrBlock, { form = form.copy(cidrBlock = it) }, "Blocco CIDR *", error = errors["cidrBlock"], hint = "Es. 192.168.10.0/24")
            FormField(form.gatewayIp, { form = form.copy(gatewayIp = it) }, "Gateway", error = errors["gatewayIp"])
            OptionPicker(
                "VLAN", project.vlans.sortedBy { it.vlanId }, project.vlans.find { it.id == form.vlanRefId },
                { "VLAN ${it.vlanId} · ${it.name}" }, { form = form.copy(vlanRefId = it?.id) }, noneLabel = "Nessuna VLAN"
            )
            FormField(form.name, { form = form.copy(name = it) }, "Nome")
            FormField(form.description, { form = form.copy(description = it) }, "Descrizione")
        }
    }
}

@Composable
private fun InterfacesTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var editing by remember { mutableStateOf<LogicalInterface?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Interfacce logiche", subtitle = "SVI, loopback e altre interfacce di livello 3") {
            Button(onClick = { changeDetail { creating = true } }, enabled = index.devices.isNotEmpty()) { Text("+ Nuova interfaccia") }
        }
        if (project.logicalInterfaces.isEmpty()) EmptyState("Nessuna interfaccia logica.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.logicalInterfaces, key = { it.id }) { li ->
                ItemCard(
                    title = "${index.deviceName(li.deviceId, "Apparato mancante")} › ${li.name}",
                    details = listOf(
                        listOfNotNull(li.ipAddress, li.subnetCidr, li.vlanId?.let { vlanLabel(project, it) }, if (li.isL3) "L3" else "L2").joinToString(" · "),
                        li.notes.orEmpty()
                    )
                ) {
                    EditButton { editing = li }
                    DeleteButton(li.name, onDelete = { onProjectUpdated(ProjectEdits.deleteLogicalInterface(project, li.id), "Interfaccia eliminata.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val li = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, li) { mutableStateOf(LogicalInterfaceForm.from(li)) }
        val errors = form.errors()
        EditPanel(
            title = if (li == null) "Nuova interfaccia logica" else "Modifica interfaccia",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toInterface(li)
                creating = false; editing = null
                onProjectUpdated(if (li == null) ProjectEdits.addLogicalInterface(project, saved) else ProjectEdits.updateLogicalInterface(project, saved), "Interfaccia salvata.")
            }
        ) {
            DevicePicker("Apparato *", index, form.deviceId, { form = form.copy(deviceId = it) }, error = errors["deviceId"])
            FormField(form.name, { form = form.copy(name = it) }, "Nome *", error = errors["name"], hint = "Es. Vlan10, Loopback0")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.ipAddress, { form = form.copy(ipAddress = it) }, "Indirizzo IP", Modifier.weight(1f), errors["ipAddress"])
                FormField(form.subnetCidr, { form = form.copy(subnetCidr = it) }, "Subnet (CIDR)", Modifier.weight(1f), errors["subnetCidr"])
            }
            OptionPicker(
                "VLAN", project.vlans.sortedBy { it.vlanId }, project.vlans.find { it.vlanId == form.vlanId },
                { "VLAN ${it.vlanId} · ${it.name}" }, { form = form.copy(vlanId = it?.vlanId) }, noneLabel = "Nessuna VLAN"
            )
            FormField(form.macAddress, { form = form.copy(macAddress = it) }, "MAC", error = errors["macAddress"])
            LabeledCheckbox(form.isL3, { form = form.copy(isL3 = it) }, "Interfaccia di livello 3 (con indirizzo IP)")
            FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false)
        }
    }
}

@Composable
private fun WanTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var editing by remember { mutableStateOf<WanVpnConnection?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Connessioni WAN / VPN") { Button(onClick = { changeDetail { creating = true } }) { Text("+ Nuova connessione") } }
        if (project.wanVpnConnections.isEmpty()) EmptyState("Nessuna connessione WAN o VPN.", actionLabel = "+ Nuova connessione", onAction = { creating = true })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.wanVpnConnections, key = { it.id }) { c ->
                val local = c.localEndpointDeviceId?.let { index.deviceName(it) } ?: c.localEndpointSiteDescription ?: "?"
                val remote = c.remoteEndpointDeviceId?.let { index.deviceName(it) } ?: c.remoteEndpointSiteDescription ?: "?"
                ItemCard(
                    title = c.name,
                    badge = c.type.toDisplayString(),
                    details = listOf("$local → $remote", listOfNotNull(c.providerOrCarrier, c.bandwidth).joinToString(" · "), c.notes.orEmpty())
                ) {
                    EditButton { editing = c }
                    DeleteButton(c.name, onDelete = { onProjectUpdated(ProjectEdits.deleteWanVpnConnection(project, c.id), "Connessione eliminata.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val c = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, c) { mutableStateOf(WanForm.from(c)) }
        val errors = form.errors()
        EditPanel(
            title = if (c == null) "Nuova connessione" else "Modifica connessione",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toConnection(c)
                creating = false; editing = null
                onProjectUpdated(if (c == null) ProjectEdits.addWanVpnConnection(project, saved) else ProjectEdits.updateWanVpnConnection(project, saved), "Connessione salvata.")
            },
            width = 640.dp
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.name, { form = form.copy(name = it) }, "Nome / circuito *", Modifier.weight(1.4f), errors["name"])
                EnumPicker("Tipo", WanVpnType.entries, form.type, { it.toDisplayString() }, { form = form.copy(type = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.provider, { form = form.copy(provider = it) }, "Operatore", Modifier.weight(1f))
                FormField(form.bandwidth, { form = form.copy(bandwidth = it) }, "Banda", Modifier.weight(1f), hint = "Es. 1 Gbps")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevicePicker("Apparato locale", index, form.localDeviceId, { form = form.copy(localDeviceId = it) }, Modifier.weight(1f), noneLabel = "Non nel progetto")
                FormField(form.localSite, { form = form.copy(localSite = it) }, "Sede locale (descrizione)", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevicePicker("Apparato remoto", index, form.remoteDeviceId, { form = form.copy(remoteDeviceId = it) }, Modifier.weight(1f), noneLabel = "Non nel progetto")
                FormField(form.remoteSite, { form = form.copy(remoteSite = it) }, "Sede remota (descrizione)", Modifier.weight(1f))
            }
            FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false)
        }
    }
}

@Composable
private fun ConfigsTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var editing by remember { mutableStateOf<DeviceConfiguration?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Configurazioni apparati", subtitle = "Copie testuali di running-config e script") {
            Button(onClick = { changeDetail { creating = true } }, enabled = index.devices.isNotEmpty()) { Text("+ Nuova configurazione") }
        }
        if (project.deviceConfigurations.isEmpty()) EmptyState("Nessuna configurazione salvata.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.deviceConfigurations, key = { it.id }) { cfg ->
                ItemCard(
                    title = "${index.deviceName(cfg.deviceId, "Apparato mancante")} › ${cfg.title}",
                    details = listOf(cfg.configText?.lines()?.firstOrNull { it.isNotBlank() }?.take(120).orEmpty(), "${cfg.configText?.lines()?.size ?: 0} righe")
                ) {
                    EditButton { editing = cfg }
                    DeleteButton(cfg.title, onDelete = { onProjectUpdated(ProjectEdits.deleteDeviceConfiguration(project, cfg.id), "Configurazione eliminata.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val cfg = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, cfg) { mutableStateOf(DeviceConfigForm.from(cfg)) }
        val errors = form.errors()
        EditPanel(
            title = if (cfg == null) "Nuova configurazione" else "Modifica configurazione",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toConfig(cfg)
                creating = false; editing = null
                onProjectUpdated(if (cfg == null) ProjectEdits.addDeviceConfiguration(project, saved) else ProjectEdits.updateDeviceConfiguration(project, saved), "Configurazione salvata.")
            },
            width = 760.dp
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevicePicker("Apparato *", index, form.deviceId, { form = form.copy(deviceId = it) }, Modifier.weight(1f), error = errors["deviceId"])
                FormField(form.title, { form = form.copy(title = it) }, "Titolo *", Modifier.weight(1f), errors["title"], hint = "Es. Running config 03/10")
            }
            val markDirty = LocalMarkDirty.current
            OutlinedTextField(
                value = form.configText,
                onValueChange = { markDirty(); form = form.copy(configText = it) },
                label = { Text("Testo della configurazione") },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                minLines = 12,
                modifier = Modifier.fillMaxWidth()
            )
            FormField(form.notes, { form = form.copy(notes = it) }, "Note")
        }
    }
}

@Composable
private fun ExtraFieldsTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var editing by remember { mutableStateOf<CustomExtraField?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Campi extra", subtitle = "Informazioni aggiuntive libere su progetto, apparati, rack, porte o aree") {
            Button(onClick = { changeDetail { creating = true } }) { Text("+ Nuovo campo") }
        }
        if (project.customExtraFields.isEmpty()) EmptyState("Nessun campo extra.", actionLabel = "+ Nuovo campo", onAction = { creating = true })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.customExtraFields, key = { it.id }) { f ->
                ItemCard(
                    title = "${f.fieldKey}: ${f.fieldValue}",
                    badge = f.classification.takeIf { it != AttachmentClassification.SHAREABLE }?.toDisplayString(),
                    details = listOf(index.targetLabel(f.targetType, f.targetId), f.fieldType.toDisplayString())
                ) {
                    EditButton { editing = f }
                    DeleteButton(f.fieldKey, onDelete = { onProjectUpdated(ProjectEdits.deleteCustomExtraField(project, f.id), "Campo eliminato.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val f = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, f) { mutableStateOf(ExtraFieldForm.from(f)) }
        val errors = form.errors()
        EditPanel(
            title = if (f == null) "Nuovo campo extra" else "Modifica campo extra",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toField(f, project.id)
                creating = false; editing = null
                onProjectUpdated(if (f == null) ProjectEdits.addCustomExtraField(project, saved) else ProjectEdits.updateCustomExtraField(project, saved), "Campo salvato.")
            },
            width = 620.dp
        ) {
            TargetPicker(index, form.target, { form = form.copy(target = it) }, errors["target"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.key, { form = form.copy(key = it) }, "Nome campo *", Modifier.weight(1f), errors["key"], hint = "Es. Numero di serie")
                EnumPicker("Tipo", CustomFieldType.entries, form.fieldType, { it.toDisplayString() }, { form = form.copy(fieldType = it) }, Modifier.weight(0.6f))
            }
            FormField(form.value, { form = form.copy(value = it) }, "Valore", error = errors["value"])
            EnumPicker("Classificazione", AttachmentClassification.entries, form.classification, { it.toDisplayString() }, { form = form.copy(classification = it) })
        }
    }
}
