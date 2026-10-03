package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.ui.LocalMessages

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

private enum class DocFormat(private val labelKey: String) { PDF("text.f172f84f7cb2"), XLSX("text.48d53635551c"), MARKDOWN("text.0e52f6b9d025"), PRINT("text.cd69ebaf5932") ; fun localizedLabel(i18n: Messages) = i18n.text(labelKey) }

@Composable
fun DocumentsScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

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
    val labels = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { it?.let { u -> vm.exportLabels(context.contentResolver, u) } }
    val md = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { it?.let { u -> vm.exportMarkdown(context.contentResolver, u, filter()) } }

    AppScaffold(i18n.text("text.f7ac8562de3a"), onBack = { vm.back() }, snackbarHost = snackbar, busy = vm.busy) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(i18n.text("text.73e1804eb968"))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                DocFormat.entries.forEachIndexed { i, f ->
                    SegmentedButton(selected = format == f, onClick = { format = f }, shape = SegmentedButtonDefaults.itemShape(i, DocFormat.entries.size)) { Text(f.localizedLabel(i18n)) }
                }
            }
            FormField(author, { author = it }, i18n.text("text.80e3789ff0b4"), hint = i18n.text("text.46c9badc9878"))
            FormField(title, { title = it }, i18n.text("text.15cdb122536e"))
            LabeledCheckbox(includeConfidential, { includeConfidential = it }, i18n.text("text.96c317b67877"))
            if (needsReview) {
                LabeledCheckbox(reviewConfirmed, { reviewConfirmed = it }, i18n.text("text.1a2a82084c2d"))
                if (!reviewConfirmed) Text(i18n.text("text.9f00da843217"), style = MaterialTheme.typography.bodySmall)
            }
            Text(i18n.text("text.92babd6c1067"), style = MaterialTheme.typography.bodySmall)
            if (format == DocFormat.PDF || format == DocFormat.PRINT) {
                SectionTitle(i18n.text("text.ad0136b0ca69"))
                LabeledCheckbox(selection.includeInventoryTable, { selection = selection.copy(includeInventoryTable = it) }, i18n.text("text.a26fdd05a46b"))
                LabeledCheckbox(selection.includeRackCards, { selection = selection.copy(includeRackCards = it) }, i18n.text("text.37ee6ae1d2bd"))
                LabeledCheckbox(selection.includeCablingAndPorts, { selection = selection.copy(includeCablingAndPorts = it) }, i18n.text("text.541795bb3ec5"))
                LabeledCheckbox(selection.includeLogicalNetwork, { selection = selection.copy(includeLogicalNetwork = it) }, i18n.text("text.7070d68f65b5"))
                LabeledCheckbox(selection.includePowerAndBadges, { selection = selection.copy(includePowerAndBadges = it) }, i18n.text("text.0a2258044f91"))
                LabeledCheckbox(selection.includeNotesAndAttachments, { selection = selection.copy(includeNotesAndAttachments = it) }, i18n.text("text.1d02ed0f43ac"))
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
            ) { Text(if (format == DocFormat.PRINT) i18n.text("text.cd69ebaf5932") else i18n.text("text.edb428dc4ee8")) }
            SectionTitle(i18n.text("text.6972df666045"))
            Text(i18n.text("text.a80f05eed78b"), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { labels.launch("${baseName}_etichette.pdf") }, modifier = Modifier.fillMaxWidth()) { Text(i18n.text("text.5973001e5892")) }
        }
    }
}
