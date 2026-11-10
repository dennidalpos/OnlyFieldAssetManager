package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.ui.NetworkLogicalSection
import com.onlyfield.assetmanager.pc.ui.components.LocalConfirm
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VlanDeletionUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun referencedVlanShowsErrorAndDoesNotSave() {
        val vlan = Vlan(vlanId = 10, name = "LAN")
        val initial = Project(name = "Network", createdEpochMs = 0, updatedEpochMs = 0,
            vlans = listOf(vlan), subnets = listOf(Subnet(cidrBlock = "10.0.0.0/24", vlanId = vlan.id)))
        var project by mutableStateOf(initial)
        var saves = 0
        val messages = Messages()
        compose.setContent {
            CompositionLocalProvider(LocalMessages provides messages, LocalConfirm provides { it.onConfirm() }) {
                MaterialTheme { NetworkLogicalSection(project, { changed, _ -> project = changed; saves++ }) }
            }
        }
        fun delete() {
            compose.onNodeWithContentDescription(messages.text("ux.more") + " · " + messages.text("text.da4da5c165af", 10)).performClick()
            compose.onNodeWithText(messages.text("text.7efe336bd548")).performClick()
        }
        delete()
        compose.onNodeWithText(messages.text("network.vlanInUse")).assertExists()
        compose.runOnIdle { assertEquals(initial, project); assertEquals(0, saves); project = initial.copy(subnets = initial.subnets.map { it.copy(vlanId = null) }) }
        delete()
        compose.runOnIdle { assertEquals(1, saves); assertTrue(project.vlans.isEmpty()); assertEquals(1, project.subnets.size) }
    }
}
