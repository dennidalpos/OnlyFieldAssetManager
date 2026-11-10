package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.theme.ContentDialog
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.i18n.Messages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryTabs(racks: Boolean, i18n: Messages, onSelect: (Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // The desktop master stays composed at zero width while its editor fills the window.
        if (maxWidth >= 96.dp) {
            SecondaryTabRow(selectedTabIndex = if (racks) 1 else 0) {
                listOf(ProjectDestination.DEVICES, ProjectDestination.RACKS).forEachIndexed { index, destination ->
                    Tab(selected = racks == (index == 1), onClick = { onSelect(index == 1) },
                        text = { Text(destination.title(i18n), maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
            }
        }
    }
}

data class ActiveFilter(val label: String, val remove: () -> Unit)

@Composable
fun FilterAction(count: Int, i18n: Messages, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(i18n.text("ux.filters") + if (count > 0) " ($count)" else "")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFilters(filters: List<ActiveFilter>, i18n: Messages, clear: () -> Unit) {
    if (filters.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEach { filter ->
            InputChip(selected = true, onClick = filter.remove, modifier = Modifier.heightIn(min = 48.dp),
                label = { Text(filter.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = { Icon(SymbolIcons.close, i18n.text("ux.removeFilter", filter.label), Modifier.size(18.dp)) })
        }
        TextButton(onClick = clear) { Text(i18n.text("text.c4483e052140")) }
    }
}

/** Filters apply immediately; closing the panel keeps their visible chips. */
@Composable
fun FiltersDialog(i18n: Messages, onClose: () -> Unit, clear: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ContentDialog(onDismissRequest = onClose,
        title = { Text(i18n.text("ux.filters"), style = MaterialTheme.typography.titleLarge) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) },
        confirmButton = { TextButton(onClick = onClose) { Text(i18n.text("text.32d4079b315b")) } },
        dismissButton = { TextButton(onClick = clear) { Text(i18n.text("text.c4483e052140")) } })
}
