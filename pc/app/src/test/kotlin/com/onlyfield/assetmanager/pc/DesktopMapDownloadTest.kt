package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.AttachmentTargetType
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import javax.imageio.ImageIO

class DesktopMapDownloadTest {
    @get:Rule val folder = TemporaryFolder()

    private fun tileBytes(): ByteArray {
        val tile = BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB)
        tile.createGraphics().apply {
            color = Color.BLUE
            fillRect(0, 0, 256, 256)
            dispose()
        }
        return ByteArrayOutputStream().also { ImageIO.write(tile, "png", it) }.toByteArray()
    }

    @Test
    fun snapshotContainsNineTilesAndAttributionFooter() {
        val urls = mutableListOf<String>()
        val bytes = tileBytes()
        val snapshot = DesktopCartographyManager.acquireMapSnapshot(12.4964, 41.9028, 15) {
            urls += it
            bytes
        }
        assertEquals(9, urls.distinct().size)
        assertTrue(urls.all { it.startsWith("https://a.tile.opentopomap.org/15/") })
        val image = ImageIO.read(ByteArrayInputStream(snapshot.imageBytes))
        assertEquals(768, image.width)
        assertEquals(800, image.height)
        assertEquals(Color.BLUE.rgb, image.getRGB(384, 384))
        assertEquals(Color.WHITE.rgb, image.getRGB(0, 799))
        assertTrue((768 until 800).any { y -> (0 until 768).any { x -> image.getRGB(x, y) != Color.WHITE.rgb } })
        assertEquals(DesktopMapSource.OPEN_TOPO_MAP.attribution, snapshot.attributionText)
    }

    @Test
    fun invalidTileAbortsWithoutProducingASnapshot() {
        val error = assertThrows(MapDownloadException::class.java) {
            DesktopCartographyManager.acquireMapSnapshot(12.0, 42.0, 15) { "invalid image".toByteArray() }
        }
        assertTrue(error.message!!.contains("non valida"))
    }

    @Test
    fun httpErrorsAndNoConnectionHaveExplicitMessages() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val bytes = tileBytes()
        server.createContext("/tile") { exchange ->
            assertTrue(exchange.requestHeaders.getFirst("User-Agent").contains("OnlyFieldAssetManager"))
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/unavailable") { exchange ->
            exchange.sendResponseHeaders(503, -1)
            exchange.close()
        }
        server.start()
        try {
            val base = "http://127.0.0.1:${server.address.port}"
            assertArrayEquals(bytes, DesktopCartographyManager.fetchTileBytes("$base/tile"))
            val error = assertThrows(MapDownloadException::class.java) {
                DesktopCartographyManager.fetchTileBytes("$base/unavailable")
            }
            assertTrue(error.message!!.contains("503"))
        } finally {
            server.stop(0)
        }
        val port = ServerSocket(0).use { it.localPort }
        val offline = assertThrows(MapDownloadException::class.java) {
            DesktopCartographyManager.fetchTileBytes("http://127.0.0.1:$port/tile", 500)
        }
        assertEquals(DesktopCartographyManager.NO_NETWORK_MESSAGE, offline.message)
    }

    @Test
    fun boundaryCoordinatesStayInsideTileRangeAndUnsupportedZoomIsRejected() {
        assertEquals(1, DesktopCartographyManager.lonLatToTileCoord(180.0, -85.0, 1).x)
        assertThrows(IllegalArgumentException::class.java) {
            DesktopCartographyManager.lonLatToTileCoord(0.0, 0.0, 18)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DesktopCartographyManager.lonLatToTileCoord(Double.NaN, 0.0, 15)
        }
    }

    @Test
    fun mapSurvivesEncryptedTransferAndReopeningWithoutNetwork() {
        val storage = DesktopStorageManager(folder.newFolder("source"))
        val state = DesktopAppState(storage)
        val other = DesktopStorageManager(folder.newFolder("destination"))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", businessUnits = listOf(com.onlyfield.assetmanager.core.model.BusinessUnit(name = "BU", areas = listOf(com.onlyfield.assetmanager.core.model.Area(name = "CED")))))))
            val snapshot = DesktopCartographyManager.acquireMapSnapshot(12.0, 42.0, 15) { tileBytes() }
            assertTrue(state.addMapSnapshot(snapshot, "Mappa CED"))
            val project = state.project!!
            val attachment = project.attachments.single()
            assertEquals(AttachmentTargetType.PROJECT, attachment.targetType)
            assertEquals(project.id, attachment.targetId)
            assertEquals(snapshot.attributionText, attachment.attributionText)
            val projectFile = state.storedProjects.single().file
            state.closeProject()
            state.openStored(projectFile)
            assertArrayEquals(snapshot.imageBytes, state.attachmentFile(state.project!!.attachments.single())!!.readBytes())
            val exported = File(folder.root, "map.ofam")
            storage.exportPackageToFile(state.project!!, exported, "test-password")
            val imported = other.importPackageFromFile(exported, "test-password").pkg!!
            assertEquals(snapshot.attributionText, imported.project.attachments.single().attributionText)
            assertArrayEquals(snapshot.imageBytes, AttachmentFiles.bytesIn(imported, attachment))
            assertEquals(1, other.extractAttachments(imported))
            assertArrayEquals(snapshot.imageBytes, other.attachmentFile(project.id, attachment).readBytes())
        } finally {
            state.shutdown()
            other.releaseAllLocks()
        }
    }
}
