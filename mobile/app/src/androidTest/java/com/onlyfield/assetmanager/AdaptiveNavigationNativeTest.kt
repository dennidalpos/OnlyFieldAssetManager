package com.onlyfield.assetmanager

import android.graphics.Bitmap
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AdaptiveNavigationNativeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun browsingAndFullPageEditorRetainStateWithKeyboard() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = ProjectRepository(db)
        val owner = ViewModelStore()
        lateinit var vm: ProjectViewModel
        var keyboardVisible = false
        val floor = Area(name = "Laboratorio")
        val site = Site(name = "Sede", areas = listOf(floor), devices = listOf(
            Device(technicalName = "SW-UX", areaId = floor.id, category = DeviceCategory.NETWORK_SWITCH),
            Device(technicalName = "AP-UX", category = DeviceCategory.CUSTOM)))
        val project = Project(name = "Progetto UX isolato", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(site))
        try {
            runBlocking { repository.saveProject(project) }
            rule.runOnIdle { vm = ProjectViewModel(repository); owner.put("ux", vm); vm.openProject(project.id) }
            rule.waitUntil(10_000) { vm.project.value != null && vm.busy == null }
            rule.setContent {
                CompositionLocalProvider(LocalMessages provides vm.i18n,
                    LocalDensity provides Density(context.resources.displayMetrics.widthPixels / 360f, 1.3f)) {
                    keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                    AppRoot(vm) { error("Unexpected exit") }
                }
            }
            rule.onAllNodes(isSelectable() and hasAnyAncestor(hasTestTag("main-navigation"))).assertCountEquals(4)
            rule.onNode(hasText("Inventario") and isSelectable()).performClick()
            rule.onNodeWithText("Filtri").performClick()
            rule.onNodeWithText("Tutte le aree").performTouchInput { click() }
            rule.onNode(hasText("Laboratorio") and hasAnyAncestor(isDialog())).performClick()
            rule.onNodeWithText("Chiudi").performClick()
            rule.onNodeWithText("AP-UX").assertDoesNotExist()
            rule.onNode(hasSetTextAction()).performTextInput("SW")
            closeSoftKeyboard()
            rule.onNode(hasText("Rack") and isSelectable()).performClick()
            rule.onNode(hasText("Cablaggio") and isSelectable()).performClick()
            rule.onNode(hasText("Inventario") and isSelectable()).performClick()
            rule.runOnIdle { assertEquals(Screen.Racks, vm.currentScreen) }
            rule.onNode(hasText("Dispositivi") and isSelectable()).performClick()
            rule.onNode(hasText("SW") and hasSetTextAction()).assertExists()
            rule.onNodeWithContentDescription("Rimuovi filtro Laboratorio").assertExists()
            snapshot("inventory-filters")
            rule.onNodeWithText("SW-UX").performClick()
            rule.onNodeWithText(vm.i18n.text("text.49e493ba9d9c")).performClick()
            rule.onNodeWithTag("main-navigation").assertDoesNotExist()
            rule.onNode(hasText("SW-UX") and hasSetTextAction()).performClick().performTextReplacement("SW-UX salvato")
            rule.waitUntil(5_000) { keyboardVisible }
            rule.onNodeWithText(vm.i18n.text("ux.saveChanges")).assertIsDisplayed()
            rule.onNodeWithText(vm.i18n.text("ux.cancelChanges")).assertIsDisplayed()
            snapshot("editor-keyboard")
            rule.onNodeWithText(vm.i18n.text("ux.saveChanges")).performClick()
            rule.waitUntil(10_000) { vm.busy == null && vm.project.value!!.sites.single().devices.first().technicalName == "SW-UX salvato" }
            closeSoftKeyboard()
            rule.onNodeWithTag("main-navigation").assertIsDisplayed()
            rule.runOnIdle { vm.back() }
            rule.onNode(hasText("SW") and hasSetTextAction()).assertExists()
            rule.runOnIdle { vm.navigate(Screen.Documents) }
            rule.onNodeWithText(vm.i18n.text("ux.documentDetails")).performClick()
            rule.onNode(hasText(vm.i18n.text("text.80e3789ff0b4")) and hasSetTextAction()).performClick().performTextInput("Tecnico UX")
            rule.waitUntil(5_000) { keyboardVisible }
            rule.onNodeWithText(vm.i18n.text("text.edb428dc4ee8")).assertIsDisplayed()
            snapshot("documents-keyboard")
        } finally {
            closeSoftKeyboard()
            rule.runOnIdle { owner.clear() }
            db.close()
        }
    }

    private fun snapshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(checkNotNull(context.getExternalFilesDir(null)), "ux-20261011-evidence").apply { check(mkdirs() || isDirectory) }
        rule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
