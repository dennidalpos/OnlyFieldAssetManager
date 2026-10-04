package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.display.sortedForDisplay
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
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubTabs(listOf(i18n.text("text.690325ff1b4c", project.attachments.size), i18n.text("text.2ebfe0133d0c")), tab) { tab = it }
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
    val i18n = LocalMessages.current

    val changeDetail = LocalDetailChange.current
    var adding by remember { mutableStateOf(false) }
    var floorplanFor by remember { mutableStateOf<Attachment?>(null) }
    var planAreaId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.92a776eacf2a"), subtitle = i18n.text("text.3ea328688d27")) {
            Button(onClick = { changeDetail { adding = true } }) { Text(i18n.text("text.eb6a4870f326")) }
        }
        if (project.attachments.isEmpty()) EmptyState(i18n.text("text.4bb26fa604b4"), actionLabel = i18n.text("text.eb6a4870f326"), onAction = { adding = true })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.attachments.sortedForDisplay(i18n) { it.name }, key = { it.id }) { att ->
                val usedBy = index.areas.filter { it.floorplanAttachmentId == att.id }.map { it.name }
                ItemCard(
                    title = att.name,
                    badge = att.classification.toDisplayString(i18n = i18n),
                    details = listOf(
                        "${att.fileType.toDisplayString(i18n = i18n)} · ${att.originalFileName}" + if (attachmentFile(att) == null) i18n.text("text.17abaf4534d2") else "",
                        index.attachmentTarget(att, i18n = i18n).orEmpty(),
                        usedBy.takeIf { it.isNotEmpty() }?.let { i18n.text("text.a63270a10e72", it.joinToString()) }.orEmpty()
                    )
                ) {
                    attachmentFile(att)?.let { f ->
                        TextButton(onClick = { runCatching { java.awt.Desktop.getDesktop().open(f) } }) { Text(i18n.text("text.12abcf9ee7d6")) }
                    }
                    TextButton(onClick = { changeDetail { floorplanFor = att } }, enabled = index.areas.isNotEmpty()) { Text(i18n.text("text.fa7e72cbd571")) }
                    DeleteButton(att.name, onDelete = { onProjectUpdated(ProjectEdits.deleteAttachment(project, att.id), i18n.text("text.0a1dce905d01")) },
                        message = if (usedBy.isNotEmpty()) i18n.text("text.927f290752ab", usedBy.joinToString()) else null)
                }
            }
        }
    }

    if (adding) {
        var file by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf<File?>(null) }
        var name by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf("") }
        var classification by remember(LocalDetailSlot.current?.editorVersion) { mutableStateOf(AttachmentClassification.SHAREABLE) }
        EditPanel(
            title = i18n.text("text.606ec551af4a"),
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
                    i18n.text("text.7a70b30138ab"), i18n.text("text.dd430b275e27"),
                    "jpg", "jpeg", "png", "gif", "bmp", "webp", "pdf", "doc", "docx", "xls", "xlsx", "txt", "md",
                    i18n = i18n)?.let { file = it; if (name.isBlank()) name = it.nameWithoutExtension }
            }) { Text(file?.let { i18n.text("text.12610d51818d", it.name) } ?: i18n.text("text.26d8bab1dd5d")) }
            FormField(name, { name = it }, i18n.text("text.2e245546ff59"))
            EnumPicker(i18n.text("text.57fbd1029ff6"), AttachmentClassification.entries, classification, { it.toDisplayString(i18n = i18n) }, { classification = it })
        }
    }

    floorplanFor?.let { att ->
        var areaId by remember(LocalDetailSlot.current?.editorVersion, att) { mutableStateOf<String?>(null) }
        if (planAreaId == null) EditPanel(
            title = i18n.text("text.17a5dcd48a7e", att.name),
            onDismiss = { floorplanFor = null },
            confirmEnabled = areaId != null,
            confirmLabel = i18n.text("text.125d6d4967e5"),
            onConfirm = {
                planAreaId = areaId
            },
            width = 460.dp
        ) {
            OptionPicker(i18n.text("text.ddbccb18e085"), index.areas, index.area(areaId), { it.name },
                { areaId = it?.id }, optionDetail = { a -> a.floorplanAttachmentId?.let { i18n.text("text.613f9fe4c7c9") } })
        }
    }
    if (planAreaId != null && floorplanFor != null) PlanChooser(project, index.area(planAreaId)!!, floorplanFor!!.id, attachmentFile, {}, { id, page, pages ->
        onProjectUpdated(ProjectEdits.setAreaFloorplan(project, planAreaId!!, id, page, pages), i18n.text("text.fcd1cc58f46b")); planAreaId = null; floorplanFor = null
    }, { planAreaId = null; floorplanFor = null })

}

@Composable
private fun CartographyTab(onAddMapSnapshot: (DesktopMapSnapshot, String) -> Boolean) {
    val i18n = LocalMessages.current

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
    val latError = FieldValidators.decimal(lat, -85.0, 85.0, i18n = i18n)
    val lonError = FieldValidators.decimal(lon, -180.0, 180.0, i18n = i18n)
    val zoomError = FieldValidators.int(zoom, 1, 17, required = true, i18n = i18n)
    val latValue = FieldValidators.parseDecimal(lat)
    val lonValue = FieldValidators.parseDecimal(lon)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.2ebfe0133d0c"), subtitle = i18n.text("text.3937e81f67c1"))
        Text(source.attribution, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(lat, { lat = it }, i18n.text("text.9b0a8bdbd3df"), Modifier.width(160.dp), latError, hint = i18n.text("text.c68adbb89b8d"))
            FormField(lon, { lon = it }, i18n.text("text.094c15c97b20"), Modifier.width(160.dp), lonError, hint = i18n.text("text.5e6ab81bedad"))
            FormField(zoom, { zoom = it }, i18n.text("text.002fdf567c4f"), Modifier.width(130.dp), zoomError)
        }
        FormField(name, { name = it }, i18n.text("text.9cc76babaef2"), hint = i18n.text("text.609e94eb3a8b"))
        Button(
            enabled = !busy && latValue != null && lonValue != null && latError == null && lonError == null && zoomError == null,
            onClick = {
                val latitude = requireNotNull(latValue)
                val longitude = requireNotNull(lonValue)
                val requestedZoom = requireNotNull(FieldValidators.parseInt(zoom))
                val mapName = name.trim().ifBlank { i18n.text("text.266d928fbf52", latitude, longitude) }
                busy = true
                message = null
                scope.launch {
                    try {
                        val snapshot = withContext(Dispatchers.IO) {
                            DesktopCartographyManager.acquireMapSnapshot(longitude, latitude, requestedZoom, i18n = i18n)
                        }
                        preview = org.jetbrains.skia.Image.makeFromEncoded(snapshot.imageBytes)
                        message = if (saveSnapshot(snapshot, mapName)) i18n.text("text.1f67cb117a2e")
                            else i18n.text("text.5d5ff46c47f9")
                    } catch (e: IOException) {
                        message = e.message ?: i18n.text("map.network.desktop")
                    } finally {
                        busy = false
                    }
                }
            }
        ) { Text(if (busy) i18n.text("text.7d0c748fc03f") else i18n.text("text.5a34e09b9356")) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it) }
        preview?.let { image ->
            Image(image.toComposeImageBitmap(), i18n.text("text.4233b4daedc0"), Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit)
        }
    }
}
