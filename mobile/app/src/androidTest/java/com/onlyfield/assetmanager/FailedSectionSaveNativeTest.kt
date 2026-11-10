package com.onlyfield.assetmanager

import android.graphics.pdf.PdfDocument
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.ui.LocalMessages
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.UiMessage
import com.onlyfield.assetmanager.ui.screens.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FailedSectionSaveNativeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun inventoryPickerRetainsDraft() = verify("inventory-picker")
    @Test fun newDeviceEditorRetainsDraft() = verify("new-device")
    @Test fun deviceEditorRetainsDraft() = verify("device")
    @Test fun batchEditorRetainsSelection() = verify("batch")
    @Test fun rackPickerRetainsDraft() = verify("rack-picker")
    @Test fun newRackEditorRetainsDraft() = verify("new-rack")
    @Test fun rackEditorRetainsDraft() = verify("rack")
    @Test fun rackUnitPickerRetainsPosition() = verify("unit")
    @Test fun placementEditorRetainsSelection() = verify("place")
    @Test fun newSiteEditorRetainsDraft() = verify("new-site")
    @Test fun siteEditorRetainsDraft() = verify("site")
    @Test fun newAreaEditorRetainsDraft() = verify("new-area")
    @Test fun areaEditorRetainsDraft() = verify("area")
    @Test fun newModelEditorRetainsDraft() = verify("new-model")
    @Test fun modelEditorRetainsDraft() = verify("model")
    @Test fun modelApplicationRetainsSelection() = verify("apply-model")
    @Test fun attachmentPageRetainsSelection() = verify("pdf")

    private fun verify(editor: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "section-save-${UUID.randomUUID()}").apply { check(mkdir()) }
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = ProjectRepository(db, root, "isolated-section-recovery")
        val owner = ViewModelStore()
        val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val messages = mutableListOf<UiMessage>()
        lateinit var vm: ProjectViewModel
        val area = Area(name = "Existing floor")
        val device = Device(technicalName = "Existing device", areaId = area.id, mountingType = MountingType.OUT_OF_RACK)
        val site = Site(name = "Existing site", areas = listOf(area), devices = listOf(device))
        val rack = Rack(name = "Existing rack", heightU = 12, areaId = area.id)
        val model = DeviceModel(name = "Existing model", category = DeviceCategory.NETWORK_SWITCH, defaultHeightU = 2)
        val attachment = Attachment(name = "Test plan", originalFileName = "pages.pdf", relativePath = "",
            fileType = AttachmentType.PDF, mimeType = "application/pdf", pageCount = 2)
        val initial = Project(name = "Isolated section test", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(site), racks = listOf(rack), deviceModels = listOf(model),
            attachments = if (editor == "pdf") listOf(attachment) else emptyList())
        try {
            if (editor == "pdf") {
                val file = AttachmentFiles.localFile(root, initial.id, attachment)
                check(file.parentFile!!.mkdirs())
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
            runBlocking { repository.saveProject(initial) }
            rule.runOnIdle {
                vm = ProjectViewModel(repository)
                owner.put("section-save", vm)
                collector.launch { vm.messages.collect { messages += it } }
                vm.openProject(initial.id)
            }
            rule.waitUntil(10_000) { vm.project.value != null && vm.busy == null }
            rule.setContent {
                val project by vm.project.collectAsState()
                CompositionLocalProvider(LocalMessages provides vm.i18n) {
                    MaterialTheme { project?.let { p ->
                        val snackbar = remember { SnackbarHostState() }
                        when (editor) {
                            "inventory-picker", "new-device", "batch" -> InventoryScreen(vm, p, snackbar)
                            "device" -> DeviceDetailScreen(vm, p, device.id, snackbar)
                            "rack-picker", "new-rack" -> RacksScreen(vm, p, snackbar)
                            "rack", "unit", "place" -> RackDetailScreen(vm, p, rack.id, snackbar)
                            "new-site", "site", "new-area", "area" -> StructureScreen(vm, p, snackbar)
                            "new-model", "model", "apply-model" -> ModelsScreen(vm, p, snackbar)
                            else -> AttachmentsScreen(vm, p, snackbar)
                        }
                    } }
                }
            }
            fun click(key: String) = rule.onAllNodesWithText(vm.i18n.text(key), useUnmergedTree = true).onLast().performClick()
            fun rename(value: String) = rule.onNode(hasSetTextAction() and hasText(value)).performScrollTo().performTextReplacement("Retained draft")
            val action: String
            when (editor) {
                "inventory-picker", "new-device", "rack-picker", "new-rack", "unit" -> {
                    if (editor == "unit") rule.onNodeWithContentDescription(vm.i18n.text("rack.freeUnit", 12))
                        .performSemanticsAction(SemanticsActions.OnClick) { it() }
                    else click(if (editor == "rack-picker" || editor == "new-rack") "text.4cd265c2b8c6" else "text.cf301d95d32c")
                    if (editor != "rack-picker" && editor != "new-rack") {
                        rule.onNodeWithText(vm.i18n.text("text.271a55491c4f")).performTextInput("switch")
                        rule.onNodeWithText("Switch").performClick()
                    }
                    rule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("Retained draft")
                    if (editor == "new-device" || editor == "new-rack") click("quick.addAndEdit")
                    action = vm.i18n.text("ux.add")
                }
                "device", "rack" -> {
                    click("text.49e493ba9d9c")
                    rename(if (editor == "device") device.technicalName else rack.name)
                    action = vm.i18n.text("ux.saveChanges")
                }
                "batch" -> {
                    click("text.5b62ee34518b")
                    rule.onNodeWithText(device.technicalName).performClick()
                    rule.onNodeWithText(vm.i18n.text("text.19c9647425aa", 1)).performClick()
                    rule.onNodeWithText(vm.i18n.text("text.d0210741d000")).performScrollTo().performClick()
                    rule.onNode(hasSetTextAction() and hasText(vm.i18n.text("text.d8da2c49df39")))
                        .performScrollTo().performTextInput("Retained draft")
                    action = vm.i18n.text("text.7a1ff7ffd286")
                }
                "place" -> {
                    click("text.d6616679ad89")
                    click("text.e7f2c0e68768")
                    rule.onNodeWithText(device.technicalName).performClick()
                    action = vm.i18n.text("text.ee84d8c1e721")
                }
                "new-site", "site", "new-area", "area" -> {
                    when (editor) {
                        "new-site" -> click("text.e4de7d26b141")
                        "site" -> {
                            rule.onAllNodesWithContentDescription(vm.i18n.text("text.93f019bac960")).onFirst().performClick()
                            click("text.98b79b084f23")
                        }
                        "new-area" -> rule.onNodeWithText(site.name).performClick()
                        else -> rule.onNodeWithText(area.name).performClick()
                    }
                    val old = when (editor) { "site" -> site.name; "area" -> area.name; else -> vm.i18n.text("text.2e245546ff59") }
                    rename(old)
                    action = vm.i18n.text("text.c5997e85ae51")
                }
                "new-model", "model", "apply-model" -> {
                    when (editor) {
                        "new-model" -> click("text.90c2d339a9d5")
                        "model" -> rule.onNodeWithText(model.name).performClick()
                        else -> {
                            rule.onNodeWithContentDescription(vm.i18n.text("text.93f019bac960")).performClick()
                            click("text.b3313b6d6747")
                            click("config.device")
                            rule.onNodeWithText(device.technicalName).performClick()
                        }
                    }
                    if (editor != "apply-model") rename(if (editor == "model") model.name else vm.i18n.text("config.name"))
                    action = vm.i18n.text(when (editor) { "new-model" -> "ux.add"; "model" -> "ux.saveChanges"; else -> "ux.apply" })
                }
                else -> {
                    rule.onNodeWithContentDescription(vm.i18n.text("text.93f019bac960")).performClick()
                    click("text.fa7e72cbd571")
                    click("text.ddbccb18e085")
                    rule.onNodeWithText(area.name).performClick()
                    click("text.125d6d4967e5")
                    rule.waitUntil(10_000) { rule.onAllNodesWithText(vm.i18n.text("text.c192249f9066", 2)).fetchSemanticsNodes().isNotEmpty() }
                    rule.onNode(hasScrollAction() and hasAnyAncestor(isDialog()))
                        .performScrollToNode(hasText(vm.i18n.text("text.c192249f9066", 2)))
                    rule.onNodeWithText(vm.i18n.text("text.c192249f9066", 2)).performClick()
                    action = vm.i18n.text("text.f62c5da23e83", 2)
                }
            }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_section_save BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'isolated section save denied'); END")
            closeSoftKeyboard()
            rule.waitForIdle()
            rule.onNodeWithText(action).performClick()
            rule.waitUntil(10_000) { vm.busy == null }
            rule.runOnIdle {
                assertEquals(initial, vm.project.value)
                assertTrue(messages.any { it.isError })
                assertTrue(messages.none { it.undo != null })
            }
            runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
            if (editor !in listOf("place", "apply-model", "pdf"))
                rule.onNode(hasSetTextAction() and hasText("Retained draft")).performScrollTo().assertIsDisplayed()
            rule.onNodeWithText(action).assertIsDisplayed()
            val error = rule.onNodeWithText("isolated section save denied", substring = true)
            if (editor !in listOf("inventory-picker", "rack-picker", "unit", "pdf")) error.performScrollTo()
            error.assertIsDisplayed()
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_section_save")
            rule.onNodeWithText(action).performClick()
            rule.waitUntil(10_000) { vm.busy == null }
            rule.runOnIdle {
                val saved = vm.project.value!!
                when (editor) {
                    "rack-picker", "new-rack", "rack" -> assertEquals(1, saved.racks.count { it.name == "Retained draft" })
                    "place" -> assertEquals(rack.id, saved.sites.single().devices.single().rackId)
                    "batch" -> assertEquals("Retained draft", saved.sites.single().devices.single().observation?.notes)
                    "new-site", "site" -> assertEquals(1, saved.sites.count { it.name == "Retained draft" })
                    "new-area", "area" -> assertEquals(1, saved.sites.single().areas.count { it.name == "Retained draft" })
                    "new-model", "model" -> assertEquals(1, saved.deviceModels.count { it.name == "Retained draft" })
                    "apply-model" -> assertEquals(model.id, saved.sites.single().devices.single().deviceModelId)
                    "pdf" -> {
                        val savedArea = saved.sites.single().areas.single()
                        assertEquals(attachment.id, savedArea.floorplanAttachmentId)
                        assertEquals(1, savedArea.floorplanPageIndex)
                    }
                    else -> {
                        val added = saved.sites.single().devices.single { it.technicalName == "Retained draft" }
                        if (editor == "unit") { assertEquals(rack.id, added.rackId); assertEquals(12, added.positionU) }
                    }
                }
                assertEquals(1, messages.count { it.undo != null })
            }
            runBlocking { assertEquals(vm.project.value, repository.getProjectById(initial.id)) }
            rule.onNode(hasSetTextAction() and hasText("Retained draft")).assertDoesNotExist()
            rule.runOnIdle { messages.single { it.undo != null }.undo!!.invoke() }
            rule.waitUntil(10_000) { vm.busy == null }
            rule.runOnIdle { assertEquals(initial, vm.project.value) }
            runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
        } finally {
            collector.cancel()
            rule.runOnIdle { owner.clear() }
            db.close()
            check(root.canonicalFile.parentFile == context.cacheDir.canonicalFile)
            check(root.deleteRecursively())
        }
    }
}
