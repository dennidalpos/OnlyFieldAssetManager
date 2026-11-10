package com.onlyfield.assetmanager.pc

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.onlyfield.assetmanager.configurator.PortQuickActions
import com.onlyfield.assetmanager.configurator.PortQuickDialog
import com.onlyfield.assetmanager.configurator.map.GlobalSearchDialog
import com.onlyfield.assetmanager.configurator.map.MapObjectPicker
import com.onlyfield.assetmanager.configurator.theme.OnlyFieldTheme
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.jetbrains.skia.Image
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Dialogs with lists or drawings use the window: full screen on phones, wide and tall on desktops. */
@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class DialogLayoutTest(private val width: Int, private val height: Int) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}x{1}")
        fun sizes() = listOf(arrayOf<Any>(360, 720), arrayOf<Any>(1280, 800))
    }

    private val project = Project(name = "Rete", createdEpochMs = 1, updatedEpochMs = 1,
        sites = listOf(Site(id = "a", name = "Municipio", areas = listOf(Area(id = "t", name = "Terra")), devices = listOf(
            Device(id = "s", technicalName = "SW-01", objectTypeId = "switch", areaId = "t", ports = (1..24).map { Port(id = "s-$it", deviceId = "s", name = "$it") }),
            Device(id = "r", technicalName = "RTR-01", objectTypeId = "router", areaId = "t", ports = (1..4).map { Port(id = "r-$it", deviceId = "r", name = "$it") })))))
        .let { HardwareConfigurator.connect(it, "r-1", "s-1", CableMedium.ETHERNET_COPPER) }

    private fun ComposeUiTest.capture(name: String) {
        val output = File("build/reports/visual-configurator/dialog-$name-$width.png")
        output.parentFile.mkdirs()
        Image.makeFromBitmap(onNode(isDialog()).captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { output.writeBytes(it.bytes) } }
    }

    @Test fun searchUsesTheWindow() = runDesktopComposeUiTest(width, height) {
        setContent { OnlyFieldTheme(false) { GlobalSearchDialog(project, Messages(), emptyList(), {}) {} } }
        onNode(hasSetTextAction()).performTextInput("0")
        onNodeWithText("SW-01").assertIsDisplayed()
        onNodeWithText("Chiudi").assertIsDisplayed()
        capture("search")
    }

    @Test fun objectPickerShowsTypesThenTheDrawing() = runDesktopComposeUiTest(width, height) {
        setContent { OnlyFieldTheme(false) { MapObjectPicker(project, Messages(), "t", null, null, {}, {}, {}) } }
        onNodeWithText("Annulla").assertIsDisplayed()
        capture("picker-types")
        onNodeWithText("Switch").performClick()
        onNodeWithText("Aggiungi").assertIsDisplayed()
        capture("picker-draft")
    }

    @Test fun portCardKeepsActionsVisible() = runDesktopComposeUiTest(width, height) {
        setContent { OnlyFieldTheme(false) { PortQuickDialog(project, "s-1", Messages(), PortQuickActions(update = { _, _ -> }), {}) } }
        onNodeWithText("Chiudi").assertIsDisplayed()
        capture("port")
    }
}
