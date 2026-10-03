package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.EntityTypeLabels
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.TrashItem
import com.onlyfield.assetmanager.pc.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrashBatchSection(
    project: Project,
    trashItems: List<TrashItem>,
    onRestore: (TrashItem) -> Unit,
    onTrashUpdated: (List<TrashItem>) -> Unit
) {
    val i18n = LocalMessages.current

    val confirm = LocalConfirm.current
    val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            i18n.text("text.9a3a36d5fa15"),
            subtitle = i18n.text("text.a5ff244c16a3")
        ) {
            if (trashItems.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        confirm(
                            ConfirmRequest(
                                title = i18n.text("text.5d7057a189e9"),
                                message = i18n.text("text.11635221973f", trashItems.size),
                                confirmLabel = i18n.text("text.1c01eff38735")
                            ) { onTrashUpdated(emptyList()) }
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(i18n.text("text.1c01eff38735")) }
            }
        }

        if (trashItems.isEmpty()) {
            EmptyState(i18n.text("text.9744f9e3a70f"))
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(trashItems.sortedByDescending { it.deletedEpochMs }, key = { it.id }) { item ->
                ItemCard(
                    title = item.displayName,
                    badge = EntityTypeLabels.of(item.itemType, i18n = i18n),
                    details = listOf(i18n.text("text.7bf96f41098a", timeFormat.format(Date(item.deletedEpochMs))), item.affectedReferencesSummary.orEmpty())
                ) {
                    Button(onClick = {
                        onRestore(item)
                    }) { Text(i18n.text("text.cf1718087073")) }
                    DeleteButton(item.displayName, label = i18n.text("text.8a62e526a037"),
                        message = i18n.text("text.e0be6b7281c4"),
                        onDelete = { onTrashUpdated(trashItems.filterNot { it.id == item.id }) })
                }
            }
        }
    }
}
