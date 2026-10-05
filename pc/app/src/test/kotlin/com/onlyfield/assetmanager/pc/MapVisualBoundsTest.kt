package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.map.*
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import java.io.File
import org.jetbrains.skia.Image
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MapVisualBoundsTest {
    @Test fun denseMapNeverPaintsOverOtherPanelsAfterZoomAndPan() = runDesktopComposeUiTest(1000, 800) {
        val area = Area(name = "Dense floor")
        val devices = (1..53).map { Device(technicalName = "SW-$it", objectTypeId = "switch", areaId = area.id) }
        val project = Project(name = "Dense", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Site", areas = listOf(area), devices = devices)))
        val sentinel = Color(0xFFB0007A)
        setContent { MaterialTheme { Column(Modifier.fillMaxSize()) {
            MapWorkspace(project, area.id, null, Messages(), MapActions({ _, _ -> }, { _, _ -> }, { _, _ -> }), Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(60.dp).background(sentinel).testTag("outside-map"))
        } } }
        repeat(6) { onNodeWithText("+").performClick() }
        onNodeWithTag("floor-map").performTouchInput { swipe(Offset(5f, 5f), Offset(350f, 300f)) }
        val image = onRoot().captureToImage()
        val pixels = image.toPixelMap()
        val bounds = onNodeWithTag("outside-map").fetchSemanticsNode().boundsInRoot
        for (x in 20 until image.width - 20 step 40) assertEquals(sentinel.toArgb(), pixels[x, (bounds.top + 20).toInt()].toArgb())
        val output = File("build/reports/visual-configurator/dense-map-zoom-pan.png")
        output.parentFile.mkdirs()
        Image.makeFromBitmap(image.asSkiaBitmap()).use { bitmap -> bitmap.encodeToData()!!.use { output.writeBytes(it.bytes) } }
    }
}
