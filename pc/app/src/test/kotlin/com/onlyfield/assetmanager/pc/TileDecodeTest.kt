package com.onlyfield.assetmanager.pc

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.*
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import javax.imageio.ImageIO

class TileDecodeTest {
    private fun png(size: Int): ByteArray = ByteArrayOutputStream().also {
        check(ImageIO.write(BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY), "png", it))
    }.toByteArray()
    @Test fun normalTileAndMalformedOrOversizedLocalResponsesAreHandled() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var response = png(256)
        var requests = 0
        server.createContext("/tile") { exchange ->
            requests++
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            val url = "http://127.0.0.1:${server.address.port}/tile"
            val snapshot = DesktopCartographyManager.acquireMapSnapshot(12.0, 41.0, 15) { DesktopCartographyManager.fetchTileBytes(url) }
            assertEquals(9, requests)
            assertEquals(768, ImageIO.read(snapshot.imageBytes.inputStream()).width)
            assertTrue(snapshot.attributionText.contains("OpenTopoMap"))
            response = png(4096)
            assertTrue(response.size < 2 * 1024 * 1024)
            assertThrows(MapDownloadException::class.java) { DesktopCartographyManager.acquireMapSnapshot(12.0, 41.0, 15) { DesktopCartographyManager.fetchTileBytes(url) } }
            response = byteArrayOf(1, 2, 3)
            assertThrows(MapDownloadException::class.java) { DesktopCartographyManager.acquireMapSnapshot(12.0, 41.0, 15) { DesktopCartographyManager.fetchTileBytes(url) } }
            response = ByteArray(2 * 1024 * 1024 + 1)
            assertThrows(MapDownloadException::class.java) { DesktopCartographyManager.acquireMapSnapshot(12.0, 41.0, 15) { DesktopCartographyManager.fetchTileBytes(url) } }
        } finally { server.stop(0) }
    }
    @Test fun oversizedHeaderIsRejectedBeforeReadingImagePixels() {
        val bytes = png(4096)
        // Keep only PNG signature and IHDR: read() would fail on the missing image data.
        val error = assertThrows(MapDownloadException::class.java) {
            DesktopCartographyManager.acquireMapSnapshot(12.0, 41.0, 15) { bytes.copyOf(33) }
        }
        assertEquals(com.onlyfield.assetmanager.core.i18n.Messages().text("text.31677e1f659a"), error.message)
    }
}
