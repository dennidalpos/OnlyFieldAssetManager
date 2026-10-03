package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.FieldValidators

@Composable
fun DeviceModelsSection(project: Project, onProjectUpdated: (Project, String) -> Unit) {
    val index = remember(project) { ProjectIndex(project) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<DeviceModel?>(null) }
    var creating by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf<DeviceModel?>(null) }
    val models = project.deviceModels.filter { matchesQuery(query, it.name, it.brand, it.modelNumber) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            "Modelli di apparato",
            subtitle = "Modelli riutilizzabili: categoria, altezza e porte generate automaticamente",
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = "Cerca modello, marca, codice…"
        ) {
            Button(onClick = { creating = true }) { Text("+ Nuovo modello") }
        }
        if (models.isEmpty()) {
            EmptyState(if (project.deviceModels.isEmpty()) "Nessun modello definito." else "Nessun modello corrisponde alla ricerca.",
                actionLabel = "+ Nuovo modello".takeIf { project.deviceModels.isEmpty() }, onAction = { creating = true })
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(models, key = { it.id }) { m ->
                val usage = index.devices.count { it.deviceModelId == m.id }
                ItemCard(
                    title = m.name,
                    badge = m.category.toDisplayString(),
                    details = listOf(
                        listOfNotNull(m.brand, m.modelNumber, "${m.defaultHeightU}U").joinToString(" · "),
                        m.portTemplates.joinToString(", ") { "${it.portCount}× ${it.namePrefix}${it.startNumber}…" }.ifBlank { "Nessun template porte" },
                        "Usato da $usage apparati"
                    )
                ) {
                    TextButton(onClick = { applying = m }, enabled = index.devices.isNotEmpty()) { Text("Applica…") }
                    EditButton { editing = m }
                    DeleteButton(m.name, onDelete = { onProjectUpdated(ProjectEdits.deleteDeviceModel(project, m.id), "Modello «${m.name}» eliminato.") },
                        message = if (usage > 0) "$usage apparati fanno riferimento a questo modello; manterranno i propri dati." else null)
                }
            }
        }
    }

    if (creating || editing != null) {
        ModelDialog(editing, onDismiss = { creating = false; editing = null }) { saved, isNew ->
            creating = false; editing = null
            onProjectUpdated(if (isNew) ProjectEdits.addDeviceModel(project, saved) else ProjectEdits.updateDeviceModel(project, saved), "Modello «${saved.name}» salvato.")
        }
    }

    applying?.let { model ->
        var deviceId by remember(model) { mutableStateOf<String?>(null) }
        val device = index.device(deviceId)
        FormDialog(
            title = "Applica «${model.name}»",
            onDismiss = { applying = null },
            onConfirm = {
                applying = null
                onProjectUpdated(ProjectEdits.applyModelToDevice(project, deviceId!!, model.id), "Modello «${model.name}» applicato a «${device?.technicalName}».")
            },
            confirmEnabled = deviceId != null,
            confirmLabel = "Applica",
            width = 520.dp
        ) {
            Text("Imposta categoria (${model.category.toDisplayString()}) e altezza (${model.defaultHeightU}U) dell'apparato.")
            DevicePicker("Apparato *", index, deviceId, { deviceId = it })
            device?.let {
                Text(
                    if (it.ports.isEmpty()) "Verranno create ${model.portTemplates.sumOf { t -> t.portCount }} porte dal modello."
                    else "L'apparato ha già ${it.ports.size} porte: non verranno modificate.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ModelDialog(model: DeviceModel?, onDismiss: () -> Unit, onSave: (DeviceModel, Boolean) -> Unit) {
    var name by remember { mutableStateOf(model?.name.orEmpty()) }
    var brand by remember { mutableStateOf(model?.brand.orEmpty()) }
    var modelNumber by remember { mutableStateOf(model?.modelNumber.orEmpty()) }
    var category by remember { mutableStateOf(model?.category ?: DeviceCategory.NETWORK_SWITCH) }
    var height by remember { mutableStateOf(model?.defaultHeightU?.toString() ?: "1") }
    var templates by remember { mutableStateOf(model?.portTemplates.orEmpty()) }

    var prefix by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("1") }
    var count by remember { mutableStateOf("") }
    var side by remember { mutableStateOf(PortSide.FRONT) }

    val heightError = FieldValidators.int(height, 1, 60, required = true)
    val countError = FieldValidators.int(count, 1, 512)
    val startError = FieldValidators.int(start, 0, 9999, required = true)

    FormDialog(
        title = if (model == null) "Nuovo modello" else "Modifica «${model.name}»",
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank() && heightError == null,
        onConfirm = {
            val saved = (model ?: DeviceModel(name = name.trim())).copy(
                name = name.trim(),
                brand = brand.trim().ifBlank { null },
                modelNumber = modelNumber.trim().ifBlank { null },
                category = category,
                defaultHeightU = height.trim().toInt(),
                portTemplates = templates
            )
            onSave(saved, model == null)
        },
        width = 640.dp
    ) {
        FormField(name, { name = it }, "Nome *", hint = "Es. Switch 48 porte PoE")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(brand, { brand = it }, "Marca", Modifier.weight(1f))
            FormField(modelNumber, { modelNumber = it }, "Codice modello", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnumPicker("Categoria", DeviceCategory.entries, category, { it.toDisplayString() }, { category = it }, Modifier.weight(1.4f))
            FormField(height, { height = it }, "Altezza (U) *", Modifier.weight(0.6f), heightError)
        }

        Text("Porte generate", fontWeight = FontWeight.SemiBold)
        templates.forEachIndexed { i, t ->
            ItemCard(
                title = "${t.portCount} porte: ${t.namePrefix}${t.startNumber} … ${t.namePrefix}${t.startNumber + t.portCount - 1}",
                details = listOf(t.side.toDisplayString())
            ) {
                TextButton(onClick = { templates = templates.filterIndexed { j, _ -> j != i } }) { Text("Rimuovi") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            FormField(prefix, { prefix = it }, "Prefisso", Modifier.weight(1f), hint = "Es. Gi1/0/")
            FormField(start, { start = it }, "Da n°", Modifier.weight(0.5f), startError)
            FormField(count, { count = it }, "Quante", Modifier.weight(0.5f), countError)
            EnumPicker("Lato", PortSide.entries, side, { it.toDisplayString() }, { side = it }, Modifier.weight(0.8f))
            OutlinedButton(
                enabled = prefix.isNotBlank() && count.isNotBlank() && countError == null && startError == null,
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    templates = templates + PortTemplate(namePrefix = prefix.trim(), startNumber = start.trim().toInt(), portCount = count.trim().toInt(), side = side)
                    prefix = ""; count = ""; start = "1"
                }
            ) { Text("Aggiungi") }
        }
    }
}
