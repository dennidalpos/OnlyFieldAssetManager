package com.onlyfield.assetmanager.ui.components

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
) = OptionPicker(
    label = label,
    options = index.devices,
    selected = index.device(selectedId),
    optionLabel = { it.technicalName },
    optionDetail = { d -> listOfNotNull(d.category.toDisplayString(), d.ipAddress, index.areaName(d.areaId, "").ifBlank { null }).joinToString(" · ") },
    onSelected = { onSelected(it?.id) },
    noneLabel = noneLabel,
    isError = error != null,
    supportingText = error ?: if (index.devices.isEmpty()) "Nessun apparato: crealo prima nell'inventario" else null,
    modifier = modifier
)

@Composable
fun PortPicker(
    label: String,
    index: ProjectIndex,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    noneLabel: String? = "Non collegata",
    error: String? = null,
    busyPortIds: Set<String> = emptySet(),
) = OptionPicker(
    label = label,
    options = index.ports,
    selected = index.port(selectedId),
    optionLabel = { "${it.device.technicalName} › ${it.port.name}" },
    optionDetail = { ref -> listOfNotNull(ref.port.label, if (ref.port.id in busyPortIds) "già collegata" else null).joinToString(" · ") },
    onSelected = { onSelected(it?.port?.id) },
    noneLabel = noneLabel,
    isError = error != null,
    supportingText = error ?: if (index.ports.isEmpty()) "Nessuna porta: aggiungile dal dettaglio apparato" else null,
    modifier = modifier
)

@Composable
fun TargetPicker(index: ProjectIndex, target: TargetRef, onChange: (TargetRef) -> Unit, error: String? = null) {
    OptionPicker(
        label = "Riferito a",
        options = TargetRef.TARGET_TYPES,
        selected = target.type,
        optionLabel = EntityTypeLabels::of,
        onSelected = { it?.let { type -> if (type != target.type) onChange(TargetRef(type, null)) } }
    )
    when (target.type) {
        "DEVICE" -> DevicePicker("Apparato *", index, target.id, { onChange(target.copy(id = it)) }, error = error)
        "RACK" -> OptionPicker("Rack *", index.project.racks, index.rack(target.id), { it.name }, { onChange(target.copy(id = it?.id)) },
            isError = error != null, supportingText = error)
        "AREA" -> OptionPicker("Area *", index.areas, index.area(target.id), { it.name }, { onChange(target.copy(id = it?.id)) },
            isError = error != null, supportingText = error)
        "PORT" -> PortPicker("Porta *", index, target.id, { onChange(target.copy(id = it)) }, noneLabel = null, error = error)
        else -> Unit
    }
}
