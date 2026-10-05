package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class VisualHardwareExchangeTest {
    @Test fun packageKeepsLayoutPoePortIdsAndHiddenDimensions() {
        val groups = listOf(PortTemplate("P", portCount = 4, connector = "RJ45", mediaType = "Copper"))
        val device = Device(technicalName = "SW", objectTypeId = "switch", hardware = HardwareSpec(widthMm = 440, depthMm = 280, portGroups = groups,
            portLayouts = listOf(PortLayout(group = "P", rows = 2, order = listOf("4", "1", "2", "3"))),
            portPoeOverrides = listOf(PortPoeOverride(group = "P", key = "4", standard = PoeStandard.IEEE_802_3BT))))
        val p = Project(name = "Visual", createdEpochMs = 1, updatedEpochMs = 1, sites = listOf(Site(name = "Site", devices = listOf(device))))
        val configured = HardwareConfigurator.configure(p, device)
        val imported = PackageSerializer.importPackage(PackageSerializer.exportPackage(configured, emptyMap()))
        assertTrue(imported.validationResult.isValid)
        assertEquals(configured.sites.single().devices.single(), imported.pkg!!.project.sites.single().devices.single())
    }
}
