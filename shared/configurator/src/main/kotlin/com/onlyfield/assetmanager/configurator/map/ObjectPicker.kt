package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.SelectField
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.DevicePresets
import com.onlyfield.assetmanager.core.forms.PresetResult
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/** Labelled dropdown with fixed values. */
@Composable
fun <T> ValueMenu(label: String, value: T, values: List<T>, display: (T) -> String, modifier: Modifier = Modifier, change: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        SelectField(label, display(value)) { open = true }
        DropdownMenu(open, { open = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            values.forEach { v -> DropdownMenuItem(text = { Text(display(v)) }, onClick = { change(v); open = false }) }
        }
    }
}

fun familyLabel(family: ObjectFamily, i18n: Messages) = i18n.text("map.family.${family.name}")

/** Ports produced by a preset, e.g. "24 × RJ45 (PoE) · 4 × SFP+". */
fun presetSummary(result: PresetResult): String = result.groups.joinToString(" · ") { g ->
    "${g.portCount} × ${g.connector ?: g.namePrefix}" + (if (g.poeStandard != null) " (PoE)" else "") + (if (g.pairedSides) " ⇄" else "")
}

/**
 * Insertion flow: type (grouped by family, searchable) → preset menus when available.
 * A single dialog; the configurator opens afterwards with the essentials only.
 */
@Composable
fun ObjectPickerDialog(project: Project, i18n: Messages, allowCables: Boolean, onClose: () -> Unit, onPick: (ObjectType, PresetResult?) -> Unit, onCustom: (ObjectType) -> Unit) {
    var query by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<ObjectType?>(null) }
    val preset = chosen?.let { DevicePresets.forType(it.id) }
    var values by remember(chosen) { mutableStateOf(preset?.defaults().orEmpty()) }
    AlertDialog(onDismissRequest = onClose, title = { Text(chosen?.let { ObjectCatalog.displayName(it, i18n) } ?: i18n.text("text.8502424a65de")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                preset != null -> {
                    Text(i18n.text("catalog.presetHint"), style = MaterialTheme.typography.bodySmall)
                    preset.params.forEach { param ->
                        ValueMenu(i18n.text("preset.param.${param.key}"), values.getValue(param.key), param.values,
                            { if (param.labelled) i18n.text("preset.value.$it") else it }) { values = values + (param.key to it) }
                    }
                    Text(presetSummary(preset.result(values)), style = MaterialTheme.typography.titleSmall)
                }
                custom -> CustomTypeForm(i18n, onCustom)
                else -> {
                    OutlinedTextField(query, { query = it }, label = { Text(i18n.text("text.271a55491c4f")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    val types = ObjectCatalog.types(project).filter { (allowCables || it.kind != ObjectKind.CABLE) && ObjectCatalog.displayName(it, i18n).contains(query.trim(), true) }
                    val grouped = types.groupBy { if (it.kind == ObjectKind.CABLE) null else ObjectGlyph.of(it).family }
                        .toSortedMap(compareBy(nullsLast()) { it?.ordinal })
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        grouped.forEach { (family, list) ->
                            item(key = "h-${family?.name}") {
                                Text(family?.let { familyLabel(it, i18n) } ?: i18n.text("map.family.CABLE"), style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                            }
                            items(list.sortedBy { ObjectCatalog.displayName(it, i18n).lowercase() }, key = { it.id }) { type ->
                                TextButton(onClick = { if (DevicePresets.forType(type.id) != null) chosen = type else onPick(type, null) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(ObjectGlyph.of(type).code, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
                                    Text(ObjectCatalog.displayName(type, i18n), modifier = Modifier.weight(1f))
                                    if (DevicePresets.forType(type.id) != null) Text("›")
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { custom = true }) { Text(i18n.text("text.7a83d7ae0c15")) }
                }
            }
        }
    }, confirmButton = {
        if (preset != null) Button(onClick = { onPick(chosen!!, preset.result(values)) }) { Text(i18n.text("catalog.continue")) }
        else TextButton(onClick = onClose) { Text(i18n.text("text.32d4079b315b")) }
    }, dismissButton = {
        if (preset != null) Row {
            TextButton(onClick = { chosen = null }) { Text(i18n.text("map.back")) }
            TextButton(onClick = { onPick(chosen!!, null) }) { Text(i18n.text("catalog.noPreset")) }
        } else if (custom) TextButton(onClick = { custom = false }) { Text(i18n.text("map.back")) }
    })
}

@Composable
private fun CustomTypeForm(i18n: Messages, onCustom: (ObjectType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DeviceCategory.CUSTOM) }
    var container by remember { mutableStateOf(false) }
    OutlinedTextField(name, { name = it }, label = { Text(i18n.text("text.12db8de268a5")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    ValueMenu(i18n.text("text.430f47238ebc"), category, DeviceCategory.entries, { it.toDisplayString(i18n = i18n) }) { category = it }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(container, role = Role.Checkbox) { container = it }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(container, null); Text(i18n.text("text.686247e8bde9"))
    }
    Button(enabled = name.isNotBlank(), onClick = { onCustom(ObjectType(name = name.trim(), category = category, canContainObjects = container)) }) { Text(i18n.text("text.62a5786b6d5a")) }
}
