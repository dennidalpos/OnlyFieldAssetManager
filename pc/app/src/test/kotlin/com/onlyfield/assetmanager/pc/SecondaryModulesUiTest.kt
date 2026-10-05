package com.onlyfield.assetmanager.pc

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.onlyfield.assetmanager.configurator.SecondaryModule
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import org.junit.Assert.assertEquals
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

    @Test fun usedModulesAreAlwaysShown() {
        val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, credentials = listOf(Credential(username = "u", secret = "s")))
        assertEquals(listOf(SecondaryModule.CONFIGURATIONS, SecondaryModule.BADGES), SecondaryModule.hidden(project))
    }
}
