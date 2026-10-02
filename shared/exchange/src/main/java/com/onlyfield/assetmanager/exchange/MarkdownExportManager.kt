package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Project
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera un report tecnico e schede di documentazione in formato Markdown (.md).
 * Esclude esplicitamente i campi segreti ed applica i filtri di selezione e riservatezza.
 */
object MarkdownExportManager {

    fun exportMarkdownToStream(
        project: Project,
        filterConfig: ExportFilterConfig,
        outputStream: OutputStream
    ) {
        val filteredDevices = project.businessUnits
            .filter { filterConfig.selectedBusinessUnitId == null || it.id == filterConfig.selectedBusinessUnitId }
            .flatMap { bu ->
                bu.sites.filter { filterConfig.selectedSiteId == null || it.id == filterConfig.selectedSiteId }
                    .flatMap { site ->
                        site.areas.filter { filterConfig.selectedAreaId == null || it.id == filterConfig.selectedAreaId }
                            .flatMap { area -> bu.devices.filter { it.areaId == area.id } }
                    } + bu.devices.filter { it.siteId == null && it.areaId == null }
            }
            .filter { filterConfig.selectedCategory == null || it.category == filterConfig.selectedCategory }
            .distinctBy { it.id }

        val filteredDeviceIds = filteredDevices.map { it.id }.toSet()

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val dateStr = sdf.format(Date())

        val sb = StringBuilder()

        // 1. Header & Metadata
        sb.append("# Documentazione Tecnica — ${filterConfig.titleOverride ?: project.name}\n\n")
        sb.append("- **Progetto:** ${project.name}\n")
        if (project.description != null) {
            sb.append("- **Descrizione:** ${project.description}\n")
        }
        sb.append("- **Data Generazione:** $dateStr\n")
        sb.append("- **Autore / Compilatore:** ${filterConfig.authorName}\n")
        sb.append("- **Filtro Riservatezza:** ${if (filterConfig.includeConfidential) "Inclusi contenuti riservati" else "Solo contenuti condivisibili"}\n\n")

        sb.append("---\n\n")

        // 2. Summary KPI
        sb.append("## Sommario Esecutivo\n\n")
        sb.append("| Metrica | Valore |\n")
        sb.append("| :--- | :--- |\n")
        sb.append("| Business Unit Censite | ${project.businessUnits.size} |\n")
        sb.append("| Apparati in Ambito | ${filteredDevices.size} |\n")
        sb.append("| Armadi Rack | ${project.racks.size} |\n")
        sb.append("| Cavi e Collegamenti | ${project.cables.size} |\n")
        sb.append("| VLAN e Subnet | ${project.vlans.size} VLAN / ${project.subnets.size} Subnet |\n")

        val openIssues = filteredDevices.count { it.observation?.status == ObservationStatus.TO_VERIFY || it.observation?.status == ObservationStatus.CONFLICT }
        sb.append("| Osservazioni da Verificare / In Conflitto | $openIssues |\n\n")

        sb.append("---\n\n")

        // 3. Equipment Inventory Table
        sb.append("## Inventario Apparati\n\n")
        sb.append("| Nome Tecnico | IP | Categoria | Ubicazione / Rack | Porte | Stato |\n")
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n")

        for (dev in filteredDevices) {
            val rackName = project.racks.find { it.id == dev.rackId }?.name ?: "Fuori Rack"
            val rackLoc = if (dev.rackId != null) "$rackName (U${dev.positionU ?: "-"})" else "Fuori Rack"
            val statusStr = dev.observation?.status?.name ?: "VERIFIED"

            sb.append("| **${dev.technicalName}** | `${dev.ipAddress ?: "-"}` | ${dev.category.name} | $rackLoc | ${dev.ports.size} | `$statusStr` |\n")
        }
        sb.append("\n")

        // 4. Rack Details
        if (project.racks.isNotEmpty()) {
            sb.append("## Schede Armadi Rack\n\n")
            for (rack in project.racks) {
                val devicesInRack = filteredDevices.filter { it.rackId == rack.id }
                sb.append("### Rack: ${rack.name} (${rack.heightU} U)\n\n")
                if (devicesInRack.isEmpty()) {
                    sb.append("*Nessun apparato montato in questo armadio.*\n\n")
                } else {
                    sb.append("| Posizione U | Lato | Apparato | Categoria | Porte |\n")
                    sb.append("| :--- | :--- | :--- | :--- | :--- |\n")
                    for (dev in devicesInRack.sortedByDescending { it.positionU ?: 0 }) {
                        sb.append("| U${dev.positionU ?: "-"} | ${dev.rackSide.name} | **${dev.technicalName}** | ${dev.category.name} | ${dev.ports.size} |\n")
                    }
                    sb.append("\n")
                }
            }
        }

        // 5. Cables & Connections
        if (project.cables.isNotEmpty()) {
            sb.append("## Cablaggio e Collegamenti Fisici\n\n")
            sb.append("| Cavo | Apparato A (Porta) | Mezzo | Orientamento | Apparato B (Porta) | Stato |\n")
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
                    devB != null -> "${devB.technicalName} (${portB?.name ?: "-"})"
                    cable.portBId != null -> "*Fuori Ambito* (Porta `${cable.portBId}`)"
                    else -> "*Ignoto / Scollegato*"
                }

                sb.append("| ${cable.codeOrLabel ?: cable.id.take(8)} | $endpointAStr | ${cable.medium.name} | ${cable.orientation.name} | $endpointBStr | `${portA?.endpointStatus?.name ?: "UNKNOWN"}` |\n")
            }
            sb.append("\n")
        }

        // 6. Logical Network & VLANs
        if (project.vlans.isNotEmpty()) {
            sb.append("## Rete Logica e VLAN\n\n")
            sb.append("| ID VLAN | Nome VLAN | Ambito | Subnet CIDR | Gateway L3 |\n")
            sb.append("| :--- | :--- | :--- | :--- | :--- |\n")

            for (vlan in project.vlans) {
                val subnetsForVlan = project.subnets.filter { it.vlanId == vlan.id }
                val cidrs = subnetsForVlan.joinToString(", ") { "`${it.cidrBlock}`" }
                val gateways = subnetsForVlan.mapNotNull { it.gatewayIp }.joinToString(", ") { "`$it`" }

                sb.append("| **${vlan.vlanId}** | ${vlan.name} | ${vlan.scopeType.name} | ${cidrs.ifEmpty { "-" }} | ${gateways.ifEmpty { "-" }} |\n")
            }
            sb.append("\n")
        }

        // 7. Power & Badges
        if (project.powerFeeds.isNotEmpty() || project.documentBadges.isNotEmpty()) {
            sb.append("## Alimentazione e Badge Documentali\n\n")
            sb.append("| Apparato | Feed A | Feed B | Carico (VA / W) | Badge |\n")
            sb.append("| :--- | :--- | :--- | :--- | :--- |\n")

            for (dev in filteredDevices) {
                val feeds = project.powerFeeds.filter { it.deviceId == dev.id }
                val feedA = feeds.find { it.feedName == "A" }?.let { "${it.feedType.name} (${it.sourceOutletDescription ?: "-"})" } ?: "-"
                val feedB = feeds.find { it.feedName == "B" }?.let { "${it.feedType.name} (${it.sourceOutletDescription ?: "-"})" } ?: "-"

                val va = feeds.mapNotNull { it.loadVa }.sum()
                val w = feeds.mapNotNull { it.loadWatts }.sum()
                val loadStr = if (va > 0 || w > 0) "$va VA / $w W" else "-"

                val badges = project.documentBadges.filter { it.targetId == dev.id }.joinToString(", ") { "`${it.label}`" }

                sb.append("| **${dev.technicalName}** | $feedA | $feedB | $loadStr | ${badges.ifEmpty { "-" }} |\n")
            }
            sb.append("\n")
        }

        // 8. Attachments & Notes
        sb.append("## Note e Allegati\n\n")
        for (att in project.attachments) {
            if (att.classification == AttachmentClassification.CONFIDENTIAL && !filterConfig.includeConfidential) {
                continue
            }
            val classBadge = when (att.classification) {
                AttachmentClassification.SHAREABLE -> "[Condivisibile]"
                AttachmentClassification.CONFIDENTIAL -> "[RISERVATO]"
                AttachmentClassification.REVIEW_REQUIRED -> "[DA RIESAMINARE]"
            }
            val attrNote = if (!att.attributionText.isNullOrBlank()) " — *Attribuzione:* ${att.attributionText}" else ""
            sb.append("- **${att.name}** $classBadge (`${att.originalFileName}`)$attrNote\n")
        }

        outputStream.use { stream ->
            stream.write(sb.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }
}
