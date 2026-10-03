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

/** Reads and writes a whole [Project] as Room rows (one row set per entity type). */
internal class ProjectStore(private val db: AppDatabase) {
    private val projectDao = db.projectDao()
    private val inventoryDao = db.inventoryDao()

    suspend fun load(projectId: String): Project? {
        val projEntity = projectDao.getProjectById(projectId) ?: return null
        val buEntities = inventoryDao.getBusinessUnitsByProjectId(projectId)
        val buIds = buEntities.map { it.id }

        val siteEntities = if (buIds.isNotEmpty()) inventoryDao.getSitesByBuIds(buIds) else emptyList()
        val areaEntities = if (buIds.isNotEmpty()) inventoryDao.getAreasByBuIds(buIds) else emptyList()
        val deviceEntities = if (buIds.isNotEmpty()) inventoryDao.getDevicesByBuIds(buIds) else emptyList()
        val devIds = deviceEntities.map { it.id }
        val portEntities = if (devIds.isNotEmpty()) inventoryDao.getPortsByDeviceIds(devIds) else emptyList()
        val credentialEntities = inventoryDao.getCredentialsByProjectId(projectId)
        val rackEntities = inventoryDao.getRacksByProjectId(projectId)
        val deviceModelEntities = inventoryDao.getDeviceModelsByProjectId(projectId)
        val attachmentEntities = inventoryDao.getAttachmentsByProjectId(projectId)
        val annotationEntities = inventoryDao.getAnnotationsByProjectId(projectId)
        val placementEntities = inventoryDao.getFloorplanPlacementsByProjectId(projectId)
        val sharedPathSegmentEntities = inventoryDao.getSharedPathSegmentsByProjectId(projectId)
        val cableEntities = inventoryDao.getCablesByProjectId(projectId)
        val panelMappingEntities = inventoryDao.getPanelMappingsByProjectId(projectId)
        val vlanEntities = inventoryDao.getVlansByProjectId(projectId)
        val subnetEntities = inventoryDao.getSubnetsByProjectId(projectId)
        val portVlanMembershipEntities = inventoryDao.getPortVlanMembershipsByProjectId(projectId)
        val logicalInterfaceEntities = inventoryDao.getLogicalInterfacesByProjectId(projectId)
        val lagGroupEntities = inventoryDao.getLagGroupsByProjectId(projectId)
        val deviceConfigurationEntities = inventoryDao.getDeviceConfigurationsByProjectId(projectId)
        val wanVpnConnectionEntities = inventoryDao.getWanVpnConnectionsByProjectId(projectId)
        val videoSurveillanceMappingEntities = inventoryDao.getVideoSurveillanceMappingsByProjectId(projectId)
        val customExtraFieldEntities = inventoryDao.getCustomExtraFieldsByProjectId(projectId)
        val powerFeedEntities = inventoryDao.getPowerFeedsByProjectId(projectId)
        val poeMappingEntities = inventoryDao.getPoeMappingsByProjectId(projectId)
        val documentBadgeEntities = inventoryDao.getDocumentBadgesByProjectId(projectId)

        return toProject(
            entity = projEntity,
            buEntities = buEntities,
            siteEntities = siteEntities,
            areaEntities = areaEntities,
            deviceEntities = deviceEntities,
            portEntities = portEntities,
            credentialEntities = credentialEntities,
            rackEntities = rackEntities,
            deviceModelEntities = deviceModelEntities,
            attachmentEntities = attachmentEntities,
            annotationEntities = annotationEntities,
            placementEntities = placementEntities,
            sharedPathSegmentEntities = sharedPathSegmentEntities,
            cableEntities = cableEntities,
            panelMappingEntities = panelMappingEntities,
            vlanEntities = vlanEntities,
            subnetEntities = subnetEntities,
            portVlanMembershipEntities = portVlanMembershipEntities,
            logicalInterfaceEntities = logicalInterfaceEntities,
            lagGroupEntities = lagGroupEntities,
            deviceConfigurationEntities = deviceConfigurationEntities,
            wanVpnConnectionEntities = wanVpnConnectionEntities,
            videoSurveillanceMappingEntities = videoSurveillanceMappingEntities,
            customExtraFieldEntities = customExtraFieldEntities,
            powerFeedEntities = powerFeedEntities,
            poeMappingEntities = poeMappingEntities,
            documentBadgeEntities = documentBadgeEntities
        )
    }

    /** Replaces every row of the project in one transaction; the password hash is kept. */
    suspend fun save(project: Project) {
        db.withTransaction {
            val existing = projectDao.getProjectById(project.id)
            val updatedProjEntity = toProjectEntity(project).copy(
                passwordHash = existing?.passwordHash
            )

            // Save project entity
            if (existing == null) projectDao.insertProject(updatedProjEntity) else projectDao.updateProject(updatedProjEntity)

            // Delete existing inventory tree & all related entities
            inventoryDao.deleteBusinessUnitsByProjectId(project.id)
            inventoryDao.deleteCredentialsByProjectId(project.id)
            inventoryDao.deleteRacksByProjectId(project.id)
            inventoryDao.deleteDeviceModelsByProjectId(project.id)
            inventoryDao.deleteAttachmentsByProjectId(project.id)
            inventoryDao.deleteAnnotationsByProjectId(project.id)
            inventoryDao.deleteFloorplanPlacementsByProjectId(project.id)
            inventoryDao.deleteSharedPathSegmentsByProjectId(project.id)
            inventoryDao.deleteCablesByProjectId(project.id)
            inventoryDao.deletePanelMappingsByProjectId(project.id)
            inventoryDao.deleteVlansByProjectId(project.id)
            inventoryDao.deleteSubnetsByProjectId(project.id)
            inventoryDao.deletePortVlanMembershipsByProjectId(project.id)
            inventoryDao.deleteLogicalInterfacesByProjectId(project.id)
            inventoryDao.deleteLagGroupsByProjectId(project.id)
            inventoryDao.deleteDeviceConfigurationsByProjectId(project.id)
            inventoryDao.deleteWanVpnConnectionsByProjectId(project.id)
            inventoryDao.deleteVideoSurveillanceMappingsByProjectId(project.id)
            inventoryDao.deleteCustomExtraFieldsByProjectId(project.id)
            inventoryDao.deletePowerFeedsByProjectId(project.id)
            inventoryDao.deletePoeMappingsByProjectId(project.id)
            inventoryDao.deleteDocumentBadgesByProjectId(project.id)

            val buEntities = mutableListOf<BusinessUnitEntity>()
            val siteEntities = mutableListOf<SiteEntity>()
            val areaEntities = mutableListOf<AreaEntity>()
            val deviceEntities = mutableListOf<DeviceEntity>()
            val portEntities = mutableListOf<PortEntity>()
            val credentialEntities = mutableListOf<CredentialEntity>()
            val rackEntities = mutableListOf<RackEntity>()
            val deviceModelEntities = mutableListOf<DeviceModelEntity>()
            val attachmentEntities = mutableListOf<com.onlyfield.assetmanager.data.local.AttachmentEntity>()
            val annotationEntities = mutableListOf<com.onlyfield.assetmanager.data.local.AnnotationEntity>()
            val placementEntities = mutableListOf<com.onlyfield.assetmanager.data.local.FloorplanPlacementEntity>()
            val sharedPathSegmentEntities = mutableListOf<com.onlyfield.assetmanager.data.local.SharedPathSegmentEntity>()
            val cableEntities = mutableListOf<com.onlyfield.assetmanager.data.local.CableEntity>()
            val panelMappingEntities = mutableListOf<com.onlyfield.assetmanager.data.local.PanelMappingEntity>()
            val vlanEntities = mutableListOf<com.onlyfield.assetmanager.data.local.VlanEntity>()
            val subnetEntities = mutableListOf<com.onlyfield.assetmanager.data.local.SubnetEntity>()
            val portVlanMembershipEntities = mutableListOf<com.onlyfield.assetmanager.data.local.PortVlanMembershipEntity>()
            val logicalInterfaceEntities = mutableListOf<com.onlyfield.assetmanager.data.local.LogicalInterfaceEntity>()
            val lagGroupEntities = mutableListOf<com.onlyfield.assetmanager.data.local.LagGroupEntity>()
            val deviceConfigurationEntities = mutableListOf<com.onlyfield.assetmanager.data.local.DeviceConfigurationEntity>()
            val wanVpnConnectionEntities = mutableListOf<com.onlyfield.assetmanager.data.local.WanVpnConnectionEntity>()
            val videoSurveillanceMappingEntities = mutableListOf<com.onlyfield.assetmanager.data.local.VideoSurveillanceMappingEntity>()
            val customExtraFieldEntities = mutableListOf<com.onlyfield.assetmanager.data.local.CustomExtraFieldEntity>()
            val powerFeedEntities = mutableListOf<com.onlyfield.assetmanager.data.local.PowerFeedEntity>()
            val poeMappingEntities = mutableListOf<com.onlyfield.assetmanager.data.local.PoeMappingEntity>()
            val documentBadgeEntities = mutableListOf<com.onlyfield.assetmanager.data.local.DocumentBadgeEntity>()

            for (bu in project.businessUnits) {
                buEntities.add(toBusinessUnitEntity(project.id, bu))

                for (site in bu.sites) {
                    siteEntities.add(toSiteEntity(bu.id, site))
                    for (area in site.areas) {
                        areaEntities.add(toAreaEntity(bu.id, site.id, area))
                    }
                }

                for (area in bu.areas) {
                    areaEntities.add(toAreaEntity(bu.id, null, area))
                }

                for (device in bu.devices) {
                    deviceEntities.add(toDeviceEntity(bu.id, device))
                    for (port in device.ports) {
                        portEntities.add(toPortEntity(port))
                    }
                }
            }

            for (cred in project.credentials) {
                credentialEntities.add(toCredentialEntity(project.id, cred))
            }

            for (rack in project.racks) {
                rackEntities.add(toRackEntity(project.id, rack))
            }

            for (model in project.deviceModels) {
                deviceModelEntities.add(toDeviceModelEntity(project.id, model))
            }

            for (att in project.attachments) {
                attachmentEntities.add(toAttachmentEntity(project.id, att))
            }

            for (ann in project.annotations) {
                annotationEntities.add(toAnnotationEntity(project.id, ann))
            }

            for (placement in project.floorplanPlacements) {
                placementEntities.add(toFloorplanPlacementEntity(project.id, placement))
            }

            for (segment in project.sharedPathSegments) {
                sharedPathSegmentEntities.add(toSharedPathSegmentEntity(project.id, segment))
            }

            for (cable in project.cables) {
                cableEntities.add(toCableEntity(project.id, cable))
            }

            for (mapping in project.panelMappings) {
                panelMappingEntities.add(toPanelMappingEntity(project.id, mapping))
            }

            for (vlan in project.vlans) {
                vlanEntities.add(toVlanEntity(project.id, vlan))
            }

            for (subnet in project.subnets) {
                subnetEntities.add(toSubnetEntity(project.id, subnet))
            }

            for (membership in project.portVlanMemberships) {
                portVlanMembershipEntities.add(toPortVlanMembershipEntity(project.id, membership))
            }

            for (l3Int in project.logicalInterfaces) {
                logicalInterfaceEntities.add(toLogicalInterfaceEntity(project.id, l3Int))
            }

            for (lag in project.lagGroups) {
                lagGroupEntities.add(toLagGroupEntity(project.id, lag))
            }

            for (config in project.deviceConfigurations) {
                deviceConfigurationEntities.add(toDeviceConfigurationEntity(project.id, config))
            }

            for (conn in project.wanVpnConnections) {
                wanVpnConnectionEntities.add(toWanVpnConnectionEntity(project.id, conn))
            }

            for (video in project.videoSurveillanceMappings) {
                videoSurveillanceMappingEntities.add(toVideoSurveillanceMappingEntity(project.id, video))
            }

            for (field in project.customExtraFields) {
                customExtraFieldEntities.add(toCustomExtraFieldEntity(project.id, field))
            }

            for (feed in project.powerFeeds) {
                powerFeedEntities.add(toPowerFeedEntity(project.id, feed))
            }

            for (poe in project.poeMappings) {
                poeMappingEntities.add(toPoeMappingEntity(project.id, poe))
            }

            for (badge in project.documentBadges) {
                documentBadgeEntities.add(toDocumentBadgeEntity(project.id, badge))
            }

            if (buEntities.isNotEmpty()) inventoryDao.insertBusinessUnits(buEntities)
            if (siteEntities.isNotEmpty()) inventoryDao.insertSites(siteEntities)
            if (areaEntities.isNotEmpty()) inventoryDao.insertAreas(areaEntities)
            if (deviceEntities.isNotEmpty()) inventoryDao.insertDevices(deviceEntities)
            if (portEntities.isNotEmpty()) inventoryDao.insertPorts(portEntities)
            if (credentialEntities.isNotEmpty()) inventoryDao.insertCredentials(credentialEntities)
            if (rackEntities.isNotEmpty()) inventoryDao.insertRacks(rackEntities)
            if (deviceModelEntities.isNotEmpty()) inventoryDao.insertDeviceModels(deviceModelEntities)
            if (attachmentEntities.isNotEmpty()) inventoryDao.insertAttachments(attachmentEntities)
            if (annotationEntities.isNotEmpty()) inventoryDao.insertAnnotations(annotationEntities)
            if (placementEntities.isNotEmpty()) inventoryDao.insertFloorplanPlacements(placementEntities)
            if (sharedPathSegmentEntities.isNotEmpty()) inventoryDao.insertSharedPathSegments(sharedPathSegmentEntities)
            if (cableEntities.isNotEmpty()) inventoryDao.insertCables(cableEntities)
            if (panelMappingEntities.isNotEmpty()) inventoryDao.insertPanelMappings(panelMappingEntities)
            if (vlanEntities.isNotEmpty()) inventoryDao.insertVlans(vlanEntities)
            if (subnetEntities.isNotEmpty()) inventoryDao.insertSubnets(subnetEntities)
            if (portVlanMembershipEntities.isNotEmpty()) inventoryDao.insertPortVlanMemberships(portVlanMembershipEntities)
            if (logicalInterfaceEntities.isNotEmpty()) inventoryDao.insertLogicalInterfaces(logicalInterfaceEntities)
            if (lagGroupEntities.isNotEmpty()) inventoryDao.insertLagGroups(lagGroupEntities)
            if (deviceConfigurationEntities.isNotEmpty()) inventoryDao.insertDeviceConfigurations(deviceConfigurationEntities)
            if (wanVpnConnectionEntities.isNotEmpty()) inventoryDao.insertWanVpnConnections(wanVpnConnectionEntities)
            if (videoSurveillanceMappingEntities.isNotEmpty()) inventoryDao.insertVideoSurveillanceMappings(videoSurveillanceMappingEntities)
            if (customExtraFieldEntities.isNotEmpty()) inventoryDao.insertCustomExtraFields(customExtraFieldEntities)
            if (powerFeedEntities.isNotEmpty()) inventoryDao.insertPowerFeeds(powerFeedEntities)
            if (poeMappingEntities.isNotEmpty()) inventoryDao.insertPoeMappings(poeMappingEntities)
            if (documentBadgeEntities.isNotEmpty()) inventoryDao.insertDocumentBadges(documentBadgeEntities)
        }
    }

    /** Stores [project] as the merge base: the state the other device now has. */
    suspend fun saveBase(project: Project) {
        val json = PackageSerializer.jsonConfig.encodeToString(Project.serializer(), project)
        projectDao.saveSyncSnapshot(com.onlyfield.assetmanager.data.local.SyncSnapshotEntity(project.id, json, System.currentTimeMillis()))
    }

    suspend fun loadBase(projectId: String): Project? = projectDao.getSyncSnapshot(projectId)?.let {
        runCatching { PackageSerializer.jsonConfig.decodeFromString(Project.serializer(), it.projectJson) }.getOrNull()
    }
}
