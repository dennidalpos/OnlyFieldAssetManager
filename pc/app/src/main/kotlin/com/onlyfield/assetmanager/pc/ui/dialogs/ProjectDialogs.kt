package com.onlyfield.assetmanager.pc.ui.dialogs

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.onlyfield.assetmanager.exchange.MergeSide
import com.onlyfield.assetmanager.pc.AppDialog
import com.onlyfield.assetmanager.pc.DesktopAppState
import com.onlyfield.assetmanager.pc.DesktopDocumentManager
import com.onlyfield.assetmanager.pc.DesktopStorageHelper
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.pc.ui.toDisplayString

@Composable
fun ProjectDialogs(state: DesktopAppState) {
    if (state.busy) return
    when (val d = state.dialog) {
        null -> Unit
        AppDialog.NewProject -> NewProjectDialog(state)
        is AppDialog.ImportPassword -> ImportPasswordDialog(state, d)
        is AppDialog.LocalReplacementPassword -> LocalReplacementPasswordDialog(state, d)
        AppDialog.ManagePassword -> ManagePasswordDialog(state)
        is AppDialog.Compare -> CompareDialog(state, d)
        is AppDialog.Merge -> MergeDialog(state, d)
        AppDialog.Documents -> DocumentsDialog(state)
        AppDialog.Validation -> ValidationDialog(state)
    }
}

/** "Nuovo sito" wizard; steps and validation come from core.onboarding.NewSiteWizard. */
@Composable
private fun NewProjectDialog(state: DesktopAppState) {
    val i18n = LocalMessages.current

    var w by remember { mutableStateOf(NewSiteWizard()) }
    val errors = w.errors(i18n = i18n)
    fun set(t: (NewSiteDraft) -> NewSiteDraft) {
        w = w.update(t)
    }

    AlertDialog(
        onDismissRequest = { state.dialog = null },
        modifier = Modifier.width(560.dp),
        title = { Text(i18n.text("text.e5fb2fba05af", w.stepNumber, w.stepCount)) },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(progress = { w.stepNumber / w.stepCount.toFloat() }, modifier = Modifier.fillMaxWidth())
                Text(w.step.localizedTitle(i18n), style = MaterialTheme.typography.titleMedium)
                Text(w.step.localizedHint(i18n), style = MaterialTheme.typography.bodySmall)
                if (w.isFirst && state.project != null) {
                    Text(i18n.text("text.f86e27a6906c"), style = MaterialTheme.typography.bodySmall)
                }
                // key(): the fields' "touched" state restarts on each step
                key(w.step) {
                    val d = w.draft
                    when (w.step) {
                        NewSiteStep.PROJECT -> {
                            FormField(d.projectName, { v -> set { it.copy(projectName = v) } }, i18n.text("text.85afe7453202"), error = errors["projectName"])
                            FormField(d.customer, { v -> set { it.copy(customer = v) } }, i18n.text("text.f851d9a83ab0"), hint = i18n.text("text.98c72991302e"))
                        }
                        NewSiteStep.SITE, NewSiteStep.AREA -> WizardLists(w) { w = it }
                        NewSiteStep.PASSWORD -> {
                            WizardPassword(d.password, { v -> set { it.copy(password = v) } }, i18n.text("text.e7cf3ef4f17c"), null)
                            WizardPassword(d.passwordConfirm, { v -> set { it.copy(passwordConfirm = v) } }, i18n.text("text.44d09ab8e50d"), errors["passwordConfirm"])
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (w.step.skippable && !w.isLast) TextButton(onClick = { w = w.skip() }) { Text(i18n.text("text.fb397a42956c")) }
                Button(
                    enabled = w.canProceed,
                    onClick = { if (w.isLast) state.createProject(w) else w = w.next() }
                ) { Text(if (w.isLast) i18n.text("text.6c4a7984bdc6") else i18n.text("text.29ddfd8a8643")) }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (w.isFirst) state.dialog = null else w = w.back() }) {
                Text(if (w.isFirst) i18n.text("text.18c9d912a210") else i18n.text("text.80426885bb74"))
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
private fun LocalReplacementPasswordDialog(state: DesktopAppState, d: AppDialog.LocalReplacementPassword) {
    val i18n = LocalMessages.current
    var password by remember(d.pkg) { mutableStateOf("") }
    FormDialog(
        title = i18n.text("import.localPasswordTitle"),
        onDismiss = { state.dialog = null },
        onConfirm = { state.acceptIncomingWithLocalPassword(d.pkg, d.incomingPassword, password) },
        confirmEnabled = password.isNotEmpty(),
        confirmLabel = i18n.text("text.12abcf9ee7d6"),
        width = 460.dp,
    ) {
        Text(i18n.text("import.localPasswordHint", d.pkg.project.name))
        WizardPassword(password, { password = it }, i18n.text("text.e7cf3ef4f17c"),
            if (d.wrongPassword) i18n.text("text.972b256c2416") else null)
    }
}

@Composable
private fun ImportPasswordDialog(state: DesktopAppState, d: AppDialog.ImportPassword) {
    val i18n = LocalMessages.current

    var password by remember(d.file) { mutableStateOf("") }
    FormDialog(
        title = i18n.text("text.9c4199f65693"),
        onDismiss = { state.dialog = null },
        onConfirm = { state.importFile(d.file, password) },
        confirmEnabled = password.isNotEmpty(),
        confirmLabel = i18n.text("text.12abcf9ee7d6"),
        width = 460.dp
    ) {
        Text(i18n.text("text.cf10d0d223fe", d.file.name))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(i18n.text("text.e7cf3ef4f17c")) },
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
    val i18n = LocalMessages.current

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
        title = if (protected) i18n.text("text.f6a32b19c4b1") else i18n.text("text.e31ad11302ab"),
        onDismiss = { state.dialog = null },
        onConfirm = { error = state.changePassword(current, newPassword, confirm) },
        confirmEnabled = (!protected || current.isNotEmpty()) && (protected || newPassword.isNotEmpty()),
        confirmLabel = if (protected && newPassword.isEmpty()) i18n.text("text.01f6d7886781") else i18n.text("text.08ea8f8fecc8"),
        width = 480.dp
    ) {
        Text(
            i18n.text("text.4dedfc9cbd6f"),
            style = MaterialTheme.typography.bodySmall
        )
        if (protected) PasswordField(current, { current = it }, i18n.text("text.9300c9cacb41"))
        PasswordField(newPassword, { newPassword = it }, if (protected) i18n.text("text.7dc14b395c9e") else i18n.text("text.b791727eacc1"))
        PasswordField(confirm, { confirm = it }, i18n.text("text.8499e9c5410a"))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun CompareDialog(state: DesktopAppState, d: AppDialog.Compare) {
    val i18n = LocalMessages.current

    AlertDialog(
        onDismissRequest = { state.dialog = null },
        title = { Text(i18n.text("text.d9894806b1de")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(i18n.text("text.620d36696411", d.pkg.project.name), fontWeight = FontWeight.SemiBold)
                Text(i18n.text("text.9d284912dcdd", if (d.comparison.currentProjectId == null) d.comparison.summary else d.comparison.status.toDisplayString(i18n = i18n)))
                d.comparison.warningMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                d.warnings.forEach { Text(it.message, color = MaterialTheme.colorScheme.error) }
                if (d.comparison.currentProjectId != null) Text(
                    i18n.text("text.bdd5139031cd") +
                        i18n.text("text.5f9922718875"),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sameProject = d.comparison.currentProjectId == d.pkg.project.id
                if (sameProject) OutlinedButton(onClick = { state.startMerge(d.pkg) }) { Text(i18n.text("text.6f5114885bfc")) }
                Button(onClick = { state.acceptIncoming(d.pkg, d.password) }) { Text(i18n.text(if (d.comparison.currentProjectId == null) "text.4f01aabad5cf" else "text.3260dc474cbc")) }
            }
        },
        dismissButton = { TextButton(onClick = { state.dialog = null }) { Text(i18n.text("text.18c9d912a210")) } }
    )
}

@Composable
private fun MergeDialog(state: DesktopAppState, d: AppDialog.Merge) {
    val i18n = LocalMessages.current

    val conflict = d.current ?: return
    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.width(600.dp),
        title = { Text(i18n.text("text.e81e208100f0", d.choices.size + 1, d.result.conflicts.size)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${conflict.kindLabel}: ${conflict.name}", fontWeight = FontWeight.SemiBold)
                Text(conflict.localizedDescription(i18n))
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(conflict.differences(i18n = i18n)) { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (d.result.autoApplied > 0) Text(i18n.text("text.cdaed04f8505", d.result.autoApplied), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { state.chooseMergeSide(MergeSide.LOCAL) }) { Text(i18n.text("text.1527bc9e78ef")) }
                Button(onClick = { state.chooseMergeSide(MergeSide.INCOMING) }) { Text(i18n.text("text.b4c64eaa84c7")) }
            }
        },
        dismissButton = { TextButton(onClick = { state.dialog = null }) { Text(i18n.text("text.5b99d7ce433a")) } }
    )
}

private enum class DocFormat(private val labelKey: String) {
    PDF("text.f172f84f7cb2"), XLSX("text.46984818ac9d"), MARKDOWN("text.4c4cd897af3d"), PRINT("text.cd69ebaf5932")
; fun localizedLabel(i18n: Messages) = i18n.text(labelKey) }

@Composable
private fun DocumentsDialog(state: DesktopAppState) {
    val i18n = LocalMessages.current

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
            authorName = author.ifBlank { i18n.text("text.2bf644bed677") },
            titleOverride = title.ifBlank { null }
        )
        val baseName = DesktopAppState.safeFileName(project.name)
        try {
            when (format) {
                DocFormat.XLSX -> DesktopStorageHelper.pickSaveFile(i18n.text("text.168dd30fb1a2"), "$baseName.xlsx", i18n.text("text.27d3a667b1cb"), "xlsx", i18n = i18n)
                    ?.also { f -> state.runIo { f.outputStream().use { DesktopDocumentManager.exportXlsx(project, filter, it, i18n = i18n) } } }
                DocFormat.MARKDOWN -> DesktopStorageHelper.pickSaveFile(i18n.text("text.d1e5e5fedfc4"), "$baseName.md", i18n.text("text.091a5fb0185f"), "md", i18n = i18n)
                    ?.also { f -> state.runIo { f.outputStream().use { DesktopDocumentManager.exportMarkdown(project, filter, it, i18n = i18n) } } }
                DocFormat.PDF -> DesktopStorageHelper.pickSaveFile(i18n.text("text.25c282ca0290"), "${baseName}_report.pdf", i18n.text("text.7e9c89b812eb"), "pdf", i18n = i18n)
                    ?.also { f -> state.runIo { f.outputStream().use { DesktopDocumentManager.exportCompositePdf(project, filter, selection, it, i18n = i18n, planImage = state::planImage) } } }
                DocFormat.PRINT -> {
                    val printed = state.runIo { DesktopDocumentManager.printDocumentNative(project, filter, selection, i18n = i18n, planImage = state::planImage) }
                    state.notify(if (printed) i18n.text("text.c984feea82e6") else i18n.text("text.947bcd7a84c3"))
                    null
                }
            }?.let { state.notify(i18n.text("text.e1856cf2f6fe", it.absolutePath)) }
        } catch (e: Exception) {
            state.error = i18n.text("text.17f5484a09df", e.message)
        }
    }

    FormDialog(
        title = i18n.text("text.b59593297419"),
        onDismiss = { state.dialog = null },
        onConfirm = ::generate,
        confirmLabel = if (format == DocFormat.PRINT) i18n.text("text.f3af4cb18a36") else i18n.text("text.38f38c0c651b")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(i18n.text("text.88e997fd79f3"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = {
                state.dialog = null
                DesktopStorageHelper.pickSaveFile(i18n.text("text.4db3339e27f7"), "${DesktopAppState.safeFileName(project.name)}_etichette.pdf", i18n.text("text.7e9c89b812eb"), "pdf", i18n = i18n)
                    ?.let { f ->
                        runCatching { state.runIo { f.outputStream().use { LabelSheetPdf.write(LabelSheetPdf.labelsFor(project, i18n = i18n), it) } } }
                            .onSuccess { state.notify(i18n.text("text.dc5741dd59ff", f.absolutePath)) }
                            .onFailure { state.error = i18n.text("text.f887637decf7", it.message) }
                    }
            }) { Text(i18n.text("text.76b0eaaea378")) }
        }
        HorizontalDivider()
        Text(i18n.text("text.73e1804eb968"), fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DocFormat.entries.forEach { f ->
                FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.localizedLabel(i18n)) })
            }
        }
        FormField(author, { author = it }, i18n.text("text.80e3789ff0b4"), hint = i18n.text("text.a56d92347ec7"))
        FormField(title, { title = it }, i18n.text("text.15cdb122536e"))
        LabeledCheckbox(includeConfidential, { includeConfidential = it }, i18n.text("text.eb8f7d4268dc"))
        Text(i18n.text("text.307a7c99d840"), style = MaterialTheme.typography.bodySmall)
        if (format == DocFormat.PDF || format == DocFormat.PRINT) {
            Text(i18n.text("text.ad0136b0ca69"), fontWeight = FontWeight.SemiBold)
            LabeledCheckbox(selection.includeInventoryTable, { selection = selection.copy(includeInventoryTable = it) }, i18n.text("text.dae8f6194460"))
            LabeledCheckbox(selection.includeFloorPlans, { selection = selection.copy(includeFloorPlans = it) }, i18n.text("report.floorPlans"))
            LabeledCheckbox(selection.includeRackCards, { selection = selection.copy(includeRackCards = it) }, i18n.text("text.37ee6ae1d2bd"))
            LabeledCheckbox(selection.includePaths, { selection = selection.copy(includePaths = it) }, i18n.text("report.paths"))
            LabeledCheckbox(selection.includeTopology, { selection = selection.copy(includeTopology = it) }, i18n.text("report.topology"))
            LabeledCheckbox(selection.includeCablingAndPorts, { selection = selection.copy(includeCablingAndPorts = it) }, i18n.text("text.541795bb3ec5"))
            LabeledCheckbox(selection.includeLogicalNetwork, { selection = selection.copy(includeLogicalNetwork = it) }, i18n.text("text.7070d68f65b5"))
            LabeledCheckbox(selection.includePowerAndBadges, { selection = selection.copy(includePowerAndBadges = it) }, i18n.text("text.0a2258044f91"))
            LabeledCheckbox(selection.includeNotesAndAttachments, { selection = selection.copy(includeNotesAndAttachments = it) }, i18n.text("text.1d02ed0f43ac"))
        }
    }
}

@Composable
private fun ValidationDialog(state: DesktopAppState) {
    val i18n = LocalMessages.current

    val project = state.project ?: return
    val index = remember(project) { ProjectIndex(project) }
    val grouped = state.issues.groupBy { it.severity }
    AlertDialog(
        onDismissRequest = { state.dialog = null },
        modifier = Modifier.width(720.dp),
        title = { Text(i18n.text("text.a590fcd25b56")) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Text(
                        i18n.text("text.99f37796ec4a"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                listOf(ValidationSeverity.STRUCTURAL_ERROR, ValidationSeverity.DOCUMENTARY_WARNING).forEach { severity ->
                    val list = grouped[severity].orEmpty()
                    if (list.isNotEmpty()) {
                        item {
                            Text(
                                "${severity.toDisplayString(i18n = i18n)} (${list.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (severity == ValidationSeverity.STRUCTURAL_ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(list) { issue ->
                            val target = state.sectionOf(issue.targetEntityId)
                            ItemCard(
                                title = index.entityName(issue.targetEntityId, i18n = i18n) ?: i18n.text("text.b7700d71d0ce"),
                                details = listOf(issue.message)
                            ) {
                                if (target != null) {
                                    TextButton(onClick = { state.section = target; state.dialog = null }) { Text(i18n.text("text.5b1d21b49b10", target.localizedTitle(i18n))) }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { state.dialog = null }) { Text(i18n.text("text.32d4079b315b")) } }
    )
}
