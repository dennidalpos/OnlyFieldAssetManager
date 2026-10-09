package com.onlyfield.assetmanager.data.repository

import com.onlyfield.assetmanager.data.repository.mappers.*
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.local.AreaEntity
import com.onlyfield.assetmanager.data.local.SiteEntity
import com.onlyfield.assetmanager.data.local.CredentialEntity
import com.onlyfield.assetmanager.data.local.DeviceEntity
import com.onlyfield.assetmanager.data.local.DeviceModelEntity
import com.onlyfield.assetmanager.data.local.PortEntity
import com.onlyfield.assetmanager.data.local.RackEntity
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads and writes a whole [Project] as Room rows (one row set per entity type). */
internal class ProjectStore(private val db: AppDatabase) {
    private val projectDao = db.projectDao()
    private val inventoryDao = db.inventoryDao()

    suspend fun load(projectId: String): Project? = db.withTransaction {
        val projEntity = projectDao.getProjectById(projectId) ?: return@withTransaction null
        val siteEntities = inventoryDao.getSitesByProjectId(projectId)
        val siteIds = siteEntities.map { it.id }

        val areaEntities = if (siteIds.isNotEmpty()) inventoryDao.getAreasBySiteIds(siteIds) else emptyList()
        val deviceEntities = if (siteIds.isNotEmpty()) inventoryDao.getDevicesBySiteIds(siteIds) else emptyList()
        val devIds = deviceEntities.map { it.id }
        val portEntities = if (devIds.isNotEmpty()) inventoryDao.getPortsByDeviceIds(devIds) else emptyList()
        val credentialEntities = inventoryDao.getCredentialsByProjectId(projectId)
        val rackEntities = inventoryDao.getRacksByProjectId(projectId)
        val deviceModelEntities = inventoryDao.getDeviceModelsByProjectId(projectId)
        val attachmentEntities = inventoryDao.getAttachmentsByProjectId(projectId)
        val annotationEntities = inventoryDao.getAnnotationsByProjectId(projectId)
        val placementEntities = inventoryDao.getFloorplanPlacementsByProjectId(projectId)
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

        toProject(
            entity = projEntity,
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

    /** Initializes import protection before replacing any stored data. */
    suspend fun saveImported(project: Project, password: String?) {
        val hash = if (project.isPasswordProtected) {
            require(!password.isNullOrBlank()) { "A protected import requires its package password" }
            withContext(Dispatchers.Default) { PasswordHasher.hash(password) }
        } else null
        save(project, hash)
    }

    /** Replaces the project atomically; ordinary edits retain its verifier. */
    suspend fun save(project: Project, importedHash: String? = null) {
        com.onlyfield.assetmanager.core.validation.ModelValidator.requireFiniteNumbers(project)
        db.withTransaction {
            checkOwnership(project)
            val existing = projectDao.getProjectById(project.id)
            val updatedProjEntity = toProjectEntity(project).copy(
                passwordHash = if (project.isPasswordProtected) importedHash ?: existing?.passwordHash else null
            )

            // Save project entity
            if (existing == null) projectDao.insertProject(updatedProjEntity) else projectDao.updateProject(updatedProjEntity)

            // Delete existing inventory tree & all related entities
            inventoryDao.deleteSitesByProjectId(project.id)
            inventoryDao.deleteCredentialsByProjectId(project.id)
            inventoryDao.deleteRacksByProjectId(project.id)
            inventoryDao.deleteDeviceModelsByProjectId(project.id)
            inventoryDao.deleteAttachmentsByProjectId(project.id)
            inventoryDao.deleteAnnotationsByProjectId(project.id)
            inventoryDao.deleteFloorplanPlacementsByProjectId(project.id)
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

            for (site in project.sites) {
                siteEntities.add(toSiteEntity(project.id, site))

                for (area in site.areas) {
                    areaEntities.add(toAreaEntity(site.id, area))
                }

                for (device in site.devices) {
                    deviceEntities.add(toDeviceEntity(site.id, device))
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

    /** Check global primary keys inside the write transaction, before any deletion. */
    private fun checkOwnership(project: Project) {
        fun checkIds(table: String, ids: List<String>, joins: String = "", owner: String = "r.projectId") {
            for (chunk in ids.distinct().chunked(900)) {
                val placeholders = List(chunk.size) { "?" }.joinToString(",")
                val query = SimpleSQLiteQuery(
                    "SELECT r.id FROM $table r $joins WHERE r.id IN ($placeholders) AND ($owner IS NULL OR $owner != ?) LIMIT 1",
                    (chunk + project.id).toTypedArray(),
                )
                db.openHelper.readableDatabase.query(query).use { cursor ->
                    check(!cursor.moveToFirst()) { Messages().text("storage.idConflict", cursor.getString(0)) }
                }
            }
        }
        checkIds("sites", project.sites.map { it.id })
        checkIds("areas", project.sites.flatMap { it.areas }.map { it.id }, "LEFT JOIN sites s ON s.id = r.siteId", "s.projectId")
        val devices = project.sites.flatMap { it.devices }
        checkIds("devices", devices.map { it.id }, "LEFT JOIN sites s ON s.id = r.siteId", "s.projectId")
        checkIds("ports", devices.flatMap { it.ports }.map { it.id },
            "LEFT JOIN devices d ON d.id = r.deviceId LEFT JOIN sites s ON s.id = d.siteId", "s.projectId")
        for ((table, ids) in listOf(
            "credentials" to project.credentials.map { it.id },
            "racks" to project.racks.map { it.id },
            "device_models" to project.deviceModels.map { it.id },
            "attachments" to project.attachments.map { it.id },
            "annotations" to project.annotations.map { it.id },
            "floorplan_placements" to project.floorplanPlacements.map { it.id },
            "cables" to project.cables.map { it.id },
            "panel_mappings" to project.panelMappings.map { it.id },
            "vlans" to project.vlans.map { it.id },
            "subnets" to project.subnets.map { it.id },
            "port_vlan_memberships" to project.portVlanMemberships.map { it.id },
            "logical_interfaces" to project.logicalInterfaces.map { it.id },
            "lag_groups" to project.lagGroups.map { it.id },
            "device_configurations" to project.deviceConfigurations.map { it.id },
            "wan_vpn_connections" to project.wanVpnConnections.map { it.id },
            "video_surveillance_mappings" to project.videoSurveillanceMappings.map { it.id },
            "custom_extra_fields" to project.customExtraFields.map { it.id },
            "power_feeds" to project.powerFeeds.map { it.id },
            "poe_mappings" to project.poeMappings.map { it.id },
            "document_badges" to project.documentBadges.map { it.id },
        )) checkIds(table, ids)
    }

    /** Stores [project] as the merge base: the state the other device now has. */
    suspend fun saveBase(project: Project) = db.withTransaction {
        val json = PackageSerializer.jsonConfig.encodeToString(Project.serializer(), project)
        val previous = projectDao.getSyncSnapshot(project.id)?.savedEpochMs ?: 0L
        val saved = maxOf(System.currentTimeMillis(), Math.addExact(previous, 1L))
        projectDao.saveSyncSnapshot(com.onlyfield.assetmanager.data.local.SyncSnapshotEntity(project.id, json, saved))
    }

    suspend fun loadBase(projectId: String): Project? = projectDao.getSyncSnapshot(projectId)?.let {
        runCatching { PackageSerializer.jsonConfig.decodeFromString(Project.serializer(), it.projectJson) }.getOrNull()
    }
}
