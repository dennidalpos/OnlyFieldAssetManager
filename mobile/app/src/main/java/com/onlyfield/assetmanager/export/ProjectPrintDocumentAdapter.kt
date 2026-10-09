package com.onlyfield.assetmanager.export

import com.onlyfield.assetmanager.core.i18n.Messages

import android.content.Context
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
import kotlinx.coroutines.*
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/** Android print adapter for preview, printer selection and PDF output. */
class ProjectPrintDocumentAdapter(
    private val context: Context,
    private val project: Project,
    private val filterConfig: ExportFilterConfig = ExportFilterConfig(),
    private val reportSelection: ReportSelection = ReportSelection(),
    private val i18n: Messages = Messages()
) : PrintDocumentAdapter() {
    private var attributes: PrintAttributes? = null
    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        if (newAttributes == null) {
            callback?.onLayoutFailed(i18n.text("text.6a0fbef6bf9b"))
            return
        }
        attributes = null
        val delivered = AtomicBoolean()
        val job = worker.launch {
            var count = 0
            var failure: String? = null
            var cancelled = false
            try {
                count = PdfExportManager.layoutPrint(context, newAttributes, project, filterConfig, reportSelection, i18n) {
                    ensureActive()
                    cancellationSignal?.throwIfCanceled()
                }
            } catch (_: CancellationException) {
                cancelled = true
            } catch (e: Exception) {
                failure = e.message ?: i18n.text("text.6a0fbef6bf9b")
            }
            cancelled = cancelled || !isActive || cancellationSignal?.isCanceled == true
            withContext(NonCancellable + Dispatchers.Main) {
                if (!delivered.compareAndSet(false, true)) return@withContext
                when {
                    cancelled -> callback?.onLayoutCancelled()
                    failure != null -> callback?.onLayoutFailed(failure)
                    else -> {
                        attributes = newAttributes
                        val info = PrintDocumentInfo.Builder("Documento_${project.name}.pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(count).build()
                        callback?.onLayoutFinished(info, newAttributes != oldAttributes)
                    }
                }
            }
        }
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException) Handler(Looper.getMainLooper()).post {
                if (delivered.compareAndSet(false, true)) callback?.onLayoutCancelled()
            }
        }
        cancellationSignal?.setOnCancelListener { job.cancel() }
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
        if (cancellationSignal?.isCanceled == true) {
            destination.close()
            callback?.onWriteCancelled()
            return
        }

        val layoutAttributes = attributes
        if (layoutAttributes == null || pages.isNullOrEmpty()) {
            destination.close()
            callback?.onWriteFailed(i18n.text("text.6a0fbef6bf9b"))
            return
        }
        val requested = pages.copyOf()
        val delivered = AtomicBoolean()
        val job = worker.launch {
            var written: Array<PageRange> = emptyArray()
            var failure: String? = null
            var cancelled = false
            try {
                ensureActive()
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use { outputStream ->
                    written = PdfExportManager.writePrint(context, layoutAttributes, requested, project, filterConfig,
                        reportSelection, outputStream, i18n) { ensureActive(); cancellationSignal?.throwIfCanceled() }
                }
            } catch (_: CancellationException) {
                cancelled = true
            } catch (e: Exception) {
                failure = e.message ?: i18n.text("text.6a0fbef6bf9b")
            }
            cancelled = cancelled || !isActive || cancellationSignal?.isCanceled == true
            withContext(NonCancellable + Dispatchers.Main) {
                if (!delivered.compareAndSet(false, true)) return@withContext
                when {
                    cancelled -> callback?.onWriteCancelled()
                    failure != null -> callback?.onWriteFailed(failure)
                    else -> callback?.onWriteFinished(written)
                }
            }
        }
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException) Handler(Looper.getMainLooper()).post {
                destination.close()
                if (delivered.compareAndSet(false, true)) callback?.onWriteCancelled()
            }
        }
        cancellationSignal?.setOnCancelListener { job.cancel() }

    }

    override fun onFinish() {
        worker.cancel()
        super.onFinish()
    }
}
