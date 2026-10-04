package com.onlyfield.assetmanager.configurator.map

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.GlyphBadge
import com.onlyfield.assetmanager.configurator.SelectField
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.DevicePresets
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.PresetResult
import com.onlyfield.assetmanager.core.forms.QuickAdd
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
 * Quick insertion from the map: [onAdd] gets a draft ready to save, [onEdit] opens the full editor.
 * Cables can only be added on the floor ([parent] null) and always open the editor.
 */
@Composable
fun MapObjectPicker(project: Project, i18n: Messages, areaId: String, parent: ObjectRef?, point: MapPoint?, onClose: () -> Unit,
                    onAdd: (MapObjectDraft) -> Unit, onEdit: (MapObjectDraft) -> Unit) {
    val subtitle = remember(project, areaId, parent) { placementLabel(project, areaId, parent, i18n) }
    ObjectPickerDialog(project, i18n, subtitle, onClose, base = { newObjectDraft(project, it, null, areaId, parent, point) }, onAdd = onAdd, onEdit = onEdit,
        filter = { parent == null || it.kind != ObjectKind.CABLE })
}

/**
 * Quick insertion in one dialog: type (grouped, searchable, with icons), then prefilled menus
 * (name, preset ports, rack height, business unit when missing) and Add, which saves at once.
 * Types without menus are added with one tap; a single matching type skips the list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObjectPickerDialog(project: Project, i18n: Messages, subtitle: String?, onClose: () -> Unit,
                       base: (ObjectType) -> MapObjectDraft, onAdd: (MapObjectDraft) -> Unit, onEdit: ((MapObjectDraft) -> Unit)? = null,
                       filter: (ObjectType) -> Boolean = { true }) {
    val allTypes = remember(project) { ObjectCatalog.types(project).filter(filter) }
    var query by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf(false) }
    var customType by remember { mutableStateOf<ObjectType?>(null) }
    var chosen by remember { mutableStateOf(allTypes.singleOrNull()) }
    val start = remember(chosen) { chosen?.let(base) }
    val preset = chosen?.let { DevicePresets.forType(it.id) }
    // A business unit is asked only when the context has none and there is a real choice.
    val askBu = start?.type?.kind == ObjectKind.DEVICE && start.buId.isBlank() && project.businessUnits.size > 1
    var values by remember(chosen) { mutableStateOf(preset?.defaults().orEmpty()) }
    var name by remember(chosen) { mutableStateOf(chosen?.let { suggestName(project, it) }.orEmpty()) }
    var height by remember(chosen) { mutableStateOf(start?.rack?.heightU?.toIntOrNull() ?: 42) }
    var buId by remember(chosen) { mutableStateOf(start?.takeIf { it.buId.isBlank() }?.let { project.businessUnits.firstOrNull()?.id }) }
    val draft = start?.let { QuickAdd.draft(it, name, preset?.result(values), height, buId) }
    val errors = draft?.errors(project, i18n).orEmpty()

    fun pick(type: ObjectType) {
        val first = base(type)
        if (type.kind == ObjectKind.CABLE) { onEdit?.invoke(first); return }
        val bu = if (first.buId.isBlank()) project.businessUnits.firstOrNull()?.id else null
        val quick = QuickAdd.draft(first, suggestName(project, type), buId = bu)
        val needs = QuickAdd.needsDetails(quick, DevicePresets.forType(type.id) != null, quick.errors(project, i18n).isNotEmpty()) ||
            (first.buId.isBlank() && project.businessUnits.size > 1)
        if (needs) chosen = type else onAdd(quick)
    }

    val title = when {
        chosen != null -> ObjectCatalog.displayName(chosen!!, i18n)
        custom -> i18n.text("text.7a83d7ae0c15")
        else -> i18n.text("map.addObject")
    }
    AlertDialog(onDismissRequest = onClose, title = {
        Column {
            Text(title, modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                draft != null -> {
                    OutlinedTextField(name, { name = it }, label = { Text(i18n.text("config.name")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        isError = errors["technicalName"] != null || errors["name"] != null)
                    if (draft.type.kind == ObjectKind.RACK)
                        ValueMenu(i18n.text("config.units"), height, (HardwareConfigurator.rackHeights + height).distinct().sorted(), { "$it U" }, Modifier.fillMaxWidth()) { height = it }
                    if (askBu) ValueMenu(i18n.text("config.bu"), project.businessUnits.find { it.id == buId }, project.businessUnits, { it?.name.orEmpty() }, Modifier.fillMaxWidth()) { buId = it?.id }
                    preset?.let { p ->
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.params.forEach { param ->
                                ValueMenu(i18n.text("preset.param.${param.key}"), values.getValue(param.key), param.values,
                                    { if (param.labelled) i18n.text("preset.value.$it") else it }, Modifier.widthIn(min = 140.dp).weight(1f)) { values = values + (param.key to it) }
                            }
                        }
                        Text(presetSummary(p.result(values)), style = MaterialTheme.typography.titleSmall)
                    }
                    errors.values.distinct().takeIf { it.isNotEmpty() }?.let {
                        Text(it.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    // Secondary path inside the body, so Back and Add always share one row on phones.
                    if (onEdit != null) OutlinedButton(enabled = errors.isEmpty(), onClick = { onEdit(draft) }, modifier = Modifier.fillMaxWidth()) { Text(i18n.text("quick.addAndEdit")) }
                }
                custom -> CustomTypeForm(i18n) { customType = it }
                else -> {
                    OutlinedTextField(query, { query = it }, label = { Text(i18n.text("text.271a55491c4f")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    val types = allTypes.filter { ObjectCatalog.displayName(it, i18n).contains(query.trim(), true) }
                    val grouped = types.groupBy { if (it.kind == ObjectKind.CABLE) null else ObjectGlyph.of(it).family }
                        .toSortedMap(compareBy(nullsLast()) { it?.ordinal })
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        grouped.forEach { (family, list) ->
                            item(key = "h-${family?.name}") {
                                Text(family?.let { familyLabel(it, i18n) } ?: i18n.text("map.family.CABLE"), style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
                            }
                            items(list.sortedBy { ObjectCatalog.displayName(it, i18n).lowercase() }, key = { it.id }) { type ->
                                TextButton(onClick = { pick(type) }, modifier = Modifier.fillMaxWidth()) {
                                    GlyphBadge(ObjectGlyph.of(type), 28.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(ObjectCatalog.displayName(type, i18n), modifier = Modifier.weight(1f))
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
            draft != null -> Button(enabled = errors.isEmpty(), onClick = { onAdd(draft) }) { Text(i18n.text("ux.add")) }
            // A custom type is created together with the object: the draft carries it into the project.
            custom -> Button(enabled = customType != null, onClick = { custom = false; chosen = customType }) { Text(i18n.text("catalog.continue")) }
        }
    }, dismissButton = {
        val back = custom || (chosen != null && allTypes.size > 1)
        if (back) TextButton(onClick = { if (custom) { custom = false; customType = null } else chosen = null }) { Text(i18n.text("map.back")) }
        else TextButton(onClick = onClose) { Text(i18n.text("ux.cancel")) }
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
