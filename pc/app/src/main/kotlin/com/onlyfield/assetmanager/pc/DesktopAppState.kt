package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.i18n.AppLanguage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.configurator.SymbolIcons
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.core.validation.ValidationIssue
import com.onlyfield.assetmanager.core.validation.ValidationSeverity
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageManifest
import com.onlyfield.assetmanager.exchange.ProjectComparison
import com.onlyfield.assetmanager.exchange.ProjectComparisonEvaluator
import com.onlyfield.assetmanager.exchange.ProjectPackage
import com.onlyfield.assetmanager.exchange.MergeConflict
import com.onlyfield.assetmanager.exchange.MergeKey
import com.onlyfield.assetmanager.exchange.MergeResult
import com.onlyfield.assetmanager.exchange.MergeSide
import com.onlyfield.assetmanager.exchange.ProjectMerger
import java.io.File

/** Editor areas in navigation order. */
enum class AppSection(private val titleKey: String, val icon: ImageVector, val needsProject: Boolean = true) {
    INVENTORY("text.a26fdd05a46b", SymbolIcons.inventory2),
    RACKS("text.4cd265c2b8c6", SymbolIcons.dns),
    MODELS("text.7351fc8f354e", SymbolIcons.category),
    FLOORPLANS("text.af06bf3ae6ac", SymbolIcons.map),
    CREDENTIALS("text.52f7e6721e97", SymbolIcons.inventory2),
    MEDIA("text.f68e5b719a18", SymbolIcons.map),
    CABLING("text.3b40d8bd6081", SymbolIcons.cable),
    NETWORK("text.a0dd274e04a0", SymbolIcons.lan),
    POWER("text.acedc1948e5f", SymbolIcons.bolt),
    TRASH("text.9a3a36d5fa15", SymbolIcons.delete),
    PROJECT("text.b7700d71d0ce", SymbolIcons.settings, needsProject = false),;
    val title: String get() = localizedTitle(Messages())
    fun localizedTitle(i18n: Messages): String = i18n.text(titleKey)
}

sealed interface AppDialog {
    data object NewProject : AppDialog
    data class ImportPassword(val file: File, val error: String? = null) : AppDialog
    data class LocalReplacementPassword(val pkg: ProjectPackage, val incomingPassword: String?, val wrongPassword: Boolean = false,
        val compareBeforeReplace: Boolean = false, val warnings: List<ValidationIssue> = emptyList()) : AppDialog
    data object ManagePassword : AppDialog
    data class Compare(val comparison: ProjectComparison, val pkg: ProjectPackage, val password: String?, val warnings: List<ValidationIssue> = emptyList(),
        val localProject: Project? = null, val localPassword: String? = null) : AppDialog
    /** Merge conflicts resolved one at a time. */
    data class Merge(val pkg: ProjectPackage, val result: MergeResult, val choices: Map<MergeKey, MergeSide> = emptyMap(),
        val replaceClosedCopy: Boolean = false, val localPassword: String? = null) : AppDialog {
        val current: MergeConflict? get() = result.conflicts.getOrNull(choices.size)
    }
    data object Documents : AppDialog
    data object Validation : AppDialog
}

/** Desktop state; [update] validates and saves project changes. */
class DesktopAppState(val storage: DesktopStorageManager) {
    var busy by mutableStateOf(false)
        private set
    private val io = DesktopIo { busy = it }
    fun <T> runIo(action: () -> T): T {
        check(!busy || !java.awt.EventQueue.isDispatchThread()) { "An operation is already running" }
        return io.run(action)
    }


    var project by mutableStateOf<Project?>(null)
        private set
    private var password: String? = null
    private var manifest: PackageManifest? = null

    private var trashState by mutableStateOf<List<TrashItem>>(emptyList())

    private var trashBeforeEdit: List<TrashItem>? = null

    /** Persisted trash for the open project. */
    var trash: List<TrashItem>
        get() = trashState
        set(value) {
            val p = project ?: return
            try {
                val removed = trashState.filterNot { item -> value.any { it.id == item.id } }
                val updated = ProjectEdits.purgeTrashAttachments(p, removed)
                runIo { storage.saveProjectLocally(updated, password, trashItems = value) }
                project = updated
                trashState = value
                clearHistory()
                issues = runIo { ModelValidator.validateProject(updated, i18n = i18n).issues }
                error = null
            } catch (e: Exception) { error = i18n.text("trash.saveFailed", e.message) }
        }

    /** Project history for undo. */
    private val history = ArrayDeque<Triple<Project, List<TrashItem>, String>>()
    var canUndo by mutableStateOf(false)
        private set
    val undoLabel: String? get() = history.lastOrNull()?.third
    val mapUiState = com.onlyfield.assetmanager.configurator.map.MapUiState()
    var selectedSiteId by mutableStateOf<String?>(null)
    var selectedAreaId by mutableStateOf<String?>(null)
    /** Ids opened from the project search, most recent first (session only). */
    var recentSearch by mutableStateOf<List<String>>(emptyList())
    /** "Other modules" opened: unused optional modules are listed too (session only). */
    var showSecondary by mutableStateOf(false)
    val detailSlot = com.onlyfield.assetmanager.pc.ui.components.DetailSlot()
    private var currentSection by mutableStateOf(AppSection.PROJECT)
    var section: AppSection
        get() = currentSection
        set(value) { if (value != currentSection) requestChange { currentSection = value } }

    fun requestChange(action: () -> Unit) { if (!busy) detailSlot.requestChange(action) }
    var newSiteWizard by mutableStateOf(NewSiteWizard())
    fun newProject() = requestChange { newSiteWizard = NewSiteWizard(); dialog = AppDialog.NewProject }
    var dialog by mutableStateOf<AppDialog?>(null)

    fun dismissDialog() {
        if (dialog == AppDialog.NewProject) newSiteWizard = NewSiteWizard()
        val pending = when (val current = dialog) {
            is AppDialog.Compare -> current.pkg
            is AppDialog.Merge -> current.pkg
            is AppDialog.LocalReplacementPassword -> current.pkg
            else -> null
        }
        dialog = null
        pending?.let { runIo { it.close() } }
    }

    var status by mutableStateOf("Pronto")
        private set
    var error by mutableStateOf<String?>(null)
    var issues by mutableStateOf<List<ValidationIssue>>(emptyList())
        private set
    var storedProjects by mutableStateOf(runIo { storage.listStoredProjects() })
        private set
    var dataDir by mutableStateOf(runIo { storage.checkDataDirectoryStatus() })
        private set

    // Portable settings live beside project data.
    private val settingsFile get() = File(storage.dataDir, "settings.properties")
    private val initialSettings = try { runIo { loadSettings() } }
    catch (e: java.io.IOException) {
        error = Messages().text("settings.loadFailed", e.message)
        java.util.Properties()
    } catch (e: IllegalArgumentException) {
        error = Messages().text("settings.loadFailed", e.message)
        java.util.Properties()
    }

    var darkTheme by mutableStateOf(initialSettings.getProperty("theme") == "dark")
        private set

    fun toggleDarkTheme() {
        if (busy) return
        try {
            val next = !darkTheme
            val cleanup = runIo { saveSettings("theme", if (next) "dark" else "light") }
            darkTheme = next
            error = settingsCleanupWarning(cleanup)
        } catch (e: java.io.IOException) {
            error = i18n.text("settings.saveFailed", e.message)
        } catch (e: IllegalArgumentException) {
            error = i18n.text("settings.saveFailed", e.message)
        }
    }

    private fun loadSettings() = java.util.Properties().apply {
        if (settingsFile.exists()) settingsFile.inputStream().use { load(it) }
    }

    private fun saveSettings(key: String, value: String): List<Exception> {
        val props = loadSettings().apply { setProperty(key, value) }
        return com.onlyfield.assetmanager.exchange.ReversibleFiles().use { files ->
            files.replace(settingsFile) { props.store(it, null) }
            files.apply()
            files.commit()
        }
    }

    private fun settingsCleanupWarning(errors: List<Exception>): String? = errors.takeIf { it.isNotEmpty() }
        ?.let { i18n.text("settings.cleanupFailed", it.joinToString("; ") { e -> e.message.orEmpty() }) }

    var language by mutableStateOf(AppLanguage.entries.firstOrNull { it.tag == initialSettings.getProperty("language", "") } ?: AppLanguage.SYSTEM)
        private set
    val i18n: Messages get() = Messages(language.resolve())

    fun changeLanguage(value: AppLanguage) = requestChange {
        try {
            val cleanup = runIo { saveSettings("language", value.tag) }
            language = value
            storage.i18n = i18n
            status = i18n.text("status.ready")
            issues = project?.let { ModelValidator.validateProject(it, i18n = i18n).issues } ?: emptyList()
            error = settingsCleanupWarning(cleanup)
        } catch (e: java.io.IOException) { error = i18n.text("settings.saveFailed", e.message) }
        catch (e: IllegalArgumentException) { error = i18n.text("settings.saveFailed", e.message) }
    }

    init { storage.i18n = i18n; status = i18n.text("status.ready") }

    val hasPassword: Boolean get() = password != null
    val errorCount: Int get() = issues.count { it.severity == ValidationSeverity.STRUCTURAL_ERROR }
    val warningCount: Int get() = issues.count { it.severity == ValidationSeverity.DOCUMENTARY_WARNING }
    val windowTitle: String get() = project?.let { "${it.name} — OnlyField Asset Manager" } ?: "OnlyField Asset Manager"

    fun update(updated: Project, message: String) = update(updated, message, true)

    private fun update(updated: Project, message: String, persist: Boolean) {
        val previousTrash = trashBeforeEdit ?: trashState
        if (persist) {
            try { runIo { storage.saveProjectLocally(updated, password, trashItems = trashState,
                recoverableAttachments = retainedForEdit()) } } catch (e: Exception) {
                trashState = previousTrash
                trashBeforeEdit = null
                error = i18n.text("text.4d26209b4111", e.message)
                return
            }
        }
        project?.takeIf { it.id == updated.id }?.let {
            history.addLast(Triple(it, previousTrash, message))
            if (history.size > MAX_UNDO) history.removeFirst()
            canUndo = true
        }
        trashBeforeEdit = null
        project = updated
        issues = runIo { ModelValidator.validateProject(updated, i18n = i18n).issues }
        status = message
        error = null
        refreshStoredList()
    }

    private fun retainedForEdit(): List<Attachment> =
        (history.takeLast(MAX_UNDO - 1).flatMap { it.first.attachments } + project?.attachments.orEmpty()).distinctBy { it.id }

    fun saveMapObject(draft: com.onlyfield.assetmanager.core.forms.MapObjectDraft, photos: List<File>, removed: Set<String>): Boolean =
        saveMapEdit(photos, draft.targetType, draft.id) { p -> draft.apply(p, i18n = i18n).let { it.copy(attachments = it.attachments.filterNot { a -> a.id in removed }) } } != null

    /** Saves [files] at once as photos of the target (map pane, port card, editor). */
    fun attachPhotos(files: List<File>, type: AttachmentTargetType, targetId: String): Boolean = saveMapEdit(files, type, targetId) { it } != null

    fun importFloorplan(file: File, areaId: String): Attachment? =
        saveMapEdit(listOf(file), AttachmentTargetType.AREA, areaId, { it })?.singleOrNull()

    private fun saveMediaChange(message: String, created: MutableList<File>, build: () -> Project): Boolean {
        val before = project ?: return false
        val previousMedia = try { runIo {
            storage.mediaSnapshot(before.id)?.let { source ->
                com.onlyfield.assetmanager.exchange.PackagePayloads(storage.getTempFolder()).also { snapshot ->
                    try { snapshot.copyFrom(source) } catch (e: Exception) { snapshot.close(); throw e }
                }
            }
        } } catch (e: Exception) { error = i18n.text("text.48b913a738e1", e.message); return false }
        val previousProtection = storage.mediaProtected(before.id)
        var committed = false
        return try {
            val saved = runIo { build().also { storage.saveProjectLocally(it, password, trashItems = trashState,
                recoverableAttachments = retainedForEdit()) } }
            committed = true
            update(saved, message, persist = false)
            true
        } catch (e: Exception) {
            if (!committed) runIo {
                storage.restoreMedia(before.id, previousMedia, previousProtection)
                created.forEach { java.nio.file.Files.deleteIfExists(it.toPath()) }
            }
            error = i18n.text("text.48b913a738e1", e.message)
            false
        } finally { if (committed) previousMedia?.let { runIo { it.close() } } }
    }

    private fun saveMapEdit(files: List<File>, type: AttachmentTargetType, targetId: String, transform: (Project) -> Project): List<Attachment>? {
        val before = project ?: return null
        val created = mutableListOf<File>()
        var attachments = emptyList<Attachment>()
        val saved = saveMediaChange(i18n.text("text.b1b5983f51f1"), created) {
            files.forEach { AttachmentFiles.validateSize(it.length(), i18n) }
            attachments = files.map { source ->
                val pdf = source.extension.equals("pdf", true)
                val pages = if (pdf) PlanMedia.pageCount(source, i18n = i18n) else { PlanMedia.validateImage(source, i18n = i18n); 1 }
                require(pages > 0) { i18n.text("text.4acdb5acf0b0") }
                val att = Attachment(name = source.nameWithoutExtension, originalFileName = source.name, relativePath = "", pageCount = pages,
                    fileType = if (pdf) AttachmentType.PDF else AttachmentType.IMAGE, mimeType = if (pdf) "application/pdf" else java.nio.file.Files.probeContentType(source.toPath()) ?: "image/jpeg",
                    targetType = type, targetId = targetId)
                created += storage.attachmentFile(before.id, att)
                storage.storeAttachmentFile(before.id, att, source)
                att.copy(relativePath = AttachmentFiles.entryName(att))
            }
            val edited = transform(before)
            edited.copy(attachments = edited.attachments + attachments, updatedEpochMs = System.currentTimeMillis())
        }
        return attachments.takeIf { saved }
    }

    /** Copies [file] into project attachments. */
    fun addAttachment(file: File, name: String, classification: AttachmentClassification) {
        val p = project ?: return
        val attachment = Attachment(
            name = name.trim().ifBlank { file.nameWithoutExtension },
            originalFileName = file.name,
            fileType = when (file.extension.lowercase()) {
                "pdf" -> AttachmentType.PDF
                "jpg", "jpeg", "png", "gif", "bmp", "webp" -> AttachmentType.IMAGE
                "doc", "docx", "xls", "xlsx", "txt", "md" -> AttachmentType.DOCUMENT
                else -> AttachmentType.OTHER
            },
            mimeType = java.nio.file.Files.probeContentType(file.toPath()) ?: "application/octet-stream",
            relativePath = "",
            classification = classification
        )
        val created = mutableListOf<File>()
        saveMediaChange(i18n.text("text.5d5df1229dea", attachment.name), created) {
            AttachmentFiles.validateSize(file.length(), i18n)
            val checked = attachment.copy(pageCount = if (attachment.fileType == AttachmentType.PDF) PlanMedia.pageCount(file, i18n = i18n) else 1)
            created += storage.attachmentFile(p.id, checked)
            storage.storeAttachmentFile(p.id, checked, file)
            ProjectEdits.addAttachment(p, checked.copy(relativePath = AttachmentFiles.entryName(checked)))
        }
    }

    /** Null only when no background was requested; unreadable plans fail the document. */
    fun planImage(area: Area): java.awt.image.BufferedImage? {
        val id = area.floorplanAttachmentId ?: return null
        val message = i18n.text("document.planUnreadable", area.name)
        val attachment = project?.attachments?.find { it.id == id } ?: throw java.io.IOException(message)
        val bytes = attachmentBytes(attachment) ?: throw java.io.IOException(message)
        return PlanMedia.bufferedImage(bytes, attachment.fileType == AttachmentType.PDF, area.floorplanPageIndex, i18n = i18n)
            ?: throw java.io.IOException(message)
    }

    fun attachmentBytes(attachment: Attachment): ByteArray? = project?.let { storage.attachmentBytes(it.id, attachment) }

    fun hasAttachment(attachment: Attachment): Boolean = project?.let { storage.hasAttachment(it.id, attachment) } == true

    fun openAttachment(attachment: Attachment) {
        try {
            val existing = attachmentFile(attachment)
            val target = existing ?: DesktopStorageHelper.pickSaveFile(
                title = i18n.text("media.exportAndOpen"),
                defaultFileName = attachment.originalFileName,
                extensionDescription = attachment.originalFileName,
                extensions = File(attachment.originalFileName).extension.takeIf { it.isNotBlank() }?.let { arrayOf(it) } ?: emptyArray(),
                i18n = i18n,
            ) ?: return
            runIo {
                if (existing == null) target.writeBytes(requireNotNull(attachmentBytes(attachment)) { i18n.text("text.dad522b5d9b7") })
                java.awt.Desktop.getDesktop().open(target)
            }
        } catch (e: Exception) { error = e.message }
    }

    fun attachmentFile(attachment: Attachment): File? =
        project?.let { storage.attachmentFile(it.id, attachment) }?.takeIf { it.isFile }

    fun addMapSnapshot(snapshot: DesktopMapSnapshot, name: String): Boolean {
        val p = project ?: return false
        val attachment = Attachment(
            name = name.trim(), originalFileName = "map_snapshot.png",
            fileType = AttachmentType.IMAGE, mimeType = "image/png", relativePath = "",
            classification = AttachmentClassification.SHAREABLE,
            targetType = AttachmentTargetType.PROJECT, targetId = p.id,
            attributionText = snapshot.attributionText,
        ).let { it.copy(relativePath = AttachmentFiles.entryName(it)) }
        val created = mutableListOf<File>()
        return saveMediaChange(i18n.text("text.18c905e167a3", attachment.name), created) {
            created += storage.attachmentFile(p.id, attachment)
            storage.storeAttachmentBytes(p.id, attachment, snapshot.imageBytes)
            ProjectEdits.addAttachment(p, attachment)
        }
    }

    /** Restores the previous project state. */
    fun undo() = requestChange { undoNow() }

    private fun undoNow() {
        val (previous, previousTrash, message) = history.lastOrNull() ?: return
        try { runIo { storage.saveProjectLocally(previous, password, trashItems = previousTrash,
            recoverableAttachments = history.dropLast(1).flatMap { it.first.attachments }) } } catch (e: Exception) {
            error = i18n.text("text.1bf1df0e9d53", e.message)
            return
        }
        history.removeLast()
        canUndo = history.isNotEmpty()
        project = previous
        trashState = previousTrash
        issues = runIo { ModelValidator.validateProject(previous, i18n = i18n).issues }
        status = i18n.text("text.14ca15d15945", message)
        error = null
        refreshStoredList()
    }

    private fun clearHistory() {
        history.clear()
        trashBeforeEdit = null
        canUndo = false
    }

    fun notify(message: String) {
        status = message
    }

    fun addToTrash(item: TrashItem) {
        if (trashBeforeEdit == null) trashBeforeEdit = trashState
        trashState += item
    }

    fun restoreTrash(item: TrashItem) {
        val p = project ?: return
        val restored = try {
            check(item in trashState) { i18n.text("trash.invalidEntry") }
            ProjectEdits.restoreFromTrash(p, item, i18n = i18n)
        }
        catch (e: Exception) { error = e.message; return }
        trashBeforeEdit = trashState
        trashState = trashState.filterNot { it.id == item.id }
        update(restored, i18n.text("text.57a18cfa77e6", item.displayName))
    }

    fun mergeDevices(survivorId: String, duplicateId: String, choices: MergeDataChoices): Boolean {
        val before = project ?: return false
        val (updated, item) = try { runIo { ProjectEdits.mergeDevices(before, survivorId, duplicateId, choices, i18n) } }
        catch (e: IllegalStateException) { error = e.message; return false }
        if (item == null) return false
        addToTrash(item)
        val index = ProjectIndex(before)
        update(updated, i18n.text("text.83e8fd9fecb8", index.deviceName(duplicateId, i18n = i18n), index.deviceName(survivorId, i18n = i18n)))
        return project == updated
    }

    fun refreshStoredList() {
        val (projects, status) = runIo { storage.listStoredProjects() to storage.checkDataDirectoryStatus() }
        storedProjects = projects
        dataDir = status
    }

    private fun open(newProject: Project, newManifest: PackageManifest?, newPassword: String?, message: String, incoming: ProjectPackage? = null, releaseLockOnFailure: Boolean = !storage.ownsProjectLock(newProject.id), localPackage: Boolean = false, localPassword: String? = newPassword, localPasswordAttempt: Boolean = false): Boolean {
        val previousProject = project
        val previousPassword = password
        val previousTrash = trashState
        val previousId = project?.id
        val previousMedia = storage.mediaSnapshot(newProject.id)
        val previousMediaProtected = storage.mediaProtected(newProject.id)
        var committed = false
        var loadedTrash = emptyList<TrashItem>()
        var openedProject = newProject
        try {
            runIo {
                storage.acquireProjectLock(newProject.id)
                val localState = if (previousId == newProject.id) LocalProjectState(trashState, storage.mediaSnapshot(newProject.id).orEmpty(), project)
                    else storage.loadLocalState(newProject.id, localPassword)
                try {
                    localState.project?.let { openedProject = ProjectEdits.retainTrashAttachments(newProject, it, localState.trash) }
                    storage.prepareMedia(openedProject, incoming, newPassword != null, local = localPackage || incoming == null, retainedMedia = localState.media)
                    loadedTrash = localState.trash
                    if (previousId != newProject.id) localState.close()
                    storage.saveProjectLocally(openedProject, newPassword, trashItems = localState.trash,
                        syncBase = incoming?.project?.takeUnless { localPackage }, recoveryPassword = localPassword ?: newPassword)
                    committed = true
                } finally { if (previousId != newProject.id) localState.close() }
            }
            project = openedProject
            manifest = newManifest
            password = newPassword
            clearHistory()
            trashState = loadedTrash
            issues = runIo { ModelValidator.validateProject(openedProject, i18n = i18n).issues }
            mapUiState.clear()
            selectedSiteId = null
            selectedAreaId = null
            currentSection = AppSection.FLOORPLANS
            status = message
            error = storage.saveCleanupErrors.takeIf { it.isNotEmpty() }?.joinToString("; ") { it.message.orEmpty() }
            if (previousId != newProject.id && previousProject != null) {
                try { runIo { if (storage.hasRecoveryMedia(previousProject, previousTrash))
                    storage.saveProjectLocally(previousProject, previousPassword, trashItems = previousTrash) } }
                catch (e: Exception) { error = e.message }
                finally { storage.releaseProjectLock(previousProject.id) }
            }
            refreshStoredList()
            if (previousMedia !== storage.mediaSnapshot(newProject.id)) (previousMedia as? AutoCloseable)?.close()
            return true
        } catch (e: Exception) {
            if (committed) {
                error = e.message
                return true
            }
            if (releaseLockOnFailure) storage.releaseProjectLock(newProject.id)
            storage.restoreMedia(newProject.id, previousMedia, previousMediaProtected)
            if ((e is LocalPasswordRequired || e is com.onlyfield.assetmanager.exchange.RecoveryPasswordRequired) && incoming != null && !localPackage) {
                dialog = AppDialog.LocalReplacementPassword(incoming, newPassword, wrongPassword = localPasswordAttempt)
                error = null
            } else error = i18n.text("text.803d70d07f6f", e.message)
            return false
        } finally {
            if ((dialog as? AppDialog.LocalReplacementPassword)?.pkg !== incoming) incoming?.let { runIo { it.close() } }
        }
    }


    /** Creates and opens the wizard project. */
    fun createProject(wizard: NewSiteWizard) {
        val newPassword = wizard.password
        val newProject = wizard.buildProject().copy(isPasswordProtected = newPassword != null)
        val expectedDialog = dialog
        if (open(newProject, null, newPassword, i18n.text("text.a5af7b4f324d", newProject.name)) && dialog == expectedDialog) {
            dialog = null
            newSiteWizard = NewSiteWizard()
        }
    }

    fun closeProject() = requestChange { closeProjectNow() }

    private fun closeProjectNow() {
        val current = project
        if (current != null) try {
            runIo { if (storage.hasRecoveryMedia(current, trashState)) storage.saveProjectLocally(current, password, trashItems = trashState) }
        } catch (e: Exception) { error = e.message; return }
        project?.let { storage.releaseProjectLock(it.id) }
        project = null
        manifest = null
        password = null
        trashState = emptyList()
        clearHistory()
        issues = emptyList()
        section = AppSection.PROJECT
        status = i18n.text("text.01d39a91cad2")
    }

    fun pickAndImport() = requestChange {
        DesktopStorageHelper.pickOpenFile(i18n = i18n)?.let { importFile(it) }
    }

    /** Opens a saved project without comparison. */
    fun openStored(file: File) = requestChange { importFile(file, password = null, compare = false) }

    fun importFile(file: File, password: String? = null, compare: Boolean = true) {
        val releaseLockOnFailure = !storage.ownsProjectLock(file.nameWithoutExtension)
        val result = try {
            runIo { if (storage.isLocalProjectFile(file)) storage.loadLocalProject(file.nameWithoutExtension, password) else storage.importPackageFromFile(file, password) }
        } catch (e: Exception) {
            if (e is com.onlyfield.assetmanager.exchange.RecoveryPasswordRequired) {
                dialog = AppDialog.ImportPassword(file, error = i18n.text("recovery.password"))
                return
            }
            error = i18n.text("text.34b2135f1370", file.name, e.message)
            return
        }
        val issuesFound = result.validationResult.issues
        if (issuesFound.any { ((it.code == "PASSWORD_REQUIRED") || (it.code == "INVALID_PACKAGE_PASSWORD")) }) {
            dialog = AppDialog.ImportPassword(file, error = if (password != null) i18n.text("text.972b256c2416") else null)
            return
        }
        val pkg = result.pkg
        if (pkg == null) {
            dialog = null
            error = i18n.text("text.953ce2808a1f", file.name) + issuesFound.joinToString("; ") { it.message }
            return
        }
        val warnings = issuesFound.filter { it.severity == ValidationSeverity.DOCUMENTARY_WARNING }
        if (compare && !storage.isLocalProjectFile(file)) {
            reviewIncoming(pkg, password, warnings)
            return
        }
        dialog = null
        open(pkg.project, pkg.manifest, password.takeIf { pkg.manifest.isEncrypted }, i18n.text("text.644b750a4abb", pkg.project.name, pkg.project.sites.sumOf { it.devices.size }), pkg, if (storage.isLocalProjectFile(file)) releaseLockOnFailure else !storage.ownsProjectLock(pkg.project.id), localPackage = storage.isLocalProjectFile(file))
    }

    fun acceptIncoming(pkg: ProjectPackage, incomingPassword: String?) {
        val review = (dialog as? AppDialog.Compare)?.takeIf { it.pkg === pkg }
        dialog = null
        open(pkg.project, pkg.manifest, incomingPassword, i18n.text("text.8e68c1630b23", pkg.project.name), pkg,
                localPassword = review?.localPassword ?: incomingPassword)
    }

    fun acceptIncomingWithLocalPassword(pkg: ProjectPackage, incomingPassword: String?, localPassword: String) {
        val prompt = dialog as? AppDialog.LocalReplacementPassword
        if (prompt?.pkg === pkg && prompt.compareBeforeReplace) {
            reviewIncoming(pkg, incomingPassword, prompt.warnings, localPassword, passwordAttempt = true)
            return
        }
        dialog = null
        open(pkg.project, pkg.manifest, incomingPassword, i18n.text("text.8e68c1630b23", pkg.project.name), pkg,
                localPassword = localPassword, localPasswordAttempt = true)
    }

    private fun reviewIncoming(pkg: ProjectPackage, incomingPassword: String?, warnings: List<ValidationIssue>,
        localPassword: String? = incomingPassword, passwordAttempt: Boolean = false) {
        try {
            val current = project?.takeIf { it.id == pkg.project.id }
            val localFile = File(storage.getProjectsFolder(), "${pkg.project.id}.ofam")
            val review = if (current != null) {
                AppDialog.Compare(runIo { ProjectComparisonEvaluator.evaluate(current, manifest, pkg, i18n) },
                    pkg, incomingPassword, warnings, current, password)
            } else if (localFile.exists()) {
                runIo {
                    val result = storage.importPackageFromFile(localFile, localPassword)
                    if (result.validationResult.issues.any { it.code in setOf("PASSWORD_REQUIRED", "INVALID_PACKAGE_PASSWORD") }) {
                        return@runIo null
                    }
                    val local = requireNotNull(result.pkg) { result.validationResult.issues.joinToString("; ") { it.message } }
                    local.use {
                        AppDialog.Compare(ProjectComparisonEvaluator.evaluate(it.project, it.manifest, pkg, i18n),
                            pkg, incomingPassword, warnings, it.project, localPassword.takeIf { _ -> it.manifest.isEncrypted })
                    }
                } ?: run {
                    dialog = AppDialog.LocalReplacementPassword(pkg, incomingPassword, passwordAttempt, true, warnings)
                    return
                }
            } else {
                AppDialog.Compare(runIo { ProjectComparisonEvaluator.evaluate(null, null, pkg, i18n) }, pkg, incomingPassword, warnings)
            }
            dialog = review
            if (review.localProject == null && project == null && warnings.isEmpty()) acceptIncoming(pkg, incomingPassword)
        } catch (e: Exception) {
            if (e is com.onlyfield.assetmanager.exchange.RecoveryPasswordRequired) {
                dialog = AppDialog.LocalReplacementPassword(pkg, incomingPassword, passwordAttempt, true, warnings)
                return
            }
            dialog = null
            runIo { pkg.close() }
            error = i18n.text("text.34b2135f1370", pkg.project.name, e.message)
        }
    }

    /** Merges only the incoming project's local copy. */
    fun startMerge(pkg: ProjectPackage) {
        val review = (dialog as? AppDialog.Compare)?.takeIf { it.pkg === pkg }
        val current = review?.localProject ?: project?.takeIf { it.id == pkg.project.id } ?: return
        if (current.id != pkg.project.id) return
        val localPassword = if (review != null) review.localPassword else password
        var baseError: String? = null
        val base = try { runIo { storage.loadSyncBase(current.id, localPassword) } } catch (e: Exception) { baseError = e.message; null }
        if (base == null && baseError == null) baseError = i18n.text("merge.baseUnavailable")
        val result = runIo { ProjectMerger.merge(base, current, pkg.project, i18n = i18n) }
        val merge = AppDialog.Merge(pkg, result, replaceClosedCopy = project?.id != current.id, localPassword = localPassword)
        if (result.conflicts.isEmpty()) applyMerge(merge) else dialog = merge
        if (baseError != null) error = baseError
    }

    fun chooseMergeSide(side: MergeSide) {
        val merge = dialog as? AppDialog.Merge ?: return
        val conflict = merge.current ?: return
        val next = merge.copy(choices = merge.choices + (conflict.key to side))
        if (next.current == null) applyMerge(next) else dialog = next
    }

    private fun applyMerge(merge: AppDialog.Merge) {
        dialog = null
        val id = merge.pkg.project.id
        val previousMedia = storage.mediaSnapshot(id)
        val previousProtected = storage.mediaProtected(id)
        var committed = false
        try {
            if (merge.replaceClosedCopy) {
                val resolved = runIo { merge.result.resolve(merge.choices, i18n = i18n) }
                    .copy(isPasswordProtected = merge.localPassword != null)
                open(resolved, merge.pkg.manifest, merge.localPassword,
                        i18n.text("text.123fb31b11bf", merge.result.autoApplied, merge.choices.size), merge.pkg,
                        localPassword = merge.localPassword)
                return
            }
            val resolved = runIo { ProjectEdits.retainTrashAttachments(
                merge.result.resolve(merge.choices, i18n = i18n).copy(isPasswordProtected = password != null),
                requireNotNull(project), trashState) }
            runIo {
                storage.prepareMedia(resolved, merge.pkg, password != null, local = true)
                storage.saveProjectLocally(resolved, password, trashItems = trashState, syncBase = merge.pkg.project,
                    recoverableAttachments = retainedForEdit())
                committed = true
            }
            update(resolved, i18n.text("text.123fb31b11bf", merge.result.autoApplied, merge.choices.size), persist = false)
            error = storage.saveCleanupErrors.takeIf { it.isNotEmpty() }?.joinToString("; ") { it.message.orEmpty() }
            if (previousMedia !== storage.mediaSnapshot(id)) (previousMedia as? AutoCloseable)?.close()
        } catch (e: Exception) {
            if (!committed) storage.restoreMedia(id, previousMedia, previousProtected)
            error = i18n.text("text.4d26209b4111", e.message)
        } finally { runIo { merge.pkg.close() } }
    }

    fun exportPackage() {
        val p = project ?: return
        val file = DesktopStorageHelper.pickSaveFile(
            title = i18n.text("text.505a2a51b914"),
            defaultFileName = "${safeFileName(p.name)}.ofam",
            i18n = i18n) ?: return
        try {
            val missing = runIo {
                val exported = ProjectEdits.purgeTrashAttachments(p, trashState).copy(updatedEpochMs = p.updatedEpochMs)
                storage.exportPackageToFile(exported, file, password)
                storage.saveSyncBase(exported, password)
                storage.missingAttachments(p)
            }
            error = if (missing.isEmpty()) null
            else i18n.text("text.58892a723cfb", missing.size) + missing.joinToString { it.name }
            status = i18n.text("text.801c3393c549", file.absolutePath) + if (password != null) i18n.text("text.2bd60c532b7b") else "."
        } catch (e: Exception) {
            error = i18n.text("text.7e77043ab420", e.message)
        }
    }

    /** Returns null when the password changes. */
    fun changePassword(current: String, newPassword: String, confirm: String): String? {
        val p = project ?: return i18n.text("text.f6a2e7b34cdb")
        if (password != null && current != password) return i18n.text("text.0ab6e626d98f")
        if (newPassword != confirm) return i18n.text("text.32482057acc4")
        val nextPassword = newPassword.ifEmpty { null }
        val updated = p.copy(isPasswordProtected = nextPassword != null, updatedEpochMs = System.currentTimeMillis())
        try {
            runIo { storage.changeProjectPassword(updated, password, nextPassword, trashState) }
        } catch (e: Exception) {
            val message = i18n.text("text.e19ade76e0dd", e.message)
            error = message
            return message
        }
        password = nextPassword
        project = updated
        issues = runIo { ModelValidator.validateProject(updated, i18n = i18n).issues }
        dialog = null
        status = if (nextPassword != null) i18n.text("text.866d536805c5") else i18n.text("text.666e96d6fcf0")
        clearHistory()
        error = null
        refreshStoredList()
        return null
    }

    fun shutdown() {
        dismissDialog()
        if (project != null && storage.ownsProjectLock(project!!.id)) closeProjectNow()
        runIo { storage.releaseAllLocks() }
        io.close()
    }


    /** Editor section for [id], when present. */
    fun sectionOf(id: String?): AppSection? {
        val p = project ?: return null
        if (id == null) return null
        val index = ProjectIndex(p)
        return when {
            index.device(id) != null || index.port(id) != null -> AppSection.INVENTORY
            index.rack(id) != null -> AppSection.RACKS
            p.deviceModels.any { it.id == id } -> AppSection.MODELS
            index.area(id) != null || p.attachments.any { it.id == id } || p.floorplanPlacements.any { it.id == id } -> AppSection.FLOORPLANS
            p.cables.any { it.id == id } || p.panelMappings.any { it.id == id } -> AppSection.CABLING
            p.vlans.any { it.id == id } || p.subnets.any { it.id == id } || p.logicalInterfaces.any { it.id == id } ||
                p.wanVpnConnections.any { it.id == id } || p.deviceConfigurations.any { it.id == id } -> AppSection.NETWORK
            p.powerFeeds.any { it.id == id } || p.poeMappings.any { it.id == id } || p.documentBadges.any { it.id == id } -> AppSection.POWER
            else -> null
        }
    }

    companion object {
        private const val MAX_UNDO = 50

        fun safeFileName(name: String): String = name.trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").ifBlank { "progetto" }
    }
}
