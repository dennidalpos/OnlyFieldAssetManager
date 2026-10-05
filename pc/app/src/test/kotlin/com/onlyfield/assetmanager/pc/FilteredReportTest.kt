package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.pc.report.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class FilteredReportTest {
    private val a = Area(name = "SELECTED_FLOOR")
    private val b = Area(name = "EXCLUDED_FLOOR")
    private val chosen = Device(technicalName = "SELECTED_SWITCH", areaId = a.id, category = DeviceCategory.NETWORK_SWITCH)
    private val excludedFloor = Device(technicalName = "EXCLUDED_SWITCH", areaId = b.id, category = DeviceCategory.NETWORK_SWITCH)
    private val excludedCategory = Device(technicalName = "EXCLUDED_CAMERA", areaId = a.id, category = DeviceCategory.CUSTOM)
    private val excludedSite = Device(technicalName = "EXCLUDED_SITE_DEVICE", category = DeviceCategory.CUSTOM)
    private val site = Site(name = "Selected site", areas = listOf(a, b), devices = listOf(chosen, excludedFloor, excludedCategory))
    private val other = Site(name = "Excluded site", devices = listOf(excludedSite))
    private val p = Project(name = "Report", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(site, other),
        vlans = listOf(Vlan(vlanId = 2, name = "SELECTED_VLAN", scopeType = VlanScopeType.SITE, scopeTargetId = site.id),
            Vlan(vlanId = 3, name = "EXCLUDED_VLAN", scopeType = VlanScopeType.SITE, scopeTargetId = other.id)))

    @Test fun pdfAndFiguresApplyAllThreeFilters() {
        val filters = listOf(ExportFilterConfig(selectedSiteId = site.id), ExportFilterConfig(selectedAreaId = a.id),
            ExportFilterConfig(selectedCategory = DeviceCategory.NETWORK_SWITCH),
            ExportFilterConfig(selectedSiteId = site.id, selectedAreaId = a.id, selectedCategory = DeviceCategory.NETWORK_SWITCH))
        for (filter in filters) {
            val bytes = ByteArrayOutputStream().also { DesktopDocumentManager.exportCompositePdf(p, filter, ReportSelection(), it) }.toByteArray()
            val text = Loader.loadPDF(bytes).use { PDFTextStripper().getText(it) }
            assertTrue(text.contains(chosen.technicalName))
            assertFalse(text.contains(excludedSite.technicalName))
            assertFalse(text.contains("EXCLUDED_VLAN"))
            if (filter.selectedAreaId != null) assertFalse(text.contains(excludedFloor.technicalName))
            if (filter.selectedCategory != null) assertFalse(text.contains(excludedCategory.technicalName))
            val figures = ReportContent.build(p, filter, ReportSelection()).filterIsInstance<ReportLine.Figure>()
            if (filter.selectedAreaId != null) assertFalse(figures.any { (it.figure as? ReportFigure.FloorPlan)?.areaId == b.id })
            val topology = figures.mapNotNull { it.figure as? ReportFigure.Topology }.single()
            assertFalse(topology.context.sites.flatMap { it.devices }.any { it.id == excludedSite.id })
        }
    }
}
