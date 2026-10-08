package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.configurator.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VisualConfiguratorUiTest {
    @get:Rule val rule = createComposeRule()
    private fun device(name: String, type: String = "switch") = Device(technicalName = name, objectTypeId = type,
        category = if (type == "patch-panel") DeviceCategory.PATCH_PANEL else DeviceCategory.NETWORK_SWITCH,
        hardware = HardwareSpec(passive = type == "patch-panel", portGroups = DevicePresets.forType(type)!!.result(mapOf("ports" to "4", "uplinks" to "0")).groups))
        .let { it.copy(ports = HardwareConfigurator.ports(it.hardware.portGroups, it.id)) }
    private val sw = device("SW")
    private val original = Project(name = "Visual", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "Site", devices = listOf(sw))))
    private val state = mutableStateOf(original)
    private fun open() {
        rule.setContent { MaterialTheme { PortQuickDialog(state.value, sw.ports.first().id, Messages(), PortQuickActions({ p, _ -> state.value = p }), {}) } }
        rule.onNodeWithText("Configura apparato").performScrollTo().performClick()
    }
    private fun reorder() {
        rule.onNodeWithText("Modifica disposizione").performScrollTo().performClick()
        rule.onNodeWithContentDescription("P1: Libera").performScrollTo().performClick()
        rule.onNodeWithContentDescription("P4: Libera").performClick()
    }

    @Test fun rearrangingRequiresSaveAndPreservesPortIds() {
        open(); reorder()
        rule.runOnIdle { assertEquals(original, state.value) }
        rule.onNodeWithText("Salva modifiche").assertIsDisplayed().performClick()
        rule.runOnIdle {
            val updated = state.value.sites.single().devices.single()
            assertEquals(listOf("4", "2", "3", "1"), updated.hardware.portLayouts.single().order)
            assertEquals(sw.ports.map { it.id }, updated.ports.map { it.id })
        }
    }

    @Test fun cancellingDiscardsTheStagedLayout() {
        open(); reorder()
        rule.onNodeWithText("Indietro").performClick()
        rule.onNodeWithText("Annullare le modifiche?").assertIsDisplayed()
        rule.onNodeWithText("Annulla modifiche").performClick()
        rule.runOnIdle { assertEquals(original, state.value) }
    }

    @Test fun poeCapabilityIsChosenOnTheDrawingAndSavedExplicitly() {
        open()
        rule.onNodeWithText("Porte PoE").performScrollTo().performClick()
        rule.onNodeWithContentDescription("P3: Libera").performScrollTo().performClick()
        rule.onNodeWithText("Assegna supporto PoE").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(original, state.value) }
        rule.onNodeWithText("Salva modifiche").performClick()
        rule.runOnIdle {
            val ports = state.value.sites.single().devices.single().ports
            assertEquals(listOf("P3"), ports.filter { it.hardware.poeStandard != null }.map { it.name })
        }
    }

    @Test fun fixedCableFromAnOccupiedFrontConnectsTheRearPorts() {
        val a = device("Panel A", "patch-panel")
        val b = device("Panel B", "patch-panel")
        val p = original.copy(sites = listOf(Site(name = "Site", devices = listOf(a, b, sw))))
        val paired = ProjectEdits.withInternalPassages(ProjectEdits.withInternalPassages(p, a.ports), b.ports)
        val frontA = a.ports.first { it.hardware.side == PortSide.FRONT }
        val frontB = b.ports.first { it.hardware.side == PortSide.FRONT }
        state.value = HardwareConfigurator.connect(paired, frontA.id, sw.ports.first().id, CableMedium.ETHERNET_COPPER)
        rule.setContent { MaterialTheme { PortQuickDialog(state.value, frontA.id, Messages(), PortQuickActions({ updated, _ -> state.value = updated }), {}) } }
        rule.onNodeWithText("Tratta fissa sui retro").performScrollTo().performClick()
        rule.onNodeWithText("Panel B").performClick()
        rule.onNodeWithContentDescription("P1: Libera").performClick()
        rule.onNodeWithText("Porte fisiche: Panel A › P1 · Retro → Panel B › P1 · Retro").assertExists()
        rule.onNodeWithText("Collega").performClick()
        rule.runOnIdle {
            val cable = state.value.cables.last()
            assertEquals(PassiveCabling.rear(state.value, frontA.id)?.id, cable.portAId)
            assertEquals(PassiveCabling.rear(state.value, frontB.id)?.id, cable.portBId)
            assertEquals(2, state.value.cables.size)
        }
        rule.onNodeWithText("Retro").performScrollTo().performClick()
        rule.onNode(hasContentDescription("P1: Occupata", substring = true)).performScrollTo().assertIsDisplayed()

    }
}
