package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.configurator.SecondaryModule
import com.onlyfield.assetmanager.configurator.shown
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.ui.LocalMessages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.configurator.ProjectDestination
import com.onlyfield.assetmanager.configurator.SymbolIcons
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
    val i18n = LocalMessages.current

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
        subtitle = if (project.isPasswordProtected) i18n.text("text.8c42691cd614") else i18n.text("text.b1f6bb96d793"),
        onBack = null,
        snackbarHost = snackbar,
        busy = vm.busy,
        actions = { LanguagePicker(vm) }
    ) { padding ->
        // Everything the bottom bar does not reach, grouped, with project actions last.
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            project.description?.ifBlank { null }?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp)) } }
            if (issues.isNotEmpty()) item {
                ListItem(headlineContent = { Text(i18n.text("text.48139e9146f0", errors, warnings)) },
                    leadingContent = { Icon(SymbolIcons.description, null, tint = if (errors > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary) },
                    modifier = Modifier.fillMaxWidth().clickable { vm.navigate(Screen.Issues) })
            }
            ProjectDestination.entries.filterNot { it.primary }.filter { it.shown(project, vm.showSecondary) }.groupBy { it.groupKey }.forEach { (group, destinations) ->
                item { GroupHeader(i18n.text(group)) }
                items(destinations) { destination ->
                    val screen = when (destination) {
                        ProjectDestination.NETWORK -> Screen.Network
                        ProjectDestination.POWER -> Screen.Power
                        ProjectDestination.MODELS -> Screen.Models
                        ProjectDestination.ATTACHMENTS -> Screen.Attachments
                        ProjectDestination.CREDENTIALS -> Screen.Credentials
                        ProjectDestination.DOCUMENTS -> Screen.Documents
                        ProjectDestination.PROJECT -> Screen.Structure
                        ProjectDestination.TRASH -> Screen.Trash
                        else -> Screen.Home
                    }
                    ListItem(headlineContent = { Text(destination.title(i18n)) }, leadingContent = { Icon(destination.icon, null) },
                        modifier = Modifier.fillMaxWidth().clickable { vm.navigate(screen) })
                }
            }
            // Unused optional modules stay behind one entry.
            val hidden = SecondaryModule.hidden(project)
            if (hidden.isNotEmpty()) item {
                ListItem(headlineContent = { Text(i18n.text(if (vm.showSecondary) "nav.lessModules" else "nav.moreModules")) },
                    supportingContent = { Text(hidden.joinToString(", ") { i18n.text(it.labelKey) }) },
                    leadingContent = { Icon(SymbolIcons.category, null) },
                    modifier = Modifier.fillMaxWidth().clickable { vm.showSecondary = !vm.showSecondary })
            }
            item { GroupHeader(i18n.text("ux.nav.actions")) }
            item { ActionItem(i18n.text("text.2c4c51a93ca7"), ::startExport) }
            item { ActionItem(i18n.text("text.a6afc0c52be6")) { importLauncher.launch(arrayOf("*/*")) } }
            item { ActionItem(if (project.isPasswordProtected) i18n.text("text.e7ce0854e521") else i18n.text("text.07298c58b48f")) { managingPassword = true } }
            item { ActionItem(i18n.text("text.c00df9e3726e")) { vm.closeProject() } }
        }
    }

    if (askExportPassword) {
        var pwd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askExportPassword = false },
            title = { Text(i18n.text("text.e4b38d139b28")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(i18n.text("text.4cca746f444e"))
                    OutlinedTextField(pwd, { pwd = it }, label = { Text(i18n.text("text.f6a32b19c4b1")) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(enabled = pwd.isNotEmpty(), onClick = {
                    askExportPassword = false
                    exportPassword = pwd
                    exportLauncher.launch("${safeFileName(project.name)}.ofam")
                }) { Text(i18n.text("text.be66f958b50a")) }
            },
            dismissButton = { TextButton(onClick = { askExportPassword = false }) { Text(i18n.text("text.18c9d912a210")) } }
        )
    }

    if (managingPassword) PasswordDialog(vm, project) { managingPassword = false }
}

@Composable
private fun PasswordDialog(vm: ProjectViewModel, project: Project, onClose: () -> Unit) {
    val i18n = LocalMessages.current

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
        title = if (protected) i18n.text("text.f6a32b19c4b1") else i18n.text("text.77374a369396"),
        onDismiss = onClose,
        onConfirm = {
            vm.changePassword(current, newPassword) { err -> if (err == null) onClose() else error = err }
        },
        confirmEnabled = !mismatch && (if (protected) current.isNotEmpty() else newPassword.isNotEmpty() && confirm == newPassword),
        confirmLabel = if (protected && newPassword.isEmpty()) i18n.text("text.01f6d7886781") else i18n.text("text.c5997e85ae51")
    ) {
        Text(i18n.text("text.4652bd354b32"), style = MaterialTheme.typography.bodySmall)
        if (protected) Pwd(current, { current = it }, i18n.text("text.9300c9cacb41"))
        Pwd(newPassword, { newPassword = it }, if (protected) i18n.text("text.7dc14b395c9e") else i18n.text("text.b791727eacc1"))
        Pwd(confirm, { confirm = it }, i18n.text("text.8499e9c5410a"), isError = mismatch)
        if (mismatch) Text(i18n.text("text.f73c1f4c5d77"), color = MaterialTheme.colorScheme.error)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun IssuesScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    val issues by vm.issues.collectAsState()
    AppScaffold(i18n.text("text.a590fcd25b56"), onBack = { vm.back() }, snackbarHost = snackbar) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    i18n.text("text.98071310d496"),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            listOf(ValidationSeverity.STRUCTURAL_ERROR, ValidationSeverity.DOCUMENTARY_WARNING).forEach { severity ->
                val list = issues.filter { it.severity == severity }
                if (list.isNotEmpty()) {
                    item { SectionTitle("${severity.toDisplayString(i18n = i18n)} (${list.size})") }
                    items(list) { issue ->
                        ItemCard(title = index.entityName(issue.targetEntityId, i18n = i18n) ?: i18n.text("text.b7700d71d0ce"), details = listOf(issue.message))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(text: String) =
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp))

@Composable
private fun ActionItem(label: String, onClick: () -> Unit) =
    ListItem(headlineContent = { Text(label) }, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick))
