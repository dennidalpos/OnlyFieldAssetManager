package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.components.*

@Composable
fun ProjectsScreen(vm: ProjectViewModel, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val context = LocalContext.current
    val confirm = LocalConfirm.current
    val projects by vm.projects.collectAsState()
    var renaming by remember { mutableStateOf<ProjectEntity?>(null) }
    var unlocking by remember { mutableStateOf<ProjectEntity?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.startImport(context.contentResolver, it) }
    }

    AppScaffold(
        title = "OnlyField Asset Manager",
        subtitle = i18n.text("text.3d46bfbe7868"),
        onBack = null,
        snackbarHost = snackbar,
        busy = vm.busy,
        actions = { LanguagePicker(vm); TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text(i18n.text("text.a53503bbd801")) } },
        floatingActionButton = {
            if (projects.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = vm::startNewSite, icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.ac667fe865c9")) })
            }
        }
    ) { padding ->
        val open = { p: ProjectEntity -> if (p.isPasswordProtected) unlocking = p else vm.openProject(p.id) }
        if (projects.isEmpty()) {
            StartActions(
                onNewSite = vm::startNewSite,
                onImport = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.padding(padding)
            )
        } else LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Projects are ordered by last change: the first one is where the user left off.
            item(key = "continue") {
                val last = projects.first()
                Button(onClick = { open(last) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(i18n.text("text.d5555c9b8e0b", last.name), style = MaterialTheme.typography.titleMedium)
                }
            }
            item(key = "header") {
                Text(i18n.text("text.8102b2c0ceb4"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            }
            items(projects, key = { it.id }) { p ->
                ItemCard(
                    title = p.name,
                    badge = if (p.isPasswordProtected) i18n.text("text.a73763f25907") else null,
                    details = listOf(p.description.orEmpty(), i18n.text("text.c95c16b49c20", formatDateTime(p.updatedEpochMs))),
                    onClick = { open(p) },
                    menu = listOf(
                        MenuAction(i18n.text("text.98b79b084f23")) { renaming = p },
                        MenuAction(i18n.text("text.7efe336bd548"), destructive = true) {
                            confirm(ConfirmRequest(i18n.text("text.e36c23dfb086", p.name), i18n.text("text.548a4a17a081")) {
                                vm.deleteProject(p.id)
                            })
                        }
                    )
                )
            }
        }
    }

    renaming?.let { p ->
        var name by remember(p) { mutableStateOf(p.name) }
        FormDialog(i18n.text("text.62e351b7ecb0"), { renaming = null }, { renaming = null; vm.renameProject(p.id, name) }, confirmEnabled = name.isNotBlank()) {
            FormField(name, { name = it }, i18n.text("text.2e245546ff59"))
        }
    }

    unlocking?.let { p ->
        var password by remember(p) { mutableStateOf("") }
        var wrong by remember(p) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { unlocking = null },
            title = { Text(i18n.text("text.dc30ca04bf34", p.name)) },
            text = {
                OutlinedTextField(
                    value = password, onValueChange = { password = it; wrong = false }, label = { Text(i18n.text("text.e7cf3ef4f17c")) },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true, isError = wrong,
                    supportingText = if (wrong) { { Text(i18n.text("text.c409f4b96322")) } } else null
                )
            },
            confirmButton = {
                TextButton(enabled = password.isNotEmpty(), onClick = {
                    vm.openProtectedProject(p.id, password) { wrong = true }
                }) { Text(i18n.text("text.12abcf9ee7d6")) }
            },
            dismissButton = { TextButton(onClick = { unlocking = null }) { Text(i18n.text("text.18c9d912a210")) } }
        )
    }
}

/** First launch: the two ways to get a project onto the phone. */
@Composable
private fun StartActions(onNewSite: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    val i18n = LocalMessages.current

    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(i18n.text("text.adb53755cdc3"), style = MaterialTheme.typography.headlineMedium)
        Text(
            i18n.text("text.55f623cd4bf4"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onNewSite, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text(i18n.text("text.7559294aebf8"), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text(i18n.text("text.b93f2228bcd7"), style = MaterialTheme.typography.titleMedium)
        }
    }
}
