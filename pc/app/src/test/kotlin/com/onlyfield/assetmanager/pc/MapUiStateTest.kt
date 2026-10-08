package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.configurator.map.MapCamera
import com.onlyfield.assetmanager.configurator.map.MapUiState
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class MapUiStateTest {
    @Test fun centerSurvivesRotationAndDelayedImage() {
        val camera = MapCamera()
        camera.capture(MapViewport(900f, 600f, 1200f, 900f, 3f, -120f, 40f))
        val center = camera.center
        for ((w, h) in listOf(360f to 720f, 1024f to 768f, 720f to 360f)) {
            val view = camera.viewport(w, h, 2400f, 1800f)
            val actual = view.relative(w / 2, h / 2)
            assertEquals(center.x, actual.x, .0001f)
            assertEquals(center.y, actual.y, .0001f)
            assertEquals(3f, view.zoom)
        }
    }

    @Test fun floorsAreIndependentAndProjectChangeOrDeletionDropsOldContext() {
        val a = Area(name = "A")
        val b = Area(name = "B")
        val project = Project(name = "First", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Site", areas = listOf(a, b))))
        val state = MapUiState()
        val first = state.floor(project, a.id)
        first.camera(null).zoom = 2f
        assertEquals(1f, state.floor(project, b.id).camera(null).zoom)
        assertSame(first, state.floor(project, a.id))
        state.floor(project.copy(sites = listOf(project.sites.single().copy(areas = listOf(b)))), b.id)
        assertNotSame(first, state.floor(project, a.id))
        assertEquals(1f, state.floor(project.copy(id = "other-project"), a.id).camera(null).zoom)
    }
}
