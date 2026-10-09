package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.configurator.SecondaryModule
import com.onlyfield.assetmanager.configurator.visibleTabs
import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.pc.LocalMessages

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
fun NetworkLogicalSection(project: Project, onProjectUpdated: (Project, String) -> Unit, showSecondary: Boolean = false, saveError: () -> String? = { null }) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val labels = listOf(
            i18n.text("text.474ee27d6727", project.vlans.size),
            i18n.text("text.348cff30ff92", project.subnets.size),
            i18n.text("text.56518f5f56d4", project.logicalInterfaces.size),
            i18n.text("text.05845d50b58c", project.wanVpnConnections.size),
            i18n.text("text.469e8a3048fe", project.deviceConfigurations.size),
            i18n.text("text.36d5a76a9cc7", project.customExtraFields.size)
        )
        // Configurations are an optional module: no tab until used or "Other modules" is open.
        val shown = visibleTabs(labels.size, project, showSecondary, mapOf(4 to SecondaryModule.CONFIGURATIONS))
        if (tab !in shown) tab = shown.first()
        SubTabs(shown.map(labels::get), shown.indexOf(tab)) { tab = shown[it] }
        when (tab) {
            0 -> VlanTab(project, onProjectUpdated, saveError)
            1 -> SubnetTab(project, onProjectUpdated, saveError)
            2 -> InterfacesTab(project, index, onProjectUpdated, saveError)
            3 -> WanTab(project, index, onProjectUpdated, saveError)
            4 -> ConfigsTab(project, index, onProjectUpdated, saveError)
            5 -> ExtraFieldsTab(project, index, onProjectUpdated, saveError)
        }
    }
}

private fun vlanLabel(project: Project, vlanNumber: Int?, i18n: Messages = Messages()): String =
    vlanNumber?.let { n -> project.vlans.find { it.vlanId == n }?.let { i18n.text("text.15fd0dfb7614", n, it.name) } ?: i18n.text("text.da4da5c165af", n) } ?: "—"

@Composable
private fun VlanTab(project: Project, onProjectUpdated: (Project, String) -> Unit, saveError: () -> String?) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Vlan?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }
    val vlans = project.vlans.filter { matchesQuery(query, it.vlanId.toString(), it.name, it.description) }.sortedBy { it.vlanId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("VLAN", searchQuery = query, onSearchChange = { query = it }, searchPlaceholder = i18n.text("text.7c4eb2cff540")) {
            Button(onClick = { changeDetail { editing = null; creating = true } }) { Text(i18n.text("text.62ba123ec0bc")) }
        }
        if (vlans.isEmpty()) EmptyState(i18n.text("text.7de621e87842"), actionLabel = i18n.text("text.62ba123ec0bc"), onAction = { changeDetail { editing = null; creating = true } })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vlans, key = { it.id }) { v ->
                val scope = if (v.scopeType == VlanScopeType.PROJECT) i18n.text("text.04b1a5d29d23")
                else "${v.scopeType.toDisplayString(i18n = i18n)}: ${index.entityName(v.scopeTargetId, i18n = i18n) ?: i18n.text("text.c0a49765082f")}"
                ItemCard(title = i18n.text("text.15fd0dfb7614", v.vlanId, v.name), details = listOf(scope, v.description.orEmpty())) {
                    EditButton { changeDetail { creating = false; editing = v } }
                    DeleteButton(i18n.text("text.da4da5c165af", v.vlanId), onDelete = { onProjectUpdated(ProjectEdits.deleteVlan(project, v.id), i18n.text("text.f3832376a5cb", v.vlanId)) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val v = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, v) { mutableStateOf(VlanForm.from(v)) }
        val taken = project.vlans.filter { it.id != v?.id && it.scopeType == VlanScopeType.PROJECT }.map { it.vlanId }.toSet()
        val errors = form.errors(taken, i18n = i18n)
        EditPanel(
            title = if (v == null) i18n.text("text.864bdda898fc") else i18n.text("text.7413bdc75d86"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toVlan(v)
                onProjectUpdated(if (v == null) ProjectEdits.addVlan(project, saved) else ProjectEdits.updateVlan(project, saved), i18n.text("text.9c856115c4fc", saved.vlanId))
                if (saveError() == null) { creating = false; editing = null }
            },
            width = 520.dp
        ) {
            saveError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.vlanId, { form = form.copy(vlanId = it) }, i18n.text("text.21d0e8b7d284"), Modifier.weight(0.6f), errors["vlanId"], hint = "1–4094")
                FormField(form.name, { form = form.copy(name = it) }, i18n.text("text.2e245546ff59"), Modifier.weight(1f), errors["name"])
            }
            EnumPicker(i18n.text("text.03cbc24f25f2"), VlanScopeType.entries, form.scopeType, { it.toDisplayString(i18n = i18n) }, { form = form.copy(scopeType = it, scopeTargetId = null) })
            when (form.scopeType) {
                VlanScopeType.SITE -> OptionPicker(i18n.text("text.e4de7d26b141"), project.sites, project.sites.find { it.id == form.scopeTargetId }, { it.name }, { form = form.copy(scopeTargetId = it?.id) })
                VlanScopeType.DEVICE -> DevicePicker(i18n.text("text.cf301d95d32c"), index, form.scopeTargetId, { form = form.copy(scopeTargetId = it) })
                VlanScopeType.PROJECT -> Unit
            }
            FormField(form.description, { form = form.copy(description = it) }, i18n.text("text.6fb818621896"))
        }
    }
}

@Composable
private fun SubnetTab(project: Project, onProjectUpdated: (Project, String) -> Unit, saveError: () -> String?) {
    val i18n = LocalMessages.current

    var editing by remember { mutableStateOf<Subnet?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.bfea90e5ae18")) { Button(onClick = { changeDetail { editing = null; creating = true } }) { Text(i18n.text("text.976cc683faed")) } }
        if (project.subnets.isEmpty()) EmptyState(i18n.text("text.446498be42fe"), actionLabel = i18n.text("text.976cc683faed"), onAction = { changeDetail { editing = null; creating = true } })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.subnets.sortedForDisplay(i18n) { it.name ?: it.cidrBlock }, key = { it.id }) { s ->
                val vlan = project.vlans.find { it.id == s.vlanId }
                ItemCard(
                    title = s.cidrBlock + (s.name?.let { " · $it" } ?: ""),
                    details = listOf(
                        listOfNotNull(s.gatewayIp?.let { i18n.text("text.87b3d4c8eb9f", it) }, vlan?.let { i18n.text("text.15fd0dfb7614", it.vlanId, it.name) }).joinToString(" · "),
                        s.description.orEmpty()
                    )
                ) {
                    EditButton { changeDetail { creating = false; editing = s } }
                    DeleteButton(s.cidrBlock, onDelete = { onProjectUpdated(ProjectEdits.deleteSubnet(project, s.id), i18n.text("text.e85fcab6d72c", s.cidrBlock)) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val s = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, s) { mutableStateOf(SubnetForm.from(s)) }
        val errors = form.errors(i18n = i18n)
        EditPanel(
            title = if (s == null) i18n.text("text.5c8aa6f5ef27") else i18n.text("text.0db1e97aa029"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toSubnet(s)
                onProjectUpdated(if (s == null) ProjectEdits.addSubnet(project, saved) else ProjectEdits.updateSubnet(project, saved), i18n.text("text.dabaa46695cb", saved.cidrBlock))
                if (saveError() == null) { creating = false; editing = null }
            },
            width = 520.dp
        ) {
            saveError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            FormField(form.cidrBlock, { form = form.copy(cidrBlock = it) }, i18n.text("text.a1f614a904b2"), error = errors["cidrBlock"], hint = i18n.text("text.ee4725fa1904"))
            FormField(form.gatewayIp, { form = form.copy(gatewayIp = it) }, i18n.text("text.41ed52921661"), error = errors["gatewayIp"])
            OptionPicker(
                "VLAN", project.vlans.sortedBy { it.vlanId }, project.vlans.find { it.id == form.vlanRefId },
                { i18n.text("text.15fd0dfb7614", it.vlanId, it.name) }, { form = form.copy(vlanRefId = it?.id) }, sortByName = false, noneLabel = i18n.text("text.b60b955a9981")
            )
            FormField(form.name, { form = form.copy(name = it) }, i18n.text("text.5086900635fe"))
            FormField(form.description, { form = form.copy(description = it) }, i18n.text("text.6fb818621896"))
        }
    }
}

@Composable
private fun InterfacesTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit, saveError: () -> String?) {
    val i18n = LocalMessages.current

    var editing by remember { mutableStateOf<LogicalInterface?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.9800d0f86f5d"), subtitle = i18n.text("text.b9efd4504080")) {
            Button(onClick = { changeDetail { editing = null; creating = true } }, enabled = index.devices.isNotEmpty()) { Text(i18n.text("text.c8edd2f4418c")) }
        }
        if (project.logicalInterfaces.isEmpty()) EmptyState(i18n.text("text.22e22d133b84"))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.logicalInterfaces.sortedForDisplay(i18n) { it.name }, key = { it.id }) { li ->
                ItemCard(
                    title = "${index.deviceName(li.deviceId, i18n.text("text.befe1ad89357"), i18n = i18n)} › ${li.name}",
                    details = listOf(
                        listOfNotNull(li.ipAddress, li.subnetCidr, li.vlanId?.let { vlanLabel(project, it, i18n = i18n) }, if (li.isL3) "L3" else "L2").joinToString(" · "),
                        li.notes.orEmpty()
                    )
                ) {
                    EditButton { changeDetail { creating = false; editing = li } }
                    DeleteButton(li.name, onDelete = { onProjectUpdated(ProjectEdits.deleteLogicalInterface(project, li.id), i18n.text("text.15971cc8f189")) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val li = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, li) { mutableStateOf(LogicalInterfaceForm.from(li)) }
        val errors = form.errors(i18n = i18n)
        EditPanel(
            title = if (li == null) i18n.text("text.2bc2fde3f211") else i18n.text("text.a4d9c2f8f102"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toInterface(li)
                onProjectUpdated(if (li == null) ProjectEdits.addLogicalInterface(project, saved) else ProjectEdits.updateLogicalInterface(project, saved), i18n.text("text.4292a16c17ce"))
                if (saveError() == null) { creating = false; editing = null }
            }
        ) {
            saveError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            DevicePicker(i18n.text("text.e7f2c0e68768"), index, form.deviceId, { form = form.copy(deviceId = it) }, error = errors["deviceId"])
            FormField(form.name, { form = form.copy(name = it) }, i18n.text("text.2e245546ff59"), error = errors["name"], hint = i18n.text("text.0da5b4623535"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.ipAddress, { form = form.copy(ipAddress = it) }, i18n.text("text.ebb396f2d486"), Modifier.weight(1f), errors["ipAddress"])
                FormField(form.subnetCidr, { form = form.copy(subnetCidr = it) }, i18n.text("text.7cfdb8aaa25f"), Modifier.weight(1f), errors["subnetCidr"])
            }
            OptionPicker(
                "VLAN", project.vlans.sortedBy { it.vlanId }, project.vlans.find { it.vlanId == form.vlanId },
                { i18n.text("text.15fd0dfb7614", it.vlanId, it.name) }, { form = form.copy(vlanId = it?.vlanId) }, sortByName = false, noneLabel = i18n.text("text.b60b955a9981")
            )
            FormField(form.macAddress, { form = form.copy(macAddress = it) }, "MAC", error = errors["macAddress"])
            LabeledCheckbox(form.isL3, { form = form.copy(isL3 = it) }, i18n.text("text.1a4ad0ed2912"))
            FormField(form.notes, { form = form.copy(notes = it) }, i18n.text("text.d8da2c49df39"), singleLine = false)
        }
    }
}

@Composable
private fun WanTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit, saveError: () -> String?) {
    val i18n = LocalMessages.current

    var editing by remember { mutableStateOf<WanVpnConnection?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.04bc126d165e")) { Button(onClick = { changeDetail { editing = null; creating = true } }) { Text(i18n.text("text.da56a933d02e")) } }
        if (project.wanVpnConnections.isEmpty()) EmptyState(i18n.text("text.acd5d0fdeec2"), actionLabel = i18n.text("text.da56a933d02e"), onAction = { changeDetail { editing = null; creating = true } })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.wanVpnConnections.sortedForDisplay(i18n) { it.name }, key = { it.id }) { c ->
                val local = c.localEndpointDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: c.localEndpointSiteDescription ?: "?"
                val remote = c.remoteEndpointDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: c.remoteEndpointSiteDescription ?: "?"
                ItemCard(
                    title = c.name,
                    badge = c.type.toDisplayString(i18n = i18n),
                    details = listOf("$local → $remote", listOfNotNull(c.providerOrCarrier, c.bandwidth).joinToString(" · "), c.notes.orEmpty())
                ) {
                    EditButton { changeDetail { creating = false; editing = c } }
                    DeleteButton(c.name, onDelete = { onProjectUpdated(ProjectEdits.deleteWanVpnConnection(project, c.id), i18n.text("text.36f755a3cda8")) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val c = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, c) { mutableStateOf(WanForm.from(c)) }
        val errors = form.errors(i18n = i18n)
        EditPanel(
            title = if (c == null) i18n.text("text.cfcfd59f1b90") else i18n.text("text.716af1fcb21e"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toConnection(c)
                onProjectUpdated(if (c == null) ProjectEdits.addWanVpnConnection(project, saved) else ProjectEdits.updateWanVpnConnection(project, saved), i18n.text("text.ec170e822ebb"))
                if (saveError() == null) { creating = false; editing = null }
            },
            width = 640.dp
        ) {
            saveError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.name, { form = form.copy(name = it) }, i18n.text("text.656e4a65e6cc"), Modifier.weight(1.4f), errors["name"])
                EnumPicker(i18n.text("text.3868d2843d59"), WanVpnType.entries, form.type, { it.toDisplayString(i18n = i18n) }, { form = form.copy(type = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.provider, { form = form.copy(provider = it) }, i18n.text("text.ad528b8d1f72"), Modifier.weight(1f))
                FormField(form.bandwidth, { form = form.copy(bandwidth = it) }, i18n.text("text.a4ed9939fab6"), Modifier.weight(1f), hint = i18n.text("text.b2cbeb3179e9"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevicePicker(i18n.text("text.588719de20b9"), index, form.localDeviceId, { form = form.copy(localDeviceId = it) }, Modifier.weight(1f), noneLabel = i18n.text("text.65389ba5d2fd"))
                FormField(form.localSite, { form = form.copy(localSite = it) }, i18n.text("text.8ebb949349c6"), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevicePicker(i18n.text("text.c0771cebf192"), index, form.remoteDeviceId, { form = form.copy(remoteDeviceId = it) }, Modifier.weight(1f), noneLabel = i18n.text("text.65389ba5d2fd"))
                FormField(form.remoteSite, { form = form.copy(remoteSite = it) }, i18n.text("text.dac81001f88c"), Modifier.weight(1f))
            }
            FormField(form.notes, { form = form.copy(notes = it) }, i18n.text("text.d8da2c49df39"), singleLine = false)
        }
    }
}

@Composable
private fun ConfigsTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit, saveError: () -> String?) {
    val i18n = LocalMessages.current

    var editing by remember { mutableStateOf<DeviceConfiguration?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.acac03d2f58d"), subtitle = i18n.text("text.36f971d40c96")) {
            Button(onClick = { changeDetail { editing = null; creating = true } }, enabled = index.devices.isNotEmpty()) { Text(i18n.text("text.d940cb0bf5d2")) }
        }
        if (project.deviceConfigurations.isEmpty()) EmptyState(i18n.text("text.c15c34e10908"))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.deviceConfigurations.sortedForDisplay(i18n) { it.title }, key = { it.id }) { cfg ->
                ItemCard(
                    title = "${index.deviceName(cfg.deviceId, i18n.text("text.befe1ad89357"), i18n = i18n)} › ${cfg.title}",
                    details = listOf(cfg.configText?.lines()?.firstOrNull { it.isNotBlank() }?.take(120).orEmpty(), i18n.text("text.e9b35f11a28b", cfg.configText?.lines()?.size ?: 0))
                ) {
                    EditButton { changeDetail { creating = false; editing = cfg } }
                    DeleteButton(cfg.title, onDelete = { onProjectUpdated(ProjectEdits.deleteDeviceConfiguration(project, cfg.id), i18n.text("text.fb507a93e55d")) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val cfg = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, cfg) { mutableStateOf(DeviceConfigForm.from(cfg)) }
        val errors = form.errors(i18n = i18n)
        EditPanel(
            title = if (cfg == null) i18n.text("text.66b4e494cd62") else i18n.text("text.dfff9fc66812"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toConfig(cfg)
                onProjectUpdated(if (cfg == null) ProjectEdits.addDeviceConfiguration(project, saved) else ProjectEdits.updateDeviceConfiguration(project, saved), i18n.text("text.b2ed6f265907"))
                if (saveError() == null) { creating = false; editing = null }
            },
            width = 760.dp
        ) {
            saveError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevicePicker(i18n.text("text.e7f2c0e68768"), index, form.deviceId, { form = form.copy(deviceId = it) }, Modifier.weight(1f), error = errors["deviceId"])
                FormField(form.title, { form = form.copy(title = it) }, i18n.text("text.b03a74353bbd"), Modifier.weight(1f), errors["title"], hint = i18n.text("text.30da32455b47"))
            }
            val markDirty = LocalMarkDirty.current
            OutlinedTextField(
                value = form.configText,
                onValueChange = { markDirty(); form = form.copy(configText = it) },
                label = { Text(i18n.text("text.ae44cab9c2ac")) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                minLines = 12,
                modifier = Modifier.fillMaxWidth()
            )
            FormField(form.notes, { form = form.copy(notes = it) }, i18n.text("text.d8da2c49df39"))
        }
    }
}

@Composable
private fun ExtraFieldsTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit, saveError: () -> String?) {
    val i18n = LocalMessages.current

    var editing by remember { mutableStateOf<CustomExtraField?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.00158c772cda"), subtitle = i18n.text("text.1641198a6b14")) {
            Button(onClick = { changeDetail { editing = null; creating = true } }) { Text(i18n.text("text.68e7427942cd")) }
        }
        if (project.customExtraFields.isEmpty()) EmptyState(i18n.text("text.ca56999e82d8"), actionLabel = i18n.text("text.68e7427942cd"), onAction = { changeDetail { editing = null; creating = true } })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.customExtraFields.sortedForDisplay(i18n) { it.fieldKey }, key = { it.id }) { f ->
                ItemCard(
                    title = "${f.fieldKey}: ${f.fieldValue}",
                    badge = f.classification.takeIf { it != AttachmentClassification.SHAREABLE }?.toDisplayString(i18n = i18n),
                    details = listOf(index.targetLabel(f.targetType, f.targetId, i18n = i18n), f.fieldType.toDisplayString(i18n = i18n))
                ) {
                    EditButton { changeDetail { creating = false; editing = f } }
                    DeleteButton(f.fieldKey, onDelete = { onProjectUpdated(ProjectEdits.deleteCustomExtraField(project, f.id), i18n.text("text.b43abaee4bcd")) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val f = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, f) { mutableStateOf(ExtraFieldForm.from(f)) }
        val errors = form.errors(i18n = i18n)
        EditPanel(
            title = if (f == null) i18n.text("text.84b0d716f809") else i18n.text("text.d6c18565dd61"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toField(f, project.id)
                onProjectUpdated(if (f == null) ProjectEdits.addCustomExtraField(project, saved) else ProjectEdits.updateCustomExtraField(project, saved), i18n.text("text.b24051277c7c"))
                if (saveError() == null) { creating = false; editing = null }
            },
            width = 620.dp
        ) {
            saveError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TargetPicker(index, form.target, { form = form.copy(target = it) }, errors["target"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.key, { form = form.copy(key = it) }, i18n.text("text.4702cf978b67"), Modifier.weight(1f), errors["key"], hint = i18n.text("text.08d801a474bf"))
                EnumPicker(i18n.text("text.3868d2843d59"), CustomFieldType.entries, form.fieldType, { it.toDisplayString(i18n = i18n) }, { form = form.copy(fieldType = it) }, Modifier.weight(0.6f))
            }
            FormField(form.value, { form = form.copy(value = it) }, i18n.text("text.3b50ed0e6ec2"), error = errors["value"])
            EnumPicker(i18n.text("text.57fbd1029ff6"), AttachmentClassification.entries, form.classification, { it.toDisplayString(i18n = i18n) }, { form = form.copy(classification = it) })
        }
    }
}
