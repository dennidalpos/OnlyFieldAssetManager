package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.MapObjectDraft
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.onboarding.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class FloorMediaTest {
    @Test fun pdfPagesRenderWithCorrectShapeAndBounds() {
        val dir = Files.createTempDirectory("ofam_pdf_test").toFile()
        try {
            val file = File(dir, "plans.pdf")
            PDDocument().use { d -> d.addPage(PDPage(PDRectangle(800f, 200f))); d.addPage(PDPage(PDRectangle(200f, 800f))); d.save(file) }
            assertEquals(2, PlanMedia.pageCount(file))
            val a = PlanMedia.image(file, true, 0, 400)
            val b = PlanMedia.image(file, true, 1, 400)
            assertEquals(400, a.width); assertEquals(100, a.height)
            assertEquals(100, b.width); assertEquals(400, b.height)
            assertThrows(IllegalArgumentException::class.java) { PlanMedia.image(file, true, 2) }
            val protected = File(dir, "protected.pdf")
            PDDocument().use { d -> d.addPage(PDPage()); d.protect(StandardProtectionPolicy("owner", "user", AccessPermission())); d.save(protected) }
            assertThrows(java.io.IOException::class.java) { PlanMedia.pageCount(protected) }
            File(dir, "bad.pdf").writeText("invalid PDF")
            assertThrows(java.io.IOException::class.java) { PlanMedia.pageCount(File(dir, "bad.pdf")) }
        } finally { dir.deleteRecursively() }
    }
    @Test fun sharedPdfKeepsFloorPagesAndPositionsAfterBackgroundRemoval() {
        val dir = Files.createTempDirectory("ofam_floor_background_test").toFile()
        val state = DesktopAppState(DesktopStorageManager(File(dir, "data")))
        try {
            val first = Area(name = "Terra")
            val second = Area(name = "Primo")
            val site = Site(name = "BU", areas = listOf(first, second))
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", sites = listOf(site))))
            val draft = MapObjectDraft(type = ObjectCatalog.builtins.first(), siteId = site.id, areaId = first.id)
            assertTrue(state.saveMapObject(draft.copy(device = draft.device.copy(technicalName = "SW")), emptyList(), emptySet()))
            val positions = state.project!!.floorplanPlacements
            val pdf = File(dir, "plan.pdf")
            PDDocument().use { d -> d.addPage(PDPage()); d.addPage(PDPage()); d.save(pdf) }
            val attachment = state.importFloorplan(pdf, first.id)!!
            assertEquals(2, attachment.pageCount)
            state.update(ProjectEdits.setAreaFloorplan(ProjectEdits.setAreaFloorplan(state.project!!, first.id, attachment.id, 0, 2), second.id, attachment.id, 1, 2), "Pagine")
            pdf.delete()
            state.closeProject(); state.openStored(state.storedProjects.first().file)
            val floors = state.project!!.sites.single().areas
            assertEquals(listOf(0, 1), floors.map { it.floorplanPageIndex })
            assertEquals(attachment.id, floors[1].floorplanAttachmentId)
            assertEquals(2, PlanMedia.pageCount(state.attachmentFile(attachment)!!))
            state.update(ProjectEdits.setAreaFloorplan(state.project!!, first.id, null), "Schema")
            assertEquals(positions, state.project!!.floorplanPlacements)
            assertEquals(1, ObjectMap.nodes(state.project!!, first.id).size)
        } finally { state.shutdown(); dir.deleteRecursively() }
    }

    @Test fun cableTypeAndPhotosSurviveEditingAndEncryptedExchange() {
        val dir = Files.createTempDirectory("ofam_cable_photo_test").toFile()
        val state = DesktopAppState(DesktopStorageManager(File(dir, "data")))
        try {
            val area = Area(name = "Terra")
            val site = Site(name = "BU", areas = listOf(area))
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", sites = listOf(site))))
            val photo = File(dir, "cable.png")
            ImageIO.write(BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB), "png", photo)
            val draft = MapObjectDraft(type = ObjectCatalog.builtins.first { it.id == "coax-cable" }, siteId = site.id, areaId = area.id)
            assertTrue(state.saveMapObject(draft, listOf(photo), emptySet()))
            val saved = state.project!!
            val edit = MapObjectDraft.cable(saved, site.id, area.id, draft.id)
            assertEquals("coax-cable", edit.type.id)
            assertTrue(state.saveMapObject(edit.copy(cable = edit.cable.copy(notes = "Connessione TV")), emptyList(), emptySet()))
            assertEquals(saved.attachments, state.project!!.attachments)
            val files = state.project!!.attachments.associate { com.onlyfield.assetmanager.exchange.AttachmentFiles.entryName(it) to state.attachmentFile(it)!!.readBytes() }
            val encrypted = com.onlyfield.assetmanager.exchange.PackageSerializer.exportPackage(state.project!!, password = "test-password", attachments = files)
            val imported = com.onlyfield.assetmanager.exchange.PackageSerializer.importPackage(encrypted, password = "test-password")
            assertTrue(imported.validationResult.isValid)
            assertEquals(state.project, imported.pkg!!.project)
            assertEquals(AttachmentTargetType.CABLE, imported.pkg!!.project.attachments.single().targetType)
        } finally { state.shutdown(); dir.deleteRecursively() }
    }

    @Test fun savedObjectAndPhotosReopenAndFailedPhotoDoesNotCreateObject() {
        val dir = Files.createTempDirectory("ofam_object_test").toFile()
        val state = DesktopAppState(DesktopStorageManager(File(dir, "data")))
        try {
            val area = Area(name = "Terra")
            val site = Site(name = "BU", areas = listOf(area))
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", sites = listOf(site))))
            assertNull(state.selectedSiteId); assertNull(state.selectedAreaId)
            val type = ObjectType(name = "Gateway")
            state.update(state.project!!.copy(objectTypes = listOf(type)), "Tipologia")
            val draft = MapObjectDraft(type = type, siteId = site.id, areaId = area.id)
            val filled = draft.copy(device = draft.device.copy(technicalName = "GW-01"))
            val photo = File(dir, "photo.png")
            ImageIO.write(BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB), "png", photo)
            assertTrue(state.saveMapObject(filled, listOf(photo), emptySet()))
            val beforeFailure = state.project!!
            val badDraft = draft.copy(id = java.util.UUID.randomUUID().toString(), device = draft.device.copy(technicalName = "Failed"))
            assertFalse(state.saveMapObject(badDraft, listOf(photo, File(dir, "missing.png")), emptySet()))
            assertEquals(beforeFailure, state.project)
            assertEquals(1, state.storage.getMediaFolder().walkTopDown().count { it.isFile })
            val stored = state.storedProjects.first().file
            state.closeProject(); state.openStored(stored)
            assertEquals(beforeFailure, state.project)
            assertEquals(type.id, state.project!!.sites.single().devices.single().objectTypeId)
            val attachment = state.project!!.attachments.single()
            assertEquals(filled.id, attachment.targetId)
            assertTrue(state.attachmentFile(attachment)!!.isFile)
            assertNull(state.selectedSiteId); assertNull(state.selectedAreaId)
        } finally { state.shutdown(); dir.deleteRecursively() }
    }
}
