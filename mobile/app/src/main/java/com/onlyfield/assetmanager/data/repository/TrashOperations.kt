package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.data.repository.mappers.*
import androidx.room.withTransaction
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.DeviceModel
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.local.AreaEntity
import com.onlyfield.assetmanager.data.local.BusinessUnitEntity
import com.onlyfield.assetmanager.data.local.CredentialEntity
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.data.local.DeviceEntity
import com.onlyfield.assetmanager.data.local.DeviceModelEntity
import com.onlyfield.assetmanager.data.local.PortEntity
import android.content.Context
import android.print.PrintManager
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.local.RackEntity
import com.onlyfield.assetmanager.data.local.SiteEntity
import com.onlyfield.assetmanager.exchange.DeviceModelSerializer
import com.onlyfield.assetmanager.exchange.MarkdownExportManager
import com.onlyfield.assetmanager.exchange.PackageImportResult
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.PasswordHasher
import com.onlyfield.assetmanager.exchange.ProjectComparison
import com.onlyfield.assetmanager.exchange.ProjectComparisonEvaluator
import com.onlyfield.assetmanager.exchange.ProjectPackage
import com.onlyfield.assetmanager.exchange.XlsxExportManager
import com.onlyfield.assetmanager.export.PdfExportManager
import com.onlyfield.assetmanager.export.ProjectPrintDocumentAdapter
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers

/** Trash, device replacement, duplicate merge and batch edit, written row by row. */
internal class TrashOperations(private val db: AppDatabase, private val load: suspend (String) -> Project?) {
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
                "DEVICE" -> {
                    val device = project.businessUnits.flatMap { it.devices }.find { it.id == itemId } ?: return@withTransaction null
                    val jsonStr = jsonSerializer.encodeToString(com.onlyfield.assetmanager.core.model.Device.serializer(), device)
                    val affectedPorts = device.ports.map { it.id }

                    val allCables = project.cables
                    val updatedCables = mutableListOf<com.onlyfield.assetmanager.core.model.Cable>()
                    for (cable in allCables) {
                        if (affectedPorts.contains(cable.portAId) || affectedPorts.contains(cable.portBId)) {
                            val newCable = cable.copy(
                                portAId = if (affectedPorts.contains(cable.portAId)) null else cable.portAId,
                                portBId = if (affectedPorts.contains(cable.portBId)) null else cable.portBId,
                                observation = com.onlyfield.assetmanager.core.model.Observation(
                                    source = "System Trash",
                                    timestampEpochMs = System.currentTimeMillis(),
                                    status = com.onlyfield.assetmanager.core.model.ObservationStatus.TO_VERIFY,
                                    notes = "Estremità scollegata per eliminazione apparato '${device.technicalName}'"
                                )
                            )
                            updatedCables.add(newCable)
                            inventoryDao.insertCables(listOf(toCableEntity(projectId, newCable)))
                        }
                    }

                    val summary = "Porte: ${device.ports.size}, Cavi scollegati: ${updatedCables.size}"
                    val item = com.onlyfield.assetmanager.core.model.TrashItem(
                        projectId = projectId,
                        itemType = "DEVICE",
                        itemId = itemId,
                        displayName = device.technicalName,
                        serializedJson = jsonStr,
                        affectedReferencesSummary = summary
                    )
                    inventoryDao.insertTrashItems(listOf(toTrashItemEntity(item)))
                    inventoryDao.deletePortsByDeviceId(itemId)
                    inventoryDao.deleteDeviceById(itemId)
                    item
                }
                "RACK" -> {
                    val rack = project.racks.find { it.id == itemId } ?: return@withTransaction null
                    val jsonStr = jsonSerializer.encodeToString(com.onlyfield.assetmanager.core.model.Rack.serializer(), rack)
                    val devicesInRack = project.businessUnits.flatMap { it.devices }.filter { it.rackId == itemId }

                    for (dev in devicesInRack) {
                        val unassignedDev = dev.copy(rackId = null, positionU = null)
                        val devBU = project.businessUnits.find { bu -> bu.devices.any { it.id == dev.id } }
                        if (devBU != null) {
                            inventoryDao.insertDevices(listOf(toDeviceEntity(devBU.id, unassignedDev)))
                        }
                    }

                    val summary = "Apparati dislocati dal rack: ${devicesInRack.size}"
                    val item = com.onlyfield.assetmanager.core.model.TrashItem(
                        projectId = projectId,
                        itemType = "RACK",
                        itemId = itemId,
                        displayName = rack.name,
                        serializedJson = jsonStr,
                        affectedReferencesSummary = summary
                    )
                    inventoryDao.insertTrashItems(listOf(toTrashItemEntity(item)))
                    inventoryDao.deleteRackById(itemId)
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
                "DEVICE" -> {
                    val device = jsonSerializer.decodeFromString(com.onlyfield.assetmanager.core.model.Device.serializer(), trashItem.serializedJson)
                    val project = getProjectById(projectId)
                    val buId = project?.businessUnits?.firstOrNull()?.id ?: return@withTransaction
                    inventoryDao.insertDevices(listOf(toDeviceEntity(buId, device)))
                    if (device.ports.isNotEmpty()) {
                        val portEntities = device.ports.map { toPortEntity(it) }
                        inventoryDao.insertPorts(portEntities)
                    }
                    inventoryDao.deleteTrashItemById(trashId)
                }
                "RACK" -> {
                    val rack = jsonSerializer.decodeFromString(com.onlyfield.assetmanager.core.model.Rack.serializer(), trashItem.serializedJson)
                    inventoryDao.insertRacks(listOf(toRackEntity(projectId, rack)))
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
        val devicesToUpdate = mutableListOf<Pair<String, com.onlyfield.assetmanager.core.model.Device>>()

        for (bu in project.businessUnits) {
            for (dev in bu.devices) {
                if (deviceIds.contains(dev.id)) {
                    var updated = dev
                    val cat = changes.category
                    val mType = changes.mountingType
                    if (changes.updateSiteId) updated = updated.copy(siteId = changes.siteId)
                    if (changes.updateAreaId) updated = updated.copy(areaId = changes.areaId)
                    if (changes.updateCategory && cat != null) updated = updated.copy(category = cat)
                    if (changes.updateRackId) updated = updated.copy(rackId = changes.rackId)
                    if (changes.updateMountingType && mType != null) updated = updated.copy(mountingType = mType)
                    if (changes.updateObservationNotes) {
                        val obs = updated.observation ?: com.onlyfield.assetmanager.core.model.Observation("BatchEdit", System.currentTimeMillis())
                        updated = updated.copy(observation = obs.copy(notes = changes.observationNotes))
                    }
                    devicesToUpdate.add(bu.id to updated)
                }
            }
        }

        if (devicesToUpdate.isNotEmpty()) {
            db.withTransaction {
                for ((buId, dev) in devicesToUpdate) {
                    inventoryDao.insertDevices(listOf(toDeviceEntity(buId, dev)))
                }
            }
        }
    }
}
