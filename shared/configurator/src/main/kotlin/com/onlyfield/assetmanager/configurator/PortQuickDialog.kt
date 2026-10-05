package com.onlyfield.assetmanager.configurator

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.map.ValueMenu
import com.onlyfield.assetmanager.configurator.map.newObjectDraft
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*

/** Host photo action (camera on Android, file picker on desktop); null hides photo buttons. */
val LocalPhotoAction = staticCompositionLocalOf<((AttachmentTargetType, String) -> Unit)?> { null }

/** Host operations for [PortQuickDialog]; null hides the matching button. */
class PortQuickActions(
    val update: (Project, String) -> Unit,
    val photo: ((AttachmentTargetType, String) -> Unit)? = null,
    /** Full port page (VLAN, PoE, hardware). */
    val details: ((Port) -> Unit)? = null,
    val openDevice: ((Device) -> Unit)? = null,
)

private enum class QuickStep { MAIN, DEVICE, PORT, PASSAGE, CONFIGURE }

/**
 * Port card for field work: state, path and cable label first; connect, insert a passage,
 * disconnect and photos in one or two taps. Every change is a single [PortQuickActions.update].
 */
@Composable
fun PortQuickDialog(project: Project, portId: String, i18n: Messages, actions: PortQuickActions, onClose: () -> Unit, insertPassage: Boolean = false) {
    val index = remember(project) { ProjectIndex(project) }
    val graph = remember(project) { ConnectionGraph(project) }
    // Continuous cabling moves the card to the next free port without closing it.
    var currentId by remember(portId) { mutableStateOf(portId) }
    val summary = remember(project, currentId) { PortSummaries.of(project, currentId, graph, index) }
    val path = remember(project, currentId) { PathSchematics.of(project, currentId, graph, index) }
    // The port may disappear (undo, sync): close instead of showing stale data.
    if (summary == null) { LaunchedEffect(currentId) { onClose() }; return }
    val port = summary.port
    val device = summary.device
    val areaId = remember(project, device.id) { ObjectMap.areaId(project, device) }
    var step by remember(portId) { mutableStateOf(if (insertPassage && summary.cable != null) QuickStep.PASSAGE else QuickStep.MAIN) }
    var target by remember(portId) { mutableStateOf<Device?>(null) }
    var targetPort by remember(portId) { mutableStateOf<String?>(null) }
    var sameFloor by remember(portId) { mutableStateOf(areaId != null) }
    var medium by remember(portId) { mutableStateOf(defaultMedium(port)) }
    var label by remember(portId) { mutableStateOf("") }
    var count by remember(portId) { mutableStateOf(1) }
    var continueNext by remember { mutableStateOf(false) }
    // Cable connected from this card: its photo is offered right away.
    var justConnected by remember(portId) { mutableStateOf<String?>(null) }
    var asking by remember { mutableStateOf(false) }
    var fixed by remember(portId) { mutableStateOf(PassiveCabling.supported(device) && port.hardware.side == PortSide.FRONT) }
    var configuration by remember(portId) { mutableStateOf<ConfigurationSession?>(null) }
    var discard by remember { mutableStateOf(false) }
    val dirty = configuration?.let { it.original != it.project } == true
    fun close() { if (dirty) discard = true else onClose() }
    fun pairs(to: String, requested: Int): List<Pair<Port, Port>> = if (fixed) PassiveCabling.pairs(project, port.id, to, requested)
        else BulkCabling.pairs(project, port.id, to, requested, graph)


    fun done(updated: Project, message: String) { actions.update(updated, message); step = QuickStep.MAIN }

    val title = index.portLabel(port.id)
    AlertDialog(onDismissRequest = ::close, title = {
        Column {
            Text(title, modifier = Modifier.semantics { heading() })
            Text(listOfNotNull(port.hardware.side?.takeIf { device.isPassive() }?.toDisplayString(i18n), stateLabel(summary.state, i18n), port.hardware.connector)
                .joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (step) {
                QuickStep.CONFIGURE -> configuration?.let { session ->
                    val configured = session.project.sites.flatMap { it.devices }.first { it.id == device.id }
                    DeviceDrawing(session.project, configured, i18n, onDeviceChange = { updated ->
                        configuration = session.copy(project = ProjectEdits.updateDevice(session.project, updated))
                    })
                }
                QuickStep.MAIN -> {
                    DeviceDrawing(project, device, i18n, selectedPortIds = setOf(port.id), onPort = { currentId = it.port.id })
                    OutlinedButton(onClick = { configuration = ConfigurationSession(project); step = QuickStep.CONFIGURE }) { Text(i18n.text("visual.configure")) }
                    if (PassiveCabling.supported(device) && port.hardware.side == PortSide.FRONT) {
                        val rear = PassiveCabling.rear(project, port.id)
                        if (rear == null) Text(i18n.text("visual.missingRear"), style = MaterialTheme.typography.bodySmall)
                        else if (summary.cable != null) OutlinedButton(enabled = !graph.occupied(rear.id), onClick = { fixed = true; target = null; targetPort = null; step = QuickStep.DEVICE }) { Text(i18n.text("visual.fixedCable")) }
                    }
                    MainStep(project, summary, path, index, i18n, actions, fresh = justConnected != null && summary.cable?.id == justConnected)
                }
                QuickStep.DEVICE -> {
                    if (PassiveCabling.supported(device) && port.hardware.side == PortSide.FRONT)
                        ValueMenu(i18n.text("visual.cablingMode"), fixed, listOf(true, false), { i18n.text(if (it) "visual.fixedCable" else "visual.frontCable") }) { fixed = it }
                    DeviceStep(project, device, areaId, sameFloor, { sameFloor = it }, graph, port, i18n, fixed) { d ->
                    target = d; targetPort = null; step = QuickStep.PORT
                    }
                }
                QuickStep.PORT -> target?.let { d ->
                    // Continuous cabling: photo of the cable just made without leaving the card.
                    justConnected?.let { id -> project.cables.find { it.id == id } }?.let { prev ->
                        val shots = project.attachments.count { it.targetType == AttachmentTargetType.CABLE && it.targetId == prev.id }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(i18n.text("quick.connectedPrev", prev.codeOrLabel ?: CableLabels.suggest(project, prev, index)), Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            actions.photo?.let { photo -> OutlinedButton(onClick = { photo(AttachmentTargetType.CABLE, prev.id) }) { Text(i18n.text("quick.photoCable") + shotCount(shots)) } }
                        }
                    }
                    Text(i18n.text("quick.choosePort", d.technicalName), style = MaterialTheme.typography.bodyMedium)
                    val cells = remember(project, d.id, fixed) { PortLogic.panel(project, d, graph, index).filter { !fixed || it.port.hardware.side == PortSide.FRONT } }
                    PortPanel(cells, i18n, device = d, selected = setOfNotNull(targetPort), onClick = { cell ->
                        if (if (fixed) PassiveCabling.rear(project, cell.port.id)?.let { !graph.occupied(it.id) } == true else !cell.occupied) {
                            targetPort = cell.port.id
                            count = 1
                            label = CableLabels.suggest(project, Cable(portAId = port.id, portBId = cell.port.id), index)
                        }
                    })
                    ValueMenu(i18n.text("config.medium"), medium, CableMedium.entries, { it.toDisplayString(i18n) }, Modifier.fillMaxWidth()) { medium = it }
                    targetPort?.let { to ->
                        val actual = pairs(to, 1).firstOrNull()
                        if (actual != null) Text(i18n.text("visual.actualEnds", index.portLabel(actual.first.id) + (if (fixed) " · " + i18n.text("port.side.REAR") else ""), index.portLabel(actual.second.id) + (if (fixed) " · " + i18n.text("port.side.REAR") else "")), style = MaterialTheme.typography.bodySmall)
                        val max = pairs(to, Int.MAX_VALUE).size
                        if (max > 1) ValueMenu(i18n.text("quick.series"), count.coerceAtMost(max), seriesOptions(max), { i18n.plural("quick.seriesCount", it) }, Modifier.fillMaxWidth()) { count = it }
                        if (count > 1) {
                            val pairs = pairs(to, count)
                            Text(i18n.text("quick.seriesPreview", "${pairs.first().first.name} → ${pairs.first().second.name}", "${pairs.last().first.name} → ${pairs.last().second.name}"),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else OutlinedTextField(label, { label = it }, label = { Text(i18n.text("quick.cableLabel")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth().toggleable(continueNext, role = Role.Switch, onValueChange = { continueNext = it }), verticalAlignment = Alignment.CenterVertically) {
                            Text(i18n.text("quick.continueNext"), Modifier.weight(1f)); Switch(continueNext, null)
                        }
                    }
                }
                QuickStep.PASSAGE -> summary.cable?.let { cable ->
                    PassageStep(project, cable, device, areaId, graph, index, i18n) { updated -> done(updated, i18n.text("quick.insertPassage")) }
                }
            }
        }
    }, confirmButton = {
        when (step) {
            QuickStep.MAIN -> MainActions(summary, i18n, actions, fresh = justConnected != null && summary.cable?.id == justConnected,
                onConnect = { fixed = PassiveCabling.supported(device) && port.hardware.side == PortSide.FRONT; justConnected = null; step = QuickStep.DEVICE }, onPassage = { step = QuickStep.PASSAGE }, onDisconnect = { asking = true })
            QuickStep.CONFIGURE -> Button(enabled = dirty, onClick = {
                configuration?.let { actions.update(it.apply(project), i18n.text("ux.saveChanges")) }
                configuration = null; step = QuickStep.MAIN
            }) { Text(i18n.text("ux.saveChanges")) }
            QuickStep.PORT -> Button(enabled = targetPort?.let { pairs(it, 1).isNotEmpty() } == true, onClick = {
                val to = targetPort ?: return@Button
                val pairs = pairs(to, count)
                if (pairs.isEmpty()) return@Button
                val physicalFrom = pairs.first().first.id
                val physicalTo = pairs.first().second.id
                val updated = if (count > 1) BulkCabling.connect(project, pairs, medium) else {
                    val connected = HardwareConfigurator.connect(project, physicalFrom, physicalTo, medium)
                    val cable = connected.cables.first { setOf(it.portAId, it.portBId) == setOf(physicalFrom, physicalTo) }
                    ProjectEdits.updateCable(connected, cable.copy(codeOrLabel = label.trim().ifBlank { null }))
                }
                val message = if (count > 1) i18n.plural("quick.seriesDone", pairs.size) else i18n.text("quick.connect")
                // A series makes many cables at once: no single photo to offer.
                justConnected = if (count > 1) null else updated.cables.firstOrNull { setOf(it.portAId, it.portBId) == setOf(physicalFrom, physicalTo) }?.id
                val last = pairs.lastOrNull() ?: (port to null)
                val nextFrom = if (!continueNext) null else if (fixed) PassiveCabling.nextFront(updated, port.id) else BulkCabling.nextFree(updated, last.first.id)
                val nextTo = if (nextFrom == null) null else if (fixed) PassiveCabling.nextFront(updated, to) else last.second?.let { BulkCabling.nextFree(updated, it.id) }
                if (nextFrom == null) done(updated, message) else {
                    // Same destination device, next pair already proposed: one tap per cable.
                    actions.update(updated, message)
                    currentId = nextFrom; targetPort = nextTo; count = 1
                    label = nextTo?.let { CableLabels.suggest(updated, Cable(portAId = nextFrom, portBId = it), ProjectIndex(updated)) }.orEmpty()
                }
            }) { Text(if (count > 1) i18n.plural("quick.connectSeries", count) else i18n.text("quick.connect")) }
            else -> TextButton(onClick = ::close) { Text(i18n.text("ux.close")) }
        }
    }, dismissButton = {
        if (step != QuickStep.MAIN) TextButton(onClick = { if (dirty) discard = true else { configuration = null; step = if (step == QuickStep.PORT) QuickStep.DEVICE else QuickStep.MAIN } }) { Text("‹ " + i18n.text("quick.back")) }
        else Column {
            TextButton(onClick = ::close) { Text(i18n.text("ux.close")) }
            actions.details?.let { open -> TextButton(onClick = { onClose(); open(port) }) { Text(i18n.text("quick.details")) } }
        }
    })

    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(i18n.text("visual.discardTitle")) },
        text = { Text(i18n.text("visual.discardMessage")) }, confirmButton = { TextButton(onClick = { discard = false; configuration = null; step = QuickStep.MAIN }) { Text(i18n.text("ux.cancelChanges")) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(i18n.text("ux.cancel")) } })
    if (asking) AlertDialog(onDismissRequest = { asking = false }, title = { Text(i18n.text("quick.disconnectTitle", title)) },
        text = { Text(i18n.text("quick.disconnectMessage")) },
        confirmButton = { TextButton(onClick = { asking = false; done(HardwareConfigurator.disconnect(project, port.id), i18n.text("quick.disconnect")) },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(i18n.text("quick.disconnect")) } },
        dismissButton = { TextButton(onClick = { asking = false }) { Text(i18n.text("text.18c9d912a210")) } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainStep(project: Project, summary: PortSummary, path: PathSchematic?, index: ProjectIndex, i18n: Messages, actions: PortQuickActions, fresh: Boolean) {
    val cable = summary.cable
    if (cable != null) {
        Text(cable.codeOrLabel ?: CableLabels.suggest(project, cable, index), style = MaterialTheme.typography.titleSmall)
        Text(listOfNotNull(cable.medium.toDisplayString(i18n), cable.color, cable.lengthValue?.let { "$it ${cable.lengthUnit ?: "m"}" }).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Whole path, both sides of a pass-through included, from the active end when there is one.
        path?.takeIf { it.segments.isNotEmpty() }?.let {
            SectionTitle(i18n.text("config.traceTitle"))
            PathSchematicView(it, i18n, onOpen = actions.openDevice)
        }
    }
    if (fresh && actions.photo != null) Text(i18n.text("quick.photoPrompt"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainActions(summary: PortSummary, i18n: Messages, actions: PortQuickActions, fresh: Boolean,
                        onConnect: () -> Unit, onPassage: () -> Unit, onDisconnect: () -> Unit) {
    val cable = summary.cable
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Photos are always available: the port itself and, when present, its cable (first right after connecting).
        actions.photo?.let { photo ->
            val cablePhoto: @Composable () -> Unit = { cable?.let { c ->
                val text = i18n.text("quick.photoCable") + shotCount(summary.cablePhotos)
                if (fresh) Button(onClick = { photo(AttachmentTargetType.CABLE, c.id) }) { Text(text) }
                else OutlinedButton(onClick = { photo(AttachmentTargetType.CABLE, c.id) }) { Text(text) }
            } }
            if (fresh) cablePhoto()
            val portText = i18n.text(if (cable == null) "quick.photo" else "quick.photoPort") + shotCount(summary.photos)
            if (fresh) OutlinedButton(onClick = { photo(AttachmentTargetType.PORT, summary.port.id) }) { Text(portText) }
            else Button(onClick = { photo(AttachmentTargetType.PORT, summary.port.id) }) { Text(portText) }
            if (!fresh) cablePhoto()
        }
        if (cable == null) Button(onClick = onConnect) { Text(i18n.text(if (PassiveCabling.supported(summary.device) && summary.port.hardware.side == PortSide.FRONT) "visual.fixedCable" else "quick.connectTo")) }
        else {
            OutlinedButton(onClick = onPassage) { Text(i18n.text("quick.insertPassage")) }
            OutlinedButton(onClick = onDisconnect, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(i18n.text("quick.disconnect")) }
        }
    }
}

/** Devices with at least one free port; same floor first, then a matching connector. */
@Composable
private fun DeviceStep(project: Project, device: Device, areaId: String?, sameFloor: Boolean, onSameFloor: (Boolean) -> Unit,
                       graph: ConnectionGraph, port: Port, i18n: Messages, fixed: Boolean, onPick: (Device) -> Unit) {
    var query by remember { mutableStateOf("") }
    val candidates = remember(project, sameFloor, fixed) {
        project.sites.flatMap { it.devices }.filter { it.id != device.id && it.ports.any { p -> if (fixed) PassiveCabling.rear(project, p.id)?.let { rear -> !graph.occupied(rear.id) } == true else !graph.occupied(p.id) } }
            .map { it to ObjectMap.areaId(project, it) }
            .filter { (_, area) -> !sameFloor || area == areaId }
            .sortedWith(compareBy({ it.second != areaId }, { (d, _) -> d.ports.none { p -> !graph.occupied(p.id) && p.hardware.connector == port.hardware.connector } }, { it.first.technicalName }))
    }
    val index = remember(project) { ProjectIndex(project) }
    OutlinedTextField(query, { query = it }, label = { Text(i18n.text("quick.search")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    if (areaId != null) Row(Modifier.fillMaxWidth().toggleable(sameFloor, role = Role.Switch, onValueChange = onSameFloor), verticalAlignment = Alignment.CenterVertically) {
        Text(i18n.text("quick.sameFloor"), Modifier.weight(1f)); Switch(sameFloor, null)
    }
    val shown = candidates.filter { (d, _) -> query.isBlank() || d.technicalName.contains(query, true) || d.physicalLabel?.contains(query, true) == true }
    if (shown.isEmpty()) Text(i18n.text("quick.noDevices"), style = MaterialTheme.typography.bodySmall)
    LazyColumn(Modifier.heightIn(max = 300.dp)) {
        items(shown, key = { it.first.id }) { (d, area) ->
            val free = d.ports.count { !graph.occupied(it.id) }
            Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(d) }.padding(vertical = 8.dp)) {
                Text(d.technicalName, style = MaterialTheme.typography.bodyLarge)
                Text(listOfNotNull(i18n.plural("quick.freePorts", free), area?.takeIf { it != areaId }?.let { index.areaName(it) }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Free pass-throughs (one entry per pair) or a new junction box next to [device]. */
@Composable
private fun PassageStep(project: Project, cable: Cable, device: Device, areaId: String?, graph: ConnectionGraph, index: ProjectIndex, i18n: Messages, onInsert: (Project) -> Unit) {
    Text(i18n.text("quick.passageHint"), style = MaterialTheme.typography.bodySmall)
    val entries = remember(project) {
        HardwareConfigurator.freePassages(project, graph).keys.mapNotNull(index::port)
            .filter { it.port.hardware.side != PortSide.FRONT }
            .sortedWith(compareBy({ ObjectMap.areaId(project, it.device) != areaId }, { index.portLabel(it.port.id) }))
    }
    if (areaId != null) OutlinedButton(onClick = { onInsert(withNewJunction(project, cable, device, areaId, i18n)) }) { Text(i18n.text("quick.newJunction")) }
    if (entries.isEmpty()) Text(i18n.text("quick.noPassages"), style = MaterialTheme.typography.bodySmall)
    LazyColumn(Modifier.heightIn(max = 280.dp)) {
        items(entries, key = { it.port.id }) { ref ->
            Text(listOfNotNull(index.portLabel(ref.port.id), ref.port.hardware.side?.toDisplayString(i18n)).joinToString(" · "), Modifier.fillMaxWidth().clickable(role = Role.Button) {
                onInsert(HardwareConfigurator.insertPassage(project, cable.id, ref.port.id))
            }.padding(vertical = 10.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Adds a one-way junction box on the floor of [device] and routes [cable] through it. */
private fun withNewJunction(project: Project, cable: Cable, device: Device, areaId: String, i18n: Messages): Project {
    val type = ObjectCatalog.builtins.first { it.id == "junction-box" }
    val kind = if (cable.medium == CableMedium.FIBER_OVERALL) "LC" else "RJ45"
    val preset = DevicePresets.forType(type.id)!!.result(mapOf("ports" to "1", "kind" to kind))
    val near = project.floorplanPlacements.firstOrNull { it.areaId == areaId && (it.targetId == device.id || it.targetId == device.rackId) }
    val draft = newObjectDraft(project, type, preset, areaId, null, near?.let { MapPoint((it.xRatio + .06f).coerceAtMost(.95f), it.yRatio) } ?: MapPoint(.5f, .5f))
    val added = draft.apply(project, i18n)
    val rear = added.sites.flatMap { it.devices }.first { it.id == draft.id }.ports.first { it.hardware.side == PortSide.REAR }
    return HardwareConfigurator.insertPassage(added, cable.id, rear.id)
}

/** " (n)" after a photo button label; empty without photos. */
private fun shotCount(photos: Int): String = if (photos > 0) " ($photos)" else ""

/** Series lengths offered in the menu: common panel sizes up to [max], plus [max] itself. */
internal fun seriesOptions(max: Int): List<Int> = (listOf(1, 2, 4, 8, 12, 16, 24, 48).filter { it < max } + max).distinct()

private fun defaultMedium(port: Port): CableMedium = when (port.hardware.mediaType) {
    "Fiber" -> CableMedium.FIBER_OVERALL
    "Radio" -> CableMedium.RADIO
    "Console" -> CableMedium.CONSOLE
    "Power" -> CableMedium.OTHER
    else -> CableMedium.ETHERNET_COPPER
}

fun stateLabel(state: ConnectionState, i18n: Messages): String = i18n.text(when (state) {
    ConnectionState.AVAILABLE -> "config.available"
    ConnectionState.COMPLETE -> "config.complete"
    ConnectionState.INCOMPLETE -> "config.incomplete"
    ConnectionState.CONFLICT -> "config.conflict"
})
