package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.data.repository.mappers.*
import androidx.room.withTransaction
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.data.local.AppDatabase

/** Trash, device replacement, duplicate merge and batch edit, written row by row. */
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

    suspend fun restoreFromTrash(projectId: String, trashId: String, i18n: Messages = Messages()): Boolean {
        val trashEntity = inventoryDao.getTrashItemById(trashId) ?: return false
        val trashItem = toTrashItem(trashEntity)

        db.withTransaction {
            when (trashItem.itemType.uppercase()) {
                "DEVICE", "RACK" -> {
                    val project = getProjectById(projectId) ?: return@withTransaction
                    save(com.onlyfield.assetmanager.core.edit.ProjectEdits.restoreFromTrash(project, trashItem, i18n = i18n))
                    inventoryDao.deleteTrashItemById(trashId)
                }
                "CREDENTIAL" -> {
                    val cred = jsonSerializer.decodeFromString(com.onlyfield.assetmanager.core.model.Credential.serializer(), trashItem.serializedJson)
                    inventoryDao.insertCredentials(listOf(toCredentialEntity(projectId, cred)))
                    inventoryDao.deleteTrashItemById(trashId)
                }
            }
        }
        return true
    }

    suspend fun replaceDevice(
        projectId: String,
        oldDeviceId: String,
        newTechnicalName: String,
        newCategory: com.onlyfield.assetmanager.core.model.DeviceCategory,
        i18n: Messages = Messages()): Pair<com.onlyfield.assetmanager.core.model.TrashItem?, com.onlyfield.assetmanager.core.model.Device> {
        val project = getProjectById(projectId) ?: throw IllegalArgumentException(i18n.text("text.758e8416eb8a"))
        val site = project.sites.find { site -> site.devices.any { it.id == oldDeviceId } }
            ?: throw IllegalArgumentException(i18n.text("text.4ac20cd01b41", oldDeviceId))
        val oldDevice = site.devices.find { it.id == oldDeviceId }!!

        val trashItem = moveToTrash(projectId, "DEVICE", oldDeviceId, i18n = i18n)

        val newDevice = com.onlyfield.assetmanager.core.model.Device(
            id = java.util.UUID.randomUUID().toString(),
            technicalName = newTechnicalName,
            category = newCategory,
            areaId = oldDevice.areaId,
            rackId = oldDevice.rackId,
            ports = emptyList()
        )

        db.withTransaction {
            inventoryDao.insertDevices(listOf(toDeviceEntity(site.id, newDevice)))
        }

        return Pair(trashItem, newDevice)
    }

    suspend fun mergeDevices(
        projectId: String,
        survivingDeviceId: String,
        duplicateDeviceId: String,
        choices: com.onlyfield.assetmanager.core.model.MergeDataChoices,
        i18n: Messages = Messages()): com.onlyfield.assetmanager.core.model.Device? {
        if (survivingDeviceId == duplicateDeviceId) return null
        val project = getProjectById(projectId) ?: return null

        var survivingSiteId: String? = null
        var survivingDev: com.onlyfield.assetmanager.core.model.Device? = null
        var duplicateDev: com.onlyfield.assetmanager.core.model.Device? = null

        for (site in project.sites) {
            for (dev in site.devices) {
                if (dev.id == survivingDeviceId) {
                    survivingDev = dev
                    survivingSiteId = site.id
                }
                if (dev.id == duplicateDeviceId) {
                    duplicateDev = dev
                }
            }
        }

        if (survivingDev == null || duplicateDev == null || survivingSiteId == null) return null

        val mergedTechnicalName = if (choices.useTechnicalNameFromDuplicate) duplicateDev.technicalName else survivingDev.technicalName
        val mergedPhysicalLabel = if (choices.usePhysicalLabelFromDuplicate) duplicateDev.physicalLabel else survivingDev.physicalLabel
        val mergedAlias = if (choices.useAliasFromDuplicate) duplicateDev.alias else survivingDev.alias
        val mergedIp = if (choices.useIpFromDuplicate) duplicateDev.ipAddress else survivingDev.ipAddress
        val mergedMac = if (choices.useMacFromDuplicate) duplicateDev.macAddress else survivingDev.macAddress
        val mergedAreaId = if (choices.useLocationFromDuplicate) duplicateDev.areaId else survivingDev.areaId

        val mergedPorts = survivingDev.ports.toMutableList()
        if (choices.mergePorts) {
            for (dupPort in duplicateDev.ports) {
                mergedPorts.add(dupPort.copy(id = java.util.UUID.randomUUID().toString(), deviceId = survivingDeviceId))
            }
        }

        val updatedSurvivingDevice = survivingDev.copy(
            technicalName = mergedTechnicalName,
            physicalLabel = mergedPhysicalLabel,
            alias = mergedAlias,
            ipAddress = mergedIp,
            macAddress = mergedMac,
            areaId = mergedAreaId,
            ports = mergedPorts
        )

        db.withTransaction {
            inventoryDao.insertDevices(listOf(toDeviceEntity(survivingSiteId, updatedSurvivingDevice)))
            if (updatedSurvivingDevice.ports.isNotEmpty()) {
                inventoryDao.insertPorts(updatedSurvivingDevice.ports.map { toPortEntity(it) })
            }

            moveToTrash(projectId, "DEVICE", duplicateDeviceId, i18n = i18n)
        }

        return updatedSurvivingDevice
    }

    suspend fun batchEditDevices(
        projectId: String,
        deviceIds: List<String>,
        changes: com.onlyfield.assetmanager.core.model.BatchDeviceChanges,
        i18n: Messages = Messages()) {
        val project = getProjectById(projectId) ?: return
        save(com.onlyfield.assetmanager.core.edit.ProjectEdits.batchEditDevices(project, deviceIds, changes, i18n = i18n))
    }
}
