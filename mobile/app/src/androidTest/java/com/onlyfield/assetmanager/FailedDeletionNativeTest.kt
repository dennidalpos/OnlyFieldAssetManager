package com.onlyfield.assetmanager

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FailedDeletionNativeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun deviceDeletionRetainsScreenUntilCommit() = verify("DEVICE")
    @Test fun rackDeletionRetainsScreenUntilCommit() = verify("RACK")

    private fun verify(kind: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "delete-save-${UUID.randomUUID()}").apply { check(mkdir()) }
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = ProjectRepository(db, root, "isolated-deletion-recovery")
        val owner = ViewModelStore()
        val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val messages = mutableListOf<UiMessage>()
        lateinit var vm: ProjectViewModel
        val rack = Rack(name = "Synthetic rack", heightU = 12)
        val device = Device(technicalName = "Synthetic device", rackId = rack.id, positionU = 1, mountingType = MountingType.RACK_MOUNT)
        val targetId = if (kind == "DEVICE") device.id else rack.id
        val attachment = Attachment(name = "Synthetic document", originalFileName = "document.txt", relativePath = "",
            fileType = AttachmentType.DOCUMENT, mimeType = "text/plain",
            targetType = if (kind == "DEVICE") AttachmentTargetType.DEVICE else AttachmentTargetType.RACK, targetId = targetId)
        val initial = ObjectHierarchy.synchronize(Project(name = "Isolated deletion", createdEpochMs = 1, updatedEpochMs = 1,
            racks = listOf(rack), sites = listOf(Site(name = "Synthetic site", devices = listOf(device))),
            attachments = listOf(attachment)))
        val media = AttachmentFiles.localFile(root, initial.id, attachment)
        check(media.parentFile!!.mkdirs())
        media.writeText("Synthetic retained media")
        val bytes = media.readBytes()
        val detail = if (kind == "DEVICE") Screen.DeviceDetail(device.id) else Screen.RackDetail(rack.id)
        val parent = if (kind == "DEVICE") Screen.Inventory else Screen.Racks
        try {
            runBlocking { repository.saveProject(initial) }
            rule.runOnIdle {
                vm = ProjectViewModel(repository)
                owner.put("deletion", vm)
                collector.launch { vm.messages.collect { messages += it } }
                vm.openProject(initial.id)
            }
            rule.waitUntil(10_000) { vm.project.value != null && vm.busy == null }
            rule.runOnIdle { vm.navigate(parent); vm.navigate(detail) }
            val stack = vm.backStack.toList()
            val selectedSite = vm.selectedSiteId
            val selectedArea = vm.selectedAreaId
            rule.setContent {
                CompositionLocalProvider(LocalMessages provides vm.i18n) { AppRoot(vm) { error("Unexpected exit") } }
            }
            fun click(key: String) = rule.onAllNodesWithText(vm.i18n.text(key), useUnmergedTree = true).onLast().performClick()
            fun openDelete() {
                rule.onNodeWithContentDescription(vm.i18n.text("text.dd41b3275173")).performClick()
            }
            openDelete()
            click("text.18c9d912a210")
            rule.runOnIdle { assertEquals(stack, vm.backStack.toList()); assertEquals(initial, vm.project.value) }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_delete BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'isolated deletion denied'); END")
            openDelete()
            click("text.dd41b3275173")
            rule.waitUntil(10_000) { vm.busy == null && messages.any { it.isError } }
            rule.runOnIdle {
                assertEquals(stack, vm.backStack.toList())
                assertEquals(selectedSite, vm.selectedSiteId)
                assertEquals(selectedArea, vm.selectedAreaId)
                assertEquals(initial, vm.project.value)
                assertTrue(vm.trash.value.isEmpty())
                assertTrue(messages.none { it.undo != null })
            }
            rule.onNodeWithText("isolated deletion denied", substring = true).assertIsDisplayed()
            runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
            assertArrayEquals(bytes, media.readBytes())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_delete")
            openDelete()
            click("text.dd41b3275173")
            rule.waitUntil(10_000) { vm.busy == null && vm.currentScreen == parent }
            rule.runOnIdle {
                assertEquals(stack.dropLast(1), vm.backStack.toList())
                assertEquals(1, vm.trash.value.size)
                assertEquals(1, messages.count { it.undo != null })
                val saved = vm.project.value!!
                if (kind == "DEVICE") assertTrue(saved.sites.single().devices.isEmpty())
                else { assertTrue(saved.racks.isEmpty()); assertNull(saved.sites.single().devices.single().rackId) }
            }
            runBlocking { assertEquals(vm.project.value, repository.getProjectById(initial.id)) }
            assertArrayEquals(bytes, media.readBytes())
            rule.waitUntil(15_000) {
                rule.onAllNodesWithText(vm.i18n.text("action.undo")).fetchSemanticsNodes().isNotEmpty()
            }
            click("action.undo")
            rule.waitUntil(10_000) { vm.busy == null && vm.trash.value.isEmpty() }
            rule.runOnIdle {
                assertEquals(stack.dropLast(1), vm.backStack.toList())
                assertEquals(initial.racks, vm.project.value!!.racks)
                assertEquals(initial.sites, vm.project.value!!.sites)
                assertEquals(initial.attachments, vm.project.value!!.attachments)
            }
            runBlocking { assertEquals(vm.project.value, repository.getProjectById(initial.id)) }
            assertArrayEquals(bytes, media.readBytes())
            assertEquals(1, AttachmentFiles.ownedFiles(root, initial.id).size)
        } finally {
            collector.cancel()
            rule.runOnIdle { owner.clear() }
            db.close()
            check(root.canonicalFile.parentFile == context.cacheDir.canonicalFile)
            check(root.deleteRecursively())
        }
    }
}
