package com.onlyfield.assetmanager.pc

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1024.dp, 720.dp))

    Window(
        onCloseRequest = ::exitApplication,
        title = "OnlyField Asset Manager — Windows Desktop",
        state = windowState
    ) {
        DesktopApp()
    }
}
