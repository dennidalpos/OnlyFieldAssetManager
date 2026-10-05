package com.onlyfield.assetmanager.configurator.map

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.TextButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.WanForm
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/**
 * WAN/VPN link seen from [deviceId]: new links put the device on the local side, edits keep its side.
 * Only the far side is editable here; notes and the underlying access are preserved.
 */
@Composable
fun LogicalLinkDialog(project: Project, deviceId: String, existing: WanVpnConnection?, i18n: Messages, onClose: () -> Unit, onSave: (WanVpnConnection) -> Unit) {
    var form by remember(existing) { mutableStateOf(existing?.let(WanForm::from) ?: WanForm(type = WanVpnType.VPN, localDeviceId = deviceId)) }
    var tried by remember { mutableStateOf(false) }
    val local = form.remoteDeviceId != deviceId || form.localDeviceId == deviceId
    val farId = if (local) form.remoteDeviceId else form.localDeviceId
    val farSite = if (local) form.remoteSite else form.localSite
    fun setFar(id: String?, site: String) {
        form = if (local) form.copy(remoteDeviceId = id, remoteSite = site) else form.copy(localDeviceId = id, localSite = site)
    }
    val devices = remember(project, deviceId) {
        project.sites.flatMap { it.devices }.filter { it.id != deviceId }.sortedBy { it.technicalName.lowercase() }.associate { it.id to it.technicalName }
    }
    val errors = form.errors(i18n)
    val nameError = errors["name"]?.takeIf { tried }
    AlertDialog(onDismissRequest = onClose,
        title = { Text(i18n.text(if (existing == null) "text.cfcfd59f1b90" else "text.716af1fcb21e")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(form.name, { form = form.copy(name = it) }, label = { Text(i18n.text("text.656e4a65e6cc")) }, singleLine = true,
                    isError = nameError != null, supportingText = nameError?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
                ValueMenu(i18n.text("text.3868d2843d59"), form.type, WanVpnType.entries, { it.toDisplayString(i18n) }, Modifier.fillMaxWidth()) { form = form.copy(type = it) }
                OutlinedTextField(form.provider, { form = form.copy(provider = it) }, label = { Text(i18n.text("text.ad528b8d1f72")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(form.bandwidth, { form = form.copy(bandwidth = it) }, label = { Text(i18n.text("text.a4ed9939fab6")) },
                    placeholder = { Text(i18n.text("text.b2cbeb3179e9")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ValueMenu(i18n.text("text.c0771cebf192"), farId, listOf<String?>(null) + devices.keys,
                    { id -> id?.let(devices::get) ?: i18n.text("text.65389ba5d2fd") }, Modifier.fillMaxWidth()) { setFar(it, farSite) }
                OutlinedTextField(farSite, { setFar(farId, it) }, label = { Text(i18n.text("text.dac81001f88c")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { tried = true; if (errors.isEmpty()) onSave(form.toConnection(existing)) }) { Text(i18n.text("text.c5997e85ae51")) } },
        dismissButton = { TextButton(onClick = onClose) { Text(i18n.text("ux.cancel")) } })
}
