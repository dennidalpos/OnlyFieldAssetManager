package com.onlyfield.assetmanager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.components.*

private enum class DocFormat(val label: String) { PDF("Report PDF"), XLSX("Excel"), MARKDOWN("Markdown"), PRINT("Stampa") }

@Composable
fun DocumentsScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    var format by remember { mutableStateOf(DocFormat.PDF) }
    var author by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var includeConfidential by remember { mutableStateOf(false) }
    var reviewConfirmed by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(ReportSelection()) }
    val needsReview = project.attachments.any { it.classification == AttachmentClassification.REVIEW_REQUIRED }

    fun filter() = ExportFilterConfig(
        includeConfidential = includeConfidential,
        reviewRequiredConfirmed = !needsReview || reviewConfirmed,
        authorName = author.ifBlank { "Tecnico" },
        titleOverride = title.ifBlank { null }
    )
    val baseName = safeFileName(project.name)
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { it?.let { u -> vm.exportPdf(context.contentResolver, u, filter(), selection) } }
    val xlsx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
        it?.let { u -> vm.exportXlsx(context.contentResolver, u, filter()) }
    }
    val md = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { it?.let { u -> vm.exportMarkdown(context.contentResolver, u, filter()) } }

    AppScaffold("Documenti", onBack = { vm.back() }, snackbarHost = snackbar, busy = vm.busy) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle("Formato")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                DocFormat.entries.forEachIndexed { i, f ->
                    SegmentedButton(selected = format == f, onClick = { format = f }, shape = SegmentedButtonDefaults.itemShape(i, DocFormat.entries.size)) { Text(f.label) }
                }
            }
            FormField(author, { author = it }, "Autore", hint = "Compare nell'intestazione")
            FormField(title, { title = it }, "Titolo (facoltativo)")
            LabeledCheckbox(includeConfidential, { includeConfidential = it }, "Includi elementi riservati")
            if (needsReview) {
                LabeledCheckbox(reviewConfirmed, { reviewConfirmed = it }, "Ho riesaminato gli allegati «Da riesaminare»")
                if (!reviewConfirmed) Text("Senza conferma, gli allegati da riesaminare non verranno inclusi.", style = MaterialTheme.typography.bodySmall)
            }
            Text("Credenziali e password non vengono mai incluse.", style = MaterialTheme.typography.bodySmall)
            if (format == DocFormat.PDF || format == DocFormat.PRINT) {
                SectionTitle("Sezioni")
                LabeledCheckbox(selection.includeInventoryTable, { selection = selection.copy(includeInventoryTable = it) }, "Inventario")
                LabeledCheckbox(selection.includeRackCards, { selection = selection.copy(includeRackCards = it) }, "Schede rack")
                LabeledCheckbox(selection.includeCablingAndPorts, { selection = selection.copy(includeCablingAndPorts = it) }, "Cablaggio e porte")
                LabeledCheckbox(selection.includeLogicalNetwork, { selection = selection.copy(includeLogicalNetwork = it) }, "Rete logica")
                LabeledCheckbox(selection.includePowerAndBadges, { selection = selection.copy(includePowerAndBadges = it) }, "Alimentazione e badge")
                LabeledCheckbox(selection.includeNotesAndAttachments, { selection = selection.copy(includeNotesAndAttachments = it) }, "Note e allegati")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    when (format) {
                        DocFormat.PDF -> pdf.launch("${baseName}_report.pdf")
                        DocFormat.XLSX -> xlsx.launch("$baseName.xlsx")
                        DocFormat.MARKDOWN -> md.launch("$baseName.md")
                        DocFormat.PRINT -> vm.print(context, filter(), selection)
                    }
                }
            ) { Text(if (format == DocFormat.PRINT) "Stampa" else "Genera e salva…") }
        }
    }
}
