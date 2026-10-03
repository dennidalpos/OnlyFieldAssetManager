package com.onlyfield.assetmanager.core.onboarding

import com.onlyfield.assetmanager.core.model.*

enum class NewSiteStep(val title: String, val hint: String, val skippable: Boolean = false) {
    PROJECT("Progetto", "Dai un nome al lavoro e indica il cliente."),
    BUSINESS_UNIT("Business unit", "Aggiungi le BU del progetto. Puoi aggiungerne altre in seguito."),
    AREA("Piani", "Scegli una BU e aggiungi piani, locali o zone. Ogni voce avrà la propria mappa."),
    PASSWORD("Password", "Facoltativa: protegge il progetto. Puoi impostarla anche in seguito.", true),
}

data class NewSiteDraft(
    val projectName: String = "",
    val customer: String = "",
    val businessUnits: List<BusinessUnit> = emptyList(),
    val selectedBuId: String? = null,
    val password: String = "",
    val passwordConfirm: String = "",
)

/** Shared draft; editing a list preserves the IDs of its remaining entries. */
data class NewSiteWizard(val step: NewSiteStep = NewSiteStep.PROJECT, val draft: NewSiteDraft = NewSiteDraft()) {
    val stepNumber get() = step.ordinal + 1
    val stepCount get() = NewSiteStep.entries.size
    val isFirst get() = step == NewSiteStep.PROJECT
    val isLast get() = step == NewSiteStep.PASSWORD
    val password get() = draft.password.takeIf { it.isNotEmpty() }
    fun errors(): Map<String, String> = buildMap {
        when (step) {
            NewSiteStep.PROJECT -> if (draft.projectName.isBlank()) put("projectName", "Nome progetto obbligatorio")
            NewSiteStep.BUSINESS_UNIT -> if (draft.businessUnits.isEmpty() || draft.businessUnits.any { it.name.isBlank() }) put("businessUnits", "Aggiungi almeno una BU con un nome")
            NewSiteStep.AREA -> if (draft.businessUnits.none { it.areas.isNotEmpty() } || draft.businessUnits.any { b -> b.areas.any { it.name.isBlank() } }) put("areas", "Aggiungi almeno un piano con un nome")
            NewSiteStep.PASSWORD -> if (draft.password != draft.passwordConfirm) put("passwordConfirm", "Le password non coincidono")
        }
    }
    val canProceed get() = errors().isEmpty()
    fun update(transform: (NewSiteDraft) -> NewSiteDraft) = copy(draft = transform(draft))
    fun next() = if (!canProceed || isLast) this else copy(step = NewSiteStep.entries[step.ordinal + 1])
    fun back() = if (isFirst) this else copy(step = NewSiteStep.entries[step.ordinal - 1])
    fun skip() = if (step.skippable) copy(draft = draft.copy(password = "", passwordConfirm = "")) else this
    fun addBusinessUnit(name: String): NewSiteWizard {
        require(name.isNotBlank())
        val bu = BusinessUnit(name = name.trim())
        return update { it.copy(businessUnits = it.businessUnits + bu, selectedBuId = bu.id) }
    }
    fun addArea(buId: String, name: String): NewSiteWizard {
        require(name.isNotBlank() && draft.businessUnits.any { it.id == buId })
        return update { it.copy(businessUnits = it.businessUnits.map { b -> if (b.id == buId) b.copy(areas = b.areas + Area(name = name.trim())) else b }) }
    }
    fun buildProject(now: Long = System.currentTimeMillis()): Project {
        require(draft.projectName.isNotBlank() && draft.businessUnits.isNotEmpty())
        require(draft.businessUnits.all { b -> b.name.isNotBlank() && b.areas.all { it.name.isNotBlank() } } && draft.businessUnits.any { it.areas.isNotEmpty() })
        require(draft.password == draft.passwordConfirm)
        return Project(name = draft.projectName.trim(), description = draft.customer.trim().ifBlank { null },
            businessUnits = draft.businessUnits, createdEpochMs = now, updatedEpochMs = now)
    }
}
