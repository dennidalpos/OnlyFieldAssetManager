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
import com.onlyfield.assetmanager.configurator.map.MapActions
import com.onlyfield.assetmanager.configurator.map.MapWorkspace
import com.onlyfield.assetmanager.configurator.map.arc
import com.onlyfield.assetmanager.core.i18n.Messages
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
    private val initial = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "BU", areas = listOf(area), devices = listOf(device))), floorplanPlacements = listOf(FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE, targetId = device.id, xRatio = .3f, yRatio = .4f)))

    private fun viewport(node: SemanticsNodeInteraction) = node.fetchSemanticsNode().size.let { MapViewport(it.width.toFloat(), it.height.toFloat(), 1200f, 900f) }
    private fun SemanticsNodeInteraction.clickAt(viewport: MapViewport, point: MapPoint) = viewport.screen(point).let { p -> performTouchInput { click(Offset(p.x, p.y)) } }

    @Test fun clickOpensObjectAndDragSavesOnlyOnRelease() {
        var p by mutableStateOf(initial)
        var saves = 0
        rule.setContent { MaterialTheme { Box(Modifier.size(800.dp, 600.dp)) {
            MapWorkspace(p, area.id, null, Messages(), MapActions({ updated, _ -> p = updated; saves++ }, { _, _ -> }, { _, _ -> }))
        } } }
        val node = rule.onNodeWithTag("floor-map")
        val start = viewport(node).screen(MapPoint(.3f, .4f)).let { Offset(it.x, it.y) }
        node.performTouchInput { click(start) }
        rule.onNode(hasTestTag("map-summary") and hasAnyDescendant(hasText("SW-01"))).assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, saves) }
        // The bottom pane shrinks the map on narrow windows: recompute the node position.
        val moved = viewport(node).screen(MapPoint(.3f, .4f)).let { Offset(it.x, it.y) }
        node.performTouchInput { down(moved); moveBy(Offset(10f, 2f)); moveBy(Offset(60f, 30f)); up() }
        rule.runOnIdle {
            assertEquals(1, saves)
            assertTrue(p.floorplanPlacements.single().xRatio > .3f)
            assertTrue(p.floorplanPlacements.single().yRatio > .4f)
        }
    }
    @Test fun selectedObjectHasSecondaryConfirmedTrash() {
        var trashed: ObjectRef? = null
        rule.setContent { MaterialTheme { Box(Modifier.size(800.dp, 600.dp)) {
            MapWorkspace(initial, area.id, null, Messages(), MapActions({ _, _ -> }, { _, _ -> }, { _, _ -> }, trash = { trashed = it }))
        } } }
        val node = rule.onNodeWithTag("floor-map")
        node.clickAt(viewport(node), MapPoint(.3f, .4f))
        rule.onNodeWithText("Espandi").performClick()
        rule.onNodeWithText("Altre azioni").performClick()
        rule.onNodeWithText("Sposta nel cestino").assertIsDisplayed().performClick()
        rule.onNodeWithText("Spostare «SW-01» nel cestino?").assertIsDisplayed()
        rule.onAllNodesWithText("Sposta nel cestino").filterToOne(hasAnyAncestor(isDialog())).performClick()
        rule.runOnIdle { assertEquals(ObjectRef(PlacementTargetType.DEVICE, device.id), trashed) }
    }
    @Test fun longPressDoesNotMoveAndDragKeepsGrabOffset() {
        var p by mutableStateOf(initial)
        var saves = 0
        rule.setContent { MaterialTheme { Box(Modifier.size(800.dp, 600.dp)) {
            MapWorkspace(p, area.id, null, Messages(), MapActions({ updated, _ -> p = updated; saves++ }, { _, _ -> }, { _, _ -> }))
        } } }
        val node = rule.onNodeWithTag("floor-map")
        val v = viewport(node)
        val centre = v.screen(MapPoint(.3f, .4f)).let { Offset(it.x, it.y) }
        node.performTouchInput { longClick(centre) }
        rule.runOnIdle { assertEquals(0, saves) }
        // Grab 8 px right of the centre: the centre moves by the drag delta, not under the finger.
        node.performTouchInput { down(centre + Offset(8f, 0f)); moveBy(Offset(10f, 2f)); moveBy(Offset(60f, 30f)); up() }
        rule.runOnIdle {
            assertEquals(1, saves)
            val expected = v.relative(centre.x + 70f, centre.y + 32f)
            val placed = p.floorplanPlacements.single()
            assertEquals(expected.x, placed.xRatio, .002f)
            assertEquals(expected.y, placed.yRatio, .002f)
        }
    }

    @Test fun hierarchySupportsLaterAdditionsBackNavigationAndCatalogCancellation() {
        val dir = Files.createTempDirectory("ofam_navigation_test").toFile()
        val state = DesktopAppState(DesktopStorageManager(dir))
        try {
            val second = Area(name = "Primo")
            val site = Site(name = "BU-A", areas = listOf(area, second), devices = listOf(device))
            val empty = Site(name = "BU-B")
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", sites = listOf(site, empty))))
            rule.setContent { MaterialTheme { Box(Modifier.size(900.dp, 700.dp)) { FloorHomeSection(state) } } }
            rule.onNodeWithText("BU-A · 2 piani / zone").performClick()
            rule.onNodeWithText("Terra · 1 oggetti").performClick()
            rule.onNodeWithTag("floor-map").assertIsDisplayed()
            rule.onNodeWithText("Aggiungi oggetto").performClick()
            rule.onNodeWithText("Cerca tipologia…").performTextInput("modem")
            rule.onNodeWithText("Modem").assertIsDisplayed()
            rule.onNodeWithText("Annulla").performClick()
            rule.runOnIdle { assertEquals(1, state.project!!.sites.first().devices.size) }
            rule.onNodeWithText("BU-A / Terra").performClick()
            rule.onNodeWithText("BU-A").performClick()
            rule.onNodeWithText("Primo · 0 oggetti").performClick()
            rule.onNodeWithTag("floor-map").assertIsDisplayed()
            rule.onNodeWithText("BU-A / Primo").performClick()
            rule.onNodeWithText("BU-A").performClick()
            rule.onNodeWithText("Sito").performClick()
            rule.onNodeWithText("Aggiungi sede").performClick()
            rule.onNodeWithText("Nome *").performTextInput("BU-C")
            rule.onNodeWithText("Salva").performClick()
            rule.onNodeWithText("BU-C · 0 piani / zone").performClick()
            rule.onNodeWithText("Aggiungi piano").performClick()
            rule.onNodeWithText("Nome *").performTextInput("Zona nuova")
            rule.onNodeWithText("Salva").performClick()
            rule.onNodeWithText("Zona nuova · 0 oggetti").performClick()
            rule.onNodeWithTag("floor-map").assertIsDisplayed()
            rule.runOnIdle { assertEquals(3, state.project!!.sites.size); assertEquals(empty, state.project!!.sites[1]) }
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

    @Test fun cableGroupsAndInternalIndicatorsSelectRealCables() {
        val rack = Rack(name = "R1", areaId = area.id)
        val inside = device.copy(rackId = rack.id)
        val peer = Device(technicalName = "AP", areaId = area.id)
        val otherInside = Device(technicalName = "SW-02", rackId = rack.id)
        val c1 = Cable(codeOrLabel = "C1", deviceAId = inside.id, deviceBId = peer.id)
        val c2 = Cable(codeOrLabel = "C2", deviceAId = otherInside.id, deviceBId = peer.id)
        val internal = Cable(codeOrLabel = "INT", deviceAId = inside.id, deviceBId = otherInside.id)
        val p = initial.copy(racks = listOf(rack), sites = listOf(initial.sites.single().copy(devices = listOf(inside, peer, otherInside))),
            cables = listOf(c1, c2, internal), floorplanPlacements = listOf(
                FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.RACK, targetId = rack.id, xRatio = .2f, yRatio = .5f),
                FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE, targetId = peer.id, xRatio = .8f, yRatio = .5f)))
        var p2 by mutableStateOf(p)
        var opened: String? = null
        rule.setContent { MaterialTheme { Box(Modifier.size(800.dp, 600.dp)) {
            MapWorkspace(p2, area.id, null, Messages(), MapActions({ updated, _ -> p2 = updated }, { draft, _ -> opened = draft.id }, { _, _ -> }))
        } } }
        val canvas = rule.onNodeWithTag("floor-map")
        // Links are drawn as arcs: tap the middle of the curve.
        fun middle(a: MapPoint, b: MapPoint) = arc(a, b).let { it[it.size / 2] }
        canvas.clickAt(viewport(canvas), middle(MapPoint(.2f, .5f), MapPoint(.8f, .5f)))
        rule.onNodeWithText("2 cavi", substring = true).assertIsDisplayed()
        rule.onNodeWithText("INT").assertDoesNotExist()
        rule.onNodeWithText("Espandi").performClick()
        rule.onNodeWithText("C2").performScrollTo().performClick()
        rule.onNodeWithText("Modifica cavo").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(c2.id, opened) }
        rule.onNodeWithText("Riduci").performClick()
        // Opening the rack shows the internal cable between its children.
        canvas.clickAt(viewport(canvas), MapPoint(.2f, .5f))
        rule.onNodeWithText("Indietro").assertIsDisplayed()
        canvas.clickAt(viewport(canvas), middle(MapPoint(.5f, .25f), MapPoint(.5f, .75f)))
        rule.onNodeWithText("Espandi").performClick()
        rule.onNodeWithText("INT").performScrollTo().performClick()
        rule.onNodeWithText("Modifica cavo").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(internal.id, opened) }
    }

    @Test fun remoteEndLeadsToTheOtherFloorWithTheObjectSelected() {
        val north = Area(name = "Nord 1")
        val rack = Rack(name = "RN", areaId = north.id)
        val remote = Device(technicalName = "SW-05", rackId = rack.id, positionU = 1)
        val cable = Cable(codeOrLabel = "C9", deviceAId = device.id, deviceBId = remote.id)
        val p = initial.copy(sites = initial.sites + Site(name = "BU Nord", areas = listOf(north), devices = listOf(remote)),
            racks = listOf(rack), cables = listOf(cable))
        var shownArea by mutableStateOf(area.id)
        var focus by mutableStateOf<ObjectRef?>(null)
        rule.setContent { MaterialTheme { Box(Modifier.size(800.dp, 600.dp)) {
            MapWorkspace(p, shownArea, null, Messages(), MapActions({ _, _ -> }, { _, _ -> }, { _, _ -> }, goTo = { a, ref -> shownArea = a; focus = ref }), focus = focus)
        } } }
        val canvas = rule.onNodeWithTag("floor-map")
        canvas.clickAt(viewport(canvas), MapPoint(.3f, .4f))
        rule.onNodeWithText("Espandi").performClick()
        rule.onNodeWithText("→ SW-05 · Nord 1 · BU Nord").performScrollTo().performClick()
        rule.onNodeWithText("C9").performScrollTo().performClick()
        rule.onNodeWithText("Estremità remota: SW-05 · Nord 1 · BU Nord").assertExists()
        rule.onNodeWithText("Vai a SW-05").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(north.id, shownArea); assertEquals(remote.id, focus?.id) }
        // Arrival opens the rack and selects the device.
        rule.onNodeWithText("Indietro").assertIsDisplayed()
        rule.onNodeWithText("Espandi").performClick()
        rule.onNode(hasTestTag("map-detail") and hasAnyDescendant(hasText("SW-05"))).assertIsDisplayed()
    }

    @Test fun deviceListsCreatesAndEditsItsLogicalLinks() {
        val vpn = WanVpnConnection(name = "VPN-1", type = WanVpnType.VPN, localEndpointDeviceId = device.id, remoteEndpointSiteDescription = "Sede B",
            underlyingAccessId = "access-1", notes = "Nota")
        val wan = WanVpnConnection(name = "FTTH", providerOrCarrier = "Operatore X", bandwidth = "1 Gbps", remoteEndpointDeviceId = device.id)
        var p by mutableStateOf(initial.copy(wanVpnConnections = listOf(vpn, wan)))
        rule.setContent { MaterialTheme { Box(Modifier.size(800.dp, 600.dp)) {
            MapWorkspace(p, area.id, null, Messages(), MapActions({ updated, _ -> p = updated }, { _, _ -> }, { _, _ -> }))
        } } }
        val canvas = rule.onNodeWithTag("floor-map")
        canvas.clickAt(viewport(canvas), MapPoint(.3f, .4f))
        rule.onNodeWithText("Espandi").performClick()
        rule.onNodeWithText("Altri dettagli ▾").performScrollTo().performClick()
        rule.onNodeWithText("Collegamenti logici (2)").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("verso Sede B", substring = true).assertExists()
        rule.onNodeWithText("Operatore X · 1 Gbps", substring = true).assertExists()
        // Editing keeps hidden fields.
        rule.onNodeWithText("VPN · VPN-1").performScrollTo().performClick()
        rule.onNode(hasText("Banda") and hasSetTextAction()).performTextInput("100 Mbps")
        rule.onNodeWithText("Salva").performClick()
        rule.runOnIdle {
            val saved = p.wanVpnConnections.single { it.id == vpn.id }
            assertEquals("100 Mbps", saved.bandwidth)
            assertEquals("access-1", saved.underlyingAccessId); assertEquals("Nota", saved.notes); assertEquals(device.id, saved.localEndpointDeviceId)
        }
        // A new link starts as VPN with this device on the local side; the name is required.
        rule.onNodeWithText("+ Nuovo").performScrollTo().performClick()
        rule.onNodeWithText("Salva").performClick()
        rule.runOnIdle { assertEquals(2, p.wanVpnConnections.size) }
        rule.onNode(hasText("Nome / circuito *") and hasSetTextAction()).performTextInput("VPN-2")
        rule.onNodeWithText("Salva").performClick()
        rule.runOnIdle {
            val created = p.wanVpnConnections.single { it.name == "VPN-2" }
            assertEquals(WanVpnType.VPN, created.type); assertEquals(device.id, created.localEndpointDeviceId)
        }
    }

    @Test fun cableIsSelectableAndItsFreeEndCanBeDragged() {
        val cable = Cable(codeOrLabel = "C1")
        val route = CableRoute(cableId = cable.id, areaId = area.id)
        var p by mutableStateOf(initial.copy(cables = listOf(cable), cableRoutes = listOf(route)))
        var opened: String? = null
        rule.setContent { MaterialTheme { Box(Modifier.size(800.dp, 600.dp)) {
            MapWorkspace(p, area.id, null, Messages(), MapActions({ updated, _ -> p = updated }, { draft, _ -> opened = draft.id }, { _, _ -> }))
        } } }
        val node = rule.onNodeWithTag("floor-map")
        node.clickAt(viewport(node), arc(MapPoint(.4f, .5f), MapPoint(.6f, .5f)).let { it[it.size / 2] })
        rule.onNodeWithText("C1").assertIsDisplayed()
        rule.onNodeWithText("Modifica cavo").assertIsDisplayed()
        val end = viewport(node).screen(MapPoint(.6f, .5f))
        node.performTouchInput { down(Offset(end.x, end.y)); moveBy(Offset(10f, 2f)); moveBy(Offset(40f, 25f)); up() }
        rule.runOnIdle { assertTrue(p.cableRoutes.single().points.last().x > .6f) }
        rule.onNodeWithText("Modifica cavo").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(cable.id, opened) }
    }
}
