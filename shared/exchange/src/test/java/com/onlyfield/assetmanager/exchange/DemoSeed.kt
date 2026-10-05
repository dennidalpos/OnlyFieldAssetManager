package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.DevicePresets
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.PortGroups
import com.onlyfield.assetmanager.core.forms.PortKind
import com.onlyfield.assetmanager.core.forms.PresetResult
import com.onlyfield.assetmanager.core.forms.QuickAdd
import com.onlyfield.assetmanager.core.forms.RackForm
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import java.io.File

/**
 * Demo project for manual tests: a municipality network.
 * - Comune – Municipio: CED star rack (fibre panel, core switch, firewall, router, ONT, servers, NAS, UPS) and two office floors.
 * - Teatro comunale: own FTTH line, VPN to the Municipio firewall, one run extended through a junction box.
 * - Scuola media: radio bridge from the Municipio roof, relaying by radio to the Scuola materna.
 * Every floor has a wiring rack with 2 × 48 patch panels, 3/4 of the ports cabled to wall outlets,
 * two PoE switches and endpoints (AP, cameras, phones, PCs) on the first outlets.
 * Built through the same drafts the apps use, so ports, rack units, map placement and containment are real.
 */
object DemoSeed {
    private val i18n = Messages()
    private fun type(id: String) = ObjectCatalog.builtins.first { it.id == id }
    private fun preset(typeId: String, values: Map<String, String> = emptyMap()) = DevicePresets.forType(typeId)!!.let { it.result(it.defaults() + values) }

    /** Wiring rack of one floor; [fibre] is its fibre panel towards the star rack (null when the switches are the core). */
    private class Kit(val rack: Rack, val swA: String, val swB: String, val fibre: String?)

    /** Cabled outlets per floor: 36 × 2 ports = 72 of the 96 panel ports. */
    const val OUTLETS = 36

    fun build(now: Long = 1_760_000_000_000): Project {
        val com = BusinessUnit(name = "Comune – Municipio", code = "COM", areas = listOf(
            Area(name = "CED", floor = "-1"), Area(name = "Piano terra", floor = "0"), Area(name = "Primo piano", floor = "1"), Area(name = "Copertura", floor = "2")))
        val tea = BusinessUnit(name = "Teatro comunale", code = "TEA", areas = listOf(Area(name = "Piano terra", floor = "0")))
        val med = BusinessUnit(name = "Scuola media", code = "MED", areas = listOf(
            Area(name = "Piano terra", floor = "0"), Area(name = "Primo piano", floor = "1"), Area(name = "Copertura", floor = "2")))
        val mat = BusinessUnit(name = "Scuola materna", code = "MAT", areas = listOf(Area(name = "Piano terra", floor = "0"), Area(name = "Copertura", floor = "1")))
        var p = Project(name = "Demo Comune", description = "Rete comunale dimostrativa: municipio, teatro, scuola media e materna",
            createdEpochMs = now, updatedEpochMs = now, businessUnits = listOf(com, tea, med, mat))

        fun add(draft: MapObjectDraft): String { p = draft.apply(p, i18n); return draft.id }
        fun device(id: String) = p.businessUnits.flatMap { it.devices }.first { it.id == id }
        fun port(deviceId: String, name: String, side: PortSide? = null) = device(deviceId).ports.first { it.name == name && (side == null || it.hardware.side == side) }.id
        fun cable(code: String?, a: String, b: String, medium: CableMedium = CableMedium.ETHERNET_COPPER, length: Double? = null, color: String? = null) {
            p = p.copy(cables = p.cables + Cable(codeOrLabel = code, portAId = a, portBId = b, medium = medium, lengthValue = length, color = color))
        }
        fun onMap(bu: BusinessUnit, area: Area, typeId: String, name: String, point: MapPoint, result: PresetResult? = DevicePresets.forType(typeId)?.let { preset(typeId) }) =
            add(QuickAdd.draft(MapObjectDraft(type = type(typeId), buId = bu.id, areaId = area.id, mapPoint = point), name, result))
        fun rack(bu: BusinessUnit, area: Area, name: String, point: MapPoint): Rack {
            val id = add(QuickAdd.draft(MapObjectDraft(type = type("rack"), buId = bu.id, areaId = area.id, rack = RackForm(areaId = area.id), mapPoint = point), name, rackHeightU = 42))
            return p.racks.first { it.id == id }
        }
        fun mounted(rack: Rack, typeId: String, name: String, unit: Int, result: PresetResult?) =
            add(QuickAdd.draft(QuickAdd.inRack(p, rack, type(typeId), unit, RackSide.FRONT), name, result))

        /** Floor rack, outlets on the map, horizontal cables, patch cords and endpoints. */
        fun floor(bu: BusinessUnit, area: Area, fibre: Boolean): Kit {
            val code = "${bu.code}-${if (area.floor == "0") "PT" else "P${area.floor}"}"
            val rack = rack(bu, area, "RK-$code", MapPoint(.06f, .5f))
            val ppA = mounted(rack, "patch-panel", "PP-$code-A", 41, preset("patch-panel", mapOf("ports" to "48")))
            val swA = mounted(rack, "switch", "SW-$code-A", 40, preset("switch", mapOf("ports" to "48", "poe" to "ALL")))
            val ppB = mounted(rack, "patch-panel", "PP-$code-B", 38, preset("patch-panel", mapOf("ports" to "48")))
            val swB = mounted(rack, "switch", "SW-$code-B", 37, preset("switch", mapOf("ports" to "48", "poe" to "ALL")))
            val ppf = if (fibre) mounted(rack, "patch-panel", "PPF-$code", 36, preset("patch-panel", mapOf("ports" to "12", "kind" to "LC"))) else null
            // Two rows above and two below the floor, nine outlets each.
            val outlets = (1..OUTLETS).map { k ->
                val row = (k - 1) / 9
                onMap(bu, area, "outlet", "PR-$code-%02d".format(k), MapPoint(.20f + (k - 1) % 9 * .095f, listOf(.10f, .24f, .76f, .90f)[row]))
            }
            outlets.forEachIndexed { o, outlet ->
                (1..2).forEach { side ->
                    val i = o * 2 + side
                    val (pp, n) = if (i <= 48) ppA to i else ppB to i - 48
                    cable("H-$code-%02d".format(i), port(outlet, "P$side", PortSide.REAR), port(pp, "P$n", PortSide.REAR), length = 12.0 + o % 9 * 4, color = "Blu")
                    val (sw, s) = if (i <= 36) swA to i else swB to i - 36
                    cable("PC-$code-%02d".format(i), port(pp, "P$n", PortSide.FRONT), port(sw, "P$s"), length = 1.0, color = "Grigio")
                }
            }
            // Endpoints on port 1 of the first outlets, in the middle of the floor.
            val endpoints = listOf("access-point" to "AP", "access-point" to "AP", "access-point" to "AP", "access-point" to "AP",
                "camera" to "CAM", "camera" to "CAM", "ip-phone" to "TEL", "ip-phone" to "TEL", "ip-phone" to "TEL", "ip-phone" to "TEL",
                "workstation" to "PC", "workstation" to "PC", "workstation" to "PC", "workstation" to "PC", "workstation" to "PC", "workstation" to "PC")
            val counters = mutableMapOf<String, Int>()
            endpoints.forEachIndexed { k, (typeId, prefix) ->
                val n = counters.merge(prefix, 1, Int::plus)!!
                val values = if (typeId == "workstation") mapOf("nics" to "1", "management" to "NO") else emptyMap()
                val id = onMap(bu, area, typeId, "$prefix-$code-%02d".format(n), MapPoint(.20f + k % 9 * .095f, if (k < 9) .40f else .58f), preset(typeId, values))
                cable(null, device(id).ports.first().id, port(outlets[k], "P1", PortSide.FRONT), length = 3.0)
            }
            return Kit(rack, swA, swB, ppf)
        }

        /** Floor uplinks through its fibre panel and a fibre trunk to the star panel, then into [core] SFP+ ports. */
        val starPorts = mutableMapOf<String, Int>()
        fun backbone(kit: Kit, star: String, core: String, coreUplinks: Iterator<String>, length: Double) {
            listOf(kit.swA, kit.swB).forEachIndexed { k, sw ->
                val fibre = requireNotNull(kit.fibre)
                val n = starPorts.merge(star, 1, Int::plus)!!
                cable(null, port(sw, "X1"), port(fibre, "F${k + 1}", PortSide.FRONT), CableMedium.FIBER_OVERALL, 2.0)
                cable("FO-${device(sw).technicalName.removePrefix("SW-")}", port(fibre, "F${k + 1}", PortSide.REAR), port(star, "F$n", PortSide.REAR), CableMedium.FIBER_OVERALL, length)
                cable(null, port(star, "F$n", PortSide.FRONT), port(core, coreUplinks.next()), CableMedium.FIBER_OVERALL, 2.0)
            }
        }
        /** Single-rack sites: switch B uplinks to switch A with a DAC. */
        fun stack(kit: Kit) = cable(null, port(kit.swB, "X1"), port(kit.swA, "X1"), CableMedium.DAC, 1.0)

        // Comune – Municipio: CED star rack with WAN, two office floors, radio on the roof.
        val (ced, comPt, comP1, comRoof) = com.areas
        val rkCed = rack(com, ced, "RK-COM-CED", MapPoint(.5f, .5f))
        val ppfCed = mounted(rkCed, "patch-panel", "PPF-COM-CED", 42, preset("patch-panel", mapOf("ports" to "24", "kind" to "LC")))
        val coreGroups = PortGroups.create(emptyList(), PortKind.RJ45, 24).let { listOf(it, PortGroups.create(listOf(it), PortKind.SFP_PLUS, 12)) }
        val core = mounted(rkCed, "switch", "SW-COM-CORE", 41, PresetResult(coreGroups, heightU = 1))
        val fw = mounted(rkCed, "firewall", "FW-COM-01", 39, preset("firewall", mapOf("lan" to "8", "wan" to "2", "uplinks" to "0")))
        val rtr = mounted(rkCed, "router", "RTR-COM-01", 38, preset("router", mapOf("lan" to "4", "wan" to "2", "uplinks" to "0")))
        val ontGroups = PortGroups.create(emptyList(), PortKind.SC, 1, prefix = "PON").let { listOf(it, PortGroups.create(listOf(it), PortKind.RJ45, 1, prefix = "LAN")) }
        val ont = mounted(rkCed, "ont", "ONT-COM-01", 37, PresetResult(ontGroups, heightU = 1))
        val servers = (1..2).map { mounted(rkCed, "server", "SRV-COM-%02d".format(it), 32 - it * 2, preset("server", mapOf("nics" to "2", "management" to "YES"))) }
        val nas = mounted(rkCed, "nas", "NAS-COM-01", 26, preset("nas", mapOf("nics" to "2")))
        mounted(rkCed, "ups", "UPS-COM-01", 2, preset("ups"))
        cable("WAN-COM-01", port(ont, "LAN1"), port(rtr, "WAN1"), length = 1.0, color = "Rosso")
        cable("WAN-COM-02", port(rtr, "LAN1"), port(fw, "WAN1"), length = 1.0, color = "Rosso")
        cable("LAN-COM-01", port(fw, "LAN1"), port(core, "P1"), length = 1.0, color = "Verde")
        var corePort = 2
        (servers.flatMap { listOf(it to "NIC1", it to "NIC2", it to "MGMT1") } + listOf(nas to "LAN1", nas to "LAN2")).forEach { (d, name) ->
            cable(null, port(d, name), port(core, "P${corePort++}"), length = 2.0, color = "Giallo")
        }
        val coreUplinks = (1..12).map { "X$it" }.iterator()
        listOf(comPt, comP1).forEach { area -> backbone(floor(com, area, fibre = true), ppfCed, core, coreUplinks, 40.0) }
        val comFloor1 = p.businessUnits.flatMap { it.devices }.first { it.technicalName == "SW-COM-P1-A" }.id
        val radCom = onMap(com, comRoof, "radio-bridge", "RAD-COM-01", MapPoint(.7f, .3f))
        cable("RAD-COM-01-LAN", port(comFloor1, "P40"), port(radCom, "LAN1", PortSide.FRONT), length = 25.0)

        // Teatro comunale: one floor, own FTTH and a VPN to the Municipio firewall.
        val teaKit = floor(tea, tea.areas.single(), fibre = false)
        stack(teaKit)
        val ontTea = mounted(teaKit.rack, "ont", "ONT-TEA-01", 36, PresetResult(ontGroups, heightU = 1))
        val rtrTea = mounted(teaKit.rack, "router", "RTR-TEA-01", 35, preset("router", mapOf("lan" to "4", "wan" to "1", "uplinks" to "0")))
        cable("WAN-TEA-01", port(ontTea, "LAN1"), port(rtrTea, "WAN1"), length = 1.0, color = "Rosso")
        cable("LAN-TEA-01", port(rtrTea, "LAN1"), port(teaKit.swA, "P41"), length = 1.0, color = "Verde")
        // Extended run: the first outlet of the stage is reached through a wall junction box.
        val box = onMap(tea, tea.areas.single(), "junction-box", "GB-TEA-PT-01", MapPoint(.20f, .17f), preset("junction-box", mapOf("ports" to "1", "kind" to "RJ45")))
        p = HardwareConfigurator.insertPassage(p, p.cables.first { it.codeOrLabel == "H-TEA-PT-01" }.id, port(box, "P1", PortSide.REAR))

        // Scuola media: ground-floor rack is the school star; radio from the Municipio and relay to the materna.
        val (medPt, medP1, medRoof) = med.areas
        val medStar = floor(med, medPt, fibre = true)
        stack(medStar)
        backbone(floor(med, medP1, fibre = true), requireNotNull(medStar.fibre), medStar.swA, listOf("X2", "X3").iterator(), 25.0)
        val radMed = onMap(med, medRoof, "radio-bridge", "RAD-MED-01", MapPoint(.3f, .3f))
        val radRelay = onMap(med, medRoof, "radio-bridge", "RAD-MED-02", MapPoint(.7f, .3f))
        cable("RAD-MED-01-LAN", port(medStar.swA, "P40"), port(radMed, "LAN1", PortSide.FRONT), length = 30.0)
        cable("RAD-MED-02-LAN", port(medStar.swA, "P41"), port(radRelay, "LAN1", PortSide.FRONT), length = 30.0)
        cable("PR-COM-MED", port(radCom, "RF1", PortSide.REAR), port(radMed, "RF1", PortSide.REAR), CableMedium.RADIO, 1200.0)

        // Scuola materna: one floor, reached by the radio relay of the scuola media.
        val (matPt, matRoof) = mat.areas
        val matKit = floor(mat, matPt, fibre = false)
        stack(matKit)
        val radMat = onMap(mat, matRoof, "radio-bridge", "RAD-MAT-01", MapPoint(.5f, .3f))
        cable("RAD-MAT-01-LAN", port(matKit.swA, "P40"), port(radMat, "LAN1", PortSide.FRONT), length = 20.0)
        cable("PR-MED-MAT", port(radRelay, "RF1", PortSide.REAR), port(radMat, "RF1", PortSide.REAR), CableMedium.RADIO, 450.0)

        // Logical links: Internet lines and the theatre VPN (shown in the device panel, not in the physical path).
        p = p.copy(wanVpnConnections = listOf(
            WanVpnConnection(name = "FTTH Municipio", type = WanVpnType.INTERNET, providerOrCarrier = "Operatore FTTH", bandwidth = "1 Gbps", localEndpointDeviceId = rtr),
            WanVpnConnection(name = "FTTH Teatro", type = WanVpnType.INTERNET, providerOrCarrier = "Operatore FTTH", bandwidth = "300 Mbps", localEndpointDeviceId = rtrTea),
            WanVpnConnection(name = "VPN Teatro–Municipio", type = WanVpnType.VPN, localEndpointDeviceId = rtrTea, remoteEndpointDeviceId = fw),
        ))
        return p.copy(updatedEpochMs = now)
    }

    /** Writes the importable package; run with `.\gradlew.bat :shared:exchange:demoPackage`. */
    @JvmStatic fun main(args: Array<String>) {
        val out = File(args.firstOrNull() ?: "fixtures/demo/onlyfield-demo.ofam").absoluteFile
        out.parentFile.mkdirs()
        out.writeBytes(PackageSerializer.exportPackage(build(), exportedEpochMs = 1_760_000_000_000))
        println("Demo package: $out")
    }
}
