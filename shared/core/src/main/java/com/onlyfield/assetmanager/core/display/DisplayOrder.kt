package com.onlyfield.assetmanager.core.display

import com.onlyfield.assetmanager.core.i18n.Messages
import java.text.Collator

/** Sorts a presentation copy without changing the saved or technical order. */
fun <T> List<T>.sortedForDisplay(i18n: Messages, name: (T) -> String): List<T> =
    sortedWith(compareBy(Collator.getInstance(i18n.locale), name))
