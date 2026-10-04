package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.configurator.map.ValueMenu
import com.onlyfield.assetmanager.configurator.map.portGroupsSummary
import com.onlyfield.assetmanager.configurator.map.presetSummary

@Composable
private fun Choice(label: String, value: String, values: List<String>, error: String? = null, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(value, change, label = { Text(label) }, singleLine = true,
            isError = error != null, supportingText = error?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { SymbolButton("▾", LocalConfiguratorMessages.current.text("ux.showOptions")) { expanded = true } })
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 300.dp)) {
            values.distinctBy { it.trim().lowercase() }.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { change(item); expanded = false })
            }
        }
    }
}

@Composable
private fun <T> Pick(label: String, selected: T?, options: List<T>, i18n: Messages, display: (T) -> String,
                     error: String? = null, allowClear: Boolean = true, sortByName: Boolean = true, change: (T?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val ordered = remember(options, i18n.locale) {
        if (sortByName) options.sortedWith(compareBy(java.text.Collator.getInstance(i18n.locale)) { display(it) }) else options
    }
    val filtered = ordered.filter { query.isBlank() || display(it).contains(query.trim(), true) }
    Box {
        SelectField(label, selected?.let(display) ?: i18n.text("ux.noSelection"), error = error) { query = ""; expanded = true }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 360.dp).widthIn(max = 480.dp)) {
            if (options.size > 7) OutlinedTextField(query, { query = it }, label = { Text(i18n.text("ux.search")) },
                singleLine = true, modifier = Modifier.padding(horizontal = 8.dp).widthIn(max = 320.dp))
            if (allowClear && query.isBlank()) DropdownMenuItem(text = { Text(i18n.text("ux.noSelection")) }, onClick = { change(null); expanded = false })
            filtered.forEach { item ->
                DropdownMenuItem(text = { Text(display(item)) }, onClick = { change(item); expanded = false })
            }
            if (filtered.isEmpty()) DropdownMenuItem(text = { Text(i18n.text(if (options.isEmpty()) "ux.emptyOptions" else "ux.noResults")) }, onClick = {}, enabled = false)
        }
    }
}

@Composable
private fun Field(label: String, value: String, error: String? = null, singleLine: Boolean = true, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, isError = error != null,
        supportingText = error?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), singleLine = singleLine)
}

/** One staged session is committed by the host's existing Save/Undo operation. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ObjectConfigurator(project: Project, draft: MapObjectDraft, i18n: Messages, modelEditor: Boolean = false, initialSection: ConfiguratorPage = ConfiguratorPage.ESSENTIALS, change: (MapObjectDraft) -> Unit) {
    CompositionLocalProvider(LocalConfiguratorMessages provides i18n) {
        ConfiguratorBody(project, draft, i18n, modelEditor, initialSection, change)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ConfiguratorBody(project: Project, draft: MapObjectDraft, i18n: Messages, modelEditor: Boolean, initialSection: ConfiguratorPage, change: (MapObjectDraft) -> Unit) {
    val preview = remember(project, draft, i18n) { draft.preview(project, i18n) }
    val index = remember(preview) { ProjectIndex(preview) }
    val graph = remember(preview) { ConnectionGraph(preview) }
    var activePage by remember(draft.id) { mutableStateOf(initialSection) }
    var portId by remember(draft.id) { mutableStateOf<String?>(null) }
    var nested by remember(draft.id) { mutableStateOf<MapObjectDraft?>(null) }
    var saveModel by remember(draft.id) { mutableStateOf(false) }
    var modelName by remember(draft.id) { mutableStateOf("") }
    var side by remember(draft.id) { mutableStateOf(PortSide.FRONT) }
    var selectedPorts by remember(draft.id) { mutableStateOf(emptySet<String>()) }
    fun stage(p: Project) = change(draft.copy(session = ConfigurationSession(draft.session?.original ?: project, p)))
    fun openDevice(device: Device) {
        stage(preview)
        val bu = index.businessUnitOf(device.id)?.id ?: draft.buId
        nested = MapObjectDraft.device(preview, bu, ObjectMap.areaId(preview, device).orEmpty(), device.id, i18n)
    }
    val rootName = when (draft.type.kind) { ObjectKind.DEVICE -> draft.device.technicalName; ObjectKind.RACK -> draft.rack.name; ObjectKind.CABLE -> draft.cable.codeOrLabel }
    nested?.let { child ->
        Trail(listOf(rootName, child.device.technicalName.ifBlank { ObjectCatalog.displayName(child.type, i18n) }))
        TextButton(onClick = { nested = null }) { Text(i18n.text("config.cancelBack")) }
        ObjectConfigurator(preview, child, i18n) { nested = it }
        Button(onClick = { stage(child.apply(preview, i18n)); nested = null }, enabled = child.errors(preview, i18n).isEmpty()) { Text(i18n.text("config.applyBack")) }
        return
    }
    portId?.let { id -> index.port(id)?.let { ref ->
        val bringIntoView = remember(id) { BringIntoViewRequester() }
        Trail(listOf(rootName, ref.port.name))
        TextButton(onClick = { portId = null }, modifier = Modifier.bringIntoViewRequester(bringIntoView)) { Text(i18n.text("config.backToObject")) }
        LaunchedEffect(id) { bringIntoView.bringIntoView() }
        PortConfiguration(preview, ref, draft.id, i18n, { stage(it) }, { openDevice(it) }, { type ->
            stage(preview)
            nested = MapObjectDraft(type = type, buId = draft.buId, areaId = draft.areaId)
        })
        return
    } }

    val errors = remember(project, draft) { draft.errors(project, i18n) }
    val bu = preview.businessUnits.find { it.id == draft.device.businessUnitId }
    Text(i18n.text("ux.essentialHint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val name = when (draft.type.kind) {
        ObjectKind.DEVICE -> draft.device.technicalName
        ObjectKind.RACK -> draft.rack.name
        ObjectKind.CABLE -> draft.cable.codeOrLabel
    }
    Field(i18n.text("config.name"), name, error = errors["technicalName"] ?: errors["name"]) { value ->
        change(when (draft.type.kind) {
            ObjectKind.DEVICE -> draft.copy(device = draft.device.copy(technicalName = value))
            ObjectKind.RACK -> draft.copy(rack = draft.rack.copy(name = value))
            ObjectKind.CABLE -> draft.copy(cable = draft.cable.copy(codeOrLabel = value))
        })
    }
    Pick(i18n.text("config.objectType"), draft.type, ObjectCatalog.types(preview).filter { it.kind == draft.type.kind }, i18n, { ObjectCatalog.displayName(it, i18n) }, allowClear = false) { type ->
        if (type != null) change(draft.copy(type = type, device = draft.device.copy(objectTypeId = type.id, category = type.category)))
    }
    val models = preview.deviceModels.filter { it.kind == draft.type.kind && (it.objectTypeId == null || it.objectTypeId == draft.type.id || (draft.type.kind == ObjectKind.DEVICE && it.category == draft.device.category)) }
    if (!modelEditor) {
        val selectedModelId = when (draft.type.kind) { ObjectKind.DEVICE -> draft.device.deviceModelId; ObjectKind.RACK -> draft.rack.deviceModelId; ObjectKind.CABLE -> draft.cable.deviceModelId }
        Pick(i18n.text("config.model"), models.find { it.id == selectedModelId }, models, i18n, { it.name }) { model ->
            if (model != null) {
                val oldExtras = draft.extraFields ?: preview.customExtraFields.filter { it.targetId == draft.id }
                change(HardwareConfigurator.applyModel(draft.copy(extraFields = oldExtras), model).copy(allowConnectedRemoval = false))
            } else change(draft.copy(device = draft.device.copy(deviceModelId = null), rack = draft.rack.copy(deviceModelId = null), cable = draft.cable.copy(deviceModelId = null)))
        }
    }
    if (!modelEditor && draft.type.kind != ObjectKind.CABLE) {
        val self = ObjectRef(if (draft.type.kind == ObjectKind.RACK) PlacementTargetType.RACK else PlacementTargetType.DEVICE, draft.id)
        val parents = ObjectHierarchy.refs(preview).filter { it != self && ObjectHierarchy.canContain(preview, it) && self !in ObjectHierarchy.ancestors(preview, it) }
        Pick(i18n.text("config.container"), draft.parentRef, parents, i18n, { ObjectHierarchy.name(preview, it, i18n) }) { parent ->
            val rack = parent?.let { (listOf(it) + ObjectHierarchy.ancestors(preview, it)).firstOrNull { it.type == PlacementTargetType.RACK } }
            change(draft.copy(parentRef = parent, device = draft.device.copy(rackId = rack?.id, mountingType = if (rack == null) MountingType.OUT_OF_RACK else MountingType.RACK_MOUNT)))
        }
    }
    when (draft.type.kind) {
        ObjectKind.DEVICE -> {
            val d = draft.device
            val h = d.hardware
            if (!modelEditor) {
                Pick(i18n.text("config.bu"), preview.businessUnits.find { it.id == d.businessUnitId }, preview.businessUnits, i18n, { it.name }, error = errors["businessUnitId"], allowClear = false) { it?.let { b -> change(draft.copy(buId = b.id, device = d.copy(businessUnitId = b.id))) } }
                Pick(i18n.text("config.floor"), index.area(d.areaId), bu?.let { ObjectMap.areas(it) }.orEmpty(), i18n, { it.name }) { change(draft.copy(device = d.copy(areaId = it?.id))) }
            }
            if (modelEditor || d.rackId != null) {
                Choice(i18n.text("config.units"), d.heightU, (listOf(1, 2, 3, 4) + index.devices.map { it.heightU }).map { it.toString() }, error = errors["heightU"]) { change(draft.copy(device = d.copy(heightU = it))) }
                if (!modelEditor && d.rackId != null) {
                    val rack = index.rack(d.rackId)
                    val free = rack?.let { RackLayout.freeStartPositions(it, index.devices, d.heightU.toIntOrNull() ?: 1, d.rackSide, draft.id) }.orEmpty()
                    Choice(i18n.text("config.position"), d.positionU, free.map { it.toString() }, error = errors["positionU"]) { change(draft.copy(device = d.copy(positionU = it))) }
                    Pick(i18n.text("config.side"), d.rackSide, RackSide.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { s -> change(draft.copy(device = d.copy(rackSide = s))) } }
                }
            }
            ConfiguratorSection(i18n.text("ux.ports"), activePage == ConfiguratorPage.PORTS, errors["ports"], focusOnOpen = activePage == ConfiguratorPage.PORTS) {
                PresetBar(draft, i18n, change)
                PortGroups(preview, draft, i18n, change)
                val device = index.device(draft.id)
                if (device != null) {
                    if ((draft.portsConfigured || h.portGroups.isNotEmpty()) && HardwareConfigurator.validGroups(h.portGroups)) {
                        val existing = (draft.session?.apply(project) ?: project).businessUnits.flatMap { it.devices }.find { it.id == draft.id } ?: device.copy(ports = emptyList())
                        val differences = HardwareConfigurator.preview(preview, existing, h.portGroups)
                        Text(i18n.text("config.differences", differences.added, differences.removed.size, differences.connectedRemoved.size))
                        if (differences.connectedRemoved.isNotEmpty()) Toggle(i18n.text("config.removeConnected"), draft.allowConnectedRemoval) { change(draft.copy(allowConnectedRemoval = it)) }
                    }
                    val cells = remember(preview, device.id) { PortLogic.panel(preview, device, graph, index) }
                    // Unsaved ports get new ids on every preview: keep only ids still shown.
                    val selection = selectedPorts.filterTo(mutableSetOf()) { id -> cells.any { it.port.id == id } }
                    fun toggle(id: String) {
                        // Staging the preview freezes generated port ids while the selection is in use.
                        if (selection.isEmpty()) stage(preview)
                        selectedPorts = if (id in selection) selection - id else selection + id
                    }
                    PortPanel(cells, i18n, selected = selection,
                        onClick = { cell -> if (selection.isNotEmpty()) toggle(cell.port.id) else if (!modelEditor) { stage(preview); activePage = ConfiguratorPage.PORTS; portId = cell.port.id } },
                        onLongClick = { cell: PortCell -> toggle(cell.port.id) }.takeIf { !modelEditor })
                    if (!modelEditor) BulkPortBar(preview, cells, selection, i18n, { if (it.isNotEmpty() && selection.isEmpty()) stage(preview); selectedPorts = it }) { stage(it) }
                }
            }
            ConfiguratorSection(i18n.text("ux.hardware"), error = if (d.rackId == null) errors["heightU"] else null) {
                Pick(i18n.text("config.category"), d.category, DeviceCategory.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { c -> change(draft.copy(device = d.copy(category = c))) } }
                // Built-in types have a fixed container role; only custom types can change it.
                if (ObjectCatalog.builtins.none { it.id == draft.type.id } && draft.type.id != "legacy")
                    Toggle(i18n.text("config.canContain"), draft.type.canContainObjects) { enabled ->
                        if (enabled || ObjectHierarchy.children(preview, ObjectRef(PlacementTargetType.DEVICE, draft.id)).isEmpty())
                            change(draft.copy(type = draft.type.copy(canContainObjects = enabled)))
                    }
                if (!modelEditor && d.rackId == null) {
                    Choice(i18n.text("config.units"), d.heightU, listOf("1", "2", "3", "4"), error = errors["heightU"]) { change(draft.copy(device = d.copy(heightU = it))) }
                }
                Pick(i18n.text("config.mount"), d.mountingType, MountingType.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { m -> change(draft.copy(device = d.copy(mountingType = m))) } }
                NumericHardware(i18n.text("config.width"), h.widthMm) { change(draft.copy(device = d.copy(hardware = h.copy(widthMm = it)))) }
                NumericHardware(i18n.text("config.depth"), h.depthMm) { change(draft.copy(device = d.copy(hardware = h.copy(depthMm = it)))) }
                Field(i18n.text("config.poeBudget"), h.poeBudgetWatts?.toString().orEmpty()) { value -> if (value.isBlank() || value.toDoubleOrNull()?.let { it >= 0 } == true) change(draft.copy(device = d.copy(hardware = h.copy(poeBudgetWatts = value.toDoubleOrNull())))) }
                Toggle(i18n.text("config.redundant"), h.redundantPower) { change(draft.copy(device = d.copy(hardware = h.copy(redundantPower = it)))) }
                Toggle(i18n.text("config.passive"), h.passive || d.category == DeviceCategory.PATCH_PANEL || draft.type.id == "outlet") { change(draft.copy(device = d.copy(hardware = h.copy(passive = it)))) }
                Choice(i18n.text("config.features"), h.features.joinToString(", "), (listOf("PoE", "LACP", "Stack", "Fanless", "Redundant PSU") + index.devices.flatMap { it.hardware.features })) { value -> change(draft.copy(device = d.copy(hardware = h.copy(features = value.split(',').map { it.trim() }.filter { it.isNotBlank() })))) }
            }
            if (!modelEditor) ConfiguratorSection(i18n.text("ux.identifiers"), error = errors["ipAddress"] ?: errors["macAddress"]) {
                Field(i18n.text("config.label"), d.physicalLabel) { change(draft.copy(device = d.copy(physicalLabel = it))) }
                Field(i18n.text("config.alias"), d.alias) { change(draft.copy(device = d.copy(alias = it))) }
                Field("IP", d.ipAddress, error = errors["ipAddress"]) { change(draft.copy(device = d.copy(ipAddress = it))) }
                Field("MAC", d.macAddress, error = errors["macAddress"]) { change(draft.copy(device = d.copy(macAddress = it))) }
                Field(i18n.text("config.serial"), d.serialNumber) { change(draft.copy(device = d.copy(serialNumber = it))) }
            }
            if (!modelEditor) ConfiguratorSection(i18n.text("ux.notes")) {
                Pick(i18n.text("config.observation"), d.observationStatus, ObservationStatus.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { s -> change(draft.copy(device = d.copy(observationStatus = s))) } }
                Field(i18n.text("config.notes"), d.notes, singleLine = false) { change(draft.copy(device = d.copy(notes = it))) }
            }
        }
        ObjectKind.RACK -> {
            val r = draft.rack
            Choice(i18n.text("config.units"), r.heightU, (HardwareConfigurator.rackHeights + preview.racks.map { it.heightU }).map { it.toString() }, error = errors["heightU"]) { change(draft.copy(rack = r.copy(heightU = it))) }
            if (!modelEditor) Pick(i18n.text("config.floor"), index.area(r.areaId), index.areas, i18n, { it.name }) { change(draft.copy(rack = r.copy(areaId = it?.id))) }
            ConfiguratorSection(i18n.text("ux.hardware"), error = errors["depthMm"] ?: errors["mountingDepthMm"]) {
                Choice(i18n.text("config.depth"), r.depthMm, (HardwareConfigurator.rackDepths + preview.racks.mapNotNull { it.depthMm }).map { it.toString() }, error = errors["depthMm"]) { change(draft.copy(rack = r.copy(depthMm = it))) }
                Choice(i18n.text("config.mountDepth"), r.mountingDepthMm, preview.racks.mapNotNull { it.mountingDepthMm }.map { it.toString() }, error = errors["mountingDepthMm"]) { change(draft.copy(rack = r.copy(mountingDepthMm = it))) }
                Choice(i18n.text("config.mount"), r.mountingType.orEmpty(), listOf("19 inch", "Wall", "Floor", "Open frame") + preview.racks.mapNotNull { it.mountingType }) { change(draft.copy(rack = r.copy(mountingType = it))) }
                Pick(i18n.text("config.numbering"), r.numberingDirection, NumberingDirection.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { n -> change(draft.copy(rack = r.copy(numberingDirection = n))) } }
            }
            if (!modelEditor) ConfiguratorSection(i18n.text("ux.rackContents")) {
                Row { listOf(PortSide.FRONT, PortSide.REAR).forEach { s -> TextButton(onClick = { side = s }) { Text(s.toDisplayString(i18n)) } } }
                val rack = index.rack(draft.id)
                if (rack != null && rack.heightU in 1..60) {
                    SchematicGeometry.rackUnits(rack).forEach { u ->
                        val device = index.devices.firstOrNull { it.rackId == rack.id && it.positionU?.let { p -> u in p until p + it.heightU } == true && (it.rackSide == RackSide.BOTH || it.rackSide.name == side.name) }
                        OutlinedButton(enabled = !modelEditor, onClick = { if (device != null) openDevice(device) else {
                            stage(preview)
                            nested = MapObjectDraft.newObject(preview, ObjectCatalog.builtins.first { it.id == "switch" }, draft.buId, r.areaId.orEmpty(), ObjectRef(PlacementTargetType.RACK, draft.id)).let { child -> child.copy(device = child.device.copy(positionU = u.toString())) }
                        } }, modifier = Modifier.fillMaxWidth()) { Text("U$u · ${device?.technicalName ?: i18n.text("config.available")}") }
                    }
                }
            }
            if (!modelEditor) ConfiguratorSection(i18n.text("ux.notes")) {
                Field(i18n.text("config.notes"), r.notes, singleLine = false) { change(draft.copy(rack = r.copy(notes = it))) }
            }
        }
        ObjectKind.CABLE -> {
            val c = draft.cable
            Pick(i18n.text("config.medium"), c.medium, CableMedium.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { m -> change(draft.copy(cable = c.copy(medium = m))) } }
            if (!modelEditor) ConfiguratorSection(i18n.text("ux.endpoints"), error = errors["ports"] ?: errors["portBId"]) {
                CableEndpoints(preview, draft, i18n, change)
                listOfNotNull(c.portAId, c.portBId).forEach { id -> TextButton(onClick = { stage(preview); portId = id }) { Text(index.portLabel(id)) } }
            }
            ConfiguratorSection(i18n.text("ux.hardware"), error = errors["lengthValue"]) {
                Choice(i18n.text("config.connectorA"), c.connectorA, listOf("RJ45", "LC", "SC", "MPO", "SFP", "SFP+", "QSFP", "USB-C", "C13", "C14") + preview.cables.mapNotNull { it.connectorA }) { change(draft.copy(cable = c.copy(connectorA = it))) }
                Choice(i18n.text("config.connectorB"), c.connectorB, listOf("RJ45", "LC", "SC", "MPO", "SFP", "SFP+", "QSFP", "USB-C", "C13", "C14") + preview.cables.mapNotNull { it.connectorB }) { change(draft.copy(cable = c.copy(connectorB = it))) }
                Choice(i18n.text("config.characteristics"), c.nominalCharacteristics, listOf("Cat5e", "Cat6", "Cat6A", "OS2", "OM3", "OM4", "OM5") + preview.cables.mapNotNull { it.nominalCharacteristics }) { change(draft.copy(cable = c.copy(nominalCharacteristics = it))) }
                if (!modelEditor) Field(i18n.text("config.speed"), c.observedSpeed) { change(draft.copy(cable = c.copy(observedSpeed = it))) }
                Choice(i18n.text("config.color"), c.color, preview.cables.mapNotNull { it.color }) { change(draft.copy(cable = c.copy(color = it))) }
                if (!modelEditor) Field(i18n.text("config.length"), c.lengthValue, error = errors["lengthValue"]) { change(draft.copy(cable = c.copy(lengthValue = it))) }
                Pick(i18n.text("config.orientation"), c.orientation, CableOrientation.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { o -> change(draft.copy(cable = c.copy(orientation = o))) } }
                if (!modelEditor) preview.sharedPathSegments.forEach { segment -> Toggle(segment.name, segment.id in c.sharedPathSegmentIds) { checked -> change(draft.copy(cable = c.copy(sharedPathSegmentIds = if (checked) c.sharedPathSegmentIds + segment.id else c.sharedPathSegmentIds - segment.id))) } }
            }
            if (!modelEditor) ConfiguratorSection(i18n.text("ux.notes")) {
                Field(i18n.text("config.notes"), c.notes, singleLine = false) { change(draft.copy(cable = c.copy(notes = it))) }
            }
        }
    }

    val extras = draft.extraFields ?: preview.customExtraFields.filter { it.targetId == draft.id }
    val reusableExtras = preview.customExtraFields.filter { field ->
        when (draft.type.kind) {
            ObjectKind.DEVICE -> index.device(field.targetId)?.let { it.objectTypeId == draft.type.id || (it.objectTypeId == null && it.category == draft.device.category) } == true
            ObjectKind.RACK -> preview.racks.any { it.id == field.targetId }
            ObjectKind.CABLE -> preview.cables.any { it.id == field.targetId && it.objectTypeId == draft.type.id }
        }
    }
    ConfiguratorSection(i18n.text("ux.customFields")) {
        extras.forEach { field ->
            Choice(i18n.text("config.field"), field.fieldKey, reusableExtras.map { it.fieldKey }) { v -> change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(fieldKey = v) else it })) }
            Choice(i18n.text("config.value"), field.fieldValue, reusableExtras.filter { it.fieldKey == field.fieldKey && it.fieldType == field.fieldType && it.classification == field.classification }.map { it.fieldValue }) { v -> change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(fieldValue = v) else it })) }
            Pick(i18n.text("config.fieldType"), field.fieldType, CustomFieldType.entries, i18n, { it.name }) { t -> t?.let { change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(fieldType = t) else it })) } }
            Pick(i18n.text("config.classification"), field.classification, if (modelEditor) listOf(AttachmentClassification.SHAREABLE) else AttachmentClassification.entries, i18n, { it.toDisplayString(i18n) }) { t -> t?.let { change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(classification = t) else it })) } }
            TextButton(onClick = { change(draft.copy(extraFields = extras.filterNot { it.id == field.id })) }) { Text(i18n.text("config.remove")) }
        }
        OutlinedButton(onClick = { change(draft.copy(extraFields = extras + CustomExtraField(targetType = draft.targetType.name, targetId = draft.id, fieldKey = "", fieldValue = ""))) }) { Text(i18n.text("config.addField")) }
    }
    if (!modelEditor) TextButton(onClick = { saveModel = !saveModel }) { Text(i18n.text("config.saveModel")) }
    if (saveModel) {
        Field(i18n.text("config.model"), modelName) { modelName = it }
        val duplicate = preview.deviceModels.any { it.kind == draft.type.kind && it.name.equals(modelName.trim(), true) }
        if (duplicate) Text(i18n.text("config.modelExists"), color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            val model = HardwareConfigurator.model(preview, draft, modelName)
            change(draft.copy(session = ConfigurationSession(draft.session?.original ?: project, ProjectEdits.addDeviceModel(preview, model)), device = draft.device.copy(deviceModelId = model.id), rack = draft.rack.copy(deviceModelId = model.id), cable = draft.cable.copy(deviceModelId = model.id))); saveModel = false
        }, enabled = modelName.isNotBlank() && !duplicate) { Text(i18n.text("config.saveModel")) }
    }
    if (errors.isNotEmpty()) {
        Text(i18n.text("ux.completeRequired"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
        errors.values.distinct().forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun NumericHardware(label: String, value: Int?, change: (Int?) -> Unit) {
    Field(label, value?.toString().orEmpty()) { s -> if (s.isBlank() || s.toIntOrNull()?.let { it > 0 } == true) change(s.toIntOrNull()) }
}

@Composable
private fun Toggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = value, role = Role.Checkbox, onValueChange = change), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(value, null)
        Text(label, modifier = Modifier.weight(1f))
    }
}

/** Where the editor is: object › port or nested object. */
@Composable
private fun Trail(items: List<String>) {
    Text(items.filter { it.isNotBlank() }.joinToString(" › "), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** Built-in preset for the type: menus with values, applied as port groups. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetBar(draft: MapObjectDraft, i18n: Messages, change: (MapObjectDraft) -> Unit) {
    val preset = DevicePresets.forType(draft.type.id) ?: return
    val groups = draft.device.hardware.portGroups
    // Ports already defined (from the picker or a model): show them, and the menus only on request.
    var open by remember(draft.id, preset.id) { mutableStateOf(groups.isEmpty()) }
    if (!open) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(portGroupsSummary(groups), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { open = true }) { Text(i18n.text("config.changePreset")) }
        }
        return
    }
    var values by remember(draft.id, preset.id) { mutableStateOf(preset.defaults()) }
    Text(i18n.text("preset.title"), style = MaterialTheme.typography.titleSmall)
    Text(i18n.text("preset.hint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        preset.params.forEach { param ->
            ValueMenu(i18n.text("preset.param.${param.key}"), values.getValue(param.key), param.values,
                { if (param.labelled) i18n.text("preset.value.$it") else it }, Modifier.widthIn(min = 140.dp, max = 220.dp)) { values = values + (param.key to it) }
        }
    }
    val result = preset.result(values)
    Text(presetSummary(result), style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { change(DevicePresets.apply(draft, result)); open = false }) { Text(i18n.text("preset.apply")) }
}

private const val CUSTOM_NAMING = "CUSTOM"

private fun namingOf(group: PortTemplate, kind: PortKind?): String = when {
    kind == null -> CUSTOM_NAMING
    group.namePrefix == PortNaming.SHORT.prefix(kind) -> PortNaming.SHORT.name
    group.namePrefix == PortNaming.INTERFACE.prefix(kind, group.speed) -> PortNaming.INTERFACE.name
    else -> CUSTOM_NAMING
}

/** Port groups in three steps: type → quantity → label. Advanced fields stay folded. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PortGroups(project: Project, draft: MapObjectDraft, i18n: Messages, change: (MapObjectDraft) -> Unit) {
    val groups = draft.device.hardware.portGroups
    fun update(next: List<PortTemplate>) = change(draft.copy(device = draft.device.copy(hardware = draft.device.hardware.copy(portGroups = next)), allowConnectedRemoval = false, portsConfigured = true))
    var editingGroup by remember(draft.id) { mutableStateOf<Int?>(null) }
    groups.forEachIndexed { n, g ->
        val others = groups.filterIndexed { i, _ -> i != n }
        fun set(next: PortTemplate) = update(groups.mapIndexed { index, value -> if (index == n) next else value })
        fun rename(prefix: String) = set(g.copy(namePrefix = prefix, startNumber = PortGroups.nextStart(others, prefix)))
        val kind = PortKind.of(g)
        val summary = listOfNotNull("${g.portCount} × ${kind?.let { i18n.text("port.kind.${it.name}") } ?: g.connector ?: g.namePrefix}",
            PortGroups.range(g), g.poeStandard?.let { "PoE" }, g.side.toDisplayString(i18n)).joinToString(" · ")
        OutlinedButton(onClick = { editingGroup = if (editingGroup == n) null else n }, modifier = Modifier.fillMaxWidth()) {
            Text(summary, modifier = Modifier.weight(1f)); Text(if (editingGroup == n) "▴" else "▾")
        }
        if (editingGroup == n) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ValueMenu(i18n.text("preset.param.kind"), kind, PortKind.entries, { it?.let { k -> i18n.text("port.kind.${k.name}") } ?: g.connector.orEmpty() }) { k ->
                if (k != null) {
                    val retyped = PortGroups.retype(g, k)
                    // Keep the naming scheme when the prefix was generated from the old type.
                    val naming = namingOf(g, kind)
                    val prefix = if (naming == CUSTOM_NAMING) g.namePrefix else PortNaming.valueOf(naming).prefix(k)
                    set(retyped.copy(namePrefix = prefix, startNumber = if (prefix == g.namePrefix) g.startNumber else PortGroups.nextStart(others, prefix)))
                }
            }
            Choice(i18n.text("config.count"), g.portCount.toString(), (PortGroups.counts + project.deviceModels.flatMap { it.portTemplates }.map { it.portCount }).distinct().sorted().map { it.toString() }) { v -> v.toIntOrNull()?.takeIf { it in 1..512 }?.let { set(g.copy(portCount = it)) } }
            // "Custom" can be chosen even while the prefix still matches a generated scheme.
            var customChosen by remember(n) { mutableStateOf(false) }
            val naming = if (customChosen) CUSTOM_NAMING else namingOf(g, kind)
            ValueMenu(i18n.text("port.label"), naming, listOfNotNull(PortNaming.SHORT.name.takeIf { kind != null }, PortNaming.INTERFACE.name.takeIf { kind != null }, CUSTOM_NAMING),
                { i18n.text("port.naming.$it") }) { value ->
                customChosen = value == CUSTOM_NAMING
                if (value != CUSTOM_NAMING && kind != null) rename(PortNaming.valueOf(value).prefix(kind, g.speed))
            }
            if (naming == CUSTOM_NAMING) {
                // Typed locally; applied (and renumbered) only on confirm, so typing never resets the start number.
                var prefix by remember(n, g.namePrefix) { mutableStateOf(g.namePrefix) }
                Field(i18n.text("config.prefix"), prefix) { prefix = it }
                TextButton(onClick = { rename(prefix.trim()) }, enabled = prefix.isNotBlank() && prefix.trim() != g.namePrefix) { Text(i18n.text("port.applyPrefix")) }
            }
            Field(i18n.text("config.start"), g.startNumber.toString()) { v -> v.toIntOrNull()?.takeIf { it in 0..9999 }?.let { set(g.copy(startNumber = it)) } }
            Text(i18n.text("port.preview", PortGroups.range(g)), style = MaterialTheme.typography.bodySmall)
            if (g.mediaType == "Copper" || kind == PortKind.RJ45)
                ValueMenu("PoE", g.poeStandard, listOf(null, PoeStandard.IEEE_802_3AF, PoeStandard.IEEE_802_3AT, PoeStandard.IEEE_802_3BT),
                    { it?.toDisplayString(i18n) ?: i18n.text("preset.value.NONE") }) { set(g.copy(poeStandard = it)) }
            ConfiguratorSection(i18n.text("ux.advancedPorts")) {
                Pick(i18n.text("config.side"), g.side, PortSide.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { s -> set(g.copy(side = s)) } }
                Choice(i18n.text("config.speed"), g.speed.orEmpty(), listOf("100M", "1G", "2.5G", "5G", "10G", "25G", "40G", "100G")) { set(g.copy(speed = it)) }
                Choice(i18n.text("config.role"), g.role, listOf("DATA", "UPLINK", "MANAGEMENT", "CONSOLE", "POWER")) { set(g.copy(role = it)) }
                Toggle(i18n.text("config.paired"), g.pairedSides) { set(g.copy(pairedSides = it)) }
                Field(i18n.text("config.combo"), g.comboGroup.orEmpty()) { set(g.copy(comboGroup = it.trim().ifBlank { null })) }
            }
            TextButton(onClick = { editingGroup = null; update(groups.filterIndexed { index, _ -> index != n }) }) { Text(i18n.text("config.remove")) }
        }
    }
    val paired = draft.device.category == DeviceCategory.PATCH_PANEL || draft.type.id == "outlet"
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { editingGroup = groups.size; update(groups + PortGroups.create(groups, PortKind.RJ45, 8, paired = paired)) }) { Text(i18n.text("config.addGroup")) }
        OutlinedButton(onClick = { editingGroup = groups.size; update(groups + PortGroups.create(groups, PortKind.SFP_PLUS, 2)) }) { Text(i18n.text("config.addFiber")) }
    }
}

/** Bulk VLAN/PoE on the ports selected in the panel (long press to start selecting). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BulkPortBar(project: Project, cells: List<PortCell>, selected: Set<String>, i18n: Messages, select: (Set<String>) -> Unit, stage: (Project) -> Unit) {
    if (selected.isEmpty()) {
        if (cells.isNotEmpty()) Text(i18n.text("port.selectHint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var mode by remember { mutableStateOf(PortVlanMode.ACCESS) }
    var untagged by remember { mutableStateOf("") }
    var tagged by remember { mutableStateOf("") }
    var poe by remember { mutableStateOf<PoeStandard?>(PoeStandard.IEEE_802_3AT) }
    val untaggedVlan = untagged.trim().toIntOrNull()?.takeIf { it in 1..4094 }
    val taggedVlans = tagged.split(',', ' ', ';').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..4094 }
    Text(i18n.text("port.selected", selected.size), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { select(cells.map { it.port.id }.toSet()) }) { Text(i18n.text("port.selectAll")) }
        TextButton(onClick = { select(cells.filter { !it.occupied }.map { it.port.id }.toSet()) }) { Text(i18n.text("port.selectFree")) }
        TextButton(onClick = { select(emptySet()) }) { Text(i18n.text("port.clearSelection")) }
    }
    ValueMenu(i18n.text("port.vlanMode"), mode, listOf(PortVlanMode.ACCESS, PortVlanMode.TRUNK), { it.toDisplayString(i18n) }) { mode = it }
    Field(i18n.text(if (mode == PortVlanMode.TRUNK) "port.vlanNative" else "port.vlanUntagged"), untagged) { untagged = it.filter(Char::isDigit).take(4) }
    if (mode == PortVlanMode.TRUNK) Field(i18n.text("port.vlanTagged"), tagged) { tagged = it }
    PortLogic.subnets(project, untaggedVlan).takeIf { it.isNotEmpty() }?.let { subnets ->
        Text(i18n.text("port.subnets", subnets.joinToString { it.cidrBlock }), style = MaterialTheme.typography.bodySmall)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = untaggedVlan != null || (mode == PortVlanMode.TRUNK && taggedVlans.isNotEmpty()),
            onClick = { stage(PortLogic.setVlan(project, selected, mode, untaggedVlan, taggedVlans)) }) { Text(i18n.text("port.applyVlan")) }
        TextButton(onClick = { stage(PortLogic.clearVlan(project, selected)) }) { Text(i18n.text("port.removeVlan")) }
    }
    ValueMenu("PoE", poe, listOf(null, PoeStandard.IEEE_802_3AF, PoeStandard.IEEE_802_3AT, PoeStandard.IEEE_802_3BT), { it?.toDisplayString(i18n) ?: i18n.text("preset.value.NONE") }) { poe = it }
    OutlinedButton(onClick = { stage(PortLogic.setPoe(project, selected, poe)) }) { Text(i18n.text("port.applyPoe")) }
}

@Composable
private fun CableEndpoints(project: Project, draft: MapObjectDraft, i18n: Messages, change: (MapObjectDraft) -> Unit) {
    val index = remember(project) { ProjectIndex(project) }
    val graph = remember(project) { ConnectionGraph(project) }
    listOf(true, false).forEach { first ->
        val id = if (first) draft.cable.portAId else draft.cable.portBId
        val device = id?.let { index.port(it)?.device } ?: index.device(if (first) draft.deviceAId else draft.deviceBId)
        Pick(i18n.text(if (first) "config.endpointA" else "config.endpointB"), device, index.devices, i18n, { it.technicalName }) { d ->
            change(if (first) draft.copy(deviceAId = d?.id, cable = draft.cable.copy(portAId = null)) else draft.copy(deviceBId = d?.id, cable = draft.cable.copy(portBId = null)))
        }
        if (device != null) {
            val ports = device.ports.filter { it.id == id || !graph.occupied(it.id, draft.id) }.filter { it.id != if (first) draft.cable.portBId else draft.cable.portAId }
            Pick(i18n.text("config.port"), device.ports.find { it.id == id }, ports, i18n, { "${it.name} · ${it.hardware.side?.toDisplayString(i18n).orEmpty()} · ${i18n.text("config.available")}" }, sortByName = false) { p ->
                change(if (first) draft.copy(cable = draft.cable.copy(portAId = p?.id)) else draft.copy(cable = draft.cable.copy(portBId = p?.id)))
            }
            Text(i18n.text("config.portCount", device.ports.size, ports.size))
        }
    }
}

private enum class PortTab { LINK, LOGIC, HARDWARE }

/** One port: connection, logical settings (VLAN, PoE, subnet) and hardware, as tabs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortConfiguration(project: Project, ref: ProjectIndex.PortRef, rootId: String, i18n: Messages, stage: (Project) -> Unit, openDevice: (Device) -> Unit, create: (ObjectType) -> Unit) {
    val graph = remember(project) { ConnectionGraph(project) }
    val index = remember(project) { ProjectIndex(project) }
    val port = ref.port
    var tab by remember(port.id) { mutableStateOf(PortTab.LINK) }
    Text("${ref.device.technicalName} › ${port.name}", style = MaterialTheme.typography.titleLarge)
    Text(i18n.text("config.${graph.state(port.id).name.lowercase()}"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PortTab.entries.forEach { t -> FilterChip(selected = tab == t, onClick = { tab = t }, label = { Text(i18n.text("port.tab.${t.name}")) }) }
    }
    when (tab) {
        PortTab.LINK -> PortLink(project, ref, rootId, i18n, graph, index, stage, openDevice, create)
        PortTab.LOGIC -> PortLogicTab(project, port, i18n, stage)
        PortTab.HARDWARE -> PortHardwareTab(project, ref, i18n, index, stage)
    }
}

@Composable
private fun PortLink(project: Project, ref: ProjectIndex.PortRef, rootId: String, i18n: Messages, graph: ConnectionGraph, index: ProjectIndex,
                     stage: (Project) -> Unit, openDevice: (Device) -> Unit, create: (ObjectType) -> Unit) {
    val port = ref.port
    val existing = project.cables.singleOrNull { it.portAId == port.id || it.portBId == port.id }
    val currentDestination = existing?.let { if (it.portAId == port.id) it.portBId else it.portAId }
    var destination by remember(port.id, existing) { mutableStateOf(currentDestination) }
    var medium by remember(port.id, existing) { mutableStateOf(existing?.medium ?: if (port.hardware.mediaType == "Fiber") CableMedium.FIBER_OVERALL else CableMedium.ETHERNET_COPPER) }
    val floor = ObjectMap.areaId(project, ref.device)
    var sameFloor by remember(port.id) { mutableStateOf(floor != null) }
    // One searchable list replaces the BU/floor/rack/device filters; same connector first.
    val candidates = index.ports.filter { p ->
        p.port.id != port.id && (p.port.id == currentDestination || !graph.occupied(p.port.id, existing?.id)) &&
            (!sameFloor || ObjectMap.areaId(project, p.device) == floor)
    }.sortedWith(compareBy({ if (it.port.hardware.connector == port.hardware.connector) 0 else 1 }, { it.device.technicalName.lowercase() }))
    if (floor != null) Toggle(i18n.text("port.sameFloor"), sameFloor) { sameFloor = it }
    Pick(i18n.text("config.destination"), index.port(destination), candidates, i18n, { "${it.device.technicalName} › ${it.port.name} · ${it.port.hardware.side?.toDisplayString(i18n).orEmpty()}" }, sortByName = false) {
        destination = it?.port?.id
        if (!graph.occupied(port.id, existing?.id)) stage(HardwareConfigurator.connect(project, port.id, destination, medium, existing?.id))
    }
    Pick(i18n.text("config.medium"), medium, CableMedium.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { m -> medium = m } }
    val destinationPort = index.port(destination)?.port
    if (port.hardware.connector != null && destinationPort?.hardware?.connector != null && port.hardware.connector != destinationPort.hardware.connector)
        Text(i18n.text("config.incompatible"))
    Button(onClick = { stage(HardwareConfigurator.connect(project, port.id, destination, medium, existing?.id)) }, enabled = !graph.occupied(port.id, existing?.id) && (destination == null || !graph.occupied(destination!!, existing?.id))) { Text(i18n.text("config.connect")) }
    destination?.let { id -> index.port(id)?.device?.let { d -> TextButton(onClick = { openDevice(d) }, enabled = d.id != rootId) { Text(i18n.text("config.openDestination")) } } }
    graph.trace(port.id, i18n).forEach { step ->
        Text("${step.stepIndex}. ${step.description}")
        step.currentDevice?.takeIf { it.id != rootId }?.let { d -> TextButton(onClick = { openDevice(d) }) { Text(d.technicalName) } }
    }
    val mapping = project.panelMappings.singleOrNull { it.portAId == port.id || it.portBId == port.id }
    if ((ref.device.hardware.passive || ref.device.category == DeviceCategory.PATCH_PANEL || ref.device.objectTypeId == "outlet") && project.panelMappings.count { it.portAId == port.id || it.portBId == port.id } <= 1) {
        val other = mapping?.let { if (it.portAId == port.id) it.portBId else it.portAId }
        val freePassages = index.ports.filter { p -> p.port.id != port.id && project.panelMappings.none { it.id != mapping?.id && (it.portAId == p.port.id || it.portBId == p.port.id) } }
        Pick(i18n.text("config.passage"), index.port(other), freePassages, i18n, { "${it.device.technicalName} › ${it.port.name}" }, sortByName = false) { p ->
            stage(HardwareConfigurator.passage(project, port.id, p?.port?.id, mapping?.id))
        }
    }
    Pick(i18n.text("config.createIntermediate"), null, ObjectCatalog.types(project).filter { it.kind == ObjectKind.DEVICE }, i18n, { ObjectCatalog.displayName(it, i18n) }) { it?.let(create) }
}

@Composable
private fun PortLogicTab(project: Project, port: Port, i18n: Messages, stage: (Project) -> Unit) {
    val membership = project.portVlanMemberships.find { it.portId == port.id }
    val poe = project.poeMappings.find { it.portId == port.id }
    var mode by remember(port.id, membership) { mutableStateOf(membership?.mode?.takeIf { it == PortVlanMode.TRUNK } ?: PortVlanMode.ACCESS) }
    var untagged by remember(port.id, membership) { mutableStateOf(membership?.untaggedVlanId?.toString().orEmpty()) }
    var tagged by remember(port.id, membership) { mutableStateOf(membership?.taggedVlanIds?.joinToString(", ").orEmpty()) }
    val untaggedVlan = untagged.trim().toIntOrNull()?.takeIf { it in 1..4094 }
    val taggedVlans = tagged.split(',', ' ', ';').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..4094 }
    Text("VLAN", style = MaterialTheme.typography.titleSmall)
    ValueMenu(i18n.text("port.vlanMode"), mode, listOf(PortVlanMode.ACCESS, PortVlanMode.TRUNK), { it.toDisplayString(i18n) }) { mode = it }
    Field(i18n.text(if (mode == PortVlanMode.TRUNK) "port.vlanNative" else "port.vlanUntagged"), untagged) { untagged = it.filter(Char::isDigit).take(4) }
    if (mode == PortVlanMode.TRUNK) Field(i18n.text("port.vlanTagged"), tagged) { tagged = it }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = untaggedVlan != null || (mode == PortVlanMode.TRUNK && taggedVlans.isNotEmpty()),
            onClick = { stage(PortLogic.setVlan(project, listOf(port.id), mode, untaggedVlan, taggedVlans)) }) { Text(i18n.text("port.applyVlan")) }
        if (membership != null) TextButton(onClick = { stage(PortLogic.clearVlan(project, listOf(port.id))) }) { Text(i18n.text("port.removeVlan")) }
    }
    val subnets = PortLogic.subnets(project, membership?.untaggedVlanId)
    Text(if (subnets.isEmpty()) i18n.text("port.noSubnet") else i18n.text("port.subnets", subnets.joinToString { listOfNotNull(it.cidrBlock, it.gatewayIp?.let { g -> "GW $g" }).joinToString(" ") }),
        style = MaterialTheme.typography.bodySmall)
    Text("PoE", style = MaterialTheme.typography.titleSmall)
    ValueMenu(i18n.text("port.poeStandard"), poe?.standard, listOf(null, PoeStandard.IEEE_802_3AF, PoeStandard.IEEE_802_3AT, PoeStandard.IEEE_802_3BT),
        { it?.toDisplayString(i18n) ?: i18n.text("preset.value.NONE") }) { stage(PortLogic.setPoe(project, listOf(port.id), it, poe?.role ?: PoeRole.PSE_SOURCE)) }
    if (poe != null) ValueMenu(i18n.text("port.poeRole"), poe.role, listOf(PoeRole.PSE_SOURCE, PoeRole.PD_SINK), { it.toDisplayString(i18n) }) {
        stage(PortLogic.setPoe(project, listOf(port.id), poe.standard, it))
    }
}

@Composable
private fun PortHardwareTab(project: Project, ref: ProjectIndex.PortRef, i18n: Messages, index: ProjectIndex, stage: (Project) -> Unit) {
    val port = ref.port
    fun updatePort(transform: (Port) -> Port) = stage(ProjectEdits.updateDevice(project, ref.device.copy(ports = ref.device.ports.map { if (it.id == port.id) transform(it) else it })))
    fun updateHardware(hardware: PortHardware) = updatePort { it.copy(hardware = hardware.copy(customized = true)) }
    Field(i18n.text("config.label"), port.label.orEmpty()) { value -> updatePort { it.copy(label = value.ifBlank { null }) } }
    Choice(i18n.text("config.connector"), port.hardware.connector.orEmpty(), PortKind.entries.map { it.connector }.distinct() + index.ports.mapNotNull { it.port.hardware.connector }) { updateHardware(port.hardware.copy(connector = it)) }
    Choice(i18n.text("config.speed"), port.hardware.speed.orEmpty(), listOf("100M", "1G", "2.5G", "10G", "25G", "100G") + index.ports.mapNotNull { it.port.hardware.speed }) { updateHardware(port.hardware.copy(speed = it)) }
    Choice(i18n.text("config.module"), port.hardware.opticalModule.orEmpty(), index.ports.mapNotNull { it.port.hardware.opticalModule }) { value -> updatePort { it.copy(hardware = it.hardware.copy(opticalModule = value.ifBlank { null })) } }
    ValueMenu(i18n.text("port.poeCapable"), port.hardware.poeStandard, listOf(null) + PoeStandard.entries, { it?.toDisplayString(i18n) ?: i18n.text("preset.value.NONE") }) { updateHardware(port.hardware.copy(poeStandard = it)) }
}
