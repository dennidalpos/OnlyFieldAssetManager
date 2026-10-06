package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.configurator.theme.OutlinedButton
import com.onlyfield.assetmanager.configurator.theme.TextButton
import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.onlyfield.assetmanager.configurator.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.pc.*
import com.onlyfield.assetmanager.pc.ui.components.*
import java.io.File

@Composable
internal fun FloorObjectEditor(state: DesktopAppState, project: Project, initial: MapObjectDraft, initialSection: ConfiguratorPage = ConfiguratorPage.ESSENTIALS, close: () -> Unit) {
    val i18n = LocalMessages.current

    if (!LocalHasMasterDetail.current) {
        MasterDetailHost(Modifier.fillMaxSize()) { FloorObjectEditor(state, project, initial, initialSection, close) }
        return
    }
    var draft by remember { mutableStateOf(initial) }
    val photos = remember { mutableStateListOf<File>() }
    var removed by remember { mutableStateOf<Set<String>>(emptySet()) }
    val dirty = draft != initial || photos.isNotEmpty() || removed.isNotEmpty()
    val slot = LocalDetailSlot.current!!
    val latestClose by rememberUpdatedState(close)
    val dismiss = remember { { latestClose() } }
    DisposableEffect(slot) {
        slot.dismiss = dismiss
        onDispose {
            if (slot.dismiss === dismiss) {
                slot.dismiss = null
                slot.dirty = false
                slot.pendingChange = null
            }
        }
    }
    SideEffect { slot.dirty = dirty }
    EditPanel(configuratorTitle(project, initial, i18n), { slot.requestChange {} }, {
        if (state.saveMapObject(draft, photos.toList(), removed)) close()
    }, validationMessage = configuratorValidation(project, draft, i18n), confirmEnabled = draft.errors(project, i18n).isEmpty(), confirmLabel = configuratorAction(project, initial, i18n), width = 640.dp) {
        ObjectFields(project, draft, initialSection, extraSections = {
            ConfiguratorSection(i18n.text("ux.attachments"), i18n = i18n, summary = (project.attachments.count { it.targetId == draft.id && it.id !in removed } + photos.size).takeIf { it > 0 }?.let { i18n.text("config.attachmentsCount", it) }) {
                project.attachments.filter { it.targetId == draft.id && it.id !in removed }.forEach { a ->
                    Text(a.name); MediaThumbnail(a.id, { state.attachmentBytes(a) }, a.fileType == AttachmentType.PDF)
                    TextButton(onClick = { removed = removed + a.id }) { Text(i18n.text("text.960630ee842c")) }
                }
                photos.toList().forEach { file ->
                    Text(file.name); MediaThumbnail(file)
                    TextButton(onClick = { photos.remove(file) }) { Text(i18n.text("text.f5115aa0e57e")) }
                }
                OutlinedButton(onClick = { DesktopStorageHelper.pickOpenFile(i18n.text("text.a111cc717443"), i18n.text("text.a9f46a362f48"), "png", "jpg", "jpeg", "webp", "bmp", i18n = i18n)?.let { photos += it } }) { Text(i18n.text("text.e2ca686d60a1")) }
            }
        }) { draft = it }
    }
}
