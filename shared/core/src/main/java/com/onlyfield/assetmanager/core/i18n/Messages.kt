package com.onlyfield.assetmanager.core.i18n

import java.text.MessageFormat
import java.util.Locale
import java.util.PropertyResourceBundle

enum class AppLanguage(val tag: String, val nativeName: String) {
    SYSTEM("", "Sistema"), ITALIAN("it", "Italiano"), ENGLISH("en", "English"), SPANISH("es", "Español");

    fun resolve(system: Locale = Locale.getDefault()): Locale {
        val language = if (this == SYSTEM) system.language else tag
        return Locale.forLanguageTag(language.takeIf { it in setOf("it", "en", "es") } ?: "it")
    }
}

/** Immutable locale snapshot, shared by both apps and document exporters. */
class Messages(val locale: Locale = Locale.ITALIAN) {
    private val language = locale.language.takeIf { it in setOf("it", "en", "es") } ?: "it"
    private val bundle = bundles.getValue(language)

    fun text(key: String, vararg arguments: Any?): String =
        MessageFormat(bundle.getString(key), locale).format(arguments.map { it.toString() }.toTypedArray())

    companion object {
        private val bundles = listOf("it", "en", "es").associateWith { language ->
            val path = "com/onlyfield/assetmanager/core/i18n/messages_$language.properties"
            val stream = requireNotNull(Messages::class.java.classLoader.getResourceAsStream(path)) { "Missing language catalog: $language" }
            stream.reader(Charsets.UTF_8).use { PropertyResourceBundle(it) }
        }
    }
}
