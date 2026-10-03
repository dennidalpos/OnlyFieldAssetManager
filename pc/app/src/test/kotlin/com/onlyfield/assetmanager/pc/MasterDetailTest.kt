package com.onlyfield.assetmanager.pc

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import com.onlyfield.assetmanager.pc.ui.components.EditPanel
import com.onlyfield.assetmanager.pc.ui.components.FormField
import com.onlyfield.assetmanager.pc.ui.components.MasterDetailHost
import com.onlyfield.assetmanager.pc.ui.components.LocalDetailChange
import com.onlyfield.assetmanager.pc.ui.components.SubTabs
import com.onlyfield.assetmanager.pc.ui.InventorySection
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MasterDetailTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun panelShowsEditorAndAsksBeforeDiscardingChanges() {
        var saved = ""
        rule.setContent {
            var open by remember { mutableStateOf(true) }
            var name by remember { mutableStateOf("SW-01") }
            MasterDetailHost {
                Text(if (open) "lista" else "chiuso")
                if (open) EditPanel("Modifica apparato", { open = false }, { saved = name; open = false }) {
                    FormField(name, { name = it }, "Nome")
                }
            }
        }
        rule.onNodeWithText("Modifica apparato").assertIsDisplayed()
        rule.onNodeWithText("SW-01").performTextReplacement("SW-02")
        rule.onNodeWithText("Annulla").performClick()
        rule.onNodeWithText("Scartare le modifiche?").assertIsDisplayed()
        rule.onNodeWithText("Continua a modificare").performClick()
        rule.onNodeWithText("Salva").performClick()
        rule.onNodeWithText("chiuso").assertIsDisplayed()
        assertEquals("SW-02", saved)
    }

    @Test
    fun switchingSameTitleEditorsKeepsDraftUntilDiscardIsConfirmed() {
        rule.setContent {
            MasterDetailHost {
                val changeDetail = LocalDetailChange.current
                var selected by remember { mutableStateOf(1) }
                var open by remember { mutableStateOf(true) }
                var name by remember(selected) { mutableStateOf("Apparato $selected") }
                TextButton(onClick = { changeDetail { selected = 2; open = true } }) { Text("Secondo") }
                if (open) EditPanel("Modifica apparato", { open = false }, { open = false }) {
                    FormField(name, { name = it }, "Nome")
                }
            }
        }
        rule.onNodeWithText("Apparato 1").performTextReplacement("Bozza")
        rule.onNodeWithText("Secondo").performClick()
        rule.onNodeWithText("Scartare le modifiche?").assertIsDisplayed()
        rule.onNodeWithText("Continua a modificare").performClick()
        rule.onNodeWithText("Bozza").assertIsDisplayed()
        rule.onNodeWithText("Secondo").performClick()
        rule.onNodeWithText("Scarta").performClick()
        rule.onNodeWithText("Apparato 2").assertIsDisplayed()
        rule.onNodeWithText("Annulla").performClick()
        rule.onNodeWithText("Scartare le modifiche?").assertDoesNotExist()
    }

    @Test
    fun inventorySwitchAndNewDeviceRespectUnsavedChanges() {
        var project by mutableStateOf(Project(
            id = "project", name = "Sito", createdEpochMs = 1, updatedEpochMs = 1,
            businessUnits = listOf(BusinessUnit(
                id = "bu", name = "BU", devices = listOf(
                    Device(id = "first", technicalName = "SW-01", category = DeviceCategory.NETWORK_SWITCH),
                    Device(id = "second", technicalName = "SW-02", category = DeviceCategory.NETWORK_SWITCH)
                )
            ))
        ))
        rule.setContent {
            MasterDetailHost {
                InventorySection(project, { updated, _ -> project = updated }, {})
            }
        }
        rule.onAllNodesWithText("Modifica")[0].performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNode(hasSetTextAction() and hasText("SW-01")).performTextReplacement("SW-01 modificato")
        rule.onNodeWithText("Cerca nome, etichetta, IP, MAC, seriale…").performTextInput("SW-02")
        rule.onNodeWithText("Modifica").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithText("Continua a modificare").performClick()
        rule.onNode(hasSetTextAction() and hasText("SW-01 modificato")).assertExists()
        rule.onNodeWithText("+ Nuovo apparato").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithText("Continua a modificare").performClick()
        rule.onNodeWithText("Salva").performClick()
        assertEquals("SW-01 modificato", project.businessUnits.single().devices[0].technicalName)
        assertEquals("SW-02", project.businessUnits.single().devices[1].technicalName)
        rule.onNodeWithText("Modifica").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNode(hasSetTextAction() and hasText("SW-02") and hasText("Nome tecnico *")).assertExists()
        rule.onNodeWithText("Scartare le modifiche?").assertDoesNotExist()
        rule.onNode(hasSetTextAction() and hasText("SW-02") and hasText("Nome tecnico *")).performTextReplacement("Bozza secondo")
        rule.onNodeWithText("Modifica").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithText("Scarta").performClick()
        rule.onNode(hasSetTextAction() and hasText("SW-02") and hasText("Nome tecnico *")).assertExists()
        rule.onNodeWithText("Bozza secondo").assertDoesNotExist()
    }

    @Test
    fun switchingSubTabsRequiresDiscardingTheDraft() {
        rule.setContent {
            MasterDetailHost {
                var selected by remember { mutableStateOf(0) }
                var open by remember { mutableStateOf(true) }
                var name by remember { mutableStateOf("Cavo") }
                SubTabs(listOf("Cavi", "Percorsi"), selected, { selected = it })
                if (open && selected == 0) EditPanel("Modifica cavo", { open = false }, { open = false }) {
                    FormField(name, { name = it }, "Nome")
                }
            }
        }
        rule.onNodeWithText("Cavo").performTextReplacement("Bozza cavo")
        rule.onNodeWithText("Percorsi").performClick()
        rule.onNodeWithText("Continua a modificare").performClick()
        rule.onNodeWithText("Bozza cavo").assertIsDisplayed()
        rule.onNodeWithText("Percorsi").performClick()
        rule.onNodeWithText("Scarta").performClick()
        rule.onNodeWithText("Modifica cavo").assertDoesNotExist()
    }
}
