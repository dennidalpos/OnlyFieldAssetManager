package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.GlyphBadge
import com.onlyfield.assetmanager.configurator.SelectField
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.DevicePresets
import com.onlyfield.assetmanager.core.forms.PresetResult
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/** Labelled dropdown with fixed values. */
@Composable
fun <T> ValueMenu(label: String, value: T, values: List<T>, display: (T) -> String, modifier: Modifier = Modifier, change: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    // The popup matches the field width, so long labels stay readable on phones.
    Box(modifier.onSizeChanged { width = with(density) { it.width.toDp() } }) {
        SelectField(label, display(value)) { open = true }
        DropdownMenu(open, { open = false }, modifier = Modifier.width(width).heightIn(max = 360.dp)) {
            values.forEach { v -> DropdownMenuItem(text = { Text(display(v)) }, onClick = { change(v); open = false }) }
        }
    }
}

fun familyLabel(family: ObjectFamily, i18n: Messages) = i18n.text("map.family.${family.name}")

/** Port groups in one line, e.g. "24 × RJ45 (PoE) · 4 × SFP+". */
fun portGroupsSummary(groups: List<PortTemplate>): String = groups.joinToString(" · ") { g ->
    "${g.portCount} × ${g.connector ?: g.namePrefix}" + (if (g.poeStandard != null) " (PoE)" else "") + (if (g.pairedSides) " ⇄" else "")
}

/** Ports produced by a preset. */
fun presetSummary(result: PresetResult): String = portGroupsSummary(result.groups)

/** "In RACK-A" or "On floor Terra": where the new object will be placed. */
fun placementLabel(project: Project, areaId: String, parent: ObjectRef?, i18n: Messages): String =
    parent?.let { i18n.text("picker.inContainer", ObjectHierarchy.name(project, it, i18n)) }
        ?: i18n.text("picker.onFloor", ProjectIndex(project).areaName(areaId))

/**
 * Insertion flow in one dialog: type (grouped by family, searchable) → preset values when the
 * type has one. The title always says where the object goes; the configurator opens afterwards.
 * Cables can only be added on the floor ([parent] null).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObjectPickerDialog(project: Project, i18n: Messages, areaId: String, parent: ObjectRef?, onClose: () -> Unit,
                       onPick: (ObjectType, PresetResult?) -> Unit, onCustom: (ObjectType) -> Unit) {
    var query by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<ObjectType?>(null) }
    val preset = chosen?.let { DevicePresets.forType(it.id) }
    var values by remember(chosen) { mutableStateOf(preset?.defaults().orEmpty()) }
    var customType by remember { mutableStateOf<ObjectType?>(null) }
    val placement = remember(project, areaId, parent) { placementLabel(project, areaId, parent, i18n) }
    val title = when {
        preset != null -> ObjectCatalog.displayName(chosen!!, i18n)
        custom -> i18n.text("text.7a83d7ae0c15")
        else -> i18n.text("map.addObject")
    }
    val step = i18n.text(if (preset != null) "picker.step.ports" else "picker.step.type")
    AlertDialog(onDismissRequest = onClose, title = {
        Column {
            Text(title, modifier = Modifier.semantics { heading() })
            Text("$placement · $step", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                preset != null -> {
                    Text(i18n.text("catalog.presetHint"), style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        preset.params.forEach { param ->
                            ValueMenu(i18n.text("preset.param.${param.key}"), values.getValue(param.key), param.values,
                                { if (param.labelled) i18n.text("preset.value.$it") else it }, Modifier.widthIn(min = 160.dp).weight(1f)) { values = values + (param.key to it) }
                        }
                    }
                    Text(presetSummary(preset.result(values)), style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { onPick(chosen!!, null) }) { Text(i18n.text("picker.manualPorts")) }
                }
                custom -> CustomTypeForm(i18n) { customType = it }
                else -> {
                    OutlinedTextField(query, { query = it }, label = { Text(i18n.text("text.271a55491c4f")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    val types = ObjectCatalog.types(project).filter { (parent == null || it.kind != ObjectKind.CABLE) && ObjectCatalog.displayName(it, i18n).contains(query.trim(), true) }
                    val grouped = types.groupBy { if (it.kind == ObjectKind.CABLE) null else ObjectGlyph.of(it).family }
                        .toSortedMap(compareBy(nullsLast()) { it?.ordinal })
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        grouped.forEach { (family, list) ->
                            item(key = "h-${family?.name}") {
                                Text(family?.let { familyLabel(it, i18n) } ?: i18n.text("map.family.CABLE"), style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
                            }
                            items(list.sortedBy { ObjectCatalog.displayName(it, i18n).lowercase() }, key = { it.id }) { type ->
                                val hasPreset = DevicePresets.forType(type.id) != null
                                TextButton(onClick = { if (hasPreset) chosen = type else onPick(type, null) }, modifier = Modifier.fillMaxWidth()) {
                                    GlyphBadge(ObjectGlyph.of(type), 28.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(ObjectCatalog.displayName(type, i18n), modifier = Modifier.weight(1f))
                                    if (hasPreset) Text("›", Modifier.clearAndSetSemantics {})
                                }
                            }
                        }
                        if (types.isEmpty()) item(key = "empty") { Text(i18n.text("ux.noResults"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                    // Pinned below the list so it stays visible whatever the scroll position.
                    HorizontalDivider()
                    TextButton(onClick = { custom = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("+ " + i18n.text("text.7a83d7ae0c15"), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }, confirmButton = {
        when {
            preset != null -> Button(onClick = { onPick(chosen!!, preset.result(values)) }) { Text(i18n.text("catalog.continue")) }
            custom -> Button(enabled = customType != null, onClick = { customType?.let(onCustom) }) { Text(i18n.text("text.62a5786b6d5a")) }
        }
    }, dismissButton = {
        when {
            preset != null -> TextButton(onClick = { chosen = null }) { Text(i18n.text("map.back")) }
            custom -> TextButton(onClick = { custom = false; customType = null }) { Text(i18n.text("map.back")) }
            else -> TextButton(onClick = onClose) { Text(i18n.text("ux.cancel")) }
        }
    })
}

/** Reports a valid type (name filled) on every change, or null; the dialog button creates it. */
@Composable
private fun CustomTypeForm(i18n: Messages, onValid: (ObjectType?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DeviceCategory.CUSTOM) }
    var container by remember { mutableStateOf(false) }
    LaunchedEffect(name, category, container) {
        onValid(name.trim().takeIf { it.isNotEmpty() }?.let { ObjectType(name = it, category = category, canContainObjects = container) })
    }
    OutlinedTextField(name, { name = it }, label = { Text(i18n.text("text.12db8de268a5")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    ValueMenu(i18n.text("text.430f47238ebc"), category, DeviceCategory.entries, { it.toDisplayString(i18n = i18n) }) { category = it }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(container, role = Role.Checkbox) { container = it }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(container, null); Text(i18n.text("text.686247e8bde9"))
    }
}
