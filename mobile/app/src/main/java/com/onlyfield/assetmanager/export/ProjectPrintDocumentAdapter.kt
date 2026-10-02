package com.onlyfield.assetmanager.export

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.ReportSelection
import java.io.FileOutputStream

/**
 * Adattatore per l'integrazione con il framework di stampa nativo Android (PrintManager).
 * Consente l'anteprima di stampa, la selezione della stampante o il salvataggio diretto in PDF.
 */
class ProjectPrintDocumentAdapter(
    private val project: Project,
    private val filterConfig: ExportFilterConfig = ExportFilterConfig(),
    private val reportSelection: ReportSelection = ReportSelection()
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("Documento_${project.name}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()

        callback?.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (destination == null) {
            callback?.onWriteFailed("Output file descriptor non valido")
            return
        }

        try {
            FileOutputStream(destination.fileDescriptor).use { outputStream ->
                PdfExportManager.exportCompositeReportPdfToStream(
                    project = project,
                    filterConfig = filterConfig,
                    selection = reportSelection,
                    outputStream = outputStream
                )
            }

            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
            } else {
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        }
    }
}
