package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.configurator.theme.BrandMark
import com.onlyfield.assetmanager.configurator.theme.SectionCard
import com.onlyfield.assetmanager.configurator.theme.AppSpacing
import com.onlyfield.assetmanager.configurator.SymbolIcons
import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
        title = "OnlyField",
        subtitle = i18n.text("text.3d46bfbe7868"),
        onBack = null,
        snackbarHost = snackbar,
        actions = { LanguagePicker(vm) },
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
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Projects are ordered by last change: the first one is where the user left off.
            item(key = "continue") {
                val last = projects.first()
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BrandMark(40.dp)
                                Text(
                                    text = i18n.text("text.3d46bfbe7868"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            if (last.isPasswordProtected) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = i18n.text("text.a73763f25907"),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = last.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!last.description.isNullOrBlank()) {
                                Text(
                                    text = last.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = i18n.text("text.c95c16b49c20", formatDateTime(last.updatedEpochMs)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = { open(last) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = i18n.text("text.d5555c9b8e0b", last.name),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item(key = "quick_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = vm::startNewSite,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(i18n.text("text.ac667fe865c9"), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(SymbolIcons.map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(i18n.text("text.a53503bbd801"), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            item(key = "header") {
                Text(
                    text = i18n.text("text.8102b2c0ceb4"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            items(projects, key = { it.id }) { p ->
                ItemCard(
                    title = p.name,
                    leading = { Icon(SymbolIcons.map, null, tint = MaterialTheme.colorScheme.primary) },
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
        val save = rememberEditSave(vm, p)
        FormDialog(i18n.text("text.62e351b7ecb0"), { renaming = null }, {
            save.submit({ renaming = null }) { result -> vm.renameProject(p.id, name, result) }
        }, confirmEnabled = name.isNotBlank() && vm.busy == null) {
            FormField(name, { name = it }, i18n.text("text.2e245546ff59"))
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }

    unlocking?.let { p ->
        var password by remember(p) { mutableStateOf("") }
        var wrong by remember(p) { mutableStateOf(false) }
        FormDialog(i18n.text("text.dc30ca04bf34", p.name), { unlocking = null }, { vm.openProtectedProject(p.id, password) { wrong = true } },
            confirmEnabled = password.isNotEmpty(), confirmLabel = i18n.text("text.12abcf9ee7d6"), dismissLabel = i18n.text("text.18c9d912a210")) {
            OutlinedTextField(
                value = password, onValueChange = { password = it; wrong = false }, label = { Text(i18n.text("text.e7cf3ef4f17c")) },
                visualTransformation = PasswordVisualTransformation(), singleLine = true, isError = wrong,
                supportingText = if (wrong) { { Text(i18n.text("text.c409f4b96322")) } } else null, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** First launch: the two ways to get a project onto the phone. */
@Composable
private fun StartActions(onNewSite: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    val i18n = LocalMessages.current

    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BrandMark(64.dp)
                Text(
                    text = i18n.text("text.adb53755cdc3"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = i18n.text("text.55f623cd4bf4"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = onNewSite, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(i18n.text("text.7559294aebf8"), style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(SymbolIcons.map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(i18n.text("text.b93f2228bcd7"), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

