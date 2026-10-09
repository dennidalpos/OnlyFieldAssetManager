package com.onlyfield.assetmanager.core.validation

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.display.toDisplayString
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

    fun validateProject(project: Project, i18n: Messages = Messages()): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        validateFiniteNumbers(project, issues, i18n)
        val seenIds = mutableSetOf<String>()
        issues += ObjectHierarchy.errors(project, i18n = i18n).map { ValidationIssue("INVALID_OBJECT_CONTAINMENT", it, ValidationSeverity.STRUCTURAL_ERROR) }

        checkUuid("INVALID_PROJECT_UUID", project.id, i18n.text("text.02a97e1a7888"), issues)
        trackId(project.id, "DUPLICATE_PROJECT_ID", i18n.text("text.ab40e25c2fe0", project.id), seenIds, issues)

        val racksById = mutableMapOf<String, Rack>()
        for (rack in project.racks) {
            checkUuid("INVALID_RACK_UUID", rack.id, i18n.text("text.739f725784ad"), issues)
            trackId(rack.id, "DUPLICATE_RACK_ID", i18n.text("text.2c155aba5959", rack.id), seenIds, issues)
            if (rack.heightU <= 0) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_RACK_HEIGHT",
                        message = i18n.text("text.2494bd604387", rack.name),
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = rack.id
                    )
                )
            }
            racksById[rack.id] = rack
        }

        for (model in project.deviceModels) {
            checkUuid("INVALID_MODEL_UUID", model.id, i18n.text("text.e704f269fdf5"), issues)
            trackId(model.id, "DUPLICATE_MODEL_ID", i18n.text("text.18975c9e34a8", model.id), seenIds, issues)
            if (!com.onlyfield.assetmanager.core.forms.HardwareConfigurator.validGroups(model.portTemplates) || !com.onlyfield.assetmanager.core.forms.PortArrangement.valid(model.hardware) || model.defaultHeightU !in 1..60) {
                issues.add(ValidationIssue("INVALID_MODEL_HARDWARE", i18n.text("config.invalidHardware"), ValidationSeverity.STRUCTURAL_ERROR, model.id))
            }
        }

        val allPorts = mutableMapOf<String, Port>()
        val allDevices = mutableListOf<Pair<String, Device>>() // Site ID and device.

        for (site in project.sites) {
            checkUuid("INVALID_SITE_UUID", site.id, i18n.text("text.95d1c4267bec"), issues)
            trackId(site.id, "DUPLICATE_SITE_ID", i18n.text("text.cb0e7cb5a360", site.id), seenIds, issues)

            for (area in site.areas) {
                checkUuid("INVALID_AREA_UUID", area.id, i18n.text("text.b4bcc3ce0e37"), issues)
                trackId(area.id, "DUPLICATE_AREA_ID", i18n.text("text.951d5d37b4da", area.id), seenIds, issues)
            }

            for (device in site.devices) {
                allDevices.add(site.id to device)
                checkUuid("INVALID_DEVICE_UUID", device.id, i18n.text("text.7bcae484093f"), issues)
                trackId(device.id, "DUPLICATE_DEVICE_ID", i18n.text("text.8b3f7ead59e2", device.id), seenIds, issues)

                if ((device.areaId == null) && (device.rackId == null)) {
                    issues.add(
                        ValidationIssue(
                            code = "UNPOSITIONED_DEVICE",
                            message = i18n.text("text.1602c55be9b1", device.technicalName),
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = device.id
                        )
                    )
                }

                device.rackId?.let { rackId ->
                    val rack = racksById[rackId]
                    if (rack == null) {
                        issues.add(
                            ValidationIssue(
                                code = "INVALID_RACK_REFERENCE",
                                message = i18n.text("text.f01c47e831cf", device.technicalName),
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
                                        message = i18n.text("text.94af6773b8a9", device.technicalName, pos, topU, rack.name, rack.heightU),
                                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                                        targetEntityId = device.id
                                    )
                                )
                            }
                        }
                    }
                }

                if (device.observation.effectiveStatus() == ObservationStatus.TO_VERIFY ||
                    device.observation.effectiveStatus() == ObservationStatus.CONFLICT
                ) {
                    issues.add(
                        ValidationIssue(
                            code = "UNVERIFIED_DEVICE_OBSERVATION",
                            message = i18n.text("text.9c2ca030d9f5", device.technicalName, device.observation.effectiveStatus().toDisplayString(i18n = i18n).lowercase(i18n.locale)),
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = device.id
                        )
                    )
                }

                if (!com.onlyfield.assetmanager.core.forms.HardwareConfigurator.validGroups(device.hardware.portGroups) || !com.onlyfield.assetmanager.core.forms.PortArrangement.valid(device.hardware)) {
                    issues.add(ValidationIssue("INVALID_DEVICE_HARDWARE", i18n.text("config.invalidHardware"), ValidationSeverity.STRUCTURAL_ERROR, device.id))
                }
                for (port in device.ports) {
                    checkUuid("INVALID_PORT_UUID", port.id, i18n.text("text.06e78b6179b8"), issues)
                    trackId(port.id, "DUPLICATE_PORT_ID", i18n.text("text.558ee3bae10a", port.id), seenIds, issues)

                    if (port.deviceId != device.id) {
                        issues.add(
                            ValidationIssue(
                                code = "PORT_DEVICE_MISMATCH",
                                message = i18n.text("text.dceb0794e6d6", port.name, device.technicalName),
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
                                message = i18n.text("text.6d8683663196", port.name, device.technicalName),
                                severity = ValidationSeverity.DOCUMENTARY_WARNING,
                                targetEntityId = port.id
                            )
                        )
                    }
                }
            }
        }

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

                    val uOverlaps = kotlin.math.max(pos1Start, pos2Start) <= kotlin.math.min(pos1End, pos2End)
                    val sideOverlaps = d1.rackSide == RackSide.BOTH || d2.rackSide == RackSide.BOTH || d1.rackSide == d2.rackSide

                    if (uOverlaps && sideOverlaps) {
                        issues.add(
                            ValidationIssue(
                                code = "RACK_SLOT_OVERLAP",
                                message = i18n.text("text.04eb08c5280e", rackName, d1.technicalName, pos1Start, pos1End, d2.technicalName, pos2Start, pos2End),
                                severity = ValidationSeverity.DOCUMENTARY_WARNING,
                                targetEntityId = d1.id
                            )
                        )
                    }
                }
            }
        }

        val connectionGraph = ConnectionGraph(project)
        for (port in allPorts.values) {
            if (connectionGraph.state(port.id) == ConnectionState.CONFLICT) {
                issues.add(ValidationIssue("PHYSICAL_CONNECTION_CONFLICT", i18n.text("config.conflict"), ValidationSeverity.DOCUMENTARY_WARNING, port.id))
            }
        }

        val devicesBySite = allDevices.groupBy { it.first }
        for ((siteId, siteDevices) in devicesBySite) {
            val namesInSite = mutableMapOf<String, String>() // name -> deviceId
            val ipsInSite = mutableMapOf<String, String>()   // ip -> deviceId

            for ((_, device) in siteDevices) {
                val existingNameDeviceId = namesInSite[device.technicalName.lowercase()]
                if (existingNameDeviceId != null) {
                    issues.add(
                        ValidationIssue(
                            code = "DUPLICATE_DEVICE_NAME_IN_SITE",
                            message = i18n.text("text.e18788ee03d7", device.technicalName),
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = device.id
                        )
                    )
                } else {
                    namesInSite[device.technicalName.lowercase()] = device.id
                }

                device.ipAddress?.let { ip ->
                    if (ip.isNotBlank()) {
                        val existingIpDeviceId = ipsInSite[ip]
                        if (existingIpDeviceId != null) {
                            issues.add(
                                ValidationIssue(
                                    code = "DUPLICATE_IP_IN_SITE",
                                    message = i18n.text("text.2c04c9e1b0bf", ip),
                                    severity = ValidationSeverity.DOCUMENTARY_WARNING,
                                    targetEntityId = device.id
                                )
                            )
                        } else {
                            ipsInSite[ip] = device.id
                        }
                    }
                }
            }
        }

        for (cred in project.credentials) {
            checkUuid("INVALID_CREDENTIAL_UUID", cred.id, i18n.text("text.c1257b966e13"), issues)
            trackId(cred.id, "DUPLICATE_CREDENTIAL_ID", i18n.text("text.46f1ab481fed", cred.id), seenIds, issues)

            if (cred.username.isBlank()) {
                issues.add(
                    ValidationIssue(
                        code = "BLANK_CREDENTIAL_USERNAME",
                        message = i18n.text("text.bc9c4df6e567"),
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = cred.id
                    )
                )
            }
        }

        val attachmentsById = project.attachments.associateBy { it.id }
        for (att in project.attachments) {
            checkUuid("INVALID_ATTACHMENT_UUID", att.id, i18n.text("text.44d4461cb53d"), issues)
            trackId(att.id, "DUPLICATE_ATTACHMENT_ID", i18n.text("text.377ad7bf34ba", att.id), seenIds, issues)

            if (att.classification == com.onlyfield.assetmanager.core.model.AttachmentClassification.REVIEW_REQUIRED) {
                issues.add(
                    ValidationIssue(
                        code = "ATTACHMENT_NEEDS_REVIEW",
                        message = i18n.text("text.440e04f7570f", att.name),
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = att.id
                    )
                )
            }
        }

        val allAreaIds = mutableSetOf<String>()
        for (site in project.sites) {
            for (area in site.areas) {
                allAreaIds.add(area.id)
                area.floorplanAttachmentId?.let { fpId ->
                    if (!attachmentsById.containsKey(fpId)) {
                        issues.add(
                            ValidationIssue(
                                code = "INVALID_FLOORPLAN_ATTACHMENT",
                                message = i18n.text("text.8fd88b9ec7a0", area.name, fpId),
                                severity = ValidationSeverity.STRUCTURAL_ERROR,
                                targetEntityId = area.id
                            )
                        )
                    }
                }
            }
        }

        val allDeviceIds = allDevices.map { it.second.id }.toSet()
        val allRackIds = racksById.keys
        for (placement in project.floorplanPlacements) {
            checkUuid("INVALID_PLACEMENT_UUID", placement.id, i18n.text("text.5b35f8c18c02"), issues)
            trackId(placement.id, "DUPLICATE_PLACEMENT_ID", i18n.text("text.b884c2aa0bae", placement.id), seenIds, issues)

            if (!allAreaIds.contains(placement.areaId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_PLACEMENT_AREA",
                        message = i18n.text("text.fbb9417724c8"),
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
                        message = i18n.text("text.fb455bb6066e"),
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = placement.id
                    )
                )
            }

            if (placement.xRatio < 0f || placement.xRatio > 1f || placement.yRatio < 0f || placement.yRatio > 1f) {
                issues.add(
                    ValidationIssue(
                        code = "PLACEMENT_OUT_OF_BOUNDS",
                        message = i18n.text("text.becac17fa6bd"),
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = placement.id
                    )
                )
            }
        }

        for (type in project.objectTypes) {
            checkUuid("INVALID_OBJECT_TYPE_UUID", type.id, i18n.text("text.1d0fbaeb70a5"), issues)
            trackId(type.id, "DUPLICATE_OBJECT_TYPE_ID", i18n.text("text.7849faba6efd"), seenIds, issues)
            if (type.name.isBlank() || com.onlyfield.assetmanager.core.model.ObjectCatalog.builtins.any { it.id == type.id }) {
                issues += ValidationIssue("INVALID_OBJECT_TYPE", i18n.text("text.da1acad0ecac"), ValidationSeverity.STRUCTURAL_ERROR, type.id)
            }
        }
        val routeKeys = mutableSetOf<Pair<String, String>>()
        for (route in project.cableRoutes) {
            checkUuid("INVALID_CABLE_ROUTE_UUID", route.id, i18n.text("text.4f9f45a15a81"), issues)
            trackId(route.id, "DUPLICATE_CABLE_ROUTE_ID", i18n.text("text.44b8403ac8aa"), seenIds, issues)
            if (!routeKeys.add(route.cableId to route.areaId) || route.areaId !in allAreaIds || project.cables.none { it.id == route.cableId }) {
                issues += ValidationIssue("INVALID_CABLE_ROUTE", i18n.text("text.c5fa6ea2eacd"), ValidationSeverity.STRUCTURAL_ERROR, route.id)
            }
            if (route.points.size < 2 || route.points.any { !it.x.isFinite() || !it.y.isFinite() || it.x !in 0f..1f || it.y !in 0f..1f }) {
                issues += ValidationIssue("INVALID_CABLE_ROUTE_POINTS", i18n.text("text.44aa633d518d"), ValidationSeverity.STRUCTURAL_ERROR, route.id)
            }
        }
        for (cable in project.cables) {
            if (listOfNotNull(cable.deviceAId, cable.deviceBId).any { it !in allDeviceIds }) {
                issues += ValidationIssue("CABLE_DEVICE_TO_VERIFY", i18n.text("text.8c99fd4d3b61"), ValidationSeverity.DOCUMENTARY_WARNING, cable.id)
            }
        }

        for (ann in project.annotations) {
            checkUuid("INVALID_ANNOTATION_UUID", ann.id, i18n.text("text.89907d0dbe9a"), issues)
            trackId(ann.id, "DUPLICATE_ANNOTATION_ID", i18n.text("text.e179516c1b1b", ann.id), seenIds, issues)

            if (!allAreaIds.contains(ann.areaId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_ANNOTATION_AREA",
                        message = i18n.text("text.fbf734680fb7", ann.id, ann.areaId),
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = ann.id
                    )
                )
            }

            if (ann.classification == com.onlyfield.assetmanager.core.model.AttachmentClassification.REVIEW_REQUIRED) {
                issues.add(
                    ValidationIssue(
                        code = "ANNOTATION_NEEDS_REVIEW",
                        message = i18n.text("text.f42121cecd50", ann.id),
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = ann.id
                    )
                )
            }
        }

        for (cable in project.cables) {
            checkUuid("INVALID_CABLE_UUID", cable.id, i18n.text("text.c879515e30e7"), issues)
            trackId(cable.id, "DUPLICATE_CABLE_ID", i18n.text("text.eebfcf38b5b5", cable.id), seenIds, issues)

            if ((cable.portAId == null && cable.deviceAId == null) || (cable.portBId == null && cable.deviceBId == null)) {
                issues.add(
                    ValidationIssue(
                        code = "DETACHED_CABLE_ENDPOINT",
                        message = i18n.text("text.a7178c87cd4a", cable.codeOrLabel ?: cable.id),
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
                            message = i18n.text("text.8be3e37c6021", cable.codeOrLabel ?: cable.id, portA),
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
                            message = i18n.text("text.ecadbb52c901", cable.codeOrLabel ?: cable.id, portB),
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = cable.id
                        )
                    )
                }
            }

            if (cable.observation?.status == ObservationStatus.TO_VERIFY ||
                cable.observation?.status == ObservationStatus.CONFLICT
            ) {
                issues.add(
                    ValidationIssue(
                        code = "UNVERIFIED_CABLE",
                        message = i18n.text("text.aa25688be809", cable.codeOrLabel ?: cable.id, cable.observation.status.toDisplayString(i18n = i18n).lowercase()),
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = cable.id
                    )
                )
            }
        }

        for (mapping in project.panelMappings) {
            checkUuid("INVALID_PANEL_MAPPING_UUID", mapping.id, i18n.text("text.7dc146223b7b"), issues)
            trackId(mapping.id, "DUPLICATE_PANEL_MAPPING_ID", i18n.text("text.04f94f16bbbf", mapping.id), seenIds, issues)

            if (!allPorts.containsKey(mapping.portAId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_PORT_REFERENCE",
                        message = i18n.text("text.06302f2683ef", mapping.id, mapping.portAId),
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
                            message = i18n.text("text.38db947816ca", mapping.id, portB),
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
                        message = i18n.text("text.9acd0880c983", mapping.id),
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = mapping.id
                    )
                )
            }
        }

        val vlanNumbersByScope = mutableMapOf<String, MutableSet<Int>>()
        val knownVlanIds = project.vlans.map { it.vlanId }.toSet()
        val vlanIds = project.vlans.map { it.id }.toSet()
        val siteIds = project.sites.map { it.id }.toSet()
        val deviceIds = allDevices.map { it.second.id }.toSet()
        fun validateNetworkScope(type: VlanScopeType, target: String?, id: String) {
            if (type == VlanScopeType.PROJECT && target == null) return
            if (type != VlanScopeType.PROJECT && target.isNullOrBlank()) {
                issues += ValidationIssue("MISSING_NETWORK_SCOPE_TARGET", i18n.text("network.scopeMissing"), ValidationSeverity.DOCUMENTARY_WARNING, id)
                return
            }
            val valid = when (type) {
                VlanScopeType.PROJECT -> target == project.id
                VlanScopeType.SITE -> target in siteIds
                VlanScopeType.DEVICE -> target in deviceIds
            }
            if (!valid) issues += ValidationIssue("INVALID_NETWORK_SCOPE_TARGET", i18n.text("network.scopeInvalid"), ValidationSeverity.STRUCTURAL_ERROR, id)
        }

        for (vlan in project.vlans) {
            validateNetworkScope(vlan.scopeType, vlan.scopeTargetId, vlan.id)
            checkUuid("INVALID_VLAN_UUID", vlan.id, i18n.text("text.f1620ea04aeb"), issues)
            trackId(vlan.id, "DUPLICATE_VLAN_ID", i18n.text("text.01f8a71e9988", vlan.id), seenIds, issues)

            if (vlan.vlanId !in 1..4094) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_VLAN_NUMBER",
                        message = i18n.text("text.9598660cddac", vlan.vlanId),
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
                        message = i18n.text("text.f8b8257476de", vlan.vlanId, scopeKey),
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = vlan.id
                    )
                )
            } else {
                scopeSet.add(vlan.vlanId)
            }
        }

        for (subnet in project.subnets) {
            validateNetworkScope(subnet.scopeType, subnet.scopeTargetId, subnet.id)
            if (subnet.vlanId != null && subnet.vlanId !in vlanIds) {
                issues += ValidationIssue("INVALID_SUBNET_VLAN_REFERENCE", i18n.text("network.vlanInvalid"), ValidationSeverity.STRUCTURAL_ERROR, subnet.id)
            }
            checkUuid("INVALID_SUBNET_UUID", subnet.id, i18n.text("text.61df76270ad5"), issues)
            trackId(subnet.id, "DUPLICATE_SUBNET_ID", i18n.text("text.481a7568314f", subnet.id), seenIds, issues)

            if (com.onlyfield.assetmanager.core.forms.FieldValidators.cidr(subnet.cidrBlock, required = true, i18n = i18n) != null) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_SUBNET_CIDR",
                        message = i18n.text("text.e090d62efeb9", subnet.cidrBlock),
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = subnet.id
                    )
                )
            }
        }

        for (membership in project.portVlanMemberships) {
            checkUuid("INVALID_PORT_VLAN_MEMBERSHIP_UUID", membership.id, i18n.text("text.17b7672895e6"), issues)
            trackId(membership.id, "DUPLICATE_PORT_VLAN_MEMBERSHIP_ID", i18n.text("text.587e47123a02", membership.id), seenIds, issues)

            if (!allPorts.containsKey(membership.portId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_PORT_REFERENCE",
                        message = i18n.text("text.da890bb0a3f9", membership.portId),
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
                            message = i18n.text("text.1bce9e95935a", vlanId),
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
                            message = i18n.text("text.4d3fbe7a4f19", tvId),
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = membership.id
                        )
                    )
                }
            }
        }

        for (l3Int in project.logicalInterfaces) {
            checkUuid("INVALID_LOGICAL_INTERFACE_UUID", l3Int.id, i18n.text("text.352cc3385a7d"), issues)
            trackId(l3Int.id, "DUPLICATE_LOGICAL_INTERFACE_ID", i18n.text("text.ee0a1b225328", l3Int.id), seenIds, issues)

            if (!allDeviceIds.contains(l3Int.deviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_DEVICE_REFERENCE",
                        message = i18n.text("text.4333a07a8c7e", l3Int.name, l3Int.deviceId),
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = l3Int.id
                    )
                )
            }
        }

        for (lag in project.lagGroups) {
            checkUuid("INVALID_LAG_GROUP_UUID", lag.id, i18n.text("text.afe6ae0705a1"), issues)
            trackId(lag.id, "DUPLICATE_LAG_GROUP_ID", i18n.text("text.975f7803ace9", lag.id), seenIds, issues)

            if (!allDeviceIds.contains(lag.deviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_DEVICE_REFERENCE",
                        message = i18n.text("text.b0bce70a5331", lag.name, lag.deviceId),
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
                            message = i18n.text("text.1bd46f15eba4", lag.name, pId),
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = lag.id
                        )
                    )
                }
            }
        }

        for (config in project.deviceConfigurations) {
            checkUuid("INVALID_DEVICE_CONFIG_UUID", config.id, i18n.text("text.0cc55dc2828d"), issues)
            trackId(config.id, "DUPLICATE_DEVICE_CONFIG_ID", i18n.text("text.768e86492ada", config.id), seenIds, issues)

            if (!allDeviceIds.contains(config.deviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_DEVICE_REFERENCE",
                        message = i18n.text("text.79c51cc879a5", config.title, config.deviceId),
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
                            message = i18n.text("text.677a6c1a51fe", config.title, attId),
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = config.id
                        )
                    )
                }
            }
        }

        for (conn in project.wanVpnConnections) {
            checkUuid("INVALID_WAN_VPN_UUID", conn.id, i18n.text("text.d652ce41458f"), issues)
            trackId(conn.id, "DUPLICATE_WAN_VPN_ID", i18n.text("text.d6d376f48002", conn.id), seenIds, issues)

            conn.localEndpointDeviceId?.let { devId ->
                if (!allDeviceIds.contains(devId)) {
                    issues.add(
                        ValidationIssue(
                            code = "INVALID_DEVICE_REFERENCE",
                            message = i18n.text("text.a67084e3551f", conn.name, devId),
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
                            message = i18n.text("text.22860f412529", conn.name, devId),
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = conn.id
                        )
                    )
                }
            }

            // Documentary only: incomplete links are allowed while surveying.
            val described = listOf(conn.localEndpointDeviceId, conn.localEndpointSiteDescription, conn.remoteEndpointDeviceId, conn.remoteEndpointSiteDescription).any { !it.isNullOrBlank() }
            if (!described) issues.add(ValidationIssue("WAN_VPN_WITHOUT_ENDPOINTS", i18n.text("validation.wanVpnNoEndpoints", conn.name), ValidationSeverity.DOCUMENTARY_WARNING, conn.id))
            if (conn.localEndpointDeviceId != null && conn.localEndpointDeviceId == conn.remoteEndpointDeviceId)
                issues.add(ValidationIssue("WAN_VPN_SAME_DEVICE", i18n.text("validation.wanVpnSameDevice", conn.name), ValidationSeverity.DOCUMENTARY_WARNING, conn.id))
        }

        for (video in project.videoSurveillanceMappings) {
            checkUuid("INVALID_VIDEO_MAPPING_UUID", video.id, i18n.text("text.bee37c27f111"), issues)
            trackId(video.id, "DUPLICATE_VIDEO_MAPPING_ID", i18n.text("text.5d71ad933b74", video.id), seenIds, issues)

            if (!allDeviceIds.contains(video.cameraDeviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_DEVICE_REFERENCE",
                        message = i18n.text("text.6a6357c88fe7", video.cameraDeviceId),
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
                            message = i18n.text("text.18f187c335c9", devId),
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = video.id
                        )
                    )
                }
            }
        }

        for (field in project.customExtraFields) {
            checkUuid("INVALID_CUSTOM_FIELD_UUID", field.id, i18n.text("text.1fc3d96ea2ea"), issues)
            trackId(field.id, "DUPLICATE_CUSTOM_FIELD_ID", i18n.text("text.b092fca6fb3c", field.id), seenIds, issues)

            if (field.classification == com.onlyfield.assetmanager.core.model.AttachmentClassification.REVIEW_REQUIRED) {
                issues.add(
                    ValidationIssue(
                        code = "CUSTOM_FIELD_NEEDS_REVIEW",
                        message = i18n.text("text.3523a7339a55", field.fieldKey),
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = field.id
                    )
                )
            }
        }

        val allPortIds = allDevices.flatMap { it.second.ports }.map { it.id }.toSet()
        val powerFeedsByDevice = project.powerFeeds.groupBy { it.deviceId }
        if (project.powerFeeds.hasPowerFeedCycle()) issues.add(ValidationIssue(
            code = "POWER_FEED_CYCLE_DETECTED",
            message = i18n.text("validation.powerCycle"),
            severity = ValidationSeverity.STRUCTURAL_ERROR,
        ))
        for (feed in project.powerFeeds) {
            checkUuid("INVALID_POWER_FEED_UUID", feed.id, i18n.text("text.a10fba81028f"), issues)
            trackId(feed.id, "DUPLICATE_POWER_FEED_ID", i18n.text("text.09783358748b", feed.id), seenIds, issues)

            if (!allDeviceIds.contains(feed.deviceId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_POWER_FEED_DEVICE",
                        message = i18n.text("text.42e37de799be", feed.feedName, feed.deviceId),
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
                            message = i18n.text("text.1059883f726a", feed.feedName, srcId),
                            severity = ValidationSeverity.STRUCTURAL_ERROR,
                            targetEntityId = feed.id
                        )
                    )
                }
            }

            if (feed.observedRuntimeMinutes != null && (feed.observedSource.isNull_or_blank() || feed.observedEpochMs == null)) {
                issues.add(
                    ValidationIssue(
                        code = "CALCULATED_AUTONOMIA_PROHIBITED_WARNING",
                        message = i18n.text("text.2a538889f18e", feed.observedRuntimeMinutes),
                        severity = ValidationSeverity.DOCUMENTARY_WARNING,
                        targetEntityId = feed.id
                    )
                )
            }
        }

        for ((_, device) in allDevices) {
            val feeds = powerFeedsByDevice[device.id] ?: emptyList()
            if (feeds.isNotEmpty() && device.category != com.onlyfield.assetmanager.core.model.DeviceCategory.SHELF && device.category != com.onlyfield.assetmanager.core.model.DeviceCategory.BLANK_PANEL) {
                val hasFeedA = feeds.any { it.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.PRIMARY_A }
                val hasFeedB = feeds.any { it.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.SECONDARY_B }
                if (!(hasFeedA && hasFeedB)) {
                    issues.add(
                        ValidationIssue(
                            code = "SINGLE_FEED_PARTIAL_COVERAGE_WARNING",
                            message = i18n.text("text.423c29b5d100", device.technicalName),
                            severity = ValidationSeverity.DOCUMENTARY_WARNING,
                            targetEntityId = device.id
                        )
                    )
                }
            }
        }

        for (poe in project.poeMappings) {
            checkUuid("INVALID_POE_MAPPING_UUID", poe.id, i18n.text("text.b9bb89ac9d9e"), issues)
            trackId(poe.id, "DUPLICATE_POE_MAPPING_ID", i18n.text("text.b72c3e18b174", poe.id), seenIds, issues)

            if (!allPortIds.contains(poe.portId)) {
                issues.add(
                    ValidationIssue(
                        code = "INVALID_POE_PORT_REFERENCE",
                        message = i18n.text("text.d03f6e073cda", poe.portId),
                        severity = ValidationSeverity.STRUCTURAL_ERROR,
                        targetEntityId = poe.id
                    )
                )
            }
        }

        for (badge in project.documentBadges) {
            checkUuid("INVALID_DOCUMENT_BADGE_UUID", badge.id, i18n.text("text.41c5560480fd"), issues)
            trackId(badge.id, "DUPLICATE_DOCUMENT_BADGE_ID", i18n.text("text.d9a9317b5874", badge.id), seenIds, issues)
        }

        return ValidationResult(issues)
    }

    fun deriveBadges(project: Project, targetType: String, targetId: String, i18n: Messages = Messages()): List<com.onlyfield.assetmanager.core.model.DocumentBadge> {
        val result = mutableListOf<com.onlyfield.assetmanager.core.model.DocumentBadge>()

        val freeLabels = project.documentBadges.filter {
            it.targetType == targetType && it.targetId == targetId && !it.isDerived
        }
        result.addAll(freeLabels)

        when (targetType) {
            "DEVICE" -> {
                val device = project.sites.flatMap { it.devices }.firstOrNull { it.id == targetId }
                if (device != null) {
                    val portIds = device.ports.map { it.id }.toSet()

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
                                label = i18n.text("text.b6d96a03ea99", vlanIds.sorted().joinToString()),
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.VLAN,
                                isDerived = true
                            )
                        )
                    }

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
                                label = i18n.text("text.c576c7498dba", media.joinToString()),
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.MEDIUM,
                                isDerived = true
                            )
                        )
                    }

                    val poes = project.poeMappings.filter { portIds.contains(it.portId) }
                    if (poes.isNotEmpty()) {
                        val poeText = poes.map { "${it.role} ${it.standard}" }.distinct().joinToString()
                        result.add(
                            com.onlyfield.assetmanager.core.model.DocumentBadge(
                                targetType = targetType,
                                targetId = targetId,
                                label = i18n.text("text.80fbc2776190", poeText),
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.POE,
                                isDerived = true
                            )
                        )
                    }

                    val feeds = project.powerFeeds.filter { it.deviceId == targetId }
                    if (feeds.isNotEmpty()) {
                        val hasA = feeds.any { it.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.PRIMARY_A }
                        val hasB = feeds.any { it.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.SECONDARY_B }
                        val covLabel = if (hasA && hasB) i18n.text("text.a19df1843eae") else i18n.text("text.424fa56accd4")
                        result.add(
                            com.onlyfield.assetmanager.core.model.DocumentBadge(
                                targetType = targetType,
                                targetId = targetId,
                                label = covLabel,
                                category = com.onlyfield.assetmanager.core.model.BadgeCategory.COVERAGE,
                                isDerived = true
                            )
                        )

                        val hasUps = feeds.any { f ->
                            f.feedType == com.onlyfield.assetmanager.core.model.PowerFeedType.UPS_BACKUP ||
                                    (f.sourceDeviceId != null && project.sites.flatMap { it.devices }
                                        .firstOrNull { it.id == f.sourceDeviceId }?.category == com.onlyfield.assetmanager.core.model.DeviceCategory.UPS_PDU)
                        }
                        val upsLabel = if (hasUps) i18n.text("text.d65d2454047d") else i18n.text("text.dbdeacd918a5")
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

                    if (device.observation?.status == com.onlyfield.assetmanager.core.model.ObservationStatus.TO_VERIFY ||
                        device.observation?.status == com.onlyfield.assetmanager.core.model.ObservationStatus.CONFLICT
                    ) {
                        result.add(
                            com.onlyfield.assetmanager.core.model.DocumentBadge(
                                targetType = targetType,
                                targetId = targetId,
                                label = i18n.text("text.e5205cc54a13", device.observation.status.toDisplayString(i18n = i18n).lowercase()),
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
                    membership.untaggedVlanId?.let { vlans.add(i18n.text("text.5d4192a73511", it)) }
                    if (membership.taggedVlanIds.isNotEmpty()) {
                        vlans.add(i18n.text("text.29697e42d0d3", membership.taggedVlanIds.joinToString()))
                    }
                    result.add(
                        com.onlyfield.assetmanager.core.model.DocumentBadge(
                            targetType = targetType,
                            targetId = targetId,
                            label = i18n.text("text.b6d96a03ea99", vlans.joinToString(", ")),
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
                            label = i18n.text("text.c0e033a33b08", poe.role, poe.standard),
                            category = com.onlyfield.assetmanager.core.model.BadgeCategory.POE,
                            isDerived = true
                        )
                    )
                }
            }
        }

        return result
    }

    fun validateMergeTargets(survivingDeviceId: String, duplicateDeviceId: String, i18n: Messages = Messages()): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        if (survivingDeviceId == duplicateDeviceId) {
            issues.add(
                ValidationIssue(
                    code = "CANNOT_MERGE_SAME_DEVICE",
                    message = i18n.text("text.2a50b6ed4103"),
                    severity = ValidationSeverity.STRUCTURAL_ERROR,
                    targetEntityId = survivingDeviceId
                )
            )
        }
        return ValidationResult(issues)
    }

    fun generateBatchEditPreview(devices: List<Device>, changes: BatchDeviceChanges, i18n: Messages = Messages()): BatchEditPreview {
        val summaries = mutableListOf<String>()
        if (changes.updateAreaId) summaries.add(i18n.text("text.a8fa7b3696e9", changes.areaId ?: i18n.text("text.f56b9cfaeb27")))
        if (changes.updateCategory) summaries.add(i18n.text("text.8a307dae951d", changes.category ?: i18n.text("text.7925666e5976")))
        if (changes.updateRackId) summaries.add(i18n.text("text.badf46c13b7d", changes.rackId ?: i18n.text("text.da968f7d518f")))
        if (changes.updateMountingType) summaries.add(i18n.text("text.0a2d16e9070e", changes.mountingType))
        if (changes.updateObservationNotes) summaries.add(i18n.text("text.5f2d69b5aec2"))

        return BatchEditPreview(
            targetDeviceIds = devices.map { it.id },
            affectedDeviceNames = devices.map { it.technicalName },
            changesSummary = summaries,
            isProhibitedFieldAttempted = false
        )
    }

    fun requireFiniteNumbers(project: Project, i18n: Messages = Messages()) {
        val issues = mutableListOf<ValidationIssue>()
        validateFiniteNumbers(project, issues, i18n)
        require(issues.isEmpty()) { issues.joinToString("; ") { it.message } }
    }

    private fun validateFiniteNumbers(project: Project, issues: MutableList<ValidationIssue>, i18n: Messages) {
        fun check(id: String, field: String, vararg values: Double?) {
            if (values.any { it != null && !it.isFinite() }) {
                issues += ValidationIssue("NON_FINITE_NUMBER", i18n.text("validation.finite", field), ValidationSeverity.STRUCTURAL_ERROR, id)
            }
        }
        project.sites.flatMap { it.devices }.forEach { check(it.id, i18n.text("config.poeBudget"), it.hardware.poeBudgetWatts) }
        project.deviceModels.forEach { check(it.id, i18n.text("config.poeBudget"), it.hardware.poeBudgetWatts) }
        project.cables.forEach { check(it.id, i18n.text("config.length"), it.lengthValue) }
        project.powerFeeds.forEach {
            check(it.id, i18n.text("text.eb98296d7970"), it.loadWatts)
            check(it.id, i18n.text("text.e821b548ca4b"), it.loadVa)
        }
        project.poeMappings.forEach { check(it.id, i18n.text("text.548f9030240c"), it.allocatedPowerWatts) }
        project.floorplanPlacements.forEach { check(it.id, i18n.text("validation.coordinates"), it.xRatio.toDouble(), it.yRatio.toDouble()) }
        project.annotations.forEach { check(it.id, i18n.text("validation.coordinates"), it.x1Ratio.toDouble(), it.y1Ratio.toDouble(), it.x2Ratio.toDouble(), it.y2Ratio.toDouble()) }
        project.cableRoutes.forEach { route -> route.points.forEach { check(route.id, i18n.text("validation.coordinates"), it.x.toDouble(), it.y.toDouble()) } }
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
