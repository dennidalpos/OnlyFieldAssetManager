package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import com.onlyfield.assetmanager.configurator.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.PanelMappingForm

@Composable
fun CablingSection(project: Project, onProjectUpdated: (Project, String) -> Unit, saveError: () -> String? = { null }) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubTabs(
            listOf(i18n.text("text.bc2fc31a1e0f", project.cables.size), i18n.text("text.3a84102d29b6", project.panelMappings.size)),
            tab
        ) { tab = it }
        when (tab) {
            0 -> CablesTab(project, index, onProjectUpdated, saveError)
            1 -> MappingsTab(project, index, onProjectUpdated, saveError)
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
    noneLabel: String? = LocalMessages.current.text("text.f6ea027385f8"),
    error: String? = null,
    busyPortIds: Set<String> = emptySet(),
) {
    val i18n = LocalMessages.current

    OptionPicker(
        label = label,
        options = index.ports,
        selected = index.port(selectedId),
        optionLabel = { "${it.device.technicalName} › ${it.port.name}" },
        optionDetail = { ref ->
            listOfNotNull(ref.port.label, if (ref.port.id in busyPortIds) i18n.text("text.7c4d126e67ea") else null, index.areaName(ref.device.areaId, "").ifBlank { null })
                .joinToString(" · ")
        },
        onSelected = { onSelected(it?.port?.id) },
        noneLabel = noneLabel,
        isError = error != null,
        supportingText = error ?: if (index.ports.isEmpty()) i18n.text("text.b99f95c58f1c") else null,
        modifier = modifier
    )
}

@Composable
private fun CablesTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit, saveError: () -> String?) {
    val i18n = LocalMessages.current

    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Cable?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }
    val cables = project.cables.filter {
        matchesQuery(query, it.codeOrLabel, it.color, index.portLabel(it.portAId), index.portLabel(it.portBId))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.d80a762409b3"), searchQuery = query, onSearchChange = { query = it }, searchPlaceholder = i18n.text("text.7a29a24c6681")) {
            Button(onClick = { changeDetail { editing = null; creating = true } }) { Text(i18n.text("text.f72283c631b5")) }
        }
        if (cables.isEmpty()) {
            EmptyState(if (project.cables.isEmpty()) i18n.text("text.9fbf0fecd44a") else i18n.text("text.bcc05b916ded"),
                actionLabel = i18n.text("text.f72283c631b5").takeIf { project.cables.isEmpty() }, onAction = { changeDetail { editing = null; creating = true } })
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cables.sortedForDisplay(i18n) { it.codeOrLabel.orEmpty() }, key = { it.id }) { cable ->
                ItemCard(
                    title = cable.codeOrLabel ?: i18n.text("text.cfe760f7574e"),
                    badge = cable.medium.toDisplayString(i18n = i18n),
                    details = listOf(
                        "${index.portLabel(cable.portAId, i18n.text("text.05637cfbb159"))}  ⇄  ${index.portLabel(cable.portBId, i18n.text("text.62e52fecec9d"))}",
                        listOfNotNull(
                            cable.lengthValue?.let { "${formatLength(it)} ${cable.lengthUnit ?: "m"}" },
                            cable.color,
                        ).joinToString(" · "),
                        cable.notes.orEmpty()
                    )
                ) {
                    EditButton { changeDetail { creating = false; editing = cable } }
                    DeleteButton(cable.codeOrLabel ?: i18n.text("text.89dbe18e8407"), onDelete = { onProjectUpdated(ProjectEdits.deleteCable(project, cable.id), i18n.text("text.20c7dd63256f")) })
                }
            }
        }
    }

    if (creating || editing != null) {
        var draft by remember(LocalDetailSlot.current?.editorVersion, editing) { mutableStateOf(com.onlyfield.assetmanager.core.forms.MapObjectDraft.forCable(project, editing)) }
        EditPanel(title = configuratorTitle(project, draft, i18n), confirmLabel = configuratorAction(project, draft, i18n), onDismiss = { creating = false; editing = null }, width = 800.dp,
            validationMessage = configuratorValidation(project, draft, i18n), confirmEnabled = draft.errors(project, i18n).isEmpty(), onConfirm = {
                onProjectUpdated(draft.apply(project, i18n), configuratorTitle(project, draft, i18n))
                if (saveError() == null) { creating = false; editing = null }
            }) {
                saveError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                ObjectFields(project, draft) { draft = it }
            }
    }
}

@Composable
private fun MappingsTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit, saveError: () -> String?) {
    val i18n = LocalMessages.current

    var editing by remember { mutableStateOf<PanelMapping?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.66cdda39c617"), subtitle = i18n.text("text.dbb1113ac371")) {
            Button(onClick = { changeDetail { editing = null; creating = true } }) { Text(i18n.text("text.8c5385b5431b")) }
        }
        if (project.panelMappings.isEmpty()) {
            EmptyState(i18n.text("text.65ca63c9b90c"), actionLabel = i18n.text("text.8c5385b5431b"), onAction = { changeDetail { editing = null; creating = true } })
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.panelMappings, key = { it.id }) { m ->
                ItemCard(
                    title = "${index.portLabel(m.portAId, i18n.text("text.f7f4f7cb6de5"))}  ⇄  ${index.portLabel(m.portBId, i18n.text("common.none"))}",
                    details = emptyList(),
                    badge = if (m.isUnknownPassage) i18n.text("text.8c5c99642be7") else null
                ) {
                    EditButton { changeDetail { creating = false; editing = m } }
                    DeleteButton("permutazione", onDelete = { onProjectUpdated(ProjectEdits.deletePanelMapping(project, m.id), i18n.text("text.838b61a43b92")) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val m = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, m) { mutableStateOf(PanelMappingForm.from(m)) }
        val errors = form.errors(i18n = i18n)
        EditPanel(
            title = if (m == null) i18n.text("text.985706a0b378") else i18n.text("text.cedf7d64c712"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toMapping(m)
                onProjectUpdated(if (m == null) ProjectEdits.addPanelMapping(project, saved) else ProjectEdits.updatePanelMapping(project, saved), i18n.text("text.c64f4269ee7c"))
                if (saveError() == null) { creating = false; editing = null }
            },
            width = 640.dp
        ) {
            saveError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PortPicker(i18n.text("text.87af12314823"), index, form.portAId, { form = form.copy(portAId = it) }, noneLabel = null, error = errors["portAId"])
            PortPicker(i18n.text("text.dcc43f317d0c"), index, form.portBId, { form = form.copy(portBId = it) }, noneLabel = i18n.text("text.f56b9cfaeb27"), error = errors["portBId"])
            LabeledCheckbox(form.isUnknownPassage, { form = form.copy(isUnknownPassage = it) }, i18n.text("text.a4e483155a37"))
        }
    }
}

private fun formatLength(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().replace('.', ',')
