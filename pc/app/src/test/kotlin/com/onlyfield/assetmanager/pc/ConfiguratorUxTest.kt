package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ConfiguratorUxTest {
    @get:Rule val rule = createComposeRule()
    private val bu = BusinessUnit(name = "Operations")
    private val project = Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(bu))

    @Test fun essentialsCanSaveWithoutPortsOrLocationDetails() {
        val draft = mutableStateOf(inventoryDeviceDraft(project, null))
        rule.setContent { MaterialTheme {
            Column(Modifier.width(412.dp).height(700.dp).verticalScroll(rememberScrollState())) {
                ObjectConfigurator(project, draft.value, Messages()) { draft.value = it }
            }
        } }
        rule.onNodeWithText("Numero di serie").assertDoesNotExist()
        rule.onNodeWithText("Numero porte").assertDoesNotExist()
        rule.onNode(hasSetTextAction() and hasText("Nome oggetto")).performTextInput("SW-01")
        rule.runOnIdle { assertTrue(draft.value.errors(project).containsKey("businessUnitId")) }
        rule.onNode(hasContentDescription("Business unit:", substring = true)).performScrollTo().performClick()
        rule.onNodeWithText("Operations").performClick()
        rule.runOnIdle {
            assertTrue(draft.value.errors(project).isEmpty())
            val device = draft.value.apply(project).businessUnits.single().devices.single()
            assertEquals("SW-01", device.technicalName)
            assertTrue(device.ports.isEmpty())
            assertNull(device.areaId)
            assertNull(device.rackId)
        }
    }

    @Test fun collapsingDetailsRetainsEditsAndUntouchedHardware() {
        val original = Device(technicalName = "SW", serialNumber = "OLD", physicalLabel = "Label", hardware = HardwareSpec(features = listOf("Custom")))
        val p = project.copy(businessUnits = listOf(bu.copy(devices = listOf(original))))
        val draft = mutableStateOf(MapObjectDraft.forDevice(p, original))
        rule.setContent { MaterialTheme {
            Column(Modifier.width(560.dp).height(700.dp).verticalScroll(rememberScrollState())) { ObjectConfigurator(p, draft.value, Messages()) { draft.value = it } }
        } }
        rule.onNodeWithText("Identificativi e rete").performScrollTo().performClick()
        rule.onNode(hasSetTextAction() and hasText("Numero di serie")).performScrollTo().performTextReplacement("NEW")
        rule.onNodeWithText("Identificativi e rete").performScrollTo().performClick()
        rule.onNodeWithText("Numero di serie").assertDoesNotExist()
        rule.onNodeWithText("Identificativi e rete").performClick()
        rule.onNode(hasSetTextAction() and hasText("NEW")).assertExists()
        rule.runOnIdle { assertEquals(original.copy(serialNumber = "NEW"), draft.value.apply(p).businessUnits.single().devices.single()) }
    }

    @Test fun invalidHiddenNetworkFieldAutomaticallyOpensItsSection() {
        val original = Device(technicalName = "SW")
        val p = project.copy(businessUnits = listOf(bu.copy(devices = listOf(original))))
        val draft = mutableStateOf(MapObjectDraft.forDevice(p, original))
        rule.setContent { MaterialTheme {
            Column(Modifier.width(360.dp).height(700.dp).verticalScroll(rememberScrollState())) { ObjectConfigurator(p, draft.value, Messages()) { draft.value = it } }
        } }
        rule.onNodeWithText("IP").assertDoesNotExist()
        rule.runOnIdle { draft.value = draft.value.copy(device = draft.value.device.copy(ipAddress = "invalid")) }
        rule.onNode(hasSetTextAction() and hasText("invalid")).assertExists()
        rule.runOnIdle { assertTrue(draft.value.errors(p).containsKey("ipAddress")) }
    }

    @Test fun selectingModelPreservesOperationalIdentifiers() {
        val model = DeviceModel(name = "Standard switch", category = DeviceCategory.NETWORK_SWITCH, defaultHeightU = 2, portTemplates = listOf(PortTemplate("P", portCount = 4, connector = "RJ45")))
        val original = Device(technicalName = "SW", serialNumber = "KEEP", ipAddress = "192.0.2.1")
        val p = project.copy(deviceModels = listOf(model), businessUnits = listOf(bu.copy(devices = listOf(original))))
        val draft = mutableStateOf(MapObjectDraft.forDevice(p, original))
        rule.setContent { MaterialTheme {
            Column(Modifier.width(560.dp).height(700.dp).verticalScroll(rememberScrollState())) { ObjectConfigurator(p, draft.value, Messages()) { draft.value = it } }
        } }
        rule.onNode(hasContentDescription("Modello:", substring = true)).performClick()
        rule.onNodeWithText("Standard switch").performClick()
        rule.runOnIdle {
            val saved = draft.value.apply(p).businessUnits.single().devices.single()
            assertEquals("SW", saved.technicalName)
            assertEquals("KEEP", saved.serialNumber)
            assertEquals("192.0.2.1", saved.ipAddress)
            assertEquals(2, saved.heightU)
            assertEquals(4, saved.ports.size)
        }
    }

    @Test fun modelSearchFiltersChoicesWithoutChangingTheDraft() {
        val models = (1..12).map { DeviceModel(name = "Modello $it", category = DeviceCategory.NETWORK_SWITCH) }
        val p = project.copy(deviceModels = models)
        val draft = mutableStateOf(inventoryDeviceDraft(p, null))
        rule.setContent { MaterialTheme {
            Column(Modifier.width(412.dp).height(700.dp).verticalScroll(rememberScrollState())) { ObjectConfigurator(p, draft.value, Messages()) { draft.value = it } }
        } }
        val original = draft.value
        rule.onNode(hasContentDescription("Modello:", substring = true)).performClick()
        rule.onNode(hasSetTextAction() and hasText("Cerca")).performTextInput("12")
        rule.onNodeWithText("Modello 1").assertDoesNotExist()
        rule.onNodeWithText("Modello 12").assertIsDisplayed()
        rule.runOnIdle { assertEquals(original, draft.value) }
        rule.onNodeWithText("Modello 12").performClick()
        rule.runOnIdle { assertEquals(models.last().id, draft.value.device.deviceModelId) }
    }

    @Test fun reducingConnectedGroupRequiresExplicitApprovalAndKeepsOtherPortIds() {
        val group = PortTemplate("P", portCount = 2, connector = "RJ45", speed = "1G")
        val original = Device(technicalName = "SW", hardware = HardwareSpec(portGroups = listOf(group))).let {
            it.copy(ports = HardwareConfigurator.ports(listOf(group), it.id))
        }
        val remote = Device(technicalName = "Remote").let { it.copy(ports = listOf(Port(name = "Uplink", deviceId = it.id))) }
        val p = HardwareConfigurator.connect(project.copy(businessUnits = listOf(bu.copy(devices = listOf(original, remote)))),
            original.ports.last().id, remote.ports.single().id, CableMedium.ETHERNET_COPPER)
        val draft = mutableStateOf(MapObjectDraft.forDevice(p, original))
        rule.setContent { MaterialTheme {
            Column(Modifier.width(560.dp).height(700.dp).verticalScroll(rememberScrollState())) {
                ObjectConfigurator(p, draft.value, Messages(), initialSection = ConfiguratorPage.PORTS) { draft.value = it }
            }
        } }
        rule.onNode(hasText("2 × RJ45", substring = true)).performScrollTo().performClick()
        rule.onNode(hasSetTextAction() and hasText("Numero porte")).performScrollTo().performTextReplacement("1")
        val approval = Messages().text("config.removeConnected")
        rule.runOnIdle { assertTrue(draft.value.errors(p).containsKey("ports")) }
        rule.onNode(hasText(approval) and isToggleable()).performScrollTo().performClick()
        rule.onNodeWithText("Porte", useUnmergedTree = true).performScrollTo().performClick()
        rule.runOnIdle {
            assertTrue(draft.value.errors(p).isEmpty())
            val saved = draft.value.apply(p)
            assertEquals(original.ports.first().id, saved.businessUnits.single().devices.first().ports.single().id)
            assertEquals(p.cables.single().id, saved.cables.single().id)
            assertNull(saved.cables.single().portAId)
            assertEquals(remote, saved.businessUnits.single().devices.last())
        }
    }
}
