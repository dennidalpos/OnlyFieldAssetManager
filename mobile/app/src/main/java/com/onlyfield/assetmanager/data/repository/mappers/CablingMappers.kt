package com.onlyfield.assetmanager.data.repository.mappers

import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus

// Room <-> domain mapping: cables and panel mappings.

internal fun toCableEntity(projectId: String, cable: com.onlyfield.assetmanager.core.model.Cable): com.onlyfield.assetmanager.data.local.CableEntity {
    return com.onlyfield.assetmanager.data.local.CableEntity(
        id = cable.id,
        deviceModelId = cable.deviceModelId,
        projectId = projectId,
        codeOrLabel = cable.codeOrLabel,
        objectTypeId = cable.objectTypeId,
        deviceAId = cable.deviceAId,
        deviceBId = cable.deviceBId,
        portAId = cable.portAId,
        portBId = cable.portBId,
        medium = cable.medium.name,
        color = cable.color,
        lengthValue = cable.lengthValue,
        lengthUnit = cable.lengthUnit,
        obsSource = cable.observation?.source,
        obsTimestampEpochMs = cable.observation?.timestampEpochMs,
        obsStatus = cable.observation?.status?.name,
        obsNotes = cable.observation?.notes,
        notes = cable.notes
    )
}

internal fun toCable(entity: com.onlyfield.assetmanager.data.local.CableEntity): com.onlyfield.assetmanager.core.model.Cable {
    val obs = if ((entity.obsSource != null) && (entity.obsTimestampEpochMs != null) && (entity.obsStatus != null)) {
        Observation(
            source = entity.obsSource,
            timestampEpochMs = entity.obsTimestampEpochMs,
            status = ObservationStatus.valueOf(entity.obsStatus),
            notes = entity.obsNotes
        )
    } else null

    return com.onlyfield.assetmanager.core.model.Cable(
        deviceModelId = entity.deviceModelId,
        id = entity.id,
        codeOrLabel = entity.codeOrLabel,
        objectTypeId = entity.objectTypeId,
        deviceAId = entity.deviceAId,
        deviceBId = entity.deviceBId,
        portAId = entity.portAId,
        portBId = entity.portBId,
        medium = com.onlyfield.assetmanager.core.model.CableMedium.valueOf(entity.medium),
        color = entity.color,
        lengthValue = entity.lengthValue,
        lengthUnit = entity.lengthUnit,
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
        isUnknownPassage = mapping.isUnknownPassage,
    )
}

internal fun toPanelMapping(entity: com.onlyfield.assetmanager.data.local.PanelMappingEntity): com.onlyfield.assetmanager.core.model.PanelMapping {
    return com.onlyfield.assetmanager.core.model.PanelMapping(
        id = entity.id,
        portAId = entity.portAId,
        portBId = entity.portBId,
        isUnknownPassage = entity.isUnknownPassage,
    )
}
