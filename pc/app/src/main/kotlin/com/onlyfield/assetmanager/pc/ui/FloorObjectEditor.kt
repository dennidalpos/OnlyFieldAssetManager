package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.pc.*
import com.onlyfield.assetmanager.pc.ui.components.*
import java.io.File

@Composable
internal fun FloorObjectEditor(state: DesktopAppState, project: Project, initial: MapObjectDraft, close: () -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    val photos = remember { mutableStateListOf<File>() }
    var removed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var askDiscard by remember { mutableStateOf(false) }
    val dirty = draft != initial || photos.isNotEmpty() || removed.isNotEmpty()
    FormDialog(initial.type.name, { if (dirty) askDiscard = true else close() }, {
        if (state.saveMapObject(draft, photos.toList(), removed)) close()
    }, confirmEnabled = draft.errors(project).isEmpty(), width = 680.dp) {
        ObjectFields(project, draft) { draft = it }
        Text("Foto e allegati", style = MaterialTheme.typography.titleMedium)
        project.attachments.filter { it.targetId == draft.id && it.id !in removed }.forEach { a ->
            Text(a.name); MediaThumbnail(state.attachmentFile(a), a.fileType == AttachmentType.PDF)
            TextButton(onClick = { removed = removed + a.id }) { Text("Rimuovi allegato") }
        }
        photos.toList().forEach { file ->
            Text(file.name); MediaThumbnail(file)
            TextButton(onClick = { photos.remove(file) }) { Text("Rimuovi foto") }
        }
        OutlinedButton(onClick = { DesktopStorageHelper.pickOpenFile("Scegli foto", "Immagini", "png", "jpg", "jpeg", "webp", "bmp")?.let { photos += it } }) { Text("Aggiungi foto…") }
    }
    if (askDiscard) AlertDialog(onDismissRequest = { askDiscard = false }, title = { Text("Scartare le modifiche?") }, text = { Text("Le modifiche non salvate andranno perse.") },
        confirmButton = { TextButton(onClick = close) { Text("Scarta") } }, dismissButton = { TextButton(onClick = { askDiscard = false }) { Text("Continua a modificare") } })
}
