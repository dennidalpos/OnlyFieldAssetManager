package com.onlyfield.assetmanager.data.repository.mappers

import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus

// Room <-> domain mapping: cables, shared paths, panel mappings.

internal fun toSharedPathSegmentEntity(projectId: String, segment: com.onlyfield.assetmanager.core.model.SharedPathSegment): com.onlyfield.assetmanager.data.local.SharedPathSegmentEntity {
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

internal fun toSharedPathSegment(entity: com.onlyfield.assetmanager.data.local.SharedPathSegmentEntity): com.onlyfield.assetmanager.core.model.SharedPathSegment {
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

internal fun toCableEntity(projectId: String, cable: com.onlyfield.assetmanager.core.model.Cable): com.onlyfield.assetmanager.data.local.CableEntity {
    return com.onlyfield.assetmanager.data.local.CableEntity(
        id = cable.id,
        projectId = projectId,
        codeOrLabel = cable.codeOrLabel,
        objectTypeId = cable.objectTypeId,
        deviceAId = cable.deviceAId,
        deviceBId = cable.deviceBId,
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
        sharedPathSegmentIdsJson = mapperJson.encodeToString(cable.sharedPathSegmentIds),
        obsSource = cable.observation?.source,
        obsTimestampEpochMs = cable.observation?.timestampEpochMs,
        obsStatus = cable.observation?.status?.name,
        obsNotes = cable.observation?.notes,
        notes = cable.notes
    )
}

internal fun toCable(entity: com.onlyfield.assetmanager.data.local.CableEntity): com.onlyfield.assetmanager.core.model.Cable {
    val segmentIds = try {
        mapperJson.decodeFromString<List<String>>(entity.sharedPathSegmentIdsJson)
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
        objectTypeId = entity.objectTypeId,
        deviceAId = entity.deviceAId,
        deviceBId = entity.deviceBId,
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

internal fun toPanelMappingEntity(projectId: String, mapping: com.onlyfield.assetmanager.core.model.PanelMapping): com.onlyfield.assetmanager.data.local.PanelMappingEntity {
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

internal fun toPanelMapping(entity: com.onlyfield.assetmanager.data.local.PanelMappingEntity): com.onlyfield.assetmanager.core.model.PanelMapping {
    return com.onlyfield.assetmanager.core.model.PanelMapping(
        id = entity.id,
        portAId = entity.portAId,
        portBId = entity.portBId,
        mappingType = entity.mappingType,
        isUnknownPassage = entity.isUnknownPassage,
        notes = entity.notes
    )
}
