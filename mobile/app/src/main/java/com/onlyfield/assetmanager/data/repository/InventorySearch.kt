package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.data.local.AppDatabase

/** Device search on the Room tables (name, IP, label, alias). */
internal class InventorySearch(db: AppDatabase) {
    private val inventoryDao = db.inventoryDao()

    suspend fun searchInventory(projectId: String, query: String, i18n: Messages = Messages()): List<SearchResult> {
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
                devEnt.technicalName.lowercase().contains(q) -> i18n.text("text.d3456d0760b8", devEnt.technicalName)
                devEnt.ipAddress?.lowercase()?.contains(q) == true -> i18n.text("text.e22da0229a30", devEnt.ipAddress)
                devEnt.physicalLabel?.lowercase()?.contains(q) == true -> i18n.text("text.9ee2fe3ec524", devEnt.physicalLabel)
                devEnt.alias?.lowercase()?.contains(q) == true -> i18n.text("text.2feff34b895f", devEnt.alias)
                else -> i18n.text("text.a87d25bca969")
            }

            SearchResult(
                device = device,
                businessUnitName = bu?.name ?: i18n.text("text.aabb8f0ddb86"),
                siteName = site?.name,
                areaName = area?.name,
                matchedField = matchedField
            )
        }
    }
}
