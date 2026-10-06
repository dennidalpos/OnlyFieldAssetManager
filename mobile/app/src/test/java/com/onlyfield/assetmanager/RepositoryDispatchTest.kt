package com.onlyfield.assetmanager

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.repository.DocumentExports
import com.onlyfield.assetmanager.data.repository.PackageExchange
import com.onlyfield.assetmanager.exchange.PackageSerializer
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.Executors

class RepositoryDispatchTest {
    private val project = Project(name = "Dispatch", createdEpochMs = 0, updatedEpochMs = 0)
    private fun onWorker() = assertEquals("ofam-test-io", Thread.currentThread().name.substringBefore(" @coroutine#"))
    private fun output() = object : ByteArrayOutputStream() {
        override fun write(b: ByteArray, off: Int, len: Int) { onWorker(); super.write(b, off, len) }
    }

    @Test fun importComparisonLoadsOnlyTheIncomingProjectAndLeavesCopiesUnchanged() = runBlocking {
        val other = project.copy(id = java.util.UUID.randomUUID().toString(), name = "Other project")
        val newer = project.copy(name = "Newer local copy", updatedEpochMs = 200)
        for (copies in listOf(emptyMap(), mapOf(other.id to other), mapOf(project.id to newer), mapOf(other.id to other, project.id to newer))) {
            val requested = mutableListOf<String>()
            val exchange = PackageExchange(null, { requested += it; copies[it] },
                { fail("Review must not save") }, { fail("Review must not save a base") }, { _, _ -> fail("Review must not import") })
            val evaluation = exchange.evaluateImportPackage(ByteArrayInputStream(PackageSerializer.exportPackage(project)))
            evaluation.importResult.pkg!!.use {
                assertEquals(listOf(project.id), requested)
                assertEquals(copies[project.id]?.id, evaluation.comparison?.currentProjectId)
                if (project.id in copies) {
                    assertEquals(com.onlyfield.assetmanager.exchange.ComparisonStatus.OLDER_REVISION, evaluation.comparison?.status)
                    assertNotNull(evaluation.comparison?.warningMessage)
                }
            }
        }
    }

    @Test fun exchangeReadsWritesAndPersistsOnTheInjectedWorker() = runBlocking {
        Executors.newSingleThreadExecutor { Thread(it, "ofam-test-io") }.asCoroutineDispatcher().use { dispatcher ->
            val exchange = PackageExchange(null, { onWorker(); project }, { onWorker() }, { onWorker() }, { _, _ -> onWorker() }, dispatcher)
            val out = output()
            assertTrue(exchange.exportProjectPackageToStream(project.id, out))
            val input = object : ByteArrayInputStream(out.toByteArray()) {
                override fun read(b: ByteArray, off: Int, len: Int): Int { onWorker(); return super.read(b, off, len) }
            }
            val result = exchange.evaluateImportPackage(input)
            assertNotNull(result.importResult.pkg)
            assertTrue(exchange.importProjectPackage(result.importResult.pkg!!, null))
            exchange.importMerged(result.importResult.pkg!!, project)
        }
    }

    @Test fun documentGenerationUsesWorkerAndPropagatesWriteFailures() = runBlocking {
        Executors.newSingleThreadExecutor { Thread(it, "ofam-test-io") }.asCoroutineDispatcher().use { dispatcher ->
            val documents = DocumentExports({ onWorker(); project }, dispatcher)
            assertTrue(documents.exportXlsxToStream(project.id, ExportFilterConfig(), output()))
            assertTrue(documents.exportMarkdownToStream(project.id, ExportFilterConfig(), output()))
            val failed = object : ByteArrayOutputStream() {
                override fun write(b: ByteArray, off: Int, len: Int) { onWorker(); throw IOException("dummy write failure") }
            }
            val error = runCatching { documents.exportMarkdownToStream(project.id, ExportFilterConfig(), failed) }.exceptionOrNull()
            assertTrue(error is IOException)
        }
    }

    @Test fun cancellationIsPropagatedWithoutImportingOrSaving() = runBlocking {
        Executors.newSingleThreadExecutor { Thread(it, "ofam-test-io") }.asCoroutineDispatcher().use { dispatcher ->
            var saved = false
            val exchange = PackageExchange(null, { throw CancellationException("dummy cancellation") },
                { saved = true }, { saved = true }, { _, _ -> saved = true }, dispatcher)
            val error = runCatching { exchange.evaluateImportPackage(ByteArrayInputStream(PackageSerializer.exportPackage(project))) }.exceptionOrNull()
            assertTrue(error is CancellationException)
            assertFalse(saved)
        }
    }
}
