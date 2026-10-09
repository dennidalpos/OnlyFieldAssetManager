package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.configurator.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.PanelMappingForm
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
    var mapDialog by remember { mutableStateOf<PanelMapping?>(null) }
    var newMap by remember { mutableStateOf(false) }

    AppScaffold(
        i18n.text("text.3b40d8bd6081"), onBack = { vm.back() }, snackbarHost = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (tab == 0) newCable = true else newMap = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(listOf(i18n.text("text.89dbe18e8407"), i18n.text("text.6adce9b9a19d"))[tab]) }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SubTabs(listOf(i18n.text("text.bc2fc31a1e0f", project.cables.size), i18n.text("text.3a84102d29b6", project.panelMappings.size)), tab) { tab = it }
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> {
                        item { SearchField(query, { query = it }, i18n.text("text.7a29a24c6681")) }
                        val cables = project.cables.filter { matchesQuery(query, it.codeOrLabel, it.color, index.portLabel(it.portAId), index.portLabel(it.portBId)) }
                        if (cables.isEmpty()) item { EmptyState(if (project.cables.isEmpty()) i18n.text("text.abaa596f8755") else i18n.text("text.4657d31fd783")) }
                        items(cables.sortedForDisplay(i18n) { it.codeOrLabel.orEmpty() }, key = { it.id }) { c ->
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
                    else -> {
                        if (project.panelMappings.isEmpty()) item { EmptyState(i18n.text("text.d71ba8b8466b")) }
                        items(project.panelMappings, key = { it.id }) { m ->
                            ItemCard(
                                title = "${index.portLabel(m.portAId)} ⇄ ${index.portLabel(m.portBId, i18n.text("common.none"))}",
                                details = emptyList(),
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
        val save = rememberEditSave(vm, newCable, cableDialog)
        var draft by remember(cableDialog) { mutableStateOf(com.onlyfield.assetmanager.core.forms.MapObjectDraft.forCable(project, cableDialog)) }
        EditScreen(configuratorTitle(project, draft, i18n), { newCable = false; cableDialog = null }, {
            save.save(configuratorTitle(project, draft, i18n), { newCable = false; cableDialog = null }) { draft.apply(it, i18n) }
        }, validationMessage = configuratorValidation(project, draft, i18n), confirmEnabled = draft.errors(project, i18n).isEmpty(), confirmLabel = configuratorAction(project, draft, i18n)) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ObjectFields(project, draft) { draft = it }
        }
    }
    if (newMap || mapDialog != null) {
        val m = mapDialog
        val save = rememberEditSave(vm, newMap, m)
        var form by remember(m) { mutableStateOf(PanelMappingForm.from(m)) }
        val errors = form.errors(i18n = i18n)
        EditScreen(if (m == null) i18n.text("text.985706a0b378") else i18n.text("text.cedf7d64c712"), { newMap = false; mapDialog = null }, {
            val saved = form.toMapping(m)
            save.save(i18n.text("text.c64f4269ee7c"), { newMap = false; mapDialog = null }) { if (m == null) ProjectEdits.addPanelMapping(it, saved) else ProjectEdits.updatePanelMapping(it, saved) }
        }, confirmEnabled = errors.isEmpty()) {
            save.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PortPicker(i18n.text("text.87af12314823"), index, form.portAId, { form = form.copy(portAId = it) }, noneLabel = null, error = errors["portAId"].takeIf { form.portAId != null })
            PortPicker(i18n.text("text.dcc43f317d0c"), index, form.portBId, { form = form.copy(portBId = it) }, noneLabel = i18n.text("text.f56b9cfaeb27"), error = errors["portBId"])
            LabeledCheckbox(form.isUnknownPassage, { form = form.copy(isUnknownPassage = it) }, i18n.text("text.8c5c99642be7"))
        }
    }
}
