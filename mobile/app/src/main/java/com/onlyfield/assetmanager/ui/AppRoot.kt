package com.onlyfield.assetmanager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import com.onlyfield.assetmanager.exchange.MergeSide
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.ui.components.ConfirmHost
import com.onlyfield.assetmanager.ui.screens.*
import com.onlyfield.assetmanager.ui.theme.OnlyFieldTheme

/** Root of the UI: routes the current [Screen], shows snackbars and the import flow. */
@Composable
fun AppRoot(vm: ProjectViewModel, onExit: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val project by vm.project.collectAsState()
    val importState by vm.importState.collectAsState()

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = msg.text,
                actionLabel = if (msg.undo != null) "Annulla" else null,
                withDismissAction = msg.isError,
                duration = if (msg.isError) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) msg.undo?.invoke()
        }
    }

    BackHandler { if (!vm.back()) onExit() }

    OnlyFieldTheme {
        ConfirmHost {
            Surface {
                val screen = vm.currentScreen
                val p = project
                if (screen == Screen.NewSite) {
                    NewSiteScreen(vm, snackbar)
                } else if (p == null || screen == Screen.Projects) {
                    ProjectsScreen(vm, snackbar)
                } else when (screen) {
                    Screen.Home -> FloorHomeScreen(vm, p, snackbar)
                    Screen.ProjectTools -> ProjectToolsScreen(vm, p, snackbar)
                    Screen.Inventory -> InventoryScreen(vm, p, snackbar)
                    is Screen.DeviceDetail -> DeviceDetailScreen(vm, p, screen.deviceId, snackbar)
                    Screen.Structure -> StructureScreen(vm, p, snackbar)
                    Screen.Racks -> RacksScreen(vm, p, snackbar)
                    is Screen.RackDetail -> RackDetailScreen(vm, p, screen.rackId, snackbar)
                    Screen.Models -> ModelsScreen(vm, p, snackbar)
                    Screen.Cabling -> CablingScreen(vm, p, snackbar)
                    Screen.Network -> NetworkScreen(vm, p, snackbar)
                    Screen.Power -> PowerScreen(vm, p, snackbar)
                    Screen.Attachments -> AttachmentsScreen(vm, p, snackbar)
                    Screen.Floorplan -> FloorHomeScreen(vm, p, snackbar)
                    Screen.Credentials -> CredentialsScreen(vm, p, snackbar)
                    Screen.Trash -> TrashScreen(vm, snackbar)
                    Screen.Documents -> DocumentsScreen(vm, p, snackbar)
                    Screen.Issues -> IssuesScreen(vm, p, snackbar)
                    Screen.Projects, Screen.NewSite -> Unit
                }
            }
            ImportDialogs(vm, importState)
        }
    }
}

@Composable
private fun ImportDialogs(vm: ProjectViewModel, state: ImportState?) {
    val context = LocalContext.current
    when (state) {
        null -> Unit
        is ImportState.NeedsPassword -> {
            var password by remember(state) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = vm::cancelImport,
                title = { Text("Pacchetto protetto") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Inserisci la password con cui è stato esportato il progetto.")
                        OutlinedTextField(
                            value = password, onValueChange = { password = it }, label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(), singleLine = true,
                            isError = state.wrongPassword,
                            supportingText = if (state.wrongPassword) { { Text("Password errata, riprova.") } } else null
                        )
                    }
                },
                confirmButton = {
                    TextButton(enabled = password.isNotEmpty(), onClick = { vm.startImport(context.contentResolver, state.uri, password) }) { Text("Apri") }
                },
                dismissButton = { TextButton(onClick = vm::cancelImport) { Text("Annulla") } }
            )
        }
        is ImportState.Review -> {
            val pkg = state.evaluation.importResult.pkg ?: return
            val comparison = state.evaluation.comparison
            val sameProjectExists = comparison?.currentProjectId != null
            AlertDialog(
                onDismissRequest = vm::cancelImport,
                title = { Text(if (sameProjectExists) "Sostituire la copia sul dispositivo?" else "Importare il progetto?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(pkg.project.name, fontWeight = FontWeight.SemiBold)
                        Text("${pkg.project.businessUnits.sumOf { it.devices.size }} apparati · ${pkg.project.racks.size} rack · ${pkg.project.cables.size} cavi")
                        comparison?.let { c ->
                            Text(comparisonLabel(c.status.name))
                            c.warningMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                        if (sameProjectExists) Text("«Sostituisci» usa solo il pacchetto. «Unisci» tiene le modifiche di entrambe le copie e chiede cosa fare quando lo stesso elemento è cambiato in tutte e due.", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Row {
                        if (sameProjectExists && comparison?.status?.name != "IDENTICAL") TextButton(onClick = vm::startMerge) { Text("Unisci…") }
                        TextButton(onClick = vm::confirmImport) { Text(if (sameProjectExists) "Sostituisci" else "Importa") }
                    }
                },
                dismissButton = { TextButton(onClick = vm::cancelImport) { Text("Annulla") } }
            )
        }
        is ImportState.Merging -> {
            val conflict = state.current ?: return
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Conflitto ${state.choices.size + 1} di ${state.result.conflicts.size}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("${conflict.kindLabel}: ${conflict.name}", fontWeight = FontWeight.SemiBold)
                        Text(conflict.description)
                        conflict.differences().forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        if (state.result.autoApplied > 0) Text("${state.result.autoApplied} modifiche senza conflitto verranno applicate da sole.", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = { vm.chooseMergeSide(MergeSide.LOCAL) }) { Text("Tieni mio") }
                        TextButton(onClick = { vm.chooseMergeSide(MergeSide.INCOMING) }) { Text("Tieni importato") }
                    }
                },
                dismissButton = { TextButton(onClick = vm::cancelImport) { Text("Annulla unione") } }
            )
        }
    }
}

private fun comparisonLabel(status: String) = when (status) {
    "IDENTICAL" -> "Identico alla copia presente"
    "NEWER_REVISION" -> "Il pacchetto è più recente della copia presente"
    "OLDER_REVISION" -> "Attenzione: il pacchetto è meno recente della copia presente"
    "DIVERGENT" -> "Le due copie sono state modificate separatamente"
    "DIFFERENT_PROJECT" -> "Nuovo progetto"
    else -> status
}
