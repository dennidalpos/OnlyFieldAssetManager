package com.onlyfield.assetmanager

import com.onlyfield.assetmanager.cartography.CartographicMapManager
import com.onlyfield.assetmanager.cartography.OfflineMapException
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress

class CartographyLimitsTest {
    @Test fun oversizedResponsesAreRejectedWithAndWithoutContentLength() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val size = if (exchange.requestURI.path == "/exact") 2 * 1024 * 1024 else 2 * 1024 * 1024 + 1
            exchange.sendResponseHeaders(200, if (exchange.requestURI.path == "/chunked") 0 else size.toLong())
            try { exchange.responseBody.use { it.write(ByteArray(size)) } } catch (_: java.io.IOException) { /* Client may reject the header before reading. */ }
            finally { exchange.close() }
        }
        server.start()
        try {
            val url = "http://127.0.0.1:${server.address.port}"
            for (path in listOf("chunked", "length")) {
                try { CartographicMapManager.fetchTileBytes("$url/$path"); fail("Oversized tile accepted: $path") }
                catch (_: OfflineMapException) { }
            }
            assertEquals(2 * 1024 * 1024, CartographicMapManager.fetchTileBytes("$url/exact").size)
        } finally { server.stop(0) }
    }
}
