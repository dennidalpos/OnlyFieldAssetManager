package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.HardwareConfigurator
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class ConfiguratorExchangeTest {
    private fun project(): Project {
        val model = DeviceModel(name = "HP 24p PoE", objectTypeId = "switch", category = DeviceCategory.NETWORK_SWITCH,
            portTemplates = listOf(PortTemplate("Gi", portCount = 24, connector = "RJ45", poeStandard = PoeStandard.IEEE_802_3AT), PortTemplate("SFP", portCount = 4, mediaType = "Fiber", connector = "SFP")),
            hardware = HardwareSpec(depthMm = 250, poeBudgetWatts = 370.0, redundantPower = true))
        val d = Device(technicalName = "SW1", deviceModelId = model.id, hardware = model.hardware.copy(portGroups = model.portTemplates))
        val device = d.copy(ports = HardwareConfigurator.ports(model.portTemplates, d.id).mapIndexed { n, p -> if (n == 24) p.copy(hardware = p.hardware.copy(opticalModule = "OPTICAL-MODULE")) else p })
        return Project(name = "Test", createdEpochMs = 1, updatedEpochMs = 1, businessUnits = listOf(BusinessUnit(name = "BU", devices = listOf(device))),
            deviceModels = listOf(model, DeviceModel(name = "Rack", kind = ObjectKind.RACK, rackDefaults = RackDefaults(42, 1000, 900))), racks = listOf(Rack(name = "R1", mountingDepthMm = 900)))
    }

    @Test fun packagePreservesHardwarePortIdsAndAllModelKinds() {
        val p = project()
        listOf<String?>(null, "test-password").forEach { password ->
            val bytes = PackageSerializer.exportPackage(p, password = password)
            val result = PackageSerializer.importPackage(bytes, password = password)
            assertTrue(result.validationResult.isValid)
            assertEquals("1.11", result.pkg?.manifest?.formatVersion)
            assertEquals(p, result.pkg?.project)
        }
    }

    @Test fun mergeCarriesHardwareChangesWithStablePorts() {
        val base = project()
        val original = base.businessUnits.single().devices.single()
        val incoming = base.copy(businessUnits = listOf(base.businessUnits.single().copy(devices = listOf(original.copy(hardware = original.hardware.copy(poeBudgetWatts = 500.0))))))
        val local = base.copy(name = "Local")
        val plan = ProjectMerger.merge(base, local, incoming)
        assertTrue(plan.conflicts.isEmpty())
        val result = plan.resolve(emptyMap())
        assertEquals("Local", result.name)
        assertEquals(500.0, result.businessUnits.single().devices.single().hardware.poeBudgetWatts)
        assertEquals(original.ports.map { it.id }, result.businessUnits.single().devices.single().ports.map { it.id })
    }

    @Test fun tabularExportsIncludePortHardware() {
        val p = project()
        val markdown = ByteArrayOutputStream().also { MarkdownExportManager.exportMarkdownToStream(p, ExportFilterConfig(), it) }.toString("UTF-8")
        assertTrue(markdown.contains("OPTICAL-MODULE"))
        assertTrue(markdown.contains("SFP"))
        val xlsx = ByteArrayOutputStream().also { XlsxExportManager.exportXlsxToStream(p, ExportFilterConfig(), it) }.toByteArray()
        val content = java.util.zip.ZipInputStream(xlsx.inputStream()).use { zip ->
            buildString { var entry = zip.nextEntry; while (entry != null) { if (entry.name.endsWith(".xml")) append(zip.readBytes().toString(Charsets.UTF_8)); entry = zip.nextEntry } }
        }
        assertTrue(content.contains("OPTICAL-MODULE"))
        assertTrue(content.contains("RJ45"))
    }
}
