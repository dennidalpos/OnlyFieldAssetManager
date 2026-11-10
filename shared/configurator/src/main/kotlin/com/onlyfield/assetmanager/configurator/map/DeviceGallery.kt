package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.DeviceDrawing
import com.onlyfield.assetmanager.configurator.GlyphBadge
import com.onlyfield.assetmanager.configurator.PortQuickActions
import com.onlyfield.assetmanager.configurator.PortQuickDialog
import com.onlyfield.assetmanager.configurator.RackElevation
import com.onlyfield.assetmanager.configurator.theme.SectionCard
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.forms.DeviceGalleries
import com.onlyfield.assetmanager.core.forms.RackLayout
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

private val DrawingWidth = 340.dp

/**
 * Drawings of the devices in scope: each rack as an elevation, each device as its front with the
 * ports. A port opens its card when [portActions] is set; a heading or a rack block opens the device.
 */
@Composable
fun DeviceGallery(project: Project, i18n: Messages, siteId: String?, areaId: String?, onDevice: (Device) -> Unit, modifier: Modifier = Modifier,
                  container: ObjectRef? = null, portActions: PortQuickActions? = null) {
    var endpoints by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var quickPort by remember { mutableStateOf<String?>(null) }
    val hierarchy = remember(project) { HierarchyIndex(project) }
    val index = remember(project) { ProjectIndex(project) }
    val graph = remember(project) { ConnectionGraph(project) }
    val all = remember(project, siteId, areaId, container, endpoints) { DeviceGalleries.build(project, siteId, areaId, container, endpoints, hierarchy) }
    val text = query.trim()
    fun matches(d: Device) = d.technicalName.contains(text, true) || d.physicalLabel?.contains(text, true) == true
    val groups = if (text.isEmpty()) all else all.mapNotNull { g ->
        if (g.rack?.name?.contains(text, true) == true) g else g.copy(devices = g.devices.filter(::matches)).takeIf { it.devices.isNotEmpty() }
    }
    BoxWithConstraints(modifier.fillMaxWidth().testTag("device-gallery")) {
        val perRow = (maxWidth / DrawingWidth).toInt().coerceIn(1, 4)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (all.sumOf { it.devices.size } > 10 || text.isNotEmpty()) OutlinedTextField(query, { query = it }, label = { Text(i18n.text("map.filter")) }, singleLine = true,
                    modifier = Modifier.weight(1f).widthIn(max = 360.dp))
                Row(Modifier.toggleable(endpoints, role = Role.Switch) { endpoints = it }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(endpoints, null); Text(i18n.text("topology.endpoints"))
                }
            }
            if (groups.isEmpty()) Text(i18n.text(if (text.isEmpty()) "devices.empty" else "ux.noResults"), style = MaterialTheme.typography.bodyMedium)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                groups.forEach { group ->
                    item(key = group.container?.id ?: "loose") {
                        val rack = group.rack
                        val title = group.container?.let { ObjectHierarchy.name(project, it, i18n) } ?: i18n.text("devices.loose")
                        SectionCard(title, count = group.devices.size, detail = rack?.let { i18n.text("map.rackUnitsValue", RackLayout.usedUnits(it, index.devices), it.heightU) },
                            trailing = { GlyphBadge(rack?.let { ObjectGlyph.RACK } ?: group.devices.firstOrNull()?.let { ObjectGlyph.of(project, it) } ?: ObjectGlyph.RACK, 28.dp) }) {
                            // Elevation and fronts side by side where they fit: the rack stays in view while reading its devices.
                            val cells: List<@Composable () -> Unit> = listOfNotNull<@Composable () -> Unit>(rack?.let { r -> { RackElevation(project, r, RackSide.FRONT, i18n, onDevice = onDevice) } }) +
                                group.devices.map { d -> {
                                    DeviceDrawing(project, d, i18n, onPort = portActions?.let { { cell -> quickPort = cell.port.id } }, onOpen = { onDevice(d) }, graph = graph, index = index)
                                } }
                            cells.chunked(perRow).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                                    row.forEach { cell -> Box(Modifier.weight(1f)) { cell() } }
                                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val actions = portActions
    quickPort?.let { id -> if (actions != null) PortQuickDialog(project, id, i18n, actions, onClose = { quickPort = null }) }
}
