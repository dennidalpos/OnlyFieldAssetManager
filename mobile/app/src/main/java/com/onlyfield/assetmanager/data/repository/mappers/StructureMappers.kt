package com.onlyfield.assetmanager.data.repository.mappers

import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.model.Credential
import com.onlyfield.assetmanager.core.model.CredentialType
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.core.model.DeviceModel
import com.onlyfield.assetmanager.core.model.EndpointStatus
import com.onlyfield.assetmanager.core.model.MountingType
import com.onlyfield.assetmanager.core.model.NumberingDirection
import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.PortTemplate
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.model.RackSide
import com.onlyfield.assetmanager.data.local.AreaEntity
import com.onlyfield.assetmanager.data.local.SiteEntity
import com.onlyfield.assetmanager.data.local.CredentialEntity
import com.onlyfield.assetmanager.data.local.DeviceEntity
import com.onlyfield.assetmanager.data.local.DeviceModelEntity
import com.onlyfield.assetmanager.data.local.PortEntity
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.local.RackEntity
import kotlinx.serialization.json.Json

// Room <-> domain mapping: project tree, devices, racks, models, credentials.

/** JSON for list columns stored as text. */
internal val mapperJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun toProjectEntity(project: Project): ProjectEntity {
    return ProjectEntity(
        id = project.id,
        name = project.name,
        description = project.description,
        createdEpochMs = project.createdEpochMs,
        updatedEpochMs = project.updatedEpochMs,
        isPasswordProtected = project.isPasswordProtected,
        objectTypesJson = mapperJson.encodeToString(project.objectTypes),
        cableRoutesJson = mapperJson.encodeToString(project.cableRoutes),
        objectContainmentsJson = mapperJson.encodeToString(com.onlyfield.assetmanager.core.model.ObjectHierarchy.relations(project)),
    )
}

internal fun toRackEntity(projectId: String, rack: Rack): RackEntity {
    return RackEntity(
        id = rack.id,
        projectId = projectId,
        name = rack.name,
        areaId = rack.areaId,
        heightU = rack.heightU,
        numberingDirection = rack.numberingDirection.name,
        depthMm = rack.depthMm,
        mountingDepthMm = rack.mountingDepthMm,
        deviceModelId = rack.deviceModelId,
        mountingType = rack.mountingType,
        notes = rack.notes,
    )
}

internal fun toRack(entity: RackEntity): Rack {
    return Rack(
        id = entity.id,
        name = entity.name,
        areaId = entity.areaId,
        heightU = entity.heightU,
        numberingDirection = try { NumberingDirection.valueOf(entity.numberingDirection) } catch (_: Exception) { NumberingDirection.BOTTOM_TO_TOP },
        depthMm = entity.depthMm,
        mountingDepthMm = entity.mountingDepthMm,
        deviceModelId = entity.deviceModelId,
        mountingType = entity.mountingType,
        notes = entity.notes,
    )
}

internal fun toDeviceModelEntity(projectId: String, model: DeviceModel): DeviceModelEntity {
    return DeviceModelEntity(
        id = model.id,
        projectId = projectId,
        name = model.name,
        brand = model.brand,
        modelNumber = model.modelNumber,
        category = model.category.name,
        defaultHeightU = model.defaultHeightU,
        portTemplatesJson = mapperJson.encodeToString(model.portTemplates),
        notes = model.notes,
        configurationJson = mapperJson.encodeToString(model),
    )
}

internal fun toDeviceModel(entity: DeviceModelEntity): DeviceModel {
    if (entity.configurationJson != "{}") return mapperJson.decodeFromString<DeviceModel>(entity.configurationJson)
    val templates = mapperJson.decodeFromString<List<PortTemplate>>(entity.portTemplatesJson)
    return DeviceModel(
        id = entity.id,
        name = entity.name,
        brand = entity.brand,
        modelNumber = entity.modelNumber,
        category = try { DeviceCategory.valueOf(entity.category) } catch (_: Exception) { DeviceCategory.CUSTOM },
        defaultHeightU = entity.defaultHeightU,
        portTemplates = templates,
        notes = entity.notes,
    )
}

internal fun toCredentialEntity(projectId: String, cred: Credential): CredentialEntity {
    return CredentialEntity(
        id = cred.id,
        projectId = projectId,
        deviceId = cred.deviceId,
        groupName = cred.groupName,
        username = cred.username,
        secret = cred.secret,
        type = cred.type.name,
        notes = cred.notes,
    )
}

internal fun toCredential(entity: CredentialEntity): Credential {
    return Credential(
        id = entity.id,
        deviceId = entity.deviceId,
        groupName = entity.groupName,
        username = entity.username,
        secret = entity.secret,
        type = try { CredentialType.valueOf(entity.type) } catch (_: Exception) { CredentialType.PASSWORD },
        notes = entity.notes,
    )
}

internal fun toSiteEntity(projectId: String, site: Site): SiteEntity {
    return SiteEntity(
        id = site.id,
        projectId = projectId,
        name = site.name,
        code = site.code,
        groupName = site.group,
        address = site.address
    )
}

internal fun toAreaEntity(siteId: String, area: Area): AreaEntity {
    return AreaEntity(
        id = area.id,
        siteId = siteId,
        name = area.name,
        floor = area.floor,
        description = area.description,
        floorplanAttachmentId = area.floorplanAttachmentId,
        floorplanPageIndex = area.floorplanPageIndex
    )
}

internal fun toDeviceEntity(siteId: String, device: Device): DeviceEntity {
    return DeviceEntity(
        id = device.id,
        siteId = siteId,
        areaId = device.areaId,
        technicalName = device.technicalName,
        physicalLabel = device.physicalLabel,
        alias = device.alias,
        ipAddress = device.ipAddress,
        macAddress = device.macAddress,
        obsSource = device.observation?.source,
        obsTimestampEpochMs = device.observation?.timestampEpochMs,
        obsStatus = device.observation?.status?.name,
        obsNotes = device.observation?.notes,
        rackId = device.rackId,
        positionU = device.positionU,
        heightU = device.heightU,
        rackSide = device.rackSide.name,
        mountingType = device.mountingType.name,
        deviceModelId = device.deviceModelId,
        category = device.category.name,
        objectTypeId = device.objectTypeId,
        hardwareJson = mapperJson.encodeToString(device.hardware),
        serialNumber = device.serialNumber,
        operationalStatus = device.operationalStatus.name
    )
}

internal fun toPortEntity(port: Port): PortEntity {
    return PortEntity(
        id = port.id,
        deviceId = port.deviceId,
        name = port.name,
        label = port.label,
        endpointStatus = port.endpointStatus.name,
        obsSource = port.observation?.source,
        obsTimestampEpochMs = port.observation?.timestampEpochMs,
        obsStatus = port.observation?.status?.name,
        hardwareJson = mapperJson.encodeToString(port.hardware),
        obsNotes = port.observation?.notes
    )
}

internal fun toProject(
    entity: ProjectEntity,
    siteEntities: List<SiteEntity>,
    areaEntities: List<AreaEntity>,
    deviceEntities: List<DeviceEntity>,
    portEntities: List<PortEntity>,
    credentialEntities: List<CredentialEntity> = emptyList(),
    rackEntities: List<RackEntity> = emptyList(),
    deviceModelEntities: List<DeviceModelEntity> = emptyList(),
    attachmentEntities: List<com.onlyfield.assetmanager.data.local.AttachmentEntity> = emptyList(),
    annotationEntities: List<com.onlyfield.assetmanager.data.local.AnnotationEntity> = emptyList(),
    placementEntities: List<com.onlyfield.assetmanager.data.local.FloorplanPlacementEntity> = emptyList(),
    cableEntities: List<com.onlyfield.assetmanager.data.local.CableEntity> = emptyList(),
    panelMappingEntities: List<com.onlyfield.assetmanager.data.local.PanelMappingEntity> = emptyList(),
    vlanEntities: List<com.onlyfield.assetmanager.data.local.VlanEntity> = emptyList(),
    subnetEntities: List<com.onlyfield.assetmanager.data.local.SubnetEntity> = emptyList(),
    portVlanMembershipEntities: List<com.onlyfield.assetmanager.data.local.PortVlanMembershipEntity> = emptyList(),
    logicalInterfaceEntities: List<com.onlyfield.assetmanager.data.local.LogicalInterfaceEntity> = emptyList(),
    lagGroupEntities: List<com.onlyfield.assetmanager.data.local.LagGroupEntity> = emptyList(),
    deviceConfigurationEntities: List<com.onlyfield.assetmanager.data.local.DeviceConfigurationEntity> = emptyList(),
    wanVpnConnectionEntities: List<com.onlyfield.assetmanager.data.local.WanVpnConnectionEntity> = emptyList(),
    videoSurveillanceMappingEntities: List<com.onlyfield.assetmanager.data.local.VideoSurveillanceMappingEntity> = emptyList(),
    customExtraFieldEntities: List<com.onlyfield.assetmanager.data.local.CustomExtraFieldEntity> = emptyList(),
    powerFeedEntities: List<com.onlyfield.assetmanager.data.local.PowerFeedEntity> = emptyList(),
    poeMappingEntities: List<com.onlyfield.assetmanager.data.local.PoeMappingEntity> = emptyList(),
    documentBadgeEntities: List<com.onlyfield.assetmanager.data.local.DocumentBadgeEntity> = emptyList()
): Project {
    val portsByDevice = portEntities.groupBy { it.deviceId }
    val devicesBySite = deviceEntities.groupBy { it.siteId }
    val areasBySite = areaEntities.groupBy { it.siteId }

    val sites = siteEntities.map { siteEnt ->
        val siteDirectAreas = areasBySite[siteEnt.id].orEmpty()
            .map { areaEnt ->
                Area(
                    id = areaEnt.id,
                    name = areaEnt.name,
                    floor = areaEnt.floor,
                    description = areaEnt.description,
                    floorplanAttachmentId = areaEnt.floorplanAttachmentId,
                    floorplanPageIndex = areaEnt.floorplanPageIndex
                )
            }

        val siteDevices = devicesBySite[siteEnt.id].orEmpty().map { devEnt ->
            val devPorts = portsByDevice[devEnt.id].orEmpty().map { portEnt ->
                val obs = if ((portEnt.obsSource != null) && (portEnt.obsTimestampEpochMs != null) && (portEnt.obsStatus != null)) {
                    Observation(
                        source = portEnt.obsSource,
                        timestampEpochMs = portEnt.obsTimestampEpochMs,
                        status = try { ObservationStatus.valueOf(portEnt.obsStatus) } catch (_: Exception) { ObservationStatus.TO_VERIFY },
                        notes = portEnt.obsNotes
                    )
                } else null

                Port(
                    id = portEnt.id,
                    deviceId = portEnt.deviceId,
                    name = portEnt.name,
                    label = portEnt.label,
                    hardware = mapperJson.decodeFromString<com.onlyfield.assetmanager.core.model.PortHardware>(portEnt.hardwareJson),
                    endpointStatus = try { EndpointStatus.valueOf(portEnt.endpointStatus) } catch (_: Exception) { EndpointStatus.DISCONNECTED },
                    observation = obs
                )
            }

            val devObs = if ((devEnt.obsSource != null) && (devEnt.obsTimestampEpochMs != null) && (devEnt.obsStatus != null)) {
                Observation(
                    source = devEnt.obsSource,
                    timestampEpochMs = devEnt.obsTimestampEpochMs,
                    status = try { ObservationStatus.valueOf(devEnt.obsStatus) } catch (_: Exception) { ObservationStatus.TO_VERIFY },
                    notes = devEnt.obsNotes
                )
            } else null

            Device(
                id = devEnt.id,
                technicalName = devEnt.technicalName,
                physicalLabel = devEnt.physicalLabel,
                alias = devEnt.alias,
                ipAddress = devEnt.ipAddress,
                macAddress = devEnt.macAddress,
                areaId = devEnt.areaId,
                ports = devPorts,
                observation = devObs,
                rackId = devEnt.rackId,
                positionU = devEnt.positionU,
                heightU = devEnt.heightU,
                rackSide = try { RackSide.valueOf(devEnt.rackSide) } catch (_: Exception) { RackSide.BOTH },
                mountingType = try { MountingType.valueOf(devEnt.mountingType) } catch (_: Exception) { MountingType.OUT_OF_RACK },
                deviceModelId = devEnt.deviceModelId,
                category = try { DeviceCategory.valueOf(devEnt.category) } catch (_: Exception) { DeviceCategory.CUSTOM },
                objectTypeId = devEnt.objectTypeId,
                serialNumber = devEnt.serialNumber,
                hardware = mapperJson.decodeFromString<com.onlyfield.assetmanager.core.model.HardwareSpec>(devEnt.hardwareJson),
                operationalStatus = runCatching { com.onlyfield.assetmanager.core.model.OperationalStatus.valueOf(devEnt.operationalStatus) }.getOrDefault(com.onlyfield.assetmanager.core.model.OperationalStatus.IN_SERVICE),
            )
        }

        Site(
            id = siteEnt.id,
            name = siteEnt.name,
            code = siteEnt.code,
            group = siteEnt.groupName,
            address = siteEnt.address,
            areas = siteDirectAreas,
            devices = siteDevices
        )
    }

    val credentials = credentialEntities.map { toCredential(it) }
    val racks = rackEntities.map { toRack(it) }
    val deviceModels = deviceModelEntities.map { toDeviceModel(it) }
    val attachments = attachmentEntities.map { toAttachment(it) }
    val annotations = annotationEntities.map { toAnnotation(it) }
    val placements = placementEntities.map { toFloorplanPlacement(it) }
    val cables = cableEntities.map { toCable(it) }
    val panelMappings = panelMappingEntities.map { toPanelMapping(it) }
    val vlans = vlanEntities.map { toVlan(it) }
    val subnets = subnetEntities.map { toSubnet(it) }
    val portVlanMemberships = portVlanMembershipEntities.map { toPortVlanMembership(it) }
    val logicalInterfaces = logicalInterfaceEntities.map { toLogicalInterface(it) }
    val lagGroups = lagGroupEntities.map { toLagGroup(it) }
    val deviceConfigurations = deviceConfigurationEntities.map { toDeviceConfiguration(it) }
    val wanVpnConnections = wanVpnConnectionEntities.map { toWanVpnConnection(it) }
    val videoSurveillanceMappings = videoSurveillanceMappingEntities.map { toVideoSurveillanceMapping(it) }
    val customExtraFields = customExtraFieldEntities.map { toCustomExtraField(it) }
    val powerFeeds = powerFeedEntities.map { toPowerFeed(it) }
    val poeMappings = poeMappingEntities.map { toPoeMapping(it) }
    val documentBadges = documentBadgeEntities.map { toDocumentBadge(it) }

    return com.onlyfield.assetmanager.core.model.ObjectHierarchy.normalize(Project(
        id = entity.id,
        name = entity.name,
        description = entity.description,
        createdEpochMs = entity.createdEpochMs,
        updatedEpochMs = entity.updatedEpochMs,
        sites = sites,
        credentials = credentials,
        racks = racks,
        deviceModels = deviceModels,
        attachments = attachments,
        annotations = annotations,
        floorplanPlacements = placements,
        cables = cables,
        panelMappings = panelMappings,
        vlans = vlans,
        subnets = subnets,
        portVlanMemberships = portVlanMemberships,
        logicalInterfaces = logicalInterfaces,
        lagGroups = lagGroups,
        deviceConfigurations = deviceConfigurations,
        wanVpnConnections = wanVpnConnections,
        videoSurveillanceMappings = videoSurveillanceMappings,
        customExtraFields = customExtraFields,
        powerFeeds = powerFeeds,
        poeMappings = poeMappings,
        documentBadges = documentBadges,
        objectTypes = mapperJson.decodeFromString(entity.objectTypesJson),
        cableRoutes = mapperJson.decodeFromString(entity.cableRoutesJson),
        objectContainments = mapperJson.decodeFromString(entity.objectContainmentsJson),
        isPasswordProtected = entity.isPasswordProtected
    ))
}
