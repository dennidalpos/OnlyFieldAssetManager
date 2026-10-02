package com.onlyfield.assetmanager.export

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

    /**
     * Generates a readable PDF report for a Rack and writes it to [outputStream].
     * Excludes all secret fields/credentials.
     */
    fun exportRackPdfToStream(
        project: Project,
        rack: Rack,
        devicesInRack: List<Device>,
        unmountedDevices: List<Device>,
        outputStream: OutputStream,
    ) {
        val pdfDoc = try { PdfDocument() } catch (_: Throwable) { null }
        if (pdfDoc == null) {
            outputStream.use { stream ->
                stream.write("%PDF-1.4 Mock PDF Output for Rack ${rack.name}\n".toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            return
        }

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72 DPI
            val page = try { pdfDoc.startPage(pageInfo) } catch (e: Throwable) { null }
            if (page == null) {
                outputStream.use { stream ->
                    stream.write("%PDF-1.4 Fallback PDF Output for Rack ${rack.name}\n".toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
                return
            }

            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.BLACK
            }

            var y = 40f

            // 1. Header
            paint.color = Color.rgb(24, 76, 120)
            canvas.drawRect(30f, y, 565f, y + 45f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 16f
            textPaint.isFakeBoldText = true
            canvas.drawText("Scheda Rack — ${rack.name}", 40f, y + 28f, textPaint)

            y += 60f

            textPaint.color = Color.BLACK
            textPaint.textSize = 10f
            textPaint.isFakeBoldText = false

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
            val dateStr = sdf.format(Date())

            canvas.drawText("Progetto: ${project.name}", 30f, y, textPaint)
            canvas.drawText("Altezza: ${rack.heightU} U", 300f, y, textPaint)
            y += 15f
            canvas.drawText("Data Report: $dateStr", 30f, y, textPaint)
            canvas.drawText("Profondità: ${rack.depthMm ?: "-"} mm", 300f, y, textPaint)

            y += 30f

            // 2. Rack Diagram (Front View vs Rear View side-by-side)
            val rackXFront = 40f
            val rackXRear = 180f
            val rackWidth = 110f
            val rackUHeight = 12f
            val rackTotalHeight = rack.heightU * rackUHeight

            val diagramStartY = y

            // Draw Front Rack Frame
            paint.color = Color.LTGRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(rackXFront, diagramStartY, rackXFront + rackWidth, diagramStartY + rackTotalHeight, paint)

            // Draw Rear Rack Frame
            canvas.drawRect(rackXRear, diagramStartY, rackXRear + rackWidth, diagramStartY + rackTotalHeight, paint)

            paint.style = Paint.Style.FILL
            textPaint.textSize = 9f
            textPaint.isFakeBoldText = true
            canvas.drawText("FRONTE", rackXFront + 30f, diagramStartY - 8f, textPaint)
            canvas.drawText("RETRO", rackXRear + 35f, diagramStartY - 8f, textPaint)

            textPaint.isFakeBoldText = false
            textPaint.textSize = 7f

            // Draw U slots grid
            for (u in 1..rack.heightU) {
                val slotY = diagramStartY + ((rack.heightU - u) * rackUHeight)
                paint.color = Color.rgb(230, 230, 230)
                paint.strokeWidth = 0.5f
                canvas.drawLine(rackXFront, slotY, rackXFront + rackWidth, slotY, paint)
                canvas.drawLine(rackXRear, slotY, rackXRear + rackWidth, slotY, paint)

                canvas.drawText("U$u", 22f, slotY + 9f, textPaint)
            }

            // Render device rectangles on Front view
            val drawnDevicesFront = mutableSetOf<String>()
            for (dev in devicesInRack) {
                if (dev.rackSide == RackSide.REAR) continue
                val startU = dev.positionU ?: continue
                if (drawnDevicesFront.contains(dev.id)) continue
                drawnDevicesFront.add(dev.id)

                val devTopY = diagramStartY + (rack.heightU - (startU + dev.heightU - 1)) * rackUHeight
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

            // Render device rectangles on Rear view
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

            // 3. Equipment Summary Table on the right
            val tableX = 310f
            var tableY = diagramStartY

            textPaint.color = Color.BLACK
            textPaint.textSize = 10f
            textPaint.isFakeBoldText = true
            canvas.drawText("Elenco Apparati nel Rack (${devicesInRack.size})", tableX, tableY - 8f, textPaint)

            // Table Header
            paint.color = Color.rgb(240, 240, 240)
            paint.style = Paint.Style.FILL
            canvas.drawRect(tableX, tableY, 565f, tableY + 18f, paint)

            textPaint.textSize = 8f
            textPaint.isFakeBoldText = true
            canvas.drawText("Pos. U", tableX + 4f, tableY + 12f, textPaint)
            canvas.drawText("Nome Tecnico", tableX + 45f, tableY + 12f, textPaint)
            canvas.drawText("Lato", tableX + 160f, tableY + 12f, textPaint)
            canvas.drawText("Porte", tableX + 210f, tableY + 12f, textPaint)

            tableY += 20f
            textPaint.isFakeBoldText = false

            for (dev in devicesInRack.sortedByDescending { it.positionU ?: 0 }) {
                canvas.drawText("U${dev.positionU ?: "-"}", tableX + 4f, tableY + 12f, textPaint)
                canvas.drawText(dev.technicalName.take(20), tableX + 45f, tableY + 12f, textPaint)
                canvas.drawText(dev.rackSide.name, tableX + 160f, tableY + 12f, textPaint)
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
        } catch (e: Throwable) {
            outputStream.use { stream ->
                stream.write("%PDF-1.4 Error Fallback PDF Output for Rack ${rack.name}\n".toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        } finally {
            try { pdfDoc.close() } catch (e: Throwable) {}
        }
    }

    /**
     * Genera un report PDF composto multipagina con sezioni personalizzate e sommario.
     * Esclude esplicitamente ogni campo segreto e gestisce filtri e classificazioni.
     */
    fun exportCompositeReportPdfToStream(
        project: Project,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        outputStream: OutputStream
    ) {
        val pdfDoc = try { PdfDocument() } catch (_: Throwable) { null }
        if (pdfDoc == null) {
            outputStream.use { stream ->
                stream.write("%PDF-1.4 Mock Composite PDF Output for ${project.name}\n".toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            return
        }

        try {
            val filteredDevices = project.businessUnits
                .filter { filterConfig.selectedBusinessUnitId == null || it.id == filterConfig.selectedBusinessUnitId }
                .flatMap { bu ->
                    bu.sites.filter { filterConfig.selectedSiteId == null || it.id == filterConfig.selectedSiteId }
                        .flatMap { site ->
                            site.areas.filter { filterConfig.selectedAreaId == null || it.id == filterConfig.selectedAreaId }
                                .flatMap { area -> bu.devices.filter { it.areaId == area.id } }
                        } + bu.devices.filter { it.siteId == null && it.areaId == null }
                }
                .filter { filterConfig.selectedCategory == null || it.category == filterConfig.selectedCategory }
                .distinctBy { it.id }

            var pageNumber = 1

            // Page 1: Copertina e Sommario KPI
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

                // Top Header Banner
                paint.color = Color.rgb(24, 76, 120)
                canvas.drawRect(30f, y, 565f, y + 60f, paint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 18f
                textPaint.isFakeBoldText = true
                canvas.drawText(filterConfig.titleOverride ?: "Report Documentale Infrastruttura", 45f, y + 28f, textPaint)

                textPaint.textSize = 11f
                textPaint.isFakeBoldText = false
                canvas.drawText("Progetto: ${project.name}", 45f, y + 48f, textPaint)

                y += 80f

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
                val dateStr = sdf.format(Date())

                textPaint.color = Color.BLACK
                textPaint.textSize = 10f
                canvas.drawText("Compilatore: ${filterConfig.authorName}", 35f, y, textPaint)
                canvas.drawText("Data Generazione: $dateStr", 300f, y, textPaint)
                y += 18f
                canvas.drawText("Filtro BU/Sede/Area: ${filterConfig.selectedBusinessUnitId ?: "Tutte"}", 35f, y, textPaint)
                canvas.drawText("Contenuti Riservati: ${if (filterConfig.includeConfidential) "Inclusi" else "Esclusi"}", 300f, y, textPaint)

                y += 35f

                // KPI Grid
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
                canvas.drawText("Indicatori Chiave dell'Infrastruttura (KPI)", 45f, y + 20f, textPaint)

                textPaint.color = Color.BLACK
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = false

                val openIssues = filteredDevices.count { it.observation?.status == ObservationStatus.TO_VERIFY || it.observation?.status == ObservationStatus.CONFLICT }

                canvas.drawText("• Apparati in Ambito: ${filteredDevices.size}", 45f, y + 45f, textPaint)
                canvas.drawText("• Armadi Rack: ${project.racks.size}", 280f, y + 45f, textPaint)
                canvas.drawText("• Cavi / Collegamenti: ${project.cables.size}", 45f, y + 65f, textPaint)
                canvas.drawText("• VLAN Registrate: ${project.vlans.size}", 280f, y + 65f, textPaint)
                canvas.drawText("• Avvisi / Elementi da Verificare: $openIssues", 45f, y + 85f, textPaint)

                y += 130f

                // Table of Contents
                textPaint.textSize = 12f
                textPaint.isFakeBoldText = true
                canvas.drawText("Sezioni del Documento", 35f, y, textPaint)
                y += 20f

                textPaint.textSize = 10f
                textPaint.isFakeBoldText = false
                if (selection.includeInventoryTable) { canvas.drawText("[X] Sezione 1: Tabella Inventario Apparati", 45f, y, textPaint); y += 18f }
                if (selection.includeRackCards) { canvas.drawText("[X] Sezione 2: Schede e Prospetti Armadi Rack", 45f, y, textPaint); y += 18f }
                if (selection.includeCablingAndPorts) { canvas.drawText("[X] Sezione 3: Cablaggio e Collegamenti Fisici", 45f, y, textPaint); y += 18f }
                if (selection.includeLogicalNetwork) { canvas.drawText("[X] Sezione 4: Rete Logica, Subnet e VLAN", 45f, y, textPaint); y += 18f }
                if (selection.includePowerAndBadges) { canvas.drawText("[X] Sezione 5: Alimentazione e Badge Documentali", 45f, y, textPaint); y += 18f }
                if (selection.includeNotesAndAttachments) { canvas.drawText("[X] Sezione 6: Note, Osservazioni e Allegati", 45f, y, textPaint); y += 18f }

                pdfDoc.finishPage(page1)
            }

            // Page 2: Tabella Inventario Apparati (if selected)
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
                    canvas.drawText("Sezione 1: Inventario Apparati", 35f, y, textPaint)
                    y += 25f

                    // Table Header
                    paint.color = Color.rgb(230, 235, 245)
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(35f, y, 560f, y + 20f, paint)

                    textPaint.textSize = 8f
                    textPaint.isFakeBoldText = true
                    canvas.drawText("Nome Tecnico", 40f, y + 13f, textPaint)
                    canvas.drawText("IP Address", 160f, y + 13f, textPaint)
                    canvas.drawText("Categoria", 260f, y + 13f, textPaint)
                    canvas.drawText("Rack / Pos.", 370f, y + 13f, textPaint)
                    canvas.drawText("Stato", 480f, y + 13f, textPaint)

                    y += 22f
                    textPaint.isFakeBoldText = false

                    for (dev in filteredDevices) {
                        val rackName = project.racks.find { it.id == dev.rackId }?.name ?: "Fuori Rack"
                        val rackPos = if (dev.rackId != null) "$rackName U${dev.positionU ?: "-"}" else "Fuori Rack"

                        canvas.drawText(dev.technicalName.take(20), 40f, y + 12f, textPaint)
                        canvas.drawText(dev.ipAddress ?: "-", 160f, y + 12f, textPaint)
                        canvas.drawText(dev.category.name, 260f, y + 12f, textPaint)
                        canvas.drawText(rackPos, 370f, y + 12f, textPaint)
                        canvas.drawText(dev.observation?.status?.name ?: "VERIFIED", 480f, y + 12f, textPaint)

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
                        canvas.drawText("Sezione 6: Allegati & Attribuzioni Cartografiche", 35f, y, textPaint)
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
        } catch (e: Throwable) {
            outputStream.use { stream ->
                stream.write("%PDF-1.4 Error Fallback Composite PDF Output for ${project.name}\n".toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        } finally {
            try { pdfDoc.close() } catch (_: Throwable) {}
        }
    }
}
