package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
) {
    val size = if (compact) 26.dp else 38.dp
    val occupied = MaterialTheme.colorScheme.primary
    val warning = MaterialTheme.colorScheme.error
    val summary = i18n.text("map.portsUsage", cells.count { it.occupied }, cells.size)
    val whole = if (onPanelClick != null) Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = i18n.text("ux.ports")) { onPanelClick() }
        .clearAndSetSemantics { contentDescription = summary; role = Role.Button; onClick(i18n.text("ux.ports")) { onPanelClick(); true } } else Modifier
    Column(modifier.then(whole), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (cells.isEmpty()) Text(i18n.text("config.noPorts"), style = MaterialTheme.typography.bodySmall)
        val blocks = cells.groupBy { (it.port.hardware.side ?: PortSide.FRONT) to it.port.hardware.group }
        blocks.forEach { (key, block) ->
            val (side, group) = key
            if (blocks.size > 1) Text(listOfNotNull(block.first().port.hardware.connector ?: group?.ifBlank { null },
                side.name.takeIf { cells.any { c -> c.port.hardware.side == PortSide.REAR } }?.let { i18n.text("port.side.$it") })
                .joinToString(" · "), style = MaterialTheme.typography.labelSmall)
            val rows = if (block.size > 8) listOf(block.filterIndexed { i, _ -> i % 2 == 0 }, block.filterIndexed { i, _ -> i % 2 == 1 }) else listOf(block)
            Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        row.forEach { cell ->
                            val isSelected = cell.port.id in selected
                            val state = i18n.text(if (cell.occupied) "port.occupied" else "port.free")
                            val marks = listOfNotNull(state, cell.poe?.let { "PoE" }, cell.vlan?.untaggedVlanId?.let { "VLAN $it" }, cell.peer, i18n.text("port.warning").takeIf { cell.warning })
                            Surface(
                                color = if (cell.occupied) occupied else MaterialTheme.colorScheme.surface,
                                contentColor = if (cell.occupied) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(size)
                                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.tertiary else occupied, RoundedCornerShape(4.dp))
                                    .then(if (onClick != null) Modifier.combinedClickable(onLongClick = onLongClick?.let { { it(cell) } }) { onClick(cell) } else Modifier)
                                    .semantics { contentDescription = "${cell.port.name}: ${marks.joinToString(", ")}"; this.selected = isSelected },
                            ) {
                                Box(Modifier.fillMaxSize().padding(1.dp)) {
                                    Text(cell.port.hardware.position?.toString() ?: cell.port.name, Modifier.align(Alignment.TopCenter),
                                        fontSize = if (compact) 9.sp else 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                                    if (!compact) cell.vlan?.untaggedVlanId?.let { Text(it.toString(), Modifier.align(Alignment.BottomCenter), fontSize = 8.sp, maxLines = 1) }
                                    if (cell.poe != null || cell.poeCapable != null) Text("⚡", Modifier.align(Alignment.BottomStart), fontSize = 8.sp,
                                        color = if (cell.poe != null) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline)
                                    if (cell.warning) Text("!", Modifier.align(Alignment.TopEnd).padding(end = 2.dp), fontSize = 9.sp, color = warning)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!compact && cells.isNotEmpty()) Text(i18n.text("port.legend", cells.count { it.occupied }, cells.size), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
