package com.onlyfield.assetmanager.pc.ui

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
fun CredentialsSection(project: Project, update: (Project, String) -> Unit) {
    val index = remember(project) { ProjectIndex(project) }
    var editing by remember { mutableStateOf<Credential?>(null) }
    var creating by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf<Set<String>>(emptySet()) }
    val changeDetail = LocalDetailChange.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Credenziali", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = { changeDetail { editing = null; creating = true } }) { Text("Aggiungi credenziale") }
        }
        Text("Mai incluse nei documenti esportati", style = MaterialTheme.typography.bodySmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.credentials, key = { it.id }) { credential ->
                ItemCard(title = credential.username, badge = credential.type.toDisplayString(), details = listOf(
                    listOfNotNull(credential.deviceId?.let { index.deviceName(it) }, credential.groupName).joinToString(" · "),
                    if (credential.id in revealed) credential.secret else "••••••••"
                )) {
                    TextButton(onClick = { revealed = if (credential.id in revealed) revealed - credential.id else revealed + credential.id }) { Text(if (credential.id in revealed) "Nascondi" else "Mostra") }
                    EditButton { changeDetail { creating = false; editing = credential } }
                    DeleteButton(credential.username, onDelete = { update(project.copy(credentials = project.credentials.filterNot { it.id == credential.id }), "Credenziale eliminata.") })
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
        EditPanel(if (original == null) "Nuova credenziale" else "Modifica credenziale", { creating = false; editing = null }, {
            val saved = (original ?: Credential(username = username, secret = secret)).copy(username = username.trim(), secret = secret,
                type = type, groupName = group.trim().ifBlank { null }, deviceId = deviceId, notes = notes.trim().ifBlank { null })
            update(project.copy(credentials = if (original == null) project.credentials + saved else project.credentials.map { if (it.id == saved.id) saved else it }), "Credenziale salvata.")
            creating = false; editing = null
        }, confirmEnabled = username.isNotBlank() && secret.isNotEmpty()) {
            FormField(username, { username = it }, "Utente *")
            val dirty = LocalMarkDirty.current
            OutlinedTextField(secret, { dirty(); secret = it }, label = { Text("Password / segreto *") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            EnumPicker("Tipo", CredentialType.entries, type, { it.toDisplayString() }, { type = it })
            DevicePicker("Apparato", index, deviceId, { deviceId = it }, noneLabel = "Credenziale di gruppo")
            FormField(group, { group = it }, "Gruppo")
            FormField(notes, { notes = it }, "Note")
        }
    }
}
