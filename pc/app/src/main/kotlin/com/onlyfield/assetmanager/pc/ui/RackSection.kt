package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.onlyfield.assetmanager.configurator.RackElevation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.configurator.*
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
import com.onlyfield.assetmanager.core.forms.RackLayout

@Composable
fun RackSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit,
    onTrashItemCreated: (TrashItem) -> Unit
) {
    val i18n = LocalMessages.current

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
            i18n.text("text.4cd265c2b8c6"),
            subtitle = i18n.plural("text.2c2d174aee1f", project.racks.size),
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = i18n.text("text.0c4a991d1c4b")
        ) {
            Button(onClick = { changeDetail { creating = true } }) { Text(i18n.text("text.b049315ba1c3")) }
        }

        if (project.racks.isEmpty()) {
            EmptyState(i18n.text("text.f42d154a6e60"), actionLabel = i18n.text("text.b049315ba1c3"), onAction = { creating = true })
            return@Column
        }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(modifier = Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(racks.sortedForDisplay(i18n) { it.name }, key = { it.id }) { rack ->
                    val isSelected = rack.id == selected?.id
                    val used = RackLayout.usedUnits(rack, index.devices)
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .border(if (isSelected) 2.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.medium)
                            .clickable { if (selectedRackId != rack.id) changeDetail { selectedRackId = rack.id } },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(rack.name, fontWeight = FontWeight.SemiBold)
                            Text(i18n.text("text.4abc2837dcad", index.areaName(rack.areaId, i18n.text("text.0eb949b8ab9b")), used, rack.heightU), style = MaterialTheme.typography.bodySmall)
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
                                    i18n.text("text.89c9553a084e", rack.heightU, rack.numberingDirection.toDisplayString(i18n = i18n).lowercase()) +
                                        (rack.depthMm?.let { i18n.text("text.0b78a6a7d85b", it) } ?: ""),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            SingleChoiceSegmentedButtonRow {
                                listOf(RackSide.FRONT, RackSide.REAR).forEachIndexed { i, s ->
                                    SegmentedButton(selected = side == s, onClick = { side = s }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                                        Text(s.toDisplayString(i18n = i18n))
                                    }
                                }
                            }
                            Button(onClick = { changeDetail { placing = true } }) { Text(i18n.text("text.9b3e3f84b6a8")) }
                            EditButton { editing = rack }
                            DeleteButton(rack.name, label = i18n.text("text.dd41b3275173"), message = i18n.text("text.309921cb8d51"), onDelete = {
                                val (updated, trashItem) = ProjectEdits.deleteRackToTrash(project, rack.id, i18n = i18n)
                                trashItem?.let(onTrashItemCreated)
                                onProjectUpdated(updated, i18n.text("text.d447cfecb33f", rack.name))
                            })
                        }
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1.3f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                                RackElevation(project, rack, side, i18n)
                            }
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(i18n.text("text.0a7e55b149a2", inRack.size), fontWeight = FontWeight.SemiBold)
                                if (unplaced.isNotEmpty()) {
                                    Text(
                                        i18n.text("text.b9b94499eef0", unplaced.size),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(inRack.sortedByDescending { it.positionU ?: 0 }, key = { it.id }) { dev ->
                                        ItemCard(
                                            title = dev.technicalName,
                                            details = listOf(
                                                (dev.positionU?.let { "U$it" } ?: i18n.text("text.eb0f3a47d9d5")) + i18n.text("text.b84d9d34ca27", dev.heightU, dev.rackSide.toDisplayString(i18n = i18n))
                                            )
                                        ) {
                                            TextButton(onClick = {
                                                confirm(
                                                    ConfirmRequest(
                                                        i18n.text("text.8d2ed151a026", dev.technicalName),
                                                        i18n.text("text.5d77c728e7e5"),
                                                        confirmLabel = i18n.text("text.a499a55ecb5c"),
                                                        destructive = false
                                                    ) {
                                                        val moved = dev.copy(rackId = null, positionU = null, mountingType = MountingType.OUT_OF_RACK)
                                                        onProjectUpdated(ProjectEdits.updateDevice(project, moved, i18n = i18n), i18n.text("text.2a7e4947b621", dev.technicalName))
                                                    }
                                                )
                                            }) { Text(i18n.text("text.a6f1f723fa07")) }
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
                        onProjectUpdated(ProjectEdits.updateDevice(project, placed, i18n = i18n), i18n.text("text.f42fb5a22142", device.technicalName, rack.name, startU))
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        RackDialog(index, editing, onDismiss = { creating = false; editing = null }) { updated, saved, isNew ->
            creating = false; editing = null
            if (isNew) selectedRackId = saved.id
            onProjectUpdated(updated, i18n.text("text.e4ffb3690fdf", saved.name))
        }
    }
}

@Composable
private fun PlaceDeviceDialog(rack: Rack, index: ProjectIndex, initialSide: RackSide, onDismiss: () -> Unit, onPlace: (Device, Int, RackSide) -> Unit) {
    val i18n = LocalMessages.current

    val candidates = index.devices.filter { it.rackId != rack.id || it.positionU == null }
    var device by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf<Device?>(null) }
    var side by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(initialSide) }
    var start by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf<Int?>(null) }
    val free = device?.let { RackLayout.freeStartPositions(rack, index.devices, it.heightU, side, it.id) } ?: emptyList()
    LaunchedEffect(device, side) { start = free.firstOrNull() }

    EditPanel(
        title = i18n.text("text.ad0e11decb9e", rack.name),
        onDismiss = onDismiss,
        onConfirm = { onPlace(device!!, start!!, side) },
        confirmEnabled = device != null && start != null,
        confirmLabel = i18n.text("text.ee84d8c1e721"),
        width = 540.dp
    ) {
        OptionPicker(
            label = i18n.text("text.e7f2c0e68768"),
            options = candidates,
            selected = device,
            optionLabel = { it.technicalName },
            optionDetail = { d -> i18n.text("text.4cb2663bfe55", d.heightU) + (d.rackId?.let { i18n.text("text.d44a8dda0e78", index.rackName(it)) } ?: i18n.text("text.9c568c88f11c")) },
            onSelected = { device = it }
        )
        EnumPicker(i18n.text("text.22ab4cafea0c"), RackSide.entries, side, { it.toDisplayString(i18n = i18n) }, { side = it })
        if (device != null) {
            if (free.isEmpty()) {
                Text(i18n.text("text.89e4d9c3a7e7", device!!.heightU), color = MaterialTheme.colorScheme.error)
            } else {
                OptionPicker(
                    label = i18n.text("text.e4a3ed1f764d"),
                    options = free,
                    selected = start,
                    optionLabel = { "U$it" + if (device!!.heightU > 1) "–U${it + device!!.heightU - 1}" else "" },
                    onSelected = { start = it },
                    supportingText = i18n.text("text.7de7ec5efe1c")
                )
            }
        }
    }
}

@Composable
private fun RackDialog(index: ProjectIndex, rack: Rack?, onDismiss: () -> Unit, onSave: (Project, Rack, Boolean) -> Unit) {
    val i18n = LocalMessages.current
    var draft by remember(LocalDetailSlot.current?.editorVersion, rack) { mutableStateOf(com.onlyfield.assetmanager.core.forms.MapObjectDraft.forRack(index.project, rack)) }
    EditPanel(title = configuratorTitle(index.project, draft, i18n), confirmLabel = configuratorAction(index.project, draft, i18n), onDismiss = onDismiss, width = 800.dp,
        validationMessage = configuratorValidation(index.project, draft, i18n), confirmEnabled = draft.errors(index.project, i18n).isEmpty(), onConfirm = {
            val updated = draft.apply(index.project, i18n)
            onSave(updated, updated.racks.first { it.id == draft.id }, rack == null)
        }) { ObjectFields(index.project, draft) { draft = it } }
}
