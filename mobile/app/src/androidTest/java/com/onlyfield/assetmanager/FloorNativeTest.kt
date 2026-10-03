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
    @Test fun encryptedUpgradeBacksUpOldDataAndBackupCanBeRestored() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(base.cacheDir, "native-migration-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String) = File(root, name)
            override fun getNoBackupFilesDir() = File(root, "no_backup").apply { mkdirs() }
        }
        val project = Project(name = "Native fixture", createdEpochMs = 1, updatedEpochMs = 1,
            businessUnits = listOf(BusinessUnit(name = "BU", devices = listOf(Device(technicalName = "SW", serialNumber = "S123")))))
        try {
            val template = Room.inMemoryDatabaseBuilder(base, AppDatabase::class.java).build()
            val schemas = try {
                template.openHelper.writableDatabase.query("SELECT sql FROM sqlite_master WHERE sql IS NOT NULL AND type IN ('table','index') AND name NOT IN ('android_metadata','room_master_table') ORDER BY type DESC").use { c ->
                    buildList { while (c.moveToNext()) add(c.getString(0)) }
                }
            } finally { template.close() }
            val source = context.getDatabasePath(EncryptedDatabase.DB_NAME)
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(source, null).use { db ->
                schemas.forEach { sql -> db.execSQL(sql.replace(", `objectTypesJson` TEXT NOT NULL", "").replace(", `cableRoutesJson` TEXT NOT NULL", "")
                    .replace(", `objectTypeId` TEXT", "").replace("`objectTypeId` TEXT, ", "").replace("`deviceAId` TEXT, ", "").replace("`deviceBId` TEXT, ", "")) }
                db.execSQL("INSERT INTO projects (id,name,createdEpochMs,updatedEpochMs,isPasswordProtected) VALUES (?,?,?,?,0)", arrayOf<Any>(project.id, project.name, 1L, 1L))
                val bu = project.businessUnits.single()
                db.execSQL("INSERT INTO business_units (id,projectId,name) VALUES (?,?,?)", arrayOf(bu.id, project.id, bu.name))
                val device = bu.devices.single()
                db.execSQL("INSERT INTO devices (id,businessUnitId,technicalName,heightU,rackSide,mountingType,category,serialNumber) VALUES (?,?,?,1,'BOTH','OUT_OF_RACK','CUSTOM',?)", arrayOf(device.id, bu.id, device.technicalName, device.serialNumber))
                db.version = 11
            }
            val upgraded = EncryptedDatabase.open(context)
            try {
                assertEquals(project, ProjectRepository(upgraded).getProjectById(project.id))
                assertEquals(12, upgraded.openHelper.writableDatabase.version)
            } finally { upgraded.close() }
            val backup = File(context.noBackupFilesDir, "${EncryptedDatabase.DB_NAME}.v11.backup")
            assertTrue(backup.isFile && backup.length() > 0)
            assertFalse(EncryptedDatabase.isPlaintextSqlite(backup))
            assertFalse(EncryptedDatabase.isPlaintextSqlite(source))
            backup.copyTo(source, overwrite = true)
            val restored = EncryptedDatabase.open(context)
            try { assertEquals(project, ProjectRepository(restored).getProjectById(project.id)) }
            finally { restored.close() }
        } finally { root.deleteRecursively() }
    }

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
