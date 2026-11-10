package com.onlyfield.assetmanager.pc

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.onlyfield.assetmanager.configurator.SecondaryModule
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
class SecondaryModulesUiTest {
    @Test fun unusedModulesWaitBehindOtherModules() = runDesktopComposeUiTest(1360, 860) {
        val directory = Files.createTempDirectory("ofam-modules").toFile()
        val state = DesktopAppState(DesktopStorageManager(directory))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", sites = listOf(Site(name = "Sede", areas = listOf(Area(name = "Terra")))))))
            state.section = AppSection.POWER
            setContent { DesktopApp(state) }
            onNodeWithText("Credenziali").assertDoesNotExist()
            onNodeWithText("Badge documentali (0)").assertDoesNotExist()
            onNodeWithText("Credenziali, Configurazioni apparati, Badge Documentali").assertExists()
            // The entry closes the scrolling sidebar: bring it into view first.
            onNodeWithText("Altri moduli").performScrollTo().performClick()
            // Opened: every module is reachable again, still empty.
            onNodeWithText("Credenziali").assertExists()
            onNodeWithText("Badge documentali (0)").assertExists()
            onNodeWithText("Nascondi i moduli non usati").assertExists()
        } finally {
            state.shutdown()
            directory.deleteRecursively()
        }
    }

    /** AUD-65: hiding the modules removes the tab that holds the draft, so it goes through the discard guard. */
    private fun hidingAsksBeforeDroppingDraft(section: AppSection, tabKey: String, newKey: String, fieldKey: String) = runDesktopComposeUiTest(1600, 1000) {
        val directory = Files.createTempDirectory("ofam-modules").toFile()
        val state = DesktopAppState(DesktopStorageManager(directory))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", sites = listOf(Site(name = "Sede", areas = listOf(Area(name = "Terra")), devices = listOf(Device(technicalName = "SW")))))))
            val i18n = state.i18n
            state.showSecondary = true
            state.section = section
            setContent { DesktopApp(state) }
            onNodeWithText(i18n.text(tabKey, 0)).performClick()
            onAllNodesWithText(i18n.text(newKey))[0].performClick()
            onNode(hasText(i18n.text(fieldKey)) and hasSetTextAction()).performTextInput("BOZZA")
            onNodeWithText(i18n.text("nav.lessModules")).performScrollTo().performClick()
            onNodeWithText(i18n.text("text.500cb3f3c51b")).performClick()
            onNodeWithText("BOZZA").assertExists()
            runOnIdle { assertTrue(state.showSecondary) }
            onNodeWithText(i18n.text("nav.lessModules")).performScrollTo().performClick()
            onNodeWithText(i18n.text("text.8386702d2ee2")).performClick()
            onNodeWithText("BOZZA").assertDoesNotExist()
            runOnIdle {
                assertFalse(state.showSecondary)
                assertTrue(state.project!!.deviceConfigurations.isEmpty() && state.project!!.documentBadges.isEmpty())
            }
        } finally {
            state.shutdown()
            directory.deleteRecursively()
        }
    }

    @Test fun hidingModulesGuardsConfigurationDraft() = hidingAsksBeforeDroppingDraft(AppSection.NETWORK, "text.469e8a3048fe", "text.d940cb0bf5d2", "text.b03a74353bbd")
    @Test fun hidingModulesGuardsBadgeDraft() = hidingAsksBeforeDroppingDraft(AppSection.POWER, "text.605ad6ef80b0", "text.5e9867b48f83", "text.77a1b70aa654")

    /** AUD-66: sub-tab and section searches survive going away and back. */
    @Test fun searchesSurviveSubTabAndSectionSwitches() = runDesktopComposeUiTest(1360, 860) {
        val directory = Files.createTempDirectory("ofam-modules").toFile()
        val state = DesktopAppState(DesktopStorageManager(directory))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", sites = listOf(Site(name = "Sede", areas = listOf(Area(name = "Terra")))))))
            val i18n = state.i18n
            state.section = AppSection.NETWORK
            setContent { DesktopApp(state) }
            onNode(hasSetTextAction()).performTextInput("FILTRO")
            onNodeWithText(i18n.text("text.348cff30ff92", 0)).performClick()
            onNodeWithText(i18n.text("text.474ee27d6727", 0)).performClick()
            onNode(hasText("FILTRO") and hasSetTextAction()).assertExists()
            runOnIdle { state.section = AppSection.MODELS }
            onNode(hasSetTextAction()).performTextInput("MODELLO")
            runOnIdle { state.section = AppSection.NETWORK }
            onNode(hasText("FILTRO") and hasSetTextAction()).assertExists()
            runOnIdle { state.section = AppSection.MODELS }
            onNode(hasText("MODELLO") and hasSetTextAction()).assertExists()
        } finally {
            state.shutdown()
            directory.deleteRecursively()
        }
    }

    @Test fun usedModulesAreAlwaysShown() {
        val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, credentials = listOf(Credential(username = "u", secret = "s")))
        assertEquals(listOf(SecondaryModule.CONFIGURATIONS, SecondaryModule.BADGES), SecondaryModule.hidden(project))
    }
}
