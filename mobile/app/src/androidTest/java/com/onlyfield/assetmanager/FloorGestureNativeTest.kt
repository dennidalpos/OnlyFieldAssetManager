package com.onlyfield.assetmanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.ui.screens.FloorCanvas
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloorGestureNativeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun tapAndDragUpdateTheMapOnlyOnRelease() {
        val area = Area(name = "Terra")
        val device = Device(technicalName = "SW", areaId = area.id)
        var project by mutableStateOf(Project(name = "Native map", createdEpochMs = 1, updatedEpochMs = 1,
            businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area), devices = listOf(device))),
            floorplanPlacements = listOf(FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE, targetId = device.id, xRatio = .3f, yRatio = .4f))))
        var opened: String? = null
        var saves = 0
        rule.setContent { MaterialTheme { Box(Modifier.fillMaxSize()) {
            FloorCanvas(project, area.id, null, { updated, _ -> project = updated; saves++ }, { opened = it.id }, {})
        } } }
        val map = rule.onNodeWithTag("floor-map")
        val size = map.fetchSemanticsNode().size
        val viewport = MapViewport(size.width.toFloat(), size.height.toFloat(), 1200f, 900f)
        val position = viewport.screen(MapPoint(.3f, .4f))
        val start = Offset(position.x, position.y)
        map.performTouchInput { click(start) }
        rule.runOnIdle { assertEquals(device.id, opened); assertEquals(0, saves) }
        map.performTouchInput { down(start); moveBy(Offset(10f, 2f)); moveBy(Offset(70f, 35f)); up() }
        rule.runOnIdle {
            assertEquals(1, saves)
            assertTrue(project.floorplanPlacements.single().xRatio > .3f)
            assertTrue(project.floorplanPlacements.single().yRatio > .4f)
        }
    }
}
