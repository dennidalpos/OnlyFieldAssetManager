package com.onlyfield.assetmanager.exchange

import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.DigestOutputStream
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class RecoveryPasswordRequired : IOException("Recovery requires the local password")

/** Encrypted, per-project recovery; failed rollback keeps every backup. */
class FileRecovery private constructor(
    private val root: File,
    private val directory: File,
    private val owner: String,
    private val key: SecretKey,
    private var journal: Journal,
) {
    @Serializable private data class Header(val version: Int = 1, val owner: String, val salt: String? = null, val key: String? = null)
    @Serializable private data class Blob(val file: String, val bytes: Long, val hash: String)
    @Serializable private data class Change(val target: String, val staged: Blob?, val backup: Blob? = null)
    @Serializable private data class Journal(val phase: String = "STAGING", val beforeState: String? = null,
        val expectedState: String? = null, val changes: List<Change> = emptyList())
    private val json get() = PackageSerializer.jsonConfig
    private var committed = false
    private var databaseCommitted = false

    fun replace(target: File, write: (OutputStream) -> Unit) {
        check(journal.phase == "STAGING")
        val name = relativeTarget(target)
        check(journal.changes.none { it.target == name }) { "A target may change only once" }
        val blob = writeBlob("${journal.changes.size}-new.enc", write)
        journal = journal.copy(changes = journal.changes + Change(name, blob))
        persist()
    }

    fun remove(target: File) {
        check(journal.phase == "STAGING")
        val name = relativeTarget(target)
        if (!Files.exists(target.toPath(), NOFOLLOW_LINKS)) return
        check(Files.isRegularFile(target.toPath(), NOFOLLOW_LINKS)) { "Expected a regular file" }
        check(journal.changes.none { it.target == name }) { "A target may change only once" }
        journal = journal.copy(changes = journal.changes + Change(name, null))
        persist()
    }

    /** Room records the outcome inside its transaction, before file application. */
    fun expectState(state: String) {
        check(journal.phase == "STAGING")
        journal = journal.copy(expectedState = state)
        persist()
    }

    fun apply() {
        check(journal.phase == "STAGING")
        val prepared = journal.changes.mapIndexed { index, change ->
            val target = target(change.target)
            if (!Files.exists(target.toPath(), NOFOLLOW_LINKS)) change
            else {
                check(Files.isRegularFile(target.toPath(), NOFOLLOW_LINKS)) { "Expected a regular file" }
                change.copy(backup = writeBlob("$index-old.enc") { out -> target.inputStream().use { it.copyTo(out) } })
            }
        }
        journal = journal.copy(phase = "PREPARED", changes = prepared)
        persist()
        install(forward = true)
    }

    fun commit(): List<Exception> {
        check(journal.phase == "PREPARED")
        journal = journal.copy(phase = "COMMITTED")
        try { persist() } catch (e: Exception) {
            journal = journal.copy(phase = "PREPARED")
            if (!databaseCommitted) throw e
            committed = true
            return listOf(e)
        }
        committed = true
        return cleanup()
    }

    fun close() {
        if (committed || databaseCommitted) return
        if (journal.phase == "PREPARED") {
            install(forward = false)
            journal = journal.copy(phase = "COMMITTED")
            persist()
        }
        // A second I/O failure above leaves the journal and backups for retry.
        val errors = cleanup()
        if (errors.isNotEmpty()) throw errors.first().also { first -> errors.drop(1).forEach(first::addSuppressed) }
    }

    fun databaseCommitted() { databaseCommitted = true }

    private fun install(forward: Boolean) {
        val currentHashes = mutableMapOf<String, String?>()
        journal.changes.forEach { change ->
            val file = target(change.target)
            if (file.isFile) {
                val hash = MessageDigest.getInstance("SHA-256")
                file.inputStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var count = input.read(buffer)
                    while (count >= 0) { hash.update(buffer, 0, count); count = input.read(buffer) }
                }
                val current = hash.digest().joinToString("") { "%02x".format(it) }
                check(current == change.backup?.hash || current == change.staged?.hash) { "File changed while recovery was pending" }
                currentHashes[change.target] = current
            } else {
                check(!Files.exists(file.toPath(), NOFOLLOW_LINKS)) { "Recovery target is not a regular file" }
                currentHashes[change.target] = null
            }
        }
        val entries = journal.changes.withIndex().let { if (forward) it.toList() else it.toList().asReversed() }
        for ((index, change) in entries) {
            val target = target(change.target)
            val blob = if (forward) change.staged else change.backup
            if (currentHashes[change.target] == blob?.hash) continue
            if (blob == null) Files.deleteIfExists(target.toPath())
            else {
                Files.createDirectories(target.parentFile.toPath())
                val temp = targetTemp(target, index)
                try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    temp.outputStream().buffered().use { output -> decrypt(blobFile(blob.file), DigestOutputStream(output, digest)) }
                    check(temp.length() == blob.bytes && digest.digest().joinToString("") { "%02x".format(it) } == blob.hash) {
                        "Invalid recovery payload"
                    }
                    force(temp)
                    move(temp, target)
                } finally { Files.deleteIfExists(temp.toPath()) }
            }
        }
    }

    private fun writeBlob(name: String, write: (OutputStream) -> Unit): Blob {
        val file = blobFile(name)
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        encrypt(file) { encrypted ->
            val measured = object : OutputStream() {
                override fun write(value: Int) = write(byteArrayOf(value.toByte()))
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    count += length
                    require(count <= MAX_BLOB_BYTES) { "Recovery file exceeds package limit" }
                    digest.update(bytes, offset, length)
                    encrypted.write(bytes, offset, length)
                }
            }
            write(measured)
        }
        return Blob(name, count, digest.digest().joinToString("") { "%02x".format(it) })
    }

    private fun encrypt(file: File, write: (OutputStream) -> Unit) {
        safePath(root, file)
        check(!Files.exists(file.toPath(), NOFOLLOW_LINKS) || Files.isRegularFile(file.toPath(), NOFOLLOW_LINKS))
        file.outputStream().buffered().use { raw ->
            ZipOutputStream(raw).use { zip ->
                zip.setLevel(0)
                val buffer = ByteArray(CHUNK_BYTES)
                var used = 0
                var index = 0
                fun flushChunk() {
                    if (used == 0 && index > 0) return
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, key)
                    zip.putNextEntry(ZipEntry("$index.enc"))
                    zip.write(cipher.iv)
                    zip.write(cipher.doFinal(buffer, 0, used))
                    zip.closeEntry()
                    used = 0
                    index++
                }
                val chunks = object : OutputStream() {
                    override fun write(value: Int) = write(byteArrayOf(value.toByte()))
                    override fun write(bytes: ByteArray, offset: Int, length: Int) {
                        var start = offset
                        var remaining = length
                        while (remaining > 0) {
                            val copied = minOf(remaining, buffer.size - used)
                            bytes.copyInto(buffer, used, start, start + copied)
                            used += copied; start += copied; remaining -= copied
                            if (used == buffer.size) flushChunk()
                        }
                    }
                }
                write(chunks)
                flushChunk()
            }
        }
        force(file)
    }

    private fun decrypt(file: File, output: OutputStream, limit: Long = MAX_BLOB_BYTES) {
        require(file.length() <= MAX_BLOB_BYTES + ENCRYPTED_OVERHEAD_BYTES) { "Invalid recovery payload size" }
        file.inputStream().buffered().use { raw -> ZipInputStream(raw).use { zip ->
            var index = 0
            var bytes = 0L
            var entry = zip.nextEntry
            while (entry != null) {
                require(entry.name == "$index.enc" && !entry.isDirectory) { "Invalid recovery chunk" }
                val encrypted = zip.readNBytes(CHUNK_BYTES + 29)
                require(encrypted.size in 28..CHUNK_BYTES + 28) { "Invalid recovery chunk size" }
                bytes += encrypted.size - 28
                require(bytes <= limit)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, encrypted, 0, 12))
                output.write(cipher.doFinal(encrypted, 12, encrypted.size - 12))
                index++
                entry = zip.nextEntry
            }
            require(index > 0) { "Missing recovery chunks" }
        } }
    }

    private fun persist() {
        val bytes = json.encodeToString(Journal.serializer(), journal).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_JOURNAL_BYTES)
        val temp = File(directory, "journal.tmp")
        encrypt(temp) { it.write(bytes) }
        move(temp, File(directory, "journal.enc"))
    }

    private fun relativeTarget(file: File): String {
        val path = file.toPath().toAbsolutePath().normalize()
        val base = root.toPath().toAbsolutePath().normalize()
        require(path.startsWith(base) && path != base) { "Recovery target outside storage" }
        val relative = base.relativize(path).toString().replace('\\', '/')
        require(relative.startsWith("$owner/") || relative.startsWith("media/$owner/") ||
            relative in setOf("projects/$owner.ofam", "sync/$owner.ofam", "trash/$owner.json")) { "Recovery target outside project" }
        safePath(root, path.toFile())
        return relative
    }

    private fun target(name: String): File = File(root, name).also { require(relativeTarget(it) == name) }
    private fun targetTemp(target: File, index: Int): File = File(target.parentFile, ".ofam-recover-${directory.name}-$index.tmp").also {
        safePath(root, it)
        check(!Files.exists(it.toPath(), NOFOLLOW_LINKS) || Files.isRegularFile(it.toPath(), NOFOLLOW_LINKS)) { "Recovery temp is not a regular file" }
    }
    private fun blobFile(name: String): File {
        require(Regex("[0-9]+-(new|old)\\.enc").matches(name))
        return File(directory, name).also { safePath(root, it) }
    }

    private fun cleanup(): List<Exception> {
        val errors = mutableListOf<Exception>()
        for ((index, change) in journal.changes.withIndex()) {
            try { Files.deleteIfExists(targetTemp(target(change.target), index).toPath()) } catch (e: IOException) { errors += e }
        }
        val owned = directory.listFiles() ?: return errors + IOException("Cannot enumerate recovery files")
        for (file in owned) {
            try {
                safePath(root, file)
                require(file.name in setOf("header.json", "journal.enc", "journal.tmp") || Regex("[0-9]+-(new|old)\\.enc").matches(file.name)) {
                    "Unknown file in recovery directory"
                }
                require(Files.isRegularFile(file.toPath(), NOFOLLOW_LINKS))
            } catch (e: IllegalArgumentException) { return errors + e }
        }
        // Keep the header and journal until all payload cleanup succeeds.
        for (file in owned.filter { it.name !in setOf("header.json", "journal.enc") }) {
            try { Files.deleteIfExists(file.toPath()) } catch (e: IOException) { errors += e }
        }
        if (errors.isNotEmpty()) return errors
        for (name in listOf("journal.enc", "header.json")) {
            try { Files.deleteIfExists(File(directory, name).toPath()) } catch (e: IOException) { return errors + e }
        }
        try { Files.deleteIfExists(directory.toPath()) } catch (e: IOException) { errors += e }
        return errors
    }

    companion object {
        private const val MAX_JOURNAL_BYTES = 16 * 1024 * 1024
        private const val MAX_BLOB_BYTES = 256L * 1024 * 1024
        private const val CHUNK_BYTES = 256 * 1024
        private const val ENCRYPTED_OVERHEAD_BYTES = 2L * 1024 * 1024
        private val json get() = PackageSerializer.jsonConfig
        private fun ownerDirectory(root: File, owner: String): File {
            require(UUID.fromString(owner).toString() == owner)
            return File(root, ".recovery/$owner").also { safePath(root, it) }
        }
        private fun safePath(root: File, file: File) {
            val base = root.toPath().toAbsolutePath().normalize()
            var current = file.toPath().toAbsolutePath().normalize()
            require(current.startsWith(base))
            require(file.canonicalFile.toPath() == root.canonicalFile.toPath().resolve(base.relativize(current))) { "Recovery path contains a redirect" }
            while (current.startsWith(base)) {
                require(!Files.isSymbolicLink(current)) { "Recovery path contains a link" }
                if (current == base) break
                current = current.parent
            }
        }
        private fun force(file: File) = FileChannel.open(file.toPath(), StandardOpenOption.WRITE).use { it.force(true) }
        private fun move(source: File, target: File) {
            try { Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
            catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        fun start(root: File, owner: String, password: String?, beforeState: String? = null): FileRecovery {
            val parent = ownerDirectory(root, owner)
            Files.createDirectories(parent.toPath())
            check(pending(root, owner).isEmpty()) { "Unresolved recovery blocks this project" }
            val directory = File(parent, "txn-${UUID.randomUUID()}")
            Files.createDirectory(directory.toPath())
            val salt = password?.let { ByteArray(16).also(SecureRandom()::nextBytes) }
            val key = if (password != null) PackageSerializer.deriveKey(password, salt!!) else KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            val header = Header(owner = owner, salt = salt?.let { Base64.getEncoder().encodeToString(it) },
                key = if (salt == null) Base64.getEncoder().encodeToString(key.encoded) else null)
            val file = File(directory, "header.tmp")
            file.writeText(json.encodeToString(Header.serializer(), header), Charsets.UTF_8)
            force(file)
            move(file, File(directory, "header.json"))
            return FileRecovery(root, directory, owner, key, Journal(beforeState = beforeState)).also { it.persist() }
        }

        private fun pending(root: File, owner: String): List<File> {
            val parent = ownerDirectory(root, owner)
            if (!parent.exists()) return emptyList()
            return (parent.listFiles() ?: throw IOException("Cannot enumerate recovery directory")).sortedBy { it.name }.onEach {
                require(it.name.startsWith("txn-") && UUID.fromString(it.name.removePrefix("txn-")).toString() == it.name.removePrefix("txn-"))
                safePath(root, it)
                require(Files.isDirectory(it.toPath(), NOFOLLOW_LINKS))
            }
        }

        fun owners(root: File): List<String> {
            val parent = File(root, ".recovery")
            safePath(root, parent)
            if (!parent.exists()) return emptyList()
            return (parent.listFiles() ?: throw IOException("Cannot enumerate recovery owners")).mapNotNull { file ->
                val id = file.name
                ownerDirectory(root, id)
                id.takeIf { pending(root, id).isNotEmpty() }
            }
        }

        fun requiresPassword(root: File, owner: String): Boolean = pending(root, owner).any {
            if (it.list()?.isEmpty() == true) return@any false
            val header = File(it, "header.json")
            safePath(root, header)
            if (!header.exists() && it.list()?.toSet() == setOf("header.tmp")) return@any false
            require(header.length() <= 4096)
            json.decodeFromString(Header.serializer(), header.readText(Charsets.UTF_8)).salt != null
        }

        fun recover(root: File, owner: String, password: String?, currentState: String? = null) {
            for (directory in pending(root, owner)) {
                if (directory.list()?.isEmpty() == true) { Files.delete(directory.toPath()); continue }
                val headerFile = File(directory, "header.json")
                safePath(root, headerFile)
                if (!headerFile.exists() && directory.list()?.toSet() == setOf("header.tmp")) {
                    val temp = File(directory, "header.tmp")
                    safePath(root, temp)
                    require(Files.isRegularFile(temp.toPath(), NOFOLLOW_LINKS))
                    Files.delete(temp.toPath())
                    Files.delete(directory.toPath())
                    continue
                }
                require(headerFile.length() <= 4096)
                val header = json.decodeFromString(Header.serializer(), headerFile.readText(Charsets.UTF_8))
                require(header.version == 1 && header.owner == owner)
                val key = if (header.salt == null) {
                    val bytes = Base64.getDecoder().decode(requireNotNull(header.key))
                    require(bytes.size == 32)
                    SecretKeySpec(bytes, "AES")
                } else {
                    if (password == null) throw RecoveryPasswordRequired()
                    val salt = Base64.getDecoder().decode(header.salt)
                    require(salt.size == 16 && header.key == null)
                    PackageSerializer.deriveKey(password, salt)
                }
                val session = FileRecovery(root, directory, owner, key, Journal())
                val journalFile = File(directory, "journal.enc")
                safePath(root, journalFile)
                if (journalFile.exists()) {
                    require(journalFile.length() <= MAX_JOURNAL_BYTES + ENCRYPTED_OVERHEAD_BYTES)
                    val bytes = java.io.ByteArrayOutputStream()
                    try { session.decrypt(journalFile, bytes, MAX_JOURNAL_BYTES.toLong()) }
                    catch (e: AEADBadTagException) { if (header.salt != null) throw RecoveryPasswordRequired().also { it.initCause(e) }; throw e }
                    session.journal = json.decodeFromString(Journal.serializer(), bytes.toString(Charsets.UTF_8))
                } else {
                    check(directory.list()?.all { it in setOf("header.json", "journal.tmp") } == true) { "Missing recovery journal; backups retained" }
                }
                require(session.journal.phase in setOf("STAGING", "PREPARED", "COMMITTED"))
                session.journal.changes.forEach { session.target(it.target) }
                if (session.journal.phase == "PREPARED") {
                    val forward = if (currentState == null) false else {
                        check(currentState == session.journal.beforeState || currentState == session.journal.expectedState) { "Database changed while recovery was pending" }
                        currentState == session.journal.expectedState
                    }
                    session.install(forward)
                    session.journal = session.journal.copy(phase = "COMMITTED")
                    session.persist()
                }
                val errors = session.cleanup()
                if (errors.isNotEmpty()) throw errors.first().also { first -> errors.drop(1).forEach(first::addSuppressed) }
            }
        }
    }
}
