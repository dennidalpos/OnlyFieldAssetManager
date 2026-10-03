package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.core.i18n.Messages

import androidx.compose.ui.graphics.Color
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.exchange.ComparisonStatus

fun ComparisonStatus.toDisplayString(i18n: Messages = Messages()): String = when (this) {
    ComparisonStatus.IDENTICAL -> i18n.text("text.55dbfb0ee9de")
    ComparisonStatus.NEWER_REVISION -> i18n.text("text.9674db0772d1")
    ComparisonStatus.OLDER_REVISION -> i18n.text("text.4e6073a1d4d3")
    ComparisonStatus.DIVERGENT -> i18n.text("text.ebf3f84d3433")
    ComparisonStatus.DIFFERENT_PROJECT -> i18n.text("text.1fc92901e187")
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
