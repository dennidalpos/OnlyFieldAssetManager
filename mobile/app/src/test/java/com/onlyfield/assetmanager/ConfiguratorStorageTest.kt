package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConfiguratorStorageTest {
    @Test fun roomRoundTripPreservesHardwareAndModelKinds() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val model = DeviceModel(name = "Model", hardware = HardwareSpec(poeBudgetWatts = 370.0), portTemplates = listOf(PortTemplate("P", portCount = 24), PortTemplate("SFP", portCount = 4, connector = "SFP")))
            val d = Device(technicalName = "SW", hardware = model.hardware.copy(portGroups = model.portTemplates), deviceModelId = model.id)
            val device = d.copy(ports = HardwareConfigurator.ports(model.portTemplates, d.id).mapIndexed { n, port -> if (n == 24) port.copy(hardware = port.hardware.copy(opticalModule = "Optic")) else port })
            val rackModel = DeviceModel(name = "Rack model", kind = ObjectKind.RACK, rackDefaults = RackDefaults(42, 1000, 900))
            val cableModel = DeviceModel(name = "Cable model", kind = ObjectKind.CABLE, cableDefaults = CableDefaults())
            val p = Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(BusinessUnit(name = "BU", devices = listOf(device))),
                racks = listOf(Rack(name = "Rack", depthMm = 1000, mountingDepthMm = 900, deviceModelId = rackModel.id)), deviceModels = listOf(model, rackModel, cableModel),
                cables = listOf(Cable(portAId = device.ports.first().id, deviceModelId = cableModel.id)))
            val repository = ProjectRepository(db)
            repository.saveProject(p)
            assertEquals(p, repository.getProjectById(p.id))
        } finally { db.close() }
    }

    @Test fun migration13To14PreservesOriginalPortAndAddsUnknownMetadata() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "cfg-${UUID.randomUUID().toString().take(8)}.db"
        try {
            val schema = Json.parseToJsonElement(File("schemas/com.onlyfield.assetmanager.data.local.AppDatabase/13.json").readText()).jsonObject.getValue("database").jsonObject
            val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
            val projectId = UUID.randomUUID().toString(); val buId = UUID.randomUUID().toString(); val deviceId = UUID.randomUUID().toString(); val portId = UUID.randomUUID().toString()
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                schema.getValue("entities").jsonArray.forEach { item ->
                    val entity = item.jsonObject
                    val table = entity.getValue("tableName").jsonPrimitive.content
                    old.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                    entity["indices"]?.jsonArray.orEmpty().forEach { index -> old.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)) }
                }
                old.execSQL("INSERT INTO projects(id,name,createdEpochMs,updatedEpochMs,isPasswordProtected,objectTypesJson,cableRoutesJson,objectContainmentsJson) VALUES(?,?,1,1,0,'[]','[]','[]')", arrayOf(projectId, "Legacy"))
                old.execSQL("INSERT INTO business_units(id,projectId,name) VALUES(?,?,?)", arrayOf(buId, projectId, "BU"))
                old.execSQL("INSERT INTO devices(id,businessUnitId,technicalName,heightU,rackSide,mountingType,category) VALUES(?,?,?,1,'BOTH','OUT_OF_RACK','PATCH_PANEL')", arrayOf(deviceId, buId, "Panel"))
                old.execSQL("INSERT INTO ports(id,deviceId,name,endpointStatus) VALUES(?,?,?,'DISCONNECTED')", arrayOf(portId, deviceId, "P1"))
                old.version = 13
            }
            val db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().addMigrations(AppDatabase.MIGRATION_13_14).build()
            try {
                val p = ProjectRepository(db).getProjectById(projectId)!!
                val port = p.businessUnits.single().devices.single().ports.single()
                assertEquals(portId, port.id)
                assertNull(port.hardware.side)
                assertTrue(p.panelMappings.isEmpty())
                assertEquals(14, db.openHelper.writableDatabase.version)
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
