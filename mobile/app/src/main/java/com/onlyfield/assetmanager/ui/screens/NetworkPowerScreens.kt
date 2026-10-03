package com.onlyfield.assetmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.components.*

/** Generic "list with a dialog to create/edit" used by the network and power tabs. */
private class EditTarget<T>(val item: T?)

private fun <T> LazyListScope.entityItems(
    list: List<T>,
    empty: String,
    key: (T) -> String,
    card: @Composable (T) -> Unit,
) {
    if (list.isEmpty()) item { EmptyState(empty) }
    items(list, key = key) { card(it) }
}

@Composable
fun NetworkScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current
    var tab by remember { mutableStateOf(0) }
    var vlan by remember { mutableStateOf<EditTarget<Vlan>?>(null) }
    var subnet by remember { mutableStateOf<EditTarget<Subnet>?>(null) }
    var iface by remember { mutableStateOf<EditTarget<LogicalInterface>?>(null) }
    var wan by remember { mutableStateOf<EditTarget<WanVpnConnection>?>(null) }
    var config by remember { mutableStateOf<EditTarget<DeviceConfiguration>?>(null) }
    var extra by remember { mutableStateOf<EditTarget<CustomExtraField>?>(null) }
    fun del(name: String, msg: String, op: (Project) -> Project) = confirm(ConfirmRequest("Eliminare «$name»?", "L'elemento verrà eliminato dal progetto.") { vm.edit(msg, op) })
    fun vlanLabel(n: Int?) = n?.let { v -> project.vlans.find { it.vlanId == v }?.let { "VLAN $v · ${it.name}" } ?: "VLAN $v" }

    val tabs = listOf("VLAN", "Subnet", "Interfacce", "WAN/VPN", "Configurazioni", "Campi extra")
    AppScaffold(
        "Rete", onBack = { vm.back() }, snackbarHost = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                when (tab) {
                    0 -> vlan = EditTarget(null); 1 -> subnet = EditTarget(null); 2 -> iface = EditTarget(null)
                    3 -> wan = EditTarget(null); 4 -> config = EditTarget(null); else -> extra = EditTarget(null)
                }
            }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Aggiungi") })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SubTabs(tabs, tab) { tab = it }
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> entityItems(project.vlans.sortedBy { it.vlanId }, "Nessuna VLAN.", { it.id }) { v ->
                        ItemCard("VLAN ${v.vlanId} · ${v.name}", listOf(v.scopeType.toDisplayString(), v.description.orEmpty()), onClick = { vlan = EditTarget(v) },
                            menu = listOf(MenuAction("Elimina", true) { del("VLAN ${v.vlanId}", "VLAN eliminata.") { ProjectEdits.deleteVlan(it, v.id) } }))
                    }
                    1 -> entityItems(project.subnets, "Nessuna subnet.", { it.id }) { s ->
                        val v = project.vlans.find { it.id == s.vlanId }
                        ItemCard(s.cidrBlock + (s.name?.let { " · $it" } ?: ""), listOf(listOfNotNull(s.gatewayIp?.let { "Gateway $it" }, v?.let { "VLAN ${it.vlanId}" }).joinToString(" · ")),
                            onClick = { subnet = EditTarget(s) },
                            menu = listOf(MenuAction("Elimina", true) { del(s.cidrBlock, "Subnet eliminata.") { ProjectEdits.deleteSubnet(it, s.id) } }))
                    }
                    2 -> entityItems(project.logicalInterfaces, "Nessuna interfaccia logica.", { it.id }) { i ->
                        ItemCard("${index.deviceName(i.deviceId)} › ${i.name}", listOf(listOfNotNull(i.ipAddress, i.subnetCidr, vlanLabel(i.vlanId)).joinToString(" · ")),
                            onClick = { iface = EditTarget(i) },
                            menu = listOf(MenuAction("Elimina", true) { del(i.name, "Interfaccia eliminata.") { ProjectEdits.deleteLogicalInterface(it, i.id) } }))
                    }
                    3 -> entityItems(project.wanVpnConnections, "Nessuna connessione WAN o VPN.", { it.id }) { c ->
                        val local = c.localEndpointDeviceId?.let { index.deviceName(it) } ?: c.localEndpointSiteDescription ?: "?"
                        val remote = c.remoteEndpointDeviceId?.let { index.deviceName(it) } ?: c.remoteEndpointSiteDescription ?: "?"
                        ItemCard(c.name, listOf("$local → $remote", listOfNotNull(c.providerOrCarrier, c.bandwidth).joinToString(" · ")), badge = c.type.toDisplayString(),
                            onClick = { wan = EditTarget(c) },
                            menu = listOf(MenuAction("Elimina", true) { del(c.name, "Connessione eliminata.") { ProjectEdits.deleteWanVpnConnection(it, c.id) } }))
                    }
                    4 -> entityItems(project.deviceConfigurations, "Nessuna configurazione salvata.", { it.id }) { c ->
                        ItemCard("${index.deviceName(c.deviceId)} › ${c.title}", listOf("${c.configText?.lines()?.size ?: 0} righe · ${formatDateTime(c.capturedEpochMs)}"),
                            onClick = { config = EditTarget(c) },
                            menu = listOf(MenuAction("Elimina", true) { del(c.title, "Configurazione eliminata.") { ProjectEdits.deleteDeviceConfiguration(it, c.id) } }))
                    }
                    else -> entityItems(project.customExtraFields, "Nessun campo extra.", { it.id }) { f ->
                        ItemCard("${f.fieldKey}: ${f.fieldValue}", listOf(index.targetLabel(f.targetType, f.targetId)), onClick = { extra = EditTarget(f) },
                            menu = listOf(MenuAction("Elimina", true) { del(f.fieldKey, "Campo eliminato.") { ProjectEdits.deleteCustomExtraField(it, f.id) } }))
                    }
                }
            }
        }
    }

    vlan?.let { t ->
        val v = t.item
        var form by remember(t) { mutableStateOf(VlanForm.from(v)) }
        val taken = project.vlans.filter { it.id != v?.id && it.scopeType == VlanScopeType.PROJECT }.map { it.vlanId }.toSet()
        val errors = form.errors(taken)
        FormDialog(if (v == null) "Nuova VLAN" else "Modifica VLAN", { vlan = null }, {
            vlan = null; val saved = form.toVlan(v)
            vm.edit("VLAN ${saved.vlanId} salvata.") { if (v == null) ProjectEdits.addVlan(it, saved) else ProjectEdits.updateVlan(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            FormField(form.vlanId, { form = form.copy(vlanId = it) }, "Numero VLAN *", error = errors["vlanId"], hint = "1–4094", kind = FieldKind.NUMBER)
            FormField(form.name, { form = form.copy(name = it) }, "Nome *", error = errors["name"])
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

    subnet?.let { t ->
        val s = t.item
        var form by remember(t) { mutableStateOf(SubnetForm.from(s)) }
        val errors = form.errors()
        FormDialog(if (s == null) "Nuova subnet" else "Modifica subnet", { subnet = null }, {
            subnet = null; val saved = form.toSubnet(s)
            vm.edit("Subnet salvata.") { if (s == null) ProjectEdits.addSubnet(it, saved) else ProjectEdits.updateSubnet(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            FormField(form.cidrBlock, { form = form.copy(cidrBlock = it) }, "Blocco CIDR *", error = errors["cidrBlock"], hint = "Es. 192.168.10.0/24", kind = FieldKind.IP)
            FormField(form.gatewayIp, { form = form.copy(gatewayIp = it) }, "Gateway", error = errors["gatewayIp"], kind = FieldKind.IP)
            OptionPicker("VLAN", project.vlans.sortedBy { it.vlanId }, project.vlans.find { it.id == form.vlanRefId }, { "VLAN ${it.vlanId} · ${it.name}" },
                { form = form.copy(vlanRefId = it?.id) }, noneLabel = "Nessuna VLAN")
            FormField(form.name, { form = form.copy(name = it) }, "Nome")
            FormField(form.description, { form = form.copy(description = it) }, "Descrizione")
        }
    }

    iface?.let { t ->
        val i = t.item
        var form by remember(t) { mutableStateOf(LogicalInterfaceForm.from(i)) }
        val errors = form.errors()
        FormDialog(if (i == null) "Nuova interfaccia" else "Modifica interfaccia", { iface = null }, {
            iface = null; val saved = form.toInterface(i)
            vm.edit("Interfaccia salvata.") { if (i == null) ProjectEdits.addLogicalInterface(it, saved) else ProjectEdits.updateLogicalInterface(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            DevicePicker("Apparato *", index, form.deviceId, { form = form.copy(deviceId = it) })
            FormField(form.name, { form = form.copy(name = it) }, "Nome *", error = errors["name"], hint = "Es. Vlan10")
            FormField(form.ipAddress, { form = form.copy(ipAddress = it) }, "Indirizzo IP", error = errors["ipAddress"], kind = FieldKind.IP)
            FormField(form.subnetCidr, { form = form.copy(subnetCidr = it) }, "Subnet (CIDR)", error = errors["subnetCidr"], kind = FieldKind.IP)
            OptionPicker("VLAN", project.vlans.sortedBy { it.vlanId }, project.vlans.find { it.vlanId == form.vlanId }, { "VLAN ${it.vlanId} · ${it.name}" },
                { form = form.copy(vlanId = it?.vlanId) }, noneLabel = "Nessuna VLAN")
            FormField(form.macAddress, { form = form.copy(macAddress = it) }, "MAC", error = errors["macAddress"])
            LabeledCheckbox(form.isL3, { form = form.copy(isL3 = it) }, "Interfaccia di livello 3")
        }
    }

    wan?.let { t ->
        val c = t.item
        var form by remember(t) { mutableStateOf(WanForm.from(c)) }
        val errors = form.errors()
        FormDialog(if (c == null) "Nuova connessione" else "Modifica connessione", { wan = null }, {
            wan = null; val saved = form.toConnection(c)
            vm.edit("Connessione salvata.") { if (c == null) ProjectEdits.addWanVpnConnection(it, saved) else ProjectEdits.updateWanVpnConnection(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            FormField(form.name, { form = form.copy(name = it) }, "Nome / circuito *", error = errors["name"])
            EnumPicker("Tipo", WanVpnType.entries, form.type, { it.toDisplayString() }, { form = form.copy(type = it) })
            FormField(form.provider, { form = form.copy(provider = it) }, "Operatore")
            FormField(form.bandwidth, { form = form.copy(bandwidth = it) }, "Banda", hint = "Es. 1 Gbps")
            DevicePicker("Apparato locale", index, form.localDeviceId, { form = form.copy(localDeviceId = it) }, noneLabel = "Non nel progetto")
            FormField(form.localSite, { form = form.copy(localSite = it) }, "Sede locale")
            DevicePicker("Apparato remoto", index, form.remoteDeviceId, { form = form.copy(remoteDeviceId = it) }, noneLabel = "Non nel progetto")
            FormField(form.remoteSite, { form = form.copy(remoteSite = it) }, "Sede remota")
        }
    }

    config?.let { t ->
        val c = t.item
        var form by remember(t) { mutableStateOf(DeviceConfigForm.from(c)) }
        val errors = form.errors()
        FormDialog(if (c == null) "Nuova configurazione" else "Modifica configurazione", { config = null }, {
            config = null; val saved = form.toConfig(c)
            vm.edit("Configurazione salvata.") { if (c == null) ProjectEdits.addDeviceConfiguration(it, saved) else ProjectEdits.updateDeviceConfiguration(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            DevicePicker("Apparato *", index, form.deviceId, { form = form.copy(deviceId = it) })
            FormField(form.title, { form = form.copy(title = it) }, "Titolo *", error = errors["title"])
            OutlinedTextField(form.configText, { form = form.copy(configText = it) }, label = { Text("Testo della configurazione") },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), minLines = 8, modifier = Modifier.fillMaxWidth())
        }
    }

    extra?.let { t ->
        val f = t.item
        var form by remember(t) { mutableStateOf(ExtraFieldForm.from(f)) }
        val errors = form.errors()
        FormDialog(if (f == null) "Nuovo campo extra" else "Modifica campo extra", { extra = null }, {
            extra = null; val saved = form.toField(f, project.id)
            vm.edit("Campo salvato.") { if (f == null) ProjectEdits.addCustomExtraField(it, saved) else ProjectEdits.updateCustomExtraField(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            TargetPicker(index, form.target, { form = form.copy(target = it) })
            FormField(form.key, { form = form.copy(key = it) }, "Nome campo *", error = errors["key"], hint = "Es. Numero di serie")
            EnumPicker("Tipo", CustomFieldType.entries, form.fieldType, { it.toDisplayString() }, { form = form.copy(fieldType = it) })
            FormField(form.value, { form = form.copy(value = it) }, "Valore", error = errors["value"])
            EnumPicker("Classificazione", AttachmentClassification.entries, form.classification, { it.toDisplayString() }, { form = form.copy(classification = it) })
        }
    }
}

@Composable
fun PowerScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current
    var tab by remember { mutableStateOf(0) }
    var feed by remember { mutableStateOf<EditTarget<PowerFeed>?>(null) }
    var poe by remember { mutableStateOf<EditTarget<PoeMapping>?>(null) }
    var badge by remember { mutableStateOf<EditTarget<DocumentBadge>?>(null) }
    fun del(name: String, msg: String, op: (Project) -> Project) = confirm(ConfirmRequest("Eliminare «$name»?", "L'elemento verrà eliminato dal progetto.") { vm.edit(msg, op) })

    AppScaffold(
        "Alimentazione", onBack = { vm.back() }, snackbarHost = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                when (tab) { 0 -> feed = EditTarget(null); 1 -> poe = EditTarget(null); else -> badge = EditTarget(null) }
            }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Aggiungi") })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SubTabs(listOf("Alimentazioni (${project.powerFeeds.size})", "PoE (${project.poeMappings.size})", "Badge (${project.documentBadges.size})"), tab) { tab = it }
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> entityItems(project.powerFeeds, "Nessuna alimentazione registrata.", { it.id }) { f ->
                        ItemCard("${index.deviceName(f.deviceId)} · ${f.feedName}", listOf(
                            (f.sourceDeviceId?.let { index.deviceName(it) } ?: f.sourceOutletDescription)?.let { "Da $it" }.orEmpty(),
                            listOfNotNull(f.voltageVolts?.let { "$it V" }, f.loadWatts?.let { "${formatNumber(it)} W" }, f.observedRuntimeMinutes?.let { "autonomia $it min" }).joinToString(" · ")
                        ), badge = f.feedType.toDisplayString(), onClick = { feed = EditTarget(f) },
                            menu = listOf(MenuAction("Elimina", true) { del(f.feedName, "Alimentazione eliminata.") { ProjectEdits.deletePowerFeed(it, f.id) } }))
                    }
                    1 -> entityItems(project.poeMappings, "Nessuna porta PoE.", { it.id }) { p ->
                        ItemCard(index.portLabel(p.portId), listOf(listOfNotNull(p.standard.toDisplayString(), p.allocatedPowerWatts?.let { "${formatNumber(it)} W" }).joinToString(" · ")),
                            badge = p.role.toDisplayString(), onClick = { poe = EditTarget(p) },
                            menu = listOf(MenuAction("Elimina", true) { del("PoE ${index.portLabel(p.portId)}", "PoE eliminato.") { ProjectEdits.deletePoeMapping(it, p.id) } }))
                    }
                    else -> entityItems(project.documentBadges, "Nessun badge.", { it.id }) { b ->
                        ItemCard(b.label, listOf(index.targetLabel(b.targetType, b.targetId)), badge = b.category.toDisplayString(),
                            onClick = if (b.isDerived) null else ({ badge = EditTarget(b) }),
                            menu = listOf(MenuAction("Elimina", true) { del(b.label, "Badge eliminato.") { ProjectEdits.deleteDocumentBadge(it, b.id) } }))
                    }
                }
            }
        }
    }

    feed?.let { t ->
        val f = t.item
        var form by remember(t) { mutableStateOf(PowerFeedForm.from(f)) }
        val errors = form.errors()
        FormDialog(if (f == null) "Nuova alimentazione" else "Modifica alimentazione", { feed = null }, {
            feed = null; val saved = form.toFeed(f)
            vm.edit("Alimentazione salvata.") { if (f == null) ProjectEdits.addPowerFeed(it, saved) else ProjectEdits.updatePowerFeed(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            DevicePicker("Apparato alimentato *", index, form.deviceId, { form = form.copy(deviceId = it) })
            FormField(form.feedName, { form = form.copy(feedName = it) }, "Nome linea *", error = errors["feedName"], hint = "Es. Alimentatore 1")
            EnumPicker("Tipo", PowerFeedType.entries, form.feedType, { it.toDisplayString() }, { form = form.copy(feedType = it) })
            DevicePicker("Sorgente (UPS / PDU)", index, form.sourceDeviceId, { form = form.copy(sourceDeviceId = it) }, noneLabel = "Non nel progetto", error = errors["sourceDeviceId"])
            FormField(form.sourceOutlet, { form = form.copy(sourceOutlet = it) }, "Presa / uscita")
            FormField(form.voltage, { form = form.copy(voltage = it) }, "Tensione (V)", error = errors["voltage"], kind = FieldKind.NUMBER)
            FormField(form.loadWatts, { form = form.copy(loadWatts = it) }, "Carico (W)", error = errors["loadWatts"], kind = FieldKind.DECIMAL)
            FormField(form.loadVa, { form = form.copy(loadVa = it) }, "Carico (VA)", error = errors["loadVa"], kind = FieldKind.DECIMAL)
            FormField(form.runtimeMinutes, { form = form.copy(runtimeMinutes = it) }, "Autonomia (min)", error = errors["runtimeMinutes"], kind = FieldKind.NUMBER)
            FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false)
        }
    }

    poe?.let { t ->
        val p = t.item
        var form by remember(t) { mutableStateOf(PoeForm.from(p)) }
        val errors = form.errors()
        FormDialog(if (p == null) "Nuova porta PoE" else "Modifica PoE", { poe = null }, {
            poe = null; val saved = form.toMapping(p)
            vm.edit("PoE salvato.") { ProjectEdits.addOrUpdatePoeMapping(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            PortPicker("Porta *", index, form.portId, { form = form.copy(portId = it) }, noneLabel = null)
            EnumPicker("Ruolo", PoeRole.entries, form.role, { it.toDisplayString() }, { form = form.copy(role = it) })
            EnumPicker("Standard", PoeStandard.entries, form.standard, { it.toDisplayString() }, { form = form.copy(standard = it) })
            FormField(form.watts, { form = form.copy(watts = it) }, "Potenza allocata (W)", error = errors["watts"], kind = FieldKind.DECIMAL)
        }
    }

    badge?.let { t ->
        val b = t.item
        var form by remember(t) { mutableStateOf(BadgeForm.from(b)) }
        val errors = form.errors()
        FormDialog(if (b == null) "Nuovo badge" else "Modifica badge", { badge = null }, {
            badge = null; val saved = form.toBadge(b, project.id)
            vm.edit("Badge salvato.") { if (b == null) ProjectEdits.addDocumentBadge(it, saved) else ProjectEdits.updateDocumentBadge(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            TargetPicker(index, form.target, { form = form.copy(target = it) })
            FormField(form.label, { form = form.copy(label = it) }, "Etichetta *", error = errors["label"])
            EnumPicker("Categoria", BadgeCategory.entries, form.category, { it.toDisplayString() }, { form = form.copy(category = it) })
        }
    }
}
