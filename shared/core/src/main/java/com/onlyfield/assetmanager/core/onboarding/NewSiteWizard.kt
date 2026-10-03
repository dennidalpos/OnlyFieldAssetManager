package com.onlyfield.assetmanager.core.onboarding

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.forms.FieldValidators
import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Project

/** Steps of the "Nuovo sito" wizard, in order. Only [DEVICE] and [PASSWORD] can be skipped. */
enum class NewSiteStep(val title: String, val hint: String, val skippable: Boolean = false) {
    PROJECT("Progetto", "Dai un nome al lavoro e indica il cliente."),
    BUSINESS_UNIT("Sede", "La sede o business unit dove lavori; potrai aggiungerne altre."),
    AREA("Prima area", "Un locale della sede: sala server, piano, armadio di zona."),
    DEVICE("Primo apparato", "Il primo apparato da censire. Puoi saltare e aggiungerlo dopo.", skippable = true),
    PASSWORD("Password", "Facoltativa: cifra il progetto e i pacchetti esportati. Non è recuperabile.", skippable = true),
}

data class NewSiteDraft(
    val projectName: String = "",
    val customer: String = "",
    val businessUnit: String = "Sede principale",
    val area: String = "",
    val deviceName: String = "",
    val deviceIp: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
)

/** Immutable wizard state shared by both apps: UIs render [step] and call [next]/[back]/[skip]. */
data class NewSiteWizard(val step: NewSiteStep = NewSiteStep.PROJECT, val draft: NewSiteDraft = NewSiteDraft()) {
    val stepNumber: Int get() = step.ordinal + 1
    val stepCount: Int get() = NewSiteStep.entries.size
    val isFirst: Boolean get() = step.ordinal == 0
    val isLast: Boolean get() = step == NewSiteStep.entries.last()

    /** Field errors of the current step (field name -> Italian message); empty when the step is complete. */
    fun errors(): Map<String, String> = buildMap {
        when (step) {
            NewSiteStep.PROJECT -> FieldValidators.required(draft.projectName, "Nome progetto")?.let { put("projectName", it) }
            NewSiteStep.BUSINESS_UNIT -> FieldValidators.required(draft.businessUnit, "Nome sede")?.let { put("businessUnit", it) }
            NewSiteStep.AREA -> FieldValidators.required(draft.area, "Nome area")?.let { put("area", it) }
            NewSiteStep.DEVICE -> {
                if (draft.deviceName.isBlank() && draft.deviceIp.isNotBlank()) put("deviceName", "Nome apparato obbligatorio")
                FieldValidators.ipv4(draft.deviceIp)?.let { put("deviceIp", it) }
            }
            NewSiteStep.PASSWORD ->
                if (draft.password != draft.passwordConfirm) put("passwordConfirm", "Le password non coincidono")
        }
    }

    val canProceed: Boolean get() = errors().isEmpty()

    fun update(transform: (NewSiteDraft) -> NewSiteDraft): NewSiteWizard = copy(draft = transform(draft))

    /** Next step, or this same state when the current step has errors or is the last. */
    fun next(): NewSiteWizard =
        if (!canProceed || isLast) this else copy(step = NewSiteStep.entries[step.ordinal + 1])

    fun back(): NewSiteWizard = if (isFirst) this else copy(step = NewSiteStep.entries[step.ordinal - 1])

    /** Clears the optional step's fields and moves on (on the last step it only clears them). */
    fun skip(): NewSiteWizard {
        if (!step.skippable) return this
        val cleared = when (step) {
            NewSiteStep.DEVICE -> draft.copy(deviceName = "", deviceIp = "")
            NewSiteStep.PASSWORD -> draft.copy(password = "", passwordConfirm = "")
            else -> draft
        }
        return copy(draft = cleared).let { if (it.isLast) it else it.copy(step = NewSiteStep.entries[step.ordinal + 1]) }
    }

    /** Password chosen in the wizard, or null for an unprotected project. */
    val password: String? get() = draft.password.takeIf { it.isNotEmpty() }

    /** Builds the new project; the caller stores it and applies [password] with its own mechanism. */
    fun buildProject(now: Long = System.currentTimeMillis()): Project {
        val d = draft
        var project = Project(
            name = d.projectName.trim(),
            description = d.customer.trim().ifBlank { null },
            createdEpochMs = now,
            updatedEpochMs = now,
        )
        project = ProjectEdits.addBusinessUnit(project, d.businessUnit.trim())
        val buId = project.businessUnits.single().id
        val area = Area(name = d.area.trim())
        project = ProjectEdits.addArea(project, buId, area)
        if (d.deviceName.isNotBlank()) {
            val device = Device(technicalName = d.deviceName.trim(), ipAddress = d.deviceIp.trim().ifBlank { null }, areaId = area.id)
            project = ProjectEdits.addDevice(project, buId, device)
        }
        return project.copy(createdEpochMs = now, updatedEpochMs = now)
    }
}
