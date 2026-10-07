package com.onlyfield.assetmanager

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.data.local.EncryptedDatabase
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.PackageSerializer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EncryptedSchemaUpgradeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val assets = InstrumentationRegistry.getInstrumentation().context.assets

    private fun isolatedContext(name: String) = object : ContextWrapper(context) {
        override fun getApplicationContext(): Context = this
        override fun getDatabasePath(ignored: String): File = context.getDatabasePath(name)
    }

    private suspend fun <T> withDatabase(context: Context, block: suspend (AppDatabase) -> T): T {
        val database = EncryptedDatabase.open(context)
        try { return block(database) } finally { database.close() }
    }

    @Test fun previousV1SchemaIsRecreatedAndDemoCanBeReimported() = runBlocking {
        val name = "schema-upgrade-${UUID.randomUUID()}.db"
        val isolated = isolatedContext(name)
        val legacy = assets.open("legacy-room-v1.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        try {
            withDatabase(isolated) { database ->
                val sql = database.openHelper.writableDatabase
                sql.execSQL("PRAGMA foreign_keys=OFF")
                val tables = sql.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").use { cursor ->
                    buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
                }
                tables.forEach { sql.execSQL("DROP TABLE `${it.replace("`", "``")}`") }
                val entities = legacy.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                    val indices = entity.optJSONArray("indices")
                    if (indices != null) for (j in 0 until indices.length()) {
                        sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                    }
                }
                val setup = legacy.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) sql.execSQL(setup.getString(i))
                sql.execSQL("INSERT INTO projects (id, name, createdEpochMs, updatedEpochMs, isPasswordProtected, objectTypesJson, cableRoutesJson, objectContainmentsJson) VALUES ('legacy-project', 'Old test data', 0, 0, 0, '[]', '[]', '[]')")
                sql.version = 1
            }
            withDatabase(isolated) { database ->
                assertEquals(2, database.openHelper.writableDatabase.version)
                database.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE name='business_units'").use { assertFalse(it.moveToFirst()) }
                val repository = ProjectRepository(database)
                assertTrue(repository.getAllProjects().first().isEmpty())
                val demo = assets.open("onlyfield-demo.ofam").use { requireNotNull(PackageSerializer.importPackage(it).pkg) }
                assertTrue(repository.importProjectPackage(demo))
                val restored = requireNotNull(repository.getProjectById(demo.project.id))
                assertEquals("Demo Comune", restored.name)
                assertEquals(356, restored.sites.sumOf { it.devices.size })
            }
            withDatabase(isolated) { database ->
                assertEquals("Demo Comune", ProjectRepository(database).getAllProjects().first().single().name)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun sameVersionReopensWithoutDroppingData() = runBlocking {
        val name = "schema-reopen-${UUID.randomUUID()}.db"
        val isolated = isolatedContext(name)
        val project = Project(name = "Keep v2", createdEpochMs = 0, updatedEpochMs = 0)
        try {
            withDatabase(isolated) { database -> ProjectRepository(database).saveProject(project) }
            withDatabase(isolated) { database ->
                assertEquals(2, database.openHelper.writableDatabase.version)
                assertEquals(project, ProjectRepository(database).getProjectById(project.id))
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun newerVersionIsRecreatedOnDowngrade() = runBlocking {
        val name = "schema-downgrade-${UUID.randomUUID()}.db"
        val isolated = isolatedContext(name)
        try {
            withDatabase(isolated) { database ->
                ProjectRepository(database).saveProject(Project(name = "Old v14 test data", createdEpochMs = 0, updatedEpochMs = 0))
                database.openHelper.writableDatabase.version = 14
            }
            withDatabase(isolated) { database ->
                assertEquals(2, database.openHelper.writableDatabase.version)
                assertTrue(ProjectRepository(database).getAllProjects().first().isEmpty())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun demoPackageIsImportedOnDisk() = runBlocking {
        val seedApplication = InstrumentationRegistry.getArguments().getString("seedApplicationDemo") == "true"
        val name = "schema-demo-${UUID.randomUUID()}.db"
        val target = if (seedApplication) context else isolatedContext(name)
        val attachments = if (seedApplication) File(context.filesDir, "attachments") else File(context.cacheDir, name)
        val demo = assets.open("onlyfield-demo.ofam").use { requireNotNull(PackageSerializer.importPackage(it).pkg) }
        try {
            withDatabase(target) { database ->
                val repository = ProjectRepository(database, attachments, recoveryPassword = EncryptedDatabase.recoveryPassword(target))
                assertEquals(2, database.openHelper.writableDatabase.version)
                // Opt-in phone setup requires an empty database and never replaces an existing project.
                assertTrue(repository.getAllProjects().first().isEmpty())
                assertTrue(repository.importProjectPackage(demo))
                assertEquals(demo.project.sites.sumOf { it.devices.size }, repository.getProjectById(demo.project.id)!!.sites.sumOf { it.devices.size })
                assertTrue(repository.missingAttachments(repository.getProjectById(demo.project.id)!!).isEmpty())
            }
            withDatabase(target) { database ->
                assertEquals("Demo Comune", ProjectRepository(database).getAllProjects().first().single().name)
            }
        } finally {
            if (!seedApplication) {
                context.deleteDatabase(name)
                check(attachments.canonicalFile.parentFile == context.cacheDir.canonicalFile)
                check(!attachments.exists() || attachments.deleteRecursively())
            }
        }
    }
}
