package com.onlyfield.assetmanager.core.onboarding

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.*

enum class NewSiteStep(private val titleKey: String, private val hintKey: String, val skippable: Boolean = false) {
    PROJECT("text.b7700d71d0ce", "text.ff3e9f5e4512"),
    SITE("text.e4de7d26b141", "text.2a085f40fe9c"),
    AREA("text.10d0ab90b7a1", "text.f7e120035f34"),
    PASSWORD("text.e7cf3ef4f17c", "text.dad3c0c4fb5c", true),;
    val title: String get() = localizedTitle(Messages())
    val hint: String get() = localizedHint(Messages())
    fun localizedTitle(i18n: Messages): String = i18n.text(titleKey)
    fun localizedHint(i18n: Messages): String = i18n.text(hintKey)
}

data class NewSiteDraft(
    val projectName: String = "",
    val customer: String = "",
    val sites: List<Site> = emptyList(),
    val selectedSiteId: String? = null,
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
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        when (step) {
            NewSiteStep.PROJECT -> if (draft.projectName.isBlank()) put("projectName", i18n.text("text.2eebccc90d26"))
            NewSiteStep.SITE -> if (draft.sites.isEmpty() || draft.sites.any { it.name.isBlank() }) put("sites", i18n.text("text.ad7c1999df92"))
            NewSiteStep.AREA -> if (draft.sites.none { it.areas.isNotEmpty() } || draft.sites.any { b -> b.areas.any { it.name.isBlank() } }) put("areas", i18n.text("text.2463faba3a65"))
            NewSiteStep.PASSWORD -> if (draft.password != draft.passwordConfirm) put("passwordConfirm", i18n.text("text.536706a96f8b"))
        }
    }
    val canProceed get() = errors().isEmpty()
    fun update(transform: (NewSiteDraft) -> NewSiteDraft) = copy(draft = transform(draft))
    fun next() = if (!canProceed || isLast) this else copy(step = NewSiteStep.entries[step.ordinal + 1])
    fun back() = if (isFirst) this else copy(step = NewSiteStep.entries[step.ordinal - 1])
    fun skip() = if (step.skippable) copy(draft = draft.copy(password = "", passwordConfirm = "")) else this
    fun addSite(name: String): NewSiteWizard {
        require(name.isNotBlank())
        val site = Site(name = name.trim())
        return update { it.copy(sites = it.sites + site, selectedSiteId = site.id) }
    }
    fun addArea(siteId: String, name: String): NewSiteWizard {
        require(name.isNotBlank() && draft.sites.any { it.id == siteId })
        return update { it.copy(sites = it.sites.map { b -> if (b.id == siteId) b.copy(areas = b.areas + Area(name = name.trim())) else b }) }
    }
    fun buildProject(now: Long = System.currentTimeMillis()): Project {
        require(draft.projectName.isNotBlank() && draft.sites.isNotEmpty())
        require(draft.sites.all { b -> b.name.isNotBlank() && b.areas.all { it.name.isNotBlank() } } && draft.sites.any { it.areas.isNotEmpty() })
        require(draft.password == draft.passwordConfirm)
        return Project(name = draft.projectName.trim(), description = draft.customer.trim().ifBlank { null },
            sites = draft.sites, createdEpochMs = now, updatedEpochMs = now)
    }
}
