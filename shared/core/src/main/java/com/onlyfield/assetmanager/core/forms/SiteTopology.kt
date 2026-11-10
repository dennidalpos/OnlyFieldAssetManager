package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.display.sitesForDisplay
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/**
 * Node of the project overview: a surveyed [site], or an external end (provider, remote office) known
 * only by [label]. [row] and [column] place it in wrapped rows.
 */
data class SiteNode(val id: String, val label: String, val site: Site?, val row: Int, val column: Int) {
    val floors: Int get() = site?.areas?.size ?: 0
    val activeDevices: Int get() = site?.devices?.count { !it.isPassive() } ?: 0
}

/** Everything joining two nodes: cables crossing the two sites and WAN/VPN connections. */
data class SiteLink(val a: String, val b: String, val cables: Int, val radio: Boolean, val connections: List<WanVpnConnection>)

data class SiteOverview(val nodes: List<SiteNode>, val links: List<SiteLink>, val rowSizes: List<Int>)

/** Sites of a project and how they reach each other. */
object SiteTopology {
    fun build(project: Project, perRow: Int = 4, i18n: Messages = Messages()): SiteOverview {
        val devices = project.sites.flatMap { s -> s.devices.map { it to s.id } }
        val siteOf = devices.associate { (d, site) -> d.id to site }
        val portSite = devices.flatMap { (d, site) -> d.ports.map { it.id to site } }.toMap()

        class Acc(var cables: Int = 0, var radio: Boolean = false, val connections: MutableList<WanVpnConnection> = mutableListOf())
        val links = linkedMapOf<Pair<String, String>, Acc>()
        fun link(a: String, b: String) = links.getOrPut(if (a <= b) a to b else b to a) { Acc() }

        for (c in project.cables) {
            val a = c.portAId?.let(portSite::get) ?: c.deviceAId?.let(siteOf::get) ?: continue
            val b = c.portBId?.let(portSite::get) ?: c.deviceBId?.let(siteOf::get) ?: continue
            if (a != b) link(a, b).apply { cables++; radio = radio || c.medium == CableMedium.RADIO }
        }
        // Ends without a surveyed device become external nodes, shared by label (one "TIM" for every site using it).
        val external = linkedMapOf<String, String>()
        for (c in project.wanVpnConnections) {
            fun outside(description: String?): String {
                val label = listOf(description, c.providerOrCarrier).firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotEmpty) } ?: i18n.text("sites.external")
                return "external:${label.lowercase()}".also { external.putIfAbsent(it, label) }
            }
            val local = c.localEndpointDeviceId?.let(siteOf::get)
            val remote = c.remoteEndpointDeviceId?.let(siteOf::get)
            if (local == null && remote == null) continue
            val a = local ?: outside(c.localEndpointSiteDescription)
            val b = remote ?: outside(c.remoteEndpointSiteDescription)
            if (a != b) link(a, b).connections += c
        }

        val ordered = project.sites.sitesForDisplay(i18n).map { Triple(it.id, it.code?.takeIf(String::isNotBlank) ?: it.name, it) } +
            external.map { (id, label) -> Triple(id, label, null) }
        val rows = ordered.chunked(perRow.coerceAtLeast(1))
        val nodes = rows.flatMapIndexed { row, chunk -> chunk.mapIndexed { column, (id, label, site) -> SiteNode(id, label, site, row, column) } }
        return SiteOverview(nodes, links.map { (k, acc) -> SiteLink(k.first, k.second, acc.cables, acc.radio, acc.connections) }, rows.map { it.size })
    }
}
