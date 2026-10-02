package com.onlyfield.assetmanager.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Credential
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.core.model.DeviceModel
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.repository.PackageImportEvaluation
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.data.repository.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ProjectViewModel(
    private val repository: ProjectRepository,
) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = repository.getAllProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _saveState = MutableStateFlow("Salvato")
    val saveState: StateFlow<String> = _saveState.asStateFlow()

    private val _pendingImportEvaluation = MutableStateFlow<PackageImportEvaluation?>(null)
    val pendingImportEvaluation: StateFlow<PackageImportEvaluation?> = _pendingImportEvaluation.asStateFlow()

    private val _importErrorMessage = MutableStateFlow<String?>(null)
    val importErrorMessage: StateFlow<String?> = _importErrorMessage.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    private val _passwordPromptForImport = MutableStateFlow(false)
    val passwordPromptForImport: StateFlow<Boolean> = _passwordPromptForImport.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            if (projectId.isBlank()) {
                _currentProject.value = null
            } else {
                _currentProject.value = repository.getProjectById(projectId)
            }
        }
    }

    fun createNewProject(name: String, description: String? = null) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio in corso..."
            try {
                val now = System.currentTimeMillis()
                val newProject = Project(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    createdEpochMs = now,
                    updatedEpochMs = now,
                    businessUnits = listOf(
                        BusinessUnit(
                            id = UUID.randomUUID().toString(),
                            name = "Business Unit Iniziale"
                        )
                    )
                )
                repository.saveProject(newProject)
                _currentProject.value = newProject
                _saveState.value = "Salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore durante il salvataggio: ${e.message}"
            }
        }
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio in corso..."
            try {
                repository.renameProject(projectId, newName)
                val updated = repository.getProjectById(projectId)
                _currentProject.value = updated
                _saveState.value = "Salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore durante il salvataggio: ${e.message}"
            }
        }
    }

    fun setProjectPassword(projectId: String, currentPassword: String?, newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.setProjectPassword(projectId, currentPassword, newPassword)
            if (success) {
                _currentProject.value = repository.getProjectById(projectId)
                _saveState.value = "Password aggiornata"
            }
            onResult(success)
        }
    }

    fun removeProjectPassword(projectId: String, currentPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.removeProjectPassword(projectId, currentPassword)
            if (success) {
                _currentProject.value = repository.getProjectById(projectId)
                _saveState.value = "Protezione password rimossa"
            }
            onResult(success)
        }
    }

    fun addCredentialToProject(projectId: String, credential: Credential) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio in corso..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updatedCredentials = current.credentials + credential
                val updatedProject = current.copy(
                    credentials = updatedCredentials,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Credenziale salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore durante il salvataggio credenziale: ${e.message}"
            }
        }
    }

    fun addPowerFeedToProject(projectId: String, feed: com.onlyfield.assetmanager.core.model.PowerFeed) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio in corso..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updated = current.copy(
                    powerFeeds = current.powerFeeds.filterNot { it.id == feed.id } + feed,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Alimentazione salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore durante il salvataggio alimentazione: ${e.message}"
            }
        }
    }

    fun addPoeMappingToProject(projectId: String, poe: com.onlyfield.assetmanager.core.model.PoeMapping) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio in corso..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updated = current.copy(
                    poeMappings = current.poeMappings.filterNot { it.id == poe.id } + poe,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Configurazione PoE salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore durante il salvataggio PoE: ${e.message}"
            }
        }
    }

    fun addDocumentBadgeToProject(projectId: String, badge: com.onlyfield.assetmanager.core.model.DocumentBadge) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio in corso..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updated = current.copy(
                    documentBadges = current.documentBadges.filterNot { it.id == badge.id } + badge,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Badge salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore durante il salvataggio badge: ${e.message}"
            }
        }
    }

    fun addRackToProject(projectId: String, name: String, heightU: Int, areaId: String? = null) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio in corso..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newRack = Rack(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    areaId = areaId,
                    heightU = heightU
                )
                val updatedRacks = current.racks + newRack
                val updatedProject = current.copy(
                    racks = updatedRacks,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Rack salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore durante il salvataggio del rack: ${e.message}"
            }
        }
    }

    fun addDeviceModelToProject(projectId: String, name: String, brand: String?, modelNumber: String?, category: DeviceCategory, heightU: Int) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio in corso..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newModel = DeviceModel(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    brand = brand,
                    modelNumber = modelNumber,
                    category = category,
                    defaultHeightU = heightU
                )
                val updatedModels = current.deviceModels + newModel
                val updatedProject = current.copy(
                    deviceModels = updatedModels,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Modello apparato salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore durante il salvataggio del modello: ${e.message}"
            }
        }
    }

    fun exportRackPdf(projectId: String, rackId: String, outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val success = repository.exportRackPdfToStream(projectId, rackId, outputStream)
                if (success) {
                    _exportStatusMessage.value = "PDF Scheda Rack esportato con successo"
                } else {
                    _exportStatusMessage.value = "Errore durante la generazione del PDF"
                }
            } catch (e: Exception) {
                _exportStatusMessage.value = "Errore PDF: ${e.message}"
            }
        }
    }

    fun exportXlsx(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val success = repository.exportXlsxToStream(projectId, filterConfig, outputStream)
                if (success) {
                    _exportStatusMessage.value = "Foglio Excel (.xlsx) esportato con successo"
                } else {
                    _exportStatusMessage.value = "Errore durante la generazione del foglio Excel"
                }
            } catch (e: Exception) {
                _exportStatusMessage.value = "Errore Excel: ${e.message}"
            }
        }
    }

    fun exportMarkdown(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val success = repository.exportMarkdownToStream(projectId, filterConfig, outputStream)
                if (success) {
                    _exportStatusMessage.value = "Documento Markdown (.md) esportato con successo"
                } else {
                    _exportStatusMessage.value = "Errore durante la generazione del Markdown"
                }
            } catch (e: Exception) {
                _exportStatusMessage.value = "Errore Markdown: ${e.message}"
            }
        }
    }

    fun exportCompositePdf(
        projectId: String,
        filterConfig: ExportFilterConfig,
        reportSelection: ReportSelection,
        outputStream: OutputStream
    ) {
        viewModelScope.launch {
            try {
                val success = repository.exportCompositePdfToStream(projectId, filterConfig, reportSelection, outputStream)
                if (success) {
                    _exportStatusMessage.value = "Report PDF Composto esportato con successo"
                } else {
                    _exportStatusMessage.value = "Errore durante la generazione del report PDF"
                }
            } catch (e: Exception) {
                _exportStatusMessage.value = "Errore PDF: ${e.message}"
            }
        }
    }

    fun printProject(
        context: Context,
        projectId: String,
        filterConfig: ExportFilterConfig,
        reportSelection: ReportSelection
    ) {
        viewModelScope.launch {
            try {
                val success = repository.printProjectDocument(context, projectId, filterConfig, reportSelection)
                if (success) {
                    _exportStatusMessage.value = "Avviata la sessione di stampa Android"
                } else {
                    _exportStatusMessage.value = "Servizio di stampa non disponibile"
                }
            } catch (e: Exception) {
                _exportStatusMessage.value = "Errore di stampa: ${e.message}"
            }
        }
    }

    fun exportDeviceModel(model: DeviceModel, outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                repository.exportDeviceModelToStream(model, outputStream)
                _exportStatusMessage.value = "Modello apparato esportato con successo"
            } catch (e: Exception) {
                _exportStatusMessage.value = "Errore esportazione modello: ${e.message}"
            }
        }
    }

    fun importDeviceModel(projectId: String, inputStream: InputStream) {
        viewModelScope.launch {
            try {
                val model = repository.importDeviceModelFromStream(inputStream)
                val current = repository.getProjectById(projectId) ?: return@launch
                val updatedModels = current.deviceModels + model
                val updatedProject = current.copy(
                    deviceModels = updatedModels,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _exportStatusMessage.value = "Modello '${model.name}' importato con successo"
            } catch (e: Exception) {
                _importErrorMessage.value = "Errore importazione modello: ${e.message}"
            }
        }
    }

    fun search(projectId: String, query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                _searchResults.value = repository.searchInventory(projectId, query)
            }
        }
    }

    fun exportProjectToStream(projectId: String, outputStream: OutputStream, password: String? = null) {
        viewModelScope.launch {
            try {
                val success = repository.exportProjectPackageToStream(projectId, outputStream, password = password)
                if (success) {
                    _exportStatusMessage.value = "Esportazione pacchetto .ofam completata"
                } else {
                    _exportStatusMessage.value = "Errore durante l'esportazione: progetto non trovato"
                }
            } catch (e: Exception) {
                _exportStatusMessage.value = "Errore durante l'esportazione: ${e.message}"
            }
        }
    }

    fun evaluateImportFromStream(inputStream: InputStream, password: String? = null) {
        viewModelScope.launch {
            try {
                val evaluation = repository.evaluateImportPackage(inputStream, password, _currentProject.value?.id)
                val requiredIssue = evaluation.importResult.validationResult.issues.find { (code) -> code == "PASSWORD_REQUIRED" }

                if (requiredIssue != null) {
                    _passwordPromptForImport.value = true
                    _importErrorMessage.value = null
                    _pendingImportEvaluation.value = null
                    return@launch
                }

                _passwordPromptForImport.value = false

                if ((evaluation.comparison != null) && (evaluation.importResult.pkg != null)) {
                    _pendingImportEvaluation.value = evaluation
                    _importErrorMessage.value = null
                } else {
                    val issuesStr = evaluation.importResult.validationResult.issues.joinToString("\n") { (code, message) -> "$code: $message" }
                    _importErrorMessage.value = issuesStr.ifBlank { "Pacchetto non valido o corrotto" }
                    _pendingImportEvaluation.value = null
                }
            } catch (e: Exception) {
                _importErrorMessage.value = "Impossibile leggere il pacchetto: ${e.message}"
                _pendingImportEvaluation.value = null
            }
        }
    }

    fun confirmImport() {
        viewModelScope.launch {
            val eval = _pendingImportEvaluation.value ?: return@launch
            val pkg = eval.importResult.pkg ?: return@launch
            try {
                repository.importProjectPackage(pkg)
                _currentProject.value = repository.getProjectById(pkg.project.id)
                _saveState.value = "Importazione completata con successo"
                _pendingImportEvaluation.value = null
                _importErrorMessage.value = null
            } catch (e: Exception) {
                _importErrorMessage.value = "Errore durante l'importazione nel database: ${e.message}"
            }
        }
    }

    fun addAttachmentToProject(
        projectId: String,
        name: String,
        fileType: com.onlyfield.assetmanager.core.model.AttachmentType,
        mimeType: String,
        classification: com.onlyfield.assetmanager.core.model.AttachmentClassification,
        targetType: com.onlyfield.assetmanager.core.model.AttachmentTargetType? = null,
        targetId: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio allegato..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newAtt = com.onlyfield.assetmanager.core.model.Attachment(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    originalFileName = name,
                    fileType = fileType,
                    mimeType = mimeType,
                    relativePath = "attachments/${UUID.randomUUID()}_$name",
                    classification = classification,
                    targetType = targetType,
                    targetId = targetId
                )
                val updatedAtts = current.attachments + newAtt
                val updatedProject = current.copy(
                    attachments = updatedAtts,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Allegato salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore durante il salvataggio allegato: ${e.message}"
            }
        }
    }

    fun updateAttachmentClassification(projectId: String, attachmentId: String, newClassification: com.onlyfield.assetmanager.core.model.AttachmentClassification) {
        viewModelScope.launch {
            _saveState.value = "Aggiornamento classificazione..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updatedAtts = current.attachments.map { att ->
                    if (att.id == attachmentId) att.copy(classification = newClassification) else att
                }
                val updatedProject = current.copy(
                    attachments = updatedAtts,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Classificazione aggiornata"
            } catch (e: Exception) {
                _saveState.value = "Errore aggiornamento: ${e.message}"
            }
        }
    }

    fun deleteAttachment(projectId: String, attachmentId: String) {
        viewModelScope.launch {
            _saveState.value = "Eliminazione allegato..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updatedAtts = current.attachments.filterNot { it.id == attachmentId }
                val updatedProject = current.copy(
                    attachments = updatedAtts,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Allegato eliminato"
            } catch (e: Exception) {
                _saveState.value = "Errore eliminazione: ${e.message}"
            }
        }
    }

    fun acquireCartographicBackground(
        projectId: String,
        areaId: String,
        request: com.onlyfield.assetmanager.cartography.MapSnapshotRequest,
        context: Context
    ) {
        viewModelScope.launch {
            _saveState.value = "Acquisizione sfondo cartografico in corso..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val result = com.onlyfield.assetmanager.cartography.CartographicMapManager.acquireMapSnapshot(request)

                val attachmentsDir = java.io.File(context.filesDir, "attachments/$projectId").apply { mkdirs() }
                val attachmentId = UUID.randomUUID().toString()
                val filename = "map_snapshot_$attachmentId.png"
                val file = java.io.File(attachmentsDir, filename)
                file.writeBytes(result.imageBytes)

                val relativePath = "attachments/$projectId/$filename"
                val attachment = com.onlyfield.assetmanager.core.model.Attachment(
                    id = attachmentId,
                    name = "Sfondo Cartografico (${result.sourceName})",
                    originalFileName = filename,
                    fileType = com.onlyfield.assetmanager.core.model.AttachmentType.IMAGE,
                    mimeType = "image/png",
                    relativePath = relativePath,
                    classification = com.onlyfield.assetmanager.core.model.AttachmentClassification.SHAREABLE,
                    targetType = com.onlyfield.assetmanager.core.model.AttachmentTargetType.AREA,
                    targetId = areaId,
                    attributionText = result.attributionText
                )

                val updatedAtts = current.attachments + attachment
                val updatedBus = current.businessUnits.map { bu ->
                    val updatedSites = bu.sites.map { site ->
                        val updatedAreas = site.areas.map { area ->
                            if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = 0)
                            else area
                        }
                        site.copy(areas = updatedAreas)
                    }
                    val updatedDirectAreas = bu.areas.map { area ->
                        if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = 0)
                        else area
                    }
                    bu.copy(sites = updatedSites, areas = updatedDirectAreas)
                }

                val updatedProject = current.copy(
                    attachments = updatedAtts,
                    businessUnits = updatedBus,
                    updatedEpochMs = System.currentTimeMillis()
                )

                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Sfondo cartografico acquisito con successo"
            } catch (e: com.onlyfield.assetmanager.cartography.OfflineMapException) {
                _saveState.value = e.message ?: "Errore acquisizione mappa offline"
            } catch (e: Exception) {
                _saveState.value = "Errore durante l'acquisizione cartografica: ${e.message}"
            }
        }
    }

    fun addFloorplanPlacement(
        projectId: String,
        areaId: String,
        targetType: com.onlyfield.assetmanager.core.model.PlacementTargetType,
        targetId: String,
        xRatio: Float,
        yRatio: Float
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio collocazione..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newPlacement = com.onlyfield.assetmanager.core.model.FloorplanPlacement(
                    id = UUID.randomUUID().toString(),
                    areaId = areaId,
                    targetType = targetType,
                    targetId = targetId,
                    xRatio = xRatio,
                    yRatio = yRatio
                )
                val updatedPlacements = current.floorplanPlacements + newPlacement
                val updatedProject = current.copy(
                    floorplanPlacements = updatedPlacements,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Posizione registrata"
            } catch (e: Exception) {
                _saveState.value = "Errore collocazione: ${e.message}"
            }
        }
    }

    fun deleteFloorplanPlacement(projectId: String, placementId: String) {
        viewModelScope.launch {
            _saveState.value = "Rimozione collocazione..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updatedPlacements = current.floorplanPlacements.filterNot { it.id == placementId }
                val updatedProject = current.copy(
                    floorplanPlacements = updatedPlacements,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Collocazione rimossa"
            } catch (e: Exception) {
                _saveState.value = "Errore rimozione: ${e.message}"
            }
        }
    }

    fun addAnnotationToArea(
        projectId: String,
        areaId: String,
        type: com.onlyfield.assetmanager.core.model.AnnotationType,
        x1Ratio: Float,
        y1Ratio: Float,
        x2Ratio: Float = x1Ratio,
        y2Ratio: Float = y1Ratio,
        label: String = "",
        colorHex: String = "#FF0000",
        classification: com.onlyfield.assetmanager.core.model.AttachmentClassification = com.onlyfield.assetmanager.core.model.AttachmentClassification.SHAREABLE
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio annotazione..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newAnn = com.onlyfield.assetmanager.core.model.Annotation(
                    id = UUID.randomUUID().toString(),
                    areaId = areaId,
                    type = type,
                    x1Ratio = x1Ratio,
                    y1Ratio = y1Ratio,
                    x2Ratio = x2Ratio,
                    y2Ratio = y2Ratio,
                    label = label,
                    colorHex = colorHex,
                    classification = classification
                )
                val updatedAnns = current.annotations + newAnn
                val updatedProject = current.copy(
                    annotations = updatedAnns,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Annotazione salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore annotazione: ${e.message}"
            }
        }
    }

    fun deleteAnnotation(projectId: String, annotationId: String) {
        viewModelScope.launch {
            _saveState.value = "Rimozione annotazione..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updatedAnns = current.annotations.filterNot { it.id == annotationId }
                val updatedProject = current.copy(
                    annotations = updatedAnns,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Annotazione rimossa"
            } catch (e: Exception) {
                _saveState.value = "Errore annotazione: ${e.message}"
            }
        }
    }

    fun setAreaFloorplanAttachment(projectId: String, areaId: String, attachmentId: String?) {
        viewModelScope.launch {
            _saveState.value = "Aggiornamento planimetria..."
            try {
                repository.updateAreaFloorplan(projectId, areaId, attachmentId)
                _currentProject.value = repository.getProjectById(projectId)
                _saveState.value = "Planimetria area aggiornata (posizioni preservate)"
            } catch (e: Exception) {
                _saveState.value = "Errore planimetria: ${e.message}"
            }
        }
    }

    fun addSharedPathSegment(
        projectId: String,
        name: String,
        sourceAreaId: String? = null,
        targetAreaId: String? = null,
        description: String? = null,
        capacityMaxCables: Int? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio percorso condiviso..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newSegment = com.onlyfield.assetmanager.core.model.SharedPathSegment(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    sourceAreaId = sourceAreaId,
                    targetAreaId = targetAreaId,
                    description = description,
                    capacityMaxCables = capacityMaxCables,
                    notes = notes
                )
                val updatedSegments = current.sharedPathSegments + newSegment
                val updatedProject = current.copy(
                    sharedPathSegments = updatedSegments,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Percorso condiviso salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore percorso: ${e.message}"
            }
        }
    }

    fun deleteSharedPathSegment(projectId: String, segmentId: String) {
        viewModelScope.launch {
            _saveState.value = "Rimozione percorso..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updatedSegments = current.sharedPathSegments.filterNot { it.id == segmentId }
                val updatedProject = current.copy(
                    sharedPathSegments = updatedSegments,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Percorso condiviso rimosso"
            } catch (e: Exception) {
                _saveState.value = "Errore percorso: ${e.message}"
            }
        }
    }

    fun addCable(
        projectId: String,
        codeOrLabel: String?,
        portAId: String?,
        portBId: String?,
        medium: com.onlyfield.assetmanager.core.model.CableMedium,
        connectorA: String? = null,
        connectorB: String? = null,
        nominalCharacteristics: String? = null,
        observedSpeed: String? = null,
        color: String? = null,
        lengthValue: Double? = null,
        lengthUnit: String? = "m",
        orientation: com.onlyfield.assetmanager.core.model.CableOrientation = com.onlyfield.assetmanager.core.model.CableOrientation.NONE,
        sharedPathSegmentIds: List<String> = emptyList(),
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio cavo..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newCable = com.onlyfield.assetmanager.core.model.Cable(
                    id = UUID.randomUUID().toString(),
                    codeOrLabel = codeOrLabel,
                    portAId = portAId,
                    portBId = portBId,
                    medium = medium,
                    connectorA = connectorA,
                    connectorB = connectorB,
                    nominalCharacteristics = nominalCharacteristics,
                    observedSpeed = observedSpeed,
                    color = color,
                    lengthValue = lengthValue,
                    lengthUnit = lengthUnit,
                    orientation = orientation,
                    sharedPathSegmentIds = sharedPathSegmentIds,
                    notes = notes
                )
                val updatedCables = current.cables + newCable
                val updatedProject = current.copy(
                    cables = updatedCables,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Cavo salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore cavo: ${e.message}"
            }
        }
    }

    fun deleteCable(projectId: String, cableId: String) {
        viewModelScope.launch {
            _saveState.value = "Rimozione cavo..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updatedCables = current.cables.filterNot { it.id == cableId }
                val updatedProject = current.copy(
                    cables = updatedCables,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Cavo rimosso"
            } catch (e: Exception) {
                _saveState.value = "Errore cavo: ${e.message}"
            }
        }
    }

    fun addPanelMapping(
        projectId: String,
        portAId: String,
        portBId: String?,
        mappingType: String = "CROSS_CONNECT",
        isUnknownPassage: Boolean = false,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio mapping pannello..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newMapping = com.onlyfield.assetmanager.core.model.PanelMapping(
                    id = UUID.randomUUID().toString(),
                    portAId = portAId,
                    portBId = portBId,
                    mappingType = mappingType,
                    isUnknownPassage = isUnknownPassage,
                    notes = notes
                )
                val updatedMappings = current.panelMappings + newMapping
                val updatedProject = current.copy(
                    panelMappings = updatedMappings,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Mapping pannello salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore mapping: ${e.message}"
            }
        }
    }

    fun deletePanelMapping(projectId: String, mappingId: String) {
        viewModelScope.launch {
            _saveState.value = "Rimozione mapping..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updatedMappings = current.panelMappings.filterNot { it.id == mappingId }
                val updatedProject = current.copy(
                    panelMappings = updatedMappings,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updatedProject)
                _currentProject.value = updatedProject
                _saveState.value = "Mapping pannello rimosso"
            } catch (e: Exception) {
                _saveState.value = "Errore mapping: ${e.message}"
            }
        }
    }

    fun addVlan(
        projectId: String,
        vlanId: Int,
        name: String,
        scopeType: com.onlyfield.assetmanager.core.model.VlanScopeType = com.onlyfield.assetmanager.core.model.VlanScopeType.PROJECT,
        scopeTargetId: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio VLAN..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newVlan = com.onlyfield.assetmanager.core.model.Vlan(
                    id = UUID.randomUUID().toString(),
                    vlanId = vlanId,
                    name = name,
                    scopeType = scopeType,
                    scopeTargetId = scopeTargetId,
                    description = description
                )
                val updated = current.copy(
                    vlans = current.vlans + newVlan,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "VLAN $vlanId salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore VLAN: ${e.message}"
            }
        }
    }

    fun deleteVlan(projectId: String, vlanUuid: String) {
        viewModelScope.launch {
            _saveState.value = "Rimozione VLAN..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updated = current.copy(
                    vlans = current.vlans.filterNot { it.id == vlanUuid },
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "VLAN rimossa"
            } catch (e: Exception) {
                _saveState.value = "Errore VLAN: ${e.message}"
            }
        }
    }

    fun addSubnet(
        projectId: String,
        cidrBlock: String,
        gatewayIp: String? = null,
        vlanId: String? = null,
        name: String? = null,
        scopeType: com.onlyfield.assetmanager.core.model.VlanScopeType = com.onlyfield.assetmanager.core.model.VlanScopeType.PROJECT,
        scopeTargetId: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio Subnet..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newSubnet = com.onlyfield.assetmanager.core.model.Subnet(
                    id = UUID.randomUUID().toString(),
                    cidrBlock = cidrBlock,
                    gatewayIp = gatewayIp,
                    vlanId = vlanId,
                    name = name,
                    scopeType = scopeType,
                    scopeTargetId = scopeTargetId,
                    description = description
                )
                val updated = current.copy(
                    subnets = current.subnets + newSubnet,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Subnet $cidrBlock salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore Subnet: ${e.message}"
            }
        }
    }

    fun deleteSubnet(projectId: String, subnetId: String) {
        viewModelScope.launch {
            _saveState.value = "Rimozione Subnet..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val updated = current.copy(
                    subnets = current.subnets.filterNot { it.id == subnetId },
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Subnet rimossa"
            } catch (e: Exception) {
                _saveState.value = "Errore Subnet: ${e.message}"
            }
        }
    }

    fun addPortVlanMembership(
        projectId: String,
        portId: String,
        mode: com.onlyfield.assetmanager.core.model.PortVlanMode,
        untaggedVlanId: Int? = null,
        taggedVlanIds: List<Int> = emptyList(),
        nativeVlanId: Int? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio membership VLAN..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newMembership = com.onlyfield.assetmanager.core.model.PortVlanMembership(
                    id = UUID.randomUUID().toString(),
                    portId = portId,
                    mode = mode,
                    untaggedVlanId = untaggedVlanId,
                    taggedVlanIds = taggedVlanIds,
                    nativeVlanId = nativeVlanId,
                    notes = notes
                )
                val updated = current.copy(
                    portVlanMemberships = current.portVlanMemberships.filterNot { it.portId == portId } + newMembership,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Membership VLAN salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore Membership VLAN: ${e.message}"
            }
        }
    }

    fun addLogicalInterface(
        projectId: String,
        deviceId: String,
        name: String,
        ipAddress: String? = null,
        subnetCidr: String? = null,
        vlanId: Int? = null,
        isL3: Boolean = true,
        macAddress: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio interfaccia logica..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newInt = com.onlyfield.assetmanager.core.model.LogicalInterface(
                    id = UUID.randomUUID().toString(),
                    deviceId = deviceId,
                    name = name,
                    ipAddress = ipAddress,
                    subnetCidr = subnetCidr,
                    vlanId = vlanId,
                    isL3 = isL3,
                    macAddress = macAddress,
                    notes = notes
                )
                val updated = current.copy(
                    logicalInterfaces = current.logicalInterfaces + newInt,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Interfaccia logica '$name' salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore interfaccia: ${e.message}"
            }
        }
    }

    fun addLagGroup(
        projectId: String,
        deviceId: String,
        name: String,
        mode: com.onlyfield.assetmanager.core.model.LagMode = com.onlyfield.assetmanager.core.model.LagMode.LACP,
        memberPortIds: List<String> = emptyList(),
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio Gruppo LAG..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newLag = com.onlyfield.assetmanager.core.model.LagGroup(
                    id = UUID.randomUUID().toString(),
                    deviceId = deviceId,
                    name = name,
                    mode = mode,
                    memberPortIds = memberPortIds,
                    notes = notes
                )
                val updated = current.copy(
                    lagGroups = current.lagGroups + newLag,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Gruppo LAG '$name' salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore LAG: ${e.message}"
            }
        }
    }

    fun addDeviceConfiguration(
        projectId: String,
        deviceId: String,
        title: String,
        configText: String? = null,
        attachmentId: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio configurazione..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newConfig = com.onlyfield.assetmanager.core.model.DeviceConfiguration(
                    id = UUID.randomUUID().toString(),
                    deviceId = deviceId,
                    title = title,
                    configText = configText,
                    attachmentId = attachmentId,
                    capturedEpochMs = System.currentTimeMillis(),
                    notes = notes
                )
                val updated = current.copy(
                    deviceConfigurations = current.deviceConfigurations + newConfig,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Configurazione '$title' salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore configurazione: ${e.message}"
            }
        }
    }

    fun addWanVpnConnection(
        projectId: String,
        name: String,
        type: com.onlyfield.assetmanager.core.model.WanVpnType = com.onlyfield.assetmanager.core.model.WanVpnType.WAN,
        providerOrCarrier: String? = null,
        bandwidth: String? = null,
        localEndpointDeviceId: String? = null,
        localEndpointSiteDescription: String? = null,
        remoteEndpointDeviceId: String? = null,
        remoteEndpointSiteDescription: String? = null,
        underlyingAccessId: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio connessione WAN/VPN..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newConn = com.onlyfield.assetmanager.core.model.WanVpnConnection(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    providerOrCarrier = providerOrCarrier,
                    bandwidth = bandwidth,
                    localEndpointDeviceId = localEndpointDeviceId,
                    localEndpointSiteDescription = localEndpointSiteDescription,
                    remoteEndpointDeviceId = remoteEndpointDeviceId,
                    remoteEndpointSiteDescription = remoteEndpointSiteDescription,
                    underlyingAccessId = underlyingAccessId,
                    notes = notes
                )
                val updated = current.copy(
                    wanVpnConnections = current.wanVpnConnections + newConn,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Connessione WAN/VPN '$name' salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore connessione WAN/VPN: ${e.message}"
            }
        }
    }

    fun addVideoSurveillanceMapping(
        projectId: String,
        cameraDeviceId: String,
        managerDeviceId: String? = null,
        externalManagerDescription: String? = null,
        channelNumber: Int? = null,
        streamUrl: String? = null,
        resolution: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio mappa videosorveglianza..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newMap = com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping(
                    id = UUID.randomUUID().toString(),
                    cameraDeviceId = cameraDeviceId,
                    managerDeviceId = managerDeviceId,
                    externalManagerDescription = externalManagerDescription,
                    channelNumber = channelNumber,
                    streamUrl = streamUrl,
                    resolution = resolution,
                    notes = notes
                )
                val updated = current.copy(
                    videoSurveillanceMappings = current.videoSurveillanceMappings + newMap,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Mappa videosorveglianza salvata"
            } catch (e: Exception) {
                _saveState.value = "Errore videosorveglianza: ${e.message}"
            }
        }
    }

    fun addCustomExtraField(
        projectId: String,
        targetType: String,
        targetId: String,
        fieldKey: String,
        fieldValue: String,
        fieldType: com.onlyfield.assetmanager.core.model.CustomFieldType = com.onlyfield.assetmanager.core.model.CustomFieldType.STRING,
        classification: com.onlyfield.assetmanager.core.model.AttachmentClassification = com.onlyfield.assetmanager.core.model.AttachmentClassification.SHAREABLE,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = "Salvataggio campo extra..."
            try {
                val current = repository.getProjectById(projectId) ?: return@launch
                val newField = com.onlyfield.assetmanager.core.model.CustomExtraField(
                    id = UUID.randomUUID().toString(),
                    targetType = targetType,
                    targetId = targetId,
                    fieldKey = fieldKey,
                    fieldValue = fieldValue,
                    fieldType = fieldType,
                    classification = classification,
                    notes = notes
                )
                val updated = current.copy(
                    customExtraFields = current.customExtraFields + newField,
                    updatedEpochMs = System.currentTimeMillis()
                )
                repository.saveProject(updated)
                _currentProject.value = updated
                _saveState.value = "Campo extra '$fieldKey' salvato"
            } catch (e: Exception) {
                _saveState.value = "Errore campo extra: ${e.message}"
            }
        }
    }

    fun traceCableChain(project: Project, startPortId: String): List<com.onlyfield.assetmanager.data.repository.ChainStep> {
        return repository.traceCableChain(project, startPortId)
    }

    private val _trashItems = MutableStateFlow<List<com.onlyfield.assetmanager.core.model.TrashItem>>(emptyList())
    val trashItems: StateFlow<List<com.onlyfield.assetmanager.core.model.TrashItem>> = _trashItems.asStateFlow()

    private val _canUndoState = MutableStateFlow(false)
    val canUndoState: StateFlow<Boolean> = _canUndoState.asStateFlow()

    fun refreshTrashItems(projectId: String) {
        viewModelScope.launch {
            _trashItems.value = repository.getTrashItems(projectId)
            _canUndoState.value = repository.canUndo()
        }
    }

    fun moveToTrash(projectId: String, itemType: String, itemId: String) {
        viewModelScope.launch {
            _saveState.value = "Spostamento nel cestino..."
            try {
                repository.moveToTrash(projectId, itemType, itemId)
                loadProject(projectId)
                refreshTrashItems(projectId)
                _saveState.value = "Elemento spostato nel cestino"
            } catch (e: Exception) {
                _saveState.value = "Errore cestino: ${e.message}"
            }
        }
    }

    fun restoreFromTrash(projectId: String, trashId: String) {
        viewModelScope.launch {
            _saveState.value = "Ripristino dal cestino..."
            try {
                repository.restoreFromTrash(projectId, trashId)
                loadProject(projectId)
                refreshTrashItems(projectId)
                _saveState.value = "Elemento ripristinato dal cestino"
            } catch (e: Exception) {
                _saveState.value = "Errore ripristino: ${e.message}"
            }
        }
    }

    fun emptyTrash(projectId: String) {
        viewModelScope.launch {
            _saveState.value = "Svuotamento cestino..."
            try {
                repository.emptyTrash(projectId)
                refreshTrashItems(projectId)
                _saveState.value = "Cestino svuotato"
            } catch (e: Exception) {
                _saveState.value = "Errore svuotamento cestino: ${e.message}"
            }
        }
    }

    fun undoLastAction(projectId: String) {
        viewModelScope.launch {
            _saveState.value = "Annullamento..."
            try {
                repository.performUndo()
                loadProject(projectId)
                refreshTrashItems(projectId)
                _saveState.value = "Operazione annullata"
            } catch (e: Exception) {
                _saveState.value = "Errore annullamento: ${e.message}"
            }
        }
    }

    fun replaceDevice(projectId: String, oldDeviceId: String, newName: String, category: DeviceCategory) {
        viewModelScope.launch {
            _saveState.value = "Sostituzione apparato in corso..."
            try {
                val (trash, newDev) = repository.replaceDevice(projectId, oldDeviceId, newName, category)
                loadProject(projectId)
                refreshTrashItems(projectId)
                _saveState.value = "Apparato sostituito con nuovo '${newDev.technicalName}'"
            } catch (e: Exception) {
                _saveState.value = "Errore sostituzione: ${e.message}"
            }
        }
    }

    fun mergeDevices(projectId: String, survivingDeviceId: String, duplicateDeviceId: String, choices: com.onlyfield.assetmanager.core.model.MergeDataChoices) {
        viewModelScope.launch {
            _saveState.value = "Fusione apparati in corso..."
            try {
                val merged = repository.mergeDevices(projectId, survivingDeviceId, duplicateDeviceId, choices)
                loadProject(projectId)
                refreshTrashItems(projectId)
                if (merged != null) {
                    _saveState.value = "Apparati fusi in '${merged.technicalName}'"
                } else {
                    _saveState.value = "Errore fusione: apparati non validi"
                }
            } catch (e: Exception) {
                _saveState.value = "Errore fusione: ${e.message}"
            }
        }
    }

    fun batchEditDevices(projectId: String, deviceIds: List<String>, changes: com.onlyfield.assetmanager.core.model.BatchDeviceChanges) {
        viewModelScope.launch {
            _saveState.value = "Modifica multipla in corso..."
            try {
                repository.batchEditDevices(projectId, deviceIds, changes)
                loadProject(projectId)
                _saveState.value = "Modificati ${deviceIds.size} apparati"
            } catch (e: Exception) {
                _saveState.value = "Errore modifica multipla: ${e.message}"
            }
        }
    }

    fun clearPendingImport() {
        _pendingImportEvaluation.value = null
        _importErrorMessage.value = null
        _passwordPromptForImport.value = false
    }

    fun clearExportStatus() {
        _exportStatusMessage.value = null
    }
}
