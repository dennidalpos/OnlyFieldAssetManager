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
    val updatedEpochMs: Long
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
    val description: String?
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
    val obsNotes: String?
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
