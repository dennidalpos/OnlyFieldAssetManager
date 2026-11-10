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

// Room <-> domain mapping: power feeds, PoE, document badges, trash items.

internal fun toPowerFeedEntity(projectId: String, feed: com.onlyfield.assetmanager.core.model.PowerFeed): com.onlyfield.assetmanager.data.local.PowerFeedEntity {
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

internal fun toPowerFeed(entity: com.onlyfield.assetmanager.data.local.PowerFeedEntity): com.onlyfield.assetmanager.core.model.PowerFeed {
    return com.onlyfield.assetmanager.core.model.PowerFeed(
        id = entity.id,
        deviceId = entity.deviceId,
        feedName = entity.feedName,
        feedType = com.onlyfield.assetmanager.core.model.PowerFeedType.valueOf(entity.feedType),
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

internal fun toPoeMappingEntity(projectId: String, poe: com.onlyfield.assetmanager.core.model.PoeMapping): com.onlyfield.assetmanager.data.local.PoeMappingEntity {
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

internal fun toPoeMapping(entity: com.onlyfield.assetmanager.data.local.PoeMappingEntity): com.onlyfield.assetmanager.core.model.PoeMapping {
    return com.onlyfield.assetmanager.core.model.PoeMapping(
        id = entity.id,
        portId = entity.portId,
        role = com.onlyfield.assetmanager.core.model.PoeRole.valueOf(entity.role),
        standard = com.onlyfield.assetmanager.core.model.PoeStandard.valueOf(entity.standard),
        allocatedPowerWatts = entity.allocatedPowerWatts,
        notes = entity.notes
    )
}

internal fun toDocumentBadgeEntity(projectId: String, badge: com.onlyfield.assetmanager.core.model.DocumentBadge): com.onlyfield.assetmanager.data.local.DocumentBadgeEntity {
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

internal fun toDocumentBadge(entity: com.onlyfield.assetmanager.data.local.DocumentBadgeEntity): com.onlyfield.assetmanager.core.model.DocumentBadge {
    return com.onlyfield.assetmanager.core.model.DocumentBadge(
        id = entity.id,
        targetType = entity.targetType,
        targetId = entity.targetId,
        label = entity.label,
        category = com.onlyfield.assetmanager.core.model.BadgeCategory.valueOf(entity.category),
        isDerived = entity.isDerived,
        notes = entity.notes
    )
}

internal fun toTrashItemEntity(trashItem: com.onlyfield.assetmanager.core.model.TrashItem): com.onlyfield.assetmanager.data.local.TrashItemEntity {
    return com.onlyfield.assetmanager.data.local.TrashItemEntity(
        id = trashItem.id,
        projectId = trashItem.projectId,
        itemType = trashItem.itemType,
        itemId = trashItem.itemId,
        displayName = trashItem.displayName,
        serializedJson = trashItem.serializedJson,
        deletedEpochMs = trashItem.deletedEpochMs,
        affectedReferencesSummary = trashItem.affectedReferencesSummary,
        containmentMetadataJson = mapperJson.encodeToString(trashItem)
    )
}

internal fun toTrashItem(entity: com.onlyfield.assetmanager.data.local.TrashItemEntity): com.onlyfield.assetmanager.core.model.TrashItem {
    entity.containmentMetadataJson?.let { return mapperJson.decodeFromString(it) }
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
