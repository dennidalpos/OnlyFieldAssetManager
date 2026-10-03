package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.DesktopCartographyManager
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.DesktopMapSource
import com.onlyfield.assetmanager.pc.DesktopStorageHelper
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.FieldValidators
import java.io.File

@Composable
fun FloorplanMediaSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit,
    onAddAttachment: (File, String, AttachmentClassification) -> Unit,
    attachmentFile: (Attachment) -> File?,
) {
    val index = remember(project) { ProjectIndex(project) }
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubTabs(listOf("Planimetrie (${project.floorplanPlacements.size})", "Allegati (${project.attachments.size})", "Coordinate geografiche"), tab) { tab = it }
        when (tab) {
            0 -> FloorplanTab(project, index, onProjectUpdated, attachmentFile)
            1 -> AttachmentsTab(project, index, onProjectUpdated, onAddAttachment, attachmentFile)
            2 -> CartographyTab()
        }
    }
}

@Composable
private fun FloorplanTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit, attachmentFile: (Attachment) -> File?) {
    var areaId by remember { mutableStateOf(index.areas.firstOrNull()?.id) }
    val area = index.area(areaId)
    var targetType by remember { mutableStateOf(PlacementTargetType.RACK) }
    var targetId by remember { mutableStateOf<String?>(null) }
    var movingId by remember { mutableStateOf<String?>(null) }

    if (index.areas.isEmpty()) {
        EmptyState("Crea prima un'area nella sezione Progetto › Struttura.")
        return
    }
    val placements = project.floorplanPlacements.filter { it.areaId == area?.id }
    val floorplan = project.attachments.find { it.id == area?.floorplanAttachmentId }
    val background = remember(floorplan?.id) {
        floorplan?.takeIf { it.fileType == AttachmentType.IMAGE }?.let(attachmentFile)?.let { f ->
            try { org.jetbrains.skia.Image.makeFromEncoded(f.readBytes()).toComposeImageBitmap() } catch (_: Exception) { null }
        }
    }

    fun targetName(p: FloorplanPlacement) = p.labelOverride ?: when (p.targetType) {
        PlacementTargetType.RACK -> index.rackName(p.targetId, "Rack mancante")
        PlacementTargetType.DEVICE -> index.deviceName(p.targetId, "Apparato mancante")
    }

    val placing = targetId != null || movingId != null
    val hint = when {
        movingId != null -> "Clicca sulla planimetria per spostare «${placements.find { it.id == movingId }?.let(::targetName)}»."
        targetId != null -> "Clicca sulla planimetria nel punto in cui si trova l'elemento."
        else -> "Scegli un rack o un apparato a destra, poi clicca sulla planimetria per posizionarlo."
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OptionPicker("Area", index.areas, area, { it.name }, { areaId = it?.id; movingId = null }, Modifier.width(280.dp))
                Text(
                    floorplan?.let { "Planimetria: ${it.name}" } ?: "Nessuna planimetria associata (impostala da Allegati)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(hint, color = if (placing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (placing) FontWeight.SemiBold else FontWeight.Normal)
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .border(if (placing) 2.dp else 1.dp, if (placing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .background(Color(0xFF263238), RoundedCornerShape(8.dp))
            ) {
                background?.let {
                    Image(it, contentDescription = "Planimetria", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize().padding(8.dp))
                }
                Canvas(
                    modifier = Modifier.fillMaxSize().padding(8.dp).pointerInput(area?.id, targetId, targetType, movingId, project) {
                        detectTapGestures { offset ->
                            val x = (offset.x / size.width).coerceIn(0f, 1f)
                            val y = (offset.y / size.height).coerceIn(0f, 1f)
                            val currentArea = area ?: return@detectTapGestures
                            val moving = placements.find { it.id == movingId }
                            if (moving != null) {
                                movingId = null
                                onProjectUpdated(ProjectEdits.updateFloorplanPlacement(project, moving.copy(xRatio = x, yRatio = y)), "Posizione aggiornata.")
                            } else targetId?.let { id ->
                                targetId = null
                                val placement = FloorplanPlacement(areaId = currentArea.id, targetType = targetType, targetId = id, xRatio = x, yRatio = y)
                                onProjectUpdated(ProjectEdits.addFloorplanPlacement(project, placement), "Elemento posizionato su ${currentArea.name}.")
                            }
                        }
                    }
                ) {
                    val grid = Color.White.copy(alpha = if (background != null) 0f else 0.08f)
                    for (i in 0..10) {
                        drawLine(grid, Offset(i * size.width / 10f, 0f), Offset(i * size.width / 10f, size.height))
                        drawLine(grid, Offset(0f, i * size.height / 10f), Offset(size.width, i * size.height / 10f))
                    }
                    placements.forEach { p ->
                        val c = Offset(p.xRatio * size.width, p.yRatio * size.height)
                        val color = if (p.targetType == PlacementTargetType.RACK) Color(0xFF42A5F5) else Color(0xFF66BB6A)
                        drawCircle(color, 12f, c)
                        drawCircle(if (p.id == movingId) Color.Yellow else Color.White, 14f, c, style = Stroke(2f))
                    }
                }
                placements.forEach { p ->
                    // Labels are composables so they stay readable; positioned with the same ratios as the dots.
                    BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp)) {
                        Text(
                            targetName(p),
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.offset(x = maxWidth * p.xRatio + 10.dp, y = maxHeight * p.yRatio - 8.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Posiziona elemento", fontWeight = FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow {
                PlacementTargetType.entries.forEachIndexed { i, t ->
                    SegmentedButton(selected = targetType == t, onClick = { targetType = t; targetId = null }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                        Text(t.toDisplayString())
                    }
                }
            }
            if (targetType == PlacementTargetType.RACK) {
                OptionPicker("Rack", project.racks, index.rack(targetId), { it.name }, { targetId = it?.id; movingId = null })
            } else {
                DevicePicker("Apparato", index, targetId, { targetId = it; movingId = null })
            }
            if (placing) TextButton(onClick = { targetId = null; movingId = null }) { Text("Annulla posizionamento") }

            HorizontalDivider()
            Text("Elementi su ${area?.name ?: "—"} (${placements.size})", fontWeight = FontWeight.SemiBold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(placements, key = { it.id }) { p ->
                    ItemCard(title = targetName(p), badge = p.targetType.toDisplayString(), details = emptyList()) {
                        TextButton(onClick = { movingId = p.id; targetId = null }) { Text("Sposta") }
                        DeleteButton(targetName(p), label = "Rimuovi", message = "L'elemento viene tolto solo dalla planimetria.", onDelete = {
                            onProjectUpdated(ProjectEdits.deleteFloorplanPlacement(project, p.id), "Elemento rimosso dalla planimetria.")
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentsTab(
    project: Project,
    index: ProjectIndex,
    onProjectUpdated: (Project, String) -> Unit,
    onAddAttachment: (File, String, AttachmentClassification) -> Unit,
    attachmentFile: (Attachment) -> File?,
) {
    var adding by remember { mutableStateOf(false) }
    var floorplanFor by remember { mutableStateOf<Attachment?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Allegati", subtitle = "Foto, planimetrie e documenti del progetto") {
            Button(onClick = { adding = true }) { Text("+ Aggiungi allegato") }
        }
        if (project.attachments.isEmpty()) EmptyState("Nessun allegato.", actionLabel = "+ Aggiungi allegato", onAction = { adding = true })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.attachments, key = { it.id }) { att ->
                val usedBy = index.areas.filter { it.floorplanAttachmentId == att.id }.map { it.name }
                ItemCard(
                    title = att.name,
                    badge = att.classification.toDisplayString(),
                    details = listOf(
                        "${att.fileType.toDisplayString()} · ${att.originalFileName}" + if (attachmentFile(att) == null) " · file non presente su questo PC" else "",
                        usedBy.takeIf { it.isNotEmpty() }?.let { "Planimetria di: ${it.joinToString()}" }.orEmpty()
                    )
                ) {
                    attachmentFile(att)?.let { f ->
                        TextButton(onClick = { runCatching { java.awt.Desktop.getDesktop().open(f) } }) { Text("Apri") }
                    }
                    TextButton(onClick = { floorplanFor = att }, enabled = index.areas.isNotEmpty()) { Text("Usa come planimetria…") }
                    DeleteButton(att.name, onDelete = { onProjectUpdated(ProjectEdits.deleteAttachment(project, att.id), "Allegato eliminato.") },
                        message = if (usedBy.isNotEmpty()) "È la planimetria di ${usedBy.joinToString()}: le aree resteranno senza planimetria." else null)
                }
            }
        }
    }

    if (adding) {
        var file by remember { mutableStateOf<File?>(null) }
        var name by remember { mutableStateOf("") }
        var classification by remember { mutableStateOf(AttachmentClassification.SHAREABLE) }
        EditPanel(
            title = "Aggiungi allegato",
            onDismiss = { adding = false },
            confirmEnabled = file != null && name.isNotBlank(),
            onConfirm = {
                adding = false
                onAddAttachment(file!!, name, classification)
            },
            width = 520.dp
        ) {
            OutlinedButton(onClick = {
                DesktopStorageHelper.pickOpenFile(
                    "Scegli il file da allegare", "Immagini, PDF e documenti",
                    "jpg", "jpeg", "png", "gif", "bmp", "webp", "pdf", "doc", "docx", "xls", "xlsx", "txt", "md"
                )?.let { file = it; if (name.isBlank()) name = it.nameWithoutExtension }
            }) { Text(file?.let { "File: ${it.name}" } ?: "Scegli file…") }
            FormField(name, { name = it }, "Nome *")
            EnumPicker("Classificazione", AttachmentClassification.entries, classification, { it.toDisplayString() }, { classification = it })
        }
    }

    floorplanFor?.let { att ->
        var areaId by remember(att) { mutableStateOf<String?>(null) }
        EditPanel(
            title = "Usa «${att.name}» come planimetria",
            onDismiss = { floorplanFor = null },
            confirmEnabled = areaId != null,
            confirmLabel = "Imposta",
            onConfirm = {
                floorplanFor = null
                onProjectUpdated(ProjectEdits.setAreaFloorplan(project, areaId!!, att.id), "Planimetria di «${index.areaName(areaId)}» impostata.")
            },
            width = 460.dp
        ) {
            OptionPicker("Area *", index.areas, index.area(areaId), { it.name },
                { areaId = it?.id }, optionDetail = { a -> a.floorplanAttachmentId?.let { "ha già una planimetria" } })
        }
    }
}

@Composable
private fun CartographyTab() {
    var source by remember { mutableStateOf(DesktopMapSource.OPEN_TOPO_MAP) }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var zoom by remember { mutableStateOf("15") }
    val latError = FieldValidators.decimal(lat, -85.0, 85.0)
    val lonError = FieldValidators.decimal(lon, -180.0, 180.0)
    val zoomError = FieldValidators.int(zoom, 0, 19)
    val latValue = FieldValidators.parseDecimal(lat)
    val lonValue = FieldValidators.parseDecimal(lon)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Coordinate geografiche", subtitle = "Calcolo della tessera cartografica per una posizione. La mappa non viene scaricata.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionPicker("Fonte", DesktopMapSource.entries, source, { it.displayName }, { it?.let { s -> source = s } }, Modifier.width(260.dp))
            FormField(lat, { lat = it }, "Latitudine", Modifier.width(160.dp), latError, hint = "Es. 45,4642")
            FormField(lon, { lon = it }, "Longitudine", Modifier.width(160.dp), lonError, hint = "Es. 9,1900")
            FormField(zoom, { zoom = it }, "Zoom", Modifier.width(100.dp), zoomError)
        }
        if (latValue != null && lonValue != null && latError == null && lonError == null && zoomError == null) {
            val tile = DesktopCartographyManager.lonLatToTileCoord(lonValue, latValue, zoom.trim().toIntOrNull() ?: 15)
            ItemCard(
                title = "Tessera Z${tile.zoom} / X${tile.x} / Y${tile.y}",
                details = listOf(DesktopCartographyManager.getTileUrl(source, tile), source.attribution)
            )
        } else {
            EmptyState("Inserisci latitudine e longitudine.")
        }
    }
}
