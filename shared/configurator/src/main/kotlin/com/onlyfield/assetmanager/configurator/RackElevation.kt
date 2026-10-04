package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.map.MapStyle
import com.onlyfield.assetmanager.core.forms.SchematicGeometry
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

private val UnitHeight = 24.dp
private const val UnitGap = 2

/**
 * Front or rear view of [rack] at full width: each device is one block spanning its units, with
 * icon and name; free units are outlined rows that add an object there when [onAddAt] is set.
 * Not lazy, so it can sit inside scrolling editors.
 */
@Composable
fun RackElevation(project: Project, rack: Rack, side: RackSide, i18n: Messages, modifier: Modifier = Modifier,
                  onDevice: ((Device) -> Unit)? = null, onAddAt: ((Int) -> Unit)? = null) {
    val devices = project.businessUnits.flatMap { it.devices }.filter { it.rackId == rack.id && it.positionU != null && (it.rackSide == RackSide.BOTH || it.rackSide == side) }
    fun at(u: Int) = devices.find { d -> u >= d.positionU!! && u < d.positionU!! + d.heightU }
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(UnitGap.dp)) {
            val units = SchematicGeometry.rackUnits(rack)
            var i = 0
            while (i < units.size) {
                val u = units[i]
                val device = at(u)
                if (device == null) {
                    FreeUnit(u, i18n, onAddAt)
                    i++
                } else {
                    // One block for all consecutive units of the device, in drawing order.
                    val span = units.drop(i).takeWhile { at(it) == device }.size
                    DeviceBlock(project, device, span, i18n, onDevice)
                    i += span
                }
            }
        }
    }
}

@Composable
private fun UnitLabel(u: Int, color: Color) =
    Text("$u", style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.width(24.dp).clearAndSetSemantics {})

@Composable
private fun FreeUnit(u: Int, i18n: Messages, onAddAt: ((Int) -> Unit)?) {
    val label = i18n.text("rack.freeUnit", u)
    Row(Modifier.fillMaxWidth().height(UnitHeight)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)
        .then(if (onAddAt != null) Modifier.clickable(role = Role.Button, onClickLabel = i18n.text("config.addAtUnit")) { onAddAt(u) } else Modifier)
        .semantics(mergeDescendants = true) { contentDescription = label }
        .padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        UnitLabel(u, MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        if (onAddAt != null) Text("+", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
private fun DeviceBlock(project: Project, device: Device, span: Int, i18n: Messages, onDevice: ((Device) -> Unit)?) {
    val glyph = ObjectGlyph.of(project, device)
    val p = device.positionU!!
    val units = if (device.heightU > 1) "U$p–${p + device.heightU - 1}" else "U$p"
    val height = UnitHeight * span + (UnitGap * (span - 1)).dp
    Surface(color = MapStyle.family(glyph.family), contentColor = Color.White, shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.fillMaxWidth().height(height)
            .then(if (onDevice != null) Modifier.clickable(role = Role.Button) { onDevice(device) } else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = "$units ${device.technicalName}" }) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            UnitLabel(p, Color.White.copy(alpha = .8f))
            GlyphBadge(glyph, 20.dp)
            Text(device.technicalName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (span > 1 || device.heightU > 1) Text(units, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .8f), modifier = Modifier.clearAndSetSemantics {})
        }
    }
}
