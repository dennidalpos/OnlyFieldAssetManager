package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.*
import com.onlyfield.assetmanager.exchange.*
import com.onlyfield.assetmanager.pc.ui.*
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp() {
    val storageManager = remember { DesktopStorageManager() }

    var dataDirStatus by remember { mutableStateOf(storageManager.checkDataDirectoryStatus()) }
    var storedProjects by remember { mutableStateOf(storageManager.listStoredProjects()) }

    var currentProject by remember { mutableStateOf<Project?>(null) }
    var currentProjectPassword by remember { mutableStateOf<String?>(null) }
    var currentManifest by remember { mutableStateOf<PackageManifest?>(null) }
    var trashItems by remember { mutableStateOf<List<TrashItem>>(emptyList()) }

    var activeTab by remember { mutableStateOf(0) }

    var statusMessage by remember { mutableStateOf("Pronto. Storage desktop attivo in: ${dataDirStatus.path.absolutePath}") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var validationIssues by remember { mutableStateOf<List<ValidationIssue>>(emptyList()) }

    // Dialog States
    var showPasswordImportDialog by remember { mutableStateOf(false) }
    var pendingImportFile by remember { mutableStateOf<File?>(null) }
    var inputPasswordForImport by remember { mutableStateOf("") }

    var showPasswordManageDialog by remember { mutableStateOf(false) }
    var currentPassInput by remember { mutableStateOf("") }
    var newPassInput by remember { mutableStateOf("") }
    var confirmPassInput by remember { mutableStateOf("") }

    var showComparisonDialog by remember { mutableStateOf(false) }
    var pendingComparison by remember { mutableStateOf<ProjectComparison?>(null) }
    var pendingImportPackage by remember { mutableStateOf<ProjectPackage?>(null) }

    var showDocumentExportDialog by remember { mutableStateOf(false) }
    var docFormat by remember { mutableStateOf("COMPOSITE_PDF") }
    var docAuthor by remember { mutableStateOf("Tecnico Operativo Desktop") }
    var docTitleOverride by remember { mutableStateOf("") }
    var docIncludeConfidential by remember { mutableStateOf(false) }
    var docIncRackCards by remember { mutableStateOf(true) }
    var docIncInventoryTable by remember { mutableStateOf(true) }
    var docIncCablingAndPorts by remember { mutableStateOf(true) }
    var docIncLogicalNetwork by remember { mutableStateOf(true) }

    fun refreshStoredList() {
        storedProjects = storageManager.listStoredProjects()
        dataDirStatus = storageManager.checkDataDirectoryStatus()
    }

    fun handleProjectUpdated(updatedProject: Project, msg: String) {
        currentProject = updatedProject
        validationIssues = ModelValidator.validateProject(updatedProject).issues
        statusMessage = msg
        try {
            storageManager.saveProjectLocally(updatedProject, currentProjectPassword)
            refreshStoredList()
        } catch (e: Exception) {
            errorMessage = "Errore salvataggio locale transazionale: ${e.message}"
        }
    }

    fun handleImportFile(file: File, password: String? = null) {
        try {
            val result = storageManager.importPackageFromFile(file, password)
            val issues = result.validationResult.issues

            val passReqIssue = issues.find { it.code == "PASSWORD_REQUIRED" || it.code == "INVALID_PACKAGE_PASSWORD" }
            if (passReqIssue != null && password == null) {
                pendingImportFile = file
                showPasswordImportDialog = true
                return
            }

            val pkg = result.pkg
            if (pkg != null) {
                val comparison = ProjectComparisonEvaluator.evaluate(
                    currentProject = currentProject,
                    currentManifest = currentManifest,
                    incomingPackage = pkg
                )

                if (currentProject != null && comparison.status != ComparisonStatus.IDENTICAL) {
                    pendingComparison = comparison
                    pendingImportPackage = pkg
                    showComparisonDialog = true
                } else {
                    currentProject = pkg.project
                    currentManifest = pkg.manifest
                    currentProjectPassword = password
                    validationIssues = issues
                    errorMessage = null
                    statusMessage = "Importato con successo '${pkg.project.name}' (${pkg.project.businessUnits.sumOf { it.devices.size }} apparati). Contratto v1.7 verificato."
                    try {
                        storageManager.saveProjectLocally(pkg.project, password = password)
                        refreshStoredList()
                    } catch (e: Exception) {
                        errorMessage = "Avviso salvataggio locale: ${e.message}"
                    }
                }
            } else {
                errorMessage = "Impossibile importare '${file.name}': " + issues.joinToString("; ") { it.message }
            }
        } catch (e: Exception) {
            errorMessage = "Errore durante l'importazione di ${file.name}: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OnlyField Asset Manager — Desktop Windows 11 (W03)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Error Message Banner if any
            errorMessage?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ $err",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { errorMessage = null }) {
                            Text("Chiudi", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Top Quick Info Status Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )

                    currentProject?.let { proj ->
                        Text(
                            text = "Progetto: ${proj.name} | Apparati: ${proj.businessUnits.sumOf { it.devices.size }} | Rack: ${proj.racks.size} | Cavi: ${proj.cables.size} | VLAN: ${proj.vlans.size}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Main Primary Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                edgePadding = 0.dp
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Text("📦 Inventario Apparati", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Text("🗄️ Rack & Layout Elevation U", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                    Text("📐 Catalogo Modelli", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                    Text("🖼️ Planimetrie & Media", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == 4, onClick = { activeTab = 4 }) {
                    Text("🔌 Cablaggio & Percorsi", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == 5, onClick = { activeTab = 5 }) {
                    Text("🌐 Rete Logica & SVI", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == 6, onClick = { activeTab = 6 }) {
                    Text("⚡ Alimentazione & Badge", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == 7, onClick = { activeTab = 7 }) {
                    Text("🗑️ Cestino (${trashItems.size})", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == 8, onClick = { activeTab = 8 }) {
                    Text("⚙️ Storage & Progetto", modifier = Modifier.padding(12.dp))
                }
            }

            // Main Tab View Content
            val proj = currentProject
            if (proj == null && activeTab != 8) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Nessun progetto aperto al momento.", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val fixture = storageManager.loadAndroidFixtureFile()
                                    currentProject = fixture
                                    validationIssues = ModelValidator.validateProject(fixture).issues
                                    statusMessage = "Caricata fixture Android '${fixture.name}'."
                                    storageManager.saveProjectLocally(fixture)
                                    refreshStoredList()
                                }) {
                                    Text("Carica Fixture Android")
                                }

                                Button(onClick = {
                                    val file = DesktopStorageHelper.pickOpenFile()
                                    if (file != null) handleImportFile(file)
                                }) {
                                    Text("Apri / Importa .ofam")
                                }
                            }
                        }
                    }
                }
            } else {
                when (activeTab) {
                    0 -> proj?.let { p ->
                        InventorySection(
                            project = p,
                            onProjectUpdated = ::handleProjectUpdated,
                            onTrashItemCreated = { trashItems = trashItems + it }
                        )
                    }
                    1 -> proj?.let { p ->
                        RackSection(
                            project = p,
                            onProjectUpdated = ::handleProjectUpdated,
                            onTrashItemCreated = { trashItems = trashItems + it }
                        )
                    }
                    2 -> proj?.let { p ->
                        DeviceModelsSection(
                            project = p,
                            onProjectUpdated = ::handleProjectUpdated
                        )
                    }
                    3 -> proj?.let { p ->
                        FloorplanMediaSection(
                            project = p,
                            onProjectUpdated = ::handleProjectUpdated
                        )
                    }
                    4 -> proj?.let { p ->
                        CablingSection(
                            project = p,
                            onProjectUpdated = ::handleProjectUpdated
                        )
                    }
                    5 -> proj?.let { p ->
                        NetworkLogicalSection(
                            project = p,
                            onProjectUpdated = ::handleProjectUpdated
                        )
                    }
                    6 -> proj?.let { p ->
                        PowerBadgeSection(
                            project = p,
                            onProjectUpdated = ::handleProjectUpdated
                        )
                    }
                    7 -> proj?.let { p ->
                        TrashBatchSection(
                            project = p,
                            trashItems = trashItems,
                            onProjectUpdated = ::handleProjectUpdated,
                            onTrashUpdated = { trashItems = it }
                        )
                    }
                    8 -> {
                        // Project & Storage Management Section (from W01)
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Primary Control Buttons Bar
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(onClick = {
                                    try {
                                        val p = storageManager.loadAndroidFixtureFile()
                                        currentProject?.let { storageManager.releaseProjectLock(it.id) }
                                        currentProject = p
                                        currentManifest = null
                                        currentProjectPassword = null
                                        validationIssues = ModelValidator.validateProject(p).issues
                                        errorMessage = null
                                        statusMessage = "Caricata fixture Android '${p.name}'."
                                        storageManager.saveProjectLocally(p)
                                        refreshStoredList()
                                    } catch (e: Exception) {
                                        errorMessage = "Errore caricamento fixture: ${e.message}"
                                    }
                                }) {
                                    Text("Carica Fixture Android")
                                }

                                Button(onClick = {
                                    val projId = UUID.randomUUID().toString()
                                    val buId = UUID.randomUUID().toString()
                                    val areaId = UUID.randomUUID().toString()
                                    val devId = UUID.randomUUID().toString()
                                    val now = System.currentTimeMillis()

                                    val newP = Project(
                                        id = projId,
                                        name = "Progetto Windows Desktop",
                                        description = "Progetto creato da Desktop Windows W02",
                                        createdEpochMs = now,
                                        updatedEpochMs = now,
                                        businessUnits = listOf(
                                            BusinessUnit(
                                                id = buId,
                                                name = "Sede Principale",
                                                areas = listOf(Area(id = areaId, name = "Sala Server PC")),
                                                devices = listOf(
                                                    Device(
                                                        id = devId,
                                                        areaId = areaId,
                                                        technicalName = "SW-PC-CORE01",
                                                        category = DeviceCategory.NETWORK_SWITCH
                                                    )
                                                )
                                            )
                                        )
                                    )

                                    currentProject?.let { storageManager.releaseProjectLock(it.id) }
                                    currentProject = newP
                                    currentManifest = null
                                    currentProjectPassword = null
                                    validationIssues = ModelValidator.validateProject(newP).issues
                                    errorMessage = null
                                    statusMessage = "Creato nuovo progetto '${newP.name}'."
                                    storageManager.saveProjectLocally(newP)
                                    refreshStoredList()
                                }) {
                                    Text("Nuovo Progetto")
                                }

                                Button(onClick = {
                                    val file = DesktopStorageHelper.pickOpenFile()
                                    if (file != null) handleImportFile(file)
                                }) {
                                    Text("Apri/Importa .ofam")
                                }

                                Button(
                                    enabled = currentProject != null,
                                    onClick = {
                                        val currentP = currentProject ?: return@Button
                                        val file = DesktopStorageHelper.pickSaveFile(
                                            title = "Esporta pacchetto .ofam",
                                            defaultFileName = "${currentP.name.replace(" ", "_")}.ofam"
                                        )
                                        if (file != null) {
                                            try {
                                                storageManager.exportPackageToFile(
                                                    project = currentP,
                                                    targetFile = file,
                                                    password = currentProjectPassword
                                                )
                                                errorMessage = null
                                                statusMessage = "Esportato con successo in '${file.absolutePath}'."
                                            } catch (e: Exception) {
                                                errorMessage = "Errore durante l'esportazione: ${e.message}"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Esporta .ofam")
                                }

                                Button(
                                    enabled = currentProject != null,
                                    onClick = { showDocumentExportDialog = true }
                                ) {
                                    Text("🖨️ Documenti & Stampa")
                                }

                                OutlinedButton(
                                    enabled = currentProject != null,
                                    onClick = {
                                        currentPassInput = ""
                                        newPassInput = ""
                                        confirmPassInput = ""
                                        showPasswordManageDialog = true
                                    }
                                ) {
                                    Text(if (currentProject?.isPasswordProtected == true) "Cambia Password" else "Imposta Password")
                                }
                            }

                            // Current Active Project Details Card
                            currentProject?.let { p ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Progetto Attivo: ${p.name}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                color = if (p.isPasswordProtected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(
                                                    text = if (p.isPasswordProtected) "🔒 Cifrato / Protetto" else "🔓 Non Protetto",
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("ID Progetto: ${p.id}")
                                        Text("Descrizione: ${(p.description ?: "").ifBlank { "Nessuna descrizione" }}")
                                        Text("Business Unit: ${p.businessUnits.size} | Apparati totali: ${p.businessUnits.sumOf { it.devices.size }} | Rack: ${p.racks.size} | Modelli: ${p.deviceModels.size}")

                                        if (validationIssues.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Esito Validazione ModelValidator (${validationIssues.size} avvisi):",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            validationIssues.take(5).forEach { issue ->
                                                Text("• [${issue.severity}] ${issue.message}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }

                            // Stored Projects List Card
                            Card(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Progetti Salvati in Cartella Dati (${storedProjects.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = { refreshStoredList() }) {
                                            Text("Aggiorna Elenco")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (storedProjects.isEmpty()) {
                                        Text("Nessun progetto salvato nella cartella dati corrente.", style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        storedProjects.forEach { item ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(item.name, fontWeight = FontWeight.Bold)
                                                        Text("File: ${item.file.name} — ID: ${item.id}", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    Button(onClick = { handleImportFile(item.file) }) {
                                                        Text("Apri Progetto")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Password Import Dialog
    if (showPasswordImportDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordImportDialog = false },
            title = { Text("Pacchetto Cifrato da Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Il pacchetto '.ofam' è protetto da password. Inserisci la password per decifrare e importare.")
                    OutlinedTextField(
                        value = inputPasswordForImport,
                        onValueChange = { inputPasswordForImport = it },
                        label = { Text("Password del Progetto") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val file = pendingImportFile
                    showPasswordImportDialog = false
                    if (file != null) {
                        handleImportFile(file, inputPasswordForImport)
                        inputPasswordForImport = ""
                    }
                }) {
                    Text("Sblocca & Importa")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPasswordImportDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Password Management Dialog
    if (showPasswordManageDialog) {
        val isProtected = currentProject?.isPasswordProtected == true
        AlertDialog(
            onDismissRequest = { showPasswordManageDialog = false },
            title = { Text(if (isProtected) "Gestisci / Cambia Password Progetto" else "Imposta Password Progetto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isProtected) {
                        Text("Modifica o rimuovi la password di protezione per il progetto corrente.")
                        OutlinedTextField(
                            value = currentPassInput,
                            onValueChange = { currentPassInput = it },
                            label = { Text("Password Attuale (Obbligatoria)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        label = { Text("Nuova Password (lascia vuota per rimuovere)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it },
                        label = { Text("Conferma Nuova Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = currentProject ?: return@Button
                    if (isProtected && currentPassInput != currentProjectPassword) {
                        errorMessage = "La password attuale inserita non è corretta."
                        return@Button
                    }
                    if (newPassInput != confirmPassInput) {
                        errorMessage = "La nuova password e la conferma non coincidono."
                        return@Button
                    }

                    val newPass = newPassInput.ifBlank { null }
                    val updatedP = p.copy(
                        isPasswordProtected = (newPass != null),
                        updatedEpochMs = System.currentTimeMillis()
                    )

                    currentProject = updatedP
                    currentProjectPassword = newPass
                    showPasswordManageDialog = false

                    try {
                        storageManager.saveProjectLocally(updatedP, password = newPass)
                        refreshStoredList()
                        errorMessage = null
                        statusMessage = if (newPass != null) "Password impostata con successo." else "Protezione da password rimossa."
                    } catch (e: Exception) {
                        errorMessage = "Errore aggiornamento password: ${e.message}"
                    }
                }) {
                    Text("Salva Modifiche")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPasswordManageDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Comparison Warning Dialog on Import
    if (showComparisonDialog) {
        val comp = pendingComparison
        val pkg = pendingImportPackage
        AlertDialog(
            onDismissRequest = { showComparisonDialog = false },
            title = { Text("Confronto Copia Importata") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Esito confronto: ${comp?.summary}")
                    comp?.warningMessage?.let { warn ->
                        Text("⚠️ $warn", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    Text("Vuoi sostituire la copia di lavoro attuale con il pacchetto importato?")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showComparisonDialog = false
                    if (pkg != null) {
                        currentProject = pkg.project
                        currentManifest = pkg.manifest
                        errorMessage = null
                        statusMessage = "Sostituita copia locale con '${pkg.project.name}'."
                        try {
                            storageManager.saveProjectLocally(pkg.project, password = currentProjectPassword)
                            refreshStoredList()
                        } catch (e: Exception) {
                            errorMessage = "Errore salvataggio locale: ${e.message}"
                        }
                    }
                }) {
                    Text("Sostituisci Copia")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showComparisonDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Document Export & Native Printing Dialog
    if (showDocumentExportDialog) {
        val currentP = currentProject
        if (currentP != null) {
            AlertDialog(
                onDismissRequest = { showDocumentExportDialog = false },
                title = { Text("Esportazione Documenti e Stampa Nativa") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Seleziona il formato del documento da generare:")

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = docFormat == "COMPOSITE_PDF", onClick = { docFormat = "COMPOSITE_PDF" })
                            Text("Report PDF Composto")
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(selected = docFormat == "XLSX", onClick = { docFormat = "XLSX" })
                            Text("Foglio Excel (.xlsx)")
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(selected = docFormat == "MARKDOWN", onClick = { docFormat = "MARKDOWN" })
                            Text("Markdown (.md)")
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(selected = docFormat == "PRINT_NATIVE", onClick = { docFormat = "PRINT_NATIVE" })
                            Text("Stampa Nativa Windows 11")
                        }

                        OutlinedTextField(
                            value = docAuthor,
                            onValueChange = { docAuthor = it },
                            label = { Text("Nome Autore / Tecnico Operativo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = docTitleOverride,
                            onValueChange = { docTitleOverride = it },
                            label = { Text("Titolo Personalizzato Documento (opzionale)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = docIncludeConfidential, onCheckedChange = { docIncludeConfidential = it })
                            Text("Includi dati con classificazione Riservato (Zero Secret Leakage garantito)")
                        }

                        if (docFormat == "COMPOSITE_PDF" || docFormat == "PRINT_NATIVE") {
                            Text("Sezioni da Includere nel Report:", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = docIncRackCards, onCheckedChange = { docIncRackCards = it })
                                Text("Schede Rack")
                                Spacer(modifier = Modifier.width(12.dp))
                                Checkbox(checked = docIncInventoryTable, onCheckedChange = { docIncInventoryTable = it })
                                Text("Inventario")
                                Spacer(modifier = Modifier.width(12.dp))
                                Checkbox(checked = docIncCablingAndPorts, onCheckedChange = { docIncCablingAndPorts = it })
                                Text("Cablaggio")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showDocumentExportDialog = false
                        val filterConfig = ExportFilterConfig(
                            includeConfidential = docIncludeConfidential,
                            reviewRequiredConfirmed = true,
                            authorName = docAuthor,
                            titleOverride = docTitleOverride.ifBlank { null }
                        )
                        val selection = ReportSelection(
                            includeRackCards = docIncRackCards,
                            includeInventoryTable = docIncInventoryTable,
                            includeCablingAndPorts = docIncCablingAndPorts,
                            includeLogicalNetwork = docIncLogicalNetwork
                        )

                        when (docFormat) {
                            "XLSX" -> {
                                val file = DesktopStorageHelper.pickSaveFile("Esporta Foglio Excel XLSX", "${currentP.name.replace(" ", "_")}.xlsx")
                                if (file != null) {
                                    file.outputStream().use { stream -> DesktopDocumentManager.exportXlsx(currentP, filterConfig, stream) }
                                    statusMessage = "Esportato XLSX in '${file.absolutePath}'."
                                }
                            }
                            "MARKDOWN" -> {
                                val file = DesktopStorageHelper.pickSaveFile("Esporta Documento Markdown", "${currentP.name.replace(" ", "_")}.md")
                                if (file != null) {
                                    file.outputStream().use { stream -> DesktopDocumentManager.exportMarkdown(currentP, filterConfig, stream) }
                                    statusMessage = "Esportato Markdown in '${file.absolutePath}'."
                                }
                            }
                            "COMPOSITE_PDF" -> {
                                val file = DesktopStorageHelper.pickSaveFile("Esporta Report Tecnico PDF", "${currentP.name.replace(" ", "_")}_report.pdf")
                                if (file != null) {
                                    file.outputStream().use { stream -> DesktopDocumentManager.exportCompositePdf(currentP, filterConfig, selection, stream) }
                                    statusMessage = "Esportato Report PDF in '${file.absolutePath}'."
                                }
                            }
                            "PRINT_NATIVE" -> {
                                val printed = DesktopDocumentManager.printDocumentNative(currentP, filterConfig, selection)
                                statusMessage = if (printed) "Inviato alla stampante Windows." else "Operazione di stampa annullata."
                            }
                        }
                    }) {
                        Text(if (docFormat == "PRINT_NATIVE") "Invia alla Stampante" else "Genera Documento")
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
}
