package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.DesktopCartographyManager
import com.onlyfield.assetmanager.pc.DesktopMapSnapshot
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.DesktopMapSource
import com.onlyfield.assetmanager.pc.DesktopStorageHelper
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.FieldValidators
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FloorplanMediaSection(
    project: Project,
    onProjectUpdated: (Project, String) -> Unit,
    onAddAttachment: (File, String, AttachmentClassification) -> Unit,
    attachmentFile: (Attachment) -> File?,
    onAddMapSnapshot: (DesktopMapSnapshot, String) -> Boolean,
) {
    val index = remember(project) { ProjectIndex(project) }
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubTabs(listOf("Allegati (${project.attachments.size})", "Cartografia"), tab) { tab = it }
        when (tab) {
            0 -> AttachmentsTab(project, index, onProjectUpdated, onAddAttachment, attachmentFile)
            1 -> key(project.id) { CartographyTab(onAddMapSnapshot) }
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
    val changeDetail = LocalDetailChange.current
    var adding by remember { mutableStateOf(false) }
    var floorplanFor by remember { mutableStateOf<Attachment?>(null) }
    var planAreaId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Allegati", subtitle = "Foto, planimetrie e documenti del progetto") {
            Button(onClick = { changeDetail { adding = true } }) { Text("+ Aggiungi allegato") }
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
                        index.attachmentTarget(att).orEmpty(),
                        usedBy.takeIf { it.isNotEmpty() }?.let { "Planimetria di: ${it.joinToString()}" }.orEmpty()
                    )
                ) {
                    attachmentFile(att)?.let { f ->
                        TextButton(onClick = { runCatching { java.awt.Desktop.getDesktop().open(f) } }) { Text("Apri") }
                    }
                    TextButton(onClick = { changeDetail { floorplanFor = att } }, enabled = index.areas.isNotEmpty()) { Text("Usa come planimetria…") }
                    DeleteButton(att.name, onDelete = { onProjectUpdated(ProjectEdits.deleteAttachment(project, att.id), "Allegato eliminato.") },
                        message = if (usedBy.isNotEmpty()) "È la planimetria di ${usedBy.joinToString()}: le aree resteranno senza planimetria." else null)
                }
            }
        }
    }

    if (adding) {
        var file by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf<File?>(null) }
        var name by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf("") }
        var classification by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(AttachmentClassification.SHAREABLE) }
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
        var areaId by remember(LocalDetailSlot.current?.editorVersion, att) { mutableStateOf<String?>(null) }
        if (planAreaId == null) EditPanel(
            title = "Usa «${att.name}» come planimetria",
            onDismiss = { floorplanFor = null },
            confirmEnabled = areaId != null,
            confirmLabel = "Imposta",
            onConfirm = {
                planAreaId = areaId
            },
            width = 460.dp
        ) {
            OptionPicker("Area *", index.areas, index.area(areaId), { it.name },
                { areaId = it?.id }, optionDetail = { a -> a.floorplanAttachmentId?.let { "ha già una planimetria" } })
        }
    }
    if (planAreaId != null && floorplanFor != null) PlanChooser(project, index.area(planAreaId)!!, floorplanFor!!.id, attachmentFile, {}, { id, page, pages ->
        onProjectUpdated(ProjectEdits.setAreaFloorplan(project, planAreaId!!, id, page, pages), "Planimetria impostata."); planAreaId = null; floorplanFor = null
    }, { planAreaId = null; floorplanFor = null })

}

@Composable
private fun CartographyTab(onAddMapSnapshot: (DesktopMapSnapshot, String) -> Boolean) {
    val source = DesktopMapSource.OPEN_TOPO_MAP
    val scope = rememberCoroutineScope()
    val saveSnapshot by rememberUpdatedState(onAddMapSnapshot)
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var zoom by remember { mutableStateOf("15") }
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<org.jetbrains.skia.Image?>(null) }
    preview?.let { displayed -> DisposableEffect(displayed) { onDispose { displayed.close() } } }
    val latError = FieldValidators.decimal(lat, -85.0, 85.0)
    val lonError = FieldValidators.decimal(lon, -180.0, 180.0)
    val zoomError = FieldValidators.int(zoom, 1, 17, required = true)
    val latValue = FieldValidators.parseDecimal(lat)
    val lonValue = FieldValidators.parseDecimal(lon)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Cartografia", subtitle = "Scarica una mappa OpenTopoMap attorno al punto e salvala negli allegati. Serve la rete solo per il download.")
        Text(source.attribution, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(lat, { lat = it }, "Latitudine", Modifier.width(160.dp), latError, hint = "Es. 45,4642")
            FormField(lon, { lon = it }, "Longitudine", Modifier.width(160.dp), lonError, hint = "Es. 9,1900")
            FormField(zoom, { zoom = it }, "Zoom (1-17)", Modifier.width(130.dp), zoomError)
        }
        FormField(name, { name = it }, "Nome mappa", hint = "Vuoto = coordinate")
        Button(
            enabled = !busy && latValue != null && lonValue != null && latError == null && lonError == null && zoomError == null,
            onClick = {
                val latitude = requireNotNull(latValue)
                val longitude = requireNotNull(lonValue)
                val requestedZoom = requireNotNull(FieldValidators.parseInt(zoom))
                val mapName = name.trim().ifBlank { "OpenTopoMap $latitude, $longitude" }
                busy = true
                message = null
                scope.launch {
                    try {
                        val snapshot = withContext(Dispatchers.IO) {
                            DesktopCartographyManager.acquireMapSnapshot(longitude, latitude, requestedZoom)
                        }
                        preview = org.jetbrains.skia.Image.makeFromEncoded(snapshot.imageBytes)
                        message = if (saveSnapshot(snapshot, mapName)) "Mappa salvata negli allegati. Puoi usarla come planimetria anche offline."
                            else "Mappa scaricata, ma il salvataggio non è riuscito. Consulta il messaggio di errore e riprova."
                    } catch (e: IOException) {
                        message = e.message ?: DesktopCartographyManager.NO_NETWORK_MESSAGE
                    } finally {
                        busy = false
                    }
                }
            }
        ) { Text(if (busy) "Download in corso…" else "Scarica e salva mappa") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it) }
        preview?.let { image ->
            Image(image.toComposeImageBitmap(), "Mappa OpenTopoMap scaricata", Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit)
        }
    }
}
