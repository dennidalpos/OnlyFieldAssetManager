package com.onlyfield.assetmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        BusinessUnitEntity::class,
        SiteEntity::class,
        AreaEntity::class,
        DeviceEntity::class,
        PortEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun inventoryDao(): InventoryDao
}
