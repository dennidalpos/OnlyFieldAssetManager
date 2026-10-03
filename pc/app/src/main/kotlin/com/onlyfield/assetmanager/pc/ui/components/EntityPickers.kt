package com.onlyfield.assetmanager.pc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.EntityTypeLabels
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.TargetRef

@Composable
fun DevicePicker(
    label: String,
    index: ProjectIndex,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    noneLabel: String? = null,
    error: String? = null,
) {
    OptionPicker(
        label = label,
        options = index.devices,
        selected = index.device(selectedId),
        optionLabel = { it.technicalName },
        optionDetail = { d -> listOfNotNull(d.category.toDisplayString(), d.ipAddress, index.areaName(d.areaId, "").ifBlank { null }).joinToString(" · ") },
        onSelected = { onSelected(it?.id) },
        noneLabel = noneLabel,
        isError = error != null,
        supportingText = error ?: if (index.devices.isEmpty()) "Nessun apparato: crealo prima in Inventario" else null,
        modifier = modifier
    )
}

/** Chooses what a badge or extra field refers to: the project, or a device / rack / port / area. */
@Composable
fun TargetPicker(index: ProjectIndex, target: TargetRef, onChange: (TargetRef) -> Unit, error: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptionPicker(
            label = "Riferito a",
            options = TargetRef.TARGET_TYPES,
            selected = target.type,
            optionLabel = EntityTypeLabels::of,
            onSelected = { it?.let { type -> if (type != target.type) onChange(TargetRef(type, null)) } },
            modifier = Modifier.weight(0.7f)
        )
        val pickerModifier = Modifier.weight(1.3f)
        when (target.type) {
            "DEVICE" -> DevicePicker("Apparato *", index, target.id, { onChange(target.copy(id = it)) }, pickerModifier, error = error)
            "RACK" -> OptionPicker("Rack *", index.project.racks, index.rack(target.id), { it.name }, { onChange(target.copy(id = it?.id)) }, pickerModifier,
                isError = error != null, supportingText = error)
            "AREA" -> OptionPicker("Area *", index.areas, index.area(target.id), { it.name }, { onChange(target.copy(id = it?.id)) }, pickerModifier,
                isError = error != null, supportingText = error)
            "PORT" -> OptionPicker(
                "Porta *", index.ports, index.port(target.id), { "${it.device.technicalName} › ${it.port.name}" }, { onChange(target.copy(id = it?.port?.id)) },
                pickerModifier, isError = error != null, supportingText = error
            )
            else -> OptionPicker("Progetto", listOf(index.project.name), index.project.name, { it }, {}, pickerModifier, enabled = false)
        }
    }
}
