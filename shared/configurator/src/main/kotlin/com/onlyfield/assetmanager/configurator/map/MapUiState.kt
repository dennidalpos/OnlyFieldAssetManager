package com.onlyfield.assetmanager.configurator.map

import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.model.*

/** Session-only map context; owned by the platform's project host. */
class MapUiState {
    private var projectId: String? = null
    private val floors = mutableMapOf<String, FloorUiState>()

    fun floor(project: Project, areaId: String): FloorUiState {
        if (projectId != project.id) {
            clear()
            projectId = project.id
        }
        floors.keys.retainAll(project.sites.flatMap { it.areas }.map { it.id }.toSet())
        return floors.getOrPut(areaId) { FloorUiState() }
    }

    fun clear() { floors.clear(); projectId = null }
}

class FloorUiState {
    var path by mutableStateOf<List<ObjectRef>>(emptyList())
    var selection by mutableStateOf<MapSelection?>(null)
    var listOpen by mutableStateOf(false)
    var expanded by mutableStateOf(false)
    var wideDetails by mutableStateOf(false)
    private val cameras = mutableMapOf<ObjectRef?, MapCamera>()
    fun camera(container: ObjectRef?) = cameras.getOrPut(container) { MapCamera() }
}

/** Normalized centre survives canvas resize and asynchronous image decoding. */
class MapCamera {
    var zoom by mutableFloatStateOf(1f)
    var center by mutableStateOf(MapPoint(.5f, .5f))

    fun viewport(width: Float, height: Float, contentWidth: Float, contentHeight: Float): MapViewport {
        val base = MapViewport(width, height, contentWidth, contentHeight, zoom)
        return base.copy(panX = (.5f - center.x) * base.pageWidth,
            panY = (.5f - center.y) * base.pageHeight).clamped()
    }

    fun capture(view: MapViewport) {
        val bounded = view.clamped()
        zoom = bounded.zoom
        center = MapPoint(.5f - bounded.panX / bounded.pageWidth, .5f - bounded.panY / bounded.pageHeight)
    }

    fun reset() { zoom = 1f; center = MapPoint(.5f, .5f) }
}
