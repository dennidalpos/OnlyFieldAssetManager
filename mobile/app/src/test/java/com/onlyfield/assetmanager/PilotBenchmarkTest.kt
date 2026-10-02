package com.onlyfield.assetmanager

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.AttachmentType
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Cable
import com.onlyfield.assetmanager.core.model.CableMedium
import com.onlyfield.assetmanager.core.model.Credential
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.model.Vlan
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PilotBenchmarkTest {

    @Test
    fun testPilotBenchmarkPerformanceAndIntegrity() {
        // Build Pilot Volume: 5 Racks, 100 Devices, Cables, VLANs, Credentials
        val projectId = UUID.randomUUID().toString()
        val racks = (1..5).map { i ->
            Rack(id = UUID.randomUUID().toString(), name = "Rack 0$i", heightU = 42)
        }

        val devices = (1..100).map { i ->
            val devId = UUID.randomUUID().toString()
            val rack = racks[(i - 1) % 5]
            val ports = (1..24).map { p -> Port(id = UUID.randomUUID().toString(), deviceId = devId, name = "eth$p", label = "Port $p") }
            Device(
                id = devId,
                technicalName = "sw-core-$i",
                physicalLabel = "SW-CORE-$i",
                ipAddress = "10.10.${(i / 25) + 1}.${(i % 25) + 1}",
                macAddress = "00:11:22:33:44:%02X".format(i),
                rackId = rack.id,
                positionU = ((i - 1) % 40) + 1,
                heightU = 1,
                category = DeviceCategory.NETWORK_SWITCH,
                ports = ports,
                observation = Observation(source = "Rilievo Tecnico Pilota", timestampEpochMs = System.currentTimeMillis(), status = ObservationStatus.VERIFIED)
            )
        }

        val bu = BusinessUnit(
            id = UUID.randomUUID().toString(),
            name = "Business Unit Pilota Milano",
            code = "BU-MIL-01",
            devices = devices
        )

        val vlans = (1..20).map { v ->
            Vlan(id = UUID.randomUUID().toString(), vlanId = 100 + v, name = "VLAN_DATA_$v")
        }

        val credentials = (1..5).map { c ->
            Credential(id = UUID.randomUUID().toString(), username = "admin_pilota_$c", secret = "SecretP@ss$c")
        }

        val cables = (1..50).map { k ->
            val devA = devices[k * 2 - 2]
            val devB = devices[k * 2 - 1]
            Cable(
                id = UUID.randomUUID().toString(),
                codeOrLabel = "CABLE-PILOT-$k",
                portAId = devA.ports[0].id,
                portBId = devB.ports[0].id,
                medium = CableMedium.ETHERNET_COPPER
            )
        }

        val attachment = Attachment(
            id = UUID.randomUUID().toString(),
            name = "Sfondo Cartografico Pilota",
            originalFileName = "map_pilota.png",
            fileType = AttachmentType.IMAGE,
            mimeType = "image/png",
            relativePath = "attachments/map_pilota.png",
            classification = AttachmentClassification.SHAREABLE,
            attributionText = "© OpenTopoMap (CC-BY-SA), © OpenStreetMap contributors"
        )

        val pilotProject = Project(
            id = projectId,
            name = "Progetto Pilota Collaudo A13",
            createdEpochMs = System.currentTimeMillis(),
            updatedEpochMs = System.currentTimeMillis(),
            businessUnits = listOf(bu),
            racks = racks,
            vlans = vlans,
            credentials = credentials,
            cables = cables,
            attachments = listOf(attachment)
        )

        // 1. Model Validation
        val startValMs = System.currentTimeMillis()
        val valResult = ModelValidator.validateProject(pilotProject)
        val valDurationMs = System.currentTimeMillis() - startValMs

        assertTrue("Validazione pilota riuscita", valResult.isValid)
        assertTrue("Tempo validazione < 500 ms", valDurationMs < 500)

        // 2. Export Package Benchmark
        val attachmentsMap = mapOf("attachments/map_pilota.png" to "fake_image_bytes_pilota".toByteArray(Charsets.UTF_8))
        val startExportMs = System.currentTimeMillis()
        val zipBytes = PackageSerializer.exportPackage(pilotProject, attachmentsMap, password = "PilotaPassword123!")
        val exportDurationMs = System.currentTimeMillis() - startExportMs

        assertTrue("Dimensione ZIP generata > 0", zipBytes.isNotEmpty())
        assertTrue("Tempo esportazione cifrata < 2000 ms", exportDurationMs < 2000)

        // 3. Import Package Benchmark
        val startImportMs = System.currentTimeMillis()
        val importResult = PackageSerializer.importPackage(zipBytes, password = "PilotaPassword123!")
        val importDurationMs = System.currentTimeMillis() - startImportMs

        assertNotNull("Importazione riuscita", importResult.pkg)
        assertTrue("Tempo importazione cifrata < 2000 ms", importDurationMs < 2000)

        val reloadedProject = importResult.pkg!!.project
        assertEquals(1, reloadedProject.businessUnits.size)
        assertEquals(100, reloadedProject.businessUnits[0].devices.size)
        assertEquals(5, reloadedProject.racks.size)
        assertEquals(20, reloadedProject.vlans.size)
        assertEquals(50, reloadedProject.cables.size)
        assertEquals("© OpenTopoMap (CC-BY-SA), © OpenStreetMap contributors", reloadedProject.attachments[0].attributionText)
    }
}
