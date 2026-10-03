package com.onlyfield.assetmanager.pc

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.pc.ui.components.FormDialog
import com.onlyfield.assetmanager.pc.ui.components.FormField
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class LocalizedUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun defaultFormActionsFollowTheLocaleAndUserTextIsPreserved() {
        val messages = mutableStateOf(Messages())
        val value = mutableStateOf("Nombre dell'utente")
        rule.setContent {
            CompositionLocalProvider(LocalMessages provides messages.value) {
                FormDialog("USER_TITLE", {}, {}) { FormField(value.value, { value.value = it }, "USER_FIELD") }
            }
        }
        listOf("it" to "Salva", "en" to "Save", "es" to "Guardar").forEach { (language, action) ->
            rule.runOnIdle { messages.value = Messages(Locale.forLanguageTag(language)) }
            rule.onNodeWithText(action).assertIsDisplayed()
            rule.onNodeWithText("Nombre dell'utente").assertIsDisplayed()
            rule.onNodeWithText("USER_TITLE").assertIsDisplayed()
        }
    }
}
