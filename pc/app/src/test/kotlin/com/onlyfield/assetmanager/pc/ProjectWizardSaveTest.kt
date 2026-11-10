package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.onboarding.*
import com.onlyfield.assetmanager.pc.ui.dialogs.ProjectDialogs
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProjectWizardSaveTest {
    @get:Rule val folder = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    @Test fun failedCreationRetainsFieldsAcrossBusyCompositionAndRetrySavesOnce() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val wizard = NewSiteWizard(step = NewSiteStep.PASSWORD, draft = NewSiteDraft(
            projectName = "Retained site", customer = "Retained customer",
            sites = listOf(Site(name = "Building", areas = listOf(Area(name = "Floor")))),
            password = "dummy-password", passwordConfirm = "dummy-password"))
        try {
            state.newProject()
            state.newSiteWizard = wizard
            compose.setContent { CompositionLocalProvider(LocalMessages provides state.i18n) { MaterialTheme { ProjectDialogs(state) } } }
            val projects = storage.getProjectsFolder()
            assertTrue(projects.delete())
            projects.writeText("Isolated storage failure")
            compose.onNodeWithText(state.i18n.text("text.6c4a7984bdc6")).performClick()
            compose.runOnIdle {
                assertNotNull(state.error)
                assertEquals(AppDialog.NewProject, state.dialog)
                assertEquals(wizard, state.newSiteWizard)
                assertNull(state.project)
            }
            compose.onNodeWithText(state.i18n.text("text.80426885bb74")).performClick()
            compose.onNodeWithText("Floor").assertExists()
            compose.onNodeWithText(state.i18n.text("text.29ddfd8a8643")).performClick()
            assertTrue(projects.delete())
            assertTrue(projects.mkdir())
            compose.onNodeWithText(state.i18n.text("text.6c4a7984bdc6")).performClick()
            compose.runOnIdle {
                assertNull(state.dialog)
                assertEquals("Retained site", state.project!!.name)
                assertTrue(state.project!!.isPasswordProtected)
                assertEquals(NewSiteWizard(), state.newSiteWizard)
                assertEquals(1, storage.listStoredProjects().size)
            }
        } finally { state.closeProject() }
    }
}
