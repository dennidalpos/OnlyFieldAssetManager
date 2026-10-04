package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelConfigurator(project: Project, model: DeviceModel, i18n: Messages, change: (DeviceModel) -> Unit) {
    var draft by remember(model.id, model.kind) { mutableStateOf(HardwareConfigurator.modelDraft(project, model)) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ObjectKind.entries.forEach { kind ->
            FilterChip(selected = model.kind == kind, onClick = { change(model.copy(kind = kind, objectTypeId = ObjectCatalog.types(project).first { it.kind == kind }.id)) }, label = { Text(i18n.text("config.${when (kind) { ObjectKind.DEVICE -> "device"; ObjectKind.RACK -> "rack"; ObjectKind.CABLE -> "cable" }}")) })
        }
    }
    ObjectConfigurator(project, draft, i18n, modelEditor = true, change = { next ->
        draft = next
        val name = when (next.type.kind) { ObjectKind.DEVICE -> next.device.technicalName; ObjectKind.RACK -> next.rack.name; ObjectKind.CABLE -> next.cable.codeOrLabel }
        change(HardwareConfigurator.model(project, next, name).copy(id = model.id, brand = model.brand, modelNumber = model.modelNumber, notes = model.notes))
    })
    ConfiguratorSection(i18n.text("ux.modelDetails"), i18n = i18n) {
        OutlinedTextField(model.brand.orEmpty(), { change(model.copy(brand = it.ifBlank { null })) }, label = { Text(i18n.text("text.8e2ca9b0cc8a")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model.modelNumber.orEmpty(), { change(model.copy(modelNumber = it.ifBlank { null })) }, label = { Text(i18n.text("text.c04ab9879078")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model.notes.orEmpty(), { change(model.copy(notes = it.ifBlank { null })) }, label = { Text(i18n.text("config.notes")) }, modifier = Modifier.fillMaxWidth())
    }
}
