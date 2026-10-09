package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.model.Annotation
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CorruptStoredDataTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private val area = Area(name = "Floor")
    private val deviceId = java.util.UUID.randomUUID().toString()
    private val observation = Observation(source = "Synthetic", timestampEpochMs = 0, status = ObservationStatus.VERIFIED)
    private val port = Port(deviceId = deviceId, name = "P1", observation = observation)
    private val device = Device(id = deviceId, technicalName = "Switch", ports = listOf(port), observation = observation)
    private val project = Project(name = "Stored data", createdEpochMs = 0, updatedEpochMs = 0,
        sites = listOf(Site(name = "Building", areas = listOf(area), devices = listOf(device))),
        racks = listOf(Rack(name = "Rack")), deviceModels = listOf(DeviceModel(name = "Model")),
        credentials = listOf(Credential(username = "dummy", secret = "dummy")),
        attachments = listOf(Attachment(name = "Media", originalFileName = "x.png", relativePath = "x.png", targetType = AttachmentTargetType.DEVICE, targetId = deviceId)),
        annotations = listOf(Annotation(areaId = area.id, x1Ratio = 0.5f, y1Ratio = 0.5f)),
        floorplanPlacements = listOf(FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE, targetId = deviceId, xRatio = 0.5f, yRatio = 0.5f)),
        cables = listOf(Cable(observation = observation)), vlans = listOf(Vlan(vlanId = 10, name = "LAN")),
        subnets = listOf(Subnet(cidrBlock = "10.0.0.0/24")),
        portVlanMemberships = listOf(PortVlanMembership(portId = port.id, taggedVlanIds = listOf(10))),
        lagGroups = listOf(LagGroup(deviceId = deviceId, name = "LAG", memberPortIds = listOf(port.id))),
        wanVpnConnections = listOf(WanVpnConnection(name = "WAN")),
        customExtraFields = listOf(CustomExtraField(targetType = "DEVICE", targetId = deviceId, fieldKey = "Key", fieldValue = "Value")),
        powerFeeds = listOf(PowerFeed(deviceId = deviceId, feedName = "A")), poeMappings = listOf(PoeMapping(portId = port.id)),
        documentBadges = listOf(DocumentBadge(targetType = "DEVICE", targetId = deviceId, label = "Badge")))

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        repository = ProjectRepository(db)
        runBlocking { repository.saveProject(project) }
    }
    @After fun close() { db.close() }
    private fun value(table: String, column: String): String = db.openHelper.readableDatabase.query("SELECT $column FROM $table").use {
        assertTrue(it.moveToFirst()); it.getString(0)
    }
    private suspend fun corruptAndRetry(table: String, column: String) {
        val original = value(table, column)
        db.openHelper.writableDatabase.execSQL("UPDATE $table SET $column = ?", arrayOf("UNKNOWN_OR_CORRUPT"))
        assertNotNull("$table.$column must fail", runCatching { repository.getProjectById(project.id) }.exceptionOrNull())
        assertEquals("UNKNOWN_OR_CORRUPT", value(table, column))
        assertEquals("Stored data", db.projectDao().getProjectById(project.id)!!.name)
        db.openHelper.writableDatabase.execSQL("UPDATE $table SET $column = ?", arrayOf(original))
        assertEquals(project, repository.getProjectById(project.id))
    }
    @Test fun malformedJsonIsRejectedWithoutReplacingItsRows() = runBlocking {
        corruptAndRetry("port_vlan_memberships", "taggedVlanIdsJson")
        corruptAndRetry("lag_groups", "memberPortIdsJson")
    }
    @Test fun unknownEnumsCannotBecomeDefaults() = runBlocking {
        // Exercise the retained row-based model format as well as current entities.
        db.openHelper.writableDatabase.execSQL("UPDATE device_models SET configurationJson = '{}'")
        for ((table, columns) in mapOf(
            "racks" to listOf("numberingDirection"), "device_models" to listOf("category"), "credentials" to listOf("type"),
            "devices" to listOf("obsStatus", "rackSide", "mountingType", "category", "operationalStatus"),
            "ports" to listOf("obsStatus", "endpointStatus"), "cables" to listOf("obsStatus", "medium"),
            "attachments" to listOf("fileType", "classification", "targetType"), "annotations" to listOf("type", "classification"),
            "floorplan_placements" to listOf("targetType"), "vlans" to listOf("scopeType"), "subnets" to listOf("scopeType"),
            "port_vlan_memberships" to listOf("mode"), "lag_groups" to listOf("mode"), "wan_vpn_connections" to listOf("type"),
            "custom_extra_fields" to listOf("fieldType", "classification"), "power_feeds" to listOf("feedType"),
            "poe_mappings" to listOf("role", "standard"), "document_badges" to listOf("category"))) {
            for (column in columns) corruptAndRetry(table, column)
        }
    }
}
