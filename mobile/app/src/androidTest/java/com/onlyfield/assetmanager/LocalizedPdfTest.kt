package com.onlyfield.assetmanager

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizedPdfTest {
    @Test
    fun repositoryProducesRealRackAndCompositePdfsInThreeLanguages() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val area = Area(name = "Planta del usuario")
        val rack = Rack(name = "Rack del usuario", heightU = 42, areaId = area.id)
        val project = Project(name = "Proyecto España", createdEpochMs = 0, updatedEpochMs = 0,
            racks = listOf(rack), businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(area), devices = listOf(
                Device(technicalName = "SW-CODICE-001", rackId = rack.id, areaId = area.id, positionU = 1)
            ))), credentials = listOf(Credential(username = "SECRET_USER", secret = "SECRET_VALUE")))
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val file = File.createTempFile("localized-report-", ".pdf", context.cacheDir)
        try {
            val repository = ProjectRepository(database)
            repository.saveProject(project)
            listOf("it", "en", "es").forEach { language ->
                val messages = Messages(Locale.forLanguageTag(language))
                val outputs = listOf(
                    ByteArrayOutputStream().also { assertTrue(repository.exportRackPdfToStream(project.id, rack.id, it, messages)) },
                    ByteArrayOutputStream().also { assertTrue(repository.exportCompositePdfToStream(project.id, ExportFilterConfig(), ReportSelection(), it, messages)) }
                )
                outputs.forEachIndexed { index, out ->
                    val bytes = out.toByteArray()
                    val raw = bytes.toString(Charsets.ISO_8859_1)
                    assertTrue("$language header", raw.startsWith("%PDF-"))
                    assertTrue("$language trailer", raw.contains("%%EOF"))
                    assertFalse(raw.contains("SECRET_VALUE"))
                    assertFalse(raw.contains("SECRET_USER"))
                    file.writeBytes(bytes)
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            assertTrue("$language pages", renderer.pageCount >= if (index == 0) 1 else 2)
                            val text = (0 until renderer.pageCount).joinToString("\n") { pageIndex ->
                                renderer.openPage(pageIndex).use { page -> page.textContents.joinToString(" ") { it.text } }
                            }
                            assertTrue("$language user title", text.contains(project.name))
                            assertTrue("$language device", text.contains("SW-CODICE-001"))
                            val heading = if (index == 0) messages.text("text.f24b33e201a1", rack.name) else messages.text("text.40ceb13eaea5")
                            assertTrue("$language heading: $heading", text.contains(heading))
                            assertFalse(text.contains("SECRET_USER"))
                            assertFalse(text.contains("SECRET_VALUE"))
                        }
                    }
                }
            }
        } finally {
            database.close()
            file.delete()
        }
    }
}
