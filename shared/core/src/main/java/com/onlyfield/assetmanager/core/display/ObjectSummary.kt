package com.onlyfield.assetmanager.core.display

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/**
 * Read-only facts about one map object, grouped by what they describe so detail panes
 * never mix identity with location: identifiers, then where the object is.
 */
data class ObjectSummary(
    /** Labelled identifiers that have a value (label, alias, IP, MAC, serial). */
    val identity: List<Fact>,
    /** Floor name, then containers from the outermost one. */
    val location: List<String>,
    /** Rack units and side, e.g. "U12–13 · Fronte", when mounted at a position. */
    val mount: String?,
) {
    data class Fact(val label: String, val value: String)

    /** "Terra › RACK-A › U12" or null when the object is nowhere. */
    fun place(): String? = (location + listOfNotNull(mount)).takeIf { it.isNotEmpty() }?.joinToString(" › ")

    companion object {
        fun of(
            project: Project,
            ref: ObjectRef,
            i18n: Messages = Messages(),
            index: ProjectIndex = ProjectIndex(project),
            hierarchy: HierarchyIndex = HierarchyIndex(project),
        ): ObjectSummary {
            val device = if (ref.type == PlacementTargetType.DEVICE) index.device(ref.id) else null
            fun fact(key: String, value: String?) = value?.trim()?.takeIf { it.isNotEmpty() }?.let { Fact(i18n.text(key), it) }
            val identity = listOfNotNull(
                fact("config.label", device?.physicalLabel),
                fact("config.alias", device?.alias),
                fact("config.ip", device?.ipAddress),
                fact("config.mac", device?.macAddress),
                fact("config.serial", device?.serialNumber),
            )
            val containers = hierarchy.ancestors(ref).reversed().map { ObjectHierarchy.name(project, it, i18n) }
            val location = listOfNotNull(index.area(hierarchy.areaId(ref))?.name) + containers
            val mount = device?.takeIf { it.rackId != null }?.positionU?.let { start ->
                val units = if (device.heightU > 1) "U$start–${start + device.heightU - 1}" else "U$start"
                listOfNotNull(units, device.rackSide.takeIf { it != RackSide.BOTH }?.toDisplayString(i18n)).joinToString(" · ")
            }
            return ObjectSummary(identity, location, mount)
        }
    }
}
