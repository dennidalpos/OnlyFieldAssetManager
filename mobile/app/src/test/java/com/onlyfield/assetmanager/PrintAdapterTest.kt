package com.onlyfield.assetmanager

import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.annotation.GraphicsMode
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.export.ProjectPrintDocumentAdapter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PrintAdapterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val project = Project(name = "Print fixture", createdEpochMs = 0, updatedEpochMs = 0,
        sites = listOf(Site(name = "Site", devices = (0 until 160).map { Device(technicalName = "PRINT-DEVICE-$it") })))
    private fun main(action: () -> Unit) = action()
    private fun attributes(landscape: Boolean = false) = PrintAttributes.Builder()
        .setMediaSize(if (landscape) PrintAttributes.MediaSize.ISO_A5.asLandscape() else PrintAttributes.MediaSize.ISO_A4)
        .setResolution(PrintAttributes.Resolution("test", "Test", 300, 300))
        .setMinMargins(PrintAttributes.Margins(300, 400, 500, 600)).setColorMode(PrintAttributes.COLOR_MODE_COLOR).build()

    private fun layout(adapter: ProjectPrintDocumentAdapter, attrs: PrintAttributes, signal: CancellationSignal = CancellationSignal()): Pair<Int, Int> {
        val done = CountDownLatch(1)
        val calls = AtomicInteger()
        var count = -1
        var error: CharSequence? = null
        main { adapter.onLayout(null, attrs, signal, object : TestPrintCallbacks.Layout() {
            override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) { count = info.pageCount; calls.incrementAndGet(); done.countDown() }
            override fun onLayoutCancelled() { count = -2; calls.incrementAndGet(); done.countDown() }
            override fun onLayoutFailed(message: CharSequence?) { error = message; calls.incrementAndGet(); done.countDown() }
        }, null) }
        await(done)
        assertNull(error)
        return count to calls.get()
    }

    private fun await(done: CountDownLatch) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (done.count > 0 && System.nanoTime() < deadline) { ShadowLooper.idleMainLooper(); Thread.sleep(10) }
        ShadowLooper.idleMainLooper()
        assertEquals("Callback timed out", 0, done.count)
    }

    @Test fun layoutReportsActualPagination() {
        val adapter = ProjectPrintDocumentAdapter(context, project)
        try {
            val (count, calls) = layout(adapter, attributes())
            assertTrue("Layout must report actual pages: $count", count > 3)
            assertEquals(1, calls)
        } finally { main { adapter.onFinish() } }
    }

    @Test fun unsupportedJvmPdfGenerationFailsOnceAndClosesDestination() {
        // Robolectric has no PdfDocument native writer: verify the adapter's failure path here.
        val adapter = ProjectPrintDocumentAdapter(context, project)
        val file = File.createTempFile("print-adapter-", ".pdf", context.cacheDir)
        try {
            layout(adapter, attributes())
            val done = CountDownLatch(1); val calls = AtomicInteger()
            var error: CharSequence? = null
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
            adapter.onWrite(arrayOf(PageRange(1, 1)), descriptor, CancellationSignal(), object : TestPrintCallbacks.Write() {
                override fun onWriteFinished(pages: Array<out PageRange>) { calls.incrementAndGet(); done.countDown() }
                override fun onWriteFailed(message: CharSequence?) { error = message; calls.incrementAndGet(); done.countDown() }
            })
            await(done); assertFalse(error.isNullOrBlank()); assertEquals(1, calls.get())
            assertFalse(descriptor.fileDescriptor.valid())
        } finally { adapter.onFinish(); check(file.delete()) }
    }

    @Test fun cancellationAndInvalidDestinationCompleteOnce() {
        val adapter = ProjectPrintDocumentAdapter(context, project)
        try {
            val cancelled = CancellationSignal().apply { cancel() }
            assertEquals(-2 to 1, layout(adapter, attributes(), cancelled))
            val calls = AtomicInteger()
            adapter.onWrite(arrayOf(PageRange.ALL_PAGES), null, CancellationSignal(), object : TestPrintCallbacks.Write() {
                override fun onWriteFailed(error: CharSequence?) { assertFalse(error.isNullOrBlank()); calls.incrementAndGet() }
            })
            assertEquals(1, calls.get())
        } finally { adapter.onFinish() }
    }
}
