package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Project
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
    fun serialNumberRoundTripsInVersion1() {
        val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(project))
        assertEquals("1", result.pkg!!.manifest.formatVersion)
        assertEquals("FOC123", result.pkg!!.project.businessUnits.single().devices.single().serialNumber)
    }

    @Test
    fun preGreenfieldPackagesAreRejected() {
        val entries = unzip(PackageSerializer.exportPackage(project)).toMutableMap()
        val manifest = PackageSerializer.jsonConfig.decodeFromString(PackageManifest.serializer(), entries.getValue("manifest.json").toString(Charsets.UTF_8))
        entries["manifest.json"] = PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest.copy(formatVersion = "1.11")).toByteArray(Charsets.UTF_8)
        val result = PackageSerializer.importPackage(zip(entries))
        assertNull(result.pkg)
        assertTrue(result.validationResult.issues.any { it.code == "UNSUPPORTED_FORMAT_VERSION" })
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
