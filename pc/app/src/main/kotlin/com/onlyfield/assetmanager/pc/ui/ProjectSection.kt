package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.pc.LocalMessages

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
    val i18n = LocalMessages.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(i18n.text("text.f9f3bd8a9b95"), style = MaterialTheme.typography.headlineSmall)
            state.storedProjects.maxByOrNull { it.lastModifiedEpochMs }?.let { last ->
                Button(onClick = { state.openStored(last.file) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(i18n.text("text.d5555c9b8e0b", last.name), style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                i18n.text("text.792c143202b5") +
                    i18n.text("text.dc52919dadd4"),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val big = Modifier.weight(1f).heightIn(min = 52.dp)
                Button(onClick = { state.newProject() }, modifier = big) { Text(i18n.text("text.7559294aebf8")) }
                OutlinedButton(onClick = state::pickAndImport, modifier = big) { Text(i18n.text("text.107b5c74d18f")) }
            }
        }
    }
}

@Composable
private fun ProjectInfoCard(project: Project, state: DesktopAppState) {
    val i18n = LocalMessages.current

    var editing by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(project.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (project.isPasswordProtected) Tag(i18n.text("text.a73763f25907"))
                EditButton { editing = true }
                OutlinedButton(onClick = state::closeProject) { Text(i18n.text("text.c00df9e3726e")) }
            }
            Text(project.description?.ifBlank { null } ?: i18n.text("text.842f2d4cab3f"), style = MaterialTheme.typography.bodyMedium)
            val devices = project.businessUnits.sumOf { it.devices.size }
            Text(
                i18n.text("text.d2c89e148209", devices, project.racks.size, project.cables.size, project.vlans.size) +
                    i18n.text("text.766c94472b45", formatDate(project.updatedEpochMs)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { state.dialog = AppDialog.Validation }, enabled = state.issues.isNotEmpty()) {
                    Text(if (state.issues.isEmpty()) i18n.text("text.782158dd0587") else i18n.text("text.663d270128cf", state.errorCount, state.warningCount))
                }
            }
        }
    }
    if (editing) {
        var name by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(project.name) }
        var description by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(project.description.orEmpty()) }
        EditPanel(
            title = i18n.text("text.bb9b7a5f351a"),
            onDismiss = { editing = false },
            onConfirm = {
                editing = false
                state.update(project.copy(name = name.trim(), description = description.trim().ifBlank { null }, updatedEpochMs = System.currentTimeMillis()), i18n.text("text.e3442afb1525"))
            },
            confirmEnabled = name.isNotBlank()
        ) {
            FormField(name, { name = it }, i18n.text("text.85afe7453202"))
            FormField(description, { description = it }, i18n.text("text.6fb818621896"), singleLine = false, minLines = 3)
        }
    }
}

/** Business units and areas: needed to place devices, racks and floorplans. */
@Composable
private fun StructureCard(project: Project, state: DesktopAppState, modifier: Modifier) {
    val i18n = LocalMessages.current

    var editBu by remember { mutableStateOf<BusinessUnit?>(null) }
    val changeDetail = LocalDetailChange.current
    var newBu by remember { mutableStateOf(false) }
    var areaTarget by remember { mutableStateOf<Pair<BusinessUnit, Area?>?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(
                i18n.text("text.bec99a784067"),
                subtitle = i18n.text("text.ec43b2d77293")
            ) {
                OutlinedButton(onClick = { changeDetail { newBu = true } }) { Text(i18n.text("text.a3236c18a4f4")) }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(project.businessUnits, key = { it.id }) { bu ->
                    val buAreas = bu.areas + bu.sites.flatMap { it.areas }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ItemCard(
                            title = bu.name,
                            details = listOf(i18n.text("text.6c039d300c29", bu.devices.size, buAreas.size))
                        ) {
                            TextButton(onClick = { changeDetail { areaTarget = bu to null } }) { Text(i18n.text("text.4e331706b4c0")) }
                            EditButton { editBu = bu }
                            DeleteButton(bu.name, onDelete = {
                                val updated = ProjectEdits.deleteBusinessUnit(project, bu.id)
                                if (updated == null) state.error = i18n.text("text.88b433158eb8", bu.name)
                                else state.update(updated, i18n.text("text.03814dddd9fb", bu.name))
                            })
                        }
                        buAreas.forEach { area ->
                            ItemCard(
                                title = area.name,
                                details = listOfNotNull(area.floor?.let { i18n.text("text.6e61502a3560", it) }, area.description),
                                modifier = Modifier.padding(start = 24.dp)
                            ) {
                                EditButton { areaTarget = bu to area }
                                DeleteButton(area.name, onDelete = {
                                    val updated = ProjectEdits.deleteArea(project, area.id)
                                    if (updated == null) state.error = i18n.text("text.7ef2296f1ba3", area.name)
                                    else state.update(updated, i18n.text("text.3b867ddd547d", area.name))
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
            title = if (bu == null) i18n.text("text.9058af538683") else i18n.text("text.f31c64d94469"),
            onDismiss = { newBu = false; editBu = null },
            onConfirm = {
                val updated = if (bu == null) ProjectEdits.addBusinessUnit(project, name.trim())
                else ProjectEdits.renameBusinessUnit(project, bu.id, name.trim())
                newBu = false; editBu = null
                state.update(updated, i18n.text("text.0a195dba51c9", name.trim()))
            },
            confirmEnabled = name.isNotBlank(),
            width = 440.dp
        ) {
            FormField(name, { name = it }, i18n.text("text.2e245546ff59"))
        }
    }

    areaTarget?.let { (bu, area) ->
        var name by remember(LocalDetailSlot.current?.editorVersion, area) { mutableStateOf(area?.name.orEmpty()) }
        var floor by remember(LocalDetailSlot.current?.editorVersion, area) { mutableStateOf(area?.floor.orEmpty()) }
        var description by remember(LocalDetailSlot.current?.editorVersion, area) { mutableStateOf(area?.description.orEmpty()) }
        EditPanel(
            title = if (area == null) i18n.text("text.ca0d7a2e19a4", bu.name) else i18n.text("text.e325f13a6dee"),
            onDismiss = { areaTarget = null },
            onConfirm = {
                val edited = (area ?: Area(name = name.trim())).copy(
                    name = name.trim(), floor = floor.trim().ifBlank { null }, description = description.trim().ifBlank { null }
                )
                val updated = if (area == null) ProjectEdits.addArea(project, bu.id, edited) else ProjectEdits.updateArea(project, edited)
                areaTarget = null
                state.update(updated, i18n.text("text.b0e1d3b2c943", edited.name))
            },
            confirmEnabled = name.isNotBlank(),
            width = 460.dp
        ) {
            FormField(name, { name = it }, i18n.text("text.2e245546ff59"), hint = i18n.text("text.0b93bb0c53bc"))
            FormField(floor, { floor = it }, i18n.text("text.fa2bd181d8ba"))
            FormField(description, { description = it }, i18n.text("text.6fb818621896"))
        }
    }
}

@Composable
private fun StoredProjectsCard(state: DesktopAppState, modifier: Modifier) {
    val i18n = LocalMessages.current

    val confirm = LocalConfirm.current
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(i18n.text("text.8ea81e5c4655"), subtitle = state.dataDir.path.absolutePath) {
                TextButton(onClick = state::refreshStoredList) { Text(i18n.text("text.39dfe8da7df1")) }
            }
            if (state.storedProjects.isEmpty()) {
                EmptyState(i18n.text("text.6e65b6d1e483"))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.storedProjects, key = { it.file.absolutePath }) { item ->
                        val isOpen = item.id == state.project?.id
                        StoredProjectRow(item, isOpen) {
                            if (state.project == null || isOpen) state.openStored(item.file)
                            else confirm(
                                ConfirmRequest(
                                    title = i18n.text("text.46b503201812", item.name),
                                    message = i18n.text("text.e31ad5cb02a0"),
                                    confirmLabel = i18n.text("text.12abcf9ee7d6"),
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
    val i18n = LocalMessages.current

    ItemCard(
        title = item.name,
        details = listOf(i18n.text("text.c95c16b49c20", formatDate(item.lastModifiedEpochMs))),
        badge = when {
            isOpen -> i18n.text("text.5f42eb4dd012")
            item.isEncrypted -> "🔒"
            else -> null
        }
    ) {
        if (!isOpen) Button(onClick = onOpen) { Text(i18n.text("text.12abcf9ee7d6")) }
    }
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date(epochMs))
