package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.display.toDisplayString

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.effectiveStatus
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date

/** Writes filtered Markdown without credentials. */
object MarkdownExportManager {

    private fun singleLine(value: String) = value.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ')

    private fun escaped(value: String?) = buildString {
        for (character in singleLine(value.orEmpty())) {
            if (character in "\\`*_{}[]()#+!|<>~&") append('\\')
            append(character)
        }
    }

    // Keep table separators outside code spans so renderers preserve literal backslashes.
    private fun code(value: String): String = singleLine(value).split('|').joinToString("\\|") {
        if (it.isEmpty()) "" else codePart(it)
    }

    private fun codePart(text: String): String {
        val delimiter = "`".repeat((Regex("`+").findAll(text).maxOfOrNull { it.value.length } ?: 0) + 1)
        val pad = if (text.startsWith('`') || text.endsWith('`') ||
            (text.startsWith(' ') && text.endsWith(' ') && text.isNotBlank())) " " else ""
        return "$delimiter$pad$text$pad$delimiter"
    }

    fun exportMarkdownToStream(
        project: Project,
        filterConfig: ExportFilterConfig,
        outputStream: OutputStream,
        i18n: Messages = Messages()) {
        val scope = DocumentSelection(project, filterConfig)
        val selectedProject = scope.project
        val filteredDevices = scope.devices

        val filteredDeviceIds = filteredDevices.map { it.id }.toSet()

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", i18n.locale)
        val dateStr = sdf.format(Date())

        val sb = StringBuilder()

        sb.append(i18n.text("text.51d55eca0da1", escaped(filterConfig.titleOverride ?: selectedProject.name)))
        sb.append(i18n.text("text.a9f159e06d8c", escaped(selectedProject.name)))
        if (selectedProject.description != null) {
            sb.append(i18n.text("text.928cda9b0882", escaped(selectedProject.description)))
        }
        sb.append(i18n.text("text.5079451365c9", dateStr))
        sb.append(i18n.text("text.2fc8bcdb6d41", escaped(filterConfig.authorName)))
        sb.append(i18n.text("text.bb00e8824c7e", if (filterConfig.includeConfidential) i18n.text("text.de50b753caa5") else i18n.text("text.067d356a24e8")))

        sb.append("---\n\n")

        sb.append(i18n.text("text.e064d5838d84"))
        sb.append(i18n.text("text.763262b5c2db"))
        sb.append("| :--- | :--- |\n")
        sb.append(i18n.text("text.5213a63a1aab", selectedProject.sites.size))
        sb.append(i18n.text("text.d26f1e1ab803", filteredDevices.size))
        sb.append(i18n.text("text.ba8e0c852cf8", selectedProject.racks.size))
        sb.append(i18n.text("text.d4fb508e5005", selectedProject.cables.size))
        sb.append(i18n.text("text.1676ecf6cdbe", selectedProject.vlans.size, selectedProject.subnets.size))

        val openIssues = filteredDevices.count { it.observation.effectiveStatus() == ObservationStatus.TO_VERIFY || it.observation.effectiveStatus() == ObservationStatus.CONFLICT }
        sb.append(i18n.text("text.46d91c6ab242", openIssues))

        sb.append("---\n\n")

        sb.append(i18n.text("text.b94006e17dfd"))
        sb.append(i18n.text("text.e1f506cffab5"))
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n")

        for (dev in filteredDevices) {
            val rackName = selectedProject.racks.find { it.id == dev.rackId }?.name ?: i18n.text("text.3f03be4817b0")
            val rackLoc = if (dev.rackId != null) i18n.text("text.6a25a1235a6f", rackName, dev.positionU ?: "-") else i18n.text("text.3f03be4817b0")
            val statusStr = dev.observation.effectiveStatus().toDisplayString(i18n)

            sb.append("| **${escaped(dev.technicalName)}** | ${code(dev.ipAddress ?: "-")} | ${dev.category.toDisplayString(i18n)} | ${escaped(rackLoc)} | ${dev.ports.size} | ${dev.operationalStatus.toDisplayString(i18n)} | `$statusStr` |\n")
        }
        sb.append("\n")

        sb.append("## ${i18n.text("config.title")}\n\n")
        val graph = com.onlyfield.assetmanager.core.model.ConnectionGraph(project)
        filteredDevices.forEach { device ->
            sb.append("### ${escaped(device.technicalName)}\n\n")
            sb.append("${i18n.text("config.features")}: ${escaped(device.hardware.features.joinToString(", "))}\n\n")
            val keys = listOf("port", "side", "connector", "speed", "module", "observation")
            sb.append("| " + keys.joinToString(" | ") { i18n.text("config.$it") } + " |\n")
            sb.append("| " + keys.joinToString(" | ") { "---" } + " |\n")
            device.ports.forEach { port ->
                val values = listOf(port.name, port.hardware.side?.toDisplayString(i18n).orEmpty(), port.hardware.connector.orEmpty(), port.hardware.speed.orEmpty(), port.hardware.opticalModule.orEmpty(),
                    i18n.text("config.${graph.state(port.id).name.lowercase()}") + " · " + port.observation.effectiveStatus().toDisplayString(i18n))
                sb.append("| " + values.joinToString(" | ", transform = ::escaped) + " |\n")
            }
            sb.append("\n")
        }

        if (selectedProject.racks.isNotEmpty()) {
            sb.append(i18n.text("text.6ebb98387f5e"))
            for (rack in selectedProject.racks) {
                val devicesInRack = filteredDevices.filter { it.rackId == rack.id }
                sb.append(i18n.text("text.9d606200307f", escaped(rack.name), rack.heightU))
                sb.append("${i18n.text("config.depth")}: ${rack.depthMm ?: "-"}; ${i18n.text("config.mountDepth")}: ${rack.mountingDepthMm ?: "-"}\n\n")
                if (devicesInRack.isEmpty()) {
                    sb.append(i18n.text("text.63850e0d2ec8"))
                } else {
                    sb.append(i18n.text("text.26a1e997de6d"))
                    sb.append("| :--- | :--- | :--- | :--- | :--- |\n")
                    for (dev in devicesInRack.sortedByDescending { it.positionU ?: 0 }) {
                        sb.append(i18n.text("text.7a8d1ba5decb", dev.positionU ?: "-", dev.rackSide.toDisplayString(i18n), escaped(dev.technicalName), dev.category.toDisplayString(i18n), dev.ports.size))
                    }
                    sb.append("\n")
                }
            }
        }

        if (selectedProject.cables.isNotEmpty()) {
            sb.append(i18n.text("text.4d78d18352a8"))
            sb.append(i18n.text("text.453b1efb174a"))
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n")

            val allPorts = selectedProject.sites.flatMap { it.devices }.flatMap { it.ports }.associateBy { it.id }
            val allDevices = selectedProject.sites.flatMap { it.devices }.associateBy { it.id }

            for (cable in selectedProject.cables) {
                val portA = cable.portAId?.let { allPorts[it] }
                val devA = portA?.let { allDevices[it.deviceId] }

                if (devA != null && !filteredDeviceIds.contains(devA.id)) continue

                val portB = cable.portBId?.let { allPorts[it] }
                val devB = portB?.let { allDevices[it.deviceId] }

                val endpointAStr = "${devA?.technicalName ?: "-"} (${portA?.name ?: "-"})"
                val endpointBStr = when {
                    devB != null -> "${devB.technicalName} (${portB.name})"
                    cable.portBId != null -> i18n.text("text.614eb80d7434", cable.portBId)
                    else -> i18n.text("text.ba7cc7a170dd")
                }

                sb.append("| ${escaped(cable.codeOrLabel ?: cable.id.take(8))} | ${escaped(endpointAStr)} | ${cable.medium.toDisplayString(i18n)} | ${escaped(cable.color ?: "-")} | ${escaped(endpointBStr)} | `${(portA?.endpointStatus ?: com.onlyfield.assetmanager.core.model.EndpointStatus.UNKNOWN).toDisplayString(i18n)} · ${cable.observation.effectiveStatus().toDisplayString(i18n)}` |\n")
            }
            sb.append("\n")
        }

        if (selectedProject.vlans.isNotEmpty()) {
            sb.append(i18n.text("text.bc013965b27a"))
            sb.append(i18n.text("text.013a63422a55"))
            sb.append("| :--- | :--- | :--- | :--- | :--- |\n")

            for (vlan in selectedProject.vlans) {
                val subnetsForVlan = selectedProject.subnets.filter { it.vlanId == vlan.id }
                val cidrs = subnetsForVlan.joinToString(", ") { code(it.cidrBlock) }
                val gateways = subnetsForVlan.mapNotNull { it.gatewayIp }.joinToString(", ") { code(it) }

                sb.append("| **${vlan.vlanId}** | ${escaped(vlan.name)} | ${vlan.scopeType.toDisplayString(i18n)} | ${cidrs.ifEmpty { "-" }} | ${gateways.ifEmpty { "-" }} |\n")
            }
            sb.append("\n")
        }

        if (selectedProject.powerFeeds.isNotEmpty() || selectedProject.poeMappings.isNotEmpty() || selectedProject.documentBadges.isNotEmpty()) {
            sb.append(i18n.text("text.1577688da00d"))
            val index = com.onlyfield.assetmanager.core.display.ProjectIndex(project)
            val headers = PowerFeedRows.headers(i18n)
            sb.append("| " + headers.joinToString(" | ") + " |\n")
            sb.append("| " + headers.joinToString(" | ") { "---" } + " |\n")
            selectedProject.powerFeeds.forEach { feed ->
                sb.append("| " + PowerFeedRows.values(feed, index, i18n).joinToString(" | ", transform = ::escaped) + " |\n")
            }
            sb.append("\n")
            selectedProject.poeMappings.forEach { poe ->
                sb.append("- " + escaped(i18n.text("text.cb79585925c3", index.portLabel(poe.portId), poe.role.toDisplayString(i18n), poe.standard.toDisplayString(i18n))) + "\n")
            }
            selectedProject.documentBadges.forEach { badge ->
                sb.append("- " + escaped(i18n.text("text.3b424f3a179d", badge.label, index.targetLabel(badge.targetType, badge.targetId, i18n))) + "\n")
            }
            sb.append("\n")
        }

        sb.append(i18n.text("text.ddf3a938a4e2"))
        for (att in selectedProject.attachments) {
            val classBadge = when (att.classification) {
                AttachmentClassification.SHAREABLE -> "[${att.classification.toDisplayString(i18n)}]"
                AttachmentClassification.CONFIDENTIAL -> "[${att.classification.toDisplayString(i18n)}]"
                AttachmentClassification.REVIEW_REQUIRED -> i18n.text("text.f69c1736c6b2")
            }
            val attrNote = if (!att.attributionText.isNullOrBlank()) i18n.text("text.8e1a6699510b", escaped(att.attributionText)) else ""
            sb.append("- **${escaped(att.name)}** $classBadge (${code(att.originalFileName)})$attrNote\n")
        }

        val notes = scope.observations.filter { !it.observation?.notes.isNullOrBlank() }
        if (notes.isNotEmpty()) {
            sb.append("\n## ${i18n.text("config.observation")}\n\n")
            notes.forEach { sb.append("- ${escaped(it.label)} · ${it.observation.effectiveStatus().toDisplayString(i18n)}: ${escaped(it.observation?.notes.orEmpty())}\n") }
        }
        val warnings = scope.warnings(i18n)
        if (warnings.isNotEmpty()) {
            sb.append("\n## ${i18n.text("document.warnings")}\n\n")
            warnings.forEach { sb.append("- ${escaped(it.message)}\n") }
        }

        outputStream.use { stream ->
            stream.write(sb.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }
}
