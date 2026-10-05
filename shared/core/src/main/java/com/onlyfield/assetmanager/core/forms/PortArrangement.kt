package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.*

/** Reusable order within a connector group and side. */
object PortArrangement {
    fun rebind(device: Device, ports: List<Port>): Device {
        val latest = ports.associateBy { it.id }
        fun partner(side: PortSide, group: String?, key: String) = device.ports.singleOrNull {
            side(it) == side && it.hardware.group == group && key(it) == key
        }?.let { latest[it.id] } ?: ports.singleOrNull { side(it) == side && it.hardware.group == group && key(it) == key }
        val layouts = device.hardware.portLayouts.mapNotNull { layout ->
            val rebound = layout.order.mapNotNull { partner(layout.side, layout.group, it) }
            val anchor = rebound.firstOrNull() ?: ports.firstOrNull { side(it) == layout.side && it.hardware.group == layout.group } ?: return@mapNotNull null
            layout.copy(side = side(anchor), group = anchor.hardware.group, order = rebound.map(::key))
        }
        val overrides = device.hardware.portPoeOverrides.mapNotNull { override ->
            partner(override.side, override.group, override.key)?.let { override.copy(side = side(it), group = it.hardware.group, key = key(it)) }
        }
        return reconcile(device.copy(ports = ports, hardware = device.hardware.copy(portLayouts = layouts, portPoeOverrides = overrides)))
    }

    fun key(port: Port): String = port.hardware.position?.toString() ?: port.name
    fun side(port: Port): PortSide = port.hardware.side ?: PortSide.FRONT

    fun layout(device: Device, ports: List<Port>): PortLayout {
        require(ports.isNotEmpty())
        val side = side(ports.first())
        val group = ports.first().hardware.group
        val saved = device.hardware.portLayouts.singleOrNull { it.side == side && it.group == group }
        val keys = ports.map(::key)
        val defaults = if (ports.size > 8 && device.objectTypeId != "patch-panel")
            keys.filterIndexed { i, _ -> i % 2 == 0 } + keys.filterIndexed { i, _ -> i % 2 == 1 } else keys
        val order = saved?.order.orEmpty().filter { it in keys }.distinct()
        return PortLayout(side, group, saved?.rows?.coerceIn(1, 2) ?: if (ports.size > 8 && device.objectTypeId != "patch-panel") 2 else 1,
            if (saved == null) defaults else order + keys.filterNot { it in order })
    }

    fun set(device: Device, layout: PortLayout): Device {
        require(layout.rows in 1..2 && layout.order.distinct().size == layout.order.size)
        return device.copy(hardware = device.hardware.copy(portLayouts =
            device.hardware.portLayouts.filterNot { it.side == layout.side && it.group == layout.group } + layout))
    }

    fun valid(hardware: HardwareSpec): Boolean = hardware.portLayouts.all { it.rows in 1..2 && it.order.distinct().size == it.order.size } &&
        hardware.portLayouts.map { it.side to it.group }.distinct().size == hardware.portLayouts.size &&
        hardware.portPoeOverrides.map { Triple(it.side, it.group, it.key) }.distinct().size == hardware.portPoeOverrides.size

    fun reconcile(device: Device): Device = device.copy(hardware = device.hardware.copy(portLayouts =
        device.ports.groupBy { side(it) to it.hardware.group }.values.mapNotNull { ports ->
            if (device.hardware.portLayouts.any { it.side == side(ports.first()) && it.group == ports.first().hardware.group }) layout(device, ports) else null
        }, portPoeOverrides = device.hardware.portPoeOverrides.filter { override ->
            device.ports.any { side(it) == override.side && it.hardware.group == override.group && key(it) == override.key }
        }))
}
