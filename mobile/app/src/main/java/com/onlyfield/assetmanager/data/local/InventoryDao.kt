package com.onlyfield.assetmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessUnits(bus: List<BusinessUnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreas(areas: List<AreaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPorts(ports: List<PortEntity>)

    @Query("SELECT * FROM business_units WHERE projectId = :projectId")
    suspend fun getBusinessUnitsByProjectId(projectId: String): List<BusinessUnitEntity>

    @Query("SELECT * FROM sites WHERE businessUnitId IN (:buIds)")
    suspend fun getSitesByBuIds(buIds: List<String>): List<SiteEntity>

    @Query("SELECT * FROM areas WHERE businessUnitId IN (:buIds)")
    suspend fun getAreasByBuIds(buIds: List<String>): List<AreaEntity>

    @Query("SELECT * FROM devices WHERE businessUnitId IN (:buIds)")
    suspend fun getDevicesByBuIds(buIds: List<String>): List<DeviceEntity>

    @Query("SELECT * FROM ports WHERE deviceId IN (:deviceIds)")
    suspend fun getPortsByDeviceIds(deviceIds: List<String>): List<PortEntity>

    @Query("DELETE FROM business_units WHERE projectId = :projectId")
    suspend fun deleteBusinessUnitsByProjectId(projectId: String)

    @Query("SELECT * FROM devices WHERE businessUnitId IN (:buIds) AND (technicalName LIKE '%' || :query || '%' OR ipAddress LIKE '%' || :query || '%' OR physicalLabel LIKE '%' || :query || '%' OR alias LIKE '%' || :query || '%')")
    suspend fun searchDevices(buIds: List<String>, query: String): List<DeviceEntity>
}
