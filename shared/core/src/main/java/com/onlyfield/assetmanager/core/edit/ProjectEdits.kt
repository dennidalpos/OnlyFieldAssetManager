package com.onlyfield.assetmanager.core.edit

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.*
import kotlinx.serialization.json.Json
import java.util.UUID

/** Pure, immutable edit operations on a [Project], shared by the Android and Windows apps. */
object ProjectEdits {

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // --- STRUCTURE (business units and areas) ---

    fun addBusinessUnit(project: Project, name: String): Project = project.copy(
        businessUnits = project.businessUnits + BusinessUnit(name = name),
        updatedEpochMs = System.currentTimeMillis()
    )

    fun renameBusinessUnit(project: Project, buId: String, name: String): Project = project.copy(
        businessUnits = project.businessUnits.map { if (it.id == buId) it.copy(name = name) else it },
        updatedEpochMs = System.currentTimeMillis()
    )

    /** Deletes an empty business unit; returns null when it still contains devices or areas. */
    fun deleteBusinessUnit(project: Project, buId: String): Project? {
        val bu = project.businessUnits.find { it.id == buId } ?: return project
        if (bu.devices.isNotEmpty() || bu.areas.isNotEmpty() || bu.sites.any { it.areas.isNotEmpty() }) return null
        return project.copy(businessUnits = project.businessUnits - bu, updatedEpochMs = System.currentTimeMillis())
    }

    fun addArea(project: Project, buId: String, area: Area): Project = project.copy(
        businessUnits = project.businessUnits.map { if (it.id == buId) it.copy(areas = it.areas + area) else it },
        updatedEpochMs = System.currentTimeMillis()
    )

    fun updateArea(project: Project, area: Area): Project = project.copy(
        businessUnits = project.businessUnits.map { bu ->
            bu.copy(
                areas = bu.areas.map { if (it.id == area.id) area else it },
                sites = bu.sites.map { site -> site.copy(areas = site.areas.map { if (it.id == area.id) area else it }) }
            )
        },
        updatedEpochMs = System.currentTimeMillis()
    )

    /** Deletes an area; returns null when devices, racks or placements still reference it. */
    fun deleteArea(project: Project, areaId: String): Project? {
        val inUse = project.businessUnits.any { bu -> bu.devices.any { it.areaId == areaId } } ||
            project.racks.any { it.areaId == areaId } ||
            project.floorplanPlacements.any { it.areaId == areaId } || project.cableRoutes.any { it.areaId == areaId }
        if (inUse) return null
        return project.copy(
            businessUnits = project.businessUnits.map { bu ->
                bu.copy(
                    areas = bu.areas.filterNot { it.id == areaId },
                    sites = bu.sites.map { site -> site.copy(areas = site.areas.filterNot { it.id == areaId }) }
                )
            },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    // --- DEVICES ---

    fun addDevice(project: Project, buId: String, device: Device): Project {
        val updatedBus = project.businessUnits.map { bu ->
            if (bu.id == buId) {
                bu.copy(devices = bu.devices + device)
            } else {
                bu
            }
        }
        return project.copy(
            businessUnits = updatedBus,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateDevice(project: Project, updatedDevice: Device, i18n: Messages = Messages()): Project {
        val previous = project.businessUnits.flatMap { it.devices }.find { it.id == updatedDevice.id }
        val updatedBus = project.businessUnits.map { bu ->
            val hasDev = bu.devices.any { it.id == updatedDevice.id }
            if (hasDev) {
                val newDevs = bu.devices.map { if (it.id == updatedDevice.id) updatedDevice else it }
                bu.copy(devices = newDevs)
            } else {
                bu
            }
        }
        val result = project.copy(
            businessUnits = updatedBus,
            updatedEpochMs = System.currentTimeMillis()
        )
        return if (previous?.rackId != updatedDevice.rackId) ObjectHierarchy.assign(result, ObjectRef(PlacementTargetType.DEVICE, updatedDevice.id), updatedDevice.rackId?.let { ObjectRef(PlacementTargetType.RACK, it) }, i18n = i18n) else ObjectHierarchy.synchronize(result)
    }

    fun deleteDeviceToTrash(project: Project, deviceId: String, i18n: Messages = Messages()): Pair<Project, TrashItem?> {
        val deviceBU = project.businessUnits.find { bu -> bu.devices.any { it.id == deviceId } }
            ?: return Pair(project, null)
        val device = deviceBU.devices.find { it.id == deviceId } ?: return Pair(project, null)

        val jsonStr = jsonSerializer.encodeToString(Device.serializer(), device)
        val affectedPortIds = device.ports.map { it.id }.toSet()

        // Disconnect cables attached to deleted device ports
        var updatedCablesCount = 0
        val updatedCables = project.cables.map { cable ->
            if (affectedPortIds.contains(cable.portAId) || affectedPortIds.contains(cable.portBId) || cable.deviceAId == deviceId || cable.deviceBId == deviceId) {
                updatedCablesCount++
                cable.copy(
                    deviceAId = cable.deviceAId?.takeUnless { it == deviceId },
                    deviceBId = cable.deviceBId?.takeUnless { it == deviceId },
                    portAId = if (affectedPortIds.contains(cable.portAId)) null else cable.portAId,
                    portBId = if (affectedPortIds.contains(cable.portBId)) null else cable.portBId,
                    observation = Observation(
                        source = i18n.text("text.d8d551a2ac65"),
                        timestampEpochMs = System.currentTimeMillis(),
                        status = ObservationStatus.TO_VERIFY,
                        notes = i18n.text("text.049cd5acb246", device.technicalName)
                    )
                )
            } else {
                cable
            }
        }

        val trashItem = TrashItem(
            projectId = project.id,
            itemType = "DEVICE",
            itemId = deviceId,
            displayName = device.technicalName,
            serializedJson = jsonStr,
            affectedReferencesSummary = i18n.text("text.0a039cbd68d3", device.ports.size, updatedCablesCount)
        )

        val updatedBus = project.businessUnits.map { bu ->
            if (bu.id == deviceBU.id) {
                bu.copy(devices = bu.devices.filterNot { it.id == deviceId })
            } else {
                bu
            }
        }

        val updatedProject = project.copy(
            businessUnits = updatedBus,
            cables = updatedCables,
            cableRoutes = project.businessUnits.flatMap { ObjectMap.areas(it) }.flatMap { ObjectMap.routes(project, it.id) }.map { route ->
                route.copy(points = ObjectMap.routePoints(project, route, ObjectMap.nodes(project, route.areaId)))
            },
            floorplanPlacements = project.floorplanPlacements.filterNot { it.targetType == PlacementTargetType.DEVICE && it.targetId == deviceId },
            updatedEpochMs = System.currentTimeMillis()
        )

        val ref = ObjectRef(PlacementTargetType.DEVICE, deviceId)
        return Pair(ObjectHierarchy.afterDeletion(project, updatedProject, ref), ObjectHierarchy.snapshot(project, ref, trashItem))
    }

    fun batchEditDevices(
        project: Project,
        deviceIds: List<String>,
        changes: BatchDeviceChanges,
        i18n: Messages = Messages()): Project {
        if (deviceIds.isEmpty()) return project

        val deviceIdSet = deviceIds.toSet()
        val updatedBus = project.businessUnits.map { bu ->
            val updatedDevs = bu.devices.map { dev ->
                if (deviceIdSet.contains(dev.id)) {
                    var updated = dev
                    if (changes.updateSiteId) updated = updated.copy(siteId = changes.siteId)
                    if (changes.updateAreaId) updated = updated.copy(areaId = changes.areaId)
                    if (changes.updateCategory && changes.category != null) updated = updated.copy(category = changes.category)
                    if (changes.updateRackId) updated = updated.copy(rackId = changes.rackId)
                    if (changes.updateMountingType && changes.mountingType != null) updated = updated.copy(mountingType = changes.mountingType)
                    if (changes.updateObservationNotes) {
                        val obs = updated.observation ?: Observation("BatchEditDesktop", System.currentTimeMillis())
                        updated = updated.copy(observation = obs.copy(notes = changes.observationNotes))
                    }
                    updated
                } else {
                    dev
                }
            }
            bu.copy(devices = updatedDevs)
        }

        var result = project.copy(
            businessUnits = updatedBus,
            updatedEpochMs = System.currentTimeMillis()
        )
        if (changes.updateRackId) for (id in deviceIds) result = ObjectHierarchy.assign(result, ObjectRef(PlacementTargetType.DEVICE, id), changes.rackId?.let { ObjectRef(PlacementTargetType.RACK, it) }, i18n = i18n)
        return ObjectHierarchy.synchronize(result)
    }

    fun replaceDevice(
        project: Project,
        oldDeviceId: String,
        newTechnicalName: String,
        newCategory: DeviceCategory,
        i18n: Messages = Messages()): Pair<Project, TrashItem?> {
        val (projAfterTrash, trashItem) = deleteDeviceToTrash(project, oldDeviceId, i18n = i18n)
        if (trashItem == null) return Pair(project, null)

        val oldDevice = jsonSerializer.decodeFromString(Device.serializer(), trashItem.serializedJson)
        val targetBU = project.businessUnits.find { bu -> bu.devices.any { it.id == oldDeviceId } }
            ?: project.businessUnits.firstOrNull() ?: return Pair(projAfterTrash, trashItem)

        val newDevice = Device(
            id = UUID.randomUUID().toString(),
            technicalName = newTechnicalName,
            category = newCategory,
            siteId = oldDevice.siteId,
            areaId = oldDevice.areaId,
            rackId = oldDevice.rackId,
            mountingType = oldDevice.mountingType,
            positionU = oldDevice.positionU,
            heightU = oldDevice.heightU
        )

        val finalProj = addDevice(projAfterTrash, targetBU.id, newDevice)
        return Pair(finalProj, trashItem)
    }

    fun mergeDevices(
        project: Project,
        survivingDeviceId: String,
        duplicateDeviceId: String,
        choices: MergeDataChoices,
        i18n: Messages = Messages()): Pair<Project, TrashItem?> {
        if (survivingDeviceId == duplicateDeviceId) return Pair(project, null)

        var survivingDev: Device? = null
        var duplicateDev: Device? = null

        for (bu in project.businessUnits) {
            for (dev in bu.devices) {
                if (dev.id == survivingDeviceId) survivingDev = dev
                if (dev.id == duplicateDeviceId) duplicateDev = dev
            }
        }

        if (survivingDev == null || duplicateDev == null) return Pair(project, null)

        val mergedTechnicalName = if (choices.useTechnicalNameFromDuplicate) duplicateDev.technicalName else survivingDev.technicalName
        val mergedPhysicalLabel = if (choices.usePhysicalLabelFromDuplicate) duplicateDev.physicalLabel else survivingDev.physicalLabel
        val mergedAlias = if (choices.useAliasFromDuplicate) duplicateDev.alias else survivingDev.alias
        val mergedIp = if (choices.useIpFromDuplicate) duplicateDev.ipAddress else survivingDev.ipAddress
        val mergedMac = if (choices.useMacFromDuplicate) duplicateDev.macAddress else survivingDev.macAddress
        val mergedSiteId = if (choices.useLocationFromDuplicate) duplicateDev.siteId else survivingDev.siteId
        val mergedAreaId = if (choices.useLocationFromDuplicate) duplicateDev.areaId else survivingDev.areaId

        val mergedPorts = survivingDev.ports.toMutableList()
        if (choices.mergePorts) {
            for (dupPort in duplicateDev.ports) {
                mergedPorts.add(dupPort.copy(id = UUID.randomUUID().toString(), deviceId = survivingDeviceId))
            }
        }

        val updatedSurviving = survivingDev.copy(
            technicalName = mergedTechnicalName,
            physicalLabel = mergedPhysicalLabel,
            alias = mergedAlias,
            ipAddress = mergedIp,
            macAddress = mergedMac,
            siteId = mergedSiteId,
            areaId = mergedAreaId,
            ports = mergedPorts
        )

        val projWithUpdated = updateDevice(project, updatedSurviving, i18n = i18n)
        return deleteDeviceToTrash(projWithUpdated, duplicateDeviceId, i18n = i18n)
    }

    fun addPortToDevice(project: Project, deviceId: String, portName: String, label: String? = null): Project {
        val newPort = Port(
            id = UUID.randomUUID().toString(),
            deviceId = deviceId,
            name = portName,
            label = label
        )

        val updatedBus = project.businessUnits.map { bu ->
            val updatedDevs = bu.devices.map { dev ->
                if (dev.id == deviceId) {
                    dev.copy(ports = dev.ports + newPort)
                } else {
                    dev
                }
            }
            bu.copy(devices = updatedDevs)
        }

        return project.copy(
            businessUnits = updatedBus,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deletePortFromDevice(project: Project, deviceId: String, portId: String): Project {
        val updatedBus = project.businessUnits.map { bu ->
            val updatedDevs = bu.devices.map { dev ->
                if (dev.id == deviceId) {
                    dev.copy(ports = dev.ports.filterNot { it.id == portId }.map { if (it.connectedPortId == portId) it.copy(connectedPortId = null, endpointStatus = EndpointStatus.DETACHED_TO_VERIFY) else it })
                } else {
                    dev.copy(ports = dev.ports.map { if (it.connectedPortId == portId) it.copy(connectedPortId = null, endpointStatus = EndpointStatus.DETACHED_TO_VERIFY) else it })
                }
            }
            bu.copy(devices = updatedDevs)
        }

        val updatedCables = project.cables.map { cable ->
            if (cable.portAId == portId || cable.portBId == portId) {
                cable.copy(
                    deviceAId = if (cable.portAId == portId) deviceId else cable.deviceAId,
                    deviceBId = if (cable.portBId == portId) deviceId else cable.deviceBId,
                    portAId = if (cable.portAId == portId) null else cable.portAId,
                    portBId = if (cable.portBId == portId) null else cable.portBId,
                    observation = (cable.observation ?: Observation("Port removal", System.currentTimeMillis())).copy(status = ObservationStatus.TO_VERIFY)
                )
            } else {
                cable
            }
        }

        return project.copy(
            businessUnits = updatedBus,
            cables = updatedCables,
            panelMappings = project.panelMappings.mapNotNull { m ->
                when {
                    m.portAId == portId -> m.portBId?.takeUnless { it == portId }?.let { m.copy(portAId = it, portBId = null, isUnknownPassage = true) }
                    m.portBId == portId -> m.copy(portBId = null, isUnknownPassage = true)
                    else -> m
                }
            },
            portVlanMemberships = project.portVlanMemberships.filterNot { it.portId == portId },
            poeMappings = project.poeMappings.filterNot { it.portId == portId },
            lagGroups = project.lagGroups.map { it.copy(memberPortIds = it.memberPortIds - portId) },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    // --- RACKS ---

    fun addRack(project: Project, rack: Rack): Project {
        return project.copy(
            racks = project.racks + rack,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateRack(project: Project, updatedRack: Rack): Project {
        val updated = project.racks.map { if (it.id == updatedRack.id) updatedRack else it }
        return ObjectHierarchy.synchronize(project.copy(
            racks = updated,
            updatedEpochMs = System.currentTimeMillis()
        ))
    }

    fun deleteRackToTrash(project: Project, rackId: String, i18n: Messages = Messages()): Pair<Project, TrashItem?> {
        val rack = project.racks.find { it.id == rackId } ?: return Pair(project, null)
        val jsonStr = jsonSerializer.encodeToString(Rack.serializer(), rack)

        val devicesInRack = project.businessUnits.flatMap { it.devices }.filter { it.rackId == rackId }
        val updatedBus = project.businessUnits.map { bu ->
            val updatedDevs = bu.devices.map { dev ->
                if (dev.rackId == rackId) {
                    dev.copy(rackId = null, positionU = null, areaId = dev.areaId ?: rack.areaId)
                } else {
                    dev
                }
            }
            bu.copy(devices = updatedDevs)
        }

        val trashItem = TrashItem(
            projectId = project.id,
            itemType = "RACK",
            itemId = rackId,
            displayName = rack.name,
            serializedJson = jsonStr,
            affectedReferencesSummary = i18n.text("text.62a5d83cfe5e", devicesInRack.size)
        )

        val updatedProject = project.copy(
            businessUnits = updatedBus,
            racks = project.racks.filterNot { it.id == rackId },
            floorplanPlacements = project.floorplanPlacements.filterNot { it.targetType == PlacementTargetType.RACK && it.targetId == rackId },
            updatedEpochMs = System.currentTimeMillis()
        )

        val ref = ObjectRef(PlacementTargetType.RACK, rackId)
        return Pair(ObjectHierarchy.afterDeletion(project, updatedProject, ref), ObjectHierarchy.snapshot(project, ref, trashItem))
    }

    // --- DEVICE MODELS ---

    fun addDeviceModel(project: Project, model: DeviceModel): Project {
        return project.copy(
            deviceModels = project.deviceModels + model,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateDeviceModel(project: Project, updatedModel: DeviceModel): Project {
        val updated = project.deviceModels.map { if (it.id == updatedModel.id) updatedModel else it }
        return project.copy(
            deviceModels = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteDeviceModel(project: Project, modelId: String): Project {
        return project.copy(
            deviceModels = project.deviceModels.filterNot { it.id == modelId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun generatePortsFromTemplates(model: DeviceModel, deviceId: String, i18n: Messages = Messages()): List<Port> {
        return com.onlyfield.assetmanager.core.forms.HardwareConfigurator.ports(model.portTemplates, deviceId)
    }

    fun applyModelToDevice(project: Project, deviceId: String, modelId: String, i18n: Messages = Messages()): Project {
        val model = project.deviceModels.find { it.id == modelId } ?: return project
        require(model.kind == ObjectKind.DEVICE)
        val device = project.businessUnits.flatMap { it.devices }.find { it.id == deviceId } ?: return project
        return com.onlyfield.assetmanager.core.forms.HardwareConfigurator.configure(project, device.copy(
            deviceModelId = model.id, category = model.category, heightU = model.defaultHeightU,
            hardware = model.hardware.copy(portGroups = model.portTemplates)))
    }

    fun addAttachment(project: Project, attachment: Attachment): Project {
        return project.copy(
            attachments = project.attachments + attachment,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteAttachment(project: Project, attachmentId: String): Project {
        // Clear references from areas
        val updatedBus = project.businessUnits.map { bu ->
            val updatedSites = bu.sites.map { site ->
                val updatedAreas = site.areas.map { area ->
                    if (area.floorplanAttachmentId == attachmentId) area.copy(floorplanAttachmentId = null, floorplanPageIndex = 0) else area
                }
                site.copy(areas = updatedAreas)
            }
            val updatedAreas = bu.areas.map { area ->
                if (area.floorplanAttachmentId == attachmentId) area.copy(floorplanAttachmentId = null, floorplanPageIndex = 0) else area
            }
            bu.copy(sites = updatedSites, areas = updatedAreas)
        }

        return project.copy(
            businessUnits = updatedBus,
            attachments = project.attachments.filterNot { it.id == attachmentId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun setAreaFloorplan(project: Project, areaId: String, attachmentId: String?, pageIndex: Int = 0, pageCount: Int? = null): Project {
        require(pageIndex >= 0)
        require(pageCount == null || attachmentId == null || pageIndex < pageCount)
        require(project.businessUnits.any { ObjectMap.areas(it).any { a -> a.id == areaId } })
        if (attachmentId != null) {
            val attachment = project.attachments.first { it.id == attachmentId }
            require(attachment.fileType == AttachmentType.IMAGE || attachment.fileType == AttachmentType.PDF)
            // Legacy imports may have pageCount=1; the picker refreshes it from the actual file.
            require(attachment.fileType != AttachmentType.IMAGE || pageIndex == 0)
        }
        val updatedBus = project.businessUnits.map { bu ->
            val updatedSites = bu.sites.map { site ->
                val updatedAreas = site.areas.map { area ->
                    if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = pageIndex) else area
                }
                site.copy(areas = updatedAreas)
            }
            val updatedAreas = bu.areas.map { area ->
                if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = pageIndex) else area
            }
            bu.copy(sites = updatedSites, areas = updatedAreas)
        }

        return project.copy(
            businessUnits = updatedBus,
            attachments = project.attachments.map { if (it.id == attachmentId && pageCount != null) it.copy(pageCount = pageCount) else it },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addFloorplanPlacement(project: Project, placement: FloorplanPlacement): Project {
        return project.copy(
            floorplanPlacements = project.floorplanPlacements + placement,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateFloorplanPlacement(project: Project, placement: FloorplanPlacement): Project {
        return project.copy(
            floorplanPlacements = project.floorplanPlacements.map { if (it.id == placement.id) placement else it },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteFloorplanPlacement(project: Project, placementId: String): Project {
        return project.copy(
            floorplanPlacements = project.floorplanPlacements.filterNot { it.id == placementId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addAnnotation(project: Project, annotation: com.onlyfield.assetmanager.core.model.Annotation): Project {
        return project.copy(
            annotations = project.annotations + annotation,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteAnnotation(project: Project, annotationId: String): Project {
        return project.copy(
            annotations = project.annotations.filterNot { it.id == annotationId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    // --- TRASH RESTORE ---

    fun restoreFromTrash(project: Project, trashItem: TrashItem, i18n: Messages = Messages()): Project {
        val restored = when (trashItem.itemType.uppercase()) {
            "DEVICE" -> {
                val device = jsonSerializer.decodeFromString(Device.serializer(), trashItem.serializedJson)
                val targetBU = project.businessUnits.find { it.id == trashItem.originalBusinessUnitId } ?: project.businessUnits.firstOrNull() ?: return project
                addDevice(project, targetBU.id, device)
            }
            "RACK" -> {
                val rack = jsonSerializer.decodeFromString(Rack.serializer(), trashItem.serializedJson)
                addRack(project, rack)
            }
            else -> project
        }
        return ObjectHierarchy.restore(restored, trashItem, i18n = i18n)
    }

    // --- CABLING & PHYSICAL PATHS (W03) ---

    fun addCable(project: Project, cable: Cable): Project {
        return project.copy(
            cables = project.cables + cable,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateCable(project: Project, updatedCable: Cable): Project {
        val updated = project.cables.map { if (it.id == updatedCable.id) updatedCable else it }
        return project.copy(
            cables = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteCable(project: Project, cableId: String): Project {
        return project.copy(
            cables = project.cables.filterNot { it.id == cableId },
            cableRoutes = project.cableRoutes.filterNot { it.cableId == cableId },
            attachments = project.attachments.filterNot { it.targetType == AttachmentTargetType.CABLE && it.targetId == cableId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addSharedPathSegment(project: Project, segment: SharedPathSegment): Project {
        return project.copy(
            sharedPathSegments = project.sharedPathSegments + segment,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateSharedPathSegment(project: Project, updatedSegment: SharedPathSegment): Project {
        val updated = project.sharedPathSegments.map { if (it.id == updatedSegment.id) updatedSegment else it }
        return project.copy(
            sharedPathSegments = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteSharedPathSegment(project: Project, segmentId: String): Project {
        val updatedCables = project.cables.map { cable ->
            if (cable.sharedPathSegmentIds.contains(segmentId)) {
                cable.copy(sharedPathSegmentIds = cable.sharedPathSegmentIds.filterNot { it == segmentId })
            } else {
                cable
            }
        }
        return project.copy(
            sharedPathSegments = project.sharedPathSegments.filterNot { it.id == segmentId },
            cables = updatedCables,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addPanelMapping(project: Project, mapping: PanelMapping): Project {
        return project.copy(
            panelMappings = project.panelMappings + mapping,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updatePanelMapping(project: Project, updatedMapping: PanelMapping): Project {
        val updated = project.panelMappings.map { if (it.id == updatedMapping.id) updatedMapping else it }
        return project.copy(
            panelMappings = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deletePanelMapping(project: Project, mappingId: String): Project {
        return project.copy(
            panelMappings = project.panelMappings.filterNot { it.id == mappingId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    // --- LOGICAL NETWORK (W03) ---

    fun addVlan(project: Project, vlan: Vlan): Project {
        return project.copy(
            vlans = project.vlans + vlan,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateVlan(project: Project, updatedVlan: Vlan): Project {
        val updated = project.vlans.map { if (it.id == updatedVlan.id) updatedVlan else it }
        return project.copy(
            vlans = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteVlan(project: Project, vlanId: String): Project {
        return project.copy(
            vlans = project.vlans.filterNot { it.id == vlanId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addSubnet(project: Project, subnet: Subnet): Project {
        return project.copy(
            subnets = project.subnets + subnet,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateSubnet(project: Project, updatedSubnet: Subnet): Project {
        val updated = project.subnets.map { if (it.id == updatedSubnet.id) updatedSubnet else it }
        return project.copy(
            subnets = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteSubnet(project: Project, subnetId: String): Project {
        return project.copy(
            subnets = project.subnets.filterNot { it.id == subnetId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addOrUpdatePortVlanMembership(project: Project, membership: PortVlanMembership): Project {
        val existing = project.portVlanMemberships.find { it.id == membership.id || it.portId == membership.portId }
        val updatedList = if (existing != null) {
            project.portVlanMemberships.map { if (it.id == existing.id) membership else it }
        } else {
            project.portVlanMemberships + membership
        }
        return project.copy(
            portVlanMemberships = updatedList,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deletePortVlanMembership(project: Project, membershipId: String): Project {
        return project.copy(
            portVlanMemberships = project.portVlanMemberships.filterNot { it.id == membershipId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addLogicalInterface(project: Project, logicalInterface: LogicalInterface): Project {
        return project.copy(
            logicalInterfaces = project.logicalInterfaces + logicalInterface,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateLogicalInterface(project: Project, updatedInterface: LogicalInterface): Project {
        val updated = project.logicalInterfaces.map { if (it.id == updatedInterface.id) updatedInterface else it }
        return project.copy(
            logicalInterfaces = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteLogicalInterface(project: Project, interfaceId: String): Project {
        return project.copy(
            logicalInterfaces = project.logicalInterfaces.filterNot { it.id == interfaceId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addLagGroup(project: Project, lagGroup: LagGroup): Project {
        return project.copy(
            lagGroups = project.lagGroups + lagGroup,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateLagGroup(project: Project, updatedLagGroup: LagGroup): Project {
        val updated = project.lagGroups.map { if (it.id == updatedLagGroup.id) updatedLagGroup else it }
        return project.copy(
            lagGroups = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteLagGroup(project: Project, lagGroupId: String): Project {
        return project.copy(
            lagGroups = project.lagGroups.filterNot { it.id == lagGroupId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addWanVpnConnection(project: Project, connection: WanVpnConnection): Project {
        return project.copy(
            wanVpnConnections = project.wanVpnConnections + connection,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateWanVpnConnection(project: Project, updatedConnection: WanVpnConnection): Project {
        val updated = project.wanVpnConnections.map { if (it.id == updatedConnection.id) updatedConnection else it }
        return project.copy(
            wanVpnConnections = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteWanVpnConnection(project: Project, connectionId: String): Project {
        return project.copy(
            wanVpnConnections = project.wanVpnConnections.filterNot { it.id == connectionId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addVideoSurveillanceMapping(project: Project, mapping: VideoSurveillanceMapping): Project {
        return project.copy(
            videoSurveillanceMappings = project.videoSurveillanceMappings + mapping,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateVideoSurveillanceMapping(project: Project, updatedMapping: VideoSurveillanceMapping): Project {
        val updated = project.videoSurveillanceMappings.map { if (it.id == updatedMapping.id) updatedMapping else it }
        return project.copy(
            videoSurveillanceMappings = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteVideoSurveillanceMapping(project: Project, mappingId: String): Project {
        return project.copy(
            videoSurveillanceMappings = project.videoSurveillanceMappings.filterNot { it.id == mappingId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addDeviceConfiguration(project: Project, config: DeviceConfiguration): Project {
        return project.copy(
            deviceConfigurations = project.deviceConfigurations + config,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateDeviceConfiguration(project: Project, updatedConfig: DeviceConfiguration): Project {
        val updated = project.deviceConfigurations.map { if (it.id == updatedConfig.id) updatedConfig else it }
        return project.copy(
            deviceConfigurations = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteDeviceConfiguration(project: Project, configId: String): Project {
        return project.copy(
            deviceConfigurations = project.deviceConfigurations.filterNot { it.id == configId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addCustomExtraField(project: Project, field: CustomExtraField): Project {
        return project.copy(
            customExtraFields = project.customExtraFields + field,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateCustomExtraField(project: Project, updatedField: CustomExtraField): Project {
        val updated = project.customExtraFields.map { if (it.id == updatedField.id) updatedField else it }
        return project.copy(
            customExtraFields = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteCustomExtraField(project: Project, fieldId: String): Project {
        return project.copy(
            customExtraFields = project.customExtraFields.filterNot { it.id == fieldId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    // --- POWER & BADGES (W03) ---

    fun addPowerFeed(project: Project, feed: PowerFeed): Project {
        return project.copy(
            powerFeeds = project.powerFeeds + feed,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updatePowerFeed(project: Project, updatedFeed: PowerFeed): Project {
        val updated = project.powerFeeds.map { if (it.id == updatedFeed.id) updatedFeed else it }
        return project.copy(
            powerFeeds = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deletePowerFeed(project: Project, feedId: String): Project {
        return project.copy(
            powerFeeds = project.powerFeeds.filterNot { it.id == feedId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addOrUpdatePoeMapping(project: Project, poe: PoeMapping): Project {
        val existing = project.poeMappings.find { it.id == poe.id || it.portId == poe.portId }
        val updatedList = if (existing != null) {
            project.poeMappings.map { if (it.id == existing.id) poe else it }
        } else {
            project.poeMappings + poe
        }
        return project.copy(
            poeMappings = updatedList,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deletePoeMapping(project: Project, poeId: String): Project {
        return project.copy(
            poeMappings = project.poeMappings.filterNot { it.id == poeId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun addDocumentBadge(project: Project, badge: DocumentBadge): Project {
        return project.copy(
            documentBadges = project.documentBadges + badge,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateDocumentBadge(project: Project, updatedBadge: DocumentBadge): Project {
        val updated = project.documentBadges.map { if (it.id == updatedBadge.id) updatedBadge else it }
        return project.copy(
            documentBadges = updated,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deleteDocumentBadge(project: Project, badgeId: String): Project {
        return project.copy(
            documentBadges = project.documentBadges.filterNot { it.id == badgeId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }
}

