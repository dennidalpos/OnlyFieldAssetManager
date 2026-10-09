package com.onlyfield.assetmanager.data.repository.mappers

import com.onlyfield.assetmanager.core.model.Area
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.core.model.Credential
import com.onlyfield.assetmanager.core.model.CredentialType
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.core.model.DeviceModel
import com.onlyfield.assetmanager.core.model.EndpointStatus
import com.onlyfield.assetmanager.core.model.MountingType
import com.onlyfield.assetmanager.core.model.NumberingDirection
import com.onlyfield.assetmanager.core.model.Observation
import com.onlyfield.assetmanager.core.model.ObservationStatus
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.PortTemplate
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.model.RackSide
import com.onlyfield.assetmanager.data.local.AreaEntity
import com.onlyfield.assetmanager.data.local.SiteEntity
import com.onlyfield.assetmanager.data.local.CredentialEntity
import com.onlyfield.assetmanager.data.local.DeviceEntity
import com.onlyfield.assetmanager.data.local.DeviceModelEntity
import com.onlyfield.assetmanager.data.local.PortEntity
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.local.RackEntity
import kotlinx.serialization.json.Json

// Room <-> domain mapping: VLANs, subnets, L3 interfaces, LAGs, WAN/VPN, configurations, extra fields.

internal fun toVlanEntity(projectId: String, vlan: com.onlyfield.assetmanager.core.model.Vlan): com.onlyfield.assetmanager.data.local.VlanEntity {
    return com.onlyfield.assetmanager.data.local.VlanEntity(
        id = vlan.id,
        projectId = projectId,
        vlanId = vlan.vlanId,
        name = vlan.name,
        scopeType = vlan.scopeType.name,
        scopeTargetId = vlan.scopeTargetId,
        description = vlan.description
    )
}

internal fun toVlan(entity: com.onlyfield.assetmanager.data.local.VlanEntity): com.onlyfield.assetmanager.core.model.Vlan {
    return com.onlyfield.assetmanager.core.model.Vlan(
        id = entity.id,
        vlanId = entity.vlanId,
        name = entity.name,
        scopeType = com.onlyfield.assetmanager.core.model.VlanScopeType.valueOf(entity.scopeType),
        scopeTargetId = entity.scopeTargetId,
        description = entity.description
    )
}

internal fun toSubnetEntity(projectId: String, subnet: com.onlyfield.assetmanager.core.model.Subnet): com.onlyfield.assetmanager.data.local.SubnetEntity {
    return com.onlyfield.assetmanager.data.local.SubnetEntity(
        id = subnet.id,
        projectId = projectId,
        cidrBlock = subnet.cidrBlock,
        gatewayIp = subnet.gatewayIp,
        vlanId = subnet.vlanId,
        name = subnet.name,
        scopeType = subnet.scopeType.name,
        scopeTargetId = subnet.scopeTargetId,
        description = subnet.description
    )
}

internal fun toSubnet(entity: com.onlyfield.assetmanager.data.local.SubnetEntity): com.onlyfield.assetmanager.core.model.Subnet {
    return com.onlyfield.assetmanager.core.model.Subnet(
        id = entity.id,
        cidrBlock = entity.cidrBlock,
        gatewayIp = entity.gatewayIp,
        vlanId = entity.vlanId,
        name = entity.name,
        scopeType = com.onlyfield.assetmanager.core.model.VlanScopeType.valueOf(entity.scopeType),
        scopeTargetId = entity.scopeTargetId,
        description = entity.description
    )
}

internal fun toPortVlanMembershipEntity(projectId: String, membership: com.onlyfield.assetmanager.core.model.PortVlanMembership): com.onlyfield.assetmanager.data.local.PortVlanMembershipEntity {
    return com.onlyfield.assetmanager.data.local.PortVlanMembershipEntity(
        id = membership.id,
        projectId = projectId,
        portId = membership.portId,
        mode = membership.mode.name,
        untaggedVlanId = membership.untaggedVlanId,
        taggedVlanIdsJson = mapperJson.encodeToString(membership.taggedVlanIds),
        nativeVlanId = membership.nativeVlanId,
        notes = membership.notes
    )
}

internal fun toPortVlanMembership(entity: com.onlyfield.assetmanager.data.local.PortVlanMembershipEntity): com.onlyfield.assetmanager.core.model.PortVlanMembership {
    val tagged = mapperJson.decodeFromString<List<Int>>(entity.taggedVlanIdsJson)
    return com.onlyfield.assetmanager.core.model.PortVlanMembership(
        id = entity.id,
        portId = entity.portId,
        mode = com.onlyfield.assetmanager.core.model.PortVlanMode.valueOf(entity.mode),
        untaggedVlanId = entity.untaggedVlanId,
        taggedVlanIds = tagged,
        nativeVlanId = entity.nativeVlanId,
        notes = entity.notes
    )
}

internal fun toLogicalInterfaceEntity(projectId: String, l3Int: com.onlyfield.assetmanager.core.model.LogicalInterface): com.onlyfield.assetmanager.data.local.LogicalInterfaceEntity {
    return com.onlyfield.assetmanager.data.local.LogicalInterfaceEntity(
        id = l3Int.id,
        projectId = projectId,
        deviceId = l3Int.deviceId,
        name = l3Int.name,
        ipAddress = l3Int.ipAddress,
        subnetCidr = l3Int.subnetCidr,
        vlanId = l3Int.vlanId,
        isL3 = l3Int.isL3,
        macAddress = l3Int.macAddress,
        notes = l3Int.notes
    )
}

internal fun toLogicalInterface(entity: com.onlyfield.assetmanager.data.local.LogicalInterfaceEntity): com.onlyfield.assetmanager.core.model.LogicalInterface {
    return com.onlyfield.assetmanager.core.model.LogicalInterface(
        id = entity.id,
        deviceId = entity.deviceId,
        name = entity.name,
        ipAddress = entity.ipAddress,
        subnetCidr = entity.subnetCidr,
        vlanId = entity.vlanId,
        isL3 = entity.isL3,
        macAddress = entity.macAddress,
        notes = entity.notes
    )
}

internal fun toLagGroupEntity(projectId: String, lag: com.onlyfield.assetmanager.core.model.LagGroup): com.onlyfield.assetmanager.data.local.LagGroupEntity {
    return com.onlyfield.assetmanager.data.local.LagGroupEntity(
        id = lag.id,
        projectId = projectId,
        deviceId = lag.deviceId,
        name = lag.name,
        mode = lag.mode.name,
        memberPortIdsJson = mapperJson.encodeToString(lag.memberPortIds),
        notes = lag.notes
    )
}

internal fun toLagGroup(entity: com.onlyfield.assetmanager.data.local.LagGroupEntity): com.onlyfield.assetmanager.core.model.LagGroup {
    val members = mapperJson.decodeFromString<List<String>>(entity.memberPortIdsJson)
    return com.onlyfield.assetmanager.core.model.LagGroup(
        id = entity.id,
        deviceId = entity.deviceId,
        name = entity.name,
        mode = com.onlyfield.assetmanager.core.model.LagMode.valueOf(entity.mode),
        memberPortIds = members,
        notes = entity.notes
    )
}

internal fun toDeviceConfigurationEntity(projectId: String, config: com.onlyfield.assetmanager.core.model.DeviceConfiguration): com.onlyfield.assetmanager.data.local.DeviceConfigurationEntity {
    return com.onlyfield.assetmanager.data.local.DeviceConfigurationEntity(
        id = config.id,
        projectId = projectId,
        deviceId = config.deviceId,
        title = config.title,
        configText = config.configText,
        attachmentId = config.attachmentId,
        capturedEpochMs = config.capturedEpochMs,
        notes = config.notes
    )
}

internal fun toDeviceConfiguration(entity: com.onlyfield.assetmanager.data.local.DeviceConfigurationEntity): com.onlyfield.assetmanager.core.model.DeviceConfiguration {
    return com.onlyfield.assetmanager.core.model.DeviceConfiguration(
        id = entity.id,
        deviceId = entity.deviceId,
        title = entity.title,
        configText = entity.configText,
        attachmentId = entity.attachmentId,
        capturedEpochMs = entity.capturedEpochMs,
        notes = entity.notes
    )
}

internal fun toWanVpnConnectionEntity(projectId: String, conn: com.onlyfield.assetmanager.core.model.WanVpnConnection): com.onlyfield.assetmanager.data.local.WanVpnConnectionEntity {
    return com.onlyfield.assetmanager.data.local.WanVpnConnectionEntity(
        id = conn.id,
        projectId = projectId,
        name = conn.name,
        type = conn.type.name,
        providerOrCarrier = conn.providerOrCarrier,
        bandwidth = conn.bandwidth,
        localEndpointDeviceId = conn.localEndpointDeviceId,
        localEndpointSiteDescription = conn.localEndpointSiteDescription,
        remoteEndpointDeviceId = conn.remoteEndpointDeviceId,
        remoteEndpointSiteDescription = conn.remoteEndpointSiteDescription,
        underlyingAccessId = conn.underlyingAccessId,
        notes = conn.notes
    )
}

internal fun toWanVpnConnection(entity: com.onlyfield.assetmanager.data.local.WanVpnConnectionEntity): com.onlyfield.assetmanager.core.model.WanVpnConnection {
    return com.onlyfield.assetmanager.core.model.WanVpnConnection(
        id = entity.id,
        name = entity.name,
        type = com.onlyfield.assetmanager.core.model.WanVpnType.valueOf(entity.type),
        providerOrCarrier = entity.providerOrCarrier,
        bandwidth = entity.bandwidth,
        localEndpointDeviceId = entity.localEndpointDeviceId,
        localEndpointSiteDescription = entity.localEndpointSiteDescription,
        remoteEndpointDeviceId = entity.remoteEndpointDeviceId,
        remoteEndpointSiteDescription = entity.remoteEndpointSiteDescription,
        underlyingAccessId = entity.underlyingAccessId,
        notes = entity.notes
    )
}

internal fun toVideoSurveillanceMappingEntity(projectId: String, video: com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping): com.onlyfield.assetmanager.data.local.VideoSurveillanceMappingEntity {
    return com.onlyfield.assetmanager.data.local.VideoSurveillanceMappingEntity(
        id = video.id,
        projectId = projectId,
        cameraDeviceId = video.cameraDeviceId,
        managerDeviceId = video.managerDeviceId,
        externalManagerDescription = video.externalManagerDescription,
        channelNumber = video.channelNumber,
        streamUrl = video.streamUrl,
        resolution = video.resolution,
        notes = video.notes
    )
}

internal fun toVideoSurveillanceMapping(entity: com.onlyfield.assetmanager.data.local.VideoSurveillanceMappingEntity): com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping {
    return com.onlyfield.assetmanager.core.model.VideoSurveillanceMapping(
        id = entity.id,
        cameraDeviceId = entity.cameraDeviceId,
        managerDeviceId = entity.managerDeviceId,
        externalManagerDescription = entity.externalManagerDescription,
        channelNumber = entity.channelNumber,
        streamUrl = entity.streamUrl,
        resolution = entity.resolution,
        notes = entity.notes
    )
}

internal fun toCustomExtraFieldEntity(projectId: String, field: com.onlyfield.assetmanager.core.model.CustomExtraField): com.onlyfield.assetmanager.data.local.CustomExtraFieldEntity {
    return com.onlyfield.assetmanager.data.local.CustomExtraFieldEntity(
        id = field.id,
        projectId = projectId,
        targetType = field.targetType,
        targetId = field.targetId,
        fieldKey = field.fieldKey,
        fieldValue = field.fieldValue,
        fieldType = field.fieldType.name,
        classification = field.classification.name,
        notes = field.notes
    )
}

internal fun toCustomExtraField(entity: com.onlyfield.assetmanager.data.local.CustomExtraFieldEntity): com.onlyfield.assetmanager.core.model.CustomExtraField {
    return com.onlyfield.assetmanager.core.model.CustomExtraField(
        id = entity.id,
        targetType = entity.targetType,
        targetId = entity.targetId,
        fieldKey = entity.fieldKey,
        fieldValue = entity.fieldValue,
        fieldType = com.onlyfield.assetmanager.core.model.CustomFieldType.valueOf(entity.fieldType),
        classification = com.onlyfield.assetmanager.core.model.AttachmentClassification.valueOf(entity.classification),
        notes = entity.notes
    )
}
