package com.onlyfield.assetmanager.pc.ui.components

import com.onlyfield.assetmanager.configurator.theme.AppSpacing
import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Marks unsaved form changes. */
val LocalMarkDirty = staticCompositionLocalOf { {} }

/** Editor shown in the right pane. */
class DetailSlot {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
    var panelWidth by mutableStateOf(560.dp)
    var dirty by mutableStateOf(false)
    var editorVersion by mutableStateOf(0)
    var pendingChange by mutableStateOf<(() -> Unit)?>(null)
    var dismiss: (() -> Unit)? = null

    fun requestChange(action: () -> Unit) {
        if (pendingChange != null) return
        if (dirty) pendingChange = action else change(action)
    }

    fun change(action: () -> Unit) {
        pendingChange = null
        dirty = false
        val close = dismiss
        dismiss = null
        close?.invoke()
        editorVersion++
        action()
    }
}

val LocalHasMasterDetail = staticCompositionLocalOf { false }

val LocalPanelWidth = staticCompositionLocalOf { 560.dp }

val LocalDetailSlot = staticCompositionLocalOf<DetailSlot?> { null }
val LocalDetailChange = staticCompositionLocalOf<(() -> Unit) -> Unit> { { it() } }

/** List section and open editor. */
@Composable
fun MasterDetailHost(modifier: Modifier = Modifier, master: @Composable () -> Unit) {
    val inherited = LocalDetailSlot.current
    if (inherited == null) {
        val slot = remember { DetailSlot() }
        DetailChangeHost(slot) { MasterDetailHost(modifier, master) }
        return
    }
    val slot = inherited
    BoxWithConstraints(modifier) {
        val availableWidth = maxWidth
        val editorVisible = slot.content != null
        val editorWidth = slot.panelWidth.coerceIn(440.dp, 640.dp).coerceAtMost(availableWidth)
        val fullEditor = editorVisible && availableWidth < editorWidth + 376.dp
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (fullEditor) 0.dp else AppSpacing.content)) {
            // Keep the master composed so it retains the editor and draft while narrow.
            Box(Modifier.width(if (fullEditor) 0.dp else if (editorVisible) availableWidth - editorWidth - 16.dp else availableWidth).fillMaxHeight().clipToBounds()) {
                CompositionLocalProvider(LocalHasMasterDetail provides true, LocalDetailSlot provides slot, LocalDetailChange provides slot::requestChange) { master() }
            }
            CompositionLocalProvider(LocalPanelWidth provides if (fullEditor) availableWidth else editorWidth) {
                // Keyed by the panel too, so a different editor never inherits the previous scroll offset.
                slot.content?.let { panel -> key(slot.editorVersion, panel) { panel() } }
            }
        }
    }
}

/** Shares the discard guard. */
@Composable
fun DetailChangeHost(slot: DetailSlot, content: @Composable () -> Unit) {
    val i18n = LocalMessages.current

    CompositionLocalProvider(LocalDetailSlot provides slot, LocalDetailChange provides slot::requestChange) { content() }
    slot.pendingChange?.let { action ->
        AlertDialog(
            onDismissRequest = { slot.pendingChange = null },
            title = { Text(i18n.text("text.80079f180a43")) },
            text = { Text(i18n.text("text.1c197da4d332")) },
            confirmButton = { TextButton(onClick = { slot.change(action) }) { Text(i18n.text("text.8386702d2ee2")) } },
            dismissButton = { TextButton(onClick = { slot.pendingChange = null }) { Text(i18n.text("text.500cb3f3c51b")) } }
        )
    }
}

/** Editor with save and close shortcuts. */
@Composable
fun EditPanel(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmLabel: String = LocalMessages.current.text("text.c5997e85ae51"),
    width: Dp = 560.dp,
    validationMessage: String? = null,
    wideContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {

    val slot = LocalDetailSlot.current
    if (slot == null) {
        FormDialog(title, onDismiss, onConfirm, confirmEnabled, confirmLabel, width, validationMessage, content)
        return
    }
    val args by rememberUpdatedState(PanelArgs(title, onDismiss, onConfirm, confirmEnabled, confirmLabel, validationMessage, wideContent, content))
    val panel: @Composable () -> Unit = remember { { PanelBody(args, slot) } }
    DisposableEffect(slot, slot.editorVersion) {
        slot.panelWidth = width
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
    val validationMessage: String?,
    val wideContent: Boolean,
    val content: @Composable ColumnScope.() -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanelBody(args: PanelArgs, slot: DetailSlot) {
    val i18n = LocalMessages.current

    val close = { slot.requestChange {} }

    Surface(Modifier.width(LocalPanelWidth.current).fillMaxHeight(), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.fillMaxSize().padding(AppSpacing.content).onPreviewKeyEvent { e ->
                when {
                    e.type != KeyEventType.KeyDown -> false
                    ((e.isCtrlPressed) && (e.key == Key.S)) -> { if (args.confirmEnabled) args.onConfirm(); true }
                    e.key == Key.Escape -> { close(); true }
                    else -> false
                }
            },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    args.title, style = MaterialTheme.typography.titleLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider()
            CompositionLocalProvider(LocalMarkDirty provides { slot.dirty = true }) {
                Column(
                    Modifier.weight(1f).align(Alignment.CenterHorizontally).then(if (args.wideContent) Modifier else Modifier.widthIn(max = AppSpacing.formWidth)).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = args.content
                )
            }
            HorizontalDivider()
            if (!args.confirmEnabled) Text(args.validationMessage ?: i18n.text("ux.completeRequired"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = close) { Text(i18n.text("ux.cancelChanges")) }
                Button(onClick = args.onConfirm, enabled = args.confirmEnabled) { Text(args.confirmLabel) }
            }
        }
    }
}
