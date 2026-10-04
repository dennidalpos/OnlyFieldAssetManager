package com.onlyfield.assetmanager.export

import com.onlyfield.assetmanager.core.i18n.Messages

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

/** Android print adapter for preview, printer selection and PDF output. */
class ProjectPrintDocumentAdapter(
    private val project: Project,
    private val filterConfig: ExportFilterConfig = ExportFilterConfig(),
    private val reportSelection: ReportSelection = ReportSelection(),
    private val i18n: Messages = Messages()
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
            callback?.onWriteFailed(i18n.text("text.6a0fbef6bf9b"))
            return
        }

        try {
            FileOutputStream(destination.fileDescriptor).use { outputStream ->
                PdfExportManager.exportCompositeReportPdfToStream(
                    project = project,
                    filterConfig = filterConfig,
                    selection = reportSelection,
                    outputStream = outputStream,
                    i18n = i18n)
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
