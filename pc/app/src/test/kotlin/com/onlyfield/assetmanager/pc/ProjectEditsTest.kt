package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ProjectEditsTest {

    private fun createSampleProject(): Project {
        val now = System.currentTimeMillis()
        val area = Area(id = "area-1", name = "Sala Server")
        val bu = BusinessUnit(
            id = "bu-1",
            name = "Sede Principale",
            areas = listOf(area),
            devices = listOf(
                Device(
                    id = "dev-1",
                    technicalName = "SW-CORE-01",
                    category = DeviceCategory.NETWORK_SWITCH,
                    areaId = "area-1",
                    ports = listOf(
                        Port(id = "port-1", deviceId = "dev-1", name = "Gi1/0/1"),
                        Port(id = "port-2", deviceId = "dev-1", name = "Gi1/0/2")
                    )
                ),
                Device(
                    id = "dev-2",
                    technicalName = "SW-ACCESS-01",
                    category = DeviceCategory.NETWORK_SWITCH,
                    areaId = "area-1"
                )
            )
        )
        val rack = Rack(id = "rack-1", name = "Rack 01", areaId = "area-1", heightU = 42)
        val model = DeviceModel(
            id = "model-1",
            name = "Cisco Catalyst 2960",
            category = DeviceCategory.NETWORK_SWITCH,
            defaultHeightU = 1,
            portTemplates = listOf(PortTemplate(namePrefix = "Gi1/0/", startNumber = 1, portCount = 24))
        )
        val cable = Cable(id = "cable-1", portAId = "port-1", portBId = "other-port")

        return Project(
            id = "proj-1",
            name = "Project Test Desktop",
            createdEpochMs = now,
            updatedEpochMs = now,
            businessUnits = listOf(bu),
            racks = listOf(rack),
            deviceModels = listOf(model),
            cables = listOf(cable)
        )
    }

    @Test
    fun testAddAndUpdateDevice() {
        var proj = createSampleProject()
        val newDev = Device(id = "dev-3", technicalName = "SRV-DB-01", category = DeviceCategory.SERVER_STORAGE)

        proj = ProjectEdits.addDevice(proj, "bu-1", newDev)
        assertEquals(3, proj.businessUnits[0].devices.size)

        val updatedDev = newDev.copy(physicalLabel = "LBL-SRV-01")
        proj = ProjectEdits.updateDevice(proj, updatedDev)
        val found = proj.businessUnits[0].devices.find { it.id == "dev-3" }
        assertNotNull(found)
        assertEquals("LBL-SRV-01", found?.physicalLabel)
    }

    @Test
    fun testDeleteDeviceToTrashAndDisconnectCables() {
        var proj = createSampleProject()
        val (updatedProj, trashItem) = ProjectEdits.deleteDeviceToTrash(proj, "dev-1")

        assertNotNull(trashItem)
        assertEquals("DEVICE", trashItem?.itemType)
        assertEquals("SW-CORE-01", trashItem?.displayName)

        assertEquals(1, updatedProj.businessUnits[0].devices.size)
        // Cable should have portAId set to null because port-1 was on dev-1
        val cable = updatedProj.cables.find { it.id == "cable-1" }
        assertNotNull(cable)
        assertNull(cable?.portAId)
        assertEquals("other-port", cable?.portBId)

        // Test Restore
        val restoredProj = ProjectEdits.restoreFromTrash(updatedProj, trashItem!!)
        assertEquals(2, restoredProj.businessUnits[0].devices.size)
    }

    @Test
    fun testBatchEditDevices() {
        var proj = createSampleProject()
        val changes = BatchDeviceChanges(
            updateCategory = true,
            category = DeviceCategory.SERVER_STORAGE,
            updateRackId = true,
            rackId = "rack-1"
        )

        proj = ProjectEdits.batchEditDevices(proj, listOf("dev-1", "dev-2"), changes)
        for (dev in proj.businessUnits[0].devices) {
            assertEquals(DeviceCategory.SERVER_STORAGE, dev.category)
            assertEquals("rack-1", dev.rackId)
        }
    }

    @Test
    fun testReplaceDevice() {
        var proj = createSampleProject()
        val (updatedProj, trashItem) = ProjectEdits.replaceDevice(
            proj,
            oldDeviceId = "dev-1",
            newTechnicalName = "SW-CORE-NEXTGEN",
            newCategory = DeviceCategory.NETWORK_SWITCH
        )

        assertNotNull(trashItem)
        assertEquals("SW-CORE-01", trashItem?.displayName)

        val newDev = updatedProj.businessUnits[0].devices.find { it.technicalName == "SW-CORE-NEXTGEN" }
        assertNotNull(newDev)
        assertEquals(DeviceCategory.NETWORK_SWITCH, newDev?.category)
    }

    @Test
    fun testMergeDevices() {
        var proj = createSampleProject()
        val choices = MergeDataChoices(
            useTechnicalNameFromDuplicate = true,
            mergePorts = true
        )

        val (updatedProj, trashItem) = ProjectEdits.mergeDevices(
            proj,
            survivingDeviceId = "dev-1",
            duplicateDeviceId = "dev-2",
            choices = choices
        )

        assertNotNull(trashItem)
        assertEquals(1, updatedProj.businessUnits[0].devices.size)
        val survivor = updatedProj.businessUnits[0].devices[0]
        assertEquals("SW-ACCESS-01", survivor.technicalName) // Used duplicate name
    }

    @Test
    fun testRacksManagement() {
        var proj = createSampleProject()
        val newRack = Rack(id = "rack-2", name = "Rack 02 Core", heightU = 24)

        proj = ProjectEdits.addRack(proj, newRack)
        assertEquals(2, proj.racks.size)

        val updated = newRack.copy(notes = "Note rack 02")
        proj = ProjectEdits.updateRack(proj, updated)
        assertEquals("Note rack 02", proj.racks.find { it.id == "rack-2" }?.notes)

        val (projAfterDel, trashItem) = ProjectEdits.deleteRackToTrash(proj, "rack-2")
        assertNotNull(trashItem)
        assertEquals(1, projAfterDel.racks.size)
    }

    @Test
    fun testDeviceModelsAndPortTemplates() {
        var proj = createSampleProject()
        val model = proj.deviceModels[0]

        val ports = ProjectEdits.generatePortsFromTemplates(model, "dev-2")
        assertEquals(24, ports.size)
        assertEquals("Gi1/0/1", ports[0].name)
        assertEquals("Gi1/0/24", ports[23].name)

        proj = ProjectEdits.applyModelToDevice(proj, "dev-2", model.id)
        val dev2 = proj.businessUnits[0].devices.find { it.id == "dev-2" }
        assertEquals(24, dev2?.ports?.size)
        assertEquals(model.id, dev2?.deviceModelId)
    }

    @Test
    fun testAttachmentsAndFloorplans() {
        var proj = createSampleProject()
        val attachment = Attachment(
            id = "att-1",
            name = "Planimetria Piano 1",
            originalFileName = "p1.png",
            relativePath = "media/p1.png"
        )

        proj = ProjectEdits.addAttachment(proj, attachment)
        assertEquals(1, proj.attachments.size)

        proj = ProjectEdits.setAreaFloorplan(proj, "area-1", attachment.id)
        val area = proj.businessUnits[0].areas.find { it.id == "area-1" }
        assertEquals(attachment.id, area?.floorplanAttachmentId)

        val placement = FloorplanPlacement(
            id = "place-1",
            areaId = "area-1",
            targetType = PlacementTargetType.RACK,
            targetId = "rack-1",
            xRatio = 0.5f,
            yRatio = 0.3f
        )
        proj = ProjectEdits.addFloorplanPlacement(proj, placement)
        assertEquals(1, proj.floorplanPlacements.size)

        proj = ProjectEdits.deleteFloorplanPlacement(proj, "place-1")
        assertEquals(0, proj.floorplanPlacements.size)
    }

    @Test
    fun testCablingAndSharedPaths() {
        var proj = createSampleProject()

        val cable = Cable(id = "cable-2", codeOrLabel = "CBL-002", portAId = "port-1", portBId = "port-2")
        proj = ProjectEdits.addCable(proj, cable)
        assertEquals(2, proj.cables.size)

        val updatedCable = cable.copy(color = "Rosso", lengthValue = 10.0)
        proj = ProjectEdits.updateCable(proj, updatedCable)
        assertEquals("Rosso", proj.cables.find { it.id == "cable-2" }?.color)

        val segment = SharedPathSegment(id = "seg-1", name = "Canalina Principale Piano 1", capacityMaxCables = 50)
        proj = ProjectEdits.addSharedPathSegment(proj, segment)
        assertEquals(1, proj.sharedPathSegments.size)

        val panelMap = PanelMapping(id = "map-1", portAId = "port-1", portBId = "port-2", mappingType = "PATCH_PANEL")
        proj = ProjectEdits.addPanelMapping(proj, panelMap)
        assertEquals(1, proj.panelMappings.size)

        proj = ProjectEdits.deleteCable(proj, "cable-2")
        assertEquals(1, proj.cables.size)

        proj = ProjectEdits.deleteSharedPathSegment(proj, "seg-1")
        assertEquals(0, proj.sharedPathSegments.size)

        proj = ProjectEdits.deletePanelMapping(proj, "map-1")
        assertEquals(0, proj.panelMappings.size)
    }

    @Test
    fun testLogicalNetworkAndSubnets() {
        var proj = createSampleProject()

        val vlan = Vlan(id = "vlan-10", vlanId = 10, name = "VLAN_DATA")
        proj = ProjectEdits.addVlan(proj, vlan)
        assertEquals(1, proj.vlans.size)

        val subnet = Subnet(id = "sub-10", cidrBlock = "10.0.10.0/24", gatewayIp = "10.0.10.1", vlanId = "vlan-10")
        proj = ProjectEdits.addSubnet(proj, subnet)
        assertEquals(1, proj.subnets.size)

        val logInt = LogicalInterface(id = "int-10", deviceId = "dev-1", name = "Vlan10", ipAddress = "10.0.10.254")
        proj = ProjectEdits.addLogicalInterface(proj, logInt)
        assertEquals(1, proj.logicalInterfaces.size)

        val wanConn = WanVpnConnection(id = "wan-1", name = "MPLS-Primary", providerOrCarrier = "Telecom")
        proj = ProjectEdits.addWanVpnConnection(project = proj, connection = wanConn)
        assertEquals(1, proj.wanVpnConnections.size)

        proj = ProjectEdits.deleteVlan(proj, "vlan-10")
        assertEquals(0, proj.vlans.size)
    }

    @Test
    fun testPowerFeedsAndBadges() {
        var proj = createSampleProject()

        val feed = PowerFeed(id = "feed-1", deviceId = "dev-1", feedName = "Feed A - Main UPS", voltageVolts = 230, loadWatts = 450.0)
        proj = ProjectEdits.addPowerFeed(proj, feed)
        assertEquals(1, proj.powerFeeds.size)

        val poe = PoeMapping(id = "poe-1", portId = "port-1", role = PoeRole.PSE_SOURCE, allocatedPowerWatts = 30.0)
        proj = ProjectEdits.addOrUpdatePoeMapping(proj, poe)
        assertEquals(1, proj.poeMappings.size)

        val badge = DocumentBadge(id = "badge-1", targetType = "DEVICE", targetId = "dev-1", label = "CRITICAL_CORE")
        proj = ProjectEdits.addDocumentBadge(proj, badge)
        assertEquals(1, proj.documentBadges.size)

        proj = ProjectEdits.deletePowerFeed(proj, "feed-1")
        assertEquals(0, proj.powerFeeds.size)
    }
}

