package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.DevicePresets
import com.onlyfield.assetmanager.core.forms.CableForm
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.PortGroups
import com.onlyfield.assetmanager.core.forms.PortKind
import com.onlyfield.assetmanager.core.forms.PresetResult
import com.onlyfield.assetmanager.core.forms.QuickAdd
import com.onlyfield.assetmanager.core.forms.RackForm
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ModelValidator
import java.io.File

/**
 * Municipality network plus a labelled survey lab. Uses the apps' drafts and hardware APIs.
 * The lab adds incomplete/conflicting surveys, models, nested maps and synthetic media.
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
        val com = Site(name = "Comune – Municipio", code = "COM", group = "Sedi comunali", address = "Piazza del Municipio 1", areas = listOf(
            Area(name = "CED", floor = "-1"), Area(name = "Piano terra", floor = "0"), Area(name = "Primo piano", floor = "1"), Area(name = "Copertura", floor = "2"),
            Area(name = "Laboratorio collaudi", floor = "0", description = "Casi sintetici: porte, modelli, contenitori e rilievi da verificare")))
        val tea = Site(name = "Teatro comunale", code = "TEA", group = "Sedi comunali", address = "Via del Teatro 5", areas = listOf(Area(name = "Piano terra", floor = "0")))
        val med = Site(name = "Scuola media", code = "MED", group = "Scuole", address = "Via delle Scuole 10", areas = listOf(
            Area(name = "Piano terra", floor = "0"), Area(name = "Primo piano", floor = "1"), Area(name = "Copertura", floor = "2")))
        val mat = Site(name = "Scuola materna", code = "MAT", group = "Scuole", address = "Via dei Giardini 3", areas = listOf(Area(name = "Piano terra", floor = "0"), Area(name = "Copertura", floor = "1")))
        var p = Project(id = "d0000000-0000-0000-0000-000000000001", name = "Demo Comune", description = "Rete comunale dimostrativa: municipio, teatro, scuola media e materna",
            createdEpochMs = now, updatedEpochMs = now, sites = listOf(com, tea, med, mat))

        fun add(draft: MapObjectDraft): String { p = draft.apply(p, i18n); return draft.id }
        fun device(id: String) = p.sites.flatMap { it.devices }.first { it.id == id }
        fun port(deviceId: String, name: String, side: PortSide? = null) = device(deviceId).ports.first { it.name == name && (side == null || it.hardware.side == side) }.id
        fun cable(code: String?, a: String, b: String?, medium: CableMedium = CableMedium.ETHERNET_COPPER, length: Double? = null, color: String? = null): String {
            val c = Cable(codeOrLabel = code, portAId = a, portBId = b, medium = medium, lengthValue = length, color = color)
            p = p.copy(cables = p.cables + c)
            return c.id
        }
        fun onMap(site: Site, area: Area, typeId: String, name: String, point: MapPoint, result: PresetResult? = DevicePresets.forType(typeId)?.let { preset(typeId) }) =
            add(QuickAdd.draft(MapObjectDraft(type = type(typeId), siteId = site.id, areaId = area.id, mapPoint = point), name, result))
        fun rack(site: Site, area: Area, name: String, point: MapPoint): Rack {
            val id = add(QuickAdd.draft(MapObjectDraft(type = type("rack"), siteId = site.id, areaId = area.id, rack = RackForm(areaId = area.id), mapPoint = point), name, rackHeightU = 42))
            return p.racks.first { it.id == id }
        }
        fun mounted(rack: Rack, typeId: String, name: String, unit: Int, result: PresetResult?) =
            add(QuickAdd.draft(QuickAdd.inRack(p, rack, type(typeId), unit, RackSide.FRONT), name, result))

        /** Floor rack, outlets on the map, horizontal cables, patch cords and endpoints. */
        fun floor(site: Site, area: Area, fibre: Boolean): Kit {
            val code = "${site.code}-${if (area.floor == "0") "PT" else "P${area.floor}"}"
            val rack = rack(site, area, "RK-$code", MapPoint(.06f, .5f))
            val ppA = mounted(rack, "patch-panel", "PP-$code-A", 41, preset("patch-panel", mapOf("ports" to "48")))
            val swA = mounted(rack, "switch", "SW-$code-A", 40, preset("switch", mapOf("ports" to "48", "poe" to "ALL")))
            val ppB = mounted(rack, "patch-panel", "PP-$code-B", 38, preset("patch-panel", mapOf("ports" to "48")))
            val swB = mounted(rack, "switch", "SW-$code-B", 37, preset("switch", mapOf("ports" to "48", "poe" to "ALL")))
            val ppf = if (fibre) mounted(rack, "patch-panel", "PPF-$code", 36, preset("patch-panel", mapOf("ports" to "12", "kind" to "LC"))) else null
            // Two rows above and two below the floor, nine outlets each.
            val outlets = (1..OUTLETS).map { k ->
                val row = (k - 1) / 9
                onMap(site, area, "outlet", "PR-$code-%02d".format(k), MapPoint(.20f + (k - 1) % 9 * .095f, listOf(.10f, .24f, .76f, .90f)[row]))
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
                val id = onMap(site, area, typeId, "$prefix-$code-%02d".format(n), MapPoint(.20f + k % 9 * .095f, if (k < 9) .40f else .58f), preset(typeId, values))
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
        val comFloor1 = p.sites.flatMap { it.devices }.first { it.technicalName == "SW-COM-P1-A" }.id
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
        // Keep lab anomalies separate from the municipality's complete paths.
        val lab = com.areas.last()
        val switchModels = listOf(8, 16, 24, 48).flatMap { count -> listOf("NONE", "HALF", "ALL").map { poe ->
            val draft = QuickAdd.draft(MapObjectDraft(type = type("switch"), siteId = com.id, areaId = lab.id), "Modello",
                preset("switch", mapOf("ports" to count.toString(), "poe" to poe)))
            val suffix = when (poe) { "NONE" -> "senza PoE"; "HALF" -> "PoE metà porte"; else -> "PoE tutte le porte" }
            HardwareConfigurator.model(p, draft, "DEMO Switch %02d – %s".format(count, suffix)).copy(brand = "Demo", notes = "Modello sintetico per ricerca e applicazione")
        } }
        val rackDraft = QuickAdd.draft(MapObjectDraft(type = type("rack"), siteId = com.id, areaId = lab.id,
            rack = RackForm(areaId = lab.id, depthMm = "600", mountingDepthMm = "450", numberingDirection = NumberingDirection.TOP_TO_BOTTOM)), "RK-COM-LAB", rackHeightU = 12)
        val rackModel = HardwareConfigurator.model(p, rackDraft, "DEMO Rack 12U – numerazione dall’alto")
        val cableDraft = MapObjectDraft(type = type("copper-cable"), siteId = com.id, areaId = lab.id,
            cable = CableForm(color = "Arancione"))
        val cableModel = HardwareConfigurator.model(p, cableDraft, "DEMO Rame – arancione")
        p = p.copy(deviceModels = switchModels + rackModel + cableModel)
        val labRackId = add(HardwareConfigurator.applyModel(rackDraft, rackModel).copy(mapPoint = MapPoint(.12f, .5f)))
        val labRack = p.racks.single { it.id == labRackId }
        val labSwitchModel = switchModels.single { it.name == "DEMO Switch 08 – PoE metà porte" }
        val swLab = add(HardwareConfigurator.applyModel(QuickAdd.draft(QuickAdd.inRack(p, labRack, type("switch"), 12, RackSide.FRONT), "SW-COM-LAB"), labSwitchModel))
        val swHardware = device(swLab).hardware.copy(
            portLayouts = listOf(PortLayout(group = "P", rows = 2, order = listOf("2", "1", "4", "3", "6", "5", "8", "7"))),
            portPoeOverrides = listOf(PortPoeOverride(group = "P", key = "3", standard = PoeStandard.IEEE_802_3BT), PortPoeOverride(group = "P", key = "4")))
        p = HardwareConfigurator.configure(p, device(swLab).copy(hardware = swHardware))
        val ppLab = mounted(labRack, "patch-panel", "PP-COM-LAB", 11, preset("patch-panel", mapOf("ports" to "12")))
        val nvr = mounted(labRack, "nvr", "NVR-COM-LAB", 10, preset("nvr", mapOf("ports" to "0")))
        val pdu = add(QuickAdd.draft(QuickAdd.inRack(p, rkCed, type("pdu"), 1, RackSide.REAR), "PDU-COM-CED", preset("pdu")))
        val apLab = onMap(com, lab, "access-point", "AP-COM-LAB", MapPoint(.45f, .22f), preset("access-point", mapOf("poe" to "IEEE_802_3AF")))
        val camLab = onMap(com, lab, "camera", "CAM-COM-LAB", MapPoint(.8f, .22f))
        val cabinet = onMap(com, lab, "cabinet", "ARM-COM-LAB", MapPoint(.8f, .72f))
        fun inside(parent: String, typeId: String, name: String, point: MapPoint): String = add(QuickAdd.draft(
            MapObjectDraft.newObject(p, type(typeId), com.id, lab.id, ObjectRef(PlacementTargetType.DEVICE, parent)).copy(mapPoint = point), name, DevicePresets.forType(typeId)?.let { preset(typeId) }))
        val enclosure = inside(cabinet, "enclosure", "CAS-COM-LAB", MapPoint(.5f, .5f))
        inside(enclosure, "power-supply", "ALIM-COM-LAB", MapPoint(.25f, .5f))
        val sensor = inside(enclosure, "sensor", "SENS-COM-LAB", MapPoint(.75f, .5f))
        cable("AOC-COM-LAB", port(swLab, "X1"), port(core, "X5"), CableMedium.AOC, 10.0)
        val apCable = cable("AP-COM-LAB-01", port(swLab, "P1"), device(apLab).ports.single().id, length = 8.0, color = "Arancione")
        cable("CAM-COM-LAB-01", port(swLab, "P2"), device(camLab).ports.single().id, length = 14.0)
        val openCable = cable("APERTO-LAB-01", port(swLab, "P3"), null, length = 6.0, color = "Arancione")
        cable("NVR-COM-LAB-01", port(swLab, "P5"), port(nvr, "LAN1"), length = 1.0)
        cable("PASSANTE-LAB-01", port(swLab, "P6"), port(ppLab, "P1", PortSide.FRONT), length = 1.0)
        p = p.copy(cables = p.cables.map { c -> when (c.id) {
            apCable -> c.copy(deviceModelId = cableModel.id, objectTypeId = "copper-cable")
            openCable -> c.copy(notes = "Estremità B non rilevata: completare dalla scheda porta")
            else -> c
        } }, cableRoutes = listOf(CableRoute(cableId = apCable, areaId = lab.id,
            points = listOf(MapPoint(.12f, .5f), MapPoint(.3f, .5f), MapPoint(.3f, .22f), MapPoint(.45f, .22f)))))

        val vlans = listOf(10 to "Uffici", 20 to "Voce", 30 to "Videosorveglianza", 90 to "Laboratorio").map { (number, name) -> Vlan(vlanId = number, name = name) }
        val ups = p.sites.flatMap { it.devices }.single { it.technicalName == "UPS-COM-01" }.id
        p = p.copy(vlans = vlans, subnets = vlans.map { v -> Subnet(cidrBlock = "10.10.${v.vlanId}.0/24", gatewayIp = "10.10.${v.vlanId}.1", vlanId = v.id, name = v.name) },
            portVlanMemberships = listOf(
                PortVlanMembership(portId = port(swLab, "P1"), mode = PortVlanMode.ACCESS, untaggedVlanId = 90),
                PortVlanMembership(portId = port(swLab, "P2"), mode = PortVlanMode.ACCESS, untaggedVlanId = 30),
                PortVlanMembership(portId = port(swLab, "X1"), mode = PortVlanMode.TRUNK, taggedVlanIds = vlans.map { it.vlanId }),
                PortVlanMembership(portId = port(core, "X5"), mode = PortVlanMode.TRUNK, taggedVlanIds = vlans.map { it.vlanId })),
            logicalInterfaces = vlans.map { v -> LogicalInterface(deviceId = core, name = "VLAN${v.vlanId}", ipAddress = "10.10.${v.vlanId}.1", subnetCidr = "10.10.${v.vlanId}.0/24", vlanId = v.vlanId) },
            lagGroups = listOf(LagGroup(deviceId = core, name = "LAG-SRV-01", memberPortIds = listOf(port(core, "P2"), port(core, "P3"))),
                LagGroup(deviceId = servers.first(), name = "bond0", memberPortIds = listOf(port(servers.first(), "NIC1"), port(servers.first(), "NIC2")))),
            deviceConfigurations = listOf(DeviceConfiguration(deviceId = swLab, title = "Configurazione demo", capturedEpochMs = now,
                configText = "hostname SW-COM-LAB\ninterface P1\n description AP laboratorio\n access vlan 90", notes = "Esempio sintetico, non destinato ad apparati reali")),
            videoSurveillanceMappings = listOf(VideoSurveillanceMapping(cameraDeviceId = camLab, managerDeviceId = nvr, channelNumber = 1, resolution = "1920x1080")),
            poeMappings = listOf(PoeMapping(portId = port(swLab, "P1"), allocatedPowerWatts = 12.0),
                PoeMapping(portId = device(apLab).ports.single().id, role = PoeRole.PD_SINK, standard = PoeStandard.IEEE_802_3AF, allocatedPowerWatts = 9.0)),
            powerFeeds = listOf(PowerFeed(deviceId = pdu, feedName = "UPS → PDU", feedType = PowerFeedType.UPS_BACKUP, sourceDeviceId = ups,
                observedRuntimeMinutes = 18, observedSource = "Rilievo sintetico demo", observedEpochMs = now),
                PowerFeed(deviceId = servers.first(), feedName = "Ingresso A", feedType = PowerFeedType.PRIMARY_A, sourceDeviceId = pdu, sourceOutletDescription = "OUT1"),
                PowerFeed(deviceId = servers.first(), feedName = "Ingresso B", feedType = PowerFeedType.SECONDARY_B, voltageVolts = 230, notes = "Rete diretta demo"),
                PowerFeed(deviceId = nvr, feedName = "Alimentazione singola", feedType = PowerFeedType.MAINS_DIRECT, voltageVolts = 230)),
            customExtraFields = listOf(CustomExtraField(targetType = "DEVICE", targetId = swLab, fieldKey = "Inventario", fieldValue = "DEMO-LAB-001"),
                CustomExtraField(targetType = "DEVICE", targetId = swLab, fieldKey = "Nota interna", fieldValue = "Dato sintetico riservato", classification = AttachmentClassification.CONFIDENTIAL)),
            annotations = listOf(Annotation(areaId = lab.id, x1Ratio = .35f, y1Ratio = .85f, label = "Laboratorio: anomalie intenzionali"),
                Annotation(areaId = lab.id, type = AnnotationType.HIGHLIGHT_ZONE, x1Ratio = .65f, y1Ratio = .6f, x2Ratio = .95f, y2Ratio = .9f,
                    label = "Contenitore da verificare", classification = AttachmentClassification.REVIEW_REQUIRED)))

        fun attachment(name: String, target: AttachmentTargetType, id: String, classification: AttachmentClassification = AttachmentClassification.SHAREABLE): Attachment {
            val a = Attachment(name = name, originalFileName = "$name.png", mimeType = "image/png", relativePath = "attachments/$name.png",
                targetType = target, targetId = id, classification = classification, createdAtEpochMs = now, attributionText = "Illustrazione sintetica DemoSeed; nessuna fotografia reale")
            p = p.copy(attachments = p.attachments + a)
            return a
        }
        val floorPlan = attachment("Pianta-COM-PT", AttachmentTargetType.AREA, comPt.id)
        val labPlan = attachment("Pianta-COM-LAB", AttachmentTargetType.AREA, lab.id)
        attachment("Guida-demo", AttachmentTargetType.PROJECT, p.id, AttachmentClassification.REVIEW_REQUIRED)
        attachment("Rack-LAB", AttachmentTargetType.RACK, labRack.id)
        attachment("Apparato-LAB-riservato", AttachmentTargetType.DEVICE, swLab, AttachmentClassification.CONFIDENTIAL)
        attachment("Cavo-LAB-da-verificare", AttachmentTargetType.CABLE, apCable, AttachmentClassification.REVIEW_REQUIRED)
        attachment("Porta-LAB-P1", AttachmentTargetType.PORT, port(swLab, "P1"))

        val states = mapOf("PC-TEA-PT-06" to OperationalStatus.OFF, "TEL-MAT-PT-04" to OperationalStatus.DECOMMISSIONED, "CAM-MED-PT-02" to OperationalStatus.TO_VERIFY)
        val graph = ConnectionGraph(p)
        val openPort = port(swLab, "P3")
        p = p.copy(sites = p.sites.map { s -> s.copy(areas = s.areas.map { a -> a.copy(floorplanAttachmentId = when (a.id) { comPt.id -> floorPlan.id; lab.id -> labPlan.id; else -> a.floorplanAttachmentId }) },
            devices = s.devices.map { d -> d.copy(operationalStatus = states[d.technicalName] ?: d.operationalStatus,
                observation = Observation("DemoSeed sintetico", now, when (d.id) { sensor -> ObservationStatus.NOT_DETECTED; camLab -> ObservationStatus.CONFLICT; swLab -> ObservationStatus.TO_VERIFY; else -> ObservationStatus.VERIFIED }),
                hardware = if (d.id == servers.first()) d.hardware.copy(redundantPower = true) else d.hardware,
                ports = d.ports.map { pt -> pt.copy(endpointStatus = when { pt.id == openPort -> EndpointStatus.DETACHED_TO_VERIFY; graph.occupied(pt.id) -> EndpointStatus.CONNECTED; else -> EndpointStatus.DISCONNECTED },
                    observation = if (d.id == camLab) Observation("DemoSeed sintetico", now, ObservationStatus.CONFLICT, "Identificazione porta discordante: caso di collaudo") else pt.observation) })
        }) })
        return p.copy(updatedEpochMs = now)
    }

    /** Writes the importable package; run with `.\gradlew.bat :shared:exchange:demoPackage`. */
    @JvmStatic fun main(args: Array<String>) {
        val out = File(args.firstOrNull() ?: "fixtures/demo/onlyfield-demo.ofam").absoluteFile
        val project = build()
        val validation = ModelValidator.validateProject(project)
        check(validation.isValid) { validation.issues.toString() }
        val bytes = PackageSerializer.exportPackage(project, DemoMedia.payloads(project), exportedEpochMs = 1_760_000_000_000)
        PackageSerializer.importPackage(bytes).pkg?.use { check(it.project == project && it.attachments.size == project.attachments.size) }
            ?: error("Generated demo could not be imported")
        check(out.parentFile.isDirectory || out.parentFile.mkdirs())
        out.writeBytes(bytes)
        println("Demo package: $out (${project.sites.sumOf { it.devices.size }} devices, ${project.racks.size} racks, ${project.cables.size} cables, ${project.deviceModels.size} models, ${project.attachments.size} attachments)")
    }
}
