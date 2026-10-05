package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.model.Cable
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class LabelSheetPdfTest {
    @Test
    fun buildsOneLabelPerDeviceRackAndLabelledCable() {
        val devices = (1..25).map { Device(technicalName = "SW-$it") }
        val project = Project(
            name = "Etichette", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = "BU", devices = devices)),
            racks = listOf(Rack(name = "R1")),
            cables = listOf(Cable(codeOrLabel = "CV-1"), Cable())
        )
        val labels = LabelSheetPdf.labelsFor(project)
        assertEquals(27, labels.size)
        assertTrue(labels.all { it.code.toString().startsWith("ofam://${project.id}/") })

        val pdf = ByteArrayOutputStream().also { LabelSheetPdf.write(labels, it) }.toString(Charsets.ISO_8859_1)
        assertTrue(pdf.startsWith("%PDF-1.4"))
        assertTrue(pdf.contains("/Count 2")) // 21 labels per page
        assertTrue(pdf.contains("(SW-25)"))
    }
}
