package com.onlyfield.assetmanager

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.*
import com.onlyfield.assetmanager.ui.*
import com.onlyfield.assetmanager.ui.components.rememberPhotoCapture
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class PhotoSeriesNativeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun syntheticDeviceSeriesPersistsAndExports() = verify(AttachmentTargetType.DEVICE)
    @Test fun syntheticPortSeriesPersistsAndExports() = verify(AttachmentTargetType.PORT)
    @Test fun syntheticCableSeriesPersistsAndExports() = verify(AttachmentTargetType.CABLE)
    @Test fun rejectedSyntheticPhotoStopsThenRetries() = verify(AttachmentTargetType.DEVICE, reject = true)
    @Test fun disposedPhotoHostDoesNotRestartTheSeries() = verify(AttachmentTargetType.DEVICE, dispose = true)

    private fun verify(type: AttachmentTargetType, reject: Boolean = false, dispose: Boolean = false) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val parent = File(context.cacheDir, "object_photos").apply { check(isDirectory || mkdir()) }
        val root = File(parent, "native-series-${UUID.randomUUID()}").apply { check(mkdir()) }
        val executor = Executors.newSingleThreadExecutor()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).setTransactionExecutor(executor).build()
        val repository = ProjectRepository(db, root, "isolated-photo-recovery")
        val owner = ViewModelStore()
        val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val messages = mutableListOf<UiMessage>()
        val requests = AtomicInteger()
        var shown by mutableStateOf(true)
        lateinit var vm: ProjectViewModel
        val device = Device(technicalName = "Synthetic device")
        val port = Port(deviceId = device.id, name = "Synthetic port")
        val cable = Cable(portAId = port.id, codeOrLabel = "Synthetic cable")
        val target = when (type) { AttachmentTargetType.PORT -> port.id; AttachmentTargetType.CABLE -> cable.id; else -> device.id }
        val initial = Project(name = "Synthetic photo host", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Synthetic site", devices = listOf(device.copy(ports = listOf(port))))), cables = listOf(cable))
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { output ->
            try { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)); output.toByteArray() }
            finally { bitmap.recycle() }
        }
        // Camera and permission results are synthetic; launcher, provider and persistence are real.
        val registry = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                    if (contract is ActivityResultContracts.RequestPermission) { dispatchResult(requestCode, true); return }
                    check(contract is ActivityResultContracts.TakePicture)
                    requests.incrementAndGet()
                    val saved = vm.project.value!!.attachments.size < 2
                    if (saved) context.contentResolver.openOutputStream(input as Uri)!!.use { it.write(bytes) }
                    dispatchResult(requestCode, saved)
                }
            }
        }
        var release: CountDownLatch? = null
        try {
            runBlocking { repository.saveProject(initial) }
            rule.runOnIdle {
                vm = ProjectViewModel(repository)
                owner.put("photos", vm)
                collector.launch { vm.messages.collect { messages += it } }
                vm.openProject(initial.id)
            }
            rule.waitUntil(10_000) { vm.project.value != null && vm.busy == null }
            rule.setContent {
                CompositionLocalProvider(LocalMessages provides vm.i18n, LocalActivityResultRegistryOwner provides registry) {
                    MaterialTheme { if (shown) {
                        val capture = rememberPhotoCapture(vm)
                        Button(onClick = { capture(type, target) }) { Text("Synthetic photo") }
                    } }
                }
            }
            if (reject) db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_photo BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'isolated photo denied'); END")
            if (dispose) {
                val reached = CountDownLatch(1)
                release = CountDownLatch(1)
                val gate = release
                executor.execute { reached.countDown(); check(gate.await(10, TimeUnit.SECONDS)) }
                assertTrue(reached.await(5, TimeUnit.SECONDS))
            }
            rule.onNodeWithText("Synthetic photo").performClick()
            if (dispose) {
                rule.waitUntil(10_000) { vm.busy != null }
                rule.runOnIdle { shown = false }
                rule.waitForIdle()
                release!!.countDown()
                rule.waitUntil(10_000) { vm.busy == null }
                rule.runOnIdle { assertEquals(1, requests.get()); assertEquals(1, vm.project.value!!.attachments.size) }
                return
            }
            if (reject) {
                rule.waitUntil(10_000) { vm.busy == null && messages.any { it.isError } }
                rule.runOnIdle { assertEquals(1, requests.get()); assertEquals(initial, vm.project.value) }
                assertTrue(AttachmentFiles.ownedFiles(root, initial.id).isEmpty())
                db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_photo")
                rule.onNodeWithText("Synthetic photo").performClick()
            }
            rule.waitUntil(10_000) { vm.busy == null && requests.get() == if (reject) 4 else 3 }
            rule.runOnIdle {
                assertEquals(2, vm.project.value!!.attachments.size)
                assertEquals(2, messages.count { it.undo != null })
                vm.project.value!!.attachments.forEach { assertEquals(type, it.targetType); assertEquals(target, it.targetId) }
            }
            runBlocking {
                val saved = repository.getProjectById(initial.id)!!
                assertEquals(vm.project.value, saved)
                saved.attachments.forEach { assertArrayEquals(bytes, AttachmentFiles.localFile(root, initial.id, it).readBytes()) }
                val encrypted = requireNotNull(repository.exportProjectPackage(initial.id, "dummy-photo-password"))
                val imported = requireNotNull(PackageSerializer.importPackage(encrypted, "dummy-photo-password").pkg)
                imported.use { pkg ->
                    assertEquals(saved.attachments, pkg.project.attachments)
                    saved.attachments.forEach { assertArrayEquals(bytes, AttachmentFiles.bytesIn(pkg, it)) }
                }
            }
            rule.runOnIdle { messages.last { it.undo != null }.undo!!.invoke() }
            rule.waitUntil(10_000) { vm.busy == null && vm.project.value!!.attachments.size == 1 }
            assertEquals(1, AttachmentFiles.ownedFiles(root, initial.id).size)
        } finally {
            release?.countDown()
            collector.cancel()
            rule.runOnIdle { owner.clear() }
            db.close()
            executor.shutdownNow()
            check(root.canonicalFile.parentFile == parent.canonicalFile)
            check(root.deleteRecursively())
        }
    }
}
