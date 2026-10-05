package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.DeviceDrawing
import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.i18n.Messages
import java.io.File
import org.jetbrains.skia.Image
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class VisualLayoutTest(private val width: Int, private val dark: Boolean, private val count: Int, private val type: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}dp dark={1} ports={2} {3}")
        fun sizes() = listOf(arrayOf<Any>(360, false, 24, "switch"), arrayOf<Any>(360, true, 48, "switch"),
            arrayOf<Any>(560, false, 24, "patch-panel"), arrayOf<Any>(1024, true, 48, "patch-panel"))
    }
    @Test fun drawingsFitAndEveryPortCanBeReached() = runDesktopComposeUiTest(width, 800) {
        val groups = DevicePresets.forType(type)!!.result(mapOf("ports" to count.toString(), "uplinks" to "0")).groups
        val device = Device(technicalName = "${type.uppercase()}-$count", objectTypeId = type, category = if (type == "patch-panel") DeviceCategory.PATCH_PANEL else DeviceCategory.NETWORK_SWITCH, hardware = HardwareSpec(passive = type == "patch-panel", portGroups = groups))
            .let { it.copy(ports = HardwareConfigurator.ports(groups, it.id)) }
        val project = Project(name = "Visual", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "Site", devices = listOf(device))),
            cables = if (type == "patch-panel") listOf(Cable(portAId = device.ports.first { it.hardware.side == PortSide.REAR && it.hardware.position == 2 }.id, medium = CableMedium.ETHERNET_COPPER)) else emptyList())
        var clicked: String? = null
        setContent { MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            Surface { Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                DeviceDrawing(project, device, Messages(), onPort = { clicked = it.port.id })
            } }
        } }
        onNodeWithContentDescription("P1: Libera").assertIsDisplayed().performClick()
        runOnIdle { assertEquals(device.ports.first().id, clicked) }
        onNodeWithContentDescription("P$count: Libera").performScrollTo().assertIsDisplayed()
        val output = File("build/reports/visual-configurator/$type-$count-$width-$dark.png")
        output.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { output.writeBytes(it.bytes) } }
        if (type == "patch-panel") {
            onNodeWithText("Retro").performScrollTo().performClick()
            onNodeWithText("Terminazioni posteriori e cavi attestati").assertExists()
            onNodeWithContentDescription("P1: Libera").performScrollTo().performClick()
            runOnIdle { assertEquals(device.ports.first { it.hardware.side == PortSide.REAR }.id, clicked) }
            Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { File(output.parent, "rear-$width-$dark.png").writeBytes(it.bytes) } }
        }
    }
}
