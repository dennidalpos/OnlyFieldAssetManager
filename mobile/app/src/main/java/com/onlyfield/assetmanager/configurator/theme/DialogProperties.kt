package com.onlyfield.assetmanager.configurator.theme

import androidx.compose.ui.window.DialogProperties
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

internal fun contentDialogProperties() = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

/** Compose 1.7 dialogs otherwise measure against the display, including system bars. */
@Composable
internal fun contentDialogMaxHeight(): Dp {
    LocalConfiguration.current
    val metrics = LocalContext.current.getSystemService(WindowManager::class.java).currentWindowMetrics
    val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
    return with(LocalDensity.current) { (metrics.bounds.height() - insets.top - insets.bottom).coerceAtLeast(1).toDp() }
}
