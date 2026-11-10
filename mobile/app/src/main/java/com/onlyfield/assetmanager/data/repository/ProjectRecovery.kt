package com.onlyfield.assetmanager.data.repository

import androidx.room.withTransaction
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.exchange.FileRecovery
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.ReversibleFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.io.IOException

/** Serializes access while durable file recovery compares the Room outcome. */
internal class ProjectRecovery(private val db: AppDatabase, private val root: File?, private val password: String?) {
    private val mutex = Mutex()
    private val store = ProjectStore(db)
    private val trash = TrashOperations(db, store::load, { store.save(it) })

    init { require(root == null || !password.isNullOrBlank()) { "File storage requires a recovery key" } }

    suspend fun <T> access(projectId: String, block: suspend () -> T): T {
        if (root == null) return block()
        return withContext(Dispatchers.IO) { mutex.withLock { recover(projectId); block() } }
    }

    suspend fun recoverAll(i18n: Messages): List<String> {
        if (root == null) return emptyList()
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val errors = mutableListOf<String>()
                for (id in FileRecovery.owners(root)) {
                    try { recover(id) }
                    catch (e: IOException) { errors += i18n.text("recovery.blocked", id) }
                }
                errors
            }
        }
    }

    private suspend fun recover(projectId: String) {
        if (root == null || projectId !in FileRecovery.owners(root)) return
        try { FileRecovery.recover(root, projectId, password, fingerprint(projectId)) }
        catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw IOException("Recovery blocked; project data retained: $projectId", e)
        }
    }

    suspend fun files(projectId: String): ReversibleFiles = if (root == null) ReversibleFiles()
        else ReversibleFiles(recovery = FileRecovery.start(root, projectId, password, fingerprint(projectId)))

    suspend fun expectState(files: ReversibleFiles, projectId: String) {
        if (root != null) files.expectState(fingerprint(projectId))
    }

    /** Include local protection, base and trash; collection ordering is not a commit marker. */
    suspend fun fingerprint(projectId: String): String = db.withTransaction {
        val json = PackageSerializer.jsonConfig
        val base = db.projectDao().getSyncSnapshot(projectId)
        val state = buildJsonObject {
            put("project", store.load(projectId)?.let { json.encodeToJsonElement(Project.serializer(), it) } ?: JsonNull)
            put("passwordHash", db.projectDao().getProjectById(projectId)?.passwordHash?.let(::JsonPrimitive) ?: JsonNull)
            put("base", base?.projectJson?.let(::JsonPrimitive) ?: JsonNull)
            put("baseGeneration", base?.savedEpochMs?.let(::JsonPrimitive) ?: JsonNull)
            put("trash", json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(com.onlyfield.assetmanager.core.model.TrashItem.serializer()), trash.getTrashItems(projectId)))
        }
        PackageSerializer.calculateSha256(canonical(state).toString().toByteArray(Charsets.UTF_8))
    }

    private fun canonical(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> JsonObject(value.toSortedMap().mapValues { canonical(it.value) })
        is JsonArray -> {
            val normalized = value.map(::canonical)
            JsonArray(if (normalized.all { it is JsonObject && it["id"] != null }) normalized.sortedBy { (it as JsonObject)["id"].toString() } else normalized)
        }
        else -> value
    }
}
