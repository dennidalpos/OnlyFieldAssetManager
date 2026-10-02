package com.onlyfield.assetmanager.core.validation

import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.EndpointStatus
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.Project
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ValidationSeverity {
    STRUCTURAL_ERROR,
    DOCUMENTARY_WARNING
}

@Serializable
data class ValidationIssue(
    val code: String,
    val message: String,
    val severity: ValidationSeverity,
    val targetEntityId: String? = null
)

data class ValidationResult(
    val issues: List<ValidationIssue> = emptyList()
) {
    val isValid: Boolean get() = issues.none { it.severity == ValidationSeverity.STRUCTURAL_ERROR }
    val hasWarnings: Boolean get() = issues.any { it.severity == ValidationSeverity.DOCUMENTARY_WARNING }
}

object ModelValidator {

    fun validateProject(project: Project): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        val seenIds = mutableSetOf<String>()

        // 1. Project ID validity
        checkUuid("INVALID_PROJECT_UUID", project.id, "Project ID is not a valid UUID", issues)
        trackId(project.id, "DUPLICATE_PROJECT_ID", "Duplicate project ID: ${project.id}", seenIds, issues)

        val allPorts = mutableMapOf<String, Port>()
        val allDevices = mutableListOf<Pair<String, Device>>() // Pair(BU_ID, Device)

        // Traverse Business Units
        for (bu in project.businessUnits) {
            checkUuid("INVALID_BU_UUID", bu.id, "Business Unit ID is not a valid UUID", issues)
            trackId(bu.id, "DUPLICATE_BU_ID", "Duplicate Business Unit ID: ${bu.id}", seenIds, issues)

            // Traverse Sites
            for (site in bu.sites) {
                checkUuid("INVALID_SITE_UUID", site.id, "Site ID is not a valid UUID", issues)
                trackId(site.id, "DUPLICATE_SITE_ID", "Duplicate Site ID: ${site.id}", seenIds, issues)

                for (area in site.areas) {
                    checkUuid("INVALID_AREA_UUID", area.id, "Area ID is not a valid UUID", issues)
                    trackId(area.id, "DUPLICATE_AREA_ID", "Duplicate Area ID: ${area.id}", seenIds, issues)
                }
            }

            // Traverse BU Direct Areas
            for (area in bu.areas) {
                checkUuid("INVALID_AREA_UUID", area.id, "Area ID is not a valid UUID", issues)
                trackId(area.id, "DUPLICATE_AREA_ID", "Duplicate Area ID: ${area.id}", seenIds, issues)
            }

            // Collect Devices
            for (device in bu.devices) {
                allDevices.add(bu.id to device)
                checkUuid("INVALID_DEVICE_UUID", device.id, "Device ID is not a valid UUID", issues)
                trackId(device.id, "DUPLICATE_DEVICE_ID", "Duplicate Device ID: ${device.id}", seenIds, issues)

                // Unpositioned device check (Documentary warning)
                if (device.siteId == null && device.areaId == null) {
                    issues.add(
                        ValidationIssue(
                            code = "UNPOSITIONED_DEVICE",
                            message = "Device '${device.technicalName}' has no site or area assigned",
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = device.id
                        )
                    )
                }

                // Observation check
                if (device.observation?.status == ObservationStatus.TO_VERIFY ||
                    device.observation?.status == ObservationStatus.CONFLICT
                ) {
                    issues.add(
                        ValidationIssue(
                            code = "UNVERIFIED_DEVICE_OBSERVATION",
                            message = "Device '${device.technicalName}' observation status is ${device.observation.status}",
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = device.id
                        )
                    )
                }

                // Collect Ports
                for (port in device.ports) {
                    checkUuid("INVALID_PORT_UUID", port.id, "Port ID is not a valid UUID", issues)
                    trackId(port.id, "DUPLICATE_PORT_ID", "Duplicate Port ID: ${port.id}", seenIds, issues)

                    if (port.deviceId != device.id) {
                        issues.add(
                            ValidationIssue(
                                code = "PORT_DEVICE_MISMATCH",
                                message = "Port '${port.id}' deviceId '${port.deviceId}' does not match parent device '${device.id}'",
                                severity = ValidationSeverity.STRUCTURAL_ERROR,
                                targetEntityId = port.id
                            )
                        )
                    }

                    allPorts[port.id] = port

                    if (port.endpointStatus == EndpointStatus.DETACHED_TO_VERIFY) {
                        issues.add(
                            ValidationIssue(
                                code = "DETACHED_PORT_ENDPOINT",
                                message = "Port '${port.name}' on device '${device.technicalName}' marked detached for verification",
                                severity = ValidationSeverity.DOCUMENTARY_WARNING,
                                targetEntityId = port.id
                            )
                        )
                    }
                }
            }
        }

        // Port connection targets integrity check
        for (port in allPorts.values) {
            port.connectedPortId?.let { targetPortId ->
                val targetPort = allPorts[targetPortId]
                if (targetPort == null) {
                    issues.add(
                        ValidationIssue(
                            code = "BROKEN_PORT_CONNECTION",
                            message = "Port '${port.id}' connects to non-existent port '${targetPortId}'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = port.id
                        )
                    )
                }
            }
        }

        // Scope validation for duplicate technical names / IPs within the same Business Unit
        val devicesByBu = allDevices.groupBy { it.first }
        for ((buId, buDevices) in devicesByBu) {
            val namesInBu = mutableMapOf<String, String>() // name -> deviceId
            val ipsInBu = mutableMapOf<String, String>()   // ip -> deviceId

            for ((_, device) in buDevices) {
                val existingNameDeviceId = namesInBu[device.technicalName.lowercase()]
                if (existingNameDeviceId != null) {
                    issues.add(
                        ValidationIssue(
                            code = "DUPLICATE_DEVICE_NAME_IN_BU",
                            message = "Duplicate technical name '${device.technicalName}' in Business Unit $buId",
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = device.id
                        )
                    )
                } else {
                    namesInBu[device.technicalName.lowercase()] = device.id
                }

                device.ipAddress?.let { ip ->
                    if (ip.isNotBlank()) {
                        val existingIpDeviceId = ipsInBu[ip]
                        if (existingIpDeviceId != null) {
                            issues.add(
                                ValidationIssue(
                                    code = "DUPLICATE_IP_IN_BU",
                                    message = "Duplicate IP address '$ip' in Business Unit $buId",
                                    severity = ValidationSeverity.DOCUMENTARY_WARNING,
                                    targetEntityId = device.id
                                )
                            )
                        } else {
                            ipsInBu[ip] = device.id
                        }
                    }
                }
            }
        }

        return ValidationResult(issues)
    }

    private fun checkUuid(code: String, uuidStr: String, message: String, issues: MutableList<ValidationIssue>) {
        try {
            UUID.fromString(uuidStr)
        } catch (e: IllegalArgumentException) {
            issues.add(
                ValidationIssue(
                    code = code,
                    message = "$message: '$uuidStr'",
                    severity = ValidationSeverity.STRUCTURAL_ERROR,
                    targetEntityId = uuidStr
                )
            )
        }
    }

    private fun trackId(id: String, code: String, message: String, seenIds: MutableSet<String>, issues: MutableList<ValidationIssue>) {
        if (seenIds.contains(id)) {
            issues.add(
                ValidationIssue(
                    code = code,
                    message = message,
                    severity = ValidationSeverity.STRUCTURAL_ERROR,
                    targetEntityId = id
                )
            )
        } else {
            seenIds.add(id)
        }
    }
}
