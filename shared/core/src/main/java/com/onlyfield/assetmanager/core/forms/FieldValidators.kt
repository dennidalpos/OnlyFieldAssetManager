package com.onlyfield.assetmanager.core.forms

/**
 * Field-level checks used by the desktop forms. Each function returns an Italian error message,
 * or null when the value is acceptable. Empty optional values are always accepted.
 */
object FieldValidators {

    fun required(text: String, what: String = "Campo"): String? =
        if (text.isBlank()) "$what obbligatorio" else null

    fun int(text: String, min: Int? = null, max: Int? = null, required: Boolean = false): String? {
        if (text.isBlank()) return if (required) "Valore obbligatorio" else null
        val value = text.trim().toIntOrNull() ?: return "Inserire un numero intero"
        if (min != null && value < min) return "Minimo $min"
        if (max != null && value > max) return "Massimo $max"
        return null
    }

    fun decimal(text: String, min: Double? = null, max: Double? = null): String? {
        if (text.isBlank()) return null
        val value = parseDecimal(text) ?: return "Inserire un numero"
        if (min != null && value < min) return "Minimo ${formatNumber(min)}"
        if (max != null && value > max) return "Massimo ${formatNumber(max)}"
        return null
    }

    fun ipv4(text: String): String? {
        if (text.isBlank()) return null
        return if (isIpv4(text.trim())) null else "Indirizzo IPv4 non valido (es. 192.168.1.10)"
    }

    fun cidr(text: String, required: Boolean = false): String? {
        if (text.isBlank()) return if (required) "Blocco CIDR obbligatorio" else null
        val parts = text.trim().split("/")
        val ok = parts.size == 2 && isIpv4(parts[0]) && parts[1].toIntOrNull()?.let { it in 0..32 } == true
        return if (ok) null else "CIDR non valido (es. 10.0.0.0/24)"
    }

    fun mac(text: String): String? {
        if (text.isBlank()) return null
        val ok = Regex("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$").matches(text.trim()) ||
            Regex("^([0-9A-Fa-f]{4}\\.){2}[0-9A-Fa-f]{4}$").matches(text.trim())
        return if (ok) null else "MAC non valido (es. AA:BB:CC:DD:EE:FF)"
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
