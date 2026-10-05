package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.configurator.PortQuickActions
import com.onlyfield.assetmanager.configurator.PortQuickDialog
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuickCablingUiTest {
    @get:Rule val rule = createComposeRule()

    private fun device(name: String) = Device(technicalName = name, hardware = HardwareSpec(portGroups = listOf(PortTemplate("P", portCount = 4))))
        .let { it.copy(ports = HardwareConfigurator.ports(it.hardware.portGroups, it.id)) }
    private val a = device("A")
    private val b = device("B")
    private val project = mutableStateOf(Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "Sede", devices = listOf(a, b)))))

    private val shots = mutableListOf<Pair<AttachmentTargetType, String>>()

    private fun open() {
        rule.setContent { MaterialTheme { PortQuickDialog(project.value, a.ports.first().id, Messages(), PortQuickActions(update = { p, _ -> project.value = p }, photo = { t, id -> shots += t to id }), onClose = {}) } }
        rule.onNodeWithText("Collega a…").performClick()
        rule.onNodeWithText("B").performClick()
        rule.onAllNodesWithContentDescription("P1: Libera").onLast().performClick()
    }

    @Test fun seriesConnectsConsecutivePortsWithLabels() {
        open()
        rule.onNode(hasContentDescription("Porte in serie:", substring = true)).performClick()
        rule.onNodeWithText("4 porte").performClick()
        rule.onNodeWithText("Collega 4 porte").performClick()
        rule.runOnIdle {
            val cables = project.value.cables
            assertEquals(4, cables.size)
            assertEquals(listOf("A/P1 – B/P1", "A/P2 – B/P2", "A/P3 – B/P3", "A/P4 – B/P4"), cables.map { it.codeOrLabel })
        }
    }

    @Test fun continuousModeProposesTheNextPair() {
        open()
        rule.onNodeWithText("Poi passa alla porta successiva").performClick()
        rule.onNodeWithText("Collega").performClick()
        // The card moved to A › P2 with B › P2 already chosen: one more tap connects it.
        rule.onNodeWithText("A › P2").assertExists()
        rule.onNodeWithText("Collega").performClick()
        rule.runOnIdle {
            assertEquals(2, project.value.cables.size)
            assertEquals(ConnectionState.COMPLETE, ConnectionGraph(project.value).state(a.ports[1].id))
        }
    }

    @Test fun connectingOffersTheCablePhotoAtOnce() {
        open()
        rule.onNodeWithText("Collega").performClick()
        rule.onNodeWithText("Collegato. Fotografa ora il cavo o la porta.").assertExists()
        rule.onNodeWithText("Foto cavo").performClick()
        rule.runOnIdle { assertEquals(listOf(AttachmentTargetType.CABLE to project.value.cables.single().id), shots) }
    }

    @Test fun continuousModeKeepsThePreviousCablePhotoAtHand() {
        open()
        rule.onNodeWithText("Poi passa alla porta successiva").performClick()
        rule.onNodeWithText("Collega").performClick()
        rule.onNodeWithText("Collegato: A/P1 – B/P1").assertExists()
        rule.onNodeWithText("Foto cavo").performClick()
        rule.runOnIdle { assertEquals(AttachmentTargetType.CABLE, shots.single().first) }
    }
}
