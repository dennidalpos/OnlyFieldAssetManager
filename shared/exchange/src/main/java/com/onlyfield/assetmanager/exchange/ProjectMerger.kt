package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Project
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

enum class MergeSide { LOCAL, INCOMING }

/** One mergeable element: `kind` is the JSON list it lives in ("devices", "vlans", ...), or "project". */
data class MergeKey(val kind: String, val id: String)

/** An element changed differently in the two copies; the user picks a side. Null json = absent. */
data class MergeConflict(
    val key: MergeKey,
    val kindLabel: String,
    val name: String,
    val local: JsonObject?,
    val incoming: JsonObject?,
    private val hasBase: Boolean,
) {
    val description: String
        get() = when {
            local == null -> if (hasBase) "Eliminato in questa copia, modificato nel pacchetto" else "Presente solo nel pacchetto"
            incoming == null -> if (hasBase) "Modificato in questa copia, eliminato nel pacchetto" else "Presente solo in questa copia"
            else -> "Modificato in entrambe le copie"
        }

    /** Changed fields as "campo: mio → importato" (nested values are shown compactly). */
    fun differences(): List<String> {
        if (local == null || incoming == null) return emptyList()
        return (local.keys + incoming.keys).filter { local[it] != incoming[it] }.map { k ->
            "$k: ${shortValue(local[k])} → ${shortValue(incoming[k])}"
        }
    }

    private fun shortValue(e: JsonElement?): String = when (e) {
        null, is kotlinx.serialization.json.JsonNull -> "—"
        is JsonPrimitive -> e.contentOrNull ?: "—"
        is JsonArray -> "${e.size} elementi"
        is JsonObject -> "{…}"
    }
}

/** Outcome of [ProjectMerger.merge]: automatic changes are already decided, conflicts wait for [resolve]. */
class MergeResult internal constructor(
    val conflicts: List<MergeConflict>,
    /** Elements taken from the package without asking. */
    val autoApplied: Int,
    private val decided: Map<MergeKey, ProjectMerger.Node?>,
    private val local: Map<MergeKey, ProjectMerger.Node>,
    private val incoming: Map<MergeKey, ProjectMerger.Node>,
    private val order: List<MergeKey>,
    private val listKinds: List<String>,
) {
    /** Builds the merged project; a conflict without a choice keeps the local version. */
    fun resolve(choices: Map<MergeKey, MergeSide>, nowMs: Long = System.currentTimeMillis()): Project {
        val nodes = LinkedHashMap<MergeKey, ProjectMerger.Node>()
        for (key in order) {
            val node = if (key in decided) decided[key]
            else if (choices[key] == MergeSide.INCOMING) incoming[key] else local[key]
            if (node != null) nodes[key] = node
        }
        return ProjectMerger.rebuild(nodes, listKinds, nowMs)
    }
}

/**
 * Three-way merge of two copies of the same project, element by element (matched by id).
 * With a base (last synced snapshot) a change made on one side only is applied automatically;
 * without it every difference is a conflict.
 */
object ProjectMerger {

    internal data class Node(val json: JsonObject, val parent: MergeKey?)

    private val json = PackageSerializer.jsonConfig
    private const val BU = "businessUnits"
    private val nested = setOf("sites", "areas", "devices")

    fun merge(base: Project?, local: Project, incoming: Project): MergeResult {
        require(local.id == incoming.id) { "Merge requires two copies of the same project" }
        val b = base?.let(::flatten)
        val l = flatten(local)
        val i = flatten(incoming)
        val order = (l.keys + i.keys).distinct()
        val decided = LinkedHashMap<MergeKey, Node?>()
        val conflicts = mutableListOf<MergeConflict>()
        var auto = 0
        for (key in order) {
            val lv = l[key]
            val iv = i[key]
            val bv = b?.get(key)
            when {
                lv == iv -> decided[key] = lv
                b != null && lv == bv -> { decided[key] = iv; auto++ }
                b != null && iv == bv -> decided[key] = lv
                else -> conflicts += MergeConflict(key, kindLabel(key.kind), nameOf(lv ?: iv), lv?.json, iv?.json, hasBase = b != null)
            }
        }
        val listKinds = (listKindsOf(local) + listKindsOf(incoming)).distinct()
        return MergeResult(conflicts, auto, decided, l, i, order, listKinds)
    }

    // --- Flatten / rebuild ------------------------------------------------------------------

    private fun root(p: Project) = json.encodeToJsonElement(Project.serializer(), com.onlyfield.assetmanager.core.model.ObjectHierarchy.normalize(p)).jsonObject

    /** Top-level lists of entities with an id (all of them except business units). */
    private fun listKindsOf(p: Project) = root(p).filter { (k, v) -> k != BU && isEntityList(v) }.keys.toList()

    private fun isEntityList(v: JsonElement) = v is JsonArray && v.all { it is JsonObject && "id" in it.jsonObject }

    private fun JsonObject.id() = getValue("id").jsonPrimitive.content

    private fun JsonObject.without(vararg keys: String) = JsonObject(filterKeys { it !in keys })

    private fun flatten(p: Project): LinkedHashMap<MergeKey, Node> {
        val r = root(p)
        val out = LinkedHashMap<MergeKey, Node>()
        // Project fields; updatedEpochMs changes on every save and is set again on rebuild.
        out[MergeKey("project", "project")] = Node(JsonObject(r.filter { (k, v) -> k != "updatedEpochMs" && k != BU && !isEntityList(v) }), null)
        for (buEl in r.getValue(BU).jsonArray) {
            val bu = buEl.jsonObject
            val buKey = MergeKey(BU, bu.id())
            out[buKey] = Node(bu.without("sites", "areas", "devices"), null)
            for (siteEl in bu["sites"]?.jsonArray.orEmpty()) {
                val site = siteEl.jsonObject
                val siteKey = MergeKey("sites", site.id())
                out[siteKey] = Node(site.without("areas"), buKey)
                site["areas"]?.jsonArray.orEmpty().forEach { a -> out[MergeKey("areas", a.jsonObject.id())] = Node(a.jsonObject, siteKey) }
            }
            bu["areas"]?.jsonArray.orEmpty().forEach { a -> out[MergeKey("areas", a.jsonObject.id())] = Node(a.jsonObject, buKey) }
            bu["devices"]?.jsonArray.orEmpty().forEach { d -> out[MergeKey("devices", d.jsonObject.id())] = Node(d.jsonObject, buKey) }
        }
        for ((kind, value) in r) {
            if (kind == BU || !isEntityList(value)) continue
            value.jsonArray.forEach { e -> out[MergeKey(kind, e.jsonObject.id())] = Node(e.jsonObject, null) }
        }
        return out
    }

    internal fun rebuild(nodes: Map<MergeKey, Node>, listKinds: List<String>, nowMs: Long): Project {
        fun children(kind: String, parent: MergeKey) = nodes.filter { (k, n) -> k.kind == kind && n.parent == parent }
        val bus = nodes.filterKeys { it.kind == BU }.toMutableMap()
        // Children whose parent no longer exists are kept under the first business unit (no data loss).
        val orphans = nodes.filter { (k, n) -> k.kind in nested && n.parent != null && n.parent !in nodes }
        if (orphans.isNotEmpty() && bus.isEmpty()) {
            val id = UUID.randomUUID().toString()
            bus[MergeKey(BU, id)] = Node(JsonObject(mapOf("id" to JsonPrimitive(id), "name" to JsonPrimitive("Elementi recuperati"))), null)
        }
        val firstBu = bus.keys.firstOrNull()
        val buArray = JsonArray(bus.map { (buKey, buNode) ->
            fun adopt(kind: String) = children(kind, buKey) + if (buKey == firstBu) orphans.filterKeys { it.kind == kind } else emptyMap()
            val sites = adopt("sites").map { (siteKey, site) ->
                JsonObject(site.json + ("areas" to JsonArray(children("areas", siteKey).values.map { it.json })))
            }
            JsonObject(
                buNode.json + mapOf(
                    "sites" to JsonArray(sites),
                    "areas" to JsonArray(adopt("areas").values.map { it.json }),
                    "devices" to JsonArray(adopt("devices").values.map { it.json }),
                )
            )
        })
        val meta = nodes[MergeKey("project", "project")]?.json ?: JsonObject(emptyMap())
        val lists = listKinds.associateWith { kind -> JsonArray(nodes.filterKeys { it.kind == kind }.values.map { it.json }) }
        val project = JsonObject(meta + lists + mapOf(BU to buArray, "updatedEpochMs" to JsonPrimitive(nowMs)))
        return com.onlyfield.assetmanager.core.model.ObjectHierarchy.synchronize(json.decodeFromJsonElement(Project.serializer(), project))
    }

    // --- Labels -----------------------------------------------------------------------------

    private fun nameOf(node: Node?): String {
        val j = node?.json ?: return "—"
        return listOf("technicalName", "name", "codeOrLabel", "cidrBlock", "feedName", "username", "title", "label")
            .firstNotNullOfOrNull { (j[it] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            ?: (j["vlanId"] as? JsonPrimitive)?.contentOrNull?.let { "VLAN $it" }
            ?: j.id()
    }

    private fun kindLabel(kind: String) = when (kind) {
        "project" -> "Dati del progetto"
        BU -> "Business unit"
        "sites" -> "Sede"
        "areas" -> "Piano / zona"
        "devices" -> "Apparato"
        "credentials" -> "Credenziale"
        "racks" -> "Rack"
        "deviceModels" -> "Modello"
        "attachments" -> "Allegato"
        "annotations" -> "Annotazione"
        "floorplanPlacements" -> "Posizione in planimetria"
        "cables" -> "Cavo"
        "objectTypes" -> "Tipologia"
        "cableRoutes" -> "Percorso sulla mappa"
        "objectContainments" -> "Contenitore dell’oggetto"
        "sharedPathSegments" -> "Percorso"
        "panelMappings" -> "Permutazione"
        "vlans" -> "VLAN"
        "subnets" -> "Subnet"
        "portVlanMemberships" -> "Porta in VLAN"
        "logicalInterfaces" -> "Interfaccia"
        "lagGroups" -> "LAG"
        "deviceConfigurations" -> "Configurazione"
        "wanVpnConnections" -> "Connessione WAN/VPN"
        "videoSurveillanceMappings" -> "Videosorveglianza"
        "customExtraFields" -> "Campo extra"
        "powerFeeds" -> "Alimentazione"
        "poeMappings" -> "PoE"
        "documentBadges" -> "Badge"
        else -> kind
    }
}
