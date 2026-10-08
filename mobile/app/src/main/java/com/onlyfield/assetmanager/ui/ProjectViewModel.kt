package com.onlyfield.assetmanager.ui

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.i18n.AppLanguage

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
import com.onlyfield.assetmanager.cartography.CartographicMapManager
import com.onlyfield.assetmanager.cartography.MapSnapshotRequest
import com.onlyfield.assetmanager.cartography.MapSnapshotResult
import com.onlyfield.assetmanager.cartography.OfflineMapException
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.OutputStream

/** Snackbar message with an optional undo. */
data class UiMessage(val text: String, val undo: (() -> Unit)? = null, val isError: Boolean = false)

/** Pending `.ofam` import. */
sealed interface ImportState {
    data class NeedsPassword(val uri: Uri, val wrongPassword: Boolean = false) : ImportState
    data class Review(val evaluation: PackageImportEvaluation) : ImportState
    /** Merge conflicts resolved one at a time. */
    data class Merging(val pkg: ProjectPackage, val result: MergeResult, val choices: Map<MergeKey, MergeSide> = emptyMap()) : ImportState {
        val current: MergeConflict? get() = result.conflicts.getOrNull(choices.size)
    }
}

/** App state; [edit] saves each change and supports one undo. */
class ProjectViewModel internal constructor(
    private val repository: ProjectRepository,
    private val acquireMapSnapshot: (MapSnapshotRequest, Messages) -> MapSnapshotResult,
) : ViewModel() {
    constructor(repository: ProjectRepository) : this(repository, { request, i18n -> CartographicMapManager.acquireMapSnapshot(request, i18n) })

    var language by mutableStateOf(AppLanguage.SYSTEM)
        private set
    val i18n: Messages get() = Messages(language.resolve())
    var saveLanguage: (AppLanguage) -> Boolean = { true }

    fun changeLanguage(value: AppLanguage) {
        if (!saveLanguage(value)) { fail(i18n.text("language.saveFailed")); return }
        language = value
        _issues.value = _project.value?.let { ModelValidator.validateProject(it, i18n = i18n).issues } ?: emptyList()
    }

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
    private var importPassword: String? = null
    private var importJob: kotlinx.coroutines.Job? = null
    private var importRequest = 0L

    val mapUiState = com.onlyfield.assetmanager.configurator.map.MapUiState()
    var selectedSiteId by mutableStateOf<String?>(null)
    var selectedAreaId by mutableStateOf<String?>(null)
    /** Ids opened from the project search, most recent first (session only). */
    var recentSearch by mutableStateOf<List<String>>(emptyList())
    /** "Other modules" opened: unused optional modules are listed too (session only). */
    var showSecondary by mutableStateOf(false)

    private var operationBusy by mutableStateOf<String?>(null)
    private var pendingCommands by mutableStateOf(0)
    val busy: String? get() = operationBusy ?: if (pendingCommands > 0) i18n.text("text.e02d15067dea") else null
    private val commands = Mutex()
    private var session = 0L
    private var commandSession: Long? = null
    private var revision = 0L
    private var undoSnapshot: Project? = null
    private val mediaCleanup = mutableSetOf<String>()

    init {
        launchCommand { repository.recoverAll(i18n).forEach { fail(it) } }
    }

    /** One command owns persistence; navigation invalidates only its UI publication. */
    private fun launchCommand(block: suspend CoroutineScope.() -> Unit): kotlinx.coroutines.Job {
        val requestedSession = session
        pendingCommands++
        return viewModelScope.launch {
            try {
                commands.withLock {
                    commandSession = requestedSession
                    try {
                        block()
                        _project.value?.id?.let(mediaCleanup::add)
                        for (id in mediaCleanup.toList()) {
                            val recoverable = undoSnapshot?.takeIf { it.id == id }?.attachments.orEmpty() +
                                listOfNotNull(pendingPhoto?.takeIf { it.projectId == id }?.attachment)
                            repository.collectMedia(id, recoverable)
                            mediaCleanup.remove(id)
                        }
                    } finally { commandSession = null; operationBusy = null }
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { fail(i18n.text("text.9b6ca71eb272"), e) }
        }.also { job -> job.invokeOnCompletion { pendingCommands-- } }
    }

    /** Navigation retained across rotation. */
    val backStack = mutableStateListOf<Screen>(Screen.Projects)
    val currentScreen: Screen get() = backStack.last()

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    /** Bottom-bar tab: drops everything above the map, then opens [screen] (the map itself stays). */
    fun openTab(screen: Screen) {
        val home = backStack.indexOf(Screen.Home)
        if (home < 0) return
        while (backStack.lastIndex > home) backStack.removeAt(backStack.lastIndex)
        if (screen != Screen.Home) backStack.add(screen)
    }

    /** Returns false at the root. */
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
        if (commandSession != null && commandSession != session) return
        revision++
        undoSnapshot = null
        _project.value?.id?.let(mediaCleanup::add)
        p?.id?.let(mediaCleanup::add)
        _project.value = p
        _issues.value = p?.let { ModelValidator.validateProject(it, i18n = i18n).issues } ?: emptyList()
    }


    fun openProject(projectId: String) {
        session++
        launchCommand {
            val p = repository.getProjectById(projectId)
            if (commandSession != session) return@launchCommand
            if (p == null) {
                fail(i18n.text("text.05065b58f085"))
                return@launchCommand
            }
            mapUiState.clear()
            selectedSiteId = null
            selectedAreaId = null
            setProject(p)
            refreshTrash()
            if (commandSession != session) return@launchCommand
            backStack.clear()
            backStack.addAll(listOf(Screen.Projects, Screen.Home))
        }
    }

    fun openProtectedProject(projectId: String, password: String, onWrongPassword: () -> Unit) {
        launchCommand {
            val verified = repository.verifyProjectPassword(projectId, password)
            if (commandSession == session) { if (verified) openProject(projectId) else onWrongPassword() }
        }
    }

    fun closeProject() {
        _project.value?.id?.let(mediaCleanup::add)
        undoSnapshot = null
        session++
        revision++
        _project.value = null
        _issues.value = emptyList()
        _trash.value = emptyList()
        backStack.clear()
        backStack.add(Screen.Projects)
        launchCommand { }
    }

    /** Wizard state retained across rotation. */
    var newSite by mutableStateOf(NewSiteWizard())

    fun startNewSite() {
        newSite = NewSiteWizard()
        navigate(Screen.NewSite)
    }

    fun finishNewSite() {
        val wizard = newSite
        if (!wizard.canProceed) return
        launchCommand {
            operationBusy = i18n.text("text.e02d15067dea")
            try {
                val p = wizard.buildProject()
                repository.saveProject(p)
                wizard.password?.let { repository.setProjectPassword(p.id, currentPassword = null, newPassword = it) }
                if (commandSession == session) openProject(p.id)
                notify(i18n.text("text.b0656bad125d", p.name))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fail(i18n.text("text.cae20309d41d"), e)
            } finally {
                operationBusy = null
            }
        }
    }

    fun renameProject(projectId: String, newName: String) {
        launchCommand {
            repository.renameProject(projectId, newName.trim())
            if (_project.value?.id == projectId) setProject(repository.getProjectById(projectId))
            notify(i18n.text("text.7aec8adfe336"))
        }
    }

    fun deleteProject(projectId: String) {
        launchCommand {
            try {
                repository.deleteProject(projectId)
                if (_project.value?.id == projectId) closeProject()
                notify(i18n.text("text.070c3a74ee96"))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fail(i18n.text("text.5d8f97c003c8"), e)
            }
        }
    }


    /** Applies and saves [transform], then offers undo. */
    fun editMap(updated: Project, message: String) {
        val expected = _project.value ?: return
        edit(message) { current ->
            require(current.id == updated.id)
            require(current == expected) { "The map changed while this edit was pending" }
            updated
        }
    }

    fun edit(message: String, transform: (Project) -> Project) {
        edit(message, {}, transform)
    }

    /** Reports the save outcome only to the originating project session. */
    fun edit(message: String, onResult: (String?) -> Unit, transform: (Project) -> Project) {
        val projectId = _project.value?.id ?: return
        launchCommand {
            val result = try {
                val before = repository.getProjectById(projectId) ?: error(i18n.text("text.05065b58f085"))
                val after = transform(before)
                require(after.id == projectId)
                if (after != before) {
                    val saved = after.copy(updatedEpochMs = System.currentTimeMillis())
                    repository.saveProject(saved)
                    coroutineContext.ensureActive()
                    setProject(saved)
                    if (commandSession == session) notify(message, undo = snapshotUndo(before))
                }
                null
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val error = i18n.text("text.9b6ca71eb272") + (e.message?.let { " ($it)" } ?: "")
                if (commandSession == session) fail(error)
                error
            }
            coroutineContext.ensureActive()
            if (commandSession == session && _project.value?.id == projectId) onResult(result)
        }
    }

    private fun snapshotUndo(snapshot: Project): () -> Unit {
        undoSnapshot = snapshot
        val expectedSession = session
        val expectedRevision = revision
        return { restoreSnapshot(snapshot, expectedSession, expectedRevision) }
    }

    private fun restoreSnapshot(snapshot: Project, expectedSession: Long, expectedRevision: Long) {
        launchCommand {
            try {
                if (session != expectedSession || revision != expectedRevision || _project.value?.id != snapshot.id) return@launchCommand
                repository.saveProject(snapshot)
                setProject(snapshot)
                notify(i18n.text("text.1e7b4fb4b88a"))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fail(i18n.text("text.80eba573acd4"), e)
            }
        }
    }

    private suspend fun reload(id: String) {
        if (commandSession != session || _project.value?.id != id) return
        setProject(repository.getProjectById(id))
        refreshTrash()
    }


    private suspend fun refreshTrash() {
        val id = _project.value?.id ?: return
        val items = repository.getTrashItems(id)
        if (commandSession == session && _project.value?.id == id) _trash.value = items
    }

    fun moveToTrash(itemType: String, itemId: String, name: String) {
        val projectId = _project.value?.id ?: return
        val requestedSession = session
        launchCommand {
            try {
                val item = repository.moveToTrash(projectId, itemType, itemId, i18n = i18n)
                coroutineContext.ensureActive()
                reload(projectId)
                coroutineContext.ensureActive()
                if (item != null && commandSession == session && _project.value?.id == projectId) notify(i18n.text("text.4e2629d50c9b", name), undo = {
                    if (session == requestedSession && _project.value?.id == projectId) restoreFromTrash(item.id)
                })
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (commandSession == session && _project.value?.id == projectId) fail(i18n.text("text.ce9e7fe8c973"), e)
            }
        }
    }

    fun restoreFromTrash(trashId: String) {
        val projectId = _project.value?.id ?: return
        launchCommand {
            try {
                check(repository.restoreFromTrash(projectId, trashId, i18n = i18n)) { i18n.text("trash.invalidEntry") }
                reload(projectId)
                notify(i18n.text("text.5d47acb0a3de"))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fail(i18n.text("text.23d93971ed41"), e)
            }
        }
    }

    fun deleteFromTrash(trashId: String) {
        launchCommand {
            repository.deleteTrashItemPermanently(trashId)
            _project.value?.id?.let { reload(it) }
            notify(i18n.text("text.af6b7821ed8a"))
        }
    }

    fun emptyTrash() {
        val projectId = _project.value?.id ?: return
        launchCommand {
            repository.emptyTrash(projectId)
            reload(projectId)
            notify(i18n.text("text.9259b5bf717b"))
        }
    }

    fun replaceDevice(oldDeviceId: String, newName: String, category: DeviceCategory) {
        replaceDevice(oldDeviceId, newName, category, {})
    }

    fun replaceDevice(oldDeviceId: String, newName: String, category: DeviceCategory, onResult: (String?) -> Unit) {
        val projectId = _project.value?.id ?: return
        launchCommand {
            val result = try {
                val (_, newDevice) = repository.replaceDevice(projectId, oldDeviceId, newName, category, i18n = i18n)
                coroutineContext.ensureActive()
                reload(projectId)
                if (commandSession == session && _project.value?.id == projectId) notify(i18n.text("text.f033a5d0dac5", newDevice.technicalName))
                null
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                i18n.text("text.a86610be9d16") + (e.message?.let { " ($it)" } ?: "")
            }
            publishCommandResult(projectId, result, onResult)
        }
    }

    fun mergeDevices(survivorId: String, duplicateId: String, choices: MergeDataChoices) {
        mergeDevices(survivorId, duplicateId, choices, {})
    }

    fun mergeDevices(survivorId: String, duplicateId: String, choices: MergeDataChoices, onResult: (String?) -> Unit) {
        val projectId = _project.value?.id ?: return
        launchCommand {
            val result = try {
                val merged = repository.mergeDevices(projectId, survivorId, duplicateId, choices, i18n = i18n)
                coroutineContext.ensureActive()
                reload(projectId)
                if (merged == null) i18n.text("text.03f600b15086") else {
                    if (commandSession == session && _project.value?.id == projectId) notify(i18n.text("text.fc7aa94c3fb8", merged.technicalName))
                    null
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                i18n.text("text.932f8500aa16") + (e.message?.let { " ($it)" } ?: "")
            }
            publishCommandResult(projectId, result, onResult)
        }
    }

    /** Publishes only to the active originating session after cleanup. */
    private suspend fun publishCommandResult(projectId: String, result: String?, onResult: (String?) -> Unit) {
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        if (commandSession == session && _project.value?.id == projectId) {
            if (result != null) fail(result)
            onResult(result)
        }
    }


    fun changePassword(current: String, newPassword: String, onResult: (String?) -> Unit) {
        val p = _project.value ?: return
        launchCommand {
            val result = try {
                val ok = if (newPassword.isEmpty()) repository.removeProjectPassword(p.id, current)
                else repository.setProjectPassword(p.id, current.ifEmpty { null }, newPassword)
                if (ok) {
                    setProject(repository.getProjectById(p.id))
                    null
                } else i18n.text("text.0ab6e626d98f")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                fail(i18n.text("text.9b6ca71eb272"), e)
                i18n.text("text.9b6ca71eb272") + (e.message?.let { " ($it)" } ?: "")
            }
            if (commandSession != session || _project.value?.id != p.id) return@launchCommand
            onResult(result)
            if (result == null) notify(if (newPassword.isEmpty()) i18n.text("text.666e96d6fcf0") else i18n.text("text.85a2bd406195"))
        }
    }


    fun exportPackage(resolver: ContentResolver, uri: Uri, password: String?) {
        val p = _project.value ?: return
        launchCommand {
            operationBusy = i18n.text("text.263e01561fa7")
            try {
                val current = repository.getProjectById(p.id) ?: return@launchCommand
                if (current.isPasswordProtected && (password == null || !repository.verifyProjectPassword(p.id, password))) {
                    fail(i18n.text("text.ec4889ca63e4"))
                    return@launchCommand
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { output ->
                        check(repository.exportProjectPackageToStream(p.id, output, password, i18n = i18n)) { i18n.text("text.8ad65d90f29b") }
                    } ?: error(i18n.text("text.f9b9a0075030"))
                }
                val missing = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.missingAttachments(current) }
                if (missing.isEmpty()) notify(if (password != null) i18n.text("text.b73d46f629a2") else i18n.text("text.5814b1d61f28"))
                else fail(i18n.text("text.56a129cb0f6c", missing.size))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fail(i18n.text("text.ad15389bdede"), e)
            } finally {
                operationBusy = null
            }
        }
    }

    fun startImport(resolver: ContentResolver, uri: Uri, password: String? = null) {
        cancelImport()
        val request = importRequest
        val messages = i18n
        importJob = launchCommand {
            operationBusy = i18n.text("text.e73a6312c02c")
            var pending: PackageImportEvaluation? = null
            try {
                val evaluation = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val stream = resolver.openInputStream(uri) ?: error(messages.text("text.fb0c10a218fc"))
                    stream.use { repository.evaluateImportPackage(it, password, i18n = messages) }
                        .also { pending = it }
                }
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                if (request != importRequest) return@launchCommand
                val codes = evaluation.importResult.validationResult.issues.map { it.code }
                when {
                    "PASSWORD_REQUIRED" in codes || "INVALID_PACKAGE_PASSWORD" in codes ->
                        _importState.value = ImportState.NeedsPassword(uri, wrongPassword = password != null)
                    evaluation.importResult.pkg != null && evaluation.comparison != null -> {
                        importPassword = password
                        _importState.value = ImportState.Review(evaluation)
                        pending = null
                    }
                    else -> {
                        _importState.value = null
                        val reasons = evaluation.importResult.validationResult.issues.joinToString("; ") { it.message }
                        fail(i18n.text("text.d8f9e7b1df7e") + if (reasons.isNotBlank()) ": $reasons" else ".")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (request != importRequest) return@launchCommand
                _importState.value = null
                fail(i18n.text("text.a503d881af39"), e)
            } finally {
                // The worker owns the package until Review accepts it, including cancelled returns.
                pending?.importResult?.pkg?.close()
                operationBusy = null
            }
        }
    }

    fun confirmImport() {
        val review = _importState.value as? ImportState.Review ?: return
        val pkg = review.evaluation.importResult.pkg ?: return
        val password = importPassword
        val request = importRequest
        _importState.value = null
        importPassword = null
        importJob = launchCommand {
            try {
                repository.importProjectPackage(pkg, password)
                if (request == importRequest && commandSession == session) {
                    openProject(pkg.project.id)
                    notify(i18n.text("text.66f7b7ff42d4", pkg.project.name))
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fail(i18n.text("text.7b94ff9ddb10"), e)
            }
        }.also { job -> job.invokeOnCompletion { pkg.close() } }
    }

    fun cancelImport() {
        importRequest++
        importJob?.cancel()
        importJob = null
        when (val state = _importState.value) {
            is ImportState.Review -> state.evaluation.importResult.pkg?.close()
            is ImportState.Merging -> state.pkg.close()
            else -> Unit
        }
        _importState.value = null
        importPassword = null
    }

    override fun onCleared() {
        cancelImport()
        super.onCleared()
    }

    /** Merges a package with the local copy. */
    fun startMerge() {
        val review = _importState.value as? ImportState.Review ?: return
        val pkg = review.evaluation.importResult.pkg ?: return
        val request = importRequest
        var transferred = false
        _importState.value = null
        importPassword = null
        importJob = launchCommand {
            try {
                val local = repository.getProjectById(pkg.project.id) ?: error(i18n.text("text.03d3a4092bb0"))
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    ProjectMerger.merge(repository.getSyncBase(pkg.project.id), local, pkg.project, i18n = i18n)
                }
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                if (request != importRequest) return@launchCommand
                if (result.conflicts.isEmpty()) saveMerge(ImportState.Merging(pkg, result), request)
                else {
                    _importState.value = ImportState.Merging(pkg, result)
                    transferred = true
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (request == importRequest) fail(i18n.text("text.932f8500aa16"), e)
            }
        }.also { job -> job.invokeOnCompletion { if (!transferred) pkg.close() } }
    }

    fun chooseMergeSide(side: MergeSide) {
        val merging = _importState.value as? ImportState.Merging ?: return
        val conflict = merging.current ?: return
        val next = merging.copy(choices = merging.choices + (conflict.key to side))
        if (next.current == null) applyMerge(next) else _importState.value = next
    }

    private fun applyMerge(merging: ImportState.Merging) {
        val request = importRequest
        _importState.value = null
        importJob = launchCommand {
            try {
                saveMerge(merging, request)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fail(i18n.text("text.932f8500aa16"), e)
            }
        }.also { job -> job.invokeOnCompletion { merging.pkg.close() } }
    }

    private suspend fun saveMerge(merging: ImportState.Merging, request: Long) {
        val merged = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { merging.result.resolve(merging.choices, i18n = i18n) }
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        if (request != importRequest) return
        repository.importMergedPackage(merging.pkg, merged)
        if (request == importRequest && commandSession == session) {
            openProject(merging.pkg.project.id)
            notify(i18n.text("text.123fb31b11bf", merging.result.autoApplied, merging.choices.size))
        }
    }


    private fun writeDocument(resolver: ContentResolver, uri: Uri, label: String, block: suspend (String, OutputStream) -> Boolean) {
        val p = _project.value ?: return
        launchCommand {
            operationBusy = i18n.text("text.7e484517449f", label)
            try {
                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { block(p.id, it) } ?: false
                }
                if (ok) notify(i18n.text("text.edbaabf3f213", label)) else fail(i18n.text("text.91981b4324f5", label))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fail(i18n.text("text.41b538bf4cba", label), e)
            } finally {
                operationBusy = null
            }
        }
    }

    fun exportPdf(resolver: ContentResolver, uri: Uri, filter: ExportFilterConfig, selection: ReportSelection) = i18n.let { i18n ->
        writeDocument(resolver, uri, i18n.text("text.76ab48d47754")) { id, out -> repository.exportCompositePdfToStream(id, filter, selection, out, i18n = i18n) }
    }

    fun exportXlsx(resolver: ContentResolver, uri: Uri, filter: ExportFilterConfig) = i18n.let { i18n ->
        writeDocument(resolver, uri, i18n.text("text.b46f77c8367e")) { id, out -> repository.exportXlsxToStream(id, filter, out, i18n = i18n) }
    }

    fun exportMarkdown(resolver: ContentResolver, uri: Uri, filter: ExportFilterConfig) = i18n.let { i18n ->
        writeDocument(resolver, uri, i18n.text("text.6f1caee9d678")) { id, out -> repository.exportMarkdownToStream(id, filter, out, i18n = i18n) }
    }

    /** QR labels for devices, racks and labelled cables. */
    fun exportLabels(resolver: ContentResolver, uri: Uri) = i18n.let { i18n ->
        writeDocument(resolver, uri, i18n.text("text.601ccac1ac2a")) { id, out ->
            repository.getProjectById(id)?.let { LabelSheetPdf.write(LabelSheetPdf.labelsFor(it, i18n = i18n), out); true } ?: false
        }
    }

    fun exportRackPdf(resolver: ContentResolver, uri: Uri, rackId: String) = i18n.let { i18n ->
        writeDocument(resolver, uri, i18n.text("text.38145d73944c")) { id, out -> repository.exportRackPdfToStream(id, rackId, out, i18n = i18n) }
    }

    fun print(context: Context, filter: ExportFilterConfig, selection: ReportSelection) {
        val p = _project.value ?: return
        val i18n = this.i18n
        launchCommand {
            val ok = try {
                repository.printProjectDocument(context, p.id, filter, selection, i18n = i18n)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fail(i18n.text("text.103f48541a86"), e)
                return@launchCommand
            }
            if (!ok) fail(i18n.text("text.103f48541a86"))
        }
    }

    fun attachmentFile(attachment: Attachment): File? = _project.value?.let { repository.attachmentFile(it.id, attachment) }

    fun saveMapObject(context: Context, draft: com.onlyfield.assetmanager.core.forms.MapObjectDraft, photos: List<Uri>, removed: Set<String>, onSaved: () -> Unit) {
        saveMediaEdit(context, photos, draft.targetType, draft.id, AttachmentClassification.SHAREABLE, { p -> draft.apply(p, i18n = i18n).let { it.copy(attachments = it.attachments.filterNot { a -> a.id in removed }) } }) { onSaved() }
    }

    fun importFloorplan(context: Context, uri: Uri, areaId: String, onSaved: (Attachment) -> Unit) {
        saveMediaEdit(context, listOf(uri), AttachmentTargetType.AREA, areaId, AttachmentClassification.SHAREABLE, { it }) { attachments -> onSaved(attachments.single()) }
    }

    private fun saveMediaEdit(context: Context, sources: List<Uri>, type: AttachmentTargetType?, targetId: String?, classification: AttachmentClassification,
        transform: (Project) -> Project, mediaOnly: Boolean = true, title: String? = null, onResult: (String?) -> Unit = {}, onSaved: (List<Attachment>) -> Unit) {
        if (busy != null) return
        val initial = _project.value ?: return
        launchCommand {
            operationBusy = i18n.text("text.c4f57f0165aa")
            val created = mutableListOf<File>()
            var committed = false
            val result = try {
                val attachments = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    sources.map { uri ->
                        val resolver = context.contentResolver
                        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                            ?: if (mediaOnly) "foto.jpg" else "allegato"
                        val mime = resolver.getType(uri) ?: if (mediaOnly) "image/jpeg" else "application/octet-stream"
                        require(!mediaOnly || mime.startsWith("image/") || mime == "application/pdf") { i18n.text("text.2f0112e6d87f") }
                        val att = Attachment(name = title?.trim()?.takeIf { it.isNotBlank() } ?: name.substringBeforeLast('.'), originalFileName = name, relativePath = "", mimeType = mime,
                            fileType = when {
                                mime == "application/pdf" -> AttachmentType.PDF
                                mime.startsWith("image/") -> AttachmentType.IMAGE
                                mime.startsWith("text/") || mime.contains("document") || mime.contains("sheet") -> AttachmentType.DOCUMENT
                                else -> AttachmentType.OTHER
                            }, classification = classification, targetType = type, targetId = targetId)
                        val root = repository.attachmentsRoot() ?: error(i18n.text("text.333abdb13e5a"))
                        val file = AttachmentFiles.localFile(root, initial.id, att)
                        file.parentFile?.mkdirs(); created += file
                        resolver.openInputStream(uri)?.use { input -> file.outputStream().use { AttachmentFiles.copyBounded(input, it, i18n) } } ?: error(i18n.text("text.2af75627a512"))
                        val pages = when (att.fileType) {
                            AttachmentType.PDF -> PlanMedia.pageCount(file, i18n = i18n)
                            AttachmentType.IMAGE -> { PlanMedia.image(file, false, 0, 256, i18n = i18n); 1 }
                            else -> 1
                        }
                        require(pages > 0) { i18n.text("text.4acdb5acf0b0") }
                        att.copy(relativePath = AttachmentFiles.entryName(att), pageCount = pages)
                    }
                }
                val before = _project.value?.takeIf { it.id == initial.id } ?: error(i18n.text("text.2af4c267c11f"))
                val edited = transform(before)
                val saved = edited.copy(attachments = edited.attachments + attachments, updatedEpochMs = System.currentTimeMillis())
                repository.saveMediaProject(saved, i18n) { committed = true }
                coroutineContext.ensureActive()
                if (commandSession == session && _project.value?.id == initial.id) {
                    setProject(saved)
                    notify(i18n.text("text.b1b5983f51f1"), undo = snapshotUndo(before))
                    onSaved(attachments)
                }
                null
            } catch (e: Exception) {
                if (!committed) kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
                    created.forEach { java.nio.file.Files.deleteIfExists(it.toPath()) }
                }
                if (e is kotlinx.coroutines.CancellationException) throw e
                i18n.text("text.9b6ca71eb272") + (e.message?.let { " ($it)" } ?: "")
            } finally { operationBusy = null }
            publishCommandResult(initial.id, result, onResult)
        }
    }


    /** Copies a picked file into app storage. */
    fun addAttachment(context: Context, uri: Uri, name: String, classification: AttachmentClassification) {
        addAttachment(context, uri, name, classification, {})
    }

    fun addAttachment(context: Context, uri: Uri, name: String, classification: AttachmentClassification, onResult: (String?) -> Unit) {
        saveMediaEdit(context, listOf(uri), null, null, classification, { it }, mediaOnly = false, title = name, onResult = onResult, onSaved = {})
    }


    /** Pending camera attachment and target file. */
    private data class PendingPhoto(val projectId: String, val attachment: Attachment, val file: File, val session: Long)
    private var pendingPhoto: PendingPhoto? = null

    /** Creates a photo target for [type]/[targetId]. */
    fun preparePhoto(type: AttachmentTargetType, targetId: String?): File? {
        if (busy != null || pendingPhoto != null) return null
        val p = _project.value ?: return null
        val root = repository.attachmentsRoot() ?: return null
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.ROOT).format(java.util.Date())
        val attachment = Attachment(
            name = i18n.text("text.9f1847b1bb2e", formatPhotoTitle()),
            originalFileName = "foto_$stamp.jpg",
            fileType = AttachmentType.IMAGE,
            mimeType = "image/jpeg",
            relativePath = "",
            targetType = type,
            targetId = targetId,
        )
        val file = AttachmentFiles.localFile(root, p.id, attachment).apply { parentFile?.mkdirs() }
        pendingPhoto = PendingPhoto(p.id, attachment, file, session)
        return file
    }

    /** Continues the series only after validation and persistence succeed. */
    fun onPhotoResult(saved: Boolean, onSaved: (Boolean) -> Unit) {
        val (projectId, attachment, file, requestedSession) = pendingPhoto ?: run { onSaved(false); return }
        pendingPhoto = null
        val available = busy == null && requestedSession == session
        if (available) operationBusy = i18n.text("text.c4f57f0165aa")
        var committed = false
        val job = launchCommand {
            try {
                if (!available || !saved || file.length() == 0L) return@launchCommand
                val before = _project.value?.takeIf { it.id == projectId } ?: return@launchCommand
                val added = attachment.copy(relativePath = AttachmentFiles.entryName(attachment))
                val updated = ProjectEdits.addAttachment(before, added)
                repository.saveMediaProject(updated, i18n) { committed = true }
                coroutineContext.ensureActive()
                if (commandSession == session && _project.value?.id == projectId) {
                    setProject(updated)
                    notify(i18n.text("text.ef0af7808f11"), undo = snapshotUndo(before))
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (requestedSession == session && _project.value?.id == projectId) fail(i18n.text("text.9b6ca71eb272"), e)
            } finally {
                if (!committed) kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
                    java.nio.file.Files.deleteIfExists(file.toPath())
                }
                if (available) operationBusy = null
            }
        }
        viewModelScope.launch {
            job.join()
            if (!job.isCancelled && requestedSession == session && _project.value?.id == projectId) onSaved(committed)
        }
    }

    private fun formatPhotoTitle() = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.ITALY).format(java.util.Date())


    fun notifyError(text: String) = fail(text)

    fun notifyInfo(text: String) = notify(text)


    /** Opens a scanned entity or returns an unknown code. */
    fun openScannedCode(code: String): String? {
        val p = _project.value ?: return null
        return when (val match = CodeLookup.find(ProjectIndex(p), code, i18n = i18n)) {
            is CodeMatch.DeviceMatch -> { navigate(Screen.DeviceDetail(match.device.id)); notify(i18n.text("text.bb7c60d5c6c5", match.field.lowercase(), match.device.technicalName)); null }
            is CodeMatch.PortMatch -> { navigate(Screen.DeviceDetail(match.port.device.id)); notify(i18n.text("text.bea0b67ed98a", match.port.port.name, match.port.device.technicalName)); null }
            is CodeMatch.CableMatch -> { navigate(Screen.Cabling); notify(i18n.text("text.dae31b0efa98", match.cable.codeOrLabel)); null }
            is CodeMatch.RackMatch -> { navigate(Screen.RackDetail(match.rack.id)); null }
            is CodeMatch.OtherProject -> { fail(i18n.text("text.b18a8bb53b19")); null }
            is CodeMatch.NotFound -> match.code
        }
    }



    /** Downloads and stores the requested 3x3 tile map. */
    fun downloadMap(request: MapSnapshotRequest, name: String) {
        downloadMap(request, name, {})
    }

    fun downloadMap(request: MapSnapshotRequest, name: String, onResult: (String?) -> Unit) {
        if (busy != null) return
        val p = _project.value ?: return
        launchCommand {
            operationBusy = i18n.text("text.4618bc8c688e")
            var target: File? = null
            var committed = false
            val result = try {
                val attachment = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val snapshot = acquireMapSnapshot(request, i18n)
                    AttachmentFiles.validateSize(snapshot.imageBytes.size.toLong(), i18n)
                    val attachment = Attachment(
                        name = name.trim().ifBlank { i18n.text("text.67c7c6400b1f", request.centerLatitude, request.centerLongitude) },
                        originalFileName = "mappa_z${request.zoomLevel}.png", fileType = AttachmentType.IMAGE,
                        mimeType = snapshot.mimeType, relativePath = "", attributionText = snapshot.attributionText,
                    )
                    val root = repository.attachmentsRoot() ?: error(i18n.text("text.01d086263d55"))
                    val file = AttachmentFiles.localFile(root, p.id, attachment)
                    target = file
                    java.nio.file.Files.createDirectories(checkNotNull(file.parentFile).toPath())
                    file.writeBytes(snapshot.imageBytes)
                    attachment.copy(relativePath = AttachmentFiles.entryName(attachment))
                }
                val before = _project.value?.takeIf { it.id == p.id } ?: error(i18n.text("text.2af4c267c11f"))
                val updated = ProjectEdits.addAttachment(before, attachment)
                repository.saveMediaProject(updated, i18n) { committed = true }
                coroutineContext.ensureActive()
                if (commandSession == session && _project.value?.id == p.id) {
                    setProject(updated)
                    notify(i18n.text("text.9c32fd33c6a0", attachment.name), undo = snapshotUndo(before))
                }
                null
            } catch (e: OfflineMapException) {
                e.message ?: i18n.text("map.network.android")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                i18n.text("text.d96c472cc255") + (e.message?.let { " ($it)" } ?: "")
            } finally {
                if (!committed) kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
                    target?.let { java.nio.file.Files.deleteIfExists(it.toPath()) }
                }
                operationBusy = null
            }
            publishCommandResult(p.id, result, onResult)
        }
    }


}
