package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.onlyfield.assetmanager.configurator.theme.Button
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

/** What the Map area shows above the floor level: drawings first, the plain list one tap away. */
enum class MapHomeView(val labelKey: String) { DIAGRAM("maphome.diagram"), LOGICAL("maphome.logical"), LIST("map.list") }

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
    /** Device topology of the whole project, offered at project level. */
    onTopology: (() -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MapHomeView.entries.forEach { view ->
                FilterChip(state.homeView == view, { state.homeView = view }, { Text(i18n.text(view.labelKey)) }, Modifier.heightIn(min = 48.dp))
            }
            if (site == null) {
                Button(onClick = onAddSite) { Text(i18n.text("text.4e90901d9fa2")) }
                if (onTopology != null && project.sites.isNotEmpty()) OutlinedButton(onClick = onTopology) { Text(i18n.text("topology.title")) }
            } else {
                Button(onClick = onAddFloor) { Text(i18n.text("text.3575ad226840")) }
                // A single site is opened directly: keep the way to add the second one.
                if (project.sites.size == 1) OutlinedButton(onClick = onAddSite) { Text(i18n.text("text.4e90901d9fa2")) }
            }
        }
        val floors = remember(site, i18n.locale) { site?.areas.orEmpty().sortedForDisplay(i18n) { it.name } }
        when {
            project.sites.isEmpty() -> Text(i18n.text("sites.empty"))
            state.homeView == MapHomeView.LIST -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (site == null) project.sites.sitesForDisplay(i18n).forEach { s ->
                    ListCard(i18n.text("text.edc54eb6f93e", s.displayName(), s.areas.size)) { onSite(s) }
                } else floors.forEach { a ->
                    ListCard(i18n.text("text.0b16578ff793", a.name, ObjectMap.nodes(project, a.id).size + ObjectMap.routes(project, a.id).size)) { onFloor(a) }
                }
            }
            state.homeView == MapHomeView.LOGICAL -> LogicalView(project, i18n, site?.id, onDevice, Modifier.weight(1f))
            site == null -> SiteOverviewView(project, i18n, onSite, Modifier.weight(1f))
            else -> {
                if (floors.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(i18n.text("maphome.floors"), Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.labelLarge)
                    floors.forEach { a -> AssistChip({ onFloor(a) }, { Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, Modifier.heightIn(min = 48.dp)) }
                }
                TopologyView(project, i18n, site.id, null, onDevice, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ListCard(text: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Text(text, Modifier.padding(16.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            Text(i18n.text("sites.links"), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall)
            if (overview.links.isEmpty()) Text(i18n.text("sites.noLinks"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            overview.links.forEach { link ->
                Column {
                    Text("${labels[link.a]} ↔ ${labels[link.b]}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(describe(link, i18n), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
