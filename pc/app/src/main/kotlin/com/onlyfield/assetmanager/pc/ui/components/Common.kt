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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// --- Confirmation of destructive actions -------------------------------------------------------

data class ConfirmRequest(
    val title: String,
    val message: String,
    val confirmLabel: String = "Elimina",
    val destructive: Boolean = true,
    val onConfirm: () -> Unit,
)

/** Asks the user to confirm an action. Provided by [ConfirmHost]. */
val LocalConfirm = staticCompositionLocalOf<(ConfirmRequest) -> Unit> { { it.onConfirm() } }

@Composable
fun ConfirmHost(content: @Composable () -> Unit) {
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
                Button(
                    onClick = { pending = null; req.onConfirm() },
                    colors = if (req.destructive) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) else ButtonDefaults.buttonColors()
                ) { Text(req.confirmLabel) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Annulla") } }
        )
    }
}

// --- Dialog and form building blocks -----------------------------------------------------------

/** Dialog for short forms; entity editors use [EditPanel]. */
@Composable
fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmLabel: String = "Salva",
    width: androidx.compose.ui.unit.Dp = 560.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(width),
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        },
        confirmButton = { Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

/** Text field that shows [error] below itself and turns red when it is not null. */
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
    // Errors appear only once the user has typed something, not on a freshly opened form.
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { markDirty(); onCheckedChange(it) })
        Text(label)
    }
}

// --- Section layout ------------------------------------------------------------------------------

/** Title row of a section with an optional search box and the primary actions on the right. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    searchQuery: String? = null,
    onSearchChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "Cerca…",
    /** Enter in the search box; USB barcode readers type the code and press Enter. */
    onSearchSubmit: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (searchQuery != null && onSearchChange != null) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(searchPlaceholder) },
                singleLine = true,
                modifier = Modifier.width(280.dp).onPreviewKeyEvent { e ->
                    if (onSearchSubmit != null && e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                        onSearchSubmit(); true
                    } else false
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { TextButton(onClick = { onSearchChange("") }) { Text("✕") } }
                } else null
            )
        }
        actions()
    }
}

/** One entity in a list: title, detail lines and the row actions (edit, delete…). */
@Composable
fun ItemCard(
    title: String,
    details: List<String>,
    modifier: Modifier = Modifier,
    badge: String? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    badge?.let { Tag(it) }
                }
                details.filter { it.isNotBlank() }.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

@Composable
fun Tag(text: String, container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun EditButton(onClick: () -> Unit) {
    val changeDetail = LocalDetailChange.current
    TextButton(onClick = { changeDetail(onClick) }) { Text("Modifica") }
}

/** Delete action that always asks for confirmation first. */
@Composable
fun DeleteButton(itemName: String, onDelete: () -> Unit, label: String = "Elimina", message: String? = null) {
    val confirm = LocalConfirm.current
    TextButton(
        onClick = {
            confirm(
                ConfirmRequest(
                    title = "$label «$itemName»?",
                    message = message ?: "L'elemento verrà eliminato definitivamente dal progetto.",
                    confirmLabel = label,
                    onConfirm = onDelete
                )
            )
        },
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) { Text(label) }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val changeDetail = LocalDetailChange.current
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null && onAction != null) {
                Button(onClick = { changeDetail(onAction) }) { Text(actionLabel) }
            }
        }
    }
}

/** Sub-navigation inside a section (e.g. Cavi / Percorsi / Permutazioni). */
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
