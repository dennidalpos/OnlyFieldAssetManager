package com.onlyfield.assetmanager.export

import android.content.Context
import android.graphics.Rect
import android.print.PageRange
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.DocumentSelection
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date

object PdfExportManager {
    /** Writes the elevation and complete device list without credentials. */
    fun exportRackPdfToStream(
        project: Project,
        rack: Rack,
        devicesInRack: List<Device>,
        unmountedDevices: List<Device>,
        outputStream: OutputStream,
        i18n: Messages = Messages(),
    ) {
        PagedReport(i18n).use { report ->
            report.heading(i18n.text("text.f24b33e201a1", rack.name), 18f)
            report.text(i18n.text("text.c9cbbfd9ed3d", project.name))
            report.rack(rack, devicesInRack)
            if (unmountedDevices.isNotEmpty()) {
                report.heading(i18n.text("text.3f03be4817b0"))
                unmountedDevices.forEach { report.device(it) }
            }
            val devices = devicesInRack + unmountedDevices
            devices.filter { !it.observation?.notes.isNullOrBlank() }.forEach {
                report.text("${it.technicalName} · ${it.observation.effectiveStatus().toDisplayString(i18n)}: ${it.observation?.notes}")
            }
            val ids = devices.map { it.id }.toSet() + rack.id
            val warnings = DocumentSelection(project, ExportFilterConfig()).warnings(i18n).filter { it.targetEntityId in ids }
            if (warnings.isNotEmpty()) {
                report.heading(i18n.text("document.warnings"))
                warnings.forEach { report.text(it.message) }
            }
            outputStream.use(report::write)
        }
    }

    /** All six sections offered by Android share the same paginated writer. */
    fun exportCompositeReportPdfToStream(
        project: Project,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        outputStream: OutputStream,
        i18n: Messages = Messages(),
    ) {
        PagedReport(i18n).use { report ->
            renderComposite(project, filterConfig, selection, i18n, report)
            outputStream.use(report::write)
        }
    }

    internal fun layoutPrint(context: Context, attributes: PrintAttributes, project: Project,
        filter: ExportFilterConfig, selection: ReportSelection, i18n: Messages, checkCancelled: () -> Unit): Int =
        printReport(context, attributes, emptyArray(), i18n, checkCancelled).use { report ->
            renderComposite(project, filter, selection, i18n, report)
            report.pageCount
        }

    internal fun writePrint(context: Context, attributes: PrintAttributes, pages: Array<out PageRange>,
        project: Project, filter: ExportFilterConfig, selection: ReportSelection, output: OutputStream,
        i18n: Messages, checkCancelled: () -> Unit): Array<PageRange> =
        printReport(context, attributes, pages, i18n, checkCancelled).use { report ->
            renderComposite(project, filter, selection, i18n, report)
            report.write(output)
            report.writtenRanges()
        }

    private fun printReport(context: Context, attributes: PrintAttributes, pages: Array<out PageRange>,
        i18n: Messages, checkCancelled: () -> Unit): PagedReport {
        val geometry = PrintedPdfDocument(context, attributes)
        val width: Int
        val height: Int
        val bounds: Rect
        try { width = geometry.pageWidth; height = geometry.pageHeight; bounds = Rect(geometry.pageContentRect) }
        finally { geometry.close() }
        return PagedReport(i18n, width, height, bounds, pages, checkCancelled)
    }

    private fun renderComposite(project: Project, filterConfig: ExportFilterConfig, selection: ReportSelection,
        i18n: Messages, report: PagedReport) {
        val scope = DocumentSelection(project, filterConfig)
        val selected = scope.project
        val index = ProjectIndex(selected)
        report.heading(filterConfig.titleOverride?.ifBlank { null } ?: i18n.text("text.40ceb13eaea5"), 18f)
        report.text(i18n.text("text.c9cbbfd9ed3d", selected.name))
        report.text(i18n.text("text.b1b3e27e33a1", filterConfig.authorName, SimpleDateFormat("dd/MM/yyyy HH:mm", i18n.locale).format(Date())))
        report.text(i18n.text("text.9046ee10595c", index.devices.size, selected.racks.size, selected.cables.size) +
            i18n.text("text.fde3438fc2a1", selected.vlans.size, selected.powerFeeds.size))

        if (selection.includeInventoryTable) {
            report.heading(i18n.text("text.334e5b0cffff"), newPage = true)
            selected.sites.forEach { site ->
                report.heading(site.name, 11f)
                site.devices.forEach { device ->
                    report.device(device, listOfNotNull(device.ipAddress, device.macAddress, device.physicalLabel,
                        index.area(device.areaId)?.name, index.rack(device.rackId)?.name,
                        device.positionU?.let { "U$it" }).joinToString(" · "))
                }
            }
        }
        if (selection.includeRackCards) {
            selected.racks.forEach { rack ->
                report.heading(i18n.text("text.f24b33e201a1", rack.name), newPage = true)
                report.rack(rack, index.devices.filter { it.rackId == rack.id })
            }
        }
        if (selection.includeCablingAndPorts) {
            report.heading(i18n.text("text.3b40d8bd6081"), newPage = true)
            index.devices.forEach { device ->
                if (device.ports.isNotEmpty()) report.heading(device.technicalName, 11f)
                device.ports.forEach { port ->
                    report.text(listOfNotNull(port.name, port.label, port.hardware.side?.toDisplayString(i18n),
                        port.hardware.connector, port.hardware.speed, port.hardware.opticalModule,
                        port.endpointStatus.toDisplayString(i18n), port.observation.effectiveStatus().toDisplayString(i18n)).joinToString(" · "), indent = 12)
                }
            }
            selected.cables.forEach { cable ->
                report.text("${cable.codeOrLabel ?: cable.id}: ${index.portLabel(cable.portAId)} ↔ ${index.portLabel(cable.portBId)} · " +
                    listOfNotNull(cable.medium.toDisplayString(i18n), cable.observation.effectiveStatus().toDisplayString(i18n), cable.color, cable.lengthValue?.let { "$it ${cable.lengthUnit ?: "m"}" }).joinToString(" · "))
            }
            selected.panelMappings.forEach { report.text("${index.portLabel(it.portAId)} ↔ ${index.portLabel(it.portBId)}") }
        }
        if (selection.includeLogicalNetwork) {
            report.heading(i18n.text("text.7070d68f65b5"), newPage = true)
            selected.vlans.sortedBy { it.vlanId }.forEach { report.text(i18n.text("text.91b4234e3dfd", it.vlanId, it.name) + (it.description?.let { value -> ": $value" } ?: "")) }
            selected.subnets.forEach { subnet -> report.text(listOfNotNull(subnet.cidrBlock, subnet.name, subnet.gatewayIp).joinToString(" · ")) }
            selected.logicalInterfaces.forEach { port -> report.text("${index.deviceName(port.deviceId)} › ${port.name}: " + listOfNotNull(port.ipAddress, port.subnetCidr, port.vlanId?.toString()).joinToString(" · ")) }
            selected.portVlanMemberships.forEach { vlan -> report.text("${index.portLabel(vlan.portId)}: ${vlan.mode.toDisplayString(i18n)} · " +
                listOfNotNull(vlan.untaggedVlanId?.let { i18n.text("text.5d4192a73511", it) },
                    vlan.taggedVlanIds.takeIf { it.isNotEmpty() }?.let { i18n.text("text.29697e42d0d3", it.joinToString(", ")) }, vlan.nativeVlanId?.let { "${i18n.text("port.vlanNative")}: $it" }).joinToString(" · ")) }
            selected.lagGroups.forEach { lag -> report.text("${index.deviceName(lag.deviceId)} › ${lag.name}: ${lag.mode.toDisplayString(i18n)} · ${lag.memberPortIds.joinToString(", ") { index.portLabel(it) }}") }
            selected.wanVpnConnections.forEach { wan -> report.text("${wan.type.toDisplayString(i18n)} ${wan.name}: " +
                listOfNotNull(wan.providerOrCarrier, wan.bandwidth, wan.localEndpointDeviceId?.let { index.deviceName(it) }, wan.remoteEndpointDeviceId?.let { index.deviceName(it) }).joinToString(" · ")) }
        }
        if (selection.includePowerAndBadges) {
            report.heading(i18n.text("text.acedc1948e5f"), newPage = true)
            val sourceIndex = ProjectIndex(project)
            selected.powerFeeds.forEach { feed ->
                val sourceId = project.powerFeeds.firstOrNull { it.id == feed.id }?.sourceDeviceId
                val details = listOfNotNull(sourceId?.let { sourceIndex.deviceName(it) }, feed.sourceOutletDescription,
                    feed.voltageVolts?.let { "$it V" }, feed.loadVa?.let { "$it VA" }, feed.loadWatts?.let { "$it W" },
                    feed.observedRuntimeMinutes?.let { i18n.text("text.2dc280aa0f83", it) },
                    feed.observedSource?.let { "${i18n.text("document.observedSource")}: $it" },
                    feed.observedEpochMs?.let { "${i18n.text("document.observedDate")}: ${SimpleDateFormat("dd/MM/yyyy HH:mm", i18n.locale).format(Date(it))}" }, feed.notes)
                report.text("${index.deviceName(feed.deviceId)} · ${feed.feedName} (${feed.feedType.toDisplayString(i18n)})" +
                    details.joinToString(" · ", prefix = if (details.isEmpty()) "" else " · "))
            }
            selected.poeMappings.forEach { poe -> report.text(i18n.text("text.cb79585925c3", index.portLabel(poe.portId), poe.role.toDisplayString(i18n), poe.standard.toDisplayString(i18n))) }
            selected.documentBadges.forEach { badge -> report.text(i18n.text("text.3b424f3a179d", badge.label, index.targetLabel(badge.targetType, badge.targetId, i18n))) }
        }
        if (selection.includeNotesAndAttachments) {
            report.heading(i18n.text("text.1d02ed0f43ac"), newPage = true)
            fun note(label: String, value: String?) { if (!value.isNullOrBlank()) report.text("$label: $value") }
            selected.description?.let(report::text)
            scope.observations.filter { !it.observation?.notes.isNullOrBlank() }.forEach {
                note("${it.label} · ${it.observation.effectiveStatus().toDisplayString(i18n)}", it.observation?.notes)
            }
            selected.racks.forEach { note(it.name, it.notes) }
            selected.cables.forEach { note(it.codeOrLabel ?: it.id, it.notes) }
            selected.logicalInterfaces.forEach { note(it.name, it.notes) }
            selected.portVlanMemberships.forEach { note(index.portLabel(it.portId), it.notes) }
            selected.lagGroups.forEach { note(it.name, it.notes) }
            selected.wanVpnConnections.forEach { note(it.name, it.notes) }
            selected.powerFeeds.forEach { note(it.feedName, it.notes) }
            selected.poeMappings.forEach { note(index.portLabel(it.portId), it.notes) }
            selected.documentBadges.forEach { note(it.label, it.notes) }
            selected.customExtraFields.forEach { field -> report.text("${index.targetLabel(field.targetType, field.targetId, i18n)} · ${field.fieldKey}: ${field.fieldValue}") }
            selected.attachments.forEach { attachment ->
                report.text(i18n.text("text.17e998f7cad2", attachment.name, attachment.fileType.toDisplayString(i18n), attachment.originalFileName))
                attachment.attributionText?.let { report.text(it, indent = 12) }
            }
        }
        val warnings = scope.warnings(i18n, selection.copy(includeFloorPlans = false, includePaths = false, includeTopology = false))
        if (warnings.isNotEmpty()) {
            report.heading(i18n.text("document.warnings"), newPage = true)
            warnings.forEach { report.text(it.message) }
        }
    }

    private class PagedReport(private val i18n: Messages, private val pageWidth: Int = 595,
        private val pageHeight: Int = 842, private val bounds: Rect = Rect(35, 35, 560, 815),
        private val requested: Array<out PageRange> = arrayOf(PageRange.ALL_PAGES),
        private val checkCancelled: () -> Unit = {}) : AutoCloseable {
        init { require(bounds.width() >= 100 && bounds.height() >= 100) { i18n.text("text.6a0fbef6bf9b") } }
        private val margin = bounds.top.toFloat()
        private val left = bounds.left.toFloat()
        private val width = bounds.width()
        private val bottom = bounds.bottom - 20f
        private var pageOpen = false
        private val written = mutableListOf<Int>()
        val pageCount: Int get() = pageNumber
        private val document = PdfDocument()
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = margin
        private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        private val skippedCanvas = Canvas()
        private val canvas: Canvas get() = page?.canvas ?: skippedCanvas

        private fun finishPage() {
            page?.let { current ->
                val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.DKGRAY }
                current.canvas.drawText(pageNumber.toString(), bounds.right - 10f, bounds.bottom.toFloat(), footer)
                document.finishPage(current)
                page = null
            }
            pageOpen = false
        }

        private fun newPage() {
            finishPage()
            pageNumber++
            checkCancelled()
            pageOpen = true
            if (requested.any { pageNumber - 1 in it.start..it.end }) {
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber - 1).create())
                page!!.canvas.clipRect(bounds)
                written += pageNumber - 1
            }
            y = margin
        }

        private fun ensure(height: Float) { if (!pageOpen || y + height > bottom) newPage() }

        fun heading(value: String, size: Float = 14f, newPage: Boolean = false) {
            if (newPage && pageOpen && y > margin) newPage()
            ensure(size * 4)
            text(value, size = size, bold = true)
            y += 8f
        }

        fun text(value: String, indent: Int = 0, size: Float = 10f, bold: Boolean = false) {
            checkCancelled()
            if (value.isBlank()) return
            paint.textSize = size
            paint.isFakeBoldText = bold
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width - indent)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(2f, 1f).build()
            if (layout.height <= bottom - margin) ensure(layout.height.toFloat())
            var line = 0
            // Split only between lines, including paragraphs longer than one page.
            while (line < layout.lineCount) {
                checkCancelled()
                val top = layout.getLineTop(line)
                ensure((layout.getLineBottom(line) - top).toFloat())
                var end = line + 1
                while (end < layout.lineCount && y + layout.getLineBottom(end) - top <= bottom) end++
                val height = layout.getLineBottom(end - 1) - top
                val saved = canvas.save()
                try {
                    canvas.clipRect(left + indent, y, left + width, y + height)
                    canvas.translate(left + indent, y - top)
                    layout.draw(canvas)
                } finally { canvas.restoreToCount(saved) }
                y += height
                line = end
            }
            y += 4f
        }

        fun device(device: Device, location: String = "") {
            text("${device.technicalName} · ${device.category.toDisplayString(i18n)} · ${device.operationalStatus.toDisplayString(i18n)} · " +
                device.observation.effectiveStatus().toDisplayString(i18n) +
                location.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty())
        }

        fun rack(rack: Rack, devices: List<Device>) {
            text(i18n.text("text.01c4d3f4fffb", rack.heightU))
            val units = rack.heightU.coerceAtLeast(1)
            val unitHeight = minOf(12f, minOf(480f, bottom - margin - 40f) / units)
            val height = units * unitHeight
            ensure(height + 26f)
            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = .5f }
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = minOf(8f, unitHeight * .8f) }
            val top = y + 20f
            val frameWidth = (width - 95f) / 2f
            val sides = listOf(RackSide.FRONT to left + 30f, RackSide.REAR to left + width / 2f + 22.5f)
            sides.forEach { (side, x) ->
                textPaintLabel(canvas, side.toDisplayString(i18n), x, y + 10f)
                canvas.drawRect(x, top, x + frameWidth, top + height, frame)
                for (row in 0 until units) {
                    val u = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) units - row else row + 1
                    val rowY = top + row * unitHeight
                    canvas.drawLine(x, rowY, x + frameWidth, rowY, frame)
                    canvas.drawText("U$u", x - 25f, rowY + unitHeight * .8f, label)
                }
                devices.filter { it.rackSide == side || it.rackSide == RackSide.BOTH }.forEach { device ->
                    val u = device.positionU ?: return@forEach
                    if (u !in 1..units || u + device.heightU - 1 > units) return@forEach
                    val row = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) units - u - device.heightU + 1 else u - 1
                    val deviceTop = top + row * unitHeight
                    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(220, 235, 252) }
                    canvas.drawRect(x + 1, deviceTop + 1, x + frameWidth - 1, deviceTop + device.heightU * unitHeight - 1, fill)
                    val count = label.breakText(device.technicalName, true, frameWidth - 10f, null)
                    canvas.drawText(device.technicalName, 0, count, x + 4, deviceTop + unitHeight * .8f, label)
                }
            }
            y = top + height + 12f
            devices.sortedByDescending { it.positionU ?: 0 }.forEach { device ->
                device(device, "${device.positionU?.let { "U$it" } ?: i18n.text("text.3fc3745f990b")} · ${device.rackSide.toDisplayString(i18n)} · " +
                    i18n.plural("text.53a2e3b94696", device.ports.size))
            }
        }

        private fun textPaintLabel(canvas: Canvas, value: String, x: Float, y: Float) {
            canvas.drawText(value, x, y, Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; isFakeBoldText = true })
        }

        fun write(output: OutputStream) {
            finishPage()
            checkCancelled()
            check(written.isNotEmpty()) { i18n.text("text.6a0fbef6bf9b") }
            document.writeTo(output)
        }

        override fun close() { finishPage(); document.close() }

        fun writtenRanges(): Array<PageRange> {
            val ranges = mutableListOf<PageRange>()
            for (number in written) {
                val previous = ranges.lastOrNull()
                if (previous != null && previous.end + 1 == number) ranges[ranges.lastIndex] = PageRange(previous.start, number)
                else ranges += PageRange(number, number)
            }
            return ranges.toTypedArray()
        }
    }
}
