package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages

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
    val description: String get() = localizedDescription(Messages())
    fun localizedDescription(i18n: Messages): String = when {
            local == null -> if (hasBase) i18n.text("text.4a4cfd3fb9e3") else i18n.text("text.0f3e052956fd")
            incoming == null -> if (hasBase) i18n.text("text.d6774ac444fe") else i18n.text("text.d5b87c5397b6")
            else -> i18n.text("text.2f861673cd6a")
        }

    /** Changed fields as "campo: mio → importato" (nested values are shown compactly). */
    fun differences(i18n: Messages = Messages()): List<String> {
        if (local == null || incoming == null) return emptyList()
        return (local.keys + incoming.keys).filter { local[it] != incoming[it] }.map { k ->
            "$k: ${shortValue(local[k], i18n = i18n)} → ${shortValue(incoming[k], i18n = i18n)}"
        }
    }

    private fun shortValue(e: JsonElement?, i18n: Messages = Messages()): String = when (e) {
        null, is kotlinx.serialization.json.JsonNull -> "—"
        is JsonPrimitive -> e.contentOrNull ?: "—"
        is JsonArray -> i18n.text("text.d64ee382dcac", e.size)
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
    fun resolve(choices: Map<MergeKey, MergeSide>, nowMs: Long = System.currentTimeMillis(), i18n: Messages = Messages()): Project {
        val nodes = LinkedHashMap<MergeKey, ProjectMerger.Node>()
        for (key in order) {
            val node = if (key in decided) decided[key]
            else if (choices[key] == MergeSide.INCOMING) incoming[key] else local[key]
            if (node != null) nodes[key] = node
        }
        return ProjectMerger.rebuild(nodes, listKinds, nowMs, i18n = i18n)
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

    fun merge(base: Project?, local: Project, incoming: Project, i18n: Messages = Messages()): MergeResult {
        require(local.id == incoming.id) { i18n.text("text.8eb69c5de288") }
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
                else -> conflicts += MergeConflict(key, kindLabel(key.kind, i18n = i18n), nameOf(lv ?: iv, i18n = i18n), lv?.json, iv?.json, hasBase = b != null)
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

    internal fun rebuild(nodes: Map<MergeKey, Node>, listKinds: List<String>, nowMs: Long, i18n: Messages = Messages()): Project {
        fun children(kind: String, parent: MergeKey) = nodes.filter { (k, n) -> k.kind == kind && n.parent == parent }
        val bus = nodes.filterKeys { it.kind == BU }.toMutableMap()
        // Children whose parent no longer exists are kept under the first business unit (no data loss).
        val orphans = nodes.filter { (k, n) -> k.kind in nested && n.parent != null && n.parent !in nodes }
        if (orphans.isNotEmpty() && bus.isEmpty()) {
            val id = UUID.randomUUID().toString()
            bus[MergeKey(BU, id)] = Node(JsonObject(mapOf("id" to JsonPrimitive(id), "name" to JsonPrimitive(i18n.text("text.bbd413756f43")))), null)
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

    private fun nameOf(node: Node?, i18n: Messages = Messages()): String {
        val j = node?.json ?: return "—"
        return listOf("technicalName", "name", "codeOrLabel", "cidrBlock", "feedName", "username", "title", "label")
            .firstNotNullOfOrNull { (j[it] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            ?: (j["vlanId"] as? JsonPrimitive)?.contentOrNull?.let { i18n.text("text.da4da5c165af", it) }
            ?: j.id()
    }

    private fun kindLabel(kind: String, i18n: Messages = Messages()) = when (kind) {
        "project" -> i18n.text("text.388c767cf744")
        BU -> i18n.text("text.e4de7d26b141")
        "sites" -> i18n.text("text.f163aa3f6310")
        "areas" -> i18n.text("text.7b417b994cc4")
        "devices" -> i18n.text("text.cf301d95d32c")
        "credentials" -> i18n.text("text.602206d4ebfc")
        "racks" -> i18n.text("text.4cd265c2b8c6")
        "deviceModels" -> i18n.text("text.90c2d339a9d5")
        "attachments" -> i18n.text("text.59cc6c3e1526")
        "annotations" -> i18n.text("text.b3c719ac7329")
        "floorplanPlacements" -> i18n.text("text.d409fb630a1f")
        "cables" -> i18n.text("text.89dbe18e8407")
        "objectTypes" -> i18n.text("text.f0ccc7d5d697")
        "cableRoutes" -> i18n.text("text.bab757fdc2a2")
        "objectContainments" -> i18n.text("text.e675c751e388")
        "sharedPathSegments" -> i18n.text("text.9ea2e0562fb5")
        "panelMappings" -> i18n.text("text.6adce9b9a19d")
        "vlans" -> "VLAN"
        "subnets" -> i18n.text("text.bfea90e5ae18")
        "portVlanMemberships" -> i18n.text("text.dd7d04274921")
        "logicalInterfaces" -> i18n.text("text.a86468029422")
        "lagGroups" -> "LAG"
        "deviceConfigurations" -> i18n.text("text.7c585e06d4ec")
        "wanVpnConnections" -> i18n.text("text.9736145d9e40")
        "videoSurveillanceMappings" -> i18n.text("text.87b2c263a710")
        "customExtraFields" -> i18n.text("text.eef8ce706be5")
        "powerFeeds" -> i18n.text("text.acedc1948e5f")
        "poeMappings" -> i18n.text("text.64f63dbe7bbe")
        "documentBadges" -> i18n.text("text.002474e36821")
        else -> kind
    }
}
