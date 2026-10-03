package com.onlyfield.assetmanager.pc.ui

import androidx.compose.ui.graphics.Color
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.exchange.ComparisonStatus

fun ComparisonStatus.toDisplayString(): String = when (this) {
    ComparisonStatus.IDENTICAL -> "Identico alla copia attuale"
    ComparisonStatus.NEWER_REVISION -> "Il pacchetto è più recente della copia attuale"
    ComparisonStatus.OLDER_REVISION -> "Attenzione: il pacchetto è meno recente della copia attuale"
    ComparisonStatus.DIVERGENT -> "Le due copie sono state modificate separatamente"
    ComparisonStatus.DIFFERENT_PROJECT -> "È un progetto diverso da quello aperto"
}

fun categoryColor(category: DeviceCategory): Color = when (category) {
    DeviceCategory.NETWORK_SWITCH -> Color(0xFF1565C0)
    DeviceCategory.PATCH_PANEL -> Color(0xFF2E7D32)
    DeviceCategory.UPS_PDU -> Color(0xFFD84315)
    DeviceCategory.SERVER_STORAGE -> Color(0xFF6A1B9A)
    DeviceCategory.CAMERA_NVR -> Color(0xFF00838F)
    DeviceCategory.SHELF -> Color(0xFF616161)
    DeviceCategory.BLANK_PANEL -> Color(0xFF455A64)
    DeviceCategory.CUSTOM -> Color(0xFF5D4037)
}
