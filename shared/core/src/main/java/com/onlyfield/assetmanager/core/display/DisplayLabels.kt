package com.onlyfield.assetmanager.core.display

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ValidationSeverity

/** Shared localized domain labels. */

fun ObservationStatus.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    ObservationStatus.VERIFIED -> i18n.text("text.32bbb08a6f47")
    ObservationStatus.TO_VERIFY -> i18n.text("text.ac4e0792e577")
    ObservationStatus.CONFLICT -> i18n.text("text.820ded51c684")
    ObservationStatus.NOT_DETECTED -> i18n.text("text.d57bb6a9696c")
}

fun EndpointStatus.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    EndpointStatus.CONNECTED -> i18n.text("text.67f964220772")
    EndpointStatus.DETACHED_TO_VERIFY -> i18n.text("text.8d8fdf8e6b6d")
    EndpointStatus.DISCONNECTED -> i18n.text("text.f9a97b4eb7a1")
    EndpointStatus.UNKNOWN -> i18n.text("text.43d7b5eae9c8")
}

fun NumberingDirection.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    NumberingDirection.BOTTOM_TO_TOP -> i18n.text("text.5c9d7720da2d")
    NumberingDirection.TOP_TO_BOTTOM -> i18n.text("text.eda932f10a7d")
}

fun DeviceCategory.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    DeviceCategory.NETWORK_SWITCH -> i18n.text("text.04dddd76838e")
    DeviceCategory.PATCH_PANEL -> i18n.text("text.e97fc26f3676")
    DeviceCategory.UPS_PDU -> i18n.text("text.648f724a35cd")
    DeviceCategory.SERVER_STORAGE -> i18n.text("text.17d4401e689d")
    DeviceCategory.CAMERA_NVR -> i18n.text("text.c24db3e9dfd7")
    DeviceCategory.SHELF -> i18n.text("text.0697cceadf3b")
    DeviceCategory.BLANK_PANEL -> i18n.text("text.3f0dca5e90e5")
    DeviceCategory.CUSTOM -> i18n.text("text.78f5742268e4")
}

fun PortSide.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    PortSide.FRONT -> i18n.text("text.da8d6541cd38")
    PortSide.REAR -> i18n.text("text.f41c7e0a6f97")
    PortSide.BOTH -> i18n.text("text.af1c8440aca6")
}

fun RackSide.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    RackSide.FRONT -> i18n.text("text.da8d6541cd38")
    RackSide.REAR -> i18n.text("text.f41c7e0a6f97")
    RackSide.BOTH -> i18n.text("text.af1c8440aca6")
}

fun MountingType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    MountingType.RACK_MOUNT -> i18n.text("text.088bed51ac75")
    MountingType.VERTICAL_MOUNT -> i18n.text("text.332772092049")
    MountingType.SHELF_MOUNT -> i18n.text("text.5ca8768f1bc1")
    MountingType.OUT_OF_RACK -> i18n.text("text.da968f7d518f")
}

fun AttachmentClassification.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    AttachmentClassification.SHAREABLE -> i18n.text("text.4500af3e8394")
    AttachmentClassification.CONFIDENTIAL -> i18n.text("text.dae42950d62c")
    AttachmentClassification.REVIEW_REQUIRED -> i18n.text("text.6ed2123cae2f")
}

fun AttachmentType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    AttachmentType.IMAGE -> i18n.text("text.850b6f310354")
    AttachmentType.PDF -> "PDF"
    AttachmentType.DOCUMENT -> i18n.text("text.cf4279e00d07")
    AttachmentType.OTHER -> i18n.text("text.78f5742268e4")
}

fun AttachmentTargetType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    AttachmentTargetType.PROJECT -> i18n.text("text.b7700d71d0ce")
    AttachmentTargetType.RACK -> i18n.text("text.4cd265c2b8c6")
    AttachmentTargetType.DEVICE -> i18n.text("text.cf301d95d32c")
    AttachmentTargetType.AREA -> i18n.text("text.7b417b994cc4")
    AttachmentTargetType.CABLE -> i18n.text("text.89dbe18e8407")
    AttachmentTargetType.PORT -> i18n.text("config.port")
}

fun AnnotationType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    AnnotationType.TEXT -> i18n.text("text.ebb9e60cfec0")
    AnnotationType.ARROW -> i18n.text("text.36f657fcb11e")
    AnnotationType.RECTANGLE -> i18n.text("text.77056a6dfa2c")
    AnnotationType.CIRCLE -> i18n.text("text.3fea581550f4")
    AnnotationType.HIGHLIGHT_ZONE -> i18n.text("text.a056b1dfee92")
}

fun PlacementTargetType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    PlacementTargetType.RACK -> i18n.text("text.4cd265c2b8c6")
    PlacementTargetType.DEVICE -> i18n.text("text.cf301d95d32c")
}

fun CredentialType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    CredentialType.PASSWORD -> i18n.text("text.e7cf3ef4f17c")
    CredentialType.SSH_KEY -> i18n.text("text.00deae8cd541")
    CredentialType.SNMP_COMMUNITY -> i18n.text("text.83f399bfaa21")
    CredentialType.OTHER -> i18n.text("text.78f5742268e4")
}

fun CableMedium.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    CableMedium.ETHERNET_COPPER -> i18n.text("text.93b38a860ae6")
    CableMedium.DAC -> "DAC"
    CableMedium.AOC -> "AOC"
    CableMedium.FIBER_OVERALL -> i18n.text("text.3a17868d205c")
    CableMedium.RADIO -> i18n.text("medium.radio")
    CableMedium.CONSOLE -> i18n.text("text.29a40861bafe")
    CableMedium.OTHER -> i18n.text("text.78f5742268e4")
    CableMedium.UNKNOWN -> i18n.text("text.43d7b5eae9c8")
}

fun VlanScopeType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    VlanScopeType.PROJECT -> i18n.text("text.b7700d71d0ce")
    VlanScopeType.BUSINESS_UNIT -> i18n.text("text.e4de7d26b141")
    VlanScopeType.SITE -> i18n.text("text.f163aa3f6310")
    VlanScopeType.DEVICE -> i18n.text("text.cf301d95d32c")
}

fun PortVlanMode.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    PortVlanMode.ACCESS -> i18n.text("text.ec5ba0abb717")
    PortVlanMode.TRUNK -> i18n.text("text.10c81e7cf2ea")
    PortVlanMode.HYBRID -> i18n.text("text.d5c866a822b0")
    PortVlanMode.UNTAGGED -> i18n.text("text.9a28cbfaf271")
    PortVlanMode.TAGGED -> i18n.text("text.e9eda90d969f")
    PortVlanMode.UNSPECIFIED -> i18n.text("text.0efe5beee12f")
}

fun LagMode.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    LagMode.LACP -> "LACP"
    LagMode.STATIC -> i18n.text("text.a3899a90c9dc")
    LagMode.OTHER -> i18n.text("text.78f5742268e4")
    LagMode.UNSPECIFIED -> i18n.text("text.29aa7fddd7bf")
}

fun WanVpnType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    WanVpnType.WAN -> "WAN"
    WanVpnType.VPN -> "VPN"
    WanVpnType.INTERNET -> i18n.text("text.57e8a431deec")
    WanVpnType.OTHER -> i18n.text("text.78f5742268e4")
}

fun CustomFieldType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    CustomFieldType.STRING -> i18n.text("text.ebb9e60cfec0")
    CustomFieldType.NUMBER -> i18n.text("text.ad14784441da")
    CustomFieldType.BOOLEAN -> i18n.text("text.47fbffad2cae")
    CustomFieldType.DATE -> i18n.text("text.cec3a9b89b2e")
}

fun PowerFeedType.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    PowerFeedType.PRIMARY_A -> i18n.text("text.37959ecfb327")
    PowerFeedType.SECONDARY_B -> i18n.text("text.584f9f0b938f")
    PowerFeedType.UPS_BACKUP -> i18n.text("text.722b056b65fd")
    PowerFeedType.PDU_DISTRIBUTION -> i18n.text("text.bfe399cfb57e")
    PowerFeedType.MAINS_DIRECT -> i18n.text("text.930c6d2cd50e")
    PowerFeedType.OTHER -> i18n.text("text.78f5742268e4")
    PowerFeedType.UNKNOWN -> i18n.text("text.250d49293bbe")
}

fun PoeRole.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    PoeRole.PSE_SOURCE -> i18n.text("text.3b2832bfad96")
    PoeRole.PD_SINK -> i18n.text("text.28b16667cb5a")
    PoeRole.PASSIVE_INJECTOR -> i18n.text("text.18b61ca94636")
    PoeRole.NONE -> i18n.text("text.aa201262e9a7")
}

fun PoeStandard.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    PoeStandard.IEEE_802_3AF -> i18n.text("text.6658e04f0146")
    PoeStandard.IEEE_802_3AT -> i18n.text("text.1b9dadfc6c6e")
    PoeStandard.IEEE_802_3BT -> i18n.text("text.68a3de05ed07")
    PoeStandard.PASSIVE_24V -> i18n.text("text.a4e21471fd4f")
    PoeStandard.PASSIVE_48V -> i18n.text("text.1ced0fe18bef")
    PoeStandard.OTHER -> i18n.text("text.78f5742268e4")
}

fun BadgeCategory.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    BadgeCategory.VLAN -> "VLAN"
    BadgeCategory.MEDIUM -> i18n.text("text.2de5f6131596")
    BadgeCategory.POE -> i18n.text("text.64f63dbe7bbe")
    BadgeCategory.UPS_DEPENDENCY -> i18n.text("text.f66083a59c7f")
    BadgeCategory.COVERAGE -> i18n.text("text.4cd0d886b40b")
    BadgeCategory.OPEN_ISSUE -> i18n.text("text.7dce0a3c04d1")
    BadgeCategory.FREE_LABEL -> i18n.text("text.bc2c4cb17f02")
}

fun ValidationSeverity.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    ValidationSeverity.STRUCTURAL_ERROR -> i18n.text("text.d2799174e404")
    ValidationSeverity.DOCUMENTARY_WARNING -> i18n.text("text.d8a8733a7084")
}

/** Stored target types. */
object EntityTypeLabels {
    fun of(type: String, i18n: Messages = Messages()): String = when (type.uppercase()) {
        "PROJECT" -> i18n.text("text.b7700d71d0ce")
        "BUSINESS_UNIT" -> i18n.text("text.e4de7d26b141")
        "SITE" -> i18n.text("text.f163aa3f6310")
        "AREA" -> i18n.text("text.024dc204d7ba")
        "RACK" -> i18n.text("text.4cd265c2b8c6")
        "DEVICE" -> i18n.text("text.cf301d95d32c")
        "PORT" -> i18n.text("text.946d1f8153ce")
        "CABLE" -> i18n.text("text.89dbe18e8407")
        "ATTACHMENT" -> i18n.text("text.59cc6c3e1526")
        "CREDENTIAL" -> i18n.text("text.602206d4ebfc")
        else -> type.lowercase().replaceFirstChar { it.uppercase() }
    }
}
