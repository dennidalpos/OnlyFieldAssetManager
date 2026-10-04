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

    @Test fun demoHasTwoSitesWithTwoFloorsAndValidStructure() {
        assertEquals(listOf(2, 2), project.businessUnits.map { it.areas.size })
        assertTrue(ModelValidator.validateProject(project).issues.filter { it.severity == ValidationSeverity.STRUCTURAL_ERROR }.toString(),
            ModelValidator.validateProject(project).isValid)
        assertEquals(4, project.racks.size)
        assertEquals(mapOf("switch" to 8, "patch-panel" to 4, "outlet" to 24, "access-point" to 8), devices.groupingBy { it.objectTypeId!! }.eachCount())
        assertTrue(devices.filter { it.objectTypeId == "switch" }.all { it.rackId != null && it.positionU != null })
    }

    @Test fun threeQuartersOfSwitchesAreCabledAndLinkedAcrossFloors() {
        val switches = devices.filter { it.objectTypeId == "switch" }
        val cabled = switches.filter { sw -> sw.ports.any { graph.occupied(it.id) } }
        assertEquals(6, cabled.size)
        assertEquals(setOf("SW-S0-B", "SW-S1-B"), (switches - cabled.toSet()).map { it.technicalName }.toSet())
        val owner = devices.flatMap { d -> d.ports.map { it.id to d } }.toMap()
        cabled.forEach { sw ->
            val farFloors = project.cables.filter { c -> sw.ports.any { it.id == c.portAId || it.id == c.portBId } }
                .mapNotNull { c -> owner[if (sw.ports.any { it.id == c.portAId }) c.portBId else c.portAId] }
                .filter { it.objectTypeId == "switch" }.map { it.areaId }
            assertTrue("${sw.technicalName} has no uplink to another floor", farFloors.any { it != sw.areaId })
        }
    }

    @Test fun accessPointsReachASwitchThroughOutletAndPanel() {
        devices.filter { it.objectTypeId == "access-point" }.forEach { ap ->
            assertEquals(ap.technicalName, ConnectionState.COMPLETE, graph.state(ap.ports.single().id))
        }
    }

    @Test fun demoPackageImports() {
        val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(project))
        assertEquals(project.cables.size, result.pkg?.project?.cables?.size)
    }
}
