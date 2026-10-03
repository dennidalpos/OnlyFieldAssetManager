package com.onlyfield.assetmanager.pc.ui.components

import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Read-only field that opens a dropdown of [options]. With more than a few options a search box
 * is shown at the top of the list, so long lists of devices or ports stay usable.
 *
 * When [noneLabel] is set, an extra entry lets the user clear the selection ([onSelected] receives null).
 */
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
    placeholder: String = LocalMessages.current.text("text.60d6013749f3"),
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    val i18n = LocalMessages.current

    val markDirty = LocalMarkDirty.current
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var fieldWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val searchable = options.size > 7

    val shownText = selected?.let(optionLabel) ?: noneLabel ?: ""
    val filtered = remember(options, query) {
        if (query.isBlank()) options
        else options.filter { opt ->
            optionLabel(opt).contains(query, ignoreCase = true) ||
                (optionDetail?.invoke(opt)?.contains(query, ignoreCase = true) == true)
        }
    }

    Box(modifier = modifier.onGloballyPositioned { fieldWidthPx = it.size.width }) {
        OutlinedTextField(
            value = shownText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { Text(if (expanded) "▴" else "▾") },
            supportingText = supportingText?.let { { Text(it) } },
            isError = isError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay: a read-only text field does not open the menu on click by itself.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = true; query = "" }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(density) { fieldWidthPx.toDp() }.coerceAtLeast(240.dp))
                .heightIn(max = 380.dp)
        ) {
            if (searchable) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(i18n.text("text.30109da716dd")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (noneLabel != null && query.isBlank()) {
                DropdownMenuItem(
                    text = { Text(noneLabel, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { markDirty(); onSelected(null); expanded = false }
                )
                HorizontalDivider()
            }
            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(if (options.isEmpty()) i18n.text("text.0391aa0b5565") else i18n.text("text.0af987882e51")) },
                    onClick = {},
                    enabled = false
                )
            }
            filtered.forEach { opt ->
                val detail = optionDetail?.invoke(opt)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(optionLabel(opt), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!detail.isNullOrBlank()) {
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    onClick = { markDirty(); onSelected(opt); expanded = false }
                )
            }
        }
    }
}

/** Picker over every value of an enum, shown with its Italian label. */
@Composable
fun <E : Enum<E>> EnumPicker(
    label: String,
    values: List<E>,
    selected: E,
    valueLabel: (E) -> String,
    onSelected: (E) -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionPicker(
        label = label,
        options = values,
        selected = selected,
        optionLabel = valueLabel,
        onSelected = { it?.let(onSelected) },
        modifier = modifier
    )
}
