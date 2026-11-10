package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.*

/** Port type chosen first in the port flow: it fixes connector, medium and default speed. */
enum class PortKind(val connector: String, val media: String, val speed: String?, val role: String = "DATA", val shortPrefix: String) {
    RJ45("RJ45", "Copper", "1G", shortPrefix = "P"),
    SFP("SFP", "Fiber", "1G", "UPLINK", "S"),
    SFP_PLUS("SFP+", "Fiber", "10G", "UPLINK", "X"),
    SFP28("SFP28", "Fiber", "25G", "UPLINK", "Y"),
    QSFP("QSFP28", "Fiber", "100G", "UPLINK", "Q"),
    LC("LC", "Fiber", null, shortPrefix = "F"),
    SC("SC", "Fiber", null, shortPrefix = "F"),
    CONSOLE("RJ45", "Console", null, "CONSOLE", "CON"),
    C13("C13", "Power", null, "POWER", "OUT"),
    SCHUKO("Schuko", "Power", null, "POWER", "OUT");

    companion object {
        fun of(template: PortTemplate): PortKind? = entries.firstOrNull { it.connector == template.connector && it.media == template.mediaType }
            ?: entries.firstOrNull { it.connector == template.connector }
    }
}

/** Label scheme for generated port names; prefixes must be non-blank. */
enum class PortNaming {
    SHORT, INTERFACE;

    fun prefix(kind: PortKind, speed: String? = kind.speed): String = when (this) {
        SHORT -> kind.shortPrefix
        INTERFACE -> when {
            kind.role == "CONSOLE" -> "Con"
            kind.role == "POWER" -> "PS"
            else -> when (speed) {
                "100M" -> "Fa1/0/"; "2.5G" -> "Tw1/0/"; "10G" -> "Te1/1/"; "25G" -> "Twe1/1/"
                "40G" -> "Fo1/1/"; "100G" -> "Hu1/1/"; null -> kind.shortPrefix; else -> "Gi1/0/"
            }
        }
    }
}

object PortGroups {
    val counts = listOf(1, 2, 4, 5, 8, 12, 16, 24, 48)

    /** First free number for [prefix], so a new group never overlaps the existing ones. */
    fun nextStart(groups: List<PortTemplate>, prefix: String): Int =
        groups.filter { it.namePrefix == prefix }.maxOfOrNull { it.startNumber + it.portCount } ?: 1

    fun create(groups: List<PortTemplate>, kind: PortKind, count: Int, naming: PortNaming = PortNaming.SHORT, prefix: String = naming.prefix(kind),
               poe: PoeStandard? = null, paired: Boolean = false): PortTemplate =
        PortTemplate(namePrefix = prefix, startNumber = nextStart(groups, prefix), portCount = count, connector = kind.connector,
            mediaType = kind.media, speed = kind.speed, role = kind.role, poeStandard = poe, pairedSides = paired)

    /** Changes the type of an existing group while keeping its count, numbering and side. */
    fun retype(group: PortTemplate, kind: PortKind): PortTemplate =
        group.copy(connector = kind.connector, mediaType = kind.media, speed = kind.speed, role = kind.role, poeStandard = group.poeStandard.takeIf { kind == PortKind.RJ45 })

    fun range(group: PortTemplate): String = "${group.namePrefix}${group.startNumber}–${group.namePrefix}${group.startNumber + group.portCount - 1}"
}

/** A menu in the preset form; [labelled] values are translated with `preset.value.<value>`. */
data class PresetParam(val key: String, val values: List<String>, val default: String = values.first(), val labelled: Boolean = false)

data class PresetResult(val groups: List<PortTemplate>, val heightU: Int? = null, val poeBudgetWatts: Double? = null, val passive: Boolean = false)

data class DevicePreset(val id: String, val typeIds: Set<String>, val params: List<PresetParam>, val build: (Map<String, String>) -> PresetResult) {
    fun defaults(): Map<String, String> = params.associate { it.key to it.default }
    fun result(values: Map<String, String> = emptyMap()): PresetResult = build(defaults() + values)
}

/**
 * Built-in hardware presets (code only, not stored in packages).
 * Suggested PoE budget: 15.4 W per PoE port (IEEE 802.3af PSE output); editable afterwards.
 */
object DevicePresets {
    private const val AF_WATTS = 15.4
    private const val AT_WATTS = 30.0

    private fun groups(vararg specs: Triple<PortKind, Int, String?>?, poe: Map<Int, PoeStandard> = emptyMap(), paired: Boolean = false): List<PortTemplate> =
        specs.mapIndexedNotNull { i, spec -> spec?.takeIf { it.second > 0 }?.let { it to poe[i] } }.fold(emptyList()) { acc, (spec, standard) ->
            acc + PortGroups.create(acc, spec.first, spec.second, prefix = spec.third ?: spec.first.shortPrefix, poe = standard, paired = paired)
        }

    private fun uplinks(values: Map<String, String>): Triple<PortKind, Int, String?>? =
        values.getValue("uplinks").toInt().takeIf { it > 0 }?.let { Triple(PortKind.valueOf(values.getValue("uplinkKind")), it, null) }

    val all: List<DevicePreset> = listOf(
        DevicePreset("switch", setOf("switch"), listOf(
            PresetParam("ports", listOf("5", "8", "16", "24", "48"), "24"),
            PresetParam("uplinks", listOf("0", "2", "4"), "4"),
            PresetParam("uplinkKind", listOf("SFP", "SFP_PLUS"), "SFP_PLUS", labelled = true),
            PresetParam("poe", listOf("NONE", "ALL", "HALF"), labelled = true),
        )) { v ->
            val ports = v.getValue("ports").toInt()
            val poePorts = when (v.getValue("poe")) { "ALL" -> ports; "HALF" -> ports / 2; else -> 0 }
            // Split RJ45 so PoE applies to the first block only; numbering stays continuous.
            val g = groups(Triple(PortKind.RJ45, poePorts, null), Triple(PortKind.RJ45, ports - poePorts, null), uplinks(v),
                poe = if (poePorts > 0) mapOf(0 to PoeStandard.IEEE_802_3AT) else emptyMap())
            PresetResult(g, heightU = 1, poeBudgetWatts = if (poePorts > 0) poePorts * AF_WATTS else null)
        },
        DevicePreset("patch-panel", setOf("patch-panel"), listOf(
            PresetParam("ports", listOf("12", "24", "48"), "24"),
            PresetParam("kind", listOf("RJ45", "LC", "SC"), labelled = true),
        )) { v ->
            val ports = v.getValue("ports").toInt()
            PresetResult(groups(Triple(PortKind.valueOf(v.getValue("kind")), ports, null), paired = true), heightU = if (ports > 24) 2 else 1, passive = true)
        },
        DevicePreset("outlet", setOf("outlet"), listOf(PresetParam("ports", listOf("1", "2", "4"), "2"))) { v ->
            PresetResult(groups(Triple(PortKind.RJ45, v.getValue("ports").toInt(), null), paired = true), passive = true)
        },
        // Transparent radio bridge: LAN (PoE) on the front, RF on the rear; the air link is a RADIO cable between RF ports.
        DevicePreset("radio-bridge", setOf("radio-bridge"), emptyList()) {
            PresetResult(listOf(PortTemplate("LAN", portCount = 1, connector = "RJ45", mediaType = "Copper", speed = "1G",
                poeStandard = PoeStandard.IEEE_802_3AT, pairedSides = true, rearPrefix = "RF", rearConnector = "RF", rearMedia = "Radio")), passive = true)
        },
        // Wall box or splice joining two cable runs: one front/rear pass-through per run.
        DevicePreset("junction-box", setOf("junction-box"), listOf(
            PresetParam("ports", listOf("1", "2", "4"), "1"),
            PresetParam("kind", listOf("RJ45", "LC", "SC"), labelled = true),
        )) { v ->
            PresetResult(groups(Triple(PortKind.valueOf(v.getValue("kind")), v.getValue("ports").toInt(), null), paired = true), passive = true)
        },
        DevicePreset("router", setOf("router", "firewall"), listOf(
            PresetParam("lan", listOf("4", "8"), "8"),
            PresetParam("wan", listOf("1", "2")),
            PresetParam("uplinks", listOf("0", "1", "2"), "2"),
            PresetParam("uplinkKind", listOf("SFP", "SFP_PLUS"), labelled = true),
        )) { v ->
            PresetResult(groups(Triple(PortKind.RJ45, v.getValue("wan").toInt(), "WAN"), Triple(PortKind.RJ45, v.getValue("lan").toInt(), "LAN"), uplinks(v)), heightU = 1)
        },
        DevicePreset("powered-endpoint", setOf("access-point", "camera", "ip-phone", "sensor", "access-control"), listOf(
            PresetParam("ports", listOf("1", "2")),
            PresetParam("poe", listOf("IEEE_802_3AF", "IEEE_802_3AT", "IEEE_802_3BT"), "IEEE_802_3AT", labelled = true),
        )) { v ->
            PresetResult(groups(Triple(PortKind.RJ45, v.getValue("ports").toInt(), null), poe = mapOf(0 to PoeStandard.valueOf(v.getValue("poe")))))
        },
        DevicePreset("server", setOf("server", "workstation"), listOf(
            PresetParam("nics", listOf("1", "2", "4"), "2"),
            PresetParam("speed", listOf("RJ45", "SFP_PLUS"), labelled = true),
            PresetParam("management", listOf("YES", "NO"), labelled = true),
        )) { v ->
            PresetResult(groups(Triple(PortKind.valueOf(v.getValue("speed")), v.getValue("nics").toInt(), "NIC"),
                Triple(PortKind.RJ45, if (v.getValue("management") == "YES") 1 else 0, "MGMT")).map { if (it.namePrefix == "MGMT") it.copy(role = "MANAGEMENT") else it }, heightU = 1)
        },
        DevicePreset("nas", setOf("nas", "san"), listOf(PresetParam("nics", listOf("1", "2", "4"), "2"))) { v ->
            PresetResult(groups(Triple(PortKind.RJ45, v.getValue("nics").toInt(), "LAN")), heightU = 1)
        },
        DevicePreset("nvr", setOf("nvr"), listOf(PresetParam("ports", listOf("0", "4", "8", "16"), "8"))) { v ->
            val poe = v.getValue("ports").toInt()
            PresetResult(groups(Triple(PortKind.RJ45, poe, "CAM"), Triple(PortKind.RJ45, 1, "LAN"), poe = if (poe > 0) mapOf(0 to PoeStandard.IEEE_802_3AT) else emptyMap()),
                heightU = 1, poeBudgetWatts = if (poe > 0) poe * AF_WATTS else null)
        },
        DevicePreset("power", setOf("ups", "pdu", "power-supply"), listOf(
            PresetParam("outlets", listOf("6", "8", "12", "24"), "8"),
            PresetParam("kind", listOf("C13", "SCHUKO"), labelled = true),
        )) { v ->
            PresetResult(groups(Triple(PortKind.valueOf(v.getValue("kind")), v.getValue("outlets").toInt(), null)), heightU = 1)
        },
    )

    fun forType(typeId: String?): DevicePreset? = all.firstOrNull { typeId in it.typeIds }

    /** Applies preset hardware to a device draft; ports are regenerated on save. */
    fun apply(draft: MapObjectDraft, result: PresetResult): MapObjectDraft {
        val d = draft.device
        return draft.copy(portsConfigured = true, allowConnectedRemoval = false, device = d.copy(
            heightU = result.heightU?.toString() ?: d.heightU,
            hardware = d.hardware.copy(portGroups = result.groups, poeBudgetWatts = result.poeBudgetWatts ?: d.hardware.poeBudgetWatts, passive = result.passive || d.hardware.passive),
        ))
    }

    /** Max PoE draw for a standard, used to check the budget. */
    fun watts(standard: PoeStandard?): Double = when (standard) {
        PoeStandard.IEEE_802_3AF -> AF_WATTS
        PoeStandard.IEEE_802_3AT -> AT_WATTS
        PoeStandard.IEEE_802_3BT -> 90.0
        else -> 0.0
    }
}
