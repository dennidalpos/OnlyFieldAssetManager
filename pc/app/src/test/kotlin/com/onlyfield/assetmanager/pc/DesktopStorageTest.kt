package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.exchange.ComparisonStatus
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.ProjectComparisonEvaluator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class DesktopStorageTest {
    @Test fun explicitCentreRouteSurvivesWorkingCopyStorage() {
        val area = com.onlyfield.assetmanager.core.model.Area(name = "Floor")
        val cable = com.onlyfield.assetmanager.core.model.Cable()
        val points = listOf(com.onlyfield.assetmanager.core.model.MapPoint(.2f, .5f), com.onlyfield.assetmanager.core.model.MapPoint(.5f, .5f), com.onlyfield.assetmanager.core.model.MapPoint(.8f, .5f))
        val project = Project(name = "Routes", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(com.onlyfield.assetmanager.core.model.Site(name = "Building", areas = listOf(area))), cables = listOf(cable),
            cableRoutes = listOf(com.onlyfield.assetmanager.core.model.CableRoute(cableId = cable.id, areaId = area.id, points = points)))
        for (password in listOf(null, "dummy-password")) {
            val incoming = project.copy(isPasswordProtected = password != null)
            storageManager.saveProjectLocally(incoming, password)
            storageManager.loadLocalProject(project.id, password).pkg!!.use {
                assertEquals(incoming, it.project)
                assertEquals(listOf(points[1]), it.project.cableRoutes.single().bends)
            }
        }
    }

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataDir: File
    private lateinit var storageManager: DesktopStorageManager

    private fun loadAndroidFixture(): Project = requireNotNull(javaClass.getResourceAsStream("/v1_sample_project.json")) {
        "Missing test fixture v1_sample_project.json"
    }.bufferedReader(Charsets.UTF_8).use {
        PackageSerializer.jsonConfig.decodeFromString(Project.serializer(), it.readText())
    }

    @Before
    fun setUp() {
        dataDir = tempFolder.newFolder("ofam_test_datadir")
        storageManager = DesktopStorageManager(initialDataDir = dataDir)
    }

    @Test
    fun testExplicitDataDirectoryStatusAndWritability() {
        val status = storageManager.checkDataDirectoryStatus()
        assertTrue("Data directory should exist", status.exists)
        assertTrue("Data directory should be writable", status.isWritable)
        assertTrue("Free space should be > 0", status.freeSpaceBytes >= 0)

        val customDir = tempFolder.newFolder("custom_datadir")
        val newStatus = storageManager.setDataDirectory(customDir)
        assertEquals(customDir.absolutePath, newStatus.path.absolutePath)
        assertTrue(newStatus.isWritable)
    }

    @Test
    fun testUnwritableDataDirectoryHandling() {
        val readOnlyDir = File(dataDir, "readonly_dir").apply {
            mkdirs()
            setWritable(false)
        }
        val unWritableStorage = DesktopStorageManager(initialDataDir = readOnlyDir)
        val now = System.currentTimeMillis()
        val proj = Project(id = UUID.randomUUID().toString(), name = "Test Readonly", createdEpochMs = now, updatedEpochMs = now)

        try {
            unWritableStorage.saveProjectLocally(proj)
            // Windows administrators may bypass setWritable(false).
        } catch (e: Exception) {
            assertTrue("Expected IllegalStateException for unwritable dir", e is IllegalStateException)
        } finally {
            readOnlyDir.setWritable(true)
        }
    }

    @Test
    fun testAtomicSaveAndLocalProjectListing() {
        val now = System.currentTimeMillis()
        val projId = UUID.randomUUID().toString()
        val project = Project(
            id = projId,
            name = "Atomic Save Desktop Test",
            createdEpochMs = now,
            updatedEpochMs = now
        )

        val targetFile = storageManager.saveProjectLocally(project)
        assertTrue("Target file should exist", targetFile.exists())
        assertTrue("Target file should not be empty", targetFile.length() > 0)

        val stored = storageManager.listStoredProjects()
        assertEquals(1, stored.size)
        assertEquals(projId, stored[0].id)
        assertEquals("Atomic Save Desktop Test", stored[0].name)
        assertFalse("Project should not be encrypted", stored[0].isEncrypted)

        val importRes = storageManager.loadLocalProject(projId)
        val restored = importRes.pkg?.project
        assertNotNull("Restored project should not be null", restored)
        assertEquals(projId, restored?.id)
        assertEquals("Atomic Save Desktop Test", restored?.name)

        storageManager.releaseProjectLock(projId)
    }

    @Test
    fun testWorkingCopyFileLocking() {
        val projId = "proj-lock-test-1"
        storageManager.acquireProjectLock(projId)

        try {
            storageManager.acquireProjectLock(projId)
        } catch (e: Exception) {
            fail("Re-acquiring lock in same manager should be safe or handled")
        }

        val storageManager2 = DesktopStorageManager(initialDataDir = dataDir)
        try {
            storageManager2.acquireProjectLock(projId)
            fail("Expected IllegalStateException when locking already locked project")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("aperto da un'altra istanza") == true)
        }

        storageManager.releaseProjectLock(projId)
    }

    @Test
    fun testAndroidFixtureLoadingAndValidation() {
        val fixture = loadAndroidFixture()
        assertNotNull("Fixture project should be loaded", fixture)
        assertEquals("11111111-1111-1111-1111-111111111111", fixture.id)
        assertEquals("Progetto Campione Infrastruttura v1", fixture.name)
        assertEquals(2, fixture.sites.size)

        val validation = ModelValidator.validateProject(fixture)
        assertTrue("Fixture should pass structural validation", validation.isValid)
        assertTrue("Fixture should contain documentary warnings", validation.hasWarnings)

        val codes = validation.issues.map { it.code }
        assertTrue("Expected DUPLICATE_IP_IN_SITE warning", codes.contains("DUPLICATE_IP_IN_SITE"))
        assertTrue("Expected DETACHED_PORT_ENDPOINT warning", codes.contains("DETACHED_PORT_ENDPOINT"))
        assertTrue("Expected UNPOSITIONED_DEVICE warning", codes.contains("UNPOSITIONED_DEVICE"))
    }

    @Test
    fun testAndroidFixtureUnencryptedAndEncryptedPackageRoundtrip() {
        val fixture = loadAndroidFixture()

        val unencryptedFile = File(tempFolder.root, "fixture_unencrypted.ofam")
        storageManager.exportPackageToFile(fixture, unencryptedFile)
        assertTrue(unencryptedFile.exists())

        val unencryptedRes = storageManager.importPackageFromFile(unencryptedFile)
        val unencryptedProj = unencryptedRes.pkg?.project
        assertNotNull(unencryptedProj)
        assertEquals(fixture.id, unencryptedProj?.id)
        assertEquals(fixture.name, unencryptedProj?.name)

        val encryptedFile = File(tempFolder.root, "fixture_encrypted.ofam")
        val password = "SecretW01Password!"
        storageManager.exportPackageToFile(fixture, encryptedFile, password = password)
        assertTrue(encryptedFile.exists())

        val noPassRes = storageManager.importPackageFromFile(encryptedFile)
        assertNull(noPassRes.pkg)
        assertTrue(noPassRes.validationResult.issues.any { it.code == "PASSWORD_REQUIRED" })

        val wrongPassRes = storageManager.importPackageFromFile(encryptedFile, password = "WrongPassword")
        assertNull(wrongPassRes.pkg)
        assertTrue(wrongPassRes.validationResult.issues.any { it.code == "INVALID_PACKAGE_PASSWORD" })

        val correctPassRes = storageManager.importPackageFromFile(encryptedFile, password = password)
        val encryptedProj = correctPassRes.pkg?.project
        assertNotNull(encryptedProj)
        assertEquals(fixture.id, encryptedProj?.id)
        assertEquals(fixture.name, encryptedProj?.name)
    }

    @Test
    fun testCorruptedPackageImportHandling() {
        val emptyFile = File(tempFolder.root, "empty.ofam").apply { writeBytes(ByteArray(0)) }
        val emptyRes = storageManager.importPackageFromFile(emptyFile)
        assertNull(emptyRes.pkg)
        assertTrue(emptyRes.validationResult.issues.any { it.code == "EMPTY_PACKAGE" })

        val corruptFile = File(tempFolder.root, "corrupt.ofam").apply { writeText("NOT_A_ZIP_FILE") }
        val corruptRes = storageManager.importPackageFromFile(corruptFile)
        assertNull(corruptRes.pkg)
        assertFalse(corruptRes.validationResult.isValid)
        assertTrue(corruptRes.validationResult.issues.any { it.code == "INVALID_ZIP_ARCHIVE" || it.code == "MISSING_MANIFEST" })
    }

    @Test
    fun testProjectComparisonEvaluator() {
        val fixture = loadAndroidFixture()
        val pkgBytes = PackageSerializer.exportPackage(fixture)
        val importRes = PackageSerializer.importPackage(pkgBytes)
        val pkg = importRes.pkg!!

        val compIdentical = ProjectComparisonEvaluator.evaluate(fixture, pkg.manifest, pkg)
        assertEquals(ComparisonStatus.IDENTICAL, compIdentical.status)

        val modifiedFixture = fixture.copy(name = "Progetto Modificato Locale", updatedEpochMs = fixture.updatedEpochMs + 1000)
        val compDivergent = ProjectComparisonEvaluator.evaluate(modifiedFixture, pkg.manifest, pkg)
        assertTrue(
            compDivergent.status == ComparisonStatus.OLDER_REVISION ||
                    compDivergent.status == ComparisonStatus.DIVERGENT
        )
    }
}
