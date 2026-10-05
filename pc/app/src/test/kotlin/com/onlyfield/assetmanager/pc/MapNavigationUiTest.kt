package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.ConfiguratorPage
import com.onlyfield.assetmanager.configurator.map.MapActions
import com.onlyfield.assetmanager.configurator.map.MapWorkspace
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MapNavigationUiTest {
    @get:Rule val rule = createComposeRule()
    private val area = Area(name = "Terra")
    private val rack = Rack(name = "R1", areaId = area.id)
    private val type = ObjectType(name = "Scatola", canContainObjects = true)
    private val box = Device(technicalName = "BOX", rackId = rack.id, areaId = area.id, objectTypeId = type.id)
    private val sw = Device(technicalName = "SW", rackId = rack.id, areaId = area.id)
    private val ap = Device(technicalName = "AP", areaId = area.id)
    private val start = ObjectHierarchy.assign(Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1,
        sites = listOf(Site(name = "BU", areas = listOf(area), devices = listOf(box, sw, ap))), racks = listOf(rack), objectTypes = listOf(type),
        floorplanPlacements = listOf(
            FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.RACK, targetId = rack.id, xRatio = .3f, yRatio = .4f),
            FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE, targetId = ap.id, xRatio = .7f, yRatio = .4f))),
        ObjectRef(PlacementTargetType.DEVICE, sw.id), ObjectRef(PlacementTargetType.DEVICE, box.id))

    private fun SemanticsNodeInteraction.clickAt(point: MapPoint) {
        val size = fetchSemanticsNode().size
        val p = MapViewport(size.width.toFloat(), size.height.toFloat(), 1200f, 900f).screen(point)
        performTouchInput { click(Offset(p.x, p.y)) }
    }

    @Test fun containersOpenLevelByLevelAndChildrenCanBeReleasedOrAssigned() {
        var project by mutableStateOf(start)
        var edited: Pair<String, ConfiguratorPage>? = null
        rule.setContent { MaterialTheme { Box(Modifier.size(1000.dp, 700.dp)) {
            MapWorkspace(project, area.id, null, Messages(), MapActions({ p, _ -> project = p }, { d, page -> edited = d.id to page }, { _, _ -> }))
        } } }
        val map = rule.onNodeWithTag("floor-map")
        map.clickAt(MapPoint(.3f, .4f))
        rule.onNodeWithText("‹ Indietro").assertIsDisplayed()
        map.clickAt(MapPoint(.5f, .5f))
        rule.onNode(hasText("BOX") and hasClickAction().not()).assertExists()
        map.clickAt(MapPoint(.5f, .5f))
        rule.onNodeWithText("Altre azioni").performClick()
        rule.onNodeWithText("Rimuovi dal contenitore").performClick()
        rule.runOnIdle {
            assertNull(ObjectHierarchy.parent(project, ObjectRef(PlacementTargetType.DEVICE, sw.id)))
            assertTrue(ObjectMap.nodes(project, area.id).any { it.id == sw.id })
        }
        rule.onNodeWithText("‹ Indietro").performClick()
        rule.onNodeWithText("Assegna esistente").performClick()
        rule.onNodeWithText("AP").performClick()
        rule.runOnIdle { assertEquals(ObjectRef(PlacementTargetType.RACK, rack.id), ObjectHierarchy.parent(project, ObjectRef(PlacementTargetType.DEVICE, ap.id))) }
        rule.onNodeWithText("Altre azioni").performClick()
        rule.onNodeWithText("Modifica").performClick()
        rule.runOnIdle { assertEquals(rack.id to ConfiguratorPage.ESSENTIALS, edited) }
    }
}
