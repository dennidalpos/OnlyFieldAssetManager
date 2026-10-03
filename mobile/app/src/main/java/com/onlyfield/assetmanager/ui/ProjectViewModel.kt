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
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import com.onlyfield.assetmanager.core.scan.CodeLookup
import com.onlyfield.assetmanager.core.scan.CodeMatch
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.core.validation.ValidationIssue
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.repository.PackageImportEvaluation
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.LabelSheetPdf
import com.onlyfield.assetmanager.exchange.MergeConflict
import com.onlyfield.assetmanager.exchange.MergeKey
import com.onlyfield.assetmanager.exchange.MergeResult
import com.onlyfield.assetmanager.exchange.MergeSide
import com.onlyfield.assetmanager.exchange.ProjectMerger
import com.onlyfield.assetmanager.exchange.ProjectPackage
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
    /** Conflicts of a merge, answered one at a time (F04). */
    data class Merging(val pkg: ProjectPackage, val result: MergeResult, val choices: Map<MergeKey, MergeSide> = emptyMap()) : ImportState {
        val current: MergeConflict? get() = result.conflicts.getOrNull(choices.size)
    }
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
        if (currentScreen == Screen.NewSite && !newSite.isFirst) {
            newSite = newSite.back()
            return true
        }
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

    /** State of the "Nuovo sito" wizard; kept here so it survives rotation. */
    var newSite by mutableStateOf(NewSiteWizard())

    fun startNewSite() {
        newSite = NewSiteWizard()
        navigate(Screen.NewSite)
    }

    fun finishNewSite() {
        val wizard = newSite
        if (!wizard.canProceed) return
        viewModelScope.launch {
            busy = "Creazione del progetto…"
            try {
                val p = wizard.buildProject()
                repository.saveProject(p)
                wizard.password?.let { repository.setProjectPassword(p.id, currentPassword = null, newPassword = it) }
                openProject(p.id)
                notify("Progetto «${p.name}» creato.")
            } catch (e: Exception) {
                fail("Impossibile creare il progetto", e)
            } finally {
                busy = null
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

    /** Three-way merge of the package with the local copy; without conflicts it is applied at once. */
    fun startMerge() {
        val review = _importState.value as? ImportState.Review ?: return
        val pkg = review.evaluation.importResult.pkg ?: return
        viewModelScope.launch {
            try {
                val local = repository.getProjectById(pkg.project.id) ?: error("copia locale non trovata")
                val result = ProjectMerger.merge(repository.getSyncBase(pkg.project.id), local, pkg.project)
                if (result.conflicts.isEmpty()) applyMerge(ImportState.Merging(pkg, result))
                else _importState.value = ImportState.Merging(pkg, result)
            } catch (e: Exception) {
                _importState.value = null
                fail("Unione non riuscita", e)
            }
        }
    }

    fun chooseMergeSide(side: MergeSide) {
        val merging = _importState.value as? ImportState.Merging ?: return
        val conflict = merging.current ?: return
        val next = merging.copy(choices = merging.choices + (conflict.key to side))
        if (next.current == null) applyMerge(next) else _importState.value = next
    }

    private fun applyMerge(merging: ImportState.Merging) {
        _importState.value = null
        viewModelScope.launch {
            try {
                repository.importMergedPackage(merging.pkg, merging.result.resolve(merging.choices))
                openProject(merging.pkg.project.id)
                notify("Unione completata: ${merging.result.autoApplied} modifiche dal pacchetto, ${merging.choices.size} conflitti risolti.")
            } catch (e: Exception) {
                fail("Unione non riuscita", e)
            }
        }
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

    /** QR labels of every device, rack and labelled cable (F03). */
    fun exportLabels(resolver: ContentResolver, uri: Uri) =
        writeDocument(resolver, uri, "foglio etichette") { _, out ->
            _project.value?.let { LabelSheetPdf.write(LabelSheetPdf.labelsFor(it), out); true } ?: false
        }

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

    // --- Camera (F01) ---------------------------------------------------------------------------

    /** Photo being taken: the attachment to add and the file the camera app writes. */
    private var pendingPhoto: Pair<Attachment, File>? = null

    /** Creates the target file for a new photo linked to [type]/[targetId]; null without a project. */
    fun preparePhoto(type: AttachmentTargetType, targetId: String?): File? {
        val p = _project.value ?: return null
        val root = repository.attachmentsRoot() ?: return null
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.ROOT).format(java.util.Date())
        val attachment = Attachment(
            name = "Foto ${formatPhotoTitle()}",
            originalFileName = "foto_$stamp.jpg",
            fileType = AttachmentType.IMAGE,
            mimeType = "image/jpeg",
            relativePath = "",
            targetType = type,
            targetId = targetId,
        )
        val file = AttachmentFiles.localFile(root, p.id, attachment).apply { parentFile?.mkdirs() }
        pendingPhoto = attachment to file
        return file
    }

    fun onPhotoResult(saved: Boolean) {
        val (attachment, file) = pendingPhoto ?: return
        pendingPhoto = null
        if (!saved || file.length() == 0L) {
            file.parentFile?.deleteRecursively()
            return
        }
        val added = attachment.copy(relativePath = AttachmentFiles.entryName(attachment))
        edit("Foto aggiunta agli allegati.") { ProjectEdits.addAttachment(it, added) }
    }

    private fun formatPhotoTitle() = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.ITALY).format(java.util.Date())


    fun notifyError(text: String) = fail(text)

    // --- Code scanning (F02) --------------------------------------------------------------------

    /** Opens what a scanned code points to; an unknown code is returned so the UI can offer a new device. */
    fun openScannedCode(code: String): String? {
        val p = _project.value ?: return null
        return when (val match = CodeLookup.find(ProjectIndex(p), code)) {
            is CodeMatch.DeviceMatch -> { navigate(Screen.DeviceDetail(match.device.id)); notify("Trovato per ${match.field.lowercase()}: ${match.device.technicalName}"); null }
            is CodeMatch.PortMatch -> { navigate(Screen.DeviceDetail(match.port.device.id)); notify("Porta ${match.port.port.name} di ${match.port.device.technicalName}"); null }
            is CodeMatch.CableMatch -> { navigate(Screen.Cabling); notify("Cavo ${match.cable.codeOrLabel}"); null }
            is CodeMatch.RackMatch -> { navigate(Screen.RackDetail(match.rack.id)); null }
            is CodeMatch.OtherProject -> { fail("L'etichetta appartiene a un altro progetto."); null }
            is CodeMatch.NotFound -> match.code
        }
    }

}
