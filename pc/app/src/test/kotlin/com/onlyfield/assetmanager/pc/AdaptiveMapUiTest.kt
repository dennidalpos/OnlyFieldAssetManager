package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import com.onlyfield.assetmanager.configurator.map.*
import com.onlyfield.assetmanager.configurator.theme.OnlyFieldTheme
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.jetbrains.skia.Image
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class AdaptiveMapUiTest(private val width: Int, private val dark: Boolean, private val font: Float) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}dp-dark{1}-font{2}")
        fun sizes() = listOf(360, 412, 600, 840, 1024).flatMap { w ->
            listOf(false, true).flatMap { d -> listOf(1f, 1.3f).map { f -> arrayOf<Any>(w, d, f) } }
        }
    }

    @Test fun selectionExpandsAndCameraSurvivesLeavingAndResizing() = runDesktopComposeUiTest(width, 860) {
        val floor = Area(name = "Piano terra")
        val device = Device(technicalName = "SW-01", areaId = floor.id)
        val project = Project(name = "UI fixture", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Sede", areas = listOf(floor), devices = listOf(device))))
        val state = MapUiState().floor(project, floor.id)
        state.selection = MapSelection.Node(ObjectRef(PlacementTargetType.DEVICE, device.id))
        var shown by mutableStateOf(true)
        var edited = false
        var photo = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, font)) {
                OnlyFieldTheme(dark) { Surface {
                    if (shown) MapWorkspace(project, floor.id, null, Messages(),
                        MapActions({ _, _ -> }, { _, _ -> edited = true }, { _, _ -> }, photo = { _, _ -> photo = true }),
                        Modifier.fillMaxSize(), state = state, onSearch = {}, tools = listOf(com.onlyfield.assetmanager.configurator.PaneAction("Planimetria") {}))
                } }
            }
        }
        onNodeWithText("Modifica").performClick()
        onNodeWithText("Foto").performClick()
        runOnIdle { assertTrue(edited); assertTrue(photo) }
        onNodeWithContentDescription("Ingrandisci mappa").performClick()
        if (width < 840) {
            onNodeWithText("Espandi").performClick()
            onNodeWithTag("map-detail").assertIsDisplayed()
            onNodeWithTag("floor-map").assertDoesNotExist()
            onNodeWithText("Riduci").performClick()
            onNodeWithTag("map-summary").assertIsDisplayed()
        } else onNodeWithTag("map-detail").assertIsDisplayed()
        val before = state.camera(null).zoom
        runOnIdle { shown = false }
        onNodeWithTag("floor-map").assertDoesNotExist()
        runOnIdle { shown = true }
        onNodeWithTag("floor-map").assertIsDisplayed()
        runOnIdle {
            assertEquals(before, state.camera(null).zoom)
            assertEquals(device.id, (state.selection as MapSelection.Node).ref.id)
        }
        val output = File("build/reports/ux-map/$width-$dark-$font.png")
        output.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { output.writeBytes(it.bytes) }
        }
    }
}
