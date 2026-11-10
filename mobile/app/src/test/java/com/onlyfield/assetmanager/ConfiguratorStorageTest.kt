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
            val p = Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "BU", devices = listOf(device))),
                racks = listOf(Rack(name = "Rack", depthMm = 1000, mountingDepthMm = 900, deviceModelId = rackModel.id)), deviceModels = listOf(model, rackModel, cableModel),
                cables = listOf(Cable(portAId = device.ports.first().id, deviceModelId = cableModel.id)))
            val repository = ProjectRepository(db)
            repository.saveProject(p)
            assertEquals(p, repository.getProjectById(p.id))
        } finally { db.close() }
    }
}
