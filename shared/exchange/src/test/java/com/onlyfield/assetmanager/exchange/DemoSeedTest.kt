package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.forms.PortArrangement
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

class DemoSeedTest {
    private val project = DemoSeed.build()
    private val devices = project.sites.flatMap { it.devices }
    private val graph = ConnectionGraph(project)
    private fun named(name: String) = devices.single { it.technicalName == name }
    private fun port(device: String, name: String) = named(device).ports.single { it.name == name }.id
    /** Device at the far end of the physical path starting from [portId]. */
    private fun end(portId: String): String? {
        val last = graph.trace(portId).lastOrNull()?.cable ?: return null
        val from = graph.trace(portId).last().currentPort?.id
        val far = if (last.portAId == from) last.portBId else last.portAId
        return devices.firstOrNull { d -> d.ports.any { it.id == far } }?.technicalName
    }

    @Test fun municipalityHasFourSitesAndValidStructure() {
        assertEquals(listOf("COM", "TEA", "MED", "MAT"), project.sites.map { it.code })
        assertEquals(listOf(5, 1, 3, 2), project.sites.map { it.areas.size })
        val result = ModelValidator.validateProject(project)
        assertTrue(result.issues.filter { it.severity == ValidationSeverity.STRUCTURAL_ERROR }.toString(), result.isValid)
        val count = devices.groupingBy { it.objectTypeId!! }.eachCount()
        assertEquals(6 * DemoSeed.OUTLETS, count["outlet"])
        assertEquals(4, count["radio-bridge"])
        assertEquals(14, count["switch"])
    }

    @Test fun everyOperationalFloorHasTwo48PortPanelsThreeQuartersCabled() {
        val operatingRacks = project.racks.filter { it.heightU == 42 }.map { it.id }.toSet()
        val panels = devices.filter { it.objectTypeId == "patch-panel" && it.ports.first().hardware.connector == "RJ45" && it.rackId in operatingRacks }
        assertEquals(12, panels.size)
        assertTrue(panels.all { p -> p.ports.count { it.hardware.side == PortSide.REAR } == 48 })
        panels.groupBy { it.technicalName.dropLast(2) }.forEach { (floor, pair) ->
            assertEquals(floor, 72, pair.sumOf { p -> p.ports.count { it.hardware.side == PortSide.REAR && graph.occupied(it.id) } })
        }
    }

    @Test fun endpointsUplinksAndWanAreComplete() {
        devices.filter { it.objectTypeId == "access-point" }.forEach { assertEquals(it.technicalName, ConnectionState.COMPLETE, graph.state(it.ports.single().id)) }
        devices.filter { it.objectTypeId == "switch" && it.technicalName != "SW-COM-CORE" }.forEach { sw ->
            assertEquals(sw.technicalName, ConnectionState.COMPLETE, graph.state(sw.ports.single { it.name == "X1" }.id))
        }
        assertEquals("SW-COM-CORE", end(port("SW-COM-P1-B", "X1")))
        assertEquals("SW-COM-CORE", end(port("FW-COM-01", "LAN1")))
        assertEquals("RTR-COM-01", end(port("ONT-COM-01", "LAN1")))
    }

    @Test fun radioLinksJoinMunicipioSchoolAndKindergarten() {
        assertEquals(ConnectionState.COMPLETE, graph.state(port("SW-COM-P1-A", "P40")))
        assertEquals("SW-MED-PT-A", end(port("SW-COM-P1-A", "P40")))
        assertEquals("SW-MED-PT-A", end(port("SW-MAT-PT-A", "P40")))
        assertEquals(2, project.cables.count { it.medium == CableMedium.RADIO })
    }

    @Test fun junctionBoxKeepsTheTheatrePathComplete() {
        named("GB-TEA-PT-01").ports.forEach { assertEquals(ConnectionState.COMPLETE, graph.state(it.id)) }
        assertEquals("SW-TEA-PT-A", end(named("AP-TEA-PT-01").ports.single().id))
    }

    @Test fun demoPackageImports() {
        val payloads = DemoMedia.payloads(project)
        for (password in listOf(null, "demo-test-password")) {
            val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(project, attachments = payloads, password = password), password)
            assertTrue(result.validationResult.issues.toString(), result.validationResult.isValid)
            requireNotNull(result.pkg).use { pkg ->
                assertEquals(project, pkg.project)
                assertEquals(payloads.keys, pkg.attachments.keys)
                payloads.forEach { (path, bytes) -> assertEquals(path, PackageSerializer.calculateSha256(bytes), pkg.payloadChecksum(path)) }
            }
        }
    }

    @Test fun labOffersAllConnectionAndSurveyStatesWithoutStructuralErrors() {
        assertEquals(ConnectionState.COMPLETE, graph.state(port("SW-COM-LAB", "P1")))
        assertEquals(ConnectionState.CONFLICT, graph.state(port("SW-COM-LAB", "P2")))
        assertEquals(ConnectionState.INCOMPLETE, graph.state(port("SW-COM-LAB", "P3")))
        assertEquals(ConnectionState.INCOMPLETE, graph.state(port("SW-COM-LAB", "P6")))
        assertEquals(ConnectionState.AVAILABLE, graph.state(port("SW-COM-LAB", "P7")))
        assertEquals(ObservationStatus.entries.toSet(), devices.map { it.observation.effectiveStatus() }.toSet())
        assertEquals(OperationalStatus.entries.toSet(), devices.map { it.operationalStatus }.toSet())
        val result = ModelValidator.validateProject(project)
        assertTrue(result.issues.toString(), result.isValid)
        assertEquals(setOf("SW-COM-LAB", "CAM-COM-LAB").map { named(it).id }.toSet(), result.issues.filter { it.code == "UNVERIFIED_DEVICE_OBSERVATION" }.map { it.targetEntityId }.toSet())
        assertTrue(result.issues.any { it.code == "DETACHED_CABLE_ENDPOINT" && it.targetEntityId == project.cables.single { c -> c.codeOrLabel == "APERTO-LAB-01" }.id })
    }

    @Test fun searchableModelsApplyWithoutLosingPortIdentityOrCableEndpoints() {
        val switches = project.deviceModels.filter { it.objectTypeId == "switch" }
        assertEquals(12, switches.size)
        assertEquals(ObjectKind.entries.toSet(), project.deviceModels.map { it.kind }.toSet())
        val before = named("SW-COM-LAB")
        val model = switches.single { it.name == "DEMO Switch 08 – PoE tutte le porte" }
        val draft = HardwareConfigurator.applyModel(MapObjectDraft.forDevice(project, before), model)
        assertTrue(draft.errors(project).toString(), draft.errors(project).isEmpty())
        val updated = draft.apply(project)
        val after = updated.sites.flatMap { it.devices }.single { it.id == before.id }
        assertEquals(before.ports.map { it.id }, after.ports.map { it.id })
        assertEquals(project.cables, updated.cables)
        val rack = project.racks.single { it.name == "RK-COM-LAB" }
        assertEquals(12, rack.heightU)
        assertEquals(NumberingDirection.TOP_TO_BOTTOM, rack.numberingDirection)
        assertNotNull(rack.deviceModelId)
        assertTrue(devices.any { it.rackSide == RackSide.REAR && it.rackId != null })
        assertEquals(2, ObjectHierarchy.ancestors(project, ObjectRef(PlacementTargetType.DEVICE, named("SENS-COM-LAB").id)).size)
    }

    @Test fun hardwareAndLogicalDocumentationCoverLayoutPoePowerAndNetwork() {
        val sw = named("SW-COM-LAB")
        assertEquals(listOf("2", "1", "4", "3", "6", "5", "8", "7"), PortArrangement.layout(sw, sw.ports.filter { it.hardware.group == "P" }).order)
        assertEquals(PoeStandard.IEEE_802_3BT, sw.ports.single { it.name == "P3" }.hardware.poeStandard)
        assertNull(sw.ports.single { it.name == "P4" }.hardware.poeStandard)
        assertEquals(setOf(10, 20, 30, 90), project.vlans.map { it.vlanId }.toSet())
        assertEquals(4, project.subnets.size)
        assertEquals(setOf(PortVlanMode.ACCESS, PortVlanMode.TRUNK), project.portVlanMemberships.map { it.mode }.toSet())
        assertEquals(2, project.lagGroups.size)
        val server = named("SRV-COM-01")
        assertTrue(server.hardware.redundantPower)
        assertEquals(setOf(PowerFeedType.PRIMARY_A, PowerFeedType.SECONDARY_B), project.powerFeeds.filter { it.deviceId == server.id }.map { it.feedType }.toSet())
        assertEquals(named("NVR-COM-LAB").id, project.videoSurveillanceMappings.single().managerDeviceId)
    }

    @Test fun generatedFixtureIncludesDecodableMediaForEveryTargetAndClassification() {
        val fixture = File("../../fixtures/demo/onlyfield-demo.ofam")
        val result = PackageSerializer.importPackage(fixture.readBytes())
        assertTrue(result.validationResult.issues.toString(), result.validationResult.isValid)
        requireNotNull(result.pkg).use { pkg ->
            assertEquals(366, pkg.project.sites.sumOf { it.devices.size })
            assertEquals(14, pkg.project.deviceModels.size)
            assertEquals(AttachmentTargetType.entries.toSet(), pkg.project.attachments.map { it.targetType }.toSet())
            assertEquals(AttachmentClassification.entries.toSet(), pkg.project.attachments.map { it.classification }.toSet())
            assertEquals(7, pkg.attachments.size)
            pkg.project.attachments.forEach { a ->
                val image = ImageIO.read(ByteArrayInputStream(pkg.attachments.getValue(a.relativePath)))
                assertNotNull(a.name, image)
                assertEquals(1200, image.width)
                assertEquals(800, image.height)
            }
            assertEquals(2, pkg.project.sites.flatMap { it.areas }.count { it.floorplanAttachmentId != null })
        }
    }

    @Test fun documentFiltersExcludeConfidentialAndUnreviewedLabContent() {
        val public = DocumentSelection(project, ExportFilterConfig(includeConfidential = false, reviewRequiredConfirmed = false)).project
        assertEquals(4, public.attachments.size)
        assertTrue(public.attachments.all { it.classification == AttachmentClassification.SHAREABLE })
        assertEquals(listOf("Inventario"), public.customExtraFields.map { it.fieldKey })
        val complete = DocumentSelection(project, ExportFilterConfig(includeConfidential = true, reviewRequiredConfirmed = true)).project
        assertEquals(project.attachments, complete.attachments)
        val lab = project.sites.single { it.code == "COM" }.areas.single { it.name == "Laboratorio collaudi" }
        val labOnly = DocumentSelection(project, ExportFilterConfig(selectedAreaId = lab.id)).project
        assertTrue(labOnly.sites.flatMap { it.devices }.any { it.id == named("SENS-COM-LAB").id })
        assertFalse(labOnly.sites.flatMap { it.devices }.any { it.technicalName == "SW-COM-CORE" })
    }
}
