package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.ui.LocalMessages

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
    val i18n = LocalMessages.current

    val w = vm.newSite
    val errors = w.errors(i18n = i18n)
    fun set(t: (NewSiteDraft) -> NewSiteDraft) {
        vm.newSite = vm.newSite.update(t)
    }
    AppScaffold(
        title = i18n.text("text.ac667fe865c9"),
        subtitle = i18n.text("text.fdda08f5952a", w.stepNumber, w.stepCount, w.step.localizedTitle(i18n)),
        onBack = { vm.back() },
        snackbarHost = snackbar,
        busy = vm.busy,
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(progress = { w.stepNumber / w.stepCount.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(w.step.localizedTitle(i18n), style = MaterialTheme.typography.headlineSmall)
            Text(w.step.localizedHint(i18n), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // key(): the fields' "touched" state restarts on each step
            key(w.step) {
                val d = w.draft
                when (w.step) {
                    NewSiteStep.PROJECT -> {
                        FormField(d.projectName, { v -> set { it.copy(projectName = v) } }, i18n.text("text.85afe7453202"), error = errors["projectName"])
                        FormField(d.customer, { v -> set { it.copy(customer = v) } }, i18n.text("text.f851d9a83ab0"), hint = i18n.text("text.98c72991302e"))
                    }
                    NewSiteStep.BUSINESS_UNIT, NewSiteStep.AREA -> WizardLists(w) { vm.newSite = it }
                    NewSiteStep.PASSWORD -> {
                        PasswordInput(d.password, { v -> set { it.copy(password = v) } }, i18n.text("text.e7cf3ef4f17c"), null)
                        PasswordInput(d.passwordConfirm, { v -> set { it.copy(passwordConfirm = v) } }, i18n.text("text.44d09ab8e50d"), errors["passwordConfirm"])
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!w.isFirst) OutlinedButton(onClick = { vm.back() }) { Text(i18n.text("text.80426885bb74")) }
                Spacer(Modifier.weight(1f))
                if (w.step.skippable && !w.isLast) TextButton(onClick = { vm.newSite = w.skip() }) { Text(i18n.text("text.fb397a42956c")) }
                Button(
                    enabled = w.canProceed && vm.busy == null,
                    onClick = { if (w.isLast) vm.finishNewSite() else vm.newSite = w.next() }
                ) { Text(if (w.isLast) i18n.text("text.6c4a7984bdc6") else i18n.text("text.29ddfd8a8643")) }
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
