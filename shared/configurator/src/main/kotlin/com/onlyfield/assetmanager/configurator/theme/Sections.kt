package com.onlyfield.assetmanager.configurator.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Outlined group: whatever is inside visibly belongs to [title]. */
@Composable
fun SectionCard(title: String?, modifier: Modifier = Modifier, count: Int? = null, detail: String? = null,
                trailing: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(AppSpacing.inset), verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            if (title != null) Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (count != null) "$title ($count)" else title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                    detail?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                trailing()
            }
            content()
        }
    }
}

/** List entry flush with the section title: fixed leading slot, one-line title, optional second line. */
@Composable
fun ListRow(title: String, modifier: Modifier = Modifier, subtitle: String? = null, leading: (@Composable () -> Unit)? = null,
            trailing: (@Composable RowScope.() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .heightIn(min = 48.dp).padding(vertical = AppSpacing.tiny),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.inset)) {
        leading?.let { Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { it() } }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke(this)
    }
}

/** [ListRow]s separated by hairlines. */
@Composable
fun <T> ListRows(items: List<T>, row: @Composable (T) -> Unit) {
    items.forEachIndexed { i, item ->
        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        row(item)
    }
}

/** Short fields side by side where they fit, stacked on phones; give each field [formCell]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormRow(modifier: Modifier = Modifier, content: @Composable FormRowScope.() -> Unit) =
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.inset),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.inset)) { remember(this) { FormRowScope(this) }.content() }

/** Keeps the experimental FlowRow scope out of the screens. */
@OptIn(ExperimentalLayoutApi::class)
class FormRowScope internal constructor(private val flow: FlowRowScope) {
    /** Equal share of the line; wraps below [min], which suits short numeric fields when lowered. */
    fun formCell(min: Dp = 200.dp): Modifier = with(flow) { Modifier.widthIn(min = min).weight(1f) }
}
