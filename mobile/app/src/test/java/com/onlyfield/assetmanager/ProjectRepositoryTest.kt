package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.display.GlobalSearch
import com.onlyfield.assetmanager.core.display.HitKind
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.ConnectionGraph
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.model.Credential
import com.onlyfield.assetmanager.core.model.CredentialType
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.core.model.DeviceModel
import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.OperationalStatus
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.model.RackSide
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
        val siteId = UUID.randomUUID().toString()
        val devId = UUID.randomUUID().toString()
        val portId = UUID.randomUUID().toString()
        val rackId = UUID.randomUUID().toString()
        val modelId = UUID.randomUUID().toString()

        val port = Port(
            id = portId,
            deviceId = devId,
            name = "ge-0/0/1",
            label = "Pannello A - Porta 1"
        )

        val rack = Rack(
            id = rackId,
            name = "Rack CED 01",
            heightU = 42
        )

        val model = DeviceModel(
            id = modelId,
            name = "Cisco Catalyst 9200",
            brand = "Cisco",
            modelNumber = "C9200-24T",
            category = DeviceCategory.NETWORK_SWITCH,
            defaultHeightU = 1
        )

        val device = Device(
            id = devId,
            technicalName = "sw-access-01",
            physicalLabel = "Targhetta-1234",
            alias = "Switch CED",
            ipAddress = "192.168.1.50",
            ports = listOf(port),
            rackId = rackId,
            positionU = 10,
            heightU = 1,
            rackSide = RackSide.FRONT,
            operationalStatus = OperationalStatus.OFF,
            deviceModelId = modelId,
            category = DeviceCategory.NETWORK_SWITCH,
            observation = Observation(source = "Test", timestampEpochMs = 1000L, status = ObservationStatus.VERIFIED)
        )

        val cred = Credential(
            username = "admin_network",
            secret = "SecurePassword99",
            type = CredentialType.PASSWORD,
            groupName = "Core Switches"
        )

        val site = Site(id = siteId, name = "Sede Milano", group = "Uffici", address = "Via Roma 1", devices = listOf(device))

        val project = Project(
            id = projId,
            name = "Progetto Test Persistence",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            sites = listOf(site),
            credentials = listOf(cred),
            racks = listOf(rack),
            deviceModels = listOf(model)
        )

        repository.saveProject(project)

        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertEquals(projId, reloaded!!.id)
        assertEquals("Progetto Test Persistence", reloaded.name)
        assertEquals(1, reloaded.sites.size)

        val reloadedSite = reloaded.sites[0]
        assertEquals(siteId, reloadedSite.id)
        assertEquals("Uffici", reloadedSite.group)
        assertEquals("Via Roma 1", reloadedSite.address)

        assertEquals(1, reloadedSite.devices.size)
        val reloadedDev = reloadedSite.devices[0]
        assertEquals(devId, reloadedDev.id)
        assertEquals("sw-access-01", reloadedDev.technicalName)
        assertEquals("Targhetta-1234", reloadedDev.physicalLabel)
        assertEquals(OperationalStatus.OFF, reloadedDev.operationalStatus)
        assertEquals("Switch CED", reloadedDev.alias)
        assertEquals("192.168.1.50", reloadedDev.ipAddress)
        assertEquals(rackId, reloadedDev.rackId)
        assertEquals(10, reloadedDev.positionU)
        assertEquals(1, reloadedDev.heightU)
        assertEquals(RackSide.FRONT, reloadedDev.rackSide)
        assertEquals(DeviceCategory.NETWORK_SWITCH, reloadedDev.category)

        assertEquals(1, reloadedDev.ports.size)
        assertEquals(portId, reloadedDev.ports[0].id)

        assertEquals(1, reloaded.credentials.size)
        assertEquals("admin_network", reloaded.credentials[0].username)

        assertEquals(1, reloaded.racks.size)
        assertEquals("Rack CED 01", reloaded.racks[0].name)

        assertEquals(1, reloaded.deviceModels.size)
        assertEquals("Cisco Catalyst 9200", reloaded.deviceModels[0].name)
    }

    @Test
    fun testProjectPasswordManagement() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val project = Project(
            id = projId,
            name = "Password Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L
        )

        repository.saveProject(project)

        assertTrue(repository.verifyProjectPassword(projId, "any_pass"))

        val setOk = repository.setProjectPassword(projId, currentPassword = null, newPassword = "MySecretPassword")
        assertTrue(setOk)

        assertFalse(repository.verifyProjectPassword(projId, "WrongPass"))
        assertTrue(repository.verifyProjectPassword(projId, "MySecretPassword"))

        val changeWrong = repository.setProjectPassword(projId, currentPassword = "WrongPass", newPassword = "NewSecretPassword")
        assertFalse(changeWrong)

        val changeRight = repository.setProjectPassword(projId, currentPassword = "MySecretPassword", newPassword = "NewSecretPassword")
        assertTrue(changeRight)
        assertTrue(repository.verifyProjectPassword(projId, "NewSecretPassword"))

        val removeRight = repository.removeProjectPassword(projId, currentPassword = "NewSecretPassword")
        assertTrue(removeRight)
        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertFalse(reloaded!!.isPasswordProtected)
    }

    @Test
    fun syncBaseFollowsExportAndSurvivesSaves() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val p = Project(id = projId, name = "Base", createdEpochMs = 1L, updatedEpochMs = 1L)
        repository.saveProject(p)
        assertEquals(null, repository.getSyncBase(projId))
        repository.exportProjectPackage(projId)
        repository.saveProject(p.copy(name = "Rinominato", updatedEpochMs = 2L))
        assertEquals("Base", repository.getSyncBase(projId)!!.name)
        repository.deleteProject(projId)
        assertEquals(null, repository.getSyncBase(projId))
    }

    @Test
    fun serialNumberIsStoredAndSearchable() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val device = Device(technicalName = "SW-SER", serialNumber = "FOC1234X0AB")
        repository.saveProject(
            Project(id = projId, name = "Seriali", createdEpochMs = 1L, updatedEpochMs = 1L,
                sites = listOf(Site(name = "BU", devices = listOf(device))))
        )
        assertEquals("FOC1234X0AB", repository.getProjectById(projId)!!.sites.single().devices.single().serialNumber)
        assertEquals(listOf(device.id), GlobalSearch(repository.getProjectById(projId)!!).search("1234X0").map { it.id })
    }

    @Test
    fun legacySha256PasswordIsUpgradedOnUnlock() = runBlocking {
        val projId = UUID.randomUUID().toString()
        repository.saveProject(Project(id = projId, name = "Legacy", createdEpochMs = 1L, updatedEpochMs = 1L))
        // Legacy SHA-256 of "password".
        val legacy = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
        val dao = db.projectDao()
        dao.updateProject(dao.getProjectById(projId)!!.copy(isPasswordProtected = true, passwordHash = legacy))

        assertFalse(repository.verifyProjectPassword(projId, "wrong"))
        assertEquals(legacy, dao.getProjectById(projId)!!.passwordHash)
        assertTrue(repository.verifyProjectPassword(projId, "password"))
        val upgraded = dao.getProjectById(projId)!!.passwordHash!!
        assertTrue(upgraded.startsWith("pbkdf2-sha256$"))
        assertTrue(repository.verifyProjectPassword(projId, "password"))
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
        val siteId = UUID.randomUUID().toString()

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

        val site = Site(id = siteId, name = "HQ BU", devices = listOf(dev1, dev2))
        val project = Project(
            id = projId,
            name = "Search Test Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            sites = listOf(site)
        )

        repository.saveProject(project)

        val search = GlobalSearch(repository.getProjectById(projId)!!)
        val res1 = search.search("router").filter { it.kind == HitKind.DEVICE }
        assertEquals(1, res1.size)
        assertEquals("router-core-hq", res1[0].title)

        val res2 = search.search("10.0.0.50").filter { it.kind == HitKind.DEVICE }
        assertEquals(1, res2.size)
        assertEquals("sw-poe-floor1", res2[0].title)

        val res3 = search.search("RTR-01").filter { it.kind == HitKind.DEVICE }
        assertEquals(1, res3.size)
        assertEquals("router-core-hq", res3[0].title)

        val res4 = search.search("Telecamere").filter { it.kind == HitKind.DEVICE }
        assertEquals(1, res4.size)
        assertEquals("sw-poe-floor1", res4[0].title)
    }

    @Test
    fun testExportAndImportPackageStream() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val project = Project(
            id = projId,
            name = "Export Stream Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            sites = listOf(Site(id = UUID.randomUUID().toString(), name = "BU Test"))
        )

        repository.saveProject(project)

        val baos = ByteArrayOutputStream()
        val exportSuccess = repository.exportProjectPackageToStream(projId, baos)
        assertTrue(exportSuccess)

        val zipBytes = baos.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

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
        val eval = repository.evaluateImportPackage(ByteArrayInputStream(badBytes))

        assertFalse(eval.importResult.validationResult.isValid)
        assertNull(eval.importResult.pkg)
        assertNull(eval.comparison)

        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertEquals("Original Intact Project", reloaded!!.name)
    }

    @Test
    fun testAttachmentFloorplanAndAnnotationsPersistence() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val areaId = UUID.randomUUID().toString()
        val devId = UUID.randomUUID().toString()
        val attId = UUID.randomUUID().toString()

        val attachment = com.onlyfield.assetmanager.core.model.Attachment(
            id = attId,
            name = "planimetria_p1.png",
            originalFileName = "plan_p1.png",
            fileType = com.onlyfield.assetmanager.core.model.AttachmentType.IMAGE,
            mimeType = "image/png",
            relativePath = "attachments/plan_p1.png",
            classification = com.onlyfield.assetmanager.core.model.AttachmentClassification.SHAREABLE
        )

        val area = com.onlyfield.assetmanager.core.model.Area(
            id = areaId,
            name = "Sala Server 1",
            floorplanAttachmentId = attId
        )

        val device = Device(
            id = devId,
            technicalName = "sw-rack-01",
            areaId = areaId
        )

        val site = Site(
            id = UUID.randomUUID().toString(),
            name = "BU Tech",
            areas = listOf(area),
            devices = listOf(device)
        )

        val placement = com.onlyfield.assetmanager.core.model.FloorplanPlacement(
            areaId = areaId,
            targetType = com.onlyfield.assetmanager.core.model.PlacementTargetType.DEVICE,
            targetId = devId,
            xRatio = 0.4f,
            yRatio = 0.6f
        )

        val annotation = com.onlyfield.assetmanager.core.model.Annotation(
            areaId = areaId,
            type = com.onlyfield.assetmanager.core.model.AnnotationType.HIGHLIGHT_ZONE,
            x1Ratio = 0.1f,
            y1Ratio = 0.1f,
            x2Ratio = 0.3f,
            y2Ratio = 0.3f,
            label = "Zona Condizionatore",
            colorHex = "#00FF00"
        )

        val project = Project(
            id = projId,
            name = "Floorplan Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            sites = listOf(site),
            attachments = listOf(attachment),
            annotations = listOf(annotation),
            floorplanPlacements = listOf(placement)
        )

        repository.saveProject(project)

        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertEquals(1, reloaded!!.attachments.size)
        assertEquals("planimetria_p1.png", reloaded.attachments[0].name)

        assertEquals(1, reloaded.annotations.size)
        assertEquals("Zona Condizionatore", reloaded.annotations[0].label)

        assertEquals(1, reloaded.floorplanPlacements.size)
        assertEquals(0.4f, reloaded.floorplanPlacements[0].xRatio, 0.001f)

        val newAttId = UUID.randomUUID().toString()
        val newAttachment = com.onlyfield.assetmanager.core.model.Attachment(
            id = newAttId,
            name = "planimetria_p1_v2.png",
            originalFileName = "plan_p1_v2.png",
            fileType = com.onlyfield.assetmanager.core.model.AttachmentType.IMAGE,
            mimeType = "image/png",
            relativePath = "attachments/plan_p1_v2.png"
        )

        repository.saveProject(ProjectEdits.setAreaFloorplan(
            ProjectEdits.addAttachment(reloaded, newAttachment), areaId, newAttId))

        val updatedReloaded = repository.getProjectById(projId)
        assertNotNull(updatedReloaded)
        val updatedArea = updatedReloaded!!.sites[0].areas.find { it.id == areaId }
        assertNotNull(updatedArea)
        assertEquals(newAttId, updatedArea!!.floorplanAttachmentId)
        assertEquals(1, updatedReloaded.floorplanPlacements.size)
        assertEquals(0.4f, updatedReloaded.floorplanPlacements[0].xRatio, 0.001f)
    }

    @Test
    fun testCablePanelMappingAndChainTracingPersistence() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val areaId = UUID.randomUUID().toString()

        val dev1Id = UUID.randomUUID().toString()
        val dev2Id = UUID.randomUUID().toString() // Patch Panel
        val dev3Id = UUID.randomUUID().toString() // Switch Core

        val port1Id = UUID.randomUUID().toString()
        val port2FrontId = UUID.randomUUID().toString()
        val port2RearId = UUID.randomUUID().toString()
        val port3Id = UUID.randomUUID().toString()

        val port1 = Port(id = port1Id, deviceId = dev1Id, name = "ge-0/0/1")
        val port2Front = Port(id = port2FrontId, deviceId = dev2Id, name = "PP-01 FRONT")
        val port2Rear = Port(id = port2RearId, deviceId = dev2Id, name = "PP-01 REAR")
        val port3 = Port(id = port3Id, deviceId = dev3Id, name = "xe-0/0/1")

        val dev1 = Device(id = dev1Id, technicalName = "srv-app-01", areaId = areaId, ports = listOf(port1))
        val dev2 = Device(id = dev2Id, technicalName = "patch-panel-01", category = DeviceCategory.PATCH_PANEL, areaId = areaId, ports = listOf(port2Front, port2Rear))
        val dev3 = Device(id = dev3Id, technicalName = "sw-core-01", category = DeviceCategory.NETWORK_SWITCH, areaId = areaId, ports = listOf(port3))

        val site = Site(id = UUID.randomUUID().toString(), name = "BU Net", devices = listOf(dev1, dev2, dev3))

        val cable1 = com.onlyfield.assetmanager.core.model.Cable(
            id = UUID.randomUUID().toString(),
            codeOrLabel = "PATCH-01",
            portAId = port1Id,
            portBId = port2FrontId,
            medium = com.onlyfield.assetmanager.core.model.CableMedium.ETHERNET_COPPER,
            color = "Blu",
        )

        val panelMapping = com.onlyfield.assetmanager.core.model.PanelMapping(
            id = UUID.randomUUID().toString(),
            portAId = port2FrontId,
            portBId = port2RearId,
            isUnknownPassage = false
        )

        val cable2 = com.onlyfield.assetmanager.core.model.Cable(
            id = UUID.randomUUID().toString(),
            codeOrLabel = "TRUNK-01",
            portAId = port2RearId,
            portBId = port3Id,
            medium = com.onlyfield.assetmanager.core.model.CableMedium.FIBER_OVERALL,
            lengthValue = 25.0,
        )

        val project = Project(
            id = projId,
            name = "Cabling Persistence Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            sites = listOf(site),
            cables = listOf(cable1, cable2),
            panelMappings = listOf(panelMapping)
        )

        repository.saveProject(project)

        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertEquals(2, reloaded!!.cables.size)
        assertEquals("PATCH-01", reloaded.cables[0].codeOrLabel)
        assertEquals("Blu", reloaded.cables[0].color)

        assertEquals(1, reloaded.panelMappings.size)
        assertEquals(project.panelMappings, reloaded.panelMappings)

        val chain = ConnectionGraph(reloaded).trace(port1Id)
        assertTrue(chain.isNotEmpty())
        assertEquals(3, chain.size)
        assertEquals("srv-app-01", chain[0].currentDevice?.technicalName)
        assertEquals(cable1.id, chain[0].cable?.id)
        assertEquals("patch-panel-01", chain[1].currentDevice?.technicalName)
        assertEquals(panelMapping.id, chain[1].panelMapping?.id)
        assertEquals(cable2.id, chain[2].cable?.id)
    }

    @Test
    fun testA08EntitiesPersistenceAndMigration() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val devId = UUID.randomUUID().toString()
        val portId = UUID.randomUUID().toString()

        val port = Port(id = portId, deviceId = devId, name = "Gi0/1")
        val dev = Device(id = devId, technicalName = "sw-core-a08", ports = listOf(port))
        val site = Site(id = UUID.randomUUID().toString(), name = "BU A08", devices = listOf(dev))

        val vlan = com.onlyfield.assetmanager.core.model.Vlan(vlanId = 20, name = "VOIP")
        val subnet = com.onlyfield.assetmanager.core.model.Subnet(cidrBlock = "10.20.0.0/24")
        val membership = com.onlyfield.assetmanager.core.model.PortVlanMembership(portId = portId, mode = com.onlyfield.assetmanager.core.model.PortVlanMode.ACCESS, untaggedVlanId = 20)
        val l3Int = com.onlyfield.assetmanager.core.model.LogicalInterface(deviceId = devId, name = "vlan20", ipAddress = "10.20.0.1")
        val lag = com.onlyfield.assetmanager.core.model.LagGroup(deviceId = devId, name = "lag1", memberPortIds = listOf(portId))
        val config = com.onlyfield.assetmanager.core.model.DeviceConfiguration(deviceId = devId, title = "Config_A08")
        val wan = com.onlyfield.assetmanager.core.model.WanVpnConnection(name = "VPN_Branch_1", type = com.onlyfield.assetmanager.core.model.WanVpnType.VPN)
        val video = com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping(cameraDeviceId = devId, channelNumber = 2)
        val customField = com.onlyfield.assetmanager.core.model.CustomExtraField(targetType = "PROJECT", targetId = projId, fieldKey = "Env", fieldValue = "Production")

        val project = Project(
            id = projId,
            name = "A08 Persistence Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            sites = listOf(site),
            vlans = listOf(vlan),
            subnets = listOf(subnet),
            portVlanMemberships = listOf(membership),
            logicalInterfaces = listOf(l3Int),
            lagGroups = listOf(lag),
            deviceConfigurations = listOf(config),
            wanVpnConnections = listOf(wan),
            videoSurveillanceMappings = listOf(video),
            customExtraFields = listOf(customField)
        )

        repository.saveProject(project)

        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertEquals(1, reloaded!!.vlans.size)
        assertEquals(20, reloaded.vlans[0].vlanId)
        assertEquals("VOIP", reloaded.vlans[0].name)

        assertEquals(1, reloaded.subnets.size)
        assertEquals("10.20.0.0/24", reloaded.subnets[0].cidrBlock)

        assertEquals(1, reloaded.portVlanMemberships.size)
        assertEquals(portId, reloaded.portVlanMemberships[0].portId)

        assertEquals(1, reloaded.logicalInterfaces.size)
        assertEquals("vlan20", reloaded.logicalInterfaces[0].name)

        assertEquals(1, reloaded.lagGroups.size)
        assertEquals("lag1", reloaded.lagGroups[0].name)

        assertEquals(1, reloaded.deviceConfigurations.size)
        assertEquals("Config_A08", reloaded.deviceConfigurations[0].title)

        assertEquals(1, reloaded.wanVpnConnections.size)
        assertEquals("VPN_Branch_1", reloaded.wanVpnConnections[0].name)

        assertEquals(1, reloaded.videoSurveillanceMappings.size)
        assertEquals(2, reloaded.videoSurveillanceMappings[0].channelNumber)

        assertEquals(1, reloaded.customExtraFields.size)
        assertEquals("Production", reloaded.customExtraFields[0].fieldValue)
    }

    @Test
    fun testA09PowerAndBadgesPersistenceAndMigration() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val devId = UUID.randomUUID().toString()
        val portId = UUID.randomUUID().toString()

        val dev = Device(id = devId, technicalName = "sw-a09-test", ports = listOf(Port(id = portId, deviceId = devId, name = "port1")))
        val site = Site(id = UUID.randomUUID().toString(), name = "BU A09", devices = listOf(dev))

        val feed = com.onlyfield.assetmanager.core.model.PowerFeed(
            deviceId = devId,
            feedName = "Feed A",
            feedType = com.onlyfield.assetmanager.core.model.PowerFeedType.PRIMARY_A,
            loadVa = 400.0,
            loadWatts = 360.0,
            observedRuntimeMinutes = 30,
            observedSource = "UPS Display",
            observedEpochMs = 1234567890L
        )

        val poe = com.onlyfield.assetmanager.core.model.PoeMapping(
            portId = portId,
            role = com.onlyfield.assetmanager.core.model.PoeRole.PSE_SOURCE,
            standard = com.onlyfield.assetmanager.core.model.PoeStandard.IEEE_802_3AT,
            allocatedPowerWatts = 30.0
        )

        val badge = com.onlyfield.assetmanager.core.model.DocumentBadge(
            targetType = "DEVICE",
            targetId = devId,
            label = "TEST_BADGE",
            category = com.onlyfield.assetmanager.core.model.BadgeCategory.FREE_LABEL,
            isDerived = false
        )

        val project = Project(
            id = projId,
            name = "A09 Persistence Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            sites = listOf(site),
            powerFeeds = listOf(feed),
            poeMappings = listOf(poe),
            documentBadges = listOf(badge)
        )

        repository.saveProject(project)

        val reloaded = repository.getProjectById(projId)
        assertNotNull(reloaded)
        assertEquals(1, reloaded!!.powerFeeds.size)
        assertEquals("Feed A", reloaded.powerFeeds[0].feedName)
        assertEquals(400.0, reloaded.powerFeeds[0].loadVa)
        assertEquals(30, reloaded.powerFeeds[0].observedRuntimeMinutes)

        assertEquals(1, reloaded.poeMappings.size)
        assertEquals(30.0, reloaded.poeMappings[0].allocatedPowerWatts)

        assertEquals(1, reloaded.documentBadges.size)
        assertEquals("TEST_BADGE", reloaded.documentBadges[0].label)
    }

    @Test
    fun testTrashAndRestoreDevice() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val siteId = UUID.randomUUID().toString()
        val devId = UUID.randomUUID().toString()
        val portId = UUID.randomUUID().toString()

        val port = Port(id = portId, deviceId = devId, name = "ge-0/0/1")
        val dev = Device(id = devId, technicalName = "sw-trash-test", ports = listOf(port))
        val site = Site(id = siteId, name = "BU Trash", devices = listOf(dev))
        val project = Project(id = projId, name = "Trash Test Project", createdEpochMs = 1000L, updatedEpochMs = 1000L, sites = listOf(site))

        repository.saveProject(project)

        val trashItem = repository.moveToTrash(projId, "DEVICE", devId)
        assertNotNull(trashItem)
        assertEquals("sw-trash-test", trashItem!!.displayName)

        val trashList = repository.getTrashItems(projId)
        assertEquals(1, trashList.size)

        val projAfterTrash = repository.getProjectById(projId)
        assertNotNull(projAfterTrash)
        assertTrue(projAfterTrash!!.sites.flatMap { it.devices }.none { it.id == devId })

        val restored = repository.restoreFromTrash(projId, trashItem.id)
        assertTrue(restored)

        val projAfterRestore = repository.getProjectById(projId)
        assertNotNull(projAfterRestore)
        assertTrue(projAfterRestore!!.sites.flatMap { it.devices }.any { it.id == devId })
    }

    @Test
    fun testReplaceDevice() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val siteId = UUID.randomUUID().toString()
        val oldDevId = UUID.randomUUID().toString()

        val oldDev = Device(id = oldDevId, technicalName = "old-sw", ipAddress = "192.168.1.100")
        val site = Site(id = siteId, name = "BU Replace", devices = listOf(oldDev))
        val project = Project(id = projId, name = "Replace Test Project", createdEpochMs = 1000L, updatedEpochMs = 1000L, sites = listOf(site))

        repository.saveProject(project)

        val (trash, newDev) = repository.replaceDevice(
            projectId = projId,
            oldDeviceId = oldDevId,
            newTechnicalName = "new-sw-clean",
            newCategory = DeviceCategory.NETWORK_SWITCH
        )

        assertNotNull(trash)
        assertNotNull(newDev)
        assertEquals("new-sw-clean", newDev.technicalName)
        assertNull(newDev.ipAddress)

        val updatedProj = repository.getProjectById(projId)
        assertNotNull(updatedProj)
        val devices = updatedProj!!.sites.flatMap { it.devices }
        assertTrue(devices.any { it.technicalName == "new-sw-clean" })
        assertFalse(devices.any { it.id == oldDevId })
    }

    @Test
    fun testMergeDevices() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val siteId = UUID.randomUUID().toString()
        val dev1Id = UUID.randomUUID().toString()
        val dev2Id = UUID.randomUUID().toString()

        val dev1 = Device(id = dev1Id, technicalName = "sw-primary", ipAddress = "10.0.0.1")
        val dev2 = Device(id = dev2Id, technicalName = "sw-primary-dup", ipAddress = "10.0.0.2")
        val site = Site(id = siteId, name = "BU Merge", devices = listOf(dev1, dev2))
        val project = Project(id = projId, name = "Merge Test Project", createdEpochMs = 1000L, updatedEpochMs = 1000L, sites = listOf(site))

        repository.saveProject(project)

        val choices = com.onlyfield.assetmanager.core.model.MergeDataChoices(useIpFromDuplicate = true)
        val merged = repository.mergeDevices(projId, dev1Id, dev2Id, choices)

        assertNotNull(merged)
        assertEquals("10.0.0.2", merged!!.ipAddress)

        val updatedProj = repository.getProjectById(projId)
        assertNotNull(updatedProj)
        val devices = updatedProj!!.sites.flatMap { it.devices }
        assertEquals(1, devices.size)
        assertEquals(dev1Id, devices[0].id)
    }

    @Test
    fun testBatchEditDevices() = runBlocking {
        val projId = UUID.randomUUID().toString()
        val siteId = UUID.randomUUID().toString()
        val dev1Id = UUID.randomUUID().toString()
        val dev2Id = UUID.randomUUID().toString()

        val dev1 = Device(id = dev1Id, technicalName = "sw-batch-1")
        val dev2 = Device(id = dev2Id, technicalName = "sw-batch-2")
        val site = Site(id = siteId, name = "BU Batch", devices = listOf(dev1, dev2))
        val project = Project(id = projId, name = "Batch Test Project", createdEpochMs = 1000L, updatedEpochMs = 1000L, sites = listOf(site))

        repository.saveProject(project)

        val changes = com.onlyfield.assetmanager.core.model.BatchDeviceChanges(
            observationNotes = "Batch Audit Complete",
            updateObservationNotes = true
        )

        repository.saveProject(ProjectEdits.batchEditDevices(repository.getProjectById(projId)!!, listOf(dev1Id, dev2Id), changes))

        val updatedProj = repository.getProjectById(projId)
        assertNotNull(updatedProj)
        val devices = updatedProj!!.sites.flatMap { it.devices }
        assertEquals("Batch Audit Complete", devices[0].observation?.notes)
        assertEquals("Batch Audit Complete", devices[1].observation?.notes)
    }
}
