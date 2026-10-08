package com.onlyfield.assetmanager.configurator.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** One scrolling body and persistent actions, including with an open keyboard. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentDialog(onDismissRequest: () -> Unit, title: @Composable () -> Unit,
    text: @Composable () -> Unit, confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit = {}, width: Dp = AppSpacing.formWidth) {
    val density = LocalDensity.current
    val windowHeight = contentDialogMaxHeight()
    Dialog(onDismissRequest, properties = contentDialogProperties()) {
        CompositionLocalProvider(LocalDensity provides density) {
            BoxWithConstraints(Modifier.heightIn(max = windowHeight).fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
                val compact = maxWidth < 600.dp || maxHeight < 480.dp
                val margin = if (compact) 0.dp else AppSpacing.section
                Surface(Modifier.padding(margin).widthIn(max = width).fillMaxWidth()
                    .heightIn(max = (maxHeight - margin * 2).coerceAtLeast(1.dp)),
                    shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
                    Column(Modifier.padding(AppSpacing.content), verticalArrangement = Arrangement.spacedBy(AppSpacing.content)) {
                        title()
                        Box(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) { text() }
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
