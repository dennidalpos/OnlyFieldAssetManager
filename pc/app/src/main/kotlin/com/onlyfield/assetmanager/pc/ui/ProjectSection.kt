package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.pc.AppDialog
import com.onlyfield.assetmanager.pc.DesktopAppState
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.StoredProjectInfo
import com.onlyfield.assetmanager.pc.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectSection(state: DesktopAppState) {
    val project = state.project
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (project == null) {
                WelcomeCard(state)
            } else {
                ProjectInfoCard(project, state)
                StructureCard(project, state, Modifier.weight(1f))
            }
        }
        StoredProjectsCard(state, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun WelcomeCard(state: DesktopAppState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Benvenuto in OnlyField Asset Manager", style = MaterialTheme.typography.headlineSmall)
            state.storedProjects.maxByOrNull { it.lastModifiedEpochMs }?.let { last ->
                Button(onClick = { state.openStored(last.file) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Continua: «${last.name}»", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                "Censisci un sito partendo da zero, oppure apri il pacchetto .ofam ricevuto dal telefono o da un collega. " +
                    "Le modifiche vengono salvate automaticamente nella cartella dati.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val big = Modifier.weight(1f).heightIn(min = 52.dp)
                Button(onClick = { state.newProject() }, modifier = big) { Text("Inizia un nuovo sito") }
                OutlinedButton(onClick = state::pickAndImport, modifier = big) { Text("Apri un pacchetto ricevuto (.ofam)…") }
            }
        }
    }
}

@Composable
private fun ProjectInfoCard(project: Project, state: DesktopAppState) {
    var editing by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(project.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (project.isPasswordProtected) Tag("🔒 Protetto")
                EditButton { editing = true }
                OutlinedButton(onClick = state::closeProject) { Text("Chiudi progetto") }
            }
            Text(project.description?.ifBlank { null } ?: "Nessuna descrizione", style = MaterialTheme.typography.bodyMedium)
            val devices = project.businessUnits.sumOf { it.devices.size }
            Text(
                "$devices apparati · ${project.racks.size} rack · ${project.cables.size} cavi · ${project.vlans.size} VLAN · " +
                    "ultima modifica ${formatDate(project.updatedEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { state.dialog = AppDialog.Validation }, enabled = state.issues.isNotEmpty()) {
                    Text(if (state.issues.isEmpty()) "✓ Nessun problema rilevato" else "Controllo: ${state.errorCount} errori, ${state.warningCount} avvisi")
                }
            }
        }
    }
    if (editing) {
        var name by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(project.name) }
        var description by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(project.description.orEmpty()) }
        EditPanel(
            title = "Modifica progetto",
            onDismiss = { editing = false },
            onConfirm = {
                editing = false
                state.update(project.copy(name = name.trim(), description = description.trim().ifBlank { null }, updatedEpochMs = System.currentTimeMillis()), "Progetto aggiornato.")
            },
            confirmEnabled = name.isNotBlank()
        ) {
            FormField(name, { name = it }, "Nome progetto *")
            FormField(description, { description = it }, "Descrizione", singleLine = false, minLines = 3)
        }
    }
}

/** Business units and areas: needed to place devices, racks and floorplans. */
@Composable
private fun StructureCard(project: Project, state: DesktopAppState, modifier: Modifier) {
    var editBu by remember { mutableStateOf<BusinessUnit?>(null) }
    val changeDetail = LocalDetailChange.current
    var newBu by remember { mutableStateOf(false) }
    var areaTarget by remember { mutableStateOf<Pair<BusinessUnit, Area?>?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(
                "Struttura",
                subtitle = "Business unit e aree in cui si trovano apparati e rack"
            ) {
                OutlinedButton(onClick = { changeDetail { newBu = true } }) { Text("+ Business unit") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(project.businessUnits, key = { it.id }) { bu ->
                    val buAreas = bu.areas + bu.sites.flatMap { it.areas }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ItemCard(
                            title = bu.name,
                            details = listOf("${bu.devices.size} apparati · ${buAreas.size} aree")
                        ) {
                            TextButton(onClick = { changeDetail { areaTarget = bu to null } }) { Text("+ Area") }
                            EditButton { editBu = bu }
                            DeleteButton(bu.name, onDelete = {
                                val updated = ProjectEdits.deleteBusinessUnit(project, bu.id)
                                if (updated == null) state.error = "«${bu.name}» contiene ancora apparati o aree: spostali o eliminali prima."
                                else state.update(updated, "Business unit «${bu.name}» eliminata.")
                            })
                        }
                        buAreas.forEach { area ->
                            ItemCard(
                                title = area.name,
                                details = listOfNotNull(area.floor?.let { "Piano $it" }, area.description),
                                modifier = Modifier.padding(start = 24.dp)
                            ) {
                                EditButton { areaTarget = bu to area }
                                DeleteButton(area.name, onDelete = {
                                    val updated = ProjectEdits.deleteArea(project, area.id)
                                    if (updated == null) state.error = "L'area «${area.name}» è ancora usata da apparati, rack o planimetrie."
                                    else state.update(updated, "Area «${area.name}» eliminata.")
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    if (newBu || editBu != null) {
        val bu = editBu
        var name by remember(LocalDetailSlot.current?.editorVersion, bu) { mutableStateOf(bu?.name.orEmpty()) }
        EditPanel(
            title = if (bu == null) "Nuova business unit" else "Rinomina business unit",
            onDismiss = { newBu = false; editBu = null },
            onConfirm = {
                val updated = if (bu == null) ProjectEdits.addBusinessUnit(project, name.trim())
                else ProjectEdits.renameBusinessUnit(project, bu.id, name.trim())
                newBu = false; editBu = null
                state.update(updated, "Business unit «${name.trim()}» salvata.")
            },
            confirmEnabled = name.isNotBlank(),
            width = 440.dp
        ) {
            FormField(name, { name = it }, "Nome *")
        }
    }

    areaTarget?.let { (bu, area) ->
        var name by remember(LocalDetailSlot.current?.editorVersion, area) { mutableStateOf(area?.name.orEmpty()) }
        var floor by remember(LocalDetailSlot.current?.editorVersion, area) { mutableStateOf(area?.floor.orEmpty()) }
        var description by remember(LocalDetailSlot.current?.editorVersion, area) { mutableStateOf(area?.description.orEmpty()) }
        EditPanel(
            title = if (area == null) "Nuova area in «${bu.name}»" else "Modifica area",
            onDismiss = { areaTarget = null },
            onConfirm = {
                val edited = (area ?: Area(name = name.trim())).copy(
                    name = name.trim(), floor = floor.trim().ifBlank { null }, description = description.trim().ifBlank { null }
                )
                val updated = if (area == null) ProjectEdits.addArea(project, bu.id, edited) else ProjectEdits.updateArea(project, edited)
                areaTarget = null
                state.update(updated, "Area «${edited.name}» salvata.")
            },
            confirmEnabled = name.isNotBlank(),
            width = 460.dp
        ) {
            FormField(name, { name = it }, "Nome *", hint = "Es. Sala server, Ufficio tecnico")
            FormField(floor, { floor = it }, "Piano")
            FormField(description, { description = it }, "Descrizione")
        }
    }
}

@Composable
private fun StoredProjectsCard(state: DesktopAppState, modifier: Modifier) {
    val confirm = LocalConfirm.current
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Progetti salvati", subtitle = state.dataDir.path.absolutePath) {
                TextButton(onClick = state::refreshStoredList) { Text("Aggiorna") }
            }
            if (state.storedProjects.isEmpty()) {
                EmptyState("Nessun progetto salvato in questa cartella.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.storedProjects, key = { it.file.absolutePath }) { item ->
                        val isOpen = item.id == state.project?.id
                        StoredProjectRow(item, isOpen) {
                            if (state.project == null || isOpen) state.openStored(item.file)
                            else confirm(
                                ConfirmRequest(
                                    title = "Aprire «${item.name}»?",
                                    message = "Il progetto attuale viene chiuso (le modifiche sono già salvate).",
                                    confirmLabel = "Apri",
                                    destructive = false
                                ) { state.openStored(item.file) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoredProjectRow(item: StoredProjectInfo, isOpen: Boolean, onOpen: () -> Unit) {
    ItemCard(
        title = item.name,
        details = listOf("Modificato ${formatDate(item.lastModifiedEpochMs)}"),
        badge = when {
            isOpen -> "Aperto"
            item.isEncrypted -> "🔒"
            else -> null
        }
    ) {
        if (!isOpen) Button(onClick = onOpen) { Text("Apri") }
    }
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date(epochMs))
