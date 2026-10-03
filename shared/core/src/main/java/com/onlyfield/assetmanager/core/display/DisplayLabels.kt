package com.onlyfield.assetmanager.core.display

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ValidationSeverity

/**
 * Italian, user-facing labels for domain enums, shared by the Android and Windows UIs.
 * UIs must never show raw enum names to the user.
 */

fun ObservationStatus.toDisplayString(): String = when (this) {
    ObservationStatus.VERIFIED -> "Verificato"
    ObservationStatus.TO_VERIFY -> "Da verificare"
    ObservationStatus.CONFLICT -> "In conflitto"
    ObservationStatus.NOT_DETECTED -> "Non rilevato"
}

fun EndpointStatus.toDisplayString(): String = when (this) {
    EndpointStatus.CONNECTED -> "Collegato"
    EndpointStatus.DETACHED_TO_VERIFY -> "Scollegato, da verificare"
    EndpointStatus.DISCONNECTED -> "Libero"
    EndpointStatus.UNKNOWN -> "Sconosciuto"
}

fun NumberingDirection.toDisplayString(): String = when (this) {
    NumberingDirection.BOTTOM_TO_TOP -> "Dal basso verso l'alto"
    NumberingDirection.TOP_TO_BOTTOM -> "Dall'alto verso il basso"
}

fun DeviceCategory.toDisplayString(): String = when (this) {
    DeviceCategory.NETWORK_SWITCH -> "Switch di rete"
    DeviceCategory.PATCH_PANEL -> "Patch panel"
    DeviceCategory.UPS_PDU -> "UPS / PDU"
    DeviceCategory.SERVER_STORAGE -> "Server / Storage"
    DeviceCategory.CAMERA_NVR -> "Telecamera / NVR"
    DeviceCategory.SHELF -> "Ripiano"
    DeviceCategory.BLANK_PANEL -> "Pannello cieco"
    DeviceCategory.CUSTOM -> "Altro"
}

fun PortSide.toDisplayString(): String = when (this) {
    PortSide.FRONT -> "Fronte"
    PortSide.REAR -> "Retro"
    PortSide.BOTH -> "Fronte e retro"
}

fun RackSide.toDisplayString(): String = when (this) {
    RackSide.FRONT -> "Fronte"
    RackSide.REAR -> "Retro"
    RackSide.BOTH -> "Fronte e retro"
}

fun MountingType.toDisplayString(): String = when (this) {
    MountingType.RACK_MOUNT -> "A rack"
    MountingType.VERTICAL_MOUNT -> "Verticale"
    MountingType.SHELF_MOUNT -> "Su ripiano"
    MountingType.OUT_OF_RACK -> "Fuori rack"
}

fun AttachmentClassification.toDisplayString(): String = when (this) {
    AttachmentClassification.SHAREABLE -> "Condivisibile"
    AttachmentClassification.CONFIDENTIAL -> "Riservato"
    AttachmentClassification.REVIEW_REQUIRED -> "Da riesaminare"
}

fun AttachmentType.toDisplayString(): String = when (this) {
    AttachmentType.IMAGE -> "Immagine"
    AttachmentType.PDF -> "PDF"
    AttachmentType.DOCUMENT -> "Documento"
    AttachmentType.OTHER -> "Altro"
}

fun AttachmentTargetType.toDisplayString(): String = when (this) {
    AttachmentTargetType.PROJECT -> "Progetto"
    AttachmentTargetType.RACK -> "Rack"
    AttachmentTargetType.DEVICE -> "Apparato"
    AttachmentTargetType.AREA -> "Area"
}

fun AnnotationType.toDisplayString(): String = when (this) {
    AnnotationType.TEXT -> "Testo"
    AnnotationType.ARROW -> "Freccia"
    AnnotationType.RECTANGLE -> "Rettangolo"
    AnnotationType.CIRCLE -> "Cerchio"
    AnnotationType.HIGHLIGHT_ZONE -> "Zona evidenziata"
}

fun PlacementTargetType.toDisplayString(): String = when (this) {
    PlacementTargetType.RACK -> "Rack"
    PlacementTargetType.DEVICE -> "Apparato"
}

fun CredentialType.toDisplayString(): String = when (this) {
    CredentialType.PASSWORD -> "Password"
    CredentialType.SSH_KEY -> "Chiave SSH"
    CredentialType.SNMP_COMMUNITY -> "Community SNMP"
    CredentialType.OTHER -> "Altro"
}

fun CableMedium.toDisplayString(): String = when (this) {
    CableMedium.ETHERNET_COPPER -> "Rame (Ethernet)"
    CableMedium.DAC -> "DAC"
    CableMedium.AOC -> "AOC"
    CableMedium.FIBER_OVERALL -> "Fibra ottica"
    CableMedium.CONSOLE -> "Console"
    CableMedium.OTHER -> "Altro"
    CableMedium.UNKNOWN -> "Sconosciuto"
}

fun CableOrientation.toDisplayString(): String = when (this) {
    CableOrientation.NONE -> "Nessuna"
    CableOrientation.A_TO_B -> "Da A verso B"
    CableOrientation.B_TO_A -> "Da B verso A"
    CableOrientation.BOTH -> "Bidirezionale"
}

fun VlanScopeType.toDisplayString(): String = when (this) {
    VlanScopeType.PROJECT -> "Progetto"
    VlanScopeType.BUSINESS_UNIT -> "Business unit"
    VlanScopeType.SITE -> "Sede"
    VlanScopeType.DEVICE -> "Apparato"
}

fun PortVlanMode.toDisplayString(): String = when (this) {
    PortVlanMode.ACCESS -> "Access"
    PortVlanMode.TRUNK -> "Trunk"
    PortVlanMode.HYBRID -> "Ibrida"
    PortVlanMode.UNTAGGED -> "Untagged"
    PortVlanMode.TAGGED -> "Tagged"
    PortVlanMode.UNSPECIFIED -> "Non specificata"
}

fun LagMode.toDisplayString(): String = when (this) {
    LagMode.LACP -> "LACP"
    LagMode.STATIC -> "Statico"
    LagMode.OTHER -> "Altro"
    LagMode.UNSPECIFIED -> "Non specificato"
}

fun WanVpnType.toDisplayString(): String = when (this) {
    WanVpnType.WAN -> "WAN"
    WanVpnType.VPN -> "VPN"
    WanVpnType.INTERNET -> "Internet"
    WanVpnType.OTHER -> "Altro"
}

fun CustomFieldType.toDisplayString(): String = when (this) {
    CustomFieldType.STRING -> "Testo"
    CustomFieldType.NUMBER -> "Numero"
    CustomFieldType.BOOLEAN -> "Sì/No"
    CustomFieldType.DATE -> "Data"
}

fun PowerFeedType.toDisplayString(): String = when (this) {
    PowerFeedType.PRIMARY_A -> "Primaria (A)"
    PowerFeedType.SECONDARY_B -> "Secondaria (B)"
    PowerFeedType.UPS_BACKUP -> "UPS di backup"
    PowerFeedType.PDU_DISTRIBUTION -> "Da PDU"
    PowerFeedType.MAINS_DIRECT -> "Rete diretta"
    PowerFeedType.OTHER -> "Altro"
    PowerFeedType.UNKNOWN -> "Sconosciuta"
}

fun PoeRole.toDisplayString(): String = when (this) {
    PoeRole.PSE_SOURCE -> "Eroga (PSE)"
    PoeRole.PD_SINK -> "Alimentato (PD)"
    PoeRole.PASSIVE_INJECTOR -> "Iniettore passivo"
    PoeRole.NONE -> "Nessuno"
}

fun PoeStandard.toDisplayString(): String = when (this) {
    PoeStandard.IEEE_802_3AF -> "802.3af (PoE)"
    PoeStandard.IEEE_802_3AT -> "802.3at (PoE+)"
    PoeStandard.IEEE_802_3BT -> "802.3bt (PoE++)"
    PoeStandard.PASSIVE_24V -> "Passivo 24 V"
    PoeStandard.PASSIVE_48V -> "Passivo 48 V"
    PoeStandard.OTHER -> "Altro"
}

fun BadgeCategory.toDisplayString(): String = when (this) {
    BadgeCategory.VLAN -> "VLAN"
    BadgeCategory.MEDIUM -> "Mezzo"
    BadgeCategory.POE -> "PoE"
    BadgeCategory.UPS_DEPENDENCY -> "Dipendenza UPS"
    BadgeCategory.COVERAGE -> "Copertura"
    BadgeCategory.OPEN_ISSUE -> "Problema aperto"
    BadgeCategory.FREE_LABEL -> "Etichetta libera"
}

fun ValidationSeverity.toDisplayString(): String = when (this) {
    ValidationSeverity.STRUCTURAL_ERROR -> "Errore strutturale"
    ValidationSeverity.DOCUMENTARY_WARNING -> "Avviso documentale"
}

/** Target types stored as strings in [DocumentBadge], [CustomExtraField] and [TrashItem]. */
object EntityTypeLabels {
    fun of(type: String): String = when (type.uppercase()) {
        "PROJECT" -> "Progetto"
        "BUSINESS_UNIT" -> "Business unit"
        "SITE" -> "Sede"
        "AREA" -> "Area"
        "RACK" -> "Rack"
        "DEVICE" -> "Apparato"
        "PORT" -> "Porta"
        "CABLE" -> "Cavo"
        "ATTACHMENT" -> "Allegato"
        "CREDENTIAL" -> "Credenziale"
        else -> type.lowercase().replaceFirstChar { it.uppercase() }
    }
}
