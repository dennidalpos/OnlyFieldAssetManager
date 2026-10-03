package com.onlyfield.assetmanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.Screen
import com.onlyfield.assetmanager.ui.components.*

@Composable
fun ProjectToolsScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val issues by vm.issues.collectAsState()
    var askExportPassword by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf<String?>(null) }
    var managingPassword by remember { mutableStateOf(false) }
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
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(project.description?.ifBlank { null } ?: "Strumenti del progetto", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { vm.navigate(Screen.Issues) }, enabled = issues.isNotEmpty()) { Text("Controllo: $errors errori · $warnings avvisi") }
            }
            items(listOf("Inventario" to Screen.Inventory, "Rack" to Screen.Racks, "Cablaggio" to Screen.Cabling,
                "Rete" to Screen.Network, "Alimentazione" to Screen.Power, "Allegati" to Screen.Attachments,
                "Credenziali" to Screen.Credentials, "Modelli" to Screen.Models, "BU e piani" to Screen.Structure,
                "Cestino" to Screen.Trash, "Documenti" to Screen.Documents)) { (label, screen) ->
                TextButton(onClick = { vm.navigate(screen) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
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
    val index = remember(project) { ProjectIndex(project) }
    val issues by vm.issues.collectAsState()
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
