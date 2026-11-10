package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.ui.NetworkLogicalSection
import com.onlyfield.assetmanager.pc.ui.CablingSection
import com.onlyfield.assetmanager.pc.ui.components.MasterDetailHost
import com.sun.nio.file.ExtendedOpenOption
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import java.nio.file.StandardOpenOption

@RunWith(Parameterized::class)
class FailedNetworkSaveTest(private val kind: String, private val creating: Boolean, private val panel: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}, new={1}, panel={2}")
        fun editors() = listOf("vlan", "subnet", "interface", "wan", "config", "extra", "cable", "mapping")
            .flatMap { kind -> listOf(false, true).flatMap { new -> listOf(false, true).map { arrayOf<Any>(kind, new, it) } } }
    }
    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun failureRetainsDraftAndRetryPreservesEntityAndUndo() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val area = Area(name = "Test floor")
        val deviceId = java.util.UUID.randomUUID().toString()
        val port = Port(deviceId = deviceId, name = "Panel port")
        val device = Device(id = deviceId, technicalName = "Test device", areaId = area.id,
            mountingType = MountingType.OUT_OF_RACK, ports = listOf(port))
        val vlan = Vlan(vlanId = 10, name = "Existing VLAN", description = "Retained description")
        val subnet = Subnet(cidrBlock = "192.0.2.0/24", name = "Existing subnet", scopeType = VlanScopeType.SITE, scopeTargetId = null)
        val iface = LogicalInterface(deviceId = device.id, name = "Existing interface", notes = "Retained notes")
        val wan = WanVpnConnection(name = "Existing WAN", notes = "Retained notes")
        val config = DeviceConfiguration(deviceId = device.id, title = "Existing configuration", capturedEpochMs = 123)
        val projectId = java.util.UUID.randomUUID().toString()
        val extra = CustomExtraField(targetType = "PROJECT", targetId = projectId, fieldKey = "Existing key", fieldValue = "Existing value", notes = "Retained notes")
        val cable = Cable(codeOrLabel = "Existing cable", notes = "Retained notes")
        val mapping = PanelMapping(portAId = port.id, isUnknownPassage = true)
        val initial = Project(id = projectId, name = "Network retry", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "Site", areas = listOf(area), devices = listOf(device))),
            vlans = if (!creating && kind == "vlan") listOf(vlan) else emptyList(),
            subnets = if (!creating && kind == "subnet") listOf(subnet.copy(scopeType = VlanScopeType.PROJECT)) else emptyList(),
            logicalInterfaces = if (!creating && kind == "interface") listOf(iface) else emptyList(),
            wanVpnConnections = if (!creating && kind == "wan") listOf(wan) else emptyList(),
            deviceConfigurations = if (!creating && kind == "config") listOf(config) else emptyList(),
            customExtraFields = if (!creating && kind == "extra") listOf(extra) else emptyList(),
            cables = if (!creating && kind == "cable") listOf(cable) else emptyList(),
            panelMappings = if (!creating && kind == "mapping") listOf(mapping) else emptyList())
        try {
            state.importFile(storage.saveProjectLocally(initial), compare = false)
            assertNotNull("Fixture import failed: ${state.error}", state.project)
            compose.setContent {
                CompositionLocalProvider(LocalMessages provides state.i18n) { MaterialTheme {
                    val section: @androidx.compose.runtime.Composable () -> Unit = {
                        if (kind in listOf("cable", "mapping")) CablingSection(state.project!!, state::update, { state.error })
                        else NetworkLogicalSection(state.project!!, state::update, true, { state.error })
                    }
                    if (panel) MasterDetailHost { section() } else section()
                } }
            }
            val tab = when (kind) {
                "vlan" -> "text.474ee27d6727"; "subnet" -> "text.348cff30ff92"; "interface" -> "text.56518f5f56d4"
                "wan" -> "text.05845d50b58c"; "config" -> "text.469e8a3048fe"; "extra" -> "text.36d5a76a9cc7"
                "cable" -> "text.bc2fc31a1e0f"; else -> "text.3a84102d29b6"
            }
            compose.onNodeWithText(state.i18n.text(tab, if (creating) 0 else 1)).performClick()
            val add = when (kind) {
                "vlan" -> "text.62ba123ec0bc"; "subnet" -> "text.976cc683faed"; "interface" -> "text.c8edd2f4418c"
                "wan" -> "text.da56a933d02e"; "config" -> "text.d940cb0bf5d2"; "extra" -> "text.68e7427942cd"
                "cable" -> "text.f72283c631b5"; else -> "text.8c5385b5431b"
            }
            compose.onAllNodesWithText(state.i18n.text(if (creating) add else "text.49e493ba9d9c")).onFirst().performClick()
            fun field(key: String, value: String) = compose.onNode(hasSetTextAction() and hasText(state.i18n.text(key)))
                .performScrollTo().performTextReplacement(value)
            if (creating) when (kind) {
                "vlan" -> field("text.21d0e8b7d284", "20")
                "subnet" -> field("text.a1f614a904b2", "192.0.2.0/24")
                "interface", "config" -> {
                    compose.onNodeWithText(state.i18n.text("text.e7f2c0e68768")).performClick()
                    compose.onNodeWithText(device.technicalName).performClick()
                }
                "extra" -> field("text.3b50ed0e6ec2", "Retained value")
                "mapping" -> {
                    compose.onNodeWithText(state.i18n.text("text.87af12314823")).performClick()
                    compose.onNode(hasClickAction() and hasText(port.name, substring = true)).performClick()
                }
            }
            val label = when (kind) {
                "vlan", "interface" -> "text.2e245546ff59"; "subnet" -> "text.5086900635fe"
                "wan" -> "text.656e4a65e6cc"; "config" -> "text.b03a74353bbd"
                "extra" -> "text.4702cf978b67"; "cable" -> "config.name"; else -> null
            }
            if (label != null) field(label, "Retained draft")
            else compose.onNodeWithText(state.i18n.text("text.a4e483155a37")).performScrollTo().performClick()
            val before = state.project
            val local = storage.listStoredProjects().single().file
            val bytes = local.readBytes()
            val undo = state.canUndo
            val save = state.i18n.text(if (kind == "cable") { if (creating) "ux.add" else "ux.saveChanges" } else "text.c5997e85ae51")
            fun saveButton() = compose.onNodeWithText(save)
            Files.newByteChannel(local.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
                saveButton().performClick()
                compose.runOnIdle {
                    assertNotNull(state.error); assertEquals(before, state.project); assertEquals(undo, state.canUndo)
                    assertTrue(state.trash.isEmpty()); assertArrayEquals(bytes, local.readBytes())
                }
                if (label != null) compose.onNode(hasSetTextAction() and hasText("Retained draft")).performScrollTo().assertIsDisplayed()
                else saveButton().assertIsDisplayed()
                compose.onNodeWithText(state.error!!).performScrollTo().assertIsDisplayed()
            }
            saveButton().performClick()
            compose.runOnIdle {
                assertNull(state.error)
                val saved = state.project!!
                when (kind) {
                    "vlan" -> { val v = saved.vlans.single(); assertEquals("Retained draft", v.name); if (!creating) assertEquals(vlan.copy(name = v.name), v) }
                    "subnet" -> { val v = saved.subnets.single(); assertEquals("Retained draft", v.name); if (!creating) assertEquals(initial.subnets.single().copy(name = v.name), v) }
                    "interface" -> { val v = saved.logicalInterfaces.single(); assertEquals(device.id, v.deviceId); assertEquals("Retained draft", v.name); if (!creating) assertEquals(iface.copy(name = v.name), v) }
                    "wan" -> { val v = saved.wanVpnConnections.single(); assertEquals("Retained draft", v.name); if (!creating) assertEquals(wan.copy(name = v.name), v) }
                    "config" -> { val v = saved.deviceConfigurations.single(); assertEquals(device.id, v.deviceId); assertEquals("Retained draft", v.title); if (!creating) assertEquals(config.copy(title = v.title), v) }
                    "extra" -> { val v = saved.customExtraFields.single(); assertEquals("Retained draft", v.fieldKey); if (!creating) assertEquals(extra.copy(fieldKey = v.fieldKey), v) }
                    "cable" -> { val v = saved.cables.single(); assertEquals("Retained draft", v.codeOrLabel); if (!creating) assertEquals(cable.copy(codeOrLabel = v.codeOrLabel), v) }
                    "mapping" -> { val v = saved.panelMappings.single(); assertEquals(port.id, v.portAId); if (!creating) assertEquals(mapping.copy(isUnknownPassage = v.isUnknownPassage), v) }
                }
                val imported = storage.importPackageFromFile(local)
                assertNotNull(imported.validationResult.issues.toString(), imported.pkg)
                imported.pkg!!.use { assertEquals(saved, it.project) }
                state.undo(); assertEquals(before, state.project); assertEquals(undo, state.canUndo)
            }
            if (label != null) compose.onNode(hasSetTextAction() and hasText("Retained draft")).assertDoesNotExist()
        } finally { state.shutdown() }
    }
}
