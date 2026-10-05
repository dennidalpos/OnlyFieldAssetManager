package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import org.junit.Assert.*
import org.junit.Test

class DemoSeedTest {
    private val project = DemoSeed.build()
    private val devices = project.businessUnits.flatMap { it.devices }
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
        assertEquals(listOf("COM", "TEA", "MED", "MAT"), project.businessUnits.map { it.code })
        assertEquals(listOf(4, 1, 3, 2), project.businessUnits.map { it.areas.size })
        val result = ModelValidator.validateProject(project)
        assertTrue(result.issues.filter { it.severity == ValidationSeverity.STRUCTURAL_ERROR }.toString(), result.isValid)
        val count = devices.groupingBy { it.objectTypeId!! }.eachCount()
        assertEquals(6 * DemoSeed.OUTLETS, count["outlet"])
        assertEquals(4, count["radio-bridge"])
        assertEquals(13, count["switch"])
    }

    @Test fun everyFloorHasTwo48PortPanelsThreeQuartersCabled() {
        val panels = devices.filter { it.objectTypeId == "patch-panel" && it.ports.first().hardware.connector == "RJ45" }
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
        val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(project))
        assertEquals(project.cables.size, result.pkg?.project?.cables?.size)
    }
}
