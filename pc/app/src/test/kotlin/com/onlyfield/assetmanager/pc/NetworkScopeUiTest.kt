package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.ui.InventorySection
import com.onlyfield.assetmanager.pc.ui.components.LocalConfirm
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NetworkScopeUiTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun inventoryTrashRefusalShowsErrorWithoutSavingOrAddingTrashThenAllowsRetry() {
        val device = Device(technicalName = "Scoped device")
        val original = Project(name = "Scopes", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "Site", devices = listOf(device))),
            subnets = listOf(Subnet(cidrBlock = "10.0.0.0/24", scopeType = VlanScopeType.DEVICE, scopeTargetId = device.id)))
        var project by mutableStateOf(original)
        var saves = 0
        val trash = mutableListOf<TrashItem>()
        val messages = Messages()
        compose.setContent {
            CompositionLocalProvider(LocalMessages provides messages, LocalConfirm provides { it.onConfirm() }) {
                MaterialTheme { InventorySection(project, { updated, _ -> project = updated; saves++ }, { trash += it }, { _, _, _ -> false }) }
            }
        }
        fun delete() {
            compose.onNodeWithContentDescription(messages.text("ux.more") + " · " + device.technicalName).performClick()
            compose.onNodeWithText(messages.text("text.dd41b3275173")).performClick()
        }
        delete()
        compose.onNodeWithText(messages.text("network.scopeInUse")).assertExists()
        compose.runOnIdle { assertEquals(original, project); assertEquals(0, saves); assertTrue(trash.isEmpty()); project = original.copy(subnets = emptyList()) }
        delete()
        compose.runOnIdle { assertEquals(1, saves); assertEquals(1, trash.size); assertTrue(project.sites.single().devices.isEmpty()) }
    }

    @Test fun mergeRefusalPreservesWorkingCopyTrashAndUndoForPlainAndProtectedProjects() {
        for (password in listOf(null, "dummy-password")) {
            val survivor = Device(technicalName = "Survivor")
            val duplicate = Device(technicalName = "Duplicate")
            val original = Project(name = "Scopes", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Site", devices = listOf(survivor, duplicate))), isPasswordProtected = password != null,
                vlans = listOf(Vlan(vlanId = 10, name = "LAN", scopeType = VlanScopeType.DEVICE, scopeTargetId = survivor.id)))
            val storage = DesktopStorageManager(folder.newFolder())
            val file = storage.saveProjectLocally(original, password)
            val state = DesktopAppState(storage)
            try {
                state.importFile(file, password, compare = false)
                val bytes = file.readBytes()
                assertFalse(state.mergeDevices(survivor.id, duplicate.id, MergeDataChoices()))
                assertEquals(Messages().text("network.scopeInUse"), state.error)
                assertEquals(original, state.project); assertTrue(state.trash.isEmpty()); assertFalse(state.canUndo)
                assertArrayEquals(bytes, file.readBytes())
                state.update(original.copy(vlans = emptyList()), "Explicitly remove scope")
                assertTrue(state.mergeDevices(survivor.id, duplicate.id, MergeDataChoices()))
                assertEquals(1, state.trash.size)
                state.undo()
                assertEquals(2, state.project!!.sites.single().devices.size)
                assertTrue(state.trash.isEmpty())
            } finally { state.shutdown() }
        }
    }
}
