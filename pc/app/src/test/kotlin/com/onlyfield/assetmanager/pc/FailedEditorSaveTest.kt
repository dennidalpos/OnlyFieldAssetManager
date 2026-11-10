package com.onlyfield.assetmanager.pc

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import com.onlyfield.assetmanager.pc.ui.InventorySection
import com.onlyfield.assetmanager.pc.ui.RackSection
import com.onlyfield.assetmanager.pc.ui.FloorplanMediaSection
import com.onlyfield.assetmanager.core.model.*
import com.sun.nio.file.ExtendedOpenOption
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.awt.Container
import java.awt.Window
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.Timer

@RunWith(Parameterized::class)
class FailedEditorSaveTest(private val editor: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun editors() = listOf("new-device", "device", "new-rack", "rack", "place", "replace", "batch", "attachment")
            .map { arrayOf(it) }
    }

    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun failedSaveRetainsEditorAndRetryCommitsOnce() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val area = Area(name = "Test floor")
        val device = Device(technicalName = "Existing device", areaId = area.id, mountingType = MountingType.OUT_OF_RACK)
        val site = Site(name = "Test site", areas = listOf(area), devices = listOf(device))
        val rack = Rack(name = "Existing rack", areaId = area.id, heightU = 12)
        val source = folder.newFile("source.txt").apply { writeText("Synthetic attachment") }
        try {
            state.importFile(storage.saveProjectLocally(Project(name = "Editor retry", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(site), racks = listOf(rack))), compare = false)
            state.section = when (editor) {
                "new-rack", "rack", "place" -> AppSection.RACKS
                "attachment" -> AppSection.MEDIA
                else -> AppSection.INVENTORY
            }
            compose.setContent {
                CompositionLocalProvider(LocalMessages provides state.i18n) {
                    MaterialTheme {
                        Column {
                            state.error?.let { Text(it) }
                            when (state.section) {
                                AppSection.INVENTORY -> InventorySection(state.project!!, state::update, state::addToTrash, state::mergeDevices, { state.error })
                                AppSection.RACKS -> RackSection(state.project!!, state::update, state::addToTrash, { state.error })
                                else -> FloorplanMediaSection(state.project!!, state::update, state::addAttachment, state::attachmentBytes,
                                    state::hasAttachment, state::openAttachment, state.hasPassword, state::addMapSnapshot, { state.error })
                            }
                        }
                    }
                }
            }
            val action: String
            when (editor) {
                "new-device", "new-rack" -> {
                    compose.onNodeWithText(state.i18n.text(if (editor == "new-rack") "text.b049315ba1c3" else "text.8650e4573818")).performClick()
                    if (editor == "new-device") {
                        compose.onNodeWithText("Cerca tipologia…").performTextInput("switch")
                        compose.onNodeWithText("Switch").performClick()
                    }
                    compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("Retained draft")
                    compose.onNodeWithText(state.i18n.text("quick.addAndEdit")).performClick()
                    action = state.i18n.text("ux.add")
                }
                "device", "rack" -> {
                    compose.onNodeWithText(state.i18n.text("text.49e493ba9d9c")).performClick()
                    compose.onNode(hasSetTextAction() and hasText(if (editor == "rack") rack.name else device.technicalName))
                        .performTextReplacement("Retained draft")
                    action = state.i18n.text("ux.saveChanges")
                }
                "replace" -> {
                    compose.onNodeWithText(state.i18n.text("text.9cc22637b4a3")).performClick()
                    compose.onNodeWithText(state.i18n.text("text.475df7909caf")).performClick()
                    compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Retained draft")
                    action = state.i18n.text("text.3260dc474cbc")
                }
                "batch" -> {
                    compose.onNodeWithText(state.i18n.text("text.0e5b81f76ba6")).performClick()
                    compose.onNodeWithText(state.i18n.text("text.12fea36b47a9")).performClick()
                    compose.onNodeWithText(state.i18n.text("text.b8320c85f325")).performScrollTo().performClick()
                    compose.onNode(hasSetTextAction() and hasText(state.i18n.text("text.d8da2c49df39")))
                        .performScrollTo().performTextInput("Retained draft")
                    action = state.i18n.text("text.635382b11942", 1)
                }
                "place" -> {
                    compose.onNodeWithText(state.i18n.text("text.9b3e3f84b6a8")).performClick()
                    compose.onNodeWithText(state.i18n.text("text.e7f2c0e68768")).performClick()
                    compose.onNodeWithText(device.technicalName).performClick()
                    action = state.i18n.text("text.ee84d8c1e721")
                }
                else -> {
                    compose.onAllNodesWithText(state.i18n.text("text.eb6a4870f326")).onFirst().performClick()
                    val timer = Timer(100, null)
                    timer.addActionListener {
                        fun chooser(container: Container): JFileChooser? =
                            container.components.firstNotNullOfOrNull { child ->
                                (child as? JFileChooser) ?: (child as? Container)?.let { chooser(it) }
                            }
                        Window.getWindows().filter { it.isShowing }.firstNotNullOfOrNull { chooser(it) }?.let {
                            timer.stop()
                            it.selectedFile = source
                            it.approveSelection()
                        }
                    }
                    SwingUtilities.invokeAndWait { timer.start() }
                    try { compose.onNodeWithText(state.i18n.text("text.26d8bab1dd5d")).performClick() }
                    finally { SwingUtilities.invokeAndWait { timer.stop() } }
                    compose.onNode(hasSetTextAction() and hasText("source")).performTextReplacement("Retained draft")
                    action = state.i18n.text("text.c5997e85ae51")
                }
            }
            val before = state.project
            val trash = state.trash
            val undo = state.canUndo
            val local = storage.listStoredProjects().single().file
            val bytes = local.readBytes()
            Files.newByteChannel(local.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
                compose.onNodeWithText(action).performClick()
                compose.runOnIdle {
                    assertNotNull(state.error)
                    assertEquals(before, state.project)
                    assertEquals(trash, state.trash)
                    assertEquals(undo, state.canUndo)
                    assertArrayEquals(bytes, local.readBytes())
                }
                compose.onNodeWithText(action).assertIsDisplayed()
                if (editor == "replace") compose.onNode(hasText(state.error!!) and hasAnyAncestor(isDialog())).assertIsDisplayed()
                else compose.onNodeWithText(state.error!!).assertIsDisplayed()
                if (editor == "place") compose.onNodeWithText(device.technicalName).assertIsDisplayed()
                else compose.onNode(hasSetTextAction() and hasText("Retained draft")).performScrollTo().assertIsDisplayed()
                if (editor == "attachment") {
                    compose.onNodeWithText(state.i18n.text("text.12610d51818d", source.name)).assertIsDisplayed()
                    assertTrue(state.project!!.attachments.isEmpty())
                }
            }
            compose.onNodeWithText(action).performClick()
            compose.runOnIdle {
                assertNull(state.error)
                val saved = state.project!!
                when (editor) {
                    "new-rack", "rack" -> assertEquals(1, saved.racks.count { it.name == "Retained draft" })
                    "place" -> assertEquals(rack.id, saved.sites.single().devices.single().rackId)
                    "batch" -> assertEquals("Retained draft", saved.sites.single().devices.single().observation?.notes)
                    "attachment" -> {
                        val added = saved.attachments.single()
                        assertEquals("Retained draft", added.name)
                        assertArrayEquals(source.readBytes(), storage.attachmentBytes(saved.id, added))
                    }
                    else -> assertEquals(1, saved.sites.single().devices.count { it.technicalName == "Retained draft" })
                }
                if (editor == "replace") assertEquals(trash.size + 1, state.trash.size)
                storage.importPackageFromFile(local).pkg!!.use { assertEquals(saved, it.project) }
                state.undo()
                assertEquals(before, state.project)
                assertEquals(trash, state.trash)
                assertEquals(undo, state.canUndo)
            }
            compose.onNode(isDialog()).assertDoesNotExist()
        } finally { state.shutdown() }
    }

}
