package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.GlyphBadge
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.forms.LogicalGroup
import com.onlyfield.assetmanager.core.forms.LogicalMember
import com.onlyfield.assetmanager.core.forms.LogicalTopologies
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/**
 * Logical view: one box per subnet or VLAN with the devices that live in it. The gateway comes first,
 * devices joining several boxes are outlined; boxes wrap, so only vertical scrolling is needed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogicalView(project: Project, i18n: Messages, siteId: String?, onOpen: (Device) -> Unit, modifier: Modifier = Modifier) {
    val logical = remember(project, siteId) { LogicalTopologies.build(project, siteId) }
    val index = remember(project) { ProjectIndex(project) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (logical.groups.isEmpty()) Text(i18n.text("logical.empty"), style = MaterialTheme.typography.bodyMedium)
        logical.groups.forEach { group ->
            GroupBox(title(group, i18n), listOfNotNull(group.subnet?.name?.takeIf { it.isNotBlank() }, group.subnet?.gatewayIp?.takeIf { it.isNotBlank() }?.let { i18n.text("logical.gateway", it) }).joinToString(" · ")) {
                if (group.members.isEmpty()) Text(i18n.text("logical.noDevices"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                group.members.forEach { DeviceChip(project, it.device, i18n, member = it, onOpen = onOpen) }
            }
        }
        if (logical.unassigned.isNotEmpty()) GroupBox(i18n.text("logical.unassigned"), i18n.text("logical.unassignedHint")) {
            logical.unassigned.forEach { DeviceChip(project, it, i18n, member = null, onOpen = onOpen) }
        }
        if (logical.connections.isNotEmpty()) {
            Text(i18n.text("text.04bc126d165e"), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall)
            logical.connections.forEach { c ->
                val local = c.localEndpointDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: c.localEndpointSiteDescription ?: "?"
                val remote = c.remoteEndpointDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: c.remoteEndpointSiteDescription ?: c.providerOrCarrier ?: "?"
                Column {
                    Text("${c.type.toDisplayString(i18n = i18n)} · ${c.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(listOfNotNull("$local → $remote", c.providerOrCarrier, c.bandwidth).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun title(group: LogicalGroup, i18n: Messages): String = listOfNotNull(
    group.subnet?.cidrBlock,
    group.vlanNumber?.let { n -> group.vlanName?.let { i18n.text("text.15fd0dfb7614", n, it) } ?: i18n.text("text.da4da5c165af", n) },
).joinToString(" · ")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupBox(title: String, detail: String, content: @Composable FlowRowScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun DeviceChip(project: Project, device: Device, i18n: Messages, member: LogicalMember?, onOpen: (Device) -> Unit) {
    val glyph = ObjectGlyph.of(project, device)
    val tags = listOfNotNull(i18n.text("logical.gatewayTag").takeIf { member?.gateway == true }, i18n.text("logical.routes").takeIf { (member?.groups ?: 0) > 1 })
    val second = (listOfNotNull(member?.address) + tags).joinToString(" · ")
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MapStyle.glyph(glyph),
        contentColor = Color.White,
        // Joins several boxes: outlined, so the routing points stand out without relying on colour.
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface).takeIf { (member?.groups ?: 0) > 1 },
        modifier = Modifier.widthIn(min = 120.dp, max = 200.dp).heightIn(min = 48.dp).clickable(role = Role.Button) { onOpen(device) }
            .semantics(mergeDescendants = true) { contentDescription = (listOfNotNull(device.technicalName, member?.address) + tags).joinToString(", ") },
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlyphBadge(glyph, 22.dp)
            Column {
                Text(device.technicalName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (second.isNotBlank()) Text(second, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
