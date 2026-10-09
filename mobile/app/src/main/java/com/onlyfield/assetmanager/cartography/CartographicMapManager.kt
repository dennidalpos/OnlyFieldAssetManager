package com.onlyfield.assetmanager.cartography

import com.onlyfield.assetmanager.core.i18n.Messages

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

enum class CartographicSource(
    val id: String,
    val displayName: String,
    val tileUrlTemplate: String,
    val attributionText: String,
    val isOnline: Boolean
) {
    OPEN_TOPO_MAP(
        id = "OPEN_TOPO_MAP",
        displayName = "OpenTopoMap (Topografica)",
        tileUrlTemplate = "https://a.tile.opentopomap.org/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors, SRTM | © OpenTopoMap (CC-BY-SA)",
        isOnline = true
    ),
    LOCAL_IMPORT(
        id = "LOCAL_IMPORT",
        displayName = "Importazione Mappa Locale",
        tileUrlTemplate = "",
        attributionText = "Mappa locale importata dall'utente",
        isOnline = false
    );

    fun localizedName(i18n: Messages): String = i18n.text(if (this == OPEN_TOPO_MAP) "map.source.topographic" else "map.source.local")

    companion object {
        fun fromId(id: String): CartographicSource =
            entries.firstOrNull { it.id == id } ?: OPEN_TOPO_MAP
    }
}

class OfflineMapException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class MapSnapshotRequest(
    val source: CartographicSource = CartographicSource.OPEN_TOPO_MAP,
    val centerLatitude: Double = 41.9028, // Roma
    val centerLongitude: Double = 12.4964,
    val zoomLevel: Int = 15,
    val customAttribution: String? = null
)

data class MapSnapshotResult(
    val imageBytes: ByteArray,
    val mimeType: String = "image/png",
    val widthPx: Int,
    val heightPx: Int,
    val attributionText: String,
    val sourceName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MapSnapshotResult
        return imageBytes.contentEquals(other.imageBytes) &&
                mimeType == other.mimeType &&
                widthPx == other.widthPx &&
                heightPx == other.heightPx &&
                attributionText == other.attributionText &&
                sourceName == other.sourceName
    }

    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + widthPx.hashCode()
        result = 31 * result + heightPx.hashCode()
        result = 31 * result + attributionText.hashCode()
        result = 31 * result + sourceName.hashCode()
        return result
    }
}

object CartographicMapManager {

    private const val MAX_TILE_BYTES = 2 * 1024 * 1024

    fun lonToTileX(lon: Double, zoom: Int): Int {
        require(lon.isFinite() && lon in -180.0..180.0 && zoom in 1..17)
        val n = 1 shl zoom
        return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    }

    fun latToTileY(lat: Double, zoom: Int): Int {
        require(lat.isFinite() && lat in -85.0..85.0 && zoom in 1..17)
        val rad = Math.toRadians(lat)
        val n = 1 shl zoom
        val y = floor((1.0 - ln(tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * n).toInt()
        return y.coerceIn(0, n - 1)
    }

    fun buildTileUrl(source: CartographicSource, x: Int, y: Int, zoom: Int): String {
        return source.tileUrlTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
    }

    fun fetchTileBytes(urlStr: String, timeoutMs: Int = 3000, i18n: Messages = Messages()): ByteArray {
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
            setRequestProperty("User-Agent", "OnlyFieldAssetManager/1.0 (Android Offline App)")
        }
        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    if (connection.contentLengthLong > MAX_TILE_BYTES) throw OfflineMapException(i18n.text("text.63bc51f297f4"))
                    return connection.inputStream.use { input ->
                        val bytes = input.readNBytes(MAX_TILE_BYTES + 1)
                        if (bytes.size > MAX_TILE_BYTES) throw OfflineMapException(i18n.text("text.63bc51f297f4"))
                        bytes
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                    throw OfflineMapException(i18n.text("text.12573340ecd6", code))
                else -> throw OfflineMapException(i18n.text("text.e9a6f09680dc", code))
            }
        } catch (e: java.io.IOException) {
            throw OfflineMapException(i18n.text("map.network.android"), e)
        } finally {
            connection.disconnect()
        }
    }

    fun acquireMapSnapshot(request: MapSnapshotRequest, i18n: Messages = Messages()): MapSnapshotResult =
        acquireMapSnapshot(request, i18n) { url -> fetchTileBytes(url, i18n = i18n) }

    internal fun acquireMapSnapshot(request: MapSnapshotRequest, i18n: Messages, fetchTile: (String) -> ByteArray): MapSnapshotResult {
        require(request.source.isOnline) { i18n.text("text.cc5f9553abed", request.source.localizedName(i18n)) }
        require(request.centerLongitude.isFinite() && request.centerLongitude in -180.0..180.0) { i18n.text("text.cfbcb1936a29") }
        require(request.centerLatitude.isFinite() && request.centerLatitude in -85.0..85.0) { i18n.text("text.674222655d0c") }
        val zoom = request.zoomLevel.coerceIn(1, 17)
        val centerX = lonToTileX(request.centerLongitude, zoom)
        val centerY = latToTileY(request.centerLatitude, zoom)
        val tileCount = 1 shl zoom
        val tileSize = 256
        val gridSize = 3 * tileSize
        val result = Bitmap.createBitmap(gridSize, gridSize, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(result)
            for (row in 0..2) for (column in 0..2) {
                val x = Math.floorMod(centerX + column - 1, tileCount)
                val y = (centerY + row - 1).coerceIn(0, tileCount - 1)
                val url = buildTileUrl(request.source, x, y, zoom)
                val bytes = fetchTile(url)
                if (bytes.size > MAX_TILE_BYTES) throw OfflineMapException(i18n.text("text.63bc51f297f4"))
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth != tileSize || bounds.outHeight != tileSize) throw OfflineMapException(i18n.text("text.31677e1f659a"))
                val tile = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw OfflineMapException(i18n.text("text.46d882bc0794", url))
                try { canvas.drawBitmap(tile, (column * tileSize).toFloat(), (row * tileSize).toFloat(), null) }
                finally { tile.recycle() }
            }
            val attribution = request.customAttribution ?: request.source.attributionText
            drawAttributionBanner(canvas, attribution, gridSize, gridSize)
            val bytes = ByteArrayOutputStream().use { output ->
                check(result.compress(Bitmap.CompressFormat.PNG, 100, output)) { i18n.text("text.67b5aa03bb6a") }
                output.toByteArray()
            }
            return MapSnapshotResult(bytes, widthPx = gridSize, heightPx = gridSize,
                attributionText = attribution, sourceName = request.source.localizedName(i18n))
        } finally { result.recycle() }
    }

    private fun drawAttributionBanner(canvas: Canvas, attribution: String, width: Int, height: Int) {
        val bannerHeight = 36f
        val bannerPaint = Paint().apply {
            color = Color.argb(180, 0, 0, 0) // Translucent black
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - bannerHeight, width.toFloat(), height.toFloat(), bannerPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            isAntiAlias = true
        }
        canvas.drawText(attribution, 12f, height - 10f, textPaint)
    }
}
