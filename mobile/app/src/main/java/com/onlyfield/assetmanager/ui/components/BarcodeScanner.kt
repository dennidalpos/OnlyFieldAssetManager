package com.onlyfield.assetmanager.ui.components

import com.onlyfield.assetmanager.ui.LocalMessages

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning

/** Full-page offline QR/barcode scanner. */
@Composable
fun BarcodeScanner(onCode: (String) -> Unit, onClose: () -> Unit, hint: String = LocalMessages.current.text("text.75bf8546adbd")) {
    val i18n = LocalMessages.current

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(Unit) { if (!granted) askPermission.launch(Manifest.permission.CAMERA) }
    BackHandler(onBack = onClose)

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        if (granted) {
            val currentOnCode by rememberUpdatedState(onCode)
            val controller = remember { LifecycleCameraController(context) }
            DisposableEffect(lifecycleOwner) {
                val scanner = BarcodeScanning.getClient()
                val executor = ContextCompat.getMainExecutor(context)
                var delivered = false
                controller.setImageAnalysisAnalyzer(
                    executor,
                    MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED, executor) { result ->
                        val code = result?.getValue(scanner)?.firstOrNull()?.rawValue
                        // Deliver only the first read.
                        if ((code != null) && !delivered) {
                            delivered = true
                            currentOnCode(code)
                        }
                    }
                )
                controller.bindToLifecycle(lifecycleOwner)
                onDispose {
                    controller.unbind()
                    scanner.close()
                }
            }
            AndroidView(factory = { PreviewView(it).apply { this.controller = controller } }, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (denied) i18n.text("text.4279e55f0fca") else hint,
                    color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
                )
            }
            Button(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(i18n.text("text.32d4079b315b")) }
        }
    }
}
