package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class PresetAndPortLogicTest {
    private val area = Area(name = "Terra")
    private val site = Site(name = "BU", areas = listOf(area))
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(site))

    @Test fun everyPresetCombinationProducesValidGroups() {
        DevicePresets.all.forEach { preset ->
            val combos = preset.params.fold(listOf(emptyMap<String, String>())) { acc, param -> acc.flatMap { m -> param.values.map { m + (param.key to it) } } }
            combos.forEach { values ->
                val result = preset.result(values)
                assertTrue("${preset.id} $values", HardwareConfigurator.validGroups(result.groups))
            }
        }
    }

    @Test fun switchPresetSplitsPoeAndAddsUplinks() {
        val result = DevicePresets.forType("switch")!!.result(mapOf("ports" to "48", "uplinks" to "4", "uplinkKind" to "SFP_PLUS", "poe" to "HALF"))
        val ports = HardwareConfigurator.ports(result.groups, "d")
        assertEquals(52, ports.size)
        assertEquals(24, ports.count { it.hardware.poeStandard == PoeStandard.IEEE_802_3AT })
        assertEquals((1..48).map { "P$it" }, ports.filter { it.hardware.connector == "RJ45" }.map { it.name })
        assertEquals(4, ports.count { it.hardware.connector == "SFP+" && it.hardware.role == "UPLINK" })
        assertEquals(24 * 15.4, result.poeBudgetWatts!!, .001)
    }

    @Test fun presetIsAppliedToTheDraftAndSavedAsPorts() {
        val type = ObjectCatalog.builtins.first { it.id == "patch-panel" }
        val draft = MapObjectDraft(type = type, siteId = site.id, areaId = area.id).let { it.copy(device = it.device.copy(technicalName = "PP-01")) }
        val saved = DevicePresets.apply(draft, DevicePresets.forType("patch-panel")!!.result(mapOf("ports" to "24"))).apply(project)
        val device = saved.sites.single().devices.single()
        assertEquals(48, device.ports.size)
        assertEquals(24, saved.panelMappings.size)
        assertTrue(device.hardware.passive)
    }

    @Test fun portFlowNumbersNewGroupsAfterExistingOnes() {
        val first = PortGroups.create(emptyList(), PortKind.RJ45, 8)
        val second = PortGroups.create(listOf(first), PortKind.RJ45, 8)
        assertEquals(9, second.startNumber)
        assertEquals("Te1/1/", PortNaming.INTERFACE.prefix(PortKind.SFP_PLUS))
        assertEquals("P9–P16", PortGroups.range(second))
        assertEquals(PortKind.SFP_PLUS, PortKind.of(PortGroups.retype(first, PortKind.SFP_PLUS)))
    }

    @Test fun bulkVlanAndPoeUseExistingEntities() {
        val device = Device(technicalName = "SW", areaId = area.id).let { d -> d.copy(ports = HardwareConfigurator.ports(listOf(PortGroups.create(emptyList(), PortKind.RJ45, 4)), d.id)) }
        val p = project.copy(sites = listOf(site.copy(devices = listOf(device))), subnets = emptyList())
        val ids = device.ports.take(2).map { it.id }
        val vlan = PortLogic.setVlan(p, ids, PortVlanMode.ACCESS, 20)
        assertEquals(listOf(20), vlan.vlans.map { it.vlanId })
        assertEquals(2, vlan.portVlanMemberships.count { it.untaggedVlanId == 20 })
        val subnet = Subnet(cidrBlock = "10.0.20.0/24", vlanId = vlan.vlans.single().id)
        assertEquals(listOf(subnet), PortLogic.subnets(vlan.copy(subnets = listOf(subnet)), 20))
        val poe = PortLogic.setPoe(vlan, ids, PoeStandard.IEEE_802_3AT)
        assertEquals(60.0, PortLogic.poeLoad(poe, device), .001)
        assertTrue(PortLogic.setPoe(poe, ids, null).poeMappings.isEmpty())
        assertTrue(PortLogic.clearVlan(poe, ids).portVlanMemberships.isEmpty())
        val cells = PortLogic.panel(poe.copy(cables = listOf(Cable(portAId = ids.first()))), device)
        assertEquals(listOf(true, false, false, false), cells.map { it.occupied })
        assertEquals(20, cells.first().vlan?.untaggedVlanId)
        // Stale ids from a rebuilt preview are ignored instead of creating orphan rows.
        assertEquals(p, PortLogic.setVlan(p, listOf("gone"), PortVlanMode.ACCESS, 30))
        assertEquals(p, PortLogic.setPoe(p, listOf("gone"), PoeStandard.IEEE_802_3AF))
    }

    @Test fun builtInContainersAreRecognised() {
        listOf("rack", "shelf", "cabinet", "enclosure").forEach { id -> assertTrue(id, ObjectCatalog.builtins.first { it.id == id }.let { it.kind == ObjectKind.RACK || it.canContainObjects }) }
        assertFalse(ObjectCatalog.builtins.first { it.id == "switch" }.canContainObjects)
    }
}
