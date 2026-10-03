package com.onlyfield.assetmanager.pc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.pc.ui.SymbolIcons
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

/** Main areas of the editor, in navigation-rail order. */
enum class AppSection(val title: String, val icon: ImageVector, val needsProject: Boolean = true) {
    INVENTORY("Inventario", SymbolIcons.inventory2),
    RACKS("Rack", SymbolIcons.dns),
    MODELS("Modelli", SymbolIcons.category),
    FLOORPLANS("Mappa del piano", SymbolIcons.map),
    CREDENTIALS("Credenziali", SymbolIcons.inventory2),
    MEDIA("Allegati e cartografia", SymbolIcons.map),
    CABLING("Cablaggio", SymbolIcons.cable),
    NETWORK("Rete", SymbolIcons.lan),
    POWER("Alimentazione", SymbolIcons.bolt),
    TRASH("Cestino", SymbolIcons.delete),
    PROJECT("Progetto", SymbolIcons.settings, needsProject = false),
}

sealed interface AppDialog {
    data object NewProject : AppDialog
    data class ImportPassword(val file: File, val error: String? = null) : AppDialog
    data object ManagePassword : AppDialog
    data class Compare(val comparison: ProjectComparison, val pkg: ProjectPackage, val password: String?) : AppDialog
    /** Merge conflicts answered one at a time (F04). */
    data class Merge(val pkg: ProjectPackage, val result: MergeResult, val choices: Map<MergeKey, MergeSide> = emptyMap()) : AppDialog {
        val current: MergeConflict? get() = result.conflicts.getOrNull(choices.size)
    }
    data object Documents : AppDialog
    data object Validation : AppDialog
}

/**
 * State and actions of the desktop editor, independent from the composables that render it.
 * Every project change goes through [update], which re-validates and auto-saves to the data folder.
 */
class DesktopAppState(val storage: DesktopStorageManager) {

    var project by mutableStateOf<Project?>(null)
        private set
    private var password: String? = null
    private var manifest: PackageManifest? = null

    private var trashState by mutableStateOf<List<TrashItem>>(emptyList())

    /** Trash of the open project. Saved to disk unless the project is password-protected (it would leak data in clear). */
    var trash: List<TrashItem>
        get() = trashState
        set(value) {
            trashState = value
            project?.takeIf { !it.isPasswordProtected }?.let { storage.saveTrash(it.id, value) }
        }

    /** Previous versions of the open project, for "Annulla" (most recent last). */
    private val history = ArrayDeque<Pair<Project, String>>()
    var canUndo by mutableStateOf(false)
        private set
    val undoLabel: String? get() = history.lastOrNull()?.second
    var selectedBuId by mutableStateOf<String?>(null)
    var selectedAreaId by mutableStateOf<String?>(null)
    var section by mutableStateOf(AppSection.PROJECT)
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

    // UI preferences live next to the data, so the portable copy keeps them.
    private val settingsFile get() = File(storage.dataDir, "settings.properties")

    var darkTheme by mutableStateOf(loadSettings().getProperty("theme") == "dark")
        private set

    fun toggleDarkTheme() {
        darkTheme = !darkTheme
        try {
            val props = loadSettings().apply { setProperty("theme", if (darkTheme) "dark" else "light") }
            settingsFile.outputStream().use { props.store(it, null) }
        } catch (_: Exception) {
            // Preference not saved: the theme still applies to this session.
        }
    }

    private fun loadSettings() = java.util.Properties().apply {
        try { if (settingsFile.isFile) settingsFile.inputStream().use { load(it) } } catch (_: Exception) {}
    }

    val hasPassword: Boolean get() = password != null
    val errorCount: Int get() = issues.count { it.severity == ValidationSeverity.STRUCTURAL_ERROR }
    val warningCount: Int get() = issues.count { it.severity == ValidationSeverity.DOCUMENTARY_WARNING }
    val windowTitle: String get() = project?.let { "${it.name} — OnlyField Asset Manager" } ?: "OnlyField Asset Manager"

    fun update(updated: Project, message: String) = update(updated, message, true)

    private fun update(updated: Project, message: String, persist: Boolean) {
        project?.takeIf { it.id == updated.id }?.let {
            history.addLast(it to message)
            if (history.size > MAX_UNDO) history.removeFirst()
            canUndo = true
        }
        project = updated
        issues = ModelValidator.validateProject(updated).issues
        status = message
        if (persist) save()
    }

    fun saveMapObject(draft: com.onlyfield.assetmanager.core.forms.MapObjectDraft, photos: List<File>, removed: Set<String>): Boolean =
        saveMapEdit(photos, draft.targetType, draft.id, { p -> draft.apply(p).let { it.copy(attachments = it.attachments.filterNot { a -> a.id in removed }) } }) != null

    fun importFloorplan(file: File, areaId: String): Attachment? =
        saveMapEdit(listOf(file), AttachmentTargetType.AREA, areaId, { it })?.singleOrNull()

    private fun saveMapEdit(files: List<File>, type: AttachmentTargetType, targetId: String, transform: (Project) -> Project): List<Attachment>? {
        val before = project ?: return null
        val created = mutableListOf<File>()
        var committed = false
        return try {
            val attachments = files.map { source ->
                val pdf = source.extension.equals("pdf", true)
                val pages = if (pdf) PlanMedia.pageCount(source) else { PlanMedia.validateImage(source); 1 }
                require(pages > 0) { "PDF senza pagine" }
                val att = Attachment(name = source.nameWithoutExtension, originalFileName = source.name, relativePath = "", pageCount = pages,
                    fileType = if (pdf) AttachmentType.PDF else AttachmentType.IMAGE, mimeType = if (pdf) "application/pdf" else java.nio.file.Files.probeContentType(source.toPath()) ?: "image/jpeg",
                    targetType = type, targetId = targetId)
                created += storage.attachmentFile(before.id, att)
                storage.storeAttachmentFile(before.id, att, source)
                att.copy(relativePath = com.onlyfield.assetmanager.exchange.AttachmentFiles.entryName(att))
            }
            val edited = transform(before)
            val saved = edited.copy(attachments = edited.attachments + attachments, updatedEpochMs = System.currentTimeMillis())
            storage.saveProjectLocally(saved, password)
            committed = true
            update(saved, "Salvato.", persist = false)
            error = null
            refreshStoredList()
            attachments
        } catch (e: Exception) {
            if (!committed) created.forEach { it.delete() }
            error = "Salvataggio non riuscito: ${e.message}"
            null
        }
    }

    /** Copies [file] into the data folder and adds it to the project as an attachment. */
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
            val checked = attachment.copy(pageCount = if (attachment.fileType == AttachmentType.PDF) PlanMedia.pageCount(file) else 1)
            storage.storeAttachmentFile(p.id, checked, file)
            val saved = checked.copy(relativePath = AttachmentFiles.entryName(checked))
            update(ProjectEdits.addAttachment(p, saved), "Allegato «${saved.name}» aggiunto.")
        } catch (e: Exception) {
            error = "Impossibile copiare «${file.name}»: ${e.message}"
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
        try {
            java.nio.file.Files.createDirectories(target.parentFile.toPath())
            java.nio.file.Files.write(target.toPath(), snapshot.imageBytes)
            update(ProjectEdits.addAttachment(p, attachment), "Mappa «${attachment.name}» salvata negli allegati.")
            return error == null
        } catch (e: java.io.IOException) {
            java.nio.file.Files.deleteIfExists(target.toPath())
            error = "Impossibile salvare la mappa: ${e.message}"
            return false
        }
    }

    /** Restores the project as it was before the last change. */
    fun undo() {
        val (previous, message) = history.removeLastOrNull() ?: return
        canUndo = history.isNotEmpty()
        project = previous
        issues = ModelValidator.validateProject(previous).issues
        status = "Annullato: $message"
        save()
    }

    private fun clearHistory() {
        history.clear()
        canUndo = false
    }

    fun notify(message: String) {
        status = message
    }

    fun addToTrash(item: TrashItem) {
        trash = trash + item
    }

    private fun save() {
        val p = project ?: return
        try {
            storage.saveProjectLocally(p, password)
            error = null
        } catch (e: Exception) {
            error = "Salvataggio automatico non riuscito: ${e.message ?: "errore sconosciuto"}"
        }
        refreshStoredList()
    }

    fun refreshStoredList() {
        storedProjects = storage.listStoredProjects()
        dataDir = storage.checkDataDirectoryStatus()
    }

    private fun open(newProject: Project, newManifest: PackageManifest?, newPassword: String?, message: String) {
        project?.let { storage.releaseProjectLock(it.id) }
        project = newProject
        manifest = newManifest
        password = newPassword
        clearHistory()
        trashState = if (newProject.isPasswordProtected) emptyList() else storage.loadTrash(newProject.id)
        issues = ModelValidator.validateProject(newProject).issues
        selectedBuId = null
        selectedAreaId = null
        section = AppSection.FLOORPLANS
        status = message
        save()
    }

    // --- Project lifecycle -------------------------------------------------------------------

    /** Creates the project built by the "Nuovo sito" wizard and opens it. */
    fun createProject(wizard: NewSiteWizard) {
        val newPassword = wizard.password
        val newProject = wizard.buildProject().copy(isPasswordProtected = newPassword != null)
        dialog = null
        open(newProject, null, newPassword, "Creato il progetto «${newProject.name}».")
    }

    fun closeProject() {
        project?.let { storage.releaseProjectLock(it.id) }
        project = null
        manifest = null
        password = null
        trashState = emptyList()
        clearHistory()
        issues = emptyList()
        section = AppSection.PROJECT
        status = "Progetto chiuso."
    }

    fun pickAndImport() {
        DesktopStorageHelper.pickOpenFile()?.let { importFile(it) }
    }

    /** Opens a project saved in the data folder, replacing the current one without comparison. */
    fun openStored(file: File) = importFile(file, password = null, compare = false)

    fun importFile(file: File, password: String? = null, compare: Boolean = true) {
        val result = try {
            storage.importPackageFromFile(file, password)
        } catch (e: Exception) {
            error = "Impossibile leggere «${file.name}»: ${e.message}"
            return
        }
        val issuesFound = result.validationResult.issues
        if (issuesFound.any { it.code == "PASSWORD_REQUIRED" || it.code == "INVALID_PACKAGE_PASSWORD" }) {
            dialog = AppDialog.ImportPassword(file, error = if (password != null) "Password errata, riprova." else null)
            return
        }
        val pkg = result.pkg
        if (pkg == null) {
            dialog = null
            error = "Il file «${file.name}» non è un pacchetto valido: " + issuesFound.joinToString("; ") { it.message }
            return
        }
        val current = project
        if (compare && current != null) {
            val comparison = ProjectComparisonEvaluator.evaluate(current, manifest, pkg)
            if (comparison.status != ComparisonStatus.IDENTICAL) {
                dialog = AppDialog.Compare(comparison, pkg, password)
                return
            }
        }
        dialog = null
        storage.extractAttachments(pkg)
        open(pkg.project, pkg.manifest, password, "Aperto «${pkg.project.name}» (${pkg.project.businessUnits.sumOf { it.devices.size }} apparati).")
        if (compare) storage.saveSyncBase(pkg.project, password)
    }

    fun acceptIncoming(pkg: ProjectPackage, incomingPassword: String?) {
        dialog = null
        storage.extractAttachments(pkg)
        open(pkg.project, pkg.manifest, incomingPassword, "Sostituita la copia di lavoro con «${pkg.project.name}».")
        storage.saveSyncBase(pkg.project, incomingPassword)
    }

    /** Three-way merge of the package into the open copy; without conflicts it is applied at once. */
    fun startMerge(pkg: ProjectPackage) {
        val current = project ?: return
        val result = ProjectMerger.merge(storage.loadSyncBase(current.id, password), current, pkg.project)
        val merge = AppDialog.Merge(pkg, result)
        if (result.conflicts.isEmpty()) applyMerge(merge) else dialog = merge
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
        // Through update(): the merge can be undone with Ctrl+Z like any other change.
        update(merge.result.resolve(merge.choices), "Unione completata: ${merge.result.autoApplied} modifiche dal pacchetto, ${merge.choices.size} conflitti risolti.")
        storage.saveSyncBase(merge.pkg.project, password)
    }

    fun exportPackage() {
        val p = project ?: return
        val file = DesktopStorageHelper.pickSaveFile(
            title = "Esporta pacchetto .ofam",
            defaultFileName = "${safeFileName(p.name)}.ofam"
        ) ?: return
        try {
            storage.exportPackageToFile(p, file, password)
            storage.saveSyncBase(p, password)
            val missing = storage.missingAttachments(p)
            error = if (missing.isEmpty()) null
            else "Esportato, ma ${missing.size} allegati non hanno il file sul disco e non sono nel pacchetto: " + missing.joinToString { it.name }
            status = "Esportato in ${file.absolutePath}" + if (password != null) " (cifrato)." else "."
        } catch (e: Exception) {
            error = "Esportazione non riuscita: ${e.message}"
        }
    }

    /** Returns an error message, or null when the password was changed. */
    fun changePassword(current: String, newPassword: String, confirm: String): String? {
        val p = project ?: return "Nessun progetto aperto."
        if (password != null && current != password) return "La password attuale non è corretta."
        if (newPassword != confirm) return "La nuova password e la conferma non coincidono."
        password = newPassword.ifEmpty { null }
        dialog = null
        update(
            p.copy(isPasswordProtected = password != null, updatedEpochMs = System.currentTimeMillis()),
            if (password != null) "Password del progetto impostata." else "Protezione con password rimossa."
        )
        // A password change cannot be undone, and a protected project keeps its trash only in memory.
        clearHistory()
        storage.saveTrash(p.id, if (password != null) emptyList() else trashState)
        return null
    }

    fun shutdown() {
        storage.releaseAllLocks()
    }

    // --- Validation navigation ---------------------------------------------------------------

    /** Section where the entity with [id] is edited, if it exists. */
    fun sectionOf(id: String?): AppSection? {
        val p = project ?: return null
        if (id == null) return null
        val index = ProjectIndex(p)
        return when {
            index.device(id) != null || index.port(id) != null -> AppSection.INVENTORY
            index.rack(id) != null -> AppSection.RACKS
            p.deviceModels.any { it.id == id } -> AppSection.MODELS
            index.area(id) != null || p.attachments.any { it.id == id } || p.floorplanPlacements.any { it.id == id } -> AppSection.FLOORPLANS
            p.cables.any { it.id == id } || p.sharedPathSegments.any { it.id == id } || p.panelMappings.any { it.id == id } -> AppSection.CABLING
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
