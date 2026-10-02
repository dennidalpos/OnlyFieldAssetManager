package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.EndpointStatus
import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.data.local.AreaEntity
import com.onlyfield.assetmanager.data.local.BusinessUnitEntity
import com.onlyfield.assetmanager.data.local.DeviceEntity
import com.onlyfield.assetmanager.data.local.PortEntity
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.local.SiteEntity

object EntityMappers {

    fun toProjectEntity(project: Project): ProjectEntity {
        return ProjectEntity(
            id = project.id,
            name = project.name,
            description = project.description,
            createdEpochMs = project.createdEpochMs,
            updatedEpochMs = project.updatedEpochMs
        )
    }

    fun toBusinessUnitEntity(projectId: String, bu: BusinessUnit): BusinessUnitEntity {
        return BusinessUnitEntity(
            id = bu.id,
            projectId = projectId,
            name = bu.name,
            code = bu.code
        )
    }

    fun toSiteEntity(buId: String, site: Site): SiteEntity {
        return SiteEntity(
            id = site.id,
            businessUnitId = buId,
            name = site.name,
            address = site.address
        )
    }

    fun toAreaEntity(buId: String, siteId: String?, area: Area): AreaEntity {
        return AreaEntity(
            id = area.id,
            businessUnitId = buId,
            siteId = siteId,
            name = area.name,
            floor = area.floor,
            description = area.description
        )
    }

    fun toDeviceEntity(buId: String, device: Device): DeviceEntity {
        return DeviceEntity(
            id = device.id,
            businessUnitId = buId,
            siteId = device.siteId,
            areaId = device.areaId,
            technicalName = device.technicalName,
            physicalLabel = device.physicalLabel,
            alias = device.alias,
            ipAddress = device.ipAddress,
            macAddress = device.macAddress,
            obsSource = device.observation?.source,
            obsTimestampEpochMs = device.observation?.timestampEpochMs,
            obsStatus = device.observation?.status?.name,
            obsNotes = device.observation?.notes
        )
    }

    fun toPortEntity(port: Port): PortEntity {
        return PortEntity(
            id = port.id,
            deviceId = port.deviceId,
            name = port.name,
            label = port.label,
            connectedPortId = port.connectedPortId,
            endpointStatus = port.endpointStatus.name,
            obsSource = port.observation?.source,
            obsTimestampEpochMs = port.observation?.timestampEpochMs,
            obsStatus = port.observation?.status?.name,
            obsNotes = port.observation?.notes
        )
    }

    fun toProject(
        entity: ProjectEntity,
        buEntities: List<BusinessUnitEntity>,
        siteEntities: List<SiteEntity>,
        areaEntities: List<AreaEntity>,
        deviceEntities: List<DeviceEntity>,
        portEntities: List<PortEntity>
    ): Project {
        val portsByDevice = portEntities.groupBy { it.deviceId }
        val devicesByBu = deviceEntities.groupBy { it.businessUnitId }
        val sitesByBu = siteEntities.groupBy { it.businessUnitId }
        val areasByBu = areaEntities.groupBy { it.businessUnitId }

        val businessUnits = buEntities.map { buEnt ->
            val buSites = sitesByBu[buEnt.id].orEmpty().map { siteEnt ->
                val siteAreas = areasByBu[buEnt.id].orEmpty()
                    .filter { it.siteId == siteEnt.id }
                    .map { areaEnt ->
                        Area(
                            id = areaEnt.id,
                            name = areaEnt.name,
                            floor = areaEnt.floor,
                            description = areaEnt.description
                        )
                    }
                Site(
                    id = siteEnt.id,
                    name = siteEnt.name,
                    address = siteEnt.address,
                    areas = siteAreas
                )
            }

            val buDirectAreas = areasByBu[buEnt.id].orEmpty()
                .filter { it.siteId == null }
                .map { areaEnt ->
                    Area(
                        id = areaEnt.id,
                        name = areaEnt.name,
                        floor = areaEnt.floor,
                        description = areaEnt.description
                    )
                }

            val buDevices = devicesByBu[buEnt.id].orEmpty().map { devEnt ->
                val devPorts = portsByDevice[devEnt.id].orEmpty().map { portEnt ->
                    val obs = if (portEnt.obsSource != null && portEnt.obsTimestampEpochMs != null && portEnt.obsStatus != null) {
                        Observation(
                            source = portEnt.obsSource,
                            timestampEpochMs = portEnt.obsTimestampEpochMs,
                            status = try { ObservationStatus.valueOf(portEnt.obsStatus) } catch (e: Exception) { ObservationStatus.TO_VERIFY },
                            notes = portEnt.obsNotes
                        )
                    } else null

                    Port(
                        id = portEnt.id,
                        deviceId = portEnt.deviceId,
                        name = portEnt.name,
                        label = portEnt.label,
                        connectedPortId = portEnt.connectedPortId,
                        endpointStatus = try { EndpointStatus.valueOf(portEnt.endpointStatus) } catch (e: Exception) { EndpointStatus.DISCONNECTED },
                        observation = obs
                    )
                }

                val devObs = if (devEnt.obsSource != null && devEnt.obsTimestampEpochMs != null && devEnt.obsStatus != null) {
                    Observation(
                        source = devEnt.obsSource,
                        timestampEpochMs = devEnt.obsTimestampEpochMs,
                        status = try { ObservationStatus.valueOf(devEnt.obsStatus) } catch (e: Exception) { ObservationStatus.TO_VERIFY },
                        notes = devEnt.obsNotes
                    )
                } else null

                Device(
                    id = devEnt.id,
                    technicalName = devEnt.technicalName,
                    physicalLabel = devEnt.physicalLabel,
                    alias = devEnt.alias,
                    ipAddress = devEnt.ipAddress,
                    macAddress = devEnt.macAddress,
                    siteId = devEnt.siteId,
                    areaId = devEnt.areaId,
                    ports = devPorts,
                    observation = devObs
                )
            }

            BusinessUnit(
                id = buEnt.id,
                name = buEnt.name,
                code = buEnt.code,
                sites = buSites,
                areas = buDirectAreas,
                devices = buDevices
            )
        }

        return Project(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            createdEpochMs = entity.createdEpochMs,
            updatedEpochMs = entity.updatedEpochMs,
            businessUnits = businessUnits
        )
    }
}
