package com.onlyfield.assetmanager.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.components.*

@Composable
fun AttachmentsScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val confirm = LocalConfirm.current
    val index = remember(project) { ProjectIndex(project) }
    var picked by remember { mutableStateOf<Uri?>(null) }
    var floorplanFor by remember { mutableStateOf<Attachment?>(null) }
    var classifying by remember { mutableStateOf<Attachment?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> picked = uri }

    AppScaffold(
        "Allegati", onBack = { vm.back() }, snackbarHost = snackbar, busy = vm.busy,
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
                    details = listOf("${a.fileType.toDisplayString()} · ${a.originalFileName}", usedBy.takeIf { it.isNotEmpty() }?.let { "Planimetria di ${it.joinToString()}" }.orEmpty()),
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
        FormDialog("Nuovo allegato", { picked = null }, { picked = null; vm.addAttachment(context, uri, name, classification) }, confirmLabel = "Aggiungi") {
            FormField(name, { name = it }, "Nome", hint = "Vuoto = nome del file")
            EnumPicker("Classificazione", AttachmentClassification.entries, classification, { it.toDisplayString() }, { classification = it })
        }
    }

    floorplanFor?.let { a ->
        var area by remember(a) { mutableStateOf<Area?>(null) }
        FormDialog("Usa come planimetria", { floorplanFor = null }, {
            floorplanFor = null
            vm.edit("Planimetria di «${area!!.name}» impostata.") { ProjectEdits.setAreaFloorplan(it, area!!.id, a.id) }
        }, confirmEnabled = area != null, confirmLabel = "Imposta") {
            OptionPicker("Area *", index.areas, area, { it.name }, { area = it }, optionDetail = { ar -> ar.floorplanAttachmentId?.let { "ha già una planimetria" } })
        }
    }

    classifying?.let { a ->
        var c by remember(a) { mutableStateOf(a.classification) }
        FormDialog("Classificazione", { classifying = null }, {
            classifying = null
            vm.edit("Classificazione aggiornata.") { p -> p.copy(attachments = p.attachments.map { if (it.id == a.id) it.copy(classification = c) else it }) }
        }) {
            EnumPicker("Classificazione", AttachmentClassification.entries, c, { it.toDisplayString() }, { c = it })
            Text("Gli allegati riservati sono esclusi dai documenti, salvo scelta esplicita in fase di esportazione.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun FloorplanScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val confirm = LocalConfirm.current
    val index = remember(project) { ProjectIndex(project) }
    var areaId by remember { mutableStateOf(index.areas.firstOrNull()?.id) }
    val area = index.area(areaId)
    var targetType by remember { mutableStateOf(PlacementTargetType.DEVICE) }
    var targetId by remember { mutableStateOf<String?>(null) }
    var movingId by remember { mutableStateOf<String?>(null) }

    val placements = project.floorplanPlacements.filter { it.areaId == area?.id }
    val background = remember(area?.floorplanAttachmentId, project.attachments) {
        project.attachments.find { it.id == area?.floorplanAttachmentId && it.fileType == AttachmentType.IMAGE }?.let { att ->
            vm.attachmentFile(att)?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        }
    }
    fun nameOf(p: FloorplanPlacement) = p.labelOverride ?: if (p.targetType == PlacementTargetType.RACK) index.rackName(p.targetId) else index.deviceName(p.targetId)

    AppScaffold("Planimetrie", onBack = { vm.back() }, snackbarHost = snackbar) { padding ->
        if (index.areas.isEmpty()) {
            EmptyState("Crea prima un'area in «Sedi e aree».", Modifier.padding(padding))
            return@AppScaffold
        }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OptionPicker("Area", index.areas, area, { it.name }, { areaId = it?.id; movingId = null }) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    PlacementTargetType.entries.forEachIndexed { i, t ->
                        SegmentedButton(selected = targetType == t, onClick = { targetType = t; targetId = null }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(t.toDisplayString()) }
                    }
                }
            }
            item {
                if (targetType == PlacementTargetType.RACK) OptionPicker("Rack da posizionare", project.racks, index.rack(targetId), { it.name }, { targetId = it?.id; movingId = null })
                else DevicePicker("Apparato da posizionare", index, targetId, { targetId = it; movingId = null })
            }
            item {
                val hint = when {
                    movingId != null -> "Tocca la planimetria nel nuovo punto."
                    targetId != null -> "Tocca la planimetria nel punto in cui si trova l'elemento."
                    else -> "Scegli un elemento, poi tocca la planimetria per posizionarlo."
                }
                Text(hint, fontWeight = if (targetId != null || movingId != null) FontWeight.SemiBold else FontWeight.Normal, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(Color(0xFF263238))) {
                    background?.let { Image(it, contentDescription = "Planimetria", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                    Canvas(Modifier.fillMaxSize().pointerInput(area?.id, targetId, targetType, movingId, project) {
                        detectTapGestures { o ->
                            val x = (o.x / size.width).coerceIn(0f, 1f)
                            val y = (o.y / size.height).coerceIn(0f, 1f)
                            val a = area ?: return@detectTapGestures
                            val moving = placements.find { it.id == movingId }
                            if (moving != null) {
                                movingId = null
                                vm.edit("Posizione aggiornata.") { ProjectEdits.updateFloorplanPlacement(it, moving.copy(xRatio = x, yRatio = y)) }
                            } else targetId?.let { id ->
                                targetId = null
                                vm.edit("Elemento posizionato.") { ProjectEdits.addFloorplanPlacement(it, FloorplanPlacement(areaId = a.id, targetType = targetType, targetId = id, xRatio = x, yRatio = y)) }
                            }
                        }
                    }) {
                        if (background == null) for (i in 0..8) {
                            drawLine(Color.White.copy(alpha = 0.08f), Offset(i * size.width / 8, 0f), Offset(i * size.width / 8, size.height))
                            drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, i * size.height / 8), Offset(size.width, i * size.height / 8))
                        }
                        placements.forEach { p ->
                            val c = Offset(p.xRatio * size.width, p.yRatio * size.height)
                            drawCircle(if (p.targetType == PlacementTargetType.RACK) Color(0xFF42A5F5) else Color(0xFF66BB6A), 18f, c)
                            drawCircle(if (p.id == movingId) Color.Yellow else Color.White, 20f, c, style = Stroke(3f))
                        }
                    }
                }
            }
            item { SectionTitle("Elementi su ${area?.name ?: "—"} (${placements.size})") }
            items(placements, key = { it.id }) { p ->
                ItemCard(nameOf(p), listOf(p.targetType.toDisplayString()), menu = listOf(
                    MenuAction("Sposta") { movingId = p.id; targetId = null },
                    MenuAction("Rimuovi dalla planimetria", destructive = true) {
                        confirm(ConfirmRequest("Rimuovere «${nameOf(p)}» dalla planimetria?", "L'elemento resta nel progetto.", "Rimuovi") {
                            vm.edit("Elemento rimosso dalla planimetria.") { ProjectEdits.deleteFloorplanPlacement(it, p.id) }
                        })
                    }
                ))
            }
        }
    }
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
        FormDialog(if (c == null) "Nuova credenziale" else "Modifica credenziale", { creating = false; editing = null }, {
            creating = false; editing = null
            val saved = (c ?: Credential(username = username.trim(), secret = secret)).copy(
                username = username.trim(), secret = secret, type = type, groupName = group.trim().ifBlank { null }, deviceId = deviceId, notes = notes.trim().ifBlank { null }
            )
            vm.edit("Credenziale salvata.") { p ->
                p.copy(credentials = if (c == null) p.credentials + saved else p.credentials.map { if (it.id == saved.id) saved else it })
            }
        }, confirmEnabled = username.isNotBlank() && secret.isNotEmpty()) {
            FormField(username, { username = it }, "Utente *")
            OutlinedTextField(secret, { secret = it }, label = { Text("Password / segreto *") }, singleLine = type != CredentialType.SSH_KEY, modifier = Modifier.fillMaxWidth())
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
