package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.scan.LabelCode
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset

/** QR label content. */
data class QrLabel(val code: LabelCode, val title: String, val subtitle: String)

/** Shared A4 PDF sheet of QR labels. */
object LabelSheetPdf {
    private const val PAGE_W = 595f
    private const val PAGE_H = 842f
    private const val COLS = 3
    private const val ROWS = 7
    private const val MARGIN_X = 20f
    private const val MARGIN_Y = 25f
    private const val QR_SIZE = 78f
    /** Quiet zone around the code. */
    private const val QUIET = 12f
    private val winAnsi: Charset = Charset.forName("windows-1252")

    /** Labels project devices, racks and cables. */
    fun labelsFor(project: Project, i18n: Messages = Messages()): List<QrLabel> {
        val index = ProjectIndex(project)
        val devices = index.devices.map { d ->
            QrLabel(LabelCode(project.id, LabelCode.Type.DEVICE, d.id), d.technicalName,
                listOfNotNull(d.physicalLabel, d.areaId?.let { index.areaName(it) }).joinToString(" · ").ifBlank { i18n.text("text.cf301d95d32c") })
        }
        val racks = project.racks.map { r ->
            QrLabel(LabelCode(project.id, LabelCode.Type.RACK, r.id), r.name, i18n.text("labels.rack", index.areaName(r.areaId, i18n.text("text.1abc7243c3dd"))))
        }
        val cables = project.cables.filter { !it.codeOrLabel.isNullOrBlank() }.map { c ->
            QrLabel(LabelCode(project.id, LabelCode.Type.CABLE, c.id), c.codeOrLabel!!, i18n.text("labels.cable", index.portLabel(c.portAId), index.portLabel(c.portBId)))
        }
        return devices + racks + cables
    }

    fun write(labels: List<QrLabel>, out: OutputStream) {
        val cellW = (PAGE_W - 2 * MARGIN_X) / COLS
        val cellH = (PAGE_H - 2 * MARGIN_Y) / ROWS
        val pages = labels.chunked(COLS * ROWS).ifEmpty { listOf(emptyList()) }.map { pageLabels ->
            val content = ByteArrayOutputStream()
            pageLabels.forEachIndexed { i, label ->
                val left = MARGIN_X + (i % COLS) * cellW
                val top = PAGE_H - MARGIN_Y - (i / COLS) * cellH
                content.ascii("0.85 G 0.5 w $left ${top - cellH} $cellW $cellH re S 0 g\n")
                drawQr(content, label.code.toString(), left + QUIET, top - QUIET - QR_SIZE)
                val textX = left + QR_SIZE + QUIET + 4f
                val maxChars = ((cellW - QR_SIZE - QUIET - 8f) / 4.2f).toInt()
                content.text("F2", 9f, textX, top - 24f, label.title.take(maxChars))
                label.subtitle.chunked(maxChars + 4).take(3).forEachIndexed { line, part ->
                    content.text("F1", 7f, textX, top - 38f - line * 9f, part)
                }
            }
            content.toByteArray()
        }
        writePdf(pages, out)
    }

    private fun drawQr(out: ByteArrayOutputStream, payload: String, x: Float, y: Float) {
        val matrix = QRCodeWriter().encode(
            payload, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0)
        )
        val module = QR_SIZE / matrix.width
        val sb = StringBuilder()
        for (row in 0 until matrix.height) for (col in 0 until matrix.width) {
            if (matrix.get(col, row)) {
                // PDF origin is bottom-left.
                sb.append("%.2f %.2f %.2f %.2f re ".format(java.util.Locale.ROOT, x + col * module, y + QR_SIZE - (row + 1) * module, module, module))
            }
        }
        out.ascii(sb.append("f\n").toString())
    }

    private fun ByteArrayOutputStream.ascii(s: String) = write(s.toByteArray(Charsets.US_ASCII))

    private fun ByteArrayOutputStream.text(font: String, size: Float, x: Float, y: Float, text: String) {
        ascii("BT /$font $size Tf $x $y Td (")
        for (b in text.toByteArray(winAnsi)) {
            val c = b.toInt() and 0xFF
            if (c == '('.code || c == ')'.code || c == '\\'.code) write('\\'.code)
            write(c)
        }
        ascii(") Tj ET\n")
    }

    private fun writePdf(pages: List<ByteArray>, out: OutputStream) {
        val objects = mutableListOf<ByteArray>()
        val kids = pages.indices.joinToString(" ") { "${5 + it * 2} 0 R" }
        objects += "<< /Type /Catalog /Pages 2 0 R >>".toByteArray()
        objects += "<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>".toByteArray()
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".toByteArray()
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>".toByteArray()
        pages.forEachIndexed { i, content ->
            objects += ("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_W $PAGE_H] " +
                "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents ${6 + i * 2} 0 R >>").toByteArray()
            objects += ByteArrayOutputStream().apply {
                write("<< /Length ${content.size} >>\nstream\n".toByteArray()); write(content); write("\nendstream".toByteArray())
            }.toByteArray()
        }
        val pdf = ByteArrayOutputStream()
        pdf.write("%PDF-1.4\n".toByteArray())
        val offsets = objects.mapIndexed { i, body ->
            pdf.size().also { pdf.write("${i + 1} 0 obj\n".toByteArray()); pdf.write(body); pdf.write("\nendobj\n".toByteArray()) }
        }
        val xref = pdf.size()
        pdf.write("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n".toByteArray())
        offsets.forEach { pdf.write("%010d 00000 n \n".format(it).toByteArray()) }
        pdf.write("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n".toByteArray())
        out.write(pdf.toByteArray())
        out.flush()
    }
}
