package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.onlyfield.assetmanager.configurator.theme.TextButton
import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.forms.PortCell
import com.onlyfield.assetmanager.core.forms.SchematicGeometry
import com.onlyfield.assetmanager.core.forms.PortArrangement
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.PortSide

/**
 * Switch-like port panel: odd ports on top, even below, one block per group and side.
 * Occupied = filled, free = outlined; PoE, VLAN and warnings are small marks, never colour alone.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PortPanel(
    cells: List<PortCell>,
    i18n: Messages,
    modifier: Modifier = Modifier,
    selected: Set<String> = emptySet(),
    compact: Boolean = false,
    onClick: ((PortCell) -> Unit)? = null,
    onLongClick: ((PortCell) -> Unit)? = null,
    /** Compact panels are one touch target (cells are too small to tap one by one). */
    onPanelClick: (() -> Unit)? = null,
    device: Device? = null,
    onSwap: ((String, String) -> Unit)? = null,
) {
    val occupied = MaterialTheme.colorScheme.primary
    val warning = MaterialTheme.colorScheme.error
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val bounds = remember(cells.map { it.port.id }) { mutableMapOf<String, Rect>() }
    val summary = i18n.text("map.portsUsage", cells.count { it.occupied }, cells.size)
    val whole = if (onPanelClick != null) Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = i18n.text("ux.ports")) { onPanelClick() }
        .clearAndSetSemantics { contentDescription = summary; role = Role.Button; onClick(i18n.text("ux.ports")) { onPanelClick(); true } } else Modifier
    Column(modifier.then(whole), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (cells.isEmpty()) Text(i18n.text("config.noPorts"), style = MaterialTheme.typography.bodySmall)
        val blocks = cells.groupBy { (it.port.hardware.side ?: PortSide.FRONT) to it.port.hardware.group }
        blocks.forEach { (key, block) ->
            val (side, group) = key
            val layout = device?.let { PortArrangement.layout(it, block.map { cell -> cell.port }) }
            val ordered = if (layout == null) block else layout.order.mapNotNull { k -> block.firstOrNull { PortArrangement.key(it.port) == k } }
            if (blocks.size > 1) Text(listOfNotNull(block.first().port.hardware.connector ?: group?.ifBlank { null },
                side.name.takeIf { cells.any { c -> c.port.hardware.side == PortSide.REAR } }?.let { i18n.text("port.side.$it") })
                .joinToString(" · "), style = MaterialTheme.typography.labelSmall)
            // Cell size follows the available width; long blocks wrap into bands instead of scrolling.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val grid = if (layout != null) SchematicGeometry.arrangedGrid(ordered.size, layout.rows, maxWidth.value, 3f, if (compact) 26f else 48f)
                    else SchematicGeometry.portGrid(block.size, maxWidth.value, 3f, if (compact) 20f else 48f, if (compact) 26f else 48f)
                val size = grid.cell.dp
                val marks = !compact && grid.cell >= 32f
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    grid.bands.forEach { band ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            band.forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    row.forEach { i ->
                                        val cell = ordered[i]
                                        val isSelected = cell.port.id in selected
                                        val state = i18n.text(if (cell.occupied) "port.occupied" else "port.free")
                                        val details = listOfNotNull(state, cell.poe?.let { "PoE" }, cell.vlan?.untaggedVlanId?.let { "VLAN $it" }, cell.peer, i18n.text("port.warning").takeIf { cell.warning }, i18n.text("port.noPhoto").takeIf { cell.photoMissing })
                                        Surface(
                                            color = if (cell.occupied) occupied else MaterialTheme.colorScheme.surface,
                                            contentColor = if (cell.occupied) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(3.dp),
                                            modifier = Modifier.size(size)
                                                .onGloballyPositioned { bounds[cell.port.id] = it.boundsInRoot() }
                                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.tertiary else occupied, RoundedCornerShape(3.dp))
                                                .then(if (onSwap != null) Modifier.pointerInput(cell.port.id, ordered.map { it.port.id }) {
                                                    var drag = Offset.Zero
                                                    detectDragGestures(onDragStart = { drag = it }, onDrag = { change, delta -> change.consume(); drag += delta },
                                                        onDragEnd = {
                                                            val start = bounds[cell.port.id]?.topLeft
                                                            if (start != null) bounds.entries.firstOrNull { (id, rect) -> id != cell.port.id && rect.contains(start + drag) && ordered.any { it.port.id == id } }
                                                                ?.let { onSwap(cell.port.id, it.key) }
                                                        })
                                                } else Modifier)
                                                .then(if (onClick != null) Modifier.combinedClickable(onLongClick = onLongClick?.let { { it(cell) } }) { onClick(cell) } else Modifier)
                                                .semantics { contentDescription = "${cell.port.name}: ${details.joinToString(", ")}"; this.selected = isSelected },
                                        ) {
                                            Box(Modifier.fillMaxSize().padding(1.dp)) {
                                                Canvas(Modifier.matchParentSize()) {
                                                    val middle = this.size.width / 2
                                                    val color = if (cell.occupied) Color.White else ink
                                                    if (side == PortSide.REAR) {
                                                        if (cell.occupied) drawLine(color, Offset(middle, this.size.height * .55f), Offset(middle, this.size.height), 5.dp.toPx())
                                                        repeat(4) { pin -> drawLine(Color(0xFFD6AD45), Offset(middle - 7.dp.toPx() + pin * 4.dp.toPx(), this.size.height * .45f),
                                                            Offset(middle - 7.dp.toPx() + pin * 4.dp.toPx(), this.size.height * .65f), 2.dp.toPx()) }
                                                    } else if (cell.port.hardware.connector in setOf("C13", "Schuko")) {
                                                        drawCircle(color, this.size.width * .21f, Offset(middle, this.size.height * .57f), style = Stroke(1.dp.toPx()))
                                                        listOf(-1, 1).forEach { pin -> drawCircle(color, 2.dp.toPx(), Offset(middle + pin * 4.dp.toPx(), this.size.height * .57f)) }
                                                    } else {
                                                        drawRoundRect(color, Offset(this.size.width * .25f, this.size.height * .4f), Size(this.size.width * .5f, this.size.height * .3f),
                                                            CornerRadius(2.dp.toPx()), style = Stroke(1.dp.toPx()))
                                                        if (cell.port.hardware.connector == "RJ45") repeat(4) { pin -> drawLine(color,
                                                            Offset(middle - 6.dp.toPx() + pin * 4.dp.toPx(), this.size.height * .42f), Offset(middle - 6.dp.toPx() + pin * 4.dp.toPx(), this.size.height * .5f), 1.dp.toPx()) }
                                                        else drawLine(color, Offset(this.size.width * .3f, this.size.height * .55f), Offset(this.size.width * .7f, this.size.height * .55f), 1.dp.toPx())
                                                    }
                                                }
                                                Text(cell.port.hardware.position?.toString() ?: cell.port.name, Modifier.align(Alignment.TopCenter),
                                                    fontSize = (grid.cell * .3f).coerceIn(9f, 12f).sp,
                                                    lineHeight = ((grid.cell * .3f).coerceIn(9f, 12f) * 1.1f).sp, textAlign = TextAlign.Center, maxLines = 1)
                                                if (marks) cell.vlan?.untaggedVlanId?.let { Text(it.toString(), Modifier.align(Alignment.BottomCenter), fontSize = 8.sp, maxLines = 1) }
                                                if (cell.poe != null || cell.poeCapable != null) Text("⚡", Modifier.align(Alignment.BottomStart), fontSize = 8.sp,
                                                    color = if (cell.poe != null) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline)
                                                if (cell.warning) Text("!", Modifier.align(Alignment.TopEnd).padding(end = 2.dp), fontSize = 9.sp, color = warning)
                                                if (cell.photoMissing && !compact) Text("•", Modifier.align(Alignment.BottomEnd).padding(end = 2.dp), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!compact && cells.isNotEmpty()) {
            var showLegend by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(summary, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { showLegend = !showLegend }) { Text(i18n.text("map.legend")) }
            }
            if (showLegend) Text(i18n.text("port.legend", cells.count { it.occupied }, cells.size), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
