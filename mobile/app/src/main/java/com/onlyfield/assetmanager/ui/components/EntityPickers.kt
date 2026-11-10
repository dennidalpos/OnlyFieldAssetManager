package com.onlyfield.assetmanager.ui.components

import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
) = run { val i18n = LocalMessages.current; OptionPicker(
    label = label,
    options = index.devices,
    selected = index.device(selectedId),
    optionLabel = { it.technicalName },
    optionDetail = { d -> listOfNotNull(d.category.toDisplayString(i18n = i18n), d.ipAddress, index.areaName(d.areaId, "").ifBlank { null }).joinToString(" · ") },
    onSelected = { onSelected(it?.id) },
    noneLabel = noneLabel,
    isError = error != null,
    supportingText = error ?: if (index.devices.isEmpty()) i18n.text("text.d80a6716df22") else null,
    modifier = modifier
) }

@Composable
fun PortPicker(
    label: String,
    index: ProjectIndex,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    noneLabel: String? = LocalMessages.current.text("text.f6ea027385f8"),
    error: String? = null,
    busyPortIds: Set<String> = emptySet(),
) = run { val i18n = LocalMessages.current; OptionPicker(
    label = label,
    options = index.ports,
    selected = index.port(selectedId),
    optionLabel = { "${it.device.technicalName} › ${it.port.name}" },
    optionDetail = { ref -> listOfNotNull(ref.port.label, if (ref.port.id in busyPortIds) i18n.text("text.7c4d126e67ea") else null).joinToString(" · ") },
    onSelected = { onSelected(it?.port?.id) },
    noneLabel = noneLabel,
    isError = error != null,
    supportingText = error ?: if (index.ports.isEmpty()) i18n.text("text.e60325912bed") else null,
    modifier = modifier
) }

@Composable
fun TargetPicker(index: ProjectIndex, target: TargetRef, onChange: (TargetRef) -> Unit, error: String? = null) {
    val i18n = LocalMessages.current

    OptionPicker(
        label = i18n.text("text.254b7ac2475c"),
        options = TargetRef.TARGET_TYPES,
        selected = target.type,
        optionLabel = { EntityTypeLabels.of(it, i18n) },
        onSelected = { it?.let { type -> if (type != target.type) onChange(TargetRef(type, null)) } }
    )
    when (target.type) {
        "DEVICE" -> DevicePicker(i18n.text("text.e7f2c0e68768"), index, target.id, { onChange(target.copy(id = it)) }, error = error)
        "RACK" -> OptionPicker(i18n.text("text.67c1ef0fff56"), index.project.racks, index.rack(target.id), { it.name }, { onChange(target.copy(id = it?.id)) },
            isError = error != null, supportingText = error)
        "AREA" -> OptionPicker(i18n.text("text.ddbccb18e085"), index.areas, index.area(target.id), { it.name }, { onChange(target.copy(id = it?.id)) },
            isError = error != null, supportingText = error)
        "PORT" -> PortPicker(i18n.text("text.57c2ec879203"), index, target.id, { onChange(target.copy(id = it)) }, noneLabel = null, error = error)
        else -> Unit
    }
}
