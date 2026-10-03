package com.onlyfield.assetmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.CableForm
import com.onlyfield.assetmanager.core.forms.PanelMappingForm
import com.onlyfield.assetmanager.core.forms.SharedPathForm
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.components.*

@Composable
fun CablingScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val index = remember(project) { ProjectIndex(project) }
    val confirm = LocalConfirm.current
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var cableDialog by remember { mutableStateOf<Cable?>(null) }
    var newCable by remember { mutableStateOf(false) }
    var pathDialog by remember { mutableStateOf<SharedPathSegment?>(null) }
    var newPath by remember { mutableStateOf(false) }
    var mapDialog by remember { mutableStateOf<PanelMapping?>(null) }
    var newMap by remember { mutableStateOf(false) }

    AppScaffold(
        "Cablaggio", onBack = { vm.back() }, snackbarHost = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { when (tab) { 0 -> newCable = true; 1 -> newPath = true; else -> newMap = true } },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(listOf("Cavo", "Percorso", "Permutazione")[tab]) }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SubTabs(listOf("Cavi (${project.cables.size})", "Percorsi (${project.sharedPathSegments.size})", "Permutazioni (${project.panelMappings.size})"), tab) { tab = it }
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> {
                        item { SearchField(query, { query = it }, "Cerca codice, apparato, porta…") }
                        val cables = project.cables.filter { matchesQuery(query, it.codeOrLabel, it.color, index.portLabel(it.portAId), index.portLabel(it.portBId)) }
                        if (cables.isEmpty()) item { EmptyState(if (project.cables.isEmpty()) "Nessun cavo." else "Nessun risultato.") }
                        items(cables, key = { it.id }) { c ->
                            ItemCard(
                                title = c.codeOrLabel ?: "Cavo senza codice", badge = c.medium.toDisplayString(),
                                details = listOf(
                                    "A: ${index.portLabel(c.portAId, "libera")}", "B: ${index.portLabel(c.portBId, "libera")}",
                                    listOfNotNull(c.lengthValue?.let { "${formatNumber(it)} ${c.lengthUnit ?: "m"}" }, c.color).joinToString(" · ")
                                ),
                                onClick = { cableDialog = c },
                                menu = listOf(MenuAction("Elimina", destructive = true) {
                                    confirm(ConfirmRequest("Eliminare il cavo?", "Il cavo verrà eliminato dal progetto.") { vm.edit("Cavo eliminato.") { ProjectEdits.deleteCable(it, c.id) } })
                                })
                            )
                        }
                    }
                    1 -> {
                        if (project.sharedPathSegments.isEmpty()) item { EmptyState("Nessun percorso (canaline, dorsali).") }
                        items(project.sharedPathSegments, key = { it.id }) { s ->
                            val used = project.cables.count { s.id in it.sharedPathSegmentIds }
                            ItemCard(
                                title = s.name,
                                details = listOf("${index.areaName(s.sourceAreaId, "?")} → ${index.areaName(s.targetAreaId, "?")}", "$used cavi" + (s.capacityMaxCables?.let { " su $it" } ?: "")),
                                badge = s.capacityMaxCables?.takeIf { used > it }?.let { "Oltre capacità" },
                                onClick = { pathDialog = s },
                                menu = listOf(MenuAction("Elimina", destructive = true) {
                                    confirm(ConfirmRequest("Eliminare «${s.name}»?", "Il percorso verrà rimosso anche dai cavi.") { vm.edit("Percorso eliminato.") { ProjectEdits.deleteSharedPathSegment(it, s.id) } })
                                })
                            )
                        }
                    }
                    else -> {
                        if (project.panelMappings.isEmpty()) item { EmptyState("Nessuna permutazione.") }
                        items(project.panelMappings, key = { it.id }) { m ->
                            ItemCard(
                                title = "${index.portLabel(m.portAId)} ⇄ ${index.portLabel(m.portBId, "nessuna")}",
                                details = listOf(m.notes.orEmpty()),
                                badge = if (m.isUnknownPassage) "Passaggio non ispezionabile" else null,
                                onClick = { mapDialog = m },
                                menu = listOf(MenuAction("Elimina", destructive = true) {
                                    confirm(ConfirmRequest("Eliminare la permutazione?", "La permutazione verrà eliminata.") { vm.edit("Permutazione eliminata.") { ProjectEdits.deletePanelMapping(it, m.id) } })
                                })
                            )
                        }
                    }
                }
            }
        }
    }

    if (newCable || cableDialog != null) {
        val c = cableDialog
        var form by remember(c) { mutableStateOf(CableForm.from(c)) }
        val errors = form.errors()
        val busy = project.cables.filter { it.id != c?.id }.flatMap { listOfNotNull(it.portAId, it.portBId) }.toSet()
        val busyError = { id: String? -> if (id != null && id in busy) "Porta già usata da un altro cavo" else null }
        FormDialog(if (c == null) "Nuovo cavo" else "Modifica cavo", { newCable = false; cableDialog = null }, {
            newCable = false; cableDialog = null
            val saved = form.toCable(c)
            vm.edit("Cavo salvato.") { if (c == null) ProjectEdits.addCable(it, saved) else ProjectEdits.updateCable(it, saved) }
        }, confirmEnabled = errors.isEmpty() && busyError(form.portAId) == null && busyError(form.portBId) == null) {
            FormField(form.codeOrLabel, { form = form.copy(codeOrLabel = it) }, "Codice / etichetta")
            PortPicker("Estremità A", index, form.portAId, { form = form.copy(portAId = it) }, error = busyError(form.portAId), busyPortIds = busy)
            PortPicker("Estremità B", index, form.portBId, { form = form.copy(portBId = it) }, error = errors["portBId"] ?: busyError(form.portBId), busyPortIds = busy)
            EnumPicker("Mezzo", CableMedium.entries, form.medium, { it.toDisplayString() }, { form = form.copy(medium = it) })
            EnumPicker("Orientamento", CableOrientation.entries, form.orientation, { it.toDisplayString() }, { form = form.copy(orientation = it) })
            FormField(form.lengthValue, { form = form.copy(lengthValue = it) }, "Lunghezza (m)", error = errors["lengthValue"], kind = FieldKind.DECIMAL)
            FormField(form.color, { form = form.copy(color = it) }, "Colore")
            FormField(form.connectorA, { form = form.copy(connectorA = it) }, "Connettore A", hint = "Es. RJ45, LC")
            FormField(form.connectorB, { form = form.copy(connectorB = it) }, "Connettore B")
            FormField(form.nominalCharacteristics, { form = form.copy(nominalCharacteristics = it) }, "Caratteristiche", hint = "Es. Cat6A, OM4")
            FormField(form.observedSpeed, { form = form.copy(observedSpeed = it) }, "Velocità rilevata")
            if (project.sharedPathSegments.isNotEmpty()) {
                SectionTitle("Percorsi attraversati")
                project.sharedPathSegments.forEach { seg ->
                    LabeledCheckbox(seg.id in form.sharedPathSegmentIds, { on ->
                        form = form.copy(sharedPathSegmentIds = if (on) form.sharedPathSegmentIds + seg.id else form.sharedPathSegmentIds - seg.id)
                    }, seg.name)
                }
            }
            FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false)
        }
    }

    if (newPath || pathDialog != null) {
        val s = pathDialog
        var form by remember(s) { mutableStateOf(SharedPathForm.from(s)) }
        val errors = form.errors()
        FormDialog(if (s == null) "Nuovo percorso" else "Modifica percorso", { newPath = false; pathDialog = null }, {
            newPath = false; pathDialog = null
            val saved = form.toSegment(s)
            vm.edit("Percorso salvato.") { if (s == null) ProjectEdits.addSharedPathSegment(it, saved) else ProjectEdits.updateSharedPathSegment(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            FormField(form.name, { form = form.copy(name = it) }, "Nome *", error = errors["name"])
            OptionPicker("Da area", index.areas, index.area(form.sourceAreaId), { it.name }, { form = form.copy(sourceAreaId = it?.id) }, noneLabel = "Non indicata")
            OptionPicker("A area", index.areas, index.area(form.targetAreaId), { it.name }, { form = form.copy(targetAreaId = it?.id) }, noneLabel = "Non indicata")
            FormField(form.capacityMaxCables, { form = form.copy(capacityMaxCables = it) }, "Capacità massima (cavi)", error = errors["capacityMaxCables"], kind = FieldKind.NUMBER)
            FormField(form.description, { form = form.copy(description = it) }, "Descrizione")
        }
    }

    if (newMap || mapDialog != null) {
        val m = mapDialog
        var form by remember(m) { mutableStateOf(PanelMappingForm.from(m)) }
        val errors = form.errors()
        FormDialog(if (m == null) "Nuova permutazione" else "Modifica permutazione", { newMap = false; mapDialog = null }, {
            newMap = false; mapDialog = null
            val saved = form.toMapping(m)
            vm.edit("Permutazione salvata.") { if (m == null) ProjectEdits.addPanelMapping(it, saved) else ProjectEdits.updatePanelMapping(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            PortPicker("Porta A *", index, form.portAId, { form = form.copy(portAId = it) }, noneLabel = null, error = errors["portAId"].takeIf { form.portAId != null })
            PortPicker("Porta B", index, form.portBId, { form = form.copy(portBId = it) }, noneLabel = "Nessuna", error = errors["portBId"])
            LabeledCheckbox(form.isUnknownPassage, { form = form.copy(isUnknownPassage = it) }, "Passaggio non ispezionabile")
            FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false)
        }
    }
}
