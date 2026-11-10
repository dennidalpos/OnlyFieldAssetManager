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
import java.io.File
import kotlinx.serialization.json.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ObjectMapStorageTest {
    private fun legacySchemas(): List<String> {
        val schema = Json.parseToJsonElement(File("schemas/com.onlyfield.assetmanager.data.local.AppDatabase/13.json").readText()).jsonObject.getValue("database").jsonObject
        return schema.getValue("entities").jsonArray.flatMap { item ->
            val entity = item.jsonObject
            val table = entity.getValue("tableName").jsonPrimitive.content
            listOf(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)) +
                entity["indices"]?.jsonArray.orEmpty().map { it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table) }
        }
    }

    @Test fun roomPreservesCatalogGeometryAndCableDeviceEndpoints() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = ProjectRepository(db)
            val area = Area(name = "Terra", floorplanPageIndex = 2)
            val type = ObjectType(name = "Gateway")
            val device = Device(technicalName = "GW", areaId = area.id, objectTypeId = type.id)
            val cable = Cable(deviceAId = device.id, objectTypeId = "coax-cable")
            val p = Project(name = "Sito", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "BU", areas = listOf(area), devices = listOf(device))), objectTypes = listOf(type), cables = listOf(cable), cableRoutes = listOf(CableRoute(cableId = cable.id, areaId = area.id)))
            repo.saveProject(p)
            assertEquals(p, repo.getProjectById(p.id))
        } finally { db.close() }
    }
}
