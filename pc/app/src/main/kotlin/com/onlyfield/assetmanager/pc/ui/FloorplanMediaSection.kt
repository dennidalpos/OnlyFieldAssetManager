package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.DesktopCartographyManager
import com.onlyfield.assetmanager.pc.DesktopMapSource
import com.onlyfield.assetmanager.pc.DesktopStorageHelper
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorplanMediaSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    val allAreas = remember(project) {
        project.businessUnits.flatMap { bu ->
            bu.areas + bu.sites.flatMap { it.areas }
        }.distinctBy { it.id }
    }

    var showAddAttachmentDialog by remember { mutableStateOf(false) }
    var showAddPlacementDialog by remember { mutableStateOf(false) }
    var selectedAreaForCanvasId by remember { mutableStateOf(allAreas.firstOrNull()?.id) }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("📁 Allegati & Media (${project.attachments.size})", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("🗺️ Planimetrie & Canvas (${project.floorplanPlacements.size})", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("🌐 Cartografia & Mappe Raster", modifier = Modifier.padding(12.dp))
            }
        }

        when (selectedTab) {
            0 -> AttachmentsTabContent(
                project = project,
                allAreas = allAreas,
                onAddAttachmentClick = { showAddAttachmentDialog = true },
                onProjectUpdated = onProjectUpdated
            )
            1 -> FloorplansTabContent(
                project = project,
                allAreas = allAreas,
                selectedAreaId = selectedAreaForCanvasId,
                onAreaSelected = { selectedAreaForCanvasId = it },
                onAddPlacementClick = { showAddPlacementDialog = true },
                onProjectUpdated = onProjectUpdated
            )
            2 -> CartographyTabContent()
        }
    }

    // Add Attachment Dialog
    if (showAddAttachmentDialog) {
        var nameInput by remember { mutableStateOf("") }
        var classificationInput by remember { mutableStateOf(AttachmentClassification.SHAREABLE) }
        var selectedFile by remember { mutableStateOf<File?>(null) }

        AlertDialog(
            onDismissRequest = { showAddAttachmentDialog = false },
            title = { Text("Aggiungi Allegato Media") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = {
                        val chosen = DesktopStorageHelper.pickOpenFile("Seleziona File Media (Immagine / PDF)")
                        if (chosen != null) {
                            selectedFile = chosen
                            if (nameInput.isBlank()) nameInput = chosen.nameWithoutExtension
                        }
                    }) {
                        Text(selectedFile?.let { "File: ${it.name}" } ?: "Seleziona File da Disco...")
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nome Allegato *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    var classExp by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { classExp = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Classificazione: ${classificationInput.name}")
                        }
                        DropdownMenu(expanded = classExp, onDismissRequest = { classExp = false }) {
                            AttachmentClassification.entries.forEach { cl ->
                                DropdownMenuItem(text = { Text(cl.name) }, onClick = { classificationInput = cl; classExp = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = nameInput.isNotBlank() && selectedFile != null,
                    onClick = {
                        val file = selectedFile ?: return@Button
                        val attachment = Attachment(
                            id = UUID.randomUUID().toString(),
                            name = nameInput.trim(),
                            originalFileName = file.name,
                            fileType = if (file.extension.equals("pdf", ignoreCase = true)) AttachmentType.PDF else AttachmentType.IMAGE,
                            relativePath = "media/${file.name}",
                            classification = classificationInput
                        )

                        val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.addAttachment(project, attachment)
                        showAddAttachmentDialog = false
                        onProjectUpdated(updated, "Allegato '${attachment.name}' aggiunto al progetto.")
                    }
                ) {
                    Text("Aggiungi Allegato")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAttachmentDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Add Placement Dialog
    if (showAddPlacementDialog) {
        val selectedArea = allAreas.find { it.id == selectedAreaForCanvasId } ?: allAreas.firstOrNull()
        if (selectedArea != null) {
            var targetType by remember { mutableStateOf(PlacementTargetType.RACK) }
            val racks = project.racks
            val devices = project.businessUnits.flatMap { it.devices }
            var selectedTargetId by remember { mutableStateOf(if (targetType == PlacementTargetType.RACK) racks.firstOrNull()?.id ?: "" else devices.firstOrNull()?.id ?: "") }
            var xText by remember { mutableStateOf("0.5") }
            var yText by remember { mutableStateOf("0.5") }

            AlertDialog(
                onDismissRequest = { showAddPlacementDialog = false },
                title = { Text("Nuovo Posizionamento per '${selectedArea.name}'") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = targetType == PlacementTargetType.RACK, onClick = { targetType = PlacementTargetType.RACK; selectedTargetId = racks.firstOrNull()?.id ?: "" })
                            Text("Armadio Rack")
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(selected = targetType == PlacementTargetType.DEVICE, onClick = { targetType = PlacementTargetType.DEVICE; selectedTargetId = devices.firstOrNull()?.id ?: "" })
                            Text("Apparato Singolo")
                        }

                        var targetExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { targetExp = true }, modifier = Modifier.fillMaxWidth()) {
                                val label = if (targetType == PlacementTargetType.RACK)
                                    racks.find { it.id == selectedTargetId }?.name ?: "Seleziona Rack"
                                else
                                    devices.find { it.id == selectedTargetId }?.technicalName ?: "Seleziona Apparato"
                                Text(label)
                            }
                            DropdownMenu(expanded = targetExp, onDismissRequest = { targetExp = false }) {
                                if (targetType == PlacementTargetType.RACK) {
                                    racks.forEach { r ->
                                        DropdownMenuItem(text = { Text(r.name) }, onClick = { selectedTargetId = r.id; targetExp = false })
                                    }
                                } else {
                                    devices.forEach { d ->
                                        DropdownMenuItem(text = { Text(d.technicalName) }, onClick = { selectedTargetId = d.id; targetExp = false })
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = xText, onValueChange = { xText = it }, label = { Text("Coordinata X Ratio (0.0-1.0)") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = yText, onValueChange = { yText = it }, label = { Text("Coordinata Y Ratio (0.0-1.0)") }, modifier = Modifier.weight(1f))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = selectedTargetId.isNotBlank(),
                        onClick = {
                            val x = xText.toFloatOrNull() ?: 0.5f
                            val y = yText.toFloatOrNull() ?: 0.5f

                            val placement = FloorplanPlacement(
                                id = UUID.randomUUID().toString(),
                                areaId = selectedArea.id,
                                targetType = targetType,
                                targetId = selectedTargetId,
                                xRatio = x,
                                yRatio = y
                            )

                            val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.addFloorplanPlacement(project, placement)
                            showAddPlacementDialog = false
                            onProjectUpdated(updated, "Posizionamento salvato su planimetria.")
                        }
                    ) {
                        Text("Salva Posizionamento")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAddPlacementDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentsTabContent(
    project: Project,
    allAreas: List<Area>,
    onAddAttachmentClick: () -> Unit,
    onProjectUpdated: (Project, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestione Allegati Progetto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAddAttachmentClick) {
                Text("+ Aggiungi Allegato")
            }
        }

        if (project.attachments.isEmpty()) {
            EmptyStateCard(
                message = "Nessun allegato o media caricato nel progetto.",
                actionLabel = "+ Aggiungi Allegato",
                onAction = onAddAttachmentClick
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.attachments) { att ->
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
                                Text(att.name, fontWeight = FontWeight.Bold)
                                Text("File originale: ${att.originalFileName} | Tipo: ${att.fileType.name}")
                                Text("Percorso relativo: ${att.relativePath}")

                                Surface(
                                    color = when (att.classification) {
                                        AttachmentClassification.SHAREABLE -> MaterialTheme.colorScheme.secondaryContainer
                                        AttachmentClassification.CONFIDENTIAL -> MaterialTheme.colorScheme.errorContainer
                                        AttachmentClassification.REVIEW_REQUIRED -> MaterialTheme.colorScheme.tertiaryContainer
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Classificazione: ${att.classification.name}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                var areaExp by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(onClick = { areaExp = true }) {
                                        Text("Imposta come Planimetria", fontSize = 11.sp)
                                    }
                                    DropdownMenu(expanded = areaExp, onDismissRequest = { areaExp = false }) {
                                        allAreas.forEach { area ->
                                            DropdownMenuItem(
                                                text = { Text("Area: ${area.name}") },
                                                onClick = {
                                                    areaExp = false
                                                    val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.setAreaFloorplan(project, area.id, att.id)
                                                    onProjectUpdated(updated, "Allegato '${att.name}' impostato come planimetria per '${area.name}'.")
                                                }
                                            )
                                        }
                                    }
                                }

                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.deleteAttachment(project, att.id)
                                        onProjectUpdated(updated, "Allegato '${att.name}' rimosso.")
                                    }
                                ) {
                                    Text("Elimina", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloorplansTabContent(
    project: Project,
    allAreas: List<Area>,
    selectedAreaId: String?,
    onAreaSelected: (String) -> Unit,
    onAddPlacementClick: () -> Unit,
    onProjectUpdated: (Project, String) -> Unit
) {
    val selectedArea = allAreas.find { it.id == selectedAreaId } ?: allAreas.firstOrNull()
    val floorplanAttachment = project.attachments.find { it.id == selectedArea?.floorplanAttachmentId }
    val placementsInArea = remember(project, selectedArea) {
        if (selectedArea == null) emptyList()
        else project.floorplanPlacements.filter { it.areaId == selectedArea.id }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Seleziona Area:", fontWeight = FontWeight.Bold)
            var areaDropdownExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { areaDropdownExpanded = true }) {
                    Text(selectedArea?.name ?: "Nessuna Area")
                }
                DropdownMenu(expanded = areaDropdownExpanded, onDismissRequest = { areaDropdownExpanded = false }) {
                    allAreas.forEach { area ->
                        DropdownMenuItem(text = { Text(area.name) }, onClick = { onAreaSelected(area.id); areaDropdownExpanded = false })
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                enabled = selectedArea != null,
                onClick = onAddPlacementClick
            ) {
                Text("+ Nuovo Posizionamento Canvas")
            }
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Card(
                    modifier = Modifier.weight(2f).fillMaxHeight().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Canvas Planimetria — ${selectedArea?.name ?: "N/D"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (floorplanAttachment != null) "Planimetria: ${floorplanAttachment.name}" else "Nessuna immagine associata all'area",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }

                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                val gridColor = Color.White.copy(alpha = 0.1f)
                                for (x in 0..10) {
                                    drawLine(gridColor, start = Offset(x * canvasWidth / 10f, 0f), end = Offset(x * canvasWidth / 10f, canvasHeight))
                                }
                                for (y in 0..10) {
                                    drawLine(gridColor, start = Offset(0f, y * canvasHeight / 10f), end = Offset(canvasWidth, y * canvasHeight / 10f))
                                }

                                placementsInArea.forEach { placement ->
                                    val px = placement.xRatio * canvasWidth
                                    val py = placement.yRatio * canvasHeight
                                    val color = if (placement.targetType == PlacementTargetType.RACK) Color(0xFF1976D2) else Color(0xFF388E3C)

                                    drawCircle(color, radius = 16f, center = Offset(px, py))
                                    drawCircle(Color.White, radius = 18f, center = Offset(px, py), style = Stroke(width = 2f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text("Elementi Posizionati (${placementsInArea.size})", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (placementsInArea.isEmpty()) {
                        Text("Nessun posizionamento registrato su questa planimetria.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(placementsInArea) { place ->
                                val targetName = when (place.targetType) {
                                    PlacementTargetType.RACK -> project.racks.find { it.id == place.targetId }?.name ?: place.targetId
                                    PlacementTargetType.DEVICE -> project.businessUnits.flatMap { it.devices }.find { it.id == place.targetId }?.technicalName ?: place.targetId
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("[${place.targetType.name}] $targetName", fontWeight = FontWeight.Bold)
                                            Text("Coordinate: X=${(place.xRatio * 100).toInt()}%, Y=${(place.yRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Button(
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            onClick = {
                                                val updated = com.onlyfield.assetmanager.pc.DesktopDomainLogic.deleteFloorplanPlacement(project, place.id)
                                                onProjectUpdated(updated, "Posizionamento per '$targetName' rimosso.")
                                            }
                                        ) {
                                            Text("Rimuovi", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartographyTabContent() {
    var selectedMapSource by remember { mutableStateOf(DesktopMapSource.OPEN_TOPO_MAP) }
    var centerLatInput by remember { mutableStateOf("41.9028") }
    var centerLonInput by remember { mutableStateOf("12.4964") }
    var zoomInput by remember { mutableStateOf("15") }

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sorgente Mappa:", fontWeight = FontWeight.Bold)

                var mapExp by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { mapExp = true }) {
                        Text(selectedMapSource.displayName)
                    }
                    DropdownMenu(expanded = mapExp, onDismissRequest = { mapExp = false }) {
                        DesktopMapSource.entries.forEach { src ->
                            DropdownMenuItem(
                                text = { Text(src.displayName) },
                                onClick = { selectedMapSource = src; mapExp = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = centerLatInput,
                    onValueChange = { centerLatInput = it },
                    label = { Text("Latitudine") },
                    modifier = Modifier.width(130.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = centerLonInput,
                    onValueChange = { centerLonInput = it },
                    label = { Text("Longitudine") },
                    modifier = Modifier.width(130.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = zoomInput,
                    onValueChange = { zoomInput = it },
                    label = { Text("Zoom (0-19)") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true
                )
            }

            val latVal = centerLatInput.toDoubleOrNull() ?: 41.9028
            val lonVal = centerLonInput.toDoubleOrNull() ?: 12.4964
            val zoomVal = zoomInput.toIntOrNull() ?: 15
            val tileCoord = DesktopCartographyManager.lonLatToTileCoord(lonVal, latVal, zoomVal)

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val gridCol = Color.White.copy(alpha = 0.15f)
                        for (x in 0..4) {
                            drawLine(gridCol, start = Offset(x * w / 4f, 0f), end = Offset(x * w / 4f, h))
                        }
                        for (y in 0..4) {
                            drawLine(gridCol, start = Offset(0f, y * h / 4f), end = Offset(w, y * h / 4f))
                        }

                        drawCircle(Color(0xFFE11D48), radius = 18f, center = Offset(w / 2f, h / 2f))
                        drawCircle(Color.White, radius = 20f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 3f))
                    }

                    Column(
                        modifier = Modifier.align(Alignment.TopStart).background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(8.dp)
                    ) {
                        Text("📍 Coordinate Centro: Lat $latVal, Lon $lonVal", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("🌐 Tile Coord: Z${tileCoord.zoom} / X${tileCoord.x} / Y${tileCoord.y}", color = Color.LightGray, fontSize = 11.sp)
                        Text("URL Tile: ${DesktopCartographyManager.getTileUrl(selectedMapSource, tileCoord)}", color = Color.Cyan, fontSize = 10.sp)
                    }

                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(topStart = 6.dp)
                    ) {
                        Text(
                            text = selectedMapSource.attribution,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
