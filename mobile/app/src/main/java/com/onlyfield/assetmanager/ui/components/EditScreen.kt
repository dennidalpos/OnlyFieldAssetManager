package com.onlyfield.assetmanager.ui.components

import com.onlyfield.assetmanager.configurator.theme.AppSpacing
import com.onlyfield.assetmanager.configurator.theme.Button
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.ui.LocalMessages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Marks an [EditScreen] form as dirty. */
val LocalMarkDirty = staticCompositionLocalOf<() -> Unit> { {} }

/** Full-page editor that guards dirty-form discard. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmLabel: String = LocalMessages.current.text("text.c5997e85ae51"),
    validationMessage: String? = null,
    wideContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val i18n = LocalMessages.current

    TrackOverlay()
    var dirty by remember { mutableStateOf(false) }
    var askDiscard by remember { mutableStateOf(false) }
    val close = { if (dirty) askDiscard = true else onDismiss() }

    BackHandler(onBack = close)
    Surface(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                )
            },
            bottomBar = {
                Surface(tonalElevation = 2.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = AppSpacing.content, vertical = AppSpacing.small), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!confirmEnabled) Text(validationMessage ?: i18n.text("ux.completeRequired"), style = MaterialTheme.typography.bodySmall)
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = close) { Text(i18n.text("ux.cancelChanges")) }
                            Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
                        }
                    }
                }
            }
        ) { padding ->
            CompositionLocalProvider(LocalMarkDirty provides { dirty = true }) {
                BoxWithConstraints(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize()) {
                    Column(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
                            .then(if (wideContent) Modifier else Modifier.widthIn(max = AppSpacing.formWidth))
                            .fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(AppSpacing.page(maxWidth)),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.content), content = content
                    )
                }
            }
        }
    }
    if (askDiscard) {
        AlertDialog(
            onDismissRequest = { askDiscard = false },
            title = { Text(i18n.text("text.80079f180a43")) },
            text = { Text(i18n.text("text.1c197da4d332")) },
            confirmButton = { TextButton(onClick = { askDiscard = false; onDismiss() }) { Text(i18n.text("text.8386702d2ee2")) } },
            dismissButton = { TextButton(onClick = { askDiscard = false }) { Text(i18n.text("text.500cb3f3c51b")) } }
        )
    }
}
