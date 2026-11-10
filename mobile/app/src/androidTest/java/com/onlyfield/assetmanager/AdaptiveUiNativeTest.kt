package com.onlyfield.assetmanager

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.configurator.map.*
import com.onlyfield.assetmanager.configurator.theme.ContentDialog
import com.onlyfield.assetmanager.configurator.theme.OnlyFieldTheme
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AdaptiveUiNativeTest {
    @get:Rule val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun mapSelectionAndCameraSurvivePresentationChanges() {
        val floor = Area(name = "Floor")
        val device = Device(technicalName = "SW-UI", areaId = floor.id)
        val project = Project(name = "Isolated adaptive map", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Site", areas = listOf(floor), devices = listOf(device))))
        val state = MapUiState().floor(project, floor.id)
        state.selection = MapSelection.Node(ObjectRef(PlacementTargetType.DEVICE, device.id))
        var width by mutableIntStateOf(360)
        var dark by mutableStateOf(false)
        var font by mutableFloatStateOf(1f)
        var visible by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(context.resources.displayMetrics.widthPixels / width.toFloat(), font)) {
                OnlyFieldTheme(dark) { Surface {
                    if (visible) MapWorkspace(project, floor.id, null, Messages(), MapActions({ _, _ -> }, { _, _ -> }, { _, _ -> }),
                        Modifier.fillMaxSize().safeDrawingPadding(), state = state)
                } }
            }
        }
        for (w in listOf(360, 412, 600, 840, 1024)) for (d in listOf(false, true)) for (f in listOf(1f, 1.3f)) {
            rule.runOnIdle { width = w; dark = d; font = f }
            if (w < 840) {
                rule.onNodeWithText("Espandi").assertIsDisplayed().performClick()
                rule.onNodeWithTag("map-detail").assertIsDisplayed()
                rule.onNodeWithText("Riduci").performClick()
                rule.onNodeWithTag("map-summary").assertIsDisplayed()
            } else rule.onNodeWithTag("map-detail").assertIsDisplayed()
            rule.onNodeWithContentDescription("Ingrandisci mappa").performClick()
            val zoom = state.camera(null).zoom
            rule.runOnIdle { visible = false }
            rule.onNodeWithTag("floor-map").assertDoesNotExist()
            rule.runOnIdle { visible = true }
            rule.onNodeWithTag("floor-map").assertIsDisplayed()
            rule.runOnIdle { assertEquals(zoom, state.camera(null).zoom); assertNotNull(state.selection) }
            snapshot("map-$w-$d-$f", isRoot())
        }
    }

    @Test fun dialogFooterAndLastFieldStayReachableWithKeyboard() {
        var value by mutableStateOf("")
        var saved = ""
        var keyboardVisible = false
        rule.setContent {
            OnlyFieldTheme(false) {
                ContentDialog({}, title = { Text("Isolated keyboard form") }, text = {
                    Column {
                        repeat(25) { Text("Field group $it") }
                        OutlinedTextField(value, { value = it }, label = { Text("Last field") })
                    }
                }, confirmButton = {
                    keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                    Button(onClick = { saved = value }) { Text("Save fixture") }
                }, dismissButton = { TextButton(onClick = {}) { Text("Cancel fixture") } })
            }
        }
        try {
            rule.onNodeWithText("Last field").performScrollTo().performClick().performTextInput("Retained draft")
            rule.waitUntil(5_000) { keyboardVisible }
            rule.onNodeWithText("Last field").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("Save fixture").assertIsDisplayed()
            rule.onNodeWithText("Cancel fixture").assertIsDisplayed()
            snapshot("dialog-keyboard", isDialog())
            rule.onNodeWithText("Save fixture").performClick()
            rule.runOnIdle { assertEquals("Retained draft", saved) }
        } finally { closeSoftKeyboard() }
    }

    private fun snapshot(name: String, root: SemanticsMatcher) {
        if (InstrumentationRegistry.getArguments().getString("matrixEvidence") != "true") return
        val output = File(checkNotNull(context.getExternalFilesDir(null)), "adaptive-ui-evidence").apply { check(mkdirs() || isDirectory) }
        val bitmap = rule.onAllNodes(root).onLast().captureToImage().asAndroidBitmap()
        try { File(output, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
        finally { bitmap.recycle() }
    }
}
