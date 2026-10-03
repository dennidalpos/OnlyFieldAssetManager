package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class LocalizedExportsTest {
    @Test
    fun devicesOnDirectBusinessUnitFloorsAreIncluded() {
        val floor = Area(name = "Planta del usuario")
        val project = Project(name = "Proyecto", createdEpochMs = 0, updatedEpochMs = 0,
            businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(floor), devices = listOf(
                Device(technicalName = "SW-FLOOR-001", areaId = floor.id)
            ))))
        val messages = Messages(Locale.ENGLISH)
        val markdown = ByteArrayOutputStream().also {
            MarkdownExportManager.exportMarkdownToStream(project, ExportFilterConfig(selectedAreaId = floor.id), it, messages)
        }.toString(Charsets.UTF_8)
        assertTrue(markdown.contains("SW-FLOOR-001"))
        val bytes = ByteArrayOutputStream().also {
            XlsxExportManager.exportXlsxToStream(project, ExportFilterConfig(selectedAreaId = floor.id), it, messages)
        }.toByteArray()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            val content = generateSequence { zip.nextEntry }.map { zip.readBytes().toString(Charsets.UTF_8) }.joinToString()
            assertTrue(content.contains("SW-FLOOR-001"))
        }
    }

    @Test
    fun exportsTranslateLabelsPreserveUserContentAndExcludeCredentials() {
        val project = Project(createdEpochMs = 0, updatedEpochMs = 0, name = "Sede dell'utente — España", businessUnits = listOf(BusinessUnit(
            name = "BU del usuario", devices = listOf(Device(technicalName = "SW-CODICE-001"))
        )), credentials = listOf(Credential(username = "SECRET_USER", secret = "SECRET_VALUE")))
        listOf("it" to "Nome", "en" to "Name", "es" to "Nombre").forEach { (language, header) ->
            val messages = Messages(Locale.forLanguageTag(language))
            val markdown = ByteArrayOutputStream().also { MarkdownExportManager.exportMarkdownToStream(project, ExportFilterConfig(), it, messages) }.toString(Charsets.UTF_8)
            assertTrue(language, markdown.contains(project.name))
            assertTrue(language, markdown.contains("SW-CODICE-001"))
            assertTrue(language, markdown.contains(header))
            assertFalse(markdown.contains("SECRET_USER"))
            assertFalse(markdown.contains("SECRET_VALUE"))
            val bytes = ByteArrayOutputStream().also { XlsxExportManager.exportXlsxToStream(project, ExportFilterConfig(), it, messages) }.toByteArray()
            val entries = mutableMapOf<String, String>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry -> entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8) }
            }
            entries.values.forEach { xml -> DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray())) }
            val workbook = entries.getValue("xl/workbook.xml")
            assertTrue(language, workbook.contains(when (language) { "it" -> "Inventario Apparati"; "en" -> "Equipment Inventory"; else -> "Inventario de equipos" }))
            assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("SW-CODICE-001"))
            assertFalse(entries.values.any { it.contains("SECRET_USER") || it.contains("SECRET_VALUE") })
        }
    }
}
