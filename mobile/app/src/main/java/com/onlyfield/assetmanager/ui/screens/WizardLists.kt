package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.onboarding.*
import com.onlyfield.assetmanager.ui.components.*

@Composable
internal fun WizardLists(w: NewSiteWizard, onChange: (NewSiteWizard) -> Unit) {
    val i18n = LocalMessages.current

    var name by remember(w.step) { mutableStateOf("") }
    val sites = w.draft.sites
    val site = sites.find { it.id == w.draft.selectedSiteId } ?: sites.firstOrNull()
    if (w.step == NewSiteStep.SITE) {
        sites.forEach { b ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(b.name, { value -> onChange(w.update { d -> d.copy(sites = d.sites.map { if (it.id == b.id) it.copy(name = value) else it }) }) }, label = { Text(i18n.text("text.e4de7d26b141")) }, modifier = Modifier.weight(1f), singleLine = true)
                TextButton(onClick = { onChange(w.update { it.copy(sites = it.sites.filterNot { b2 -> b2.id == b.id }) }) }) { Text(i18n.text("text.fd69f0d7f263")) }
            }
        }
        FormField(name, { name = it }, i18n.text("text.af62c5480598"))
        OutlinedButton(enabled = name.isNotBlank(), onClick = { onChange(w.addSite(name)); name = "" }) { Text(i18n.text("text.4e90901d9fa2")) }
    } else {
        OptionPicker(i18n.text("text.e4de7d26b141"), sites, site, { it.name }, { b -> onChange(w.update { it.copy(selectedSiteId = b?.id) }) })
        site?.areas?.forEach { a ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(a.name, { value -> onChange(w.update { d -> d.copy(sites = d.sites.map { b -> if (b.id == site.id) b.copy(areas = b.areas.map { if (it.id == a.id) it.copy(name = value) else it }) else b }) }) }, label = { Text(i18n.text("text.7b417b994cc4")) }, modifier = Modifier.weight(1f), singleLine = true)
                TextButton(onClick = { onChange(w.update { d -> d.copy(sites = d.sites.map { b -> if (b.id == site.id) b.copy(areas = b.areas.filterNot { it.id == a.id }) else b }) }) }) { Text(i18n.text("text.fd69f0d7f263")) }
            }
        }
        FormField(name, { name = it }, i18n.text("text.cb6d93809fa3"))
        OutlinedButton(enabled = site != null && name.isNotBlank(), onClick = { onChange(w.addArea(site!!.id, name)); name = "" }) { Text(i18n.text("text.3575ad226840")) }
    }
    w.errors(i18n = i18n).values.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
}
