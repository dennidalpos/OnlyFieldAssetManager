package com.onlyfield.assetmanager

import android.content.Context
import android.content.ContextWrapper
import android.os.Debug
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.ProjectPackage
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.data.local.EncryptedDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class LargeImportBenchmarkTest {
    /** Without a fixture argument, measure the small bundled demo. */
    @Test fun measureImport() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val fixture = InstrumentationRegistry.getArguments().getString("benchmarkFixture")
        val persist = InstrumentationRegistry.getArguments().getString("benchmarkPersist") == "true"
        val root = File(context.cacheDir, "import-benchmark-${UUID.randomUUID()}")
        check(root.mkdirs())
        val staging = File(root, "staging").apply { check(mkdirs()) }
        var imported: ProjectPackage? = null
        val runtime = Runtime.getRuntime()
        System.gc()
        val baseline = runtime.totalMemory() - runtime.freeMemory()
        val peakHeap = AtomicLong(baseline)
        val peakPss = AtomicLong()
        val maxMainGap = AtomicLong()
        val running = AtomicBoolean(true)
        val handler = Handler(Looper.getMainLooper())
        val heartbeat = object : Runnable {
            var previous = SystemClock.elapsedRealtime()
            override fun run() {
                val now = SystemClock.elapsedRealtime()
                maxMainGap.accumulateAndGet(now - previous, ::maxOf)
                previous = now
                if (running.get()) handler.postDelayed(this, 16)
            }
        }
        handler.post(heartbeat)
        val sampler = thread(name = "import-memory-sampler", isDaemon = true) {
            val info = Debug.MemoryInfo()
            while (running.get()) {
                peakHeap.accumulateAndGet(runtime.totalMemory() - runtime.freeMemory(), ::maxOf)
                Debug.getMemoryInfo(info)
                peakPss.accumulateAndGet(info.totalPss.toLong(), ::maxOf)
                Thread.sleep(50)
            }
        }
        val start = SystemClock.elapsedRealtime()
        try {
            val result = withContext(Dispatchers.IO) {
                val stream = if (fixture == null) instrumentation.context.assets.open("onlyfield-demo.ofam")
                    else File(checkNotNull(context.getExternalFilesDir(null)), "import-benchmark/$fixture").inputStream()
                stream.buffered().use { PackageSerializer.importPackage(it,
                    password = if (fixture?.startsWith("encrypted-") == true) "benchmark-placeholder" else null,
                    stagingDirectory = staging) }
            }
            val elapsed = SystemClock.elapsedRealtime() - start
            val pkg = checkNotNull(result.pkg) { result.validationResult.issues.toString() }
            imported = pkg
            check(result.validationResult.issues.isEmpty()) { result.validationResult.issues.toString() }
            check(pkg.attachments.size == pkg.project.attachments.size)
            if (persist) withContext(Dispatchers.IO) {
                val isolated = object : ContextWrapper(context) {
                    override fun getApplicationContext(): Context = this
                    override fun getDatabasePath(name: String) = File(root, "benchmark.db")
                }
                val database = EncryptedDatabase.open(isolated)
                try {
                    val mediaRoot = File(root, "media")
                    val repository = ProjectRepository(database, mediaRoot, recoveryPassword = EncryptedDatabase.recoveryPassword(isolated))
                    check(repository.importProjectPackage(pkg, if (fixture?.startsWith("encrypted-") == true) "benchmark-placeholder" else null))
                    val reopened = checkNotNull(repository.getProjectById(pkg.project.id))
                    check(reopened.attachments.size == pkg.project.attachments.size)
                    for (attachment in reopened.attachments) {
                        val file = AttachmentFiles.localFile(mediaRoot, reopened.id, attachment)
                        val path = checkNotNull(AttachmentFiles.pathIn(pkg, attachment))
                        check(file.length() == pkg.payloadSize(path))
                        val digest = MessageDigest.getInstance("SHA-256")
                        DigestInputStream(file.inputStream(), digest).use { it.copyTo(java.io.OutputStream.nullOutputStream()) }
                        check(digest.digest().joinToString("") { "%02x".format(it) } == pkg.payloadChecksum(path))
                    }
                } finally { database.close() }
            }
            val metrics = "BENCHMARK file=${fixture ?: "onlyfield-demo.ofam"} payloadBytes=${pkg.attachments.keys.sumOf(pkg::payloadSize)} entries=${pkg.attachments.size + 2} elapsedMs=$elapsed totalMs=${SystemClock.elapsedRealtime() - start} persisted=$persist baselineHeapBytes=$baseline peakHeapBytes=${peakHeap.get()} peakPssKiB=${peakPss.get()} maxHeapBytes=${runtime.maxMemory()} maxMainGapMs=${maxMainGap.get()}"
            instrumentation.sendStatus(2, Bundle().apply { putString("stream", "$metrics\n") })
        } finally {
            running.set(false)
            handler.removeCallbacks(heartbeat)
            sampler.join()
            imported?.close()
            check(staging.listFiles()!!.isEmpty()) { "Import staging was not released" }
            check(root.deleteRecursively()) { "Cannot remove isolated benchmark resources" }
        }
    }
}
