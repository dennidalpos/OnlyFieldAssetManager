package com.onlyfield.assetmanager.pc.ui

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
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrashBatchSection(
    project: Project,
    trashItems: List<TrashItem>,
    onProjectUpdated: (Project, String) -> Unit,
    onTrashUpdated: (List<TrashItem>) -> Unit
) {
    val confirm = LocalConfirm.current
    val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            "Cestino",
            subtitle = if (project.isPasswordProtected) "Apparati e rack eliminati. Per i progetti protetti il cestino si svuota alla chiusura; ogni altra modifica si annulla con Ctrl+Z."
            else "Apparati e rack eliminati. Ogni altra modifica, comprese le eliminazioni, si annulla con Ctrl+Z."
        ) {
            if (trashItems.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        confirm(
                            ConfirmRequest(
                                title = "Svuotare il cestino?",
                                message = "${trashItems.size} elementi non potranno più essere ripristinati.",
                                confirmLabel = "Svuota cestino"
                            ) { onTrashUpdated(emptyList()) }
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Svuota cestino") }
            }
        }

        if (trashItems.isEmpty()) {
            EmptyState("Il cestino è vuoto.")
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(trashItems.sortedByDescending { it.deletedEpochMs }, key = { it.id }) { item ->
                ItemCard(
                    title = item.displayName,
                    badge = EntityTypeLabels.of(item.itemType),
                    details = listOf("Eliminato alle ${timeFormat.format(Date(item.deletedEpochMs))}", item.affectedReferencesSummary.orEmpty())
                ) {
                    Button(onClick = {
                        onTrashUpdated(trashItems.filterNot { it.id == item.id })
                        onProjectUpdated(ProjectEdits.restoreFromTrash(project, item), "«${item.displayName}» ripristinato.")
                    }) { Text("Ripristina") }
                    DeleteButton(item.displayName, label = "Elimina definitivamente",
                        message = "L'elemento non potrà più essere ripristinato.",
                        onDelete = { onTrashUpdated(trashItems.filterNot { it.id == item.id }) })
                }
            }
        }
    }
}
