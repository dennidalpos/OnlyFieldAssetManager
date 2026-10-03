package com.onlyfield.assetmanager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import com.onlyfield.assetmanager.core.model.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun FloorCanvas(project: Project, areaId: String, image: ImageBitmap?, onUpdate: (Project, String) -> Unit, onNode: (MapNode) -> Unit, onCable: (String) -> Unit, modifier: Modifier = Modifier) {
    var width by remember { mutableStateOf(1f) }; var height by remember { mutableStateOf(1f) }
    var zoom by remember(areaId, image) { mutableStateOf(1f) }
    var pan by remember(areaId, image) { mutableStateOf(Offset.Zero) }
    var preview by remember(areaId) { mutableStateOf<Map<String, MapPoint>>(emptyMap()) }
    var selected by remember(areaId) { mutableStateOf<String?>(null) }
    var routePreview by remember(areaId) { mutableStateOf<CableRoute?>(null) }
    var pointerStart by remember { mutableStateOf(Offset.Zero) }
    var movingNode by remember { mutableStateOf<MapNode?>(null) }
    var movingRoute by remember { mutableStateOf<CableRoute?>(null) }
    var movingPoint by remember { mutableStateOf(-1) }
    val nodes = ObjectMap.nodes(project, areaId).map { it.copy(point = preview[it.id] ?: it.point) }
    val routes = ObjectMap.routes(project, areaId)
    val latestNodes by rememberUpdatedState(nodes)
    val latestRoutes by rememberUpdatedState(routes)
    val latestNodeClick by rememberUpdatedState(onNode)
    val latestUpdate by rememberUpdatedState(onUpdate)
    val density = LocalDensity.current
    val radius = with(density) { 22.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val foreground = MaterialTheme.colorScheme.onSurface
    val nodeColor = MaterialTheme.colorScheme.primary
    val wireColor = MaterialTheme.colorScheme.tertiary
    fun viewport() = MapViewport(width, height, image?.width?.toFloat() ?: 1200f, image?.height?.toFloat() ?: 900f, zoom, pan.x, pan.y)
    fun screen(p: MapPoint): Offset = viewport().screen(p).let { Offset(it.x, it.y) }
    fun hitNode(p: Offset) = latestNodes.lastOrNull { (screen(it.point) - p).getDistance() <= radius }
    val transform = rememberTransformableState { change, delta, _ -> zoom = (zoom * change).coerceIn(.5f, 6f); pan += delta }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(.5f) }) { Text("−") }
            TextButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(6f) }) { Text("+") }
            TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Adatta alla vista") }
            Text("${nodes.size + routes.size} oggetti", modifier = Modifier.padding(top = 12.dp))
        }
        Canvas(Modifier.testTag("floor-map").fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f); height = it.height.toFloat().coerceAtLeast(1f) }
            .transformable(transform, canPan = { false })
            .pointerInput(areaId, project, image) {
                detectTapGestures(onPress = { pointerStart = it }, onTap = { p ->
                    val node = hitNode(p)
                    if (node != null) latestNodeClick(node)
                    else selected = latestRoutes.lastOrNull { route ->
                        ObjectMap.routePoints(project, route, latestNodes).zipWithNext().any { (a, b) ->
                            val sa = screen(a); val sb = screen(b)
                            ObjectMap.segmentDistance(MapPoint(p.x, p.y), MapPoint(sa.x, sa.y), MapPoint(sb.x, sb.y)) <= radius / 2
                        }
                    }?.cableId
                })
            }
            .pointerInput(areaId, project, image) {
                detectDragGestures(onDragStart = { _ ->
                    val p = pointerStart
                    movingNode = hitNode(p)
                    movingRoute = null; movingPoint = -1
                    if (movingNode == null) {
                        val route = latestRoutes.find { it.cableId == selected }
                        if (route != null) {
                            val points = ObjectMap.routePoints(project, route, latestNodes)
                            val cable = project.cables.find { it.id == route.cableId }
                            val pointIndex = points.indexOfFirst { (screen(it) - p).getDistance() <= radius }
                            val anchored = cable != null && (pointIndex == 0 || pointIndex == points.lastIndex) && ObjectMap.areaId(project, ObjectMap.endpoint(project, cable, pointIndex == 0)) == areaId
                            if (pointIndex >= 0 && !anchored) { movingRoute = route.copy(points = points); movingPoint = pointIndex }
                        }
                    }
                }, onDragCancel = { preview = emptyMap(); routePreview = null; movingNode = null; movingRoute = null }, onDragEnd = {
                    movingNode?.let { n -> preview[n.id]?.let { p -> latestUpdate(ObjectMap.place(project, areaId, n.type, n.id, p), "Posizione aggiornata.") } }
                    routePreview?.let { latestUpdate(ObjectMap.saveRoute(project, it), "Percorso aggiornato.") }
                    preview = emptyMap(); routePreview = null; movingNode = null; movingRoute = null
                }) { change, delta ->
                    change.consume()
                    val node = movingNode
                    val route = routePreview ?: movingRoute
                    if (node != null) {
                        val current = screen(preview[node.id] ?: node.point) + delta
                        preview = preview + (node.id to viewport().relative(current.x, current.y))
                    } else if (route != null && movingPoint >= 0) {
                        val current = screen(route.points[movingPoint]) + delta
                        routePreview = route.copy(points = route.points.mapIndexed { i, point -> if (i == movingPoint) viewport().relative(current.x, current.y) else point })
                    } else pan += delta
                }
            }) {
            val v = viewport()
            if (image != null) drawImage(image, dstOffset = IntOffset(v.left.toInt(), v.top.toInt()), dstSize = IntSize(v.pageWidth.toInt().coerceAtLeast(1), v.pageHeight.toInt().coerceAtLeast(1)))
            else for (i in 0..12) {
                val start = screen(MapPoint(i / 12f, 0f)); val end = screen(MapPoint(i / 12f, 1f))
                drawLine(foreground.copy(alpha = .12f), start, end)
                drawLine(foreground.copy(alpha = .12f), screen(MapPoint(0f, i / 12f)), screen(MapPoint(1f, i / 12f)))
            }
            routes.forEach { stored ->
                val route = routePreview?.takeIf { it.cableId == stored.cableId } ?: stored
                val points = ObjectMap.routePoints(project, route, nodes)
                points.zipWithNext().forEach { (a, b) -> drawLine(wireColor, screen(a), screen(b), if (selected == route.cableId) 6f else 3f) }
                if (selected == route.cableId) points.forEach { drawCircle(wireColor, radius / 3, screen(it)) }
            }
            nodes.forEach { node ->
                val point = screen(node.point)
                drawCircle(nodeColor, radius * .8f, point)
                val symbol = textMeasurer.measure(AnnotatedString(node.symbol), TextStyle(color = Color.White, fontSize = 12.sp))
                drawText(symbol, topLeft = point - Offset(symbol.size.width / 2f, symbol.size.height / 2f))
                val label = textMeasurer.measure(AnnotatedString(node.name), TextStyle(color = foreground, fontSize = 12.sp))
                drawRect(Color.White.copy(alpha = .85f), point + Offset(-label.size.width / 2f - 2, radius), androidx.compose.ui.geometry.Size(label.size.width + 4f, label.size.height.toFloat()))
                drawText(label, color = Color.Black, topLeft = point + Offset(-label.size.width / 2f, radius))
            }
        }
        val cable = project.cables.find { it.id == selected }
        if (cable != null) {
            val a = ObjectMap.endpoint(project, cable, true); val b = ObjectMap.endpoint(project, cable, false)
            fun endpointLabel(d: Device?): String {
                if (d == null) return "sconosciuta"
                val floor = ObjectMap.areaId(project, d)
                val bu = project.businessUnits.find { b -> b.devices.any { it.id == d.id } }
                val area = bu?.let { ObjectMap.areas(it).find { it.id == floor } }
                return d.technicalName + if (floor != areaId) " (${bu?.name ?: "BU sconosciuta"} / ${area?.let { ObjectMap.areaLabel(bu, it) } ?: "senza piano"})" else ""
            }
            Text("${cable.codeOrLabel ?: "Cavo"}: ${endpointLabel(a)} → ${endpointLabel(b)}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onCable(cable.id) }) { Text("Scheda cavo e foto") }
        } else Text("Tocca un oggetto per aprirlo; trascina per spostarlo. Trascina lo sfondo per scorrere.", style = MaterialTheme.typography.bodySmall)
    }
}
