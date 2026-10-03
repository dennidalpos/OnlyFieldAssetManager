package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import java.util.UUID

data class MapObjectDraft(
    val id: String = UUID.randomUUID().toString(),
    val type: ObjectType,
    val buId: String,
    val areaId: String,
    val device: DeviceForm = DeviceForm(businessUnitId = buId, areaId = areaId, category = type.category, objectTypeId = type.id, mountingType = MountingType.OUT_OF_RACK),
    val rack: RackForm = RackForm(areaId = areaId),
    val cable: CableForm = CableForm(medium = type.cableMedium),
    val deviceAId: String? = null,
    val deviceBId: String? = null,
    val extraFields: List<CustomExtraField>? = null,
    val parentRef: ObjectRef? = null,
) {
    val targetType get() = when (type.kind) { ObjectKind.DEVICE -> AttachmentTargetType.DEVICE; ObjectKind.RACK -> AttachmentTargetType.RACK; ObjectKind.CABLE -> AttachmentTargetType.CABLE }
    fun errors(project: Project) = when (type.kind) {
        ObjectKind.DEVICE -> device.errors(project.racks.find { it.id == device.rackId }?.heightU)
        ObjectKind.RACK -> rack.errors()
        ObjectKind.CABLE -> cable.errors()
    }
    fun apply(project: Project): Project {
        require(errors(project).isEmpty())
        val source = if (type.id != "legacy" && ObjectCatalog.builtins.none { it.id == type.id })
            project.copy(objectTypes = project.objectTypes.filterNot { it.id == type.id } + type) else project
        val devices = source.businessUnits.flatMap { it.devices }
        val updated = when (type.kind) {
            ObjectKind.DEVICE -> {
                val existing = devices.find { it.id == id }
                val saved = device.toDevice(existing, "Mappa").copy(id = id, objectTypeId = if (type.id == "legacy") existing?.objectTypeId else type.id)
                val p = if (existing == null) ProjectEdits.addDevice(source, buId, saved) else ProjectEdits.updateDevice(source, saved)
                if (existing == null) ObjectMap.placeNew(p, areaId, PlacementTargetType.DEVICE, id) else p
            }
            ObjectKind.RACK -> {
                val existing = project.racks.find { it.id == id }
                val saved = rack.toRack(existing).copy(id = id)
                val p = if (existing == null) ProjectEdits.addRack(source, saved) else ProjectEdits.updateRack(source, saved)
                if (existing == null) ObjectMap.placeNew(p, areaId, PlacementTargetType.RACK, id) else p
            }
            ObjectKind.CABLE -> {
                val existing = project.cables.find { it.id == id }
                val saved = cable.toCable(existing).copy(id = id, objectTypeId = if (existing == null) type.id else existing.objectTypeId, deviceAId = if (cable.portAId == null) deviceAId else null, deviceBId = if (cable.portBId == null) deviceBId else null)
                val p = if (existing == null) ProjectEdits.addCable(source, saved) else ProjectEdits.updateCable(source, saved)
                if (existing == null) ObjectMap.saveRoute(p, CableRoute(cableId = id, areaId = areaId)) else p
            }
        }
        val contained = if (type.kind == ObjectKind.CABLE) updated else ObjectHierarchy.assign(updated, ObjectRef(if (type.kind == ObjectKind.RACK) PlacementTargetType.RACK else PlacementTargetType.DEVICE, id), parentRef)
        return contained.copy(customExtraFields = extraFields?.let { fields -> updated.customExtraFields.filterNot { it.targetId == id } + fields } ?: updated.customExtraFields, updatedEpochMs = System.currentTimeMillis())
    }
    companion object {
        fun newObject(project: Project, type: ObjectType, buId: String, areaId: String, parent: ObjectRef): MapObjectDraft {
            val rack = (listOf(parent) + ObjectHierarchy.ancestors(project, parent)).firstOrNull { it.type == PlacementTargetType.RACK }
            val draft = MapObjectDraft(type = type, buId = buId, areaId = areaId, parentRef = parent)
            return draft.copy(device = draft.device.copy(rackId = rack?.id, mountingType = if (rack == null) MountingType.OUT_OF_RACK else MountingType.RACK_MOUNT))
        }

        fun device(project: Project, buId: String, areaId: String, id: String): MapObjectDraft {
            val d = project.businessUnits.flatMap { it.devices }.first { it.id == id }
            val type = ObjectCatalog.type(project, d.objectTypeId) ?: ObjectType("legacy", "Apparato", d.category)
            return MapObjectDraft(id = id, type = type, buId = buId, areaId = areaId, device = DeviceForm.from(d, buId), parentRef = ObjectHierarchy.parent(project, ObjectRef(PlacementTargetType.DEVICE, id)))
        }
        fun rack(project: Project, buId: String, areaId: String, id: String) = MapObjectDraft(id = id, type = ObjectCatalog.builtins.first { it.kind == ObjectKind.RACK }, buId = buId, areaId = areaId, rack = RackForm.from(project.racks.first { it.id == id }), parentRef = ObjectHierarchy.parent(project, ObjectRef(PlacementTargetType.RACK, id)))
        fun cable(project: Project, buId: String, areaId: String, id: String): MapObjectDraft {
            val c = project.cables.first { it.id == id }
            return MapObjectDraft(id = id, type = ObjectCatalog.type(project, c.objectTypeId) ?: ObjectType("cable", "Cavo", kind = ObjectKind.CABLE), buId = buId, areaId = areaId, cable = CableForm.from(c), deviceAId = c.deviceAId, deviceBId = c.deviceBId)
        }
    }
}
