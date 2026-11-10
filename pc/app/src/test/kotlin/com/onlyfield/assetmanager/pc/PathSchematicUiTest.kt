package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.configurator.PathSchematicView
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.forms.PathSchematics
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PathSchematicUiTest {
    @get:Rule val rule = createComposeRule()

    private val sw = Device(id = "sw", technicalName = "SW-01", ports = listOf(Port(id = "sw-1", deviceId = "sw", name = "P1")))
    private val outlet = Device(id = "pr", technicalName = "PR-01", objectTypeId = "outlet", ports = listOf(
        Port(id = "pr-f", deviceId = "pr", name = "P1", hardware = PortHardware(side = PortSide.FRONT)),
        Port(id = "pr-r", deviceId = "pr", name = "P1", hardware = PortHardware(side = PortSide.REAR))))
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "Sede", devices = listOf(sw, outlet))),
        panelMappings = listOf(PanelMapping(portAId = "pr-f", portBId = "pr-r")))
        .let { HardwareConfigurator.connect(it, "pr-r", "sw-1", CableMedium.ETHERNET_COPPER) }

    @Test fun drawsObjectsCableAndOpenEnd() {
        var opened: Device? = null
        rule.setContent { MaterialTheme { PathSchematicView(PathSchematics.of(project, "pr-f")!!, Messages(), onOpen = { opened = it }) } }
        rule.onNodeWithText("SW-01").assertExists()
        rule.onNodeWithText("PR-01/P1 – SW-01/P1").assertExists()
        rule.onNodeWithText("P1 · Retro → P1 · Fronte · fine aperta").assertExists()
        // The queried object has no Open button: only the switch does.
        rule.onNodeWithText("Apri").performClick()
        rule.runOnIdle { assertEquals("sw", opened?.id) }
    }
}
