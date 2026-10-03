package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.*

class ObjectMapExchangeTest {
    private val area = Area(name = "Piano 1")
    private val type = ObjectType(name = "Gateway industriale")
    private val device = Device(technicalName = "GW-01", objectTypeId = type.id, areaId = area.id)
    private val cable = Cable(codeOrLabel = "C1", deviceAId = device.id)
    private val attachment = Attachment(name = "Foto cavo", originalFileName = "cavo.jpg", relativePath = "attachments/cavo.jpg", targetType = AttachmentTargetType.CABLE, targetId = cable.id)
    private val project = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1,
        businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area), devices = listOf(device))),
        objectTypes = listOf(type), cables = listOf(cable), cableRoutes = listOf(CableRoute(cableId = cable.id, areaId = area.id)), attachments = listOf(attachment))

    @Test fun mapAndTypesRoundTripWithEncryptedPhotos() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val exported = PackageSerializer.exportPackage(project, password = "test-password", attachments = mapOf(attachment.relativePath to bytes))
        val result = PackageSerializer.importPackage(exported, "test-password")
        assertTrue(result.validationResult.isValid)
        assertEquals(project.objectTypes, result.pkg!!.project.objectTypes)
        assertEquals(project.cableRoutes, result.pkg!!.project.cableRoutes)
        assertEquals(device.objectTypeId, result.pkg!!.project.businessUnits.single().devices.single().objectTypeId)
        assertEquals(device.id, result.pkg!!.project.cables.single().deviceAId)
        assertArrayEquals(bytes, result.pkg!!.attachments[attachment.relativePath])
    }
    @Test fun newerVersionIsRejectedBeforeDeserialization() {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(PackageSerializer.exportPackage(project))).use { z ->
            generateSequence { z.nextEntry }.forEach { entries[it.name] = z.readBytes() }
        }
        entries["manifest.json"] = entries.getValue("manifest.json").toString(Charsets.UTF_8).replace("1.9", "2.0").toByteArray(Charsets.UTF_8)
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { z -> entries.forEach { (name, data) -> z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry() } }
        val result = PackageSerializer.importPackage(output.toByteArray())
        assertNull(result.pkg)
        assertTrue(result.validationResult.issues.any { it.code == "UNSUPPORTED_FORMAT_VERSION" })
    }
    @Test fun typeAndGeometryChangesMergeByStableId() {
        val local = project.copy(objectTypes = listOf(type.copy(name = "Gateway aggiornato")))
        val incoming = project.copy(cableRoutes = project.cableRoutes.map { it.copy(points = listOf(MapPoint(.1f, .1f), MapPoint(.9f, .9f))) })
        val result = ProjectMerger.merge(project, local, incoming)
        assertTrue(result.conflicts.isEmpty())
        val merged = result.resolve(emptyMap())
        assertEquals(local.objectTypes, merged.objectTypes)
        assertEquals(incoming.cableRoutes, merged.cableRoutes)
    }
}
