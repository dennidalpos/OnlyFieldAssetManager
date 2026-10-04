package com.onlyfield.assetmanager.export

import com.onlyfield.assetmanager.core.display.toDisplayString

import com.onlyfield.assetmanager.core.i18n.Messages

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.model.RackSide
import com.onlyfield.assetmanager.core.model.ReportSelection
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportManager {

    /** Writes a rack PDF without credentials. */
    fun exportRackPdfToStream(
        project: Project,
        rack: Rack,
        devicesInRack: List<Device>,
        unmountedDevices: List<Device>,
        outputStream: OutputStream,
        i18n: Messages = Messages()) {
        val pdfDoc = PdfDocument()

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72 DPI
            val page = pdfDoc.startPage(pageInfo)

            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.BLACK
            }

            var y = 40f

            paint.color = Color.rgb(24, 76, 120)
            canvas.drawRect(30f, y, 565f, y + 45f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 16f
            textPaint.isFakeBoldText = true
            canvas.drawText(i18n.text("text.f24b33e201a1", rack.name), 40f, y + 28f, textPaint)

            y += 60f

            textPaint.color = Color.BLACK
            textPaint.textSize = 10f
            textPaint.isFakeBoldText = false

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
            val dateStr = sdf.format(Date())

            canvas.drawText(i18n.text("text.c9cbbfd9ed3d", project.name), 30f, y, textPaint)
            canvas.drawText(i18n.text("text.01c4d3f4fffb", rack.heightU), 300f, y, textPaint)
            y += 15f
            canvas.drawText(i18n.text("text.4339a839c0fc", dateStr), 30f, y, textPaint)
            canvas.drawText(i18n.text("text.c8b4873bee3b", rack.depthMm ?: "-"), 300f, y, textPaint)

            y += 30f

            val rackXFront = 40f
            val rackXRear = 180f
            val rackWidth = 110f
            val rackUHeight = 12f
            val rackTotalHeight = rack.heightU * rackUHeight

            val diagramStartY = y

            paint.color = Color.LTGRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(rackXFront, diagramStartY, rackXFront + rackWidth, diagramStartY + rackTotalHeight, paint)

            canvas.drawRect(rackXRear, diagramStartY, rackXRear + rackWidth, diagramStartY + rackTotalHeight, paint)

            paint.style = Paint.Style.FILL
            textPaint.textSize = 9f
            textPaint.isFakeBoldText = true
            canvas.drawText("FRONTE", rackXFront + 30f, diagramStartY - 8f, textPaint)
            canvas.drawText("RETRO", rackXRear + 35f, diagramStartY - 8f, textPaint)

            textPaint.isFakeBoldText = false
            textPaint.textSize = 7f

            for (u in 1..rack.heightU) {
                val slotY = diagramStartY + ((rack.heightU - u) * rackUHeight)
                paint.color = Color.rgb(230, 230, 230)
                paint.strokeWidth = 0.5f
                canvas.drawLine(rackXFront, slotY, rackXFront + rackWidth, slotY, paint)
                canvas.drawLine(rackXRear, slotY, rackXRear + rackWidth, slotY, paint)

                canvas.drawText("U$u", 22f, slotY + 9f, textPaint)
            }

            val drawnDevicesFront = mutableSetOf<String>()
            for (dev in devicesInRack) {
                if (dev.rackSide == RackSide.REAR) continue
                val startU = dev.positionU ?: continue
                if (drawnDevicesFront.contains(dev.id)) continue
                drawnDevicesFront.add(dev.id)

                val devTopY = diagramStartY + ((rack.heightU - (startU + dev.heightU - 1)) * rackUHeight)
                val devHeightPx = dev.heightU * rackUHeight

                paint.color = Color.rgb(220, 235, 252)
                paint.style = Paint.Style.FILL
                val rect = RectF(rackXFront + 1f, devTopY + 1f, rackXFront + rackWidth - 1f, devTopY + devHeightPx - 1f)
                canvas.drawRoundRect(rect, 2f, 2f, paint)

                paint.color = Color.rgb(30, 90, 150)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(rect, 2f, 2f, paint)

                textPaint.color = Color.rgb(10, 40, 90)
                textPaint.textSize = 8f
                canvas.drawText(dev.technicalName.take(18), rackXFront + 4f, devTopY + devHeightPx / 2f + 3f, textPaint)
            }

            val drawnDevicesRear = mutableSetOf<String>()
            for (dev in devicesInRack) {
                if (dev.rackSide == RackSide.FRONT) continue
                val startU = dev.positionU ?: continue
                if (drawnDevicesRear.contains(dev.id)) continue
                drawnDevicesRear.add(dev.id)

                val devTopY = diagramStartY + (rack.heightU - (startU + dev.heightU - 1)) * rackUHeight
                val devHeightPx = dev.heightU * rackUHeight

                paint.color = Color.rgb(252, 235, 220)
                paint.style = Paint.Style.FILL
                val rect = RectF(rackXRear + 1f, devTopY + 1f, rackXRear + rackWidth - 1f, devTopY + devHeightPx - 1f)
                canvas.drawRoundRect(rect, 2f, 2f, paint)

                paint.color = Color.rgb(180, 90, 30)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(rect, 2f, 2f, paint)

                textPaint.color = Color.rgb(90, 40, 10)
                textPaint.textSize = 8f
                canvas.drawText(dev.technicalName.take(18), rackXRear + 4f, devTopY + devHeightPx / 2f + 3f, textPaint)
            }

            val tableX = 310f
            var tableY = diagramStartY

            textPaint.color = Color.BLACK
            textPaint.textSize = 10f
            textPaint.isFakeBoldText = true
            canvas.drawText(i18n.text("text.ac7f2dbf5993", devicesInRack.size), tableX, tableY - 8f, textPaint)

            paint.color = Color.rgb(240, 240, 240)
            paint.style = Paint.Style.FILL
            canvas.drawRect(tableX, tableY, 565f, tableY + 18f, paint)

            textPaint.textSize = 8f
            textPaint.isFakeBoldText = true
            canvas.drawText(i18n.text("text.c048ae78b322"), tableX + 4f, tableY + 12f, textPaint)
            canvas.drawText(i18n.text("text.29caae5fe1e7"), tableX + 45f, tableY + 12f, textPaint)
            canvas.drawText(i18n.text("text.22ab4cafea0c"), tableX + 160f, tableY + 12f, textPaint)
            canvas.drawText(i18n.text("text.2edfc95a3c46"), tableX + 210f, tableY + 12f, textPaint)

            tableY += 20f
            textPaint.isFakeBoldText = false

            for (dev in devicesInRack.sortedByDescending { it.positionU ?: 0 }) {
                canvas.drawText("U${dev.positionU ?: "-"}", tableX + 4f, tableY + 12f, textPaint)
                canvas.drawText(dev.technicalName.take(20), tableX + 45f, tableY + 12f, textPaint)
                canvas.drawText(dev.rackSide.toDisplayString(i18n), tableX + 160f, tableY + 12f, textPaint)
                canvas.drawText(dev.ports.size.toString(), tableX + 210f, tableY + 12f, textPaint)

                paint.color = Color.LTGRAY
                paint.strokeWidth = 0.5f
                canvas.drawLine(tableX, tableY + 16f, 565f, tableY + 16f, paint)

                tableY += 18f
                if (tableY > 780f) break
            }

            pdfDoc.finishPage(page)

            outputStream.use { stream ->
                pdfDoc.writeTo(stream)
            }
        } finally {
            pdfDoc.close()
        }
    }

    /** Writes a filtered multi-page PDF without credentials. */
    fun exportCompositeReportPdfToStream(
        project: Project,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        outputStream: OutputStream,
        i18n: Messages = Messages()) {
        val pdfDoc = PdfDocument()

        try {
            val filteredDevices = project.businessUnits
                .filter { filterConfig.selectedBusinessUnitId == null || it.id == filterConfig.selectedBusinessUnitId }
                .flatMap { bu -> bu.devices.filter { device ->
                    val siteId = device.siteId ?: bu.sites.find { site -> site.areas.any { it.id == device.areaId } }?.id
                    (filterConfig.selectedSiteId == null || siteId == filterConfig.selectedSiteId) &&
                        (filterConfig.selectedAreaId == null || device.areaId == filterConfig.selectedAreaId)
                } }
                .filter { filterConfig.selectedCategory == null || it.category == filterConfig.selectedCategory }
                .distinctBy { it.id }

            var pageNumber = 1

            val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            val page1 = pdfDoc.startPage(pageInfo1)
            if (page1 != null) {
                val canvas = page1.canvas
                val paint = Paint().apply { isAntiAlias = true }
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    color = Color.BLACK
                }

                var y = 40f

                paint.color = Color.rgb(24, 76, 120)
                canvas.drawRect(30f, y, 565f, y + 60f, paint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 18f
                textPaint.isFakeBoldText = true
                canvas.drawText(filterConfig.titleOverride ?: i18n.text("text.40ceb13eaea5"), 45f, y + 28f, textPaint)

                textPaint.textSize = 11f
                textPaint.isFakeBoldText = false
                canvas.drawText(i18n.text("text.c9cbbfd9ed3d", project.name), 45f, y + 48f, textPaint)

                y += 80f

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
                val dateStr = sdf.format(Date())

                textPaint.color = Color.BLACK
                textPaint.textSize = 10f
                canvas.drawText(i18n.text("text.0ea77b6420df", filterConfig.authorName), 35f, y, textPaint)
                canvas.drawText(i18n.text("text.ddaa8218e575", dateStr), 300f, y, textPaint)
                y += 18f
                canvas.drawText(i18n.text("text.54c2933191f6", filterConfig.selectedBusinessUnitId ?: i18n.text("text.8497975606d6")), 35f, y, textPaint)
                canvas.drawText(i18n.text("text.44f131081b2d", if (filterConfig.includeConfidential) i18n.text("text.b3186dc0586e") else i18n.text("text.0c2690153ac8")), 300f, y, textPaint)

                y += 35f

                paint.color = Color.rgb(245, 247, 250)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 100f), 6f, 6f, paint)

                paint.color = Color.rgb(200, 210, 225)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 100f), 6f, 6f, paint)

                textPaint.color = Color.rgb(24, 76, 120)
                textPaint.textSize = 12f
                textPaint.isFakeBoldText = true
                canvas.drawText(i18n.text("text.8679e4cb849b"), 45f, y + 20f, textPaint)

                textPaint.color = Color.BLACK
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = false

                val openIssues = filteredDevices.count { it.observation?.status == ObservationStatus.TO_VERIFY || it.observation?.status == ObservationStatus.CONFLICT }

                canvas.drawText(i18n.text("text.46f30e92b9c4", filteredDevices.size), 45f, y + 45f, textPaint)
                canvas.drawText(i18n.text("text.f70e0fdb83c5", project.racks.size), 280f, y + 45f, textPaint)
                canvas.drawText(i18n.text("text.2a47e0afd9c5", project.cables.size), 45f, y + 65f, textPaint)
                canvas.drawText(i18n.text("text.a197c01d82e1", project.vlans.size), 280f, y + 65f, textPaint)
                canvas.drawText(i18n.text("text.62bbfb1e07f1", openIssues), 45f, y + 85f, textPaint)

                y += 130f

                textPaint.textSize = 12f
                textPaint.isFakeBoldText = true
                canvas.drawText(i18n.text("text.d764b2e1bfed"), 35f, y, textPaint)
                y += 20f

                textPaint.textSize = 10f
                textPaint.isFakeBoldText = false
                if (selection.includeInventoryTable) { canvas.drawText(i18n.text("text.de5ddf03b157"), 45f, y, textPaint); y += 18f }
                if (selection.includeRackCards) { canvas.drawText(i18n.text("text.b10589659b2f"), 45f, y, textPaint); y += 18f }
                if (selection.includeCablingAndPorts) { canvas.drawText(i18n.text("text.d6ca39b21683"), 45f, y, textPaint); y += 18f }
                if (selection.includeLogicalNetwork) { canvas.drawText(i18n.text("text.454ad4a7351a"), 45f, y, textPaint); y += 18f }
                if (selection.includePowerAndBadges) { canvas.drawText(i18n.text("text.d96349382a35"), 45f, y, textPaint); y += 18f }
                if (selection.includeNotesAndAttachments) canvas.drawText(i18n.text("text.ab6767f4e650"), 45f, y, textPaint)

                pdfDoc.finishPage(page1)
            }

            if (selection.includeInventoryTable && filteredDevices.isNotEmpty()) {
                pageNumber++
                val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                val page2 = pdfDoc.startPage(pageInfo2)
                if (page2 != null) {
                    val canvas = page2.canvas
                    val paint = Paint().apply { isAntiAlias = true }
                    val textPaint = Paint().apply { isAntiAlias = true; textSize = 9f; color = Color.BLACK }

                    var y = 40f
                    textPaint.textSize = 14f
                    textPaint.isFakeBoldText = true
                    canvas.drawText(i18n.text("text.334e5b0cffff"), 35f, y, textPaint)
                    y += 25f

                    paint.color = Color.rgb(230, 235, 245)
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(35f, y, 560f, y + 20f, paint)

                    textPaint.textSize = 8f
                    textPaint.isFakeBoldText = true
                    canvas.drawText(i18n.text("text.29caae5fe1e7"), 40f, y + 13f, textPaint)
                    canvas.drawText(i18n.text("text.3c3de0c91c5f"), 160f, y + 13f, textPaint)
                    canvas.drawText(i18n.text("text.54276aa0307f"), 260f, y + 13f, textPaint)
                    canvas.drawText(i18n.text("text.6f4d3bf19ead"), 370f, y + 13f, textPaint)
                    canvas.drawText(i18n.text("text.5d788017bfe4"), 480f, y + 13f, textPaint)

                    y += 22f
                    textPaint.isFakeBoldText = false

                    for (dev in filteredDevices) {
                        val rackName = project.racks.find { it.id == dev.rackId }?.name ?: i18n.text("text.3f03be4817b0")
                        val rackPos = if (dev.rackId != null) i18n.text("text.5e7c17bc3374", rackName, dev.positionU ?: "-") else i18n.text("text.3f03be4817b0")

                        canvas.drawText(dev.technicalName.take(20), 40f, y + 12f, textPaint)
                        canvas.drawText(dev.ipAddress ?: "-", 160f, y + 12f, textPaint)
                        canvas.drawText(dev.category.toDisplayString(i18n), 260f, y + 12f, textPaint)
                        canvas.drawText(rackPos, 370f, y + 12f, textPaint)
                        canvas.drawText((dev.observation?.status ?: ObservationStatus.VERIFIED).toDisplayString(i18n), 480f, y + 12f, textPaint)

                        paint.color = Color.LTGRAY
                        paint.strokeWidth = 0.5f
                        canvas.drawLine(35f, y + 16f, 560f, y + 16f, paint)

                        y += 18f
                        if (y > 750f) break
                    }

                    if (selection.includeNotesAndAttachments && project.attachments.isNotEmpty()) {
                        y += 15f
                        textPaint.textSize = 11f
                        textPaint.isFakeBoldText = true
                        canvas.drawText(i18n.text("text.b0b59b565f62"), 35f, y, textPaint)
                        y += 18f

                        textPaint.textSize = 8f
                        textPaint.isFakeBoldText = false
                        for (att in project.attachments.take(5)) {
                            val attrStr = if (!att.attributionText.isNullOrBlank()) " (${att.attributionText})" else ""
                            canvas.drawText("• ${att.name}$attrStr", 40f, y, textPaint)
                            y += 14f
                            if (y > 800f) break
                        }
                    }

                    pdfDoc.finishPage(page2)
                }
            }

            outputStream.use { stream ->
                pdfDoc.writeTo(stream)
            }
        } finally {
            pdfDoc.close()
        }
    }
}
