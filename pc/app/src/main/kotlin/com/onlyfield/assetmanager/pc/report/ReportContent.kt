package com.onlyfield.assetmanager.pc.report

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.display.EntityTypeLabels
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.NumberingDirection
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.ReportSelection
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
}

/** Builds selected report content without credentials. */
object ReportContent {

    private fun allowed(classification: AttachmentClassification, filter: ExportFilterConfig) = when (classification) {
        AttachmentClassification.SHAREABLE -> true
        AttachmentClassification.CONFIDENTIAL -> filter.includeConfidential
        AttachmentClassification.REVIEW_REQUIRED -> filter.reviewRequiredConfirmed
    }

    fun build(project: Project, filter: ExportFilterConfig, selection: ReportSelection, now: Date = Date(), i18n: Messages = Messages()): List<ReportLine> {
        val index = ProjectIndex(project)
        val lines = mutableListOf<ReportLine>()
        fun heading(text: String) { lines += ReportLine.Spacer; lines += ReportLine.Heading(text) }
        fun item(text: String, indent: Int = 0) { lines += ReportLine.Item(text, indent) }

        lines += ReportLine.Title(filter.titleOverride?.ifBlank { null } ?: i18n.text("text.ba220bc18a31", project.name))
        lines += ReportLine.Meta(i18n.text("text.b1b3e27e33a1", filter.authorName, SimpleDateFormat("dd/MM/yyyy HH:mm", i18n.locale).format(now)))
        project.description?.takeIf { it.isNotBlank() }?.let { lines += ReportLine.Meta(it) }
        lines += ReportLine.Meta(
            i18n.text("text.9046ee10595c", index.devices.size, project.racks.size, project.cables.size) +
                i18n.text("text.fde3438fc2a1", project.vlans.size, project.powerFeeds.size)
        )

        if (selection.includeInventoryTable) {
            heading(i18n.text("text.dae8f6194460"))
            for (bu in project.businessUnits) {
                if (bu.devices.isEmpty()) continue
                lines += ReportLine.SubHeading(bu.name)
                bu.devices.groupBy { it.areaId }.forEach { (areaId, devices) ->
                    item(index.areaName(areaId, i18n.text("text.38426bccdab9")))
                    devices.sortedBy { it.technicalName }.forEach { d ->
                        val extra = listOfNotNull(
                            d.category.toDisplayString(i18n = i18n),
                            d.ipAddress,
                            d.rackId?.let { i18n.text("text.4d2d924e8402", index.rackName(it)) + (d.positionU?.let { u -> i18n.text("text.bf28e779d560", u) } ?: "") },
                            d.physicalLabel?.let { i18n.text("text.49ac7ff5098a", it) },
                            i18n.text("text.53a2e3b94696", d.ports.size).takeIf { d.ports.isNotEmpty() }
                        )
                        item("${d.technicalName} — ${extra.joinToString(" · ")}", indent = 1)
                    }
                }
            }
        }

        if (selection.includeRackCards && project.racks.isNotEmpty()) {
            heading(i18n.text("text.4cd265c2b8c6"))
            for (rack in project.racks) {
                lines += ReportLine.SubHeading(i18n.text("text.f0f63aaad4b6", rack.name, rack.heightU, index.areaName(rack.areaId, i18n.text("text.1abc7243c3dd"))))
                val mounted = index.devices.filter { it.rackId == rack.id }
                    .sortedBy { it.positionU ?: Int.MAX_VALUE }
                    .let { if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) it.reversed() else it }
                if (mounted.isEmpty()) item(i18n.text("text.4a1ac9701d21"), 1)
                mounted.forEach { d ->
                    val pos = d.positionU?.let { if (d.heightU > 1) "U$it–U${it + d.heightU - 1}" else "U$it" } ?: i18n.text("text.3fc3745f990b")
                    item("$pos: ${d.technicalName} (${d.category.toDisplayString(i18n = i18n)}, ${d.rackSide.toDisplayString(i18n = i18n).lowercase()})", 1)
                }
            }
        }

        if (selection.includeCablingAndPorts && (project.cables.isNotEmpty() || project.panelMappings.isNotEmpty())) {
            heading(i18n.text("text.3b40d8bd6081"))
            project.cables.forEach { c ->
                val details = listOfNotNull(c.medium.toDisplayString(i18n = i18n), c.lengthValue?.let { "$it ${c.lengthUnit ?: "m"}" }, c.color, c.nominalCharacteristics)
                item("${c.codeOrLabel ?: i18n.text("text.89dbe18e8407")}: ${index.portLabel(c.portAId, "libero")} <-> ${index.portLabel(c.portBId, "libero")} (${details.joinToString(", ")})")
            }
            if (project.panelMappings.isNotEmpty()) {
                lines += ReportLine.SubHeading(i18n.text("text.66cdda39c617"))
                project.panelMappings.forEach { m -> item("${index.portLabel(m.portAId)} <-> ${index.portLabel(m.portBId, i18n.text("common.none"))}") }
            }
        }

        if (selection.includeLogicalNetwork && (project.vlans.isNotEmpty() || project.subnets.isNotEmpty() || project.logicalInterfaces.isNotEmpty() || project.wanVpnConnections.isNotEmpty())) {
            heading(i18n.text("text.7070d68f65b5"))
            project.vlans.sortedBy { it.vlanId }.forEach { item(i18n.text("text.91b4234e3dfd", it.vlanId, it.name) + (it.description?.let { d -> ": $d" } ?: "")) }
            project.subnets.forEach { s ->
                val details = listOfNotNull(s.name, s.gatewayIp?.let { i18n.text("text.8df4844a7abf", it) }).joinToString(", ")
                item(i18n.text("text.0e43b185ea44", s.cidrBlock) + if (details.isNotEmpty()) " — $details" else "")
            }
            project.logicalInterfaces.forEach { i -> item("${index.deviceName(i.deviceId, i18n = i18n)} › ${i.name}: ${listOfNotNull(i.ipAddress, i.subnetCidr, i.vlanId?.let { i18n.text("text.da4da5c165af", it) }).joinToString(" ")}") }
            project.wanVpnConnections.forEach { w -> item("${w.type.toDisplayString(i18n = i18n)} ${w.name}: ${listOfNotNull(w.providerOrCarrier, w.bandwidth).joinToString(", ")}") }
        }

        if (selection.includePowerAndBadges && (project.powerFeeds.isNotEmpty() || project.poeMappings.isNotEmpty() || project.documentBadges.isNotEmpty())) {
            heading(i18n.text("text.acedc1948e5f"))
            project.powerFeeds.forEach { f ->
                val source = f.sourceDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: f.sourceOutletDescription
                val load = listOfNotNull(f.loadWatts?.let { i18n.text("text.cd49315c743a", it) }, f.observedRuntimeMinutes?.let { i18n.text("text.2dc280aa0f83", it) }).joinToString(", ")
                item("${index.deviceName(f.deviceId, i18n = i18n)} · ${f.feedName} (${f.feedType.toDisplayString(i18n = i18n)})" +
                    (source?.let { i18n.text("text.a863d2c56b73", it) } ?: "") + (if (load.isNotEmpty()) " — $load" else ""))
            }
            project.poeMappings.forEach { p -> item(i18n.text("text.cb79585925c3", index.portLabel(p.portId), p.role.toDisplayString(i18n = i18n), p.standard.toDisplayString(i18n = i18n))) }
            project.documentBadges.forEach { b -> item(i18n.text("text.3b424f3a179d", b.label, index.targetLabel(b.targetType, b.targetId, i18n = i18n))) }
        }

        if (selection.includeNotesAndAttachments) {
            val attachments = project.attachments.filter { allowed(it.classification, filter) }
            val fields = project.customExtraFields.filter { allowed(it.classification, filter) }
            if (attachments.isNotEmpty() || fields.isNotEmpty()) {
                heading(i18n.text("text.1d02ed0f43ac"))
                attachments.forEach { a -> item(i18n.text("text.17e998f7cad2", a.name, a.fileType.toDisplayString(i18n = i18n), a.originalFileName)) }
                fields.forEach { f -> item("${EntityTypeLabels.of(f.targetType, i18n = i18n)} ${index.targetLabel(f.targetType, f.targetId, i18n = i18n).substringAfter(": ")} — ${f.fieldKey}: ${f.fieldValue}") }
            }
        }
        return lines
    }
}
