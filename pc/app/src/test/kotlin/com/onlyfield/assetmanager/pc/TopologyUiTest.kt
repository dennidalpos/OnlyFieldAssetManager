package com.onlyfield.assetmanager.pc

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.Image
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.onlyfield.assetmanager.configurator.map.MapHome
import com.onlyfield.assetmanager.configurator.map.MapHomeView
import com.onlyfield.assetmanager.configurator.map.MapUiState
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

    private fun capture(name: String) {
        val output = File("build/reports/visual-configurator/$name.png")
        output.parentFile.mkdirs()
        Image.makeFromBitmap(rule.onRoot().captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { output.writeBytes(it.bytes) } }
    }

    /** The Map area opens on drawings: sites and their links, then the topology of the chosen site. */
    @Test fun mapHomeDrawsSitesThenTheSiteTopology() {
        val two = Project(name = "Rete", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(
            Site(id = "a", name = "Municipio", code = "COM", areas = listOf(Area(name = "Terra")), devices = listOf(device("r", "RTR-01", "router"), device("s", "SW-01", "switch"))),
            Site(id = "b", name = "Biblioteca", devices = listOf(device("t", "SW-02", "switch")))),
            wanVpnConnections = listOf(WanVpnConnection(name = "Fibra", type = WanVpnType.INTERNET, providerOrCarrier = "TIM", localEndpointDeviceId = "r")))
            .let { HardwareConfigurator.connect(it, "s-1", "t-1", CableMedium.FIBER_OVERALL) }
        var site by mutableStateOf<Site?>(null)
        var opened: Device? = null
        rule.setContent { MaterialTheme { MapHome(two, Messages(), site, remember { MapUiState() }, { site = it }, {}, { opened = it }, {}, {}) } }
        rule.onNodeWithText("COM ↔ Biblioteca").assertExists()
        rule.onNodeWithText("1 cavo").assertExists()
        rule.onNodeWithText("COM ↔ TIM").assertExists()
        capture("map-home-sites")
        rule.onNode(hasContentDescription("Municipio", substring = true)).performClick()
        rule.onNodeWithText("Terra").assertExists()
        capture("map-home-site")
        rule.onNode(hasContentDescription("RTR-01", substring = true)).performClick()
        rule.runOnIdle { assertEquals("r", opened?.id) }
        rule.onNodeWithText("Elenco").performClick()
        rule.onNodeWithText("Terra · 0 oggetti").assertExists()
    }

    /** Logical view: devices grouped by subnet, gateway first; third-party devices are marked in the topology. */
    @Test fun logicalViewGroupsDevicesBySubnetAndThirdPartyIsMarked() {
        val net = project.copy(subnets = listOf(Subnet(cidrBlock = "192.168.1.0/24", gatewayIp = "192.168.1.1", name = "Uffici")),
            sites = project.sites.map { s -> s.copy(devices = s.devices.map { if (it.id == "r") it.copy(ipAddress = "192.168.1.1", management = DeviceManagement.SUPPLIER, supplierName = "TIM") else it }) })
        var opened: Device? = null
        val state = MapUiState().apply { homeView = MapHomeView.LOGICAL }
        rule.setContent { MaterialTheme { MapHome(net, Messages(), net.sites.single(), state, {}, {}, { opened = it }, {}, {}) } }
        rule.onNodeWithText("192.168.1.0/24").assertExists()
        rule.onNodeWithText("Uffici · Gateway 192.168.1.1").assertExists()
        rule.onNodeWithText("Senza subnet o VLAN").assertExists()
        capture("map-home-logical")
        rule.onNode(hasContentDescription("RTR-01, 192.168.1.1, gateway")).performClick()
        rule.runOnIdle { assertEquals("r", opened?.id) }
        rule.runOnIdle { state.homeView = MapHomeView.DIAGRAM }
        rule.onNode(hasContentDescription("Fornitore TIM", substring = true)).assertExists()
        capture("map-home-third-party")
    }
}
