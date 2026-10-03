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

    const val NO_NETWORK_MESSAGE =
        "Mappa non scaricata: nessuna connessione o servizio cartografico non raggiungibile. L'app continua a funzionare offline; riprova quando sei connesso."

    fun lonToTileX(lon: Double, zoom: Int): Int {
        val n = 1 shl zoom
        return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    }

    fun latToTileY(lat: Double, zoom: Int): Int {
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
        return try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                setRequestProperty("User-Agent", "OnlyFieldAssetManager/1.0 (Android Offline App)")
            }
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.use { it.readBytes() }
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                    throw OfflineMapException(i18n.text("text.12573340ecd6", code))
                else -> throw OfflineMapException(i18n.text("text.e9a6f09680dc", code))
            }
        } catch (e: OfflineMapException) {
            throw e
        } catch (e: Exception) {
            throw OfflineMapException(i18n.text("map.network.android"), e)
        }
    }

    fun acquireMapSnapshot(request: MapSnapshotRequest, i18n: Messages = Messages()): MapSnapshotResult {
        if (!request.source.isOnline) {
            throw IllegalArgumentException(i18n.text("text.cc5f9553abed", request.source.localizedName(i18n)))
        }

        val zoom = request.zoomLevel.coerceIn(1, 17)
        val centerTileX = lonToTileX(request.centerLongitude, zoom)
        val centerTileY = latToTileY(request.centerLatitude, zoom)

        // 3x3 grid around center tile
        val tileXRange = (centerTileX - 1)..(centerTileX + 1)
        val tileYRange = (centerTileY - 1)..(centerTileY + 1)

        val tileBitmaps = mutableMapOf<Pair<Int, Int>, Bitmap>()
        val tileSize = 256

        for (x in tileXRange) {
            for (y in tileYRange) {
                val tileUrl = buildTileUrl(request.source, x, y, zoom)
                val bytes = fetchTileBytes(tileUrl, i18n = i18n)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw OfflineMapException(i18n.text("text.46d882bc0794", tileUrl))
                tileBitmaps[Pair(x, y)] = bitmap
            }
        }

        val gridWidth = 3 * tileSize
        val gridHeight = 3 * tileSize
        val resultBitmap = Bitmap.createBitmap(gridWidth, gridHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        tileYRange.forEachIndexed { row, y ->
            tileXRange.forEachIndexed { col, x ->
                val b = tileBitmaps[Pair(x, y)]
                if (b != null) {
                    canvas.drawBitmap(b, (col * tileSize).toFloat(), (row * tileSize).toFloat(), null)
                }
            }
        }

        // Draw attribution banner at bottom
        val attribution = request.customAttribution ?: request.source.attributionText
        drawAttributionBanner(canvas, attribution, gridWidth, gridHeight)

        val outputStream = ByteArrayOutputStream()
        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val bytes = outputStream.toByteArray()

        return MapSnapshotResult(
            imageBytes = bytes,
            mimeType = "image/png",
            widthPx = gridWidth,
            heightPx = gridHeight,
            attributionText = attribution,
            sourceName = request.source.localizedName(i18n)
        )
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
