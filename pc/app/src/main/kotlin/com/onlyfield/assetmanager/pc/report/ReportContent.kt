package com.onlyfield.assetmanager.pc.report

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.display.EntityTypeLabels
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.ConnectionState
import com.onlyfield.assetmanager.core.model.MapScene
import com.onlyfield.assetmanager.core.model.isPassive
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.OperationalStatus
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.core.model.effectiveStatus
import java.text.SimpleDateFormat
import java.util.Date

/** Printed or PDF report line. */
sealed interface ReportLine {
    val text: String

    data class Title(override val text: String) : ReportLine
    data class Meta(override val text: String) : ReportLine
    data class Heading(override val text: String) : ReportLine
    data class SubHeading(override val text: String) : ReportLine
    data class Item(override val text: String, val indent: Int = 0) : ReportLine
    data object Spacer : ReportLine { override val text = "" }
    /** Drawing between text lines; [text] is its caption. */
    data class Figure(override val text: String, val figure: ReportFigure) : ReportLine
    /** Table row; [widths] are fractions of the text width. */
    data class Row(val cells: List<String>, val widths: List<Float>, val header: Boolean = false) : ReportLine {
        override val text get() = cells.joinToString(" | ")
    }
}

sealed interface ReportFigure {
    data class FloorPlan(val areaId: String) : ReportFigure
    data class RackElevation(val rackId: String) : ReportFigure
    data class Topology(val context: Project, val selectedDeviceIds: Set<String>, val foldEndpoints: Boolean) : ReportFigure
}

/** Builds selected report content without credentials. */
object ReportContent {

    fun build(project: Project, filter: ExportFilterConfig, selection: ReportSelection, now: Date = Date(), i18n: Messages = Messages()): List<ReportLine> {
        val scope = com.onlyfield.assetmanager.exchange.DocumentSelection(project, filter)
        val selectedProject = scope.project
        val index = ProjectIndex(selectedProject)
        val lines = mutableListOf<ReportLine>()
        fun heading(text: String) { lines += ReportLine.Spacer; lines += ReportLine.Heading(text) }
        fun item(text: String, indent: Int = 0) { lines += ReportLine.Item(text, indent) }

        lines += ReportLine.Title(filter.titleOverride?.ifBlank { null } ?: i18n.text("text.ba220bc18a31", selectedProject.name))
        lines += ReportLine.Meta(i18n.text("text.b1b3e27e33a1", filter.authorName, SimpleDateFormat("dd/MM/yyyy HH:mm", i18n.locale).format(now)))
        selectedProject.description?.takeIf { it.isNotBlank() }?.let { lines += ReportLine.Meta(it) }
        lines += ReportLine.Meta(
            i18n.text("text.9046ee10595c", index.devices.size, selectedProject.racks.size, selectedProject.cables.size) +
                i18n.text("text.fde3438fc2a1", selectedProject.vlans.size, selectedProject.powerFeeds.size)
        )

        if (selection.includeInventoryTable) {
            heading(i18n.text("text.dae8f6194460"))
            for (site in selectedProject.sites) {
                if (site.devices.isEmpty()) continue
                lines += ReportLine.SubHeading(site.name)
                site.devices.groupBy { it.areaId }.forEach { (areaId, devices) ->
                    item(index.areaName(areaId, i18n.text("text.38426bccdab9")))
                    devices.sortedBy { it.technicalName }.forEach { d ->
                        val extra = listOfNotNull(
                            d.category.toDisplayString(i18n = i18n),
                            d.observation.effectiveStatus().toDisplayString(i18n),
                            d.operationalStatus.takeIf { it != OperationalStatus.IN_SERVICE }?.toDisplayString(i18n),
                            d.ipAddress,
                            d.rackId?.let { i18n.text("text.4d2d924e8402", index.rackName(it)) + (d.positionU?.let { u -> i18n.text("text.bf28e779d560", u) } ?: "") },
                            d.physicalLabel?.let { i18n.text("text.49ac7ff5098a", it) },
                            i18n.plural("text.53a2e3b94696", d.ports.size).takeIf { d.ports.isNotEmpty() }
                        )
                        item("${d.technicalName} — ${extra.joinToString(" · ")}", indent = 1)
                    }
                }
            }
        }

        if (selection.includeFloorPlans) {
            // Floors with objects or a plan: the drawing places objects and cables as on the map.
            val floors = selectedProject.sites.flatMap { site -> site.areas.map { site to it } }
                .filter { (_, area) -> area.floorplanAttachmentId != null || MapScene.area(selectedProject, area.id).nodes.isNotEmpty() }
            if (floors.isNotEmpty()) {
                heading(i18n.text("report.floorPlans"))
                floors.forEach { (site, area) -> lines += ReportLine.Figure("${site.name} › ${area.name}", ReportFigure.FloorPlan(area.id)) }
            }
        }

        if (selection.includeRackCards && selectedProject.racks.isNotEmpty()) {
            heading(i18n.text("text.4cd265c2b8c6"))
            for (rack in selectedProject.racks) {
                val caption = i18n.text("text.f0f63aaad4b6", rack.name, rack.heightU, index.areaName(rack.areaId, i18n.text("text.1abc7243c3dd")))
                val mounted = index.devices.filter { it.rackId == rack.id }
                if (mounted.isEmpty()) { lines += ReportLine.SubHeading(caption); item(i18n.text("text.4a1ac9701d21"), 1); continue }
                lines += ReportLine.Figure(caption, ReportFigure.RackElevation(rack.id))
                mounted.forEach { d ->
                    val position = d.positionU?.let { "U$it" } ?: i18n.text("text.3fc3745f990b")
                    item("$position: ${d.technicalName} · ${d.observation.effectiveStatus().toDisplayString(i18n)}", 1)
                }
            }
        }

        if (selection.includePaths) {
            val paths = scope.paths
            if (paths.isNotEmpty()) {
                heading(i18n.text("report.paths"))
                val widths = listOf(.2f, .3f, .2f, .2f, .1f)
                lines += ReportLine.Row(listOf("report.pathFrom", "report.pathVia", "report.pathTo", "report.pathCables", "report.pathState").map(i18n::text), widths, header = true)
                paths.forEach { path ->
                    fun end(i: Int) = path.stations[i].let { s ->
                        val port = (if (i == 0) s.ports.firstOrNull() else s.ports.lastOrNull())?.name
                        listOfNotNull(s.device?.technicalName ?: "?", port).joinToString("/") + if (path.openEnd(i)) " (${i18n.text("path.openEnd")})" else ""
                    }
                    val via = path.stations.drop(1).dropLast(1).joinToString(" → ") { s -> "${s.device?.technicalName ?: "?"} ${s.ports.joinToString("→") { it.name }}" }
                    val state = i18n.text(when (path.state) {
                        ConnectionState.COMPLETE -> "config.complete"; ConnectionState.CONFLICT -> "config.conflict"
                        ConnectionState.INCOMPLETE -> "config.incomplete"; ConnectionState.AVAILABLE -> "config.available"
                    })
                    lines += ReportLine.Row(listOf(end(0), via.ifBlank { "—" }, end(path.stations.lastIndex), path.segments.joinToString(", ") { it.label }, state), widths)
                }
            }
        }

        if (selection.includeTopology && index.devices.any { !it.isPassive() }) {
            heading(i18n.text("report.topology"))
            lines += ReportLine.Figure(i18n.text("report.topologyCaption"), ReportFigure.Topology(scope.physicalContext, scope.deviceIds,
                filter.selectedSiteId == null && filter.selectedAreaId == null && filter.selectedCategory == null))
        }

        if (selection.includeCablingAndPorts && (index.ports.isNotEmpty() || selectedProject.cables.isNotEmpty() || selectedProject.panelMappings.isNotEmpty())) {
            heading(i18n.text("text.3b40d8bd6081"))
            index.devices.forEach { device -> device.ports.forEach { port ->
                item("${device.technicalName} › ${port.name}: ${port.endpointStatus.toDisplayString(i18n)} · ${port.observation.effectiveStatus().toDisplayString(i18n)}")
            } }
            selectedProject.cables.forEach { c ->
                val details = listOfNotNull(c.medium.toDisplayString(i18n = i18n), c.observation.effectiveStatus().toDisplayString(i18n), c.lengthValue?.let { "$it ${c.lengthUnit ?: "m"}" }, c.color)
                item("${c.codeOrLabel ?: i18n.text("text.89dbe18e8407")}: ${index.portLabel(c.portAId, "libero")} <-> ${index.portLabel(c.portBId, "libero")} (${details.joinToString(", ")})")
            }
            if (selectedProject.panelMappings.isNotEmpty()) {
                lines += ReportLine.SubHeading(i18n.text("text.66cdda39c617"))
                selectedProject.panelMappings.forEach { m -> item("${index.portLabel(m.portAId)} <-> ${index.portLabel(m.portBId, i18n.text("common.none"))}") }
            }
        }

        if (selection.includeLogicalNetwork && (selectedProject.vlans.isNotEmpty() || selectedProject.subnets.isNotEmpty() || selectedProject.logicalInterfaces.isNotEmpty() || selectedProject.wanVpnConnections.isNotEmpty())) {
            heading(i18n.text("text.7070d68f65b5"))
            selectedProject.vlans.sortedBy { it.vlanId }.forEach { item(i18n.text("text.91b4234e3dfd", it.vlanId, it.name) + (it.description?.let { d -> ": $d" } ?: "")) }
            selectedProject.subnets.forEach { s ->
                val details = listOfNotNull(s.name, s.gatewayIp?.let { i18n.text("text.8df4844a7abf", it) }).joinToString(", ")
                item(i18n.text("text.0e43b185ea44", s.cidrBlock) + if (details.isNotEmpty()) " — $details" else "")
            }
            selectedProject.logicalInterfaces.forEach { i -> item("${index.deviceName(i.deviceId, i18n = i18n)} › ${i.name}: ${listOfNotNull(i.ipAddress, i.subnetCidr, i.vlanId?.let { i18n.text("text.da4da5c165af", it) }).joinToString(" ")}") }
            selectedProject.wanVpnConnections.forEach { w -> item("${w.type.toDisplayString(i18n = i18n)} ${w.name}: ${listOfNotNull(w.providerOrCarrier, w.bandwidth).joinToString(", ")}") }
        }

        if (selection.includePowerAndBadges && (selectedProject.powerFeeds.isNotEmpty() || selectedProject.poeMappings.isNotEmpty() || selectedProject.documentBadges.isNotEmpty())) {
            heading(i18n.text("text.acedc1948e5f"))
            selectedProject.powerFeeds.forEach { f ->
                val source = f.sourceDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: f.sourceOutletDescription
                val load = listOfNotNull(f.loadWatts?.let { i18n.text("text.cd49315c743a", it) }, f.observedRuntimeMinutes?.let { i18n.text("text.2dc280aa0f83", it) }).joinToString(", ")
                item("${index.deviceName(f.deviceId, i18n = i18n)} · ${f.feedName} (${f.feedType.toDisplayString(i18n = i18n)})" +
                    (source?.let { i18n.text("text.a863d2c56b73", it) } ?: "") + (if (load.isNotEmpty()) " — $load" else ""))
            }
            selectedProject.poeMappings.forEach { p -> item(i18n.text("text.cb79585925c3", index.portLabel(p.portId), p.role.toDisplayString(i18n = i18n), p.standard.toDisplayString(i18n = i18n))) }
            selectedProject.documentBadges.forEach { b -> item(i18n.text("text.3b424f3a179d", b.label, index.targetLabel(b.targetType, b.targetId, i18n = i18n))) }
        }

        if (selection.includeNotesAndAttachments) {
            val attachments = selectedProject.attachments
            val fields = selectedProject.customExtraFields
            val notes = scope.observations.filter { !it.observation?.notes.isNullOrBlank() }
            if (attachments.isNotEmpty() || fields.isNotEmpty() || notes.isNotEmpty()) {
                heading(i18n.text("text.1d02ed0f43ac"))
                notes.forEach { item("${it.label} · ${it.observation.effectiveStatus().toDisplayString(i18n)}: ${it.observation?.notes}") }
                attachments.forEach { a -> item(i18n.text("text.17e998f7cad2", a.name, a.fileType.toDisplayString(i18n = i18n), a.originalFileName)) }
                fields.forEach { f -> item("${EntityTypeLabels.of(f.targetType, i18n = i18n)} ${index.targetLabel(f.targetType, f.targetId, i18n = i18n).substringAfter(": ")} — ${f.fieldKey}: ${f.fieldValue}") }
            }
        }
        val warnings = scope.warnings(i18n, selection)
        if (warnings.isNotEmpty()) {
            heading(i18n.text("document.warnings"))
            warnings.forEach { item(it.message) }
        }
        return lines
    }
}
