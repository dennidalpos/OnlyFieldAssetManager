package com.onlyfield.assetmanager

import android.content.Context
import android.content.ContextWrapper
import android.graphics.pdf.PdfDocument
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.local.EncryptedDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.ui.PlanMedia
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FloorNativeTest {
    @Test fun nativePdfRendererReadsBothPagesAndKeepsAspectRatio() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "floor-${UUID.randomUUID()}.pdf")
        try {
            val document = PdfDocument()
            try {
                listOf(800 to 200, 200 to 800).forEachIndexed { index, (width, height) ->
                    val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, index + 1).create())
                    page.canvas.drawColor(android.graphics.Color.WHITE)
                    document.finishPage(page)
                }
                file.outputStream().use { document.writeTo(it) }
            } finally { document.close() }
            assertEquals(2, PlanMedia.pageCount(file))
            val first = PlanMedia.image(file, true, 0, 400)
            val second = PlanMedia.image(file, true, 1, 400)
            assertEquals(400, first.width); assertEquals(100, first.height)
            assertEquals(100, second.width); assertEquals(400, second.height)
        } finally { file.delete() }
    }
}
