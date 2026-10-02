package com.onlyfield.assetmanager

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ReportSelection
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.onlyfield.assetmanager.core.model.AnnotationType
import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.AttachmentTargetType
import com.onlyfield.assetmanager.core.model.AttachmentType
import com.onlyfield.assetmanager.core.model.Credential
import com.onlyfield.assetmanager.core.model.CredentialType
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.core.model.PlacementTargetType
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.ui.ProjectViewModel

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var viewModel: ProjectViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "onlyfield_asset_manager.db",
        )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
        )
        .build()

        repository = ProjectRepository(database)
        viewModel = ProjectViewModel(repository)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AssetManagerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetManagerApp(viewModel: ProjectViewModel) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val pendingEvaluation by viewModel.pendingImportEvaluation.collectAsState()
    val importErrorMessage by viewModel.importErrorMessage.collectAsState()
    val exportStatusMessage by viewModel.exportStatusMessage.collectAsState()
    val passwordPromptForImport by viewModel.passwordPromptForImport.collectAsState()

    var newProjectName by remember { mutableStateOf("") }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }

    val trashItems by viewModel.trashItems.collectAsState()
    val canUndoState by viewModel.canUndoState.collectAsState()

    var showPasswordManageDialog by remember { mutableStateOf(value = false) }
    var showAddCredentialDialog by remember { mutableStateOf(value = false) }
    var showAddRackDialog by remember { mutableStateOf(value = false) }
    var showAddModelDialog by remember { mutableStateOf(value = false) }
    var showAttachmentsDialog by remember { mutableStateOf(value = false) }
    var showFloorplanDialog by remember { mutableStateOf(value = false) }
    var showCartographyDialog by remember { mutableStateOf(value = false) }
    var mapSourceSelected by remember { mutableStateOf(com.onlyfield.assetmanager.cartography.CartographicSource.OPEN_TOPO_MAP) }
    var mapLatInput by remember { mutableStateOf("41.9028") }
    var mapLonInput by remember { mutableStateOf("12.4964") }
    var mapZoomInput by remember { mutableStateOf("15") }
    var showPathsDialog by remember { mutableStateOf(value = false) }
    var showCablingDialog by remember { mutableStateOf(value = false) }
    var showLogicNetworkDialog by remember { mutableStateOf(value = false) }
    var showServicesDialog by remember { mutableStateOf(value = false) }
    var showPowerAndBadgesDialog by remember { mutableStateOf(value = false) }
    var showTrashDialog by remember { mutableStateOf(value = false) }
    var showMergeAndBatchDialog by remember { mutableStateOf(value = false) }

    var showDocumentExportDialog by remember { mutableStateOf(value = false) }
    var docExportFormat by remember { mutableStateOf("COMPOSITE_PDF") } // "COMPOSITE_PDF", "XLSX", "MARKDOWN", "PRINT"
    var docExportAuthorName by remember { mutableStateOf("Tecnico Operativo") }
    var docExportTitleOverride by remember { mutableStateOf("") }
    var docIncludeConfidential by remember { mutableStateOf(false) }
    var docReviewConfirmed by remember { mutableStateOf(true) }

    var docIncRackCards by remember { mutableStateOf(true) }
    var docIncInventoryTable by remember { mutableStateOf(true) }
    var docIncCablingAndPorts by remember { mutableStateOf(true) }
    var docIncLogicalNetwork by remember { mutableStateOf(true) }
    var docIncPowerAndBadges by remember { mutableStateOf(true) }
    var docIncNotesAndAttachments by remember { mutableStateOf(true) }

    var selectedRackIdForPdf by remember { mutableStateOf<String?>(null) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    var exportPasswordInput by remember { mutableStateOf("") }
    var showExportPasswordDialog by remember { mutableStateOf(value = false) }

    val compositePdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        uri?.let {
            val projId = currentProject?.id ?: return@rememberLauncherForActivityResult
            val filterConfig = ExportFilterConfig(
                includeConfidential = docIncludeConfidential,
                reviewRequiredConfirmed = docReviewConfirmed,
                authorName = docExportAuthorName,
                titleOverride = docExportTitleOverride.ifBlank { null }
            )
            val selection = ReportSelection(
                includeRackCards = docIncRackCards,
                includeInventoryTable = docIncInventoryTable,
                includeCablingAndPorts = docIncCablingAndPorts,
                includeLogicalNetwork = docIncLogicalNetwork,
                includePowerAndBadges = docIncPowerAndBadges,
                includeNotesAndAttachments = docIncNotesAndAttachments
            )
            context.contentResolver.openOutputStream(it)?.use { stream ->
                viewModel.exportCompositePdf(projId, filterConfig, selection, stream)
            }
        }
    }

    val xlsxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri ->
        uri?.let {
            val projId = currentProject?.id ?: return@rememberLauncherForActivityResult
            val filterConfig = ExportFilterConfig(
                includeConfidential = docIncludeConfidential,
                reviewRequiredConfirmed = docReviewConfirmed,
                authorName = docExportAuthorName,
                titleOverride = docExportTitleOverride.ifBlank { null }
            )
            context.contentResolver.openOutputStream(it)?.use { stream ->
                viewModel.exportXlsx(projId, filterConfig, stream)
            }
        }
    }

    val mdExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        uri?.let {
            val projId = currentProject?.id ?: return@rememberLauncherForActivityResult
            val filterConfig = ExportFilterConfig(
                includeConfidential = docIncludeConfidential,
                reviewRequiredConfirmed = docReviewConfirmed,
                authorName = docExportAuthorName,
                titleOverride = docExportTitleOverride.ifBlank { null }
            )
            context.contentResolver.openOutputStream(it)?.use { stream ->
                viewModel.exportMarkdown(projId, filterConfig, stream)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let {
            val proj = currentProject
            if ((proj != null) && proj.isPasswordProtected) {
                pendingExportUri = uri
                showExportPasswordDialog = true
            } else {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    currentProject?.id?.let { projId ->
                        viewModel.exportProjectToStream(projId, stream)
                    }
                }
            }
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        uri?.let {
            val projId = currentProject?.id ?: return@rememberLauncherForActivityResult
            val rackId = selectedRackIdForPdf ?: return@rememberLauncherForActivityResult
            context.contentResolver.openOutputStream(it)?.use { stream ->
                viewModel.exportRackPdf(projId, rackId, stream)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            selectedImportUri = it
            context.contentResolver.openInputStream(it)?.use { stream ->
                viewModel.evaluateImportFromStream(stream)
            }
        }
    }

    LaunchedEffect(exportStatusMessage) {
        exportStatusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearExportStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = currentProject?.name ?: "OnlyField Asset Manager") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Save state banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stato Persistenza: $saveState",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (currentProject == null) {
                // Project Selection / Creation Screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progetti Offline Locali",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Text("Importa (.ofam)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Nome nuovo progetto") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newProjectName.isNotBlank()) {
                                viewModel.createNewProject(newProjectName.trim())
                                newProjectName = ""
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Crea")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(projects) { (id, name, _, _, _, isPasswordProtected) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.loadProject(id) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isPasswordProtected) {
                                        Text(
                                            text = "[Protetto]",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Text(text = "ID: $id", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            } else {
                // Active Project View & Search
                val proj = currentProject!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = proj.name, style = MaterialTheme.typography.titleMedium)
                            if (proj.isPasswordProtected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[Protetto]",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { showDocumentExportDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Documenti & Stampa")
                    }
                    Button(
                        onClick = {
                            val fileName = "${proj.name.lowercase().replace(" ", "_")}.ofam"
                            exportLauncher.launch(fileName)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Esporta (.ofam)")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Text("Importa (.ofam)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showPasswordManageDialog = true }) {
                        Text(if (proj.isPasswordProtected) "Password" else "Imposta Password")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = { showAddCredentialDialog = true }) {
                        Text("Credenziale")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = { showAddRackDialog = true }) {
                        Text("+ Rack")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = { showAddModelDialog = true }) {
                        Text("+ Modello")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showAttachmentsDialog = true }) {
                        Text("Allegati (${proj.attachments.size})")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = { showFloorplanDialog = true }) {
                        Text("Planimetria Area")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = { showPathsDialog = true }) {
                        Text("Percorsi (${proj.sharedPathSegments.size})")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = { showCablingDialog = true }) {
                        Text("Cablaggio (${proj.cables.size})")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showLogicNetworkDialog = true }) {
                        Text("Rete Logica (${proj.vlans.size})")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = { showServicesDialog = true }) {
                        Text("Conf/WAN (${proj.deviceConfigurations.size + proj.wanVpnConnections.size})")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = { showPowerAndBadgesDialog = true }) {
                        Text("Alimentazione (${proj.powerFeeds.size})")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        viewModel.refreshTrashItems(proj.id)
                        showTrashDialog = true
                    }) {
                        Text("Cestino (${trashItems.size})")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = { viewModel.undoLastAction(proj.id) },
                        enabled = canUndoState
                    ) {
                        Text("Annulla")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = { showMergeAndBatchDialog = true }) {
                        Text("Sostituzione/Fusione")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        viewModel.search(proj.id, query)
                    },
                    label = { Text("Cerca apparato (Nome, IP, Etichetta, Alias)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "Risultati Ricerca (${searchResults.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn {
                        items(searchResults) { result ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = result.device.technicalName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(text = "Match: ${result.matchedField}", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "BU: ${result.businessUnitName} | Sede: ${result.siteName ?: "Assente"} | Area: ${result.areaName ?: "Assente"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                    if (result.device.ipAddress != null) {
                                        Text(text = "IP: ${result.device.ipAddress}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Rack List & Detail Section
                    Text(text = "Armadi Rack in Progetto (${proj.racks.size})", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (proj.racks.isEmpty()) {
                        Text(text = "Nessun rack censito. Clicca su '+ Rack' per aggiungerne uno.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.height(120.dp)) {
                            items(proj.racks) { rack ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "${rack.name} (${rack.heightU}U)", style = MaterialTheme.typography.titleSmall)
                                            Text(text = "Numerazione: ${rack.numberingDirection}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                        }
                                        Button(onClick = {
                                            selectedRackIdForPdf = rack.id
                                            val pdfName = "rack_${rack.name.lowercase().replace(" ", "_")}.pdf"
                                            pdfExportLauncher.launch(pdfName)
                                        }) {
                                            Text("PDF Rack")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Models Section
                    Text(text = "Modelli Apparati (${proj.deviceModels.size})", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (proj.deviceModels.isEmpty()) {
                        Text(text = "Nessun modello di apparato censito.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.height(80.dp)) {
                            items(proj.deviceModels) { model ->
                                Text(
                                    text = "• ${model.name} (${model.brand ?: "-"} ${model.modelNumber ?: ""}) - ${model.category} [${model.defaultHeightU}U]",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Struttura Inventario", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn {
                        items(proj.businessUnits) { bu ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(text = "• BU: ${bu.name}", style = MaterialTheme.typography.titleSmall)
                                bu.devices.forEach { dev ->
                                    val rackName = proj.racks.find { it.id == dev.rackId }?.name
                                    val posStr = if (dev.positionU != null) " (U${dev.positionU} in $rackName)" else ""
                                    Text(
                                        text = "   - Apparato: ${dev.technicalName} [${dev.category}]$posStr (IP: ${dev.ipAddress ?: "Assente"})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { viewModel.loadProject("") }) {
                    Text("Chiudi Progetto Corrente")
                }
            }
        }
    }

    // Add Rack Dialog
    if (showAddRackDialog && (currentProject != null)) {
        val proj = currentProject!!
        var rackNameInput by remember { mutableStateOf("") }
        var heightUInput by remember { mutableStateOf("42") }

        AlertDialog(
            onDismissRequest = { showAddRackDialog = false },
            title = { Text("Nuovo Armadio Rack") },
            text = {
                Column {
                    OutlinedTextField(
                        value = rackNameInput,
                        onValueChange = { rackNameInput = it },
                        label = { Text("Nome Rack (es. Rack 01)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = heightUInput,
                        onValueChange = { heightUInput = it },
                        label = { Text("Altezza in Unità U (es. 42, 24, 12)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val uVal = heightUInput.toIntOrNull() ?: 42
                    if (rackNameInput.isNotBlank()) {
                        viewModel.addRackToProject(proj.id, rackNameInput.trim(), uVal)
                        showAddRackDialog = false
                    }
                }) {
                    Text("Salva Rack")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddRackDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Add Device Model Dialog
    if (showAddModelDialog && (currentProject != null)) {
        val proj = currentProject!!
        var modelNameInput by remember { mutableStateOf("") }
        var brandInput by remember { mutableStateOf("") }
        var modelNumInput by remember { mutableStateOf("") }
        var heightInput by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddModelDialog = false },
            title = { Text("Nuovo Modello Apparato") },
            text = {
                Column {
                    OutlinedTextField(
                        value = modelNameInput,
                        onValueChange = { modelNameInput = it },
                        label = { Text("Nome Modello") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = brandInput,
                        onValueChange = { brandInput = it },
                        label = { Text("Marca / Produttore (opzionale)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelNumInput,
                        onValueChange = { modelNumInput = it },
                        label = { Text("Codice Modello commerciale (opzionale)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Altezza U predefinita") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (modelNameInput.isNotBlank()) {
                        val hU = heightInput.toIntOrNull() ?: 1
                        viewModel.addDeviceModelToProject(
                            projectId = proj.id,
                            name = modelNameInput.trim(),
                            brand = brandInput.ifBlank { null },
                            modelNumber = modelNumInput.ifBlank { null },
                            category = DeviceCategory.NETWORK_SWITCH,
                            heightU = hU
                        )
                        showAddModelDialog = false
                    }
                }) {
                    Text("Salva Modello")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddModelDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Password Prompt Dialog for Encrypted Import
    if (passwordPromptForImport) {
        var importPasswordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingImport() },
            title = { Text("Pacchetto Protetto da Password") },
            text = {
                Column {
                    Text("Inserire la password del pacchetto .ofam per procedere all'importazione:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importPasswordInput,
                        onValueChange = { importPasswordInput = it },
                        label = { Text("Password pacchetto") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedImportUri?.let { uri ->
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            viewModel.evaluateImportFromStream(stream, password = importPasswordInput)
                        }
                    }
                    importPasswordInput = ""
                }) {
                    Text("Sblocca e Valuta")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    viewModel.clearPendingImport()
                    importPasswordInput = ""
                }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Export Password Prompt Dialog
    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = { Text("Password per Esportazione Pacchetto") },
            text = {
                Column {
                    Text("Il progetto è protetto. Inserire la password del progetto per cifrare il pacchetto .ofam esportato:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportPasswordInput,
                        onValueChange = { exportPasswordInput = it },
                        label = { Text("Password di cifratura") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    pendingExportUri?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            currentProject?.id?.let { projId ->
                                viewModel.exportProjectToStream(projId, stream, password = exportPasswordInput)
                            }
                        }
                    }
                    showExportPasswordDialog = false
                    exportPasswordInput = ""
                }) {
                    Text("Esporta Cifrato")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showExportPasswordDialog = false
                    exportPasswordInput = ""
                }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Password Management Dialog
    if (showPasswordManageDialog && (currentProject != null)) {
        val proj = currentProject!!
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var statusErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPasswordManageDialog = false },
            title = { Text(if (proj.isPasswordProtected) "Gestisci Password Progetto" else "Imposta Password Progetto") },
            text = {
                Column {
                    if (proj.isPasswordProtected) {
                        OutlinedTextField(
                            value = currentPass,
                            onValueChange = { currentPass = it },
                            label = { Text("Password attuale") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Nuova password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    statusErr?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Row {
                    Button(onClick = {
                        if (newPass.isBlank()) {
                            statusErr = "La nuova password non può essere vuota"
                            return@Button
                        }
                        viewModel.setProjectPassword(
                            projectId = proj.id,
                            currentPassword = if (proj.isPasswordProtected) currentPass else null,
                            newPassword = newPass
                        ) { success ->
                            if (success) {
                                showPasswordManageDialog = false
                            } else {
                                statusErr = "Password attuale errata"
                            }
                        }
                    }) {
                        Text("Salva Password")
                    }
                    if (proj.isPasswordProtected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            viewModel.removeProjectPassword(proj.id, currentPass) { success ->
                                if (success) {
                                    showPasswordManageDialog = false
                                } else {
                                    statusErr = "Password attuale errata"
                                }
                            }
                        }) {
                            Text("Rimuovi Password")
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPasswordManageDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Add Credential Dialog
    if (showAddCredentialDialog && (currentProject != null)) {
        val proj = currentProject!!
        var usernameInput by remember { mutableStateOf("") }
        var secretInput by remember { mutableStateOf("") }
        var groupInput by remember { mutableStateOf("") }
        var credType by remember { mutableStateOf(CredentialType.PASSWORD) }

        AlertDialog(
            onDismissRequest = { showAddCredentialDialog = false },
            title = { Text("Aggiungi Nuova Credenziale") },
            text = {
                Column {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username / Utente") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = secretInput,
                        onValueChange = { secretInput = it },
                        label = { Text("Password / Segreto") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = groupInput,
                        onValueChange = { groupInput = it },
                        label = { Text("Gruppo / Ambito (opzionale)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (usernameInput.isNotBlank() && secretInput.isNotBlank()) {
                        val cred = Credential(
                            username = usernameInput.trim(),
                            secret = secretInput.trim(),
                            groupName = groupInput.ifBlank { null },
                            type = credType
                        )
                        viewModel.addCredentialToProject(proj.id, cred)
                        showAddCredentialDialog = false
                    }
                }) {
                    Text("Aggiungi")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddCredentialDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Import Evaluation Confirmation Dialog
    if (pendingEvaluation != null) {
        val comp = pendingEvaluation!!.comparison
        val pkg = pendingEvaluation!!.importResult.pkg
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingImport() },
            title = { Text("Confronto Pacchetto Importato") },
            text = {
                Column {
                    Text("Progetto nel pacchetto: ${pkg?.project?.name ?: "N/D"}")
                    Text("ID: ${pkg?.project?.id ?: "N/D"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Stato Confronto: ${comp?.status ?: "N/D"}", style = MaterialTheme.typography.titleSmall)
                    Text(text = comp?.summary ?: "", style = MaterialTheme.typography.bodyMedium)
                    comp?.warningMessage?.let { warnMsg ->
                        if (warnMsg.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = warnMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmImport() }) {
                    Text("Sostituisci / Importa")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.clearPendingImport() }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Import Error Dialog
    if (importErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingImport() },
            title = { Text("Errore Importazione Pacchetto") },
            text = { Text(importErrorMessage!!) },
            confirmButton = {
                Button(onClick = { viewModel.clearPendingImport() }) {
                    Text("Chiudi")
                }
            }
        )
    }

    // Manage Attachments Dialog
    if (showAttachmentsDialog && (currentProject != null)) {
        val proj = currentProject!!
        var newAttName by remember { mutableStateOf("") }
        var selectedClassification by remember { mutableStateOf(AttachmentClassification.SHAREABLE) }

        AlertDialog(
            onDismissRequest = { showAttachmentsDialog = false },
            title = { Text("Gestione Allegati e Foto (${proj.attachments.size})") },
            text = {
                Column(modifier = Modifier.height(350.dp)) {
                    Text(text = "Nuovo Allegato / Foto:", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = newAttName,
                        onValueChange = { newAttName = it },
                        label = { Text("Nome file / Descrizione foto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Classificazione: ", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            selectedClassification = when (selectedClassification) {
                                AttachmentClassification.SHAREABLE -> AttachmentClassification.CONFIDENTIAL
                                AttachmentClassification.CONFIDENTIAL -> AttachmentClassification.REVIEW_REQUIRED
                                AttachmentClassification.REVIEW_REQUIRED -> AttachmentClassification.SHAREABLE
                            }
                        }) {
                            Text(
                                text = when (selectedClassification) {
                                    AttachmentClassification.SHAREABLE -> "Condivisibile"
                                    AttachmentClassification.CONFIDENTIAL -> "Riservato"
                                    AttachmentClassification.REVIEW_REQUIRED -> "Da riesaminare"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (newAttName.isNotBlank()) {
                                viewModel.addAttachmentToProject(
                                    projectId = proj.id,
                                    name = newAttName.trim(),
                                    fileType = AttachmentType.IMAGE,
                                    mimeType = "image/jpeg",
                                    classification = selectedClassification
                                )
                                newAttName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text("+ Aggiungi Foto / Allegato")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Allegati nel progetto:", style = MaterialTheme.typography.titleSmall)

                    if (proj.attachments.isEmpty()) {
                        Text("Nessun allegato presente.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(proj.attachments) { att ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = att.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Tipo: ${att.fileType} | Classificazione: ${att.classification}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when (att.classification) {
                                                AttachmentClassification.SHAREABLE -> Color(0xFF1B5E20)
                                                AttachmentClassification.CONFIDENTIAL -> Color(0xFFB71C1C)
                                                AttachmentClassification.REVIEW_REQUIRED -> Color(0xFFE65100)
                                            }
                                        )
                                        Row {
                                            OutlinedButton(
                                                onClick = {
                                                    val nextClass = when (att.classification) {
                                                        AttachmentClassification.SHAREABLE -> AttachmentClassification.CONFIDENTIAL
                                                        AttachmentClassification.CONFIDENTIAL -> AttachmentClassification.REVIEW_REQUIRED
                                                        AttachmentClassification.REVIEW_REQUIRED -> AttachmentClassification.SHAREABLE
                                                    }
                                                    viewModel.updateAttachmentClassification(proj.id, att.id, nextClass)
                                                },
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                Text("Cambia Classif.", style = MaterialTheme.typography.bodySmall)
                                            }
                                            OutlinedButton(onClick = { viewModel.deleteAttachment(proj.id, att.id) }) {
                                                Text("Elimina", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAttachmentsDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

    // Floorplan Dialog
    if (showFloorplanDialog && (currentProject != null)) {
        val proj = currentProject!!
        val firstArea = proj.businessUnits.flatMap { it.sites.flatMap { s -> s.areas } + it.areas }.firstOrNull()

        var selectedAreaId by remember { mutableStateOf(firstArea?.id ?: "") }
        var placementLabelInput by remember { mutableStateOf("") }
        var annotationLabelInput by remember { mutableStateOf("") }
        var xRatioInput by remember { mutableStateOf("0.5") }
        var yRatioInput by remember { mutableStateOf("0.5") }

        val context = androidx.compose.ui.platform.LocalContext.current

        AlertDialog(
            onDismissRequest = { showFloorplanDialog = false },
            title = { Text("Planimetria Area & Annotazioni") },
            text = {
                Column(modifier = Modifier.height(400.dp)) {
                    if (firstArea == null) {
                        Text("Nessuna area censita nel progetto. Creare un'area prima di accedere alla planimetria.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        val currentArea = proj.businessUnits.flatMap { it.sites.flatMap { s -> s.areas } + it.areas }.find { it.id == selectedAreaId } ?: firstArea
                        val fpAttachment = proj.attachments.find { it.id == currentArea.floorplanAttachmentId }
                        val areaPlacements = proj.floorplanPlacements.filter { it.areaId == currentArea.id }
                        val areaAnnotations = proj.annotations.filter { it.areaId == currentArea.id }

                        Text(text = "Area selezionata: ${currentArea.name}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Sfondo Planimetria: ${fpAttachment?.name ?: "Sfondo Predefinito Schematico"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                        if (!fpAttachment?.attributionText.isNullOrBlank()) {
                            Text(
                                text = "© Attribuzione Mappa: ${fpAttachment!!.attributionText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1565C0)
                            )
                        }

                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            OutlinedButton(onClick = { showCartographyDialog = true }) {
                                Text("Acquisisci Sfondo Cartografico", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedButton(onClick = {
                                if (proj.attachments.isNotEmpty()) {
                                    val nextAtt = proj.attachments.first()
                                    viewModel.setAreaFloorplanAttachment(proj.id, currentArea.id, nextAtt.id)
                                } else {
                                    viewModel.addAttachmentToProject(
                                        projectId = proj.id,
                                        name = "planimetria_${currentArea.name.lowercase().replace(" ", "_")}.png",
                                        fileType = AttachmentType.IMAGE,
                                        mimeType = "image/png",
                                        classification = AttachmentClassification.SHAREABLE,
                                        targetType = AttachmentTargetType.AREA,
                                        targetId = currentArea.id
                                    )
                                }
                            }) {
                                Text("Sostituisci Sfondo", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Graphic canvas simulation box
                        Card(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "[Mappa Grafica Floorplan: ${currentArea.name}]",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "Apparati/Rack Collocati (${areaPlacements.size}): ${areaPlacements.joinToString { "P(${it.xRatio}, ${it.yRatio})" }}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Annotazioni Grafiche (${areaAnnotations.size}): ${areaAnnotations.joinToString { "${it.type}: '${it.label}'" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add Placement section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = xRatioInput,
                                onValueChange = { xRatioInput = it },
                                label = { Text("X (0..1)") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedTextField(
                                value = yRatioInput,
                                onValueChange = { yRatioInput = it },
                                label = { Text("Y (0..1)") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(onClick = {
                                val xVal = xRatioInput.toFloatOrNull() ?: 0.5f
                                val yVal = yRatioInput.toFloatOrNull() ?: 0.5f
                                val firstDev = proj.businessUnits.flatMap { it.devices }.firstOrNull()
                                if (firstDev != null) {
                                    viewModel.addFloorplanPlacement(
                                        projectId = proj.id,
                                        areaId = currentArea.id,
                                        targetType = PlacementTargetType.DEVICE,
                                        targetId = firstDev.id,
                                        xRatio = xVal,
                                        yRatio = yVal
                                    )
                                }
                            }) {
                                Text("+ Colloca", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add Annotation section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = annotationLabelInput,
                                onValueChange = { annotationLabelInput = it },
                                label = { Text("Testo / Etichetta Annotazione") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(onClick = {
                                if (annotationLabelInput.isNotBlank()) {
                                    viewModel.addAnnotationToArea(
                                        projectId = proj.id,
                                        areaId = currentArea.id,
                                        type = AnnotationType.TEXT,
                                        x1Ratio = 0.2f,
                                        y1Ratio = 0.2f,
                                        label = annotationLabelInput.trim()
                                    )
                                    annotationLabelInput = ""
                                }
                            }) {
                                Text("+ Annotazione", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFloorplanDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

    // Cartography Background Acquisition Dialog
    if (showCartographyDialog && (currentProject != null)) {
        val proj = currentProject!!
        val firstArea = proj.businessUnits.flatMap { it.sites.flatMap { s -> s.areas } + it.areas }.firstOrNull()

        AlertDialog(
            onDismissRequest = { showCartographyDialog = false },
            title = { Text("Acquisisci Sfondo Cartografico Offline") },
            text = {
                Column {
                    Text("Seleziona la fonte cartografica (100% gratuita con attribuzione):", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    com.onlyfield.assetmanager.cartography.CartographicSource.entries.forEach { source ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            RadioButton(
                                selected = (mapSourceSelected == source),
                                onClick = { mapSourceSelected = source }
                            )
                            Text(source.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (mapSourceSelected.isOnline) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            OutlinedTextField(
                                value = mapLatInput,
                                onValueChange = { mapLatInput = it },
                                label = { Text("Latitudine") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedTextField(
                                value = mapLonInput,
                                onValueChange = { mapLonInput = it },
                                label = { Text("Longitudine") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = mapZoomInput,
                            onValueChange = { mapZoomInput = it },
                            label = { Text("Livello Zoom (1..19)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Attribuzione: ${mapSourceSelected.attributionText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Scegliere un file di immagine locale dalla galleria/file picker del dispositivo.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val currentArea = firstArea
                    if (currentArea != null) {
                        if (mapSourceSelected.isOnline) {
                            val lat = mapLatInput.toDoubleOrNull() ?: 41.9028
                            val lon = mapLonInput.toDoubleOrNull() ?: 12.4964
                            val zoom = mapZoomInput.toIntOrNull() ?: 15
                            viewModel.acquireCartographicBackground(
                                projectId = proj.id,
                                areaId = currentArea.id,
                                request = com.onlyfield.assetmanager.cartography.MapSnapshotRequest(
                                    source = mapSourceSelected,
                                    centerLatitude = lat,
                                    centerLongitude = lon,
                                    zoomLevel = zoom
                                ),
                                context = context
                            )
                        } else {
                            viewModel.addAttachmentToProject(
                                projectId = proj.id,
                                name = "mappa_locale_${currentArea.name.lowercase().replace(" ", "_")}.png",
                                fileType = AttachmentType.IMAGE,
                                mimeType = "image/png",
                                classification = AttachmentClassification.SHAREABLE,
                                targetType = AttachmentTargetType.AREA,
                                targetId = currentArea.id
                            )
                        }
                    }
                    showCartographyDialog = false
                }) {
                    Text("Acquisisci Sfondo")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCartographyDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Shared Paths Dialog
    if (showPathsDialog && (currentProject != null)) {
        val proj = currentProject!!
        var pathNameInput by remember { mutableStateOf("") }
        var pathCapacityInput by remember { mutableStateOf("") }
        var pathDescInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPathsDialog = false },
            title = { Text("Percorsi Condivisi (${proj.sharedPathSegments.size})") },
            text = {
                Column {
                    if (proj.sharedPathSegments.isEmpty()) {
                        Text("Nessun segmento di percorso condiviso definito.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.height(140.dp)) {
                            items(proj.sharedPathSegments) { segment ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = segment.name, style = MaterialTheme.typography.titleSmall)
                                            val capStr = if (segment.capacityMaxCables != null) " (Capacità max: ${segment.capacityMaxCables})" else ""
                                            Text(text = "${segment.description ?: "Nessuna descrizione"}$capStr", style = MaterialTheme.typography.bodySmall)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.deleteSharedPathSegment(proj.id, segment.id) }
                                        ) {
                                            Text("Elimina", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Aggiungi Nuovo Percorso Condiviso", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = pathNameInput,
                        onValueChange = { pathNameInput = it },
                        label = { Text("Nome Percorso (es. Cavedio A->B)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = pathCapacityInput,
                        onValueChange = { pathCapacityInput = it },
                        label = { Text("Capacità Max Cavi (opzionale)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = pathDescInput,
                        onValueChange = { pathDescInput = it },
                        label = { Text("Descrizione (opzionale)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pathNameInput.isNotBlank()) {
                        viewModel.addSharedPathSegment(
                            projectId = proj.id,
                            name = pathNameInput.trim(),
                            description = pathDescInput.ifBlank { null },
                            capacityMaxCables = pathCapacityInput.toIntOrNull()
                        )
                        pathNameInput = ""
                        pathCapacityInput = ""
                        pathDescInput = ""
                    } else {
                        showPathsDialog = false
                    }
                }) {
                    Text(if (pathNameInput.isNotBlank()) "+ Salva Percorso" else "Chiudi")
                }
            }
        )
    }

    // Cabling Dialog
    if (showCablingDialog && (currentProject != null)) {
        val proj = currentProject!!
        var cableLabelInput by remember { mutableStateOf("") }
        var cableSpeedInput by remember { mutableStateOf("1 Gbps") }
        var cableCharInput by remember { mutableStateOf("Cat6A") }
        var selectedMedium by remember { mutableStateOf(com.onlyfield.assetmanager.core.model.CableMedium.ETHERNET_COPPER) }
        var selectedOrientation by remember { mutableStateOf(com.onlyfield.assetmanager.core.model.CableOrientation.NONE) }

        var chainSteps by remember { mutableStateOf<List<com.onlyfield.assetmanager.data.repository.ChainStep>>(emptyList()) }

        AlertDialog(
            onDismissRequest = { showCablingDialog = false },
            title = { Text("Cablaggio e Cavi (${proj.cables.size})") },
            text = {
                Column {
                    if (proj.cables.isEmpty()) {
                        Text("Nessun cavo registrato.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.height(120.dp)) {
                            items(proj.cables) { cable ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "Cavo: ${cable.codeOrLabel ?: "Senza Etichetta"} [${cable.medium}]", style = MaterialTheme.typography.titleSmall)
                                            val orientStr = if (cable.orientation != com.onlyfield.assetmanager.core.model.CableOrientation.NONE) " | Dir: ${cable.orientation}" else ""
                                            Text(text = "Caratteristiche: ${cable.nominalCharacteristics ?: "N/D"} | Vel: ${cable.observedSpeed ?: "N/D"}$orientStr", style = MaterialTheme.typography.bodySmall)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.deleteCable(proj.id, cable.id) }
                                        ) {
                                            Text("Elimina", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Aggiungi Nuovo Cavo", style = MaterialTheme.typography.labelMedium)

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = cableLabelInput,
                            onValueChange = { cableLabelInput = it },
                            label = { Text("Etichetta Cavo") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = cableSpeedInput,
                            onValueChange = { cableSpeedInput = it },
                            label = { Text("Velocità") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = cableCharInput,
                            onValueChange = { cableCharInput = it },
                            label = { Text("Caratteristiche (es. Cat6A, OS2)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = {
                                selectedMedium = when (selectedMedium) {
                                    com.onlyfield.assetmanager.core.model.CableMedium.ETHERNET_COPPER -> com.onlyfield.assetmanager.core.model.CableMedium.FIBER_OVERALL
                                    com.onlyfield.assetmanager.core.model.CableMedium.FIBER_OVERALL -> com.onlyfield.assetmanager.core.model.CableMedium.DAC
                                    com.onlyfield.assetmanager.core.model.CableMedium.DAC -> com.onlyfield.assetmanager.core.model.CableMedium.AOC
                                    else -> com.onlyfield.assetmanager.core.model.CableMedium.ETHERNET_COPPER
                                }
                            }
                        ) {
                            Text(selectedMedium.name, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                selectedOrientation = when (selectedOrientation) {
                                    com.onlyfield.assetmanager.core.model.CableOrientation.NONE -> com.onlyfield.assetmanager.core.model.CableOrientation.A_TO_B
                                    com.onlyfield.assetmanager.core.model.CableOrientation.A_TO_B -> com.onlyfield.assetmanager.core.model.CableOrientation.B_TO_A
                                    com.onlyfield.assetmanager.core.model.CableOrientation.B_TO_A -> com.onlyfield.assetmanager.core.model.CableOrientation.BOTH
                                    com.onlyfield.assetmanager.core.model.CableOrientation.BOTH -> com.onlyfield.assetmanager.core.model.CableOrientation.NONE
                                }
                            }
                        ) {
                            Text("Dir: ${selectedOrientation.name}", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val firstPort = proj.businessUnits.flatMap { it.devices }.flatMap { it.ports }.firstOrNull()
                            val secondPort = proj.businessUnits.flatMap { it.devices }.flatMap { it.ports }.getOrNull(1)
                            viewModel.addCable(
                                projectId = proj.id,
                                codeOrLabel = cableLabelInput.ifBlank { "Cavo-${System.currentTimeMillis() % 1000}" },
                                portAId = firstPort?.id,
                                portBId = secondPort?.id,
                                medium = selectedMedium,
                                nominalCharacteristics = cableCharInput.ifBlank { null },
                                observedSpeed = cableSpeedInput.ifBlank { null },
                                orientation = selectedOrientation,
                                sharedPathSegmentIds = proj.sharedPathSegments.map { it.id }
                            )
                        }) {
                            Text("+ Cavo Test", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Traccia Catena di Collegamento", style = MaterialTheme.typography.labelMedium)
                    val allPorts = proj.businessUnits.flatMap { it.devices }.flatMap { it.ports }
                    if (allPorts.isNotEmpty()) {
                        Button(onClick = {
                            val portToTrace = allPorts.first()
                            chainSteps = viewModel.traceCableChain(proj, portToTrace.id)
                        }) {
                            Text("Traccia Catena da '${allPorts.first().name}'", style = MaterialTheme.typography.labelSmall)
                        }
                        if (chainSteps.isNotEmpty()) {
                            Text("Passaggi Catena (${chainSteps.size}):", style = MaterialTheme.typography.labelSmall)
                            LazyColumn(modifier = Modifier.height(80.dp)) {
                                items(chainSteps) { step ->
                                    Text(
                                        text = "${step.stepIndex}. ${step.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (step.isUnknownPassage) Color.Red else Color.Unspecified
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCablingDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

    // Logic Network & VLAN Dialog
    if (showLogicNetworkDialog && (currentProject != null)) {
        val proj = currentProject!!
        var vlanIdInput by remember { mutableStateOf("10") }
        var vlanNameInput by remember { mutableStateOf("VLAN_GUEST") }
        var subnetCidrInput by remember { mutableStateOf("192.168.10.0/24") }
        var l3NameInput by remember { mutableStateOf("vlan10") }

        AlertDialog(
            onDismissRequest = { showLogicNetworkDialog = false },
            title = { Text("Rete Logica & VLAN") },
            text = {
                Column {
                    Text("VLAN Catalogate (${proj.vlans.size}):", style = MaterialTheme.typography.labelMedium)
                    if (proj.vlans.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.height(80.dp)) {
                            items(proj.vlans) { vlan ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "VLAN ${vlan.vlanId}: ${vlan.name} [${vlan.scopeType}]",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.deleteVlan(proj.id, vlan.id) }
                                    ) {
                                        Text("Elimina", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = vlanIdInput,
                            onValueChange = { vlanIdInput = it },
                            label = { Text("ID VLAN") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = vlanNameInput,
                            onValueChange = { vlanNameInput = it },
                            label = { Text("Nome VLAN") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = {
                            val idVal = vlanIdInput.toIntOrNull() ?: 10
                            if (vlanNameInput.isNotBlank()) {
                                viewModel.addVlan(proj.id, idVal, vlanNameInput.trim())
                            }
                        }) {
                            Text("+ VLAN", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Subnet (${proj.subnets.size}):", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = subnetCidrInput,
                            onValueChange = { subnetCidrInput = it },
                            label = { Text("CIDR (es. 192.168.10.0/24)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = {
                            if (subnetCidrInput.isNotBlank()) {
                                viewModel.addSubnet(proj.id, subnetCidrInput.trim())
                            }
                        }) {
                            Text("+ Subnet", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Interfacce Logiche (${proj.logicalInterfaces.size}):", style = MaterialTheme.typography.labelMedium)
                    val allDevs = proj.businessUnits.flatMap { it.devices }
                    if (allDevs.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = l3NameInput,
                                onValueChange = { l3NameInput = it },
                                label = { Text("Nome Int. (es. vlan10)") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(onClick = {
                                viewModel.addLogicalInterface(
                                    projectId = proj.id,
                                    deviceId = allDevs.first().id,
                                    name = l3NameInput.ifBlank { "vlan10" },
                                    ipAddress = "192.168.10.1",
                                    subnetCidr = "192.168.10.0/24",
                                    vlanId = 10
                                )
                            }) {
                                Text("+ Int. Logica", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showLogicNetworkDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

    // Services, Configurations & Custom Fields Dialog
    if (showServicesDialog && (currentProject != null)) {
        val proj = currentProject!!
        var configTitleInput by remember { mutableStateOf("Config_Base_v1") }
        var wanNameInput by remember { mutableStateOf("WAN_TIM_FTTH") }
        var customKeyInput by remember { mutableStateOf("NoteInfrastruttura") }
        var customValueInput by remember { mutableStateOf("Infrastruttura verificata offline") }

        AlertDialog(
            onDismissRequest = { showServicesDialog = false },
            title = { Text("Configurazioni, WAN/VPN & Videosorveglianza") },
            text = {
                Column {
                    Text("Configurazioni Apparati (${proj.deviceConfigurations.size}):", style = MaterialTheme.typography.labelMedium)
                    val allDevs = proj.businessUnits.flatMap { it.devices }
                    if (allDevs.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = configTitleInput,
                                onValueChange = { configTitleInput = it },
                                label = { Text("Titolo Configurazione") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(onClick = {
                                viewModel.addDeviceConfiguration(
                                    projectId = proj.id,
                                    deviceId = allDevs.first().id,
                                    title = configTitleInput.ifBlank { "Config_1" },
                                    configText = "hostname ${allDevs.first().technicalName}\nvlan 10\n name GUEST\n"
                                )
                            }) {
                                Text("+ Conf.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Connessioni WAN/VPN (${proj.wanVpnConnections.size}):", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = wanNameInput,
                            onValueChange = { wanNameInput = it },
                            label = { Text("Nome WAN/VPN") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = {
                            viewModel.addWanVpnConnection(
                                projectId = proj.id,
                                name = wanNameInput.ifBlank { "WAN_Primary" },
                                type = com.onlyfield.assetmanager.core.model.WanVpnType.WAN,
                                bandwidth = "1 Gbps FTTH"
                            )
                        }) {
                            Text("+ WAN/VPN", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Mappe Videosorveglianza (${proj.videoSurveillanceMappings.size}):", style = MaterialTheme.typography.labelMedium)
                    if (allDevs.isNotEmpty()) {
                        Button(onClick = {
                            viewModel.addVideoSurveillanceMapping(
                                projectId = proj.id,
                                cameraDeviceId = allDevs.first().id,
                                channelNumber = 1,
                                resolution = "1080p"
                            )
                        }) {
                            Text("+ Mappa Telecamera Test", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Campi Extra Tipizzati (${proj.customExtraFields.size}):", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customKeyInput,
                            onValueChange = { customKeyInput = it },
                            label = { Text("Chiave") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = customValueInput,
                            onValueChange = { customValueInput = it },
                            label = { Text("Valore") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = {
                            if (customKeyInput.isNotBlank()) {
                                viewModel.addCustomExtraField(
                                    projectId = proj.id,
                                    targetType = "PROJECT",
                                    targetId = proj.id,
                                    fieldKey = customKeyInput.trim(),
                                    fieldValue = customValueInput.trim()
                                )
                            }
                        }) {
                            Text("+ Campo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showServicesDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

    if (showPowerAndBadgesDialog && (currentProject != null)) {
        val proj = currentProject!!
        val allDevs = proj.businessUnits.flatMap { it.devices }
        var feedNameInput by remember { mutableStateOf("") }
        var loadVaInput by remember { mutableStateOf("") }
        var loadWattsInput by remember { mutableStateOf("") }
        var runtimeInput by remember { mutableStateOf("") }
        var badgeLabelInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPowerAndBadgesDialog = false },
            title = { Text("Alimentazione e Badge Documentali") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Alimentazioni Registrate (${proj.powerFeeds.size}):", style = MaterialTheme.typography.titleMedium)
                    proj.powerFeeds.forEach { feed ->
                        val devName = allDevs.firstOrNull { it.id == feed.deviceId }?.technicalName ?: feed.deviceId
                        Text("• ${feed.feedName} [${feed.feedType}] su $devName - ${feed.loadVa ?: 0.0} VA / ${feed.loadWatts ?: 0.0} W", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nuova Alimentazione A/B / UPS / PDU:", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = feedNameInput,
                        onValueChange = { feedNameInput = it },
                        label = { Text("Nome Alimentazione (es. Feed A, PDU-1 Outlet 2)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        OutlinedTextField(
                            value = loadVaInput,
                            onValueChange = { loadVaInput = it },
                            label = { Text("Carico VA") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = loadWattsInput,
                            onValueChange = { loadWattsInput = it },
                            label = { Text("Carico W") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = runtimeInput,
                        onValueChange = { runtimeInput = it },
                        label = { Text("Autonomia rilevata (minuti - con fonte/data)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = {
                        if (feedNameInput.isNotBlank() && allDevs.isNotEmpty()) {
                            val targetDev = allDevs.first()
                            val va = loadVaInput.toDoubleOrNull()
                            val w = loadWattsInput.toDoubleOrNull()
                            val rt = runtimeInput.toIntOrNull()
                            viewModel.addPowerFeedToProject(
                                projectId = proj.id,
                                feed = com.onlyfield.assetmanager.core.model.PowerFeed(
                                    deviceId = targetDev.id,
                                    feedName = feedNameInput.trim(),
                                    feedType = com.onlyfield.assetmanager.core.model.PowerFeedType.PRIMARY_A,
                                    loadVa = va,
                                    loadWatts = w,
                                    observedRuntimeMinutes = rt,
                                    observedSource = if (rt != null) "Rilevamento sul campo" else null,
                                    observedEpochMs = if (rt != null) System.currentTimeMillis() else null
                                )
                            )
                            feedNameInput = ""
                            loadVaInput = ""
                            loadWattsInput = ""
                            runtimeInput = ""
                        }
                    }) {
                        Text("+ Aggiungi Alimentazione", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Badge Liberi & Derivati (${proj.documentBadges.size}):", style = MaterialTheme.typography.titleMedium)
                    proj.documentBadges.forEach { badge ->
                        Text("• [${badge.category}] ${badge.label} ${if (badge.isDerived) "(Derivato)" else "(Libero)"}", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = badgeLabelInput,
                            onValueChange = { badgeLabelInput = it },
                            label = { Text("Nuovo Badge Libero") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = {
                            if (badgeLabelInput.isNotBlank()) {
                                viewModel.addDocumentBadgeToProject(
                                    projectId = proj.id,
                                    badge = com.onlyfield.assetmanager.core.model.DocumentBadge(
                                        targetType = "PROJECT",
                                        targetId = proj.id,
                                        label = badgeLabelInput.trim(),
                                        category = com.onlyfield.assetmanager.core.model.BadgeCategory.FREE_LABEL,
                                        isDerived = false
                                    )
                                )
                                badgeLabelInput = ""
                            }
                        }) {
                            Text("+ Badge", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPowerAndBadgesDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

    if (showTrashDialog && currentProject != null) {
        val proj = currentProject!!
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = { Text("Cestino Locale (${trashItems.size})") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Elementi eliminati conservati nel cestino locale. Esclusi dagli export .ofam.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (trashItems.isEmpty()) {
                        Text("Il cestino è vuoto.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        trashItems.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("[${item.itemType}] ${item.displayName}", style = MaterialTheme.typography.titleSmall)
                                    item.affectedReferencesSummary?.let {
                                        Text("Riferimenti: $it", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(onClick = { viewModel.restoreFromTrash(proj.id, item.id) }) {
                                        Text("Ripristina")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    if (trashItems.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.emptyTrash(proj.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Svuota Cestino")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(onClick = { showTrashDialog = false }) {
                        Text("Chiudi")
                    }
                }
            }
        )
    }

    if (showMergeAndBatchDialog && currentProject != null) {
        val proj = currentProject!!
        val allDevices = proj.businessUnits.flatMap { it.devices }

        var replaceOldDevId by remember { mutableStateOf("") }
        var replaceNewName by remember { mutableStateOf("") }

        var mergeSurvivingId by remember { mutableStateOf("") }
        var mergeDuplicateId by remember { mutableStateOf("") }

        var batchSelectedIds by remember { mutableStateOf(setOf<String>()) }
        var batchNotesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showMergeAndBatchDialog = false },
            title = { Text("Gestione, Sostituzione e Fusione Apparati") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("1. Sostituisci Apparato (Nuovo oggetto pulito)", style = MaterialTheme.typography.titleMedium)
                    Text("Elimina il vecchio lasciando estremità da verificare, crea nuovo apparato senza ereditare dati.", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = replaceNewName,
                        onValueChange = { replaceNewName = it },
                        label = { Text("Nome Nuovo Apparato") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (allDevices.isNotEmpty()) {
                        Text("Seleziona Apparato da Sostituire:", style = MaterialTheme.typography.labelSmall)
                        allDevices.forEach { dev ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = (replaceOldDevId == dev.id),
                                    onClick = { replaceOldDevId = dev.id }
                                )
                                Text(dev.technicalName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(
                            onClick = {
                                if (replaceOldDevId.isNotBlank() && replaceNewName.isNotBlank()) {
                                    viewModel.replaceDevice(
                                        projectId = proj.id,
                                        oldDeviceId = replaceOldDevId,
                                        newName = replaceNewName.trim(),
                                        category = com.onlyfield.assetmanager.core.model.DeviceCategory.CUSTOM
                                    )
                                    replaceNewName = ""
                                    replaceOldDevId = ""
                                }
                            },
                            enabled = replaceOldDevId.isNotBlank() && replaceNewName.isNotBlank()
                        ) {
                            Text("Sostituisci Apparato")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("2. Fusione Guidata Duplicati", style = MaterialTheme.typography.titleMedium)
                    Text("Unisce due apparati duplicati definendo l'ID superstite e i dati da conservare.", style = MaterialTheme.typography.bodySmall)

                    if (allDevices.size >= 2) {
                        Text("Apparato Superstite:", style = MaterialTheme.typography.labelSmall)
                        allDevices.forEach { dev ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = (mergeSurvivingId == dev.id),
                                    onClick = { mergeSurvivingId = dev.id }
                                )
                                Text(dev.technicalName, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Text("Apparato Duplicato (da fondere):", style = MaterialTheme.typography.labelSmall)
                        allDevices.forEach { dev ->
                            if (dev.id != mergeSurvivingId) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = (mergeDuplicateId == dev.id),
                                        onClick = { mergeDuplicateId = dev.id }
                                    )
                                    Text(dev.technicalName, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (mergeSurvivingId.isNotBlank() && mergeDuplicateId.isNotBlank() && mergeSurvivingId != mergeDuplicateId) {
                                    viewModel.mergeDevices(
                                        projectId = proj.id,
                                        survivingDeviceId = mergeSurvivingId,
                                        duplicateDeviceId = mergeDuplicateId,
                                        choices = com.onlyfield.assetmanager.core.model.MergeDataChoices(
                                            mergePorts = true,
                                            mergeCredentials = true,
                                            mergeConfigurations = true
                                        )
                                    )
                                    mergeSurvivingId = ""
                                    mergeDuplicateId = ""
                                }
                            },
                            enabled = mergeSurvivingId.isNotBlank() && mergeDuplicateId.isNotBlank() && mergeSurvivingId != mergeDuplicateId
                        ) {
                            Text("Esegui Fusione")
                        }
                    } else {
                        Text("Richiesti almeno 2 apparati per la fusione.", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("3. Modifica Multipla (Batch Edit)", style = MaterialTheme.typography.titleMedium)
                    Text("Modifica campi ammessi (es. note osservazione) su più apparati contemporaneamente.", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = batchNotesInput,
                        onValueChange = { batchNotesInput = it },
                        label = { Text("Note da applicare a tutti") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    allDevices.forEach { dev ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = batchSelectedIds.contains(dev.id),
                                onCheckedChange = { checked ->
                                    batchSelectedIds = if (checked) batchSelectedIds + dev.id else batchSelectedIds - dev.id
                                }
                            )
                            Text(dev.technicalName, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Button(
                        onClick = {
                            if (batchSelectedIds.isNotEmpty() && batchNotesInput.isNotBlank()) {
                                viewModel.batchEditDevices(
                                    projectId = proj.id,
                                    deviceIds = batchSelectedIds.toList(),
                                    changes = com.onlyfield.assetmanager.core.model.BatchDeviceChanges(
                                        observationNotes = batchNotesInput.trim(),
                                        updateObservationNotes = true
                                    )
                                )
                                batchNotesInput = ""
                                batchSelectedIds = emptySet()
                            }
                        },
                        enabled = batchSelectedIds.isNotEmpty() && batchNotesInput.isNotBlank()
                    ) {
                        Text("Applica Modifica Multipla (${batchSelectedIds.size})")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showMergeAndBatchDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

    if (showDocumentExportDialog && currentProject != null) {
        val proj = currentProject!!
        val unclassifiedCount = proj.attachments.count { it.classification == AttachmentClassification.REVIEW_REQUIRED }

        AlertDialog(
            onDismissRequest = { showDocumentExportDialog = false },
            title = { Text("Esporta Documenti e Stampa Report") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Formato e Destinazione:", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = docExportFormat == "COMPOSITE_PDF", onClick = { docExportFormat = "COMPOSITE_PDF" })
                        Text("Report PDF Composto (.pdf)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = docExportFormat == "XLSX", onClick = { docExportFormat = "XLSX" })
                        Text("Foglio Excel NATIVO (.xlsx)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = docExportFormat == "MARKDOWN", onClick = { docExportFormat = "MARKDOWN" })
                        Text("Documento Markdown (.md)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = docExportFormat == "PRINT", onClick = { docExportFormat = "PRINT" })
                        Text("Stampa Diretta Android (PrintManager)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = docExportAuthorName,
                        onValueChange = { docExportAuthorName = it },
                        label = { Text("Nome Compilatore / Autore") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = docExportTitleOverride,
                        onValueChange = { docExportTitleOverride = it },
                        label = { Text("Titolo personalizzato (opzionale)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (docExportFormat == "COMPOSITE_PDF" || docExportFormat == "PRINT") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sezioni da Includere:", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = docIncRackCards, onCheckedChange = { docIncRackCards = it })
                            Text("Schede e Prospetti Armadi Rack")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = docIncInventoryTable, onCheckedChange = { docIncInventoryTable = it })
                            Text("Tabella Inventario Apparati")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = docIncCablingAndPorts, onCheckedChange = { docIncCablingAndPorts = it })
                            Text("Cablaggio e Collegamenti Fisici")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = docIncLogicalNetwork, onCheckedChange = { docIncLogicalNetwork = it })
                            Text("Rete Logica, Subnet e VLAN")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = docIncPowerAndBadges, onCheckedChange = { docIncPowerAndBadges = it })
                            Text("Alimentazione e Badge Documentali")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = docIncNotesAndAttachments, onCheckedChange = { docIncNotesAndAttachments = it })
                            Text("Note, Osservazioni e Allegati")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = docIncludeConfidential, onCheckedChange = { docIncludeConfidential = it })
                        Text("Includi note/allegati riservati [CONFIDENTIAL]")
                    }

                    if (unclassifiedCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Avviso: $unclassifiedCount elementi classificati 'Da Riesaminare'. Conferma il riesame prima della condivisione.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = docReviewConfirmed, onCheckedChange = { docReviewConfirmed = it })
                                    Text("Confermo il riesame eseguito", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDocumentExportDialog = false
                        val cleanName = proj.name.lowercase().replace(" ", "_")
                        when (docExportFormat) {
                            "COMPOSITE_PDF" -> compositePdfExportLauncher.launch("report_$cleanName.pdf")
                            "XLSX" -> xlsxExportLauncher.launch("inventario_$cleanName.xlsx")
                            "MARKDOWN" -> mdExportLauncher.launch("documentazione_$cleanName.md")
                            "PRINT" -> {
                                val filterConfig = ExportFilterConfig(
                                    includeConfidential = docIncludeConfidential,
                                    reviewRequiredConfirmed = docReviewConfirmed,
                                    authorName = docExportAuthorName,
                                    titleOverride = docExportTitleOverride.ifBlank { null }
                                )
                                val selection = ReportSelection(
                                    includeRackCards = docIncRackCards,
                                    includeInventoryTable = docIncInventoryTable,
                                    includeCablingAndPorts = docIncCablingAndPorts,
                                    includeLogicalNetwork = docIncLogicalNetwork,
                                    includePowerAndBadges = docIncPowerAndBadges,
                                    includeNotesAndAttachments = docIncNotesAndAttachments
                                )
                                viewModel.printProject(context, proj.id, filterConfig, selection)
                            }
                        }
                    },
                    enabled = docReviewConfirmed || unclassifiedCount == 0
                ) {
                    Text(if (docExportFormat == "PRINT") "Stampa" else "Esporta Documento")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDocumentExportDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}
