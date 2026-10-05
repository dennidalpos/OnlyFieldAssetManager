package com.onlyfield.assetmanager.configurator

import com.onlyfield.assetmanager.configurator.theme.TextButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.ObjectKind
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.MountingType

enum class ConfiguratorPage { ESSENTIALS, PORTS }

/** Expansion changes presentation only; field values stay in the parent's draft. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConfiguratorSection(
    title: String,
    initiallyExpanded: Boolean = false,
    error: String? = null,
    i18n: Messages = LocalConfiguratorMessages.current,
    focusOnOpen: Boolean = false,
    /** What the section holds, shown while it is collapsed (e.g. "24 porte · 4 occupate"). */
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(error) { if (error != null) expanded = true }
    LaunchedEffect(focusOnOpen) { if (focusOnOpen) bringIntoView.bringIntoView() }
    Column(Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { stateDescription = listOfNotNull(i18n.text(if (expanded) "ux.expanded" else "ux.collapsed"), error).joinToString(", ") },
            // Rectangular so a two-line header (title + summary) is not clipped by rounded corners.
            shape = RectangleShape,
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                if (!expanded) summary?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (error != null) Text("!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp).clearAndSetSemantics {})
            Text(if (expanded) "▴" else "▾")
        }
        if (expanded && error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        if (expanded) Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

val LocalConfiguratorMessages = staticCompositionLocalOf { Messages() }

fun configuratorExists(project: Project, draft: MapObjectDraft): Boolean = when (draft.type.kind) {
    ObjectKind.DEVICE -> project.sites.any { site -> site.devices.any { it.id == draft.id } }
    ObjectKind.RACK -> project.racks.any { it.id == draft.id }
    ObjectKind.CABLE -> project.cables.any { it.id == draft.id }
}

fun configuratorTitle(project: Project, draft: MapObjectDraft, i18n: Messages, page: ConfiguratorPage = ConfiguratorPage.ESSENTIALS): String {
    val kind = draft.type.kind.name.lowercase()
    val name = when (draft.type.kind) {
        ObjectKind.DEVICE -> draft.device.technicalName
        ObjectKind.RACK -> draft.rack.name
        ObjectKind.CABLE -> draft.cable.codeOrLabel
    }
    return when {
        page == ConfiguratorPage.PORTS -> i18n.text("ux.configurePorts", name)
        configuratorExists(project, draft) -> i18n.text("ux.edit.$kind", name)
        else -> i18n.text("ux.add.$kind")
    }
}

fun configuratorAction(project: Project, draft: MapObjectDraft, i18n: Messages): String =
    i18n.text(if (configuratorExists(project, draft)) "ux.saveChanges" else "ux.add")

fun inventoryDeviceDraft(project: Project, device: Device?): MapObjectDraft {
    val draft = MapObjectDraft.forDevice(project, device)
    return if (device != null) draft else draft.copy(siteId = "", device = draft.device.copy(
        siteId = null, areaId = null, mountingType = MountingType.OUT_OF_RACK))
}

fun configuratorValidation(project: Project, draft: MapObjectDraft, i18n: Messages): String? =
    draft.errors(project, i18n).values.distinct().joinToString(" · ").takeIf { it.isNotBlank() }
