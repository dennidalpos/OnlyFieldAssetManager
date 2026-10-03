package com.onlyfield.assetmanager.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val createdEpochMs: Long,
    val updatedEpochMs: Long,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String? = null,
    val objectTypesJson: String = "[]",
    val cableRoutesJson: String = "[]",
    val objectContainmentsJson: String = "[]",
)

@Entity(
    tableName = "racks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("areaId")],
)
data class RackEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val areaId: String?,
    val heightU: Int,
    val numberingDirection: String,
    val depthMm: Int?,
    val notes: String?,
)

@Entity(
    tableName = "device_models",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class DeviceModelEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val brand: String?,
    val modelNumber: String?,
    val category: String,
    val defaultHeightU: Int,
    val portTemplatesJson: String,
    val notes: String?
)

@Entity(
    tableName = "credentials",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("deviceId")]
)
data class CredentialEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val deviceId: String?,
    val groupName: String?,
    val username: String,
    val secret: String,
    val type: String,
    val notes: String?
)

@Entity(
    tableName = "business_units",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class BusinessUnitEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val code: String?
)

@Entity(
    tableName = "sites",
    foreignKeys = [
        ForeignKey(
            entity = BusinessUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessUnitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessUnitId")]
)
data class SiteEntity(
    @PrimaryKey val id: String,
    val businessUnitId: String,
    val name: String,
    val address: String?
)

@Entity(
    tableName = "areas",
    foreignKeys = [
        ForeignKey(
            entity = BusinessUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessUnitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessUnitId"), Index("siteId")]
)
data class AreaEntity(
    @PrimaryKey val id: String,
    val businessUnitId: String,
    val siteId: String?,
    val name: String,
    val floor: String?,
    val description: String?,
    val floorplanAttachmentId: String? = null,
    val floorplanPageIndex: Int = 0
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("targetId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val originalFileName: String,
    val fileType: String,
    val mimeType: String,
    val relativePath: String,
    val thumbnailPath: String?,
    val classification: String,
    val pageCount: Int,
    val targetType: String?,
    val targetId: String?,
    val attributionText: String? = null,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "annotations",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("areaId")]
)
data class AnnotationEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val areaId: String,
    val type: String,
    val x1Ratio: Float,
    val y1Ratio: Float,
    val x2Ratio: Float,
    val y2Ratio: Float,
    val label: String,
    val colorHex: String,
    val classification: String
)

@Entity(
    tableName = "floorplan_placements",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("areaId")]
)
data class FloorplanPlacementEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val areaId: String,
    val targetType: String,
    val targetId: String,
    val xRatio: Float,
    val yRatio: Float,
    val labelOverride: String?
)

@Entity(
    tableName = "devices",
    foreignKeys = [
        ForeignKey(
            entity = BusinessUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessUnitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("businessUnitId"),
        Index("siteId"),
        Index("areaId"),
        Index("rackId"),
        Index("deviceModelId"),
        Index("technicalName"),
        Index("ipAddress")
    ]
)
data class DeviceEntity(
    @PrimaryKey val id: String,
    val businessUnitId: String,
    val siteId: String?,
    val areaId: String?,
    val technicalName: String,
    val physicalLabel: String?,
    val alias: String?,
    val ipAddress: String?,
    val macAddress: String?,
    val obsSource: String?,
    val obsTimestampEpochMs: Long?,
    val obsStatus: String?,
    val obsNotes: String?,
    val rackId: String? = null,
    val positionU: Int? = null,
    val heightU: Int = 1,
    val rackSide: String = "BOTH",
    val mountingType: String = "OUT_OF_RACK",
    val deviceModelId: String? = null,
    val category: String = "CUSTOM",
    val objectTypeId: String? = null,
    val serialNumber: String? = null
)

@Entity(
    tableName = "ports",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deviceId"), Index("connectedPortId")]
)
data class PortEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val name: String,
    val label: String?,
    val connectedPortId: String?,
    val endpointStatus: String,
    val obsSource: String?,
    val obsTimestampEpochMs: Long?,
    val obsStatus: String?,
    val obsNotes: String?
)

@Entity(
    tableName = "shared_path_segments",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("sourceAreaId"), Index("targetAreaId")]
)
data class SharedPathSegmentEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val sourceAreaId: String?,
    val targetAreaId: String?,
    val description: String?,
    val capacityMaxCables: Int?,
    val notes: String?
)

@Entity(
    tableName = "cables",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("portAId"), Index("portBId")]
)
data class CableEntity(
    val objectTypeId: String? = null,
    val deviceAId: String? = null,
    val deviceBId: String? = null,
    @PrimaryKey val id: String,
    val projectId: String,
    val codeOrLabel: String?,
    val portAId: String?,
    val portBId: String?,
    val medium: String,
    val connectorA: String?,
    val connectorB: String?,
    val nominalCharacteristics: String?,
    val observedSpeed: String?,
    val color: String?,
    val lengthValue: Double?,
    val lengthUnit: String?,
    val orientation: String,
    val sharedPathSegmentIdsJson: String,
    val obsSource: String?,
    val obsTimestampEpochMs: Long?,
    val obsStatus: String?,
    val obsNotes: String?,
    val notes: String?
)

@Entity(
    tableName = "panel_mappings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("portAId"), Index("portBId")]
)
data class PanelMappingEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val portAId: String,
    val portBId: String?,
    val mappingType: String,
    val isUnknownPassage: Boolean,
    val notes: String?
)

@Entity(
    tableName = "vlans",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("vlanId")]
)
data class VlanEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val vlanId: Int,
    val name: String,
    val scopeType: String,
    val scopeTargetId: String?,
    val description: String?
)

@Entity(
    tableName = "subnets",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class SubnetEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val cidrBlock: String,
    val gatewayIp: String?,
    val vlanId: String?,
    val name: String?,
    val scopeType: String,
    val scopeTargetId: String?,
    val description: String?
)

@Entity(
    tableName = "port_vlan_memberships",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("portId")]
)
data class PortVlanMembershipEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val portId: String,
    val mode: String,
    val untaggedVlanId: Int?,
    val taggedVlanIdsJson: String,
    val nativeVlanId: Int?,
    val notes: String?
)

@Entity(
    tableName = "logical_interfaces",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("deviceId")]
)
data class LogicalInterfaceEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val deviceId: String,
    val name: String,
    val ipAddress: String?,
    val subnetCidr: String?,
    val vlanId: Int?,
    val isL3: Boolean,
    val macAddress: String?,
    val notes: String?
)

@Entity(
    tableName = "lag_groups",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("deviceId")]
)
data class LagGroupEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val deviceId: String,
    val name: String,
    val mode: String,
    val memberPortIdsJson: String,
    val notes: String?
)

@Entity(
    tableName = "device_configurations",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("deviceId")]
)
data class DeviceConfigurationEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val deviceId: String,
    val title: String,
    val configText: String?,
    val attachmentId: String?,
    val capturedEpochMs: Long,
    val notes: String?
)

@Entity(
    tableName = "wan_vpn_connections",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class WanVpnConnectionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val type: String,
    val providerOrCarrier: String?,
    val bandwidth: String?,
    val localEndpointDeviceId: String?,
    val localEndpointSiteDescription: String?,
    val remoteEndpointDeviceId: String?,
    val remoteEndpointSiteDescription: String?,
    val underlyingAccessId: String?,
    val notes: String?
)

@Entity(
    tableName = "video_surveillance_mappings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("cameraDeviceId")]
)
data class VideoSurveillanceMappingEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val cameraDeviceId: String,
    val managerDeviceId: String?,
    val externalManagerDescription: String?,
    val channelNumber: Int?,
    val streamUrl: String?,
    val resolution: String?,
    val notes: String?
)

@Entity(
    tableName = "custom_extra_fields",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("targetId")]
)
data class CustomExtraFieldEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val targetType: String,
    val targetId: String,
    val fieldKey: String,
    val fieldValue: String,
    val fieldType: String,
    val classification: String,
    val notes: String?
)

@Entity(
    tableName = "power_feeds",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("deviceId"), Index("sourceDeviceId")]
)
data class PowerFeedEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val deviceId: String,
    val feedName: String,
    val feedType: String,
    val sourceDeviceId: String?,
    val sourceOutletDescription: String?,
    val voltageVolts: Int?,
    val loadVa: Double?,
    val loadWatts: Double?,
    val observedRuntimeMinutes: Int?,
    val observedSource: String?,
    val observedEpochMs: Long?,
    val notes: String?
)

@Entity(
    tableName = "poe_mappings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("portId")]
)
data class PoeMappingEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val portId: String,
    val role: String,
    val standard: String,
    val allocatedPowerWatts: Double?,
    val notes: String?
)

@Entity(
    tableName = "document_badges",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("targetId")]
)
data class DocumentBadgeEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val targetType: String,
    val targetId: String,
    val label: String,
    val category: String,
    val isDerived: Boolean,
    val notes: String?
)

@Entity(
    tableName = "trash_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("itemId")]
)
data class TrashItemEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val itemType: String,
    val itemId: String,
    val displayName: String,
    val serializedJson: String,
    val deletedEpochMs: Long,
    val affectedReferencesSummary: String?,
    val containmentMetadataJson: String? = null
)

/**
 * Merge base (F04): the project as the other device last saw it, saved at every export and import.
 * No foreign key on purpose: saveProject replaces the project row, which would cascade-delete it.
 */
@Entity(tableName = "sync_snapshots")
data class SyncSnapshotEntity(
    @PrimaryKey val projectId: String,
    val projectJson: String,
    val savedEpochMs: Long,
)
