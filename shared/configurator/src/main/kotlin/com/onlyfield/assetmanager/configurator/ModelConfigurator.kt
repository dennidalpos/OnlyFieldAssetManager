package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

@Composable
fun ModelConfigurator(project: Project, model: DeviceModel, i18n: Messages, change: (DeviceModel) -> Unit) {
    var draft by remember(model.id, model.kind) { mutableStateOf(HardwareConfigurator.modelDraft(project, model)) }
    Row {
        ObjectKind.entries.forEach { kind ->
            TextButton(onClick = { change(model.copy(kind = kind, objectTypeId = ObjectCatalog.types(project).first { it.kind == kind }.id)) }) { Text(i18n.text("config.${when (kind) { ObjectKind.DEVICE -> "device"; ObjectKind.RACK -> "rack"; ObjectKind.CABLE -> "cable" }}")) }
        }
    }
    OutlinedTextField(model.brand.orEmpty(), { change(model.copy(brand = it.ifBlank { null })) }, label = { Text(i18n.text("text.8e2ca9b0cc8a")) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(model.modelNumber.orEmpty(), { change(model.copy(modelNumber = it.ifBlank { null })) }, label = { Text(i18n.text("text.c04ab9879078")) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(model.notes.orEmpty(), { change(model.copy(notes = it.ifBlank { null })) }, label = { Text(i18n.text("config.notes")) }, modifier = Modifier.fillMaxWidth())
    ObjectConfigurator(project, draft, i18n, modelEditor = true, change = { next ->
        draft = next
        val name = when (next.type.kind) { ObjectKind.DEVICE -> next.device.technicalName; ObjectKind.RACK -> next.rack.name; ObjectKind.CABLE -> next.cable.codeOrLabel }
        change(HardwareConfigurator.model(project, next, name).copy(id = model.id, brand = model.brand, modelNumber = model.modelNumber, notes = model.notes))
    })
}
