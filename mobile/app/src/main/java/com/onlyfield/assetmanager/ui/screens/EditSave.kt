package com.onlyfield.assetmanager.ui.screens

import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.ui.ProjectViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive

/** Keeps failed drafts open and ignores outcomes after the editor leaves composition. */
internal class EditSave(private val vm: ProjectViewModel, private val scope: CoroutineScope) {
    var error by mutableStateOf<String?>(null)

    fun save(message: String, onSaved: () -> Unit, transform: (Project) -> Project) {
        submit(onSaved) { result -> vm.edit(message, result, transform) }
    }

    fun submit(onSaved: () -> Unit, command: ((String?) -> Unit) -> Unit) {
        error = null
        command { result ->
            if (scope.isActive) {
                error = result
                if (result == null) onSaved()
            }
        }
    }
}

@Composable
internal fun rememberEditSave(vm: ProjectViewModel, vararg draftKeys: Any?): EditSave = key(vm, *draftKeys) {
    val scope = rememberCoroutineScope()
    remember(vm, scope) { EditSave(vm, scope) }
}
