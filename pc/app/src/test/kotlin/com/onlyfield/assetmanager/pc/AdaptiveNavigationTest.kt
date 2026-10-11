package com.onlyfield.assetmanager.pc

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
class AdaptiveNavigationTest {
    @Test fun fourAreasRetainFiltersAndGuardInventoryDrafts() = runDesktopComposeUiTest(1024, 768) {
        val directory = Files.createTempDirectory("ofam-adaptive-navigation").toFile()
        val state = DesktopAppState(DesktopStorageManager(directory))
        val floor = Area(name = "Laboratorio")
        val site = Site(name = "Sede", areas = listOf(floor), devices = listOf(
            Device(technicalName = "SW-01", areaId = floor.id, category = DeviceCategory.NETWORK_SWITCH),
            Device(technicalName = "AP-01", category = DeviceCategory.CUSTOM)))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Progetto UX", sites = listOf(site))))
            setContent { DesktopApp(state) }
            onAllNodes(isSelectable() and hasAnyAncestor(hasTestTag("main-navigation"))).assertCountEquals(4)
            onNode(hasText("Inventario") and hasClickAction()).performClick()
            onNodeWithText("Filtri").performClick()
            onNodeWithText("Tutte le aree").performTouchInput { click() }
            onNodeWithText("Laboratorio").performClick()
            onNodeWithText("Chiudi").performClick()
            onNodeWithText("AP-01").assertDoesNotExist()
            onNodeWithText("SW-01").assertIsDisplayed()
            onNodeWithText("Cerca nome, etichetta, IP, MAC, seriale…").performTextInput("SW")
            onNode(hasText("Rack") and hasClickAction()).performClick()
            onNode(hasText("Cablaggio") and hasClickAction()).performClick()
            onNode(hasText("Inventario") and hasClickAction()).performClick()
            runOnIdle { assertEquals(AppSection.RACKS, state.section) }
            onNode(hasText("Dispositivi") and hasClickAction()).performClick()
            onNode(hasText("SW") and hasSetTextAction()).assertExists()
            onNodeWithContentDescription("Rimuovi filtro Laboratorio").assertExists()
            onAllNodesWithText("Modifica").onFirst().performClick()
            onNode(hasText("SW-01") and hasSetTextAction()).performTextReplacement("SW-01 bozza")
            onNode(hasText("Progetto") and hasClickAction()).performClick()
            onNodeWithText("Continua a modificare").performClick()
            onNode(hasText("SW-01 bozza") and hasSetTextAction()).assertExists()
            onNodeWithText("Salva modifiche").performClick()
            runOnIdle { assertEquals("SW-01 bozza", state.project!!.sites.single().devices.first().technicalName) }
            onNode(hasText("Progetto") and hasClickAction()).performClick()
            onNodeWithText("Documenti").assertExists()
            onNodeWithText("Rete").assertExists()
        } finally {
            state.shutdown()
            directory.deleteRecursively()
        }
    }
}
