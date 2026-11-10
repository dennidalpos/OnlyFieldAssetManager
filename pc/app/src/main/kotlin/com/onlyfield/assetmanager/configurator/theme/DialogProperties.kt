package com.onlyfield.assetmanager.configurator.theme

import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

internal fun contentDialogProperties() = DialogProperties(usePlatformDefaultWidth = false)

@Composable
internal fun contentDialogMaxHeight(): Dp = Dp.Infinity
