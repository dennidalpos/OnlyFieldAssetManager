package com.onlyfield.assetmanager.configurator.map

import com.onlyfield.assetmanager.configurator.ObjectIcon
import com.onlyfield.assetmanager.configurator.drawObjectIcon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/** Current selection on the map; links are identified by their cables so they survive scene rebuilds. */
sealed interface MapSelection {
    data class Node(val ref: ObjectRef) : MapSelection
    data class Link(val cableIds: List<String>, val cableId: String? = null) : MapSelection
}

private enum class Press { PENDING, TAP, DRAG, PINCH, LONG }

/** Route handle: an existing point, or a virtual midpoint that inserts a bend after [index]. */
private data class Handle(val index: Int, val insert: Boolean)

/** Where a stub continues: one remote device ("→ SW-05 · Primo"), or how many; null for links with both ends in view. */
fun stubLabel(scene: MapScene, link: SceneLink, i18n: Messages): String? {
    if (link.b != null) return null
    val devices = link.remotes.values.distinctBy { it.device.id }
    return "→ " + when (devices.size) {
        0 -> i18n.text("map.outside")
        1 -> devices.single().label(scene.areaId, scene.buId)
        else -> i18n.plural("map.remoteCount", devices.size)
    }
}

const val MAP_CONTENT_WIDTH = 1200f
const val MAP_CONTENT_HEIGHT = 900f

@Composable
fun MapCanvas(
    scene: MapScene,
    image: ImageBitmap?,
    selection: MapSelection?,
    i18n: Messages,
    onSelect: (MapSelection?) -> Unit,
    onOpen: (SceneNode) -> Unit,
    onMove: (ObjectRef, MapPoint) -> Unit,
    onRoute: (SceneLink, List<MapPoint>) -> Unit,
    onLongPress: (MapPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewKey = scene.container ?: scene.areaId
    var width by remember { mutableFloatStateOf(1f) }
    var height by remember { mutableFloatStateOf(1f) }
    var zoom by remember(viewKey, image) { mutableFloatStateOf(1f) }
    var pan by remember(viewKey, image) { mutableStateOf(Offset.Zero) }
    var moving by remember(viewKey) { mutableStateOf<Pair<ObjectRef, MapPoint>?>(null) }
    var routeDraft by remember(viewKey) { mutableStateOf<List<MapPoint>?>(null) }
    val shown = moving?.let { (ref, p) -> scene.moved(ref, p) } ?: scene
    val selectedLink = (selection as? MapSelection.Link)?.let { s -> s.cableIds.firstNotNullOfOrNull { shown.linkOf(it) } }
    val selectedNode = (selection as? MapSelection.Node)?.ref
    val stubs = remember(scene, i18n) { scene.links.associate { it.routeCableId to stubLabel(scene, it, i18n) } }

    val density = LocalDensity.current
    val unit = with(density) { 2.dp.toPx() }
    val radius = with(density) { 20.dp.toPx() }
    val hitSlop = with(density) { 12.dp.toPx() }
    val measurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val copper = MaterialTheme.colorScheme.primary
    val highlight = MaterialTheme.colorScheme.tertiary

    // Gesture code runs in a long-lived coroutine: read the latest scene and selection through State.
    val current by rememberUpdatedState(shown)
    val currentLink by rememberUpdatedState(selectedLink)
    fun viewport() = MapViewport(width, height, image?.width?.toFloat() ?: MAP_CONTENT_WIDTH, image?.height?.toFloat() ?: MAP_CONTENT_HEIGHT, zoom, pan.x, pan.y)
    fun screen(p: MapPoint): Offset = viewport().screen(p).let { Offset(it.x, it.y) }
    fun points(link: SceneLink): List<MapPoint> = if (link == currentLink) routeDraft ?: current.points(link) else current.points(link)
    fun drawn(link: SceneLink): List<MapPoint> = points(link).let { if (it.size == 2 && (link != currentLink || routeDraft == null)) arc(it[0], it[1]) else it }

    fun handles(link: SceneLink): List<Pair<Handle, MapPoint>> {
        if (!current.editable) return emptyList()
        val pts = points(link)
        val fixed = pts.indices.filter { i -> (i != 0 || link.a == null) && (i != pts.lastIndex || link.b == null) }.map { Handle(it, false) to pts[it] }
        val curve = drawn(link)
        val mids = if (pts.size == 2) listOf(Handle(0, true) to curve[curve.size / 2])
        else pts.zipWithNext().mapIndexed { i, (a, b) -> Handle(i, true) to MapPoint((a.x + b.x) / 2, (a.y + b.y) / 2) }
        return fixed + mids
    }
    fun hitHandle(p: Offset) = currentLink?.let { link -> handles(link).firstOrNull { (screen(it.second) - p).getDistance() <= hitSlop * 1.5f }?.first }
    fun hitNode(p: Offset) = current.nodes.lastOrNull { (screen(it.point) - p).getDistance() <= radius + hitSlop / 2 }
    fun hitLink(p: Offset) = current.links.lastOrNull { link ->
        drawn(link).zipWithNext().any { (a, b) ->
            val sa = screen(a); val sb = screen(b)
            ObjectMap.segmentDistance(MapPoint(p.x, p.y), MapPoint(sa.x, sa.y), MapPoint(sb.x, sb.y)) <= hitSlop
        }
    }
    fun zoomAround(factor: Float, centroid: Offset) {
        val before = viewport()
        val rx = (centroid.x - before.left) / before.pageWidth
        val ry = (centroid.y - before.top) / before.pageHeight
        zoom = (zoom * factor).coerceIn(.5f, 8f)
        val after = viewport()
        pan += Offset(centroid.x - rx * after.pageWidth - after.left, centroid.y - ry * after.pageHeight - after.top)
    }

    val latest by rememberUpdatedState(Triple(onSelect, onOpen, onLongPress))
    val latestCommit by rememberUpdatedState(onMove to onRoute)
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().testTag("floor-map").background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = i18n.text("map.description", shown.nodes.size, shown.links.size) }
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f); height = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(viewKey, image) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.position
                    var reached = start
                    val raw = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var state = Press.PENDING
                        while (state == Press.PENDING) {
                            val pressed = awaitPointerEvent().changes.filter { it.pressed }
                            pressed.firstOrNull()?.let { reached = it.position }
                            state = when {
                                pressed.size > 1 -> Press.PINCH
                                pressed.isEmpty() -> Press.TAP
                                (pressed.first().position - start).getDistance() > viewConfiguration.touchSlop -> Press.DRAG
                                else -> Press.PENDING
                            }
                        }
                        state
                    }
                    // Holding still on a node or handle may still become a drag; elsewhere it is a long press.
                    val press = if (raw == null && hitHandle(start) == null && hitNode(start) == null) Press.LONG else raw ?: Press.DRAG
                    val (select, open, longPress) = latest
                    when (press) {
                        Press.TAP -> {
                            val node = hitNode(start)
                            val link = if (node == null) hitLink(start) else null
                            when {
                                node != null && node.isContainer && node.childCount > 0 -> open(node)
                                node != null -> select(MapSelection.Node(node.ref))
                                link != null -> select(MapSelection.Link(link.cableIds))
                                else -> select(null)
                            }
                        }
                        Press.LONG -> {
                            if (hitLink(start) == null && current.editable) {
                                val v = viewport(); longPress(v.relative(start.x, start.y))
                            }
                            do { val e = awaitPointerEvent(); e.changes.forEach { it.consume() } } while (e.changes.any { it.pressed })
                        }
                        else -> {
                            val handle = if (press != Press.PINCH) hitHandle(start) else null
                            val node = if (handle == null && press != Press.PINCH && current.editable) hitNode(start) else null
                            // Keep the finger where it grabbed the node instead of snapping the centre under it.
                            val grab = node?.let { n -> viewport().relative(start.x, start.y).let { MapPoint(n.point.x - it.x, n.point.y - it.y) } }
                            var dragging = raw == Press.DRAG
                            var editing = node != null || handle != null
                            var last = start
                            var route = currentLink?.let { points(it) }
                            fun step(position: Offset) {
                                if (!dragging && (position - start).getDistance() <= viewConfiguration.touchSlop) return
                                dragging = true
                                val p = viewport().relative(position.x, position.y)
                                when {
                                    editing && node != null && grab != null -> moving = node.ref to MapPoint(p.x + grab.x, p.y + grab.y)
                                    editing && handle != null && route != null -> {
                                        val r = route!!
                                        route = when {
                                            !handle.insert -> r.toMutableList().apply { this[handle.index] = p }
                                            routeDraft != null -> r.toMutableList().apply { this[handle.index + 1] = p }
                                            r.size < 12 -> r.toMutableList().apply { add(handle.index + 1, p) }
                                            else -> return // Bend limit reached: the midpoint does nothing.
                                        }
                                        routeDraft = route
                                    }
                                    else -> pan += position - last
                                }
                                last = position
                            }
                            // The move that crossed the touch slop counts too.
                            if (raw == Press.DRAG) step(reached)
                            var resync = false
                            do {
                                val e = awaitPointerEvent()
                                val pressed = e.changes.filter { it.pressed }
                                if (pressed.size > 1) {
                                    // A second finger turns any edit into zoom and pan.
                                    if (editing) { editing = false; moving = null; routeDraft = null }
                                    zoomAround(e.calculateZoom(), e.calculateCentroid())
                                    pan += e.calculatePan()
                                    resync = true
                                } else if (pressed.size == 1) {
                                    val position = pressed.first().position
                                    if (resync) { last = position; resync = false; dragging = true } else step(position)
                                }
                                e.changes.forEach { if (it.positionChange() != Offset.Zero) it.consume() }
                            } while (e.changes.any { it.pressed })
                            if (editing && dragging) {
                                val (move, saveRoute) = latestCommit
                                moving?.let { (ref, p) -> move(ref, MapPoint(p.x.coerceIn(0f, 1f), p.y.coerceIn(0f, 1f))) }
                                if (handle != null) currentLink?.let { link -> routeDraft?.let { saveRoute(link, it) } }
                            }
                            moving = null; routeDraft = null
                        }
                    }
                }
            }) {
            val v = viewport()
            if (image != null) drawImage(image, dstOffset = IntOffset(v.left.toInt(), v.top.toInt()), dstSize = IntSize(v.pageWidth.toInt().coerceAtLeast(1), v.pageHeight.toInt().coerceAtLeast(1)))
            else drawGrid(::screen, onSurface)
            shown.links.forEach { link -> drawLink(link, drawn(link).map(::screen), link == selectedLink, unit, copper, highlight, surface, onSurface, measurer, stubs[link.routeCableId]) }
            selectedLink?.let { link -> handles(link).forEach { (h, p) -> drawCircle(if (h.insert) surface else highlight, unit * 3, screen(p)); drawCircle(highlight, unit * 3, screen(p), style = Stroke(unit)) } }
            drawNodes(shown, ::screen, selectedNode, radius, unit, zoom, onSurface, surface, highlight, measurer)
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallFloatingActionButton(onClick = { zoomAround(1.25f, Offset(width / 2, height / 2)) }) { Text("+") }
            SmallFloatingActionButton(onClick = { zoomAround(.8f, Offset(width / 2, height / 2)) }) { Text("−") }
            SmallFloatingActionButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("⤢") }
        }
    }
}

/**
 * Quadratic arc sampled as a polyline; the bulge grows with length and always sits on the same
 * side of the ordered pair, so links from one hub nest instead of overlapping through other nodes.
 */
internal fun arc(a: MapPoint, b: MapPoint, segments: Int = 16): List<MapPoint> {
    val (p, q) = if (a.x < b.x || (a.x == b.x && a.y <= b.y)) a to b else b to a
    val dx = q.x - p.x; val dy = q.y - p.y
    val c = MapPoint((p.x + q.x) / 2 + dy * .18f, (p.y + q.y) / 2 - dx * .18f)
    val curve = (0..segments).map { i ->
        val t = i / segments.toFloat(); val u = 1 - t
        MapPoint(u * u * p.x + 2 * u * t * c.x + t * t * q.x, u * u * p.y + 2 * u * t * c.y + t * t * q.y)
    }
    return if (p == a) curve else curve.reversed()
}

private fun DrawScope.drawGrid(screen: (MapPoint) -> Offset, color: Color) {
    for (i in 0..12) {
        drawLine(color.copy(alpha = .08f), screen(MapPoint(i / 12f, 0f)), screen(MapPoint(i / 12f, 1f)))
        drawLine(color.copy(alpha = .08f), screen(MapPoint(0f, i / 12f)), screen(MapPoint(1f, i / 12f)))
    }
}

private fun DrawScope.drawLink(link: SceneLink, points: List<Offset>, selected: Boolean, unit: Float, copper: Color, highlight: Color, surface: Color, onSurface: Color,
                               measurer: androidx.compose.ui.text.TextMeasurer, stub: String?) {
    if (points.size < 2) return
    val color = if (selected) highlight else MapStyle.medium(link.media, copper)
    val stroke = MapStyle.width(link.cableIds.size, unit) + if (selected) unit else 0f
    points.zipWithNext().forEach { (a, b) -> drawLine(color, a, b, stroke, pathEffect = MapStyle.dash(link.media, unit * 2)) }
    // An open end means "continues elsewhere": draw it hollow.
    if (link.b == null) { drawCircle(surface, unit * 3, points.last()); drawCircle(color, unit * 3, points.last(), style = Stroke(unit)) }
    stub?.let {
        // Beside the open end, on the inner side, clamped to the canvas.
        val text = measurer.measure(AnnotatedString(MapStyle.shortName(it, 36)), TextStyle(color = onSurface, fontSize = 11.sp))
        val end = points.last()
        val x = (end.x - text.size.width / 2f).coerceIn(unit * 3, (size.width - text.size.width - unit * 3).coerceAtLeast(unit * 3))
        val y = (if (end.y > size.height / 2) end.y - text.size.height - unit * 5 else end.y + unit * 5).coerceIn(0f, (size.height - text.size.height).coerceAtLeast(0f))
        drawRoundRect(surface.copy(alpha = .9f), Offset(x - unit * 2, y), Size(text.size.width + unit * 4, text.size.height.toFloat()), CornerRadius(unit * 3))
        drawText(text, topLeft = Offset(x, y))
    }
    if (link.cableIds.size > 1) {
        val mid = if (points.size > 3) points[points.size / 2] else points.zipWithNext().maxBy { (a, b) -> (b - a).getDistance() }.let { (a, b) -> (a + b) / 2f }
        val text = measurer.measure(AnnotatedString(link.cableIds.size.toString()), TextStyle(color = surface, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        val size = Size(text.size.width + unit * 4, text.size.height.toFloat())
        drawRoundRect(color, mid - Offset(size.width / 2, size.height / 2), size, CornerRadius(size.height / 2))
        drawText(text, topLeft = mid - Offset(text.size.width / 2f, text.size.height / 2f))
    }
}

private fun DrawScope.drawNodes(scene: MapScene, screen: (MapPoint) -> Offset, selected: ObjectRef?, radius: Float, unit: Float, zoom: Float,
                                onSurface: Color, surface: Color, highlight: Color, measurer: androidx.compose.ui.text.TextMeasurer) {
    val labels = mutableListOf<Rect>()
    // Selected node first so its label always wins the collision check.
    val ordered = scene.nodes.sortedByDescending { it.ref == selected }
    val crowded = zoom < .9f && scene.nodes.size > 30
    ordered.reversed().forEach { node ->
        val c = screen(node.point)
        val color = MapStyle.family(node.glyph.family)
        if (node.ref == selected) drawCircle(highlight, radius + unit * 3, c, style = Stroke(unit * 2))
        if (node.isContainer) {
            val r = radius * .9f
            drawRoundRect(color.copy(alpha = .35f), c - Offset(r - unit * 2, r + unit * 2), Size(r * 2, r * 2), CornerRadius(unit * 1.5f))
            drawRoundRect(color, c - Offset(r, r), Size(r * 2, r * 2), CornerRadius(unit * 1.5f))
        } else drawCircle(color, radius * .85f, c)
        if (node.portsTotal > 0) {
            val sweep = 360f * node.portsUsed / node.portsTotal
            val r = radius + unit
            drawArc(onSurface.copy(alpha = .15f), -90f, 360f, false, c - Offset(r, r), Size(r * 2, r * 2), style = Stroke(unit * 1.2f))
            drawArc(onSurface.copy(alpha = .55f), -90f, sweep, false, c - Offset(r, r), Size(r * 2, r * 2), style = Stroke(unit * 1.2f))
        }
        val icon = ObjectIcon.of(node.glyph)
        if (icon != null) radius.let { drawObjectIcon(icon, c - Offset(it * .55f, it * .55f), it * 1.1f, Color.White) } else {
            val glyph = measurer.measure(AnnotatedString(node.glyph.code), TextStyle(color = Color.White, fontSize = if (node.glyph.code.length > 2) 10.sp else 12.sp, fontWeight = FontWeight.Bold))
            drawText(glyph, topLeft = c - Offset(glyph.size.width / 2f, glyph.size.height / 2f))
        }
        if (node.isContainer && node.childCount > 0) {
            val badge = measurer.measure(AnnotatedString(node.childCount.toString()), TextStyle(color = surface, fontSize = 10.sp, fontWeight = FontWeight.Bold))
            val at = c + Offset(radius * .8f, -radius * .8f)
            drawCircle(onSurface, maxOf(badge.size.width, badge.size.height) / 2f + unit, at)
            drawText(badge, topLeft = at - Offset(badge.size.width / 2f, badge.size.height / 2f))
        }
    }
    ordered.forEach { node ->
        if (crowded && node.ref != selected) return@forEach
        val c = screen(node.point)
        val text = measurer.measure(AnnotatedString(MapStyle.shortName(node.name)), TextStyle(color = onSurface, fontSize = 11.sp))
        val rect = Rect(Offset(c.x - text.size.width / 2f - unit, c.y + radius + unit * 2), Size(text.size.width + unit * 2, text.size.height.toFloat()))
        if (labels.any { it.overlaps(rect) }) return@forEach
        labels += rect
        drawRoundRect(surface.copy(alpha = .9f), rect.topLeft, rect.size, CornerRadius(unit * 2))
        drawText(text, topLeft = rect.topLeft + Offset(unit, 0f))
    }
}
