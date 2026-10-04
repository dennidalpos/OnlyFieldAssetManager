package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.DevicePresets
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.QuickAdd
import com.onlyfield.assetmanager.core.forms.RackForm
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import java.io.File

/**
 * Demo project for manual tests: 2 business units × 2 floors, one rack per floor with two switches and a patch panel,
 * wall outlets and access points on the map. 6 of 8 switches are cabled (outlet → panel → switch, AP on PoE) and
 * linked by fibre uplinks to switches on other floors; the second switch of each "Sede Sud" floor stays empty.
 * Built through the same drafts the apps use, so ports, rack units, map placement and containment are real.
 */
object DemoSeed {
    private val i18n = Messages()
    private fun type(id: String) = ObjectCatalog.builtins.first { it.id == id }

    private class Floor(val bu: BusinessUnit, val area: Area, val code: String, val cabledB: Boolean)

    fun build(now: Long = 1_760_000_000_000): Project {
        val nord = BusinessUnit(name = "Sede Nord", code = "NORD", areas = listOf(Area(name = "Piano terra", floor = "0"), Area(name = "Primo piano", floor = "1")))
        val sud = BusinessUnit(name = "Sede Sud", code = "SUD", areas = listOf(Area(name = "Piano terra", floor = "0"), Area(name = "Primo piano", floor = "1")))
        var p = Project(name = "Demo OnlyField", description = "Dati dimostrativi per test", createdEpochMs = now, updatedEpochMs = now, businessUnits = listOf(nord, sud))
        val floors = listOf(
            Floor(nord, nord.areas[0], "N0", cabledB = true), Floor(nord, nord.areas[1], "N1", cabledB = true),
            Floor(sud, sud.areas[0], "S0", cabledB = false), Floor(sud, sud.areas[1], "S1", cabledB = false),
        )
        val switches = mutableMapOf<String, String>() // "N0-A" -> device id

        fun add(draft: MapObjectDraft): String { p = draft.apply(p, i18n); return draft.id }
        fun ports(deviceId: String) = p.businessUnits.flatMap { it.devices }.first { it.id == deviceId }.ports
        fun port(deviceId: String, name: String, side: PortSide? = null) = ports(deviceId).first { it.name == name && (side == null || it.hardware.side == side) }.id
        fun cable(code: String, a: String, b: String, medium: CableMedium = CableMedium.ETHERNET_COPPER, segment: String? = null) {
            p = p.copy(cables = p.cables + Cable(codeOrLabel = code, portAId = a, portBId = b, medium = medium,
                sharedPathSegmentIds = listOfNotNull(segment), lengthValue = if (medium == CableMedium.ETHERNET_COPPER) 3.0 else 30.0))
        }

        floors.forEach { f ->
            val rackId = add(QuickAdd.draft(MapObjectDraft(type = type("rack"), buId = f.bu.id, areaId = f.area.id, rack = RackForm(areaId = f.area.id),
                mapPoint = MapPoint(.12f, .5f)), "RK-${f.code}", rackHeightU = 42))
            val rack = p.racks.first { it.id == rackId }
            fun mounted(typeId: String, name: String, unit: Int, values: Map<String, String> = emptyMap()) =
                add(QuickAdd.draft(QuickAdd.inRack(p, rack, type(typeId), unit, RackSide.FRONT), name, DevicePresets.forType(typeId)!!.result(values)))
            val panel = mounted("patch-panel", "PP-${f.code}", 42, mapOf("ports" to "24"))
            val swA = mounted("switch", "SW-${f.code}-A", 40, mapOf("ports" to "24", "poe" to "HALF"))
            val swB = mounted("switch", "SW-${f.code}-B", 39, mapOf("ports" to "24", "poe" to "HALF"))
            switches["${f.code}-A"] = swA; switches["${f.code}-B"] = swB

            // Six outlets along the floor, two APs; each outlet port runs to its own panel port.
            val outlets = (1..6).map { n ->
                add(QuickAdd.draft(MapObjectDraft(type = type("outlet"), buId = f.bu.id, areaId = f.area.id, mapPoint = MapPoint(.4f + (n - 1) % 3 * .25f, if (n <= 3) .2f else .82f)),
                    "PR-${f.code}-%02d".format(n), DevicePresets.forType("outlet")!!.result(mapOf("ports" to "2"))))
            }
            val aps = (1..2).map { n ->
                add(QuickAdd.draft(MapObjectDraft(type = type("access-point"), buId = f.bu.id, areaId = f.area.id, mapPoint = MapPoint(.52f + (n - 1) * .25f, .5f)),
                    "AP-${f.code}-%02d".format(n), DevicePresets.forType("access-point")!!.result()))
            }
            var panelPort = 1
            var portA = 1; var portB = 1
            outlets.forEachIndexed { i, outlet ->
                (1..2).forEach { op ->
                    val pp = panelPort++
                    cable("H-${f.code}-%02d".format(pp), port(outlet, "P$op", PortSide.REAR), port(panel, "P$pp", PortSide.REAR))
                    // Port 1 of each outlet goes to switch A; port 2 to switch B where B is cabled, else also to A.
                    val (sw, n) = if (op == 2 && f.cabledB) swB to portB++ else swA to portA++
                    cable("PC-${f.code}-%02d".format(pp), port(panel, "P$pp", PortSide.FRONT), port(sw, "P$n"))
                }
                if (i < aps.size) cable("AP-${f.code}-%02d".format(i + 1), port(aps[i], "P1"), port(outlet, "P1", PortSide.FRONT))
            }
        }

        // Fibre backbone: every cabled switch has an uplink to a switch on another floor; the two sites meet on the ground floors.
        val segments = listOf(
            SharedPathSegment(name = "Montante Nord", sourceAreaId = nord.areas[0].id, targetAreaId = nord.areas[1].id, capacityMaxCables = 12),
            SharedPathSegment(name = "Montante Sud", sourceAreaId = sud.areas[0].id, targetAreaId = sud.areas[1].id, capacityMaxCables = 12),
            SharedPathSegment(name = "Dorsale Nord–Sud", sourceAreaId = nord.areas[0].id, targetAreaId = sud.areas[0].id, capacityMaxCables = 24),
        )
        p = p.copy(sharedPathSegments = segments)
        fun uplink(code: String, a: String, aPort: String, b: String, bPort: String, segment: SharedPathSegment) =
            cable(code, port(switches.getValue(a), aPort), port(switches.getValue(b), bPort), CableMedium.FIBER_OVERALL, segment.id)
        uplink("FO-01", "N1-A", "X1", "N0-A", "X1", segments[0])
        uplink("FO-02", "N1-B", "X1", "N0-A", "X2", segments[0])
        uplink("FO-03", "N0-B", "X1", "N1-A", "X2", segments[0])
        uplink("FO-04", "S1-A", "X1", "S0-A", "X1", segments[1])
        uplink("FO-05", "N0-A", "X3", "S0-A", "X2", segments[2])
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
