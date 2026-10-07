package com.onlyfield.assetmanager.pc

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Site
import com.sun.nio.file.ExtendedOpenOption
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import java.nio.file.StandardOpenOption

@RunWith(Parameterized::class)
class FailedStructureSaveTest(private val editor: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun editors() = listOf("map-site", "map-area", "project", "new-site", "edit-site", "new-area", "edit-area")
            .map { arrayOf(it) }
    }

    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun failedSaveKeepsTheDraftAndRetryCommitsItOnce() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val area = Area(name = "Existing area")
        val site = Site(name = "Existing site", areas = listOf(area))
        val project = Project(name = "Existing project", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(site))
        try {
            state.importFile(storage.saveProjectLocally(project), compare = false)
            state.section = if (editor.startsWith("map-")) AppSection.FLOORPLANS else AppSection.PROJECT
            state.selectedSiteId = if (editor == "map-area") site.id else null
            state.selectedAreaId = null
            compose.setContent { DesktopApp(state) }
            val edit = state.i18n.text("text.49e493ba9d9c")
            when (editor) {
                "map-site" -> compose.onNodeWithText(state.i18n.text("text.4e90901d9fa2")).performClick()
                "map-area" -> compose.onNodeWithText(state.i18n.text("text.3575ad226840")).performClick()
                "project" -> compose.onAllNodesWithText(edit).onFirst().performClick()
                "new-site" -> compose.onNodeWithText(state.i18n.text("text.a3236c18a4f4")).performClick()
                "edit-site" -> compose.onAllNodesWithText(edit)[1].performClick()
                "new-area" -> compose.onNodeWithText(state.i18n.text("text.4e331706b4c0")).performClick()
                "edit-area" -> compose.onAllNodesWithText(edit)[2].performClick()
            }
            compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Retained draft")
            val local = storage.listStoredProjects().single().file
            val bytes = local.readBytes()
            val before = state.project
            val undo = state.canUndo
            Files.newByteChannel(local.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
                compose.onNodeWithText(state.i18n.text("text.c5997e85ae51")).performClick()
                compose.runOnIdle {
                    assertNotNull(state.error)
                    assertEquals(before, state.project)
                    assertEquals(undo, state.canUndo)
                    assertArrayEquals(bytes, local.readBytes())
                    if (!editor.startsWith("map-")) assertTrue(state.detailSlot.dirty)
                }
                compose.onNode(hasSetTextAction() and hasText("Retained draft")).assertIsDisplayed()
                val error = requireNotNull(state.error)
                if (editor.startsWith("map-")) {
                    compose.onNode(hasText(error) and hasAnyAncestor(isDialog())).assertIsDisplayed()
                } else compose.onNodeWithText(error).assertIsDisplayed()
            }
            compose.onNodeWithText(state.i18n.text("text.c5997e85ae51")).performClick()
            compose.runOnIdle {
                assertNull(state.error)
                val current = requireNotNull(state.project)
                when (editor) {
                    "project" -> assertEquals("Retained draft", current.name)
                    "map-site", "new-site", "edit-site" -> assertEquals(1, current.sites.count { it.name == "Retained draft" })
                    else -> assertEquals(1, current.sites.single().areas.count { it.name == "Retained draft" })
                }
                assertTrue(state.canUndo)
                storage.importPackageFromFile(local).pkg!!.use { assertEquals(current, it.project) }
                state.undo()
                assertEquals(before, state.project)
                assertFalse(state.canUndo)
            }
            compose.onNode(hasSetTextAction() and hasText("Retained draft")).assertDoesNotExist()
        } finally { state.shutdown() }
    }
}
