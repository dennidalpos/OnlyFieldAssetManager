package com.onlyfield.assetmanager.core.display

import com.onlyfield.assetmanager.core.forms.CableLabels
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.HierarchyIndex
import com.onlyfield.assetmanager.core.model.ObjectHierarchy
import com.onlyfield.assetmanager.core.model.ObjectRef
import com.onlyfield.assetmanager.core.model.PlacementTargetType
import com.onlyfield.assetmanager.core.model.Project

enum class HitKind { DEVICE, RACK, PORT, CABLE }

/**
 * One search result. [focus] is the object to select on the map of [areaId];
 * null [areaId] means the object has no floor yet and opens in the editor.
 */
data class SearchHit(
    val kind: HitKind,
    val id: String,
    val title: String,
    val place: String,
    val focus: ObjectRef,
    val areaId: String?,
    /** "Field: value" that matched; empty for recent items. */
    val matched: String = "",
)

/**
 * Project-wide search over names, labels, IP, MAC, serials, ports and cable labels.
 * Build once per project version; [search] runs on every keystroke.
 */
class GlobalSearch(project: Project, i18n: Messages = Messages()) {

    private class Candidate(val hit: SearchHit, val fields: List<Pair<String, String?>>)

    private val candidates: List<Candidate> = run {
        val index = ProjectIndex(project)
        val hierarchy = HierarchyIndex(project)
        val places = HashMap<ObjectRef, Pair<String, String?>>()
        // "Site › Floor › RACK-A"; objects without a floor say so. Cached: ports share their device's place.
        fun place(ref: ObjectRef) = places.getOrPut(ref) {
            val areaId = hierarchy.areaId(ref)
            val site = if (ref.type == PlacementTargetType.DEVICE) index.siteOf(ref.id)?.name else null
            val parents = hierarchy.ancestors(ref).reversed().map { ObjectHierarchy.name(project, it, i18n) }
            (listOfNotNull(site, areaId?.let { index.areaName(it) } ?: i18n.text("search.noFloor")) + parents).joinToString(" › ") to areaId
        }
        fun hit(kind: HitKind, id: String, title: String, ref: ObjectRef) = place(ref).let { (text, area) -> SearchHit(kind, id, title, text, ref, area) }

        val out = mutableListOf<Candidate>()
        for (d in index.devices) {
            out += Candidate(hit(HitKind.DEVICE, d.id, d.technicalName, ObjectRef(PlacementTargetType.DEVICE, d.id)), listOf(
                i18n.text("config.name") to d.technicalName, i18n.text("config.label") to d.physicalLabel, i18n.text("search.alias") to d.alias,
                "IP" to d.ipAddress, "MAC" to d.macAddress, i18n.text("search.serial") to d.serialNumber))
        }
        for (r in project.racks) out += Candidate(hit(HitKind.RACK, r.id, r.name, ObjectRef(PlacementTargetType.RACK, r.id)), listOf(i18n.text("config.name") to r.name))
        for (p in index.ports) {
            out += Candidate(hit(HitKind.PORT, p.port.id, index.portLabel(p.port.id), ObjectRef(PlacementTargetType.DEVICE, p.device.id)),
                listOf(i18n.text("config.label") to p.port.label, "" to "${p.device.technicalName}/${p.port.name}"))
        }
        for (c in project.cables) {
            // A cable shows on the floor of its first end that has one.
            val ends = listOfNotNull(index.port(c.portAId)?.device?.id ?: c.deviceAId, index.port(c.portBId)?.device?.id ?: c.deviceBId)
                .map { ObjectRef(PlacementTargetType.DEVICE, it) }
            val ref = ends.firstOrNull { hierarchy.areaId(it) != null } ?: ends.firstOrNull() ?: continue
            val label = c.codeOrLabel ?: CableLabels.suggest(project, c, index)
            out += Candidate(hit(HitKind.CABLE, c.id, label, ref), listOf(i18n.text("quick.cableLabel") to label))
        }
        out
    }
    private val byId = candidates.associateBy { it.hit.id }

    fun search(query: String, limit: Int = 50): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        // "SW-01 P5" and "SW-01/P5" both find the port.
        val portQuery = q.replace(Regex("\\s+"), "/")
        fun rank(value: String?, query: String): Int? = value?.trim()?.takeIf { it.isNotEmpty() }?.let {
            when { it.equals(query, true) -> 0; it.startsWith(query, true) -> 1; it.contains(query, true) -> 2; else -> null }
        }
        return candidates.mapNotNull { c ->
            c.fields.mapNotNull { (label, value) ->
                // Bare port names ("P1") would match every device: the device must be in the query.
                val r = if (label.isEmpty()) portQuery.takeIf { '/' in it }?.let { rank(value, it) } else rank(value, q)
                r?.let { it to (if (label.isEmpty()) value.orEmpty() else "$label: $value") }
            }.minByOrNull { it.first }?.let { (r, matched) -> r to c.hit.copy(matched = matched) }
        }.sortedWith(compareBy({ it.first }, { it.second.kind.ordinal }, { it.second.title.lowercase() })).map { it.second }.take(limit)
    }

    /** Hits for recently opened ids, skipping those that no longer exist. */
    fun recent(ids: List<String>): List<SearchHit> = ids.mapNotNull { byId[it]?.hit }
}
