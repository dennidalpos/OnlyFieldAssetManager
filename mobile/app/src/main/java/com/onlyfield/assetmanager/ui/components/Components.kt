package com.onlyfield.assetmanager.ui.components

import com.onlyfield.assetmanager.configurator.SymbolIcons
import com.onlyfield.assetmanager.configurator.theme.AppSpacing
import com.onlyfield.assetmanager.configurator.theme.ContentDialog
import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.core.display.sortedForDisplay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


data class ConfirmRequest(
    val title: String,
    val message: String,
    val confirmLabel: String? = null,
    val destructive: Boolean = true,
    val onConfirm: () -> Unit,
)

val LocalConfirm = staticCompositionLocalOf<(ConfirmRequest) -> Unit> { { it.onConfirm() } }

@Composable
fun ConfirmHost(content: @Composable () -> Unit) {
    val i18n = LocalMessages.current

    var pending by remember { mutableStateOf<ConfirmRequest?>(null) }
    CompositionLocalProvider(LocalConfirm provides { pending = it }) { content() }
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)?,
    snackbarHost: SnackbarHostState,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    busy: String? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val i18n = LocalMessages.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = i18n.text("text.80426885bb74")) }
                    },
                    actions = actions
                )
                if (busy != null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = floatingActionButton,
        content = content
    )
}

data class MenuAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** Secondary actions, with destructive entries separated and confirmed by the caller. */
@Composable
fun OverflowMenu(actions: List<MenuAction>, contentDescription: String = LocalMessages.current.text("text.93f019bac960")) {
    if (actions.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = contentDescription) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.filterNot { it.destructive }.forEach { a -> DropdownMenuItem(text = { Text(a.label) }, onClick = { open = false; a.onClick() }) }
            if (actions.any { it.destructive } && actions.any { !it.destructive }) HorizontalDivider()
            actions.filter { it.destructive }.forEach { a -> DropdownMenuItem(text = { Text(a.label, color = MaterialTheme.colorScheme.error) }, onClick = { open = false; a.onClick() }) }
        }
    }
}


/** List entity with primary and secondary actions. */
@Composable
fun ItemCard(
    title: String,
    details: List<String>,
    modifier: Modifier = Modifier,
    badge: String? = null,
    leading: (@Composable () -> Unit)? = null,
    menu: List<MenuAction> = emptyList(),
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(AppSpacing.content), verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                badge?.let { Tag(it) }
                details.filter { it.isNotBlank() }.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (menu.isNotEmpty()) OverflowMenu(menu)
        }
    }
}

@Composable
fun Tag(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun SearchField(query: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        trailingIcon = if (query.isNotEmpty()) { { IconButton(onClick = { onChange("") }) { Icon(SymbolIcons.close, LocalMessages.current.text("ux.clearSearch")) } } } else null,
        modifier = modifier.fillMaxWidth()
    )
}

/** Up to three fixed tabs; longer lists become one menu so nothing scrolls sideways. */
@Composable
fun SubTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    if (tabs.size <= 3) TabRow(selectedTabIndex = selected) {
        tabs.forEachIndexed { i, t -> Tab(selected = selected == i, onClick = { onSelect(i) }, text = { Text(t, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }) }
    } else com.onlyfield.assetmanager.configurator.map.ValueMenu(LocalMessages.current.text("ux.view"), selected, tabs.indices.toList(), { tabs[it] },
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) { onSelect(it) }
}

fun matchesQuery(query: String, vararg fields: String?): Boolean =
    query.isBlank() || fields.any { it?.contains(query.trim(), ignoreCase = true) == true }


/** Dialog for short forms. */
@Composable
fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmLabel: String = LocalMessages.current.text("text.c5997e85ae51"),
    content: @Composable ColumnScope.() -> Unit,
) {
    val i18n = LocalMessages.current

    ContentDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.content), content = content)
        },
        confirmButton = { Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(i18n.text("ux.cancelChanges")) } },
    )
}

enum class FieldKind { TEXT, NUMBER, DECIMAL, IP }

/** Field that delays [error] until input. */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    kind: FieldKind = FieldKind.TEXT,
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
        keyboardOptions = KeyboardOptions(
            keyboardType = when (kind) {
                FieldKind.NUMBER -> KeyboardType.Number
                FieldKind.DECIMAL -> KeyboardType.Decimal
                FieldKind.IP -> KeyboardType.Uri
                FieldKind.TEXT -> KeyboardType.Text
            }
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun LabeledCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    val markDirty = LocalMarkDirty.current
    val change = { v: Boolean -> markDirty(); onCheckedChange(v) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Checkbox, onValueChange = change)) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

/** Read-only field that opens a searchable dialog. */
@Composable
fun <T> OptionPicker(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
    optionDetail: ((T) -> String?)? = null,
    noneLabel: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    sortByName: Boolean = true,
) {
    val i18n = LocalMessages.current

    var open by remember { mutableStateOf(false) }
    val markDirty = LocalMarkDirty.current
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: noneLabel ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(i18n.text("text.60d6013749f3")) },
            trailingIcon = { Icon(SymbolIcons.expand, null) },
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier.matchParentSize().padding(top = 8.dp)
                .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null) { open = true }
        )
    }
    if (open) {
        var query by remember { mutableStateOf("") }
        val ordered = if (sortByName && options.firstOrNull() !is Number) options.sortedForDisplay(i18n, optionLabel) else options
        val filtered = ordered.filter { o -> matchesQuery(query, optionLabel(o), optionDetail?.invoke(o)) }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label.removeSuffix(" *")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (options.size > 7) SearchField(query, { query = it }, i18n.text("text.30109da716dd"))
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        if (noneLabel != null && query.isBlank()) {
                            item {
                                ListItem(headlineContent = { Text(noneLabel) }, modifier = Modifier.clickable { markDirty(); onSelected(null); open = false })
                                HorizontalDivider()
                            }
                        }
                        if (filtered.isEmpty()) item { Text(if (options.isEmpty()) i18n.text("text.0391aa0b5565") else i18n.text("text.0af987882e51"), modifier = Modifier.padding(16.dp)) }
                        items(filtered) { o ->
                            ListItem(
                                headlineContent = { Text(optionLabel(o)) },
                                supportingContent = optionDetail?.invoke(o)?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
                                trailingContent = if (o == selected) { { Text("✓") } } else null,
                                modifier = Modifier.clickable { markDirty(); onSelected(o); open = false }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(i18n.text("text.32d4079b315b")) } }
        )
    }
}

@Composable
fun <E : Enum<E>> EnumPicker(label: String, values: List<E>, selected: E, valueLabel: (E) -> String, onSelected: (E) -> Unit, modifier: Modifier = Modifier) {
    OptionPicker(label, values, selected, valueLabel, { it?.let(onSelected) }, modifier, sortByName = false)
}
