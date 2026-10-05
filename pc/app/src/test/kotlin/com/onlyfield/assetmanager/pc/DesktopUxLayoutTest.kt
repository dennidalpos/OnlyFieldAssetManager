package com.onlyfield.assetmanager.pc

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import org.jetbrains.skia.Image
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class DesktopUxLayoutTest(private val width: Int, private val height: Int) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}x{1}")
        fun sizes() = listOf(arrayOf(1360, 860), arrayOf(1024, 768))
    }

    @Test fun navigationAndEssentialEditorFitTheWindowAndKeepTheDraft() = runDesktopComposeUiTest(width, height) {
        val directory = Files.createTempDirectory("ofam-ux-layout").toFile()
        val state = DesktopAppState(DesktopStorageManager(directory))
        try {
            val site = Site(name = "Operations", areas = listOf(Area(name = "Terra")))
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito dimostrativo", sites = listOf(site))))
            state.section = AppSection.INVENTORY
            setContent { DesktopApp(state) }
            onNode(hasText("Dispositivi") and hasClickAction()).assertIsDisplayed()
            capture("navigation")
            onAllNodesWithText("+ Nuovo apparato").onFirst().performClick()
            // Quick insertion: type, prefilled menus, then the full editor on request.
            onNodeWithText("Aggiungi oggetto").assertIsDisplayed()
            onNodeWithText("Cerca tipologia…").performTextInput("switch")
            onNodeWithText("Switch").performClick()
            onNode(hasSetTextAction() and hasText("SW-01")).assertIsDisplayed()
            onNodeWithText("Aggiungi e modifica").performClick()
            onNodeWithText("Aggiungi dispositivo").assertIsDisplayed()
            onNodeWithText("Aggiungi").assertIsDisplayed().assertIsEnabled()
            onNodeWithText("Numero di serie").assertDoesNotExist()
            capture("editor")
            runOnIdle { state.section = AppSection.NETWORK }
            onNodeWithText("Continua a modificare").performClick()
            onNode(hasSetTextAction() and hasText("SW-01")).assertExists()
            onNodeWithText("Aggiungi").performClick()
            runOnIdle { assertEquals("SW-01", state.project!!.sites.single().devices.single().technicalName) }
            onNodeWithText("Porte").performClick()
            onNodeWithText("Configura porte · SW-01").assertIsDisplayed()
            onNodeWithText("Salva modifiche").assertIsDisplayed()
            capture("ports")
        } finally {
            state.shutdown()
            directory.deleteRecursively()
        }
    }

    private fun ComposeUiTest.capture(name: String) {
        val output = File("build/reports/ux/$width-$height-$name.png")
        output.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { output.writeBytes(it.bytes) }
        }
    }
}
