package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.PlanMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun MediaThumbnail(file: File?, pdf: Boolean = false, page: Int = 0, modifier: Modifier = Modifier) {
    var bitmap by remember(file, page) { mutableStateOf<ImageBitmap?>(null) }
    var failure by remember(file, page) { mutableStateOf<String?>(null) }
    LaunchedEffect(file, page) {
        try { bitmap = withContext(Dispatchers.IO) { PlanMedia.image(file ?: error("File non disponibile"), pdf, page, 512) } }
        catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; failure = e.message ?: "Anteprima non disponibile" }
    }
    bitmap?.let { Image(it, "Anteprima", modifier.height(120.dp), contentScale = ContentScale.Fit) }
    failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
internal fun PlanChooser(project: Project, area: Area, importedId: String?, file: (Attachment) -> File?, onPick: () -> Unit, onAssign: (String?, Int, Int) -> Unit, onClose: () -> Unit) {
    var selectedId by remember(importedId) { mutableStateOf(importedId) }
    var page by remember(selectedId) { mutableStateOf(if (selectedId == area.floorplanAttachmentId) area.floorplanPageIndex else 0) }
    val selected = project.attachments.find { it.id == selectedId }
    var pages by remember(selectedId) { mutableStateOf(0) }
    var failure by remember(selectedId) { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedId) {
        if (selected?.fileType == AttachmentType.PDF) {
            try { pages = withContext(Dispatchers.IO) { PlanMedia.pageCount(file(selected) ?: error("File non disponibile")) } }
            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; failure = e.message ?: "PDF non leggibile" }
        }
    }
    AlertDialog(onDismissRequest = onClose, title = { Text("Planimetria di ${area.name}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick) { Text("Scegli dal dispositivo…") }
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (selected?.fileType == AttachmentType.PDF) {
                Text("${selected.name}: scegli la pagina del piano")
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items((0 until pages).toList()) { index ->
                        Card(Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable { page = index }, colors = CardDefaults.cardColors(containerColor = if (index == page) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Pagina ${index + 1}", Modifier.padding(8.dp))
                            MediaThumbnail(file(selected), true, index, Modifier.fillMaxWidth())
                        }
                    }
                }
            } else LazyColumn(Modifier.heightIn(max = 340.dp)) {
                items(project.attachments.filter { it.fileType == AttachmentType.IMAGE || it.fileType == AttachmentType.PDF }, key = { it.id }) { a ->
                    TextButton(onClick = { if (a.fileType == AttachmentType.IMAGE) onAssign(a.id, 0, 1) else selectedId = a.id }, modifier = Modifier.fillMaxWidth()) { Text(a.name + if (a.fileType == AttachmentType.PDF) " (PDF)" else "") }
                }
            }
        }
    }, confirmButton = {
        if (selected?.fileType == AttachmentType.PDF) TextButton(enabled = page in 0 until pages && failure == null, onClick = { onAssign(selected.id, page, pages) }) { Text("Usa pagina ${page + 1}") }
        else TextButton(onClick = onClose) { Text("Chiudi") }
    }, dismissButton = {
        Row {
            if (selected != null) TextButton(onClick = { selectedId = null }) { Text("Allegati") }
            if (area.floorplanAttachmentId != null) TextButton(onClick = { onAssign(null, 0, 0) }) { Text("Rimuovi sfondo") }
            TextButton(onClick = onClose) { Text("Annulla") }
        }
    })
}
