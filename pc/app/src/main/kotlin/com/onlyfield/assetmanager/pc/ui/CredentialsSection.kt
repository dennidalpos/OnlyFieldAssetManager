package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.ui.components.*

@Composable
fun CredentialsSection(project: Project, update: (Project, String) -> Unit) = CredentialsSection(project, update, { null })

@Composable
fun CredentialsSection(project: Project, update: (Project, String) -> Unit, saveError: () -> String?) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    var editing by remember { mutableStateOf<Credential?>(null) }
    var creating by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf<Set<String>>(emptySet()) }
    val changeDetail = LocalDetailChange.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(i18n.text("text.52f7e6721e97"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = { changeDetail { editing = null; creating = true } }) { Text(i18n.text("text.5477fceee15e")) }
        }
        Text(i18n.text("text.ac82e5f35ae8"), style = MaterialTheme.typography.bodySmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.credentials.sortedForDisplay(i18n) { "${it.groupName.orEmpty()} ${it.username}" }, key = { it.id }) { credential ->
                ItemCard(title = credential.username, badge = credential.type.toDisplayString(i18n = i18n), details = listOf(
                    listOfNotNull(credential.deviceId?.let { index.deviceName(it, i18n = i18n) }, credential.groupName).joinToString(" · "),
                    if (credential.id in revealed) credential.secret else "••••••••"
                )) {
                    TextButton(onClick = { revealed = if (credential.id in revealed) revealed - credential.id else revealed + credential.id }) { Text(if (credential.id in revealed) i18n.text("text.12292af0c960") else i18n.text("text.1ca58b09d17b")) }
                    EditButton { changeDetail { creating = false; editing = credential } }
                    DeleteButton(credential.username, onDelete = { update(project.copy(credentials = project.credentials.filterNot { it.id == credential.id }), i18n.text("text.dc89060a34c7")) })
                }
            }
        }
    }
    if (creating || editing != null) {
        val original = editing
        val version = LocalDetailSlot.current?.editorVersion
        var username by remember(version, original) { mutableStateOf(original?.username.orEmpty()) }
        var secret by remember(version, original) { mutableStateOf(original?.secret.orEmpty()) }
        var type by remember(version, original) { mutableStateOf(original?.type ?: CredentialType.PASSWORD) }
        var group by remember(version, original) { mutableStateOf(original?.groupName.orEmpty()) }
        var deviceId by remember(version, original) { mutableStateOf(original?.deviceId) }
        var notes by remember(version, original) { mutableStateOf(original?.notes.orEmpty()) }
        EditPanel(if (original == null) i18n.text("text.daf354006859") else i18n.text("text.81dda9b20962"), { creating = false; editing = null }, {
            val saved = (original ?: Credential(username = username, secret = secret)).copy(username = username.trim(), secret = secret,
                type = type, groupName = group.trim().ifBlank { null }, deviceId = deviceId, notes = notes.trim().ifBlank { null })
            update(project.copy(credentials = if (original == null) project.credentials + saved else project.credentials.map { if (it.id == saved.id) saved else it }), i18n.text("text.de37f6b34612"))
            if (saveError() == null) { creating = false; editing = null }
        }, confirmEnabled = username.isNotBlank() && secret.isNotEmpty()) {
            saveError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            FormField(username, { username = it }, i18n.text("text.3255e3d5e3b4"))
            val dirty = LocalMarkDirty.current
            OutlinedTextField(secret, { dirty(); secret = it }, label = { Text(i18n.text("text.7f9bedb6b654")) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            EnumPicker(i18n.text("text.3868d2843d59"), CredentialType.entries, type, { it.toDisplayString(i18n = i18n) }, { type = it })
            DevicePicker(i18n.text("text.cf301d95d32c"), index, deviceId, { deviceId = it }, noneLabel = i18n.text("text.ab02eb85c3ac"))
            FormField(group, { group = it }, i18n.text("text.b9bb40edbe6e"))
            FormField(notes, { notes = it }, i18n.text("text.d8da2c49df39"))
        }
    }
}
