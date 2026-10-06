package com.onlyfield.assetmanager.export

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
            report.write(outputStream)
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
        val scope = DocumentSelection(project, filterConfig)
        val selected = scope.project
        val index = ProjectIndex(selected)
        PagedReport(i18n).use { report ->
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
                selected.powerFeeds.forEach { feed ->
                    report.text("${index.deviceName(feed.deviceId)} · ${feed.feedName} (${feed.feedType.toDisplayString(i18n)})" +
                        listOfNotNull(feed.sourceDeviceId?.let { index.deviceName(it) } ?: feed.sourceOutletDescription,
                            feed.loadWatts?.let { i18n.text("text.cd49315c743a", it) }, feed.loadVa?.let { "$it VA" },
                            feed.observedRuntimeMinutes?.let { i18n.text("text.2dc280aa0f83", it) }).joinToString(" · ", prefix = " · "))
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
            report.write(outputStream)
        }
    }

    private class PagedReport(private val i18n: Messages) : AutoCloseable {
        private val document = PdfDocument()
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = MARGIN
        private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        private val canvas: Canvas get() = checkNotNull(page).canvas

        private fun finishPage() {
            page?.let { current ->
                val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.DKGRAY }
                current.canvas.drawText(pageNumber.toString(), 550f, 815f, footer)
                document.finishPage(current)
                page = null
            }
        }

        private fun newPage() {
            finishPage()
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = MARGIN
        }

        private fun ensure(height: Float) { if (page == null || y + height > BOTTOM) newPage() }

        fun heading(value: String, size: Float = 14f, newPage: Boolean = false) {
            if (newPage && page != null && y > MARGIN) newPage()
            ensure(size * 4)
            text(value, size = size, bold = true)
            y += 8f
        }

        fun text(value: String, indent: Int = 0, size: Float = 10f, bold: Boolean = false) {
            if (value.isBlank()) return
            paint.textSize = size
            paint.isFakeBoldText = bold
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, WIDTH - indent)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(2f, 1f).build()
            if (layout.height <= BOTTOM - MARGIN) ensure(layout.height.toFloat())
            var line = 0
            // Split only between lines, including paragraphs longer than one page.
            while (line < layout.lineCount) {
                val top = layout.getLineTop(line)
                ensure((layout.getLineBottom(line) - top).toFloat())
                var end = line + 1
                while (end < layout.lineCount && y + layout.getLineBottom(end) - top <= BOTTOM) end++
                val height = layout.getLineBottom(end - 1) - top
                val saved = canvas.save()
                try {
                    canvas.clipRect(MARGIN + indent, y, MARGIN + WIDTH, y + height)
                    canvas.translate(MARGIN + indent, y - top)
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
            val unitHeight = minOf(12f, 480f / units)
            val height = units * unitHeight
            ensure(height + 26f)
            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = .5f }
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = minOf(8f, unitHeight * .8f) }
            val top = y + 20f
            val sides = listOf(RackSide.FRONT to 65f, RackSide.REAR to 320f)
            sides.forEach { (side, x) ->
                textPaintLabel(canvas, side.toDisplayString(i18n), x, y + 10f)
                canvas.drawRect(x, top, x + 215f, top + height, frame)
                for (row in 0 until units) {
                    val u = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) units - row else row + 1
                    val rowY = top + row * unitHeight
                    canvas.drawLine(x, rowY, x + 215f, rowY, frame)
                    canvas.drawText("U$u", x - 25f, rowY + unitHeight * .8f, label)
                }
                devices.filter { it.rackSide == side || it.rackSide == RackSide.BOTH }.forEach { device ->
                    val u = device.positionU ?: return@forEach
                    if (u !in 1..units || u + device.heightU - 1 > units) return@forEach
                    val row = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP) units - u - device.heightU + 1 else u - 1
                    val deviceTop = top + row * unitHeight
                    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(220, 235, 252) }
                    canvas.drawRect(x + 1, deviceTop + 1, x + 214, deviceTop + device.heightU * unitHeight - 1, fill)
                    val count = label.breakText(device.technicalName, true, 205f, null)
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
            output.use(document::writeTo)
        }

        override fun close() { finishPage(); document.close() }

        companion object {
            private const val MARGIN = 35f
            private const val WIDTH = 525
            private const val BOTTOM = 795f
        }
    }
}
