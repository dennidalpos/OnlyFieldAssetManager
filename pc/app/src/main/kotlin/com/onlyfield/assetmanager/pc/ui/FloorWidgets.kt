package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.pc.ui.components.*

@Composable
internal fun ObjectFields(project: Project, draft: MapObjectDraft, change: (MapObjectDraft) -> Unit) {
    val errors = draft.errors(project)
    when (draft.type.kind) {
        ObjectKind.DEVICE -> {
            val d = draft.device
            FormField(d.technicalName, { change(draft.copy(device = d.copy(technicalName = it))) }, "Nome *", error = errors["technicalName"])
            FormField(d.physicalLabel, { change(draft.copy(device = d.copy(physicalLabel = it))) }, "Etichetta fisica")
            FormField(d.alias, { change(draft.copy(device = d.copy(alias = it))) }, "Alias")
            FormField(d.ipAddress, { change(draft.copy(device = d.copy(ipAddress = it))) }, "IP", error = errors["ipAddress"])
            FormField(d.macAddress, { change(draft.copy(device = d.copy(macAddress = it))) }, "MAC", error = errors["macAddress"])
            FormField(d.serialNumber, { change(draft.copy(device = d.copy(serialNumber = it))) }, "Numero di serie")
            OptionPicker("Modello", project.deviceModels, project.deviceModels.find { it.id == d.deviceModelId }, { it.name }, { change(draft.copy(device = d.copy(deviceModelId = it?.id))) }, noneLabel = "Nessun modello")
            OptionPicker("Rack", project.racks.filter { it.areaId == draft.areaId }, project.racks.find { it.id == d.rackId }, { it.name }, { change(draft.copy(device = d.copy(rackId = it?.id, mountingType = if (it == null) MountingType.OUT_OF_RACK else MountingType.RACK_MOUNT))) }, noneLabel = "Fuori rack")
            if (d.rackId != null) {
                FormField(d.positionU, { change(draft.copy(device = d.copy(positionU = it))) }, "Posizione U", error = errors["positionU"])
                EnumPicker("Lato", RackSide.entries, d.rackSide, { it.toDisplayString() }, { change(draft.copy(device = d.copy(rackSide = it))) })
            }
            FormField(d.heightU, { change(draft.copy(device = d.copy(heightU = it))) }, "Altezza U", error = errors["heightU"])
            EnumPicker("Stato", ObservationStatus.entries, d.observationStatus, { it.toDisplayString() }, { change(draft.copy(device = d.copy(observationStatus = it))) })
            FormField(d.notes, { change(draft.copy(device = d.copy(notes = it))) }, "Note", singleLine = false)
        }
        ObjectKind.RACK -> {
            val r = draft.rack
            FormField(r.name, { change(draft.copy(rack = r.copy(name = it))) }, "Nome *", error = errors["name"])
            FormField(r.heightU, { change(draft.copy(rack = r.copy(heightU = it))) }, "Altezza U", error = errors["heightU"])
            FormField(r.depthMm, { change(draft.copy(rack = r.copy(depthMm = it))) }, "Profondità mm", error = errors["depthMm"])
            EnumPicker("Numerazione", NumberingDirection.entries, r.numberingDirection, { it.toDisplayString() }, { change(draft.copy(rack = r.copy(numberingDirection = it))) })
            FormField(r.notes, { change(draft.copy(rack = r.copy(notes = it))) }, "Note", singleLine = false)
        }
        ObjectKind.CABLE -> {
            val c = draft.cable
            val devices = project.businessUnits.flatMap { it.devices }
            FormField(c.codeOrLabel, { change(draft.copy(cable = c.copy(codeOrLabel = it))) }, "Codice / nome")
            listOf(true, false).forEach { first ->
                val selected = ObjectMap.endpoint(project, c.toCable(null).copy(deviceAId = draft.deviceAId, deviceBId = draft.deviceBId), first)
                val portId = if (first) c.portAId else c.portBId
                OptionPicker("Apparato ${if (first) "A" else "B"}", devices, selected, { it.technicalName }, { d ->
                    change(if (first) draft.copy(deviceAId = d?.id, cable = c.copy(portAId = null)) else draft.copy(deviceBId = d?.id, cable = c.copy(portBId = null)))
                }, noneLabel = "Estremità sconosciuta", optionDetail = { d -> project.businessUnits.find { b -> b.devices.any { it.id == d.id } }?.let { b -> "${b.name} / ${ObjectMap.areas(b).find { it.id == ObjectMap.areaId(project, d) }?.let { ObjectMap.areaLabel(b, it) } ?: "senza piano"}" } })
                if (selected != null) OptionPicker("Porta ${if (first) "A" else "B"}", selected.ports, selected.ports.find { it.id == portId }, { it.name }, { p -> change(if (first) draft.copy(deviceAId = selected.id, cable = c.copy(portAId = p?.id)) else draft.copy(deviceBId = selected.id, cable = c.copy(portBId = p?.id))) }, noneLabel = "Solo apparato")
            }
            EnumPicker("Mezzo", CableMedium.entries, c.medium, { it.toDisplayString() }, { change(draft.copy(cable = c.copy(medium = it))) })
            FormField(c.connectorA, { change(draft.copy(cable = c.copy(connectorA = it))) }, "Connettore A")
            FormField(c.connectorB, { change(draft.copy(cable = c.copy(connectorB = it))) }, "Connettore B")
            FormField(c.nominalCharacteristics, { change(draft.copy(cable = c.copy(nominalCharacteristics = it))) }, "Caratteristiche")
            FormField(c.color, { change(draft.copy(cable = c.copy(color = it))) }, "Colore")
            FormField(c.lengthValue, { change(draft.copy(cable = c.copy(lengthValue = it))) }, "Lunghezza (m)", error = errors["lengthValue"])
            FormField(c.notes, { change(draft.copy(cable = c.copy(notes = it))) }, "Note", singleLine = false)
        }
    }
    errors.values.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
    val extras = draft.extraFields ?: project.customExtraFields.filter { it.targetId == draft.id }
    extras.forEach { field ->
        FormField(field.fieldKey, { value -> change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(fieldKey = value) else it })) }, "Nome campo")
        FormField(field.fieldValue, { value -> change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(fieldValue = value) else it })) }, "Valore")
        EnumPicker("Classificazione", AttachmentClassification.entries, field.classification, { it.toDisplayString() }, { value -> change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(classification = value) else it })) })
        TextButton(onClick = { change(draft.copy(extraFields = extras.filterNot { it.id == field.id })) }) { Text("Rimuovi campo") }
    }
    OutlinedButton(onClick = { change(draft.copy(extraFields = extras + CustomExtraField(targetType = draft.targetType.name, targetId = draft.id, fieldKey = "Campo", fieldValue = ""))) }) { Text("Aggiungi campo") }
}

@Composable
internal fun ObjectCatalogDialog(project: Project, onClose: () -> Unit, onSelect: (ObjectType) -> Unit, onCustom: (ObjectType) -> Unit) {
    var query by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DeviceCategory.CUSTOM) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Aggiungi oggetto") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("Cerca tipologia…") }, singleLine = true)
            if (custom) {
                FormField(name, { name = it }, "Nome tipologia *")
                EnumPicker("Categoria di base", DeviceCategory.entries, category, { it.toDisplayString() }, { category = it })
                Button(enabled = name.isNotBlank(), onClick = { onCustom(ObjectType(name = name.trim(), category = category)) }) { Text("Crea e usa") }
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(ObjectCatalog.types(project).filter { it.name.contains(query, true) }, key = { it.id }) { type ->
                        TextButton(onClick = { onSelect(type) }, modifier = Modifier.fillMaxWidth()) { Text(type.name) }
                    }
                }
                OutlinedButton(onClick = { custom = true }) { Text("Tipologia personalizzata") }
            }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Chiudi") } })
}
