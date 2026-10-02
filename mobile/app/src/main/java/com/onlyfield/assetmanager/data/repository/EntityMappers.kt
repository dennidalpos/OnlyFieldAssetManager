package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.BusinessUnit
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
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.data.local.AreaEntity
import com.onlyfield.assetmanager.data.local.BusinessUnitEntity
import com.onlyfield.assetmanager.data.local.CredentialEntity
import com.onlyfield.assetmanager.data.local.DeviceEntity
import com.onlyfield.assetmanager.data.local.DeviceModelEntity
import com.onlyfield.assetmanager.data.local.PortEntity
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.local.RackEntity
import com.onlyfield.assetmanager.data.local.SiteEntity
import kotlinx.serialization.json.Json

object EntityMappers {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun toProjectEntity(project: Project): ProjectEntity {
        return ProjectEntity(
            id = project.id,
            name = project.name,
            description = project.description,
            createdEpochMs = project.createdEpochMs,
            updatedEpochMs = project.updatedEpochMs,
            isPasswordProtected = project.isPasswordProtected,
        )
    }

    fun toRackEntity(projectId: String, rack: Rack): RackEntity {
        return RackEntity(
            id = rack.id,
            projectId = projectId,
            name = rack.name,
            areaId = rack.areaId,
            heightU = rack.heightU,
            numberingDirection = rack.numberingDirection.name,
            depthMm = rack.depthMm,
            notes = rack.notes,
        )
    }

    fun toRack(entity: RackEntity): Rack {
        return Rack(
            id = entity.id,
            name = entity.name,
            areaId = entity.areaId,
            heightU = entity.heightU,
            numberingDirection = try { NumberingDirection.valueOf(entity.numberingDirection) } catch (_: Exception) { NumberingDirection.BOTTOM_TO_TOP },
            depthMm = entity.depthMm,
            notes = entity.notes,
        )
    }

    fun toDeviceModelEntity(projectId: String, model: DeviceModel): DeviceModelEntity {
        return DeviceModelEntity(
            id = model.id,
            projectId = projectId,
            name = model.name,
            brand = model.brand,
            modelNumber = model.modelNumber,
            category = model.category.name,
            defaultHeightU = model.defaultHeightU,
            portTemplatesJson = json.encodeToString(model.portTemplates),
            notes = model.notes,
        )
    }

    fun toDeviceModel(entity: DeviceModelEntity): DeviceModel {
        val templates = try {
            json.decodeFromString<List<PortTemplate>>(entity.portTemplatesJson)
        } catch (_: Exception) {
            emptyList()
        }
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

    fun toCredentialEntity(projectId: String, cred: Credential): CredentialEntity {
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

    fun toCredential(entity: CredentialEntity): Credential {
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

    fun toBusinessUnitEntity(projectId: String, bu: BusinessUnit): BusinessUnitEntity {
        return BusinessUnitEntity(
            id = bu.id,
            projectId = projectId,
            name = bu.name,
            code = bu.code
        )
    }

    fun toSiteEntity(buId: String, site: Site): SiteEntity {
        return SiteEntity(
            id = site.id,
            businessUnitId = buId,
            name = site.name,
            address = site.address
        )
    }

    fun toAreaEntity(buId: String, siteId: String?, area: Area): AreaEntity {
        return AreaEntity(
            id = area.id,
            businessUnitId = buId,
            siteId = siteId,
            name = area.name,
            floor = area.floor,
            description = area.description,
            floorplanAttachmentId = area.floorplanAttachmentId,
            floorplanPageIndex = area.floorplanPageIndex
        )
    }

    fun toAttachmentEntity(projectId: String, att: com.onlyfield.assetmanager.core.model.Attachment): com.onlyfield.assetmanager.data.local.AttachmentEntity {
        return com.onlyfield.assetmanager.data.local.AttachmentEntity(
            id = att.id,
            projectId = projectId,
            name = att.name,
            originalFileName = att.originalFileName,
            fileType = att.fileType.name,
            mimeType = att.mimeType,
            relativePath = att.relativePath,
            thumbnailPath = att.thumbnailPath,
            classification = att.classification.name,
            pageCount = att.pageCount,
            targetType = att.targetType?.name,
            targetId = att.targetId,
            attributionText = att.attributionText,
            createdAtEpochMs = att.createdAtEpochMs
        )
    }

    fun toAttachment(entity: com.onlyfield.assetmanager.data.local.AttachmentEntity): com.onlyfield.assetmanager.core.model.Attachment {
        return com.onlyfield.assetmanager.core.model.Attachment(
            id = entity.id,
            name = entity.name,
            originalFileName = entity.originalFileName,
            fileType = try { com.onlyfield.assetmanager.core.model.AttachmentType.valueOf(entity.fileType) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.AttachmentType.IMAGE },
            mimeType = entity.mimeType,
            relativePath = entity.relativePath,
            thumbnailPath = entity.thumbnailPath,
            classification = try { com.onlyfield.assetmanager.core.model.AttachmentClassification.valueOf(entity.classification) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.AttachmentClassification.SHAREABLE },
            pageCount = entity.pageCount,
            targetType = entity.targetType?.let { try { com.onlyfield.assetmanager.core.model.AttachmentTargetType.valueOf(it) } catch (_: Exception) { null } },
            targetId = entity.targetId,
            attributionText = entity.attributionText,
            createdAtEpochMs = entity.createdAtEpochMs
        )
    }

    fun toAnnotationEntity(projectId: String, ann: com.onlyfield.assetmanager.core.model.Annotation): com.onlyfield.assetmanager.data.local.AnnotationEntity {
        return com.onlyfield.assetmanager.data.local.AnnotationEntity(
            id = ann.id,
            projectId = projectId,
            areaId = ann.areaId,
            type = ann.type.name,
            x1Ratio = ann.x1Ratio,
            y1Ratio = ann.y1Ratio,
            x2Ratio = ann.x2Ratio,
            y2Ratio = ann.y2Ratio,
            label = ann.label,
            colorHex = ann.colorHex,
            classification = ann.classification.name
        )
    }

    fun toAnnotation(entity: com.onlyfield.assetmanager.data.local.AnnotationEntity): com.onlyfield.assetmanager.core.model.Annotation {
        return com.onlyfield.assetmanager.core.model.Annotation(
            id = entity.id,
            areaId = entity.areaId,
            type = try { com.onlyfield.assetmanager.core.model.AnnotationType.valueOf(entity.type) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.AnnotationType.TEXT },
            x1Ratio = entity.x1Ratio,
            y1Ratio = entity.y1Ratio,
            x2Ratio = entity.x2Ratio,
            y2Ratio = entity.y2Ratio,
            label = entity.label,
            colorHex = entity.colorHex,
            classification = try { com.onlyfield.assetmanager.core.model.AttachmentClassification.valueOf(entity.classification) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.AttachmentClassification.SHAREABLE }
        )
    }

    fun toFloorplanPlacementEntity(projectId: String, placement: com.onlyfield.assetmanager.core.model.FloorplanPlacement): com.onlyfield.assetmanager.data.local.FloorplanPlacementEntity {
        return com.onlyfield.assetmanager.data.local.FloorplanPlacementEntity(
            id = placement.id,
            projectId = projectId,
            areaId = placement.areaId,
            targetType = placement.targetType.name,
            targetId = placement.targetId,
            xRatio = placement.xRatio,
            yRatio = placement.yRatio,
            labelOverride = placement.labelOverride
        )
    }

    fun toFloorplanPlacement(entity: com.onlyfield.assetmanager.data.local.FloorplanPlacementEntity): com.onlyfield.assetmanager.core.model.FloorplanPlacement {
        return com.onlyfield.assetmanager.core.model.FloorplanPlacement(
            id = entity.id,
            areaId = entity.areaId,
            targetType = try { com.onlyfield.assetmanager.core.model.PlacementTargetType.valueOf(entity.targetType) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.PlacementTargetType.DEVICE },
            targetId = entity.targetId,
            xRatio = entity.xRatio,
            yRatio = entity.yRatio,
            labelOverride = entity.labelOverride
        )
    }

    fun toDeviceEntity(buId: String, device: Device): DeviceEntity {
        return DeviceEntity(
            id = device.id,
            businessUnitId = buId,
            siteId = device.siteId,
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
            category = device.category.name
        )
    }

    fun toPortEntity(port: Port): PortEntity {
        return PortEntity(
            id = port.id,
            deviceId = port.deviceId,
            name = port.name,
            label = port.label,
            connectedPortId = port.connectedPortId,
            endpointStatus = port.endpointStatus.name,
            obsSource = port.observation?.source,
            obsTimestampEpochMs = port.observation?.timestampEpochMs,
            obsStatus = port.observation?.status?.name,
            obsNotes = port.observation?.notes
        )
    }

    fun toProject(
        entity: ProjectEntity,
        buEntities: List<BusinessUnitEntity>,
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
        sharedPathSegmentEntities: List<com.onlyfield.assetmanager.data.local.SharedPathSegmentEntity> = emptyList(),
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
        val devicesByBu = deviceEntities.groupBy { it.businessUnitId }
        val sitesByBu = siteEntities.groupBy { it.businessUnitId }
        val areasByBu = areaEntities.groupBy { it.businessUnitId }

        val businessUnits = buEntities.map { buEnt ->
            val buSites = sitesByBu[buEnt.id].orEmpty().map { siteEnt ->
                val siteAreas = areasByBu[buEnt.id].orEmpty()
                    .filter { it.siteId == siteEnt.id }
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
                Site(
                    id = siteEnt.id,
                    name = siteEnt.name,
                    address = siteEnt.address,
                    areas = siteAreas
                )
            }

            val buDirectAreas = areasByBu[buEnt.id].orEmpty()
                .filter { it.siteId == null }
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

            val buDevices = devicesByBu[buEnt.id].orEmpty().map { devEnt ->
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
                        connectedPortId = portEnt.connectedPortId,
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
                    siteId = devEnt.siteId,
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
                )
            }

            BusinessUnit(
                id = buEnt.id,
                name = buEnt.name,
                code = buEnt.code,
                sites = buSites,
                areas = buDirectAreas,
                devices = buDevices
            )
        }

        val credentials = credentialEntities.map { toCredential(it) }
        val racks = rackEntities.map { toRack(it) }
        val deviceModels = deviceModelEntities.map { toDeviceModel(it) }
        val attachments = attachmentEntities.map { toAttachment(it) }
        val annotations = annotationEntities.map { toAnnotation(it) }
        val placements = placementEntities.map { toFloorplanPlacement(it) }
        val sharedPathSegments = sharedPathSegmentEntities.map { toSharedPathSegment(it) }
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

        return Project(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            createdEpochMs = entity.createdEpochMs,
            updatedEpochMs = entity.updatedEpochMs,
            businessUnits = businessUnits,
            credentials = credentials,
            racks = racks,
            deviceModels = deviceModels,
            attachments = attachments,
            annotations = annotations,
            floorplanPlacements = placements,
            cables = cables,
            sharedPathSegments = sharedPathSegments,
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
            isPasswordProtected = entity.isPasswordProtected
        )
    }

    fun toSharedPathSegmentEntity(projectId: String, segment: com.onlyfield.assetmanager.core.model.SharedPathSegment): com.onlyfield.assetmanager.data.local.SharedPathSegmentEntity {
        return com.onlyfield.assetmanager.data.local.SharedPathSegmentEntity(
            id = segment.id,
            projectId = projectId,
            name = segment.name,
            sourceAreaId = segment.sourceAreaId,
            targetAreaId = segment.targetAreaId,
            description = segment.description,
            capacityMaxCables = segment.capacityMaxCables,
            notes = segment.notes
        )
    }

    fun toSharedPathSegment(entity: com.onlyfield.assetmanager.data.local.SharedPathSegmentEntity): com.onlyfield.assetmanager.core.model.SharedPathSegment {
        return com.onlyfield.assetmanager.core.model.SharedPathSegment(
            id = entity.id,
            name = entity.name,
            sourceAreaId = entity.sourceAreaId,
            targetAreaId = entity.targetAreaId,
            description = entity.description,
            capacityMaxCables = entity.capacityMaxCables,
            notes = entity.notes
        )
    }

    fun toCableEntity(projectId: String, cable: com.onlyfield.assetmanager.core.model.Cable): com.onlyfield.assetmanager.data.local.CableEntity {
        return com.onlyfield.assetmanager.data.local.CableEntity(
            id = cable.id,
            projectId = projectId,
            codeOrLabel = cable.codeOrLabel,
            portAId = cable.portAId,
            portBId = cable.portBId,
            medium = cable.medium.name,
            connectorA = cable.connectorA,
            connectorB = cable.connectorB,
            nominalCharacteristics = cable.nominalCharacteristics,
            observedSpeed = cable.observedSpeed,
            color = cable.color,
            lengthValue = cable.lengthValue,
            lengthUnit = cable.lengthUnit,
            orientation = cable.orientation.name,
            sharedPathSegmentIdsJson = json.encodeToString(cable.sharedPathSegmentIds),
            obsSource = cable.observation?.source,
            obsTimestampEpochMs = cable.observation?.timestampEpochMs,
            obsStatus = cable.observation?.status?.name,
            obsNotes = cable.observation?.notes,
            notes = cable.notes
        )
    }

    fun toCable(entity: com.onlyfield.assetmanager.data.local.CableEntity): com.onlyfield.assetmanager.core.model.Cable {
        val segmentIds = try {
            json.decodeFromString<List<String>>(entity.sharedPathSegmentIdsJson)
        } catch (_: Exception) {
            emptyList()
        }
        val obs = if ((entity.obsSource != null) && (entity.obsTimestampEpochMs != null) && (entity.obsStatus != null)) {
            Observation(
                source = entity.obsSource,
                timestampEpochMs = entity.obsTimestampEpochMs,
                status = try { ObservationStatus.valueOf(entity.obsStatus) } catch (_: Exception) { ObservationStatus.TO_VERIFY },
                notes = entity.obsNotes
            )
        } else null

        return com.onlyfield.assetmanager.core.model.Cable(
            id = entity.id,
            codeOrLabel = entity.codeOrLabel,
            portAId = entity.portAId,
            portBId = entity.portBId,
            medium = try { com.onlyfield.assetmanager.core.model.CableMedium.valueOf(entity.medium) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.CableMedium.ETHERNET_COPPER },
            connectorA = entity.connectorA,
            connectorB = entity.connectorB,
            nominalCharacteristics = entity.nominalCharacteristics,
            observedSpeed = entity.observedSpeed,
            color = entity.color,
            lengthValue = entity.lengthValue,
            lengthUnit = entity.lengthUnit,
            orientation = try { com.onlyfield.assetmanager.core.model.CableOrientation.valueOf(entity.orientation) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.CableOrientation.NONE },
            sharedPathSegmentIds = segmentIds,
            observation = obs,
            notes = entity.notes
        )
    }

    fun toPanelMappingEntity(projectId: String, mapping: com.onlyfield.assetmanager.core.model.PanelMapping): com.onlyfield.assetmanager.data.local.PanelMappingEntity {
        return com.onlyfield.assetmanager.data.local.PanelMappingEntity(
            id = mapping.id,
            projectId = projectId,
            portAId = mapping.portAId,
            portBId = mapping.portBId,
            mappingType = mapping.mappingType,
            isUnknownPassage = mapping.isUnknownPassage,
            notes = mapping.notes
        )
    }

    fun toPanelMapping(entity: com.onlyfield.assetmanager.data.local.PanelMappingEntity): com.onlyfield.assetmanager.core.model.PanelMapping {
        return com.onlyfield.assetmanager.core.model.PanelMapping(
            id = entity.id,
            portAId = entity.portAId,
            portBId = entity.portBId,
            mappingType = entity.mappingType,
            isUnknownPassage = entity.isUnknownPassage,
            notes = entity.notes
        )
    }

    fun toVlanEntity(projectId: String, vlan: com.onlyfield.assetmanager.core.model.Vlan): com.onlyfield.assetmanager.data.local.VlanEntity {
        return com.onlyfield.assetmanager.data.local.VlanEntity(
            id = vlan.id,
            projectId = projectId,
            vlanId = vlan.vlanId,
            name = vlan.name,
            scopeType = vlan.scopeType.name,
            scopeTargetId = vlan.scopeTargetId,
            description = vlan.description
        )
    }

    fun toVlan(entity: com.onlyfield.assetmanager.data.local.VlanEntity): com.onlyfield.assetmanager.core.model.Vlan {
        return com.onlyfield.assetmanager.core.model.Vlan(
            id = entity.id,
            vlanId = entity.vlanId,
            name = entity.name,
            scopeType = try { com.onlyfield.assetmanager.core.model.VlanScopeType.valueOf(entity.scopeType) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.VlanScopeType.PROJECT },
            scopeTargetId = entity.scopeTargetId,
            description = entity.description
        )
    }

    fun toSubnetEntity(projectId: String, subnet: com.onlyfield.assetmanager.core.model.Subnet): com.onlyfield.assetmanager.data.local.SubnetEntity {
        return com.onlyfield.assetmanager.data.local.SubnetEntity(
            id = subnet.id,
            projectId = projectId,
            cidrBlock = subnet.cidrBlock,
            gatewayIp = subnet.gatewayIp,
            vlanId = subnet.vlanId,
            name = subnet.name,
            scopeType = subnet.scopeType.name,
            scopeTargetId = subnet.scopeTargetId,
            description = subnet.description
        )
    }

    fun toSubnet(entity: com.onlyfield.assetmanager.data.local.SubnetEntity): com.onlyfield.assetmanager.core.model.Subnet {
        return com.onlyfield.assetmanager.core.model.Subnet(
            id = entity.id,
            cidrBlock = entity.cidrBlock,
            gatewayIp = entity.gatewayIp,
            vlanId = entity.vlanId,
            name = entity.name,
            scopeType = try { com.onlyfield.assetmanager.core.model.VlanScopeType.valueOf(entity.scopeType) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.VlanScopeType.PROJECT },
            scopeTargetId = entity.scopeTargetId,
            description = entity.description
        )
    }

    fun toPortVlanMembershipEntity(projectId: String, membership: com.onlyfield.assetmanager.core.model.PortVlanMembership): com.onlyfield.assetmanager.data.local.PortVlanMembershipEntity {
        return com.onlyfield.assetmanager.data.local.PortVlanMembershipEntity(
            id = membership.id,
            projectId = projectId,
            portId = membership.portId,
            mode = membership.mode.name,
            untaggedVlanId = membership.untaggedVlanId,
            taggedVlanIdsJson = json.encodeToString(membership.taggedVlanIds),
            nativeVlanId = membership.nativeVlanId,
            notes = membership.notes
        )
    }

    fun toPortVlanMembership(entity: com.onlyfield.assetmanager.data.local.PortVlanMembershipEntity): com.onlyfield.assetmanager.core.model.PortVlanMembership {
        val tagged = try {
            json.decodeFromString<List<Int>>(entity.taggedVlanIdsJson)
        } catch (_: Exception) {
            emptyList()
        }
        return com.onlyfield.assetmanager.core.model.PortVlanMembership(
            id = entity.id,
            portId = entity.portId,
            mode = try { com.onlyfield.assetmanager.core.model.PortVlanMode.valueOf(entity.mode) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.PortVlanMode.ACCESS },
            untaggedVlanId = entity.untaggedVlanId,
            taggedVlanIds = tagged,
            nativeVlanId = entity.nativeVlanId,
            notes = entity.notes
        )
    }

    fun toLogicalInterfaceEntity(projectId: String, l3Int: com.onlyfield.assetmanager.core.model.LogicalInterface): com.onlyfield.assetmanager.data.local.LogicalInterfaceEntity {
        return com.onlyfield.assetmanager.data.local.LogicalInterfaceEntity(
            id = l3Int.id,
            projectId = projectId,
            deviceId = l3Int.deviceId,
            name = l3Int.name,
            ipAddress = l3Int.ipAddress,
            subnetCidr = l3Int.subnetCidr,
            vlanId = l3Int.vlanId,
            isL3 = l3Int.isL3,
            macAddress = l3Int.macAddress,
            notes = l3Int.notes
        )
    }

    fun toLogicalInterface(entity: com.onlyfield.assetmanager.data.local.LogicalInterfaceEntity): com.onlyfield.assetmanager.core.model.LogicalInterface {
        return com.onlyfield.assetmanager.core.model.LogicalInterface(
            id = entity.id,
            deviceId = entity.deviceId,
            name = entity.name,
            ipAddress = entity.ipAddress,
            subnetCidr = entity.subnetCidr,
            vlanId = entity.vlanId,
            isL3 = entity.isL3,
            macAddress = entity.macAddress,
            notes = entity.notes
        )
    }

    fun toLagGroupEntity(projectId: String, lag: com.onlyfield.assetmanager.core.model.LagGroup): com.onlyfield.assetmanager.data.local.LagGroupEntity {
        return com.onlyfield.assetmanager.data.local.LagGroupEntity(
            id = lag.id,
            projectId = projectId,
            deviceId = lag.deviceId,
            name = lag.name,
            mode = lag.mode.name,
            memberPortIdsJson = json.encodeToString(lag.memberPortIds),
            notes = lag.notes
        )
    }

    fun toLagGroup(entity: com.onlyfield.assetmanager.data.local.LagGroupEntity): com.onlyfield.assetmanager.core.model.LagGroup {
        val members = try {
            json.decodeFromString<List<String>>(entity.memberPortIdsJson)
        } catch (_: Exception) {
            emptyList()
        }
        return com.onlyfield.assetmanager.core.model.LagGroup(
            id = entity.id,
            deviceId = entity.deviceId,
            name = entity.name,
            mode = try { com.onlyfield.assetmanager.core.model.LagMode.valueOf(entity.mode) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.LagMode.LACP },
            memberPortIds = members,
            notes = entity.notes
        )
    }

    fun toDeviceConfigurationEntity(projectId: String, config: com.onlyfield.assetmanager.core.model.DeviceConfiguration): com.onlyfield.assetmanager.data.local.DeviceConfigurationEntity {
        return com.onlyfield.assetmanager.data.local.DeviceConfigurationEntity(
            id = config.id,
            projectId = projectId,
            deviceId = config.deviceId,
            title = config.title,
            configText = config.configText,
            attachmentId = config.attachmentId,
            capturedEpochMs = config.capturedEpochMs,
            notes = config.notes
        )
    }

    fun toDeviceConfiguration(entity: com.onlyfield.assetmanager.data.local.DeviceConfigurationEntity): com.onlyfield.assetmanager.core.model.DeviceConfiguration {
        return com.onlyfield.assetmanager.core.model.DeviceConfiguration(
            id = entity.id,
            deviceId = entity.deviceId,
            title = entity.title,
            configText = entity.configText,
            attachmentId = entity.attachmentId,
            capturedEpochMs = entity.capturedEpochMs,
            notes = entity.notes
        )
    }

    fun toWanVpnConnectionEntity(projectId: String, conn: com.onlyfield.assetmanager.core.model.WanVpnConnection): com.onlyfield.assetmanager.data.local.WanVpnConnectionEntity {
        return com.onlyfield.assetmanager.data.local.WanVpnConnectionEntity(
            id = conn.id,
            projectId = projectId,
            name = conn.name,
            type = conn.type.name,
            providerOrCarrier = conn.providerOrCarrier,
            bandwidth = conn.bandwidth,
            localEndpointDeviceId = conn.localEndpointDeviceId,
            localEndpointSiteDescription = conn.localEndpointSiteDescription,
            remoteEndpointDeviceId = conn.remoteEndpointDeviceId,
            remoteEndpointSiteDescription = conn.remoteEndpointSiteDescription,
            underlyingAccessId = conn.underlyingAccessId,
            notes = conn.notes
        )
    }

    fun toWanVpnConnection(entity: com.onlyfield.assetmanager.data.local.WanVpnConnectionEntity): com.onlyfield.assetmanager.core.model.WanVpnConnection {
        return com.onlyfield.assetmanager.core.model.WanVpnConnection(
            id = entity.id,
            name = entity.name,
            type = try { com.onlyfield.assetmanager.core.model.WanVpnType.valueOf(entity.type) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.WanVpnType.WAN },
            providerOrCarrier = entity.providerOrCarrier,
            bandwidth = entity.bandwidth,
            localEndpointDeviceId = entity.localEndpointDeviceId,
            localEndpointSiteDescription = entity.localEndpointSiteDescription,
            remoteEndpointDeviceId = entity.remoteEndpointDeviceId,
            remoteEndpointSiteDescription = entity.remoteEndpointSiteDescription,
            underlyingAccessId = entity.underlyingAccessId,
            notes = entity.notes
        )
    }

    fun toVideoSurveillanceMappingEntity(projectId: String, video: com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping): com.onlyfield.assetmanager.data.local.VideoSurveillanceMappingEntity {
        return com.onlyfield.assetmanager.data.local.VideoSurveillanceMappingEntity(
            id = video.id,
            projectId = projectId,
            cameraDeviceId = video.cameraDeviceId,
            managerDeviceId = video.managerDeviceId,
            externalManagerDescription = video.externalManagerDescription,
            channelNumber = video.channelNumber,
            streamUrl = video.streamUrl,
            resolution = video.resolution,
            notes = video.notes
        )
    }

    fun toVideoSurveillanceMapping(entity: com.onlyfield.assetmanager.data.local.VideoSurveillanceMappingEntity): com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping {
        return com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping(
            id = entity.id,
            cameraDeviceId = entity.cameraDeviceId,
            managerDeviceId = entity.managerDeviceId,
            externalManagerDescription = entity.externalManagerDescription,
            channelNumber = entity.channelNumber,
            streamUrl = entity.streamUrl,
            resolution = entity.resolution,
            notes = entity.notes
        )
    }

    fun toCustomExtraFieldEntity(projectId: String, field: com.onlyfield.assetmanager.core.model.CustomExtraField): com.onlyfield.assetmanager.data.local.CustomExtraFieldEntity {
        return com.onlyfield.assetmanager.data.local.CustomExtraFieldEntity(
            id = field.id,
            projectId = projectId,
            targetType = field.targetType,
            targetId = field.targetId,
            fieldKey = field.fieldKey,
            fieldValue = field.fieldValue,
            fieldType = field.fieldType.name,
            classification = field.classification.name,
            notes = field.notes
        )
    }

    fun toCustomExtraField(entity: com.onlyfield.assetmanager.data.local.CustomExtraFieldEntity): com.onlyfield.assetmanager.core.model.CustomExtraField {
        return com.onlyfield.assetmanager.core.model.CustomExtraField(
            id = entity.id,
            targetType = entity.targetType,
            targetId = entity.targetId,
            fieldKey = entity.fieldKey,
            fieldValue = entity.fieldValue,
            fieldType = try { com.onlyfield.assetmanager.core.model.CustomFieldType.valueOf(entity.fieldType) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.CustomFieldType.STRING },
            classification = try { com.onlyfield.assetmanager.core.model.AttachmentClassification.valueOf(entity.classification) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.AttachmentClassification.SHAREABLE },
            notes = entity.notes
        )
    }

    fun toPowerFeedEntity(projectId: String, feed: com.onlyfield.assetmanager.core.model.PowerFeed): com.onlyfield.assetmanager.data.local.PowerFeedEntity {
        return com.onlyfield.assetmanager.data.local.PowerFeedEntity(
            id = feed.id,
            projectId = projectId,
            deviceId = feed.deviceId,
            feedName = feed.feedName,
            feedType = feed.feedType.name,
            sourceDeviceId = feed.sourceDeviceId,
            sourceOutletDescription = feed.sourceOutletDescription,
            voltageVolts = feed.voltageVolts,
            loadVa = feed.loadVa,
            loadWatts = feed.loadWatts,
            observedRuntimeMinutes = feed.observedRuntimeMinutes,
            observedSource = feed.observedSource,
            observedEpochMs = feed.observedEpochMs,
            notes = feed.notes
        )
    }

    fun toPowerFeed(entity: com.onlyfield.assetmanager.data.local.PowerFeedEntity): com.onlyfield.assetmanager.core.model.PowerFeed {
        return com.onlyfield.assetmanager.core.model.PowerFeed(
            id = entity.id,
            deviceId = entity.deviceId,
            feedName = entity.feedName,
            feedType = try { com.onlyfield.assetmanager.core.model.PowerFeedType.valueOf(entity.feedType) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.PowerFeedType.PRIMARY_A },
            sourceDeviceId = entity.sourceDeviceId,
            sourceOutletDescription = entity.sourceOutletDescription,
            voltageVolts = entity.voltageVolts,
            loadVa = entity.loadVa,
            loadWatts = entity.loadWatts,
            observedRuntimeMinutes = entity.observedRuntimeMinutes,
            observedSource = entity.observedSource,
            observedEpochMs = entity.observedEpochMs,
            notes = entity.notes
        )
    }

    fun toPoeMappingEntity(projectId: String, poe: com.onlyfield.assetmanager.core.model.PoeMapping): com.onlyfield.assetmanager.data.local.PoeMappingEntity {
        return com.onlyfield.assetmanager.data.local.PoeMappingEntity(
            id = poe.id,
            projectId = projectId,
            portId = poe.portId,
            role = poe.role.name,
            standard = poe.standard.name,
            allocatedPowerWatts = poe.allocatedPowerWatts,
            notes = poe.notes
        )
    }

    fun toPoeMapping(entity: com.onlyfield.assetmanager.data.local.PoeMappingEntity): com.onlyfield.assetmanager.core.model.PoeMapping {
        return com.onlyfield.assetmanager.core.model.PoeMapping(
            id = entity.id,
            portId = entity.portId,
            role = try { com.onlyfield.assetmanager.core.model.PoeRole.valueOf(entity.role) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.PoeRole.PSE_SOURCE },
            standard = try { com.onlyfield.assetmanager.core.model.PoeStandard.valueOf(entity.standard) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.PoeStandard.IEEE_802_3AT },
            allocatedPowerWatts = entity.allocatedPowerWatts,
            notes = entity.notes
        )
    }

    fun toDocumentBadgeEntity(projectId: String, badge: com.onlyfield.assetmanager.core.model.DocumentBadge): com.onlyfield.assetmanager.data.local.DocumentBadgeEntity {
        return com.onlyfield.assetmanager.data.local.DocumentBadgeEntity(
            id = badge.id,
            projectId = projectId,
            targetType = badge.targetType,
            targetId = badge.targetId,
            label = badge.label,
            category = badge.category.name,
            isDerived = badge.isDerived,
            notes = badge.notes
        )
    }

    fun toDocumentBadge(entity: com.onlyfield.assetmanager.data.local.DocumentBadgeEntity): com.onlyfield.assetmanager.core.model.DocumentBadge {
        return com.onlyfield.assetmanager.core.model.DocumentBadge(
            id = entity.id,
            targetType = entity.targetType,
            targetId = entity.targetId,
            label = entity.label,
            category = try { com.onlyfield.assetmanager.core.model.BadgeCategory.valueOf(entity.category) } catch (_: Exception) { com.onlyfield.assetmanager.core.model.BadgeCategory.FREE_LABEL },
            isDerived = entity.isDerived,
            notes = entity.notes
        )
    }

    fun toTrashItemEntity(trashItem: com.onlyfield.assetmanager.core.model.TrashItem): com.onlyfield.assetmanager.data.local.TrashItemEntity {
        return com.onlyfield.assetmanager.data.local.TrashItemEntity(
            id = trashItem.id,
            projectId = trashItem.projectId,
            itemType = trashItem.itemType,
            itemId = trashItem.itemId,
            displayName = trashItem.displayName,
            serializedJson = trashItem.serializedJson,
            deletedEpochMs = trashItem.deletedEpochMs,
            affectedReferencesSummary = trashItem.affectedReferencesSummary
        )
    }

    fun toTrashItem(entity: com.onlyfield.assetmanager.data.local.TrashItemEntity): com.onlyfield.assetmanager.core.model.TrashItem {
        return com.onlyfield.assetmanager.core.model.TrashItem(
            id = entity.id,
            projectId = entity.projectId,
            itemType = entity.itemType,
            itemId = entity.itemId,
            displayName = entity.displayName,
            serializedJson = entity.serializedJson,
            deletedEpochMs = entity.deletedEpochMs,
            affectedReferencesSummary = entity.affectedReferencesSummary
        )
    }
}
