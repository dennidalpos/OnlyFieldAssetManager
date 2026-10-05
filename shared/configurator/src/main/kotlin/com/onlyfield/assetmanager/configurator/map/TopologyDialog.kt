package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.displayName
import com.onlyfield.assetmanager.core.display.sitesForDisplay
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.core.forms.PhysicalTopology
import com.onlyfield.assetmanager.core.forms.Topology
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

private val NodeWidth = 132.dp
private val NodeHeight = 48.dp
private val ColumnGap = 12.dp
private val RowGap = 44.dp

/**
 * Physical topology of the active devices (passives collapsed into the links), full screen.
 * Rows follow the viewport width, so only vertical scrolling is ever needed; a tap opens the device.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopologyDialog(project: Project, i18n: Messages, siteId: String?, areaId: String?, onOpen: (Device) -> Unit, onClose: () -> Unit) {
    var site by remember { mutableStateOf(siteId) }
    var area by remember { mutableStateOf(areaId) }
    var endpoints by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(i18n.text("topology.title"), Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onClose) { Text(i18n.text("ux.close")) }
                }
                // Filters wrap instead of scrolling sideways.
                val sites = remember(project) { listOf<Site?>(null) + project.sites.sitesForDisplay(i18n) }
                val current = project.sites.find { it.id == site }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValueMenu(i18n.text("topology.site"), current, sites, { it?.displayName() ?: i18n.text("topology.allSites") }, Modifier.widthIn(min = 160.dp, max = 280.dp)) {
                        site = it?.id; area = null
                    }
                    if (current != null) {
                        val areas = listOf<Area?>(null) + current.areas.sortedForDisplay(i18n) { it.name }
                        ValueMenu(i18n.text("topology.floor"), current.areas.find { it.id == area }, areas, { it?.name ?: i18n.text("topology.allFloors") },
                            Modifier.widthIn(min = 160.dp, max = 280.dp)) { area = it?.id }
                    }
                    Row(Modifier.toggleable(endpoints, role = Role.Switch) { endpoints = it }.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(endpoints, null); Text(i18n.text("topology.endpoints"))
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val width = maxWidth
                    val perRow = ((width + ColumnGap) / (NodeWidth + ColumnGap)).toInt().coerceAtLeast(1)
                    val topology = remember(project, site, area, perRow, endpoints) { PhysicalTopology.build(project, site, area, perRow, foldEndpoints = !endpoints) }
                    Column {
                        Text(i18n.text("topology.summary", topology.nodes.count { !it.outside } + topology.folded.values.sumOf { it.size }, topology.links.size, topology.stubs.values.sum()),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (topology.nodes.isEmpty()) Text(i18n.text("topology.empty"), Modifier.padding(top = 16.dp))
                        else TopologyCanvas(project, topology, i18n, width, onOpen)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopologyCanvas(project: Project, topology: Topology, i18n: Messages, width: Dp, onOpen: (Device) -> Unit) {
    val index = remember(project) { ProjectIndex(project) }
    val rowStep = NodeHeight + RowGap
    val height = rowStep * topology.rowSizes.size
    // Node centres: each row is centred in the available width.
    val centres = topology.nodes.associate { n ->
        val count = topology.rowSizes[n.row]
        val left = (width - (NodeWidth * count + ColumnGap * (count - 1))) / 2
        n.device.id to DpOffset(left + (NodeWidth + ColumnGap) * n.column + NodeWidth / 2, rowStep * n.row + NodeHeight / 2)
    }
    val line = MaterialTheme.colorScheme.outline
    val radio = MaterialTheme.colorScheme.tertiary
    Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
        Box(Modifier.width(width).height(height)) {
            Canvas(Modifier.matchParentSize()) {
                topology.links.forEach { link ->
                    val a = centres[link.a] ?: return@forEach
                    val b = centres[link.b] ?: return@forEach
                    val wireless = CableMedium.RADIO in link.media
                    // Between rows the wire leaves the bottom edge and enters the top edge, so it never crosses its own nodes.
                    val (top, bottom) = if (a.y <= b.y) a to b else b to a
                    val half = if (top.y == bottom.y) 0.dp else NodeHeight / 2
                    drawLine(if (wireless) radio else line, Offset(top.x.toPx(), (top.y + half).toPx()), Offset(bottom.x.toPx(), (bottom.y - half).toPx()),
                        strokeWidth = (1 + minOf(link.paths, 4)).dp.toPx(),
                        pathEffect = if (wireless) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null)
                }
            }
            topology.nodes.forEach { node ->
                val c = centres.getValue(node.device.id)
                val stubs = topology.stubs[node.device.id] ?: 0
                val endpointsHere = topology.folded[node.device.id]?.size ?: 0
                val links = topology.links.count { it.a == node.device.id || it.b == node.device.id }
                val faded = node.outside || node.device.operationalStatus == OperationalStatus.OFF || node.device.operationalStatus == OperationalStatus.DECOMMISSIONED
                val place = listOfNotNull(index.siteOf(node.device.id)?.code ?: index.siteOf(node.device.id)?.name, node.areaId?.let(index::areaName)).joinToString(" · ")
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (node.outside) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 1.dp,
                    modifier = Modifier.offset(c.x - NodeWidth / 2, c.y - NodeHeight / 2).size(NodeWidth, NodeHeight).alpha(if (faded) .5f else 1f)
                        .clickable(role = Role.Button) { onOpen(node.device) }
                        .semantics(mergeDescendants = true) {
                            contentDescription = listOfNotNull(node.device.technicalName, place.ifBlank { null }, i18n.plural("topology.links", links),
                                endpointsHere.takeIf { it > 0 }?.let { i18n.plural("topology.folded", it) }, stubs.takeIf { it > 0 }?.let { i18n.plural("topology.stubs", it) }).joinToString(", ")
                        },
                ) {
                    Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalArrangement = Arrangement.Center) {
                        Text(node.device.technicalName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(endpointsHere.takeIf { it > 0 }?.let { "+$it" }, place.ifBlank { null }, stubs.takeIf { it > 0 }?.let { "⋯$it" }).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private data class DpOffset(val x: Dp, val y: Dp)
