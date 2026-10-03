package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.i18n.AppLanguage
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.util.Locale

class LanguageTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun languageChangeWaitsForTheDraftGuardAndPersists() {
        val storage = DesktopStorageManager(temporary.root)
        val state = DesktopAppState(storage)
        state.detailSlot.dirty = true
        state.changeLanguage(AppLanguage.SPANISH)
        assertEquals(AppLanguage.SYSTEM, state.language)
        assertNotNull(state.detailSlot.pendingChange)
        state.detailSlot.change(state.detailSlot.pendingChange!!)
        assertEquals(AppLanguage.SPANISH, state.language)
        assertEquals("Idioma", state.i18n.text("language.label"))
        assertEquals(AppLanguage.SPANISH, DesktopAppState(DesktopStorageManager(temporary.root)).language)
    }

    @Test
    fun reportsInAllThreeLanguagesAreRealPdfsAndKeepUserText() {
        val project = Project(createdEpochMs = 0, updatedEpochMs = 0, name = "Proyecto del usuario — España", businessUnits = listOf(BusinessUnit(
            name = "BU", devices = listOf(Device(technicalName = "SW-CODICE-001"))
        )), credentials = listOf(Credential(username = "SECRET_USER", secret = "SECRET_VALUE")))
        listOf("it", "en", "es").forEach { language ->
            val out = ByteArrayOutputStream()
            DesktopDocumentManager.exportCompositePdf(project, ExportFilterConfig(), ReportSelection(), out, Messages(Locale.forLanguageTag(language)))
            val text = Loader.loadPDF(out.toByteArray()).use { PDFTextStripper().getText(it) }
            assertTrue(language, text.contains(project.name))
            assertTrue(language, text.contains("SW-CODICE-001"))
            assertTrue(language, text.contains(when (language) { "it" -> "Inventario"; "en" -> "Inventory"; else -> "Inventario" }))
            assertFalse(text.contains("SECRET_USER"))
            assertFalse(text.contains("SECRET_VALUE"))
        }
    }
}
