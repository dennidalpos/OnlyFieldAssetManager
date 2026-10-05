package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ConfiguratorTest {
    private fun device(name: String, passive: Boolean = false, groups: List<PortTemplate> = listOf(PortTemplate("P", portCount = 1))): Device {
        val d = Device(technicalName = name, hardware = HardwareSpec(portGroups = groups, passive = passive))
        return d.copy(ports = HardwareConfigurator.ports(groups, d.id))
    }
    private fun project(vararg devices: Device) = Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1,
        sites = listOf(Site(name = "BU", devices = devices.toList())))

    @Test fun switchHasSeparateCopperAndFiberGroups() {
        val d = device("SW", groups = listOf(PortTemplate("Gi", portCount = 24, connector = "RJ45"), PortTemplate("SFP", portCount = 4, connector = "SFP", role = "UPLINK")))
        assertEquals(28, d.ports.size)
        assertEquals(24, d.ports.count { it.hardware.connector == "RJ45" })
        assertEquals(4, d.ports.count { it.hardware.role == "UPLINK" })
        assertEquals(4, SchematicGeometry.rows(d, PortSide.FRONT).size)
    }

    @Test fun portGridFitsWidthAndWrapsInBalancedBands() {
        val phone = SchematicGeometry.portGrid(48, 328f, 3f, 30f, 40f)
        assertEquals(8, phone.columns)
        assertEquals(3, phone.bands.size)
        assertEquals(listOf(0, 2, 4, 6, 8, 10, 12, 14), phone.bands[0][0])
        assertEquals((0 until 48).toSet(), phone.bands.flatten().flatten().toSet())
        assertTrue(phone.columns * phone.cell + 3f * (phone.columns - 1) <= 328f)
        val wide = SchematicGeometry.portGrid(24, 560f, 3f, 30f, 40f)
        assertEquals(listOf(12), wide.bands.map { it[0].size })
        assertEquals(40f, wide.cell)
        assertEquals(listOf(listOf((0 until 8).toList())), SchematicGeometry.portGrid(8, 412f, 3f, 30f, 40f).bands)
    }

    @Test fun directConnectionIsCompleteAndIdempotent() {
        val a = device("A"); val b = device("B")
        val p = HardwareConfigurator.connect(project(a, b), a.ports.single().id, b.ports.single().id, CableMedium.ETHERNET_COPPER)
        assertEquals(ConnectionState.COMPLETE, ConnectionGraph(p).state(a.ports.single().id))
        assertEquals(1, ConnectionGraph(p).trace(a.ports.single().id).size)
        assertEquals(p.cables, HardwareConfigurator.connect(p, a.ports.single().id, b.ports.single().id, CableMedium.ETHERNET_COPPER).cables)
    }

    @Test fun internalMappingDoesNotOccupyFreeRearAttachment() {
        val panel = device("PP", true, listOf(PortTemplate("P", portCount = 1, pairedSides = true)))
        val a = device("A")
        var p = HardwareConfigurator.configure(project(a, panel), panel)
        val front = panel.ports.first { it.hardware.side == PortSide.FRONT }
        val rear = panel.ports.first { it.hardware.side == PortSide.REAR }
        p = HardwareConfigurator.connect(p, a.ports.single().id, front.id, CableMedium.ETHERNET_COPPER)
        assertEquals(ConnectionState.INCOMPLETE, ConnectionGraph(p).state(front.id))
        assertFalse(ConnectionGraph(p).occupied(rear.id))
        assertEquals(ConnectionState.AVAILABLE, ConnectionGraph(p).state(rear.id))
    }

    @Test fun tracesTwoPanelsAndJumperWithoutRevisitingIncomingCable() {
        val a = device("A"); val b = device("B")
        val p1 = device("PP1", true, listOf(PortTemplate("P", portCount = 1, pairedSides = true)))
        val p2 = device("PP2", true, listOf(PortTemplate("P", portCount = 2, pairedSides = true)))
        var p = HardwareConfigurator.configure(HardwareConfigurator.configure(project(a, b, p1, p2), p1), p2)
        fun port(d: Device, position: Int, side: PortSide) = d.ports.single { it.hardware.position == position && it.hardware.side == side }.id
        listOf(a.ports.single().id to port(p1, 1, PortSide.FRONT), port(p1, 1, PortSide.REAR) to port(p2, 1, PortSide.REAR),
            port(p2, 1, PortSide.FRONT) to port(p2, 2, PortSide.FRONT), port(p2, 2, PortSide.REAR) to b.ports.single().id).forEach { (x, y) -> p = HardwareConfigurator.connect(p, x, y, CableMedium.ETHERNET_COPPER) }
        assertEquals(ConnectionState.COMPLETE, ConnectionGraph(p).state(a.ports.single().id))
        assertEquals(ConnectionState.COMPLETE, ConnectionGraph(p).state(port(p2, 1, PortSide.FRONT)))
        val trace = ConnectionGraph(p).trace(a.ports.single().id)
        assertEquals(7, trace.size)
        assertEquals(4, trace.mapNotNull { it.cable?.id }.toSet().size)
    }

    @Test fun danglingAndConflictingConnectionsAreNotGreen() {
        val a = device("A"); val b = device("B")
        val p = HardwareConfigurator.connect(project(a, b), a.ports.single().id, null, CableMedium.ETHERNET_COPPER)
        assertEquals(ConnectionState.INCOMPLETE, ConnectionGraph(p).state(a.ports.single().id))
        assertEquals(ConnectionState.AVAILABLE, ConnectionGraph(p).state(b.ports.single().id))
        val conflict = p.copy(cables = p.cables + Cable(portAId = a.ports.single().id, portBId = b.ports.single().id))
        assertEquals(ConnectionState.CONFLICT, ConnectionGraph(conflict).state(a.ports.single().id))
        val missing = p.copy(cables = listOf(p.cables.single().copy(portBId = "missing")))
        assertEquals(ConnectionState.CONFLICT, ConnectionGraph(missing).state(a.ports.single().id))
    }

    @Test fun cyclesStopAndComboPortsShareAvailability() {
        val panel = device("PP", true, listOf(PortTemplate("P", portCount = 1, pairedSides = true)))
        var p = HardwareConfigurator.configure(project(panel), panel)
        p = HardwareConfigurator.connect(p, panel.ports[0].id, panel.ports[1].id, CableMedium.ETHERNET_COPPER)
        assertEquals(ConnectionState.CONFLICT, ConnectionGraph(p).state(panel.ports[0].id))
        assertTrue(ConnectionGraph(p).trace(panel.ports[0].id).size <= 3)
        val combo = device("SW", groups = listOf(PortTemplate("RJ", portCount = 1, comboGroup = "C"), PortTemplate("SFP", portCount = 1, comboGroup = "C")))
        val b = device("B")
        val wired = HardwareConfigurator.connect(project(combo, b), combo.ports[0].id, b.ports[0].id, CableMedium.ETHERNET_COPPER)
        assertTrue(ConnectionGraph(wired).occupied(combo.ports[1].id))
        assertThrows(IllegalArgumentException::class.java) { HardwareConfigurator.connect(wired, combo.ports[1].id, null, CableMedium.FIBER_OVERALL) }
    }

    @Test fun modelApplicationPreservesExistingPortsAndRequiresConnectedRemoval() {
        val a = device("A", groups = listOf(PortTemplate("P", portCount = 2)))
        val b = device("B")
        val p = HardwareConfigurator.connect(project(a, b), a.ports[1].id, b.ports.single().id, CableMedium.ETHERNET_COPPER)
        val reduced = a.copy(hardware = a.hardware.copy(portGroups = listOf(PortTemplate("P", portCount = 1))))
        assertThrows(IllegalArgumentException::class.java) { HardwareConfigurator.configure(p, reduced) }
        val updated = HardwareConfigurator.configure(p, reduced, true)
        assertEquals(a.ports[0].id, updated.sites.single().devices.first().ports.single().id)
        assertEquals(p.cables.single().id, updated.cables.single().id)
        assertNull(updated.cables.single().portAId)
        assertEquals(ObservationStatus.TO_VERIFY, updated.cables.single().observation?.status)
    }

    @Test fun emptyGroupsRemovePortsOnlyWithExplicitConfiguration() {
        val a = device("A")
        val updated = HardwareConfigurator.configure(project(a), a.copy(hardware = HardwareSpec()), replacePorts = true)
        assertTrue(updated.sites.single().devices.single().ports.isEmpty())
        val legacy = HardwareConfigurator.configure(project(a), a.copy(hardware = HardwareSpec()))
        assertEquals(a.ports, legacy.sites.single().devices.single().ports)
    }

    @Test fun sessionMergesOnlyItsEditsAndCanBeDiscarded() {
        val a = device("A"); val b = device("B"); val original = project(a, b)
        val edited = ProjectEdits.updateDevice(original, a.copy(alias = "session"))
        val latest = ProjectEdits.updateDevice(original.copy(name = "Concurrent"), b.copy(alias = "latest"))
        val result = ConfigurationSession(original, edited).apply(latest)
        assertEquals("Concurrent", result.name)
        assertEquals(listOf("session", "latest"), result.sites.single().devices.map { it.alias })
        assertNull(original.sites.single().devices.first().alias)
    }

    @Test fun savedModelExcludesInstanceIdentityAndPrivateFields() {
        val a = device("A").copy(ipAddress = "192.0.2.1", serialNumber = "SERIAL")
        val p = project(a).copy(customExtraFields = listOf(CustomExtraField(targetType = "DEVICE", targetId = a.id, fieldKey = "Public", fieldValue = "Value"),
            CustomExtraField(targetType = "DEVICE", targetId = a.id, fieldKey = "Private", fieldValue = "Secret", classification = AttachmentClassification.CONFIDENTIAL)))
        val model = HardwareConfigurator.model(p, MapObjectDraft.forDevice(p, a), "Model")
        assertEquals(listOf(ModelField("Public", "Value")), model.extraFields)
        assertEquals(a.hardware.portGroups, model.portTemplates)
        val next = HardwareConfigurator.applyModel(MapObjectDraft.forDevice(p, null), model)
        assertEquals("", next.device.ipAddress)
        assertEquals("", next.device.serialNumber)
        assertEquals("", next.device.technicalName)
    }

    @Test fun nestedSessionRetainsNewContainersPlacementsAndCableRoutes() {
        val original = project()
        val rackDraft = MapObjectDraft.forRack(original, null).let { it.copy(rack = it.rack.copy(name = "Rack")) }
        val stagedRack = rackDraft.apply(original)
        val parent = ObjectRef(PlacementTargetType.RACK, rackDraft.id)
        val custom = ObjectType(name = "Custom terminal", canContainObjects = true)
        val child = MapObjectDraft.newObject(stagedRack, custom, original.sites.single().id, "", parent)
            .let { it.copy(device = it.device.copy(technicalName = "Child", positionU = "1")) }
        val edited = child.apply(stagedRack).copy(
            floorplanPlacements = listOf(FloorplanPlacement(areaId = "floor", targetType = PlacementTargetType.RACK, targetId = rackDraft.id, xRatio = .5f, yRatio = .5f)),
            cableRoutes = listOf(CableRoute(cableId = "cable", areaId = "floor")),
        )
        val result = ConfigurationSession(original, edited).apply(original)
        assertEquals(custom, result.objectTypes.single())
        assertEquals(parent, ObjectHierarchy.parent(result, ObjectRef(PlacementTargetType.DEVICE, child.id)))
        assertEquals(edited.floorplanPlacements, result.floorplanPlacements)
        assertEquals(edited.cableRoutes, result.cableRoutes)
        assertTrue(original.racks.isEmpty())
    }

    @Test fun individualPortMetadataSurvivesHardwareReconciliation() {
        val a = device("A")
        val port = a.ports.single().copy(label = "Desk", hardware = a.ports.single().hardware.copy(connector = "Custom", opticalModule = "Module", customized = true))
        val edited = a.copy(ports = listOf(port))
        val result = HardwareConfigurator.configure(project(edited), edited).sites.single().devices.single().ports.single()
        assertEquals(port.id, result.id)
        assertEquals(port.label, result.label)
        assertEquals(port.hardware, result.hardware)
    }

    @Test fun cableFormRejectsSharedComboAttachmentAndTraceReportsInitialAmbiguity() {
        val a = device("A", groups = listOf(PortTemplate("RJ", portCount = 1, comboGroup = "C"), PortTemplate("SFP", portCount = 1, comboGroup = "C")))
        val b = device("B")
        val p = project(a, b)
        val draft = MapObjectDraft.forCable(p, null).let { it.copy(cable = it.cable.copy(codeOrLabel = "Cable", portAId = a.ports[0].id, portBId = a.ports[1].id)) }
        assertTrue("ports" in draft.errors(p))
        val conflict = p.copy(cables = listOf(Cable(portAId = a.ports[0].id, portBId = b.ports.single().id), Cable(portAId = a.ports[0].id)))
        val trace = ConnectionGraph(conflict).trace(a.ports[0].id)
        assertEquals(1, trace.size)
        assertTrue(trace.single().isUnknownPassage)
        assertNull(trace.single().cable)
    }

    @Test fun explicitRemovalDoesNotLeaveDanglingLogicalPortReferences() {
        val a = device("A", groups = listOf(PortTemplate("P", portCount = 2)))
        val removed = a.ports[1].id
        val original = project(a).copy(portVlanMemberships = listOf(PortVlanMembership(portId = removed)),
            poeMappings = listOf(PoeMapping(portId = removed)), lagGroups = listOf(LagGroup(deviceId = a.id, name = "LAG", memberPortIds = a.ports.map { it.id })))
        val reduced = a.copy(hardware = a.hardware.copy(portGroups = listOf(PortTemplate("P", portCount = 1))))
        assertThrows(IllegalArgumentException::class.java) { HardwareConfigurator.configure(original, reduced) }
        val edited = HardwareConfigurator.configure(original, reduced, true)
        val saved = ConfigurationSession(original, edited).apply(original)
        assertTrue(saved.portVlanMemberships.isEmpty())
        assertTrue(saved.poeMappings.isEmpty())
        assertEquals(listOf(a.ports[0].id), saved.lagGroups.single().memberPortIds)
        assertEquals(1, original.portVlanMemberships.size)
    }

    @Test fun passagePermutationRequiresFreeEndpointsAndCanBeLeftUnknown() {
        val panel = device("PP", true, listOf(PortTemplate("P", portCount = 2, pairedSides = true)))
        val original = HardwareConfigurator.configure(project(panel), panel)
        val first = original.panelMappings[0]
        val second = original.panelMappings[1]
        assertThrows(IllegalArgumentException::class.java) { HardwareConfigurator.passage(original, first.portAId, second.portBId, first.id) }
        val releasedFirst = HardwareConfigurator.passage(original, first.portAId, null, first.id)
        val released = HardwareConfigurator.passage(releasedFirst, second.portAId, null, second.id)
        val permuted = HardwareConfigurator.passage(released, first.portAId, second.portBId, first.id)
        assertEquals(second.portBId, permuted.panelMappings.find { it.id == first.id }?.portBId)
        assertEquals(2, permuted.panelMappings.size)
        assertEquals(ConnectionState.AVAILABLE, ConnectionGraph(permuted).state(first.portAId))
    }

    @Test fun traceShowsTranslatedMediumInsteadOfEnumName() {
        val a = device("A"); val b = device("B")
        val p = HardwareConfigurator.connect(project(a, b), a.ports.single().id, b.ports.single().id, CableMedium.FIBER_OVERALL)
        val step = ConnectionGraph(p).trace(a.ports.single().id).single().description
        assertFalse(step, step.contains("FIBER_OVERALL"))
    }

    @Test fun traceIsPhysicalOnlyEvenWithVpnOnLastDevice() {
        val a = device("SW"); val fw = device("FW"); val remote = device("FW-B")
        val vpn = WanVpnConnection(name = "VPN-1", type = WanVpnType.VPN, localEndpointDeviceId = fw.id, remoteEndpointDeviceId = remote.id)
        val physical = HardwareConfigurator.connect(project(a, fw, remote), a.ports.single().id, fw.ports.single().id, CableMedium.ETHERNET_COPPER)
        assertEquals(1, ConnectionGraph(physical.copy(wanVpnConnections = listOf(vpn))).trace(a.ports.single().id).size)
    }

    private fun junctionBox(p: Project): Pair<Project, Device> {
        val groups = DevicePresets.forType("junction-box")!!.result(mapOf("ports" to "1", "kind" to "RJ45")).groups
        val box = device("GB-1", passive = true, groups = groups).copy(objectTypeId = "junction-box")
        val added = p.copy(sites = p.sites.map { it.copy(devices = it.devices + box) })
        return HardwareConfigurator.configure(added, box) to box
    }

    @Test fun insertPassageSplitsCableThroughJunctionAndTraceStaysComplete() {
        val a = device("SW"); val b = device("PC")
        val cabled = HardwareConfigurator.connect(project(a, b), a.ports.single().id, b.ports.single().id, CableMedium.ETHERNET_COPPER)
        val cable = cabled.cables.single().let { it.copy(codeOrLabel = "C1", color = "Blu") }
        val (p, box) = junctionBox(cabled.copy(cables = listOf(cable)))
        val ports = p.sites.single().devices.first { it.id == box.id }.ports
        val rear = ports.single { it.hardware.side == PortSide.REAR }; val front = ports.single { it.hardware.side == PortSide.FRONT }
        assertEquals(mapOf(rear.id to front.id, front.id to rear.id), HardwareConfigurator.freePassages(p))
        val split = HardwareConfigurator.insertPassage(p, cable.id, rear.id)
        assertEquals(2, split.cables.size)
        val kept = split.cables.single { it.id == cable.id }
        assertEquals("C1", kept.codeOrLabel); assertEquals(rear.id, kept.portBId)
        val added = split.cables.single { it.id != cable.id }
        assertEquals(front.id, added.portAId); assertEquals(b.ports.single().id, added.portBId); assertEquals("Blu", added.color)
        val graph = ConnectionGraph(split)
        assertEquals(ConnectionState.COMPLETE, graph.state(a.ports.single().id))
        assertEquals(b.id, graph.trace(a.ports.single().id).last().let { step -> split.cables.single { it.id == step.cable?.id }.portBId }?.let { id -> split.sites.single().devices.first { d -> d.ports.any { it.id == id } }.id })
        assertTrue(HardwareConfigurator.freePassages(split).isEmpty())
        assertTrue(HardwareConfigurator.disconnect(split, b.ports.single().id).cables.none { it.id == added.id })
    }

    @Test fun trashingPassiveDeviceLeavesNoOrphansAndRestoreRebuildsPassages() {
        val (p, box) = junctionBox(project(device("SW")))
        assertEquals(1, p.panelMappings.size)
        val (trashed, item) = ProjectEdits.deleteDeviceToTrash(p, box.id)
        assertTrue(trashed.panelMappings.isEmpty())
        val restored = ProjectEdits.restoreFromTrash(trashed, requireNotNull(item))
        assertEquals(1, restored.panelMappings.size)
        assertEquals(ConnectionState.AVAILABLE, ConnectionGraph(restored).state(restored.panelMappings.single().portAId))
    }

    @Test fun pathSchematicCrossesTheJunctionFromEitherSide() {
        val a = device("SW"); val b = device("PC")
        val cabled = HardwareConfigurator.connect(project(a, b), a.ports.single().id, b.ports.single().id, CableMedium.ETHERNET_COPPER)
        val (p, box) = junctionBox(cabled)
        val split = HardwareConfigurator.insertPassage(p, cabled.cables.single().id, p.sites.single().devices.first { it.id == box.id }.ports.single { it.hardware.side == PortSide.REAR }.id)
        val path = requireNotNull(PathSchematics.of(split, a.ports.single().id))
        assertEquals(listOf("SW", "GB-1", "PC"), path.stations.map { it.device?.technicalName })
        assertEquals(ConnectionState.COMPLETE, path.state)
        // From the junction front port, both directions are drawn, with the junction in focus.
        val front = split.sites.single().devices.first { it.id == box.id }.ports.single { it.hardware.side == PortSide.FRONT }
        val both = requireNotNull(PathSchematics.of(split, front.id))
        assertEquals(setOf("SW", "PC"), setOf(both.stations.first().device?.technicalName, both.stations.last().device?.technicalName))
        assertEquals("GB-1", both.stations[both.focus].device?.technicalName)
        assertEquals("SW/P1 – GB-1/P1", CableLabels.suggest(split, split.cables.single { it.id == cabled.cables.single().id }))
    }
}
