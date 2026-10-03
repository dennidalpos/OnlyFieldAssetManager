package com.onlyfield.assetmanager.pc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.onlyfield.assetmanager.core.display.ProjectIndex
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
import java.io.File

/** Main areas of the editor, in navigation-rail order. */
enum class AppSection(val title: String, val icon: String, val needsProject: Boolean = true) {
    INVENTORY("Inventario", "📦"),
    RACKS("Rack", "🗄️"),
    MODELS("Modelli", "📐"),
    FLOORPLANS("Planimetrie", "🗺️"),
    CABLING("Cablaggio", "🔌"),
    NETWORK("Rete", "🌐"),
    POWER("Alimentazione", "⚡"),
    TRASH("Cestino", "🗑️"),
    PROJECT("Progetto", "⚙️", needsProject = false),
}

sealed interface AppDialog {
    data object NewProject : AppDialog
    data class ImportPassword(val file: File, val error: String? = null) : AppDialog
    data object ManagePassword : AppDialog
    data class Compare(val comparison: ProjectComparison, val pkg: ProjectPackage, val password: String?) : AppDialog
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

    val hasPassword: Boolean get() = password != null
    val errorCount: Int get() = issues.count { it.severity == ValidationSeverity.STRUCTURAL_ERROR }
    val warningCount: Int get() = issues.count { it.severity == ValidationSeverity.DOCUMENTARY_WARNING }
    val windowTitle: String get() = project?.let { "${it.name} — OnlyField Asset Manager" } ?: "OnlyField Asset Manager"

    fun update(updated: Project, message: String) {
        project?.takeIf { it.id == updated.id }?.let {
            history.addLast(it to message)
            if (history.size > MAX_UNDO) history.removeFirst()
            canUndo = true
        }
        project = updated
        issues = ModelValidator.validateProject(updated).issues
        status = message
        save()
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
            storage.storeAttachmentFile(p.id, attachment, file)
            val saved = attachment.copy(relativePath = AttachmentFiles.entryName(attachment))
            update(ProjectEdits.addAttachment(p, saved), "Allegato «${saved.name}» aggiunto.")
        } catch (e: Exception) {
            error = "Impossibile copiare «${file.name}»: ${e.message}"
        }
    }

    fun attachmentFile(attachment: Attachment): File? =
        project?.let { storage.attachmentFile(it.id, attachment) }?.takeIf { it.isFile }

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
        if (section == AppSection.PROJECT) section = AppSection.INVENTORY
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
    }

    fun acceptIncoming(pkg: ProjectPackage, incomingPassword: String?) {
        dialog = null
        storage.extractAttachments(pkg)
        open(pkg.project, pkg.manifest, incomingPassword, "Sostituita la copia di lavoro con «${pkg.project.name}».")
    }

    fun exportPackage() {
        val p = project ?: return
        val file = DesktopStorageHelper.pickSaveFile(
            title = "Esporta pacchetto .ofam",
            defaultFileName = "${safeFileName(p.name)}.ofam"
        ) ?: return
        try {
            storage.exportPackageToFile(p, file, password)
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
