package com.onlyfield.assetmanager.pc

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.sun.nio.file.ExtendedOpenOption
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO

@RunWith(Parameterized::class)
class FailedMapPickerSaveTest(private val picker: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun pickers() = listOf("object-point", "object-container", "plan-pdf", "plan-image", "plan-remove")
            .map { arrayOf(it) }
    }

    @get:Rule val compose = createComposeRule()
    @get:Rule val folder = TemporaryFolder()

    @Test fun failedSaveKeepsSelectionAndRetryCommitsItOnce() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val area = Area(name = "Test floor")
        val site = Site(name = "Test site", areas = listOf(area))
        val rack = Rack(name = "Test rack", areaId = area.id)
        val point = MapPoint(.2f, .7f)
        var attachment: Attachment? = null
        try {
            val project = Project(name = "Picker test", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(site),
                racks = if (picker == "object-container") listOf(rack) else emptyList(),
                floorplanPlacements = if (picker == "object-container") listOf(FloorplanPlacement(areaId = area.id,
                    targetType = PlacementTargetType.RACK, targetId = rack.id, xRatio = .5f, yRatio = .5f)) else emptyList())
            state.importFile(storage.saveProjectLocally(project), compare = false)
            if (picker.startsWith("plan-")) {
                val file = folder.newFile(if (picker == "plan-image") "image.png" else "pages.pdf")
                if (picker == "plan-image") ImageIO.write(BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "png", file)
                else PDDocument().use { d -> d.addPage(PDPage()); d.addPage(PDPage()); d.save(file) }
                attachment = requireNotNull(state.importFloorplan(file, area.id))
                if (picker == "plan-remove") state.update(ProjectEdits.setAreaFloorplan(state.project!!, area.id, attachment.id, 1, 2), "Assign")
            }
            state.section = AppSection.FLOORPLANS
            state.selectedSiteId = site.id
            state.selectedAreaId = area.id
            compose.setContent { DesktopApp(state) }
            val action: String
            if (picker.startsWith("object-")) {
                val canvas = compose.onNodeWithTag("floor-map")
                val size = canvas.fetchSemanticsNode().size
                val viewport = MapViewport(size.width.toFloat(), size.height.toFloat(), 1200f, 900f)
                if (picker == "object-point") {
                    val at = viewport.screen(point)
                    canvas.performTouchInput { longClick(Offset(at.x, at.y)) }
                } else {
                    val at = viewport.screen(MapPoint(.5f, .5f))
                    canvas.performTouchInput { click(Offset(at.x, at.y)) }
                    compose.onNodeWithText(state.i18n.text("map.open")).performClick()
                    compose.onNodeWithText(state.i18n.text("map.addHere")).performClick()
                }
                compose.onNodeWithText("Cerca tipologia…").performTextInput("switch")
                compose.onNodeWithText("Switch").performClick()
                compose.onNode(hasSetTextAction()).performTextReplacement("Retained switch")
                action = state.i18n.text("ux.add")
            } else {
                compose.onNodeWithContentDescription(state.i18n.text("ux.more")).performClick()
                compose.onNodeWithText(state.i18n.text("text.68f86d09412c")).performClick()
                action = when (picker) {
                    "plan-remove" -> state.i18n.text("text.f139b3096d6f")
                    "plan-image" -> attachment!!.name
                    else -> {
                        compose.onNodeWithText(attachment!!.name + state.i18n.text("text.0bd78b344dac")).performClick()
                        compose.onNodeWithText(state.i18n.text("text.c192249f9066", 2)).performClick()
                        state.i18n.text("text.f62c5da23e83", 2)
                    }
                }
            }
            val local = storage.listStoredProjects().single().file
            val bytes = local.readBytes()
            val before = state.project
            val undo = state.canUndo
            Files.newByteChannel(local.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
                compose.onNodeWithText(action).assertIsEnabled().performClick()
                compose.runOnIdle {
                    assertNotNull(state.error)
                    assertEquals(before, state.project)
                    assertEquals(undo, state.canUndo)
                    assertArrayEquals(bytes, local.readBytes())
                }
                if (picker.startsWith("object-")) compose.onNode(hasSetTextAction() and hasText("Retained switch")).assertIsDisplayed()
                else compose.onNodeWithText(action).assertIsDisplayed()
                compose.onNode(hasText(requireNotNull(state.error)) and hasAnyAncestor(isDialog())).assertIsDisplayed()
            }
            compose.onNodeWithText(action).performClick()
            compose.runOnIdle {
                assertNull(state.error)
                val saved = requireNotNull(state.project)
                if (picker.startsWith("object-")) {
                    val device = saved.sites.single().devices.single()
                    assertEquals("Retained switch", device.technicalName)
                    assertEquals(area.id, device.areaId)
                    assertEquals("switch", device.objectTypeId)
                    assertTrue(device.hardware.portGroups.isNotEmpty())
                    if (picker == "object-container") assertEquals(ObjectRef(PlacementTargetType.RACK, rack.id), ObjectHierarchy.parent(saved, ObjectRef(PlacementTargetType.DEVICE, device.id)))
                    else {
                        val placement = saved.floorplanPlacements.single()
                        assertEquals(point.x, placement.xRatio, .001f)
                        assertEquals(point.y, placement.yRatio, .001f)
                    }
                } else {
                    val savedArea = saved.sites.single().areas.single()
                    assertEquals(if (picker == "plan-remove") null else attachment!!.id, savedArea.floorplanAttachmentId)
                    assertEquals(if (picker == "plan-pdf") 1 else 0, savedArea.floorplanPageIndex)
                }
                storage.importPackageFromFile(local).pkg!!.use { assertEquals(saved, it.project) }
                state.undo()
                assertEquals(before, state.project)
                assertEquals(undo, state.canUndo)
            }
            compose.onNode(isDialog()).assertDoesNotExist()
        } finally { state.shutdown() }
    }
}
