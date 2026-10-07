package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.data.repository.mappers.*
import androidx.room.withTransaction
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.core.model.MergeDataChoices
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.TrashItem
import com.onlyfield.assetmanager.data.local.AppDatabase

/** Persists shared device edits and their trash in one transaction. */
internal class TrashOperations(private val db: AppDatabase, private val load: suspend (String) -> Project?, private val save: suspend (Project) -> Unit) {
    private val inventoryDao = db.inventoryDao()
    private val jsonSerializer = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private suspend fun getProjectById(projectId: String) = load(projectId)

    suspend fun getTrashItems(projectId: String): List<com.onlyfield.assetmanager.core.model.TrashItem> {
        return inventoryDao.getTrashItemsByProjectId(projectId).map { toTrashItem(it) }
    }

    suspend fun emptyTrash(projectId: String) {
        inventoryDao.emptyTrashByProjectId(projectId)
    }

    suspend fun deleteTrashItemPermanently(trashId: String) {
        inventoryDao.deleteTrashItemById(trashId)
    }

    suspend fun moveToTrash(projectId: String, itemType: String, itemId: String, i18n: Messages = Messages()): com.onlyfield.assetmanager.core.model.TrashItem? {
        val project = getProjectById(projectId) ?: return null

        val trashItem = db.withTransaction {
            when (itemType.uppercase()) {
                "DEVICE", "RACK" -> {
                    val (updated, item) = if (itemType.uppercase() == "DEVICE")
                        com.onlyfield.assetmanager.core.edit.ProjectEdits.deleteDeviceToTrash(project, itemId, i18n = i18n)
                    else com.onlyfield.assetmanager.core.edit.ProjectEdits.deleteRackToTrash(project, itemId, i18n = i18n)
                    if (item != null) {
                        save(updated)
                        inventoryDao.insertTrashItems(listOf(toTrashItemEntity(item)))
                    }
                    item
                }
                "CREDENTIAL" -> {
                    val cred = project.credentials.find { it.id == itemId } ?: return@withTransaction null
                    val jsonStr = jsonSerializer.encodeToString(com.onlyfield.assetmanager.core.model.Credential.serializer(), cred)
                    val item = com.onlyfield.assetmanager.core.model.TrashItem(
                        projectId = projectId,
                        itemType = "CREDENTIAL",
                        itemId = itemId,
                        displayName = cred.username,
                        serializedJson = jsonStr,
                        affectedReferencesSummary = i18n.text("text.0f40bc910c52", cred.username)
                    )
                    inventoryDao.insertTrashItems(listOf(toTrashItemEntity(item)))
                    inventoryDao.deleteCredentialById(itemId)
                    item
                }
                else -> null
            }
        }

        return trashItem
    }

    suspend fun restoreFromTrash(projectId: String, trashId: String, i18n: Messages = Messages()): Boolean = db.withTransaction {
        val trashEntity = inventoryDao.getTrashItemById(trashId) ?: return@withTransaction false
        val project = getProjectById(projectId) ?: throw IllegalStateException(i18n.text("trash.invalidEntry"))
        val restored = ProjectEdits.restoreFromTrash(project, toTrashItem(trashEntity), i18n = i18n)
        save(restored)
        inventoryDao.deleteTrashItemById(trashId)
        true
    }

    suspend fun replaceDevice(
        projectId: String,
        oldDeviceId: String,
        newTechnicalName: String,
        newCategory: DeviceCategory,
        i18n: Messages = Messages()
    ): Pair<TrashItem?, Device> = db.withTransaction {
        val project = getProjectById(projectId) ?: throw IllegalArgumentException(i18n.text("text.758e8416eb8a"))
        val site = project.sites.find { site -> site.devices.any { it.id == oldDeviceId } }
            ?: throw IllegalArgumentException(i18n.text("text.4ac20cd01b41", oldDeviceId))
        val (updated, item) = ProjectEdits.replaceDevice(project, oldDeviceId, newTechnicalName, newCategory, i18n)
        val previousIds = site.devices.map { it.id }.toSet()
        val replacement = updated.sites.first { it.id == site.id }.devices.single { it.id !in previousIds }
        save(updated)
        inventoryDao.insertTrashItems(listOf(toTrashItemEntity(requireNotNull(item))))
        item to replacement
    }

    suspend fun mergeDevices(
        projectId: String,
        survivingDeviceId: String,
        duplicateDeviceId: String,
        choices: MergeDataChoices,
        i18n: Messages = Messages()
    ): Device? = db.withTransaction {
        if (survivingDeviceId == duplicateDeviceId) return@withTransaction null
        val project = getProjectById(projectId) ?: return@withTransaction null
        val (updated, item) = ProjectEdits.mergeDevices(project, survivingDeviceId, duplicateDeviceId, choices, i18n)
        if (item == null) return@withTransaction null
        save(updated)
        inventoryDao.insertTrashItems(listOf(toTrashItemEntity(item)))
        updated.sites.flatMap { it.devices }.first { it.id == survivingDeviceId }
    }

}
