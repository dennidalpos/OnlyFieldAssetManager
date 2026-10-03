package com.onlyfield.assetmanager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.FieldValidators
import com.onlyfield.assetmanager.core.forms.RackForm
import com.onlyfield.assetmanager.core.forms.RackLayout
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.Screen
import com.onlyfield.assetmanager.ui.components.*

// --- Sites and areas ----------------------------------------------------------------------------

@Composable
fun StructureScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val confirm = LocalConfirm.current
    var editBu by remember { mutableStateOf<BusinessUnit?>(null) }
    var newBu by remember { mutableStateOf(false) }
    var areaTarget by remember { mutableStateOf<Pair<BusinessUnit, Area?>?>(null) }

    AppScaffold(
        "Sedi e aree", onBack = { vm.back() }, snackbarHost = snackbar,
        subtitle = "Dove si trovano apparati, rack e planimetrie",
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { newBu = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Business unit") }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            project.businessUnits.forEach { bu ->
                val areas = bu.areas + bu.sites.flatMap { it.areas }
                item(key = bu.id) {
                    ItemCard(
                        title = bu.name,
                        details = listOf("${bu.devices.size} apparati · ${areas.size} aree"),
                        menu = listOf(
                            MenuAction("Aggiungi area") { areaTarget = bu to null },
                            MenuAction("Rinomina") { editBu = bu },
                            MenuAction("Elimina", destructive = true) {
                                confirm(ConfirmRequest("Eliminare «${bu.name}»?", "La business unit verrà eliminata.") {
                                    vm.edit("Business unit eliminata.") { ProjectEdits.deleteBusinessUnit(it, bu.id) ?: error("contiene ancora apparati o aree") }
                                })
                            }
                        ),
                        onClick = { areaTarget = bu to null }
                    )
                }
                items(areas, key = { it.id }) { area ->
                    ItemCard(
                        title = area.name,
                        details = listOfNotNull(area.floor?.let { "Piano $it" }, area.description),
                        modifier = Modifier.padding(start = 24.dp),
                        onClick = { areaTarget = bu to area },
                        menu = listOf(MenuAction("Elimina", destructive = true) {
                            confirm(ConfirmRequest("Eliminare l'area «${area.name}»?", "L'area verrà eliminata dal progetto.") {
                                vm.edit("Area eliminata.") { ProjectEdits.deleteArea(it, area.id) ?: error("l'area è ancora usata da apparati, rack o planimetrie") }
                            })
                        })
                    )
                }
            }
        }
    }

    if (newBu || editBu != null) {
        val bu = editBu
        var name by remember(bu) { mutableStateOf(bu?.name.orEmpty()) }
        FormDialog(if (bu == null) "Nuova business unit" else "Rinomina", { newBu = false; editBu = null }, {
            newBu = false; editBu = null
            vm.edit("Business unit salvata.") { if (bu == null) ProjectEdits.addBusinessUnit(it, name.trim()) else ProjectEdits.renameBusinessUnit(it, bu.id, name.trim()) }
        }, confirmEnabled = name.isNotBlank()) { FormField(name, { name = it }, "Nome *") }
    }

    areaTarget?.let { (bu, area) ->
        var name by remember(area, bu) { mutableStateOf(area?.name.orEmpty()) }
        var floor by remember(area, bu) { mutableStateOf(area?.floor.orEmpty()) }
        var description by remember(area, bu) { mutableStateOf(area?.description.orEmpty()) }
        FormDialog(if (area == null) "Nuova area in ${bu.name}" else "Modifica area", { areaTarget = null }, {
            areaTarget = null
            val edited = (area ?: Area(name = name.trim())).copy(name = name.trim(), floor = floor.trim().ifBlank { null }, description = description.trim().ifBlank { null })
            vm.edit("Area «${edited.name}» salvata.") { if (area == null) ProjectEdits.addArea(it, bu.id, edited) else ProjectEdits.updateArea(it, edited) }
        }, confirmEnabled = name.isNotBlank()) {
            FormField(name, { name = it }, "Nome *", hint = "Es. Sala server")
            FormField(floor, { floor = it }, "Piano")
            FormField(description, { description = it }, "Descrizione")
        }
    }
}

// --- Racks --------------------------------------------------------------------------------------

@Composable
fun RacksScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val index = remember(project) { ProjectIndex(project) }
    var creating by remember { mutableStateOf(false) }
    AppScaffold(
        "Rack", onBack = { vm.back() }, snackbarHost = snackbar, subtitle = "${project.racks.size} rack",
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Rack") }) }
    ) { padding ->
        if (project.racks.isEmpty()) EmptyState("Nessun rack.", Modifier.padding(padding), "Nuovo rack") { creating = true }
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.racks, key = { it.id }) { r ->
                ItemCard(
                    title = r.name,
                    details = listOf("${index.areaName(r.areaId, "Nessuna area")} · ${RackLayout.usedUnits(r, index.devices)}/${r.heightU}U occupate"),
                    onClick = { vm.navigate(Screen.RackDetail(r.id)) }
                )
            }
        }
    }
    if (creating) RackDialog(vm, index, null) { creating = false }
}

@Composable
fun RackDetailScreen(vm: ProjectViewModel, project: Project, rackId: String, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val confirm = LocalConfirm.current
    val index = remember(project) { ProjectIndex(project) }
    val rack = index.rack(rackId)
    if (rack == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    var editing by remember { mutableStateOf(false) }
    var placing by remember { mutableStateOf(false) }
    var side by remember { mutableStateOf(RackSide.FRONT) }
    val inRack = index.devices.filter { it.rackId == rack.id }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { vm.exportRackPdf(context.contentResolver, it, rack.id) }
    }

    AppScaffold(
        rack.name, onBack = { vm.back() }, snackbarHost = snackbar, busy = vm.busy,
        subtitle = "${rack.heightU}U · ${index.areaName(rack.areaId, "nessuna area")}",
        actions = {
            TextButton(onClick = { editing = true }) { Text("Modifica") }
            OverflowMenu(listOf(
                MenuAction("Scheda PDF…") { pdfLauncher.launch("${safeFileName(rack.name)}.pdf") },
                MenuAction("Sposta nel cestino", destructive = true) {
                    confirm(ConfirmRequest("Spostare «${rack.name}» nel cestino?", "Gli apparati montati verranno segnati come fuori rack.", "Sposta nel cestino") {
                        vm.back()
                        vm.moveToTrash("RACK", rack.id, rack.name)
                    })
                }
            ))
        },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { placing = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Colloca apparato") }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(RackSide.FRONT, RackSide.REAR).forEachIndexed { i, s ->
                    SegmentedButton(selected = side == s, onClick = { side = s }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(s.toDisplayString()) }
                }
            }
            val slots = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) (rack.heightU downTo 1).toList() else (1..rack.heightU).toList()
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(slots) { u ->
                    val dev = inRack.find { d ->
                        val pos = d.positionU ?: return@find false
                        (d.rackSide == RackSide.BOTH || d.rackSide == side) && u >= pos && u < pos + d.heightU
                    }
                    Surface(
                        color = if (dev != null) categoryColor(dev.category) else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        onClick = { dev?.let { vm.navigate(Screen.DeviceDetail(it.id)) } ?: run { placing = true } }
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("U$u", fontSize = 11.sp, modifier = Modifier.width(40.dp), color = if (dev != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            if (dev != null) {
                                val top = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) dev.positionU!! + dev.heightU - 1 else dev.positionU!!
                                if (u == top) Text(dev.technicalName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                val unplaced = inRack.filter { it.positionU == null }
                if (unplaced.isNotEmpty()) {
                    item { SectionTitle("Nel rack senza posizione U") }
                    items(unplaced) { d -> ItemCard(d.technicalName, listOf("Tocca per indicare la posizione"), onClick = { vm.navigate(Screen.DeviceDetail(d.id)) }) }
                }
            }
        }
    }

    if (editing) RackDialog(vm, index, rack) { editing = false }
    if (placing) {
        val candidates = index.devices.filter { it.rackId != rack.id || it.positionU == null }
        var device by remember { mutableStateOf<Device?>(null) }
        var placeSide by remember { mutableStateOf(side) }
        val free = device?.let { RackLayout.freeStartPositions(rack, index.devices, it.heightU, placeSide, it.id) } ?: emptyList()
        var start by remember(device, placeSide) { mutableStateOf(free.firstOrNull()) }
        FormDialog("Colloca in ${rack.name}", { placing = false }, {
            placing = false
            val d = device!!
            val placed = d.copy(rackId = rack.id, positionU = start, rackSide = placeSide,
                mountingType = if (d.mountingType == MountingType.OUT_OF_RACK) MountingType.RACK_MOUNT else d.mountingType)
            vm.edit("«${d.technicalName}» collocato a U$start.") { ProjectEdits.updateDevice(it, placed) }
        }, confirmEnabled = device != null && start != null, confirmLabel = "Colloca") {
            OptionPicker("Apparato *", candidates, device, { it.technicalName }, { device = it }, optionDetail = { "${it.heightU}U" })
            EnumPicker("Lato", RackSide.entries, placeSide, { it.toDisplayString() }, { placeSide = it })
            if (device != null) {
                if (free.isEmpty()) Text("Nessuno spazio libero per ${device!!.heightU}U.", color = MaterialTheme.colorScheme.error)
                else OptionPicker("Posizione (U più bassa)", free, start, { "U$it" }, { start = it }, supportingText = "Solo posizioni libere")
            }
        }
    }
}

@Composable
private fun RackDialog(vm: ProjectViewModel, index: ProjectIndex, rack: Rack?, onClose: () -> Unit) {
    var form by remember(rack) { mutableStateOf(RackForm.from(rack)) }
    val errors = form.errors()
    FormDialog(if (rack == null) "Nuovo rack" else "Modifica rack", onClose, {
        onClose()
        val saved = form.toRack(rack)
        vm.edit("Rack «${saved.name}» salvato.") { if (rack == null) ProjectEdits.addRack(it, saved) else ProjectEdits.updateRack(it, saved) }
    }, confirmEnabled = errors.isEmpty()) {
        FormField(form.name, { form = form.copy(name = it) }, "Nome *", error = errors["name"])
        FormField(form.heightU, { form = form.copy(heightU = it) }, "Altezza (U) *", error = errors["heightU"], kind = FieldKind.NUMBER)
        FormField(form.depthMm, { form = form.copy(depthMm = it) }, "Profondità (mm)", error = errors["depthMm"], kind = FieldKind.NUMBER)
        EnumPicker("Numerazione U", NumberingDirection.entries, form.numberingDirection, { it.toDisplayString() }, { form = form.copy(numberingDirection = it) })
        OptionPicker("Area", index.areas, index.area(form.areaId), { it.name }, { form = form.copy(areaId = it?.id) }, noneLabel = "Nessuna area")
        FormField(form.notes, { form = form.copy(notes = it) }, "Note", singleLine = false)
    }
}

internal fun categoryColor(category: DeviceCategory): Color = when (category) {
    DeviceCategory.NETWORK_SWITCH -> Color(0xFF1565C0)
    DeviceCategory.PATCH_PANEL -> Color(0xFF2E7D32)
    DeviceCategory.UPS_PDU -> Color(0xFFD84315)
    DeviceCategory.SERVER_STORAGE -> Color(0xFF6A1B9A)
    DeviceCategory.CAMERA_NVR -> Color(0xFF00838F)
    DeviceCategory.SHELF -> Color(0xFF616161)
    DeviceCategory.BLANK_PANEL -> Color(0xFF455A64)
    DeviceCategory.CUSTOM -> Color(0xFF5D4037)
}

// --- Models -------------------------------------------------------------------------------------

@Composable
fun ModelsScreen(vm: ProjectViewModel, project: Project, snackbar: SnackbarHostState) {
    val confirm = LocalConfirm.current
    val index = remember(project) { ProjectIndex(project) }
    var editing by remember { mutableStateOf<DeviceModel?>(null) }
    var creating by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf<DeviceModel?>(null) }

    AppScaffold(
        "Modelli di apparato", onBack = { vm.back() }, snackbarHost = snackbar,
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Modello") }) }
    ) { padding ->
        if (project.deviceModels.isEmpty()) EmptyState("Nessun modello. Un modello definisce categoria, altezza e porte da generare.", Modifier.padding(padding))
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.deviceModels, key = { it.id }) { m ->
                ItemCard(
                    title = m.name, badge = m.category.toDisplayString(),
                    details = listOf(listOfNotNull(m.brand, m.modelNumber, "${m.defaultHeightU}U").joinToString(" · "),
                        m.portTemplates.joinToString { "${it.portCount}× ${it.namePrefix}" }),
                    onClick = { editing = m },
                    menu = listOf(
                        MenuAction("Applica a un apparato…") { applying = m },
                        MenuAction("Elimina", destructive = true) {
                            confirm(ConfirmRequest("Eliminare «${m.name}»?", "Gli apparati che lo usano manterranno i propri dati.") {
                                vm.edit("Modello eliminato.") { ProjectEdits.deleteDeviceModel(it, m.id) }
                            })
                        }
                    )
                )
            }
        }
    }

    if (creating || editing != null) {
        val m = editing
        var name by remember(m) { mutableStateOf(m?.name.orEmpty()) }
        var brand by remember(m) { mutableStateOf(m?.brand.orEmpty()) }
        var code by remember(m) { mutableStateOf(m?.modelNumber.orEmpty()) }
        var category by remember(m) { mutableStateOf(m?.category ?: DeviceCategory.NETWORK_SWITCH) }
        var height by remember(m) { mutableStateOf(m?.defaultHeightU?.toString() ?: "1") }
        var templates by remember(m) { mutableStateOf(m?.portTemplates.orEmpty()) }
        var prefix by remember { mutableStateOf("") }
        var count by remember { mutableStateOf("") }
        val heightError = FieldValidators.int(height, 1, 60, required = true)
        val countError = FieldValidators.int(count, 1, 512)
        FormDialog(if (m == null) "Nuovo modello" else "Modifica modello", { creating = false; editing = null }, {
            creating = false; editing = null
            val saved = (m ?: DeviceModel(name = name.trim())).copy(name = name.trim(), brand = brand.trim().ifBlank { null },
                modelNumber = code.trim().ifBlank { null }, category = category, defaultHeightU = height.trim().toInt(), portTemplates = templates)
            vm.edit("Modello «${saved.name}» salvato.") { if (m == null) ProjectEdits.addDeviceModel(it, saved) else ProjectEdits.updateDeviceModel(it, saved) }
        }, confirmEnabled = name.isNotBlank() && heightError == null) {
            FormField(name, { name = it }, "Nome *")
            FormField(brand, { brand = it }, "Marca")
            FormField(code, { code = it }, "Codice modello")
            EnumPicker("Categoria", DeviceCategory.entries, category, { it.toDisplayString() }, { category = it })
            FormField(height, { height = it }, "Altezza (U) *", error = heightError, kind = FieldKind.NUMBER)
            SectionTitle("Porte generate")
            templates.forEachIndexed { i, t ->
                ItemCard("${t.portCount} porte ${t.namePrefix}${t.startNumber}…", emptyList(),
                    menu = listOf(MenuAction("Rimuovi", destructive = true) { templates = templates.filterIndexed { j, _ -> j != i } }))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FormField(prefix, { prefix = it }, "Prefisso", Modifier.weight(1f), hint = "Gi1/0/")
                FormField(count, { count = it }, "Quante", Modifier.weight(0.7f), countError, kind = FieldKind.NUMBER)
            }
            OutlinedButton(enabled = prefix.isNotBlank() && count.isNotBlank() && countError == null, onClick = {
                templates = templates + PortTemplate(namePrefix = prefix.trim(), portCount = count.trim().toInt()); prefix = ""; count = ""
            }) { Text("Aggiungi gruppo di porte") }
        }
    }

    applying?.let { model ->
        var deviceId by remember(model) { mutableStateOf<String?>(null) }
        val device = index.device(deviceId)
        FormDialog("Applica «${model.name}»", { applying = null }, {
            applying = null
            vm.edit("Modello applicato a «${device?.technicalName}».") { ProjectEdits.applyModelToDevice(it, deviceId!!, model.id) }
        }, confirmEnabled = deviceId != null, confirmLabel = "Applica") {
            DevicePicker("Apparato *", index, deviceId, { deviceId = it })
            device?.let {
                Text(if (it.ports.isEmpty()) "Verranno create ${model.portTemplates.sumOf { t -> t.portCount }} porte." else "Le ${it.ports.size} porte esistenti non verranno modificate.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
