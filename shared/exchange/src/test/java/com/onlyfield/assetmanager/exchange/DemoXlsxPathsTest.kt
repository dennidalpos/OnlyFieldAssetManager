package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.PathSchematics
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class DemoXlsxPathsTest {
    private val project = DemoSeed.build()

    private fun entries(): Map<String, String> {
        val out = ByteArrayOutputStream()
        XlsxExportManager.exportXlsxToStream(project, ExportFilterConfig(), out)
        // Kept for opening in Excel by hand.
        java.io.File("build/reports").apply { mkdirs() }.resolve("demo.xlsx").writeBytes(out.toByteArray())
        val result = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            generateSequence { zip.nextEntry }.forEach { result[it.name] = zip.readBytes().toString(Charsets.UTF_8) }
        }
        return result
    }

    @Test fun pathsSheetListsEveryPathOnceWithBothEnds() {
        val files = entries()
        // Every part is well-formed XML and the new sheet is registered.
        val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        files.forEach { (name, xml) -> parser.parse(ByteArrayInputStream(xml.toByteArray())).also { assertTrue(name, it.documentElement != null) } }
        assertTrue(files.getValue("xl/workbook.xml").contains("sheetId=\"6\" r:id=\"rId7\""))
        assertTrue(files.getValue("xl/_rels/workbook.xml.rels").contains("Id=\"rId7\""))
        assertTrue(files.getValue("[Content_Types].xml").contains("sheet6.xml"))

        val sheet = files.getValue("xl/worksheets/sheet6.xml")
        val rows = Regex("<row r=\"").findAll(sheet).count()
        assertEquals(PathSchematics.all(project).size + 1, rows)
        listOf("Apparato A", "Passanti attraversati", "PR-COM-MED", "SW-MED-PT-A", "GB-TEA-PT-01", "Ponte radio", "Percorso completo").forEach {
            assertTrue(it, sheet.contains(it))
        }
        // Headers are bold and frozen on every sheet.
        files.filterKeys { it.startsWith("xl/worksheets/") }.values.forEach { assertTrue(it.contains("state=\"frozen\"")); assertTrue(it.contains("s=\"1\"")) }
    }
}
