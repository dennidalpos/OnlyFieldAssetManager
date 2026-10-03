package com.onlyfield.assetmanager.data.repository

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

    suspend fun moveToTrash(projectId: String, itemType: String, itemId: String): com.onlyfield.assetmanager.core.model.TrashItem? {
        val project = getProjectById(projectId) ?: return null

        val trashItem = db.withTransaction {
            when (itemType.uppercase()) {
                "DEVICE", "RACK" -> {
                    val (updated, item) = if (itemType.uppercase() == "DEVICE")
                        com.onlyfield.assetmanager.core.edit.ProjectEdits.deleteDeviceToTrash(project, itemId)
                    else com.onlyfield.assetmanager.core.edit.ProjectEdits.deleteRackToTrash(project, itemId)
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
                        affectedReferencesSummary = "Credenziale per utente ${cred.username}"
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

    suspend fun restoreFromTrash(projectId: String, trashId: String): Boolean {
        val trashEntity = inventoryDao.getTrashItemById(trashId) ?: return false
        val trashItem = toTrashItem(trashEntity)

        db.withTransaction {
            when (trashItem.itemType.uppercase()) {
                "DEVICE", "RACK" -> {
                    val project = getProjectById(projectId) ?: return@withTransaction
                    save(com.onlyfield.assetmanager.core.edit.ProjectEdits.restoreFromTrash(project, trashItem))
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
        newCategory: com.onlyfield.assetmanager.core.model.DeviceCategory
    ): Pair<com.onlyfield.assetmanager.core.model.TrashItem?, com.onlyfield.assetmanager.core.model.Device> {
        val project = getProjectById(projectId) ?: throw IllegalArgumentException("Project not found")
        val bu = project.businessUnits.find { bu -> bu.devices.any { it.id == oldDeviceId } }
            ?: throw IllegalArgumentException("Device $oldDeviceId not found in project")
        val oldDevice = bu.devices.find { it.id == oldDeviceId }!!

        val trashItem = moveToTrash(projectId, "DEVICE", oldDeviceId)

        val newDevice = com.onlyfield.assetmanager.core.model.Device(
            id = java.util.UUID.randomUUID().toString(),
            technicalName = newTechnicalName,
            category = newCategory,
            siteId = oldDevice.siteId,
            areaId = oldDevice.areaId,
            rackId = oldDevice.rackId,
            ports = emptyList()
        )

        db.withTransaction {
            inventoryDao.insertDevices(listOf(toDeviceEntity(bu.id, newDevice)))
        }

        return Pair(trashItem, newDevice)
    }

    suspend fun mergeDevices(
        projectId: String,
        survivingDeviceId: String,
        duplicateDeviceId: String,
        choices: com.onlyfield.assetmanager.core.model.MergeDataChoices
    ): com.onlyfield.assetmanager.core.model.Device? {
        if (survivingDeviceId == duplicateDeviceId) return null
        val project = getProjectById(projectId) ?: return null

        var survivingBuId: String? = null
        var survivingDev: com.onlyfield.assetmanager.core.model.Device? = null
        var duplicateDev: com.onlyfield.assetmanager.core.model.Device? = null

        for (bu in project.businessUnits) {
            for (dev in bu.devices) {
                if (dev.id == survivingDeviceId) {
                    survivingDev = dev
                    survivingBuId = bu.id
                }
                if (dev.id == duplicateDeviceId) {
                    duplicateDev = dev
                }
            }
        }

        if (survivingDev == null || duplicateDev == null || survivingBuId == null) return null

        val mergedTechnicalName = if (choices.useTechnicalNameFromDuplicate) duplicateDev.technicalName else survivingDev.technicalName
        val mergedPhysicalLabel = if (choices.usePhysicalLabelFromDuplicate) duplicateDev.physicalLabel else survivingDev.physicalLabel
        val mergedAlias = if (choices.useAliasFromDuplicate) duplicateDev.alias else survivingDev.alias
        val mergedIp = if (choices.useIpFromDuplicate) duplicateDev.ipAddress else survivingDev.ipAddress
        val mergedMac = if (choices.useMacFromDuplicate) duplicateDev.macAddress else survivingDev.macAddress
        val mergedSiteId = if (choices.useLocationFromDuplicate) duplicateDev.siteId else survivingDev.siteId
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
            siteId = mergedSiteId,
            areaId = mergedAreaId,
            ports = mergedPorts
        )

        db.withTransaction {
            inventoryDao.insertDevices(listOf(toDeviceEntity(survivingBuId, updatedSurvivingDevice)))
            if (updatedSurvivingDevice.ports.isNotEmpty()) {
                inventoryDao.insertPorts(updatedSurvivingDevice.ports.map { toPortEntity(it) })
            }

            moveToTrash(projectId, "DEVICE", duplicateDeviceId)
        }

        return updatedSurvivingDevice
    }

    suspend fun batchEditDevices(
        projectId: String,
        deviceIds: List<String>,
        changes: com.onlyfield.assetmanager.core.model.BatchDeviceChanges
    ) {
        val project = getProjectById(projectId) ?: return
        save(com.onlyfield.assetmanager.core.edit.ProjectEdits.batchEditDevices(project, deviceIds, changes))
    }
}
