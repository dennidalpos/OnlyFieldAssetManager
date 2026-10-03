package com.onlyfield.assetmanager.pc.ui.components

import com.onlyfield.assetmanager.pc.LocalMessages

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
    val i18n = LocalMessages.current

    OptionPicker(
        label = label,
        options = index.devices,
        selected = index.device(selectedId),
        optionLabel = { it.technicalName },
        optionDetail = { d -> listOfNotNull(d.category.toDisplayString(i18n = i18n), d.ipAddress, index.areaName(d.areaId, "").ifBlank { null }).joinToString(" · ") },
        onSelected = { onSelected(it?.id) },
        noneLabel = noneLabel,
        isError = error != null,
        supportingText = error ?: if (index.devices.isEmpty()) i18n.text("text.8b63c37b51d4") else null,
        modifier = modifier
    )
}

/** Chooses what a badge or extra field refers to: the project, or a device / rack / port / area. */
@Composable
fun TargetPicker(index: ProjectIndex, target: TargetRef, onChange: (TargetRef) -> Unit, error: String? = null) {
    val i18n = LocalMessages.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OptionPicker(
            label = i18n.text("text.254b7ac2475c"),
            options = TargetRef.TARGET_TYPES,
            selected = target.type,
            optionLabel = { EntityTypeLabels.of(it, i18n) },
            onSelected = { it?.let { type -> if (type != target.type) onChange(TargetRef(type, null)) } },
            modifier = Modifier.weight(0.7f)
        )
        val pickerModifier = Modifier.weight(1.3f)
        when (target.type) {
            "DEVICE" -> DevicePicker(i18n.text("text.e7f2c0e68768"), index, target.id, { onChange(target.copy(id = it)) }, pickerModifier, error = error)
            "RACK" -> OptionPicker(i18n.text("text.67c1ef0fff56"), index.project.racks, index.rack(target.id), { it.name }, { onChange(target.copy(id = it?.id)) }, pickerModifier,
                isError = error != null, supportingText = error)
            "AREA" -> OptionPicker(i18n.text("text.ddbccb18e085"), index.areas, index.area(target.id), { it.name }, { onChange(target.copy(id = it?.id)) }, pickerModifier,
                isError = error != null, supportingText = error)
            "PORT" -> OptionPicker(
                i18n.text("text.57c2ec879203"), index.ports, index.port(target.id), { "${it.device.technicalName} › ${it.port.name}" }, { onChange(target.copy(id = it?.port?.id)) },
                pickerModifier, isError = error != null, supportingText = error
            )
            else -> OptionPicker(i18n.text("text.b7700d71d0ce"), listOf(index.project.name), index.project.name, { it }, {}, pickerModifier, enabled = false)
        }
    }
}
