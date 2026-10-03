package com.onlyfield.assetmanager.data.repository

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

/** Device search on the Room tables (name, IP, label, alias). */
internal class InventorySearch(db: AppDatabase) {
    private val inventoryDao = db.inventoryDao()

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
