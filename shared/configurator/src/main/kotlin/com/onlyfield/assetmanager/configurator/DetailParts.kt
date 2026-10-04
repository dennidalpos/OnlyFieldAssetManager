package com.onlyfield.assetmanager.configurator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.configurator.map.MapStyle
import com.onlyfield.assetmanager.core.display.ObjectSummary
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Glyph

/** Family colour with the non-translated symbol (SW, AP…); the same badge on map, lists and editors. */
@Composable
fun GlyphBadge(glyph: Glyph, size: Dp = 40.dp) {
    Box(Modifier.size(size).background(MapStyle.family(glyph.family), RoundedCornerShape(size / 5)), contentAlignment = Alignment.Center) {
        Text(glyph.code, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1,
            style = if (size >= 36.dp) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall)
    }
}

/** Text glyph button that screen readers announce by [description], not by the symbol. */
@Composable
fun SymbolButton(symbol: String, description: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        Text(symbol, Modifier.clearAndSetSemantics {})
    }
}

/** Object identity: badge, name and one subtitle line, with an optional overline and close button. */
@Composable
fun PaneHeader(glyph: Glyph?, title: String, subtitle: String?, i18n: Messages, overline: String? = null, onClose: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        glyph?.let { GlyphBadge(it) }
        Column(Modifier.weight(1f)) {
            overline?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
            subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        onClose?.let { SymbolButton("✕", i18n.text("ux.close"), it) }
    }
}

/** Section heading with an optional count and trailing actions. */
@Composable
fun SectionTitle(title: String, count: Int? = null, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (count != null) "$title ($count)" else title, style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f).semantics { heading() })
        trailing()
    }
}

/** Label/value rows; each row is read as one "label: value" item. */
@Composable
fun FactRows(facts: List<ObjectSummary.Fact>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        facts.forEach { fact ->
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(fact.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.4f))
                Text(fact.value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

class PaneAction(val label: String, val onClick: () -> Unit)

/** One filled primary action, outlined secondaries and an overflow menu for structural changes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionRow(i18n: Messages, primary: PaneAction?, secondary: List<PaneAction> = emptyList(), overflow: List<PaneAction> = emptyList()) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        primary?.let { Button(onClick = it.onClick) { Text(it.label) } }
        secondary.forEach { OutlinedButton(onClick = it.onClick) { Text(it.label) } }
        if (overflow.isNotEmpty()) Box {
            var open by remember { mutableStateOf(false) }
            SymbolButton("⋮", i18n.text("ux.more")) { open = true }
            DropdownMenu(open, { open = false }) {
                overflow.forEach { action -> DropdownMenuItem(text = { Text(action.label) }, onClick = { open = false; action.onClick() }) }
            }
        }
    }
}

/**
 * Read-only field that opens a menu: same outline and floating label as text fields.
 * Screen readers get one button, "label: value".
 */
@Composable
fun SelectField(label: String, value: String, modifier: Modifier = Modifier, error: String? = null, enabled: Boolean = true, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = if (!enabled) OutlinedTextFieldDefaults.colors() else OutlinedTextFieldDefaults.colors(
        disabledTextColor = scheme.onSurface,
        disabledBorderColor = if (error != null) scheme.error else scheme.outline,
        disabledLabelColor = if (error != null) scheme.error else scheme.onSurfaceVariant,
        disabledTrailingIconColor = scheme.onSurfaceVariant,
        disabledSupportingTextColor = scheme.error,
    )
    Box(modifier.fillMaxWidth()
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) { onOpen() }
        .clearAndSetSemantics {
            contentDescription = "$label: $value"
            role = Role.Button
            error?.let { this.error(it) }
            if (enabled) onClick(label) { onOpen(); true } else disabled()
        }) {
        OutlinedTextField(value, {}, enabled = false, readOnly = true, singleLine = true, label = { Text(label) },
            trailingIcon = { Text("▾") }, isError = error != null, supportingText = error?.let { { Text(it) } },
            colors = colors, modifier = Modifier.fillMaxWidth())
    }
}
