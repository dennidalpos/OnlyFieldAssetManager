package com.onlyfield.assetmanager.ui.screens

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
        "Allegati", onBack = { vm.back() }, snackbarHost = snackbar, busy = vm.busy,
        actions = { TextButton(onClick = { mapping = true }) { Text("Mappa…") } },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { picker.launch(arrayOf("image/*", "application/pdf", "*/*")) },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Allegato") })
        }
    ) { padding ->
        if (project.attachments.isEmpty()) EmptyState("Nessun allegato. Aggiungi foto, planimetrie o documenti.", Modifier.padding(padding))
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.attachments, key = { it.id }) { a ->
                val usedBy = index.areas.filter { it.floorplanAttachmentId == a.id }.map { it.name }
                ItemCard(
                    title = a.name,
                    badge = a.classification.toDisplayString(),
                    details = listOf(
                        "${a.fileType.toDisplayString()} · ${a.originalFileName}",
                        index.attachmentTarget(a).orEmpty(),
                        usedBy.takeIf { it.isNotEmpty() }?.let { "Planimetria di ${it.joinToString()}" }.orEmpty()
                    ).filter { it.isNotBlank() },
                    menu = listOfNotNull(
                        MenuAction("Usa come planimetria…") { floorplanFor = a }.takeIf { index.areas.isNotEmpty() },
                        MenuAction("Cambia classificazione…") { classifying = a },
                        MenuAction("Elimina", destructive = true) {
                            confirm(ConfirmRequest("Eliminare «${a.name}»?", "L'allegato verrà rimosso dal progetto." + if (usedBy.isNotEmpty()) " Le aree collegate resteranno senza planimetria." else "") {
                                vm.edit("Allegato eliminato.") { ProjectEdits.deleteAttachment(it, a.id) }
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
        EditScreen("Nuovo allegato", { picked = null }, { picked = null; vm.addAttachment(context, uri, name, classification) }, confirmLabel = "Aggiungi") {
            FormField(name, { name = it }, "Nome", hint = "Vuoto = nome del file")
            EnumPicker("Classificazione", AttachmentClassification.entries, classification, { it.toDisplayString() }, { classification = it })
        }
    }

    floorplanFor?.let { a ->
        var area by remember(a) { mutableStateOf<Area?>(null) }
        if (planArea == null) EditScreen("Usa come planimetria", { floorplanFor = null }, {
            planArea = area
        }, confirmEnabled = area != null, confirmLabel = "Imposta") {
            OptionPicker("Area *", index.areas, area, { it.name }, { area = it }, optionDetail = { ar -> ar.floorplanAttachmentId?.let { "ha già una planimetria" } })
        }
    }

    if (planArea != null && floorplanFor != null) PlanChooser(project, planArea!!, floorplanFor!!.id, vm::attachmentFile, {}, { id, page, pages ->
        vm.edit("Planimetria impostata.") { ProjectEdits.setAreaFloorplan(it, planArea!!.id, id, page, pages) }; planArea = null; floorplanFor = null
    }, { planArea = null; floorplanFor = null })

    classifying?.let { a ->
        var c by remember(a) { mutableStateOf(a.classification) }
        EditScreen("Classificazione", { classifying = null }, {
            classifying = null
            vm.edit("Classificazione aggiornata.") { p -> p.copy(attachments = p.attachments.map { if (it.id == a.id) it.copy(classification = c) else it }) }
        }) {
            EnumPicker("Classificazione", AttachmentClassification.entries, c, { it.toDisplayString() }, { c = it })
            Text("Gli allegati riservati sono esclusi dai documenti, salvo scelta esplicita in fase di esportazione.", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (mapping) MapDownloadEditor(vm) { mapping = false }
}

@Composable
fun CredentialsScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val confirm = LocalConfirm.current
    val index = remember(project) { ProjectIndex(project) }
    var editing by remember { mutableStateOf<Credential?>(null) }
    var creating by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(setOf<String>()) }

    AppScaffold(
        "Credenziali", onBack = { vm.back() }, snackbarHost = snackbar, subtitle = "Mai incluse nei documenti esportati",
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Credenziale") }) }
    ) { padding ->
        if (project.credentials.isEmpty()) EmptyState("Nessuna credenziale salvata.", Modifier.padding(padding))
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.credentials, key = { it.id }) { c ->
                val shown = c.id in revealed
                ItemCard(
                    title = c.username,
                    badge = c.type.toDisplayString(),
                    details = listOf(
                        listOfNotNull(c.deviceId?.let { index.deviceName(it) }, c.groupName).joinToString(" · "),
                        "Segreto: " + if (shown) c.secret else "••••••••"
                    ),
                    onClick = { editing = c },
                    menu = listOf(
                        MenuAction(if (shown) "Nascondi segreto" else "Mostra segreto") { revealed = if (shown) revealed - c.id else revealed + c.id },
                        MenuAction("Sposta nel cestino", destructive = true) {
                            confirm(ConfirmRequest("Spostare la credenziale «${c.username}» nel cestino?", "Potrai ripristinarla dal Cestino.", "Sposta nel cestino") {
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
        EditScreen(if (c == null) "Nuova credenziale" else "Modifica credenziale", { creating = false; editing = null }, {
            creating = false; editing = null
            val saved = (c ?: Credential(username = username.trim(), secret = secret)).copy(
                username = username.trim(), secret = secret, type = type, groupName = group.trim().ifBlank { null }, deviceId = deviceId, notes = notes.trim().ifBlank { null }
            )
            vm.edit("Credenziale salvata.") { p ->
                p.copy(credentials = if (c == null) p.credentials + saved else p.credentials.map { if (it.id == saved.id) saved else it })
            }
        }, confirmEnabled = username.isNotBlank() && secret.isNotEmpty()) {
            FormField(username, { username = it }, "Utente *")
            val markDirty = LocalMarkDirty.current
            OutlinedTextField(secret, { markDirty(); secret = it }, label = { Text("Password / segreto *") }, singleLine = type != CredentialType.SSH_KEY, modifier = Modifier.fillMaxWidth())
            EnumPicker("Tipo", CredentialType.entries, type, { it.toDisplayString() }, { type = it })
            DevicePicker("Apparato", index, deviceId, { deviceId = it }, noneLabel = "Nessuno (credenziale di gruppo)")
            FormField(group, { group = it }, "Gruppo", hint = "Es. Switch accesso")
            FormField(notes, { notes = it }, "Note")
        }
    }
}

@Composable
fun TrashScreen(vm: ProjectViewModel, snackbar: SnackbarHostState) {
    val confirm = LocalConfirm.current
    val trash by vm.trash.collectAsState()
    AppScaffold(
        "Cestino", onBack = { vm.back() }, snackbarHost = snackbar,
        actions = {
            if (trash.isNotEmpty()) TextButton(onClick = {
                confirm(ConfirmRequest("Svuotare il cestino?", "${trash.size} elementi verranno eliminati definitivamente.", "Svuota") { vm.emptyTrash() })
            }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Svuota") }
        }
    ) { padding ->
        if (trash.isEmpty()) EmptyState("Il cestino è vuoto.", Modifier.padding(padding))
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(trash.sortedByDescending { it.deletedEpochMs }, key = { it.id }) { t ->
                ItemCard(
                    title = t.displayName,
                    badge = com.onlyfield.assetmanager.core.display.EntityTypeLabels.of(t.itemType),
                    details = listOf("Eliminato il ${formatDateTime(t.deletedEpochMs)}", t.affectedReferencesSummary.orEmpty()),
                    menu = listOf(
                        MenuAction("Ripristina") { vm.restoreFromTrash(t.id) },
                        MenuAction("Elimina definitivamente", destructive = true) {
                            confirm(ConfirmRequest("Eliminare definitivamente «${t.displayName}»?", "L'elemento non potrà più essere ripristinato.") { vm.deleteFromTrash(t.id) })
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
    val source = CartographicSource.OPEN_TOPO_MAP
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var zoom by remember { mutableStateOf("17") }
    var name by remember { mutableStateOf("") }
    val latValue = FieldValidators.parseDecimal(lat)
    val lonValue = FieldValidators.parseDecimal(lon)
    val errors = buildMap {
        FieldValidators.decimal(lat, -85.0, 85.0)?.let { put("lat", it) } ?: if (lat.isBlank()) put("lat", "Latitudine obbligatoria") else Unit
        FieldValidators.decimal(lon, -180.0, 180.0)?.let { put("lon", it) } ?: if (lon.isBlank()) put("lon", "Longitudine obbligatoria") else Unit
        FieldValidators.int(zoom, 1, 17, required = true)?.let { put("zoom", it) }
    }
    EditScreen("Mappa dal web", onClose, {
        onClose()
        vm.downloadMap(MapSnapshotRequest(source, latValue!!, lonValue!!, FieldValidators.parseInt(zoom)!!), name)
    }, confirmEnabled = errors.isEmpty(), confirmLabel = "Scarica") {
        Text(
            "Scarica una mappa (3 × 3 tessere) attorno al punto e la salva tra gli allegati con l'attribuzione. " +
                "Serve la connessione solo per questo download: il resto dell'app funziona offline.",
            style = MaterialTheme.typography.bodySmall
        )
        FormField(lat, { lat = it }, "Latitudine *", error = errors["lat"], hint = "Es. 45,4642", kind = FieldKind.DECIMAL)
        FormField(lon, { lon = it }, "Longitudine *", error = errors["lon"], hint = "Es. 9,1900", kind = FieldKind.DECIMAL)
        FormField(zoom, { zoom = it }, "Zoom (1-17)", error = errors["zoom"], kind = FieldKind.NUMBER, hint = "17 = isolato, 15 = quartiere")
        FormField(name, { name = it }, "Nome", hint = "Vuoto = coordinate")
        Text(source.attributionText, style = MaterialTheme.typography.bodySmall)
    }
}
