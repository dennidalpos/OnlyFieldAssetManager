package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.display.toDisplayString

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Project
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date

/** Writes filtered Markdown without credentials. */
object MarkdownExportManager {

    fun exportMarkdownToStream(
        project: Project,
        filterConfig: ExportFilterConfig,
        outputStream: OutputStream,
        i18n: Messages = Messages()) {
        val filteredDevices = project.businessUnits
            .filter { (filterConfig.selectedBusinessUnitId == null) || (it.id == filterConfig.selectedBusinessUnitId) }
            .flatMap { bu -> bu.devices.filter { device ->
                val siteId = device.siteId ?: bu.sites.find { site -> site.areas.any { it.id == device.areaId } }?.id
                ((filterConfig.selectedSiteId == null) || (siteId == filterConfig.selectedSiteId)) &&
                    ((filterConfig.selectedAreaId == null) || (device.areaId == filterConfig.selectedAreaId))
            } }
            .filter { filterConfig.selectedCategory == null || it.category == filterConfig.selectedCategory }
            .distinctBy { it.id }

        val filteredDeviceIds = filteredDevices.map { it.id }.toSet()

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", i18n.locale)
        val dateStr = sdf.format(Date())

        val sb = StringBuilder()

        sb.append(i18n.text("text.51d55eca0da1", filterConfig.titleOverride ?: project.name))
        sb.append(i18n.text("text.a9f159e06d8c", project.name))
        if (project.description != null) {
            sb.append(i18n.text("text.928cda9b0882", project.description))
        }
        sb.append(i18n.text("text.5079451365c9", dateStr))
        sb.append(i18n.text("text.2fc8bcdb6d41", filterConfig.authorName))
        sb.append(i18n.text("text.bb00e8824c7e", if (filterConfig.includeConfidential) i18n.text("text.de50b753caa5") else i18n.text("text.067d356a24e8")))

        sb.append("---\n\n")

        sb.append(i18n.text("text.e064d5838d84"))
        sb.append(i18n.text("text.763262b5c2db"))
        sb.append("| :--- | :--- |\n")
        sb.append(i18n.text("text.5213a63a1aab", project.businessUnits.size))
        sb.append(i18n.text("text.d26f1e1ab803", filteredDevices.size))
        sb.append(i18n.text("text.ba8e0c852cf8", project.racks.size))
        sb.append(i18n.text("text.d4fb508e5005", project.cables.size))
        sb.append(i18n.text("text.1676ecf6cdbe", project.vlans.size, project.subnets.size))

        val openIssues = filteredDevices.count { it.observation?.status == ObservationStatus.TO_VERIFY || it.observation?.status == ObservationStatus.CONFLICT }
        sb.append(i18n.text("text.46d91c6ab242", openIssues))

        sb.append("---\n\n")

        sb.append(i18n.text("text.b94006e17dfd"))
        sb.append(i18n.text("text.e1f506cffab5"))
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n")

        for (dev in filteredDevices) {
            val rackName = project.racks.find { it.id == dev.rackId }?.name ?: i18n.text("text.3f03be4817b0")
            val rackLoc = if (dev.rackId != null) i18n.text("text.6a25a1235a6f", rackName, dev.positionU ?: "-") else i18n.text("text.3f03be4817b0")
            val statusStr = (dev.observation?.status ?: ObservationStatus.VERIFIED).toDisplayString(i18n)

            sb.append("| **${dev.technicalName}** | `${dev.ipAddress ?: "-"}` | ${dev.category.toDisplayString(i18n)} | $rackLoc | ${dev.ports.size} | `$statusStr` |\n")
        }
        sb.append("\n")

        sb.append("## ${i18n.text("config.title")}\n\n")
        val graph = com.onlyfield.assetmanager.core.model.ConnectionGraph(project)
        fun escaped(value: String) = value.replace("|", "\\|").replace("\n", " ")
        filteredDevices.forEach { device ->
            sb.append("### ${escaped(device.technicalName)}\n\n")
            sb.append("${i18n.text("config.features")}: ${escaped(device.hardware.features.joinToString(", "))}\n\n")
            val keys = listOf("port", "side", "connector", "speed", "module", "observation")
            sb.append("| " + keys.joinToString(" | ") { i18n.text("config.$it") } + " |\n")
            sb.append("| " + keys.joinToString(" | ") { "---" } + " |\n")
            device.ports.forEach { port ->
                val values = listOf(port.name, port.hardware.side?.toDisplayString(i18n).orEmpty(), port.hardware.connector.orEmpty(), port.hardware.speed.orEmpty(), port.hardware.opticalModule.orEmpty(), i18n.text("config.${graph.state(port.id).name.lowercase()}"))
                sb.append("| " + values.joinToString(" | ", transform = ::escaped) + " |\n")
            }
            sb.append("\n")
        }

        if (project.racks.isNotEmpty()) {
            sb.append(i18n.text("text.6ebb98387f5e"))
            for (rack in project.racks) {
                val devicesInRack = filteredDevices.filter { it.rackId == rack.id }
                sb.append(i18n.text("text.9d606200307f", rack.name, rack.heightU))
                sb.append("${i18n.text("config.depth")}: ${rack.depthMm ?: "-"}; ${i18n.text("config.mountDepth")}: ${rack.mountingDepthMm ?: "-"}\n\n")
                if (devicesInRack.isEmpty()) {
                    sb.append(i18n.text("text.63850e0d2ec8"))
                } else {
                    sb.append(i18n.text("text.26a1e997de6d"))
                    sb.append("| :--- | :--- | :--- | :--- | :--- |\n")
                    for (dev in devicesInRack.sortedByDescending { it.positionU ?: 0 }) {
                        sb.append(i18n.text("text.7a8d1ba5decb", dev.positionU ?: "-", dev.rackSide.toDisplayString(i18n), dev.technicalName, dev.category.toDisplayString(i18n), dev.ports.size))
                    }
                    sb.append("\n")
                }
            }
        }

        if (project.cables.isNotEmpty()) {
            sb.append(i18n.text("text.4d78d18352a8"))
            sb.append(i18n.text("text.453b1efb174a"))
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n")

            val allPorts = project.businessUnits.flatMap { it.devices }.flatMap { it.ports }.associateBy { it.id }
            val allDevices = project.businessUnits.flatMap { it.devices }.associateBy { it.id }

            for (cable in project.cables) {
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

                sb.append("| ${cable.codeOrLabel ?: cable.id.take(8)} | $endpointAStr | ${cable.medium.toDisplayString(i18n)} | ${cable.orientation.toDisplayString(i18n)} | $endpointBStr | `${(portA?.endpointStatus ?: com.onlyfield.assetmanager.core.model.EndpointStatus.UNKNOWN).toDisplayString(i18n)}` |\n")
            }
            sb.append("\n")
        }

        if (project.vlans.isNotEmpty()) {
            sb.append(i18n.text("text.bc013965b27a"))
            sb.append(i18n.text("text.013a63422a55"))
            sb.append("| :--- | :--- | :--- | :--- | :--- |\n")

            for (vlan in project.vlans) {
                val subnetsForVlan = project.subnets.filter { it.vlanId == vlan.id }
                val cidrs = subnetsForVlan.joinToString(", ") { "`${it.cidrBlock}`" }
                val gateways = subnetsForVlan.mapNotNull { it.gatewayIp }.joinToString(", ") { "`$it`" }

                sb.append("| **${vlan.vlanId}** | ${vlan.name} | ${vlan.scopeType.toDisplayString(i18n)} | ${cidrs.ifEmpty { "-" }} | ${gateways.ifEmpty { "-" }} |\n")
            }
            sb.append("\n")
        }

        if (project.powerFeeds.isNotEmpty() || project.documentBadges.isNotEmpty()) {
            sb.append(i18n.text("text.1577688da00d"))
            sb.append(i18n.text("text.083528e3bd57"))
            sb.append("| :--- | :--- | :--- | :--- | :--- |\n")

            for (dev in filteredDevices) {
                val feeds = project.powerFeeds.filter { it.deviceId == dev.id }
                val feedA = feeds.find { it.feedName == "A" }?.let { "${it.feedType.toDisplayString(i18n)} (${it.sourceOutletDescription ?: "-"})" } ?: "-"
                val feedB = feeds.find { it.feedName == "B" }?.let { "${it.feedType.toDisplayString(i18n)} (${it.sourceOutletDescription ?: "-"})" } ?: "-"

                val va = feeds.mapNotNull { it.loadVa }.sum()
                val w = feeds.mapNotNull { it.loadWatts }.sum()
                val loadStr = if (va > 0 || w > 0) i18n.text("text.12cb5c0da8a5", va, w) else "-"

                val badges = project.documentBadges.filter { it.targetId == dev.id }.joinToString(", ") { "`${it.label}`" }

                sb.append("| **${dev.technicalName}** | $feedA | $feedB | $loadStr | ${badges.ifEmpty { "-" }} |\n")
            }
            sb.append("\n")
        }

        sb.append(i18n.text("text.ddf3a938a4e2"))
        for (att in project.attachments) {
            if (att.classification == AttachmentClassification.CONFIDENTIAL && !filterConfig.includeConfidential) {
                continue
            }
            val classBadge = when (att.classification) {
                AttachmentClassification.SHAREABLE -> "[${att.classification.toDisplayString(i18n)}]"
                AttachmentClassification.CONFIDENTIAL -> "[${att.classification.toDisplayString(i18n)}]"
                AttachmentClassification.REVIEW_REQUIRED -> i18n.text("text.f69c1736c6b2")
            }
            val attrNote = if (!att.attributionText.isNullOrBlank()) i18n.text("text.8e1a6699510b", att.attributionText) else ""
            sb.append("- **${att.name}** $classBadge (`${att.originalFileName}`)$attrNote\n")
        }

        outputStream.use { stream ->
            stream.write(sb.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }
}
