package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.model.RackSide

/** Occupancy checks for placing devices in a rack. */
object RackLayout {

    private fun sidesOverlap(a: RackSide, b: RackSide) = a == RackSide.BOTH || b == RackSide.BOTH || a == b

    /** Devices of [rack] that would overlap a device of [heightU] placed at [startU] on [side]. */
    fun collisions(rack: Rack, devices: List<Device>, startU: Int, heightU: Int, side: RackSide, ignoreDeviceId: String? = null): List<Device> {
        val range = startU until startU + heightU
        return devices.filter { d ->
            d.rackId == rack.id && d.id != ignoreDeviceId && d.positionU != null && sidesOverlap(d.rackSide, side) &&
                (d.positionU!! until d.positionU!! + d.heightU).any { it in range }
        }
    }

    /** Start positions where a device of [heightU] fits without overlapping and within the rack height. */
    fun freeStartPositions(rack: Rack, devices: List<Device>, heightU: Int, side: RackSide, ignoreDeviceId: String? = null): List<Int> =
        (1..(rack.heightU - heightU + 1)).filter { collisions(rack, devices, it, heightU, side, ignoreDeviceId).isEmpty() }

    fun usedUnits(rack: Rack, devices: List<Device>): Int =
        devices.filter { it.rackId == rack.id && it.positionU != null }.sumOf { it.heightU }
}
