package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.BadgeForm
import com.onlyfield.assetmanager.core.forms.PoeForm
import com.onlyfield.assetmanager.core.forms.PowerFeedForm

@Composable
fun PowerBadgeSection(project: Project, onProjectUpdated: (Project, String) -> Unit) {
    val index = remember(project) { ProjectIndex(project) }
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubTabs(
            listOf("Alimentazioni (${project.powerFeeds.size})", "PoE (${project.poeMappings.size})", "Badge documentali (${project.documentBadges.size})"),
            tab
        ) { tab = it }
        when (tab) {
            0 -> FeedsTab(project, index, onProjectUpdated)
            1 -> PoeTab(project, index, onProjectUpdated)
            2 -> BadgesTab(project, index, onProjectUpdated)
        }
    }
}

@Composable
private fun FeedsTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<PowerFeed?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }
    val feeds = project.powerFeeds.filter { matchesQuery(query, it.feedName, index.deviceName(it.deviceId), index.deviceName(it.sourceDeviceId, "")) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Alimentazioni", searchQuery = query, onSearchChange = { query = it }, searchPlaceholder = "Cerca linea o apparato…") {
            Button(onClick = { changeDetail { creating = true } }, enabled = index.devices.isNotEmpty()) { Text("+ Nuova alimentazione") }
        }
        if (feeds.isEmpty()) EmptyState(if (index.devices.isEmpty()) "Crea prima gli apparati in Inventario." else "Nessuna alimentazione registrata.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(feeds, key = { it.id }) { f ->
                val source = f.sourceDeviceId?.let { index.deviceName(it) } ?: f.sourceOutletDescription
                ItemCard(
                    title = "${index.deviceName(f.deviceId, "Apparato mancante")} · ${f.feedName}",
                    badge = f.feedType.toDisplayString(),
                    details = listOf(
                        source?.let { "Da $it" }.orEmpty(),
                        listOfNotNull(
                            f.voltageVolts?.let { "$it V" },
                            f.loadWatts?.let { "${trim(it)} W" },
                            f.loadVa?.let { "${trim(it)} VA" },
                            f.observedRuntimeMinutes?.let { "autonomia $it min" }
                        ).joinToString(" · "),
                        f.notes.orEmpty()
                    )
                ) {
                    EditButton { editing = f }
                    DeleteButton(f.feedName, onDelete = { onProjectUpdated(ProjectEdits.deletePowerFeed(project, f.id), "Alimentazione eliminata.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val f = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, f) { mutableStateOf(PowerFeedForm.from(f)) }
        val errors = form.errors()
        EditPanel(
            title = if (f == null) "Nuova alimentazione" else "Modifica alimentazione",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toFeed(f)
                creating = false; editing = null
                onProjectUpdated(if (f == null) ProjectEdits.addPowerFeed(project, saved) else ProjectEdits.updatePowerFeed(project, saved), "Alimentazione salvata.")
            },
            width = 640.dp
        ) {
            DevicePicker("Apparato alimentato *", index, form.deviceId, { form = form.copy(deviceId = it) }, error = errors["deviceId"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.feedName, { form = form.copy(feedName = it) }, "Nome linea *", Modifier.weight(1.3f), errors["feedName"], hint = "Es. Alimentatore 1")
                EnumPicker("Tipo", PowerFeedType.entries, form.feedType, { it.toDisplayString() }, { form = form.copy(feedType = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevicePicker("Sorgente (UPS / PDU)", index, form.sourceDeviceId, { form = form.copy(sourceDeviceId = it) }, Modifier.weight(1f),
                    noneLabel = "Non nel progetto", error = errors["sourceDeviceId"])
                FormField(form.sourceOutlet, { form = form.copy(sourceOutlet = it) }, "Presa / uscita", Modifier.weight(1f), hint = "Es. PDU-A presa 5")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.voltage, { form = form.copy(voltage = it) }, "Tensione (V)", Modifier.weight(1f), errors["voltage"])
                FormField(form.loadWatts, { form = form.copy(loadWatts = it) }, "Carico (W)", Modifier.weight(1f), errors["loadWatts"])
                FormField(form.loadVa, { form = form.copy(loadVa = it) }, "Carico (VA)", Modifier.weight(1f), errors["loadVa"])
                FormField(form.runtimeMinutes, { form = form.copy(runtimeMinutes = it) }, "Autonomia (min)", Modifier.weight(1f), errors["runtimeMinutes"])
            }
            FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false)
        }
    }
}

@Composable
private fun PoeTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var editing by remember { mutableStateOf<PoeMapping?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Power over Ethernet", subtitle = "Porte che erogano o ricevono alimentazione PoE") {
            Button(onClick = { changeDetail { creating = true } }, enabled = index.ports.isNotEmpty()) { Text("+ Nuova porta PoE") }
        }
        if (project.poeMappings.isEmpty()) EmptyState(if (index.ports.isEmpty()) "Aggiungi prima le porte agli apparati." else "Nessuna porta PoE registrata.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.poeMappings, key = { it.id }) { poe ->
                ItemCard(
                    title = index.portLabel(poe.portId, "Porta mancante"),
                    badge = poe.role.toDisplayString(),
                    details = listOf(listOfNotNull(poe.standard.toDisplayString(), poe.allocatedPowerWatts?.let { "${trim(it)} W" }).joinToString(" · "), poe.notes.orEmpty())
                ) {
                    EditButton { editing = poe }
                    DeleteButton("PoE ${index.portLabel(poe.portId)}", onDelete = { onProjectUpdated(ProjectEdits.deletePoeMapping(project, poe.id), "Mappatura PoE eliminata.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val poe = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, poe) { mutableStateOf(PoeForm.from(poe)) }
        val errors = form.errors()
        val existingOnPort = project.poeMappings.find { it.portId == form.portId && it.id != poe?.id }
        EditPanel(
            title = if (poe == null) "Nuova porta PoE" else "Modifica porta PoE",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toMapping(poe)
                creating = false; editing = null
                onProjectUpdated(ProjectEdits.addOrUpdatePoeMapping(project, saved), "Mappatura PoE salvata.")
            },
            width = 600.dp
        ) {
            PortPicker("Porta *", index, form.portId, { form = form.copy(portId = it) }, noneLabel = null, error = errors["portId"])
            if (existingOnPort != null) Text("La porta ha già una mappatura PoE: verrà sostituita.", color = MaterialTheme.colorScheme.tertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnumPicker("Ruolo", PoeRole.entries, form.role, { it.toDisplayString() }, { form = form.copy(role = it) }, Modifier.weight(1f))
                EnumPicker("Standard", PoeStandard.entries, form.standard, { it.toDisplayString() }, { form = form.copy(standard = it) }, Modifier.weight(1f))
            }
            FormField(form.watts, { form = form.copy(watts = it) }, "Potenza allocata (W)", error = errors["watts"])
            FormField(form.notes, { form = form.copy(notes = it) }, "Note")
        }
    }
}

@Composable
private fun BadgesTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var editing by remember { mutableStateOf<DocumentBadge?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Badge documentali", subtitle = "Etichette che compaiono nei documenti esportati") {
            Button(onClick = { changeDetail { creating = true } }) { Text("+ Nuovo badge") }
        }
        if (project.documentBadges.isEmpty()) EmptyState("Nessun badge.", actionLabel = "+ Nuovo badge", onAction = { creating = true })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.documentBadges, key = { it.id }) { b ->
                ItemCard(
                    title = b.label,
                    badge = b.category.toDisplayString(),
                    details = listOf(index.targetLabel(b.targetType, b.targetId), if (b.isDerived) "Generato automaticamente" else "", b.notes.orEmpty())
                ) {
                    if (!b.isDerived) EditButton { editing = b }
                    DeleteButton(b.label, onDelete = { onProjectUpdated(ProjectEdits.deleteDocumentBadge(project, b.id), "Badge eliminato.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val b = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, b) { mutableStateOf(BadgeForm.from(b)) }
        val errors = form.errors()
        EditPanel(
            title = if (b == null) "Nuovo badge" else "Modifica badge",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toBadge(b, project.id)
                creating = false; editing = null
                onProjectUpdated(if (b == null) ProjectEdits.addDocumentBadge(project, saved) else ProjectEdits.updateDocumentBadge(project, saved), "Badge salvato.")
            },
            width = 620.dp
        ) {
            TargetPicker(index, form.target, { form = form.copy(target = it) }, errors["target"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.label, { form = form.copy(label = it) }, "Etichetta *", Modifier.weight(1.3f), errors["label"])
                EnumPicker("Categoria", BadgeCategory.entries, form.category, { it.toDisplayString() }, { form = form.copy(category = it) }, Modifier.weight(1f))
            }
            FormField(form.notes, { form = form.copy(notes = it) }, "Note")
        }
    }
}

private fun trim(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().replace('.', ',')
