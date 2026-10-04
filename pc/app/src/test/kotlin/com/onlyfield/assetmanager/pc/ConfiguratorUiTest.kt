package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.configurator.DeviceDrawing
import com.onlyfield.assetmanager.configurator.ObjectConfigurator
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConfiguratorUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun drawingsExposePortActionsAndAvailableStates() {
        val original = Device(technicalName = "SW", hardware = HardwareSpec(portGroups = listOf(PortTemplate("P", portCount = 24), PortTemplate("SFP", portCount = 4))))
        val device = original.copy(ports = HardwareConfigurator.ports(original.hardware.portGroups, original.id))
        val p = Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(BusinessUnit(name = "BU", devices = listOf(device))))
        var selected: String? = null
        rule.setContent { MaterialTheme { DeviceDrawing(device, ConnectionGraph(p), PortSide.FRONT, 1f, null, Messages()) { selected = it.id } } }
        rule.onAllNodes(hasClickAction()).assertCountEquals(28)
        rule.onNodeWithContentDescription("SW P1").performClick()
        rule.runOnIdle { assertEquals(device.ports.first().id, selected) }
        rule.onAllNodesWithText("Disponibile").assertCountEquals(28)
    }

    @Test fun connectedPortsExposeCompleteState() {
        val a = Device(technicalName = "A").let { it.copy(ports = listOf(Port(deviceId = it.id, name = "P1"))) }
        val b = Device(technicalName = "B").let { it.copy(ports = listOf(Port(deviceId = it.id, name = "P1"))) }
        val p = HardwareConfigurator.connect(Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(BusinessUnit(name = "BU", devices = listOf(a, b)))), a.ports.single().id, b.ports.single().id, CableMedium.ETHERNET_COPPER)
        rule.setContent { MaterialTheme { DeviceDrawing(a, ConnectionGraph(p), PortSide.FRONT, 1f, null, Messages()) {} } }
        rule.onNodeWithText("Percorso completo").assertIsDisplayed()
        rule.onNodeWithContentDescription("A P1").assertHasClickAction()
    }

    @Test fun selectingDestinationStagesConnectionAndSaveKeepsPortIds() {
        val a = Device(technicalName = "A", hardware = HardwareSpec(portGroups = listOf(PortTemplate("P", portCount = 1)))).let { it.copy(ports = HardwareConfigurator.ports(it.hardware.portGroups, it.id)) }
        val b = a.copy(id = java.util.UUID.randomUUID().toString(), technicalName = "B").let { it.copy(ports = HardwareConfigurator.ports(it.hardware.portGroups, it.id)) }
        val project = Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(BusinessUnit(name = "BU", devices = listOf(a, b))))
        val draft = mutableStateOf(MapObjectDraft.forDevice(project, a))
        rule.setContent { MaterialTheme { Column(Modifier.width(800.dp).height(700.dp).testTag("editor").verticalScroll(rememberScrollState())) { ObjectConfigurator(project, draft.value, Messages()) { draft.value = it } } } }
        rule.onNodeWithText("Porte").performScrollTo().performClick()
        rule.onNodeWithText("A · 1U").performScrollTo()
        val portNode = rule.onNodeWithContentDescription("A P1").fetchSemanticsNode()
        val editorBounds = rule.onNodeWithTag("editor").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("editor").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, portNode.positionInRoot.y + portNode.size.height / 2f - editorBounds.center.y) }
        rule.mainClock.advanceTimeBy(1000)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("A P1").assertIsDisplayed()
        rule.onNodeWithContentDescription("A P1").performClick()
        rule.runOnIdle { org.junit.Assert.assertNotNull(draft.value.session) }
        rule.onNode(hasText("Porta di destinazione:", substring = true)).performScrollTo().performClick()
        rule.onNode(hasText("B › P1", substring = true)).performClick()
        rule.onNodeWithText("Torna all'oggetto").performScrollTo().performClick()
        rule.onNodeWithContentDescription("A P1").assertHasClickAction()
        rule.runOnIdle {
            val saved = draft.value.apply(project)
            assertEquals(1, saved.cables.size)
            assertEquals(a.ports.single().id, saved.cables.single().portAId)
            assertEquals(b.ports.single().id, saved.cables.single().portBId)
            assertEquals(ConnectionState.COMPLETE, ConnectionGraph(saved).state(a.ports.single().id))
            assertEquals(0, project.cables.size)
        }
    }
}
