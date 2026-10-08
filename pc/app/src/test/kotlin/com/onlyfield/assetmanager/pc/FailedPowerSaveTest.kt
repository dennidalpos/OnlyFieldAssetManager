package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.ui.PowerBadgeSection
import com.onlyfield.assetmanager.pc.ui.components.MasterDetailHost
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
class FailedPowerSaveTest(private val editor: String, private val panel: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}, panel={1}")
        fun editors() = listOf("new-feed", "feed", "new-poe", "poe", "new-badge", "badge")
            .flatMap { editor -> listOf(false, true).map { arrayOf<Any>(editor, it) } }
    }

    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun failureRetainsDraftAndRetryPreservesSelectionsAndHistory() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val area = Area(name = "Test floor")
        val port = Port(deviceId = java.util.UUID.randomUUID().toString(), name = "PoE port")
        val device = Device(id = port.deviceId, technicalName = "Powered device", areaId = area.id, ports = listOf(port), mountingType = MountingType.OUT_OF_RACK)
        val source = Device(technicalName = "Power source", areaId = area.id, mountingType = MountingType.OUT_OF_RACK)
        val feed = PowerFeed(deviceId = device.id, feedName = "Existing feed", sourceDeviceId = source.id,
            observedSource = "Retained observation", observedEpochMs = 123, notes = "Existing notes")
        val poe = PoeMapping(portId = port.id, role = PoeRole.PD_SINK, standard = PoeStandard.IEEE_802_3BT, notes = "Existing notes")
        val badge = DocumentBadge(targetType = "DEVICE", targetId = device.id, label = "Existing badge", category = BadgeCategory.OPEN_ISSUE)
        val creating = editor.startsWith("new-")
        val kind = editor.removePrefix("new-")
        try {
            state.importFile(storage.saveProjectLocally(Project(name = "Power retry", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Site", areas = listOf(area), devices = listOf(device, source))),
                powerFeeds = if (creating) emptyList() else listOf(feed),
                poeMappings = if (creating) emptyList() else listOf(poe),
                documentBadges = if (creating) emptyList() else listOf(badge))), compare = false)
            assertNotNull("Fixture import failed: ${state.error}", state.project)
            compose.setContent {
                CompositionLocalProvider(LocalMessages provides state.i18n) {
                    MaterialTheme {
                        if (panel) MasterDetailHost { PowerBadgeSection(state.project!!, state::update, true, { state.error }) }
                        else PowerBadgeSection(state.project!!, state::update, true, { state.error })
                    }
                }
            }
            val tab = when (kind) { "poe" -> "text.ffaf43588488"; "badge" -> "text.605ad6ef80b0"; else -> "text.370b792df123" }
            compose.onNodeWithText(state.i18n.text(tab, if (creating) 0 else 1)).performClick()
            val add = when (kind) { "poe" -> "text.b5aeb95142a5"; "badge" -> "text.5e9867b48f83"; else -> "text.04585136780b" }
            compose.onAllNodesWithText(state.i18n.text(if (creating) add else "text.49e493ba9d9c")).onFirst().performClick()
            if (creating && kind == "feed") {
                compose.onNodeWithText(state.i18n.text("text.a814bcd8ca15")).performClick()
                compose.onNodeWithText(device.technicalName).performClick()
                compose.onNodeWithText(state.i18n.text("text.11ae92f057eb")).performClick()
                compose.onNodeWithText(source.technicalName).performClick()
            }
            if (creating && kind == "poe") {
                compose.onNodeWithText(state.i18n.text("text.57c2ec879203")).performClick()
                compose.onNodeWithText("${device.technicalName} › ${port.name}").performClick()
            }
            val draftLabel = when (kind) { "poe" -> "text.d8da2c49df39"; "badge" -> "text.77a1b70aa654"; else -> "text.694885c1179e" }
            compose.onNode(hasSetTextAction() and hasText(state.i18n.text(draftLabel)))
                .performScrollTo().performTextReplacement("Retained power draft")
            val before = state.project
            val trash = state.trash
            val undo = state.canUndo
            val local = storage.listStoredProjects().single().file
            val bytes = local.readBytes()
            val save = state.i18n.text("text.c5997e85ae51")
            Files.newByteChannel(local.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
                compose.onNodeWithText(save).performClick()
                compose.runOnIdle {
                    assertNotNull(state.error)
                    assertEquals(before, state.project)
                    assertEquals(trash, state.trash)
                    assertEquals(undo, state.canUndo)
                    assertArrayEquals(bytes, local.readBytes())
                }
                compose.onNode(hasSetTextAction() and hasText("Retained power draft")).performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(state.error!!).performScrollTo().assertIsDisplayed()
            }
            compose.onNodeWithText(save).performClick()
            compose.runOnIdle {
                assertNull(state.error)
                val saved = state.project!!
                when (kind) {
                    "feed" -> {
                        val item = saved.powerFeeds.single()
                        assertEquals("Retained power draft", item.feedName)
                        assertEquals(device.id, item.deviceId)
                        assertEquals(source.id, item.sourceDeviceId)
                        if (!creating) { assertEquals(feed.id, item.id); assertEquals(feed.observedSource, item.observedSource)
                            assertEquals(feed.observedEpochMs, item.observedEpochMs); assertEquals(feed.notes, item.notes) }
                    }
                    "poe" -> {
                        val item = saved.poeMappings.single()
                        assertEquals("Retained power draft", item.notes)
                        assertEquals(port.id, item.portId)
                        if (!creating) { assertEquals(poe.id, item.id); assertEquals(poe.role, item.role); assertEquals(poe.standard, item.standard) }
                    }
                    "badge" -> {
                        val item = saved.documentBadges.single()
                        assertEquals("Retained power draft", item.label)
                        if (!creating) { assertEquals(badge.id, item.id); assertEquals(badge.targetId, item.targetId)
                            assertEquals(badge.targetType, item.targetType); assertEquals(badge.category, item.category) }
                        else { assertEquals("PROJECT", item.targetType); assertEquals(saved.id, item.targetId) }
                    }
                }
                storage.importPackageFromFile(local).pkg!!.use { assertEquals(saved, it.project) }
                state.undo()
                assertEquals(before, state.project)
                assertEquals(trash, state.trash)
                assertEquals(undo, state.canUndo)
            }
            compose.onNode(hasSetTextAction() and hasText("Retained power draft")).assertDoesNotExist()
        } finally { state.shutdown() }
    }
}
