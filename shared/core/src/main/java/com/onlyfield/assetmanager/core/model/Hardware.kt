package com.onlyfield.assetmanager.core.model

import kotlinx.serialization.Serializable

@Serializable
data class HardwareSpec(
    val widthMm: Int? = null,
    val depthMm: Int? = null,
    val poeBudgetWatts: Double? = null,
    val redundantPower: Boolean = false,
    val features: List<String> = emptyList(),
    val portGroups: List<PortTemplate> = emptyList(),
    val passive: Boolean = false,
)

@Serializable
data class PortHardware(
    val side: PortSide? = null,
    val position: Int? = null,
    val group: String? = null,
    val mediaType: String? = null,
    val connector: String? = null,
    val speed: String? = null,
    val role: String = "DATA",
    val poeStandard: PoeStandard? = null,
    val comboKey: String? = null,
    val passageKey: String? = null,
    val opticalModule: String? = null,
    val customized: Boolean = false,
)

@Serializable
data class RackDefaults(
    val heightU: Int = 42,
    val depthMm: Int? = null,
    val mountingDepthMm: Int? = null,
    val numberingDirection: NumberingDirection = NumberingDirection.BOTTOM_TO_TOP,
    val mountingType: String? = null,
)

@Serializable
data class CableDefaults(
    val medium: CableMedium = CableMedium.ETHERNET_COPPER,
    val color: String? = null,
)

@Serializable
data class ModelField(val key: String, val value: String, val type: CustomFieldType = CustomFieldType.STRING)
