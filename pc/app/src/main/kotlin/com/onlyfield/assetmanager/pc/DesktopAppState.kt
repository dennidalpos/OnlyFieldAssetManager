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
import com.onlyfield.assetmanager.exchange.ComparisonStatus
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
    data object ManagePassword : AppDialog
    data class Compare(val comparison: ProjectComparison, val pkg: ProjectPackage, val password: String?) : AppDialog
    /** Merge conflicts resolved one at a time. */
    data class Merge(val pkg: ProjectPackage, val result: MergeResult, val choices: Map<MergeKey, MergeSide> = emptyMap()) : AppDialog {
        val current: MergeConflict? get() = result.conflicts.getOrNull(choices.size)
    }
    data object Documents : AppDialog
    data object Validation : AppDialog
}

/** Desktop state; [update] validates and saves project changes. */
class DesktopAppState(val storage: DesktopStorageManager) {

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
                storage.saveProjectLocally(p, password, trashItems = value)
                trashState = value
                error = null
            } catch (e: Exception) { error = i18n.text("trash.saveFailed", e.message) }
        }

    /** Project history for undo. */
    private val history = ArrayDeque<Triple<Project, List<TrashItem>, String>>()
    var canUndo by mutableStateOf(false)
        private set
    val undoLabel: String? get() = history.lastOrNull()?.third
    var selectedSiteId by mutableStateOf<String?>(null)
    var selectedAreaId by mutableStateOf<String?>(null)
    /** Ids opened from the project search, most recent first (session only). */
    var recentSearch by mutableStateOf<List<String>>(emptyList())
    val detailSlot = com.onlyfield.assetmanager.pc.ui.components.DetailSlot()
    private var currentSection by mutableStateOf(AppSection.PROJECT)
    var section: AppSection
        get() = currentSection
        set(value) { if (value != currentSection) requestChange { currentSection = value } }

    fun requestChange(action: () -> Unit) = detailSlot.requestChange(action)
    fun newProject() = requestChange { dialog = AppDialog.NewProject }
    var dialog by mutableStateOf<AppDialog?>(null)

    var status by mutableStateOf("Pronto")
        private set
    var error by mutableStateOf<String?>(null)
    var issues by mutableStateOf<List<ValidationIssue>>(emptyList())
        private set
    var storedProjects by mutableStateOf(storage.listStoredProjects())
        private set
    var dataDir by mutableStateOf(storage.checkDataDirectoryStatus())
        private set

    // Portable settings live beside project data.
    private val settingsFile get() = File(storage.dataDir, "settings.properties")

    var darkTheme by mutableStateOf(loadSettings().getProperty("theme") == "dark")
        private set

    fun toggleDarkTheme() {
        darkTheme = !darkTheme
        try {
            val props = loadSettings().apply { setProperty("theme", if (darkTheme) "dark" else "light") }
            settingsFile.outputStream().use { props.store(it, null) }
        } catch (_: Exception) {
        }
    }

    private fun loadSettings() = java.util.Properties().apply {
        try { if (settingsFile.isFile) settingsFile.inputStream().use { load(it) } } catch (_: Exception) {}
    }

    var language by mutableStateOf(AppLanguage.entries.firstOrNull { it.tag == loadSettings().getProperty("language", "") } ?: AppLanguage.SYSTEM)
        private set
    val i18n: Messages get() = Messages(language.resolve())

    fun changeLanguage(value: AppLanguage) = requestChange {
        try {
            val props = loadSettings().apply { setProperty("language", value.tag) }
            settingsFile.outputStream().use { props.store(it, null) }
            language = value
            storage.i18n = i18n
            status = i18n.text("status.ready")
            issues = project?.let { ModelValidator.validateProject(it, i18n = i18n).issues } ?: emptyList()
        } catch (e: Exception) { error = e.message }
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
            try { storage.saveProjectLocally(updated, password, trashItems = trashState) } catch (e: Exception) {
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
        issues = ModelValidator.validateProject(updated, i18n = i18n).issues
        status = message
        error = null
        refreshStoredList()
    }

    fun saveMapObject(draft: com.onlyfield.assetmanager.core.forms.MapObjectDraft, photos: List<File>, removed: Set<String>): Boolean =
        saveMapEdit(photos, draft.targetType, draft.id) { p -> draft.apply(p, i18n = i18n).let { it.copy(attachments = it.attachments.filterNot { a -> a.id in removed }) } } != null

    /** Saves [files] at once as photos of the target (map pane, port card, editor). */
    fun attachPhotos(files: List<File>, type: AttachmentTargetType, targetId: String): Boolean = saveMapEdit(files, type, targetId) { it } != null

    fun importFloorplan(file: File, areaId: String): Attachment? =
        saveMapEdit(listOf(file), AttachmentTargetType.AREA, areaId, { it })?.singleOrNull()

    private fun saveMapEdit(files: List<File>, type: AttachmentTargetType, targetId: String, transform: (Project) -> Project): List<Attachment>? {
        val before = project ?: return null
        val created = mutableListOf<File>()
        var committed = false
        return try {
            val attachments = files.map { source ->
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
            val saved = edited.copy(attachments = edited.attachments + attachments, updatedEpochMs = System.currentTimeMillis())
            storage.saveProjectLocally(saved, password, trashItems = trashState)
            committed = true
            update(saved, i18n.text("text.b1b5983f51f1"), persist = false)
            error = null
            refreshStoredList()
            attachments
        } catch (e: Exception) {
            if (!committed) created.forEach { it.delete() }
            error = i18n.text("text.48b913a738e1", e.message)
            null
        }
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
        try {
            val checked = attachment.copy(pageCount = if (attachment.fileType == AttachmentType.PDF) PlanMedia.pageCount(file, i18n = i18n) else 1)
            storage.storeAttachmentFile(p.id, checked, file)
            val saved = checked.copy(relativePath = AttachmentFiles.entryName(checked))
            update(ProjectEdits.addAttachment(p, saved), i18n.text("text.5d5df1229dea", saved.name))
        } catch (e: Exception) {
            error = i18n.text("text.0fceb31dcb80", file.name, e.message)
        }
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
        val target = storage.attachmentFile(p.id, attachment)
        return try {
            java.nio.file.Files.createDirectories(target.parentFile.toPath())
            java.nio.file.Files.write(target.toPath(), snapshot.imageBytes)
            update(ProjectEdits.addAttachment(p, attachment), i18n.text("text.18c905e167a3", attachment.name))
            error == null
        } catch (e: java.io.IOException) {
            java.nio.file.Files.deleteIfExists(target.toPath())
            error = i18n.text("text.def5b23c35ea", e.message)
            false
        }
    }

    /** Restores the previous project state. */
    fun undo() = requestChange { undoNow() }

    private fun undoNow() {
        val (previous, previousTrash, message) = history.lastOrNull() ?: return
        try { storage.saveProjectLocally(previous, password, trashItems = previousTrash) } catch (e: Exception) {
            error = i18n.text("text.1bf1df0e9d53", e.message)
            return
        }
        history.removeLast()
        canUndo = history.isNotEmpty()
        project = previous
        trashState = previousTrash
        issues = ModelValidator.validateProject(previous, i18n = i18n).issues
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
        trashBeforeEdit = trashState
        trashState = trashState.filterNot { it.id == item.id }
        update(ProjectEdits.restoreFromTrash(p, item, i18n = i18n), i18n.text("text.57a18cfa77e6", item.displayName))
    }

    fun refreshStoredList() {
        storedProjects = storage.listStoredProjects()
        dataDir = storage.checkDataDirectoryStatus()
    }

    private fun open(newProject: Project, newManifest: PackageManifest?, newPassword: String?, message: String, incoming: ProjectPackage? = null, releaseLockOnFailure: Boolean = !storage.ownsProjectLock(newProject.id)): Boolean {
        val previousId = project?.id
        try {
            storage.acquireProjectLock(newProject.id)
            val loadedTrash = if (previousId == newProject.id) trashState else storage.loadTrash(newProject.id, newPassword)
            incoming?.let(storage::extractAttachments)
            storage.saveProjectLocally(newProject, newPassword, trashItems = loadedTrash)
            if (previousId != newProject.id) previousId?.let(storage::releaseProjectLock)
            project = newProject
            manifest = newManifest
            password = newPassword
            clearHistory()
            trashState = loadedTrash
            issues = ModelValidator.validateProject(newProject, i18n = i18n).issues
            selectedSiteId = null
            selectedAreaId = null
            currentSection = AppSection.FLOORPLANS
            status = message
            error = null
            refreshStoredList()
            return true
        } catch (e: Exception) {
            if (releaseLockOnFailure) storage.releaseProjectLock(newProject.id)
            error = i18n.text("text.803d70d07f6f", e.message)
            return false
        }
    }


    /** Creates and opens the wizard project. */
    fun createProject(wizard: NewSiteWizard) {
        val newPassword = wizard.password
        val newProject = wizard.buildProject().copy(isPasswordProtected = newPassword != null)
        dialog = null
        open(newProject, null, newPassword, i18n.text("text.a5af7b4f324d", newProject.name))
    }

    fun closeProject() = requestChange { closeProjectNow() }

    private fun closeProjectNow() {
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
            if (storage.isLocalProjectFile(file)) storage.loadLocalProject(file.nameWithoutExtension, password) else storage.importPackageFromFile(file, password)
        } catch (e: Exception) {
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
        val current = project
        if (compare && !storage.isLocalProjectFile(file) && current != null) {
            val comparison = ProjectComparisonEvaluator.evaluate(current, manifest, pkg, i18n = i18n)
            if (comparison.status != ComparisonStatus.IDENTICAL) {
                dialog = AppDialog.Compare(comparison, pkg, password)
                return
            }
        }
        dialog = null
        if (open(pkg.project, pkg.manifest, password, i18n.text("text.644b750a4abb", pkg.project.name, pkg.project.sites.sumOf { it.devices.size }), pkg, if (storage.isLocalProjectFile(file)) releaseLockOnFailure else !storage.ownsProjectLock(pkg.project.id)) && !storage.isLocalProjectFile(file)) rememberSyncBase(pkg.project, password)
    }

    fun acceptIncoming(pkg: ProjectPackage, incomingPassword: String?) {
        dialog = null
        if (open(pkg.project, pkg.manifest, incomingPassword, i18n.text("text.8e68c1630b23", pkg.project.name), pkg)) rememberSyncBase(pkg.project, incomingPassword)
    }

    private fun rememberSyncBase(snapshot: Project, snapshotPassword: String?) {
        try { storage.saveSyncBase(snapshot, snapshotPassword) } catch (e: Exception) {
            error = i18n.text("text.468c516c68a8", e.message)
        }
    }

    /** Merges a package into the open project. */
    fun startMerge(pkg: ProjectPackage) {
        val current = project ?: return
        var baseError: String? = null
        val base = try { storage.loadSyncBase(current.id, password) } catch (e: Exception) { baseError = e.message; null }
        if (base == null && baseError == null) baseError = i18n.text("merge.baseUnavailable")
        val result = ProjectMerger.merge(base, current, pkg.project, i18n = i18n)
        val merge = AppDialog.Merge(pkg, result)
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
        storage.extractAttachments(merge.pkg)
        update(merge.result.resolve(merge.choices, i18n = i18n), i18n.text("text.123fb31b11bf", merge.result.autoApplied, merge.choices.size))
        rememberSyncBase(merge.pkg.project, password)
    }

    fun exportPackage() {
        val p = project ?: return
        val file = DesktopStorageHelper.pickSaveFile(
            title = i18n.text("text.505a2a51b914"),
            defaultFileName = "${safeFileName(p.name)}.ofam",
            i18n = i18n) ?: return
        try {
            storage.exportPackageToFile(p, file, password)
            storage.saveSyncBase(p, password)
            val missing = storage.missingAttachments(p)
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
            storage.changeProjectPassword(updated, password, nextPassword, trashState)
        } catch (e: Exception) {
            val message = i18n.text("text.e19ade76e0dd", e.message)
            error = message
            return message
        }
        password = nextPassword
        project = updated
        issues = ModelValidator.validateProject(updated, i18n = i18n).issues
        dialog = null
        status = if (nextPassword != null) i18n.text("text.866d536805c5") else i18n.text("text.666e96d6fcf0")
        clearHistory()
        error = null
        refreshStoredList()
        return null
    }

    fun shutdown() {
        storage.releaseAllLocks()
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
