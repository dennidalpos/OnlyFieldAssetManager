package com.onlyfield.assetmanager.pc

import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.pow
import kotlin.math.tan

enum class DesktopMapSource(val displayName: String, val attribution: String, val urlTemplate: String) {
    OPEN_TOPO_MAP(
        displayName = "OpenTopoMap (Topografica)",
        attribution = "© OpenTopoMap contributors (CC-BY-SA)",
        urlTemplate = "https://a.tile.opentopomap.org/{z}/{x}/{y}.png"
    ),
    CARTO_DB(
        displayName = "CARTO Voyager (Mista)",
        attribution = "© CARTO, © OpenStreetMap contributors",
        urlTemplate = "https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png"
    )
}

data class TileCoord(val x: Int, val y: Int, val zoom: Int)

object DesktopCartographyManager {

    /**
     * Converts latitude/longitude and zoom level to OSM tile coordinate (x, y, zoom).
     */
    fun lonLatToTileCoord(lon: Double, lat: Double, zoom: Int): TileCoord {
        val n = 2.0.pow(zoom)
        val x = ((lon + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat)
        val y = ((1.0 - asinh(tan(latRad)) / PI) / 2.0 * n).toInt()
        return TileCoord(x, y, zoom)
    }

    /**
     * Returns tile URL for given source and coordinates.
     */
    fun getTileUrl(source: DesktopMapSource, tile: TileCoord): String {
        return source.urlTemplate
            .replace("{z}", tile.zoom.toString())
            .replace("{x}", tile.x.toString())
            .replace("{y}", tile.y.toString())
    }
}
