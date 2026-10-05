package com.onlyfield.assetmanager.configurator.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.display.GlobalSearch
import com.onlyfield.assetmanager.core.display.SearchHit
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Project

/** Project-wide search; the host opens the picked hit on its floor map (or in the editor without a floor). */
@Composable
fun GlobalSearchDialog(project: Project, i18n: Messages, recentIds: List<String>, onPick: (SearchHit) -> Unit, onClose: () -> Unit) {
    val search = remember(project, i18n.locale) { GlobalSearch(project, i18n) }
    var query by remember { mutableStateOf("") }
    val hits = remember(search, query) { search.search(query) }
    val recent = remember(search, recentIds) { search.recent(recentIds) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(onDismissRequest = onClose, title = { Text(i18n.text("search.title")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text(i18n.text("search.placeholder")) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus))
            val shown = if (query.isBlank()) recent else hits
            when {
                query.isBlank() && recent.isEmpty() -> Text(i18n.text("search.hint"), style = MaterialTheme.typography.bodySmall)
                query.isBlank() -> Text(i18n.text("search.recent"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                hits.isEmpty() -> Text(i18n.text("ux.noResults"), style = MaterialTheme.typography.bodySmall)
            }
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(shown, key = { it.kind.name + it.id }) { hit ->
                    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(hit) }.padding(vertical = 8.dp)) {
                        Text(hit.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf(i18n.text("search.kind.${hit.kind.name}"), hit.place).joinToString(" · ") + (hit.matched.takeIf { it.isNotEmpty() && !it.endsWith(hit.title) }?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text(i18n.text("ux.close")) } })
}

/** Most recent first, without duplicates, at most [max]. */
fun withRecent(recent: List<String>, id: String, max: Int = 8): List<String> = (listOf(id) + recent.filterNot { it == id }).take(max)

/** Editor draft for a hit without a floor (the map cannot show it). */
fun searchEditDraft(project: Project, hit: SearchHit): com.onlyfield.assetmanager.core.forms.MapObjectDraft =
    if (hit.focus.type == com.onlyfield.assetmanager.core.model.PlacementTargetType.RACK)
        com.onlyfield.assetmanager.core.forms.MapObjectDraft.forRack(project, project.racks.find { it.id == hit.focus.id })
    else com.onlyfield.assetmanager.core.forms.MapObjectDraft.forDevice(project, project.sites.flatMap { it.devices }.find { it.id == hit.focus.id })
