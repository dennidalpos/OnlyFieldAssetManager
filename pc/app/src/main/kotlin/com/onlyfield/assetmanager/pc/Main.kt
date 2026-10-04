package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.configurator.ProjectDestination
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
    val i18n = state.i18n
    val hasProject = state.project != null

    Window(
        onCloseRequest = { state.requestChange { state.shutdown(); exitApplication() } },
        title = state.windowTitle,
        icon = icon,
        state = windowState
    ) {
        MenuBar {
            Menu(i18n.text("text.50009ce1da4d"), mnemonic = 'F') {
                Item(i18n.text("text.7c974f0aa7ba"), shortcut = KeyShortcut(Key.N, ctrl = true), onClick = { state.newProject() })
                Item(i18n.text("text.93254a75cf03"), shortcut = KeyShortcut(Key.O, ctrl = true), onClick = state::pickAndImport)
                Item(i18n.text("text.8a1d8b27e511") + "…", shortcut = KeyShortcut(Key.E, ctrl = true), enabled = hasProject, onClick = state::exportPackage)
                Separator()
                Item(i18n.text("text.9e71423137cb"), shortcut = KeyShortcut(Key.P, ctrl = true), enabled = hasProject, onClick = { state.dialog = AppDialog.Documents })
                Item(i18n.text("text.025008cf46b5"), enabled = hasProject, onClick = { state.dialog = AppDialog.ManagePassword })
                Separator()
                Item(i18n.text("text.c00df9e3726e"), shortcut = KeyShortcut(Key.W, ctrl = true), enabled = hasProject, onClick = state::closeProject)
                Item(i18n.text("text.58025f3619bf"), onClick = { state.requestChange { state.shutdown(); exitApplication() } })
            }
            Menu(i18n.text("text.49e493ba9d9c"), mnemonic = 'M') {
                Item(
                    state.undoLabel?.let { i18n.text("text.03441e16eb16", it) } ?: i18n.text("action.undo"),
                    shortcut = KeyShortcut(Key.Z, ctrl = true),
                    enabled = state.canUndo,
                    onClick = state::undo
                )
            }
            Menu(i18n.text("text.9d6f5c19ad04"), mnemonic = 'S') {
                Menu(i18n.text("language.label")) {
                    com.onlyfield.assetmanager.core.i18n.AppLanguage.entries.forEach { language ->
                        CheckboxItem(if (language == com.onlyfield.assetmanager.core.i18n.AppLanguage.SYSTEM) i18n.text("language.system") else language.nativeName, checked = state.language == language, onCheckedChange = { state.changeLanguage(language) })
                    }
                }
                CheckboxItem(i18n.text("text.d25e5999cc28"), checked = state.darkTheme, onCheckedChange = { state.toggleDarkTheme() })
            }
            Menu(i18n.text("text.d0cfbdc71dab"), mnemonic = 'V') {
                val shortcuts = mapOf(AppSection.INVENTORY to Key.One, AppSection.RACKS to Key.Two, AppSection.MODELS to Key.Three,
                    AppSection.FLOORPLANS to Key.Four, AppSection.CREDENTIALS to Key.Five, AppSection.MEDIA to Key.Six,
                    AppSection.CABLING to Key.Seven, AppSection.NETWORK to Key.Eight, AppSection.POWER to Key.Nine)
                ProjectDestination.entries.filter { it != ProjectDestination.DOCUMENTS }.forEach { destination ->
                    val section = requireNotNull(destination.appSection())
                    Item(destination.title(i18n), shortcut = shortcuts[section]?.let { KeyShortcut(it, ctrl = true) },
                        enabled = hasProject || !section.needsProject, onClick = { state.section = section })
                }
                Separator()
                Item(i18n.text("text.5cb1b3fb4d8d"), enabled = hasProject, onClick = { state.dialog = AppDialog.Validation })
            }
        }
        DesktopApp(state)
    }
}
