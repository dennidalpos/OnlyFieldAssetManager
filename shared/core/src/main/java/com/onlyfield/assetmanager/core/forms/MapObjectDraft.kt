package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import java.util.UUID

data class MapObjectDraft(
    val id: String = UUID.randomUUID().toString(),
    val type: ObjectType,
    val siteId: String,
    val areaId: String,
    val device: DeviceForm = DeviceForm(siteId = siteId, areaId = areaId, category = type.category, objectTypeId = type.id, mountingType = MountingType.OUT_OF_RACK),
    val rack: RackForm = RackForm(areaId = areaId),
    val cable: CableForm = CableForm(medium = type.cableMedium),
    val deviceAId: String? = null,
    val deviceBId: String? = null,
    val extraFields: List<CustomExtraField>? = null,
    val parentRef: ObjectRef? = null,
    val session: ConfigurationSession? = null,
    val allowConnectedRemoval: Boolean = false,
    val portsConfigured: Boolean = false,
    /** Where a new object was requested on the floor; null places it in the first free slot. */
    val mapPoint: MapPoint? = null,
    /** Port page to open first in the editor (UI only, never saved). */
    val focusPortId: String? = null,
) {
    /** Same draft as another object type of the same kind (category follows the type). */
    fun withType(type: ObjectType) = copy(type = type, device = device.copy(objectTypeId = type.id, category = type.category))
    val targetType get() = when (type.kind) { ObjectKind.DEVICE -> AttachmentTargetType.DEVICE; ObjectKind.RACK -> AttachmentTargetType.RACK; ObjectKind.CABLE -> AttachmentTargetType.CABLE }
    fun errors(project: Project, i18n: Messages = Messages()): Map<String, String> {
        val p = session?.apply(project) ?: project
        val errors = when (type.kind) {
        ObjectKind.DEVICE -> device.errors(p.racks.find { it.id == device.rackId }?.heightU, i18n = i18n)
        ObjectKind.RACK -> rack.errors(i18n = i18n)
        ObjectKind.CABLE -> cable.errors(i18n = i18n)
        }
        val graph = ConnectionGraph(p)
        val allDevices = p.sites.flatMap { it.devices }
        val portIds = allDevices.flatMap { it.ports }.map { it.id }.toSet()
        return errors + buildMap {
            if (type.kind == ObjectKind.CABLE && (listOfNotNull(cable.portAId, cable.portBId).any { it !in portIds } || listOfNotNull(deviceAId, deviceBId).any { id -> allDevices.none { it.id == id } })) put("ports", i18n.text("config.invalidEndpoint"))
            if (type.kind == ObjectKind.RACK && rack.heightU.toIntOrNull()?.let { height -> allDevices.any { it.rackId == id && it.positionU?.let { position -> position + it.heightU - 1 > height } == true } } == true) put("heightU", i18n.text("config.rackTooSmall"))

            if (type.kind == ObjectKind.CABLE && (listOfNotNull(cable.portAId, cable.portBId).any { graph.occupied(it, id) })) put("ports", i18n.text("config.occupied"))
            if (type.kind == ObjectKind.CABLE && cable.portAId != null && cable.portBId != null) {
                val a = allDevices.flatMap { it.ports }.find { it.id == cable.portAId }
                val b = allDevices.flatMap { it.ports }.find { it.id == cable.portBId }
                if (a != null && b != null && (a.id == b.id || (a.deviceId == b.deviceId && a.hardware.comboKey != null && a.hardware.comboKey == b.hardware.comboKey)))
                    put("ports", i18n.text("config.invalidEndpoint"))
            }
            if (type.kind == ObjectKind.DEVICE && (portsConfigured || device.hardware.portGroups.isNotEmpty())) {
                val existing = p.sites.flatMap { it.devices }.find { it.id == id } ?: Device(id = id, technicalName = device.technicalName)
                if (!HardwareConfigurator.validGroups(device.hardware.portGroups) || !PortArrangement.valid(device.hardware)) put("ports", i18n.text("config.invalidHardware"))
                else if (!allowConnectedRemoval && HardwareConfigurator.preview(p, existing, device.hardware.portGroups).connectedRemoved.isNotEmpty()) put("ports", i18n.text("config.removeConnected"))
            }
        }
    }
    /** Unvalidated project as it would look with this draft; used for live previews. */
    fun preview(project: Project, i18n: Messages = Messages()): Project {
        val configured = session?.apply(project) ?: project
        val p = if (type.id != "legacy" && ObjectCatalog.builtins.none { it.id == type.id })
            configured.copy(objectTypes = configured.objectTypes.filterNot { it.id == type.id } + type) else configured
        return when (type.kind) {
            ObjectKind.DEVICE -> {
                val existing = p.sites.flatMap { it.devices }.find { it.id == id }
                val saved = device.toDevice(existing, i18n.text("text.2b71c6a11df1")).copy(id = id)
                val added = if (existing == null) ProjectEdits.addDevice(p, device.siteId ?: siteId, saved) else ProjectEdits.updateDevice(p, saved)
                val ports = portsConfigured || saved.hardware.portGroups.isNotEmpty()
                if (ports && HardwareConfigurator.validGroups(saved.hardware.portGroups) && (allowConnectedRemoval || HardwareConfigurator.preview(p, saved, saved.hardware.portGroups).connectedRemoved.isEmpty()))
                    HardwareConfigurator.configure(added, saved, allowConnectedRemoval, true) else added
            }
            ObjectKind.RACK -> {
                val saved = rack.toRack(p.racks.find { it.id == id }).copy(id = id)
                if (p.racks.any { it.id == id }) ProjectEdits.updateRack(p, saved) else ProjectEdits.addRack(p, saved)
            }
            ObjectKind.CABLE -> {
                val saved = cable.toCable(p.cables.find { it.id == id }).copy(id = id, deviceAId = deviceAId, deviceBId = deviceBId)
                if (p.cables.any { it.id == id }) ProjectEdits.updateCable(p, saved) else ProjectEdits.addCable(p, saved)
            }
        }
    }

    fun apply(project: Project, i18n: Messages = Messages()): Project {
        require(errors(project, i18n = i18n).isEmpty())
        val configured = session?.apply(project) ?: project
        val source = if (type.id != "legacy" && ObjectCatalog.builtins.none { it.id == type.id })
            configured.copy(objectTypes = configured.objectTypes.filterNot { it.id == type.id } + type) else configured
        val devices = source.sites.flatMap { it.devices }
        val updated = when (type.kind) {
            ObjectKind.DEVICE -> {
                val existing = devices.find { it.id == id }
                val saved = device.toDevice(existing, i18n.text("text.2b71c6a11df1")).copy(id = id, objectTypeId = if (type.id == "legacy") existing?.objectTypeId else type.id)
                val added = if (existing == null) ProjectEdits.addDevice(source, device.siteId ?: siteId, saved) else ProjectEdits.updateDevice(source, saved, i18n = i18n)
                val p = HardwareConfigurator.configure(added, saved, allowConnectedRemoval, portsConfigured || saved.hardware.portGroups.isNotEmpty())
                if (existing == null && areaId.isNotBlank()) place(p, PlacementTargetType.DEVICE) else p
            }
            ObjectKind.RACK -> {
                val existing = source.racks.find { it.id == id }
                val saved = rack.toRack(existing).copy(id = id)
                val p = if (existing == null) ProjectEdits.addRack(source, saved) else ProjectEdits.updateRack(source, saved)
                if (existing == null && areaId.isNotBlank()) place(p, PlacementTargetType.RACK) else p
            }
            ObjectKind.CABLE -> {
                val existing = source.cables.find { it.id == id }
                val saved = cable.toCable(existing).copy(id = id, objectTypeId = if (existing == null) type.id else existing.objectTypeId, deviceAId = if (cable.portAId == null) deviceAId else null, deviceBId = if (cable.portBId == null) deviceBId else null)
                val p = if (existing == null) ProjectEdits.addCable(source, saved) else ProjectEdits.updateCable(source, saved)
                if (existing == null && areaId.isNotBlank()) ObjectMap.saveRoute(p, CableRoute(cableId = id, areaId = areaId)) else p
            }
        }
        val contained = if (type.kind == ObjectKind.CABLE) updated else ObjectHierarchy.assign(updated, ObjectRef(if (type.kind == ObjectKind.RACK) PlacementTargetType.RACK else PlacementTargetType.DEVICE, id), parentRef, i18n = i18n)
        return contained.copy(customExtraFields = extraFields?.let { fields -> updated.customExtraFields.filterNot { it.targetId == id } + fields } ?: updated.customExtraFields, updatedEpochMs = System.currentTimeMillis())
    }
    private fun place(p: Project, target: PlacementTargetType) =
        mapPoint?.let { ObjectMap.place(p, areaId, target, id, it) } ?: ObjectMap.placeNew(p, areaId, target, id)

    companion object {
        fun forDevice(project: Project, device: Device?): MapObjectDraft {
            val site = device?.let { d -> project.sites.find { b -> b.devices.any { it.id == d.id } } } ?: project.sites.firstOrNull()
            val area = device?.let { ObjectMap.areaId(project, it) }.orEmpty()
            return if (device != null) device(project, site?.id.orEmpty(), area, device.id) else
                MapObjectDraft(type = ObjectCatalog.builtins.first { it.id == "switch" }, siteId = site?.id.orEmpty(), areaId = area,
                    device = DeviceForm(siteId = site?.id, areaId = null))
        }

        fun forRack(project: Project, rack: Rack?): MapObjectDraft = if (rack != null)
            rack(project, project.sites.firstOrNull()?.id.orEmpty(), rack.areaId.orEmpty(), rack.id) else
            MapObjectDraft(type = ObjectCatalog.builtins.first { it.kind == ObjectKind.RACK }, siteId = project.sites.firstOrNull()?.id.orEmpty(), areaId = "", rack = RackForm())

        fun forCable(project: Project, cable: Cable?): MapObjectDraft = if (cable != null)
            cable(project, project.sites.firstOrNull()?.id.orEmpty(), "", cable.id) else
            MapObjectDraft(type = ObjectCatalog.builtins.first { it.kind == ObjectKind.CABLE }, siteId = project.sites.firstOrNull()?.id.orEmpty(), areaId = "")

        fun newObject(project: Project, type: ObjectType, siteId: String, areaId: String, parent: ObjectRef): MapObjectDraft {
            val rack = (listOf(parent) + ObjectHierarchy.ancestors(project, parent)).firstOrNull { it.type == PlacementTargetType.RACK }
            val draft = MapObjectDraft(type = type, siteId = siteId, areaId = areaId, parentRef = parent)
            return draft.copy(device = draft.device.copy(rackId = rack?.id, mountingType = if (rack == null) MountingType.OUT_OF_RACK else MountingType.RACK_MOUNT))
        }

        fun device(project: Project, siteId: String, areaId: String, id: String, i18n: Messages = Messages()): MapObjectDraft {
            val d = project.sites.flatMap { it.devices }.first { it.id == id }
            val type = ObjectCatalog.type(project, d.objectTypeId) ?: ObjectType("legacy", i18n.text("text.cf301d95d32c"), d.category)
            return MapObjectDraft(id = id, type = type, siteId = siteId, areaId = areaId, device = DeviceForm.from(d, siteId), extraFields = project.customExtraFields.filter { it.targetId == id }, parentRef = ObjectHierarchy.parent(project, ObjectRef(PlacementTargetType.DEVICE, id)))
        }
        fun rack(project: Project, siteId: String, areaId: String, id: String) = MapObjectDraft(id = id, type = ObjectCatalog.builtins.first { it.kind == ObjectKind.RACK }, siteId = siteId, areaId = areaId, rack = RackForm.from(project.racks.first { it.id == id }), extraFields = project.customExtraFields.filter { it.targetId == id }, parentRef = ObjectHierarchy.parent(project, ObjectRef(PlacementTargetType.RACK, id)))
        fun cable(project: Project, siteId: String, areaId: String, id: String, i18n: Messages = Messages()): MapObjectDraft {
            val c = project.cables.first { it.id == id }
            return MapObjectDraft(id = id, type = ObjectCatalog.type(project, c.objectTypeId) ?: ObjectType("cable", i18n.text("text.89dbe18e8407"), kind = ObjectKind.CABLE), siteId = siteId, areaId = areaId, cable = CableForm.from(c), deviceAId = c.deviceAId, deviceBId = c.deviceBId, extraFields = project.customExtraFields.filter { it.targetId == id })
        }
    }
}
