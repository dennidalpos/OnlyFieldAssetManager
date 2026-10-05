package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.*
import org.junit.Assert.*
import org.junit.Test

/** Editing through a form must never drop fields the dialog does not show. */
class EntityFormsTest {

    @Test
    fun deviceEditPreservesHiddenFields() {
        val obs = Observation("Android", 1000L, ObservationStatus.VERIFIED, "ok")
        val original = Device(
            technicalName = "SW-01", areaId = "a1", deviceModelId = "m1",
            rackSide = RackSide.REAR, ports = listOf(Port(deviceId = "x", name = "Gi1")), observation = obs
        )
        val edited = DeviceForm.from(original, "bu1").copy(technicalName = "SW-01-NEW").toDevice(original, "Test")

        assertEquals("SW-01-NEW", edited.technicalName)
        assertEquals(original.id, edited.id)
        assertEquals("a1", edited.areaId)
        assertEquals("m1", edited.deviceModelId)
        assertEquals(RackSide.REAR, edited.rackSide)
        assertEquals(original.ports, edited.ports)
        assertSame("Unchanged observation keeps its original source and time", obs, edited.observation)
    }

    @Test
    fun deviceFormValidatesFields() {
        val rack = 42
        val form = DeviceForm(technicalName = "", siteId = null, ipAddress = "300.1.1.1", macAddress = "zz", rackId = "r", positionU = "42", heightU = "2")
        val errors = form.errors(rack)
        assertTrue(errors.keys.containsAll(listOf("technicalName", "siteId", "ipAddress", "macAddress", "positionU")))
        assertTrue(DeviceForm(technicalName = "A", siteId = "bu", ipAddress = "10.0.0.1", rackId = "r", positionU = "41", heightU = "2").errors(rack).isEmpty())
    }

    @Test
    fun rackEditPreservesAreaAndDepth() {
        val original = Rack(name = "R1", areaId = "a1", depthMm = 1000, heightU = 42)
        val edited = RackForm.from(original).copy(name = "R1 bis").toRack(original)
        assertEquals("a1", edited.areaId)
        assertEquals(1000, edited.depthMm)
        assertEquals(original.id, edited.id)
    }

    @Test
    fun cableEditPreservesHiddenFields() {
        val obs = Observation("Android", 1L)
        val original = Cable(codeOrLabel = "C1", lengthUnit = "ft", observation = obs, objectTypeId = "patch-cord")
        val edited = CableForm.from(original).copy(color = "Blu").toCable(original)
        assertEquals("ft", edited.lengthUnit)
        assertEquals(obs, edited.observation)
        assertEquals("patch-cord", edited.objectTypeId)
        assertEquals("Blu", edited.color)
    }

    @Test
    fun cableCannotConnectPortToItself() {
        assertTrue(CableForm(portAId = "p", portBId = "p").errors().containsKey("portBId"))
        assertTrue(CableForm(lengthValue = "2,5").errors().isEmpty())
        assertEquals(2.5, CableForm(lengthValue = "2,5").toCable(null).lengthValue!!, 0.0)
    }

    @Test
    fun networkEditsPreserveHiddenFields() {
        val subnet = Subnet(cidrBlock = "10.0.0.0/24", scopeType = VlanScopeType.SITE, scopeTargetId = "s1")
        val editedSubnet = SubnetForm.from(subnet).copy(name = "LAN").toSubnet(subnet)
        assertEquals(VlanScopeType.SITE, editedSubnet.scopeType)
        assertEquals("s1", editedSubnet.scopeTargetId)

        val wan = WanVpnConnection(name = "FTTH", underlyingAccessId = "acc-1")
        assertEquals("acc-1", WanForm.from(wan).copy(bandwidth = "1 Gbps").toConnection(wan).underlyingAccessId)

        val feed = PowerFeed(deviceId = "d1", feedName = "A", observedSource = "Android", observedEpochMs = 5L)
        val editedFeed = PowerFeedForm.from(feed).copy(loadWatts = "120").toFeed(feed)
        assertEquals("Android", editedFeed.observedSource)
        assertEquals(5L, editedFeed.observedEpochMs)
        assertEquals(120.0, editedFeed.loadWatts!!, 0.0)
    }

    @Test
    fun vlanNumberIsRangeCheckedAndUnique() {
        assertTrue(VlanForm(vlanId = "5000", name = "X").errors(emptySet()).containsKey("vlanId"))
        assertTrue(VlanForm(vlanId = "10", name = "X").errors(setOf(10)).containsKey("vlanId"))
        assertTrue(VlanForm(vlanId = "10", name = "X").errors(setOf(20)).isEmpty())
    }

    @Test
    fun validatorsAcceptCommonFormats() {
        assertNull(FieldValidators.ipv4("192.168.1.1"))
        assertNotNull(FieldValidators.ipv4("192.168.1"))
        assertNull(FieldValidators.cidr("10.0.0.0/8"))
        assertNotNull(FieldValidators.cidr("10.0.0.0/33"))
        assertNull(FieldValidators.mac("aa:bb:cc:dd:ee:ff"))
        assertNull(FieldValidators.mac("aabb.ccdd.eeff"))
        assertNotNull(FieldValidators.mac("aa:bb"))
        assertNull(FieldValidators.int("", required = false))
        assertNotNull(FieldValidators.int("", required = true))
    }

    @Test
    fun rackLayoutOffersOnlyFreePositions() {
        val rack = Rack(id = "r", name = "R", heightU = 10)
        val devices = listOf(
            Device(technicalName = "A", rackId = "r", positionU = 1, heightU = 2, rackSide = RackSide.BOTH),
            Device(technicalName = "B", rackId = "r", positionU = 5, heightU = 1, rackSide = RackSide.REAR),
        )
        val freeFront = RackLayout.freeStartPositions(rack, devices, heightU = 2, side = RackSide.FRONT)
        assertEquals(listOf(3, 4, 5, 6, 7, 8, 9), freeFront)
        val freeRear = RackLayout.freeStartPositions(rack, devices, heightU = 2, side = RackSide.REAR)
        assertFalse(4 in freeRear)
        assertFalse(5 in freeRear)
        assertEquals(3, RackLayout.usedUnits(rack, devices))
    }
}
