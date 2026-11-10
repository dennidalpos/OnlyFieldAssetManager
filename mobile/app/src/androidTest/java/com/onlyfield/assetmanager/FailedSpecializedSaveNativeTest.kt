package com.onlyfield.assetmanager

import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.cartography.MapSnapshotResult
import com.onlyfield.assetmanager.cartography.OfflineMapException
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.ui.LocalMessages
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.Screen
import com.onlyfield.assetmanager.ui.UiMessage
import com.onlyfield.assetmanager.ui.screens.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class FailedSpecializedSaveNativeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun replacementRetainsDraftAndNavigation() = verify("replace")
    @Test fun mergeRetainsSelectionAndChoices() = verify("merge")
    @Test fun newCredentialRetainsDraft() = verify("new-credential")
    @Test fun credentialRetainsDraft() = verify("credential")
    @Test fun attachmentImportRetainsSource() = verify("import")
    @Test fun classificationRetainsChoice() = verify("classification")
    @Test fun downloadRetainsCoordinatesAndName() = verify("download")
    @Test fun offlineDownloadRetainsDraft() = verify("offline")

    private fun verify(editor: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "special-save-${UUID.randomUUID()}").apply { check(mkdir()) }
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = ProjectRepository(db, root, "isolated-special-recovery")
        val owner = ViewModelStore()
        val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val messages = mutableListOf<UiMessage>()
        val offline = AtomicBoolean(editor == "offline")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val mapBytes = ByteArrayOutputStream().use { output ->
            try { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); output.toByteArray() }
            finally { bitmap.recycle() }
        }
        lateinit var vm: ProjectViewModel
        val device = Device(technicalName = "Existing device", mountingType = MountingType.OUT_OF_RACK)
        val duplicate = Device(technicalName = "Duplicate device", ipAddress = "192.0.2.1", mountingType = MountingType.OUT_OF_RACK)
        val credential = Credential(username = "Synthetic user", secret = "Dummy placeholder", deviceId = device.id, groupName = "Test group")
        val attachment = Attachment(name = "Test document", originalFileName = "document.txt", relativePath = "",
            fileType = AttachmentType.DOCUMENT, mimeType = "text/plain")
        val initial = Project(name = "Isolated specialized test", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Test site", devices = listOf(device, duplicate))),
            credentials = listOf(credential), attachments = listOf(attachment))
        val source = File(root, "source.txt").apply { writeText("Synthetic import") }
        // The picker supplies a fixture; repository and media persistence are real.
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                    dispatchResult(requestCode, Uri.fromFile(source))
                }
            }
        }
        val originalMedia = AttachmentFiles.localFile(root, initial.id, attachment)
        check(originalMedia.parentFile!!.mkdirs())
        originalMedia.writeText("Existing synthetic document")
        val originalBytes = originalMedia.readBytes()
        try {
            runBlocking { repository.saveProject(initial) }
            rule.runOnIdle {
                vm = ProjectViewModel(repository) { request, _ ->
                    assertEquals(41.9, request.centerLatitude, 0.0)
                    assertEquals(12.5, request.centerLongitude, 0.0)
                    assertEquals(17, request.zoomLevel)
                    if (offline.get()) throw OfflineMapException("isolated map source denied")
                    MapSnapshotResult(mapBytes, widthPx = 8, heightPx = 8, attributionText = "Synthetic map attribution", sourceName = "Test source")
                }
                owner.put("special-save", vm)
                collector.launch { vm.messages.collect { messages += it } }
                vm.openProject(initial.id)
            }
            rule.waitUntil(10_000) { vm.project.value != null && vm.busy == null }
            rule.runOnIdle {
                vm.navigate(when (editor) {
                    "replace", "merge" -> Screen.DeviceDetail(device.id)
                    "new-credential", "credential" -> Screen.Credentials
                    else -> Screen.Attachments
                })
            }
            val screen = vm.currentScreen
            rule.setContent {
                val project by vm.project.collectAsState()
                CompositionLocalProvider(LocalMessages provides vm.i18n, LocalActivityResultRegistryOwner provides registryOwner) {
                    MaterialTheme { project?.let { p ->
                        val snackbar = remember { SnackbarHostState() }
                        when (editor) {
                            "replace", "merge" -> DeviceDetailScreen(vm, p, device.id, snackbar)
                            "new-credential", "credential" -> CredentialsScreen(vm, p, snackbar)
                            else -> AttachmentsScreen(vm, p, snackbar)
                        }
                    } }
                }
            }
            fun click(key: String) = rule.onAllNodesWithText(vm.i18n.text(key), useUnmergedTree = true).onLast().performClick()
            fun field(key: String, value: String) = rule.onNode(hasSetTextAction() and hasText(vm.i18n.text(key)))
                .performScrollTo().performTextReplacement(value)
            val action: String
            when (editor) {
                "replace", "merge" -> {
                    rule.onNodeWithContentDescription(vm.i18n.text("text.93f019bac960")).performClick()
                    click(if (editor == "replace") "text.475df7909caf" else "text.99f9d71d6d8e")
                    if (editor == "replace") field("text.9d9ce7aec419", "Retained draft")
                    else {
                        click("text.505c57b1f96f")
                        rule.onNodeWithText(duplicate.technicalName).performClick()
                        rule.onNodeWithText(vm.i18n.text("text.ddd991c1f573", duplicate.technicalName)).performScrollTo().performClick()
                    }
                    action = vm.i18n.text(if (editor == "replace") "text.3260dc474cbc" else "text.c442a0f989e0")
                }
                "new-credential", "credential" -> {
                    if (editor == "new-credential") click("text.602206d4ebfc")
                    else rule.onNodeWithText(credential.username).performClick()
                    field("text.3255e3d5e3b4", "Retained draft")
                    if (editor == "new-credential") field("text.7f9bedb6b654", "Dummy retry placeholder")
                    field("text.b9bb40edbe6e", "Retained group")
                    action = vm.i18n.text("text.c5997e85ae51")
                }
                "import" -> {
                    click("text.59cc6c3e1526")
                    field("text.5086900635fe", "Retained draft")
                    action = vm.i18n.text("text.84cbef7b19b8")
                }
                "download", "offline" -> {
                    click("text.ba321e5c3cea")
                    field("text.259bd9884099", "41.9")
                    field("text.8f00dfa444aa", "12.5")
                    field("text.5086900635fe", "Retained draft")
                    action = vm.i18n.text("text.723d32f77a1d")
                }
                else -> {
                    rule.onNodeWithContentDescription(vm.i18n.text("text.93f019bac960")).performClick()
                    click("text.2434a2bbb0cf")
                    rule.onNode(hasClickAction() and hasText(vm.i18n.text("text.57fbd1029ff6"))).performClick()
                    rule.onNodeWithText(AttachmentClassification.CONFIDENTIAL.toDisplayString(vm.i18n)).performClick()
                    action = vm.i18n.text("text.c5997e85ae51")
                }
            }
            if (editor != "offline") db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_special_save BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'isolated specialized save denied'); END")
            closeSoftKeyboard()
            rule.waitForIdle()
            rule.onNodeWithText(action).performClick()
            rule.waitUntil(10_000) { vm.busy == null }
            rule.runOnIdle {
                assertEquals(initial, vm.project.value)
                assertEquals(screen, vm.currentScreen)
                assertTrue(vm.trash.value.isEmpty())
                assertTrue(messages.any { it.isError })
                assertTrue(messages.none { it.undo != null })
            }
            runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
            assertArrayEquals(originalBytes, originalMedia.readBytes())
            assertEquals(1, AttachmentFiles.ownedFiles(root, initial.id).size)
            if (editor !in listOf("merge", "classification"))
                rule.onNode(hasSetTextAction() and hasText("Retained draft")).performScrollTo().assertIsDisplayed()
            if (editor == "merge") rule.onNodeWithText(vm.i18n.text("text.ddd991c1f573", duplicate.technicalName)).performScrollTo().assertIsOn()
            if (editor == "classification") rule.onNode(hasClickAction() and hasText(AttachmentClassification.CONFIDENTIAL.toDisplayString(vm.i18n)))
                .performScrollTo().assertIsDisplayed()
            rule.onNodeWithText(action).assertIsDisplayed()
            rule.onNodeWithText(if (editor == "offline") "isolated map source denied" else "isolated specialized save denied", substring = true).performScrollTo().assertIsDisplayed()
            if (editor == "offline") offline.set(false) else db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_special_save")
            rule.onNodeWithText(action).performClick()
            rule.waitUntil(10_000) { vm.busy == null }
            rule.runOnIdle {
                val saved = vm.project.value!!
                when (editor) {
                    "replace" -> { assertEquals(1, saved.sites.single().devices.count { it.technicalName == "Retained draft" }); assertEquals(1, vm.trash.value.size)
                        assertNotEquals(screen, vm.currentScreen) }
                    "merge" -> { assertEquals(device.id, saved.sites.single().devices.single().id)
                        assertEquals(duplicate.technicalName, saved.sites.single().devices.single().technicalName); assertEquals(1, vm.trash.value.size) }
                    "new-credential", "credential" -> {
                        val added = saved.credentials.single { it.username == "Retained draft" }
                        assertEquals("Retained group", added.groupName)
                        assertEquals(if (editor == "credential") credential.secret else "Dummy retry placeholder", added.secret)
                        assertEquals(if (editor == "credential") 1 else 2, saved.credentials.size)
                        if (editor == "credential") { assertEquals(credential.id, added.id); assertEquals(device.id, added.deviceId) }
                    }
                    "import" -> { assertEquals(2, saved.attachments.size)
                        val added = saved.attachments.single { it.name == "Retained draft" }
                        assertArrayEquals(source.readBytes(), AttachmentFiles.localFile(root, initial.id, added).readBytes()) }
                    "download", "offline" -> { assertEquals(2, saved.attachments.size)
                        val added = saved.attachments.single { it.name == "Retained draft" }
                        assertEquals("Synthetic map attribution", added.attributionText)
                        assertArrayEquals(mapBytes, AttachmentFiles.localFile(root, initial.id, added).readBytes()) }
                    else -> assertEquals(AttachmentClassification.CONFIDENTIAL, saved.attachments.single().classification)
                }
                assertEquals(if (editor in listOf("replace", "merge")) 0 else 1, messages.count { it.undo != null })
            }
            runBlocking { assertEquals(vm.project.value, repository.getProjectById(initial.id)) }
            rule.onNode(hasSetTextAction() and hasText("Retained draft")).assertDoesNotExist()
            assertArrayEquals(originalBytes, originalMedia.readBytes())
            if (editor !in listOf("replace", "merge")) {
                rule.runOnIdle { messages.single { it.undo != null }.undo!!.invoke() }
                rule.waitUntil(10_000) { vm.busy == null }
                rule.runOnIdle { assertEquals(initial, vm.project.value) }
                runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
                assertEquals(1, AttachmentFiles.ownedFiles(root, initial.id).size)
            }
        } finally {
            collector.cancel()
            rule.runOnIdle { owner.clear() }
            db.close()
            check(root.canonicalFile.parentFile == context.cacheDir.canonicalFile)
            check(root.deleteRecursively())
        }
    }
}
