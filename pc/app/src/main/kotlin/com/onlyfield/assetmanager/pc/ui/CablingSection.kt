package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.CableForm
import com.onlyfield.assetmanager.core.forms.PanelMappingForm
import com.onlyfield.assetmanager.core.forms.SharedPathForm

@Composable
fun CablingSection(project: Project, onProjectUpdated: (Project, String) -> Unit) {
    val index = remember(project) { ProjectIndex(project) }
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubTabs(
            listOf("Cavi (${project.cables.size})", "Percorsi (${project.sharedPathSegments.size})", "Permutazioni (${project.panelMappings.size})"),
            tab
        ) { tab = it }
        when (tab) {
            0 -> CablesTab(project, index, onProjectUpdated)
            1 -> PathsTab(project, index, onProjectUpdated)
            2 -> MappingsTab(project, index, onProjectUpdated)
        }
    }
}

/** Picker over every port of the project, labelled "Apparato › Porta". */
@Composable
fun PortPicker(
    label: String,
    index: ProjectIndex,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    noneLabel: String? = "Non collegata",
    error: String? = null,
    busyPortIds: Set<String> = emptySet(),
) {
    OptionPicker(
        label = label,
        options = index.ports,
        selected = index.port(selectedId),
        optionLabel = { "${it.device.technicalName} › ${it.port.name}" },
        optionDetail = { ref ->
            listOfNotNull(ref.port.label, if (ref.port.id in busyPortIds) "già collegata" else null, index.areaName(ref.device.areaId, "").ifBlank { null })
                .joinToString(" · ")
        },
        onSelected = { onSelected(it?.port?.id) },
        noneLabel = noneLabel,
        isError = error != null,
        supportingText = error ?: if (index.ports.isEmpty()) "Nessuna porta: aggiungile dagli apparati in Inventario" else null,
        modifier = modifier
    )
}

@Composable
private fun CablesTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Cable?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }
    val cables = project.cables.filter {
        matchesQuery(query, it.codeOrLabel, it.color, index.portLabel(it.portAId), index.portLabel(it.portBId))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Cavi", searchQuery = query, onSearchChange = { query = it }, searchPlaceholder = "Cerca codice, apparato, porta…") {
            Button(onClick = { changeDetail { creating = true } }) { Text("+ Nuovo cavo") }
        }
        if (cables.isEmpty()) {
            EmptyState(if (project.cables.isEmpty()) "Nessun cavo censito." else "Nessun cavo corrisponde alla ricerca.",
                actionLabel = "+ Nuovo cavo".takeIf { project.cables.isEmpty() }, onAction = { creating = true })
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cables, key = { it.id }) { cable ->
                ItemCard(
                    title = cable.codeOrLabel ?: "Cavo senza codice",
                    badge = cable.medium.toDisplayString(),
                    details = listOf(
                        "${index.portLabel(cable.portAId, "estremità A libera")}  ⇄  ${index.portLabel(cable.portBId, "estremità B libera")}",
                        listOfNotNull(
                            cable.lengthValue?.let { "${formatLength(it)} ${cable.lengthUnit ?: "m"}" },
                            cable.color,
                            cable.observedSpeed?.let { "velocità $it" },
                            cable.sharedPathSegmentIds.takeIf { it.isNotEmpty() }?.let { "${it.size} percorsi" }
                        ).joinToString(" · "),
                        cable.notes.orEmpty()
                    )
                ) {
                    EditButton { editing = cable }
                    DeleteButton(cable.codeOrLabel ?: "cavo", onDelete = { onProjectUpdated(ProjectEdits.deleteCable(project, cable.id), "Cavo eliminato.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val cable = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, cable) { mutableStateOf(CableForm.from(cable)) }
        val errors = form.errors()
        val busy = project.cables.filter { it.id != cable?.id }.flatMap { listOfNotNull(it.portAId, it.portBId) }.toSet()
        val busyError = { id: String? -> if (id != null && id in busy) "Porta già usata da un altro cavo" else null }

        EditPanel(
            title = if (cable == null) "Nuovo cavo" else "Modifica cavo",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty() && busyError(form.portAId) == null && busyError(form.portBId) == null,
            onConfirm = {
                val saved = form.toCable(cable)
                creating = false; editing = null
                onProjectUpdated(if (cable == null) ProjectEdits.addCable(project, saved) else ProjectEdits.updateCable(project, saved), "Cavo salvato.")
            },
            width = 680.dp
        ) {
            FormField(form.codeOrLabel, { form = form.copy(codeOrLabel = it) }, "Codice / etichetta")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PortPicker("Estremità A", index, form.portAId, { form = form.copy(portAId = it) }, Modifier.weight(1f), error = busyError(form.portAId), busyPortIds = busy)
                PortPicker("Estremità B", index, form.portBId, { form = form.copy(portBId = it) }, Modifier.weight(1f), error = errors["portBId"] ?: busyError(form.portBId), busyPortIds = busy)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnumPicker("Mezzo", CableMedium.entries, form.medium, { it.toDisplayString() }, { form = form.copy(medium = it) }, Modifier.weight(1f))
                EnumPicker("Orientamento", CableOrientation.entries, form.orientation, { it.toDisplayString() }, { form = form.copy(orientation = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.connectorA, { form = form.copy(connectorA = it) }, "Connettore A", Modifier.weight(1f), hint = "Es. RJ45, LC")
                FormField(form.connectorB, { form = form.copy(connectorB = it) }, "Connettore B", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.lengthValue, { form = form.copy(lengthValue = it) }, "Lunghezza (m)", Modifier.weight(1f), errors["lengthValue"])
                FormField(form.color, { form = form.copy(color = it) }, "Colore", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.nominalCharacteristics, { form = form.copy(nominalCharacteristics = it) }, "Caratteristiche nominali", Modifier.weight(1f), hint = "Es. Cat6A, OM4")
                FormField(form.observedSpeed, { form = form.copy(observedSpeed = it) }, "Velocità rilevata", Modifier.weight(1f), hint = "Es. 1 Gbps")
            }
            if (project.sharedPathSegments.isNotEmpty()) {
                Text("Percorsi attraversati", fontWeight = FontWeight.SemiBold)
                project.sharedPathSegments.forEach { seg ->
                    LabeledCheckbox(seg.id in form.sharedPathSegmentIds, { checked ->
                        form = form.copy(sharedPathSegmentIds = if (checked) form.sharedPathSegmentIds + seg.id else form.sharedPathSegmentIds - seg.id)
                    }, seg.name)
                }
            }
            FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false, minLines = 2)
        }
    }
}

@Composable
private fun PathsTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var editing by remember { mutableStateOf<SharedPathSegment?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Percorsi condivisi", subtitle = "Canaline, dorsali e passaggi attraversati dai cavi") {
            Button(onClick = { changeDetail { creating = true } }) { Text("+ Nuovo percorso") }
        }
        if (project.sharedPathSegments.isEmpty()) {
            EmptyState("Nessun percorso definito.", actionLabel = "+ Nuovo percorso", onAction = { creating = true })
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.sharedPathSegments, key = { it.id }) { seg ->
                val used = project.cables.count { seg.id in it.sharedPathSegmentIds }
                ItemCard(
                    title = seg.name,
                    details = listOf(
                        "${index.areaName(seg.sourceAreaId, "?")} → ${index.areaName(seg.targetAreaId, "?")}",
                        "$used cavi" + (seg.capacityMaxCables?.let { " su $it" } ?: ""),
                        seg.description.orEmpty()
                    ),
                    badge = seg.capacityMaxCables?.takeIf { used > it }?.let { "Oltre capacità" }
                ) {
                    EditButton { editing = seg }
                    DeleteButton(seg.name, onDelete = { onProjectUpdated(ProjectEdits.deleteSharedPathSegment(project, seg.id), "Percorso eliminato.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val seg = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, seg) { mutableStateOf(SharedPathForm.from(seg)) }
        val errors = form.errors()
        EditPanel(
            title = if (seg == null) "Nuovo percorso" else "Modifica percorso",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toSegment(seg)
                creating = false; editing = null
                onProjectUpdated(if (seg == null) ProjectEdits.addSharedPathSegment(project, saved) else ProjectEdits.updateSharedPathSegment(project, saved), "Percorso salvato.")
            }
        ) {
            FormField(form.name, { form = form.copy(name = it) }, "Nome *", error = errors["name"], hint = "Es. Dorsale piano 1")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionPicker("Da area", index.areas, index.area(form.sourceAreaId), { it.name }, { form = form.copy(sourceAreaId = it?.id) }, Modifier.weight(1f), noneLabel = "Non indicata")
                OptionPicker("A area", index.areas, index.area(form.targetAreaId), { it.name }, { form = form.copy(targetAreaId = it?.id) }, Modifier.weight(1f), noneLabel = "Non indicata")
            }
            FormField(form.capacityMaxCables, { form = form.copy(capacityMaxCables = it) }, "Capacità massima (cavi)", error = errors["capacityMaxCables"])
            FormField(form.description, { form = form.copy(description = it) }, "Descrizione")
            FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false)
        }
    }
}

@Composable
private fun MappingsTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    var editing by remember { mutableStateOf<PanelMapping?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Permutazioni", subtitle = "Collegamenti interni tra porte di patch panel (fronte ↔ retro)") {
            Button(onClick = { changeDetail { creating = true } }) { Text("+ Nuova permutazione") }
        }
        if (project.panelMappings.isEmpty()) {
            EmptyState("Nessuna permutazione definita.", actionLabel = "+ Nuova permutazione", onAction = { creating = true })
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.panelMappings, key = { it.id }) { m ->
                ItemCard(
                    title = "${index.portLabel(m.portAId, "porta mancante")}  ⇄  ${index.portLabel(m.portBId, "nessuna")}",
                    details = listOf(mappingTypeLabel(m.mappingType), m.notes.orEmpty()),
                    badge = if (m.isUnknownPassage) "Passaggio non ispezionabile" else null
                ) {
                    EditButton { editing = m }
                    DeleteButton("permutazione", onDelete = { onProjectUpdated(ProjectEdits.deletePanelMapping(project, m.id), "Permutazione eliminata.") })
                }
            }
        }
    }

    if (creating || editing != null) {
        val m = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, m) { mutableStateOf(PanelMappingForm.from(m)) }
        val errors = form.errors()
        EditPanel(
            title = if (m == null) "Nuova permutazione" else "Modifica permutazione",
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toMapping(m)
                creating = false; editing = null
                onProjectUpdated(if (m == null) ProjectEdits.addPanelMapping(project, saved) else ProjectEdits.updatePanelMapping(project, saved), "Permutazione salvata.")
            },
            width = 640.dp
        ) {
            PortPicker("Porta A *", index, form.portAId, { form = form.copy(portAId = it) }, noneLabel = null, error = errors["portAId"])
            PortPicker("Porta B", index, form.portBId, { form = form.copy(portBId = it) }, noneLabel = "Nessuna", error = errors["portBId"])
            OptionPicker("Tipo", MAPPING_TYPES, form.mappingType, ::mappingTypeLabel, { it?.let { t -> form = form.copy(mappingType = t) } })
            LabeledCheckbox(form.isUnknownPassage, { form = form.copy(isUnknownPassage = it) }, "Passaggio sconosciuto o non ispezionabile")
            FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false)
        }
    }
}

private val MAPPING_TYPES = listOf("CROSS_CONNECT", "PATCH_PANEL", "INTERCONNECT", "OTHER")

private fun mappingTypeLabel(type: String) = when (type) {
    "CROSS_CONNECT" -> "Cross-connect"
    "PATCH_PANEL" -> "Patch panel"
    "INTERCONNECT" -> "Interconnessione"
    "OTHER" -> "Altro"
    else -> type
}

private fun formatLength(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().replace('.', ',')
