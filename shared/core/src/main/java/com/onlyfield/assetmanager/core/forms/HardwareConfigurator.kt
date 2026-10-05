package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*

data class PortChanges(val ports: List<Port>, val added: Int, val removed: List<Port>, val connectedRemoved: List<Port>)

object HardwareConfigurator {
    val rackHeights = listOf(6, 9, 12, 15, 24, 42, 45, 48)
    val rackDepths = listOf(600, 800, 1000, 1070, 1200)
    val portCounts = listOf(1, 2, 4, 8, 12, 16, 24, 48)

    fun validGroups(groups: List<PortTemplate>): Boolean {
        if (groups.any { it.portCount !in 1..512 || it.startNumber !in 0..9999 || it.namePrefix.isBlank() }) return false
        val positions = groups.flatMap { group ->
            (group.startNumber until group.startNumber + group.portCount).flatMap { number ->
                (if (group.pairedSides) listOf(PortSide.FRONT, PortSide.REAR) else listOf(group.side)).map { "${group.namePrefix}$number:$it" }
            }
        }
        return positions.size == positions.toSet().size
    }

    fun ports(groups: List<PortTemplate>, deviceId: String): List<Port> = groups.flatMap { g ->
        require(g.portCount in 1..512 && g.startNumber >= 0)
        (g.startNumber until g.startNumber + g.portCount).flatMap { n ->
            val sides = if (g.pairedSides) listOf(PortSide.FRONT, PortSide.REAR) else listOf(g.side)
            sides.map { side ->
                Port(deviceId = deviceId, name = "${g.namePrefix}$n", hardware = PortHardware(
                    side = side, position = n, group = g.namePrefix, mediaType = g.mediaType, connector = g.connector,
                    speed = g.speed, role = g.role, poeStandard = g.poeStandard,
                    comboKey = g.comboGroup?.let { "$it:$n" } ?: if (g.isCombo) "${g.namePrefix}:$n" else null,
                    passageKey = if (g.pairedSides) "${g.namePrefix}:$n" else null,
                ))
            }
        }
    }

    fun preview(project: Project, device: Device, groups: List<PortTemplate>): PortChanges {
        val remaining = device.ports.toMutableList()
        var added = 0
        val ports = ports(groups, device.id).map { generated ->
            val old = remaining.firstOrNull { it.name == generated.name && (it.hardware.side == null || it.hardware.side == generated.hardware.side) }
            if (old == null) { added++; generated } else {
                remaining.remove(old)
                val hardware = if (old.hardware.customized) old.hardware.copy(side = generated.hardware.side, position = generated.hardware.position,
                    group = generated.hardware.group, passageKey = generated.hardware.passageKey, comboKey = generated.hardware.comboKey)
                else generated.hardware.copy(opticalModule = old.hardware.opticalModule)
                old.copy(hardware = hardware)
            }
        }
        val referenced = project.cables.flatMap { listOfNotNull(it.portAId, it.portBId) }.toSet() +
            project.panelMappings.flatMap { listOfNotNull(it.portAId, it.portBId) } +
            project.portVlanMemberships.map { it.portId } + project.poeMappings.map { it.portId } + project.lagGroups.flatMap { it.memberPortIds }
        return PortChanges(ports, added, remaining, remaining.filter { it.id in referenced })
    }

    fun configure(project: Project, device: Device, allowConnectedRemoval: Boolean = false, replacePorts: Boolean = device.hardware.portGroups.isNotEmpty()): Project {
        if (!replacePorts) return ProjectEdits.updateDevice(project, device)
        require(validGroups(device.hardware.portGroups)) { "Invalid or overlapping port groups" }
        val changes = preview(project, device, device.hardware.portGroups)
        require(allowConnectedRemoval || changes.connectedRemoved.isEmpty()) { "Connected ports require explicit removal" }
        var result = project
        changes.removed.forEach { result = ProjectEdits.deletePortFromDevice(result, device.id, it.id) }
        result = ProjectEdits.updateDevice(result, device.copy(ports = changes.ports))
        return ProjectEdits.withInternalPassages(result, changes.ports)
    }

    fun connect(project: Project, from: String, to: String?, medium: CableMedium, existingCableId: String? = null): Project {
        val allPorts = project.businessUnits.flatMap { it.devices }.flatMap { it.ports }.associateBy { it.id }
        require(from in allPorts && (to == null || to in allPorts)) { "Unknown port" }
        require(from != to) { "A cable cannot connect a port to itself" }
        val a = allPorts.getValue(from)
        val b = allPorts[to]
        require(b == null || a.deviceId != b.deviceId || a.hardware.comboKey == null || a.hardware.comboKey != b.hardware.comboKey) { "Combo ports share one physical attachment" }
        val existing = project.cables.find { it.id == existingCableId }
            ?: project.cables.find { setOf(it.portAId, it.portBId) == setOf(from, to) }
        val graph = ConnectionGraph(project)
        require(!graph.occupied(from, existing?.id) && (to == null || !graph.occupied(to, existing?.id))) { "Port already occupied" }
        val cable = (existing ?: Cable()).copy(portAId = from, portBId = to, deviceAId = null, deviceBId = null, medium = medium)
        return if (existing == null) ProjectEdits.addCable(project, cable) else ProjectEdits.updateCable(project, cable)
    }

    fun passage(project: Project, from: String, to: String?, mappingId: String? = null): Project {
        val ports = project.businessUnits.flatMap { it.devices }.flatMap { it.ports }.map { it.id }.toSet()
        require(from in ports && (to == null || to in ports) && from != to) { "Invalid passage endpoint" }
        val existing = project.panelMappings.find { it.id == mappingId }
        require(project.panelMappings.none { it.id != existing?.id && (it.portAId in listOfNotNull(from, to) || it.portBId in listOfNotNull(from, to)) }) { "Passage endpoint already assigned" }
        val mapping = (existing ?: PanelMapping(portAId = from)).copy(portAId = from, portBId = to, isUnknownPassage = to == null)
        return project.copy(panelMappings = project.panelMappings.filterNot { it.id == mapping.id } + mapping)
    }

    /** Removes the cable plugged into [portId], with its routes and photos. */
    fun disconnect(project: Project, portId: String): Project =
        project.cables.find { it.portAId == portId || it.portBId == portId }?.let { ProjectEdits.deleteCable(project, it.id) } ?: project

    /** Free pass-throughs: port → its internal partner, both without cables, on passive devices. */
    fun freePassages(project: Project, graph: ConnectionGraph = ConnectionGraph(project)): Map<String, String> {
        val passive = project.businessUnits.flatMap { it.devices }.filter { it.isPassive() }.flatMap { it.ports }.map { it.id }.toSet()
        return project.panelMappings.filter { !it.isUnknownPassage && it.portAId in passive && it.portBId in passive }
            .filter { !graph.occupied(it.portAId) && !graph.occupied(it.portBId!!) }
            .flatMap { listOf(it.portAId to it.portBId!!, it.portBId to it.portAId) }.toMap()
    }

    /**
     * Splits cable A–B through a pass-through: A–[entryPortId] keeps the cable (id, label, photos),
     * its internal partner–B becomes a new cable with the same medium and colour.
     */
    fun insertPassage(project: Project, cableId: String, entryPortId: String): Project {
        val cable = requireNotNull(project.cables.find { it.id == cableId }) { "Unknown cable" }
        val exit = requireNotNull(freePassages(project)[entryPortId]) { "Pass-through not free" }
        val first = cable.copy(portBId = entryPortId, deviceBId = null)
        val second = Cable(portAId = exit, portBId = cable.portBId, deviceBId = cable.deviceBId, medium = cable.medium, color = cable.color, objectTypeId = cable.objectTypeId)
        return project.copy(
            cables = project.cables.map { if (it.id == cableId) first else it } + second,
            cableRoutes = project.cableRoutes.filterNot { it.cableId == cableId },
            updatedEpochMs = System.currentTimeMillis(),
        )
    }

    fun model(project: Project, draft: MapObjectDraft, name: String): DeviceModel {
        val d = draft.device
        val extras = (draft.extraFields ?: project.customExtraFields.filter { it.targetId == draft.id })
            .filter { it.classification == AttachmentClassification.SHAREABLE }
            .map { ModelField(it.fieldKey, it.fieldValue, it.fieldType) }
        return DeviceModel(name = name.trim(), category = d.category, kind = draft.type.kind,
            objectTypeId = draft.type.id, defaultHeightU = d.heightU.toIntOrNull() ?: 1,
            hardware = d.hardware, portTemplates = d.hardware.portGroups, extraFields = extras,
            rackDefaults = draft.rack.let { r -> if (draft.type.kind == ObjectKind.RACK) RackDefaults(r.heightU.toIntOrNull() ?: 42, r.depthMm.toIntOrNull(), r.mountingDepthMm.toIntOrNull(), r.numberingDirection, r.mountingType) else null },
            cableDefaults = draft.cable.let { c -> if (draft.type.kind == ObjectKind.CABLE) CableDefaults(c.medium, c.color.ifBlank { null }) else null })
    }

    fun applyModel(draft: MapObjectDraft, model: DeviceModel): MapObjectDraft {
        require(model.kind == draft.type.kind)
        val extras = model.extraFields.map { CustomExtraField(targetType = draft.targetType.name, targetId = draft.id, fieldKey = it.key, fieldValue = it.value, fieldType = it.type) }
        return draft.copy(
            portsConfigured = draft.type.kind == ObjectKind.DEVICE,
            device = draft.device.copy(deviceModelId = model.id, heightU = model.defaultHeightU.toString(), hardware = model.hardware.copy(portGroups = model.portTemplates)),
            rack = model.rackDefaults?.let { r -> draft.rack.copy(deviceModelId = model.id, heightU = r.heightU.toString(), depthMm = r.depthMm?.toString().orEmpty(), mountingDepthMm = r.mountingDepthMm?.toString().orEmpty(), numberingDirection = r.numberingDirection, mountingType = r.mountingType) } ?: draft.rack,
            cable = model.cableDefaults?.let { c -> draft.cable.copy(deviceModelId = model.id, medium = c.medium, color = c.color.orEmpty()) } ?: draft.cable,
            extraFields = draft.extraFields?.let { old -> old + extras.filter { e -> old.none { it.fieldKey == e.fieldKey } } } ?: extras,
        )
    }

    fun modelDraft(project: Project, model: DeviceModel): MapObjectDraft {
        val type = ObjectCatalog.type(project, model.objectTypeId) ?: ObjectCatalog.types(project).first { it.kind == model.kind && (model.kind != ObjectKind.DEVICE || it.category == model.category) }
        val draft = MapObjectDraft(id = model.id, type = type, buId = project.businessUnits.firstOrNull()?.id.orEmpty(), areaId = "")
        return applyModel(draft, model).let { d -> d.copy(device = d.device.copy(technicalName = model.name, category = model.category), rack = d.rack.copy(name = model.name), cable = d.cable.copy(codeOrLabel = model.name)) }
    }
}

/** Merge only session edits, preserving unrelated changes since the editor opened. */
data class ConfigurationSession(val original: Project, val project: Project = original) {
    fun apply(current: Project): Project {
        fun <T> merge(old: List<T>, edited: List<T>, latest: List<T>, id: (T) -> String): List<T> {
            val originalById = old.associateBy(id)
            val editedById = edited.associateBy(id)
            val removed = originalById.keys - editedById.keys
            val changed = edited.filter { originalById[id(it)] != it }.associateBy(id)
            return latest.filterNot { id(it) in removed }.map { changed[id(it)] ?: it } + changed.values.filter { item -> latest.none { id(it) == id(item) } }
        }
        return current.copy(
            businessUnits = merge(original.businessUnits, project.businessUnits, current.businessUnits, BusinessUnit::id).map { bu ->
                val old = original.businessUnits.find { it.id == bu.id }
                val edited = project.businessUnits.find { it.id == bu.id }
                val latest = current.businessUnits.find { it.id == bu.id }
                if (old != null && edited != null && latest != null) latest.copy(devices = merge(old.devices, edited.devices, latest.devices, Device::id)) else bu
            },
            racks = merge(original.racks, project.racks, current.racks, Rack::id),
            cables = merge(original.cables, project.cables, current.cables, Cable::id),
            deviceModels = merge(original.deviceModels, project.deviceModels, current.deviceModels, DeviceModel::id),
            panelMappings = merge(original.panelMappings, project.panelMappings, current.panelMappings, PanelMapping::id),
            customExtraFields = merge(original.customExtraFields, project.customExtraFields, current.customExtraFields, CustomExtraField::id),
            objectTypes = merge(original.objectTypes, project.objectTypes, current.objectTypes, ObjectType::id),
            objectContainments = merge(original.objectContainments, project.objectContainments, current.objectContainments, ObjectContainment::id),
            floorplanPlacements = merge(original.floorplanPlacements, project.floorplanPlacements, current.floorplanPlacements, FloorplanPlacement::id),
            cableRoutes = merge(original.cableRoutes, project.cableRoutes, current.cableRoutes, CableRoute::id),
            vlans = merge(original.vlans, project.vlans, current.vlans, Vlan::id),
            subnets = merge(original.subnets, project.subnets, current.subnets, Subnet::id),
            portVlanMemberships = merge(original.portVlanMemberships, project.portVlanMemberships, current.portVlanMemberships, PortVlanMembership::id),
            poeMappings = merge(original.poeMappings, project.poeMappings, current.poeMappings, PoeMapping::id),
            lagGroups = merge(original.lagGroups, project.lagGroups, current.lagGroups, LagGroup::id),
        )
    }
}
