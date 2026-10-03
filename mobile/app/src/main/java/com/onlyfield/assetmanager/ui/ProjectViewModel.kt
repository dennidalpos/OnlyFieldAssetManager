package com.onlyfield.assetmanager.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.core.validation.ValidationIssue
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.repository.PackageImportEvaluation
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

/** A short message for the snackbar, optionally with an "Annulla" action. */
data class UiMessage(val text: String, val undo: (() -> Unit)? = null, val isError: Boolean = false)

/** State of an in-progress import of a .ofam package. */
sealed interface ImportState {
    data class NeedsPassword(val uri: Uri, val wrongPassword: Boolean = false) : ImportState
    data class Review(val evaluation: PackageImportEvaluation) : ImportState
}

/**
 * Single ViewModel of the app. Every project change goes through [edit], which applies a pure
 * transformation (see [ProjectEdits]), saves the whole project and offers a one-step undo.
 */
class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _issues = MutableStateFlow<List<ValidationIssue>>(emptyList())
    val issues: StateFlow<List<ValidationIssue>> = _issues.asStateFlow()

    private val _trash = MutableStateFlow<List<TrashItem>>(emptyList())
    val trash: StateFlow<List<TrashItem>> = _trash.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    private val _importState = MutableStateFlow<ImportState?>(null)
    val importState: StateFlow<ImportState?> = _importState.asStateFlow()

    var busy by mutableStateOf<String?>(null)
        private set

    /** Navigation back stack; it lives here so it survives rotation. */
    val backStack = mutableStateListOf<Screen>(Screen.Projects)
    val currentScreen: Screen get() = backStack.last()

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    /** Returns false when already at the root, so the activity can close. */
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        val leaving = backStack.removeAt(backStack.lastIndex)
        if (leaving == Screen.Home) closeProject()
        return true
    }

    private fun notify(text: String, undo: (() -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, undo))
    }

    private fun fail(text: String, e: Throwable? = null) {
        _messages.tryEmit(UiMessage(if (e?.message != null) "$text (${e.message})" else text, isError = true))
    }

    private fun setProject(p: Project?) {
        _project.value = p
        _issues.value = p?.let { ModelValidator.validateProject(it).issues } ?: emptyList()
    }

    // --- Projects ------------------------------------------------------------------------------

    fun openProject(projectId: String) {
        viewModelScope.launch {
            val p = repository.getProjectById(projectId)
            if (p == null) {
                fail("Progetto non trovato.")
                return@launch
            }
            setProject(p)
            refreshTrash()
            backStack.clear()
            backStack.addAll(listOf(Screen.Projects, Screen.Home))
        }
    }

    fun openProtectedProject(projectId: String, password: String, onWrongPassword: () -> Unit) {
        viewModelScope.launch {
            if (repository.verifyProjectPassword(projectId, password)) openProject(projectId) else onWrongPassword()
        }
    }

    fun closeProject() {
        setProject(null)
        _trash.value = emptyList()
        backStack.clear()
        backStack.add(Screen.Projects)
    }

    fun createProject(name: String, description: String, businessUnit: String, area: String) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val p = Project(
                    name = name.trim(),
                    description = description.trim().ifBlank { null },
                    createdEpochMs = now,
                    updatedEpochMs = now,
                    businessUnits = listOf(
                        BusinessUnit(
                            name = businessUnit.trim().ifBlank { "Sede principale" },
                            areas = area.trim().takeIf { it.isNotEmpty() }?.let { listOf(Area(name = it)) } ?: emptyList()
                        )
                    )
                )
                repository.saveProject(p)
                openProject(p.id)
                notify("Progetto «${p.name}» creato.")
            } catch (e: Exception) {
                fail("Impossibile creare il progetto", e)
            }
        }
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newName.trim())
            if (_project.value?.id == projectId) setProject(repository.getProjectById(projectId))
            notify("Progetto rinominato.")
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProject(projectId)
                if (_project.value?.id == projectId) closeProject()
                notify("Progetto eliminato.")
            } catch (e: Exception) {
                fail("Eliminazione non riuscita", e)
            }
        }
    }

    // --- Editing -------------------------------------------------------------------------------

    /** Applies [transform] to the open project, saves it and shows [message] with an undo action. */
    fun edit(message: String, transform: (Project) -> Project) {
        val before = _project.value ?: return
        val after = try {
            transform(before)
        } catch (e: Exception) {
            fail("Modifica non applicata", e)
            return
        }
        if (after == before) return
        val saved = after.copy(updatedEpochMs = System.currentTimeMillis())
        setProject(saved)
        viewModelScope.launch {
            try {
                repository.saveProject(saved)
                notify(message, undo = { restoreSnapshot(before) })
            } catch (e: Exception) {
                setProject(before)
                fail("Salvataggio non riuscito", e)
            }
        }
    }

    private fun restoreSnapshot(snapshot: Project) {
        viewModelScope.launch {
            try {
                repository.saveProject(snapshot)
                setProject(snapshot)
                notify("Modifica annullata.")
            } catch (e: Exception) {
                fail("Impossibile annullare", e)
            }
        }
    }

    private suspend fun reload() {
        val id = _project.value?.id ?: return
        setProject(repository.getProjectById(id))
        refreshTrash()
    }

    // --- Trash and device operations (stored in the database trash table) ----------------------

    private suspend fun refreshTrash() {
        val id = _project.value?.id ?: return
        _trash.value = repository.getTrashItems(id)
    }

    fun moveToTrash(itemType: String, itemId: String, name: String) {
        val projectId = _project.value?.id ?: return
        viewModelScope.launch {
            try {
                val item = repository.moveToTrash(projectId, itemType, itemId)
                reload()
                if (item != null) notify("«$name» spostato nel cestino.", undo = { restoreFromTrash(item.id) })
            } catch (e: Exception) {
                fail("Impossibile spostare nel cestino", e)
            }
        }
    }

    fun restoreFromTrash(trashId: String) {
        val projectId = _project.value?.id ?: return
        viewModelScope.launch {
            try {
                repository.restoreFromTrash(projectId, trashId)
                reload()
                notify("Elemento ripristinato.")
            } catch (e: Exception) {
                fail("Ripristino non riuscito", e)
            }
        }
    }

    fun deleteFromTrash(trashId: String) {
        viewModelScope.launch {
            repository.deleteTrashItemPermanently(trashId)
            refreshTrash()
            notify("Elemento eliminato definitivamente.")
        }
    }

    fun emptyTrash() {
        val projectId = _project.value?.id ?: return
        viewModelScope.launch {
            repository.emptyTrash(projectId)
            refreshTrash()
            notify("Cestino svuotato.")
        }
    }

    fun replaceDevice(oldDeviceId: String, newName: String, category: DeviceCategory) {
        val projectId = _project.value?.id ?: return
        viewModelScope.launch {
            try {
                val (_, newDevice) = repository.replaceDevice(projectId, oldDeviceId, newName, category)
                reload()
                notify("Sostituito con «${newDevice.technicalName}».")
            } catch (e: Exception) {
                fail("Sostituzione non riuscita", e)
            }
        }
    }

    fun mergeDevices(survivorId: String, duplicateId: String, choices: MergeDataChoices) {
        val projectId = _project.value?.id ?: return
        viewModelScope.launch {
            try {
                val merged = repository.mergeDevices(projectId, survivorId, duplicateId, choices)
                reload()
                if (merged != null) notify("Apparati uniti in «${merged.technicalName}».") else fail("Apparati non validi per l'unione.")
            } catch (e: Exception) {
                fail("Unione non riuscita", e)
            }
        }
    }

    // --- Password --------------------------------------------------------------------------------

    fun changePassword(current: String, newPassword: String, onResult: (String?) -> Unit) {
        val p = _project.value ?: return
        viewModelScope.launch {
            val ok = if (newPassword.isEmpty()) repository.removeProjectPassword(p.id, current)
            else repository.setProjectPassword(p.id, current.ifEmpty { null }, newPassword)
            if (!ok) {
                onResult("La password attuale non è corretta.")
                return@launch
            }
            setProject(repository.getProjectById(p.id))
            onResult(null)
            notify(if (newPassword.isEmpty()) "Protezione con password rimossa." else "Password impostata.")
        }
    }

    // --- Exchange (.ofam) ------------------------------------------------------------------------

    fun exportPackage(resolver: ContentResolver, uri: Uri, password: String?) {
        val p = _project.value ?: return
        viewModelScope.launch {
            busy = "Esportazione in corso…"
            try {
                if (p.isPasswordProtected && (password == null || !repository.verifyProjectPassword(p.id, password))) {
                    fail("Password errata: esportazione annullata.")
                    return@launch
                }
                val bytes = repository.exportProjectPackage(p.id, password) ?: error("progetto non trovato")
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("destinazione non scrivibile")
                val missing = repository.missingAttachments(p)
                if (missing.isEmpty()) notify(if (password != null) "Pacchetto cifrato esportato." else "Pacchetto esportato.")
                else fail("Pacchetto esportato senza i file di ${missing.size} allegati non presenti sul dispositivo.")
            } catch (e: Exception) {
                fail("Esportazione non riuscita", e)
            } finally {
                busy = null
            }
        }
    }

    fun startImport(resolver: ContentResolver, uri: Uri, password: String? = null) {
        viewModelScope.launch {
            busy = "Lettura del pacchetto…"
            try {
                val stream = resolver.openInputStream(uri) ?: error("file non leggibile")
                val evaluation = repository.evaluateImportPackage(stream, password, _project.value?.id)
                val codes = evaluation.importResult.validationResult.issues.map { it.code }
                when {
                    "PASSWORD_REQUIRED" in codes || "INVALID_PACKAGE_PASSWORD" in codes ->
                        _importState.value = ImportState.NeedsPassword(uri, wrongPassword = password != null)
                    evaluation.importResult.pkg != null && evaluation.comparison != null ->
                        _importState.value = ImportState.Review(evaluation)
                    else -> {
                        _importState.value = null
                        val reasons = evaluation.importResult.validationResult.issues.joinToString("; ") { it.message }
                        fail("Il file non è un pacchetto valido" + if (reasons.isNotBlank()) ": $reasons" else ".")
                    }
                }
            } catch (e: Exception) {
                _importState.value = null
                fail("Impossibile leggere il pacchetto", e)
            } finally {
                busy = null
            }
        }
    }

    fun confirmImport() {
        val review = _importState.value as? ImportState.Review ?: return
        val pkg = review.evaluation.importResult.pkg ?: return
        _importState.value = null
        viewModelScope.launch {
            try {
                repository.importProjectPackage(pkg)
                openProject(pkg.project.id)
                notify("Importato «${pkg.project.name}».")
            } catch (e: Exception) {
                fail("Importazione non riuscita", e)
            }
        }
    }

    fun cancelImport() {
        _importState.value = null
    }

    // --- Documents -------------------------------------------------------------------------------

    private fun writeDocument(resolver: ContentResolver, uri: Uri, label: String, block: suspend (String, OutputStream) -> Boolean) {
        val p = _project.value ?: return
        viewModelScope.launch {
            busy = "Generazione $label…"
            try {
                val ok = resolver.openOutputStream(uri)?.use { block(p.id, it) } ?: false
                if (ok) notify("$label salvato.") else fail("Generazione $label non riuscita.")
            } catch (e: Exception) {
                fail("Generazione $label non riuscita", e)
            } finally {
                busy = null
            }
        }
    }

    fun exportPdf(resolver: ContentResolver, uri: Uri, filter: ExportFilterConfig, selection: ReportSelection) =
        writeDocument(resolver, uri, "report PDF") { id, out -> repository.exportCompositePdfToStream(id, filter, selection, out) }

    fun exportXlsx(resolver: ContentResolver, uri: Uri, filter: ExportFilterConfig) =
        writeDocument(resolver, uri, "foglio Excel") { id, out -> repository.exportXlsxToStream(id, filter, out) }

    fun exportMarkdown(resolver: ContentResolver, uri: Uri, filter: ExportFilterConfig) =
        writeDocument(resolver, uri, "documento Markdown") { id, out -> repository.exportMarkdownToStream(id, filter, out) }

    fun exportRackPdf(resolver: ContentResolver, uri: Uri, rackId: String) =
        writeDocument(resolver, uri, "scheda rack") { id, out -> repository.exportRackPdfToStream(id, rackId, out) }

    fun print(context: Context, filter: ExportFilterConfig, selection: ReportSelection) {
        val p = _project.value ?: return
        viewModelScope.launch {
            val ok = try {
                repository.printProjectDocument(context, p.id, filter, selection)
            } catch (e: Exception) {
                false
            }
            if (!ok) fail("Servizio di stampa non disponibile.")
        }
    }

    fun attachmentFile(attachment: Attachment): File? = _project.value?.let { repository.attachmentFile(it.id, attachment) }

    // --- Attachments -----------------------------------------------------------------------------

    /** Copies the picked file into app storage and registers it as an attachment. */
    fun addAttachment(context: Context, uri: Uri, name: String, classification: AttachmentClassification) {
        val p = _project.value ?: return
        viewModelScope.launch {
            busy = "Copia del file…"
            try {
                val resolver = context.contentResolver
                val original = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                } ?: "allegato"
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val attachment = Attachment(
                    name = name.trim().ifBlank { original.substringBeforeLast('.') },
                    originalFileName = original,
                    fileType = when {
                        mime.startsWith("image/") -> AttachmentType.IMAGE
                        mime == "application/pdf" -> AttachmentType.PDF
                        mime.startsWith("text/") || mime.contains("document") || mime.contains("sheet") -> AttachmentType.DOCUMENT
                        else -> AttachmentType.OTHER
                    },
                    mimeType = mime,
                    relativePath = "",
                    classification = classification
                )
                val root = repository.attachmentsRoot() ?: error("archivio allegati non disponibile")
                val target = AttachmentFiles.localFile(root, p.id, attachment)
                target.parentFile?.mkdirs()
                resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
                    ?: error("file non leggibile")
                val saved = attachment.copy(relativePath = AttachmentFiles.entryName(attachment))
                edit("Allegato «${saved.name}» aggiunto.") { ProjectEdits.addAttachment(it, saved) }
            } catch (e: Exception) {
                fail("Impossibile aggiungere l'allegato", e)
            } finally {
                busy = null
            }
        }
    }
}
