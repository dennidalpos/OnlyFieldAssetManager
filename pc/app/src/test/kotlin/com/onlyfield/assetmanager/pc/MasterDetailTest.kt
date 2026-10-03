package com.onlyfield.assetmanager.pc

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.onlyfield.assetmanager.pc.ui.components.EditPanel
import com.onlyfield.assetmanager.pc.ui.components.FormField
import com.onlyfield.assetmanager.pc.ui.components.MasterDetailHost
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
}
