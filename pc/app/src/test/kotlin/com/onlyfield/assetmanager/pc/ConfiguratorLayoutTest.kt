package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.ObjectConfigurator
import com.onlyfield.assetmanager.configurator.unitRanges
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ConfiguratorLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val area = Area(name = "Terra")
    private val rack = Rack(name = "R1", areaId = area.id, heightU = 42)
    private val sw = Device(technicalName = "SW-01", areaId = area.id, rackId = rack.id, positionU = 10, heightU = 2, ipAddress = "10.0.0.2", objectTypeId = "switch")
    private val loose = Device(technicalName = "SW-09", areaId = area.id, rackId = rack.id, objectTypeId = "switch")
    private val bu = BusinessUnit(name = "BU", areas = listOf(area), devices = listOf(sw, loose))
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(bu), racks = listOf(rack))

    private fun show(draft: MapObjectDraft, extra: @androidx.compose.runtime.Composable () -> Unit = {}) {
        val state = mutableStateOf(draft)
        rule.setContent { MaterialTheme {
            Column(Modifier.width(560.dp).height(800.dp).verticalScroll(rememberScrollState())) {
                ObjectConfigurator(project, state.value, Messages(), extraSections = extra) { state.value = it }
            }
        } }
    }

    @Test fun collapsedSectionsSummarizeTheirDataAndContextShowsThePlace() {
        show(MapObjectDraft.device(project, bu.id, area.id, sw.id))
        rule.onNodeWithText("Switch · Terra › R1 › U10–11").assertExists()
        rule.onNodeWithText("10.0.0.2").assertExists()
        rule.onNode(hasSetTextAction() and hasText("Indirizzo IP")).assertDoesNotExist()
        rule.onNodeWithText("Dati essenziali").assertExists()
    }

    @Test fun hostSectionsComeBeforeCustomFieldsAndAdvancedOptions() {
        show(MapObjectDraft.device(project, bu.id, area.id, sw.id)) { Text("HOST-SECTION") }
        fun top(text: String) = rule.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y
        assertTrue(top("Note e rilievo") < top("HOST-SECTION"))
        assertTrue(top("HOST-SECTION") < top("Campi personalizzati"))
        assertTrue(top("Campi personalizzati") < top("Opzioni avanzate"))
    }

    @Test fun rackContentsListOnlyMountedDevicesAndFreeRanges() {
        show(MapObjectDraft.rack(project, bu.id, area.id, rack.id))
        rule.onNodeWithText("Dispositivi nel rack").performScrollTo().performClick()
        rule.onNodeWithText("U10–11").assertExists()
        rule.onNodeWithText("U libere: 1–9, 12–42").assertExists()
        rule.onNodeWithText("U5").assertDoesNotExist()
        // In the rack without a unit: still listed, so the summary count matches.
        rule.onNodeWithText("Senza posizione U").assertExists()
        rule.onNodeWithText("SW-09").assertExists()
    }

    @Test fun unitRangesCollapseConsecutiveUnits() {
        assertEquals("1–3, 5, 7–8", unitRanges(listOf(8, 1, 2, 3, 5, 7)))
        assertEquals("", unitRanges(emptyList()))
    }
}
