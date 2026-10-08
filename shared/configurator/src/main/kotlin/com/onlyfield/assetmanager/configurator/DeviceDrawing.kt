package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.map.MapStyle
import com.onlyfield.assetmanager.configurator.map.ValueMenu
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/** Device-specific bezel, side selector and staged hardware edits. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceDrawing(project: Project, device: Device, i18n: Messages,
                  onPort: ((PortCell) -> Unit)? = null, onDeviceChange: ((Device) -> Unit)? = null,
                  selectedPortIds: Set<String> = emptySet(), onLongPort: ((PortCell) -> Unit)? = null, showName: Boolean = true, framed: Boolean = true) {
    var side by remember(device.id) { mutableStateOf(PortSide.FRONT) }
    var arranging by remember(device.id) { mutableStateOf(false) }
    var editingPoe by remember(device.id) { mutableStateOf(false) }
    var selected by remember(device.id) { mutableStateOf(emptySet<String>()) }
    var standard by remember(device.id) { mutableStateOf(PoeStandard.IEEE_802_3AT) }
    val glyph = ObjectGlyph.of(project, device)
    val type = ObjectCatalog.type(project, device.objectTypeId)
    val cells = PortLogic.panel(project, device).filter { PortArrangement.side(it.port) == side || it.port.hardware.side == PortSide.BOTH }
    val ids = cells.map { it.port.id }.toSet()
    val selection = selected.intersect(ids)
    fun swap(a: String, b: String) {
        val source = cells.first { it.port.id == a }.port
        val block = cells.filter { it.port.hardware.group == source.hardware.group && PortArrangement.side(it.port) == PortArrangement.side(source) }.map { it.port }
        val layout = PortArrangement.layout(device, block)
        val first = PortArrangement.key(block.first { it.id == a })
        val second = PortArrangement.key(block.first { it.id == b })
        onDeviceChange?.invoke(PortArrangement.set(device, layout.copy(order = layout.order.map { if (it == first) second else if (it == second) first else it })))
        selected = emptySet()
    }
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = if (framed) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface,
        border = if (framed) BorderStroke(1.dp, MapStyle.glyph(glyph)) else null) {
        Column(Modifier.padding(if (framed) 12.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlyphBadge(glyph, if (glyph.family in setOf(ObjectFamily.ENDPOINT, ObjectFamily.SECURITY)) 56.dp else 36.dp)
                Column(Modifier.weight(1f)) {
                    if (showName) Text(device.technicalName, style = MaterialTheme.typography.titleMedium)
                    Text(type?.let { ObjectCatalog.displayName(it, i18n) } ?: glyph.code, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (device.ports.any { it.hardware.side == PortSide.REAR }) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PortSide.FRONT, PortSide.REAR).forEach { s ->
                    FilterChip(side == s, { side = s; selected = emptySet() }, label = { Text(i18n.text("port.side.${s.name}")) })
                }
            }
            if (side == PortSide.REAR && PassiveCabling.supported(device)) Text(i18n.text("visual.rearTerminations"), style = MaterialTheme.typography.bodySmall)
            if (glyph.typeId in setOf("camera", "access-point", "ip-phone", "sensor", "access-control", "radio-bridge", "ups", "power-supply", "workstation")) {
                val icon = ObjectIcon.of(glyph)
                if (icon != null) Canvas(Modifier.fillMaxWidth().height(80.dp)
                    .then(if (onPort != null && cells.isNotEmpty()) Modifier.clickable(role = Role.Button, onClickLabel = i18n.text("ux.ports")) { onPort(cells.first()) } else Modifier)) {
                    val dimension = this.size.height
                    drawObjectIcon(icon, Offset((this.size.width - dimension) / 2, 0f), dimension, MapStyle.glyph(glyph))
                }
            }
            if (arranging) {
                Text(i18n.text("visual.arrangeHint"), style = MaterialTheme.typography.bodySmall)
                cells.groupBy { PortArrangement.side(it.port) to it.port.hardware.group }.values.forEach { block ->
                    val layout = PortArrangement.layout(device, block.map { it.port })
                    ValueMenu(i18n.text("visual.rows") + " · " + (block.first().port.hardware.connector ?: ""), layout.rows, listOf(1, 2), { it.toString() }) {
                        onDeviceChange?.invoke(PortArrangement.set(device, layout.copy(rows = it)))
                    }
                }
            }
            PortPanel(cells, i18n, device = device, compact = onPort == null && onDeviceChange == null, selected = if (arranging || editingPoe) selection else selectedPortIds,
                onClick = { cell: PortCell ->
                    when {
                        arranging -> {
                            val first = selection.singleOrNull()?.let { id -> cells.firstOrNull { it.port.id == id } }
                            if (first != null && first.port.id != cell.port.id && first.port.hardware.group == cell.port.hardware.group && PortArrangement.side(first.port) == PortArrangement.side(cell.port)) swap(first.port.id, cell.port.id)
                            else selected = setOf(cell.port.id)
                        }
                        editingPoe -> selected = if (cell.port.id in selection) selection - cell.port.id else selection + cell.port.id
                        else -> onPort?.invoke(cell)
                    }
                    Unit
                }.takeIf { onPort != null || onDeviceChange != null },
                onLongClick = if (!arranging && !editingPoe) onLongPort else null,
                onSwap = if (arranging) ::swap else null)
            if (onDeviceChange != null && cells.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { arranging = !arranging; editingPoe = false; selected = emptySet() }) { Text(i18n.text(if (arranging) "visual.finishLayout" else "visual.editLayout")) }
                if (cells.any { it.port.hardware.mediaType == "Copper" } && !device.isPassive())
                    OutlinedButton(onClick = { editingPoe = !editingPoe; arranging = false; selected = emptySet() }) { Text(i18n.text("visual.poePorts")) }
            }
            if (editingPoe) {
                Text(i18n.text("visual.poeHint"), style = MaterialTheme.typography.bodySmall)
                ValueMenu(i18n.text("port.poeStandard"), standard, listOf(PoeStandard.IEEE_802_3AF, PoeStandard.IEEE_802_3AT, PoeStandard.IEEE_802_3BT), { it.toDisplayString(i18n) }) { standard = it }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selected = cells.filter { it.port.hardware.mediaType == "Copper" }.map { it.port.id }.toSet() }) { Text(i18n.text("port.selectAll")) }
                    listOf(true, false).forEach { enable ->
                        OutlinedButton(enabled = selection.isNotEmpty(), onClick = {
                            val updated = PortLogic.setPoeCapability(project, selection, if (enable) standard else null)
                            onDeviceChange?.invoke(updated.sites.flatMap { it.devices }.first { it.id == device.id })
                        }) { Text(i18n.text(if (enable) "visual.enablePoe" else "visual.disablePoe")) }
                    }
                }
            }
        }
    }
}
