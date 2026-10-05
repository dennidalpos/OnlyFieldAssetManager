package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.*

/** Shared draft creation for visual insertion. */
object QuickAdd {
    /** [base] with name, preset hardware, rack height and site; null keeps the base value. */
    fun draft(base: MapObjectDraft, name: String, preset: PresetResult? = null, rackHeightU: Int? = null, siteId: String? = null): MapObjectDraft {
        val named = when (base.type.kind) {
            ObjectKind.DEVICE -> base.copy(device = base.device.copy(technicalName = name.trim()))
            ObjectKind.RACK -> base.copy(rack = base.rack.copy(name = name.trim(), heightU = rackHeightU?.toString() ?: base.rack.heightU))
            ObjectKind.CABLE -> base.copy(cable = base.cable.copy(codeOrLabel = name.trim()))
        }
        val placed = if (siteId != null && named.type.kind == ObjectKind.DEVICE) named.copy(siteId = siteId, device = named.device.copy(siteId = siteId)) else named
        return preset?.let { DevicePresets.apply(placed, it) } ?: placed
    }

    fun name(draft: MapObjectDraft): String = when (draft.type.kind) {
        ObjectKind.DEVICE -> draft.device.technicalName
        ObjectKind.RACK -> draft.rack.name
        ObjectKind.CABLE -> draft.cable.codeOrLabel
    }

    /** New device mounted in [rack] from [unit] on [side]; the site follows the rack's floor. */
    fun inRack(project: Project, rack: Rack, type: ObjectType, unit: Int, side: RackSide): MapObjectDraft {
        val area = rack.areaId.orEmpty()
        val d = MapObjectDraft.newObject(project, type, ObjectMap.floorSite(project, area), area, ObjectRef(PlacementTargetType.RACK, rack.id))
        return d.copy(device = d.device.copy(positionU = unit.toString(), rackSide = side))
    }
}
