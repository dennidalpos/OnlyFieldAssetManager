package com.onlyfield.assetmanager.configurator

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Project

enum class ProjectDestination(val labelKey: String, val groupKey: String) {
    MAP("ux.nav.map", "ux.nav.work"),
    DEVICES("ux.nav.devices", "ux.nav.work"),
    RACKS("text.4cd265c2b8c6", "ux.nav.work"),
    CABLING("text.3b40d8bd6081", "ux.nav.work"),
    NETWORK("text.a0dd274e04a0", "ux.nav.technical"),
    POWER("text.acedc1948e5f", "ux.nav.technical"),
    MODELS("text.7351fc8f354e", "ux.nav.support"),
    ATTACHMENTS("ux.nav.attachments", "ux.nav.support"),
    DOCUMENTS("text.f7ac8562de3a", "ux.nav.support"),
    CREDENTIALS("text.52f7e6721e97", "ux.nav.support"),
    PROJECT("ux.nav.structure", "ux.nav.project"),
    TRASH("text.9a3a36d5fa15", "ux.nav.project");

    fun title(i18n: Messages): String = i18n.text(labelKey)

    /** Everyday work areas: bottom bar on phones, top of the sidebar on PC. */
    val primary get() = groupKey == "ux.nav.work"

    val icon get() = when (this) {
        MAP -> SymbolIcons.map
        DEVICES -> SymbolIcons.inventory2
        RACKS -> SymbolIcons.dns
        CABLING -> SymbolIcons.cable
        NETWORK -> SymbolIcons.lan
        POWER -> SymbolIcons.bolt
        MODELS -> SymbolIcons.category
        ATTACHMENTS -> SymbolIcons.attachFile
        CREDENTIALS -> SymbolIcons.key
        DOCUMENTS -> SymbolIcons.description
        PROJECT -> SymbolIcons.settings
        TRASH -> SymbolIcons.delete
    }
}

/**
 * Optional modules: always kept in the model and in exports, but out of the way until a project uses
 * them or the user opens "Other modules" (session only). Video surveillance has no screen of its own.
 */
enum class SecondaryModule(val labelKey: String) {
    CREDENTIALS("text.52f7e6721e97"),
    CONFIGURATIONS("text.acac03d2f58d"),
    BADGES("text.af354b531994");

    fun used(project: Project): Boolean = when (this) {
        CREDENTIALS -> project.credentials.isNotEmpty()
        CONFIGURATIONS -> project.deviceConfigurations.isNotEmpty()
        BADGES -> project.documentBadges.isNotEmpty()
    }

    fun shown(project: Project, showAll: Boolean) = showAll || used(project)

    companion object {
        /** Unused modules, listed under "Other modules". */
        fun hidden(project: Project): List<SecondaryModule> = entries.filterNot { it.used(project) }
    }
}

/** Navigation entries for [project]: Credentials only when used or [showAll]. */
fun ProjectDestination.shown(project: Project, showAll: Boolean) =
    this != ProjectDestination.CREDENTIALS || SecondaryModule.CREDENTIALS.shown(project, showAll)

/** Indexes of the sub-tabs to show; [optional] maps a tab index to its module. */
fun visibleTabs(count: Int, project: Project, showAll: Boolean, optional: Map<Int, SecondaryModule>): List<Int> =
    (0 until count).filter { i -> optional[i]?.shown(project, showAll) ?: true }
