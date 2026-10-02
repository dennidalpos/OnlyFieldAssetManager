package com.onlyfield.assetmanager.core.validation

import com.onlyfield.assetmanager.core.model.*
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
    val targetEntityId: String? = null,
)

data class ValidationResult(
    val issues: List<ValidationIssue> = emptyList(),
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

        // Validate Racks
        val racksById = mutableMapOf<String, Rack>()
        for (rack in project.racks) {
            checkUuid("INVALID_RACK_UUID", rack.id, "Rack ID is not a valid UUID", issues)
            trackId(rack.id, "DUPLICATE_RACK_ID", "Duplicate Rack ID: ${rack.id}", seenIds, issues)
            if (rack.heightU <= 0) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_RACK_HEIGHT",
                        message = "Rack '${rack.name}' heightU must be greater than 0",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = rack.id
                    )
                )
            }
            racksById[rack.id] = rack
        }

        // Validate DeviceModels
        for (model in project.deviceModels) {
            checkUuid("INVALID_MODEL_UUID", model.id, "DeviceModel ID is not a valid UUID", issues)
            trackId(model.id, "DUPLICATE_MODEL_ID", "Duplicate DeviceModel ID: ${model.id}", seenIds, issues)
        }

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
                if ((device.siteId == null) && (device.areaId == null) && (device.rackId == null)) {
                    issues.add(
                        ValidationIssue(
                            code = "UNPOSITIONED_DEVICE",
                            message = "Device '${device.technicalName}' has no site, area, or rack assigned",
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = device.id
                        )
                    )
                }

                // Rack placement validation
                device.rackId?.let { rackId ->
                    val rack = racksById[rackId]
                    if (rack == null) {
                        issues.add(
                            ValidationIssue(
                                code = "INVALID_RACK_REFERENCE",
                                message = "Device '${device.technicalName}' references non-existent Rack '$rackId'",
                                severity = ValidationSeverity.STRUCTURAL_ERROR,
                                targetEntityId = device.id
                            )
                        )
                    } else {
                        device.positionU?.let { pos ->
                            val topU = pos + device.heightU - 1
                            if (pos < 1 || topU > rack.heightU) {
                                issues.add(
                                    ValidationIssue(
                                        code = "RACK_U_OUT_OF_BOUNDS",
                                        message = "Device '${device.technicalName}' position U$pos-$topU exceeds Rack '${rack.name}' height (U${rack.heightU})",
                                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                                        targetEntityId = device.id
                                    )
                                )
                            }
                        }
                    }
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

        // Check for U slot overlaps in racks
        val devicesByRack = allDevices.map { it.second }.filter { it.rackId != null && it.positionU != null }.groupBy { it.rackId!! }
        for ((rackId, rackDevices) in devicesByRack) {
            val rackName = racksById[rackId]?.name ?: rackId
            for (i in rackDevices.indices) {
                for (j in i + 1 until rackDevices.size) {
                    val d1 = rackDevices[i]
                    val d2 = rackDevices[j]
                    val pos1Start = d1.positionU!!
                    val pos1End = pos1Start + d1.heightU - 1
                    val pos2Start = d2.positionU!!
                    val pos2End = pos2Start + d2.heightU - 1

                    // Check U range overlap
                    val uOverlaps = kotlin.math.max(pos1Start, pos2Start) <= kotlin.math.min(pos1End, pos2End)
                    // Check side overlap
                    val sideOverlaps = d1.rackSide == RackSide.BOTH || d2.rackSide == RackSide.BOTH || d1.rackSide == d2.rackSide

                    if (uOverlaps && sideOverlaps) {
                        issues.add(
                            ValidationIssue(
                                code = "RACK_SLOT_OVERLAP",
                                message = "Device '${d1.technicalName}' (U$pos1Start-$pos1End) overlaps with '${d2.technicalName}' (U$pos2Start-$pos2End) in Rack '$rackName'",
                                severity = ValidationSeverity.DOCUMENTARY_WARNING,
                                targetEntityId = d1.id
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
                            message = "Port '${port.id}' connects to non-existent port '$targetPortId'",
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

        // Validate Credentials
        for (cred in project.credentials) {
            checkUuid("INVALID_CREDENTIAL_UUID", cred.id, "Credential ID is not a valid UUID", issues)
            trackId(cred.id, "DUPLICATE_CREDENTIAL_ID", "Duplicate Credential ID: ${cred.id}", seenIds, issues)

            if (cred.username.isBlank()) {
                issues.add(
                    ValidationIssue(
                        code = "BLANK_CREDENTIAL_USERNAME",
                        message = "Credential '${cred.id}' has a blank username",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = cred.id
                    )
                )
            }
        }

        // Validate Attachments
        val attachmentsById = project.attachments.associateBy { it.id }
        for (att in project.attachments) {
            checkUuid("INVALID_ATTACHMENT_UUID", att.id, "Attachment ID is not a valid UUID", issues)
            trackId(att.id, "DUPLICATE_ATTACHMENT_ID", "Duplicate Attachment ID: ${att.id}", seenIds, issues)

            if (att.classification == com.onlyfield.assetmanager.core.model.AttachmentClassification.REVIEW_REQUIRED) {
                issues.add(
                    ValidationIssue(
                        code = "ATTACHMENT_NEEDS_REVIEW",
                        message = "Allegato '${att.name}' classificato come 'Da riesaminare'",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = att.id
                    )
                )
            }
        }

        // Validate Area Floorplans
        val allAreaIds = mutableSetOf<String>()
        for (bu in project.businessUnits) {
            for (site in bu.sites) {
                for (area in site.areas) {
                    allAreaIds.add(area.id)
                    area.floorplanAttachmentId?.let { fpId ->
                        if (!attachmentsById.containsKey(fpId)) {
                            issues.add(
                                ValidationIssue(
                                    code = "INVALID_FLOORPLAN_ATTACHMENT",
                                    message = "Area '${area.name}' riferisce planimetria non esistente '$fpId'",
                                    severity = ValidationSeverity.STRUCTURAL_ERROR,
                                    targetEntityId = area.id
                                )
                            )
                        }
                    }
                }
            }
            for (area in bu.areas) {
                allAreaIds.add(area.id)
                area.floorplanAttachmentId?.let { fpId ->
                    if (!attachmentsById.containsKey(fpId)) {
                        issues.add(
                            ValidationIssue(
                                code = "INVALID_FLOORPLAN_ATTACHMENT",
                                message = "Area '${area.name}' riferisce planimetria non esistente '$fpId'",
                                severity = ValidationSeverity.STRUCTURAL_ERROR,
                                targetEntityId = area.id
                            )
                        )
                    }
                }
            }
        }

        // Validate Floorplan Placements
        val allDeviceIds = allDevices.map { it.second.id }.toSet()
        val allRackIds = racksById.keys
        for (placement in project.floorplanPlacements) {
            checkUuid("INVALID_PLACEMENT_UUID", placement.id, "Placement ID is not a valid UUID", issues)
            trackId(placement.id, "DUPLICATE_PLACEMENT_ID", "Duplicate Placement ID: ${placement.id}", seenIds, issues)

            if (!allAreaIds.contains(placement.areaId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_PLACEMENT_AREA",
                        message = "Placement '${placement.id}' riferisce area non esistente '${placement.areaId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = placement.id
                    )
                )
            }

            val targetExists = when (placement.targetType) {
                com.onlyfield.assetmanager.core.model.PlacementTargetType.RACK -> allRackIds.contains(placement.targetId)
                com.onlyfield.assetmanager.core.model.PlacementTargetType.DEVICE -> allDeviceIds.contains(placement.targetId)
            }
            if (!targetExists) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_PLACEMENT_TARGET",
                        message = "Placement '${placement.id}' riferisce destinazione non esistente '${placement.targetId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = placement.id
                    )
                )
            }

            if (placement.xRatio < 0f || placement.xRatio > 1f || placement.yRatio < 0f || placement.yRatio > 1f) {
                issues.add(
                    ValidationIssue(
                        code = "PLACEMENT_OUT_OF_BOUNDS",
                        message = "Placement '${placement.id}' ha coordinate fuori dai limiti (0.0..1.0)",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = placement.id
                    )
                )
            }
        }

        // Validate Annotations
        for (ann in project.annotations) {
            checkUuid("INVALID_ANNOTATION_UUID", ann.id, "Annotation ID is not a valid UUID", issues)
            trackId(ann.id, "DUPLICATE_ANNOTATION_ID", "Duplicate Annotation ID: ${ann.id}", seenIds, issues)

            if (!allAreaIds.contains(ann.areaId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_ANNOTATION_AREA",
                        message = "Annotazione '${ann.id}' riferisce area non esistente '${ann.areaId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = ann.id
                    )
                )
            }

            if (ann.classification == com.onlyfield.assetmanager.core.model.AttachmentClassification.REVIEW_REQUIRED) {
                issues.add(
                    ValidationIssue(
                        code = "ANNOTATION_NEEDS_REVIEW",
                        message = "Annotazione '${ann.id}' classificata come 'Da riesaminare'",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = ann.id
                    )
                )
            }
        }

        // Validate SharedPathSegments
        val sharedPathSegmentsById = project.sharedPathSegments.associateBy { it.id }
        val cableCountBySegmentId = mutableMapOf<String, Int>()

        for (segment in project.sharedPathSegments) {
            checkUuid("INVALID_PATH_SEGMENT_UUID", segment.id, "SharedPathSegment ID non è un UUID valido", issues)
            trackId(segment.id, "DUPLICATE_PATH_SEGMENT_ID", "ID SharedPathSegment duplicato: ${segment.id}", seenIds, issues)

            segment.sourceAreaId?.let { sAreaId ->
                if (!allAreaIds.contains(sAreaId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_PATH_AREA_REFERENCE",
                            message = "Percorso '${segment.name}' riferisce area origine non esistente '$sAreaId'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = segment.id
                        )
                    )
                }
            }

            segment.targetAreaId?.let { tAreaId ->
                if (!allAreaIds.contains(tAreaId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_PATH_AREA_REFERENCE",
                            message = "Percorso '${segment.name}' riferisce area destinazione non esistente '$tAreaId'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = segment.id
                        )
                    )
                }
            }
        }

        // Validate Cables
        for (cable in project.cables) {
            checkUuid("INVALID_CABLE_UUID", cable.id, "Cable ID non è un UUID valido", issues)
            trackId(cable.id, "DUPLICATE_CABLE_ID", "ID Cavo duplicato: ${cable.id}", seenIds, issues)

            if (cable.portAId == null || cable.portBId == null) {
                issues.add(
                    ValidationIssue(
                        code = "DETACHED_CABLE_ENDPOINT",
                        message = "Cavo '${cable.codeOrLabel ?: cable.id}' ha un'estremità scollegata o da verificare",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = cable.id
                    )
                )
            }

            cable.portAId?.let { portA ->
                if (!allPorts.containsKey(portA)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_PORT_REFERENCE",
                            message = "Cavo '${cable.codeOrLabel ?: cable.id}' riferisce porta A non esistente '$portA'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = cable.id
                        )
                    )
                }
            }

            cable.portBId?.let { portB ->
                if (!allPorts.containsKey(portB)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_PORT_REFERENCE",
                            message = "Cavo '${cable.codeOrLabel ?: cable.id}' riferisce porta B non esistente '$portB'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = cable.id
                        )
                    )
                }
            }

            for (segId in cable.sharedPathSegmentIds) {
                if (!sharedPathSegmentsById.containsKey(segId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_SHARED_PATH_REFERENCE",
                            message = "Cavo '${cable.codeOrLabel ?: cable.id}' riferisce segmento di percorso condiviso non esistente '$segId'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = cable.id
                        )
                    )
                } else {
                    cableCountBySegmentId[segId] = (cableCountBySegmentId[segId] ?: 0) + 1
                }
            }

            if (cable.observation?.status == ObservationStatus.TO_VERIFY ||
                cable.observation?.status == ObservationStatus.CONFLICT
            ) {
                issues.add(
                    ValidationIssue(
                        code = "UNVERIFIED_CABLE",
                        message = "Stato osservazione cavo '${cable.codeOrLabel ?: cable.id}' è ${cable.observation.status}",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = cable.id
                    )
                )
            }
        }

        // Check SharedPathSegment Capacity
        for (segment in project.sharedPathSegments) {
            val maxCap = segment.capacityMaxCables
            if (maxCap != null) {
                val count = cableCountBySegmentId[segment.id] ?: 0
                if (count > maxCap) {
                    issues.add(
                        ValidationIssue(
                            code = "SHARED_PATH_CAPACITY_EXCEEDED",
                            message = "Percorso condiviso '${segment.name}' supera la capacità massima ($count/$maxCap cavi)",
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = segment.id
                        )
                    )
                }
            }
        }

        // Validate PanelMappings
        for (mapping in project.panelMappings) {
            checkUuid("INVALID_PANEL_MAPPING_UUID", mapping.id, "PanelMapping ID non è un UUID valido", issues)
            trackId(mapping.id, "DUPLICATE_PANEL_MAPPING_ID", "ID PanelMapping duplicato: ${mapping.id}", seenIds, issues)

            if (!allPorts.containsKey(mapping.portAId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_PORT_REFERENCE",
                        message = "Mapping pannello '${mapping.id}' riferisce porta A non esistente '${mapping.portAId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = mapping.id
                    )
                )
            }

            mapping.portBId?.let { portB ->
                if (!mapping.isUnknownPassage && !allPorts.containsKey(portB)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_PORT_REFERENCE",
                            message = "Mapping pannello '${mapping.id}' riferisce porta B non esistente '$portB'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = mapping.id
                        )
                    )
                }
            }

            if (mapping.isUnknownPassage) {
                issues.add(
                    ValidationIssue(
                        code = "UNKNOWN_PASSAGE_IN_CHAIN",
                        message = "Mapping pannello '${mapping.id}' contiene un passaggio ignoto nella catena",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = mapping.id
                    )
                )
            }
        }

        // Validate VLANs
        val vlanNumbersByScope = mutableMapOf<String, MutableSet<Int>>()
        val knownVlanIds = project.vlans.map { it.vlanId }.toSet()

        for (vlan in project.vlans) {
            checkUuid("INVALID_VLAN_UUID", vlan.id, "VLAN ID non è un UUID valido", issues)
            trackId(vlan.id, "DUPLICATE_VLAN_ID", "ID VLAN duplicato: ${vlan.id}", seenIds, issues)

            if (vlan.vlanId !in 1..4094) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_VLAN_NUMBER",
                        message = "VLAN ID ${vlan.vlanId} fuori dal range valido (1-4094)",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = vlan.id
                    )
                )
            }

            val scopeKey = "${vlan.scopeType}_${vlan.scopeTargetId ?: "GLOBAL"}"
            val scopeSet = vlanNumbersByScope.getOrPut(scopeKey) { mutableSetOf() }
            if (scopeSet.contains(vlan.vlanId)) {
                issues.add(
                    ValidationIssue(
                        code = "DUPLICATE_VLAN_IN_SCOPE",
                        message = "VLAN ${vlan.vlanId} duplicata nell'ambito $scopeKey",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = vlan.id
                    )
                )
            } else {
                scopeSet.add(vlan.vlanId)
            }
        }

        // Validate Subnets
        for (subnet in project.subnets) {
            checkUuid("INVALID_SUBNET_UUID", subnet.id, "Subnet ID non è un UUID valido", issues)
            trackId(subnet.id, "DUPLICATE_SUBNET_ID", "ID Subnet duplicato: ${subnet.id}", seenIds, issues)

            if (subnet.cidrBlock.isBlank() || !subnet.cidrBlock.contains("/")) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_SUBNET_CIDR",
                        message = "Blocco CIDR non valido: '${subnet.cidrBlock}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = subnet.id
                    )
                )
            }
        }

        // Validate Port VLAN Memberships
        for (membership in project.portVlanMemberships) {
            checkUuid("INVALID_PORT_VLAN_MEMBERSHIP_UUID", membership.id, "PortVlanMembership ID non è un UUID valido", issues)
            trackId(membership.id, "DUPLICATE_PORT_VLAN_MEMBERSHIP_ID", "ID Membership duplicato: ${membership.id}", seenIds, issues)

            if (!allPorts.containsKey(membership.portId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_PORT_REFERENCE",
                        message = "Membership VLAN riferisce porta non esistente '${membership.portId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = membership.id
                    )
                )
            }

            membership.untaggedVlanId?.let { vlanId ->
                if (!knownVlanIds.contains(vlanId)) {
                    issues.add(
                        ValidationIssue(
                            code = "UNREFERENCED_VLAN_IN_MEMBERSHIP",
                            message = "Membership riferisce VLAN untagged non catalogata $vlanId",
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = membership.id
                        )
                    )
                }
            }

            for (tvId in membership.taggedVlanIds) {
                if (!knownVlanIds.contains(tvId)) {
                    issues.add(
                        ValidationIssue(
                            code = "UNREFERENCED_VLAN_IN_MEMBERSHIP",
                            message = "Membership riferisce VLAN tagged non catalogata $tvId",
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = membership.id
                        )
                    )
                }
            }
        }

        // Validate Logical Interfaces
        for (l3Int in project.logicalInterfaces) {
            checkUuid("INVALID_LOGICAL_INTERFACE_UUID", l3Int.id, "LogicalInterface ID non è un UUID valido", issues)
            trackId(l3Int.id, "DUPLICATE_LOGICAL_INTERFACE_ID", "ID LogicalInterface duplicato: ${l3Int.id}", seenIds, issues)

            if (!allDeviceIds.contains(l3Int.deviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_DEVICE_REFERENCE",
                        message = "Interfaccia logica '${l3Int.name}' riferisce apparato non esistente '${l3Int.deviceId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = l3Int.id
                    )
                )
            }
        }

        // Validate LAG Groups
        for (lag in project.lagGroups) {
            checkUuid("INVALID_LAG_GROUP_UUID", lag.id, "LagGroup ID non è un UUID valido", issues)
            trackId(lag.id, "DUPLICATE_LAG_GROUP_ID", "ID LagGroup duplicato: ${lag.id}", seenIds, issues)

            if (!allDeviceIds.contains(lag.deviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_DEVICE_REFERENCE",
                        message = "Gruppo LAG '${lag.name}' riferisce apparato non esistente '${lag.deviceId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = lag.id
                    )
                )
            }

            for (pId in lag.memberPortIds) {
                if (!allPorts.containsKey(pId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_PORT_REFERENCE",
                            message = "Gruppo LAG '${lag.name}' riferisce porta membro non esistente '$pId'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = lag.id
                        )
                    )
                }
            }
        }

        // Validate Device Configurations
        for (config in project.deviceConfigurations) {
            checkUuid("INVALID_DEVICE_CONFIG_UUID", config.id, "DeviceConfiguration ID non è un UUID valido", issues)
            trackId(config.id, "DUPLICATE_DEVICE_CONFIG_ID", "ID DeviceConfiguration duplicato: ${config.id}", seenIds, issues)

            if (!allDeviceIds.contains(config.deviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_DEVICE_REFERENCE",
                        message = "Configurazione '${config.title}' riferisce apparato non esistente '${config.deviceId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = config.id
                    )
                )
            }

            config.attachmentId?.let { attId ->
                if (!attachmentsById.containsKey(attId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_ATTACHMENT_REFERENCE",
                            message = "Configurazione '${config.title}' riferisce allegato non esistente '$attId'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = config.id
                        )
                    )
                }
            }
        }

        // Validate WAN/VPN Connections
        for (conn in project.wanVpnConnections) {
            checkUuid("INVALID_WAN_VPN_UUID", conn.id, "WanVpnConnection ID non è un UUID valido", issues)
            trackId(conn.id, "DUPLICATE_WAN_VPN_ID", "ID WanVpnConnection duplicato: ${conn.id}", seenIds, issues)

            conn.localEndpointDeviceId?.let { devId ->
                if (!allDeviceIds.contains(devId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_DEVICE_REFERENCE",
                            message = "Connessione '${conn.name}' riferisce apparato locale non esistente '$devId'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = conn.id
                        )
                    )
                }
            }

            conn.remoteEndpointDeviceId?.let { devId ->
                if (!allDeviceIds.contains(devId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_DEVICE_REFERENCE",
                            message = "Connessione '${conn.name}' riferisce apparato remoto non esistente '$devId'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = conn.id
                        )
                    )
                }
            }
        }

        // Validate Video Surveillance Mappings
        for (video in project.videoSurveillanceMappings) {
            checkUuid("INVALID_VIDEO_MAPPING_UUID", video.id, "VideoSurveillanceMapping ID non è un UUID valido", issues)
            trackId(video.id, "DUPLICATE_VIDEO_MAPPING_ID", "ID VideoSurveillanceMapping duplicato: ${video.id}", seenIds, issues)

            if (!allDeviceIds.contains(video.cameraDeviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_DEVICE_REFERENCE",
                        message = "Videosorveglianza riferisce telecamera non esistente '${video.cameraDeviceId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = video.id
                    )
                )
            }

            video.managerDeviceId?.let { devId ->
                if (!allDeviceIds.contains(devId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_DEVICE_REFERENCE",
                            message = "Videosorveglianza riferisce gestore/NVR non esistente '$devId'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = video.id
                        )
                    )
                }
            }
        }

        // Validate Custom Extra Fields
        for (field in project.customExtraFields) {
            checkUuid("INVALID_CUSTOM_FIELD_UUID", field.id, "CustomExtraField ID non è un UUID valido", issues)
            trackId(field.id, "DUPLICATE_CUSTOM_FIELD_ID", "ID CustomExtraField duplicato: ${field.id}", seenIds, issues)

            if (field.classification == com.onlyfield.assetmanager.core.model.AttachmentClassification.REVIEW_REQUIRED) {
                issues.add(
                    ValidationIssue(
                        code = "CUSTOM_FIELD_NEEDS_REVIEW",
                        message = "Campo extra '${field.fieldKey}' classificato come 'Da riesaminare'",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = field.id
                    )
                )
            }
        }

        // Validate Power Feeds
        val allPortIds = allDevices.flatMap { it.second.ports }.map { it.id }.toSet()
        val powerFeedsByDevice = project.powerFeeds.groupBy { it.deviceId }
        for (feed in project.powerFeeds) {
            checkUuid("INVALID_POWER_FEED_UUID", feed.id, "PowerFeed ID non è un UUID valido", issues)
            trackId(feed.id, "DUPLICATE_POWER_FEED_ID", "ID PowerFeed duplicato: ${feed.id}", seenIds, issues)

            if (!allDeviceIds.contains(feed.deviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_POWER_FEED_DEVICE",
                        message = "Alimentazione '${feed.feedName}' riferisce un apparato non esistente '${feed.deviceId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = feed.id
                    )
                )
            }

            feed.sourceDeviceId?.let { srcId ->
                if (!allDeviceIds.contains(srcId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_POWER_FEED_SOURCE",
                            message = "Alimentazione '${feed.feedName}' riferisce una sorgente non esistente '$srcId'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = feed.id
                        )
                    )
                }
            }

            // Cycle detection in power feed supply chain
            if (feed.sourceDeviceId != null) {
                var currentSourceId: String? = feed.sourceDeviceId
                val visitedDevices = mutableSetOf(feed.deviceId)
                var hasCycle = false
                while (currentSourceId != null) {
                    if (visitedDevices.contains(currentSourceId)) {
                        hasCycle = true
                        break
                    }
                    visitedDevices.add(currentSourceId)
                    val upstreamFeeds = powerFeedsByDevice[currentSourceId]
                    currentSourceId = upstreamFeeds?.firstOrNull { it.sourceDeviceId != null }?.sourceDeviceId
                }
                if (hasCycle) {
                    issues.add(
                        ValidationIssue(
                            code = "POWER_FEED_CYCLE_DETECTED",
                            message = "Rilevato ciclo nella catena di alimentazione per l'apparato '${feed.deviceId}'",
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = feed.id
                        )
                    )
                }
            }

            // Prohibit unverified calculated runtime (observed runtime requires source and timestamp)
            if (feed.observedRuntimeMinutes != null && (feed.observedSource.isNull_or_blank() || feed.observedEpochMs == null)) {
                issues.add(
                    ValidationIssue(
                        code = "CALCULATED_AUTONOMIA_PROHIBITED_WARNING",
                        message = "Autonomia espressa (${feed.observedRuntimeMinutes} min) priva di fonte o data rilevamento. Le autonomie calcolate non sono ammesse.",
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = feed.id
                    )
                )
            }
        }

        // Single feed partial coverage warnings for main devices
        for ((_, device) in allDevices) {
            val feeds = powerFeedsByDevice[device.id] ?: emptyList()
            if (feeds.isNotEmpty() && device.category != com.onlyfield.assetmanager.core.model.DeviceCategory.SHELF && device.category != com.onlyfield.assetmanager.core.model.DeviceCategory.BLANK_PANEL) {
                val hasFeedA = feeds.any { it.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.PRIMARY_A }
                val hasFeedB = feeds.any { it.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.SECONDARY_B }
                if (!(hasFeedA && hasFeedB)) {
                    issues.add(
                        ValidationIssue(
                            code = "SINGLE_FEED_PARTIAL_COVERAGE_WARNING",
                            message = "Apparato '${device.technicalName}' con copertura alimentazione parziale (manca A o B)",
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = device.id
                        )
                    )
                }
            }
        }

        // Validate PoE Mappings
        for (poe in project.poeMappings) {
            checkUuid("INVALID_POE_MAPPING_UUID", poe.id, "PoeMapping ID non è un UUID valido", issues)
            trackId(poe.id, "DUPLICATE_POE_MAPPING_ID", "ID PoeMapping duplicato: ${poe.id}", seenIds, issues)

            if (!allPortIds.contains(poe.portId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_POE_PORT_REFERENCE",
                        message = "Configurazione PoE riferisce porta non esistente '${poe.portId}'",
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = poe.id
                    )
                )
            }
        }

        // Validate Document Badges
        for (badge in project.documentBadges) {
            checkUuid("INVALID_DOCUMENT_BADGE_UUID", badge.id, "DocumentBadge ID non è un UUID valido", issues)
            trackId(badge.id, "DUPLICATE_DOCUMENT_BADGE_ID", "ID DocumentBadge duplicato: ${badge.id}", seenIds, issues)
        }

        return ValidationResult(issues)
    }

    fun deriveBadges(project: Project, targetType: String, targetId: String): List<com.onlyfield.assetmanager.core.model.DocumentBadge> {
        val result = mutableListOf<com.onlyfield.assetmanager.core.model.DocumentBadge>()

        // 1. Existing stored free labels
        val freeLabels = project.documentBadges.filter {
            it.targetType == targetType && it.targetId == targetId && !it.isDerived
        }
        result.addAll(freeLabels)

        // 2. Derive badges based on target
        when (targetType) {
            "DEVICE" -> {
                val device = project.businessUnits.flatMap { it.devices }.firstOrNull { it.id == targetId }
                if (device != null) {
                    val portIds = device.ports.map { it.id }.toSet()

                    // VLAN Badges
                    val memberships = project.portVlanMemberships.filter { portIds.contains(it.portId) }
                    val vlanIds = mutableSetOf<Int>()
                    memberships.forEach { m ->
                        m.untaggedVlanId?.let { vlanIds.add(it) }
                        vlanIds.addAll(m.taggedVlanIds)
                    }
                    if (vlanIds.isNotEmpty()) {
                        result.add(
                            com.onlyfield.assetmanager.core.model.DocumentBadge(
                                targetType = targetType,
                                targetId = targetId,
                                label = "VLAN: ${vlanIds.sorted().joinToString()}",
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.VLAN,
                                isDerived = true
                            )
                        )
                    }

                    // Medium Badges
                    val cables = project.cables.filter { c ->
                        (c.portAId != null && portIds.contains(c.portAId)) ||
                                (c.portBId != null && portIds.contains(c.portBId))
                    }
                    val media = cables.map { it.medium.name }.distinct()
                    if (media.isNotEmpty()) {
                        result.add(
                            com.onlyfield.assetmanager.core.model.DocumentBadge(
                                targetType = targetType,
                                targetId = targetId,
                                label = "MEZZO: ${media.joinToString()}",
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.MEDIUM,
                                isDerived = true
                            )
                        )
                    }

                    // PoE Badges
                    val poes = project.poeMappings.filter { portIds.contains(it.portId) }
                    if (poes.isNotEmpty()) {
                        val poeText = poes.map { "${it.role} ${it.standard}" }.distinct().joinToString()
                        result.add(
                            com.onlyfield.assetmanager.core.model.DocumentBadge(
                                targetType = targetType,
                                targetId = targetId,
                                label = "POE: $poeText",
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.POE,
                                isDerived = true
                            )
                        )
                    }

                    // Power Coverage Badges
                    val feeds = project.powerFeeds.filter { it.deviceId == targetId }
                    if (feeds.isNotEmpty()) {
                        val hasA = feeds.any { it.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.PRIMARY_A }
                        val hasB = feeds.any { it.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.SECONDARY_B }
                        val covLabel = if (hasA && hasB) "COPERTURA: Doppia (A/B)" else "COPERTURA: Parziale"
                        result.add(
                            com.onlyfield.assetmanager.core.model.DocumentBadge(
                                targetType = targetType,
                                targetId = targetId,
                                label = covLabel,
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.COVERAGE,
                                isDerived = true
                            )
                        )

                        // UPS Dependency Badges
                        val hasUps = feeds.any { f ->
                            f.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.UPS_BACKUP ||
                                    (f.sourceDeviceId != null && project.businessUnits.flatMap { it.devices }
                                        .firstOrNull { it.id == f.sourceDeviceId }?.category == com.onlyfield.assetmanager.core.model.DeviceCategory.UPS_PDU)
                        }
                        val upsLabel = if (hasUps) "UPS: Protetto" else "UPS: Nessuna protezione diretta"
                        result.add(
                            com.onlyfield.assetmanager.core.model.DocumentBadge(
                                targetType = targetType,
                                targetId = targetId,
                                label = upsLabel,
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.UPS_DEPENDENCY,
                                isDerived = true
                            )
                        )
                    }

                    // Open Issue Badges
                    if (device.observation?.status == com.onlyfield.assetmanager.core.model.ObservationStatus.TO_VERIFY ||
                        device.observation?.status == com.onlyfield.assetmanager.core.model.ObservationStatus.CONFLICT
                    ) {
                        result.add(
                            com.onlyfield.assetmanager.core.model.DocumentBadge(
                                targetType = targetType,
                                targetId = targetId,
                                label = "QUESTIONE APERTA: ${device.observation.status}",
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.OPEN_ISSUE,
                                isDerived = true
                            )
                        )
                    }
                }
            }

            "PORT" -> {
                val membership = project.portVlanMemberships.firstOrNull { it.portId == targetId }
                if (membership != null) {
                    val vlans = mutableListOf<String>()
                    membership.untaggedVlanId?.let { vlans.add("untagged $it") }
                    if (membership.taggedVlanIds.isNotEmpty()) {
                        vlans.add("tagged ${membership.taggedVlanIds.joinToString()}")
                    }
                    result.add(
                        com.onlyfield.assetmanager.core.model.DocumentBadge(
                            targetType = targetType,
                            targetId = targetId,
                            label = "VLAN: ${vlans.joinToString(", ")}",
                            category = com.onlyfield.assetmanager.core.model.BadgeCategory.VLAN,
                            isDerived = true
                        )
                    )
                }

                val poe = project.poeMappings.firstOrNull { it.portId == targetId }
                if (poe != null) {
                    result.add(
                        com.onlyfield.assetmanager.core.model.DocumentBadge(
                            targetType = targetType,
                            targetId = targetId,
                            label = "POE: ${poe.role} ${poe.standard}",
                            category = com.onlyfield.assetmanager.core.model.BadgeCategory.POE,
                            isDerived = true
                        )
                    )
                }
            }
        }

        return result
    }

    fun validateMergeTargets(survivingDeviceId: String, duplicateDeviceId: String): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        if (survivingDeviceId == duplicateDeviceId) {
            issues.add(
                ValidationIssue(
                    code = "CANNOT_MERGE_SAME_DEVICE",
                    message = "Surviving device ID and duplicate device ID must be distinct",
                    severity = ValidationSeverity.STRUCTURAL_ERROR,
                    targetEntityId = survivingDeviceId
                )
            )
        }
        return ValidationResult(issues)
    }

    fun generateBatchEditPreview(devices: List<Device>, changes: BatchDeviceChanges): BatchEditPreview {
        val summaries = mutableListOf<String>()
        if (changes.updateSiteId) summaries.add("Sede impostata su: ${changes.siteId ?: "Nessuna"}")
        if (changes.updateAreaId) summaries.add("Area impostata su: ${changes.areaId ?: "Nessuna"}")
        if (changes.updateCategory) summaries.add("Categoria impostata su: ${changes.category ?: "Personalizzato"}")
        if (changes.updateRackId) summaries.add("Rack impostato su: ${changes.rackId ?: "Fuori rack"}")
        if (changes.updateMountingType) summaries.add("Montaggio impostato su: ${changes.mountingType}")
        if (changes.updateObservationNotes) summaries.add("Note osservazione aggiornate")

        return BatchEditPreview(
            targetDeviceIds = devices.map { it.id },
            affectedDeviceNames = devices.map { it.technicalName },
            changesSummary = summaries,
            isProhibitedFieldAttempted = false
        )
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

    private fun checkUuid(code: String, uuidStr: String, message: String, issues: MutableList<ValidationIssue>) {
        try {
            UUID.fromString(uuidStr)
        } catch (_: IllegalArgumentException) {
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
