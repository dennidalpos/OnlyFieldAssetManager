package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.ui.LocalMessages

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
import com.onlyfield.assetmanager.core.forms.PanelMappingForm
import com.onlyfield.assetmanager.core.forms.SharedPathForm
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.components.*

@Composable
fun CablingScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

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
        i18n.text("text.3b40d8bd6081"), onBack = { vm.back() }, snackbarHost = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { when (tab) { 0 -> newCable = true; 1 -> newPath = true; else -> newMap = true } },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(listOf(i18n.text("text.89dbe18e8407"), i18n.text("text.9ea2e0562fb5"), i18n.text("text.6adce9b9a19d"))[tab]) }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SubTabs(listOf(i18n.text("text.bc2fc31a1e0f", project.cables.size), i18n.text("text.e29fa3cb0250", project.sharedPathSegments.size), i18n.text("text.3a84102d29b6", project.panelMappings.size)), tab) { tab = it }
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> {
                        item { SearchField(query, { query = it }, i18n.text("text.7a29a24c6681")) }
                        val cables = project.cables.filter { matchesQuery(query, it.codeOrLabel, it.color, index.portLabel(it.portAId), index.portLabel(it.portBId)) }
                        if (cables.isEmpty()) item { EmptyState(if (project.cables.isEmpty()) i18n.text("text.abaa596f8755") else i18n.text("text.4657d31fd783")) }
                        items(cables, key = { it.id }) { c ->
                            ItemCard(
                                title = c.codeOrLabel ?: i18n.text("text.cfe760f7574e"), badge = c.medium.toDisplayString(i18n = i18n),
                                details = listOf(
                                    i18n.text("text.f5069687e424", index.portLabel(c.portAId, "libera")), i18n.text("text.fba4ae015204", index.portLabel(c.portBId, "libera")),
                                    listOfNotNull(c.lengthValue?.let { "${formatNumber(it)} ${c.lengthUnit ?: "m"}" }, c.color).joinToString(" · ")
                                ),
                                onClick = { cableDialog = c },
                                menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), destructive = true) {
                                    confirm(ConfirmRequest(i18n.text("text.aca453245e79"), i18n.text("text.4d6a1c85c84a")) { vm.edit(i18n.text("text.20c7dd63256f")) { ProjectEdits.deleteCable(it, c.id) } })
                                })
                            )
                        }
                    }
                    1 -> {
                        if (project.sharedPathSegments.isEmpty()) item { EmptyState(i18n.text("text.c71767bfd1a4")) }
                        items(project.sharedPathSegments, key = { it.id }) { s ->
                            val used = project.cables.count { s.id in it.sharedPathSegmentIds }
                            ItemCard(
                                title = s.name,
                                details = listOf("${index.areaName(s.sourceAreaId, "?")} → ${index.areaName(s.targetAreaId, "?")}", i18n.text("text.e512d1fed715", used) + (s.capacityMaxCables?.let { i18n.text("text.c942a2363719", it) } ?: "")),
                                badge = s.capacityMaxCables?.takeIf { used > it }?.let { i18n.text("text.fd4332f58e2b") },
                                onClick = { pathDialog = s },
                                menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), destructive = true) {
                                    confirm(ConfirmRequest(i18n.text("text.e36c23dfb086", s.name), i18n.text("text.92254bf1fcc6")) { vm.edit(i18n.text("text.b77168059c34")) { ProjectEdits.deleteSharedPathSegment(it, s.id) } })
                                })
                            )
                        }
                    }
                    else -> {
                        if (project.panelMappings.isEmpty()) item { EmptyState(i18n.text("text.d71ba8b8466b")) }
                        items(project.panelMappings, key = { it.id }) { m ->
                            ItemCard(
                                title = "${index.portLabel(m.portAId)} ⇄ ${index.portLabel(m.portBId, i18n.text("common.none"))}",
                                details = listOf(m.notes.orEmpty()),
                                badge = if (m.isUnknownPassage) i18n.text("text.8c5c99642be7") else null,
                                onClick = { mapDialog = m },
                                menu = listOf(MenuAction(i18n.text("text.7efe336bd548"), destructive = true) {
                                    confirm(ConfirmRequest(i18n.text("text.b166745667fa"), i18n.text("text.ffad9b30b548")) { vm.edit(i18n.text("text.838b61a43b92")) { ProjectEdits.deletePanelMapping(it, m.id) } })
                                })
                            )
                        }
                    }
                }
            }
        }
    }

    if (newCable || cableDialog != null) {
        var draft by remember(cableDialog) { mutableStateOf(com.onlyfield.assetmanager.core.forms.MapObjectDraft.forCable(project, cableDialog)) }
        EditScreen(i18n.text("config.title"), { newCable = false; cableDialog = null }, {
            vm.edit(i18n.text("config.title")) { draft.apply(it, i18n) }; newCable = false; cableDialog = null
        }, confirmEnabled = draft.errors(project, i18n).isEmpty()) {
            ObjectFields(project, draft) { draft = it }
        }
    }
    if (newPath || pathDialog != null) {
        val s = pathDialog
        var form by remember(s) { mutableStateOf(SharedPathForm.from(s)) }
        val errors = form.errors(i18n = i18n)
        EditScreen(if (s == null) i18n.text("text.9782b4c668c1") else i18n.text("text.1025548c6b78"), { newPath = false; pathDialog = null }, {
            newPath = false; pathDialog = null
            val saved = form.toSegment(s)
            vm.edit(i18n.text("text.070e7cf313c4")) { if (s == null) ProjectEdits.addSharedPathSegment(it, saved) else ProjectEdits.updateSharedPathSegment(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            FormField(form.name, { form = form.copy(name = it) }, i18n.text("text.2e245546ff59"), error = errors["name"])
            OptionPicker(i18n.text("text.2ae250aa6656"), index.areas, index.area(form.sourceAreaId), { it.name }, { form = form.copy(sourceAreaId = it?.id) }, noneLabel = i18n.text("text.c04f316b46c3"))
            OptionPicker(i18n.text("text.468ebb188883"), index.areas, index.area(form.targetAreaId), { it.name }, { form = form.copy(targetAreaId = it?.id) }, noneLabel = i18n.text("text.c04f316b46c3"))
            FormField(form.capacityMaxCables, { form = form.copy(capacityMaxCables = it) }, i18n.text("text.ae939cdb1886"), error = errors["capacityMaxCables"], kind = FieldKind.NUMBER)
            FormField(form.description, { form = form.copy(description = it) }, i18n.text("text.6fb818621896"))
        }
    }

    if (newMap || mapDialog != null) {
        val m = mapDialog
        var form by remember(m) { mutableStateOf(PanelMappingForm.from(m)) }
        val errors = form.errors(i18n = i18n)
        EditScreen(if (m == null) i18n.text("text.985706a0b378") else i18n.text("text.cedf7d64c712"), { newMap = false; mapDialog = null }, {
            newMap = false; mapDialog = null
            val saved = form.toMapping(m)
            vm.edit(i18n.text("text.c64f4269ee7c")) { if (m == null) ProjectEdits.addPanelMapping(it, saved) else ProjectEdits.updatePanelMapping(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            PortPicker(i18n.text("text.87af12314823"), index, form.portAId, { form = form.copy(portAId = it) }, noneLabel = null, error = errors["portAId"].takeIf { form.portAId != null })
            PortPicker(i18n.text("text.dcc43f317d0c"), index, form.portBId, { form = form.copy(portBId = it) }, noneLabel = i18n.text("text.f56b9cfaeb27"), error = errors["portBId"])
            LabeledCheckbox(form.isUnknownPassage, { form = form.copy(isUnknownPassage = it) }, i18n.text("text.8c5c99642be7"))
            FormField(form.notes, { form = form.copy(notes = it) }, i18n.text("text.d8da2c49df39"), singleLine = false)
        }
    }
}
