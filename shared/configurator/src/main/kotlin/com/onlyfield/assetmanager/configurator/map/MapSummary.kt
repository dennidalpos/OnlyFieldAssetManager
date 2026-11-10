package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.*
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/** Compact selection card; detailed drawings live in the expanded pane. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MapSummary(project: Project, scene: MapScene, selection: MapSelection?, i18n: Messages,
    actions: MapActions, onClose: () -> Unit, onExpand: () -> Unit) {
    val ref = when (selection) {
        is MapSelection.Node -> selection.ref
        is MapSelection.Link -> null
        null -> scene.container
    }
    val node = ref?.let { scene.node(it) }
    val device = ref?.takeIf { it.type == PlacementTargetType.DEVICE }?.let { r -> project.sites.flatMap { it.devices }.find { it.id == r.id } }
    val link = selection as? MapSelection.Link
    val cables = project.cables.filter { it.id in link?.cableIds.orEmpty() }
    val cable = cables.singleOrNull()
    Surface(modifier = Modifier.testTag("map-summary"), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PaneHeader(node?.glyph, ref?.let { ObjectHierarchy.name(project, it, i18n) } ?: cable?.codeOrLabel ?: if (link != null) i18n.plural("map.cableCount", cables.size) else i18n.text("map.list"),
                device?.operationalStatus?.toDisplayString(i18n), i18n, onClose = if (selection != null) onClose else null)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (ref != null) {
                    TextButton(onClick = { actions.edit(editDraft(project, ref, scene.areaId, i18n), ConfiguratorPage.ESSENTIALS) }) { Text(i18n.text("map.edit")) }
                    actions.photo?.let { photo -> TextButton(onClick = { photo(if (ref.type == PlacementTargetType.RACK) AttachmentTargetType.RACK else AttachmentTargetType.DEVICE, ref.id) }) { Text(i18n.text("quick.photo")) } }
                }
                if (cable != null) {
                    TextButton(onClick = { actions.edit(MapObjectDraft.cable(project, floorSite(project, scene.areaId), scene.areaId, cable.id, i18n), ConfiguratorPage.ESSENTIALS) }) { Text(i18n.text("map.editCable")) }
                    actions.photo?.let { photo -> TextButton(onClick = { photo(AttachmentTargetType.CABLE, cable.id) }) { Text(i18n.text("quick.photo")) } }
                }
                TextButton(onClick = onExpand) { Text(i18n.text("ux.expand")) }
            }
        }
    }
}
