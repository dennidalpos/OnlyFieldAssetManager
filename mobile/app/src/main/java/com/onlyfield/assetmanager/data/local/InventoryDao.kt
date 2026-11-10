package com.onlyfield.assetmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreas(areas: List<AreaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPorts(ports: List<PortEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredentials(credentials: List<CredentialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRacks(racks: List<RackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceModels(models: List<DeviceModelEntity>)

    @Query("SELECT * FROM sites WHERE projectId = :projectId")
    suspend fun getSitesByProjectId(projectId: String): List<SiteEntity>


    @Query("SELECT * FROM areas WHERE siteId IN (:siteIds)")
    suspend fun getAreasBySiteIds(siteIds: List<String>): List<AreaEntity>

    @Query("SELECT * FROM devices WHERE siteId IN (:siteIds)")
    suspend fun getDevicesBySiteIds(siteIds: List<String>): List<DeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotations(annotations: List<AnnotationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloorplanPlacements(placements: List<FloorplanPlacementEntity>)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCables(cables: List<CableEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPanelMappings(mappings: List<PanelMappingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVlans(vlans: List<VlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubnets(subnets: List<SubnetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortVlanMemberships(memberships: List<PortVlanMembershipEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogicalInterfaces(interfaces: List<LogicalInterfaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLagGroups(lags: List<LagGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceConfigurations(configs: List<DeviceConfigurationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWanVpnConnections(connections: List<WanVpnConnectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoSurveillanceMappings(mappings: List<VideoSurveillanceMappingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomExtraFields(fields: List<CustomExtraFieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPowerFeeds(feeds: List<PowerFeedEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoeMappings(poes: List<PoeMappingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentBadges(badges: List<DocumentBadgeEntity>)

    @Query("SELECT * FROM ports WHERE deviceId IN (:deviceIds)")
    suspend fun getPortsByDeviceIds(deviceIds: List<String>): List<PortEntity>

    @Query("SELECT * FROM credentials WHERE projectId = :projectId")
    suspend fun getCredentialsByProjectId(projectId: String): List<CredentialEntity>

    @Query("SELECT * FROM racks WHERE projectId = :projectId")
    suspend fun getRacksByProjectId(projectId: String): List<RackEntity>

    @Query("SELECT * FROM device_models WHERE projectId = :projectId")
    suspend fun getDeviceModelsByProjectId(projectId: String): List<DeviceModelEntity>

    @Query("SELECT * FROM attachments WHERE projectId = :projectId")
    suspend fun getAttachmentsByProjectId(projectId: String): List<AttachmentEntity>

    @Query("SELECT * FROM annotations WHERE projectId = :projectId")
    suspend fun getAnnotationsByProjectId(projectId: String): List<AnnotationEntity>

    @Query("SELECT * FROM floorplan_placements WHERE projectId = :projectId")
    suspend fun getFloorplanPlacementsByProjectId(projectId: String): List<FloorplanPlacementEntity>


    @Query("SELECT * FROM cables WHERE projectId = :projectId")
    suspend fun getCablesByProjectId(projectId: String): List<CableEntity>

    @Query("SELECT * FROM panel_mappings WHERE projectId = :projectId")
    suspend fun getPanelMappingsByProjectId(projectId: String): List<PanelMappingEntity>

    @Query("SELECT * FROM vlans WHERE projectId = :projectId")
    suspend fun getVlansByProjectId(projectId: String): List<VlanEntity>

    @Query("SELECT * FROM subnets WHERE projectId = :projectId")
    suspend fun getSubnetsByProjectId(projectId: String): List<SubnetEntity>

    @Query("SELECT * FROM port_vlan_memberships WHERE projectId = :projectId")
    suspend fun getPortVlanMembershipsByProjectId(projectId: String): List<PortVlanMembershipEntity>

    @Query("SELECT * FROM logical_interfaces WHERE projectId = :projectId")
    suspend fun getLogicalInterfacesByProjectId(projectId: String): List<LogicalInterfaceEntity>

    @Query("SELECT * FROM lag_groups WHERE projectId = :projectId")
    suspend fun getLagGroupsByProjectId(projectId: String): List<LagGroupEntity>

    @Query("SELECT * FROM device_configurations WHERE projectId = :projectId")
    suspend fun getDeviceConfigurationsByProjectId(projectId: String): List<DeviceConfigurationEntity>

    @Query("SELECT * FROM wan_vpn_connections WHERE projectId = :projectId")
    suspend fun getWanVpnConnectionsByProjectId(projectId: String): List<WanVpnConnectionEntity>

    @Query("SELECT * FROM video_surveillance_mappings WHERE projectId = :projectId")
    suspend fun getVideoSurveillanceMappingsByProjectId(projectId: String): List<VideoSurveillanceMappingEntity>

    @Query("SELECT * FROM custom_extra_fields WHERE projectId = :projectId")
    suspend fun getCustomExtraFieldsByProjectId(projectId: String): List<CustomExtraFieldEntity>

    @Query("SELECT * FROM power_feeds WHERE projectId = :projectId")
    suspend fun getPowerFeedsByProjectId(projectId: String): List<PowerFeedEntity>

    @Query("SELECT * FROM poe_mappings WHERE projectId = :projectId")
    suspend fun getPoeMappingsByProjectId(projectId: String): List<PoeMappingEntity>

    @Query("SELECT * FROM document_badges WHERE projectId = :projectId")
    suspend fun getDocumentBadgesByProjectId(projectId: String): List<DocumentBadgeEntity>

    @Query("DELETE FROM sites WHERE projectId = :projectId")
    suspend fun deleteSitesByProjectId(projectId: String)

    @Query("DELETE FROM credentials WHERE projectId = :projectId")
    suspend fun deleteCredentialsByProjectId(projectId: String)

    @Query("DELETE FROM racks WHERE projectId = :projectId")
    suspend fun deleteRacksByProjectId(projectId: String)

    @Query("DELETE FROM device_models WHERE projectId = :projectId")
    suspend fun deleteDeviceModelsByProjectId(projectId: String)

    @Query("DELETE FROM attachments WHERE projectId = :projectId")
    suspend fun deleteAttachmentsByProjectId(projectId: String)

    @Query("DELETE FROM annotations WHERE projectId = :projectId")
    suspend fun deleteAnnotationsByProjectId(projectId: String)

    @Query("DELETE FROM floorplan_placements WHERE projectId = :projectId")
    suspend fun deleteFloorplanPlacementsByProjectId(projectId: String)


    @Query("DELETE FROM cables WHERE projectId = :projectId")
    suspend fun deleteCablesByProjectId(projectId: String)

    @Query("DELETE FROM panel_mappings WHERE projectId = :projectId")
    suspend fun deletePanelMappingsByProjectId(projectId: String)

    @Query("DELETE FROM vlans WHERE projectId = :projectId")
    suspend fun deleteVlansByProjectId(projectId: String)

    @Query("DELETE FROM subnets WHERE projectId = :projectId")
    suspend fun deleteSubnetsByProjectId(projectId: String)

    @Query("DELETE FROM port_vlan_memberships WHERE projectId = :projectId")
    suspend fun deletePortVlanMembershipsByProjectId(projectId: String)

    @Query("DELETE FROM logical_interfaces WHERE projectId = :projectId")
    suspend fun deleteLogicalInterfacesByProjectId(projectId: String)

    @Query("DELETE FROM lag_groups WHERE projectId = :projectId")
    suspend fun deleteLagGroupsByProjectId(projectId: String)

    @Query("DELETE FROM device_configurations WHERE projectId = :projectId")
    suspend fun deleteDeviceConfigurationsByProjectId(projectId: String)

    @Query("DELETE FROM wan_vpn_connections WHERE projectId = :projectId")
    suspend fun deleteWanVpnConnectionsByProjectId(projectId: String)

    @Query("DELETE FROM video_surveillance_mappings WHERE projectId = :projectId")
    suspend fun deleteVideoSurveillanceMappingsByProjectId(projectId: String)

    @Query("DELETE FROM custom_extra_fields WHERE projectId = :projectId")
    suspend fun deleteCustomExtraFieldsByProjectId(projectId: String)

    @Query("DELETE FROM power_feeds WHERE projectId = :projectId")
    suspend fun deletePowerFeedsByProjectId(projectId: String)

    @Query("DELETE FROM poe_mappings WHERE projectId = :projectId")
    suspend fun deletePoeMappingsByProjectId(projectId: String)

    @Query("DELETE FROM document_badges WHERE projectId = :projectId")
    suspend fun deleteDocumentBadgesByProjectId(projectId: String)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: String)

    @Query("DELETE FROM annotations WHERE id = :annotationId")
    suspend fun deleteAnnotationById(annotationId: String)

    @Query("DELETE FROM floorplan_placements WHERE id = :placementId")
    suspend fun deleteFloorplanPlacementById(placementId: String)


    @Query("DELETE FROM cables WHERE id = :cableId")
    suspend fun deleteCableById(cableId: String)

    @Query("DELETE FROM panel_mappings WHERE id = :mappingId")
    suspend fun deletePanelMappingById(mappingId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItems(trashItems: List<TrashItemEntity>)

    @Query("SELECT * FROM trash_items WHERE projectId = :projectId ORDER BY deletedEpochMs DESC")
    suspend fun getTrashItemsByProjectId(projectId: String): List<TrashItemEntity>

    @Query("SELECT * FROM trash_items WHERE id = :trashId")
    suspend fun getTrashItemById(trashId: String): TrashItemEntity?

    @Query("DELETE FROM trash_items WHERE id = :trashId")
    suspend fun deleteTrashItemById(trashId: String)

    @Query("DELETE FROM trash_items WHERE projectId = :projectId")
    suspend fun emptyTrashByProjectId(projectId: String)

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)

    @Query("DELETE FROM ports WHERE deviceId = :deviceId")
    suspend fun deletePortsByDeviceId(deviceId: String)

    @Query("DELETE FROM ports WHERE id = :portId")
    suspend fun deletePortById(portId: String)

    @Query("DELETE FROM racks WHERE id = :rackId")
    suspend fun deleteRackById(rackId: String)

    @Query("DELETE FROM areas WHERE id = :areaId")
    suspend fun deleteAreaById(areaId: String)


    @Query("DELETE FROM sites WHERE id = :siteId")
    suspend fun deleteSiteById(siteId: String)

    @Query("DELETE FROM credentials WHERE id = :credentialId")
    suspend fun deleteCredentialById(credentialId: String)
}
