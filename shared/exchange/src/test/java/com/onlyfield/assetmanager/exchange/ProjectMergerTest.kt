package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectMergerTest {
    private val sw1 = Device(id = "d1", technicalName = "SW-01", ipAddress = "10.0.0.1", ports = listOf(Port(id = "p1", deviceId = "d1", name = "Gi1")))
    private val sw2 = Device(id = "d2", technicalName = "SW-02")
    private val base = Project(
        id = "proj", name = "Sito", createdEpochMs = 1, updatedEpochMs = 1,
        sites = listOf(
            Site(
                id = "bu", name = "Sede",
                areas = listOf(Area(id = "a1", name = "CED")),
                devices = listOf(sw1, sw2)
            )
        ),
        racks = listOf(Rack(id = "r1", name = "Rack 1"))
    )

    private fun Project.device(id: String) = sites.flatMap { it.devices }.find { it.id == id }
    private fun Project.editDevice(d: Device) = copy(sites = sites.map { site -> site.copy(devices = site.devices.map { if (it.id == d.id) d else it }) })

    @Test
    fun changesInDifferentPartsMergeWithoutConflicts() {
        val local = base.editDevice(sw1.copy(ipAddress = "10.0.0.9")).copy(updatedEpochMs = 5)
        val incoming = base.editDevice(sw2.copy(technicalName = "SW-02-NEW")).copy(vlans = listOf(Vlan(id = "v10", vlanId = 10, name = "Dati")), updatedEpochMs = 6)

        val result = ProjectMerger.merge(base, local, incoming)
        assertTrue(result.conflicts.isEmpty())
        val merged = result.resolve(emptyMap(), nowMs = 99)
        assertEquals("10.0.0.9", merged.device("d1")!!.ipAddress)
        assertEquals("SW-02-NEW", merged.device("d2")!!.technicalName)
        assertEquals(listOf("v10"), merged.vlans.map { it.id })
        // Nested structure survives the round trip.
        assertEquals("CED", merged.sites.single().areas.single().name)
        assertEquals(listOf("p1"), merged.device("d1")!!.ports.map { it.id })
        assertEquals(99, merged.updatedEpochMs)
    }

    @Test
    fun sameElementChangedOnBothSidesIsAConflictResolvedByChoice() {
        val local = base.editDevice(sw1.copy(ipAddress = "10.0.0.2"))
        val incoming = base.editDevice(sw1.copy(ipAddress = "10.0.0.3"))
        val result = ProjectMerger.merge(base, local, incoming)
        val conflict = result.conflicts.single()
        assertEquals(MergeKey("devices", "d1"), conflict.key)
        assertEquals("Apparato", conflict.kindLabel)
        assertEquals("SW-01", conflict.name)
        assertEquals(listOf("ipAddress: 10.0.0.2 → 10.0.0.3"), conflict.differences())

        assertEquals("10.0.0.3", result.resolve(mapOf(conflict.key to MergeSide.INCOMING)).device("d1")!!.ipAddress)
        assertEquals("10.0.0.2", result.resolve(mapOf(conflict.key to MergeSide.LOCAL)).device("d1")!!.ipAddress)
    }

    @Test
    fun deletionsFollowTheBase() {
        // Local deleted SW-02 (incoming untouched): stays deleted. Incoming deleted the rack that local renamed: conflict.
        val local = base.copy(sites = base.sites.map { it.copy(devices = listOf(sw1)) }, racks = listOf(Rack(id = "r1", name = "Rack principale")))
        val incoming = base.copy(racks = emptyList())
        val result = ProjectMerger.merge(base, local, incoming)
        assertNull(result.resolve(emptyMap()).device("d2"))
        val conflict = result.conflicts.single()
        assertEquals(MergeKey("racks", "r1"), conflict.key)
        assertEquals("Modificato in questa copia, eliminato nel pacchetto", conflict.description)
        assertTrue(result.resolve(mapOf(conflict.key to MergeSide.INCOMING)).racks.isEmpty())
    }

    @Test
    fun withoutBaseEveryDifferenceIsAConflict() {
        val local = base.editDevice(sw1.copy(ipAddress = "10.0.0.2"))
        val incoming = base.copy(racks = emptyList())
        val result = ProjectMerger.merge(null, local, incoming)
        assertEquals(setOf(MergeKey("devices", "d1"), MergeKey("racks", "r1")), result.conflicts.map { it.key }.toSet())
        assertEquals("Presente solo in questa copia", result.conflicts.first { it.key.kind == "racks" }.description)
    }

    @Test
    fun deviceMovedToADeletedSiteIsKept() {
        val withBu2 = base.copy(sites = base.sites + Site(id = "bu2", name = "Magazzino"))
        // Local moves SW-02 into bu2; incoming deletes bu2.
        val local = withBu2.copy(sites = listOf(
            withBu2.sites[0].copy(devices = listOf(sw1)),
            withBu2.sites[1].copy(devices = listOf(sw2))
        ))
        val incoming = withBu2.copy(sites = listOf(withBu2.sites[0]))
        val merged = ProjectMerger.merge(withBu2, local, incoming).resolve(mapOf(MergeKey("sites", "bu2") to MergeSide.INCOMING))
        assertEquals("SW-02", merged.device("d2")!!.technicalName)
        assertEquals(listOf("bu"), merged.sites.map { it.id })
    }
}
