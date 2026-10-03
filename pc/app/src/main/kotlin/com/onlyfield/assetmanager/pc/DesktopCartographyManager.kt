package com.onlyfield.assetmanager.pc

import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.pow
import kotlin.math.tan

enum class DesktopMapSource(val displayName: String, val attribution: String, val urlTemplate: String) {
    OPEN_TOPO_MAP(
        displayName = "OpenTopoMap (Topografica)",
        attribution = "© OpenStreetMap contributors, SRTM | © OpenTopoMap (CC-BY-SA)",
        urlTemplate = "https://a.tile.opentopomap.org/{z}/{x}/{y}.png"
    )
}

data class TileCoord(val x: Int, val y: Int, val zoom: Int)
class DesktopMapSnapshot(val imageBytes: ByteArray, val attributionText: String)
class MapDownloadException(message: String, cause: Throwable? = null) : IOException(message, cause)

object DesktopCartographyManager {

    const val NO_NETWORK_MESSAGE = "Mappa non scaricata: nessuna connessione o servizio cartografico non raggiungibile. Riprova quando sei connesso; gli allegati locali restano disponibili."

    /** OSM tile coordinates within the supported raster range. */
    fun lonLatToTileCoord(lon: Double, lat: Double, zoom: Int): TileCoord {
        require(lon.isFinite() && lon in -180.0..180.0) { "Longitudine non valida." }
        require(lat.isFinite() && lat in -85.0..85.0) { "Latitudine non valida." }
        require(zoom in 1..17) { "Zoom supportato: da 1 a 17." }
        val n = 2.0.pow(zoom)
        val x = ((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n.toInt() - 1)
        val latRad = Math.toRadians(lat)
        val y = ((1.0 - asinh(tan(latRad)) / PI) / 2.0 * n).toInt().coerceIn(0, n.toInt() - 1)
        return TileCoord(x, y, zoom)
    }

    /** Tile URL for the selected source. */
    fun getTileUrl(source: DesktopMapSource, tile: TileCoord): String {
        return source.urlTemplate
            .replace("{z}", tile.zoom.toString())
            .replace("{x}", tile.x.toString())
            .replace("{y}", tile.y.toString())
    }

    fun fetchTileBytes(url: String, timeoutMs: Int = 5000): ByteArray {
        val connection = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", "OnlyFieldAssetManager/1.1 (+https://github.com/dennidalpos/OnlyFieldAssetManager)")
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw MapDownloadException("Mappa non scaricata: il servizio ha risposto con codice $code. Riprova più tardi o importa una mappa locale.")
            }
            return connection.inputStream.use { input ->
                val bytes = input.readNBytes(2 * 1024 * 1024 + 1)
                if (bytes.size > 2 * 1024 * 1024) throw MapDownloadException("Tessera cartografica troppo grande.")
                bytes
            }
        } catch (e: MapDownloadException) {
            throw e
        } catch (e: IOException) {
            throw MapDownloadException(NO_NETWORK_MESSAGE, e)
        } finally {
            connection.disconnect()
        }
    }

    /** Nine tiles, downloaded only on request, with attribution in the PNG. */
    fun acquireMapSnapshot(
        lon: Double,
        lat: Double,
        zoom: Int,
        fetchTile: (String) -> ByteArray = { fetchTileBytes(it) },
    ): DesktopMapSnapshot {
        val center = lonLatToTileCoord(lon, lat, zoom)
        val source = DesktopMapSource.OPEN_TOPO_MAP
        val tileCount = 1 shl zoom
        val image = BufferedImage(768, 800, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            for (row in 0..2) for (column in 0..2) {
                val tile = TileCoord(
                    Math.floorMod(center.x + column - 1, tileCount),
                    (center.y + row - 1).coerceIn(0, tileCount - 1), zoom
                )
                val bytes = fetchTile(getTileUrl(source, tile))
                val bitmap = ImageIO.read(ByteArrayInputStream(bytes))
                    ?: throw MapDownloadException("Il servizio ha restituito una tessera non valida.")
                if (bitmap.width != 256 || bitmap.height != 256) {
                    throw MapDownloadException("Dimensioni della tessera cartografica non valide.")
                }
                graphics.drawImage(bitmap, column * 256, row * 256, null)
            }
            graphics.color = Color.WHITE
            graphics.fillRect(0, 768, 768, 32)
            graphics.color = Color.BLACK
            graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
            graphics.drawString(source.attribution, 12, 789)
        } finally {
            graphics.dispose()
        }
        val output = ByteArrayOutputStream()
        check(ImageIO.write(image, "png", output)) { "Impossibile creare la mappa PNG." }
        return DesktopMapSnapshot(output.toByteArray(), source.attribution)
    }
}
