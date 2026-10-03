package com.onlyfield.assetmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        BusinessUnitEntity::class,
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
        SharedPathSegmentEntity::class,
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
    ],
    version = 10,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN isPasswordProtected INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN passwordHash TEXT DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credentials` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `deviceId` TEXT,
                        `groupName` TEXT,
                        `username` TEXT NOT NULL,
                        `secret` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_credentials_projectId` ON `credentials` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_credentials_deviceId` ON `credentials` (`deviceId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `racks` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `areaId` TEXT,
                        `heightU` INTEGER NOT NULL,
                        `numberingDirection` TEXT NOT NULL,
                        `depthMm` INTEGER,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_racks_projectId` ON `racks` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_racks_areaId` ON `racks` (`areaId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `device_models` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `brand` TEXT,
                        `modelNumber` TEXT,
                        `category` TEXT NOT NULL,
                        `defaultHeightU` INTEGER NOT NULL,
                        `portTemplatesJson` TEXT NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_device_models_projectId` ON `device_models` (`projectId`)")

                db.execSQL("ALTER TABLE `devices` ADD COLUMN `rackId` TEXT")
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `positionU` INTEGER")
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `heightU` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `rackSide` TEXT NOT NULL DEFAULT 'BOTH'")
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `mountingType` TEXT NOT NULL DEFAULT 'OUT_OF_RACK'")
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `deviceModelId` TEXT")
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'CUSTOM'")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_devices_rackId` ON `devices` (`rackId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_devices_deviceModelId` ON `devices` (`deviceModelId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE areas ADD COLUMN floorplanAttachmentId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE areas ADD COLUMN floorplanPageIndex INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `attachments` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `originalFileName` TEXT NOT NULL,
                        `fileType` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `relativePath` TEXT NOT NULL,
                        `thumbnailPath` TEXT,
                        `classification` TEXT NOT NULL,
                        `pageCount` INTEGER NOT NULL,
                        `targetType` TEXT,
                        `targetId` TEXT,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_projectId` ON `attachments` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_targetId` ON `attachments` (`targetId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `annotations` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `areaId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `x1Ratio` REAL NOT NULL,
                        `y1Ratio` REAL NOT NULL,
                        `x2Ratio` REAL NOT NULL,
                        `y2Ratio` REAL NOT NULL,
                        `label` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `classification` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_projectId` ON `annotations` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_areaId` ON `annotations` (`areaId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `floorplan_placements` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `areaId` TEXT NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `targetId` TEXT NOT NULL,
                        `xRatio` REAL NOT NULL,
                        `yRatio` REAL NOT NULL,
                        `labelOverride` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_floorplan_placements_projectId` ON `floorplan_placements` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_floorplan_placements_areaId` ON `floorplan_placements` (`areaId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shared_path_segments` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `sourceAreaId` TEXT,
                        `targetAreaId` TEXT,
                        `description` TEXT,
                        `capacityMaxCables` INTEGER,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_path_segments_projectId` ON `shared_path_segments` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_path_segments_sourceAreaId` ON `shared_path_segments` (`sourceAreaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_path_segments_targetAreaId` ON `shared_path_segments` (`targetAreaId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cables` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `codeOrLabel` TEXT,
                        `portAId` TEXT,
                        `portBId` TEXT,
                        `medium` TEXT NOT NULL,
                        `connectorA` TEXT,
                        `connectorB` TEXT,
                        `nominalCharacteristics` TEXT,
                        `observedSpeed` TEXT,
                        `color` TEXT,
                        `lengthValue` REAL,
                        `lengthUnit` TEXT,
                        `orientation` TEXT NOT NULL,
                        `sharedPathSegmentIdsJson` TEXT NOT NULL,
                        `obsSource` TEXT,
                        `obsTimestampEpochMs` INTEGER,
                        `obsStatus` TEXT,
                        `obsNotes` TEXT,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cables_projectId` ON `cables` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cables_portAId` ON `cables` (`portAId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cables_portBId` ON `cables` (`portBId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `panel_mappings` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `portAId` TEXT NOT NULL,
                        `portBId` TEXT,
                        `mappingType` TEXT NOT NULL,
                        `isUnknownPassage` INTEGER NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_panel_mappings_projectId` ON `panel_mappings` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_panel_mappings_portAId` ON `panel_mappings` (`portAId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_panel_mappings_portBId` ON `panel_mappings` (`portBId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vlans` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `vlanId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `scopeType` TEXT NOT NULL,
                        `scopeTargetId` TEXT,
                        `description` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vlans_projectId` ON `vlans` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vlans_vlanId` ON `vlans` (`vlanId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subnets` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `cidrBlock` TEXT NOT NULL,
                        `gatewayIp` TEXT,
                        `vlanId` TEXT,
                        `name` TEXT,
                        `scopeType` TEXT NOT NULL,
                        `scopeTargetId` TEXT,
                        `description` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subnets_projectId` ON `subnets` (`projectId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `port_vlan_memberships` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `portId` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `untaggedVlanId` INTEGER,
                        `taggedVlanIdsJson` TEXT NOT NULL,
                        `nativeVlanId` INTEGER,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_port_vlan_memberships_projectId` ON `port_vlan_memberships` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_port_vlan_memberships_portId` ON `port_vlan_memberships` (`portId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `logical_interfaces` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `ipAddress` TEXT,
                        `subnetCidr` TEXT,
                        `vlanId` INTEGER,
                        `isL3` INTEGER NOT NULL,
                        `macAddress` TEXT,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logical_interfaces_projectId` ON `logical_interfaces` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logical_interfaces_deviceId` ON `logical_interfaces` (`deviceId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lag_groups` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `memberPortIdsJson` TEXT NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_lag_groups_projectId` ON `lag_groups` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_lag_groups_deviceId` ON `lag_groups` (`deviceId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `device_configurations` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `configText` TEXT,
                        `attachmentId` TEXT,
                        `capturedEpochMs` INTEGER NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_device_configurations_projectId` ON `device_configurations` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_device_configurations_deviceId` ON `device_configurations` (`deviceId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wan_vpn_connections` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `providerOrCarrier` TEXT,
                        `bandwidth` TEXT,
                        `localEndpointDeviceId` TEXT,
                        `localEndpointSiteDescription` TEXT,
                        `remoteEndpointDeviceId` TEXT,
                        `remoteEndpointSiteDescription` TEXT,
                        `underlyingAccessId` TEXT,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wan_vpn_connections_projectId` ON `wan_vpn_connections` (`projectId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `video_surveillance_mappings` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `cameraDeviceId` TEXT NOT NULL,
                        `managerDeviceId` TEXT,
                        `externalManagerDescription` TEXT,
                        `channelNumber` INTEGER,
                        `streamUrl` TEXT,
                        `resolution` TEXT,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_surveillance_mappings_projectId` ON `video_surveillance_mappings` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_surveillance_mappings_cameraDeviceId` ON `video_surveillance_mappings` (`cameraDeviceId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_extra_fields` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `targetId` TEXT NOT NULL,
                        `fieldKey` TEXT NOT NULL,
                        `fieldValue` TEXT NOT NULL,
                        `fieldType` TEXT NOT NULL,
                        `classification` TEXT NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_extra_fields_projectId` ON `custom_extra_fields` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_extra_fields_targetId` ON `custom_extra_fields` (`targetId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `power_feeds` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `feedName` TEXT NOT NULL,
                        `feedType` TEXT NOT NULL,
                        `sourceDeviceId` TEXT,
                        `sourceOutletDescription` TEXT,
                        `voltageVolts` INTEGER,
                        `loadVa` REAL,
                        `loadWatts` REAL,
                        `observedRuntimeMinutes` INTEGER,
                        `observedSource` TEXT,
                        `observedEpochMs` INTEGER,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_power_feeds_projectId` ON `power_feeds` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_power_feeds_deviceId` ON `power_feeds` (`deviceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_power_feeds_sourceDeviceId` ON `power_feeds` (`sourceDeviceId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `poe_mappings` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `portId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `standard` TEXT NOT NULL,
                        `allocatedPowerWatts` REAL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poe_mappings_projectId` ON `poe_mappings` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poe_mappings_portId` ON `poe_mappings` (`portId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `document_badges` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `targetId` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isDerived` INTEGER NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_document_badges_projectId` ON `document_badges` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_document_badges_targetId` ON `document_badges` (`targetId`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trash_items` (
                        `id` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `itemId` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `serializedJson` TEXT NOT NULL,
                        `deletedEpochMs` INTEGER NOT NULL,
                        `affectedReferencesSummary` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trash_items_projectId` ON `trash_items` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trash_items_itemId` ON `trash_items` (`itemId`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attachments ADD COLUMN attributionText TEXT DEFAULT NULL")
            }
        }

        /** Contract 1.8: device serial number (F02). */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE devices ADD COLUMN serialNumber TEXT DEFAULT NULL")
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
        )
    }
}
