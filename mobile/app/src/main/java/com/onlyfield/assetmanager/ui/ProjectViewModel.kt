package com.onlyfield.assetmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Project
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
    private val repository: ProjectRepository
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

    fun exportProjectToStream(projectId: String, outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val success = repository.exportProjectPackageToStream(projectId, outputStream)
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

    fun evaluateImportFromStream(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                val evaluation = repository.evaluateImportPackage(inputStream, _currentProject.value?.id)
                if (evaluation.comparison != null && evaluation.importResult.pkg != null) {
                    _pendingImportEvaluation.value = evaluation
                    _importErrorMessage.value = null
                } else {
                    val issuesStr = evaluation.importResult.validationResult.issues.joinToString("\n") { "${it.code}: ${it.message}" }
                    _importErrorMessage.value = if (issuesStr.isNotBlank()) issuesStr else "Pacchetto non valido o corrotto"
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

    fun clearPendingImport() {
        _pendingImportEvaluation.value = null
        _importErrorMessage.value = null
    }

    fun clearExportStatus() {
        _exportStatusMessage.value = null
    }
}
