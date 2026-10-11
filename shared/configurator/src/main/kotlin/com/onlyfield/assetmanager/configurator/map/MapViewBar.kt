package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.SymbolIcons
import com.onlyfield.assetmanager.core.i18n.Messages

enum class MapLevel { PROJECT, SITE, FLOOR }

/** Ways to look at a map level: drawings first, the plain list last. */
enum class MapView {
    DIAGRAM, TOPOLOGY, LOGICAL, DEVICES, LIST;

    /** The level's own drawing is the sites diagram for a project and the plan for a floor. */
    fun label(level: MapLevel, i18n: Messages): String = i18n.text(when (this) {
        DIAGRAM -> if (level == MapLevel.FLOOR) "mapview.plan" else "maphome.diagram"
        TOPOLOGY -> "topology.title"
        LOGICAL -> "maphome.logical"
        DEVICES -> "mapview.devices"
        LIST -> "map.list"
    })

    fun icon(level: MapLevel) = when (this) {
        DIAGRAM -> if (level == MapLevel.FLOOR) SymbolIcons.map else SymbolIcons.apartment
        TOPOLOGY -> SymbolIcons.lan
        LOGICAL -> SymbolIcons.networks
        DEVICES -> SymbolIcons.dns
        LIST -> SymbolIcons.list
    }

    companion object {
        /** A site has no drawing of its own: its topology comes first. */
        fun forLevel(level: MapLevel): List<MapView> = if (level == MapLevel.SITE) entries - DIAGRAM else entries
    }
}

/** Always-visible view switch: icons with text from 600 dp, below only the active view is spelled out. */
@Composable
fun MapViewBar(views: List<MapView>, selected: MapView, level: MapLevel, i18n: Messages, modifier: Modifier = Modifier, onSelect: (MapView) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val labels = maxWidth >= 600.dp
        Row(Modifier.widthIn(max = 720.dp).fillMaxWidth().testTag("map-views").semantics { contentDescription = i18n.text("mapview.bar") }.selectableGroup()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            views.forEach { view ->
                val active = view == selected
                val label = view.label(level, i18n)
                Row(Modifier.then(if (labels || !active) Modifier.weight(1f) else Modifier).heightIn(min = 48.dp).clip(MaterialTheme.shapes.small)
                    .background(if (active) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                    .selectable(active, role = Role.Tab) { onSelect(view) }
                    // Icon-only entries still need a name.
                    .then(if (labels || active) Modifier else Modifier.semantics { contentDescription = label }).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    val tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(view.icon(level), null, tint = tint)
                    if (labels || active) Text(label, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
