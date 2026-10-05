package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.AttachmentType
import com.onlyfield.assetmanager.core.model.Project
import java.io.File
import java.lang.management.ManagementFactory
import java.security.MessageDigest
import java.util.Random
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Opt-in measurements; generated payloads never enter application storage. */
object LargeImportBenchmark {
    private const val MIB = 1024 * 1024

    @JvmStatic fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: generate DIRECTORY | measure FILE" }
        when (args[0]) {
            "generate" -> generate(File(args[1]))
            "measure" -> measure(File(args[1]))
            else -> error("Unknown benchmark operation")
        }
    }

    private fun generate(directory: File) {
        check(directory.mkdirs() || directory.isDirectory)
        fixture(directory, "representative-32", listOf(32 * MIB), random = true)
        fixture(directory, "archive-255", List(7) { 32 * MIB } + 31 * MIB, random = true)
        fixture(directory, "expanded-511", List(15) { 32 * MIB } + 31 * MIB, random = false)
        fixture(directory, "entries-10000", List(9998) { 16 }, random = false)
        val attachment = Attachment(name = "Encrypted payload", originalFileName = "sample.bin",
            fileType = AttachmentType.OTHER, mimeType = "application/octet-stream", relativePath = "sample.bin")
        val project = Project(name = "Encrypted benchmark", createdEpochMs = 0, updatedEpochMs = 0, attachments = listOf(attachment))
        File(directory, "encrypted-32.ofam").writeBytes(PackageSerializer.exportPackage(project,
            mapOf(AttachmentFiles.entryName(attachment) to ByteArray(32 * MIB).also { Random(42).nextBytes(it) }),
            password = "benchmark-placeholder"))
        for ((source, target) in listOf("archive-255" to "encrypted-255")) {
            File(directory, "$source.ofam").inputStream().use { input ->
                checkNotNull(PackageSerializer.importPackage(input).pkg).use { pkg ->
                    File(directory, "$target.ofam").outputStream().buffered().use { output ->
                        PackageSerializer.exportPackageToStream(output, pkg.project, pkg.attachments, "benchmark-placeholder")
                    }
                }
            }
        }
        kdfFixture(File(directory, "encrypted-kdf-1000000.ofam"))
        directory.listFiles()!!.filter { it.extension == "ofam" }.forEach { println("FIXTURE ${it.name} archiveBytes=${it.length()}") }
    }

    private fun kdfFixture(file: File) {
        val project = Project(name = "KDF benchmark", createdEpochMs = 0, updatedEpochMs = 0)
        val salt = ByteArray(16) { it.toByte() }
        val iv = ByteArray(12) { (it + 16).toByte() }
        val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(
            PBEKeySpec("benchmark-placeholder".toCharArray(), salt, 1_000_000, 256)).encoded
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(secret, "AES"), GCMParameterSpec(128, iv)) }
        val payload = cipher.doFinal(PackageSerializer.jsonConfig.encodeToString(Project.serializer(), project).toByteArray())
        val manifest = PackageManifest(exportId = UUID.randomUUID().toString(), exportedEpochMs = 0,
            projectId = project.id, projectName = project.name, isEncrypted = true, kdfIterations = 1_000_000,
            kdfSaltHex = salt.joinToString("") { "%02x".format(it) }, cipherIvHex = iv.joinToString("") { "%02x".format(it) },
            checksums = mapOf("project.json.enc" to PackageSerializer.calculateSha256(payload)))
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json")); zip.write(PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest).toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("project.json.enc")); zip.write(payload); zip.closeEntry()
        }
    }

    private fun fixture(directory: File, name: String, sizes: List<Int>, random: Boolean) {
        val attachments = sizes.indices.map { index -> Attachment(id = UUID.nameUUIDFromBytes("$name-$index".toByteArray()).toString(), name = "Payload $index",
            originalFileName = "sample.bin", fileType = AttachmentType.OTHER, mimeType = "application/octet-stream",
            relativePath = "payload-$index/sample.bin", createdAtEpochMs = 0) }
        val project = Project(id = UUID.nameUUIDFromBytes(name.toByteArray()).toString(), name = name,
            createdEpochMs = 0, updatedEpochMs = 0, attachments = attachments)
        val projectBytes = PackageSerializer.jsonConfig.encodeToString(Project.serializer(), project).toByteArray()
        val checksums = linkedMapOf("project.json" to PackageSerializer.calculateSha256(projectBytes))
        val buffer = ByteArray(64 * 1024)
        ZipOutputStream(File(directory, "$name.ofam").outputStream().buffered()).use { zip ->
            attachments.forEachIndexed { index, attachment ->
                val path = AttachmentFiles.entryName(attachment)
                zip.putNextEntry(ZipEntry(path))
                val digest = MessageDigest.getInstance("SHA-256")
                val generator = Random(42L + index)
                var remaining = sizes[index]
                while (remaining > 0) {
                    if (random) generator.nextBytes(buffer)
                    val length = minOf(buffer.size, remaining)
                    digest.update(buffer, 0, length)
                    zip.write(buffer, 0, length)
                    remaining -= length
                }
                zip.closeEntry()
                checksums[path] = digest.digest().joinToString("") { "%02x".format(it) }
            }
            zip.putNextEntry(ZipEntry("project.json")); zip.write(projectBytes); zip.closeEntry()
            val manifest = PackageManifest(exportId = name, exportedEpochMs = 0,
                projectId = project.id, projectName = project.name, checksums = checksums)
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(PackageSerializer.jsonConfig.encodeToString(PackageManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()
        }
    }

    private fun measure(file: File) {
        val runtime = Runtime.getRuntime()
        System.gc()
        val baseline = runtime.totalMemory() - runtime.freeMemory()
        val peak = AtomicLong(baseline)
        val running = AtomicBoolean(true)
        val pools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == java.lang.management.MemoryType.HEAP }
        pools.forEach { it.resetPeakUsage() }
        val sampler = thread(name = "import-memory-sampler", isDaemon = true) {
            while (running.get()) {
                peak.accumulateAndGet(runtime.totalMemory() - runtime.freeMemory(), ::maxOf)
                Thread.sleep(10)
            }
        }
        val start = System.nanoTime()
        try {
            val result = file.inputStream().buffered().use { PackageSerializer.importPackage(it,
                password = if (file.name.startsWith("encrypted-")) "benchmark-placeholder" else null) }
            val elapsed = (System.nanoTime() - start) / 1_000_000
            checkNotNull(result.pkg) { result.validationResult.issues.toString() }.use { pkg ->
                check(result.validationResult.issues.isEmpty()) { result.validationResult.issues.toString() }
                check(pkg.attachments.size == pkg.project.attachments.size)
                println("BENCHMARK file=${file.name} archiveBytes=${file.length()} payloadBytes=${pkg.attachments.keys.sumOf(pkg::payloadSize)} entries=${pkg.attachments.size + 2} elapsedMs=$elapsed baselineHeapBytes=$baseline peakHeapBytes=${peak.get()} poolPeakBytes=${pools.sumOf { it.peakUsage.used }} maxHeapBytes=${runtime.maxMemory()}")
            }
        } finally {
            running.set(false)
            sampler.join()
        }
    }
}
