package com.onlyfield.assetmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.room.Room
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
            "onlyfield_asset_manager.db"
        ).build()

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

    var newProjectName by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                currentProject?.id?.let { projId ->
                    viewModel.exportProjectToStream(projId, stream)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
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
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    items(projects) { project ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.loadProject(project.id) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = project.name, style = MaterialTheme.typography.titleSmall)
                                Text(text = "ID: ${project.id}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                        Text(text = "Progetto Selezionato: ${proj.name}", style = MaterialTheme.typography.titleSmall)
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
                    // Tree Overview
                    Text(text = "Struttura Inventario", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn {
                        items(proj.businessUnits) { bu ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(text = "• BU: ${bu.name}", style = MaterialTheme.typography.titleSmall)
                                bu.devices.forEach { dev ->
                                    Text(
                                        text = "   - Apparato: ${dev.technicalName} (IP: ${dev.ipAddress ?: "Assente"})",
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
}
