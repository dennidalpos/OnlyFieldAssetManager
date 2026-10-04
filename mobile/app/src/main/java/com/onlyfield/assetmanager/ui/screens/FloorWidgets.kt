package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.ui.components.*

@Composable
internal fun ObjectFields(project: Project, draft: MapObjectDraft, initialSection: com.onlyfield.assetmanager.configurator.ConfiguratorPage = com.onlyfield.assetmanager.configurator.ConfiguratorPage.ESSENTIALS,
                          extraSections: @Composable () -> Unit = {}, change: (MapObjectDraft) -> Unit) {
    val markDirty = LocalMarkDirty.current
    // A prefilled new object (quick insertion, "Add and edit") is unsaved work: closing asks first.
    LaunchedEffect(draft.id) { if (!com.onlyfield.assetmanager.configurator.configuratorExists(project, draft)) markDirty() }
    com.onlyfield.assetmanager.configurator.ObjectConfigurator(project, draft, LocalMessages.current, initialSection = initialSection, extraSections = extraSections) { markDirty(); change(it) }
}
