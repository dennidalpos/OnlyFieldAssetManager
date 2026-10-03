package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectIndexTest {

    private val port = Port(id = "p1", deviceId = "d1", name = "Gi1/0/1")
    private val project = Project(
        id = "proj",
        name = "Sede Milano",
        createdEpochMs = 0,
        updatedEpochMs = 0,
        businessUnits = listOf(
            BusinessUnit(
                id = "bu1",
                name = "IT",
                sites = listOf(Site(id = "s1", name = "HQ", areas = listOf(Area(id = "a2", name = "CED")))),
                areas = listOf(Area(id = "a1", name = "Ufficio")),
                devices = listOf(Device(id = "d1", technicalName = "SW-CORE01", ports = listOf(port)))
            )
        ),
        racks = listOf(Rack(id = "r1", name = "Rack A"))
    )
    private val index = ProjectIndex(project)

    @Test
    fun resolvesNamesInsteadOfIds() {
        assertEquals("SW-CORE01 › Gi1/0/1", index.portLabel("p1"))
        assertEquals("—", index.portLabel("missing"))
        assertEquals("CED", index.areaName("a2"))
        assertEquals(2, index.areas.size)
        assertEquals("Rack: Rack A", index.targetLabel("RACK", "r1"))
        assertEquals("Apparato: non trovato", index.targetLabel("DEVICE", "x"))
        assertEquals("SW-CORE01", index.entityName("d1"))
        assertNull(index.entityName("unknown"))
    }
}
