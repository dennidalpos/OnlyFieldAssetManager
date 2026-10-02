package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.UUID

class DesktopDocumentAndCartographyTest {

    private fun createSampleProject(): Project {
        val now = System.currentTimeMillis()
        val area = Area(id = "area-1", name = "Sala Server")
        val site = Site(id = "site-1", name = "Sede A", areas = listOf(area))
        return Project(
            id = UUID.randomUUID().toString(),
            name = "Progetto Test Documenti W04",
            createdEpochMs = now,
            updatedEpochMs = now,
            businessUnits = listOf(
                BusinessUnit(
                    id = UUID.randomUUID().toString(),
                    name = "Sede Centrale",
                    sites = listOf(site),
                    devices = listOf(
                        Device(
                            id = "dev-1",
                            siteId = "site-1",
                            areaId = "area-1",
                            technicalName = "SW-CORE-01",
                            category = DeviceCategory.NETWORK_SWITCH,
                            ipAddress = "192.168.1.1"
                        )
                    )
                )
            ),
            racks = listOf(
                Rack(id = "rack-1", name = "RACK-01", heightU = 42)
            ),
            vlans = listOf(
                Vlan(id = "vlan-1", vlanId = 10, name = "MANAGEMENT")
            )
        )
    }

    @Test
    fun testXlsxExportOnDesktop() {
        val project = createSampleProject()
        val filterConfig = ExportFilterConfig()
        val baos = ByteArrayOutputStream()

        DesktopDocumentManager.exportXlsx(project, filterConfig, baos)
        val bytes = baos.toByteArray()

        assertTrue("XLSX output should not be empty", bytes.isNotEmpty())
    }

    @Test
    fun testMarkdownExportOnDesktop() {
        val project = createSampleProject()
        val filterConfig = ExportFilterConfig()
        val baos = ByteArrayOutputStream()

        DesktopDocumentManager.exportMarkdown(project, filterConfig, baos)
        val text = baos.toString(Charsets.UTF_8.name())

        assertTrue("Markdown output should contain project name", text.contains("Progetto Test Documenti W04"))
        assertTrue("Markdown output should contain device name", text.contains("SW-CORE-01"))
    }

    @Test
    fun testCompositePdfExportOnDesktop() {
        val project = createSampleProject()
        val filterConfig = ExportFilterConfig(authorName = "Ingegnere Desktop")
        val selection = ReportSelection()
        val baos = ByteArrayOutputStream()

        DesktopDocumentManager.exportCompositePdf(project, filterConfig, selection, baos)
        val text = baos.toString(Charsets.UTF_8.name())

        assertTrue("Composite PDF output should start with %PDF", text.startsWith("%PDF"))
        assertTrue("Composite PDF should contain author name", text.contains("Ingegnere Desktop"))
        assertTrue("Composite PDF should contain device technical name", text.contains("SW-CORE-01"))
    }

    @Test
    fun testCartographyCoordinateConversionAndUrl() {
        // Rome coordinates: Lat 41.9028, Lon 12.4964, Zoom 15
        val tile = DesktopCartographyManager.lonLatToTileCoord(12.4964, 41.9028, 15)

        assertTrue("Tile X coordinate should be positive", tile.x > 0)
        assertTrue("Tile Y coordinate should be positive", tile.y > 0)
        assertEquals("Zoom level should be 15", 15, tile.zoom)

        val topoUrl = DesktopCartographyManager.getTileUrl(DesktopMapSource.OPEN_TOPO_MAP, tile)
        val cartoUrl = DesktopCartographyManager.getTileUrl(DesktopMapSource.CARTO_DB, tile)

        assertTrue("OpenTopoMap URL should contain zoom 15", topoUrl.contains("/15/"))
        assertTrue("CARTO URL should contain zoom 15", cartoUrl.contains("/15/"))
    }
}
