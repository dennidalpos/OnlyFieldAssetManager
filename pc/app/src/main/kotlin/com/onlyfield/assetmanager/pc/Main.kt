package com.onlyfield.assetmanager.pc

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.skia.Image

private fun loadAppIcon(): BitmapPainter? = try {
    Thread.currentThread().contextClassLoader.getResourceAsStream("app_icon.png")?.use {
        BitmapPainter(Image.makeFromEncoded(it.readBytes()).toComposeImageBitmap())
    }
} catch (_: Exception) {
    null
}

fun main() = application {
    val state = remember { DesktopAppState(DesktopStorageManager(PortablePaths.resolveDataDir())) }
    val windowState = rememberWindowState(size = DpSize(1360.dp, 860.dp))
    val icon = remember { loadAppIcon() }
    val hasProject = state.project != null

    // Every change is saved automatically, so closing needs no confirmation.
    Window(
        onCloseRequest = { state.shutdown(); exitApplication() },
        title = state.windowTitle,
        icon = icon,
        state = windowState
    ) {
        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item("Nuovo sito…", shortcut = KeyShortcut(Key.N, ctrl = true), onClick = { state.dialog = AppDialog.NewProject })
                Item("Apri / Importa .ofam…", shortcut = KeyShortcut(Key.O, ctrl = true), onClick = state::pickAndImport)
                Item("Esporta .ofam…", shortcut = KeyShortcut(Key.E, ctrl = true), enabled = hasProject, onClick = state::exportPackage)
                Separator()
                Item("Documenti e stampa…", shortcut = KeyShortcut(Key.P, ctrl = true), enabled = hasProject, onClick = { state.dialog = AppDialog.Documents })
                Item("Password del progetto…", enabled = hasProject, onClick = { state.dialog = AppDialog.ManagePassword })
                Separator()
                Item("Chiudi progetto", shortcut = KeyShortcut(Key.W, ctrl = true), enabled = hasProject, onClick = state::closeProject)
                Item("Esci", onClick = { state.shutdown(); exitApplication() })
            }
            Menu("Modifica", mnemonic = 'M') {
                Item(
                    state.undoLabel?.let { "Annulla: $it" } ?: "Annulla",
                    shortcut = KeyShortcut(Key.Z, ctrl = true),
                    enabled = state.canUndo,
                    onClick = state::undo
                )
            }
            Menu("Visualizza", mnemonic = 'S') {
                CheckboxItem("Tema scuro", checked = state.darkTheme, onCheckedChange = { state.toggleDarkTheme() })
            }
            Menu("Vai", mnemonic = 'V') {
                val digits = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
                AppSection.entries.forEachIndexed { i, section ->
                    Item(
                        section.title,
                        shortcut = digits.getOrNull(i)?.let { KeyShortcut(it, ctrl = true) },
                        enabled = hasProject || !section.needsProject,
                        onClick = { state.section = section }
                    )
                }
                Separator()
                Item("Controllo del progetto…", enabled = hasProject, onClick = { state.dialog = AppDialog.Validation })
            }
        }
        DesktopApp(state)
    }
}
