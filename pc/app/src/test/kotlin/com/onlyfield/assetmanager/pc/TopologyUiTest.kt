package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.Image
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.configurator.map.TopologyDialog
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TopologyUiTest {
    @get:Rule val rule = createComposeRule()

    private fun device(id: String, name: String, type: String) = Device(id = id, technicalName = name, objectTypeId = type, ports = listOf(Port(id = "$id-1", deviceId = id, name = "P1")))
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1,
        sites = listOf(Site(name = "Sede", devices = listOf(device("r", "RTR-01", "router"), device("s", "SW-01", "switch")))))
        .let { HardwareConfigurator.connect(it, "r-1", "s-1", CableMedium.ETHERNET_COPPER) }

    @Test fun showsLinkedDevicesAndOpensOnTap() {
        var opened: Device? = null
        rule.setContent { MaterialTheme { TopologyDialog(project, Messages(), null, null, { opened = it }) {} } }
        rule.onNodeWithText("2 apparati · 1 collegamenti · 0 percorsi aperti (⋯) · +N terminali raccolti").assertExists()
        val output = File("build/reports/visual-configurator/topology-current.png")
        output.parentFile.mkdirs()
        Image.makeFromBitmap(rule.onAllNodes(isRoot()).filterToOne(hasAnyDescendant(isDialog())).captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { output.writeBytes(it.bytes) } }
        rule.onNodeWithContentDescription("SW-01, Sede, 1 collegamento").performClick()
        rule.runOnIdle { assertEquals("s", opened?.id) }
    }
}
