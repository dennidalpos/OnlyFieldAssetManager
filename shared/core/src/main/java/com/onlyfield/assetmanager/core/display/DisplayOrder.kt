package com.onlyfield.assetmanager.core.display

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Site
import java.text.Collator

/** Sorts a presentation copy without changing the saved or technical order. */
fun <T> List<T>.sortedForDisplay(i18n: Messages, name: (T) -> String): List<T> =
    sortedWith(compareBy(Collator.getInstance(i18n.locale), name))

/** Sites by group (ungrouped first), then by name. */
fun List<Site>.sitesForDisplay(i18n: Messages): List<Site> {
    val collator = Collator.getInstance(i18n.locale)
    return sortedWith(compareBy<Site, String>(collator) { it.group?.trim().orEmpty() }.thenBy(collator) { it.name })
}

/** "Group · Site", or the name alone. */
fun Site.displayName(): String = group?.takeIf { it.isNotBlank() }?.let { "$it · $name" } ?: name
