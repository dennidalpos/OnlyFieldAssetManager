package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.configurator.SecondaryModule
import com.onlyfield.assetmanager.configurator.visibleTabs
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.ui.LocalMessages

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
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current
    var tab by remember { mutableStateOf(0) }
    var vlan by remember { mutableStateOf<EditTarget<Vlan>?>(null) }
    var subnet by remember { mutableStateOf<EditTarget<Subnet>?>(null) }
    var iface by remember { mutableStateOf<EditTarget<LogicalInterface>?>(null) }
    var wan by remember { mutableStateOf<EditTarget<WanVpnConnection>?>(null) }
    var config by remember { mutableStateOf<EditTarget<DeviceConfiguration>?>(null) }
    var extra by remember { mutableStateOf<EditTarget<CustomExtraField>?>(null) }
    fun del(name: String, msg: String, op: (Project) -> Project) = confirm(ConfirmRequest(i18n.text("text.e36c23dfb086", name), i18n.text("text.072043981f03")) { vm.edit(msg, op) })
    fun vlanLabel(n: Int?) = n?.let { v -> project.vlans.find { it.vlanId == v }?.let { i18n.text("text.15fd0dfb7614", v, it.name) } ?: i18n.text("text.da4da5c165af", v) }

    val tabs = listOf("VLAN", i18n.text("text.bfea90e5ae18"), i18n.text("text.ee7739e61881"), "WAN/VPN", i18n.text("text.e44eb05c3989"), i18n.text("text.00158c772cda"))
    AppScaffold(
        i18n.text("text.a0dd274e04a0"), onBack = { vm.back() }, snackbarHost = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                when (tab) {
                    0 -> vlan = EditTarget(null); 1 -> subnet = EditTarget(null); 2 -> iface = EditTarget(null)
                    3 -> wan = EditTarget(null); 4 -> config = EditTarget(null); else -> extra = EditTarget(null)
                }
            }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.84cbef7b19b8")) })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Configurations are an optional module: no tab until used or "Other modules" is open.
            val shown = visibleTabs(tabs.size, project, vm.showSecondary, mapOf(4 to SecondaryModule.CONFIGURATIONS))
            if (tab !in shown) tab = shown.first()
            SubTabs(shown.map(tabs::get), shown.indexOf(tab)) { tab = shown[it] }
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> entityItems(project.vlans.sortedBy { it.vlanId }, i18n.text("text.7de621e87842"), { it.id }) { v ->
                        ItemCard(i18n.text("text.15fd0dfb7614", v.vlanId, v.name), listOf(v.scopeType.toDisplayString(i18n = i18n), v.description.orEmpty()), onClick = { vlan = EditTarget(v) },
                            menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), true) { del(i18n.text("text.da4da5c165af", v.vlanId), i18n.text("text.17c0edc568d1")) { ProjectEdits.deleteVlan(it, v.id, i18n) } }))
                    }
                    1 -> entityItems(project.subnets.sortedForDisplay(i18n) { it.name ?: it.cidrBlock }, i18n.text("text.446498be42fe"), { it.id }) { s ->
                        val v = project.vlans.find { it.id == s.vlanId }
                        ItemCard(s.cidrBlock + (s.name?.let { " · $it" } ?: ""), listOf(listOfNotNull(s.gatewayIp?.let { i18n.text("text.87b3d4c8eb9f", it) }, v?.let { i18n.text("text.da4da5c165af", it.vlanId) }).joinToString(" · ")),
                            onClick = { subnet = EditTarget(s) },
                            menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), true) { del(s.cidrBlock, i18n.text("text.2c102aea66fb")) { ProjectEdits.deleteSubnet(it, s.id) } }))
                    }
                    2 -> entityItems(project.logicalInterfaces.sortedForDisplay(i18n) { it.name }, i18n.text("text.22e22d133b84"), { it.id }) { i ->
                        ItemCard("${index.deviceName(i.deviceId, i18n = i18n)} › ${i.name}", listOf(listOfNotNull(i.ipAddress, i.subnetCidr, vlanLabel(i.vlanId)).joinToString(" · ")),
                            onClick = { iface = EditTarget(i) },
                            menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), true) { del(i.name, i18n.text("text.15971cc8f189")) { ProjectEdits.deleteLogicalInterface(it, i.id) } }))
                    }
                    3 -> entityItems(project.wanVpnConnections.sortedForDisplay(i18n) { it.name }, i18n.text("text.acd5d0fdeec2"), { it.id }) { c ->
                        val local = c.localEndpointDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: c.localEndpointSiteDescription ?: "?"
                        val remote = c.remoteEndpointDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: c.remoteEndpointSiteDescription ?: "?"
                        ItemCard(c.name, listOf("$local → $remote", listOfNotNull(c.providerOrCarrier, c.bandwidth).joinToString(" · ")), badge = c.type.toDisplayString(i18n = i18n),
                            onClick = { wan = EditTarget(c) },
                            menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), true) { del(c.name, i18n.text("text.36f755a3cda8")) { ProjectEdits.deleteWanVpnConnection(it, c.id) } }))
                    }
                    4 -> entityItems(project.deviceConfigurations.sortedForDisplay(i18n) { it.title }, i18n.text("text.c15c34e10908"), { it.id }) { c ->
                        ItemCard("${index.deviceName(c.deviceId, i18n = i18n)} › ${c.title}", listOf(i18n.text("text.adab2670eb2a", c.configText?.lines()?.size ?: 0, formatDateTime(c.capturedEpochMs))),
                            onClick = { config = EditTarget(c) },
                            menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), true) { del(c.title, i18n.text("text.fb507a93e55d")) { ProjectEdits.deleteDeviceConfiguration(it, c.id) } }))
                    }
                    else -> entityItems(project.customExtraFields.sortedForDisplay(i18n) { it.fieldKey }, i18n.text("text.ca56999e82d8"), { it.id }) { f ->
                        ItemCard("${f.fieldKey}: ${f.fieldValue}", listOf(index.targetLabel(f.targetType, f.targetId, i18n = i18n)), onClick = { extra = EditTarget(f) },
                            menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), true) { del(f.fieldKey, i18n.text("text.b43abaee4bcd")) { ProjectEdits.deleteCustomExtraField(it, f.id) } }))
                    }
                }
            }
        }
    }

    vlan?.let { t ->
        val v = t.item
        var form by remember(t) { mutableStateOf(VlanForm.from(v)) }
        val taken = project.vlans.filter { it.id != v?.id && it.scopeType == VlanScopeType.PROJECT }.map { it.vlanId }.toSet()
        val save = rememberEditSave(vm, t)
        val errors = form.errors(taken, i18n = i18n)
        EditScreen(if (v == null) i18n.text("text.864bdda898fc") else i18n.text("text.7413bdc75d86"), { vlan = null }, {
            val saved = form.toVlan(v)
            save.save(i18n.text("text.9c856115c4fc", saved.vlanId), { vlan = null }) { if (v == null) ProjectEdits.addVlan(it, saved) else ProjectEdits.updateVlan(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            FormField(form.vlanId, { form = form.copy(vlanId = it) }, i18n.text("text.21d0e8b7d284"), error = errors["vlanId"], hint = "1–4094", kind = FieldKind.NUMBER)
            FormField(form.name, { form = form.copy(name = it) }, i18n.text("text.2e245546ff59"), error = errors["name"])
            EnumPicker(i18n.text("text.03cbc24f25f2"), VlanScopeType.entries, form.scopeType, { it.toDisplayString(i18n = i18n) }, { form = form.copy(scopeType = it, scopeTargetId = null) })
            when (form.scopeType) {
                VlanScopeType.SITE -> OptionPicker(i18n.text("text.e4de7d26b141"), project.sites, project.sites.find { it.id == form.scopeTargetId }, { it.name }, { form = form.copy(scopeTargetId = it?.id) })
                VlanScopeType.DEVICE -> DevicePicker(i18n.text("text.cf301d95d32c"), index, form.scopeTargetId, { form = form.copy(scopeTargetId = it) })
                VlanScopeType.PROJECT -> Unit
            }
            FormField(form.description, { form = form.copy(description = it) }, i18n.text("text.6fb818621896"))
        }
    }

    subnet?.let { t ->
        val s = t.item
        var form by remember(t) { mutableStateOf(SubnetForm.from(s)) }
        val save = rememberEditSave(vm, t)
        val errors = form.errors(i18n = i18n)
        EditScreen(if (s == null) i18n.text("text.5c8aa6f5ef27") else i18n.text("text.0db1e97aa029"), { subnet = null }, {
            val saved = form.toSubnet(s)
            save.save(i18n.text("text.be84c37df37d"), { subnet = null }) { if (s == null) ProjectEdits.addSubnet(it, saved) else ProjectEdits.updateSubnet(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            FormField(form.cidrBlock, { form = form.copy(cidrBlock = it) }, i18n.text("text.a1f614a904b2"), error = errors["cidrBlock"], hint = i18n.text("text.ee4725fa1904"), kind = FieldKind.IP)
            FormField(form.gatewayIp, { form = form.copy(gatewayIp = it) }, i18n.text("text.41ed52921661"), error = errors["gatewayIp"], kind = FieldKind.IP)
            OptionPicker("VLAN", project.vlans.sortedBy { it.vlanId }, project.vlans.find { it.id == form.vlanRefId }, { i18n.text("text.15fd0dfb7614", it.vlanId, it.name) },
                { form = form.copy(vlanRefId = it?.id) }, sortByName = false, noneLabel = i18n.text("text.b60b955a9981"))
            FormField(form.name, { form = form.copy(name = it) }, i18n.text("text.5086900635fe"))
            FormField(form.description, { form = form.copy(description = it) }, i18n.text("text.6fb818621896"))
        }
    }

    iface?.let { t ->
        val i = t.item
        var form by remember(t) { mutableStateOf(LogicalInterfaceForm.from(i)) }
        val save = rememberEditSave(vm, t)
        val errors = form.errors(i18n = i18n)
        EditScreen(if (i == null) i18n.text("text.80f149c18ac7") else i18n.text("text.a4d9c2f8f102"), { iface = null }, {
            val saved = form.toInterface(i)
            save.save(i18n.text("text.4292a16c17ce"), { iface = null }) { if (i == null) ProjectEdits.addLogicalInterface(it, saved) else ProjectEdits.updateLogicalInterface(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            DevicePicker(i18n.text("text.e7f2c0e68768"), index, form.deviceId, { form = form.copy(deviceId = it) })
            FormField(form.name, { form = form.copy(name = it) }, i18n.text("text.2e245546ff59"), error = errors["name"], hint = i18n.text("text.4575ffa32f9c"))
            FormField(form.ipAddress, { form = form.copy(ipAddress = it) }, i18n.text("text.ebb396f2d486"), error = errors["ipAddress"], kind = FieldKind.IP)
            FormField(form.subnetCidr, { form = form.copy(subnetCidr = it) }, i18n.text("text.7cfdb8aaa25f"), error = errors["subnetCidr"], kind = FieldKind.IP)
            OptionPicker("VLAN", project.vlans.sortedBy { it.vlanId }, project.vlans.find { it.vlanId == form.vlanId }, { i18n.text("text.15fd0dfb7614", it.vlanId, it.name) },
                { form = form.copy(vlanId = it?.vlanId) }, sortByName = false, noneLabel = i18n.text("text.b60b955a9981"))
            FormField(form.macAddress, { form = form.copy(macAddress = it) }, "MAC", error = errors["macAddress"])
            LabeledCheckbox(form.isL3, { form = form.copy(isL3 = it) }, i18n.text("text.3d62d85ca195"))
        }
    }

    wan?.let { t ->
        val c = t.item
        var form by remember(t) { mutableStateOf(WanForm.from(c)) }
        val save = rememberEditSave(vm, t)
        val errors = form.errors(i18n = i18n)
        EditScreen(if (c == null) i18n.text("text.cfcfd59f1b90") else i18n.text("text.716af1fcb21e"), { wan = null }, {
            val saved = form.toConnection(c)
            save.save(i18n.text("text.ec170e822ebb"), { wan = null }) { if (c == null) ProjectEdits.addWanVpnConnection(it, saved) else ProjectEdits.updateWanVpnConnection(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            FormField(form.name, { form = form.copy(name = it) }, i18n.text("text.656e4a65e6cc"), error = errors["name"])
            EnumPicker(i18n.text("text.3868d2843d59"), WanVpnType.entries, form.type, { it.toDisplayString(i18n = i18n) }, { form = form.copy(type = it) })
            FormField(form.provider, { form = form.copy(provider = it) }, i18n.text("text.ad528b8d1f72"))
            FormField(form.bandwidth, { form = form.copy(bandwidth = it) }, i18n.text("text.a4ed9939fab6"), hint = i18n.text("text.b2cbeb3179e9"))
            DevicePicker(i18n.text("text.588719de20b9"), index, form.localDeviceId, { form = form.copy(localDeviceId = it) }, noneLabel = i18n.text("text.65389ba5d2fd"))
            FormField(form.localSite, { form = form.copy(localSite = it) }, i18n.text("text.5b797e8b1a2c"))
            DevicePicker(i18n.text("text.c0771cebf192"), index, form.remoteDeviceId, { form = form.copy(remoteDeviceId = it) }, noneLabel = i18n.text("text.65389ba5d2fd"))
            FormField(form.remoteSite, { form = form.copy(remoteSite = it) }, i18n.text("text.dd76664512a4"))
        }
    }

    config?.let { t ->
        val c = t.item
        var form by remember(t) { mutableStateOf(DeviceConfigForm.from(c)) }
        val save = rememberEditSave(vm, t)
        val errors = form.errors(i18n = i18n)
        EditScreen(if (c == null) i18n.text("text.66b4e494cd62") else i18n.text("text.dfff9fc66812"), { config = null }, {
            val saved = form.toConfig(c)
            save.save(i18n.text("text.b2ed6f265907"), { config = null }) { if (c == null) ProjectEdits.addDeviceConfiguration(it, saved) else ProjectEdits.updateDeviceConfiguration(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            DevicePicker(i18n.text("text.e7f2c0e68768"), index, form.deviceId, { form = form.copy(deviceId = it) })
            FormField(form.title, { form = form.copy(title = it) }, i18n.text("text.b03a74353bbd"), error = errors["title"])
            val markDirty = LocalMarkDirty.current
            OutlinedTextField(form.configText, { markDirty(); form = form.copy(configText = it) }, label = { Text(i18n.text("text.ae44cab9c2ac")) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), minLines = 8, modifier = Modifier.fillMaxWidth())
        }
    }

    extra?.let { t ->
        val f = t.item
        var form by remember(t) { mutableStateOf(ExtraFieldForm.from(f)) }
        val save = rememberEditSave(vm, t)
        val errors = form.errors(i18n = i18n)
        EditScreen(if (f == null) i18n.text("text.84b0d716f809") else i18n.text("text.d6c18565dd61"), { extra = null }, {
            val saved = form.toField(f, project.id)
            save.save(i18n.text("text.b24051277c7c"), { extra = null }) { if (f == null) ProjectEdits.addCustomExtraField(it, saved) else ProjectEdits.updateCustomExtraField(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TargetPicker(index, form.target, { form = form.copy(target = it) })
            FormField(form.key, { form = form.copy(key = it) }, i18n.text("text.4702cf978b67"), error = errors["key"], hint = i18n.text("text.08d801a474bf"))
            EnumPicker(i18n.text("text.3868d2843d59"), CustomFieldType.entries, form.fieldType, { it.toDisplayString(i18n = i18n) }, { form = form.copy(fieldType = it) })
            FormField(form.value, { form = form.copy(value = it) }, i18n.text("text.3b50ed0e6ec2"), error = errors["value"])
            EnumPicker(i18n.text("text.57fbd1029ff6"), AttachmentClassification.entries, form.classification, { it.toDisplayString(i18n = i18n) }, { form = form.copy(classification = it) })
        }
    }
}

@Composable
fun PowerScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current
    var tab by remember { mutableStateOf(0) }
    var feed by remember { mutableStateOf<EditTarget<PowerFeed>?>(null) }
    var poe by remember { mutableStateOf<EditTarget<PoeMapping>?>(null) }
    var badge by remember { mutableStateOf<EditTarget<DocumentBadge>?>(null) }
    fun del(name: String, msg: String, op: (Project) -> Project) = confirm(ConfirmRequest(i18n.text("text.e36c23dfb086", name), i18n.text("text.072043981f03")) { vm.edit(msg, op) })

    AppScaffold(
        i18n.text("text.acedc1948e5f"), onBack = { vm.back() }, snackbarHost = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                when (tab) { 0 -> feed = EditTarget(null); 1 -> poe = EditTarget(null); else -> badge = EditTarget(null) }
            }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.84cbef7b19b8")) })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val labels = listOf(i18n.text("text.370b792df123", project.powerFeeds.size), i18n.text("text.ffaf43588488", project.poeMappings.size), i18n.text("text.196b8d6896ac", project.documentBadges.size))
            // Badges are an optional module: no tab until used or "Other modules" is open.
            val shown = visibleTabs(labels.size, project, vm.showSecondary, mapOf(2 to SecondaryModule.BADGES))
            if (tab !in shown) tab = shown.first()
            SubTabs(shown.map(labels::get), shown.indexOf(tab)) { tab = shown[it] }
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> entityItems(project.powerFeeds.sortedForDisplay(i18n) { "${index.deviceName(it.deviceId, i18n = i18n)} ${it.feedName}" }, i18n.text("text.90bbe6100ed6"), { it.id }) { f ->
                        ItemCard("${index.deviceName(f.deviceId, i18n = i18n)} · ${f.feedName}", listOf(
                            (f.sourceDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: f.sourceOutletDescription)?.let { i18n.text("text.4bc711ab248c", it) }.orEmpty(),
                            listOfNotNull(f.voltageVolts?.let { i18n.text("text.edb2066c2a30", it) }, f.loadWatts?.let { i18n.text("text.cd49315c743a", formatNumber(it)) }, f.observedRuntimeMinutes?.let { i18n.text("text.2dc280aa0f83", it) }).joinToString(" · ")
                        ), badge = f.feedType.toDisplayString(i18n = i18n), onClick = { feed = EditTarget(f) },
                            menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), true) { del(f.feedName, i18n.text("text.a5c30e639a4d")) { ProjectEdits.deletePowerFeed(it, f.id) } }))
                    }
                    1 -> entityItems(project.poeMappings, i18n.text("text.68cc11f03e58"), { it.id }) { p ->
                        ItemCard(index.portLabel(p.portId), listOf(listOfNotNull(p.standard.toDisplayString(i18n = i18n), p.allocatedPowerWatts?.let { i18n.text("text.cd49315c743a", formatNumber(it)) }).joinToString(" · ")),
                            badge = p.role.toDisplayString(i18n = i18n), onClick = { poe = EditTarget(p) },
                            menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), true) { del(i18n.text("text.3685af18e07a", index.portLabel(p.portId)), i18n.text("text.a4922144c86d")) { ProjectEdits.deletePoeMapping(it, p.id) } }))
                    }
                    else -> entityItems(project.documentBadges.sortedForDisplay(i18n) { it.label }, i18n.text("text.eb3e34b0bacb"), { it.id }) { b ->
                        ItemCard(b.label, listOf(index.targetLabel(b.targetType, b.targetId, i18n = i18n)), badge = b.category.toDisplayString(i18n = i18n),
                            onClick = if (b.isDerived) null else ({ badge = EditTarget(b) }),
                            menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), true) { del(b.label, i18n.text("text.fc7c2dba7445")) { ProjectEdits.deleteDocumentBadge(it, b.id) } }))
                    }
                }
            }
        }
    }

    feed?.let { t ->
        val f = t.item
        var form by remember(t) { mutableStateOf(PowerFeedForm.from(f)) }
        val save = rememberEditSave(vm, t)
        val errors = form.errors(i18n = i18n)
        EditScreen(if (f == null) i18n.text("text.4caa90bd8cd8") else i18n.text("text.ef9307d35ec6"), { feed = null }, {
            val saved = form.toFeed(f)
            save.save(i18n.text("text.4237371feb41"), { feed = null }) { if (f == null) ProjectEdits.addPowerFeed(it, saved) else ProjectEdits.updatePowerFeed(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            DevicePicker(i18n.text("text.a814bcd8ca15"), index, form.deviceId, { form = form.copy(deviceId = it) })
            FormField(form.feedName, { form = form.copy(feedName = it) }, i18n.text("text.694885c1179e"), error = errors["feedName"], hint = i18n.text("text.eb9a6bbaecd2"))
            EnumPicker(i18n.text("text.3868d2843d59"), PowerFeedType.entries, form.feedType, { it.toDisplayString(i18n = i18n) }, { form = form.copy(feedType = it) })
            DevicePicker(i18n.text("text.11ae92f057eb"), index, form.sourceDeviceId, { form = form.copy(sourceDeviceId = it) }, noneLabel = i18n.text("text.65389ba5d2fd"), error = errors["sourceDeviceId"])
            FormField(form.sourceOutlet, { form = form.copy(sourceOutlet = it) }, i18n.text("text.cd33696ca977"))
            FormField(form.voltage, { form = form.copy(voltage = it) }, i18n.text("text.5093ead90fce"), error = errors["voltage"], kind = FieldKind.NUMBER)
            FormField(form.loadWatts, { form = form.copy(loadWatts = it) }, i18n.text("text.eb98296d7970"), error = errors["loadWatts"], kind = FieldKind.DECIMAL)
            FormField(form.loadVa, { form = form.copy(loadVa = it) }, i18n.text("text.e821b548ca4b"), error = errors["loadVa"], kind = FieldKind.DECIMAL)
            FormField(form.runtimeMinutes, { form = form.copy(runtimeMinutes = it) }, i18n.text("text.08997b56437a"), error = errors["runtimeMinutes"], kind = FieldKind.NUMBER)
            FormField(form.notes, { form = form.copy(notes = it) }, i18n.text("text.d8da2c49df39"), singleLine = false)
        }
    }

    poe?.let { t ->
        val p = t.item
        var form by remember(t) { mutableStateOf(PoeForm.from(p)) }
        val save = rememberEditSave(vm, t)
        val errors = form.errors(i18n = i18n)
        EditScreen(if (p == null) i18n.text("text.129c22f2302d") else i18n.text("text.7b99bcd56c82"), { poe = null }, {
            val saved = form.toMapping(p)
            save.save(i18n.text("text.f22a08c0aa60"), { poe = null }) { ProjectEdits.addOrUpdatePoeMapping(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PortPicker(i18n.text("text.57c2ec879203"), index, form.portId, { form = form.copy(portId = it) }, noneLabel = null)
            EnumPicker(i18n.text("text.7a972bbc1480"), PoeRole.entries, form.role, { it.toDisplayString(i18n = i18n) }, { form = form.copy(role = it) })
            EnumPicker(i18n.text("text.ef6691545d2c"), PoeStandard.entries, form.standard, { it.toDisplayString(i18n = i18n) }, { form = form.copy(standard = it) })
            FormField(form.watts, { form = form.copy(watts = it) }, i18n.text("text.548f9030240c"), error = errors["watts"], kind = FieldKind.DECIMAL)
        }
    }

    badge?.let { t ->
        val b = t.item
        var form by remember(t) { mutableStateOf(BadgeForm.from(b)) }
        val save = rememberEditSave(vm, t)
        val errors = form.errors(i18n = i18n)
        EditScreen(if (b == null) i18n.text("text.66060a3a6be2") else i18n.text("text.6720d0dc04c9"), { badge = null }, {
            val saved = form.toBadge(b, project.id)
            save.save(i18n.text("text.6b86c681c041"), { badge = null }) { if (b == null) ProjectEdits.addDocumentBadge(it, saved) else ProjectEdits.updateDocumentBadge(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TargetPicker(index, form.target, { form = form.copy(target = it) })
            FormField(form.label, { form = form.copy(label = it) }, i18n.text("text.77a1b70aa654"), error = errors["label"])
            EnumPicker(i18n.text("text.54276aa0307f"), BadgeCategory.entries, form.category, { it.toDisplayString(i18n = i18n) }, { form = form.copy(category = it) })
        }
    }
}
