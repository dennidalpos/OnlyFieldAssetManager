package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.ComparisonStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ProjectRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = ProjectRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSaveAndReloadProjectPreservesAllDataAndIds() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val buId = UUID.randomUUID().toString()
        val siteId = UUID.randomUUID().toString()
        val devId = UUID.randomUUID().toString()
        val portId = UUID.randomUUID().toString()

        val port = Port(
            id = portId,
            deviceId = devId,
            name = "ge-0/0/1",
            label = "Pannello A - Porta 1"
        )

        val device = Device(
            id = devId,
            technicalName = "sw-access-01",
            physicalLabel = "Targhetta-1234",
            alias = "Switch CED",
            ipAddress = "192.168.1.50",
            siteId = siteId,
            ports = listOf(port),
            observation = Observation(source = "Test", timestampEpochMs = 1000L, status = ObservationStatus.VERIFIED)
        )

        val site = Site(id = siteId, name = "Sede Milano")
        val bu = BusinessUnit(id = buId, name = "BU Operations", sites = listOf(site), devices = listOf(device))

        val project = Project(
            id = projId,
            name = "Progetto Test Persistence",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu)
        )

        // Save project
        repository.saveProject(project)

        // Reload project
        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertEquals(projId, reloaded!!.id)
        assertEquals("Progetto Test Persistence", reloaded.name)
        assertEquals(1, reloaded.businessUnits.size)

        val reloadedBu = reloaded.businessUnits[0]
        assertEquals(buId, reloadedBu.id)
        assertEquals(1, reloadedBu.sites.size)
        assertEquals(siteId, reloadedBu.sites[0].id)

        assertEquals(1, reloadedBu.devices.size)
        val reloadedDev = reloadedBu.devices[0]
        assertEquals(devId, reloadedDev.id)
        assertEquals("sw-access-01", reloadedDev.technicalName)
        assertEquals("Targhetta-1234", reloadedDev.physicalLabel)
        assertEquals("Switch CED", reloadedDev.alias)
        assertEquals("192.168.1.50", reloadedDev.ipAddress)

        assertEquals(1, reloadedDev.ports.size)
        assertEquals(portId, reloadedDev.ports[0].id)
    }

    @Test
    fun testRenameProject() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val project = Project(
            id = projId,
            name = "Nome Iniziale",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L
        )

        repository.saveProject(project)
        repository.renameProject(projId, "Nome Aggiornato")

        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertEquals("Nome Aggiornato", reloaded!!.name)
    }

    @Test
    fun testSearchInventoryByTechnicalNameIpLabelAndAlias() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val buId = UUID.randomUUID().toString()

        val dev1 = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "router-core-hq",
            physicalLabel = "RTR-01",
            alias = "Gateway Principale",
            ipAddress = "10.0.0.1"
        )

        val dev2 = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "sw-poe-floor1",
            physicalLabel = "SW-POE-01",
            alias = "Switch Telecamere",
            ipAddress = "10.0.0.50"
        )

        val bu = BusinessUnit(id = buId, name = "HQ BU", devices = listOf(dev1, dev2))
        val project = Project(
            id = projId,
            name = "Search Test Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu)
        )

        repository.saveProject(project)

        // 1. Search by technical name
        val res1 = repository.searchInventory(projId, "router")
        assertEquals(1, res1.size)
        assertEquals("router-core-hq", res1[0].device.technicalName)

        // 2. Search by IP
        val res2 = repository.searchInventory(projId, "10.0.0.50")
        assertEquals(1, res2.size)
        assertEquals("sw-poe-floor1", res2[0].device.technicalName)

        // 3. Search by Physical Label
        val res3 = repository.searchInventory(projId, "RTR-01")
        assertEquals(1, res3.size)
        assertEquals("router-core-hq", res3[0].device.technicalName)

        // 4. Search by Alias
        val res4 = repository.searchInventory(projId, "Telecamere")
        assertEquals(1, res4.size)
        assertEquals("sw-poe-floor1", res4[0].device.technicalName)
    }

    @Test
    fun testExportAndImportPackageStream() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val project = Project(
            id = projId,
            name = "Export Stream Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(BusinessUnit(id = UUID.randomUUID().toString(), name = "BU Test"))
        )

        repository.saveProject(project)

        // Export to Stream
        val baos = ByteArrayOutputStream()
        val exportSuccess = repository.exportProjectPackageToStream(projId, baos)
        assertTrue(exportSuccess)

        val zipBytes = baos.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        // Evaluate Import on a fresh database instance
        val db2 = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val repo2 = ProjectRepository(db2)

        val eval = repo2.evaluateImportPackage(ByteArrayInputStream(zipBytes))
        assertTrue(eval.importResult.validationResult.isValid)
        assertNotNull(eval.importResult.pkg)
        assertNotNull(eval.comparison)
        assertEquals(ComparisonStatus.NEWER_REVISION, eval.comparison!!.status)

        // Import project package
        val importSuccess = repo2.importProjectPackage(eval.importResult.pkg!!)
        assertTrue(importSuccess)

        val imported = repo2.getProjectById(projId)
        assertNotNull(imported)
        assertEquals(projId, imported!!.id)
        assertEquals("Export Stream Project", imported.name)

        db2.close()
    }

    @Test
    fun testCorruptPackageImportDoesNotTouchDatabase() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val original = Project(
            id = projId,
            name = "Original Intact Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L
        )

        repository.saveProject(original)

        val badBytes = "corrupted byte stream".toByteArray()
        val eval = repository.evaluateImportPackage(ByteArrayInputStream(badBytes), projId)

        assertFalse(eval.importResult.validationResult.isValid)
        assertNull(eval.importResult.pkg)
        assertNull(eval.comparison)

        // Ensure database state was NOT altered
        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertEquals("Original Intact Project", reloaded!!.name)
    }
}
