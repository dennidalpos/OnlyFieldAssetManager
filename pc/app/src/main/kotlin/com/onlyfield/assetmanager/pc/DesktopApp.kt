package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.pc.ui.components.MasterDetailHost
import com.onlyfield.assetmanager.pc.ui.*
import com.onlyfield.assetmanager.pc.ui.components.ConfirmHost
import com.onlyfield.assetmanager.pc.ui.components.EmptyState
import com.onlyfield.assetmanager.pc.ui.dialogs.ProjectDialogs

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF545F71),
    secondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFF00696E),
    tertiaryContainer = Color(0xFFB4ECEF),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFF8F9FC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF00468C),
    onPrimaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFF80D4D9),
)

@Composable
fun DesktopApp(state: DesktopAppState) {
    MaterialTheme(colorScheme = if (state.darkTheme) DarkColors else LightColors) {
        ConfirmHost {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        ProjectToolbar(state)
                        state.error?.let { ErrorBanner(it) { state.error = null } }
                        MasterDetailHost(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                            SectionContent(state)
                        }
                        HorizontalDivider()
                        StatusBar(state)
                    }
                }
            }
            ProjectDialogs(state)
        }
    }
}

@Composable
private fun ProjectToolbar(state: DesktopAppState) {
    val project = state.project
    var tools by remember { mutableStateOf(false) }
    Surface(tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(project?.name ?: "Seleziona progetto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (project != null) Text("Salvataggio automatico" + if (project.isPasswordProtected) " · Protetto da password" else "", style = MaterialTheme.typography.bodySmall)
            }
            if (project == null) {
                OutlinedButton(onClick = { state.dialog = AppDialog.NewProject }) { Text("Nuovo sito") }
                OutlinedButton(onClick = state::pickAndImport) { Text("Importa .ofam…") }
            } else {
                TextButton(onClick = { state.section = AppSection.FLOORPLANS }) { Text("Mappa") }
                TextButton(onClick = state::undo, enabled = state.canUndo) { Text("Annulla") }
                Box {
                    OutlinedButton(onClick = { tools = true }) { Text("Strumenti") }
                    DropdownMenu(expanded = tools, onDismissRequest = { tools = false }) {
                        AppSection.entries.forEach { section -> DropdownMenuItem(text = { Text(section.title) }, onClick = { tools = false; state.section = section }) }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Documenti e stampa") }, onClick = { tools = false; state.dialog = AppDialog.Documents })
                        DropdownMenuItem(text = { Text("Password del progetto") }, onClick = { tools = false; state.dialog = AppDialog.ManagePassword })
                        DropdownMenuItem(text = { Text("Esporta .ofam") }, onClick = { tools = false; state.exportPackage() })
                        DropdownMenuItem(text = { Text("Apri / Importa…") }, onClick = { tools = false; state.pickAndImport() })
                        DropdownMenuItem(text = { Text("Nuovo sito") }, onClick = { tools = false; state.dialog = AppDialog.NewProject })
                        DropdownMenuItem(text = { Text("Chiudi progetto") }, onClick = { tools = false; state.closeProject() })
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Chiudi", color = MaterialTheme.colorScheme.onErrorContainer) }
        }
    }
}

@Composable
private fun StatusBar(state: DesktopAppState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(state.status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (state.project != null) {
            val errors = state.errorCount
            val warnings = state.warningCount
            Text(
                text = when {
                    errors == 0 && warnings == 0 -> "✓ Nessun problema"
                    else -> "⛔ $errors errori · ⚠ $warnings avvisi"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (errors > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (errors > 0) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clickable(enabled = errors + warnings > 0) { state.dialog = AppDialog.Validation }
            )
        }
        Text(
            "Dati: ${state.dataDir.path.absolutePath}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 420.dp)
        )
    }
}

@Composable
private fun SectionContent(state: DesktopAppState) {
    val project = state.project
    if (project == null || state.section == AppSection.PROJECT) {
        ProjectSection(state)
        return
    }
    val update = state::update
    when (state.section) {
        AppSection.INVENTORY -> InventorySection(project, update, state::addToTrash)
        AppSection.RACKS -> RackSection(project, update, state::addToTrash)
        AppSection.MODELS -> DeviceModelsSection(project, update)
        AppSection.FLOORPLANS -> FloorHomeSection(state)
        AppSection.CREDENTIALS -> CredentialsSection(project, update)
        AppSection.MEDIA -> FloorplanMediaSection(project, update, state::addAttachment, state::attachmentFile, state::addMapSnapshot)
        AppSection.CABLING -> CablingSection(project, update)
        AppSection.NETWORK -> NetworkLogicalSection(project, update)
        AppSection.POWER -> PowerBadgeSection(project, update)
        AppSection.TRASH -> TrashBatchSection(project, state.trash, update) { state.trash = it }
        AppSection.PROJECT -> EmptyState("")
    }
}
