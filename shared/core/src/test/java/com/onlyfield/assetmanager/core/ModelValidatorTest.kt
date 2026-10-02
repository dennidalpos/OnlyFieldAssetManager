package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.EndpointStatus
import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import org.junit.Assert.assertEquals
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
        assertTrue(result.issues.any { it.code == "INVALID_PROJECT_UUID" && it.severity == ValidationSeverity.STRUCTURAL_ERROR })
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
    fun testDuplicateIpAcrossDifferentBusIsAllowed() {
        val bu1Id = UUID.randomUUID().toString()
        val bu2Id = UUID.randomUUID().toString()
        val site1Id = UUID.randomUUID().toString()
        val site2Id = UUID.randomUUID().toString()

        val dev1 = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "router-01",
            ipAddress = "192.168.1.1",
            siteId = site1Id
        )

        val dev2 = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "router-01", // Duplicate name cross-BU allowed
            ipAddress = "192.168.1.1",   // Duplicate IP cross-BU allowed
            siteId = site2Id
        )

        val bu1 = BusinessUnit(id = bu1Id, name = "BU Operations", sites = listOf(Site(id = site1Id, name = "Site A")), devices = listOf(dev1))
        val bu2 = BusinessUnit(id = bu2Id, name = "BU Branch", sites = listOf(Site(id = site2Id, name = "Site B")), devices = listOf(dev2))

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Cross BU Scope Test",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu1, bu2)
        )

        val result = ModelValidator.validateProject(project)
        assertTrue(result.isValid)
        assertFalse(result.issues.any { it.code == "DUPLICATE_IP_IN_BU" })
        assertFalse(result.issues.any { it.code == "DUPLICATE_DEVICE_NAME_IN_BU" })
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
}
