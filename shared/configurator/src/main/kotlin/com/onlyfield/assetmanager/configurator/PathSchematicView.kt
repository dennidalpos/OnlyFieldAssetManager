package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.PathSchematic
import com.onlyfield.assetmanager.core.forms.PathStation
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.CableMedium
import com.onlyfield.assetmanager.core.model.Device

/**
 * Vertical end-to-end path: objects as nodes (active filled, passive outlined, unknown dashed)
 * joined by their cables with label, medium and length. Narrow enough for a phone dialog.
 */
@Composable
fun PathSchematicView(schematic: PathSchematic, i18n: Messages, modifier: Modifier = Modifier, onOpen: ((Device) -> Unit)? = null) {
    val line = MaterialTheme.colorScheme.outline
    Column(modifier) {
        schematic.stations.forEachIndexed { i, station ->
            Station(station, i == schematic.focus, schematic.openEnd(i), i18n, onOpen)
            schematic.segments.getOrNull(i)?.let { segment ->
                val cable = segment.cable
                Row(Modifier.height(IntrinsicSize.Min).semantics(mergeDescendants = true) {}) {
                    // The wire: dashed look for radio, solid otherwise.
                    Box(Modifier.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(if (cable.medium == CableMedium.RADIO) 1.dp else 2.dp).fillMaxHeight().background(line))
                    }
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(segment.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(cable.medium.toDisplayString(i18n), cable.color, cable.lengthValue?.let { "$it ${cable.lengthUnit ?: "m"}" }).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Station(station: PathStation, focus: Boolean, open: Boolean, i18n: Messages, onOpen: ((Device) -> Unit)?) {
    val device = station.device
    val passive = station.passive
    val colors = MaterialTheme.colorScheme
    Surface(color = if (focus) colors.secondaryContainer else Color.Transparent, shape = RoundedCornerShape(6.dp)) {
        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                val shape = if (passive) RoundedCornerShape(2.dp) else CircleShape
                Box(Modifier.size(12.dp).then(if (passive) Modifier.border(2.dp, if (device == null) colors.error else colors.primary, shape) else Modifier.background(colors.primary, shape)))
            }
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(device?.technicalName ?: "?", style = MaterialTheme.typography.bodyMedium, fontWeight = if (passive) null else FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                val ports = station.ports.joinToString(" → ") { p -> listOfNotNull(p.name, p.hardware.side?.takeIf { passive }?.toDisplayString(i18n)).joinToString(" · ") }
                val detail = listOfNotNull(ports.ifBlank { null }, i18n.text("path.openEnd").takeIf { open })
                if (detail.isNotEmpty()) Text(detail.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                    color = if (open) colors.error else colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (device != null && !focus) onOpen?.let { open -> TextButton(onClick = { open(device) }) { Text(i18n.text("quick.open")) } }
        }
    }
}
