package com.onlyfield.assetmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteStep
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.components.*

/** "Nuovo sito" wizard; steps and validation come from core.onboarding.NewSiteWizard. */
@Composable
fun NewSiteScreen(vm: ProjectViewModel, snackbar: SnackbarHostState) {
    val w = vm.newSite
    val errors = w.errors()
    fun set(t: (NewSiteDraft) -> NewSiteDraft) {
        vm.newSite = vm.newSite.update(t)
    }
    AppScaffold(
        title = "Nuovo sito",
        subtitle = "Passo ${w.stepNumber} di ${w.stepCount} · ${w.step.title}",
        onBack = { vm.back() },
        snackbarHost = snackbar,
        busy = vm.busy,
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(progress = { w.stepNumber / w.stepCount.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(w.step.title, style = MaterialTheme.typography.headlineSmall)
            Text(w.step.hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        FormField(d.deviceIp, { v -> set { it.copy(deviceIp = v) } }, "Indirizzo IP", error = errors["deviceIp"], kind = FieldKind.IP)
                    }
                    NewSiteStep.PASSWORD -> {
                        PasswordInput(d.password, { v -> set { it.copy(password = v) } }, "Password", null)
                        PasswordInput(d.passwordConfirm, { v -> set { it.copy(passwordConfirm = v) } }, "Conferma password", errors["passwordConfirm"])
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!w.isFirst) OutlinedButton(onClick = { vm.back() }) { Text("Indietro") }
                Spacer(Modifier.weight(1f))
                if (w.step.skippable && !w.isLast) TextButton(onClick = { vm.newSite = w.skip() }) { Text("Salta") }
                Button(
                    enabled = w.canProceed && vm.busy == null,
                    onClick = { if (w.isLast) vm.finishNewSite() else vm.newSite = w.next() }
                ) { Text(if (w.isLast) "Crea e apri" else "Avanti") }
            }
        }
    }
}

@Composable
private fun PasswordInput(value: String, onChange: (String) -> Unit, label: String, error: String?) = OutlinedTextField(
    value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
    visualTransformation = PasswordVisualTransformation(), isError = error != null,
    supportingText = error?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth()
)
