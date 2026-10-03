package com.onlyfield.assetmanager.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
    return { type, id ->
        vm.preparePhoto(type, id)?.let { file ->
            launcher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }
    }
}
