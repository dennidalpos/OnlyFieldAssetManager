package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.*

data class PortRow(val group: String?, val ports: List<Port>)

object SchematicGeometry {
    fun rows(device: Device, side: PortSide): List<PortRow> = device.ports
        .filter { it.hardware.side == null || it.hardware.side == side || it.hardware.side == PortSide.BOTH }
        .groupBy { it.hardware.group }.flatMap { (group, ports) -> ports.chunked(8).map { PortRow(group, it) } }

    fun rackUnits(rack: Rack): List<Int> = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP)
        (rack.heightU downTo 1).toList() else (1..rack.heightU).toList()
}
