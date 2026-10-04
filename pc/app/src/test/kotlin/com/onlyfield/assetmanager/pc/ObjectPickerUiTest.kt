package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.configurator.map.ObjectPickerDialog
import com.onlyfield.assetmanager.core.forms.PresetResult
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ObjectPickerUiTest {
    @get:Rule val rule = createComposeRule()
    private val area = Area(name = "Terra")
    private val rack = Rack(name = "R1", areaId = area.id)
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1,
        businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area))), racks = listOf(rack))

    @Test fun titleSaysWherePresetStepHasOnePrimaryAndManualPortsLink() {
        var picked: Pair<ObjectType, PresetResult?>? = null
        rule.setContent { MaterialTheme { ObjectPickerDialog(project, Messages(), area.id, null, {}, { t, p -> picked = t to p }, {}) } }
        rule.onNodeWithText("Sul piano Terra · Scegli la tipologia").assertIsDisplayed()
        rule.onNodeWithText("Cerca tipologia…").performTextInput("switch")
        rule.onNodeWithText("Switch").performClick()
        rule.onNodeWithText("Sul piano Terra · Scegli le porte").assertIsDisplayed()
        rule.onNodeWithText("Continua").assertIsDisplayed()
        rule.onNodeWithText("Indietro").assertIsDisplayed()
        rule.onNodeWithText("Configura le porte manualmente").performClick()
        rule.runOnIdle { assertEquals("switch" to null, picked?.let { it.first.id to it.second }) }
    }

    @Test fun insideAContainerCablesAreHiddenAndCustomTypeUsesTheDialogButton() {
        var created: ObjectType? = null
        rule.setContent { MaterialTheme { ObjectPickerDialog(project, Messages(), area.id, ObjectRef(PlacementTargetType.RACK, rack.id), {}, { _, _ -> }, { created = it }) } }
        rule.onNodeWithText("In R1 · Scegli la tipologia").assertIsDisplayed()
        rule.onNodeWithText("Cavi").assertDoesNotExist()
        rule.onNodeWithText("+ Tipologia personalizzata").performClick()
        rule.onNodeWithText("Crea e usa").assertIsNotEnabled()
        rule.onNodeWithText("Nome tipologia *").performTextInput("Gateway LoRa")
        rule.onNodeWithText("Crea e usa").assertIsEnabled().performClick()
        rule.runOnIdle { assertEquals("Gateway LoRa", created?.name) }
    }
}
