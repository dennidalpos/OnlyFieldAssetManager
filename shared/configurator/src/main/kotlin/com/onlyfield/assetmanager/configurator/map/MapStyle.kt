package com.onlyfield.assetmanager.configurator.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import com.onlyfield.assetmanager.core.model.LinkMedium
import com.onlyfield.assetmanager.core.model.ObjectFamily
import com.onlyfield.assetmanager.core.model.Glyph

/** Fixed hues dark enough for white glyph text (WCAG AA on 12sp bold); shape and code repeat the meaning. */
object MapStyle {
    fun glyph(glyph: Glyph): Color = when (glyph.typeId) {
        "switch" -> Color(0xFF1565C0)
        "router" -> Color(0xFF3949AB)
        "modem" -> Color(0xFF546E7A)
        "ont" -> Color(0xFF006D77)
        "access-point" -> Color(0xFF00796B)
        "radio-bridge" -> Color(0xFF7B1FA2)
        "wifi-controller" -> Color(0xFF283593)
        "firewall" -> Color(0xFFC62828)
        "camera" -> Color(0xFFAD1457)
        "nvr" -> Color(0xFF9E3B17)
        "access-control" -> Color(0xFF880E4F)
        "server" -> Color(0xFF6A1B9A)
        "nas" -> Color(0xFF512DA8)
        "san" -> Color(0xFF4527A0)
        "workstation" -> Color(0xFF2E7D32)
        "ip-phone" -> Color(0xFF4F812B)
        "pbx" -> Color(0xFF33691E)
        "sensor" -> Color(0xFF1B5E20)
        "ups" -> Color(0xFF795548)
        "pdu" -> Color(0xFF8D5A00)
        "power-supply" -> Color(0xFF827717)
        "patch-panel" -> Color(0xFF00695C)
        "outlet" -> Color(0xFF00838F)
        "junction-box" -> Color(0xFF455A64)
        "blank-panel" -> Color(0xFF616161)
        "rack" -> Color(0xFF37474F)
        "shelf" -> Color(0xFF577482)
        "cabinet" -> Color(0xFF4E6470)
        "enclosure" -> Color(0xFF5D4037)
        else -> family(glyph.family)
    }

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
        LinkMedium.RADIO -> Color(0xFF6A1B9A)
        LinkMedium.POWER -> Color(0xFFB71C1C)
        LinkMedium.OTHER -> Color(0xFF616161)
        else -> fallback
    }

    /** Copper solid, fiber dashed, radio long dashes, power dotted, other dash-dot; mixed bundles stay solid. */
    fun dash(media: Set<LinkMedium>, unit: Float): PathEffect? = when (media.singleOrNull()) {
        LinkMedium.FIBER -> PathEffect.dashPathEffect(floatArrayOf(unit * 3, unit * 2))
        LinkMedium.RADIO -> PathEffect.dashPathEffect(floatArrayOf(unit * 6, unit * 3))
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
