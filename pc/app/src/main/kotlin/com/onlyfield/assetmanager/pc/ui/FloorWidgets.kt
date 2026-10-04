package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.pc.LocalMessages

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
import com.onlyfield.assetmanager.pc.ui.components.*

@Composable
internal fun ObjectFields(project: Project, draft: MapObjectDraft, initialSection: com.onlyfield.assetmanager.configurator.ConfiguratorPage = com.onlyfield.assetmanager.configurator.ConfiguratorPage.ESSENTIALS, change: (MapObjectDraft) -> Unit) {
    val markDirty = LocalMarkDirty.current
    com.onlyfield.assetmanager.configurator.ObjectConfigurator(project, draft, LocalMessages.current, initialSection = initialSection) { markDirty(); change(it) }
}
