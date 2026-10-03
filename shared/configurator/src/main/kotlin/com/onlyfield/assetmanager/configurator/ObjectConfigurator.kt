package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.geometry.Offset
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

@Composable
private fun Choice(label: String, value: String, values: List<String>, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { TextButton(onClick = { expanded = true }) { Text("▾") } })
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 300.dp)) {
            values.distinctBy { it.trim().lowercase() }.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { change(item); expanded = false })
            }
        }
    }
}

@Composable
private fun <T> Pick(label: String, selected: T?, options: List<T>, i18n: Messages, display: (T) -> String, change: (T?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${selected?.let(display) ?: i18n.text("config.undefined")}") }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            DropdownMenuItem(text = { Text(i18n.text("config.undefined")) }, onClick = { change(null); expanded = false })
            options.forEach { item -> DropdownMenuItem(text = { Text(display(item)) }, onClick = { change(item); expanded = false }) }
        }
    }
}

@Composable
private fun Field(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

private fun snapshot(project: Project, draft: MapObjectDraft): Project {
    val configured = draft.session?.apply(project) ?: project
    val p = if (draft.type.id != "legacy" && ObjectCatalog.builtins.none { it.id == draft.type.id })
        configured.copy(objectTypes = configured.objectTypes.filterNot { it.id == draft.type.id } + draft.type) else configured
    return when (draft.type.kind) {
        ObjectKind.DEVICE -> {
            val existing = p.businessUnits.flatMap { it.devices }.find { it.id == draft.id }
            val device = draft.device.toDevice(existing, "Configurator").copy(id = draft.id)
            val added = if (existing == null) ProjectEdits.addDevice(p, draft.device.businessUnitId ?: draft.buId, device) else ProjectEdits.updateDevice(p, device)
            if ((draft.portsConfigured || draft.device.hardware.portGroups.isNotEmpty()) && HardwareConfigurator.validGroups(device.hardware.portGroups) && (draft.allowConnectedRemoval || HardwareConfigurator.preview(p, device, device.hardware.portGroups).connectedRemoved.isEmpty()))
                HardwareConfigurator.configure(added, device, draft.allowConnectedRemoval, true) else added
        }
        ObjectKind.RACK -> {
            val rack = draft.rack.toRack(p.racks.find { it.id == draft.id }).copy(id = draft.id)
            if (p.racks.any { it.id == rack.id }) ProjectEdits.updateRack(p, rack) else ProjectEdits.addRack(p, rack)
        }
        ObjectKind.CABLE -> {
            val c = draft.cable.toCable(p.cables.find { it.id == draft.id }).copy(id = draft.id, deviceAId = draft.deviceAId, deviceBId = draft.deviceBId)
            if (p.cables.any { it.id == c.id }) ProjectEdits.updateCable(p, c) else ProjectEdits.addCable(p, c)
        }
    }
}

/** One staged session is committed by the host's existing Save/Undo operation. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ObjectConfigurator(project: Project, draft: MapObjectDraft, i18n: Messages, modelEditor: Boolean = false, change: (MapObjectDraft) -> Unit) {
    val preview = remember(project, draft) { snapshot(project, draft) }
    val index = remember(preview) { ProjectIndex(preview) }
    val graph = remember(preview) { ConnectionGraph(preview) }
    var portId by remember(draft.id) { mutableStateOf<String?>(null) }
    var nested by remember(draft.id) { mutableStateOf<MapObjectDraft?>(null) }
    var modelQuery by remember(draft.id) { mutableStateOf("") }
    var saveModel by remember(draft.id) { mutableStateOf(false) }
    var modelName by remember(draft.id) { mutableStateOf("") }
    var side by remember(draft.id) { mutableStateOf(PortSide.FRONT) }
    var zoom by remember(draft.id) { mutableFloatStateOf(1f) }
    fun stage(p: Project) = change(draft.copy(session = ConfigurationSession(draft.session?.original ?: project, p)))
    fun openDevice(device: Device) {
        stage(preview)
        val bu = index.businessUnitOf(device.id)?.id ?: draft.buId
        nested = MapObjectDraft.device(preview, bu, ObjectMap.areaId(preview, device).orEmpty(), device.id, i18n)
    }
    nested?.let { child ->
        TextButton(onClick = { nested = null }) { Text(i18n.text("config.cancelBack")) }
        ObjectConfigurator(preview, child, i18n) { nested = it }
        Button(onClick = { stage(child.apply(preview, i18n)); nested = null }, enabled = child.errors(preview, i18n).isEmpty()) { Text(i18n.text("config.applyBack")) }
        return
    }
    portId?.let { id -> index.port(id)?.let { ref ->
        val bringIntoView = remember(id) { BringIntoViewRequester() }
        TextButton(onClick = { portId = null }, modifier = Modifier.bringIntoViewRequester(bringIntoView)) { Text(i18n.text("config.backToObject")) }
        LaunchedEffect(id) { bringIntoView.bringIntoView() }
        PortConfiguration(preview, ref, draft.id, i18n, { stage(it) }, { openDevice(it) }, { type ->
            stage(preview)
            nested = MapObjectDraft(type = type, buId = draft.buId, areaId = draft.areaId)
        })
        return
    } }

    Text(i18n.text("config.title"), style = MaterialTheme.typography.titleLarge)
    if (!modelEditor && draft.type.kind != ObjectKind.CABLE) {
        val self = ObjectRef(if (draft.type.kind == ObjectKind.RACK) PlacementTargetType.RACK else PlacementTargetType.DEVICE, draft.id)
        val parents = ObjectHierarchy.refs(preview).filter { it != self && ObjectHierarchy.canContain(preview, it) && self !in ObjectHierarchy.ancestors(preview, it) }
        Pick(i18n.text("config.container"), draft.parentRef, parents, i18n, { ObjectHierarchy.name(preview, it, i18n) }) { parent ->
            val rack = parent?.let { (listOf(it) + ObjectHierarchy.ancestors(preview, it)).firstOrNull { it.type == PlacementTargetType.RACK } }
            change(draft.copy(parentRef = parent, device = draft.device.copy(rackId = rack?.id)))
        }
    }
    Pick(i18n.text("config.objectType"), draft.type, ObjectCatalog.types(preview).filter { it.kind == draft.type.kind }, i18n, { ObjectCatalog.displayName(it, i18n) }) { type ->
        if (type != null) change(draft.copy(type = type, device = draft.device.copy(objectTypeId = type.id, category = type.category)))
    }
    if (draft.type.kind == ObjectKind.DEVICE) Toggle(i18n.text("config.canContain"), draft.type.canContainObjects) { enabled ->
        if (enabled || ObjectHierarchy.children(preview, ObjectRef(PlacementTargetType.DEVICE, draft.id)).isEmpty()) {
            val type = if (ObjectCatalog.builtins.any { it.id == draft.type.id } || draft.type.id == "legacy") draft.type.copy(id = java.util.UUID.randomUUID().toString(), canContainObjects = enabled) else draft.type.copy(canContainObjects = enabled)
            change(draft.copy(type = type, device = draft.device.copy(objectTypeId = type.id)))
        }
    }
    val models = preview.deviceModels.filter { it.kind == draft.type.kind && (it.objectTypeId == null || it.objectTypeId == draft.type.id || (draft.type.kind == ObjectKind.DEVICE && it.category == draft.device.category)) }
    if (!modelEditor) {
        Field(i18n.text("config.findModel"), modelQuery) { modelQuery = it }
        val selectedModelId = when (draft.type.kind) { ObjectKind.DEVICE -> draft.device.deviceModelId; ObjectKind.RACK -> draft.rack.deviceModelId; ObjectKind.CABLE -> draft.cable.deviceModelId }
        Pick(i18n.text("config.model"), models.find { it.id == selectedModelId }, models.filter { it.name.contains(modelQuery, true) }, i18n, { it.name }) { model ->
            if (model != null) {
                val oldExtras = draft.extraFields ?: preview.customExtraFields.filter { it.targetId == draft.id }
                change(HardwareConfigurator.applyModel(draft.copy(extraFields = oldExtras), model).copy(allowConnectedRemoval = false))
            }
        }
    }
    val bu = preview.businessUnits.find { it.id == draft.device.businessUnitId } ?: preview.businessUnits.find { it.id == draft.buId }
    when (draft.type.kind) {
        ObjectKind.DEVICE -> {
            val d = draft.device
            val h = d.hardware
            Field(i18n.text("config.name"), d.technicalName) { change(draft.copy(device = d.copy(technicalName = it))) }
            Pick(i18n.text("config.category"), d.category, DeviceCategory.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { c -> change(draft.copy(device = d.copy(category = c))) } }
            if (!modelEditor) {
                Pick(i18n.text("config.bu"), bu, preview.businessUnits, i18n, { it.name }) { it?.let { b -> change(draft.copy(buId = b.id, device = d.copy(businessUnitId = b.id))) } }
                Pick(i18n.text("config.floor"), index.area(d.areaId), bu?.let { ObjectMap.areas(it) }.orEmpty(), i18n, { it.name }) { change(draft.copy(device = d.copy(areaId = it?.id))) }
                Pick(i18n.text("config.rack"), index.rack(d.rackId), preview.racks, i18n, { it.name }) { r -> change(draft.copy(parentRef = r?.let { ObjectRef(PlacementTargetType.RACK, it.id) }, device = d.copy(rackId = r?.id, mountingType = if (r == null) MountingType.OUT_OF_RACK else MountingType.RACK_MOUNT))) }
            }
            Choice(i18n.text("config.units"), d.heightU, (listOf(1, 2, 3, 4) + index.devices.map { it.heightU }).map { it.toString() }) { change(draft.copy(device = d.copy(heightU = it))) }
            if (!modelEditor && d.rackId != null) {
                val rack = index.rack(d.rackId)
                val free = rack?.let { RackLayout.freeStartPositions(it, index.devices, d.heightU.toIntOrNull() ?: 1, d.rackSide, draft.id) }.orEmpty()
                Choice(i18n.text("config.position"), d.positionU, free.map { it.toString() }) { change(draft.copy(device = d.copy(positionU = it))) }
                Pick(i18n.text("config.side"), d.rackSide, RackSide.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { s -> change(draft.copy(device = d.copy(rackSide = s))) } }
            }
            Pick(i18n.text("config.mount"), d.mountingType, MountingType.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { m -> change(draft.copy(device = d.copy(mountingType = m))) } }
            NumericHardware(i18n.text("config.width"), h.widthMm) { change(draft.copy(device = d.copy(hardware = h.copy(widthMm = it)))) }
            NumericHardware(i18n.text("config.depth"), h.depthMm) { change(draft.copy(device = d.copy(hardware = h.copy(depthMm = it)))) }
            Field(i18n.text("config.poeBudget"), h.poeBudgetWatts?.toString().orEmpty()) { value -> if (value.isBlank() || value.toDoubleOrNull()?.let { it >= 0 } == true) change(draft.copy(device = d.copy(hardware = h.copy(poeBudgetWatts = value.toDoubleOrNull())))) }
            Toggle(i18n.text("config.redundant"), h.redundantPower) { change(draft.copy(device = d.copy(hardware = h.copy(redundantPower = it)))) }
            Toggle(i18n.text("config.passive"), h.passive || d.category == DeviceCategory.PATCH_PANEL || draft.type.id == "outlet") { change(draft.copy(device = d.copy(hardware = h.copy(passive = it)))) }
            Choice(i18n.text("config.features"), h.features.joinToString(", "), (listOf("PoE", "LACP", "Stack", "Fanless", "Redundant PSU") + index.devices.flatMap { it.hardware.features })) { value -> change(draft.copy(device = d.copy(hardware = h.copy(features = value.split(',').map { it.trim() }.filter { it.isNotBlank() })))) }
            PortGroups(preview, draft, i18n, change)
            val device = index.device(draft.id)
            if (device != null) {
                if ((draft.portsConfigured || h.portGroups.isNotEmpty()) && HardwareConfigurator.validGroups(h.portGroups)) {
                    val existing = (draft.session?.apply(project) ?: project).businessUnits.flatMap { it.devices }.find { it.id == draft.id } ?: device.copy(ports = emptyList())
                    val differences = HardwareConfigurator.preview(preview, existing, h.portGroups)
                    Text(i18n.text("config.differences", differences.added, differences.removed.size, differences.connectedRemoved.size))
                    if (differences.connectedRemoved.isNotEmpty()) Toggle(i18n.text("config.removeConnected"), draft.allowConnectedRemoval) { change(draft.copy(allowConnectedRemoval = it)) }
                }
                Row {
                    listOf(PortSide.FRONT, PortSide.REAR).forEach { s -> TextButton(onClick = { side = s }) { Text(s.toDisplayString(i18n)) } }
                    TextButton(onClick = { zoom = (zoom - .25f).coerceAtLeast(1f) }) { Text("−") }
                    TextButton(onClick = { zoom = (zoom + .25f).coerceAtMost(3f) }) { Text("+") }
                }
                DeviceDrawing(device, graph, side, zoom, portId, i18n) { p -> if (!modelEditor) { stage(preview); portId = p.id } }
            }
            if (!modelEditor) {
                Field(i18n.text("config.label"), d.physicalLabel) { change(draft.copy(device = d.copy(physicalLabel = it))) }
                Field(i18n.text("config.alias"), d.alias) { change(draft.copy(device = d.copy(alias = it))) }
                Field("IP", d.ipAddress) { change(draft.copy(device = d.copy(ipAddress = it))) }
                Field("MAC", d.macAddress) { change(draft.copy(device = d.copy(macAddress = it))) }
                Field(i18n.text("config.serial"), d.serialNumber) { change(draft.copy(device = d.copy(serialNumber = it))) }
                Pick(i18n.text("config.observation"), d.observationStatus, ObservationStatus.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { s -> change(draft.copy(device = d.copy(observationStatus = s))) } }
                Field(i18n.text("config.notes"), d.notes) { change(draft.copy(device = d.copy(notes = it))) }
            }
        }
        ObjectKind.RACK -> {
            val r = draft.rack
            Field(i18n.text("config.name"), r.name) { change(draft.copy(rack = r.copy(name = it))) }
            Choice(i18n.text("config.units"), r.heightU, (HardwareConfigurator.rackHeights + preview.racks.map { it.heightU }).map { it.toString() }) { change(draft.copy(rack = r.copy(heightU = it))) }
            Choice(i18n.text("config.depth"), r.depthMm, (HardwareConfigurator.rackDepths + preview.racks.mapNotNull { it.depthMm }).map { it.toString() }) { change(draft.copy(rack = r.copy(depthMm = it))) }
            Choice(i18n.text("config.mountDepth"), r.mountingDepthMm, preview.racks.mapNotNull { it.mountingDepthMm }.map { it.toString() }) { change(draft.copy(rack = r.copy(mountingDepthMm = it))) }
            Choice(i18n.text("config.mount"), r.mountingType.orEmpty(), listOf("19 inch", "Wall", "Floor", "Open frame") + preview.racks.mapNotNull { it.mountingType }) { change(draft.copy(rack = r.copy(mountingType = it))) }
            Pick(i18n.text("config.numbering"), r.numberingDirection, NumberingDirection.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { n -> change(draft.copy(rack = r.copy(numberingDirection = n))) } }
            if (!modelEditor) Pick(i18n.text("config.floor"), index.area(r.areaId), index.areas, i18n, { it.name }) { change(draft.copy(rack = r.copy(areaId = it?.id))) }
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
            if (!modelEditor) Field(i18n.text("config.notes"), r.notes) { change(draft.copy(rack = r.copy(notes = it))) }
        }
        ObjectKind.CABLE -> {
            val c = draft.cable
            Field(i18n.text("config.name"), c.codeOrLabel) { change(draft.copy(cable = c.copy(codeOrLabel = it))) }
            if (!modelEditor) CableEndpoints(preview, draft, i18n, change)
            Pick(i18n.text("config.medium"), c.medium, CableMedium.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { m -> change(draft.copy(cable = c.copy(medium = m))) } }
            Choice(i18n.text("config.connectorA"), c.connectorA, listOf("RJ45", "LC", "SC", "MPO", "SFP", "SFP+", "QSFP", "USB-C", "C13", "C14") + preview.cables.mapNotNull { it.connectorA }) { change(draft.copy(cable = c.copy(connectorA = it))) }
            Choice(i18n.text("config.connectorB"), c.connectorB, listOf("RJ45", "LC", "SC", "MPO", "SFP", "SFP+", "QSFP", "USB-C", "C13", "C14") + preview.cables.mapNotNull { it.connectorB }) { change(draft.copy(cable = c.copy(connectorB = it))) }
            Choice(i18n.text("config.characteristics"), c.nominalCharacteristics, listOf("Cat5e", "Cat6", "Cat6A", "OS2", "OM3", "OM4", "OM5") + preview.cables.mapNotNull { it.nominalCharacteristics }) { change(draft.copy(cable = c.copy(nominalCharacteristics = it))) }
            if (!modelEditor) Field(i18n.text("config.speed"), c.observedSpeed) { change(draft.copy(cable = c.copy(observedSpeed = it))) }
            Choice(i18n.text("config.color"), c.color, preview.cables.mapNotNull { it.color }) { change(draft.copy(cable = c.copy(color = it))) }
            if (!modelEditor) Field(i18n.text("config.length"), c.lengthValue) { change(draft.copy(cable = c.copy(lengthValue = it))) }
            Pick(i18n.text("config.orientation"), c.orientation, CableOrientation.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { o -> change(draft.copy(cable = c.copy(orientation = o))) } }
            if (!modelEditor) preview.sharedPathSegments.forEach { segment -> Toggle(segment.name, segment.id in c.sharedPathSegmentIds) { checked -> change(draft.copy(cable = c.copy(sharedPathSegmentIds = if (checked) c.sharedPathSegmentIds + segment.id else c.sharedPathSegmentIds - segment.id))) } }
            Canvas(Modifier.fillMaxWidth().height(45.dp)) { drawLine(Color(0xff607d8b), Offset(20f, size.height / 2), Offset(size.width - 20f, size.height / 2), 4f) }
            listOfNotNull(c.portAId, c.portBId).forEach { id -> TextButton(onClick = { stage(preview); portId = id }) { Text(index.portLabel(id)) } }
            if (!modelEditor) Field(i18n.text("config.notes"), c.notes) { change(draft.copy(cable = c.copy(notes = it))) }
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
    extras.forEach { field ->
        Choice(i18n.text("config.field"), field.fieldKey, reusableExtras.map { it.fieldKey }) { v -> change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(fieldKey = v) else it })) }
        Choice(i18n.text("config.value"), field.fieldValue, reusableExtras.filter { it.fieldKey == field.fieldKey && it.fieldType == field.fieldType && it.classification == field.classification }.map { it.fieldValue }) { v -> change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(fieldValue = v) else it })) }
        Pick(i18n.text("config.fieldType"), field.fieldType, CustomFieldType.entries, i18n, { it.name }) { t -> t?.let { change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(fieldType = t) else it })) } }
        Pick(i18n.text("config.classification"), field.classification, if (modelEditor) listOf(AttachmentClassification.SHAREABLE) else AttachmentClassification.entries, i18n, { it.toDisplayString(i18n) }) { t -> t?.let { change(draft.copy(extraFields = extras.map { if (it.id == field.id) it.copy(classification = t) else it })) } }
        TextButton(onClick = { change(draft.copy(extraFields = extras.filterNot { it.id == field.id })) }) { Text(i18n.text("config.remove")) }
    }
    OutlinedButton(onClick = { change(draft.copy(extraFields = extras + CustomExtraField(targetType = draft.targetType.name, targetId = draft.id, fieldKey = "", fieldValue = ""))) }) { Text(i18n.text("config.addField")) }
    if (!modelEditor) OutlinedButton(onClick = { saveModel = !saveModel }) { Text(i18n.text("config.saveModel")) }
    if (saveModel) {
        Field(i18n.text("config.model"), modelName) { modelName = it }
        val duplicate = preview.deviceModels.any { it.kind == draft.type.kind && it.name.equals(modelName.trim(), true) }
        if (duplicate) Text(i18n.text("config.modelExists"), color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            val model = HardwareConfigurator.model(preview, draft, modelName)
            change(draft.copy(session = ConfigurationSession(draft.session?.original ?: project, ProjectEdits.addDeviceModel(preview, model)), device = draft.device.copy(deviceModelId = model.id), rack = draft.rack.copy(deviceModelId = model.id), cable = draft.cable.copy(deviceModelId = model.id))); saveModel = false; modelQuery = modelName
        }, enabled = modelName.isNotBlank() && !duplicate) { Text(i18n.text("config.saveModel")) }
    }
    draft.errors(project, i18n).values.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun NumericHardware(label: String, value: Int?, change: (Int?) -> Unit) {
    Field(label, value?.toString().orEmpty()) { s -> if (s.isBlank() || s.toIntOrNull()?.let { it > 0 } == true) change(s.toIntOrNull()) }
}

@Composable
private fun Toggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row { Checkbox(value, change); Text(label, modifier = Modifier.padding(top = 12.dp)) }
}

@Composable
fun DeviceDrawing(device: Device, graph: ConnectionGraph, side: PortSide, zoom: Float, selected: String?, i18n: Messages, click: (Port) -> Unit) {
    Column(Modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Text("${device.technicalName} · ${device.heightU}U", style = MaterialTheme.typography.titleMedium)
        SchematicGeometry.rows(device, side).forEach { row ->
            row.group?.let { Text(it) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.ports.forEach { p ->
                    val state = graph.state(p.id)
                    val label = i18n.text("config.${state.name.lowercase()}")
                    val color = when (state) { ConnectionState.COMPLETE -> Color(0xff2e7d32); ConnectionState.INCOMPLETE -> Color(0xff966000); ConnectionState.CONFLICT -> Color(0xffb71c1c); ConnectionState.AVAILABLE -> Color(0xff455a64) }
                    Surface(onClick = { click(p) }, color = color, contentColor = Color.White, shape = RoundedCornerShape(3.dp), modifier = Modifier.width(80.dp * zoom).heightIn(min = 60.dp * zoom)
                        .border(if (selected == p.id) 3.dp else 1.dp, if (selected == p.id) MaterialTheme.colorScheme.primary else color, RoundedCornerShape(3.dp))
                        .semantics { contentDescription = "${device.technicalName} ${p.name}"; stateDescription = label }) {
                        Column(Modifier.padding(6.dp)) { Text(p.name); Text(label, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        if (device.ports.isEmpty()) Text(i18n.text("config.noPorts"))
    }
}

@Composable
private fun PortGroups(project: Project, draft: MapObjectDraft, i18n: Messages, change: (MapObjectDraft) -> Unit) {
    val groups = draft.device.hardware.portGroups
    fun update(next: List<PortTemplate>) = change(draft.copy(device = draft.device.copy(hardware = draft.device.hardware.copy(portGroups = next)), allowConnectedRemoval = false, portsConfigured = true))
    groups.forEachIndexed { n, g ->
        fun set(next: PortTemplate) = update(groups.mapIndexed { index, value -> if (index == n) next else value })
        Text(i18n.text("config.portGroup", n + 1), style = MaterialTheme.typography.titleMedium)
        Field(i18n.text("config.prefix"), g.namePrefix) { if (it.isNotBlank()) set(g.copy(namePrefix = it)) }
        Choice(i18n.text("config.count"), g.portCount.toString(), (HardwareConfigurator.portCounts + project.deviceModels.flatMap { it.portTemplates }.map { it.portCount }).map { it.toString() }) { v -> v.toIntOrNull()?.takeIf { it in 1..512 }?.let { set(g.copy(portCount = it)) } }
        Field(i18n.text("config.start"), g.startNumber.toString()) { v -> v.toIntOrNull()?.takeIf { it in 0..9999 }?.let { set(g.copy(startNumber = it)) } }
        Pick(i18n.text("config.side"), g.side, PortSide.entries, i18n, { it.toDisplayString(i18n) }) { it?.let { s -> set(g.copy(side = s)) } }
        Choice(i18n.text("config.medium"), g.mediaType.orEmpty(), listOf("Copper", "Fiber", "DAC", "AOC", "Power", "Console") + project.deviceModels.flatMap { it.portTemplates }.mapNotNull { it.mediaType }) { set(g.copy(mediaType = it)) }
        Choice(i18n.text("config.connector"), g.connector.orEmpty(), listOf("RJ45", "SFP", "SFP+", "SFP28", "QSFP+", "QSFP28", "LC", "SC", "MPO", "USB-C", "C13", "C14") + project.deviceModels.flatMap { it.portTemplates }.mapNotNull { it.connector }) { set(g.copy(connector = it)) }
        Choice(i18n.text("config.speed"), g.speed.orEmpty(), listOf("100M", "1G", "2.5G", "5G", "10G", "25G", "40G", "100G") + project.deviceModels.flatMap { it.portTemplates }.mapNotNull { it.speed }) { set(g.copy(speed = it)) }
        Choice(i18n.text("config.role"), g.role, listOf("DATA", "UPLINK", "MANAGEMENT", "CONSOLE", "POWER")) { set(g.copy(role = it)) }
        Pick("PoE", g.poeStandard, PoeStandard.entries, i18n, { it.name }) { set(g.copy(poeStandard = it)) }
        Toggle(i18n.text("config.paired"), g.pairedSides) { set(g.copy(pairedSides = it)) }
        Field(i18n.text("config.combo"), g.comboGroup.orEmpty()) { set(g.copy(comboGroup = it.trim().ifBlank { null })) }
        TextButton(onClick = { update(groups.filterIndexed { index, _ -> index != n }) }) { Text(i18n.text("config.remove")) }
    }
    Row {
        OutlinedButton(onClick = { update(groups + PortTemplate(namePrefix = "P${groups.size + 1}-", portCount = 24, connector = "RJ45", mediaType = "Copper", pairedSides = draft.device.category == DeviceCategory.PATCH_PANEL || draft.type.id == "outlet")) }) { Text(i18n.text("config.addGroup")) }
        OutlinedButton(onClick = { update(groups + PortTemplate(namePrefix = "SFP${groups.size + 1}-", portCount = 4, connector = "SFP", mediaType = "Fiber", role = "UPLINK")) }) { Text(i18n.text("config.addFiber")) }
    }
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
            Pick(i18n.text("config.port"), device.ports.find { it.id == id }, ports, i18n, { "${it.name} · ${it.hardware.side?.toDisplayString(i18n).orEmpty()} · ${i18n.text("config.available")}" }) { p ->
                change(if (first) draft.copy(cable = draft.cable.copy(portAId = p?.id)) else draft.copy(cable = draft.cable.copy(portBId = p?.id)))
            }
            Text(i18n.text("config.portCount", device.ports.size, ports.size))
        }
    }
}

@Composable
private fun PortConfiguration(project: Project, ref: ProjectIndex.PortRef, rootId: String, i18n: Messages, stage: (Project) -> Unit, openDevice: (Device) -> Unit, create: (ObjectType) -> Unit) {
    val graph = remember(project) { ConnectionGraph(project) }
    val index = remember(project) { ProjectIndex(project) }
    val port = ref.port
    fun updateHardware(hardware: PortHardware) = stage(ProjectEdits.updateDevice(project, ref.device.copy(ports = ref.device.ports.map {
        if (it.id == port.id) it.copy(hardware = hardware.copy(customized = true)) else it
    })))
    var query by remember(port.id) { mutableStateOf("") }
    var buId by remember(port.id) { mutableStateOf<String?>(null) }
    var areaId by remember(port.id) { mutableStateOf<String?>(null) }
    var rackId by remember(port.id) { mutableStateOf<String?>(null) }
    var deviceId by remember(port.id) { mutableStateOf<String?>(null) }
    val existing = project.cables.singleOrNull { it.portAId == port.id || it.portBId == port.id }
    val currentDestination = existing?.let { if (it.portAId == port.id) it.portBId else it.portAId }
    var destination by remember(port.id, existing) { mutableStateOf(currentDestination) }
    var medium by remember(port.id, existing) { mutableStateOf(existing?.medium ?: if (port.hardware.mediaType == "Fiber") CableMedium.FIBER_OVERALL else CableMedium.ETHERNET_COPPER) }
    Text("${ref.device.technicalName} › ${port.name}", style = MaterialTheme.typography.titleLarge)
    Text(i18n.text("config.${graph.state(port.id).name.lowercase()}"))
    Field(i18n.text("config.label"), port.label.orEmpty()) { value -> stage(ProjectEdits.updateDevice(project, ref.device.copy(ports = ref.device.ports.map { if (it.id == port.id) it.copy(label = value.ifBlank { null }) else it }))) }
    Choice(i18n.text("config.module"), port.hardware.opticalModule.orEmpty(), index.ports.mapNotNull { it.port.hardware.opticalModule }) { value -> stage(ProjectEdits.updateDevice(project, ref.device.copy(ports = ref.device.ports.map { if (it.id == port.id) it.copy(hardware = it.hardware.copy(opticalModule = value.ifBlank { null })) else it }))) }
    Choice(i18n.text("config.connector"), port.hardware.connector.orEmpty(), listOf("RJ45", "SFP", "SFP+", "LC", "SC", "MPO", "USB-C", "C13", "C14") + index.ports.mapNotNull { it.port.hardware.connector }) { updateHardware(port.hardware.copy(connector = it)) }
    Choice(i18n.text("config.speed"), port.hardware.speed.orEmpty(), listOf("100M", "1G", "2.5G", "10G", "25G", "100G") + index.ports.mapNotNull { it.port.hardware.speed }) { updateHardware(port.hardware.copy(speed = it)) }
    Pick("PoE", port.hardware.poeStandard, PoeStandard.entries, i18n, { it.name }) { updateHardware(port.hardware.copy(poeStandard = it)) }
    Field(i18n.text("config.search"), query) { query = it }
    Pick(i18n.text("config.bu"), project.businessUnits.find { it.id == buId }, project.businessUnits, i18n, { it.name }) { buId = it?.id }
    Pick(i18n.text("config.floor"), index.area(areaId), index.areas, i18n, { it.name }) { areaId = it?.id }
    Pick(i18n.text("config.rack"), index.rack(rackId), project.racks, i18n, { it.name }) { rackId = it?.id }
    Pick(i18n.text("config.device"), index.device(deviceId), index.devices, i18n, { it.technicalName }) { deviceId = it?.id }
    val candidates = index.ports.filter { p ->
        p.port.id != port.id && (p.port.id == currentDestination || !graph.occupied(p.port.id, existing?.id)) &&
            (buId == null || index.businessUnitOf(p.device.id)?.id == buId) && (areaId == null || ObjectMap.areaId(project, p.device) == areaId) &&
            (rackId == null || p.device.rackId == rackId) && (deviceId == null || p.device.id == deviceId) && index.portLabel(p.port.id).contains(query, true)
    }.sortedBy { if (it.port.hardware.connector == port.hardware.connector) 0 else 1 }
    Pick(i18n.text("config.destination"), index.port(destination), candidates, i18n, { "${it.device.technicalName} › ${it.port.name} · ${it.port.hardware.side?.toDisplayString(i18n).orEmpty()}" }) {
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
        Pick(i18n.text("config.passage"), index.port(other), freePassages, i18n, { "${it.device.technicalName} › ${it.port.name}" }) { p ->
            stage(HardwareConfigurator.passage(project, port.id, p?.port?.id, mapping?.id))
        }
    }
    Pick(i18n.text("config.createIntermediate"), null, ObjectCatalog.types(project).filter { it.kind == ObjectKind.DEVICE }, i18n, { ObjectCatalog.displayName(it, i18n) }) { it?.let(create) }
}
