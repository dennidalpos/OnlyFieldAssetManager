package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class XlsxTextTest {
    @Test fun controlsLiteralEscapesUnicodeAndWhitespacePreserveCellTextInEveryLanguage() {
        val text = "  à España 中文 😀 &<>\"' _x0041_ _X00af_ _x0001_x0002_ " +
            (0..31).map { it.toChar() }.joinToString("") + "\uFFFE\uFFFF\uD800\uDC00\uD800 end  "
        val device = Device(technicalName = text)
        val project = Project(name = text, createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = text, devices = listOf(device))))
        for (lang in listOf("it", "en", "es")) {
            val bytes = ByteArrayOutputStream().also { XlsxExportManager.exportXlsxToStream(project, ExportFilterConfig(), it, Messages(Locale.forLanguageTag(lang))) }.toByteArray()
            val documents = linkedMapOf<String, org.w3c.dom.Document>()
            ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    documents[entry.name] = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(zip.readBytes().inputStream())
                    entry = zip.nextEntry
                }
            }
            val cells = documents.getValue("xl/worksheets/sheet1.xml").getElementsByTagName("c")
            val deviceCell = (0 until cells.length).map { cells.item(it) as org.w3c.dom.Element }.single { it.getAttribute("r") == "D2" }
            assertEquals("inlineStr", deviceCell.getAttribute("t"))
            val node = deviceCell.getElementsByTagName("t").item(0) as org.w3c.dom.Element
            assertEquals("preserve", node.getAttribute("xml:space"))
            // Decode once: newly exposed literal escapes must not be decoded again.
            val restored = Regex("_x([0-9a-fA-F]{4})_").replace(node.textContent) { it.groupValues[1].toInt(16).toChar().toString() }
            assertEquals(lang, text, restored)
            assertTrue(node.textContent.contains("_x005F_x0041_"))
            assertTrue(node.textContent.contains("_x000D_"))
            assertTrue(node.textContent.contains('\n'))
            assertTrue(node.textContent.contains('\t'))
            assertFalse(node.textContent.contains("_x000A_"))
            assertFalse(node.textContent.contains("_x0009_"))
            val numeric = (0 until cells.length).map { cells.item(it) as org.w3c.dom.Element }.single { it.getAttribute("r") == "L2" }
            assertFalse(numeric.hasAttribute("t"))
            assertEquals(device.ports.size.toString(), numeric.textContent)
        }
        assertEquals(text, device.technicalName)
    }
}
