package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.onboarding.*
import com.onlyfield.assetmanager.pc.ui.ContainerBrowser
import com.onlyfield.assetmanager.pc.ui.components.DetailChangeHost
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ContainerUiTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val folder = TemporaryFolder()
    private val area = Area(name = "Terra")
    private val rack = Rack(name = "R1", areaId = area.id)
    private val type = ObjectType(name = "Scatola", canContainObjects = true)
    private val box = Device(technicalName = "BOX", rackId = rack.id, areaId = area.id, objectTypeId = type.id)
    private val sw = Device(technicalName = "SW", rackId = rack.id, areaId = area.id)
    private val outside = Device(technicalName = "AP", areaId = area.id)

    private fun state(): DesktopAppState {
        val state = DesktopAppState(DesktopStorageManager(folder.newFolder()))
        state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area))))))
        val p = state.project!!
        val withDevices = p.copy(racks = listOf(rack), objectTypes = listOf(type), businessUnits = listOf(p.businessUnits.single().copy(devices = listOf(box, sw, outside))))
        state.update(ObjectHierarchy.assign(withDevices, ObjectRef(PlacementTargetType.DEVICE, sw.id), ObjectRef(PlacementTargetType.DEVICE, box.id)), "Oggetti")
        rule.setContent { MaterialTheme { DetailChangeHost(state.detailSlot) {
            ContainerBrowser(state, ObjectRef(PlacementTargetType.RACK, rack.id), state.project!!.businessUnits.single().id, area.id) {}
        } } }
        return state
    }

    @Test fun nestedNavigationReadsSavedChildChangesAndKeepsTheParent() {
        val state = state()
        try {
            rule.onNodeWithText("BOX").performClick()
            rule.onNodeWithText("R1 › BOX").assertIsDisplayed()
            rule.onNodeWithText("SW").performClick()
            rule.onNodeWithText("R1 › BOX › SW").assertIsDisplayed()
            rule.onNodeWithText("Modifica oggetto").performClick()
            rule.onNode(hasSetTextAction() and hasText("SW")).performTextReplacement("SW aggiornato")
            rule.onNodeWithText("Salva modifiche").performClick()
            rule.onNodeWithText("R1 › BOX › SW aggiornato").assertIsDisplayed()
            rule.onNodeWithText("Indietro").performClick()
            rule.onNodeWithText("SW aggiornato").assertIsDisplayed()
            rule.runOnIdle { assertFalse(state.detailSlot.dirty) }
        } finally { state.shutdown() }
    }

    @Test fun assigningAndRemovingAnExistingObjectUpdatesTheFloorProjection() {
        val state = state()
        try {
            rule.onNodeWithText("Assegna esistente").performClick()
            rule.onNodeWithText("AP").performClick()
            rule.runOnIdle { assertFalse(ObjectMap.nodes(state.project!!, area.id).any { it.id == outside.id }) }
            rule.onAllNodesWithText("Rimuovi dal contenitore")[1].performClick()
            rule.runOnIdle { assertTrue(ObjectMap.nodes(state.project!!, area.id).any { it.id == outside.id }) }
        } finally { state.shutdown() }
    }

    @Test fun globalSectionChangeKeepsNestedDraftUntilDiscard() {
        val state = state()
        try {
            rule.onNodeWithText("BOX").performClick()
            rule.onNodeWithText("Modifica oggetto").performClick()
            rule.onNode(hasSetTextAction() and hasText("BOX")).performTextReplacement("Bozza")
            rule.runOnIdle { state.section = AppSection.INVENTORY }
            rule.onNodeWithText("Continua a modificare").performClick()
            rule.onNode(hasSetTextAction() and hasText("Bozza")).assertExists()
            rule.runOnIdle { state.section = AppSection.INVENTORY }
            rule.onNodeWithText("Scarta").performClick()
            rule.runOnIdle {
                assertEquals(AppSection.INVENTORY, state.section)
                assertEquals("BOX", state.project!!.businessUnits.single().devices.first { it.id == box.id }.technicalName)
            }
        } finally { state.shutdown() }
    }
}
