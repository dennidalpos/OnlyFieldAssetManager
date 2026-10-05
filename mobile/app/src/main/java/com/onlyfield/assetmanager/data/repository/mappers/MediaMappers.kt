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

// Room <-> domain mapping: attachments, annotations, floorplan placements.

internal fun toAttachmentEntity(projectId: String, att: com.onlyfield.assetmanager.core.model.Attachment): com.onlyfield.assetmanager.data.local.AttachmentEntity {
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

internal fun toAttachment(entity: com.onlyfield.assetmanager.data.local.AttachmentEntity): com.onlyfield.assetmanager.core.model.Attachment {
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

internal fun toAnnotationEntity(projectId: String, ann: com.onlyfield.assetmanager.core.model.Annotation): com.onlyfield.assetmanager.data.local.AnnotationEntity {
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

internal fun toAnnotation(entity: com.onlyfield.assetmanager.data.local.AnnotationEntity): com.onlyfield.assetmanager.core.model.Annotation {
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

internal fun toFloorplanPlacementEntity(projectId: String, placement: com.onlyfield.assetmanager.core.model.FloorplanPlacement): com.onlyfield.assetmanager.data.local.FloorplanPlacementEntity {
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

internal fun toFloorplanPlacement(entity: com.onlyfield.assetmanager.data.local.FloorplanPlacementEntity): com.onlyfield.assetmanager.core.model.FloorplanPlacement {
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
