package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class DocumentSelectionTest {
    private val a = Area(name = "FLOOR_A")
    private val b = Area(name = "FLOOR_B")
    private val other = Area(name = "FLOOR_OTHER")
    private val rack = Rack(name = "RACK_A", areaId = a.id, heightU = 42)
    private fun device(name: String, area: Area?, category: DeviceCategory, rackId: String? = null): Device {
        val d = Device(technicalName = name, areaId = area?.id, category = category, rackId = rackId)
        return d.copy(ports = listOf(Port(deviceId = d.id, name = "$name-PORT")))
    }
    private val chosen = device("SELECTED", null, DeviceCategory.NETWORK_SWITCH, rack.id)
    private val otherFloor = device("OTHER_FLOOR", b, DeviceCategory.NETWORK_SWITCH)
    private val otherCategory = device("OTHER_CATEGORY", a, DeviceCategory.CUSTOM)
    private val outside = device("OUTSIDE", other, DeviceCategory.CUSTOM)
    private val unrelated = device("UNRELATED", other, DeviceCategory.CUSTOM)
    private val site = Site(name = "SITE_A", areas = listOf(a, b), devices = listOf(chosen, otherFloor, otherCategory))
    private val otherSite = Site(name = "SITE_B", areas = listOf(other), devices = listOf(outside, unrelated))
    private val external = Cable(codeOrLabel = "CONTEXT_CABLE", portAId = chosen.ports.single().id, portBId = outside.ports.single().id)
    private fun attachment(d: Device) = Attachment(name = "${d.technicalName}_ATTACHMENT", originalFileName = "a.png", relativePath = "attachments/a.png", targetType = AttachmentTargetType.DEVICE, targetId = d.id)
    private val project = Project(name = "Filter test", createdEpochMs = 0, updatedEpochMs = 0,
        sites = listOf(site, otherSite), racks = listOf(rack), cables = listOf(external),
        attachments = listOf(chosen, otherFloor, otherCategory, outside).map(::attachment),
        vlans = listOf(Vlan(vlanId = 1, name = "GLOBAL_VLAN"),
            Vlan(vlanId = 2, name = "SITE_A_VLAN", scopeType = VlanScopeType.SITE, scopeTargetId = site.id),
            Vlan(vlanId = 3, name = "SITE_B_VLAN", scopeType = VlanScopeType.SITE, scopeTargetId = otherSite.id),
            Vlan(vlanId = 4, name = "OTHER_DEVICE_VLAN", scopeType = VlanScopeType.DEVICE, scopeTargetId = otherFloor.id)),
        powerFeeds = listOf(chosen, otherFloor, outside).map { PowerFeed(deviceId = it.id, feedName = "${it.technicalName}_POWER") },
        documentBadges = listOf(chosen, otherFloor, outside).map { DocumentBadge(targetType = "DEVICE", targetId = it.id, label = "${it.technicalName}_BADGE") })
    private val filters = listOf(ExportFilterConfig(selectedSiteId = site.id), ExportFilterConfig(selectedAreaId = a.id),
        ExportFilterConfig(selectedCategory = DeviceCategory.NETWORK_SWITCH),
        ExportFilterConfig(selectedSiteId = site.id, selectedAreaId = a.id, selectedCategory = DeviceCategory.NETWORK_SWITCH))

    @Test fun scopeUsesInheritedFloorAndFiltersAllCatalogs() {
        val scope = DocumentSelection(project, filters.last())
        val selected = scope.project
        assertEquals(listOf(chosen.id), selected.sites.flatMap { it.devices }.map { it.id })
        assertEquals(a.id, selected.sites.single().devices.single().areaId)
        assertEquals(listOf(a.id), selected.sites.flatMap { it.areas }.map { it.id })
        assertEquals(listOf(rack.id), selected.racks.map { it.id })
        assertEquals(listOf("GLOBAL_VLAN", "SITE_A_VLAN"), selected.vlans.map { it.name })
        assertEquals(listOf("SELECTED_ATTACHMENT"), selected.attachments.map { it.name })
        assertEquals(listOf("SELECTED_POWER"), selected.powerFeeds.map { it.feedName })
        assertEquals(listOf("SELECTED_BADGE"), selected.documentBadges.map { it.label })
        assertTrue(selected.cables.isEmpty())
        assertEquals(setOf(chosen.id, outside.id), scope.physicalContext.sites.flatMap { it.devices }.map { it.id }.toSet())
        assertEquals(listOf(external.id), scope.paths.flatMap { it.segments }.map { it.cable.id })
    }

    @Test fun markdownAndXlsxRespectSiteFloorAndCategory() {
        for (filter in filters) {
            val selected = DocumentSelection(project, filter).deviceIds
            val md = ByteArrayOutputStream().also { MarkdownExportManager.exportMarkdownToStream(project, filter, it) }.toString("UTF-8")
            val xlsx = ByteArrayOutputStream().also { XlsxExportManager.exportXlsxToStream(project, filter, it) }.toByteArray()
            val sheets = mutableMapOf<String, String>()
            ZipInputStream(ByteArrayInputStream(xlsx)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) { sheets[entry.name] = zip.readBytes().toString(Charsets.UTF_8); entry = zip.nextEntry }
            }
            for (d in listOf(chosen, otherFloor, otherCategory, outside)) {
                val included = d.id in selected
                val markdownName = d.technicalName.replace("_", "\\_")
                assertEquals("Markdown inventory ${d.technicalName}: $filter", included, md.contains("${markdownName}-PORT"))
                assertEquals("XLSX inventory ${d.technicalName}: $filter", included, sheets.getValue("xl/worksheets/sheet1.xml").contains(d.technicalName))
                assertEquals(included, md.contains("${markdownName}\\_ATTACHMENT"))
                assertEquals(included, sheets.getValue("xl/worksheets/sheet5.xml").contains("${d.technicalName}_ATTACHMENT"))
            }
            assertFalse(md.contains("SITE\\_B\\_VLAN"))
            assertFalse(sheets.getValue("xl/worksheets/sheet3.xml").contains("SITE_B_VLAN"))
        }
    }

    @Test fun confidentialPlanIsRemovedBeforeDrawing() {
        val plan = Attachment(name = "Private plan", originalFileName = "p.pdf", relativePath = "attachments/p.pdf", classification = AttachmentClassification.CONFIDENTIAL, targetType = AttachmentTargetType.AREA, targetId = a.id)
        val p = project.copy(sites = listOf(site.copy(areas = listOf(a.copy(floorplanAttachmentId = plan.id)))) , attachments = listOf(plan))
        assertNull(DocumentSelection(p, ExportFilterConfig()).project.sites.single().areas.single().floorplanAttachmentId)
        assertEquals(plan.id, DocumentSelection(p, ExportFilterConfig(includeConfidential = true)).project.sites.single().areas.single().floorplanAttachmentId)
    }
}
