package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.scan.CodeLookup
import com.onlyfield.assetmanager.core.scan.CodeMatch
import com.onlyfield.assetmanager.core.scan.LabelCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeLookupTest {
    private val port = Port(id = "p1", deviceId = "d1", name = "Gi1/0/1", label = "PP-A-01")
    private val index = ProjectIndex(
        Project(
            name = "P", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(
                Site(
                    name = "BU",
                    devices = listOf(
                        Device(id = "d1", technicalName = "SW-01", physicalLabel = "A-001", ports = listOf(port)),
                        Device(id = "d2", technicalName = "SW-02", serialNumber = "FOC1234X0AB"),
                    )
                )
            ),
            cables = listOf(Cable(id = "c1", codeOrLabel = "CV-0042"))
        )
    )

    @Test
    fun matchesSerialLabelCableAndPortIgnoringCase() {
        assertEquals("d2", (CodeLookup.find(index, " foc1234x0ab ") as CodeMatch.DeviceMatch).device.id)
        assertEquals("Etichetta", (CodeLookup.find(index, "A-001") as CodeMatch.DeviceMatch).field)
        assertEquals("c1", (CodeLookup.find(index, "CV-0042") as CodeMatch.CableMatch).cable.id)
        assertEquals("p1", (CodeLookup.find(index, "PP-A-01") as CodeMatch.PortMatch).port.port.id)
        assertTrue(CodeLookup.find(index, "SCONOSCIUTO") is CodeMatch.NotFound)
    }

    @Test
    fun resolvesOwnQrLabels() {
        val pid = index.project.id
        assertEquals(LabelCode(pid, LabelCode.Type.DEVICE, "d1"), LabelCode.parse("ofam://$pid/device/d1"))
        assertEquals(null, LabelCode.parse("ofam://solo/due"))
        assertEquals("d1", (CodeLookup.find(index, LabelCode(pid, LabelCode.Type.DEVICE, "d1").toString()) as CodeMatch.DeviceMatch).device.id)
        assertEquals("c1", (CodeLookup.find(index, "OFAM://$pid/CABLE/c1") as CodeMatch.CableMatch).cable.id)
        assertTrue(CodeLookup.find(index, "ofam://altro/device/d1") is CodeMatch.OtherProject)
        assertTrue(CodeLookup.find(index, "ofam://$pid/device/sparito") is CodeMatch.NotFound)
    }
}
