package com.onlyfield.assetmanager.ui.screens

import com.onlyfield.assetmanager.ui.LocalMessages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.cartography.CartographicSource
import com.onlyfield.assetmanager.cartography.MapSnapshotRequest
import com.onlyfield.assetmanager.core.forms.FieldValidators
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.components.*

@Composable
fun AttachmentsScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val context = LocalContext.current
    val confirm = LocalConfirm.current
    val index = remember(project) { ProjectIndex(project) }
    var picked by remember { mutableStateOf<Uri?>(null) }
    var floorplanFor by remember { mutableStateOf<Attachment?>(null) }
    var planArea by remember { mutableStateOf<Area?>(null) }
    var classifying by remember { mutableStateOf<Attachment?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> picked = uri }
    var mapping by remember { mutableStateOf(false) }

    AppScaffold(
        i18n.text("text.92a776eacf2a"), onBack = { vm.back() }, snackbarHost = snackbar, busy = vm.busy,
        actions = { TextButton(onClick = { mapping = true }) { Text(i18n.text("text.ba321e5c3cea")) } },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { picker.launch(arrayOf("image/*", "application/pdf", "*/*")) },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.59cc6c3e1526")) })
        }
    ) { padding ->
        if (project.attachments.isEmpty()) EmptyState(i18n.text("text.0631006a320a"), Modifier.padding(padding))
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.attachments, key = { it.id }) { a ->
                val usedBy = index.areas.filter { it.floorplanAttachmentId == a.id }.map { it.name }
                ItemCard(
                    title = a.name,
                    badge = a.classification.toDisplayString(i18n = i18n),
                    details = listOf(
                        "${a.fileType.toDisplayString(i18n = i18n)} · ${a.originalFileName}",
                        index.attachmentTarget(a, i18n = i18n).orEmpty(),
                        usedBy.takeIf { it.isNotEmpty() }?.let { i18n.text("text.f3e846ee8611", it.joinToString()) }.orEmpty()
                    ).filter { it.isNotBlank() },
                    menu = listOfNotNull(
                        MenuAction(i18n.text("text.fa7e72cbd571")) { floorplanFor = a }.takeIf { index.areas.isNotEmpty() },
                        MenuAction(i18n.text("text.2434a2bbb0cf")) { classifying = a },
                        MenuAction(i18n.text("text.7efe336bd548"), destructive = true) {
                            confirm(ConfirmRequest(i18n.text("text.e36c23dfb086", a.name), i18n.text("text.fed29593f8bf") + if (usedBy.isNotEmpty()) i18n.text("text.2d9541d6a243") else "") {
                                vm.edit(i18n.text("text.0a1dce905d01")) { ProjectEdits.deleteAttachment(it, a.id) }
                            })
                        }
                    )
                )
            }
        }
    }

    picked?.let { uri ->
        var name by remember(uri) { mutableStateOf("") }
        var classification by remember(uri) { mutableStateOf(AttachmentClassification.SHAREABLE) }
        EditScreen(i18n.text("text.686ed80a7ada"), { picked = null }, { picked = null; vm.addAttachment(context, uri, name, classification) }, confirmLabel = i18n.text("text.84cbef7b19b8")) {
            FormField(name, { name = it }, i18n.text("text.5086900635fe"), hint = i18n.text("text.634787507dfd"))
            EnumPicker(i18n.text("text.57fbd1029ff6"), AttachmentClassification.entries, classification, { it.toDisplayString(i18n = i18n) }, { classification = it })
        }
    }

    floorplanFor?.let { a ->
        var area by remember(a) { mutableStateOf<Area?>(null) }
        if (planArea == null) EditScreen(i18n.text("text.aa39391e8bb5"), { floorplanFor = null }, {
            planArea = area
        }, confirmEnabled = area != null, confirmLabel = i18n.text("text.125d6d4967e5")) {
            OptionPicker(i18n.text("text.ddbccb18e085"), index.areas, area, { it.name }, { area = it }, optionDetail = { ar -> ar.floorplanAttachmentId?.let { i18n.text("text.613f9fe4c7c9") } })
        }
    }

    if (planArea != null && floorplanFor != null) PlanChooser(project, planArea!!, floorplanFor!!.id, vm::attachmentFile, {}, { id, page, pages ->
        vm.edit(i18n.text("text.fcd1cc58f46b")) { ProjectEdits.setAreaFloorplan(it, planArea!!.id, id, page, pages) }; planArea = null; floorplanFor = null
    }, { planArea = null; floorplanFor = null })

    classifying?.let { a ->
        var c by remember(a) { mutableStateOf(a.classification) }
        EditScreen(i18n.text("text.57fbd1029ff6"), { classifying = null }, {
            classifying = null
            vm.edit(i18n.text("text.9efcb947de14")) { p -> p.copy(attachments = p.attachments.map { if (it.id == a.id) it.copy(classification = c) else it }) }
        }) {
            EnumPicker(i18n.text("text.57fbd1029ff6"), AttachmentClassification.entries, c, { it.toDisplayString(i18n = i18n) }, { c = it })
            Text(i18n.text("text.c3176e79018f"), style = MaterialTheme.typography.bodySmall)
        }
    }
    if (mapping) MapDownloadEditor(vm) { mapping = false }
}

@Composable
fun CredentialsScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val confirm = LocalConfirm.current
    val index = remember(project) { ProjectIndex(project) }
    var editing by remember { mutableStateOf<Credential?>(null) }
    var creating by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(setOf<String>()) }

    AppScaffold(
        i18n.text("text.52f7e6721e97"), onBack = { vm.back() }, snackbarHost = snackbar, subtitle = i18n.text("text.ac82e5f35ae8"),
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(i18n.text("text.602206d4ebfc")) }) }
    ) { padding ->
        if (project.credentials.isEmpty()) EmptyState(i18n.text("text.77ff79424562"), Modifier.padding(padding))
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.credentials, key = { it.id }) { c ->
                val shown = c.id in revealed
                ItemCard(
                    title = c.username,
                    badge = c.type.toDisplayString(i18n = i18n),
                    details = listOf(
                        listOfNotNull(c.deviceId?.let { index.deviceName(it, i18n = i18n) }, c.groupName).joinToString(" · "),
                        i18n.text("text.eea78f48b6d5") + if (shown) c.secret else "••••••••"
                    ),
                    onClick = { editing = c },
                    menu = listOf(
                        MenuAction(if (shown) i18n.text("text.db087165afca") else i18n.text("text.882782384909")) { revealed = if (shown) revealed - c.id else revealed + c.id },
                        MenuAction(i18n.text("text.dd41b3275173"), destructive = true) {
                            confirm(ConfirmRequest(i18n.text("text.e10428cea5cd", c.username), i18n.text("text.189761096af2"), i18n.text("text.dd41b3275173")) {
                                vm.moveToTrash("CREDENTIAL", c.id, c.username)
                            })
                        }
                    )
                )
            }
        }
    }

    if (creating || editing != null) {
        val c = editing
        var username by remember(c) { mutableStateOf(c?.username.orEmpty()) }
        var secret by remember(c) { mutableStateOf(c?.secret.orEmpty()) }
        var type by remember(c) { mutableStateOf(c?.type ?: CredentialType.PASSWORD) }
        var group by remember(c) { mutableStateOf(c?.groupName.orEmpty()) }
        var deviceId by remember(c) { mutableStateOf(c?.deviceId) }
        var notes by remember(c) { mutableStateOf(c?.notes.orEmpty()) }
        EditScreen(if (c == null) i18n.text("text.daf354006859") else i18n.text("text.81dda9b20962"), { creating = false; editing = null }, {
            creating = false; editing = null
            val saved = (c ?: Credential(username = username.trim(), secret = secret)).copy(
                username = username.trim(), secret = secret, type = type, groupName = group.trim().ifBlank { null }, deviceId = deviceId, notes = notes.trim().ifBlank { null }
            )
            vm.edit(i18n.text("text.de37f6b34612")) { p ->
                p.copy(credentials = if (c == null) p.credentials + saved else p.credentials.map { if (it.id == saved.id) saved else it })
            }
        }, confirmEnabled = username.isNotBlank() && secret.isNotEmpty()) {
            FormField(username, { username = it }, i18n.text("text.3255e3d5e3b4"))
            val markDirty = LocalMarkDirty.current
            OutlinedTextField(secret, { markDirty(); secret = it }, label = { Text(i18n.text("text.7f9bedb6b654")) }, singleLine = type != CredentialType.SSH_KEY, modifier = Modifier.fillMaxWidth())
            EnumPicker(i18n.text("text.3868d2843d59"), CredentialType.entries, type, { it.toDisplayString(i18n = i18n) }, { type = it })
            DevicePicker(i18n.text("text.cf301d95d32c"), index, deviceId, { deviceId = it }, noneLabel = i18n.text("text.375889cbf5ec"))
            FormField(group, { group = it }, i18n.text("text.b9bb40edbe6e"), hint = i18n.text("text.35a0170556ea"))
            FormField(notes, { notes = it }, i18n.text("text.d8da2c49df39"))
        }
    }
}

@Composable
fun TrashScreen(vm: ProjectViewModel, snackbar: SnackbarHostState) {
    val i18n = LocalMessages.current

    val confirm = LocalConfirm.current
    val trash by vm.trash.collectAsState()
    AppScaffold(
        i18n.text("text.9a3a36d5fa15"), onBack = { vm.back() }, snackbarHost = snackbar,
        actions = {
            if (trash.isNotEmpty()) TextButton(onClick = {
                confirm(ConfirmRequest(i18n.text("text.5d7057a189e9"), i18n.text("text.f02845011433", trash.size), i18n.text("text.2bdc2424de84")) { vm.emptyTrash() })
            }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(i18n.text("text.2bdc2424de84")) }
        }
    ) { padding ->
        if (trash.isEmpty()) EmptyState(i18n.text("text.9744f9e3a70f"), Modifier.padding(padding))
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(trash.sortedByDescending { it.deletedEpochMs }, key = { it.id }) { t ->
                ItemCard(
                    title = t.displayName,
                    badge = com.onlyfield.assetmanager.core.display.EntityTypeLabels.of(t.itemType, i18n = i18n),
                    details = listOf(i18n.text("text.ede23b9e7ffa", formatDateTime(t.deletedEpochMs)), t.affectedReferencesSummary.orEmpty()),
                    menu = listOf(
                        MenuAction(i18n.text("text.cf1718087073")) { vm.restoreFromTrash(t.id) },
                        MenuAction(i18n.text("text.8a62e526a037"), destructive = true) {
                            confirm(ConfirmRequest(i18n.text("text.66e37e4027c9", t.displayName), i18n.text("text.e0be6b7281c4")) { vm.deleteFromTrash(t.id) })
                        }
                    )
                )
            }
        }
    }
}

/** Map download form (F05): the only feature that uses the network, and only when asked. */
@Composable
private fun MapDownloadEditor(vm: ProjectViewModel, onClose: () -> Unit) {
    val i18n = LocalMessages.current

    val source = CartographicSource.OPEN_TOPO_MAP
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var zoom by remember { mutableStateOf("17") }
    var name by remember { mutableStateOf("") }
    val latValue = FieldValidators.parseDecimal(lat)
    val lonValue = FieldValidators.parseDecimal(lon)
    val errors = buildMap {
        FieldValidators.decimal(lat, -85.0, 85.0, i18n = i18n)?.let { put("lat", it) } ?: if (lat.isBlank()) put("lat", i18n.text("text.61c9ef39a147")) else Unit
        FieldValidators.decimal(lon, -180.0, 180.0, i18n = i18n)?.let { put("lon", it) } ?: if (lon.isBlank()) put("lon", i18n.text("text.cd59b62be30a")) else Unit
        FieldValidators.int(zoom, 1, 17, required = true, i18n = i18n)?.let { put("zoom", it) }
    }
    EditScreen(i18n.text("text.9d9c47709125"), onClose, {
        onClose()
        vm.downloadMap(MapSnapshotRequest(source, latValue!!, lonValue!!, FieldValidators.parseInt(zoom)!!), name)
    }, confirmEnabled = errors.isEmpty(), confirmLabel = i18n.text("text.723d32f77a1d")) {
        Text(
            i18n.text("text.25ca49b19a03") +
                i18n.text("text.4d7cad637ae4"),
            style = MaterialTheme.typography.bodySmall
        )
        FormField(lat, { lat = it }, i18n.text("text.259bd9884099"), error = errors["lat"], hint = i18n.text("text.c68adbb89b8d"), kind = FieldKind.DECIMAL)
        FormField(lon, { lon = it }, i18n.text("text.8f00dfa444aa"), error = errors["lon"], hint = i18n.text("text.5e6ab81bedad"), kind = FieldKind.DECIMAL)
        FormField(zoom, { zoom = it }, i18n.text("text.002fdf567c4f"), error = errors["zoom"], kind = FieldKind.NUMBER, hint = i18n.text("text.e2cbfeb6f905"))
        FormField(name, { name = it }, i18n.text("text.5086900635fe"), hint = i18n.text("text.609e94eb3a8b"))
        Text(source.attributionText, style = MaterialTheme.typography.bodySmall)
    }
}
