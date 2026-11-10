package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.ui.CredentialsSection
import com.onlyfield.assetmanager.pc.ui.DeviceModelsSection
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
class FailedModelCredentialSaveTest(private val editor: String, private val panel: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}, panel={1}")
        fun editors() = listOf("new-model", "model", "apply", "new-credential", "credential")
            .flatMap { editor -> listOf(false, true).map { arrayOf<Any>(editor, it) } }
    }

    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun failureRetainsDraftAndRetryPersistsOnce() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val area = Area(name = "Test floor")
        val device = Device(technicalName = "Existing device", areaId = area.id, mountingType = MountingType.OUT_OF_RACK)
        val model = DeviceModel(name = "Existing model", category = DeviceCategory.NETWORK_SWITCH)
        val credential = Credential(username = "Synthetic user", secret = "Dummy placeholder", groupName = "Test group", deviceId = device.id)
        val credentials = editor.endsWith("credential")
        try {
            state.importFile(storage.saveProjectLocally(Project(name = "Model credential retry", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Test site", areas = listOf(area), devices = listOf(device))),
                deviceModels = listOf(model), credentials = listOf(credential))), compare = false)
            compose.setContent {
                CompositionLocalProvider(LocalMessages provides state.i18n) {
                    MaterialTheme {
                        if (panel) MasterDetailHost {
                            if (credentials) CredentialsSection(state.project!!, state::update, { state.error })
                            else DeviceModelsSection(state.project!!, state::update, { state.error })
                        } else {
                            if (credentials) CredentialsSection(state.project!!, state::update, { state.error })
                            else DeviceModelsSection(state.project!!, state::update, { state.error })
                        }
                    }
                }
            }
            val action = when (editor) {
                "new-model" -> state.i18n.text("ux.add")
                "model" -> state.i18n.text("ux.saveChanges")
                "apply" -> state.i18n.text("ux.apply")
                else -> state.i18n.text("text.c5997e85ae51")
            }
            val open = when (editor) {
                "new-model" -> "text.d166b2503fad"
                "new-credential" -> "text.5477fceee15e"
                "apply" -> "text.1d13a7022c17"
                else -> "text.49e493ba9d9c"
            }
            compose.onNodeWithText(state.i18n.text(open)).performClick()
            if (editor == "apply") {
                compose.onNodeWithText(state.i18n.text("config.device")).performClick()
                compose.onNodeWithText(device.technicalName).performClick()
            }
            val label = state.i18n.text(if (credentials) "text.3255e3d5e3b4" else "config.name")
            compose.onNode(hasSetTextAction() and hasText(label)).performScrollTo().performTextReplacement("Retained draft")
            if (editor == "new-credential") {
                compose.onNode(hasSetTextAction() and hasText(state.i18n.text("text.7f9bedb6b654")))
                    .performScrollTo().performTextReplacement("Dummy retry placeholder")
                compose.onNode(hasSetTextAction() and hasText(state.i18n.text("text.b9bb40edbe6e")))
                    .performScrollTo().performTextReplacement("Retained group")
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
                compose.onNode(hasSetTextAction() and hasText("Retained draft")).performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(state.error!!).performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(action).assertIsDisplayed()
            }
            compose.onNodeWithText(action).performClick()
            compose.runOnIdle {
                assertNull(state.error)
                val saved = state.project!!
                when (editor) {
                    "new-model" -> assertEquals(2, saved.deviceModels.size)
                    "model" -> { assertEquals(1, saved.deviceModels.size); assertEquals(model.id, saved.deviceModels.single().id) }
                    "apply" -> assertEquals(model.id, saved.sites.single().devices.single().deviceModelId)
                    "new-credential" -> {
                        assertEquals(2, saved.credentials.size)
                        val added = saved.credentials.single { it.username == "Retained draft" }
                        assertEquals("Dummy retry placeholder", added.secret)
                        assertEquals("Retained group", added.groupName)
                    }
                    "credential" -> { assertEquals(1, saved.credentials.size); assertEquals(credential.id, saved.credentials.single().id)
                        assertEquals(credential.secret, saved.credentials.single().secret)
                        assertEquals(credential.deviceId, saved.credentials.single().deviceId) }
                }
                if (credentials) assertEquals(1, saved.credentials.count { it.username == "Retained draft" })
                else if (editor != "apply") assertEquals(1, saved.deviceModels.count { it.name == "Retained draft" })
                else assertEquals("Retained draft", saved.sites.single().devices.single().technicalName)
                storage.importPackageFromFile(local).pkg!!.use { assertEquals(saved, it.project) }
                state.undo()
                assertEquals(before, state.project)
                assertEquals(trash, state.trash)
                assertEquals(undo, state.canUndo)
            }
            compose.onNode(hasSetTextAction() and hasText("Retained draft")).assertDoesNotExist()
        } finally { state.shutdown() }
    }
}
