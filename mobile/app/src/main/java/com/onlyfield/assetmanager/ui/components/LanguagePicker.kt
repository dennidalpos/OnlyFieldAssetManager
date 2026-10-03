package com.onlyfield.assetmanager.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.onlyfield.assetmanager.core.i18n.AppLanguage
import com.onlyfield.assetmanager.ui.LocalMessages
import com.onlyfield.assetmanager.ui.ProjectViewModel

@Composable
fun LanguagePicker(vm: ProjectViewModel) {
    val i18n = LocalMessages.current
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }, enabled = vm.busy == null) { Text(i18n.text("language.label")) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        AppLanguage.entries.forEach { language ->
            DropdownMenuItem(
                text = { Text(if (language == AppLanguage.SYSTEM) i18n.text("language.system") else language.nativeName) },
                onClick = { expanded = false; vm.changeLanguage(language) }
            )
        }
    }
}
