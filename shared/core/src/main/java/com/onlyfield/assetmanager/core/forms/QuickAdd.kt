package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.*

/** Quick insertion: a new object from a few menus, saved at once; details are filled later. */
object QuickAdd {
    /** [base] with name, preset hardware, rack height and business unit; null keeps the base value. */
    fun draft(base: MapObjectDraft, name: String, preset: PresetResult? = null, rackHeightU: Int? = null, buId: String? = null): MapObjectDraft {
        val named = when (base.type.kind) {
            ObjectKind.DEVICE -> base.copy(device = base.device.copy(technicalName = name.trim()))
            ObjectKind.RACK -> base.copy(rack = base.rack.copy(name = name.trim(), heightU = rackHeightU?.toString() ?: base.rack.heightU))
            ObjectKind.CABLE -> base.copy(cable = base.cable.copy(codeOrLabel = name.trim()))
        }
        val placed = if (buId != null && named.type.kind == ObjectKind.DEVICE) named.copy(buId = buId, device = named.device.copy(businessUnitId = buId)) else named
        return preset?.let { DevicePresets.apply(placed, it) } ?: placed
    }

    fun name(draft: MapObjectDraft): String = when (draft.type.kind) {
        ObjectKind.DEVICE -> draft.device.technicalName
        ObjectKind.RACK -> draft.rack.name
        ObjectKind.CABLE -> draft.cable.codeOrLabel
    }

    /** Types with menus (preset or rack height) or missing data show a second step; others are added with one tap. */
    fun needsDetails(draft: MapObjectDraft, hasPreset: Boolean, hasErrors: Boolean): Boolean =
        hasPreset || hasErrors || draft.type.kind == ObjectKind.RACK

    /** New device mounted in [rack] from [unit] on [side]; the business unit follows the rack's floor. */
    fun inRack(project: Project, rack: Rack, type: ObjectType, unit: Int, side: RackSide): MapObjectDraft {
        val area = rack.areaId.orEmpty()
        val d = MapObjectDraft.newObject(project, type, ObjectMap.floorBusinessUnit(project, area), area, ObjectRef(PlacementTargetType.RACK, rack.id))
        return d.copy(device = d.device.copy(positionU = unit.toString(), rackSide = side))
    }
}
