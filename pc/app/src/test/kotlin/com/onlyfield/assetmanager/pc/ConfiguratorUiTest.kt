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
import com.onlyfield.assetmanager.configurator.PortPanel
import com.onlyfield.assetmanager.core.forms.PortLogic
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

    @Test fun portPanelExposesPortActionsAndFreeStates() {
        val original = Device(technicalName = "SW", hardware = HardwareSpec(portGroups = listOf(PortTemplate("P", portCount = 24), PortTemplate("SFP", portCount = 4))))
        val device = original.copy(ports = HardwareConfigurator.ports(original.hardware.portGroups, original.id))
        val p = Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "BU", devices = listOf(device))))
        var selected: String? = null
        rule.setContent { MaterialTheme { PortPanel(PortLogic.panel(p, device), Messages(), onClick = { selected = it.port.id }) } }
        rule.onAllNodes(hasClickAction() and hasContentDescription("Libera", substring = true)).assertCountEquals(28)
        rule.onNodeWithText("Legenda").assertHasClickAction()
        rule.onNodeWithContentDescription("P1: Libera").performClick()
        rule.runOnIdle { assertEquals(device.ports.first().id, selected) }
        rule.onAllNodes(hasContentDescription("Libera", substring = true)).assertCountEquals(28)
    }

    @Test fun connectedPortsShowOccupiedAndPeer() {
        val a = Device(technicalName = "A").let { it.copy(ports = listOf(Port(deviceId = it.id, name = "P1"))) }
        val b = Device(technicalName = "B").let { it.copy(ports = listOf(Port(deviceId = it.id, name = "P1"))) }
        val p = HardwareConfigurator.connect(Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "BU", devices = listOf(a, b)))), a.ports.single().id, b.ports.single().id, CableMedium.ETHERNET_COPPER)
        rule.setContent { MaterialTheme { PortPanel(PortLogic.panel(p, a), Messages(), onClick = {}) } }
        rule.onNodeWithContentDescription("P1: Occupata, B › P1, senza foto").assertHasClickAction()
    }

    @Test fun selectingDestinationStagesConnectionAndSaveKeepsPortIds() {
        val a = Device(technicalName = "A", hardware = HardwareSpec(portGroups = listOf(PortTemplate("P", portCount = 1)))).let { it.copy(ports = HardwareConfigurator.ports(it.hardware.portGroups, it.id)) }
        val b = a.copy(id = java.util.UUID.randomUUID().toString(), technicalName = "B").let { it.copy(ports = HardwareConfigurator.ports(it.hardware.portGroups, it.id)) }
        val project = Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "BU", devices = listOf(a, b))))
        val draft = mutableStateOf(MapObjectDraft.forDevice(project, a))
        rule.setContent { MaterialTheme { Column(Modifier.width(800.dp).height(700.dp).testTag("editor").verticalScroll(rememberScrollState())) { ObjectConfigurator(project, draft.value, Messages()) { draft.value = it } } } }
        rule.onNodeWithText("Porte").performScrollTo().performClick()
        rule.onNodeWithContentDescription("P1: Libera").performScrollTo()
        val portNode = rule.onNodeWithContentDescription("P1: Libera").fetchSemanticsNode()
        val editorBounds = rule.onNodeWithTag("editor").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("editor").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, portNode.positionInRoot.y + portNode.size.height / 2f - editorBounds.center.y) }
        rule.mainClock.advanceTimeBy(1000)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("P1: Libera").assertIsDisplayed()
        rule.onNodeWithContentDescription("P1: Libera").performClick()
        rule.runOnIdle { org.junit.Assert.assertNotNull(draft.value.session) }
        // Quick dialog: Connect to… → device → free port → Connect.
        rule.onNodeWithText("Collega a…").performClick()
        rule.onNodeWithText("B").performClick()
        rule.onAllNodesWithContentDescription("P1: Libera").onLast().performClick()
        rule.onNode(hasText("Etichetta cavo") and hasSetTextAction()).assertExists()
        rule.onNodeWithText("Collega").performClick()
        rule.onNodeWithText("Chiudi").performClick()
        rule.onNodeWithContentDescription("P1: Occupata, B › P1, senza foto").assertHasClickAction()
        rule.runOnIdle {
            val saved = draft.value.apply(project)
            assertEquals(1, saved.cables.size)
            assertEquals(a.ports.single().id, saved.cables.single().portAId)
            assertEquals(b.ports.single().id, saved.cables.single().portBId)
            assertEquals(ConnectionState.COMPLETE, ConnectionGraph(saved).state(a.ports.single().id))
            assertEquals(0, project.cables.size)
        }
    }

    @Test fun presetCreatesPortsAndSelectedPortsReceiveVlan() {
        val area = Area(name = "Terra")
        val site = Site(name = "BU", areas = listOf(area))
        val project = Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(site))
        val draft = mutableStateOf(MapObjectDraft(type = ObjectCatalog.builtins.first { it.id == "switch" }, siteId = site.id, areaId = area.id).let { it.copy(device = it.device.copy(technicalName = "SW-01")) })
        rule.setContent { MaterialTheme { Column(Modifier.width(800.dp).height(900.dp).verticalScroll(rememberScrollState())) {
            ObjectConfigurator(project, draft.value, Messages(), initialSection = com.onlyfield.assetmanager.configurator.ConfiguratorPage.PORTS) { draft.value = it }
        } } }
        rule.onNodeWithText("Applica preset").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(28, HardwareConfigurator.ports(draft.value.device.hardware.portGroups, "x").size) }
        rule.onNodeWithContentDescription("P1: Libera").performScrollTo().performSemanticsAction(SemanticsActions.OnLongClick)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("P2: Libera").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("Torna all'oggetto").assertDoesNotExist()
        rule.onAllNodes(isSelected()).assertCountEquals(2)
        rule.onNodeWithText("2 porte selezionate").assertExists()
        // Editing another field rebuilds the preview: the selection must keep pointing at real ports.
        rule.runOnIdle { draft.value = draft.value.copy(device = draft.value.device.copy(ipAddress = "10.0.0.2")) }
        rule.onAllNodes(isSelected()).assertCountEquals(2)
        rule.onNode(hasSetTextAction() and hasText("VLAN (1–4094)")).performScrollTo().performTextInput("20")
        rule.onNodeWithText("Applica VLAN").performScrollTo().performClick()
        rule.runOnIdle {
            val saved = draft.value.apply(project)
            assertEquals(listOf(20), saved.vlans.map { it.vlanId })
            assertEquals(2, saved.portVlanMemberships.size)
            val ports = saved.sites.single().devices.single().ports.map { it.id }.toSet()
            org.junit.Assert.assertTrue(saved.portVlanMemberships.all { it.portId in ports })
        }
    }
}
