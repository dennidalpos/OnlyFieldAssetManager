package com.onlyfield.assetmanager.configurator.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Height left for the body of the enclosing [ContentDialog]: nested lists size themselves from it. */
val LocalDialogBodyHeight = compositionLocalOf { 320.dp }

private val WideWidth = 960.dp
/** Title, actions and paddings around the body. */
private val DialogChrome = 168.dp

/**
 * One body and persistent actions, including with an open keyboard. Compact windows are filled;
 * larger ones keep text forms at [width] and let [wide] lists and drawings grow.
 * With [scrollBody] off the body gets the remaining height, so a lazy list can fill it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentDialog(onDismissRequest: () -> Unit, title: @Composable () -> Unit,
    text: @Composable () -> Unit, confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit = {}, width: Dp = AppSpacing.formWidth, wide: Boolean = false, scrollBody: Boolean = true) {
    val density = LocalDensity.current
    val windowHeight = contentDialogMaxHeight()
    Dialog(onDismissRequest, properties = contentDialogProperties()) {
        CompositionLocalProvider(LocalDensity provides density) {
            BoxWithConstraints(Modifier.heightIn(max = windowHeight).fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
                val compact = maxWidth < 600.dp || maxHeight < 480.dp
                val margin = if (compact) 0.dp else AppSpacing.section
                val available = (maxHeight - margin * 2).coerceAtLeast(1.dp)
                Surface(Modifier.padding(margin).widthIn(max = if (wide) maxOf(width, WideWidth) else width).fillMaxWidth()
                    .then(if (compact) Modifier.fillMaxHeight() else Modifier.heightIn(max = available)),
                    shape = if (compact) RectangleShape else MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
                    border = if (compact) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(AppSpacing.content), verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
                        title()
                        CompositionLocalProvider(LocalDialogBodyHeight provides (available - DialogChrome).coerceAtLeast(160.dp)) {
                            Box(Modifier.weight(1f, fill = compact).fillMaxWidth().then(if (scrollBody) Modifier.verticalScroll(rememberScrollState()) else Modifier)) { text() }
                        }
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.small, Alignment.End),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                            dismissButton()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
