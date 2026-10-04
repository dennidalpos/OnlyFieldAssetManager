package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.EndpointStatus
import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.model.RackSide
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ModelValidatorTest {

    @Test
    fun testValidProjectValidation() {
        val buId = UUID.randomUUID().toString()
        val siteId = UUID.randomUUID().toString()
        val areaId = UUID.randomUUID().toString()

        val dev1Id = UUID.randomUUID().toString()
        val dev2Id = UUID.randomUUID().toString()

        val port1Id = UUID.randomUUID().toString()
        val port2Id = UUID.randomUUID().toString()

        val port1 = Port(
            id = port1Id,
            deviceId = dev1Id,
            name = "ge-0/0/1",
            connectedPortId = port2Id,
            endpointStatus = EndpointStatus.CONNECTED
        )

        val port2 = Port(
            id = port2Id,
            deviceId = dev2Id,
            name = "ge-0/0/1",
            connectedPortId = port1Id,
            endpointStatus = EndpointStatus.CONNECTED
        )

        val dev1 = Device(
            id = dev1Id,
            technicalName = "sw-access-01",
            ipAddress = "10.0.0.1",
            siteId = siteId,
            areaId = areaId,
            ports = listOf(port1),
            observation = Observation("audit", System.currentTimeMillis(), ObservationStatus.VERIFIED)
        )

        val dev2 = Device(
            id = dev2Id,
            technicalName = "sw-access-02",
            ipAddress = "10.0.0.2",
            siteId = siteId,
            areaId = areaId,
            ports = listOf(port2),
            observation = Observation("audit", System.currentTimeMillis(), ObservationStatus.VERIFIED)
        )

        val area = Area(id = areaId, name = "Server Room")
        val site = Site(id = siteId, name = "HQ Building", areas = listOf(area))
        val bu = BusinessUnit(id = buId, name = "IT Ops", sites = listOf(site), devices = listOf(dev1, dev2))

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Test Project",
            createdEpochMs = System.currentTimeMillis(),
            updatedEpochMs = System.currentTimeMillis(),
            businessUnits = listOf(bu)
        )

        val result = ModelValidator.validateProject(project)
        assertTrue(result.isValid)
        assertFalse(result.hasWarnings)
    }

    @Test
    fun testInvalidUuidGeneratesStructuralError() {
        val project = Project(
            id = "not-a-valid-uuid",
            name = "Bad Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L
        )

        val result = ModelValidator.validateProject(project)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { (code, _, severity) -> (code == "INVALID_PROJECT_UUID") && (severity == ValidationSeverity.STRUCTURAL_ERROR) })
    }

    @Test
    fun testDuplicateIpInSameBuGeneratesWarning() {
        val buId = UUID.randomUUID().toString()
        val siteId = UUID.randomUUID().toString()

        val dev1 = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "sw-01",
            ipAddress = "192.168.1.1",
            siteId = siteId
        )

        val dev2 = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "sw-02",
            ipAddress = "192.168.1.1",
            siteId = siteId
        )

        val site = Site(id = siteId, name = "Main Site")
        val bu = BusinessUnit(id = buId, name = "BU1", sites = listOf(site), devices = listOf(dev1, dev2))

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Duplicate IP Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu)
        )

        val result = ModelValidator.validateProject(project)
        assertTrue(result.isValid)
        assertTrue(result.hasWarnings)
        assertTrue(result.issues.any { it.code == "DUPLICATE_IP_IN_BU" })
    }

    @Test
    fun testRackOverlapGeneratesWarning() {
        val rackId = UUID.randomUUID().toString()
        val rack = Rack(id = rackId, name = "Rack 01", heightU = 42)

        val dev1 = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "sw-01",
            rackId = rackId,
            positionU = 10,
            heightU = 2,
            rackSide = RackSide.FRONT
        )

        val dev2 = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "sw-02",
            rackId = rackId,
            positionU = 11, // Overlaps U10-11
            heightU = 1,
            rackSide = RackSide.FRONT
        )

        val bu = BusinessUnit(id = UUID.randomUUID().toString(), name = "BU1", devices = listOf(dev1, dev2))
        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Rack Overlap Test",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            racks = listOf(rack),
            businessUnits = listOf(bu)
        )

        val result = ModelValidator.validateProject(project)
        assertTrue(result.isValid)
        assertTrue(result.issues.any { it.code == "RACK_SLOT_OVERLAP" })
    }

    @Test
    fun testBrokenPortConnectionGeneratesStructuralError() {
        val devId = UUID.randomUUID().toString()
        val port = Port(
            id = UUID.randomUUID().toString(),
            deviceId = devId,
            name = "eth0",
            connectedPortId = UUID.randomUUID().toString() // Non-existent target port
        )

        val dev = Device(
            id = devId,
            technicalName = "dev-01",
            siteId = UUID.randomUUID().toString(),
            ports = listOf(port)
        )

        val bu = BusinessUnit(id = UUID.randomUUID().toString(), name = "BU1", devices = listOf(dev))
        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Broken Port Connection",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu)
        )

        val result = ModelValidator.validateProject(project)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.code == "BROKEN_PORT_CONNECTION" })
    }

    @Test
    fun testAttachmentAndFloorplanValidation() {
        val attId = UUID.randomUUID().toString()
        val areaId = UUID.randomUUID().toString()
        val devId = UUID.randomUUID().toString()

        val attachment = com.onlyfield.assetmanager.core.model.Attachment(
            id = attId,
            name = "Planimetria.png",
            originalFileName = "plan.png",
            fileType = com.onlyfield.assetmanager.core.model.AttachmentType.IMAGE,
            mimeType = "image/png",
            relativePath = "attachments/plan.png",
            classification = com.onlyfield.assetmanager.core.model.AttachmentClassification.REVIEW_REQUIRED
        )

        val area = Area(id = areaId, name = "Sala CED", floorplanAttachmentId = attId)
        val dev = Device(id = devId, technicalName = "sw-ced-01", areaId = areaId)
        val bu = BusinessUnit(id = UUID.randomUUID().toString(), name = "BU1", areas = listOf(area), devices = listOf(dev))

        val placement = com.onlyfield.assetmanager.core.model.FloorplanPlacement(
            areaId = areaId,
            targetType = com.onlyfield.assetmanager.core.model.PlacementTargetType.DEVICE,
            targetId = devId,
            xRatio = 0.5f,
            yRatio = 0.5f
        )

        val annotation = com.onlyfield.assetmanager.core.model.Annotation(
            areaId = areaId,
            type = com.onlyfield.assetmanager.core.model.AnnotationType.TEXT,
            x1Ratio = 0.1f,
            y1Ratio = 0.1f,
            label = "Zona Racks"
        )

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Attachment Floorplan Test",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu),
            attachments = listOf(attachment),
            annotations = listOf(annotation),
            floorplanPlacements = listOf(placement)
        )

        val result = ModelValidator.validateProject(project)
        assertTrue(result.isValid)
        assertTrue(result.hasWarnings)
        assertTrue(result.issues.any { it.code == "ATTACHMENT_NEEDS_REVIEW" })
    }

    @Test
    fun testCableSharedPathAndPanelMappingValidation() {
        val areaId = UUID.randomUUID().toString()
        val area = Area(id = areaId, name = "Sala CED")

        val pathSegId = UUID.randomUUID().toString()
        val pathSegment = com.onlyfield.assetmanager.core.model.SharedPathSegment(
            id = pathSegId,
            name = "Cavedio A-B",
            sourceAreaId = areaId,
            targetAreaId = areaId,
            capacityMaxCables = 1
        )

        val dev1Id = UUID.randomUUID().toString()
        val dev2Id = UUID.randomUUID().toString()
        val port1Id = UUID.randomUUID().toString()
        val port2Id = UUID.randomUUID().toString()

        val port1 = Port(id = port1Id, deviceId = dev1Id, name = "port1")
        val port2 = Port(id = port2Id, deviceId = dev2Id, name = "port2")

        val dev1 = Device(id = dev1Id, technicalName = "dev1", areaId = areaId, ports = listOf(port1))
        val dev2 = Device(id = dev2Id, technicalName = "dev2", areaId = areaId, ports = listOf(port2))
        val bu = BusinessUnit(id = UUID.randomUUID().toString(), name = "BU1", areas = listOf(area), devices = listOf(dev1, dev2))

        // Cable 1: Port1 -> Port2 using SharedPathSegment
        val cable1 = com.onlyfield.assetmanager.core.model.Cable(
            id = UUID.randomUUID().toString(),
            codeOrLabel = "Cavo 01",
            portAId = port1Id,
            portBId = port2Id,
            medium = com.onlyfield.assetmanager.core.model.CableMedium.ETHERNET_COPPER,
            orientation = com.onlyfield.assetmanager.core.model.CableOrientation.A_TO_B,
            sharedPathSegmentIds = listOf(pathSegId)
        )

        // Cable 2: Port1 -> Detached (portBId null) using same SharedPathSegment (exceeds max capacity 1)
        val cable2 = com.onlyfield.assetmanager.core.model.Cable(
            id = UUID.randomUUID().toString(),
            codeOrLabel = "Cavo 02",
            portAId = port1Id,
            portBId = null,
            medium = com.onlyfield.assetmanager.core.model.CableMedium.FIBER_OVERALL,
            sharedPathSegmentIds = listOf(pathSegId)
        )

        val mapping = com.onlyfield.assetmanager.core.model.PanelMapping(
            id = UUID.randomUUID().toString(),
            portAId = port1Id,
            portBId = null,
            isUnknownPassage = true
        )

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Cabling Test Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu),
            sharedPathSegments = listOf(pathSegment),
            cables = listOf(cable1, cable2),
            panelMappings = listOf(mapping)
        )

        val result = ModelValidator.validateProject(project)
        assertTrue(result.isValid)
        assertTrue(result.hasWarnings)
        assertTrue(result.issues.any { it.code == "SHARED_PATH_CAPACITY_EXCEEDED" })
        assertTrue(result.issues.any { it.code == "DETACHED_CABLE_ENDPOINT" })
        assertTrue(result.issues.any { it.code == "UNKNOWN_PASSAGE_IN_CHAIN" })
    }

    @Test
    fun testLogicNetworkAndA08Validation() {
        val devId = UUID.randomUUID().toString()
        val portId = UUID.randomUUID().toString()
        val port = Port(id = portId, deviceId = devId, name = "Gi0/1")
        val dev = Device(id = devId, technicalName = "sw-core-01", siteId = UUID.randomUUID().toString(), ports = listOf(port))
        val bu = BusinessUnit(id = UUID.randomUUID().toString(), name = "BU1", devices = listOf(dev))

        val vlan1 = com.onlyfield.assetmanager.core.model.Vlan(vlanId = 10, name = "MGMT")
        val vlanDuplicateScope = com.onlyfield.assetmanager.core.model.Vlan(vlanId = 10, name = "MGMT_DUP")
        val invalidVlan = com.onlyfield.assetmanager.core.model.Vlan(vlanId = 5000, name = "BAD_VLAN")

        val subnet1 = com.onlyfield.assetmanager.core.model.Subnet(cidrBlock = "10.10.0.0/24")
        val invalidSubnet = com.onlyfield.assetmanager.core.model.Subnet(cidrBlock = "invalid-cidr")

        val portMembership = com.onlyfield.assetmanager.core.model.PortVlanMembership(
            portId = portId,
            mode = com.onlyfield.assetmanager.core.model.PortVlanMode.ACCESS,
            untaggedVlanId = 10
        )

        val logicalInt = com.onlyfield.assetmanager.core.model.LogicalInterface(
            deviceId = devId,
            name = "vlan10",
            ipAddress = "10.10.0.1",
            subnetCidr = "10.10.0.0/24",
            vlanId = 10
        )

        val lagGroup = com.onlyfield.assetmanager.core.model.LagGroup(
            deviceId = devId,
            name = "lag1",
            memberPortIds = listOf(portId)
        )

        val devConfig = com.onlyfield.assetmanager.core.model.DeviceConfiguration(
            deviceId = devId,
            title = "v1.0-config",
            configText = "hostname sw-core-01"
        )

        val wanVpn = com.onlyfield.assetmanager.core.model.WanVpnConnection(
            name = "WAN_Primary",
            type = com.onlyfield.assetmanager.core.model.WanVpnType.WAN,
            localEndpointDeviceId = devId
        )

        val videoMapping = com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping(
            cameraDeviceId = devId,
            channelNumber = 1
        )

        val customField = com.onlyfield.assetmanager.core.model.CustomExtraField(
            targetType = "DEVICE",
            targetId = devId,
            fieldKey = "Criticality",
            fieldValue = "HIGH",
            classification = com.onlyfield.assetmanager.core.model.AttachmentClassification.REVIEW_REQUIRED
        )

        val invalidProject = Project(
            id = UUID.randomUUID().toString(),
            name = "A08 Test Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu),
            vlans = listOf(vlan1, vlanDuplicateScope, invalidVlan),
            subnets = listOf(subnet1, invalidSubnet),
            portVlanMemberships = listOf(portMembership),
            logicalInterfaces = listOf(logicalInt),
            lagGroups = listOf(lagGroup),
            deviceConfigurations = listOf(devConfig),
            wanVpnConnections = listOf(wanVpn),
            videoSurveillanceMappings = listOf(videoMapping),
            customExtraFields = listOf(customField)
        )

        val result = ModelValidator.validateProject(invalidProject)
        assertFalse(result.isValid) // Invalid VLAN number & invalid subnet CIDR produce structural errors
        assertTrue(result.issues.any { it.code == "INVALID_VLAN_NUMBER" })
        assertTrue(result.issues.any { it.code == "INVALID_SUBNET_CIDR" })
        assertTrue(result.issues.any { it.code == "DUPLICATE_VLAN_IN_SCOPE" })
        assertTrue(result.issues.any { it.code == "CUSTOM_FIELD_NEEDS_REVIEW" })
    }

    @Test
    fun testPowerFeedsValidationAndBadgeDerivation() {
        val dev1Id = UUID.randomUUID().toString()
        val dev2Id = UUID.randomUUID().toString()
        val portId = UUID.randomUUID().toString()

        val dev1 = com.onlyfield.assetmanager.core.model.Device(
            id = dev1Id,
            technicalName = "SW-01",
            category = com.onlyfield.assetmanager.core.model.DeviceCategory.NETWORK_SWITCH,
            ports = listOf(com.onlyfield.assetmanager.core.model.Port(id = portId, deviceId = dev1Id, name = "Gi1/0/1"))
        )
        val dev2 = com.onlyfield.assetmanager.core.model.Device(
            id = dev2Id,
            technicalName = "UPS-01",
            category = com.onlyfield.assetmanager.core.model.DeviceCategory.UPS_PDU
        )

        val bu = com.onlyfield.assetmanager.core.model.BusinessUnit(
            id = UUID.randomUUID().toString(),
            name = "BU Power",
            devices = listOf(dev1, dev2)
        )

        // Single feed partial coverage warning
        val feedA = com.onlyfield.assetmanager.core.model.PowerFeed(
            deviceId = dev1Id,
            feedName = "Feed A",
            feedType = com.onlyfield.assetmanager.core.model.PowerFeedType.PRIMARY_A,
            sourceDeviceId = dev2Id,
            loadVa = 350.0,
            loadWatts = 300.0,
            observedRuntimeMinutes = 15,
            observedSource = "Test Measurement",
            observedEpochMs = System.currentTimeMillis()
        )

        val poe = com.onlyfield.assetmanager.core.model.PoeMapping(
            portId = portId,
            role = com.onlyfield.assetmanager.core.model.PoeRole.PSE_SOURCE,
            standard = com.onlyfield.assetmanager.core.model.PoeStandard.IEEE_802_3AT,
            allocatedPowerWatts = 30.0
        )

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "A09 Test Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu),
            powerFeeds = listOf(feedA),
            poeMappings = listOf(poe)
        )

        val result = ModelValidator.validateProject(project)
        assertTrue(result.isValid)
        assertTrue(result.issues.any { it.code == "SINGLE_FEED_PARTIAL_COVERAGE_WARNING" })

        val derivedBadges = ModelValidator.deriveBadges(project, "DEVICE", dev1Id)
        assertTrue(derivedBadges.any { it.category == com.onlyfield.assetmanager.core.model.BadgeCategory.COVERAGE })
        assertTrue(derivedBadges.any { it.category == com.onlyfield.assetmanager.core.model.BadgeCategory.UPS_DEPENDENCY })
        assertTrue(derivedBadges.any { it.category == com.onlyfield.assetmanager.core.model.BadgeCategory.POE })
    }

    @Test
    fun testMergeTargetsValidation() {
        val sameId = UUID.randomUUID().toString()
        val diffId = UUID.randomUUID().toString()

        val invalidResult = ModelValidator.validateMergeTargets(sameId, sameId)
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.issues.any { it.code == "CANNOT_MERGE_SAME_DEVICE" })

        val validResult = ModelValidator.validateMergeTargets(sameId, diffId)
        assertTrue(validResult.isValid)
    }

    @Test
    fun testBatchEditPreviewGeneration() {
        val dev1 = Device(id = UUID.randomUUID().toString(), technicalName = "sw-01")
        val dev2 = Device(id = UUID.randomUUID().toString(), technicalName = "sw-02")
        val changes = com.onlyfield.assetmanager.core.model.BatchDeviceChanges(
            observationNotes = "Batch updated notes",
            updateObservationNotes = true
        )

        val preview = ModelValidator.generateBatchEditPreview(listOf(dev1, dev2), changes)
        assertTrue(preview.targetDeviceIds.size == 2)
        assertTrue(preview.affectedDeviceNames.contains("sw-01"))
        assertTrue(preview.changesSummary.isNotEmpty())
        assertFalse(preview.isProhibitedFieldAttempted)
    }

    @Test
    fun wanVpnWithoutOrWithCoincidentEndpointsIsOnlyADocumentaryWarning() {
        val fw = com.onlyfield.assetmanager.core.model.Device(technicalName = "FW")
        val empty = com.onlyfield.assetmanager.core.model.WanVpnConnection(name = "VPN-0")
        val loop = com.onlyfield.assetmanager.core.model.WanVpnConnection(name = "VPN-1", localEndpointDeviceId = fw.id, remoteEndpointDeviceId = fw.id)
        val ok = com.onlyfield.assetmanager.core.model.WanVpnConnection(name = "VPN-2", localEndpointDeviceId = fw.id, remoteEndpointSiteDescription = "Sede B")
        val project = com.onlyfield.assetmanager.core.model.Project(name = "P", createdEpochMs = 1, updatedEpochMs = 1,
            businessUnits = listOf(com.onlyfield.assetmanager.core.model.BusinessUnit(name = "BU", devices = listOf(fw))), wanVpnConnections = listOf(empty, loop, ok))
        val issues = ModelValidator.validateProject(project).issues.filter { it.code.startsWith("WAN_VPN_") }
        org.junit.Assert.assertEquals(listOf("WAN_VPN_WITHOUT_ENDPOINTS" to empty.id, "WAN_VPN_SAME_DEVICE" to loop.id), issues.map { it.code to it.targetEntityId })
        assertTrue(issues.all { it.severity == ValidationSeverity.DOCUMENTARY_WARNING })
    }
}
