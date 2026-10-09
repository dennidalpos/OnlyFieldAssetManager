package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.exchange.ComparisonStatus
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.ProjectComparisonEvaluator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class BidirectionalInteropTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataDir: File
    private lateinit var storageManager: DesktopStorageManager

    @Before
    fun setUp() {
        dataDir = tempFolder.newFolder("interop_datadir")
        storageManager = DesktopStorageManager(initialDataDir = dataDir)
    }

    private fun createFullDomainProject(): Pair<Project, Map<String, ByteArray>> {
        val projId = UUID.randomUUID().toString()
        val siteId = UUID.randomUUID().toString()
        val floorId = UUID.randomUUID().toString()
        val areaId = UUID.randomUUID().toString()
        val devId1 = UUID.randomUUID().toString()
        val devId2 = UUID.randomUUID().toString()
        val portId1 = UUID.randomUUID().toString()
        val portId2 = UUID.randomUUID().toString()
        val rackId = UUID.randomUUID().toString()
        val modelId = UUID.randomUUID().toString()

        val port1 = Port(id = portId1, deviceId = devId1, name = "Gi1/0/1", label = "Pannello A - Port 1")
        val port2 = Port(id = portId2, deviceId = devId2, name = "Gi1/0/1", label = "Pannello B - Port 1")

        val dev1 = Device(
            id = devId1,
            technicalName = "SW-CORE-01",
            physicalLabel = "TARG-001",
            alias = "Switch Core Data Center",
            ipAddress = "10.0.1.10",
            areaId = areaId,
            rackId = rackId,
            positionU = 10,
            heightU = 2,
            rackSide = RackSide.FRONT,
            ports = listOf(port1)
        )

        val dev2 = Device(
            id = devId2,
            technicalName = "SW-ACC-01",
            physicalLabel = "TARG-002",
            alias = "Switch Access Piano 1",
            ipAddress = "10.0.1.20",
            areaId = areaId,
            rackId = rackId,
            positionU = 12,
            heightU = 1,
            rackSide = RackSide.FRONT,
            ports = listOf(port2)
        )

        val rack = Rack(id = rackId, name = "Rack CED 01", areaId = areaId, heightU = 42)
        val model = DeviceModel(
            id = modelId,
            name = "Cisco Catalyst 9300",
            brand = "Cisco",
            modelNumber = "C9300-48P",
            category = DeviceCategory.NETWORK_SWITCH,
            defaultHeightU = 1
        )

        val area = Area(id = areaId, name = "Sala Server CED", floor = "Piano 1")
        val site = Site(id = siteId, name = "Network Infra BU", areas = listOf(area), devices = listOf(dev1, dev2))

        val cable = Cable(id = UUID.randomUUID().toString(), portAId = portId1, portBId = portId2, color = "BLUE", lengthValue = 5.0)

        val vlan = Vlan(vlanId = 10, name = "MGMT_VLAN", description = "Management Subnet")
        val subnet = Subnet(cidrBlock = "10.0.1.0/24", gatewayIp = "10.0.1.1", vlanId = vlan.id)
        val portVlan = PortVlanMembership(portId = portId1, mode = PortVlanMode.TAGGED, taggedVlanIds = listOf(10))
        val logicalInt = LogicalInterface(deviceId = devId1, name = "Vlan10", ipAddress = "10.0.1.10")
        val lagGroup = LagGroup(deviceId = devId1, name = "Po1", memberPortIds = listOf(portId1))
        val devConfig = DeviceConfiguration(deviceId = devId1, title = "running-config-v1", configText = "hostname SW-CORE-01")
        val wanVpn = WanVpnConnection(name = "WAN-PRIMARY-01", providerOrCarrier = "Telecom", bandwidth = "1 Gbps")
        val video = VideoSurveillanceMapping(cameraDeviceId = devId2, channelNumber = 1)
        val customField = CustomExtraField(targetType = "DEVICE", targetId = devId1, fieldKey = "VendorWarranty", fieldValue = "Active-2028")

        val powerFeed = PowerFeed(
            deviceId = devId1,
            feedName = "Feed A - UPS 1",
            feedType = PowerFeedType.PRIMARY_A,
            loadVa = 750.0,
            loadWatts = 600.0,
            observedRuntimeMinutes = 45,
            observedSource = "UPS Display",
            observedEpochMs = System.currentTimeMillis()
        )
        val poe = PoeMapping(portId = portId1, role = PoeRole.PSE_SOURCE, standard = PoeStandard.IEEE_802_3BT, allocatedPowerWatts = 60.0)
        val badge = DocumentBadge(targetType = "DEVICE", targetId = devId1, label = "CORE_SWITCH", category = BadgeCategory.FREE_LABEL)

        val cred = Credential(id = UUID.randomUUID().toString(), username = "admin", secret = "EncryptedPass123!", type = CredentialType.PASSWORD)

        val attachment = Attachment(
            id = UUID.randomUUID().toString(),
            name = "Piantina CED",
            originalFileName = "floor_plan.jpg",
            fileType = AttachmentType.IMAGE,
            mimeType = "image/jpeg",
            relativePath = "attachments/floor_plan.jpg",
            targetType = AttachmentTargetType.AREA,
            targetId = areaId
        )

        val now = System.currentTimeMillis()
        val project = Project(
            id = projId,
            name = "Android-Windows Interop Full Project",
            createdEpochMs = now,
            updatedEpochMs = now,
            sites = listOf(site),
            racks = listOf(rack),
            deviceModels = listOf(model),
            attachments = listOf(attachment),
            cables = listOf(cable),
            vlans = listOf(vlan),
            subnets = listOf(subnet),
            portVlanMemberships = listOf(portVlan),
            logicalInterfaces = listOf(logicalInt),
            lagGroups = listOf(lagGroup),
            deviceConfigurations = listOf(devConfig),
            wanVpnConnections = listOf(wanVpn),
            videoSurveillanceMappings = listOf(video),
            customExtraFields = listOf(customField),
            powerFeeds = listOf(powerFeed),
            poeMappings = listOf(poe),
            documentBadges = listOf(badge),
            credentials = listOf(cred)
        )

        val attachments = mapOf(
            "attachments/rack_elevation.png" to "PNG_DUMMY_RACK_IMAGE".toByteArray(Charsets.UTF_8),
            "attachments/floor_plan.jpg" to "JPG_DUMMY_FLOORPLAN_IMAGE".toByteArray(Charsets.UTF_8),
            "attachments/device_notes.txt" to "TXT_NOTES_CONTENT".toByteArray(Charsets.UTF_8)
        )

        return Pair(project, attachments)
    }

    @Test
    fun testAndroidToWindowsUnencryptedRoundtrip() {
        val (originalProject, attachments) = createFullDomainProject()

        val exportedZipBytes = PackageSerializer.exportPackage(originalProject, attachments)
        assertTrue("Exported ZIP bytes should not be empty", exportedZipBytes.isNotEmpty())

        val exportedFile = File(tempFolder.root, "android_export.ofam")
        exportedFile.writeBytes(exportedZipBytes)

        val desktopImportRes = storageManager.importPackageFromFile(exportedFile)
        assertTrue("Desktop import should be valid: ${desktopImportRes.validationResult.issues}", desktopImportRes.validationResult.isValid)

        val importedPkg = desktopImportRes.pkg
        assertNotNull("Imported package should not be null", importedPkg)
        val importedProj = importedPkg!!.project

        val valResult = ModelValidator.validateProject(importedProj)
        assertTrue("Imported project should pass structural validation", valResult.isValid)

        assertEquals(originalProject.id, importedProj.id)
        assertEquals(originalProject.name, importedProj.name)
        assertEquals(1, importedProj.sites.size)
        assertEquals(2, importedProj.sites[0].devices.size)
        assertEquals(1, importedProj.racks.size)
        assertEquals(1, importedProj.vlans.size)
        assertEquals(10, importedProj.vlans[0].vlanId)
        assertEquals(1, importedProj.subnets.size)
        assertEquals(1, importedProj.powerFeeds.size)
        assertEquals("Feed A - UPS 1", importedProj.powerFeeds[0].feedName)
        assertEquals(1, importedProj.poeMappings.size)
        assertEquals(1, importedProj.documentBadges.size)
        assertEquals(1, importedProj.attachments.size)
        assertEquals(1, importedProj.credentials.size)

        assertEquals(3, importedPkg.attachments.size)
        assertTrue(importedPkg.attachments.keys.any { it.endsWith("rack_elevation.png") })
        assertTrue(importedPkg.attachments.keys.any { it.endsWith("floor_plan.jpg") })

        Thread.sleep(10) // Ensure timestamp progression for semantic version check
        val newDev = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "SRV-WIN-01",
            category = DeviceCategory.SERVER_STORAGE
        )
        val modifiedProj = ProjectEdits.addDevice(importedProj, importedProj.sites[0].id, newDev)
        val targetLocalFile = storageManager.saveProjectLocally(modifiedProj)
        assertTrue("Local atomic save target file should exist", targetLocalFile.exists())

        val windowsExportFile = File(tempFolder.root, "windows_export.ofam")
        storageManager.exportPackageToFile(modifiedProj, windowsExportFile, attachments = importedPkg.attachments)
        assertTrue(windowsExportFile.exists())

        val androidReImportBytes = windowsExportFile.readBytes()
        val androidImportRes = PackageSerializer.importPackage(androidReImportBytes)
        assertTrue("Android re-import should be valid", androidImportRes.validationResult.isValid)

        val androidRestoredPkg = androidImportRes.pkg!!
        val androidRestoredProj = androidRestoredPkg.project

        assertEquals(3, androidRestoredProj.sites[0].devices.size)
        assertNotNull(androidRestoredProj.sites[0].devices.find { it.technicalName == "SRV-WIN-01" })
        assertEquals(originalProject.attachments.map { it.relativePath }.toSet(), androidRestoredPkg.attachments.keys)
        val activePath = originalProject.attachments.single().relativePath
        assertEquals(PackageSerializer.calculateSha256(attachments.getValue(activePath)),
            PackageSerializer.calculateSha256(androidRestoredPkg.attachments.getValue(activePath)))
        assertFalse(androidRestoredPkg.attachments.containsKey("attachments/rack_elevation.png"))
        assertFalse(androidRestoredPkg.attachments.containsKey("attachments/device_notes.txt"))

        val comparison = ProjectComparisonEvaluator.evaluate(originalProject, importedPkg.manifest, androidRestoredPkg)
        assertEquals("Re-imported project should be NEWER_REVISION relative to original", ComparisonStatus.NEWER_REVISION, comparison.status)

        storageManager.releaseProjectLock(originalProject.id)
    }

    @Test
    fun testAndroidToWindowsEncryptedRoundtripWithPBKDF2AESGCM() {
        val (originalProject, attachments) = createFullDomainProject()
        val encryptedProj = originalProject.copy(isPasswordProtected = true)
        val password = "StrongInterOpPass123!"

        val encryptedZipBytes = PackageSerializer.exportPackage(encryptedProj, attachments, password = password)
        assertTrue(encryptedZipBytes.isNotEmpty())

        val encryptedFile = File(tempFolder.root, "encrypted_android.ofam")
        encryptedFile.writeBytes(encryptedZipBytes)

        val noPassRes = storageManager.importPackageFromFile(encryptedFile)
        assertNull(noPassRes.pkg)
        assertTrue(noPassRes.validationResult.issues.any { it.code == "PASSWORD_REQUIRED" })

        val correctPassRes = storageManager.importPackageFromFile(encryptedFile, password = password)
        assertTrue(correctPassRes.validationResult.isValid)
        val importedPkg = correctPassRes.pkg!!
        assertEquals(encryptedProj.id, importedPkg.project.id)
        assertEquals(1, importedPkg.project.credentials.size)
        assertEquals("admin", importedPkg.project.credentials[0].username)

        val reExportFile = File(tempFolder.root, "encrypted_windows.ofam")
        storageManager.exportPackageToFile(importedPkg.project, reExportFile, attachments = importedPkg.attachments, password = password)
        assertTrue(reExportFile.exists())

        val reImportRes = PackageSerializer.importPackage(reExportFile.readBytes(), password = password)
        assertTrue(reImportRes.validationResult.isValid)
        assertNotNull(reImportRes.pkg)
        assertEquals(encryptedProj.id, reImportRes.pkg!!.project.id)
    }

    @Test
    fun testSemanticVersionComparisonInBidirectionalFlow() {
        val (p1, _) = createFullDomainProject()
        val zip1 = PackageSerializer.exportPackage(p1)
        val import1 = PackageSerializer.importPackage(zip1)
        assertTrue("Project should be valid: ${import1.validationResult.issues}", import1.validationResult.isValid)
        val pkg1 = import1.pkg!!

        val compIdentical = ProjectComparisonEvaluator.evaluate(p1, pkg1.manifest, pkg1)
        assertEquals(ComparisonStatus.IDENTICAL, compIdentical.status)

        val p1Newer = p1.copy(updatedEpochMs = p1.updatedEpochMs + 5000L)
        val zip2 = PackageSerializer.exportPackage(p1Newer)
        val pkg2 = PackageSerializer.importPackage(zip2).pkg!!

        val compNewer = ProjectComparisonEvaluator.evaluate(p1, pkg1.manifest, pkg2)
        assertEquals(ComparisonStatus.NEWER_REVISION, compNewer.status)

        val compOlder = ProjectComparisonEvaluator.evaluate(p1Newer, pkg2.manifest, pkg1)
        assertEquals(ComparisonStatus.OLDER_REVISION, compOlder.status)
        assertNotNull(compOlder.warningMessage)

        val p2 = p1.copy(id = UUID.randomUUID().toString(), name = "Other Project")
        val zipOther = PackageSerializer.exportPackage(p2)
        val pkgOther = PackageSerializer.importPackage(zipOther).pkg!!

        val compDiff = ProjectComparisonEvaluator.evaluate(p1, pkg1.manifest, pkgOther)
        assertEquals(ComparisonStatus.DIFFERENT_PROJECT, compDiff.status)
    }
}
