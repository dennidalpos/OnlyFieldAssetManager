package com.onlyfield.assetmanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.Screen
import com.onlyfield.assetmanager.ui.components.*

private data class SectionTile(val icon: String, val title: String, val count: String, val screen: Screen)

@Composable
fun HomeScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val issues by vm.issues.collectAsState()
    val trash by vm.trash.collectAsState()
    val index = remember(project) { ProjectIndex(project) }
    var askExportPassword by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf<String?>(null) }
    var managingPassword by remember { mutableStateOf(false) }
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var addingDevice by remember { mutableStateOf(false) }
    // Same fields as the Inventory search
    val found = if (query.isBlank()) emptyList() else index.devices.filter {
        matchesQuery(query, it.technicalName, it.physicalLabel, it.alias, it.ipAddress, it.macAddress)
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        uri?.let { vm.exportPackage(context.contentResolver, it, exportPassword) }
        exportPassword = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.startImport(context.contentResolver, it) }
    }
    fun startExport() {
        if (project.isPasswordProtected) askExportPassword = true else exportLauncher.launch("${safeFileName(project.name)}.ofam")
    }

    val errors = issues.count { it.severity == ValidationSeverity.STRUCTURAL_ERROR }
    val warnings = issues.size - errors
    val tiles = listOf(
        SectionTile("📦", "Inventario", "${index.devices.size} apparati", Screen.Inventory),
        SectionTile("🗄️", "Rack", "${project.racks.size}", Screen.Racks),
        SectionTile("🔌", "Cablaggio", "${project.cables.size} cavi", Screen.Cabling),
        SectionTile("🌐", "Rete", "${project.vlans.size} VLAN", Screen.Network),
        SectionTile("⚡", "Alimentazione", "${project.powerFeeds.size} linee", Screen.Power),
        SectionTile("🗺️", "Planimetrie", "${project.floorplanPlacements.size} elementi", Screen.Floorplan),
        SectionTile("📎", "Allegati", "${project.attachments.size}", Screen.Attachments),
        SectionTile("🔑", "Credenziali", "${project.credentials.size}", Screen.Credentials),
        SectionTile("📐", "Modelli", "${project.deviceModels.size}", Screen.Models),
        SectionTile("🏢", "Sedi e aree", "${index.areas.size} aree", Screen.Structure),
        SectionTile("🗑️", "Cestino", "${trash.size}", Screen.Trash),
        SectionTile("📄", "Documenti", "PDF, Excel, stampa", Screen.Documents),
    )

    AppScaffold(
        title = project.name,
        subtitle = if (project.isPasswordProtected) "🔒 Protetto da password" else "Salvataggio automatico",
        onBack = { vm.back() },
        snackbarHost = snackbar,
        busy = vm.busy,
        actions = {
            TextButton(onClick = ::startExport) { Text("Esporta") }
            OverflowMenu(
                listOf(
                    MenuAction("Importa .ofam…") { importLauncher.launch(arrayOf("*/*")) },
                    MenuAction(if (project.isPasswordProtected) "Cambia password…" else "Proteggi con password…") { managingPassword = true },
                    MenuAction("Chiudi progetto") { vm.closeProject() },
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchField(query, { query = it }, "Cerca apparato: nome, IP, etichetta, alias…")
            }
            if (query.isNotBlank()) {
                if (found.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Nessun apparato trovato.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(found, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { d ->
                    ItemCard(
                        title = d.technicalName + (d.alias?.let { " ($it)" } ?: ""),
                        details = listOf(listOfNotNull(d.ipAddress, d.physicalLabel, d.areaId?.let { index.areaName(it) }).joinToString(" · ")).filter { it.isNotBlank() },
                        onClick = { vm.navigate(Screen.DeviceDetail(d.id)) }
                    )
                }
                return@LazyVerticalGrid
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { addingDevice = true }) { Text("+ Aggiungi apparato") }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = issues.isNotEmpty()) { vm.navigate(Screen.Issues) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (errors > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(project.description?.ifBlank { null } ?: "Nessuna descrizione", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            when {
                                issues.isEmpty() -> "✓ Nessun problema rilevato"
                                else -> "$errors errori strutturali · $warnings avvisi documentali — tocca per i dettagli"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            items(tiles) { tile ->
                Card(modifier = Modifier.fillMaxWidth().clickable { vm.navigate(tile.screen) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tile.icon, style = MaterialTheme.typography.headlineSmall)
                        Text(tile.title, fontWeight = FontWeight.SemiBold)
                        Text(tile.count, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (askExportPassword) {
        var pwd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askExportPassword = false },
            title = { Text("Esporta pacchetto cifrato") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Il progetto è protetto: il pacchetto verrà cifrato con la password del progetto.")
                    OutlinedTextField(pwd, { pwd = it }, label = { Text("Password del progetto") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(enabled = pwd.isNotEmpty(), onClick = {
                    askExportPassword = false
                    exportPassword = pwd
                    exportLauncher.launch("${safeFileName(project.name)}.ofam")
                }) { Text("Scegli destinazione…") }
            },
            dismissButton = { TextButton(onClick = { askExportPassword = false }) { Text("Annulla") } }
        )
    }

    if (managingPassword) PasswordDialog(vm, project) { managingPassword = false }
    if (addingDevice) DeviceDialog(vm, project, index, null) { addingDevice = false }
}

@Composable
private fun PasswordDialog(vm: ProjectViewModel, project: Project, onClose: () -> Unit) {
    val protected = project.isPasswordProtected
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val mismatch = newPassword.isNotEmpty() && confirm.isNotEmpty() && newPassword != confirm

    @Composable
    fun Pwd(value: String, onChange: (String) -> Unit, label: String, isError: Boolean = false) = OutlinedTextField(
        value, { onChange(it); error = null }, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(),
        singleLine = true, isError = isError, modifier = Modifier.fillMaxWidth()
    )

    FormDialog(
        title = if (protected) "Password del progetto" else "Proteggi con password",
        onDismiss = onClose,
        onConfirm = {
            vm.changePassword(current, newPassword) { err -> if (err == null) onClose() else error = err }
        },
        confirmEnabled = !mismatch && (if (protected) current.isNotEmpty() else newPassword.isNotEmpty() && confirm == newPassword),
        confirmLabel = if (protected && newPassword.isEmpty()) "Rimuovi password" else "Salva"
    ) {
        Text("La password protegge l'apertura del progetto e cifra i pacchetti esportati. Non è recuperabile.", style = MaterialTheme.typography.bodySmall)
        if (protected) Pwd(current, { current = it }, "Password attuale *")
        Pwd(newPassword, { newPassword = it }, if (protected) "Nuova password (vuota per rimuoverla)" else "Nuova password *")
        Pwd(confirm, { confirm = it }, "Conferma nuova password", isError = mismatch)
        if (mismatch) Text("Le password non coincidono.", color = MaterialTheme.colorScheme.error)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun IssuesScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val issues by vm.issues.collectAsState()
    val index = remember(project) { ProjectIndex(project) }
    AppScaffold("Controllo del progetto", onBack = { vm.back() }, snackbarHost = snackbar) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    "Gli errori strutturali indicano dati incoerenti; gli avvisi documentali informazioni mancanti. Nessuno dei due blocca il salvataggio.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            listOf(ValidationSeverity.STRUCTURAL_ERROR, ValidationSeverity.DOCUMENTARY_WARNING).forEach { severity ->
                val list = issues.filter { it.severity == severity }
                if (list.isNotEmpty()) {
                    item { SectionTitle("${severity.toDisplayString()} (${list.size})") }
                    items(list) { issue ->
                        ItemCard(title = index.entityName(issue.targetEntityId) ?: "Progetto", details = listOf(issue.message))
                    }
                }
            }
        }
    }
}
