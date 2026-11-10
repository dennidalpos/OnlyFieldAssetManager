package com.onlyfield.assetmanager.ui.components

import com.onlyfield.assetmanager.ui.LocalMessages

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
import kotlinx.coroutines.isActive

/**
 * Opens the camera and links its output as an attachment. Shots come in series: after a kept photo
 * the camera opens again for the same object; cancelling it ends the series.
 */
@Composable
fun rememberPhotoCapture(vm: ProjectViewModel): (AttachmentTargetType, String?) -> Unit {
    val i18n = LocalMessages.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var series by remember { mutableStateOf<Pair<AttachmentTargetType, String?>?>(null) }
    var shots by remember { mutableStateOf(0) }
    lateinit var shoot: (AttachmentTargetType, String?) -> Unit
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val target = series
        vm.onPhotoResult(saved) { kept ->
            if (!scope.isActive) return@onPhotoResult
            if (kept && target != null) { shots++; shoot(target.first, target.second) }
            else {
                if (shots > 1) vm.notifyInfo(i18n.plural("photo.seriesDone", shots))
                series = null; shots = 0
            }
        }
    }
    shoot = { type, id ->
        series = type to id
        vm.preparePhoto(type, id)?.let { file ->
            launcher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }
    }
    // Camera permission is shared with the scanner.
    var pending by remember { mutableStateOf<Pair<AttachmentTargetType, String?>?>(null) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        pending?.let { (type, id) -> if (ok) shoot(type, id) else vm.notifyError(i18n.text("text.0ac53f93c8a1")) }
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
