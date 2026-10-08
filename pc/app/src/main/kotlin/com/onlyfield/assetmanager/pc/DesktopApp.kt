package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.configurator.SecondaryModule
import com.onlyfield.assetmanager.configurator.SymbolIcons
import com.onlyfield.assetmanager.configurator.shown
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.onlyfield.assetmanager.configurator.ProjectDestination
import com.onlyfield.assetmanager.configurator.theme.OnlyFieldTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onlyfield.assetmanager.pc.ui.components.MasterDetailHost
import com.onlyfield.assetmanager.pc.ui.*
import com.onlyfield.assetmanager.pc.ui.components.ConfirmHost
import com.onlyfield.assetmanager.pc.ui.components.EmptyState
import com.onlyfield.assetmanager.pc.ui.dialogs.ProjectDialogs

@Composable
fun DesktopApp(state: DesktopAppState) {
    CompositionLocalProvider(LocalMessages provides state.i18n, com.onlyfield.assetmanager.configurator.LocalPhotoAction provides { type, id ->
        // Several files at once: the desktop counterpart of shooting in series.
        DesktopStorageHelper.pickOpenFiles(state.i18n.text("text.a111cc717443"), state.i18n.text("text.a9f46a362f48"), "png", "jpg", "jpeg", "webp", "bmp")
            .takeIf { it.isNotEmpty() }?.let { state.attachPhotos(it, type, id) }
    }) {
    OnlyFieldTheme(state.darkTheme) {
        com.onlyfield.assetmanager.pc.ui.components.DetailChangeHost(state.detailSlot) {
        ConfirmHost {
            Surface(color = MaterialTheme.colorScheme.background) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val sidebarVisible = state.project != null && maxWidth >= 1200.dp
                    val pagePadding = com.onlyfield.assetmanager.configurator.theme.AppSpacing.page(maxWidth)
                    Column(Modifier.fillMaxSize()) {
                        ProjectToolbar(state)
                        state.error?.let { ErrorBanner(it) { state.error = null } }
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            if (sidebarVisible) ProjectSidebar(state) else if (state.project != null) ProjectRail(state)
                            MasterDetailHost(Modifier.weight(1f).fillMaxHeight().padding(pagePadding)) { SectionContent(state) }
                        }
                        HorizontalDivider()
                        StatusBar(state)
                    }
                }
            }
            ProjectDialogs(state)
        }
        }
        if (state.busy) BusyDialog()
    }
}

}

@Composable
private fun BusyDialog() {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Dialog(onDismissRequest = {}, properties = DialogProperties(
        dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false,
    )) {
        Box(Modifier.fillMaxSize().focusRequester(focus).onPreviewKeyEvent { true }.focusable(), contentAlignment = Alignment.Center) {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .97f), tonalElevation = 8.dp) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(LocalMessages.current.text("work.busy"))
                }
            }
        }
    }
}

internal fun ProjectDestination.appSection(): AppSection? = when (this) {
    ProjectDestination.MAP -> AppSection.FLOORPLANS
    ProjectDestination.DEVICES -> AppSection.INVENTORY
    ProjectDestination.RACKS -> AppSection.RACKS
    ProjectDestination.CABLING -> AppSection.CABLING
    ProjectDestination.NETWORK -> AppSection.NETWORK
    ProjectDestination.POWER -> AppSection.POWER
    ProjectDestination.MODELS -> AppSection.MODELS
    ProjectDestination.ATTACHMENTS -> AppSection.MEDIA
    ProjectDestination.CREDENTIALS -> AppSection.CREDENTIALS
    ProjectDestination.PROJECT -> AppSection.PROJECT
    ProjectDestination.TRASH -> AppSection.TRASH
    ProjectDestination.DOCUMENTS -> null
}

private fun DesktopAppState.navigate(destination: ProjectDestination) {
    val target = destination.appSection()
    if (target != null) section = target else requestChange { dialog = AppDialog.Documents }
}

@Composable
private fun ProjectSidebar(state: DesktopAppState) {
    val i18n = LocalMessages.current
    Surface(Modifier.width(208.dp).fillMaxHeight(), tonalElevation = 1.dp) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val project = state.project
            ProjectDestination.entries.filter { project == null || it.shown(project, state.showSecondary) }.groupBy { it.groupKey }.forEach { (group, entries) ->
                Text(i18n.text(group), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp))
                entries.forEach { destination ->
                    NavigationDrawerItem(label = { Text(destination.title(i18n)) }, selected = destination.appSection() == state.section,
                        onClick = { state.navigate(destination) }, icon = { Icon(destination.icon, null, Modifier.size(20.dp)) },
                        shape = MaterialTheme.shapes.small, modifier = Modifier.heightIn(min = 44.dp))
                }
            }
            // Unused optional modules stay behind one entry.
            val hidden = project?.let { SecondaryModule.hidden(it) }.orEmpty()
            if (hidden.isNotEmpty()) NavigationDrawerItem(
                label = { Column {
                    Text(i18n.text(if (state.showSecondary) "nav.lessModules" else "nav.moreModules"))
                    Text(hidden.joinToString(", ") { i18n.text(it.labelKey) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } },
                selected = false, onClick = { state.showSecondary = !state.showSecondary }, icon = { Icon(SymbolIcons.category, null, Modifier.size(20.dp)) },
                shape = MaterialTheme.shapes.small, modifier = Modifier.heightIn(min = 44.dp))
        }
    }
}

@Composable
private fun ProjectRail(state: DesktopAppState) {
    val i18n = state.i18n
    NavigationRail(Modifier.width(104.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
        ProjectDestination.entries.filter { it.primary }.forEach { destination ->
            NavigationRailItem(selected = destination.appSection() == state.section,
                onClick = { state.navigate(destination) }, icon = { Icon(destination.icon, null) },
                label = { Text(destination.title(i18n), maxLines = 2, style = MaterialTheme.typography.labelSmall) })
        }
        var open by remember { mutableStateOf(false) }
        Box {
            NavigationRailItem(selected = ProjectDestination.entries.none { it.primary && it.appSection() == state.section },
                onClick = { open = true }, icon = { Icon(SymbolIcons.settings, null) },
                label = { Text(i18n.text("ux.nav.projectHome"), style = MaterialTheme.typography.labelSmall) })
            DropdownMenu(open, { open = false }, Modifier.heightIn(max = 560.dp)) {
                ProjectDestination.entries.filter { !it.primary && state.project?.let { p -> it.shown(p, state.showSecondary) } != false }
                    .groupBy { it.groupKey }.forEach { (group, destinations) ->
                        Text(i18n.text(group), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp))
                        destinations.forEach { destination ->
                            DropdownMenuItem(text = { Text(destination.title(i18n)) }, leadingIcon = { Icon(destination.icon, null) },
                                onClick = { open = false; state.navigate(destination) })
                        }
                    }
                DropdownMenuItem(text = { Text(i18n.text(if (state.showSecondary) "nav.lessModules" else "nav.moreModules")) },
                    onClick = { state.showSecondary = !state.showSecondary })
            }
        }
    }
}

@Composable
private fun ProjectToolbar(state: DesktopAppState) {
    val i18n = LocalMessages.current

    val project = state.project
    var tools by remember { mutableStateOf(false) }
    Surface(tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(project?.name ?: i18n.text("text.57ecf1a02db6"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (project != null) Text(i18n.text("text.b1f6bb96d793") + if (project.isPasswordProtected) i18n.text("text.29322f0f7cb0") else "", style = MaterialTheme.typography.bodySmall)
            }
            if (project == null) {
                OutlinedButton(onClick = { state.newProject() }) { Text(i18n.text("text.ac667fe865c9")) }
                OutlinedButton(onClick = state::pickAndImport) { Text(i18n.text("text.a6afc0c52be6")) }
            } else {
                TextButton(onClick = state::undo, enabled = state.canUndo) { Text(i18n.text("action.undo")) }
                Box {
                    OutlinedButton(onClick = { tools = true }) { Text(i18n.text("ux.nav.operations")) }
                    DropdownMenu(expanded = tools, onDismissRequest = { tools = false }) {
                        // Exchange first, then protection, then project lifecycle.
                        DropdownMenuItem(text = { Text(i18n.text("text.8a1d8b27e511")) }, onClick = { tools = false; state.exportPackage() })
                        DropdownMenuItem(text = { Text(i18n.text("text.c890f54eece6")) }, onClick = { tools = false; state.pickAndImport() })
                        DropdownMenuItem(text = { Text(i18n.text("text.f6a32b19c4b1")) }, onClick = { tools = false; state.dialog = AppDialog.ManagePassword })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(i18n.text("text.ac667fe865c9")) }, onClick = { tools = false; state.newProject() })
                        DropdownMenuItem(text = { Text(i18n.text("text.c00df9e3726e")) }, onClick = { tools = false; state.closeProject() })
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    val i18n = LocalMessages.current

    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(i18n.text("text.32d4079b315b"), color = MaterialTheme.colorScheme.onErrorContainer) }
        }
    }
}

@Composable
private fun StatusBar(state: DesktopAppState) {
    val i18n = LocalMessages.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(state.status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (state.project != null) {
            val errors = state.errorCount
            val warnings = state.warningCount
            Text(
                text = when {
                    errors == 0 && warnings == 0 -> i18n.text("text.f040619ad6e5")
                    else -> i18n.text("text.d9cc835db98b", errors, warnings)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (errors > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (errors > 0) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clickable(enabled = errors + warnings > 0) { state.dialog = AppDialog.Validation }
            )
        }
        Text(
            i18n.text("text.2e723f924356", state.dataDir.path.absolutePath),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 420.dp)
        )
    }
}

@Composable
private fun SectionContent(state: DesktopAppState) {
    val project = state.project
    if (project == null || state.section == AppSection.PROJECT) {
        ProjectSection(state)
        return
    }
    val update = state::update
    when (state.section) {
        AppSection.INVENTORY -> InventorySection(project, update, state::addToTrash, state::mergeDevices, saveError = { state.error })
        AppSection.RACKS -> RackSection(project, update, state::addToTrash, saveError = { state.error })
        AppSection.MODELS -> DeviceModelsSection(project, update, saveError = { state.error })
        AppSection.FLOORPLANS -> FloorHomeSection(state)
        AppSection.CREDENTIALS -> CredentialsSection(project, update, saveError = { state.error })
        AppSection.MEDIA -> FloorplanMediaSection(project, update, state::addAttachment, state::attachmentBytes, state::hasAttachment, state::openAttachment, state.hasPassword, state::addMapSnapshot, saveError = { state.error })
        AppSection.CABLING -> CablingSection(project, update)
        AppSection.NETWORK -> NetworkLogicalSection(project, update, state.showSecondary)
        AppSection.POWER -> PowerBadgeSection(project, update, state.showSecondary, { state.error })
        AppSection.TRASH -> TrashBatchSection(project, state.trash, state::restoreTrash) { state.trash = it }
        AppSection.PROJECT -> EmptyState("")
    }
}
