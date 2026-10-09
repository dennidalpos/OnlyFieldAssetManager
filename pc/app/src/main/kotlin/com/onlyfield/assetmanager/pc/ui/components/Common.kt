package com.onlyfield.assetmanager.pc.ui.components

import com.onlyfield.assetmanager.configurator.SymbolIcons
import com.onlyfield.assetmanager.configurator.theme.AppSpacing
import com.onlyfield.assetmanager.configurator.theme.ContentDialog
import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


data class ConfirmRequest(
    val title: String,
    val message: String,
    val confirmLabel: String? = null,
    val destructive: Boolean = true,
    val onConfirm: () -> Unit,
)

/** Requests action confirmation. */
val LocalConfirm = staticCompositionLocalOf<(ConfirmRequest) -> Unit> { { it.onConfirm() } }

@Composable
fun ConfirmHost(content: @Composable () -> Unit) {
    val i18n = LocalMessages.current

    var pending by remember { mutableStateOf<ConfirmRequest?>(null) }
    CompositionLocalProvider(LocalConfirm provides { pending = it }) {
        content()
    }
    pending?.let { req ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(req.title) },
            text = { Text(req.message) },
            confirmButton = {
                TextButton(
                    onClick = { pending = null; req.onConfirm() },
                    colors = if (req.destructive) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    else ButtonDefaults.textButtonColors()
                ) { Text(req.confirmLabel ?: i18n.text("text.7efe336bd548")) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(i18n.text("text.18c9d912a210")) } }
        )
    }
}


/** Dialog for short forms. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmLabel: String = LocalMessages.current.text("text.c5997e85ae51"),
    width: androidx.compose.ui.unit.Dp = 560.dp,
    validationMessage: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val i18n = LocalMessages.current

    ContentDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
                content()
                if (!confirmEnabled) Text(validationMessage ?: i18n.text("ux.completeRequired"), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(i18n.text("ux.cancelChanges")) } },
        width = width,
    )
}

/** Field that displays [error]. */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    var touched by remember { mutableStateOf(value.isNotEmpty()) }
    val shownError = error?.takeIf { touched }
    val markDirty = LocalMarkDirty.current
    OutlinedTextField(
        value = value,
        onValueChange = { touched = true; markDirty(); onValueChange(it) },
        label = { Text(label) },
        isError = shownError != null,
        supportingText = (shownError ?: hint)?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun LabeledCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    val markDirty = LocalMarkDirty.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Checkbox, onValueChange = { markDirty(); onCheckedChange(it) }), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, modifier = Modifier.weight(1f))
    }
}


/** Section header with search and actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    searchQuery: String? = null,
    onSearchChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = LocalMessages.current.text("text.30109da716dd"),
    /** Enter handles keyboard and USB-reader input. */
    onSearchSubmit: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(bottom = AppSpacing.tiny),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.widthIn(min = 180.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (searchQuery != null && onSearchChange != null) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(searchPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(SymbolIcons.search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                modifier = Modifier.widthIn(max = 280.dp).onPreviewKeyEvent { e ->
                    if (onSearchSubmit != null && e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                        onSearchSubmit(); true
                    } else false
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { onSearchChange("") }) { Icon(SymbolIcons.close, LocalMessages.current.text("ux.clearSearch")) } }
                } else null
            )
        }
        actions()
    }
}

/** List entity with row actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemCard(
    title: String,
    details: List<String>,
    modifier: Modifier = Modifier,
    badge: String? = null,
    leading: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick).semantics { this.selected = selected } else it },
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        FlowRow(
            modifier = Modifier.padding(AppSpacing.content).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leading?.let { Row(verticalAlignment = Alignment.CenterVertically) { it() } }
            Column(modifier = Modifier.widthIn(min = 200.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    badge?.let { Tag(it) }
                }
                details.filter { it.isNotBlank() }.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    }
}

@Composable
fun Tag(text: String, container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.tertiaryContainer) {
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun EditButton(onClick: () -> Unit) {
    val i18n = LocalMessages.current

    val changeDetail = LocalDetailChange.current
    TextButton(onClick = { changeDetail(onClick) }) { Text(i18n.text("text.49e493ba9d9c")) }
}

/** Delete action with confirmation. */
@Composable
fun DeleteButton(itemName: String, onDelete: () -> Unit, label: String = LocalMessages.current.text("text.7efe336bd548"), message: String? = null) {
    val i18n = LocalMessages.current

    val confirm = LocalConfirm.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(SymbolIcons.more, i18n.text("ux.more") + " · " + itemName) }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text(label, color = MaterialTheme.colorScheme.error) }, onClick = {
                open = false
                confirm(ConfirmRequest(i18n.text("text.ab0928009332", label, itemName),
                    message ?: i18n.text("text.b5f4e725fbe1"), label, onConfirm = onDelete))
            })
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val changeDetail = LocalDetailChange.current
    Surface(modifier = modifier.fillMaxWidth().padding(vertical = AppSpacing.content), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                if (actionLabel != null && onAction != null) {
                    Button(onClick = { changeDetail(onAction) }) { Text(actionLabel) }
                }
            }
        }
    }
}

/** Section sub-navigation. */
@Composable
fun SubTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val changeDetail = LocalDetailChange.current
    PrimaryTabRow(selectedTabIndex = selected) {
        tabs.forEachIndexed { i, title ->
            Tab(selected = selected == i, onClick = { if (selected != i) changeDetail { onSelect(i) } }, text = { Text(title) })
        }
    }
}

fun matchesQuery(query: String, vararg fields: String?): Boolean =
    query.isBlank() || fields.any { it?.contains(query.trim(), ignoreCase = true) == true }
