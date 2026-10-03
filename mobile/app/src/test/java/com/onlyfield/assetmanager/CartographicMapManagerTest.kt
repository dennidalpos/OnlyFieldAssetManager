package com.onlyfield.assetmanager

import com.onlyfield.assetmanager.cartography.CartographicMapManager
import com.onlyfield.assetmanager.cartography.CartographicSource
import com.onlyfield.assetmanager.cartography.OfflineMapException
import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.AttachmentTargetType
import com.onlyfield.assetmanager.core.model.AttachmentType
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CartographicMapManagerTest {

    @Test
    fun testTileCoordinateMath() {
        // Roma (41.9028, 12.4964) at Zoom 15
        val x = CartographicMapManager.lonToTileX(12.4964, 15)
        val y = CartographicMapManager.latToTileY(41.9028, 15)

        assertTrue(x > 0)
        assertTrue(y > 0)

        val urlOpenTopo = CartographicMapManager.buildTileUrl(CartographicSource.OPEN_TOPO_MAP, x, y, 15)
        assertTrue(urlOpenTopo.contains("opentopomap.org/15/$x/$y.png"))

        assertEquals(listOf(CartographicSource.OPEN_TOPO_MAP), CartographicSource.entries.filter { it.isOnline })
    }

    @Test
    fun testOfflineExceptionWhenServerUnreachable() {
        try {
            // Attempt to fetch from non-existent local server/port to simulate offline/flight mode
            CartographicMapManager.fetchTileBytes("http://127.0.0.1:65534/nonexistent_tile.png", timeoutMs = 500)
            fail("Dovrebbe sollevare OfflineMapException")
        } catch (e: OfflineMapException) {
            assertEquals(CartographicMapManager.NO_NETWORK_MESSAGE, e.message)
        }
    }

    @Test
    fun testAttachmentAttributionPreservationInPackageSerializer() {
        val attachment = Attachment(
            id = UUID.randomUUID().toString(),
            name = "Sfondo OpenTopoMap Roma",
            originalFileName = "map_snapshot.png",
            fileType = AttachmentType.IMAGE,
            mimeType = "image/png",
            relativePath = "attachments/map_snapshot.png",
            classification = AttachmentClassification.SHAREABLE,
            targetType = AttachmentTargetType.AREA,
            targetId = "area-1",
            attributionText = "© OpenTopoMap (CC-BY-SA), © OpenStreetMap contributors"
        )

        val project = Project(
            id = UUID.randomUUID().toString(),
            name = "Progetto Test Cartografia",
            createdEpochMs = System.currentTimeMillis(),
            updatedEpochMs = System.currentTimeMillis(),
            attachments = listOf(attachment)
        )

        val sampleImageBytes = "fake_png_data".toByteArray(Charsets.UTF_8)
        val attachmentsMap = mapOf("attachments/map_snapshot.png" to sampleImageBytes)

        val zipBytes = PackageSerializer.exportPackage(project, attachmentsMap)
        val importResult = PackageSerializer.importPackage(zipBytes)

        assertNotNull(importResult.pkg)
        val importedProject = importResult.pkg!!.project
        assertEquals(1, importedProject.attachments.size)

        val importedAtt = importedProject.attachments[0]
        assertEquals("© OpenTopoMap (CC-BY-SA), © OpenStreetMap contributors", importedAtt.attributionText)
    }
}
