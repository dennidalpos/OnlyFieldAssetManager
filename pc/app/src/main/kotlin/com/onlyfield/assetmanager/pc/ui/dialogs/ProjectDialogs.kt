package com.onlyfield.assetmanager.pc.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteStep
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import com.onlyfield.assetmanager.exchange.LabelSheetPdf
import com.onlyfield.assetmanager.pc.AppDialog
import com.onlyfield.assetmanager.pc.DesktopAppState
import com.onlyfield.assetmanager.pc.DesktopDocumentManager
import com.onlyfield.assetmanager.pc.DesktopStorageHelper
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.pc.ui.toDisplayString

@Composable
fun ProjectDialogs(state: DesktopAppState) {
    when (val d = state.dialog) {
        null -> Unit
        AppDialog.NewProject -> NewProjectDialog(state)
        is AppDialog.ImportPassword -> ImportPasswordDialog(state, d)
        AppDialog.ManagePassword -> ManagePasswordDialog(state)
        is AppDialog.Compare -> CompareDialog(state, d)
        AppDialog.Documents -> DocumentsDialog(state)
        AppDialog.Validation -> ValidationDialog(state)
    }
}

/** "Nuovo sito" wizard; steps and validation come from core.onboarding.NewSiteWizard. */
@Composable
private fun NewProjectDialog(state: DesktopAppState) {
    var w by remember { mutableStateOf(NewSiteWizard()) }
    val errors = w.errors()
    fun set(t: (NewSiteDraft) -> NewSiteDraft) {
        w = w.update(t)
    }

    AlertDialog(
        onDismissRequest = { state.dialog = null },
        modifier = Modifier.width(560.dp),
        title = { Text("Nuovo sito · passo ${w.stepNumber} di ${w.stepCount}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(progress = { w.stepNumber / w.stepCount.toFloat() }, modifier = Modifier.fillMaxWidth())
                Text(w.step.title, style = MaterialTheme.typography.titleMedium)
                Text(w.step.hint, style = MaterialTheme.typography.bodySmall)
                if (w.isFirst && state.project != null) {
                    Text("Il progetto attuale viene chiuso; resta salvato nella cartella dati.", style = MaterialTheme.typography.bodySmall)
                }
                // key(): the fields' "touched" state restarts on each step
                key(w.step) {
                    val d = w.draft
                    when (w.step) {
                        NewSiteStep.PROJECT -> {
                            FormField(d.projectName, { v -> set { it.copy(projectName = v) } }, "Nome progetto *", error = errors["projectName"])
                            FormField(d.customer, { v -> set { it.copy(customer = v) } }, "Cliente", hint = "Facoltativo")
                        }
                        NewSiteStep.BUSINESS_UNIT ->
                            FormField(d.businessUnit, { v -> set { it.copy(businessUnit = v) } }, "Nome sede *", error = errors["businessUnit"])
                        NewSiteStep.AREA ->
                            FormField(d.area, { v -> set { it.copy(area = v) } }, "Nome area *", error = errors["area"], hint = "Es. Sala server, Piano 1")
                        NewSiteStep.DEVICE -> {
                            FormField(d.deviceName, { v -> set { it.copy(deviceName = v) } }, "Nome apparato", error = errors["deviceName"], hint = "Es. SW-CORE-01")
                            FormField(d.deviceIp, { v -> set { it.copy(deviceIp = v) } }, "Indirizzo IP", error = errors["deviceIp"])
                        }
                        NewSiteStep.PASSWORD -> {
                            WizardPassword(d.password, { v -> set { it.copy(password = v) } }, "Password", null)
                            WizardPassword(d.passwordConfirm, { v -> set { it.copy(passwordConfirm = v) } }, "Conferma password", errors["passwordConfirm"])
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (w.step.skippable && !w.isLast) TextButton(onClick = { w = w.skip() }) { Text("Salta") }
                Button(
                    enabled = w.canProceed,
                    onClick = { if (w.isLast) state.createProject(w) else w = w.next() }
                ) { Text(if (w.isLast) "Crea e apri" else "Avanti") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (w.isFirst) state.dialog = null else w = w.back() }) {
                Text(if (w.isFirst) "Annulla" else "Indietro")
            }
        }
    )
}

@Composable
private fun WizardPassword(value: String, onChange: (String) -> Unit, label: String, error: String?) = OutlinedTextField(
    value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
    visualTransformation = PasswordVisualTransformation(), isError = error != null,
    supportingText = error?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth()
)

@Composable
private fun ImportPasswordDialog(state: DesktopAppState, d: AppDialog.ImportPassword) {
    var password by remember(d.file) { mutableStateOf("") }
    FormDialog(
        title = "Pacchetto protetto",
        onDismiss = { state.dialog = null },
        onConfirm = { state.importFile(d.file, password) },
        confirmEnabled = password.isNotEmpty(),
        confirmLabel = "Apri",
        width = 460.dp
    ) {
        Text("«${d.file.name}» è cifrato. Inserisci la password del progetto.")
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = d.error != null,
            supportingText = d.error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ManagePasswordDialog(state: DesktopAppState) {
    val protected = state.hasPassword
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    @Composable
    fun PasswordField(value: String, onChange: (String) -> Unit, label: String) = OutlinedTextField(
        value = value, onValueChange = { onChange(it); error = null }, label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()
    )

    FormDialog(
        title = if (protected) "Password del progetto" else "Proteggi il progetto con password",
        onDismiss = { state.dialog = null },
        onConfirm = { error = state.changePassword(current, newPassword, confirm) },
        confirmEnabled = (!protected || current.isNotEmpty()) && (protected || newPassword.isNotEmpty()),
        confirmLabel = if (protected && newPassword.isEmpty()) "Rimuovi password" else "Salva password",
        width = 480.dp
    ) {
        Text(
            "La password cifra il progetto salvato e i pacchetti esportati (AES-256). Senza password non è possibile recuperarli.",
            style = MaterialTheme.typography.bodySmall
        )
        if (protected) PasswordField(current, { current = it }, "Password attuale *")
        PasswordField(newPassword, { newPassword = it }, if (protected) "Nuova password (vuota per rimuoverla)" else "Nuova password *")
        PasswordField(confirm, { confirm = it }, "Conferma nuova password")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun CompareDialog(state: DesktopAppState, d: AppDialog.Compare) {
    AlertDialog(
        onDismissRequest = { state.dialog = null },
        title = { Text("Sostituire la copia di lavoro?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pacchetto: ${d.pkg.project.name}", fontWeight = FontWeight.SemiBold)
                Text("Esito del confronto: ${d.comparison.status.toDisplayString()}")
                d.comparison.warningMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(
                    "Il progetto aperto verrà sostituito dal contenuto del pacchetto. Non viene eseguita alcuna unione automatica.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { Button(onClick = { state.acceptIncoming(d.pkg, d.password) }) { Text("Sostituisci") } },
        dismissButton = { TextButton(onClick = { state.dialog = null }) { Text("Annulla") } }
    )
}

private enum class DocFormat(val label: String) {
    PDF("Report PDF"), XLSX("Foglio Excel (.xlsx)"), MARKDOWN("Markdown (.md)"), PRINT("Stampa")
}

@Composable
private fun DocumentsDialog(state: DesktopAppState) {
    val project = state.project ?: return
    var format by remember { mutableStateOf(DocFormat.PDF) }
    var author by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var includeConfidential by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(ReportSelection()) }

    fun generate() {
        state.dialog = null
        val filter = ExportFilterConfig(
            includeConfidential = includeConfidential,
            reviewRequiredConfirmed = true,
            authorName = author.ifBlank { "Tecnico" },
            titleOverride = title.ifBlank { null }
        )
        val baseName = DesktopAppState.safeFileName(project.name)
        try {
            when (format) {
                DocFormat.XLSX -> DesktopStorageHelper.pickSaveFile("Esporta foglio Excel", "$baseName.xlsx", "Foglio Excel (*.xlsx)", "xlsx")
                    ?.also { f -> f.outputStream().use { DesktopDocumentManager.exportXlsx(project, filter, it) } }
                DocFormat.MARKDOWN -> DesktopStorageHelper.pickSaveFile("Esporta Markdown", "$baseName.md", "Markdown (*.md)", "md")
                    ?.also { f -> f.outputStream().use { DesktopDocumentManager.exportMarkdown(project, filter, it) } }
                DocFormat.PDF -> DesktopStorageHelper.pickSaveFile("Esporta report PDF", "${baseName}_report.pdf", "Documento PDF (*.pdf)", "pdf")
                    ?.also { f -> f.outputStream().use { DesktopDocumentManager.exportCompositePdf(project, filter, selection, it) } }
                DocFormat.PRINT -> {
                    val printed = DesktopDocumentManager.printDocumentNative(project, filter, selection)
                    state.notify(if (printed) "Documento inviato alla stampante." else "Stampa annullata.")
                    null
                }
            }?.let { state.notify("Documento salvato in ${it.absolutePath}") }
        } catch (e: Exception) {
            state.error = "Generazione del documento non riuscita: ${e.message}"
        }
    }

    FormDialog(
        title = "Documenti e stampa",
        onDismiss = { state.dialog = null },
        onConfirm = ::generate,
        confirmLabel = if (format == DocFormat.PRINT) "Stampa…" else "Genera…"
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Etichette QR per apparati, rack e cavi con codice (A4, 21 per foglio).", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = {
                state.dialog = null
                DesktopStorageHelper.pickSaveFile("Foglio etichette QR", "${DesktopAppState.safeFileName(project.name)}_etichette.pdf", "Documento PDF (*.pdf)", "pdf")
                    ?.let { f ->
                        runCatching { f.outputStream().use { LabelSheetPdf.write(LabelSheetPdf.labelsFor(project), it) } }
                            .onSuccess { state.notify("Etichette salvate in ${f.absolutePath}") }
                            .onFailure { state.error = "Etichette non salvate: ${it.message}" }
                    }
            }) { Text("Foglio etichette…") }
        }
        HorizontalDivider()
        Text("Formato", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DocFormat.entries.forEach { f ->
                FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.label) })
            }
        }
        FormField(author, { author = it }, "Autore", hint = "Compare nell'intestazione del documento")
        FormField(title, { title = it }, "Titolo (facoltativo)")
        LabeledCheckbox(includeConfidential, { includeConfidential = it }, "Includi anche gli elementi classificati come Riservati")
        Text("Le credenziali e le password non vengono mai incluse nei documenti.", style = MaterialTheme.typography.bodySmall)
        if (format == DocFormat.PDF || format == DocFormat.PRINT) {
            Text("Sezioni", fontWeight = FontWeight.SemiBold)
            LabeledCheckbox(selection.includeInventoryTable, { selection = selection.copy(includeInventoryTable = it) }, "Inventario apparati")
            LabeledCheckbox(selection.includeRackCards, { selection = selection.copy(includeRackCards = it) }, "Schede rack")
            LabeledCheckbox(selection.includeCablingAndPorts, { selection = selection.copy(includeCablingAndPorts = it) }, "Cablaggio e porte")
            LabeledCheckbox(selection.includeLogicalNetwork, { selection = selection.copy(includeLogicalNetwork = it) }, "Rete logica")
            LabeledCheckbox(selection.includePowerAndBadges, { selection = selection.copy(includePowerAndBadges = it) }, "Alimentazione e badge")
            LabeledCheckbox(selection.includeNotesAndAttachments, { selection = selection.copy(includeNotesAndAttachments = it) }, "Note e allegati")
        }
    }
}

@Composable
private fun ValidationDialog(state: DesktopAppState) {
    val project = state.project ?: return
    val index = remember(project) { ProjectIndex(project) }
    val grouped = state.issues.groupBy { it.severity }
    AlertDialog(
        onDismissRequest = { state.dialog = null },
        modifier = Modifier.width(720.dp),
        title = { Text("Controllo del progetto") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Text(
                        "Gli errori strutturali indicano dati incoerenti; gli avvisi documentali segnalano informazioni mancanti. Nessuno dei due blocca il salvataggio.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                listOf(ValidationSeverity.STRUCTURAL_ERROR, ValidationSeverity.DOCUMENTARY_WARNING).forEach { severity ->
                    val list = grouped[severity].orEmpty()
                    if (list.isNotEmpty()) {
                        item {
                            Text(
                                "${severity.toDisplayString()} (${list.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (severity == ValidationSeverity.STRUCTURAL_ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(list) { issue ->
                            val target = state.sectionOf(issue.targetEntityId)
                            ItemCard(
                                title = index.entityName(issue.targetEntityId) ?: "Progetto",
                                details = listOf(issue.message)
                            ) {
                                if (target != null) {
                                    TextButton(onClick = { state.section = target; state.dialog = null }) { Text("Vai a ${target.title}") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { state.dialog = null }) { Text("Chiudi") } }
    )
}
