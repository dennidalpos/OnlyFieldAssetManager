package com.onlyfield.assetmanager.ui

import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import com.onlyfield.assetmanager.ui.components.LocalOverlayCount
import com.onlyfield.assetmanager.ui.components.MainNavigationBar
import com.onlyfield.assetmanager.ui.components.mainTabs
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
import com.onlyfield.assetmanager.configurator.theme.OnlyFieldTheme
import androidx.compose.foundation.isSystemInDarkTheme

/** Root of the UI: routes the current [Screen], shows snackbars and the import flow. */
@Composable
fun AppRoot(vm: ProjectViewModel, onExit: () -> Unit) {
    val i18n = LocalMessages.current

    val snackbar = remember { SnackbarHostState() }
    val project by vm.project.collectAsState()
    val importState by vm.importState.collectAsState()

    LaunchedEffect(i18n.locale) {
        vm.messages.collect { msg ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = msg.text,
                actionLabel = if (msg.undo != null) i18n.text("action.undo") else null,
                withDismissAction = msg.isError,
                duration = if (msg.isError) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) msg.undo?.invoke()
        }
    }

    BackHandler { if (!vm.back()) onExit() }

    OnlyFieldTheme(isSystemInDarkTheme()) {
        ConfirmHost {
            Surface {
                val screen = vm.currentScreen
                val p = project
                val overlays = remember { mutableIntStateOf(0) }
                val tabbed = p != null && mainTabs.any { it.first == screen }
                CompositionLocalProvider(LocalOverlayCount provides overlays) {
                Column(Modifier.fillMaxSize()) {
                // The bar owns the bottom inset; screens above it must not pad for it again.
                Box(Modifier.weight(1f).then(if (tabbed && overlays.intValue == 0) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)) {
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
                if (tabbed && overlays.intValue == 0) MainNavigationBar(screen) { vm.openTab(it) }
                }
                }
            }
            ImportDialogs(vm, importState)
        }
    }
}

@Composable
private fun ImportDialogs(vm: ProjectViewModel, state: ImportState?) {
    val i18n = LocalMessages.current

    val context = LocalContext.current
    when (state) {
        null -> Unit
        is ImportState.NeedsPassword -> {
            var password by remember(state) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = vm::cancelImport,
                title = { Text(i18n.text("text.9c4199f65693")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(i18n.text("text.5d37a89596b4"))
                        OutlinedTextField(
                            value = password, onValueChange = { password = it }, label = { Text(i18n.text("text.e7cf3ef4f17c")) },
                            visualTransformation = PasswordVisualTransformation(), singleLine = true,
                            isError = state.wrongPassword,
                            supportingText = if (state.wrongPassword) { { Text(i18n.text("text.972b256c2416")) } } else null
                        )
                    }
                },
                confirmButton = {
                    TextButton(enabled = password.isNotEmpty(), onClick = { vm.startImport(context.contentResolver, state.uri, password) }) { Text(i18n.text("text.12abcf9ee7d6")) }
                },
                dismissButton = { TextButton(onClick = vm::cancelImport) { Text(i18n.text("text.18c9d912a210")) } }
            )
        }
        is ImportState.Review -> {
            val pkg = state.evaluation.importResult.pkg ?: return
            val comparison = state.evaluation.comparison
            val sameProjectExists = comparison?.currentProjectId != null
            AlertDialog(
                onDismissRequest = vm::cancelImport,
                title = { Text(if (sameProjectExists) i18n.text("text.bce1e2ff7206") else i18n.text("text.6a8f2c033f77")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(pkg.project.name, fontWeight = FontWeight.SemiBold)
                        Text(i18n.text("text.6d23c6d3c065", pkg.project.businessUnits.sumOf { it.devices.size }, pkg.project.racks.size, pkg.project.cables.size))
                        comparison?.let { c ->
                            Text(comparisonLabel(c.status.name, i18n = i18n))
                            c.warningMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                        if (sameProjectExists) Text(i18n.text("text.260e5b89f7f2"), style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Row {
                        if (sameProjectExists && comparison.status.name != "IDENTICAL") TextButton(onClick = vm::startMerge) { Text(i18n.text("text.6f5114885bfc")) }
                        TextButton(onClick = vm::confirmImport) { Text(if (sameProjectExists) i18n.text("text.3260dc474cbc") else i18n.text("text.4f01aabad5cf")) }
                    }
                },
                dismissButton = { TextButton(onClick = vm::cancelImport) { Text(i18n.text("text.18c9d912a210")) } }
            )
        }
        is ImportState.Merging -> {
            val conflict = state.current ?: return
            AlertDialog(
                onDismissRequest = {},
                title = { Text(i18n.text("text.e81e208100f0", state.choices.size + 1, state.result.conflicts.size)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("${conflict.kindLabel}: ${conflict.name}", fontWeight = FontWeight.SemiBold)
                        Text(conflict.localizedDescription(i18n))
                        conflict.differences(i18n = i18n).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        if (state.result.autoApplied > 0) Text(i18n.text("text.cdaed04f8505", state.result.autoApplied), style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = { vm.chooseMergeSide(MergeSide.LOCAL) }) { Text(i18n.text("text.1527bc9e78ef")) }
                        TextButton(onClick = { vm.chooseMergeSide(MergeSide.INCOMING) }) { Text(i18n.text("text.b4c64eaa84c7")) }
                    }
                },
                dismissButton = { TextButton(onClick = vm::cancelImport) { Text(i18n.text("text.5b99d7ce433a")) } }
            )
        }
    }
}

private fun comparisonLabel(status: String, i18n: Messages = Messages()) = when (status) {
    "IDENTICAL" -> i18n.text("text.be8e1a19f13b")
    "NEWER_REVISION" -> i18n.text("text.027cffc88743")
    "OLDER_REVISION" -> i18n.text("text.9f2c4053d145")
    "DIVERGENT" -> i18n.text("text.ebf3f84d3433")
    "DIFFERENT_PROJECT" -> i18n.text("text.6d8d966eabaf")
    else -> status
}
