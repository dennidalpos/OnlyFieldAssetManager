package com.onlyfield.assetmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        SiteEntity::class,
        AreaEntity::class,
        DeviceEntity::class,
        PortEntity::class,
        CredentialEntity::class,
        RackEntity::class,
        DeviceModelEntity::class,
        AttachmentEntity::class,
        AnnotationEntity::class,
        FloorplanPlacementEntity::class,
        CableEntity::class,
        PanelMappingEntity::class,
        VlanEntity::class,
        SubnetEntity::class,
        PortVlanMembershipEntity::class,
        LogicalInterfaceEntity::class,
        LagGroupEntity::class,
        DeviceConfigurationEntity::class,
        WanVpnConnectionEntity::class,
        VideoSurveillanceMappingEntity::class,
        CustomExtraFieldEntity::class,
        PowerFeedEntity::class,
        PoeMappingEntity::class,
        DocumentBadgeEntity::class,
        TrashItemEntity::class,
        SyncSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun inventoryDao(): InventoryDao
}
