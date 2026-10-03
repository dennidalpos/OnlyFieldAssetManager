package com.onlyfield.assetmanager.ui.screens

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
        subtitle = "Progetti su questo dispositivo",
        onBack = null,
        snackbarHost = snackbar,
        busy = vm.busy,
        actions = { TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text("Importa .ofam") } },
        floatingActionButton = {
            if (projects.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = vm::startNewSite, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Nuovo sito") })
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
                    Text("Continua: «${last.name}»", style = MaterialTheme.typography.titleMedium)
                }
            }
            item(key = "header") {
                Text("Tutti i progetti", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            }
            items(projects, key = { it.id }) { p ->
                ItemCard(
                    title = p.name,
                    badge = if (p.isPasswordProtected) "🔒 Protetto" else null,
                    details = listOf(p.description.orEmpty(), "Modificato ${formatDateTime(p.updatedEpochMs)}"),
                    onClick = { open(p) },
                    menu = listOf(
                        MenuAction("Rinomina") { renaming = p },
                        MenuAction("Elimina", destructive = true) {
                            confirm(ConfirmRequest("Eliminare «${p.name}»?", "Il progetto e tutti i suoi dati verranno eliminati da questo dispositivo. Esporta prima un pacchetto .ofam se ti serve una copia.") {
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
        FormDialog("Rinomina progetto", { renaming = null }, { renaming = null; vm.renameProject(p.id, name) }, confirmEnabled = name.isNotBlank()) {
            FormField(name, { name = it }, "Nome *")
        }
    }

    unlocking?.let { p ->
        var password by remember(p) { mutableStateOf("") }
        var wrong by remember(p) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { unlocking = null },
            title = { Text("«${p.name}» è protetto") },
            text = {
                OutlinedTextField(
                    value = password, onValueChange = { password = it; wrong = false }, label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true, isError = wrong,
                    supportingText = if (wrong) { { Text("Password errata.") } } else null
                )
            },
            confirmButton = {
                TextButton(enabled = password.isNotEmpty(), onClick = {
                    vm.openProtectedProject(p.id, password) { wrong = true }
                }) { Text("Apri") }
            },
            dismissButton = { TextButton(onClick = { unlocking = null }) { Text("Annulla") } }
        )
    }
}

/** First launch: the two ways to get a project onto the phone. */
@Composable
private fun StartActions(onNewSite: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text("Benvenuto", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Censisci un sito partendo da zero, oppure apri il progetto che hai ricevuto dal PC o da un collega.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onNewSite, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("Inizia un nuovo sito", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("Apri un pacchetto ricevuto (.ofam)", style = MaterialTheme.typography.titleMedium)
        }
    }
}
