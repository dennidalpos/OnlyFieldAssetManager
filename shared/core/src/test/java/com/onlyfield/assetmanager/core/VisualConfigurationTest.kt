package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class VisualConfigurationTest {
    private fun device(name: String, type: String = "switch", count: Int = 4): Device {
        val preset = DevicePresets.forType(type)!!.result(mapOf("ports" to count.toString(), "uplinks" to "0"))
        val d = Device(technicalName = name, objectTypeId = type, category = if (type == "patch-panel") DeviceCategory.PATCH_PANEL else DeviceCategory.NETWORK_SWITCH,
            hardware = HardwareSpec(widthMm = 440, depthMm = 300, portGroups = preset.groups))
        return d.copy(ports = HardwareConfigurator.ports(preset.groups, d.id))
    }
    private fun project(vararg devices: Device): Project = Project(name = "Field", createdEpochMs = 1, updatedEpochMs = 1,
        sites = listOf(Site(name = "Site", devices = devices.toList()))).let { p ->
        devices.fold(p) { result, device -> ProjectEdits.withInternalPassages(result, device.ports) }
    }
    private fun front(device: Device, number: Int) = device.ports.first { it.hardware.side == PortSide.FRONT && it.hardware.position == number }

    @Test fun layoutAndPoeSurviveDraftSaveAndModelReuseWithoutChangingConnections() {
        val a = device("A")
        val b = device("B")
        val original = HardwareConfigurator.connect(project(a, b), a.ports[0].id, b.ports[0].id, CableMedium.ETHERNET_COPPER)
        val rearranged = PortArrangement.set(a, PortArrangement.layout(a, a.ports).copy(rows = 2, order = listOf("4", "1", "3", "2")))
        val changed = PortLogic.setPoeCapability(ProjectEdits.updateDevice(original, rearranged), listOf(a.ports[3].id), PoeStandard.IEEE_802_3BT)
        val updated = changed.sites.single().devices.first()
        val draft = MapObjectDraft.forDevice(original, a).copy(device = DeviceForm.from(updated, original.sites.single().id), session = ConfigurationSession(original, changed))
        val saved = draft.apply(original)
        val device = saved.sites.single().devices.first()
        assertEquals(a.ports.map { it.id }, device.ports.map { it.id })
        assertEquals(original.cables, saved.cables)
        assertEquals(440, device.hardware.widthMm)
        assertEquals(300, device.hardware.depthMm)
        assertEquals(listOf("4", "1", "3", "2"), PortArrangement.layout(device, device.ports).order)
        assertEquals(PoeStandard.IEEE_802_3BT, device.ports[3].hardware.poeStandard)
        val model = HardwareConfigurator.model(saved, MapObjectDraft.forDevice(saved, device), "Custom switch")
        val fresh = HardwareConfigurator.applyModel(MapObjectDraft.forDevice(saved, null).withType(ObjectCatalog.builtins.first { it.id == "switch" }), model)
            .let { it.copy(device = it.device.copy(technicalName = "C")) }.apply(saved).sites.single().devices.last()
        assertEquals(device.hardware.portLayouts, fresh.hardware.portLayouts)
        assertEquals(PoeStandard.IEEE_802_3BT, fresh.ports[3].hardware.poeStandard)
    }

    @Test fun layoutsReconcileRemovedAndAddedPortsAndKeepRenamedLabels() {
        val d = device("SW")
        val laidOut = PortArrangement.set(d, PortLayout(group = "P", rows = 2, order = listOf("4", "2", "1", "3")))
        val altered = laidOut.copy(ports = laidOut.ports.dropLast(1).map { it.copy(label = "Custom label") } +
            laidOut.ports.last().copy(hardware = laidOut.ports.last().hardware.copy(position = 5), name = "P5"))
        assertEquals(listOf("2", "1", "3", "5"), PortArrangement.reconcile(altered).hardware.portLayouts.single().order)
        assertEquals(d.ports.map { it.id }, altered.ports.map { it.id })
    }

    @Test fun renamingAGroupPreservesCableEndpointsAndRebindsItsLayoutAndPoe() {
        val a = device("A")
        val b = device("B")
        val p = HardwareConfigurator.connect(project(a, b), a.ports[0].id, b.ports[0].id, CableMedium.ETHERNET_COPPER)
        val customized = PortArrangement.set(a, PortLayout(group = "P", rows = 2, order = listOf("4", "2", "3", "1")))
        val withPoe = PortLogic.setPoeCapability(ProjectEdits.updateDevice(p, customized), listOf(a.ports[3].id), PoeStandard.IEEE_802_3BT)
        val current = withPoe.sites.single().devices.first()
        val renamed = current.copy(hardware = current.hardware.copy(portGroups = current.hardware.portGroups.map { it.copy(namePrefix = "ETH", startNumber = 11) }))
        val saved = HardwareConfigurator.configure(withPoe, renamed).sites.single().devices.first()
        assertEquals(a.ports.map { it.id }, saved.ports.map { it.id })
        assertEquals(listOf("ETH11", "ETH12", "ETH13", "ETH14"), saved.ports.map { it.name })
        assertEquals(listOf("14", "12", "13", "11"), saved.hardware.portLayouts.single().order)
        assertEquals("ETH", saved.hardware.portLayouts.single().group)
        assertEquals("14", saved.hardware.portPoeOverrides.single().key)
        assertEquals(PoeStandard.IEEE_802_3BT, saved.ports[3].hardware.poeStandard)
    }

    @Test fun fixedCablingUsesFreeRearsEvenWhenFrontsAreOccupied() {
        val a = device("Outlet", "outlet", 2)
        val b = device("Panel", "patch-panel", 4)
        val terminal = device("Terminal")
        val p = HardwareConfigurator.connect(project(a, b, terminal), front(a, 1).id, terminal.ports[0].id, CableMedium.ETHERNET_COPPER)
        val pairs = PassiveCabling.pairs(p, front(a, 1).id, front(b, 1).id, 2)
        assertEquals(2, pairs.size)
        assertTrue(pairs.all { it.first.hardware.side == PortSide.REAR && it.second.hardware.side == PortSide.REAR })
        val connected = BulkCabling.connect(p, pairs, CableMedium.ETHERNET_COPPER)
        assertEquals(3, connected.cables.size)
        assertEquals(p.cables.first(), connected.cables.first())
        assertEquals(p.panelMappings, connected.panelMappings)
        assertTrue(PassiveCabling.pairs(connected, front(a, 1).id, front(b, 1).id, 1).isEmpty())
    }

    @Test fun fixedSeriesSkipsBusyRearAndContinuousModeFindsTheNextFront() {
        val a = device("A", "patch-panel")
        val b = device("B", "patch-panel")
        val p = project(a, b)
        val connected = BulkCabling.connect(p, PassiveCabling.pairs(p, front(a, 2).id, front(b, 2).id, 1), CableMedium.ETHERNET_COPPER)
        val pairs = PassiveCabling.pairs(connected, front(a, 1).id, front(b, 1).id, 3)
        assertEquals(listOf(1, 3, 4), pairs.map { it.first.hardware.position })
        assertEquals(front(a, 3).id, PassiveCabling.nextFront(connected, front(a, 1).id))
    }

    @Test fun unknownOrConflictingPassageCannotBeGuessed() {
        val a = device("A", "patch-panel")
        val b = device("B", "patch-panel")
        val p = project(a, b)
        val mapping = p.panelMappings.first { it.portAId == front(a, 1).id }
        val unknown = p.copy(panelMappings = p.panelMappings.map { if (it.id == mapping.id) it.copy(portBId = null, isUnknownPassage = true) else it })
        assertNull(PassiveCabling.rear(unknown, front(a, 1).id))
        assertTrue(PassiveCabling.pairs(unknown, front(a, 1).id, front(b, 1).id, 1).isEmpty())
        assertNull(PassiveCabling.rear(p.copy(panelMappings = p.panelMappings + mapping.copy(id = java.util.UUID.randomUUID().toString())), front(a, 1).id))
        val duplicateRear = p.copy(panelMappings = p.panelMappings.map { if (it.portAId == front(a, 2).id) it.copy(portBId = mapping.portBId) else it })
        assertNull(PassiveCabling.rear(duplicateRear, front(a, 1).id))
        assertTrue(PassiveCabling.pairs(duplicateRear, front(a, 1).id, front(b, 1).id, 1).isEmpty())
    }

    @Test fun optionalHardwareFieldsReadOldDataAndRoundTripNewData() {
        val json = Json { encodeDefaults = true }
        assertTrue(json.decodeFromString<HardwareSpec>("{}").portLayouts.isEmpty())
        val hardware = HardwareSpec(portLayouts = listOf(PortLayout(rows = 2, order = listOf("2", "1"))),
            portPoeOverrides = listOf(PortPoeOverride(key = "2", standard = PoeStandard.IEEE_802_3AT)))
        assertEquals(hardware, json.decodeFromString<HardwareSpec>(json.encodeToString(hardware)))
        assertFalse(PortArrangement.valid(hardware.copy(portLayouts = listOf(PortLayout(rows = 3)))))
    }

    @Test fun wideAndNarrowBandsKeepTheSameLogicalOrder() {
        val wide = SchematicGeometry.arrangedGrid(48, 2, 1400f, 3f, 48f)
        val narrow = SchematicGeometry.arrangedGrid(48, 2, 312f, 3f, 48f)
        fun row(grid: PortGrid, index: Int) = grid.bands.flatMap { it.getOrElse(index) { emptyList() } }
        assertEquals(row(wide, 0), row(narrow, 0))
        assertEquals(row(wide, 1), row(narrow, 1))
        assertEquals((0..47).toSet(), narrow.bands.flatten().flatten().toSet())
    }
}
