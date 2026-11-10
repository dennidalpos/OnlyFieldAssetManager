package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.PortQuickActions
import com.onlyfield.assetmanager.configurator.SymbolIcons
import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.ListRow
import com.onlyfield.assetmanager.configurator.theme.ListRows
import com.onlyfield.assetmanager.configurator.theme.SectionCard
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.core.display.displayName
import com.onlyfield.assetmanager.core.display.sitesForDisplay
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.SiteLink
import com.onlyfield.assetmanager.core.forms.SiteOverview
import com.onlyfield.assetmanager.core.forms.SiteTopology
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/**
 * Map area above the floor level. Project: the sites and how they reach each other. Site: its floors
 * and the physical topology of its active devices. [site] is null at project level.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapHome(
    project: Project,
    i18n: Messages,
    site: Site?,
    state: MapUiState,
    onSite: (Site) -> Unit,
    onFloor: (Area) -> Unit,
    onDevice: (Device) -> Unit,
    onAddSite: () -> Unit,
    onAddFloor: () -> Unit,
    modifier: Modifier = Modifier,
    /** Port card from the device drawings; null leaves the ports read-only. */
    portActions: PortQuickActions? = null,
) {
    val level = if (site == null) MapLevel.PROJECT else MapLevel.SITE
    val view = if (site == null) state.projectView else state.siteView
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MapViewBar(MapView.forLevel(level), view, level, i18n) { if (site == null) state.projectView = it else state.siteView = it }
        val floors = remember(site, i18n.locale) { site?.areas.orEmpty().sortedForDisplay(i18n) { it.name } }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (site == null) Button(onClick = onAddSite) { Text(i18n.text("text.4e90901d9fa2")) }
            else {
                // The floors stay one tap away whatever the view.
                if (floors.isNotEmpty()) Text(i18n.text("maphome.floors"), Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.labelLarge)
                floors.forEach { a -> AssistChip({ onFloor(a) }, { Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, Modifier.heightIn(min = 48.dp)) }
                Button(onClick = onAddFloor) { Text(i18n.text("text.3575ad226840")) }
                // A single site is opened directly: keep the way to add the second one.
                if (project.sites.size == 1) OutlinedButton(onClick = onAddSite) { Text(i18n.text("text.4e90901d9fa2")) }
            }
        }
        when {
            project.sites.isEmpty() -> Text(i18n.text("sites.empty"))
            view == MapView.LIST -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SectionCard(i18n.text(if (site == null) "mapview.sites" else "maphome.floors"), count = if (site == null) project.sites.size else floors.size) {
                    if (site == null) ListRows(project.sites.sitesForDisplay(i18n)) { s ->
                        ListRow(s.displayName(), subtitle = i18n.text("sites.node", s.areas.size, s.devices.count { !it.isPassive() }), leading = { Icon(SymbolIcons.apartment, null) }) { onSite(s) }
                    } else ListRows(floors) { a ->
                        ListRow(a.name, subtitle = i18n.plural("map.objectCount", ObjectMap.nodes(project, a.id).size + ObjectMap.routes(project, a.id).size), leading = { Icon(SymbolIcons.map, null) }) { onFloor(a) }
                    }
                }
            }
            view == MapView.LOGICAL -> LogicalView(project, i18n, site?.id, onDevice, Modifier.weight(1f))
            view == MapView.DEVICES -> DeviceGallery(project, i18n, site?.id, null, onDevice, Modifier.weight(1f), portActions = portActions)
            site != null -> TopologyView(project, i18n, site.id, null, onDevice, Modifier.weight(1f))
            view == MapView.TOPOLOGY -> {
                // Whole project, narrowed to a site and then a floor on demand.
                var siteId by remember(project.id) { mutableStateOf<String?>(null) }
                var areaId by remember(project.id) { mutableStateOf<String?>(null) }
                val current = project.sites.find { it.id == siteId }
                TopologyView(project, i18n, current?.id, areaId.takeIf { current != null }, onDevice, Modifier.weight(1f)) {
                    ValueMenu(i18n.text("topology.site"), current, listOf<Site?>(null) + project.sites.sitesForDisplay(i18n), { it?.displayName() ?: i18n.text("topology.allSites") },
                        Modifier.widthIn(min = 160.dp, max = 280.dp)) { siteId = it?.id; areaId = null }
                    if (current != null) ValueMenu(i18n.text("topology.floor"), current.areas.find { it.id == areaId }, listOf<Area?>(null) + current.areas.sortedForDisplay(i18n) { it.name },
                        { it?.name ?: i18n.text("topology.allFloors") }, Modifier.widthIn(min = 160.dp, max = 280.dp)) { areaId = it?.id }
                }
            }
            else -> SiteOverviewView(project, i18n, onSite, Modifier.weight(1f))
        }
    }
}

private val SiteWidth = 148.dp
private val SiteHeight = 68.dp
private val SiteGap = 16.dp
private val SiteRowGap = 48.dp

/** Sites as nodes, cables and WAN/VPN connections as links; the list below spells every link out. */
@Composable
fun SiteOverviewView(project: Project, i18n: Messages, onSite: (Site) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth
        val perRow = ((width + SiteGap) / (SiteWidth + SiteGap)).toInt().coerceIn(1, 4)
        val overview = remember(project, perRow, i18n.locale) { SiteTopology.build(project, perRow, i18n) }
        val labels = overview.nodes.associate { it.id to it.label }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SiteCanvas(overview, i18n, width, onSite)
            SectionCard(i18n.text("sites.links"), count = overview.links.size.takeIf { it > 0 }) {
                if (overview.links.isEmpty()) Text(i18n.text("sites.noLinks"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ListRows(overview.links) { link -> ListRow("${labels[link.a]} ↔ ${labels[link.b]}", subtitle = describe(link, i18n)) }
            }
        }
    }
}

private fun describe(link: SiteLink, i18n: Messages): String = (listOfNotNull(
    link.cables.takeIf { it > 0 }?.let { i18n.plural("sites.cables", it) },
    i18n.text("sites.radio").takeIf { link.radio },
) + link.connections.map { c -> listOfNotNull(c.type.toDisplayString(i18n = i18n), c.name, c.providerOrCarrier, c.bandwidth).joinToString(" ") }).joinToString(" · ")

@Composable
private fun SiteCanvas(overview: SiteOverview, i18n: Messages, width: Dp, onSite: (Site) -> Unit) {
    val rowStep = SiteHeight + SiteRowGap
    val centres = overview.nodes.associate { n ->
        val count = overview.rowSizes[n.row]
        val left = (width - (SiteWidth * count + SiteGap * (count - 1))) / 2
        n.id to (left + (SiteWidth + SiteGap) * n.column + SiteWidth / 2 to rowStep * n.row + SiteHeight / 2)
    }
    val cable = MaterialTheme.colorScheme.outline
    val radio = MaterialTheme.colorScheme.tertiary
    val logical = MaterialTheme.colorScheme.primary
    Box(Modifier.width(width).height(rowStep * overview.rowSizes.size)) {
        Canvas(Modifier.matchParentSize()) {
            overview.links.forEachIndexed { i, link ->
                val a = centres[link.a] ?: return@forEachIndexed
                val b = centres[link.b] ?: return@forEachIndexed
                val (top, bottom) = if (a.second <= b.second) a to b else b to a
                val half = (SiteHeight / 2).toPx()
                val path = Path().apply {
                    moveTo(top.first.toPx(), top.second.toPx() + half)
                    if (top.second == bottom.second) {
                        // Same row: dip into the gap below so the wire never hides behind a site in between.
                        val dip = top.second.toPx() + half + SiteRowGap.toPx() * (.3f + .12f * (i % 4))
                        lineTo(top.first.toPx(), dip); lineTo(bottom.first.toPx(), dip); lineTo(bottom.first.toPx(), bottom.second.toPx() + half)
                    } else lineTo(bottom.first.toPx(), bottom.second.toPx() - half)
                }
                // Cables solid, radio bridges long dashes, WAN/VPN only short dashes.
                val (color, dash) = when {
                    link.cables > 0 && !link.radio -> cable to null
                    link.cables > 0 -> radio to PathEffect.dashPathEffect(floatArrayOf(18f, 9f))
                    else -> logical to PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                }
                drawPath(path, color, style = Stroke((1 + minOf(link.cables, 4)).dp.toPx(), pathEffect = dash))
            }
        }
        overview.nodes.forEach { node ->
            val (x, y) = centres.getValue(node.id)
            val links = overview.links.count { it.a == node.id || it.b == node.id }
            val site = node.site
            val detail = if (site == null) i18n.text("sites.external") else i18n.text("sites.node", node.floors, node.activeDevices)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (site == null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (site == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline).takeIf { site == null },
                modifier = Modifier.offset(x - SiteWidth / 2, y - SiteHeight / 2).size(SiteWidth, SiteHeight)
                    .then(if (site == null) Modifier else Modifier.clickable(role = Role.Button) { onSite(site) })
                    .semantics(mergeDescendants = true) {
                        contentDescription = listOf(site?.displayName() ?: node.label, detail, i18n.plural("topology.links", links)).joinToString(", ")
                    },
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.Center) {
                    Text(node.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (site != null && site.code?.isNotBlank() == true) Text(site.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(detail, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
