package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.ui.FloorCanvas
import com.onlyfield.assetmanager.pc.ui.FloorHomeSection
import com.onlyfield.assetmanager.pc.ui.CredentialsSection
import com.onlyfield.assetmanager.core.onboarding.*
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FloorMapUiTest {
    @get:Rule val rule = createComposeRule()
    private val area = Area(name = "Terra")
    private val device = Device(technicalName = "SW-01", areaId = area.id)
    private val initial = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area), devices = listOf(device))), floorplanPlacements = listOf(FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE, targetId = device.id, xRatio = .3f, yRatio = .4f)))

    @Test fun clickOpensObjectAndDragSavesOnlyOnRelease() {
        var p by mutableStateOf(initial)
        var clicked: String? = null
        var saves = 0
        rule.setContent { MaterialTheme { Box(Modifier.size(800.dp, 600.dp)) {
            FloorCanvas(p, area.id, null, { updated, _ -> p = updated; saves++ }, { clicked = it.id }, {})
        } } }
        val node = rule.onNodeWithTag("floor-map")
        val size = node.fetchSemanticsNode().size
        val viewport = MapViewport(size.width.toFloat(), size.height.toFloat(), 1200f, 900f)
        val start = viewport.screen(MapPoint(.3f, .4f)).let { Offset(it.x, it.y) }
        node.performTouchInput { click(start) }
        rule.runOnIdle { assertEquals(device.id, clicked); assertEquals(0, saves) }
        node.performTouchInput { down(start); moveBy(Offset(10f, 2f)); moveBy(Offset(60f, 30f)); up() }
        rule.runOnIdle {
            assertEquals(1, saves)
            assertTrue(p.floorplanPlacements.single().xRatio > .3f)
            assertTrue(p.floorplanPlacements.single().yRatio > .4f)
        }
    }
    @Test fun hierarchySupportsLaterAdditionsBackNavigationAndCatalogCancellation() {
        val dir = Files.createTempDirectory("ofam_navigation_test").toFile()
        val state = DesktopAppState(DesktopStorageManager(dir))
        try {
            val second = Area(name = "Primo")
            val bu = BusinessUnit(name = "BU-A", areas = listOf(area, second), devices = listOf(device))
            val empty = BusinessUnit(name = "BU-B")
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", businessUnits = listOf(bu, empty))))
            rule.setContent { MaterialTheme { Box(Modifier.size(900.dp, 700.dp)) { FloorHomeSection(state) } } }
            rule.onNodeWithText("BU-A · 2 piani / zone").performClick()
            rule.onNodeWithText("Terra · 1 oggetti").performClick()
            rule.onNodeWithText("1 oggetti").assertIsDisplayed()
            rule.onNodeWithText("Aggiungi").performClick()
            rule.onNodeWithText("Cerca tipologia…").performTextInput("modem")
            rule.onNodeWithText("Modem").assertIsDisplayed()
            rule.onNodeWithText("Chiudi").performClick()
            rule.runOnIdle { assertEquals(1, state.project!!.businessUnits.first().devices.size) }
            rule.onNodeWithText("› BU-A").performClick()
            rule.onNodeWithText("Primo · 0 oggetti").performClick()
            rule.onNodeWithText("0 oggetti").assertIsDisplayed()
            rule.onNodeWithText("Sito").performClick()
            rule.onNodeWithText("Aggiungi BU").performClick()
            rule.onNodeWithText("Nome *").performTextInput("BU-C")
            rule.onNodeWithText("Salva").performClick()
            rule.onNodeWithText("BU-C · 0 piani / zone").performClick()
            rule.onNodeWithText("Aggiungi piano").performClick()
            rule.onNodeWithText("Nome *").performTextInput("Zona nuova")
            rule.onNodeWithText("Salva").performClick()
            rule.onNodeWithText("Zona nuova · 0 oggetti").performClick()
            rule.onNodeWithText("0 oggetti").assertIsDisplayed()
            rule.runOnIdle { assertEquals(3, state.project!!.businessUnits.size); assertEquals(empty, state.project!!.businessUnits[1]) }
        } finally { state.shutdown(); dir.deleteRecursively() }
    }

    @Test fun credentialsStayMaskedAndEditingKeepsHiddenFields() {
        val credential = Credential(username = "admin", secret = "dummy-secret", groupName = "Access", notes = "Retained note", deviceId = device.id)
        var project by mutableStateOf(initial.copy(credentials = listOf(credential)))
        rule.setContent { MaterialTheme { Box(Modifier.size(900.dp, 700.dp)) { CredentialsSection(project) { updated, _ -> project = updated } } } }
        rule.onNodeWithText("••••••••").assertIsDisplayed()
        rule.onNodeWithText("dummy-secret").assertDoesNotExist()
        rule.onNodeWithText("Modifica").performClick()
        rule.onNodeWithText("Utente *").performTextReplacement("operator")
        rule.onNodeWithText("Salva").performClick()
        rule.runOnIdle { assertEquals(credential.copy(username = "operator"), project.credentials.single()) }
    }

    @Test fun cableIsSelectableAndItsFreeEndCanBeDragged() {
        val cable = Cable(codeOrLabel = "C1")
        val route = CableRoute(cableId = cable.id, areaId = area.id)
        var p by mutableStateOf(initial.copy(cables = listOf(cable), cableRoutes = listOf(route)))
        var opened: String? = null
        rule.setContent { MaterialTheme { Box(Modifier.size(800.dp, 600.dp)) {
            FloorCanvas(p, area.id, null, { updated, _ -> p = updated }, {}, { opened = it })
        } } }
        val node = rule.onNodeWithTag("floor-map")
        var size = node.fetchSemanticsNode().size
        var view = MapViewport(size.width.toFloat(), size.height.toFloat(), 1200f, 900f)
        val middle = view.screen(MapPoint(.5f, .5f))
        node.performTouchInput { click(Offset(middle.x, middle.y)) }
        rule.onNodeWithText("Scheda cavo e foto").assertIsDisplayed()
        size = node.fetchSemanticsNode().size
        view = MapViewport(size.width.toFloat(), size.height.toFloat(), 1200f, 900f)
        val end = view.screen(MapPoint(.8f, .5f))
        node.performTouchInput { down(Offset(end.x, end.y)); moveBy(Offset(10f, 2f)); moveBy(Offset(40f, 25f)); up() }
        rule.runOnIdle { assertTrue(p.cableRoutes.single().points.last().x > .8f) }
        rule.onNodeWithText("Scheda cavo e foto").performClick()
        rule.runOnIdle { assertEquals(cable.id, opened) }
    }
}
