package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.model.ObjectHierarchy
import com.onlyfield.assetmanager.core.model.ObjectRef
import com.onlyfield.assetmanager.core.model.PlacementTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ContractVersionTest {
    private val project = Project(
        name = "Contratto", createdEpochMs = 1, updatedEpochMs = 1,
        businessUnits = listOf(BusinessUnit(name = "BU", devices = listOf(Device(technicalName = "SW-01", serialNumber = "FOC123"))))
    )

    @Test
    fun serialNumberRoundTripsInVersion110() {
        val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(project))
        assertEquals("1.10", result.pkg!!.manifest.formatVersion)
        assertEquals("FOC123", result.pkg!!.project.businessUnits.single().devices.single().serialNumber)
    }

    @Test
    fun version17PackageWithoutSerialStillImports() {
        // Rebuild the package as 1.7 wrote it: no serialNumber key, manifest 1.7, matching checksum.
        val entries = unzip(PackageSerializer.exportPackage(project)).toMutableMap()
        val projectJson = entries.getValue("project.json").toString(Charsets.UTF_8)
            .replace(Regex(""",?\s*"serialNumber"\s*:\s*"FOC123""""), "")
        assertTrue(!projectJson.contains("serialNumber"))
        entries["project.json"] = projectJson.toByteArray(Charsets.UTF_8)
        val manifest = PackageSerializer.jsonConfig.decodeFromString(PackageManifest.serializer(), entries.getValue("manifest.json").toString(Charsets.UTF_8))
        val legacy = manifest.copy(
            formatVersion = "1.7",
            checksums = manifest.checksums + ("project.json" to PackageSerializer.calculateSha256(entries.getValue("project.json")))
        )
        entries["manifest.json"] = PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), legacy).toByteArray(Charsets.UTF_8)

        val result = PackageSerializer.importPackage(zip(entries))
        assertTrue(result.validationResult.isValid)
        assertNull(result.pkg!!.project.businessUnits.single().devices.single().serialNumber)
    }

    @Test
    fun version18PackageStillImportsSerialAndDefaultsNewMapData() {
        val entries = unzip(PackageSerializer.exportPackage(project)).toMutableMap()
        val manifest = PackageSerializer.jsonConfig.decodeFromString(PackageManifest.serializer(), entries.getValue("manifest.json").toString(Charsets.UTF_8))
        entries["manifest.json"] = PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest.copy(formatVersion = "1.8")).toByteArray(Charsets.UTF_8)
        val imported = PackageSerializer.importPackage(zip(entries))
        assertTrue(imported.validationResult.isValid)
        assertEquals(project, imported.pkg!!.project)
        assertTrue(imported.pkg!!.project.objectTypes.isEmpty())
        assertTrue(imported.pkg!!.project.cableRoutes.isEmpty())
    }

    @Test
    fun version19ConvertsRackMembershipWithoutChangingOriginalPackage() {
        val rack = Rack(name = "R1")
        val device = project.businessUnits.single().devices.single().copy(rackId = rack.id, positionU = 5)
        val legacy = project.copy(racks = listOf(rack), businessUnits = listOf(project.businessUnits.single().copy(devices = listOf(device))))
        val entries = unzip(PackageSerializer.exportPackage(legacy)).toMutableMap()
        val manifest = PackageSerializer.jsonConfig.decodeFromString(PackageManifest.serializer(), entries.getValue("manifest.json").toString(Charsets.UTF_8))
        entries["manifest.json"] = PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest.copy(formatVersion = "1.9")).toByteArray(Charsets.UTF_8)
        val bytes = zip(entries)
        val original = bytes.copyOf()
        val result = PackageSerializer.importPackage(bytes)
        assertTrue(result.validationResult.isValid)
        assertEquals(ObjectRef(PlacementTargetType.RACK, rack.id), ObjectHierarchy.parent(result.pkg!!.project, ObjectRef(PlacementTargetType.DEVICE, device.id)))
        assertEquals(5, result.pkg!!.project.businessUnits.single().devices.single().positionU)
        org.junit.Assert.assertArrayEquals(original, bytes)
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> = buildMap {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zin ->
            generateSequence { zin.nextEntry }.forEach { put(it.name, zin.readBytes()) }
        }
    }

    private fun zip(entries: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z -> entries.forEach { (name, data) -> z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry() } }
    }.toByteArray()
}
