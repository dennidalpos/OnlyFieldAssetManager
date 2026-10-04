package com.onlyfield.assetmanager.core

import com.onlyfield.assetmanager.core.forms.FieldValidators
import com.onlyfield.assetmanager.core.i18n.AppLanguage
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.validation.ModelValidator
import org.junit.Assert.*
import org.junit.Test
import java.text.MessageFormat
import java.util.Locale
import java.util.PropertyResourceBundle

class MessagesTest {
    private fun bundle(language: String): PropertyResourceBundle = javaClass.classLoader
        .getResourceAsStream("com/onlyfield/assetmanager/core/i18n/messages_$language.properties")!!
        .reader(Charsets.UTF_8).use { PropertyResourceBundle(it) }

    @Test
    fun catalogsHaveTheSameKeysAndFormattingArguments() {
        val catalogs = listOf("it", "en", "es").associateWith(::bundle)
        val italian = catalogs.getValue("it")
        catalogs.forEach { (language, catalog) ->
            assertEquals(language, italian.keySet(), catalog.keySet())
            catalog.keySet().forEach { key ->
                val pattern = catalog.getString(key)
                val arguments = Regex("\\{(\\d+)}").findAll(pattern).map { it.groupValues[1] }.toSet()
                assertEquals("$language/$key", Regex("\\{(\\d+)}").findAll(italian.getString(key)).map { it.groupValues[1] }.toSet(), arguments)
                val formatted = MessageFormat(pattern, Locale.forLanguageTag(language)).format(Array(20) { "ARG_$it" })
                arguments.forEach { assertTrue("$language/$key lost argument $it", formatted.contains("ARG_$it")) }
            }
        }
    }

    @Test
    fun unsupportedLanguagesFallBackToItalianAndValidatorsKeepTheirCodes() {
        assertEquals(Locale.ITALIAN, AppLanguage.SYSTEM.resolve(Locale.GERMAN))
        assertEquals("Language", Messages(Locale.ENGLISH).text("language.label"))
        assertEquals("Idioma", Messages(Locale.forLanguageTag("es")).text("language.label"))
        assertEquals("Lingua", Messages(Locale.GERMAN).text("language.label"))
        val project = Project(createdEpochMs = 0, updatedEpochMs = 0, id = "invalid-project-id", name = "Nombre del usuario")
        val italian = ModelValidator.validateProject(project)
        val english = ModelValidator.validateProject(project, Messages(Locale.ENGLISH))
        val spanish = ModelValidator.validateProject(project, Messages(Locale.forLanguageTag("es")))
        assertEquals(italian.issues.map { it.code }, english.issues.map { it.code })
        assertEquals(italian.issues.map { it.code }, spanish.issues.map { it.code })
        assertNotEquals(italian.issues.first().message, english.issues.first().message)
        assertNotEquals(italian.issues.first().message, spanish.issues.first().message)
        assertTrue(FieldValidators.required("", "Mi equipo", Messages(Locale.forLanguageTag("es")))!!.contains("Mi equipo"))
    }

    @Test
    fun pluralUsesSingularOnlyForOne() {
        val it = Messages()
        assertEquals("1 cavo", it.plural("map.cableCount", 1))
        assertEquals("0 cavi", it.plural("map.cableCount", 0))
        assertEquals("2 devices", Messages(Locale.ENGLISH).plural("config.devicesCount", 2))
    }
}
