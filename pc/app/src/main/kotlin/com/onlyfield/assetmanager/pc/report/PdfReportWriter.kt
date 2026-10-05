package com.onlyfield.assetmanager.pc.report

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.forms.PhysicalTopology
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.OutputStream

/**
 * A4 delivery report with PDFBox: text sections from [ReportContent] plus drawn floor plans,
 * rack elevations, path tables and the physical topology. Never receives credentials.
 */
class PdfReportWriter(
    private val project: Project,
    private val i18n: Messages = Messages(),
    /** Floor plan background of an area (image or rendered PDF page); null draws a plain frame. */
    private val planImage: (Area) -> BufferedImage? = { null },
) {
    private val doc = PDDocument()
    private val regular: PDFont
    private val bold: PDFont
    private val index = ProjectIndex(project)
    private lateinit var stream: PDPageContentStream
    private var y = 0f
    private var header: ReportLine.Row? = null

    init {
        // A Unicode font keeps arrows and accented names; Helvetica (WinAnsi) is the fallback.
        val fonts = listOf("C:/Windows/Fonts/arial.ttf" to "C:/Windows/Fonts/arialbd.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf" to "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")
            .firstOrNull { (r, b) -> File(r).isFile && File(b).isFile }
        regular = fonts?.let { PDType0Font.load(doc, File(it.first)) } ?: PDType1Font(Standard14Fonts.FontName.HELVETICA)
        bold = fonts?.let { PDType0Font.load(doc, File(it.second)) } ?: PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
    }

    private val encodable = mutableMapOf<Pair<PDFont, Int>, Boolean>()

    /** Replaces characters the font cannot draw. */
    private fun safe(text: String, font: PDFont): String = buildString {
        text.codePoints().forEach { cp ->
            val ok = encodable.getOrPut(font to cp) { runCatching { font.encode(String(Character.toChars(cp))) }.isSuccess }
            append(if (ok) String(Character.toChars(cp)) else when (cp) {
                0x2192 -> "->"; 0x2190 -> "<-"; 0x21C4 -> "<->"; 0x22EF, 0x2026 -> "..."; 0x203A -> ">"; 0x2013, 0x2014 -> "-"; 0x2022 -> "*"
                else -> "?"
            })
        }
    }

    private fun width(text: String, font: PDFont, size: Float) = font.getStringWidth(safe(text, font)) / 1000f * size

    private fun newPage() {
        if (::stream.isInitialized) stream.close()
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        stream = PDPageContentStream(doc, page)
        y = TOP
    }

    /** Starts a new page unless [height] points still fit. */
    private fun ensure(height: Float) { if (y - height < BOTTOM) newPage() }

    private fun text(x: Float, baseline: Float, value: String, size: Float, font: PDFont = regular, color: Color = Color.BLACK) {
        if (value.isEmpty()) return
        stream.beginText(); stream.setNonStrokingColor(color); stream.setFont(font, size); stream.newLineAtOffset(x, baseline)
        stream.showText(safe(value, font)); stream.endText()
    }

    /** Text truncated with an ellipsis to [max] points. */
    private fun fit(value: String, font: PDFont, size: Float, max: Float): String {
        if (width(value, font, size) <= max) return value
        var cut = value
        while (cut.isNotEmpty() && width("$cut…", font, size) > max) cut = cut.dropLast(1)
        return "$cut…"
    }

    private fun wrap(value: String, font: PDFont, size: Float, max: Float): List<String> {
        val out = mutableListOf<String>()
        var line = ""
        for (word in value.split(' ')) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (line.isNotEmpty() && width(candidate, font, size) > max) { out += line; line = word } else line = candidate
        }
        out += line
        // A single word longer than the width is cut rather than overflowing.
        return out.map { fit(it, font, size, max) }
    }

    fun build(lines: List<ReportLine>, title: String): PDDocument {
        newPage()
        for (line in lines) when (line) {
            is ReportLine.Figure -> { header = null; figure(line) }
            is ReportLine.Row -> { if (line.header) header = line; row(line) }
            else -> { header = null; textLine(line) }
        }
        stream.close()
        val pages = doc.numberOfPages
        doc.pages.forEachIndexed { i, page ->
            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { footer ->
                footer.beginText(); footer.setNonStrokingColor(Color.DARK_GRAY); footer.setFont(regular, 8f); footer.newLineAtOffset(MARGIN, 28f)
                footer.showText(safe(i18n.text("text.657bcc42ca8e", title, i + 1, pages), regular)); footer.endText()
            }
        }
        doc.documentInformation.title = title
        doc.documentInformation.producer = "OnlyField Asset Manager"
        return doc
    }

    fun write(lines: List<ReportLine>, title: String, out: OutputStream) { build(lines, title).use { it.save(out) } }

    private fun textLine(line: ReportLine) {
        val (font, size, before, indent) = when (line) {
            is ReportLine.Title -> Style(bold, 18f, 0f)
            is ReportLine.Meta -> Style(regular, 9.5f, 4f)
            is ReportLine.Heading -> Style(bold, 14f, 10f)
            is ReportLine.SubHeading -> Style(bold, 11f, 6f)
            is ReportLine.Item -> Style(regular, 10f, 2f, 12f + line.indent * 14f)
            else -> Style(regular, 6f, 0f)
        }
        if (line is ReportLine.Spacer) { y -= 6f; return }
        val wrapped = wrap((if (line is ReportLine.Item) "• " else "") + line.text, font, size, CONTENT_W - indent)
        // Headings keep at least two lines of what follows on the same page.
        ensure(before + wrapped.size * size * 1.3f + if (line is ReportLine.Heading || line is ReportLine.SubHeading) size * 3 else 0f)
        y -= before
        wrapped.forEach { y -= size * 1.3f; text(MARGIN + indent, y, it, size, font) }
    }

    private data class Style(val font: PDFont, val size: Float, val before: Float, val indent: Float = 0f)

    private fun row(line: ReportLine.Row) {
        val size = 7f
        val font = if (line.header) bold else regular
        val widths = line.widths.map { it * CONTENT_W }
        val cells = line.cells.mapIndexed { i, c -> wrap(c, font, size, widths[i] - 4f) }
        val height = cells.maxOf { it.size } * size * 1.25f + 4f
        // Tables repeat their header row on every page.
        if (y - height < BOTTOM) { newPage(); header?.takeIf { it !== line }?.let(::row) }
        if (line.header) { stream.setNonStrokingColor(Color(0xE8, 0xEA, 0xF0)); stream.addRect(MARGIN, y - height, CONTENT_W, height); stream.fill() }
        var x = MARGIN
        cells.forEachIndexed { i, lines ->
            lines.forEachIndexed { k, t -> text(x + 2f, y - 2f - (k + 1) * size * 1.25f + 1.5f, t, size, font) }
            x += widths[i]
        }
        stream.setStrokingColor(Color(0xCC, 0xCC, 0xCC)); stream.setLineWidth(.4f)
        stream.moveTo(MARGIN, y - height); stream.lineTo(MARGIN + CONTENT_W, y - height); stream.stroke()
        y -= height
    }

    private fun figure(line: ReportLine.Figure) {
        when (val f = line.figure) {
            is ReportFigure.FloorPlan -> floorPlan(line.text, f.areaId)
            is ReportFigure.RackElevation -> rack(line.text, f.rackId)
            is ReportFigure.Topology -> topology(line.text)
        }
    }

    private fun caption(text: String) { y -= 6f; y -= 11f * 1.3f; text(MARGIN, y, text, 11f, bold); y -= 4f }

    private fun floorPlan(caption: String, areaId: String) {
        val area = project.sites.flatMap { it.areas }.find { it.id == areaId } ?: return
        val image = runCatching { planImage(area) }.getOrNull()
        val aspect = image?.let { it.height.toFloat() / it.width } ?: (1 / 1.414f)
        var w = CONTENT_W
        var h = w * aspect
        val maxH = TOP - BOTTOM - 30f
        if (h > maxH) { w *= maxH / h; h = maxH }
        ensure(h + 30f)
        caption(caption)
        val left = MARGIN + (CONTENT_W - w) / 2
        val bottom = y - h
        if (image != null) {
            val rgb = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
            rgb.createGraphics().apply { color = Color.WHITE; fillRect(0, 0, image.width, image.height); drawImage(image, 0, 0, null); dispose() }
            stream.drawImage(JPEGFactory.createFromImage(doc, rgb, .8f), left, bottom, w, h)
        }
        stream.setStrokingColor(Color.GRAY); stream.setLineWidth(.6f); stream.addRect(left, bottom, w, h); stream.stroke()
        fun px(p: MapPoint) = left + p.x * w
        fun py(p: MapPoint) = bottom + (1 - p.y) * h
        val scene = MapScene.area(project, areaId)
        for (link in scene.links) {
            val points = scene.points(link)
            if (points.size < 2) continue
            stream.setStrokingColor(when {
                LinkMedium.RADIO in link.media -> Color(0x1E, 0x6F, 0xD9)
                LinkMedium.FIBER in link.media -> Color(0xE0, 0x7B, 0x00)
                LinkMedium.POWER in link.media -> Color(0xC6, 0x28, 0x28)
                else -> Color(0x55, 0x55, 0x55)
            })
            stream.setLineWidth(if (link.cableIds.size > 1) 1.4f else .8f)
            stream.setLineDashPattern(if (LinkMedium.RADIO in link.media) floatArrayOf(4f, 3f) else floatArrayOf(), 0f)
            stream.moveTo(px(points.first()), py(points.first()))
            points.drop(1).forEach { stream.lineTo(px(it), py(it)) }
            stream.stroke()
        }
        stream.setLineDashPattern(floatArrayOf(), 0f)
        for (node in scene.nodes) {
            val x = px(node.point); val yy = py(node.point)
            stream.setNonStrokingColor(if (node.inactive) Color.LIGHT_GRAY else familyColor(node.glyph.family))
            if (node.isContainer) stream.addRect(x - 4f, yy - 4f, 8f, 8f) else circle(x, yy, 3.5f)
            stream.fill()
            text(x + 5f, yy - 2f, fit(node.name, regular, 5.5f, 90f), 5.5f, regular, Color(0x22, 0x22, 0x22))
        }
        y = bottom - 4f
    }

    private fun circle(x: Float, y: Float, r: Float) {
        val k = .552f * r
        stream.moveTo(x + r, y)
        stream.curveTo(x + r, y + k, x + k, y + r, x, y + r); stream.curveTo(x - k, y + r, x - r, y + k, x - r, y)
        stream.curveTo(x - r, y - k, x - k, y - r, x, y - r); stream.curveTo(x + k, y - r, x + r, y - k, x + r, y)
        stream.closePath()
    }

    private fun familyColor(family: ObjectFamily) = when (family) {
        ObjectFamily.NETWORK -> Color(0x1E, 0x6F, 0xD9)
        ObjectFamily.SECURITY -> Color(0x8E, 0x24, 0xAA)
        ObjectFamily.SERVER -> Color(0x2E, 0x7D, 0x32)
        ObjectFamily.POWER -> Color(0xC6, 0x28, 0x28)
        ObjectFamily.PASSIVE -> Color(0x75, 0x75, 0x75)
        ObjectFamily.STRUCTURE -> Color(0x45, 0x5A, 0x64)
        ObjectFamily.ENDPOINT -> Color(0xEF, 0x6C, 0x00)
        ObjectFamily.OTHER -> Color(0x60, 0x60, 0x60)
    }

    /** Front and rear elevation side by side; devices on both sides span both columns. */
    private fun rack(caption: String, rackId: String) {
        val rack = project.racks.find { it.id == rackId } ?: return
        val unit = minOf(11f, (TOP - BOTTOM - 50f) / rack.heightU)
        val h = unit * rack.heightU
        ensure(h + 40f)
        caption(caption)
        val column = 190f
        val left = MARGIN + 24f
        val top = y - 12f
        text(left, top + 3f, i18n.text("port.side.FRONT"), 8f, bold)
        text(left + column + 8f, top + 3f, i18n.text("port.side.REAR"), 8f, bold)
        // Row 0 is the top slot; U1 sits at the bottom unless numbering goes top to bottom.
        fun rowOf(u: Int) = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) rack.heightU - u else u - 1
        stream.setStrokingColor(Color.GRAY); stream.setLineWidth(.5f)
        for (side in 0..1) stream.addRect(left + side * (column + 8f), top - h, column, h)
        stream.stroke()
        for (u in 1..rack.heightU) if (u == 1 || u % 5 == 0 || u == rack.heightU) text(MARGIN, top - (rowOf(u) + 1) * unit + 2f, "U$u", 6f, regular, Color.DARK_GRAY)
        index.devices.filter { it.rackId == rack.id && it.positionU != null }.forEach { d ->
            val units = (d.positionU!! until d.positionU!! + d.heightU.coerceAtLeast(1)).filter { it in 1..rack.heightU }
            if (units.isEmpty()) return@forEach
            val firstRow = units.minOf(::rowOf)
            val rows = units.size
            val sides = when (d.rackSide) { RackSide.FRONT -> listOf(0); RackSide.REAR -> listOf(1); RackSide.BOTH -> listOf(0, 1) }
            for (side in sides) {
                val x = left + side * (column + 8f) + 1f
                val boxTop = top - firstRow * unit - .5f
                stream.setNonStrokingColor(if (d.isPassive()) Color(0xEC, 0xEF, 0xF1) else Color(0xDD, 0xE7, 0xF7))
                stream.addRect(x, boxTop - rows * unit + 1f, column - 2f, rows * unit - 1f); stream.fill()
                val size = minOf(7f, unit * .7f)
                text(x + 3f, boxTop - (rows * unit) / 2 - size / 3, fit(d.technicalName + (d.physicalLabel?.let { " · $it" } ?: ""), regular, size, column - 8f), size)
            }
        }
        y = top - h - 6f
    }

    /** Active devices with end devices folded; scaled down to fit one page when needed. */
    private fun topology(caption: String) {
        val nodeW = 88f; val nodeH = 24f; val gapX = 10f; val gapY = 22f
        val perRow = ((CONTENT_W + gapX) / (nodeW + gapX)).toInt()
        val topology = PhysicalTopology.build(project, perRow = perRow, foldEndpoints = true)
        if (topology.nodes.isEmpty()) return
        val natural = topology.rowSizes.size * (nodeH + gapY)
        val maxH = TOP - BOTTOM - 40f
        val scale = minOf(1f, maxH / natural)
        ensure(natural * scale + 30f)
        caption(caption)
        val top = y - 4f
        val centres = topology.nodes.associate { n ->
            val count = topology.rowSizes[n.row]
            val left = MARGIN + (CONTENT_W - (count * (nodeW + gapX) - gapX) * scale) / 2
            n.device.id to (left + (n.column * (nodeW + gapX) + nodeW / 2) * scale to top - (n.row * (nodeH + gapY) + nodeH / 2) * scale)
        }
        for (link in topology.links) {
            val (ax, ay) = centres[link.a] ?: continue
            val (bx, by) = centres[link.b] ?: continue
            val radio = CableMedium.RADIO in link.media
            stream.setStrokingColor(if (radio) Color(0x1E, 0x6F, 0xD9) else Color.GRAY)
            stream.setLineWidth((.5f + minOf(link.paths, 4) * .3f) * scale)
            stream.setLineDashPattern(if (radio) floatArrayOf(4f, 3f) else floatArrayOf(), 0f)
            val half = if (ay == by) 0f else nodeH / 2 * scale
            val (ux, uy, lx, ly) = if (ay >= by) listOf(ax, ay - half, bx, by + half) else listOf(bx, by - half, ax, ay + half)
            stream.moveTo(ux, uy); stream.lineTo(lx, ly); stream.stroke()
        }
        stream.setLineDashPattern(floatArrayOf(), 0f)
        for (n in topology.nodes) {
            val (cx, cy) = centres.getValue(n.device.id)
            val w = nodeW * scale; val h = nodeH * scale
            stream.setNonStrokingColor(if (n.outside) Color(0xEE, 0xEE, 0xEE) else Color(0xE3, 0xEA, 0xFB))
            stream.addRect(cx - w / 2, cy - h / 2, w, h); stream.fill()
            stream.setStrokingColor(Color(0x90, 0x9A, 0xB0)); stream.setLineWidth(.4f); stream.addRect(cx - w / 2, cy - h / 2, w, h); stream.stroke()
            val folded = topology.folded[n.device.id]?.size ?: 0
            val place = listOfNotNull(folded.takeIf { it > 0 }?.let { "+$it" }, index.siteOf(n.device.id)?.let { it.code ?: it.name }, n.areaId?.let(index::areaName)).joinToString(" · ")
            text(cx - w / 2 + 3f * scale, cy + 1.5f * scale, fit(n.device.technicalName, bold, 6.5f * scale, w - 5f * scale), 6.5f * scale, bold)
            text(cx - w / 2 + 3f * scale, cy - 7f * scale, fit(place, regular, 5f * scale, w - 5f * scale), 5f * scale, regular, Color.DARK_GRAY)
        }
        y = top - natural * scale - 6f
    }

    companion object {
        private const val MARGIN = 50f
        private val CONTENT_W = PDRectangle.A4.width - 2 * MARGIN
        private val TOP = PDRectangle.A4.height - MARGIN
        private const val BOTTOM = 50f
    }
}
