package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.validation.ModelValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FixtureTest {

    @Test
    fun testV1SampleProjectFixtureValidationAndPackageRoundTrip() {
        val fixtureFile = File("../../fixtures/v1_sample_project.json")
        val altFixtureFile = File("fixtures/v1_sample_project.json")
        val fileToRead = if (fixtureFile.exists()) fixtureFile else altFixtureFile

        assertTrue("Fixture file must exist", fileToRead.exists())
        val jsonContent = fileToRead.readText(Charsets.UTF_8)

        val project = PackageSerializer.jsonConfig.decodeFromString(Project.serializer(), jsonContent)
        assertNotNull(project)
        assertEquals("Progetto Campione Infrastruttura v1", project.name)
        assertEquals(2, project.businessUnits.size)

        // Validate model constraints
        val validationResult = ModelValidator.validateProject(project)
        assertTrue(validationResult.isValid)
        assertTrue(validationResult.hasWarnings)

        // Verify specific issues match expected fixture validation behavior
        assertTrue(validationResult.issues.any { it.code == "DUPLICATE_IP_IN_BU" })
        assertTrue(validationResult.issues.any { it.code == "UNPOSITIONED_DEVICE" })
        assertTrue(validationResult.issues.any { it.code == "DETACHED_PORT_ENDPOINT" })

        // Export as package and re-import
        val zipBytes = PackageSerializer.exportPackage(project)
        val importResult = PackageSerializer.importPackage(zipBytes)

        assertTrue(importResult.validationResult.isValid)
        assertNotNull(importResult.pkg)
        assertEquals(project.id, importResult.pkg!!.project.id)
    }
}
