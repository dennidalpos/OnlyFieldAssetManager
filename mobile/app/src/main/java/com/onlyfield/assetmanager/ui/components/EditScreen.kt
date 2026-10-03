package com.onlyfield.assetmanager.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Called by the input components on every user change; [EditScreen] uses it to know the form is dirty. */
val LocalMarkDirty = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * Full-page entity editor with Annulla/Salva in the top bar, drawn over the current screen (the root
 * Surface is a Box), so system bars and insets stay the activity's. Back or Annulla on a modified form
 * asks before discarding. Rotation keeps the state because MainActivity handles configuration changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmLabel: String = "Salva",
    content: @Composable ColumnScope.() -> Unit,
) {
    var dirty by remember { mutableStateOf(false) }
    var askDiscard by remember { mutableStateOf(false) }
    val close = { if (dirty) askDiscard = true else onDismiss() }

    // Registered after the root handler, so it wins while the editor is open.
    BackHandler(onBack = close)
    // Surface also stops touches from reaching the screen below.
    Surface(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { IconButton(onClick = close) { Icon(Icons.Default.Close, contentDescription = "Annulla") } },
                    actions = { TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) } }
                )
            }
        ) { padding ->
            CompositionLocalProvider(LocalMarkDirty provides { dirty = true }) {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().imePadding()
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
    if (askDiscard) {
        AlertDialog(
            onDismissRequest = { askDiscard = false },
            title = { Text("Scartare le modifiche?") },
            text = { Text("Le modifiche non salvate andranno perse.") },
            confirmButton = { TextButton(onClick = { askDiscard = false; onDismiss() }) { Text("Scarta") } },
            dismissButton = { TextButton(onClick = { askDiscard = false }) { Text("Continua a modificare") } }
        )
    }
}
