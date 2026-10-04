package com.onlyfield.assetmanager.configurator.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import com.onlyfield.assetmanager.core.model.LinkMedium
import com.onlyfield.assetmanager.core.model.ObjectFamily

/** Fixed hues dark enough for white glyph text (WCAG AA on 12sp bold); shape and code repeat the meaning. */
object MapStyle {
    fun family(family: ObjectFamily): Color = when (family) {
        ObjectFamily.NETWORK -> Color(0xFF1565C0)
        ObjectFamily.SECURITY -> Color(0xFFC62828)
        ObjectFamily.SERVER -> Color(0xFF6A1B9A)
        ObjectFamily.POWER -> Color(0xFF8D5A00)
        ObjectFamily.PASSIVE -> Color(0xFF00695C)
        ObjectFamily.STRUCTURE -> Color(0xFF455A64)
        ObjectFamily.ENDPOINT -> Color(0xFF2E7D32)
        ObjectFamily.OTHER -> Color(0xFF5D4037)
    }

    fun medium(media: Set<LinkMedium>, fallback: Color): Color = when (media.singleOrNull()) {
        LinkMedium.FIBER -> Color(0xFFE65100)
        LinkMedium.POWER -> Color(0xFFB71C1C)
        LinkMedium.OTHER -> Color(0xFF616161)
        else -> fallback
    }

    /** Copper solid, fiber dashed, power dotted, other dash-dot; mixed bundles stay solid. */
    fun dash(media: Set<LinkMedium>, unit: Float): PathEffect? = when (media.singleOrNull()) {
        LinkMedium.FIBER -> PathEffect.dashPathEffect(floatArrayOf(unit * 3, unit * 2))
        LinkMedium.POWER -> PathEffect.dashPathEffect(floatArrayOf(unit * .8f, unit * 1.6f))
        LinkMedium.OTHER -> PathEffect.dashPathEffect(floatArrayOf(unit * 3, unit * 1.5f, unit, unit * 1.5f))
        else -> null
    }

    fun width(cables: Int, unit: Float): Float = unit * when {
        cables >= 5 -> 3f
        cables >= 2 -> 2f
        else -> 1.3f
    }

    fun shortName(name: String, max: Int = 14): String = if (name.length <= max) name else name.take(max - 1) + "…"
}
