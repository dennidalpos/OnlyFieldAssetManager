package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.ui.LocalMessages

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.configurator.*
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FloorObjectEditor(vm: ProjectViewModel, project: Project, initial: MapObjectDraft, initialSection: ConfiguratorPage = ConfiguratorPage.ESSENTIALS, close: () -> Unit) {
    val i18n = LocalMessages.current

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
        if (allowed) shoot() else vm.notifyError(i18n.text("text.0ac53f93c8a1"))
    }
    DisposableEffect(Unit) { onDispose { temporary.forEach { it.delete() } } }
    EditScreen(configuratorTitle(project, initial, i18n), { if (vm.busy == null) close() }, { vm.saveMapObject(context, draft, photos.toList(), removed, close) }, validationMessage = configuratorValidation(project, draft, i18n), confirmEnabled = draft.errors(project, i18n).isEmpty() && vm.busy == null, wideContent = true, confirmLabel = configuratorAction(project, initial, i18n)) {
        val dirty = LocalMarkDirty.current
        SideEffect { markDirty = dirty }
        ObjectFields(project, draft, initialSection, extraSections = {
            ConfiguratorSection(i18n.text("ux.attachments"), i18n = i18n, summary = (project.attachments.count { it.targetId == draft.id && it.id !in removed } + photos.size).takeIf { it > 0 }?.let { i18n.text("config.attachmentsCount", it) }) {
                project.attachments.filter { it.targetId == draft.id && it.id !in removed }.forEach { a ->
                    Text(a.name)
                    MediaThumbnail(vm.attachmentFile(a), a.fileType == AttachmentType.PDF)
                    TextButton(onClick = { removed = removed + a.id; dirty() }) { Text(i18n.text("text.960630ee842c")) }
                }
                photos.toList().forEach { uri ->
                    Text(uri.lastPathSegment ?: i18n.text("text.7490e08564b3"))
                    UriPhotoThumbnail(uri)
                    TextButton(onClick = { photos.remove(uri); dirty() }) { Text(i18n.text("text.f5115aa0e57e")) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text(i18n.text("text.0f9162856d60")) }
                    OutlinedButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) shoot() else permission.launch(Manifest.permission.CAMERA)
                    }) { Text(i18n.text("text.d88211a9e4b9")) }
                }
            }
        }) { draft = it }
    }
}

@Composable
private fun UriPhotoThumbnail(uri: Uri) {
    val i18n = LocalMessages.current

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
                    val bitmap = android.graphics.BitmapFactory.decodeFileDescriptor(fd.fileDescriptor, null, options) ?: error(i18n.text("text.5adc189e505a"))
                    bitmap.asImageBitmap()
                } ?: error(i18n.text("text.d15d3071c8db"))
            }
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; failure = e.message }
    }
    image?.let { androidx.compose.foundation.Image(it, i18n.text("text.7490e08564b3"), Modifier.fillMaxWidth().height(140.dp)) }
    failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
