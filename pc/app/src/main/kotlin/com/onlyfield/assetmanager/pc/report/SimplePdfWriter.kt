package com.onlyfield.assetmanager.pc.report

import com.onlyfield.assetmanager.core.i18n.Messages

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset

/** Dependency-free PDF 1.4 writer for paginated WinAnsi text. */
object SimplePdfWriter {

    private const val PAGE_W = 595f
    private const val PAGE_H = 842f
    private const val MARGIN = 50f
    private val winAnsi: Charset = Charset.forName("windows-1252")

    private data class Style(val font: String, val size: Float, val before: Float, val indentPt: Float = 0f)

    private fun styleOf(line: ReportLine): Style = when (line) {
        is ReportLine.Title -> Style("F2", 18f, 0f)
        is ReportLine.Meta -> Style("F1", 9.5f, 4f)
        is ReportLine.Heading -> Style("F2", 14f, 10f)
        is ReportLine.SubHeading -> Style("F2", 11f, 6f)
        is ReportLine.Item -> Style("F1", 10f, 2f, 12f + line.indent * 14f)
        ReportLine.Spacer -> Style("F1", 6f, 0f)
    }

    /** Wraps [text] to [width] points. */
    private fun wrap(text: String, size: Float, width: Float): List<String> {
        val maxChars = (width / (size * 0.5f)).toInt().coerceAtLeast(10)
        if (text.length <= maxChars) return listOf(text)
        val out = mutableListOf<String>()
        var current = StringBuilder()
        for (word in text.split(' ')) {
            if (current.isNotEmpty() && current.length + 1 + word.length > maxChars) {
                out += current.toString(); current = StringBuilder("  ")
            }
            if (current.isNotEmpty() && !current.endsWith("  ")) current.append(' ')
            current.append(word)
        }
        if (current.isNotBlank()) out += current.toString()
        return out
    }

    private fun escape(text: String): ByteArray {
        val encoded = text.toByteArray(winAnsi)
        val out = ByteArrayOutputStream()
        for (b in encoded) {
            val c = b.toInt() and 0xFF
            if (c == '('.code || c == ')'.code || c == '\\'.code) out.write('\\'.code)
            out.write(c)
        }
        return out.toByteArray()
    }

    fun write(lines: List<ReportLine>, title: String, out: OutputStream, i18n: Messages = Messages()) {
        val pages = mutableListOf<ByteArrayOutputStream>()
        var page = ByteArrayOutputStream().also { pages += it }
        var y = PAGE_H - MARGIN
        fun newPage() { page = ByteArrayOutputStream().also { pages += it }; y = PAGE_H - MARGIN }

        for (line in lines) {
            val style = styleOf(line)
            val bullet = if (line is ReportLine.Item) "• " else ""
            val wrapped = if (line is ReportLine.Spacer) listOf("") else wrap(bullet + line.text, style.size, PAGE_W - 2 * MARGIN - style.indentPt)
            val needed = style.before + wrapped.size * style.size * 1.3f
            val reserve = if (line is ReportLine.Heading || line is ReportLine.SubHeading) style.size * 3 else 0f
            if (y - needed - reserve < MARGIN) newPage()
            y -= style.before
            for (text in wrapped) {
                y -= style.size * 1.3f
                if (text.isEmpty()) continue
                page.write("BT /${style.font} ${style.size} Tf ${MARGIN + style.indentPt} $y Td (".toByteArray(Charsets.US_ASCII))
                page.write(escape(text))
                page.write(") Tj ET\n".toByteArray(Charsets.US_ASCII))
            }
        }

        pages.forEachIndexed { i, p ->
            p.write("BT /F1 8 Tf $MARGIN 30 Td (".toByteArray(Charsets.US_ASCII))
            p.write(escape(i18n.text("text.657bcc42ca8e", title, i + 1, pages.size)))
            p.write(") Tj ET\n".toByteArray(Charsets.US_ASCII))
        }

        val objects = mutableListOf<ByteArray>()
        val firstPageObj = 6
        val kids = pages.indices.joinToString(" ") { "${firstPageObj + it * 2} 0 R" }
        objects += "<< /Type /Catalog /Pages 2 0 R >>".toByteArray()
        objects += "<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>".toByteArray()
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".toByteArray()
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>".toByteArray()
        objects += ByteArrayOutputStream().apply {
            write("<< /Title (".toByteArray()); write(escape(title)); write(") /Producer (OnlyField Asset Manager) >>".toByteArray())
        }.toByteArray()
        pages.forEachIndexed { i, p ->
            val contentObj = firstPageObj + i * 2 + 1
            objects += ("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_W $PAGE_H] " +
                "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents $contentObj 0 R >>").toByteArray()
            val content = p.toByteArray()
            objects += ByteArrayOutputStream().apply {
                write("<< /Length ${content.size} >>\nstream\n".toByteArray()); write(content); write("\nendstream".toByteArray())
            }.toByteArray()
        }

        val pdf = ByteArrayOutputStream()
        pdf.write("%PDF-1.4\n%âãÏÓ\n".toByteArray(Charsets.ISO_8859_1))
        val offsets = mutableListOf<Int>()
        objects.forEachIndexed { i, body ->
            offsets += pdf.size()
            pdf.write("${i + 1} 0 obj\n".toByteArray()); pdf.write(body); pdf.write("\nendobj\n".toByteArray())
        }
        val xref = pdf.size()
        pdf.write("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n".toByteArray())
        offsets.forEach { pdf.write("%010d 00000 n \n".format(it).toByteArray()) }
        pdf.write("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R /Info 5 0 R >>\nstartxref\n$xref\n%%EOF\n".toByteArray())
        out.write(pdf.toByteArray())
        out.flush()
    }
}
