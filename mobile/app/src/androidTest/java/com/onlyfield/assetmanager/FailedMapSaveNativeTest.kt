package com.onlyfield.assetmanager

import android.graphics.pdf.PdfDocument
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.ui.LocalMessages
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.UiMessage
import com.onlyfield.assetmanager.ui.screens.FloorHomeScreen
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FailedMapSaveNativeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun siteDraftSurvivesDatabaseFailure() = verify("site")
    @Test fun floorDraftSurvivesDatabaseFailure() = verify("floor")
    @Test fun objectDraftAndPositionSurviveDatabaseFailure() = verify("object")
    @Test fun pdfPageSurvivesDatabaseFailure() = verify("pdf")
    @Test fun imageSelectionSurvivesDatabaseFailure() = verify("image")
    @Test fun planRemovalSurvivesDatabaseFailure() = verify("remove")

    private fun verify(picker: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "map-save-${UUID.randomUUID()}").apply { check(mkdir()) }
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = ProjectRepository(db, root, "isolated-test-recovery")
        val owner = ViewModelStore()
        val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val messages = mutableListOf<UiMessage>()
        lateinit var vm: ProjectViewModel
        val area = Area(name = "Original floor")
        val site = Site(name = "Original site", areas = listOf(area))
        val attachment = Attachment(name = "Test plan", originalFileName = if (picker == "image") "plan.png" else "pages.pdf", relativePath = "",
            fileType = if (picker == "image") AttachmentType.IMAGE else AttachmentType.PDF,
            mimeType = if (picker == "image") "image/png" else "application/pdf", pageCount = if (picker == "image") 1 else 2)
        val plan = picker in listOf("pdf", "image", "remove")
        val initial = Project(name = "Isolated map test", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(site), attachments = if (plan) listOf(attachment) else emptyList()).let {
                if (picker == "remove") ProjectEdits.setAreaFloorplan(it, area.id, attachment.id, 1, 2) else it
            }
        try {
            if (plan) {
                val file = AttachmentFiles.localFile(root, initial.id, attachment)
                check(file.parentFile!!.mkdirs())
                if (picker == "image") {
                    val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888)
                    try { file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
                    finally { bitmap.recycle() }
                } else {
                    val doc = PdfDocument()
                    try {
                        repeat(2) { index ->
                            val page = doc.startPage(PdfDocument.PageInfo.Builder(200, 200, index + 1).create())
                            page.canvas.drawColor(android.graphics.Color.WHITE)
                            doc.finishPage(page)
                        }
                        file.outputStream().use { doc.writeTo(it) }
                    } finally { doc.close() }
                }
            }
            runBlocking { repository.saveProject(initial) }
            rule.runOnIdle {
                vm = ProjectViewModel(repository)
                owner.put("map-save", vm)
                collector.launch { vm.messages.collect { messages += it } }
                vm.openProject(initial.id)
            }
            rule.waitUntil(10_000) { vm.project.value != null && vm.busy == null }
            rule.runOnIdle {
                if (picker != "site") vm.selectedSiteId = site.id
                if (picker == "object" || plan) vm.selectedAreaId = area.id
            }
            rule.setContent {
                val project by vm.project.collectAsState()
                CompositionLocalProvider(LocalMessages provides vm.i18n) {
                    MaterialTheme { project?.let { FloorHomeScreen(vm, it, remember { SnackbarHostState() }) } }
                }
            }
            val point = MapPoint(.2f, .7f)
            val action: String
            when (picker) {
                "site", "floor" -> {
                    rule.onNodeWithText(vm.i18n.text(if (picker == "site") "text.4e90901d9fa2" else "text.3575ad226840")).performClick()
                    rule.onNode(hasSetTextAction()).performTextInput("Retained draft")
                    action = vm.i18n.text("text.c5997e85ae51")
                }
                "object" -> {
                    val canvas = rule.onNodeWithTag("floor-map")
                    val size = canvas.fetchSemanticsNode().size
                    val at = MapViewport(size.width.toFloat(), size.height.toFloat(), 1200f, 900f).screen(point)
                    canvas.performTouchInput { longClick(Offset(at.x, at.y)) }
                    rule.onNodeWithText(vm.i18n.text("text.271a55491c4f")).performTextInput("switch")
                    rule.onNodeWithText("Switch").performClick()
                    rule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("Retained draft")
                    action = vm.i18n.text("ux.add")
                }
                else -> {
                    if (rule.onAllNodesWithContentDescription(vm.i18n.text("ux.more")).fetchSemanticsNodes().isNotEmpty())
                        rule.onNodeWithContentDescription(vm.i18n.text("ux.more")).performClick()
                    rule.onNodeWithText(vm.i18n.text("text.68f86d09412c")).performClick()
                    action = when (picker) {
                        "image" -> attachment.name
                        "remove" -> vm.i18n.text("text.f139b3096d6f")
                        else -> {
                            rule.onNodeWithText(attachment.name + vm.i18n.text("text.0bd78b344dac")).performClick()
                            rule.waitUntil(10_000) { rule.onAllNodesWithText(vm.i18n.text("text.c192249f9066", 1)).fetchSemanticsNodes().isNotEmpty() }
                            rule.onNode(hasScrollAction() and hasAnyAncestor(isDialog()))
                                .performScrollToNode(hasText(vm.i18n.text("text.c192249f9066", 2)))
                            rule.onNodeWithText(vm.i18n.text("text.c192249f9066", 2)).performClick()
                            vm.i18n.text("text.f62c5da23e83", 2)
                        }
                    }
                }
            }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_map_save BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'isolated map save denied'); END")
            closeSoftKeyboard()
            rule.waitForIdle()
            rule.onNodeWithText(action).performClick()
            rule.waitUntil(10_000) { vm.busy == null }
            rule.runOnIdle { assertEquals(initial, vm.project.value); assertTrue(messages.any { it.isError }); assertTrue(messages.none { it.undo != null }) }
            runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
            if (!plan) rule.onNode(hasSetTextAction() and hasText("Retained draft")).performScrollTo().assertIsDisplayed()
            rule.onNodeWithText(action).assertIsDisplayed()
            val error = rule.onNodeWithText("isolated map save denied", substring = true)
            if (picker == "site" || picker == "floor") error.performScrollTo()
            error.assertIsDisplayed()
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_map_save")
            rule.onNodeWithText(action).performClick()
            rule.waitUntil(10_000) { vm.busy == null }
            rule.runOnIdle {
                val saved = vm.project.value!!
                when (picker) {
                    "site" -> assertEquals("Retained draft", saved.sites.single { it.id != site.id }.name)
                    "floor" -> assertEquals("Retained draft", saved.sites.single().areas.single { it.id != area.id }.name)
                    "pdf" -> { assertEquals(attachment.id, saved.sites.single().areas.single().floorplanAttachmentId); assertEquals(1, saved.sites.single().areas.single().floorplanPageIndex) }
                    "image" -> { assertEquals(attachment.id, saved.sites.single().areas.single().floorplanAttachmentId); assertEquals(0, saved.sites.single().areas.single().floorplanPageIndex) }
                    "remove" -> assertNull(saved.sites.single().areas.single().floorplanAttachmentId)
                    else -> {
                        assertEquals("Retained draft", saved.sites.single().devices.single().technicalName)
                        assertEquals("switch", saved.sites.single().devices.single().objectTypeId)
                        assertTrue(saved.sites.single().devices.single().hardware.portGroups.isNotEmpty())
                        assertEquals(point.x, saved.floorplanPlacements.single().xRatio, .01f)
                        assertEquals(point.y, saved.floorplanPlacements.single().yRatio, .01f)
                    }
                }
                assertEquals(1, messages.count { it.undo != null })
            }
            runBlocking { assertEquals(vm.project.value, repository.getProjectById(initial.id)) }
            rule.onNodeWithText(action).assertDoesNotExist()
        } finally {
            collector.cancel()
            rule.runOnIdle { owner.clear() }
            db.close()
            check(root.canonicalFile.parentFile == context.cacheDir.canonicalFile)
            check(root.deleteRecursively())
        }
    }
}
