package com.onlyfield.assetmanager.pc

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.model.*
import com.sun.nio.file.ExtendedOpenOption
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import java.nio.file.StandardOpenOption

@RunWith(Parameterized::class)
class FailedSectionPickerSaveTest(private val picker: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun pickers() = listOf("inventory", "rack", "rack-unit", "media-pdf").map { arrayOf(it) }
    }

    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun failedSaveKeepsDraftAndRetryPersistsOnlyOneChange() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val area = Area(name = "Test floor")
        val site = Site(name = "Test site", areas = listOf(area))
        val rack = Rack(name = "Existing rack", areaId = area.id, heightU = 12)
        try {
            val project = Project(name = "Section picker", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(site), racks = listOf(rack))
            state.importFile(storage.saveProjectLocally(project), compare = false)
            var attachment: Attachment? = null
            if (picker == "media-pdf") {
                val file = folder.newFile("pages.pdf")
                PDDocument().use { it.addPage(PDPage()); it.addPage(PDPage()); it.save(file) }
                attachment = state.importFloorplan(file, area.id)!!
            }
            state.section = when (picker) {
                "inventory" -> AppSection.INVENTORY
                "media-pdf" -> AppSection.MEDIA
                else -> AppSection.RACKS
            }
            compose.setContent { DesktopApp(state) }
            val action: String
            if (picker == "media-pdf") {
                compose.onNodeWithText(state.i18n.text("text.fa7e72cbd571")).performClick()
                compose.onNodeWithText(state.i18n.text("text.ddbccb18e085")).performClick()
                compose.onNodeWithText(area.name).performClick()
                compose.onNodeWithText(state.i18n.text("text.125d6d4967e5")).performClick()
                compose.onNodeWithText(state.i18n.text("text.c192249f9066", 2)).performClick()
                action = state.i18n.text("text.f62c5da23e83", 2)
            } else {
                if (picker == "rack-unit") compose.onNodeWithContentDescription(state.i18n.text("rack.freeUnit", 12))
                    .performSemanticsAction(SemanticsActions.OnClick) { it() }
                else compose.onAllNodesWithText(state.i18n.text(if (picker == "rack") "text.b049315ba1c3" else "text.8650e4573818")).onFirst().performClick()
                if (picker != "rack") {
                    compose.onNodeWithText("Cerca tipologia…").performTextInput("switch")
                    compose.onNodeWithText("Switch").performClick()
                }
                compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("Retained draft")
                action = state.i18n.text("ux.add")
            }
            val before = state.project
            val undo = state.canUndo
            val local = storage.listStoredProjects().single().file
            val bytes = local.readBytes()
            Files.newByteChannel(local.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
                compose.onNodeWithText(action).performClick()
                compose.runOnIdle {
                    assertNotNull(state.error)
                    assertEquals(before, state.project)
                    assertEquals(undo, state.canUndo)
                    assertArrayEquals(bytes, local.readBytes())
                }
                if (picker != "media-pdf") compose.onNode(hasSetTextAction() and hasText("Retained draft")).assertIsDisplayed()
                compose.onNodeWithText(action).assertIsDisplayed()
                compose.onNode(hasText(state.error!!) and hasAnyAncestor(isDialog())).assertIsDisplayed()
            }
            compose.onNodeWithText(action).performClick()
            compose.runOnIdle {
                assertNull(state.error)
                val saved = state.project!!
                when (picker) {
                    "rack" -> assertEquals("Retained draft", saved.racks.single { it.id != rack.id }.name)
                    "media-pdf" -> {
                        val savedArea = saved.sites.single().areas.single()
                        assertEquals(attachment!!.id, savedArea.floorplanAttachmentId)
                        assertEquals(1, savedArea.floorplanPageIndex)
                    }
                    else -> {
                        val device = saved.sites.single().devices.single()
                        assertEquals("Retained draft", device.technicalName)
                        assertEquals("switch", device.objectTypeId)
                        if (picker == "rack-unit") {
                            assertEquals(rack.id, device.rackId)
                            assertEquals(12, device.positionU)
                        }
                    }
                }
                storage.importPackageFromFile(local).pkg!!.use { assertEquals(saved, it.project) }
                state.undo()
                assertEquals(before, state.project)
                assertEquals(undo, state.canUndo)
            }
            compose.onNode(isDialog()).assertDoesNotExist()
        } finally { state.shutdown() }
    }
}
