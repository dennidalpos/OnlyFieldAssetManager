package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.pc.LocalMessages

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
    val i18n = LocalMessages.current
    MediaThumbnailSource(file, {
        file?.let { source ->
            com.onlyfield.assetmanager.exchange.AttachmentFiles.validateSize(source.length(), i18n)
            source.inputStream().use { input -> java.io.ByteArrayOutputStream().also {
                com.onlyfield.assetmanager.exchange.AttachmentFiles.copyBounded(input, it, i18n)
            }.toByteArray() }
        }
    }, pdf, page, modifier)
}

@Composable
internal fun MediaThumbnail(key: Any?, read: () -> ByteArray?, pdf: Boolean = false, page: Int = 0, modifier: Modifier = Modifier) {
    MediaThumbnailSource(key, read, pdf, page, modifier)
}

@Composable
private fun MediaThumbnailSource(key: Any?, read: () -> ByteArray?, pdf: Boolean, page: Int, modifier: Modifier) {
    val i18n = LocalMessages.current

    var bitmap by remember(key, page) { mutableStateOf<ImageBitmap?>(null) }
    var failure by remember(key, page) { mutableStateOf<String?>(null) }
    LaunchedEffect(key, page) {
        try { bitmap = withContext(Dispatchers.IO) { PlanMedia.image(read() ?: error(i18n.text("text.dad522b5d9b7")), pdf, page, 512, i18n = i18n) } }
        catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; failure = e.message ?: i18n.text("text.1d24f1640f57") }
    }
    bitmap?.let { Image(it, i18n.text("text.e9bcaec77820"), modifier.height(120.dp), contentScale = ContentScale.Fit) }
    failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
internal fun PlanChooser(project: Project, area: Area, importedId: String?, bytes: (Attachment) -> ByteArray?, onPick: () -> Unit, onAssign: (String?, Int, Int) -> Unit, onClose: () -> Unit, error: String? = null) {
    val i18n = LocalMessages.current

    var selectedId by remember(importedId) { mutableStateOf(importedId) }
    var page by remember(selectedId) { mutableStateOf(if (selectedId == area.floorplanAttachmentId) area.floorplanPageIndex else 0) }
    val selected = project.attachments.find { it.id == selectedId }
    var pages by remember(selectedId) { mutableStateOf(0) }
    var failure by remember(selectedId) { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedId) {
        if (selected?.fileType == AttachmentType.PDF) {
            try { pages = withContext(Dispatchers.IO) { PlanMedia.pageCount(bytes(selected) ?: error(i18n.text("text.dad522b5d9b7")), i18n = i18n) } }
            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; failure = e.message ?: i18n.text("text.cff201c9dd3a") }
        }
    }
    AlertDialog(onDismissRequest = onClose, title = { Text(i18n.text("text.f3e846ee8611", area.name)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick) { Text(i18n.text("text.5784dc5eaf9c")) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (selected?.fileType == AttachmentType.PDF) {
                Text(i18n.text("text.0d63c434f784", selected.name))
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items((0 until pages).toList()) { index ->
                        Card(Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable { page = index }, colors = CardDefaults.cardColors(containerColor = if (index == page) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                            Text(i18n.text("text.c192249f9066", index + 1), Modifier.padding(8.dp))
                            MediaThumbnail(selected.id, { bytes(selected) }, true, index, Modifier.fillMaxWidth())
                        }
                    }
                }
            } else LazyColumn(Modifier.heightIn(max = 340.dp)) {
                items(project.attachments.filter { it.fileType == AttachmentType.IMAGE || it.fileType == AttachmentType.PDF }.sortedForDisplay(i18n) { it.name }, key = { it.id }) { a ->
                    TextButton(onClick = { if (a.fileType == AttachmentType.IMAGE) onAssign(a.id, 0, 1) else selectedId = a.id }, modifier = Modifier.fillMaxWidth()) { Text(a.name + if (a.fileType == AttachmentType.PDF) i18n.text("text.0bd78b344dac") else "") }
                }
            }
        }
    }, confirmButton = {
        if (selected?.fileType == AttachmentType.PDF) TextButton(enabled = page in 0 until pages && failure == null, onClick = { onAssign(selected.id, page, pages) }) { Text(i18n.text("text.f62c5da23e83", page + 1)) }
        else TextButton(onClick = onClose) { Text(i18n.text("text.32d4079b315b")) }
    }, dismissButton = {
        Row {
            if (selected != null) TextButton(onClick = { selectedId = null }) { Text(i18n.text("text.92a776eacf2a")) }
            if (area.floorplanAttachmentId != null) TextButton(onClick = { onAssign(null, 0, 0) }) { Text(i18n.text("text.f139b3096d6f")) }
            TextButton(onClick = onClose) { Text(i18n.text("text.18c9d912a210")) }
        }
    })
}
