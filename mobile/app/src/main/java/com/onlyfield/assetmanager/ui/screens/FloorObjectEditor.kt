package com.onlyfield.assetmanager.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.components.*
import java.io.File
import java.util.UUID

@Composable
internal fun FloorObjectEditor(vm: ProjectViewModel, project: Project, initial: MapObjectDraft, close: () -> Unit) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(initial) }
    val photos = remember { mutableStateListOf<Uri>() }
    val temporary = remember { mutableListOf<File>() }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var removed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var markDirty by remember { mutableStateOf<() -> Unit>({}) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> photos.addAll(uris); if (uris.isNotEmpty()) markDirty() }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        cameraFile?.let { if (saved) { photos += FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it); markDirty() } else it.delete() }
    }
    fun shoot() {
        val folder = File(context.cacheDir, "object_photos").apply { mkdirs() }
        val file = File(folder, "${UUID.randomUUID()}.jpg"); temporary += file; cameraFile = file
        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) shoot() else vm.notifyError("Serve il permesso della fotocamera per scattare foto.")
    }
    DisposableEffect(Unit) { onDispose { temporary.forEach { it.delete() } } }
    EditScreen(initial.type.name, { if (vm.busy == null) close() }, { vm.saveMapObject(context, draft, photos.toList(), removed, close) }, confirmEnabled = draft.errors(project).isEmpty() && vm.busy == null) {
        val dirty = LocalMarkDirty.current
        SideEffect { markDirty = dirty }
        ObjectFields(project, draft) { draft = it }
        Text("Foto e allegati", style = MaterialTheme.typography.titleMedium)
        project.attachments.filter { it.targetId == draft.id && it.id !in removed }.forEach { a ->
            Text(a.name)
            MediaThumbnail(vm.attachmentFile(a), a.fileType == AttachmentType.PDF)
            TextButton(onClick = { removed = removed + a.id; dirty() }) { Text("Rimuovi allegato") }
        }
        photos.toList().forEach { uri ->
            Text(uri.lastPathSegment ?: "Foto selezionata")
            UriPhotoThumbnail(uri)
            TextButton(onClick = { photos.remove(uri); dirty() }) { Text("Rimuovi foto") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text("Foto dal telefono") }
            OutlinedButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) shoot() else permission.launch(Manifest.permission.CAMERA)
            }) { Text("Scatta foto") }
        }
    }
}

@Composable
private fun UriPhotoThumbnail(uri: Uri) {
    val context = LocalContext.current
    var image by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var failure by remember(uri) { mutableStateOf<String?>(null) }
    LaunchedEffect(uri) {
        try {
            image = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFileDescriptor(fd.fileDescriptor, null, bounds)
                    val options = android.graphics.BitmapFactory.Options().apply {
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 512) sample *= 2
                        inSampleSize = sample
                    }
                    val bitmap = android.graphics.BitmapFactory.decodeFileDescriptor(fd.fileDescriptor, null, options) ?: error("Foto non leggibile")
                    bitmap.asImageBitmap()
                } ?: error("Foto non disponibile")
            }
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; failure = e.message }
    }
    image?.let { androidx.compose.foundation.Image(it, "Foto selezionata", Modifier.fillMaxWidth().height(140.dp)) }
    failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
