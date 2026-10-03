package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.RackForm
import com.onlyfield.assetmanager.core.forms.RackLayout

@Composable
fun RackSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit,
    onTrashItemCreated: (TrashItem) -> Unit
) {
    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current
    var query by remember { mutableStateOf("") }
    var selectedRackId by remember { mutableStateOf(project.racks.firstOrNull()?.id) }
    var side by remember { mutableStateOf(RackSide.FRONT) }
    var editing by remember { mutableStateOf<Rack?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }
    var placing by remember { mutableStateOf(false) }

    val racks = project.racks.filter { matchesQuery(query, it.name, index.areaName(it.areaId, "")) }
    val selected = index.rack(selectedRackId) ?: racks.firstOrNull()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            "Rack",
            subtitle = "${project.racks.size} rack",
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = "Cerca rack o area…"
        ) {
            Button(onClick = { changeDetail { creating = true } }) { Text("+ Nuovo rack") }
        }

        if (project.racks.isEmpty()) {
            EmptyState("Nessun rack nel progetto.", actionLabel = "+ Nuovo rack", onAction = { creating = true })
            return@Column
        }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(modifier = Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(racks, key = { it.id }) { rack ->
                    val isSelected = rack.id == selected?.id
                    val used = RackLayout.usedUnits(rack, index.devices)
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .border(if (isSelected) 2.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { if (selectedRackId != rack.id) changeDetail { selectedRackId = rack.id } },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(rack.name, fontWeight = FontWeight.SemiBold)
                            Text("${index.areaName(rack.areaId, "Nessuna area")} · $used/${rack.heightU}U occupate", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            selected?.let { rack ->
                val inRack = index.devices.filter { it.rackId == rack.id }
                val unplaced = inRack.filter { it.positionU == null }
                Card(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rack.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "${rack.heightU}U · numerazione ${rack.numberingDirection.toDisplayString().lowercase()}" +
                                        (rack.depthMm?.let { " · profondità $it mm" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            SingleChoiceSegmentedButtonRow {
                                listOf(RackSide.FRONT, RackSide.REAR).forEachIndexed { i, s ->
                                    SegmentedButton(selected = side == s, onClick = { side = s }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                                        Text(s.toDisplayString())
                                    }
                                }
                            }
                            Button(onClick = { changeDetail { placing = true } }) { Text("Colloca apparato…") }
                            EditButton { editing = rack }
                            DeleteButton(rack.name, label = "Sposta nel cestino", message = "Gli apparati montati verranno segnati come fuori rack.", onDelete = {
                                val (updated, trashItem) = ProjectEdits.deleteRackToTrash(project, rack.id)
                                trashItem?.let(onTrashItemCreated)
                                onProjectUpdated(updated, "Rack «${rack.name}» spostato nel cestino.")
                            })
                        }
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RackElevation(rack, inRack, side, Modifier.weight(1.3f).fillMaxHeight())
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Apparati nel rack (${inRack.size})", fontWeight = FontWeight.SemiBold)
                                if (unplaced.isNotEmpty()) {
                                    Text(
                                        "${unplaced.size} senza posizione U: modificali per collocarli.",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(inRack.sortedByDescending { it.positionU ?: 0 }, key = { it.id }) { dev ->
                                        ItemCard(
                                            title = dev.technicalName,
                                            details = listOf(
                                                (dev.positionU?.let { "U$it" } ?: "Posizione non indicata") + " · ${dev.heightU}U · ${dev.rackSide.toDisplayString()}"
                                            )
                                        ) {
                                            TextButton(onClick = {
                                                confirm(
                                                    ConfirmRequest(
                                                        "Togliere «${dev.technicalName}» dal rack?",
                                                        "L'apparato resta nell'inventario come fuori rack.",
                                                        confirmLabel = "Togli dal rack",
                                                        destructive = false
                                                    ) {
                                                        val moved = dev.copy(rackId = null, positionU = null, mountingType = MountingType.OUT_OF_RACK)
                                                        onProjectUpdated(ProjectEdits.updateDevice(project, moved), "«${dev.technicalName}» tolto dal rack.")
                                                    }
                                                )
                                            }) { Text("Togli") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (placing) {
                    PlaceDeviceDialog(rack, index, side, onDismiss = { placing = false }) { device, startU, placeSide ->
                        placing = false
                        val placed = device.copy(
                            rackId = rack.id, positionU = startU, rackSide = placeSide,
                            mountingType = if (device.mountingType == MountingType.OUT_OF_RACK) MountingType.RACK_MOUNT else device.mountingType
                        )
                        onProjectUpdated(ProjectEdits.updateDevice(project, placed), "«${device.technicalName}» collocato in ${rack.name} a U$startU.")
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        RackDialog(index, editing, onDismiss = { creating = false; editing = null }) { saved, isNew ->
            creating = false; editing = null
            val updated = if (isNew) ProjectEdits.addRack(project, saved) else ProjectEdits.updateRack(project, saved)
            if (isNew) selectedRackId = saved.id
            onProjectUpdated(updated, "Rack «${saved.name}» salvato.")
        }
    }
}

@Composable
private fun RackElevation(rack: Rack, devices: List<Device>, side: RackSide, modifier: Modifier) {
    val slots = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) (rack.heightU downTo 1).toList() else (1..rack.heightU).toList()
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF202124))) {
        LazyColumn(modifier = Modifier.padding(10.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(slots) { u ->
                val dev = devices.find { d ->
                    val pos = d.positionU ?: return@find false
                    (d.rackSide == RackSide.BOTH || d.rackSide == side) && u >= pos && u < pos + d.heightU
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    color = dev?.let { categoryColor(it.category) } ?: Color(0xFF303134),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("U$u", fontSize = 10.sp, color = Color(0xFFBDC1C6), modifier = Modifier.width(36.dp))
                        if (dev != null) {
                            val top = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) dev.positionU!! + dev.heightU - 1 else dev.positionU!!
                            if (u == top) Text("${dev.technicalName} · ${dev.category.toDisplayString()}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceDeviceDialog(rack: Rack, index: ProjectIndex, initialSide: RackSide, onDismiss: () -> Unit, onPlace: (Device, Int, RackSide) -> Unit) {
    val candidates = index.devices.filter { it.rackId != rack.id || it.positionU == null }
    var device by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf<Device?>(null) }
    var side by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(initialSide) }
    var start by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf<Int?>(null) }
    val free = device?.let { RackLayout.freeStartPositions(rack, index.devices, it.heightU, side, it.id) } ?: emptyList()
    LaunchedEffect(device, side) { start = free.firstOrNull() }

    EditPanel(
        title = "Colloca apparato in «${rack.name}»",
        onDismiss = onDismiss,
        onConfirm = { onPlace(device!!, start!!, side) },
        confirmEnabled = device != null && start != null,
        confirmLabel = "Colloca",
        width = 540.dp
    ) {
        OptionPicker(
            label = "Apparato *",
            options = candidates,
            selected = device,
            optionLabel = { it.technicalName },
            optionDetail = { d -> "${d.heightU}U · " + (d.rackId?.let { "ora in ${index.rackName(it)}" } ?: "fuori rack") },
            onSelected = { device = it }
        )
        EnumPicker("Lato", RackSide.entries, side, { it.toDisplayString() }, { side = it })
        if (device != null) {
            if (free.isEmpty()) {
                Text("Non c'è spazio libero per ${device!!.heightU}U su questo lato.", color = MaterialTheme.colorScheme.error)
            } else {
                OptionPicker(
                    label = "Posizione iniziale (U più bassa)",
                    options = free,
                    selected = start,
                    optionLabel = { "U$it" + if (device!!.heightU > 1) "–U${it + device!!.heightU - 1}" else "" },
                    onSelected = { start = it },
                    supportingText = "Sono elencate solo le posizioni libere"
                )
            }
        }
    }
}

@Composable
private fun RackDialog(index: ProjectIndex, rack: Rack?, onDismiss: () -> Unit, onSave: (Rack, Boolean) -> Unit) {
    var form by remember(LocalDetailSlot.current?.editorVersion, rack) { mutableStateOf(RackForm.from(rack)) }
    val errors = form.errors()
    val tallestDevice = rack?.let { r -> index.devices.filter { it.rackId == r.id && it.positionU != null }.maxOfOrNull { it.positionU!! + it.heightU - 1 } }
    val shrinkError = tallestDevice?.let { top -> form.heightU.toIntOrNull()?.takeIf { it < top }?.let { "Ci sono apparati fino a U$top" } }

    EditPanel(
        title = if (rack == null) "Nuovo rack" else "Modifica «${rack.name}»",
        onDismiss = onDismiss,
        onConfirm = { onSave(form.toRack(rack), rack == null) },
        confirmEnabled = errors.isEmpty() && shrinkError == null,
        width = 520.dp
    ) {
        FormField(form.name, { form = form.copy(name = it) }, "Nome *", error = errors["name"])
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(form.heightU, { form = form.copy(heightU = it) }, "Altezza (U) *", Modifier.weight(1f), errors["heightU"] ?: shrinkError)
            FormField(form.depthMm, { form = form.copy(depthMm = it) }, "Profondità (mm)", Modifier.weight(1f), errors["depthMm"])
        }
        EnumPicker("Numerazione U", NumberingDirection.entries, form.numberingDirection, { it.toDisplayString() }, { form = form.copy(numberingDirection = it) })
        OptionPicker("Area", index.areas, index.area(form.areaId), { it.name }, { form = form.copy(areaId = it?.id) }, noneLabel = "Nessuna area")
        FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false, minLines = 2)
    }
}
