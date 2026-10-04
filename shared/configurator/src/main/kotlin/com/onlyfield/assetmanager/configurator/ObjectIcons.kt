package com.onlyfield.assetmanager.configurator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.onlyfield.assetmanager.core.model.Glyph

/** Line drawings of the built-in object types; custom types keep their text code. */
enum class ObjectIcon {
    RACK, SWITCH, ROUTER, FIREWALL, PATCH_PANEL, OUTLET, ACCESS_POINT, CAMERA, PHONE, SENSOR, ACCESS_CONTROL,
    SERVER, WORKSTATION, STORAGE, NVR, UPS, PDU, POWER_SUPPLY, MODEM, WIFI_CONTROLLER, PBX, BLANK_PANEL,
    SHELF, CABINET, ENCLOSURE, CABLE;

    companion object {
        fun of(glyph: Glyph): ObjectIcon? = when (val id = glyph.typeId) {
            null -> null
            "rack" -> RACK
            "switch" -> SWITCH
            "router" -> ROUTER
            "firewall" -> FIREWALL
            "patch-panel" -> PATCH_PANEL
            "outlet" -> OUTLET
            "access-point" -> ACCESS_POINT
            "camera" -> CAMERA
            "ip-phone" -> PHONE
            "sensor" -> SENSOR
            "access-control" -> ACCESS_CONTROL
            "server" -> SERVER
            "workstation" -> WORKSTATION
            "nas", "san" -> STORAGE
            "nvr" -> NVR
            "ups" -> UPS
            "pdu" -> PDU
            "power-supply" -> POWER_SUPPLY
            "modem", "ont" -> MODEM
            "wifi-controller" -> WIFI_CONTROLLER
            "pbx" -> PBX
            "blank-panel" -> BLANK_PANEL
            "shelf" -> SHELF
            "cabinet" -> CABINET
            "enclosure" -> ENCLOSURE
            else -> if (id.endsWith("-cable")) CABLE else null
        }
    }
}

/** Draws [icon] in the square at [topLeft] with side [side], on a 24-unit grid. */
fun DrawScope.drawObjectIcon(icon: ObjectIcon, topLeft: Offset, side: Float, color: Color) {
    val k = side / 24f
    val stroke = Stroke(width = 1.6f * k, cap = StrokeCap.Round)
    fun p(x: Float, y: Float) = Offset(topLeft.x + x * k, topLeft.y + y * k)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, p(x1, y1), p(x2, y2), stroke.width, StrokeCap.Round)
    fun box(x: Float, y: Float, w: Float, h: Float) = drawRect(color, p(x, y), Size(w * k, h * k), style = stroke)
    fun fill(x: Float, y: Float, w: Float, h: Float) = drawRect(color, p(x, y), Size(w * k, h * k))
    fun dot(x: Float, y: Float, r: Float = 1f) = drawCircle(color, r * k, p(x, y))
    fun ring(x: Float, y: Float, r: Float) = drawCircle(color, r * k, p(x, y), style = stroke)
    fun arc(x: Float, y: Float, r: Float, start: Float, sweep: Float) =
        drawArc(color, start, sweep, false, p(x - r, y - r), Size(2 * r * k, 2 * r * k), style = stroke)
    fun poly(vararg xy: Float, close: Boolean = false) = drawPath(Path().apply {
        moveTo(p(xy[0], xy[1]).x, p(xy[0], xy[1]).y)
        for (i in 2 until xy.size step 2) p(xy[i], xy[i + 1]).let { lineTo(it.x, it.y) }
        if (close) close()
    }, color, style = stroke)

    when (icon) {
        ObjectIcon.RACK -> { box(6f, 2f, 12f, 20f); listOf(6f, 10f, 14f, 18f).forEach { line(8f, it, 16f, it) } }
        ObjectIcon.SWITCH -> { box(2f, 8f, 20f, 8f); repeat(6) { fill(4f + it * 2.4f, 11f, 1.6f, 2f) }; box(18.2f, 11f, 1.8f, 2f) }
        ObjectIcon.ROUTER -> { box(2f, 12f, 20f, 7f); line(6f, 12f, 5f, 5f); line(18f, 12f, 19f, 5f); dot(6f, 15.5f); dot(9f, 15.5f); dot(12f, 15.5f) }
        ObjectIcon.FIREWALL -> {
            box(3f, 5f, 18f, 14f); line(3f, 9.7f, 21f, 9.7f); line(3f, 14.3f, 21f, 14.3f)
            line(9f, 5f, 9f, 9.7f); line(15f, 5f, 15f, 9.7f); line(6f, 9.7f, 6f, 14.3f); line(12f, 9.7f, 12f, 14.3f); line(18f, 9.7f, 18f, 14.3f)
            line(9f, 14.3f, 9f, 19f); line(15f, 14.3f, 15f, 19f)
        }
        ObjectIcon.PATCH_PANEL -> { box(2f, 8f, 20f, 8f); repeat(7) { ring(4.6f + it * 2.5f, 12f, .8f) } }
        ObjectIcon.OUTLET -> { box(5f, 3f, 14f, 18f); box(8.5f, 7f, 7f, 3.5f); box(8.5f, 13.5f, 7f, 3.5f) }
        ObjectIcon.ACCESS_POINT -> { box(4f, 16f, 16f, 4f); arc(12f, 13f, 3f, 225f, 90f); arc(12f, 13f, 6.5f, 225f, 90f); dot(12f, 13f) }
        ObjectIcon.CAMERA -> { box(3f, 6f, 12f, 7f); poly(15f, 8f, 21f, 6f, 21f, 13f, 15f, 11f, close = true); line(8f, 13f, 8f, 19f); line(5f, 19f, 11f, 19f) }
        ObjectIcon.PHONE -> { box(5f, 10f, 14f, 10f); box(4f, 4f, 16f, 4f); for (r in 0..1) repeat(3) { dot(9f + it * 3f, 13.5f + r * 3f, .9f) } }
        ObjectIcon.SENSOR -> { ring(12f, 12f, 3f); arc(12f, 12f, 6.5f, 135f, 90f); arc(12f, 12f, 6.5f, -45f, 90f); arc(12f, 12f, 10f, 140f, 80f); arc(12f, 12f, 10f, -40f, 80f) }
        ObjectIcon.ACCESS_CONTROL -> { box(7f, 3f, 10f, 18f); ring(12f, 9f, 2.5f); line(9.5f, 16f, 14.5f, 16f) }
        ObjectIcon.SERVER -> { box(3f, 4f, 18f, 7f); box(3f, 13f, 18f, 7f); dot(6f, 7.5f); dot(6f, 16.5f); line(10f, 7.5f, 18f, 7.5f); line(10f, 16.5f, 18f, 16.5f) }
        ObjectIcon.WORKSTATION -> { box(3f, 4f, 18f, 12f); line(12f, 16f, 12f, 20f); line(8f, 20f, 16f, 20f) }
        ObjectIcon.STORAGE -> { box(6f, 3f, 12f, 18f); line(6f, 9f, 18f, 9f); line(6f, 15f, 18f, 15f); dot(15f, 6f, .9f); dot(15f, 12f, .9f); dot(15f, 18f, .9f) }
        ObjectIcon.NVR -> { box(2f, 9f, 20f, 7f); dot(6f, 12.5f, 1.6f); line(10f, 12.5f, 19f, 12.5f) }
        ObjectIcon.UPS -> { box(3f, 7f, 15f, 11f); fill(18f, 10f, 2.5f, 5f); poly(11.5f, 8.5f, 8.5f, 13f, 12f, 13f, 9.5f, 17f) }
        ObjectIcon.PDU -> { box(2f, 9f, 20f, 6f); listOf(6f, 12f, 18f).forEach { ring(it, 12f, 1.6f) } }
        ObjectIcon.POWER_SUPPLY -> { box(7f, 9f, 10f, 8f); line(10f, 4f, 10f, 9f); line(14f, 4f, 14f, 9f); line(12f, 17f, 12f, 21f) }
        ObjectIcon.MODEM -> { box(3f, 11f, 18f, 8f); line(17f, 11f, 18f, 5f); dot(7f, 15f, .9f); dot(10f, 15f, .9f); dot(13f, 15f, .9f) }
        ObjectIcon.WIFI_CONTROLLER -> { box(2f, 14f, 20f, 6f); arc(12f, 12f, 3f, 225f, 90f); arc(12f, 12f, 6.5f, 225f, 90f); dot(5f, 17f, .9f) }
        ObjectIcon.PBX -> { box(4f, 4f, 16f, 16f); for (r in 0..2) repeat(3) { dot(8f + it * 4f, 8f + r * 4f, 1f) } }
        ObjectIcon.BLANK_PANEL -> { box(2f, 9f, 20f, 6f); ring(4.5f, 12f, .9f); ring(19.5f, 12f, .9f) }
        ObjectIcon.SHELF -> { line(3f, 14f, 21f, 14f); line(5f, 14f, 5f, 19f); line(19f, 14f, 19f, 19f); box(7f, 8f, 7f, 6f) }
        ObjectIcon.CABINET -> { box(5f, 2f, 14f, 20f); line(12f, 2f, 12f, 22f); line(10f, 10f, 10f, 13f); line(14f, 10f, 14f, 13f) }
        ObjectIcon.ENCLOSURE -> { box(4f, 6f, 16f, 13f); line(4f, 9.5f, 20f, 9.5f); fill(10.5f, 8.5f, 3f, 2.5f) }
        ObjectIcon.CABLE -> {
            drawPath(Path().apply { moveTo(p(5f, 7f).x, p(5f, 7f).y); cubicTo(p(5f, 16f).x, p(5f, 16f).y, p(19f, 8f).x, p(19f, 8f).y, p(19f, 17f).x, p(19f, 17f).y) }, color, style = stroke)
            box(3f, 3f, 4f, 4f); box(17f, 17f, 4f, 4f)
        }
    }
}
