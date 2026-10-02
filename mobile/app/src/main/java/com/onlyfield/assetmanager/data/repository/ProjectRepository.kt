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
import com.onlyfield.assetmanager.exchange.ProjectComparison
import com.onlyfield.assetmanager.exchange.ProjectComparisonEvaluator
import com.onlyfield.assetmanager.exchange.ProjectPackage
import com.onlyfield.assetmanager.exchange.XlsxExportManager
import com.onlyfield.assetmanager.export.PdfExportManager
import com.onlyfield.assetmanager.export.ProjectPrintDocumentAdapter
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

data class SearchResult(
    val device: Device,
    val businessUnitName: String,
    val siteName: String?,
    val areaName: String?,
    val matchedField: String,
)

data class PackageImportEvaluation(
    val importResult: PackageImportResult,
    val comparison: ProjectComparison?,
)

class ProjectRepository(
    private val db: AppDatabase,
) {
    private val projectDao = db.projectDao()
    private val inventoryDao = db.inventoryDao()

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

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

        return EntityMappers.toProject(
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

    suspend fun saveProject(project: Project) {
        db.withTransaction {
            val existing = projectDao.getProjectById(project.id)
            val updatedProjEntity = EntityMappers.toProjectEntity(project).copy(
                passwordHash = existing?.passwordHash
            )

            // Save project entity
            projectDao.insertProject(updatedProjEntity)

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

            for (cred in project.credentials) {
                credentialEntities.add(EntityMappers.toCredentialEntity(project.id, cred))
            }

            for (rack in project.racks) {
                rackEntities.add(EntityMappers.toRackEntity(project.id, rack))
            }

            for (model in project.deviceModels) {
                deviceModelEntities.add(EntityMappers.toDeviceModelEntity(project.id, model))
            }

            for (att in project.attachments) {
                attachmentEntities.add(EntityMappers.toAttachmentEntity(project.id, att))
            }

            for (ann in project.annotations) {
                annotationEntities.add(EntityMappers.toAnnotationEntity(project.id, ann))
            }

            for (placement in project.floorplanPlacements) {
                placementEntities.add(EntityMappers.toFloorplanPlacementEntity(project.id, placement))
            }

            for (segment in project.sharedPathSegments) {
                sharedPathSegmentEntities.add(EntityMappers.toSharedPathSegmentEntity(project.id, segment))
            }

            for (cable in project.cables) {
                cableEntities.add(EntityMappers.toCableEntity(project.id, cable))
            }

            for (mapping in project.panelMappings) {
                panelMappingEntities.add(EntityMappers.toPanelMappingEntity(project.id, mapping))
            }

            for (vlan in project.vlans) {
                vlanEntities.add(EntityMappers.toVlanEntity(project.id, vlan))
            }

            for (subnet in project.subnets) {
                subnetEntities.add(EntityMappers.toSubnetEntity(project.id, subnet))
            }

            for (membership in project.portVlanMemberships) {
                portVlanMembershipEntities.add(EntityMappers.toPortVlanMembershipEntity(project.id, membership))
            }

            for (l3Int in project.logicalInterfaces) {
                logicalInterfaceEntities.add(EntityMappers.toLogicalInterfaceEntity(project.id, l3Int))
            }

            for (lag in project.lagGroups) {
                lagGroupEntities.add(EntityMappers.toLagGroupEntity(project.id, lag))
            }

            for (config in project.deviceConfigurations) {
                deviceConfigurationEntities.add(EntityMappers.toDeviceConfigurationEntity(project.id, config))
            }

            for (conn in project.wanVpnConnections) {
                wanVpnConnectionEntities.add(EntityMappers.toWanVpnConnectionEntity(project.id, conn))
            }

            for (video in project.videoSurveillanceMappings) {
                videoSurveillanceMappingEntities.add(EntityMappers.toVideoSurveillanceMappingEntity(project.id, video))
            }

            for (field in project.customExtraFields) {
                customExtraFieldEntities.add(EntityMappers.toCustomExtraFieldEntity(project.id, field))
            }

            for (feed in project.powerFeeds) {
                powerFeedEntities.add(EntityMappers.toPowerFeedEntity(project.id, feed))
            }

            for (poe in project.poeMappings) {
                poeMappingEntities.add(EntityMappers.toPoeMappingEntity(project.id, poe))
            }

            for (badge in project.documentBadges) {
                documentBadgeEntities.add(EntityMappers.toDocumentBadgeEntity(project.id, badge))
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

    suspend fun saveRack(projectId: String, rack: Rack) {
        inventoryDao.insertRacks(listOf(EntityMappers.toRackEntity(projectId, rack)))
    }

    suspend fun saveDeviceModel(projectId: String, model: DeviceModel) {
        inventoryDao.insertDeviceModels(listOf(EntityMappers.toDeviceModelEntity(projectId, model)))
    }

    suspend fun exportRackPdfToStream(projectId: String, rackId: String, outputStream: OutputStream): Boolean {
        val project = getProjectById(projectId) ?: return false
        val rack = project.racks.find { it.id == rackId } ?: return false

        val allDevices = project.businessUnits.flatMap { it.devices }
        val devicesInRack = allDevices.filter { it.rackId == rackId }
        val unmountedDevices = allDevices.filter { (it.rackId == null) && (it.areaId == rack.areaId) }

        PdfExportManager.exportRackPdfToStream(
            project = project,
            rack = rack,
            devicesInRack = devicesInRack,
            unmountedDevices = unmountedDevices,
            outputStream = outputStream
        )
        return true
    }

    suspend fun exportXlsxToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream): Boolean {
        val project = getProjectById(projectId) ?: return false
        XlsxExportManager.exportXlsxToStream(project, filterConfig, outputStream)
        return true
    }

    suspend fun exportMarkdownToStream(projectId: String, filterConfig: ExportFilterConfig, outputStream: OutputStream): Boolean {
        val project = getProjectById(projectId) ?: return false
        MarkdownExportManager.exportMarkdownToStream(project, filterConfig, outputStream)
        return true
    }

    suspend fun exportCompositePdfToStream(
        projectId: String,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection,
        outputStream: OutputStream
    ): Boolean {
        val project = getProjectById(projectId) ?: return false
        PdfExportManager.exportCompositeReportPdfToStream(project, filterConfig, selection, outputStream)
        return true
    }

    suspend fun printProjectDocument(
        context: android.content.Context,
        projectId: String,
        filterConfig: ExportFilterConfig,
        selection: ReportSelection
    ): Boolean {
        val project = getProjectById(projectId) ?: return false
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager ?: return false
        val jobName = "Report_${project.name}"
        val adapter = ProjectPrintDocumentAdapter(project, filterConfig, selection)
        printManager.print(jobName, adapter, null)
        return true
    }

    fun exportDeviceModelToStream(model: DeviceModel, outputStream: OutputStream) {
        val jsonString = DeviceModelSerializer.serializeModel(model)
        outputStream.use { stream ->
            stream.write(jsonString.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    fun importDeviceModelFromStream(inputStream: InputStream): DeviceModel {
        val jsonString = inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        return DeviceModelSerializer.deserializeModel(jsonString)
    }

    suspend fun verifyProjectPassword(projectId: String, password: String): Boolean {
        val projEntity = projectDao.getProjectById(projectId) ?: return false
        if (!projEntity.isPasswordProtected) return true
        val storedHash = projEntity.passwordHash ?: return false
        return hashPassword(password) == storedHash
    }

    suspend fun setProjectPassword(projectId: String, currentPassword: String?, newPassword: String): Boolean {
        val projEntity = projectDao.getProjectById(projectId) ?: return false
        if (projEntity.isPasswordProtected) {
            if ((currentPassword == null) || (!verifyProjectPassword(projectId, currentPassword))) {
                return false
            }
        }
        val updated = projEntity.copy(
            isPasswordProtected = true,
            passwordHash = hashPassword(newPassword),
            updatedEpochMs = System.currentTimeMillis()
        )
        projectDao.updateProject(updated)
        return true
    }

    suspend fun removeProjectPassword(projectId: String, currentPassword: String): Boolean {
        val projEntity = projectDao.getProjectById(projectId) ?: return false
        if (!projEntity.isPasswordProtected) return true
        if (!verifyProjectPassword(projectId, currentPassword)) return false

        val updated = projEntity.copy(
            isPasswordProtected = false,
            passwordHash = null,
            updatedEpochMs = System.currentTimeMillis()
        )
        projectDao.updateProject(updated)
        return true
    }

    suspend fun exportProjectPackage(projectId: String, password: String? = null): ByteArray? {
        val project = getProjectById(projectId) ?: return null
        return PackageSerializer.exportPackage(project, password = password)
    }

    suspend fun exportProjectPackageToStream(
        projectId: String,
        outputStream: OutputStream,
        password: String? = null
    ): Boolean {
        val zipBytes = exportProjectPackage(projectId, password = password) ?: return false
        outputStream.use { stream ->
            stream.write(zipBytes)
            stream.flush()
        }
        return true
    }

    suspend fun evaluateImportPackage(
        inputStream: InputStream,
        password: String? = null,
        currentProjectId: String? = null
    ): PackageImportEvaluation {
        val bytes = inputStream.use { it.readBytes() }
        val importResult = PackageSerializer.importPackage(bytes, password = password)

        val pkg = importResult.pkg
        if ((pkg == null) || (!importResult.validationResult.isValid)) {
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

    suspend fun saveAttachment(projectId: String, attachment: com.onlyfield.assetmanager.core.model.Attachment) {
        inventoryDao.insertAttachments(listOf(EntityMappers.toAttachmentEntity(projectId, attachment)))
    }

    suspend fun deleteAttachment(attachmentId: String) {
        inventoryDao.deleteAttachmentById(attachmentId)
    }

    suspend fun saveAnnotation(projectId: String, annotation: com.onlyfield.assetmanager.core.model.Annotation) {
        inventoryDao.insertAnnotations(listOf(EntityMappers.toAnnotationEntity(projectId, annotation)))
    }

    suspend fun deleteAnnotation(annotationId: String) {
        inventoryDao.deleteAnnotationById(annotationId)
    }

    suspend fun savePlacement(projectId: String, placement: com.onlyfield.assetmanager.core.model.FloorplanPlacement) {
        inventoryDao.insertFloorplanPlacements(listOf(EntityMappers.toFloorplanPlacementEntity(projectId, placement)))
    }

    suspend fun deletePlacement(placementId: String) {
        inventoryDao.deleteFloorplanPlacementById(placementId)
    }

    suspend fun updateAreaFloorplan(projectId: String, areaId: String, attachmentId: String?, pageIndex: Int = 0) {
        val project = getProjectById(projectId) ?: return
        val updatedBus = project.businessUnits.map { bu ->
            val updatedSites = bu.sites.map { site ->
                val updatedAreas = site.areas.map { area ->
                    if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = pageIndex)
                    else area
                }
                site.copy(areas = updatedAreas)
            }
            val updatedDirectAreas = bu.areas.map { area ->
                if (area.id == areaId) area.copy(floorplanAttachmentId = attachmentId, floorplanPageIndex = pageIndex)
                else area
            }
            bu.copy(sites = updatedSites, areas = updatedDirectAreas)
        }
        val updatedProject = project.copy(
            businessUnits = updatedBus,
            updatedEpochMs = System.currentTimeMillis()
        )
        saveProject(updatedProject)
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

    suspend fun saveSharedPathSegment(projectId: String, segment: com.onlyfield.assetmanager.core.model.SharedPathSegment) {
        inventoryDao.insertSharedPathSegments(listOf(EntityMappers.toSharedPathSegmentEntity(projectId, segment)))
    }

    suspend fun deleteSharedPathSegment(segmentId: String) {
        inventoryDao.deleteSharedPathSegmentById(segmentId)
    }

    suspend fun saveCable(projectId: String, cable: com.onlyfield.assetmanager.core.model.Cable) {
        inventoryDao.insertCables(listOf(EntityMappers.toCableEntity(projectId, cable)))
    }

    suspend fun deleteCable(cableId: String) {
        inventoryDao.deleteCableById(cableId)
    }

    suspend fun savePanelMapping(projectId: String, mapping: com.onlyfield.assetmanager.core.model.PanelMapping) {
        inventoryDao.insertPanelMappings(listOf(EntityMappers.toPanelMappingEntity(projectId, mapping)))
    }

    suspend fun deletePanelMapping(mappingId: String) {
        inventoryDao.deletePanelMappingById(mappingId)
    }

    suspend fun saveVlan(projectId: String, vlan: com.onlyfield.assetmanager.core.model.Vlan) {
        inventoryDao.insertVlans(listOf(EntityMappers.toVlanEntity(projectId, vlan)))
    }

    suspend fun saveSubnet(projectId: String, subnet: com.onlyfield.assetmanager.core.model.Subnet) {
        inventoryDao.insertSubnets(listOf(EntityMappers.toSubnetEntity(projectId, subnet)))
    }

    suspend fun savePortVlanMembership(projectId: String, membership: com.onlyfield.assetmanager.core.model.PortVlanMembership) {
        inventoryDao.insertPortVlanMemberships(listOf(EntityMappers.toPortVlanMembershipEntity(projectId, membership)))
    }

    suspend fun saveLogicalInterface(projectId: String, l3Int: com.onlyfield.assetmanager.core.model.LogicalInterface) {
        inventoryDao.insertLogicalInterfaces(listOf(EntityMappers.toLogicalInterfaceEntity(projectId, l3Int)))
    }

    suspend fun saveLagGroup(projectId: String, lag: com.onlyfield.assetmanager.core.model.LagGroup) {
        inventoryDao.insertLagGroups(listOf(EntityMappers.toLagGroupEntity(projectId, lag)))
    }

    suspend fun saveDeviceConfiguration(projectId: String, config: com.onlyfield.assetmanager.core.model.DeviceConfiguration) {
        inventoryDao.insertDeviceConfigurations(listOf(EntityMappers.toDeviceConfigurationEntity(projectId, config)))
    }

    suspend fun saveWanVpnConnection(projectId: String, conn: com.onlyfield.assetmanager.core.model.WanVpnConnection) {
        inventoryDao.insertWanVpnConnections(listOf(EntityMappers.toWanVpnConnectionEntity(projectId, conn)))
    }

    suspend fun saveVideoSurveillanceMapping(projectId: String, video: com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping) {
        inventoryDao.insertVideoSurveillanceMappings(listOf(EntityMappers.toVideoSurveillanceMappingEntity(projectId, video)))
    }

    suspend fun saveCustomExtraField(projectId: String, field: com.onlyfield.assetmanager.core.model.CustomExtraField) {
        inventoryDao.insertCustomExtraFields(listOf(EntityMappers.toCustomExtraFieldEntity(projectId, field)))
    }

    fun traceCableChain(project: Project, startPortId: String): List<ChainStep> {
        val steps = mutableListOf<ChainStep>()
        val visitedPortIds = mutableSetOf<String>()

        val allPortsMap = mutableMapOf<String, Pair<com.onlyfield.assetmanager.core.model.Port, Device>>()
        for (bu in project.businessUnits) {
            for (dev in bu.devices) {
                for (port in dev.ports) {
                    allPortsMap[port.id] = port to dev
                }
            }
        }

        var currentPortId: String? = startPortId
        var stepIndex = 1

        while (currentPortId != null && !visitedPortIds.contains(currentPortId)) {
            visitedPortIds.add(currentPortId)

            val (currentPort, currentDevice) = allPortsMap[currentPortId] ?: Pair(null, null)

            // Look for a cable connected to currentPortId
            val cable = project.cables.find { it.portAId == currentPortId || it.portBId == currentPortId }

            if (cable != null) {
                val nextPortId = if (cable.portAId == currentPortId) cable.portBId else cable.portAId
                val (nextPort, nextDevice) = nextPortId?.let { allPortsMap[it] } ?: Pair(null, null)

                val sharedPathNames = cable.sharedPathSegmentIds.mapNotNull { segId ->
                    project.sharedPathSegments.find { it.id == segId }?.name
                }
                val pathInfo = if (sharedPathNames.isNotEmpty()) " [Percorso: ${sharedPathNames.joinToString(", ")}]" else ""
                val orientInfo = if (cable.orientation != com.onlyfield.assetmanager.core.model.CableOrientation.NONE) " [Orientamento: ${cable.orientation}]" else ""

                val desc = if (nextPort != null && nextDevice != null) {
                    "Cavo '${cable.codeOrLabel ?: "Senza Etichetta"}' (${cable.medium}, ${cable.observedSpeed ?: "velocità N/D"})$pathInfo$orientInfo -> Porta '${nextPort.name}' su '${nextDevice.technicalName}'"
                } else {
                    "Cavo '${cable.codeOrLabel ?: "Senza Etichetta"}' (${cable.medium})$pathInfo$orientInfo -> Estremità scollegata / da verificare"
                }

                steps.add(
                    ChainStep(
                        stepIndex = stepIndex++,
                        currentPort = currentPort,
                        currentDevice = currentDevice,
                        cable = cable,
                        panelMapping = null,
                        isUnknownPassage = nextPortId == null,
                        description = desc
                    )
                )

                if (nextPortId == null) {
                    break
                }

                currentPortId = nextPortId

                val mapping = project.panelMappings.find { it.portAId == currentPortId || it.portBId == currentPortId }
                if (mapping != null) {
                    if (mapping.isUnknownPassage || mapping.portBId == null) {
                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = nextPort,
                                currentDevice = nextDevice,
                                cable = null,
                                panelMapping = mapping,
                                isUnknownPassage = true,
                                description = "Passaggio ignoto / Ponte intermedio su '${nextDevice?.technicalName ?: "Pannello"}'"
                            )
                        )
                        break
                    } else {
                        val mappedPortId = if (mapping.portAId == currentPortId) mapping.portBId else mapping.portAId
                        val (mappedPort, mappedDevice) = mappedPortId?.let { allPortsMap[it] } ?: Pair(null, null)

                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = nextPort,
                                currentDevice = nextDevice,
                                cable = null,
                                panelMapping = mapping,
                                isUnknownPassage = false,
                                description = "Mapping Pannello (${mapping.mappingType}) -> Porta '${mappedPort?.name ?: "N/D"}' su '${mappedDevice?.technicalName ?: "Pannello"}'"
                            )
                        )

                        if (mappedPortId != null && !visitedPortIds.contains(mappedPortId)) {
                            currentPortId = mappedPortId
                        } else {
                            break
                        }
                    }
                } else {
                    val nextCable = project.cables.find { (it.portAId == currentPortId || it.portBId == currentPortId) && it.id != cable.id }
                    if (nextCable == null) {
                        break
                    }
                }
            } else {
                val mapping = project.panelMappings.find { it.portAId == currentPortId || it.portBId == currentPortId }
                if (mapping != null) {
                    if (mapping.isUnknownPassage || mapping.portBId == null) {
                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = currentPort,
                                currentDevice = currentDevice,
                                cable = null,
                                panelMapping = mapping,
                                isUnknownPassage = true,
                                description = "Passaggio ignoto / Ponte intermedio su '${currentDevice?.technicalName ?: "Pannello"}'"
                            )
                        )
                        break
                    } else {
                        val mappedPortId = if (mapping.portAId == currentPortId) mapping.portBId else mapping.portAId
                        val (mappedPort, mappedDevice) = mappedPortId?.let { allPortsMap[it] } ?: Pair(null, null)

                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = currentPort,
                                currentDevice = currentDevice,
                                cable = null,
                                panelMapping = mapping,
                                isUnknownPassage = false,
                                description = "Mapping Pannello (${mapping.mappingType}) -> Porta '${mappedPort?.name ?: "N/D"}' su '${mappedDevice?.technicalName ?: "Pannello"}'"
                            )
                        )

                        if (mappedPortId != null && !visitedPortIds.contains(mappedPortId)) {
                            currentPortId = mappedPortId
                        } else {
                            break
                        }
                    }
                } else {
                    if (steps.isEmpty()) {
                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = currentPort,
                                currentDevice = currentDevice,
                                cable = null,
                                panelMapping = null,
                                isUnknownPassage = false,
                                description = "Porta '${currentPort?.name}' su '${currentDevice?.technicalName}' (nessun cavo collegato)"
                            )
                        )
                    }
                    break
                }
            }
        }

        return steps
    }

    private val jsonSerializer = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val sessionUndoStack = mutableListOf<suspend () -> Unit>()

    fun canUndo(): Boolean = sessionUndoStack.isNotEmpty()

    suspend fun performUndo() {
        if (sessionUndoStack.isNotEmpty()) {
            val lastAction = sessionUndoStack.removeAt(sessionUndoStack.lastIndex)
            lastAction.invoke()
        }
    }

    suspend fun getTrashItems(projectId: String): List<com.onlyfield.assetmanager.core.model.TrashItem> {
        return inventoryDao.getTrashItemsByProjectId(projectId).map { EntityMappers.toTrashItem(it) }
    }

    suspend fun emptyTrash(projectId: String) {
        inventoryDao.emptyTrashByProjectId(projectId)
    }

    suspend fun deleteTrashItemPermanently(trashId: String) {
        inventoryDao.deleteTrashItemById(trashId)
    }

    suspend fun moveToTrash(projectId: String, itemType: String, itemId: String): com.onlyfield.assetmanager.core.model.TrashItem? {
        val project = getProjectById(projectId) ?: return null
        var trashItem: com.onlyfield.assetmanager.core.model.TrashItem? = null

        db.withTransaction {
            when (itemType.uppercase()) {
                "DEVICE" -> {
                    val device = project.businessUnits.flatMap { it.devices }.find { it.id == itemId } ?: return@withTransaction
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
                            inventoryDao.insertCables(listOf(EntityMappers.toCableEntity(projectId, newCable)))
                        }
                    }

                    val summary = "Porte: ${device.ports.size}, Cavi scollegati: ${updatedCables.size}"
                    trashItem = com.onlyfield.assetmanager.core.model.TrashItem(
                        projectId = projectId,
                        itemType = "DEVICE",
                        itemId = itemId,
                        displayName = device.technicalName,
                        serializedJson = jsonStr,
                        affectedReferencesSummary = summary
                    )
                    inventoryDao.insertTrashItems(listOf(EntityMappers.toTrashItemEntity(trashItem!!)))
                    inventoryDao.deletePortsByDeviceId(itemId)
                    inventoryDao.deleteDeviceById(itemId)
                }
                "RACK" -> {
                    val rack = project.racks.find { it.id == itemId } ?: return@withTransaction
                    val jsonStr = jsonSerializer.encodeToString(com.onlyfield.assetmanager.core.model.Rack.serializer(), rack)
                    val devicesInRack = project.businessUnits.flatMap { it.devices }.filter { it.rackId == itemId }

                    for (dev in devicesInRack) {
                        val unassignedDev = dev.copy(rackId = null, positionU = null)
                        val devBU = project.businessUnits.find { bu -> bu.devices.any { it.id == dev.id } }
                        if (devBU != null) {
                            inventoryDao.insertDevices(listOf(EntityMappers.toDeviceEntity(devBU.id, unassignedDev)))
                        }
                    }

                    val summary = "Apparati dislocati dal rack: ${devicesInRack.size}"
                    trashItem = com.onlyfield.assetmanager.core.model.TrashItem(
                        projectId = projectId,
                        itemType = "RACK",
                        itemId = itemId,
                        displayName = rack.name,
                        serializedJson = jsonStr,
                        affectedReferencesSummary = summary
                    )
                    inventoryDao.insertTrashItems(listOf(EntityMappers.toTrashItemEntity(trashItem!!)))
                    inventoryDao.deleteRackById(itemId)
                }
                "CREDENTIAL" -> {
                    val cred = project.credentials.find { it.id == itemId } ?: return@withTransaction
                    val jsonStr = jsonSerializer.encodeToString(com.onlyfield.assetmanager.core.model.Credential.serializer(), cred)
                    trashItem = com.onlyfield.assetmanager.core.model.TrashItem(
                        projectId = projectId,
                        itemType = "CREDENTIAL",
                        itemId = itemId,
                        displayName = cred.username,
                        serializedJson = jsonStr,
                        affectedReferencesSummary = "Credenziale per utente ${cred.username}"
                    )
                    inventoryDao.insertTrashItems(listOf(EntityMappers.toTrashItemEntity(trashItem!!)))
                    inventoryDao.deleteCredentialById(itemId)
                }
            }
        }

        if (trashItem != null) {
            sessionUndoStack.add {
                restoreFromTrash(projectId, trashItem!!.id)
            }
        }
        return trashItem
    }

    suspend fun restoreFromTrash(projectId: String, trashId: String): Boolean {
        val trashEntity = inventoryDao.getTrashItemById(trashId) ?: return false
        val trashItem = EntityMappers.toTrashItem(trashEntity)

        db.withTransaction {
            when (trashItem.itemType.uppercase()) {
                "DEVICE" -> {
                    val device = jsonSerializer.decodeFromString(com.onlyfield.assetmanager.core.model.Device.serializer(), trashItem.serializedJson)
                    val project = getProjectById(projectId)
                    val buId = project?.businessUnits?.firstOrNull()?.id ?: return@withTransaction
                    inventoryDao.insertDevices(listOf(EntityMappers.toDeviceEntity(buId, device)))
                    if (device.ports.isNotEmpty()) {
                        val portEntities = device.ports.map { EntityMappers.toPortEntity(it) }
                        inventoryDao.insertPorts(portEntities)
                    }
                    inventoryDao.deleteTrashItemById(trashId)
                }
                "RACK" -> {
                    val rack = jsonSerializer.decodeFromString(com.onlyfield.assetmanager.core.model.Rack.serializer(), trashItem.serializedJson)
                    inventoryDao.insertRacks(listOf(EntityMappers.toRackEntity(projectId, rack)))
                    inventoryDao.deleteTrashItemById(trashId)
                }
                "CREDENTIAL" -> {
                    val cred = jsonSerializer.decodeFromString(com.onlyfield.assetmanager.core.model.Credential.serializer(), trashItem.serializedJson)
                    inventoryDao.insertCredentials(listOf(EntityMappers.toCredentialEntity(projectId, cred)))
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
            inventoryDao.insertDevices(listOf(EntityMappers.toDeviceEntity(bu.id, newDevice)))
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
            inventoryDao.insertDevices(listOf(EntityMappers.toDeviceEntity(survivingBuId, updatedSurvivingDevice)))
            if (updatedSurvivingDevice.ports.isNotEmpty()) {
                inventoryDao.insertPorts(updatedSurvivingDevice.ports.map { EntityMappers.toPortEntity(it) })
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
                    inventoryDao.insertDevices(listOf(EntityMappers.toDeviceEntity(buId, dev)))
                }
            }
        }
    }
}

data class ChainStep(
    val stepIndex: Int,
    val currentPort: com.onlyfield.assetmanager.core.model.Port?,
    val currentDevice: Device?,
    val cable: com.onlyfield.assetmanager.core.model.Cable?,
    val panelMapping: com.onlyfield.assetmanager.core.model.PanelMapping?,
    val isUnknownPassage: Boolean = false,
    val description: String,
)
