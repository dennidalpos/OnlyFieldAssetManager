package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.i18n.Messages

/**
 * Field-level checks used by the desktop forms. Each function returns an error in the selected language (Italian by default),
 * or null when the value is acceptable. Empty optional values are always accepted.
 */
object FieldValidators {

    fun required(text: String, what: String? = null, i18n: Messages = Messages()): String? =
        if (text.isBlank()) i18n.text("text.6e53f0e55bf5", what ?: i18n.text("text.45789cc13c6b")) else null

    fun int(text: String, min: Int? = null, max: Int? = null, required: Boolean = false, i18n: Messages = Messages()): String? {
        if (text.isBlank()) return if (required) i18n.text("text.ebabdc9dd33e") else null
        val value = text.trim().toIntOrNull() ?: return i18n.text("text.698b1051bf1f")
        if (min != null && value < min) return i18n.text("text.d745db3f66f9", min)
        if (max != null && value > max) return i18n.text("text.9027e4e572df", max)
        return null
    }

    fun decimal(text: String, min: Double? = null, max: Double? = null, i18n: Messages = Messages()): String? {
        if (text.isBlank()) return null
        val value = parseDecimal(text) ?: return i18n.text("text.f88d461ec798")
        if (min != null && value < min) return i18n.text("text.d745db3f66f9", formatNumber(min))
        if (max != null && value > max) return i18n.text("text.9027e4e572df", formatNumber(max))
        return null
    }

    fun ipv4(text: String, i18n: Messages = Messages()): String? {
        if (text.isBlank()) return null
        return if (isIpv4(text.trim())) null else i18n.text("text.49e1d3402a2e")
    }

    fun cidr(text: String, required: Boolean = false, i18n: Messages = Messages()): String? {
        if (text.isBlank()) return if (required) i18n.text("text.da767adf5760") else null
        val parts = text.trim().split("/")
        val ok = parts.size == 2 && isIpv4(parts[0]) && parts[1].toIntOrNull()?.let { it in 0..32 } == true
        return if (ok) null else i18n.text("text.d2eb9b980937")
    }

    fun mac(text: String, i18n: Messages = Messages()): String? {
        if (text.isBlank()) return null
        val ok = Regex("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$").matches(text.trim()) ||
            Regex("^([0-9A-Fa-f]{4}\\.){2}[0-9A-Fa-f]{4}$").matches(text.trim())
        return if (ok) null else i18n.text("text.eaa6199e8b97")
    }

    /** Accepts both "1.5" and the Italian "1,5". */
    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    fun parseInt(text: String): Int? = text.trim().toIntOrNull()

    private fun isIpv4(text: String): Boolean {
        val parts = text.split(".")
        return parts.size == 4 && parts.all { p -> p.isNotEmpty() && p.length <= 3 && p.all(Char::isDigit) && p.toInt() in 0..255 }
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
