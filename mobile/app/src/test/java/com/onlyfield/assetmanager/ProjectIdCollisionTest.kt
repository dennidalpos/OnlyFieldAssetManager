package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProjectIdCollisionTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun everyInventoryTableRejectsCrossProjectIdsForNewAndExistingProjects() = runBlocking {
        val db = database()
        try {
            val repo = ProjectRepository(db)
            val owner = fixture("Owner"); val local = fixture("Local")
            repo.saveProject(owner); repo.saveProject(local)
            val variants = collisions(local, owner)
            assertEquals(24, variants.size)
            for ((table, collision) in variants) for (replace in listOf(false, true)) {
                val incoming = if (replace) collision else collision.copy(id = java.util.UUID.randomUUID().toString())
                val error = runCatching { repo.saveProject(incoming) }.exceptionOrNull()
                assertTrue(table, error is IllegalStateException)
                assertEquals(table, owner, repo.getProjectById(owner.id))
                assertEquals(table, local, repo.getProjectById(local.id))
                if (!replace) assertNull(table, repo.getProjectById(incoming.id))
            }
            repo.saveProject(local.copy(name = "Ordinary edit"))
            assertEquals("Ordinary edit", repo.getProjectById(local.id)!!.name)
            assertEquals(owner, repo.getProjectById(owner.id))
        } finally { db.close() }
    }

    @Test fun collisionAfterTheFirstQueryChunkIsRejected() = runBlocking {
        val db = database()
        try {
            val repo = ProjectRepository(db); val owner = fixture("Owner")
            repo.saveProject(owner)
            val devices = List(1000) { Device(technicalName = "Device $it") } + owner.sites.single().devices.single().copy(ports = emptyList())
            val incoming = Project(name = "Large", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(Site(name = "Site", devices = devices)))
            assertNotNull(runCatching { repo.saveProject(incoming) }.exceptionOrNull())
            assertEquals(owner, repo.getProjectById(owner.id)); assertNull(repo.getProjectById(incoming.id))
            val corrected = incoming.copy(sites = listOf(incoming.sites.single().copy(devices = devices.dropLast(1))))
            repo.saveProject(corrected)
            assertEquals(corrected, repo.getProjectById(corrected.id))
        } finally { db.close() }
    }

    @Test fun importReplacementAndMergePreserveBothProjectsMediaVerifiersBasesAndTrash() = runBlocking {
        for (mode in listOf("new", "replace", "merge")) {
            val db = database()
            try {
                val root = folder.newFolder(); val repo = ProjectRepository(db, root, "dummy-recovery-key")
                val owner = basicFixture("Owner"); val original = basicFixture("Local")
                repo.saveProject(owner); repo.saveProject(original)
                repo.setProjectPassword(owner.id, null, "owner-password"); repo.setProjectPassword(original.id, null, "local-password")
                val deleted = Device(technicalName = "Trash")
                val originalWithTrash = repo.getProjectById(original.id)!!.let { it.copy(sites = listOf(it.sites.single().copy(devices = it.sites.single().devices + deleted))) }
                repo.saveProject(originalWithTrash); repo.moveToTrash(original.id, "DEVICE", deleted.id)
                val ownerBefore = repo.getProjectById(owner.id)!!; val localBefore = repo.getProjectById(original.id)!!
                val ownerFile = AttachmentFiles.localFile(root, owner.id, owner.attachments.single()).apply { parentFile!!.mkdirs(); writeText("owner bytes") }
                val localFile = AttachmentFiles.localFile(root, original.id, original.attachments.single()).apply { parentFile!!.mkdirs(); writeText("local bytes") }
                repo.exportProjectPackage(owner.id, "owner-password"); repo.exportProjectPackage(original.id, "local-password")
                val ownerBase = repo.getSyncBase(owner.id); val localBase = repo.getSyncBase(original.id); val trash = repo.getTrashItems(original.id)
                for ((table, collision) in collisions(localBefore, ownerBefore).filter { it.first in listOf("sites", "areas", "devices", "ports", "credentials", "attachments") }) {
                    val incoming = collision.copy(id = if (mode == "new") java.util.UUID.randomUUID().toString() else original.id, isPasswordProtected = false)
                    val bytes = PackageSerializer.exportPackage(incoming, attachments = mapOf(AttachmentFiles.entryName(incoming.attachments.single()) to "incoming bytes".toByteArray()))
                    PackageSerializer.importPackage(bytes).pkg!!.use { pkg ->
                        val error = runCatching {
                            if (mode == "merge") repo.importMergedPackage(pkg, incoming) else repo.importProjectPackage(pkg)
                        }.exceptionOrNull()
                        assertTrue("$mode/$table", error is IllegalStateException)
                    }
                    assertEquals(ownerBefore, repo.getProjectById(owner.id)); assertEquals(localBefore, repo.getProjectById(original.id))
                    assertEquals(ownerBase, repo.getSyncBase(owner.id)); assertEquals(localBase, repo.getSyncBase(original.id))
                    assertEquals(trash, repo.getTrashItems(original.id)); assertTrue(repo.getTrashItems(owner.id).isEmpty())
                    assertTrue(repo.verifyProjectPassword(owner.id, "owner-password")); assertTrue(repo.verifyProjectPassword(original.id, "local-password"))
                    assertEquals("owner bytes", ownerFile.readText()); assertEquals("local bytes", localFile.readText())
                    if (mode == "new") assertNull(repo.getProjectById(incoming.id))
                }
            } finally { db.close() }
        }
    }

    @Test fun restoreRejectsDevicePortAndAssociatedIdsOwnedElsewhereThenAllowsRetry() = runBlocking {
        for (table in listOf("devices", "ports", "credentials", "device_configurations", "power_feeds", "custom_extra_fields")) {
            val db = database()
            try {
                val root = folder.newFolder(); val repo = ProjectRepository(db, root, "dummy-recovery-key")
                val local = fixture("Local"); val device = local.sites.single().devices.single()
                repo.saveProject(local); repo.setProjectPassword(local.id, null, "local-password")
                val file = AttachmentFiles.localFile(root, local.id, local.attachments.single()).apply { parentFile!!.mkdirs(); writeText("local bytes") }
                repo.moveToTrash(local.id, "DEVICE", device.id)
                val deleted = repo.getProjectById(local.id)!!; val trash = repo.getTrashItems(local.id).single()
                val owner = collisions(fixture("Owner"), local).single { it.first == table }.second
                repo.saveProject(owner); repo.setProjectPassword(owner.id, null, "owner-password")
                val ownerBefore = repo.getProjectById(owner.id)!!
                val ownerFile = AttachmentFiles.localFile(root, owner.id, owner.attachments.single()).apply { parentFile!!.mkdirs(); writeText("owner bytes") }
                repo.exportProjectPackage(local.id, "local-password"); repo.exportProjectPackage(owner.id, "owner-password")
                val localBase = repo.getSyncBase(local.id); val ownerBase = repo.getSyncBase(owner.id)
                assertTrue(table, runCatching { repo.restoreFromTrash(local.id, trash.id) }.exceptionOrNull() is IllegalStateException)
                assertEquals(deleted, repo.getProjectById(local.id)); assertEquals(ownerBefore, repo.getProjectById(owner.id))
                assertEquals(listOf(trash), repo.getTrashItems(local.id)); assertTrue(repo.getTrashItems(owner.id).isEmpty())
                assertEquals(localBase, repo.getSyncBase(local.id)); assertEquals(ownerBase, repo.getSyncBase(owner.id))
                assertTrue(repo.verifyProjectPassword(local.id, "local-password")); assertTrue(repo.verifyProjectPassword(owner.id, "owner-password"))
                assertEquals("local bytes", file.readText()); assertEquals("owner bytes", ownerFile.readText())
                repo.deleteProject(owner.id)
                assertTrue(repo.restoreFromTrash(local.id, trash.id))
                assertEquals(device, repo.getProjectById(local.id)!!.sites.single().devices.single())
                assertTrue(repo.getTrashItems(local.id).isEmpty()); assertEquals("local bytes", file.readText())
            } finally { db.close() }
        }
    }

    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()

    private fun basicFixture(name: String): Project {
        val device = Device(technicalName = name); val port = Port(deviceId = device.id, name = "1")
        return Project(name = name, createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "Site", areas = listOf(Area(name = "Floor")), devices = listOf(device.copy(ports = listOf(port))))),
            credentials = listOf(Credential(deviceId = device.id, username = "admin", secret = "dummy-$name-secret")),
            attachments = listOf(Attachment(name = "Photo", originalFileName = "photo.bin", relativePath = "")))
    }

    private fun fixture(name: String): Project {
        val project = basicFixture(name); val site = project.sites.single(); val area = site.areas.single()
        val device = site.devices.single(); val port = device.ports.single()
        return project.copy(racks = listOf(Rack(name = "Rack", areaId = area.id)), deviceModels = listOf(DeviceModel(name = "Model", category = DeviceCategory.NETWORK_SWITCH)),
            annotations = listOf(Annotation(areaId = area.id, x1Ratio = 0.2f, y1Ratio = 0.2f)),
            floorplanPlacements = listOf(FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE, targetId = device.id, xRatio = 0.2f, yRatio = 0.2f)),
            cables = listOf(Cable(deviceAId = device.id)), panelMappings = listOf(PanelMapping(portAId = port.id, isUnknownPassage = true)),
            vlans = listOf(Vlan(vlanId = 10, name = "Management")), subnets = listOf(Subnet(cidrBlock = "192.0.2.0/24")),
            portVlanMemberships = listOf(PortVlanMembership(portId = port.id)), logicalInterfaces = listOf(LogicalInterface(deviceId = device.id, name = "Management")),
            lagGroups = listOf(LagGroup(deviceId = device.id, name = "LAG")), deviceConfigurations = listOf(DeviceConfiguration(deviceId = device.id, title = "Config", capturedEpochMs = 1)),
            wanVpnConnections = listOf(WanVpnConnection(name = "WAN")), videoSurveillanceMappings = listOf(VideoSurveillanceMapping(cameraDeviceId = device.id)),
            customExtraFields = listOf(CustomExtraField(targetType = "DEVICE", targetId = device.id, fieldKey = "private", fieldValue = "dummy-value", classification = AttachmentClassification.CONFIDENTIAL)),
            powerFeeds = listOf(PowerFeed(deviceId = device.id, feedName = "A")), poeMappings = listOf(PoeMapping(portId = port.id)),
            documentBadges = listOf(DocumentBadge(targetType = "DEVICE", targetId = device.id, label = "Keep")))
    }

    private fun collisions(local: Project, owner: Project): List<Pair<String, Project>> {
        val site = local.sites.single(); val device = site.devices.single(); val ownerSite = owner.sites.single(); val ownerDevice = ownerSite.devices.single()
        return listOf(
            "sites" to local.copy(sites = listOf(site.copy(id = ownerSite.id))),
            "areas" to local.copy(sites = listOf(site.copy(areas = listOf(site.areas.single().copy(id = ownerSite.areas.single().id))))),
            "devices" to local.copy(sites = listOf(site.copy(devices = listOf(device.copy(id = ownerDevice.id, ports = device.ports.map { it.copy(deviceId = ownerDevice.id) })))),
                credentials = local.credentials.map { it.copy(deviceId = ownerDevice.id) }),
            "ports" to local.copy(sites = listOf(site.copy(devices = listOf(device.copy(ports = listOf(device.ports.single().copy(id = ownerDevice.ports.single().id))))))),
            "credentials" to local.copy(credentials = listOf(local.credentials.single().copy(id = owner.credentials.single().id))),
            "racks" to local.copy(racks = owner.racks.take(1).map { it.copy(areaId = site.areas.single().id) }),
            "device_models" to local.copy(deviceModels = owner.deviceModels),
            "attachments" to local.copy(attachments = listOf(local.attachments.single().copy(id = owner.attachments.single().id))),
            "annotations" to local.copy(annotations = owner.annotations.map { it.copy(areaId = site.areas.single().id) }),
            "floorplan_placements" to local.copy(floorplanPlacements = owner.floorplanPlacements.map { it.copy(areaId = site.areas.single().id, targetId = device.id) }),
            "cables" to local.copy(cables = owner.cables.map { it.copy(deviceAId = device.id) }),
            "panel_mappings" to local.copy(panelMappings = owner.panelMappings.map { it.copy(portAId = device.ports.single().id) }),
            "vlans" to local.copy(vlans = owner.vlans), "subnets" to local.copy(subnets = owner.subnets),
            "port_vlan_memberships" to local.copy(portVlanMemberships = owner.portVlanMemberships.map { it.copy(portId = device.ports.single().id) }),
            "logical_interfaces" to local.copy(logicalInterfaces = owner.logicalInterfaces.map { it.copy(deviceId = device.id) }),
            "lag_groups" to local.copy(lagGroups = owner.lagGroups.map { it.copy(deviceId = device.id) }),
            "device_configurations" to local.copy(deviceConfigurations = owner.deviceConfigurations.map { it.copy(deviceId = device.id) }),
            "wan_vpn_connections" to local.copy(wanVpnConnections = owner.wanVpnConnections),
            "video_surveillance_mappings" to local.copy(videoSurveillanceMappings = owner.videoSurveillanceMappings.map { it.copy(cameraDeviceId = device.id) }),
            "custom_extra_fields" to local.copy(customExtraFields = owner.customExtraFields.map { it.copy(targetId = device.id) }),
            "power_feeds" to local.copy(powerFeeds = owner.powerFeeds.map { it.copy(deviceId = device.id) }),
            "poe_mappings" to local.copy(poeMappings = owner.poeMappings.map { it.copy(portId = device.ports.single().id) }),
            "document_badges" to local.copy(documentBadges = owner.documentBadges.map { it.copy(targetId = device.id) }),
        )
    }
    @Test fun savingADeviceIdOwnedByAnotherProjectMustNotReplaceItsTree() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = ProjectRepository(db)
            val device = Device(technicalName = "Owner")
            val port = Port(deviceId = device.id, name = "1")
            val owner = Project(name = "Owner", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Site", devices = listOf(device.copy(ports = listOf(port))))))
            repo.saveProject(owner)
            val incoming = Project(name = "Incoming", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Site", devices = listOf(device.copy(technicalName = "Overwrite")))))
            assertNotNull(runCatching { repo.saveProject(incoming) }.exceptionOrNull())
            assertEquals(owner, repo.getProjectById(owner.id))
            assertNull(repo.getProjectById(incoming.id))
        } finally { db.close() }
    }
}
