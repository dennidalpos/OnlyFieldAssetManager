package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.BulkCabling
import com.onlyfield.assetmanager.core.forms.PhotoCoverage
import com.onlyfield.assetmanager.core.forms.PortLogic
import com.onlyfield.assetmanager.core.forms.PortSummaries
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoCoverageTest {
    private fun ports(deviceId: String, n: Int) = (1..n).map { Port(id = "$deviceId-$it", deviceId = deviceId, name = "P$it") }
    private val sw = Device(id = "sw", technicalName = "SW-01", ports = ports("sw", 4))
    private val pc = Device(id = "pc", technicalName = "PC-01", ports = ports("pc", 3))
    private val cabled = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "Sede", devices = listOf(sw, pc))))
        .let { BulkCabling.connect(it, BulkCabling.pairs(it, "sw-1", "pc-1", 3), CableMedium.ETHERNET_COPPER) }

    private fun photo(type: AttachmentTargetType, id: String) =
        Attachment(name = "foto", originalFileName = "f.jpg", fileType = AttachmentType.IMAGE, mimeType = "image/jpeg", relativePath = "a", targetType = type, targetId = id)

    @Test fun aPortOrCablePhotoDocumentsTheConnection() {
        assertEquals(listOf("P1", "P2", "P3"), PhotoCoverage.missing(cabled, sw).map { it.name })
        val cableP2 = cabled.cables.first { it.portAId == "sw-2" }
        val shot = cabled.copy(attachments = listOf(photo(AttachmentTargetType.PORT, "sw-1"), photo(AttachmentTargetType.CABLE, cableP2.id)))
        // P4 is free: never counted. The cable photo covers both of its ends.
        assertEquals(listOf("P3"), PhotoCoverage.missing(shot, sw).map { it.name })
        assertEquals(listOf("P1", "P3"), PhotoCoverage.missing(shot, pc).map { it.name })
        assertEquals(listOf(false, false, true, false), PortLogic.panel(shot, sw).map { it.photoMissing })
        assertEquals(1, PortSummaries.of(shot, "pc-2")!!.cablePhotos)
    }
}
