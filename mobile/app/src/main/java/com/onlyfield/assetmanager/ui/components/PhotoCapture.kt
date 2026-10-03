package com.onlyfield.assetmanager.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.onlyfield.assetmanager.core.model.AttachmentTargetType
import com.onlyfield.assetmanager.ui.ProjectViewModel

/**
 * Returns a function that opens the camera app; the photo is written straight into the project's
 * attachment folder (through FileProvider) and linked to [AttachmentTargetType]/id.
 */
@Composable
fun rememberPhotoCapture(vm: ProjectViewModel): (AttachmentTargetType, String?) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved -> vm.onPhotoResult(saved) }
    fun shoot(type: AttachmentTargetType, id: String?) {
        vm.preparePhoto(type, id)?.let { file ->
            launcher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }
    }
    // The manifest declares CAMERA (scanner), so the camera intent also requires it to be granted.
    var pending by remember { mutableStateOf<Pair<AttachmentTargetType, String?>?>(null) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        pending?.let { (type, id) -> if (ok) shoot(type, id) else vm.notifyError("Serve il permesso della fotocamera per scattare foto.") }
        pending = null
    }
    return { type, id ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) shoot(type, id)
        else {
            pending = type to id
            askPermission.launch(Manifest.permission.CAMERA)
        }
    }
}
