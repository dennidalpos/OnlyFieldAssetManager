package com.onlyfield.assetmanager.pc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.*
import com.onlyfield.assetmanager.exchange.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp() {
    var currentProject by remember { mutableStateOf<Project?>(null) }
    var statusMessage by remember { mutableStateOf("Pronto. Runtime Windows 11 x64 attivato.") }
    var validationIssues by remember { mutableStateOf<List<ValidationIssue>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OnlyField Asset Manager — Desktop Windows (W00)") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Stato Toolchain & Contratto v1.7",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    val projId = UUID.randomUUID().toString()
                    val buId = UUID.randomUUID().toString()
                    val areaId = UUID.randomUUID().toString()
                    val devId = UUID.randomUUID().toString()

                    val now = System.currentTimeMillis()
                    val proj = Project(
                        id = projId,
                        name = "Progetto Pilota Desktop Windows",
                        createdEpochMs = now,
                        updatedEpochMs = now,
                        businessUnits = listOf(
                            BusinessUnit(
                                id = buId,
                                name = "Sede Centrale PC",
                                areas = listOf(
                                    Area(
                                        id = areaId,
                                        name = "Sala CED Desktop"
                                    )
                                ),
                                devices = listOf(
                                    Device(
                                        id = devId,
                                        areaId = areaId,
                                        technicalName = "SW-CORE-PC01",
                                        category = DeviceCategory.NETWORK_SWITCH,
                                        ports = listOf(
                                            Port(id = UUID.randomUUID().toString(), deviceId = devId, name = "Gi0/1"),
                                            Port(id = UUID.randomUUID().toString(), deviceId = devId, name = "Gi0/2")
                                        )
                                    )
                                )
                            )
                        )
                    )

                    val result = ModelValidator.validateProject(proj)

                    currentProject = proj
                    validationIssues = result.issues
                    statusMessage = "Creato progetto demo '${proj.name}' con 1 BU, 1 Area e 1 Apparato. Esito validazione: ${result.issues.size} avvisi/errori."
                }) {
                    Text("Crea Progetto Demo")
                }

                Button(
                    enabled = currentProject != null,
                    onClick = {
                        val proj = currentProject ?: return@Button
                        val file = DesktopStorageHelper.pickSaveFile(
                            title = "Salva pacchetto .ofam",
                            defaultFileName = "PC-PILOT-01.ofam"
                        )
                        if (file != null) {
                            try {
                                val bytes = PackageSerializer.exportPackage(project = proj)
                                file.writeBytes(bytes)
                                statusMessage = "Esportato pacchetto .ofam (${bytes.size} byte) in: ${file.absolutePath}"
                            } catch (e: Exception) {
                                statusMessage = "Errore durante l'esportazione: ${e.message}"
                            }
                        }
                    }
                ) {
                    Text("Esporta .ofam")
                }

                Button(onClick = {
                    val file = DesktopStorageHelper.pickOpenFile()
                    if (file != null) {
                        try {
                            val bytes = file.readBytes()
                            val importResult = PackageSerializer.importPackage(zipBytes = bytes)
                            val imported = importResult.pkg?.project
                            val issues = importResult.validationResult.issues

                            if (imported != null) {
                                currentProject = imported
                                validationIssues = issues
                                val devCount = imported.businessUnits.sumOf { it.devices.size }
                                statusMessage = "Importato con successo pacchetto '${imported.name}' ($devCount apparati). Riscontrati ${issues.size} problemi di validazione."
                            } else {
                                statusMessage = "Impossibile importare pacchetto: ${issues.joinToString { it.message }}"
                            }
                        } catch (e: Exception) {
                            statusMessage = "Errore durante l'importazione di ${file.name}: ${e.message}"
                        }
                    }
                }) {
                    Text("Importa .ofam")
                }

                OutlinedButton(onClick = {
                    val available = DesktopStorageHelper.isPrinterAvailable()
                    statusMessage = if (available) {
                        "Servizio di stampa Windows disponibile (PrinterJob abilitato)."
                    } else {
                        "Nessuna stampante predefinita rilevata nel sistema Windows."
                    }
                }) {
                    Text("Verifica Stampa")
                }
            }

            val proj = currentProject
            if (proj != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Dettagli Progetto Corrente",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text("ID: ${proj.id}")
                        Text("Nome: ${proj.name}")
                        Text("Business Unit: ${proj.businessUnits.size}")
                        val totalDevices = proj.businessUnits.sumOf { it.devices.size }
                        Text("Apparati totali: $totalDevices")

                        if (validationIssues.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Problemi di validazione (${validationIssues.size}):",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            validationIssues.take(5).forEach { issue ->
                                Text("• [${issue.severity}] ${issue.message}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
