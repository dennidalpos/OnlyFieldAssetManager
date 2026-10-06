package com.onlyfield.assetmanager.exchange

import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Keeps replaced files until the enclosing project commit succeeds. */
class ReversibleFiles(scratchDirectory: File? = null) : AutoCloseable {
    private data class Change(val target: File, val staged: File?, var backup: String? = null, var applied: Boolean = false)
    private val changes = mutableListOf<Change>()
    private val backups = PackagePayloads(scratchDirectory)
    private var committed = false

    fun replace(target: File, write: (OutputStream) -> Unit) {
        Files.createDirectories(target.parentFile.toPath())
        val staged = Files.createTempFile(target.parentFile.toPath(), ".ofam-write-", ".tmp").toFile()
        changes += Change(target, staged)
        staged.outputStream().buffered().use(write)
    }

    fun remove(target: File) { if (target.isFile) changes += Change(target, null) }

    fun apply() {
        for ((index, change) in changes.withIndex()) {
            check(!change.applied)
            if (change.target.exists()) {
                require(change.target.isFile) { "Expected a file: ${change.target}" }
                val name = index.toString()
                backups.write(name) { output -> change.target.inputStream().buffered().use { it.copyTo(output) } }
                change.backup = name
            }
            change.applied = true
            if (change.staged == null) Files.deleteIfExists(change.target.toPath())
            else move(change.staged, change.target)
        }
    }

    /** Cleanup errors are warnings after commit, never a failed project save. */
    fun commit(): List<Exception> {
        committed = true
        return cleanup()
    }

    private fun cleanup(): List<Exception> {
        val errors = mutableListOf<Exception>()
        for (change in changes) {
            for (file in listOfNotNull(change.staged)) {
                try { Files.deleteIfExists(file.toPath()) } catch (e: Exception) { errors += e }
            }
        }
        try { backups.close() } catch (e: Exception) { errors += e }
        return errors
    }

    override fun close() {
        if (committed) return
        var failure: Exception? = null
        for (change in changes.asReversed()) {
            if (!change.applied) continue
            try {
                val backup = change.backup
                if (backup == null) Files.deleteIfExists(change.target.toPath())
                else change.target.outputStream().buffered().use { backups.writeTo(backup, it) }
            } catch (e: Exception) {
                if (failure == null) failure = e else failure.addSuppressed(e)
            }
        }
        val errors = cleanup()
        if (failure != null) throw failure.also { first -> errors.forEach(first::addSuppressed) }
        if (errors.isNotEmpty()) throw errors.first().also { first -> errors.drop(1).forEach(first::addSuppressed) }
    }

    private fun move(source: File, target: File) {
        try { Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
