package com.onlyfield.assetmanager.pc.report

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
import java.util.Locale

/** One line of a printed/PDF report. */
sealed interface ReportLine {
    val text: String

    data class Title(override val text: String) : ReportLine
    data class Meta(override val text: String) : ReportLine
    data class Heading(override val text: String) : ReportLine
    data class SubHeading(override val text: String) : ReportLine
    data class Item(override val text: String, val indent: Int = 0) : ReportLine
    data object Spacer : ReportLine { override val text = "" }
}

/**
 * Builds the content of the technical report from the project, honouring the section selection and
 * the confidentiality filter. Credentials are never included.
 */
object ReportContent {

    private fun allowed(classification: AttachmentClassification, filter: ExportFilterConfig) = when (classification) {
        AttachmentClassification.SHAREABLE -> true
        AttachmentClassification.CONFIDENTIAL -> filter.includeConfidential
        AttachmentClassification.REVIEW_REQUIRED -> filter.reviewRequiredConfirmed
    }

    fun build(project: Project, filter: ExportFilterConfig, selection: ReportSelection, now: Date = Date()): List<ReportLine> {
        val index = ProjectIndex(project)
        val lines = mutableListOf<ReportLine>()
        fun heading(text: String) { lines += ReportLine.Spacer; lines += ReportLine.Heading(text) }
        fun item(text: String, indent: Int = 0) { lines += ReportLine.Item(text, indent) }

        lines += ReportLine.Title(filter.titleOverride?.ifBlank { null } ?: "Rapporto tecnico — ${project.name}")
        lines += ReportLine.Meta("Autore: ${filter.authorName} · Generato il ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(now)}")
        project.description?.takeIf { it.isNotBlank() }?.let { lines += ReportLine.Meta(it) }
        lines += ReportLine.Meta(
            "${index.devices.size} apparati · ${project.racks.size} rack · ${project.cables.size} cavi · " +
                "${project.vlans.size} VLAN · ${project.powerFeeds.size} alimentazioni"
        )

        if (selection.includeInventoryTable) {
            heading("Inventario apparati")
            for (bu in project.businessUnits) {
                if (bu.devices.isEmpty()) continue
                lines += ReportLine.SubHeading(bu.name)
                bu.devices.groupBy { it.areaId }.forEach { (areaId, devices) ->
                    item(index.areaName(areaId, "Senza area"))
                    devices.sortedBy { it.technicalName }.forEach { d ->
                        val extra = listOfNotNull(
                            d.category.toDisplayString(),
                            d.ipAddress,
                            d.rackId?.let { "rack ${index.rackName(it)}" + (d.positionU?.let { u -> " U$u" } ?: "") },
                            d.physicalLabel?.let { "etichetta $it" },
                            "${d.ports.size} porte".takeIf { d.ports.isNotEmpty() }
                        )
                        item("${d.technicalName} — ${extra.joinToString(" · ")}", indent = 1)
                    }
                }
            }
        }

        if (selection.includeRackCards && project.racks.isNotEmpty()) {
            heading("Rack")
            for (rack in project.racks) {
                lines += ReportLine.SubHeading("${rack.name} — ${rack.heightU}U, ${index.areaName(rack.areaId, "nessuna area")}")
                val mounted = index.devices.filter { it.rackId == rack.id }
                    .sortedBy { it.positionU ?: Int.MAX_VALUE }
                    .let { if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) it.reversed() else it }
                if (mounted.isEmpty()) item("Nessun apparato montato", 1)
                mounted.forEach { d ->
                    val pos = d.positionU?.let { if (d.heightU > 1) "U$it–U${it + d.heightU - 1}" else "U$it" } ?: "posizione non indicata"
                    item("$pos: ${d.technicalName} (${d.category.toDisplayString()}, ${d.rackSide.toDisplayString().lowercase()})", 1)
                }
            }
        }

        if (selection.includeCablingAndPorts && (project.cables.isNotEmpty() || project.panelMappings.isNotEmpty())) {
            heading("Cablaggio")
            project.cables.forEach { c ->
                val details = listOfNotNull(c.medium.toDisplayString(), c.lengthValue?.let { "$it ${c.lengthUnit ?: "m"}" }, c.color, c.nominalCharacteristics)
                item("${c.codeOrLabel ?: "Cavo"}: ${index.portLabel(c.portAId, "libero")} <-> ${index.portLabel(c.portBId, "libero")} (${details.joinToString(", ")})")
            }
            if (project.panelMappings.isNotEmpty()) {
                lines += ReportLine.SubHeading("Permutazioni")
                project.panelMappings.forEach { m -> item("${index.portLabel(m.portAId)} <-> ${index.portLabel(m.portBId, "nessuna")}") }
            }
        }

        if (selection.includeLogicalNetwork && (project.vlans.isNotEmpty() || project.subnets.isNotEmpty() || project.logicalInterfaces.isNotEmpty() || project.wanVpnConnections.isNotEmpty())) {
            heading("Rete logica")
            project.vlans.sortedBy { it.vlanId }.forEach { item("VLAN ${it.vlanId} — ${it.name}" + (it.description?.let { d -> ": $d" } ?: "")) }
            project.subnets.forEach { s ->
                val details = listOfNotNull(s.name, s.gatewayIp?.let { "gateway $it" }).joinToString(", ")
                item("Subnet ${s.cidrBlock}" + if (details.isNotEmpty()) " — $details" else "")
            }
            project.logicalInterfaces.forEach { i -> item("${index.deviceName(i.deviceId)} › ${i.name}: ${listOfNotNull(i.ipAddress, i.subnetCidr, i.vlanId?.let { "VLAN $it" }).joinToString(" ")}") }
            project.wanVpnConnections.forEach { w -> item("${w.type.toDisplayString()} ${w.name}: ${listOfNotNull(w.providerOrCarrier, w.bandwidth).joinToString(", ")}") }
        }

        if (selection.includePowerAndBadges && (project.powerFeeds.isNotEmpty() || project.poeMappings.isNotEmpty() || project.documentBadges.isNotEmpty())) {
            heading("Alimentazione")
            project.powerFeeds.forEach { f ->
                val source = f.sourceDeviceId?.let { index.deviceName(it) } ?: f.sourceOutletDescription
                val load = listOfNotNull(f.loadWatts?.let { "$it W" }, f.observedRuntimeMinutes?.let { "autonomia $it min" }).joinToString(", ")
                item("${index.deviceName(f.deviceId)} · ${f.feedName} (${f.feedType.toDisplayString()})" +
                    (source?.let { " da $it" } ?: "") + (if (load.isNotEmpty()) " — $load" else ""))
            }
            project.poeMappings.forEach { p -> item("PoE ${index.portLabel(p.portId)}: ${p.role.toDisplayString()}, ${p.standard.toDisplayString()}") }
            project.documentBadges.forEach { b -> item("Badge «${b.label}» — ${index.targetLabel(b.targetType, b.targetId)}") }
        }

        if (selection.includeNotesAndAttachments) {
            val attachments = project.attachments.filter { allowed(it.classification, filter) }
            val fields = project.customExtraFields.filter { allowed(it.classification, filter) }
            if (attachments.isNotEmpty() || fields.isNotEmpty()) {
                heading("Note e allegati")
                attachments.forEach { a -> item("Allegato: ${a.name} (${a.fileType.toDisplayString()}, ${a.originalFileName})") }
                fields.forEach { f -> item("${EntityTypeLabels.of(f.targetType)} ${index.targetLabel(f.targetType, f.targetId).substringAfter(": ")} — ${f.fieldKey}: ${f.fieldValue}") }
            }
        }
        return lines
    }
}
