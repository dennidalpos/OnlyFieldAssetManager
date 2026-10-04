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
internal fun ObjectFields(project: Project, draft: MapObjectDraft, initialSection: com.onlyfield.assetmanager.configurator.ConfiguratorPage = com.onlyfield.assetmanager.configurator.ConfiguratorPage.ESSENTIALS, change: (MapObjectDraft) -> Unit) {
    val markDirty = LocalMarkDirty.current
    com.onlyfield.assetmanager.configurator.ObjectConfigurator(project, draft, LocalMessages.current, initialSection = initialSection) { markDirty(); change(it) }
}

@Composable
internal fun ObjectCatalogDialog(project: Project, onClose: () -> Unit, onSelect: (ObjectType) -> Unit, onCustom: (ObjectType) -> Unit, allowCables: Boolean = true) {
    val i18n = LocalMessages.current

    var query by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DeviceCategory.CUSTOM) }
    var container by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, title = { Text(i18n.text("text.8502424a65de")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(query, { query = it }, i18n.text("text.271a55491c4f"))
            if (custom) {
                FormField(name, { name = it }, i18n.text("text.12db8de268a5"))
                EnumPicker(i18n.text("text.430f47238ebc"), DeviceCategory.entries, category, { it.toDisplayString(i18n = i18n) }, { category = it })
                Row { Checkbox(container, { container = it }); Text(i18n.text("text.686247e8bde9")) }
                Button(enabled = name.isNotBlank(), onClick = { onCustom(ObjectType(name = name.trim(), category = category, canContainObjects = container)) }) { Text(i18n.text("text.62a5786b6d5a")) }
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(ObjectCatalog.types(project).filter { ObjectCatalog.displayName(it, i18n).contains(query, true) && (allowCables || it.kind != ObjectKind.CABLE) }.sortedForDisplay(i18n) { ObjectCatalog.displayName(it, i18n) }, key = { it.id }) { type ->
                        TextButton(onClick = { onSelect(type) }, modifier = Modifier.fillMaxWidth()) { Text(ObjectCatalog.displayName(type, i18n)) }
                    }
                }
                OutlinedButton(onClick = { custom = true }) { Text(i18n.text("text.7a83d7ae0c15")) }
            }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text(i18n.text("text.32d4079b315b")) } })
}
