package com.onlyfield.assetmanager.configurator

import com.onlyfield.assetmanager.core.i18n.Messages

enum class ProjectDestination(val labelKey: String, val groupKey: String) {
    MAP("ux.nav.map", "ux.nav.work"),
    DEVICES("ux.nav.devices", "ux.nav.work"),
    RACKS("text.4cd265c2b8c6", "ux.nav.work"),
    CABLING("text.3b40d8bd6081", "ux.nav.work"),
    NETWORK("text.a0dd274e04a0", "ux.nav.technical"),
    POWER("text.acedc1948e5f", "ux.nav.technical"),
    MODELS("text.7351fc8f354e", "ux.nav.support"),
    ATTACHMENTS("ux.nav.attachments", "ux.nav.support"),
    CREDENTIALS("text.52f7e6721e97", "ux.nav.support"),
    DOCUMENTS("text.f7ac8562de3a", "ux.nav.support"),
    PROJECT("ux.nav.structure", "ux.nav.project"),
    TRASH("text.9a3a36d5fa15", "ux.nav.project");

    fun title(i18n: Messages): String = i18n.text(labelKey)

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
