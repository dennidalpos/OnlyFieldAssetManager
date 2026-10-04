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

