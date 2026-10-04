package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Credential
import com.onlyfield.assetmanager.core.model.CredentialType
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageSerializerTest {

    @Test
    fun testExportAndImportRoundTrip() {
        val siteId = UUID.randomUUID().toString()
        val dev = Device(
            id = UUID.randomUUID().toString(),
            technicalName = "sw-core-01",
            ipAddress = "10.0.1.1",
            siteId = siteId
        )
        val site = Site(id = siteId, name = "Data Center 1")
        val bu = BusinessUnit(id = UUID.randomUUID().toString(), name = "HQ BU", sites = listOf(site), devices = listOf(dev))

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Export Test Project",
            createdEpochMs = 1700000000000L,
            updatedEpochMs = 1700000000000L,
            businessUnits = listOf(bu)
        )

        val attachmentBytes = "Sample Rack Diagram Content".toByteArray(Charsets.UTF_8)
        val attachments = mapOf("rack_diagram.png" to attachmentBytes)

        val zipBytes = PackageSerializer.exportPackage(project, attachments)
        assertTrue(zipBytes.isNotEmpty())

        val importResult = PackageSerializer.importPackage(zipBytes)
        assertTrue(importResult.validationResult.isValid)

        val importedPkg = importResult.pkg
        assertNotNull(importedPkg)
        assertEquals(project.id, importedPkg!!.project.id)
        assertEquals(project.name, importedPkg.project.name)
        assertEquals(1, importedPkg.project.businessUnits.size)

        val importedAttachment = importedPkg.attachments["attachments/rack_diagram.png"]
        assertNotNull(importedAttachment)
        assertArrayEquals(attachmentBytes, importedAttachment)
    }

    @Test
    fun testEncryptedPackageExportAndImportWithPassword() {
        val cred = Credential(
            username = "admin_test",
            secret = "SuperSecret123!",
            type = CredentialType.PASSWORD
        )

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Encrypted Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            credentials = listOf(cred),
            isPasswordProtected = true
        )

        val password = "ProjectPassword456"

        val encryptedZipBytes = PackageSerializer.exportPackage(project, password = password)
        assertTrue(encryptedZipBytes.isNotEmpty())

        val importNoPass = PackageSerializer.importPackage(encryptedZipBytes)
        assertFalse(importNoPass.validationResult.isValid)
        assertNull(importNoPass.pkg)
        assertTrue(importNoPass.validationResult.issues.any { it.code == "PASSWORD_REQUIRED" })

        val importWrongPass = PackageSerializer.importPackage(encryptedZipBytes, password = "WrongPassword")
        assertFalse(importWrongPass.validationResult.isValid)
        assertNull(importWrongPass.pkg)
        assertTrue(importWrongPass.validationResult.issues.any { it.code == "INVALID_PACKAGE_PASSWORD" })

        val importCorrect = PackageSerializer.importPackage(encryptedZipBytes, password = password)
        assertTrue(importCorrect.validationResult.isValid)
        assertNotNull(importCorrect.pkg)
        val importedProject = importCorrect.pkg!!.project
        assertEquals(project.id, importedProject.id)
        assertEquals("Encrypted Project", importedProject.name)
        assertEquals(1, importedProject.credentials.size)
        assertEquals("admin_test", importedProject.credentials[0].username)
        assertEquals("SuperSecret123!", importedProject.credentials[0].secret)
    }

    @Test
    fun testCorruptedChecksumRejection() {
        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Checksum Test Project",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L
        )

        val projectJsonBytes = PackageSerializer.jsonConfig.encodeToString(Project.serializer(), project).toByteArray(Charsets.UTF_8)
        
        val manifest = PackageManifest(
            formatVersion = PackageManifest.CURRENT_FORMAT_VERSION,
            exportId = UUID.randomUUID().toString(),
            exportedEpochMs = 1000L,
            projectId = project.id,
            projectName = project.name,
            checksums = mapOf(PackageManifest.PROJECT_FILE_NAME to "0000000000000000000000000000000000000000000000000000000000000000")
        )
        val manifestJsonBytes = PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest).toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry(PackageManifest.MANIFEST_FILE_NAME))
            zos.write(manifestJsonBytes)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(PackageManifest.PROJECT_FILE_NAME))
            zos.write(projectJsonBytes)
            zos.closeEntry()
        }

        val corruptedZipBytes = baos.toByteArray()

        val importResult = PackageSerializer.importPackage(corruptedZipBytes)
        assertFalse(importResult.validationResult.isValid)
        assertNull(importResult.pkg)
        assertTrue(importResult.validationResult.issues.any { (code, _, severity) -> (code == "PROJECT_CHECKSUM_MISMATCH") && (severity == ValidationSeverity.STRUCTURAL_ERROR) })
    }

    @Test
    fun testInvalidZipHandledGracefully() {
        val badBytes = "Not a zip file".toByteArray(Charsets.UTF_8)
        val importResult = PackageSerializer.importPackage(badBytes)

        assertFalse(importResult.validationResult.isValid)
        assertNull(importResult.pkg)
        assertTrue(importResult.validationResult.issues.any { (code) -> (code == "INVALID_ZIP_ARCHIVE") || (code == "MISSING_MANIFEST") })
    }

    @Test
    fun testProjectComparisonEvaluator() {
        val projId = UUID.randomUUID().toString()
        val p1 = Project(
            id = projId,
            name = "Project Alpha",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L
        )

        val pkg1 = ProjectPackage(
            manifest = PackageManifest(exportId = "e1", exportedEpochMs = 1000L, projectId = projId, projectName = "Project Alpha"),
            project = p1
        )

        val compNew = ProjectComparisonEvaluator.evaluate(null, null, pkg1)
        assertEquals(ComparisonStatus.NEWER_REVISION, compNew.status)

        val compIdentical = ProjectComparisonEvaluator.evaluate(p1, pkg1.manifest, pkg1)
        assertEquals(ComparisonStatus.IDENTICAL, compIdentical.status)

        val p1Updated = p1.copy(updatedEpochMs = 2000L)
        val pkg1Newer = ProjectPackage(
            manifest = PackageManifest(exportId = "e2", exportedEpochMs = 2000L, projectId = projId, projectName = "Project Alpha"),
            project = p1Updated
        )
        val compNewer = ProjectComparisonEvaluator.evaluate(p1, pkg1.manifest, pkg1Newer)
        assertEquals(ComparisonStatus.NEWER_REVISION, compNewer.status)

        val compOlder = ProjectComparisonEvaluator.evaluate(p1Updated, pkg1Newer.manifest, pkg1)
        assertEquals(ComparisonStatus.OLDER_REVISION, compOlder.status)
        assertNotNull(compOlder.warningMessage)

        val p2 = Project(id = UUID.randomUUID().toString(), name = "Project Beta", createdEpochMs = 1000L, updatedEpochMs = 1000L)
        val pkg2 = ProjectPackage(
            manifest = PackageManifest(exportId = "e3", exportedEpochMs = 1000L, projectId = p2.id, projectName = p2.name),
            project = p2
        )
        val compDiff = ProjectComparisonEvaluator.evaluate(p1, pkg1.manifest, pkg2)
        assertEquals(ComparisonStatus.DIFFERENT_PROJECT, compDiff.status)
    }

    @Test
    fun testA08EntitiesRoundTrip() {
        val devId = UUID.randomUUID().toString()
        val portId = UUID.randomUUID().toString()
        val dev = Device(id = devId, technicalName = "sw-a08-01", siteId = UUID.randomUUID().toString(), ports = listOf(com.onlyfield.assetmanager.core.model.Port(id = portId, deviceId = devId, name = "port1")))
        val bu = BusinessUnit(id = UUID.randomUUID().toString(), name = "BU A08", devices = listOf(dev))

        val vlan = com.onlyfield.assetmanager.core.model.Vlan(vlanId = 100, name = "SERVERS")
        val subnet = com.onlyfield.assetmanager.core.model.Subnet(cidrBlock = "10.100.0.0/24", gatewayIp = "10.100.0.1")
        val portMembership = com.onlyfield.assetmanager.core.model.PortVlanMembership(portId = portId, mode = com.onlyfield.assetmanager.core.model.PortVlanMode.ACCESS, untaggedVlanId = 100)
        val logicalInt = com.onlyfield.assetmanager.core.model.LogicalInterface(deviceId = devId, name = "vlan100", ipAddress = "10.100.0.1")
        val lagGroup = com.onlyfield.assetmanager.core.model.LagGroup(deviceId = devId, name = "lag1", memberPortIds = listOf(portId))
        val devConfig = com.onlyfield.assetmanager.core.model.DeviceConfiguration(deviceId = devId, title = "v1-running-config")
        val wanVpn = com.onlyfield.assetmanager.core.model.WanVpnConnection(name = "WAN_Primary", bandwidth = "1 Gbps")
        val video = com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping(cameraDeviceId = devId, channelNumber = 1)
        val customField = com.onlyfield.assetmanager.core.model.CustomExtraField(targetType = "DEVICE", targetId = devId, fieldKey = "VendorSupport", fieldValue = "Gold")

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "A08 Package Test",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu),
            vlans = listOf(vlan),
            subnets = listOf(subnet),
            portVlanMemberships = listOf(portMembership),
            logicalInterfaces = listOf(logicalInt),
            lagGroups = listOf(lagGroup),
            deviceConfigurations = listOf(devConfig),
            wanVpnConnections = listOf(wanVpn),
            videoSurveillanceMappings = listOf(video),
            customExtraFields = listOf(customField)
        )

        val zipBytes = PackageSerializer.exportPackage(project)
        val importResult = PackageSerializer.importPackage(zipBytes)

        assertTrue(importResult.validationResult.isValid)
        val imported = importResult.pkg!!.project
        assertEquals(1, imported.vlans.size)
        assertEquals(100, imported.vlans[0].vlanId)
        assertEquals(1, imported.subnets.size)
        assertEquals("10.100.0.0/24", imported.subnets[0].cidrBlock)
        assertEquals(1, imported.portVlanMemberships.size)
        assertEquals(1, imported.logicalInterfaces.size)
        assertEquals(1, imported.lagGroups.size)
        assertEquals(1, imported.deviceConfigurations.size)
        assertEquals(1, imported.wanVpnConnections.size)
        assertEquals(1, imported.videoSurveillanceMappings.size)
        assertEquals(1, imported.customExtraFields.size)
        assertEquals("Gold", imported.customExtraFields[0].fieldValue)
    }

    @Test
    fun testA09EntitiesRoundTrip() {
        val devId = UUID.randomUUID().toString()
        val portId = UUID.randomUUID().toString()
        val dev = Device(id = devId, technicalName = "sw-a09-01", ports = listOf(com.onlyfield.assetmanager.core.model.Port(id = portId, deviceId = devId, name = "port1")))
        val bu = BusinessUnit(id = UUID.randomUUID().toString(), name = "BU A09", devices = listOf(dev))

        val feed = com.onlyfield.assetmanager.core.model.PowerFeed(
            deviceId = devId,
            feedName = "Feed A",
            feedType = com.onlyfield.assetmanager.core.model.PowerFeedType.PRIMARY_A,
            loadVa = 500.0,
            loadWatts = 450.0,
            observedRuntimeMinutes = 20,
            observedSource = "UPS Screen",
            observedEpochMs = System.currentTimeMillis()
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
            label = "CRITICAL_FEED",
            category = com.onlyfield.assetmanager.core.model.BadgeCategory.FREE_LABEL,
            isDerived = false
        )

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "A09 Package Test",
            createdEpochMs = 1000L,
            updatedEpochMs = 1000L,
            businessUnits = listOf(bu),
            powerFeeds = listOf(feed),
            poeMappings = listOf(poe),
            documentBadges = listOf(badge)
        )

        val zipBytes = PackageSerializer.exportPackage(project)
        val importResult = PackageSerializer.importPackage(zipBytes)

        assertTrue(importResult.validationResult.isValid)
        val imported = importResult.pkg!!.project
        assertEquals(PackageManifest.CURRENT_FORMAT_VERSION, importResult.pkg!!.manifest.formatVersion)
        assertEquals(1, imported.powerFeeds.size)
        assertEquals("Feed A", imported.powerFeeds[0].feedName)
        assertEquals(500.0, imported.powerFeeds[0].loadVa)
        assertEquals(1, imported.poeMappings.size)
        assertEquals(30.0, imported.poeMappings[0].allocatedPowerWatts)
        assertEquals(1, imported.documentBadges.size)
        assertEquals("CRITICAL_FEED", imported.documentBadges[0].label)
    }
}
