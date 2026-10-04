package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.configurator.map.MapObjectPicker
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
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

    @Test fun presetTypeShowsPrefilledMenusAndAddSavesAtOnce() {
        var added: MapObjectDraft? = null
        rule.setContent { MaterialTheme { MapObjectPicker(project, Messages(), area.id, null, null, {}, { added = it }, {}) } }
        rule.onNodeWithText("Sul piano Terra").assertIsDisplayed()
        rule.onNodeWithText("Cerca tipologia…").performTextInput("switch")
        rule.onNodeWithText("Switch").performClick()
        rule.onNodeWithText("SW-01").assertIsDisplayed()
        rule.onNodeWithText("Aggiungi e modifica").assertIsDisplayed()
        rule.onNodeWithText("Indietro").assertIsDisplayed()
        rule.onNodeWithText("Aggiungi").performClick()
        rule.runOnIdle {
            assertEquals("SW-01", added?.device?.technicalName)
            assertTrue(added!!.device.hardware.portGroups.isNotEmpty())
            assertTrue(added!!.errors(project).isEmpty())
        }
    }

    @Test fun typeWithoutMenusIsAddedWithOneTap() {
        var added: MapObjectDraft? = null
        rule.setContent { MaterialTheme { MapObjectPicker(project, Messages(), area.id, null, null, {}, { added = it }, {}) } }
        rule.onNodeWithText("Cerca tipologia…").performTextInput("modem")
        rule.onNodeWithText("Modem").performClick()
        rule.runOnIdle { assertEquals("MD-01", added?.device?.technicalName) }
    }

    @Test fun insideAContainerCablesAreHiddenAndCustomTypeContinuesToAdd() {
        var added: MapObjectDraft? = null
        rule.setContent { MaterialTheme { MapObjectPicker(project, Messages(), area.id, ObjectRef(PlacementTargetType.RACK, rack.id), null, {}, { added = it }, {}) } }
        rule.onNodeWithText("In R1").assertIsDisplayed()
        rule.onNodeWithText("Cavi").assertDoesNotExist()
        rule.onNodeWithText("+ Tipologia personalizzata").performClick()
        rule.onNodeWithText("Continua").assertIsNotEnabled()
        rule.onNodeWithText("Nome tipologia *").performTextInput("Gateway LoRa")
        rule.onNodeWithText("Continua").assertIsEnabled().performClick()
        rule.onNodeWithText("Aggiungi").performClick()
        rule.runOnIdle {
            assertEquals("Gateway LoRa", added?.type?.name)
            assertTrue(added!!.apply(project).objectTypes.any { it.name == "Gateway LoRa" })
        }
    }
}
