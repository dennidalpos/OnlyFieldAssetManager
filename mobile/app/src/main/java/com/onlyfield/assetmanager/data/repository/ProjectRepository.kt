package com.onlyfield.assetmanager.data.repository

import androidx.room.withTransaction
import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.local.AreaEntity
import com.onlyfield.assetmanager.data.local.BusinessUnitEntity
import com.onlyfield.assetmanager.data.local.DeviceEntity
import com.onlyfield.assetmanager.data.local.PortEntity
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.local.SiteEntity
import com.onlyfield.assetmanager.exchange.PackageImportResult
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.ProjectComparison
import com.onlyfield.assetmanager.exchange.ProjectComparisonEvaluator
import com.onlyfield.assetmanager.exchange.ProjectPackage
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

data class SearchResult(
    val device: Device,
    val businessUnitName: String,
    val siteName: String?,
    val areaName: String?,
    val matchedField: String
)

data class PackageImportEvaluation(
    val importResult: PackageImportResult,
    val comparison: ProjectComparison?
)

class ProjectRepository(
    private val db: AppDatabase
) {
    private val projectDao = db.projectDao()
    private val inventoryDao = db.inventoryDao()

    fun getAllProjects(): Flow<List<ProjectEntity>> {
        return projectDao.getAllProjects()
    }

    suspend fun getProjectById(projectId: String): Project? {
        val projEntity = projectDao.getProjectById(projectId) ?: return null
        val buEntities = inventoryDao.getBusinessUnitsByProjectId(projectId)
        val buIds = buEntities.map { it.id }

        val siteEntities = if (buIds.isNotEmpty()) inventoryDao.getSitesByBuIds(buIds) else emptyList()
        val areaEntities = if (buIds.isNotEmpty()) inventoryDao.getAreasByBuIds(buIds) else emptyList()
        val deviceEntities = if (buIds.isNotEmpty()) inventoryDao.getDevicesByBuIds(buIds) else emptyList()
        val devIds = deviceEntities.map { it.id }
        val portEntities = if (devIds.isNotEmpty()) inventoryDao.getPortsByDeviceIds(devIds) else emptyList()

        return EntityMappers.toProject(
            entity = projEntity,
            buEntities = buEntities,
            siteEntities = siteEntities,
            areaEntities = areaEntities,
            deviceEntities = deviceEntities,
            portEntities = portEntities
        )
    }

    suspend fun saveProject(project: Project) {
        db.withTransaction {
            // Save project entity
            projectDao.insertProject(EntityMappers.toProjectEntity(project))

            // Delete existing inventory tree for this project to ensure atomic replace
            inventoryDao.deleteBusinessUnitsByProjectId(project.id)

            val buEntities = mutableListOf<BusinessUnitEntity>()
            val siteEntities = mutableListOf<SiteEntity>()
            val areaEntities = mutableListOf<AreaEntity>()
            val deviceEntities = mutableListOf<DeviceEntity>()
            val portEntities = mutableListOf<PortEntity>()

            for (bu in project.businessUnits) {
                buEntities.add(EntityMappers.toBusinessUnitEntity(project.id, bu))

                for (site in bu.sites) {
                    siteEntities.add(EntityMappers.toSiteEntity(bu.id, site))
                    for (area in site.areas) {
                        areaEntities.add(EntityMappers.toAreaEntity(bu.id, site.id, area))
                    }
                }

                for (area in bu.areas) {
                    areaEntities.add(EntityMappers.toAreaEntity(bu.id, null, area))
                }

                for (device in bu.devices) {
                    deviceEntities.add(EntityMappers.toDeviceEntity(bu.id, device))
                    for (port in device.ports) {
                        portEntities.add(EntityMappers.toPortEntity(port))
                    }
                }
            }

            if (buEntities.isNotEmpty()) inventoryDao.insertBusinessUnits(buEntities)
            if (siteEntities.isNotEmpty()) inventoryDao.insertSites(siteEntities)
            if (areaEntities.isNotEmpty()) inventoryDao.insertAreas(areaEntities)
            if (deviceEntities.isNotEmpty()) inventoryDao.insertDevices(deviceEntities)
            if (portEntities.isNotEmpty()) inventoryDao.insertPorts(portEntities)
        }
    }

    suspend fun exportProjectPackage(projectId: String): ByteArray? {
        val project = getProjectById(projectId) ?: return null
        return PackageSerializer.exportPackage(project)
    }

    suspend fun exportProjectPackageToStream(projectId: String, outputStream: OutputStream): Boolean {
        val zipBytes = exportProjectPackage(projectId) ?: return false
        outputStream.use { stream ->
            stream.write(zipBytes)
            stream.flush()
        }
        return true
    }

    suspend fun evaluateImportPackage(
        inputStream: InputStream,
        currentProjectId: String? = null
    ): PackageImportEvaluation {
        val bytes = inputStream.use { it.readBytes() }
        val importResult = PackageSerializer.importPackage(bytes)

        val pkg = importResult.pkg
        if (pkg == null || !importResult.validationResult.isValid) {
            return PackageImportEvaluation(importResult = importResult, comparison = null)
        }

        val localProjectId = currentProjectId ?: pkg.project.id
        val localProject = getProjectById(localProjectId)

        val comparison = ProjectComparisonEvaluator.evaluate(
            currentProject = localProject,
            currentManifest = null,
            incomingPackage = pkg
        )

        return PackageImportEvaluation(importResult = importResult, comparison = comparison)
    }

    suspend fun importProjectPackage(pkg: ProjectPackage): Boolean {
        saveProject(pkg.project)
        return true
    }

    suspend fun renameProject(projectId: String, newName: String) {
        val existing = projectDao.getProjectById(projectId) ?: return
        val updated = existing.copy(
            name = newName,
            updatedEpochMs = System.currentTimeMillis()
        )
        projectDao.updateProject(updated)
    }

    suspend fun searchInventory(projectId: String, query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val buEntities = inventoryDao.getBusinessUnitsByProjectId(projectId)
        val buMap = buEntities.associateBy { it.id }
        val buIds = buEntities.map { it.id }
        if (buIds.isEmpty()) return emptyList()

        val matchedDeviceEntities = inventoryDao.searchDevices(buIds, query.trim())
        if (matchedDeviceEntities.isEmpty()) return emptyList()

        val siteEntities = inventoryDao.getSitesByBuIds(buIds).associateBy { it.id }
        val areaEntities = inventoryDao.getAreasByBuIds(buIds).associateBy { it.id }
        val matchedDevIds = matchedDeviceEntities.map { it.id }
        val portEntities = inventoryDao.getPortsByDeviceIds(matchedDevIds).groupBy { it.deviceId }

        val q = query.trim().lowercase()

        return matchedDeviceEntities.map { devEnt ->
            val bu = buMap[devEnt.businessUnitId]
            val site = devEnt.siteId?.let { siteEntities[it] }
            val area = devEnt.areaId?.let { areaEntities[it] }

            val devPorts = portEntities[devEnt.id].orEmpty().map { p ->
                com.onlyfield.assetmanager.core.model.Port(
                    id = p.id,
                    deviceId = p.deviceId,
                    name = p.name,
                    label = p.label,
                    connectedPortId = p.connectedPortId
                )
            }

            val device = Device(
                id = devEnt.id,
                technicalName = devEnt.technicalName,
                physicalLabel = devEnt.physicalLabel,
                alias = devEnt.alias,
                ipAddress = devEnt.ipAddress,
                macAddress = devEnt.macAddress,
                siteId = devEnt.siteId,
                areaId = devEnt.areaId,
                ports = devPorts
            )

            val matchedField = when {
                devEnt.technicalName.lowercase().contains(q) -> "Nome Tecnico (${devEnt.technicalName})"
                devEnt.ipAddress?.lowercase()?.contains(q) == true -> "Indirizzo IP (${devEnt.ipAddress})"
                devEnt.physicalLabel?.lowercase()?.contains(q) == true -> "Etichetta Fisica (${devEnt.physicalLabel})"
                devEnt.alias?.lowercase()?.contains(q) == true -> "Alias (${devEnt.alias})"
                else -> "Corrispondenza Generale"
            }

            SearchResult(
                device = device,
                businessUnitName = bu?.name ?: "BU Sconosciuta",
                siteName = site?.name,
                areaName = area?.name,
                matchedField = matchedField
            )
        }
    }
}
