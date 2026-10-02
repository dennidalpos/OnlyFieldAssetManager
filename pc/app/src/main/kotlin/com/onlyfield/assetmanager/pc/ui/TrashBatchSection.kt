package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashBatchSection(
    project: Project,
    trashItems: List<TrashItem>,
    onProjectUpdated: (Project, String) -> Unit,
    onTrashUpdated: (List<TrashItem>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cestino Locale & Ripristino (${trashItems.size} elementi)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (trashItems.isNotEmpty()) {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onTrashUpdated(emptyList())
                        onProjectUpdated(project, "Cestino svuotato definitivamente.")
                    }
                ) {
                    Text("Svuota Cestino")
                }
            }
        }

        if (trashItems.isEmpty()) {
            EmptyStateCard(
                message = "Il cestino locale è vuoto. Gli elementi eliminati durante la sessione compariranno qui."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "[${item.itemType}] ${item.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                item.affectedReferencesSummary?.let { summary ->
                                    Text("Note impatto: $summary", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val updatedProj = com.onlyfield.assetmanager.pc.DesktopDomainLogic.restoreFromTrash(project, item)
                                    onTrashUpdated(trashItems.filterNot { it.id == item.id })
                                    onProjectUpdated(updatedProj, "Ripristinato '${item.displayName}' dal cestino.")
                                }) {
                                    Text("Ripristina", fontSize = 11.sp)
                                }

                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        onTrashUpdated(trashItems.filterNot { it.id == item.id })
                                    }
                                ) {
                                    Text("Elimina Definitivamente", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
