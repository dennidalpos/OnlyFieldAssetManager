package com.onlyfield.assetmanager.core.edit

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.model.Annotation
import kotlinx.serialization.json.Json
import java.util.UUID

/** Immutable project edits shared by both apps. */
object ProjectEdits {

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }


    fun addSite(project: Project, name: String, group: String? = null, address: String? = null): Project = project.copy(
        sites = project.sites + Site(name = name, group = group, address = address),
        updatedEpochMs = System.currentTimeMillis()
    )

    /** Replaces name, group and address of an existing site. */
    fun updateSite(project: Project, site: Site): Project = project.copy(
        sites = project.sites.map { if (it.id == site.id) it.copy(name = site.name, group = site.group, address = site.address) else it },
        updatedEpochMs = System.currentTimeMillis()
    )

    private fun hasNetworkScope(project: Project, type: VlanScopeType, targetId: String): Boolean =
        project.vlans.any { it.scopeType == type && it.scopeTargetId == targetId } ||
            project.subnets.any { it.scopeType == type && it.scopeTargetId == targetId }

    /** Deletes an empty, unreferenced site or returns null. */
    fun deleteSite(project: Project, siteId: String): Project? {
        val site = project.sites.find { it.id == siteId } ?: return project
        if (site.devices.isNotEmpty() || site.areas.isNotEmpty() || hasNetworkScope(project, VlanScopeType.SITE, siteId)) return null
        return project.copy(sites = project.sites - site, updatedEpochMs = System.currentTimeMillis())
    }

    fun addArea(project: Project, siteId: String, area: Area): Project = project.copy(
        sites = project.sites.map { if (it.id == siteId) it.copy(areas = it.areas + area) else it },
        updatedEpochMs = System.currentTimeMillis()
    )

    fun updateArea(project: Project, area: Area): Project = project.copy(
        sites = project.sites.map { site ->
            site.copy(areas = site.areas.map { if (it.id == area.id) area else it })
        },
        updatedEpochMs = System.currentTimeMillis()
    )

    /** Deletes an unreferenced area or returns null. */
    fun deleteArea(project: Project, areaId: String): Project? {
        val inUse = project.sites.any { site -> site.devices.any { it.areaId == areaId } } ||
            project.racks.any { it.areaId == areaId } ||
            project.floorplanPlacements.any { it.areaId == areaId } || project.cableRoutes.any { it.areaId == areaId } ||
            project.annotations.any { it.areaId == areaId }
        if (inUse) return null
        return project.copy(
            sites = project.sites.map { site ->
                site.copy(areas = site.areas.filterNot { it.id == areaId })
            },
            updatedEpochMs = System.currentTimeMillis()
        )
    }


    fun addDevice(project: Project, siteId: String, device: Device): Project {
        val updatedSites = project.sites.map { site ->
            if (site.id == siteId) {
                site.copy(devices = site.devices + device)
            } else {
                site
            }
        }
        return project.copy(
            sites = updatedSites,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun updateDevice(project: Project, updatedDevice: Device, i18n: Messages = Messages()): Project {
        val previous = project.sites.flatMap { it.devices }.find { it.id == updatedDevice.id }
        val updatedSites = project.sites.map { site ->
            val hasDev = site.devices.any { it.id == updatedDevice.id }
            if (hasDev) {
                val newDevs = site.devices.map { if (it.id == updatedDevice.id) updatedDevice else it }
                site.copy(devices = newDevs)
            } else {
                site
            }
        }
        val result = project.copy(
            sites = updatedSites,
            updatedEpochMs = System.currentTimeMillis()
        )
        return if (previous?.rackId != updatedDevice.rackId) ObjectHierarchy.assign(result, ObjectRef(PlacementTargetType.DEVICE, updatedDevice.id), updatedDevice.rackId?.let { ObjectRef(PlacementTargetType.RACK, it) }, i18n = i18n) else ObjectHierarchy.synchronize(result)
    }

    fun deleteDeviceToTrash(project: Project, deviceId: String, i18n: Messages = Messages()): Pair<Project, TrashItem?> {
        val deviceSite = project.sites.find { site -> site.devices.any { it.id == deviceId } }
            ?: return Pair(project, null)
        val device = deviceSite.devices.find { it.id == deviceId } ?: return Pair(project, null)
        check(!hasNetworkScope(project, VlanScopeType.DEVICE, deviceId)) { i18n.text("network.scopeInUse") }

        val retained = DeviceTrashData.capture(project, deviceId)
        val jsonStr = retained.snapshot(jsonSerializer, device)
        val affectedPortIds = device.ports.map { it.id }.toSet()

        // Disconnect deleted-device ports.
        var updatedCablesCount = 0
        val updatedCables = project.cables.map { cable ->
            if (affectedPortIds.contains(cable.portAId) || affectedPortIds.contains(cable.portBId) || (cable.deviceAId == deviceId) || (cable.deviceBId == deviceId)) {
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

        val updatedSites = project.sites.map { site ->
            if (site.id == deviceSite.id) {
                site.copy(devices = site.devices.filterNot { it.id == deviceId })
            } else {
                site
            }
        }

        val updatedProject = project.copy(
            sites = updatedSites,
            cables = updatedCables,
            cableRoutes = project.sites.flatMap { it.areas }.flatMap { ObjectMap.routes(project, it.id) }.map { route ->
                route.copy(points = ObjectMap.routePoints(project, route, ObjectMap.nodes(project, route.areaId)))
            },
            floorplanPlacements = project.floorplanPlacements.filterNot { (it.targetType == PlacementTargetType.DEVICE) && (it.targetId == deviceId) },
            updatedEpochMs = System.currentTimeMillis()
        ).let { dropPortReferences(retained.removeFrom(it), affectedPortIds) }

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
        val updatedSites = project.sites.map { site ->
            val updatedDevs = site.devices.map { dev ->
                if (deviceIdSet.contains(dev.id)) {
                    var updated = dev
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
            site.copy(devices = updatedDevs)
        }

        var result = project.copy(
            sites = updatedSites,
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
        val targetSite = project.sites.find { site -> site.devices.any { it.id == oldDeviceId } }
            ?: project.sites.firstOrNull() ?: return Pair(projAfterTrash, trashItem)

        val newDevice = Device(
            id = UUID.randomUUID().toString(),
            technicalName = newTechnicalName,
            category = newCategory,
            areaId = oldDevice.areaId,
            rackId = oldDevice.rackId,
            mountingType = oldDevice.mountingType,
            positionU = oldDevice.positionU,
            heightU = oldDevice.heightU
        )

        val finalProj = addDevice(projAfterTrash, targetSite.id, newDevice)
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

        for (site in project.sites) {
            for (dev in site.devices) {
                if (dev.id == survivingDeviceId) survivingDev = dev
                if (dev.id == duplicateDeviceId) duplicateDev = dev
            }
        }

        if (survivingDev == null || duplicateDev == null) return Pair(project, null)
        check(!hasNetworkScope(project, VlanScopeType.DEVICE, survivingDeviceId) &&
            !hasNetworkScope(project, VlanScopeType.DEVICE, duplicateDeviceId)) { i18n.text("network.scopeInUse") }

        val mergedTechnicalName = if (choices.useTechnicalNameFromDuplicate) duplicateDev.technicalName else survivingDev.technicalName
        val mergedPhysicalLabel = if (choices.usePhysicalLabelFromDuplicate) duplicateDev.physicalLabel else survivingDev.physicalLabel
        val mergedAlias = if (choices.useAliasFromDuplicate) duplicateDev.alias else survivingDev.alias
        val mergedIp = if (choices.useIpFromDuplicate) duplicateDev.ipAddress else survivingDev.ipAddress
        val mergedMac = if (choices.useMacFromDuplicate) duplicateDev.macAddress else survivingDev.macAddress
        val mergedAreaId = if (choices.useLocationFromDuplicate) duplicateDev.areaId else survivingDev.areaId

        val mergedPorts = survivingDev.ports.toMutableList()
        val copiedPortIds = mutableMapOf<String, String>()
        if (choices.mergePorts) {
            for (dupPort in duplicateDev.ports) {
                val newId = UUID.randomUUID().toString()
                copiedPortIds[dupPort.id] = newId
                mergedPorts.add(dupPort.copy(id = newId, deviceId = survivingDeviceId))
            }
        }

        val updatedSurviving = survivingDev.copy(
            technicalName = mergedTechnicalName,
            physicalLabel = mergedPhysicalLabel,
            alias = mergedAlias,
            ipAddress = mergedIp,
            macAddress = mergedMac,
            areaId = mergedAreaId,
            ports = mergedPorts
        )

        val configAttachmentIds = if (choices.mergeConfigurations) project.deviceConfigurations
            .filter { it.deviceId == duplicateDeviceId }.mapNotNull { it.attachmentId }.toSet() else emptySet()
        val duplicatePortIds = duplicateDev.ports.map { it.id }.toSet()
        val transferred = project.copy(
            attachments = project.attachments.map {
                when {
                    it.id !in configAttachmentIds -> it
                    it.targetType == AttachmentTargetType.DEVICE && it.targetId == duplicateDeviceId -> it.copy(targetId = survivingDeviceId)
                    it.targetType == AttachmentTargetType.PORT && it.targetId in duplicatePortIds -> {
                        val copiedId = copiedPortIds[it.targetId]
                        it.copy(targetType = if (copiedId == null) AttachmentTargetType.DEVICE else AttachmentTargetType.PORT,
                            targetId = copiedId ?: survivingDeviceId)
                    }
                    else -> it
                }
            },
            credentials = if (choices.mergeCredentials) project.credentials.map { if (it.deviceId == duplicateDeviceId) it.copy(deviceId = survivingDeviceId) else it } else project.credentials,
            deviceConfigurations = if (choices.mergeConfigurations) project.deviceConfigurations.map { if (it.deviceId == duplicateDeviceId) it.copy(deviceId = survivingDeviceId) else it } else project.deviceConfigurations,
            powerFeeds = if (choices.mergePowerFeeds) project.powerFeeds.map { it.copy(
                deviceId = if (it.deviceId == duplicateDeviceId) survivingDeviceId else it.deviceId,
                sourceDeviceId = if (it.sourceDeviceId == duplicateDeviceId) survivingDeviceId else it.sourceDeviceId,
            ) } else project.powerFeeds,
            customExtraFields = if (choices.mergeExtraFields) project.customExtraFields.map {
                if (it.targetType == "DEVICE" && it.targetId == duplicateDeviceId) it.copy(targetId = survivingDeviceId) else it
            } else project.customExtraFields,
        )
        check(!choices.mergePowerFeeds || !transferred.powerFeeds.hasPowerFeedCycle()) { i18n.text("merge.powerCycle") }
        val projWithUpdated = updateDevice(transferred, updatedSurviving, i18n = i18n)
        return deleteDeviceToTrash(projWithUpdated, duplicateDeviceId, i18n = i18n)
    }

    fun addPortToDevice(project: Project, deviceId: String, portName: String, label: String? = null): Project {
        val newPort = Port(
            id = UUID.randomUUID().toString(),
            deviceId = deviceId,
            name = portName,
            label = label
        )

        val updatedSites = project.sites.map { site ->
            val updatedDevs = site.devices.map { dev ->
                if (dev.id == deviceId) {
                    dev.copy(ports = dev.ports + newPort)
                } else {
                    dev
                }
            }
            site.copy(devices = updatedDevs)
        }

        return project.copy(
            sites = updatedSites,
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    fun deletePortFromDevice(project: Project, deviceId: String, portId: String): Project {
        val updatedSites = project.sites.map { site ->
            val updatedDevs = site.devices.map { dev ->
                if (dev.id == deviceId) dev.copy(ports = dev.ports.filterNot { it.id == portId }) else dev
            }
            site.copy(devices = updatedDevs)
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

        return dropPortReferences(project.copy(sites = updatedSites, cables = updatedCables), setOf(portId))
    }

    /** Removes passages, VLAN, PoE and LAG rows of deleted ports; a passage that loses one end becomes unknown. */
    private fun dropPortReferences(project: Project, portIds: Set<String>): Project = project.copy(
        panelMappings = project.panelMappings.mapNotNull { m ->
            val b = m.portBId?.takeUnless { it in portIds }
            when {
                m.portAId in portIds -> b?.let { m.copy(portAId = it, portBId = null, isUnknownPassage = true) }
                m.portBId != null && b == null -> m.copy(portBId = null, isUnknownPassage = true)
                else -> m
            }
        },
        portVlanMemberships = project.portVlanMemberships.filterNot { it.portId in portIds },
        poeMappings = project.poeMappings.filterNot { it.portId in portIds },
        lagGroups = project.lagGroups.map { it.copy(memberPortIds = it.memberPortIds - portIds) },
        updatedEpochMs = System.currentTimeMillis()
    )

    /** Adds the front↔rear passage of each paired port (same passageKey) that has none yet. */
    fun withInternalPassages(project: Project, ports: List<Port>): Project {
        val mappings = project.panelMappings.toMutableList()
        ports.filter { it.hardware.passageKey != null }.groupBy { it.hardware.passageKey }.values.forEach { pair ->
            val a = pair.singleOrNull { it.hardware.side == PortSide.FRONT } ?: return@forEach
            val b = pair.singleOrNull { it.hardware.side == PortSide.REAR } ?: return@forEach
            if (mappings.none { it.portAId in setOf(a.id, b.id) || it.portBId in setOf(a.id, b.id) }) mappings.add(PanelMapping(portAId = a.id, portBId = b.id))
        }
        return project.copy(panelMappings = mappings)
    }


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

        val devicesInRack = project.sites.flatMap { it.devices }.filter { it.rackId == rackId }
        val updatedSites = project.sites.map { site ->
            val updatedDevs = site.devices.map { dev ->
                if (dev.rackId == rackId) {
                    dev.copy(rackId = null, positionU = null, areaId = dev.areaId ?: rack.areaId)
                } else {
                    dev
                }
            }
            site.copy(devices = updatedDevs)
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
            sites = updatedSites,
            racks = project.racks.filterNot { it.id == rackId },
            floorplanPlacements = project.floorplanPlacements.filterNot { it.targetType == PlacementTargetType.RACK && it.targetId == rackId },
            updatedEpochMs = System.currentTimeMillis()
        )

        val ref = ObjectRef(PlacementTargetType.RACK, rackId)
        return Pair(ObjectHierarchy.afterDeletion(project, updatedProject, ref), ObjectHierarchy.snapshot(project, ref, trashItem))
    }


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
        val device = project.sites.flatMap { it.devices }.find { it.id == deviceId } ?: return project
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
        val updatedSites = project.sites.map { site ->
            val updatedAreas = site.areas.map { area ->
                if (area.floorplanAttachmentId == attachmentId) area.copy(floorplanAttachmentId = null, floorplanPageIndex = 0) else area
            }
            site.copy(areas = updatedAreas)
        }

        return project.copy(
            sites = updatedSites,
            attachments = project.attachments.filterNot { it.id == attachmentId },
            updatedEpochMs = System.currentTimeMillis()
        )
    }

    /** Permanent trash deletion also drops photos of the item and its ports. */
    fun purgeTrashAttachments(project: Project, removed: List<TrashItem>): Project {
        val targets = trashTargets(project, removed)
        return project.attachments.filter { it.targetType to it.targetId in targets || removed.any { item ->
            item.itemType.equals("ATTACHMENT", ignoreCase = true) && item.itemId == it.id
        } }.fold(project) { result, att -> deleteAttachment(result, att.id) }
    }

    fun retainTrashAttachments(incoming: Project, local: Project, trash: List<TrashItem>): Project {
        val targets = trashTargets(incoming, trash, local.attachments)
        val retained = local.attachments.filter { it.targetType to it.targetId in targets }
        return incoming.copy(attachments = (incoming.attachments + retained).distinctBy { it.id })
    }

    fun trashAttachments(items: List<TrashItem>): List<Attachment> = items
        .filter { it.itemType.equals("ATTACHMENT", ignoreCase = true) }
        .map { jsonSerializer.decodeFromString(Attachment.serializer(), it.serializedJson) }

    private fun trashTargets(project: Project, items: List<TrashItem>, attachments: List<Attachment> = project.attachments): Set<Pair<AttachmentTargetType, String?>> {
        val index = com.onlyfield.assetmanager.core.display.ProjectIndex(project)
        return items.flatMap { item ->
            val direct = AttachmentTargetType.entries.firstOrNull { it.name == item.itemType.uppercase() }
                ?.let { listOf(it to item.itemId) }.orEmpty()
            val ports = if (item.itemType.equals("DEVICE", ignoreCase = true) && index.device(item.itemId) == null &&
                attachments.any { it.targetType == AttachmentTargetType.PORT })
                jsonSerializer.decodeFromString(Device.serializer(), item.serializedJson).ports.map { AttachmentTargetType.PORT to it.id }
            else emptyList()
            direct + ports
        }.filterNot { (type, id) -> when (type) {
            AttachmentTargetType.DEVICE -> index.device(id) != null
            // Reused port IDs do not release photos of a device still in trash.
            AttachmentTargetType.PORT -> false
            AttachmentTargetType.RACK -> index.rack(id) != null
            AttachmentTargetType.AREA -> index.area(id) != null
            AttachmentTargetType.CABLE -> project.cables.any { it.id == id }
            AttachmentTargetType.PROJECT -> project.id == id
        } }.toSet()
    }

    fun setAreaFloorplan(project: Project, areaId: String, attachmentId: String?, pageIndex: Int = 0, pageCount: Int? = null): Project {
        require(pageIndex >= 0)
        require(pageCount == null || attachmentId == null || pageIndex < pageCount)
        require(project.sites.any { it.areas.any { a -> a.id == areaId } })
        if (attachmentId != null) {
            val attachment = project.attachments.first { it.id == attachmentId }
            require(attachment.fileType == AttachmentType.IMAGE || attachment.fileType == AttachmentType.PDF)
            // The picker refreshes legacy page counts from the file.
            require(attachment.fileType != AttachmentType.IMAGE || pageIndex == 0)
        }
        val updatedSites = project.sites.map { site ->
            val updatedAreas = site.areas.map { area ->
                if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = pageIndex) else area
            }
            site.copy(areas = updatedAreas)
        }

        return project.copy(
            sites = updatedSites,
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

    fun addAnnotation(project: Project, annotation: Annotation): Project {
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


    fun restoreFromTrash(project: Project, trashItem: TrashItem, i18n: Messages = Messages()): Project {
        check(trashItem.projectId == project.id) { i18n.text("trash.invalidEntry") }
        val restored = when (trashItem.itemType.uppercase()) {
            "DEVICE" -> {
                val device = jsonSerializer.decodeFromString(Device.serializer(), trashItem.serializedJson)
                check(device.id == trashItem.itemId) { i18n.text("trash.invalidEntry") }
                check(project.sites.none { site -> site.devices.any { it.id == device.id } }) { i18n.text("trash.alreadyExists") }
                val targetSite = project.sites.find { it.id == trashItem.originalSiteId }
                    ?: throw IllegalStateException(i18n.text("trash.siteMissing"))
                check(device.areaId == null || targetSite.areas.any { it.id == device.areaId }) { i18n.text("trash.contextMissing") }
                check(device.rackId == null || project.racks.any { it.id == device.rackId }) { i18n.text("trash.contextMissing") }
                val portIds = device.ports.map { it.id }
                val activePortIds = project.sites.flatMap { it.devices }.flatMap { it.ports }.map { it.id }.toSet()
                check(portIds.distinct().size == portIds.size && portIds.none { it in activePortIds }) { i18n.text("trash.alreadyExists") }
                DeviceTrashData.decode(jsonSerializer, trashItem.serializedJson).restore(
                    withInternalPassages(addDevice(project, targetSite.id, device), device.ports), device.id, i18n)
            }
            "RACK" -> {
                val rack = jsonSerializer.decodeFromString(Rack.serializer(), trashItem.serializedJson)
                check(rack.id == trashItem.itemId) { i18n.text("trash.invalidEntry") }
                check(project.racks.none { it.id == rack.id }) { i18n.text("trash.alreadyExists") }
                check(trashItem.originalSiteId == null || project.sites.any { it.id == trashItem.originalSiteId }) { i18n.text("trash.siteMissing") }
                check(rack.areaId == null || project.sites.any { site ->
                    (trashItem.originalSiteId == null || site.id == trashItem.originalSiteId) && site.areas.any { it.id == rack.areaId }
                }) { i18n.text("trash.contextMissing") }
                addRack(project, rack)
            }
            "CREDENTIAL" -> {
                val credential = jsonSerializer.decodeFromString(Credential.serializer(), trashItem.serializedJson)
                check(credential.id == trashItem.itemId) { i18n.text("trash.invalidEntry") }
                check(project.credentials.none { it.id == credential.id }) { i18n.text("trash.alreadyExists") }
                return project.copy(credentials = project.credentials + credential, updatedEpochMs = System.currentTimeMillis())
            }
            else -> throw IllegalStateException(i18n.text("trash.unsupportedType"))
        }
        return ObjectHierarchy.restore(restored, trashItem, i18n = i18n)
    }


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

    fun deleteVlan(project: Project, vlanId: String, i18n: Messages = Messages()): Project {
        require(project.subnets.none { it.vlanId == vlanId }) { i18n.text("network.vlanInUse") }
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

