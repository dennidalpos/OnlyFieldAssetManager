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

private val AppColors = lightColorScheme(
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

@Composable
fun DesktopApp(state: DesktopAppState) {
    MaterialTheme(colorScheme = AppColors) {
        ConfirmHost {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SectionRail(state)
                    VerticalDivider()
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
private fun SectionRail(state: DesktopAppState) {
    val project = state.project
    NavigationRail(modifier = Modifier.fillMaxHeight().width(104.dp)) {
        Spacer(Modifier.height(8.dp))
        AppSection.entries.forEach { s ->
            val enabled = project != null || !s.needsProject
            val label = if (s == AppSection.TRASH && state.trash.isNotEmpty()) "${s.title} (${state.trash.size})" else s.title
            NavigationRailItem(
                selected = state.section == s,
                onClick = { state.section = s },
                enabled = enabled,
                icon = { Text(s.icon, style = MaterialTheme.typography.titleLarge) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
private fun ProjectToolbar(state: DesktopAppState) {
    val project = state.project
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project?.name ?: "Nessun progetto aperto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (project != null) {
                    Text(
                        (if (project.isPasswordProtected) "🔒 Protetto da password · " else "") + "Salvataggio automatico attivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (project != null) {
                TextButton(onClick = state::undo, enabled = state.canUndo) { Text("↶ Annulla") }
            }
            OutlinedButton(onClick = { state.dialog = AppDialog.NewProject }) { Text("Nuovo") }
            OutlinedButton(onClick = state::pickAndImport) { Text("Apri / Importa…") }
            Button(onClick = state::exportPackage, enabled = project != null) { Text("Esporta .ofam") }
            OutlinedButton(onClick = { state.dialog = AppDialog.Documents }, enabled = project != null) { Text("Documenti e stampa") }
            OutlinedButton(onClick = { state.dialog = AppDialog.ManagePassword }, enabled = project != null) {
                Text(if (project?.isPasswordProtected == true) "Password" else "Proteggi")
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
        AppSection.FLOORPLANS -> FloorplanMediaSection(project, update, state::addAttachment, state::attachmentFile)
        AppSection.CABLING -> CablingSection(project, update)
        AppSection.NETWORK -> NetworkLogicalSection(project, update)
        AppSection.POWER -> PowerBadgeSection(project, update)
        AppSection.TRASH -> TrashBatchSection(project, state.trash, update) { state.trash = it }
        AppSection.PROJECT -> EmptyState("")
    }
}
