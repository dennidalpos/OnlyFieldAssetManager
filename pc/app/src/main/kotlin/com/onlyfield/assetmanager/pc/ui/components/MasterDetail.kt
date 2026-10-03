package com.onlyfield.assetmanager.pc.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Marks unsaved form changes. */
val LocalMarkDirty = staticCompositionLocalOf<() -> Unit> { {} }

/** Holds the editor currently shown in the right pane of [MasterDetailHost]. */
class DetailSlot {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
    var dirty by mutableStateOf(false)
    var editorVersion by mutableStateOf(0)
    var pendingChange by mutableStateOf<(() -> Unit)?>(null)
    var dismiss: (() -> Unit)? = null

    fun requestChange(action: () -> Unit) {
        if (dirty) pendingChange = action else change(action)
    }

    fun change(action: () -> Unit) {
        pendingChange = null
        dirty = false
        dismiss?.invoke()
        editorVersion++
        action()
    }
}

val LocalDetailSlot = staticCompositionLocalOf<DetailSlot?> { null }
val LocalDetailChange = staticCompositionLocalOf<(() -> Unit) -> Unit> { { it() } }

/** Section on the left (list with search), the open [EditPanel] on the right. */
@Composable
fun MasterDetailHost(modifier: Modifier = Modifier, master: @Composable () -> Unit) {
    val slot = remember { DetailSlot() }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            CompositionLocalProvider(LocalDetailSlot provides slot, LocalDetailChange provides slot::requestChange) { master() }
        }
        key(slot.editorVersion) { slot.content?.invoke() }
    }
    slot.pendingChange?.let { action ->
        AlertDialog(
            onDismissRequest = { slot.pendingChange = null },
            title = { Text("Scartare le modifiche?") },
            text = { Text("Le modifiche non salvate andranno perse.") },
            confirmButton = { TextButton(onClick = { slot.change(action) }) { Text("Scarta") } },
            dismissButton = { TextButton(onClick = { slot.pendingChange = null }) { Text("Continua a modificare") } }
        )
    }
}

/** Entity editor: Ctrl+S saves, Esc requests closing. */
@Composable
fun EditPanel(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmLabel: String = "Salva",
    width: Dp = 560.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val slot = LocalDetailSlot.current
    if (slot == null) {
        FormDialog(title, onDismiss, onConfirm, confirmEnabled, confirmLabel, width, content)
        return
    }
    // The pane composes elsewhere: read the latest arguments through updated state.
    val args by rememberUpdatedState(PanelArgs(title, onDismiss, onConfirm, confirmEnabled, confirmLabel, width, content))
    val panel: @Composable () -> Unit = remember { { PanelBody(args, slot) } }
    DisposableEffect(slot) {
        slot.content = panel
        slot.dismiss = { args.onDismiss() }
        slot.dirty = false
        onDispose {
            if (slot.content === panel) {
                slot.content = null
                slot.dismiss = null
                slot.dirty = false
                slot.pendingChange = null
            }
        }
    }
}

private class PanelArgs(
    val title: String,
    val onDismiss: () -> Unit,
    val onConfirm: () -> Unit,
    val confirmEnabled: Boolean,
    val confirmLabel: String,
    val width: Dp,
    val content: @Composable ColumnScope.() -> Unit,
)

@Composable
private fun PanelBody(args: PanelArgs, slot: DetailSlot) {
    val close = { slot.requestChange {} }

    // Wide forms (e.g. configuration text) get a wider pane.
    Surface(Modifier.width(args.width.coerceIn(440.dp, 640.dp)).fillMaxHeight(), tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.fillMaxSize().padding(16.dp).onPreviewKeyEvent { e ->
                when {
                    e.type != KeyEventType.KeyDown -> false
                    e.isCtrlPressed && e.key == Key.S -> { if (args.confirmEnabled) args.onConfirm(); true }
                    e.key == Key.Escape -> { close(); true }
                    else -> false
                }
            },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    args.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                TextButton(onClick = close) { Text("✕") }
            }
            CompositionLocalProvider(LocalMarkDirty provides { slot.dirty = true }) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = args.content
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ctrl+S salva · Esc chiude", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = close) { Text("Annulla") }
                Button(onClick = args.onConfirm, enabled = args.confirmEnabled) { Text(args.confirmLabel) }
            }
        }
    }
}
