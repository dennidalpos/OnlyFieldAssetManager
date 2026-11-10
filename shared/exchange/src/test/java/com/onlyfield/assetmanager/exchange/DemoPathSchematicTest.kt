package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.PathSchematics
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DemoPathSchematicTest {
    private val project = DemoSeed.build()
    private val devices = project.sites.flatMap { it.devices }
    private fun named(name: String) = devices.single { it.technicalName == name }
    private fun port(device: String, name: String, side: PortSide? = null) = named(device).ports.single { it.name == name && (side == null || it.hardware.side == side) }.id

    @Test fun theatreRunCrossesOutletJunctionBoxAndPanel() {
        val path = PathSchematics.of(project, named("AP-TEA-PT-01").ports.single().id)!!
        assertEquals(ConnectionState.COMPLETE, path.state)
        assertEquals("AP-TEA-PT-01", path.stations.first().device?.technicalName)
        assertEquals("SW-TEA-PT-A", path.stations.last().device?.technicalName)
        assertEquals(listOf("PR-TEA-PT-01", "GB-TEA-PT-01"), path.stations.drop(1).take(2).map { it.device?.technicalName })
        // Every pass-through shows where the path enters and leaves.
        path.stations.drop(1).dropLast(1).forEach { assertEquals(it.device?.technicalName, 2, it.ports.size) }
        assertEquals(path.stations.size - 1, path.segments.size)
        assertFalse(path.openEnd(0) || path.openEnd(path.stations.lastIndex))
        assertEquals(0, path.focus)
    }

    @Test fun fromTheJunctionBoxTheDrawingStillCoversBothSides() {
        val path = PathSchematics.of(project, port("GB-TEA-PT-01", "P1", PortSide.FRONT))!!
        assertEquals(setOf("AP-TEA-PT-01", "SW-TEA-PT-A"), setOf(path.stations.first().device?.technicalName, path.stations.last().device?.technicalName))
        assertEquals("GB-TEA-PT-01", path.stations[path.focus].device?.technicalName)
    }

    @Test fun radioBridgeJoinsMunicipioAndSchoolSwitches() {
        val path = PathSchematics.of(project, port("SW-COM-P1-A", "P40"))!!
        assertEquals(listOf("SW-COM-P1-A", "RAD-COM-01", "RAD-MED-01", "SW-MED-PT-A"), path.stations.map { it.device?.technicalName })
        assertEquals(CableMedium.RADIO, path.segments[1].cable.medium)
        assertEquals("PR-COM-MED", path.segments[1].label)
        assertEquals(listOf("LAN1", "RF1"), path.stations[1].ports.map { it.name })
    }
}
