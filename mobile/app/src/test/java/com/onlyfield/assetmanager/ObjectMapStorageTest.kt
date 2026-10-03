package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ObjectMapStorageTest {
    @Test fun versionTwelveRackMembershipMigratesAndTrashSurvivesSaves() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "containment-${UUID.randomUUID()}.db"
        val area = Area(name = "Terra")
        val rack = Rack(name = "R1", areaId = area.id)
        val device = Device(technicalName = "SW", rackId = rack.id, positionU = 7)
        val p = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, racks = listOf(rack),
            businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area), devices = listOf(device))))
        try {
            val template = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
            val schemas = try {
                template.openHelper.writableDatabase.query("SELECT sql FROM sqlite_master WHERE sql IS NOT NULL AND type IN ('table', 'index') AND name NOT IN ('android_metadata', 'room_master_table') ORDER BY type DESC").use { c ->
                    buildList { while (c.moveToNext()) add(c.getString(0)) }
                }
            } finally { template.close() }
            val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                schemas.forEach { old.execSQL(it.replace(", `objectContainmentsJson` TEXT NOT NULL", "").replace(", `containmentMetadataJson` TEXT", "")) }
                old.execSQL("INSERT INTO projects (id,name,createdEpochMs,updatedEpochMs,isPasswordProtected,objectTypesJson,cableRoutesJson) VALUES (?,?,?,?,0,'[]','[]')", arrayOf<Any>(p.id, p.name, 1L, 1L))
                val bu = p.businessUnits.single()
                old.execSQL("INSERT INTO business_units (id,projectId,name) VALUES (?,?,?)", arrayOf(bu.id, p.id, bu.name))
                old.execSQL("INSERT INTO areas (id,businessUnitId,name,floorplanPageIndex) VALUES (?,?,?,0)", arrayOf(area.id, bu.id, area.name))
                old.execSQL("INSERT INTO racks (id,projectId,name,areaId,heightU,numberingDirection) VALUES (?,?,?,?,42,'BOTTOM_TO_TOP')", arrayOf(rack.id, p.id, rack.name, area.id))
                old.execSQL("INSERT INTO devices (id,businessUnitId,technicalName,rackId,positionU,heightU,rackSide,mountingType,category) VALUES (?,?,?,?,7,1,'BOTH','RACK_MOUNT','CUSTOM')", arrayOf(device.id, bu.id, device.technicalName, rack.id))
                old.version = 12
            }
            val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().addMigrations(AppDatabase.MIGRATION_12_13).build()
            try {
                val repo = ProjectRepository(upgraded)
                val actual = repo.getProjectById(p.id)!!
                assertEquals(ObjectRef(PlacementTargetType.RACK, rack.id), ObjectHierarchy.parent(actual, ObjectRef(PlacementTargetType.DEVICE, device.id)))
                assertEquals(7, actual.businessUnits.single().devices.single().positionU)
                assertEquals(13, upgraded.openHelper.writableDatabase.version)
                val trash = repo.moveToTrash(p.id, "RACK", rack.id)!!
                repo.saveProject(repo.getProjectById(p.id)!!.copy(name = "Aggiornato"))
                assertEquals(trash, repo.getTrashItems(p.id).single())
                assertTrue(repo.restoreFromTrash(p.id, trash.id))
                val restored = repo.getProjectById(p.id)!!
                assertEquals(rack.id, restored.businessUnits.single().devices.single().rackId)
                assertEquals(7, restored.businessUnits.single().devices.single().positionU)
            } finally { upgraded.close() }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun roomPreservesCatalogGeometryAndCableDeviceEndpoints() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = ProjectRepository(db)
            val area = Area(name = "Terra", floorplanPageIndex = 2)
            val type = ObjectType(name = "Gateway")
            val device = Device(technicalName = "GW", areaId = area.id, objectTypeId = type.id)
            val cable = Cable(deviceAId = device.id, objectTypeId = "coax-cable")
            val p = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area), devices = listOf(device))), objectTypes = listOf(type), cables = listOf(cable), cableRoutes = listOf(CableRoute(cableId = cable.id, areaId = area.id)))
            repo.saveProject(p)
            assertEquals(p, repo.getProjectById(p.id))
        } finally { db.close() }
    }
    @Test fun migrationFromElevenPreservesRowsAndAddsEmptyMapDefaults() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        val p = Project(name = "Esistente", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(BusinessUnit(name = "BU", devices = listOf(Device(technicalName = "SW", serialNumber = "S123")))))
        try {
            val template = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
            val schemas = try {
                template.openHelper.writableDatabase.query("SELECT sql FROM sqlite_master WHERE sql IS NOT NULL AND type IN ('table', 'index') AND name NOT IN ('android_metadata', 'room_master_table') ORDER BY type DESC").use { c ->
                    buildList { while (c.moveToNext()) add(c.getString(0)) }
                }
            } finally { template.close() }
            val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                schemas.forEach { schema ->
                    old.execSQL(schema.replace(", `objectTypesJson` TEXT NOT NULL", "").replace(", `cableRoutesJson` TEXT NOT NULL", "")
                        .replace(", `objectContainmentsJson` TEXT NOT NULL", "").replace(", `containmentMetadataJson` TEXT", "")
                        .replace(", `objectTypeId` TEXT", "").replace("`objectTypeId` TEXT, ", "").replace("`deviceAId` TEXT, ", "").replace("`deviceBId` TEXT, ", ""))
                }
                old.execSQL("INSERT INTO projects (id,name,createdEpochMs,updatedEpochMs,isPasswordProtected) VALUES (?,?,?,?,0)", arrayOf<Any>(p.id, p.name, 1L, 1L))
                val bu = p.businessUnits.single()
                old.execSQL("INSERT INTO business_units (id,projectId,name) VALUES (?,?,?)", arrayOf(bu.id, p.id, bu.name))
                val d = bu.devices.single()
                old.execSQL("INSERT INTO devices (id,businessUnitId,technicalName,heightU,rackSide,mountingType,category,serialNumber) VALUES (?,?,?,1,'BOTH','OUT_OF_RACK','CUSTOM',?)", arrayOf(d.id, bu.id, d.technicalName, d.serialNumber))
                old.version = 11
            }
            val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().addMigrations(AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13).build()
            try {
                val actual = ProjectRepository(upgraded).getProjectById(p.id)!!
                assertEquals(p, actual)
                assertTrue(actual.objectTypes.isEmpty()); assertTrue(actual.cableRoutes.isEmpty())
                assertEquals(13, upgraded.openHelper.writableDatabase.version)
            } finally { upgraded.close() }
        } finally { context.deleteDatabase(name) }
    }
}
