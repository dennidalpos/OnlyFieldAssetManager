package com.onlyfield.assetmanager.core.scan

/** Content of the app's own QR labels: `ofam://<projectId>/<type>/<id>`. */
data class LabelCode(val projectId: String, val type: Type, val id: String) {
    enum class Type { DEVICE, RACK, CABLE }

    override fun toString() = "$SCHEME$projectId/${type.name.lowercase()}/$id"

    companion object {
        const val SCHEME = "ofam://"

        /** Null when [text] is not an ofam label (e.g. a manufacturer barcode). */
        fun parse(text: String): LabelCode? {
            val body = text.trim().takeIf { it.startsWith(SCHEME, ignoreCase = true) }?.substring(SCHEME.length) ?: return null
            val parts = body.split('/')
            if (parts.size != 3 || parts.any { it.isBlank() }) return null
            val type = Type.entries.firstOrNull { it.name.equals(parts[1], ignoreCase = true) } ?: return null
            return LabelCode(parts[0], type, parts[2])
        }
    }
}
