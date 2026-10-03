package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*

@Composable
fun DeviceModelsSection(project: Project, onProjectUpdated: (Project, String) -> Unit) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<DeviceModel?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf<DeviceModel?>(null) }
    val models = project.deviceModels.filter { matchesQuery(query, it.name, it.brand, it.modelNumber) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            i18n.text("text.19b5ea1a9124"),
            subtitle = i18n.text("text.fbcff0dba77e"),
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = i18n.text("text.97f1d0bc8a62")
        ) {
            Button(onClick = { changeDetail { creating = true } }) { Text(i18n.text("text.d166b2503fad")) }
        }
        if (models.isEmpty()) {
            EmptyState(if (project.deviceModels.isEmpty()) i18n.text("text.401ace91c1b5") else i18n.text("text.ed7e7e392823"),
                actionLabel = i18n.text("text.d166b2503fad").takeIf { project.deviceModels.isEmpty() }, onAction = { creating = true })
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(models, key = { it.id }) { m ->
                val usage = index.devices.count { it.deviceModelId == m.id }
                ItemCard(
                    title = m.name,
                    badge = m.category.toDisplayString(i18n = i18n),
                    details = listOf(
                        listOfNotNull(m.brand, m.modelNumber, "${m.defaultHeightU}U").joinToString(" · "),
                        m.portTemplates.joinToString(", ") { "${it.portCount}× ${it.namePrefix}${it.startNumber}…" }.ifBlank { i18n.text("text.d9377f02b188") },
                        i18n.text("text.0c6ac92e5d4a", usage)
                    )
                ) {
                    TextButton(onClick = { changeDetail { applying = m } }, enabled = m.kind == ObjectKind.DEVICE && index.devices.isNotEmpty()) { Text(i18n.text("text.1d13a7022c17")) }
                    EditButton { editing = m }
                    DeleteButton(m.name, onDelete = { onProjectUpdated(ProjectEdits.deleteDeviceModel(project, m.id), i18n.text("text.01fe4d6df4c7", m.name)) },
                        message = if (usage > 0) i18n.text("text.f4da185cffc0", usage) else null)
                }
            }
        }
    }

    if (creating || editing != null) {
        ModelDialog(project, editing, onDismiss = { creating = false; editing = null }) { saved, isNew ->
            creating = false; editing = null
            onProjectUpdated(if (isNew) ProjectEdits.addDeviceModel(project, saved) else ProjectEdits.updateDeviceModel(project, saved), i18n.text("text.8dd532c869de", saved.name))
        }
    }

    applying?.let { model ->
        var target by remember(model) { mutableStateOf<Device?>(null) }
        var draft by remember(model) { mutableStateOf<com.onlyfield.assetmanager.core.forms.MapObjectDraft?>(null) }
        EditPanel(title = i18n.text("config.model"), onDismiss = { applying = null }, width = 800.dp,
            confirmEnabled = draft?.errors(project, i18n)?.isEmpty() == true, onConfirm = {
                onProjectUpdated(requireNotNull(draft).apply(project, i18n), i18n.text("config.model")); applying = null
            }) {
            DevicePicker(i18n.text("config.device"), index, target?.id, { id ->
                target = index.device(id)
                draft = target?.let { com.onlyfield.assetmanager.core.forms.HardwareConfigurator.applyModel(com.onlyfield.assetmanager.core.forms.MapObjectDraft.forDevice(project, it), model) }
            })
            draft?.let { d -> ObjectFields(project, d) { draft = it } }
        }
    }
}

@Composable
private fun ModelDialog(project: Project, model: DeviceModel?, onDismiss: () -> Unit, onSave: (DeviceModel, Boolean) -> Unit) {
    val i18n = LocalMessages.current
    var edited by remember(LocalDetailSlot.current?.editorVersion, model) { mutableStateOf(model ?: DeviceModel(name = "", category = DeviceCategory.NETWORK_SWITCH)) }
    EditPanel(title = i18n.text("config.model"), onDismiss = onDismiss, width = 800.dp,
        confirmEnabled = edited.name.isNotBlank() && com.onlyfield.assetmanager.core.forms.HardwareConfigurator.validGroups(edited.portTemplates) && edited.defaultHeightU in 1..60, onConfirm = { onSave(edited, model == null) }) {
        val markDirty = LocalMarkDirty.current
        com.onlyfield.assetmanager.configurator.ModelConfigurator(project, edited, i18n) { markDirty(); edited = it }
    }
}
