package com.onlyfield.assetmanager.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ObservationStatus {
    VERIFIED,
    TO_VERIFY,
    CONFLICT,
    NOT_DETECTED
}

@Serializable
data class Observation(
    val source: String,
    val timestampEpochMs: Long,
    val status: ObservationStatus = ObservationStatus.TO_VERIFY,
    val notes: String? = null
)

@Serializable
enum class EndpointStatus {
    CONNECTED,
    DETACHED_TO_VERIFY,
    DISCONNECTED,
    UNKNOWN
}

@Serializable
data class Port(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val name: String,
    val label: String? = null,
    val connectedPortId: String? = null,
    val endpointStatus: EndpointStatus = EndpointStatus.DISCONNECTED,
    val observation: Observation? = null
)

@Serializable
data class Device(
    val id: String = UUID.randomUUID().toString(),
    val technicalName: String,
    val physicalLabel: String? = null,
    val alias: String? = null,
    val ipAddress: String? = null,
    val macAddress: String? = null,
    val siteId: String? = null,
    val areaId: String? = null,
    val ports: List<Port> = emptyList(),
    val observation: Observation? = null
)

@Serializable
data class Area(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val floor: String? = null,
    val description: String? = null
)

@Serializable
data class Site(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String? = null,
    val areas: List<Area> = emptyList()
)

@Serializable
data class BusinessUnit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val code: String? = null,
    val sites: List<Site> = emptyList(),
    val areas: List<Area> = emptyList(),
    val devices: List<Device> = emptyList()
)

@Serializable
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val createdEpochMs: Long,
    val updatedEpochMs: Long,
    val businessUnits: List<BusinessUnit> = emptyList()
)
