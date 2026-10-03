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
    val notes: String? = null,
)

@Serializable
enum class EndpointStatus {
    CONNECTED,
    DETACHED_TO_VERIFY,
    DISCONNECTED,
    UNKNOWN
}

@Serializable
enum class NumberingDirection {
    BOTTOM_TO_TOP,
    TOP_TO_BOTTOM
}

@Serializable
data class Rack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val areaId: String? = null,
    val heightU: Int = 42,
    val numberingDirection: NumberingDirection = NumberingDirection.BOTTOM_TO_TOP,
    val depthMm: Int? = null,
    val notes: String? = null,
)

@Serializable
enum class DeviceCategory {
    NETWORK_SWITCH,
    PATCH_PANEL,
    UPS_PDU,
    SERVER_STORAGE,
    CAMERA_NVR,
    SHELF,
    BLANK_PANEL,
    CUSTOM
}

@Serializable
enum class PortSide {
    FRONT,
    REAR,
    BOTH
}

@Serializable
data class PortTemplate(
    val namePrefix: String,
    val startNumber: Int = 1,
    val portCount: Int,
    val side: PortSide = PortSide.FRONT,
    val isCombo: Boolean = false,
    val mediaType: String? = null,
)

@Serializable
data class DeviceModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String? = null,
    val modelNumber: String? = null,
    val category: DeviceCategory = DeviceCategory.CUSTOM,
    val defaultHeightU: Int = 1,
    val portTemplates: List<PortTemplate> = emptyList(),
    val notes: String? = null,
)

@Serializable
enum class RackSide {
    FRONT,
    REAR,
    BOTH
}

@Serializable
enum class MountingType {
    RACK_MOUNT,
    VERTICAL_MOUNT,
    SHELF_MOUNT,
    OUT_OF_RACK
}

@Serializable
data class Port(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val name: String,
    val label: String? = null,
    val connectedPortId: String? = null,
    val endpointStatus: EndpointStatus = EndpointStatus.DISCONNECTED,
    val observation: Observation? = null,
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
    val observation: Observation? = null,
    val rackId: String? = null,
    val positionU: Int? = null,
    val heightU: Int = 1,
    val rackSide: RackSide = RackSide.BOTH,
    val mountingType: MountingType = MountingType.OUT_OF_RACK,
    val deviceModelId: String? = null,
    val category: DeviceCategory = DeviceCategory.CUSTOM,
    /** Manufacturer serial (contract 1.8); absent in 1.7 packages. */
    val serialNumber: String? = null,
    val objectTypeId: String? = null,
)

@Serializable
enum class AttachmentClassification {
    SHAREABLE,
    CONFIDENTIAL,
    REVIEW_REQUIRED
}

@Serializable
enum class AttachmentType {
    IMAGE,
    PDF,
    DOCUMENT,
    OTHER
}

@Serializable
enum class AttachmentTargetType {
    PROJECT,
    RACK,
    DEVICE,
    AREA,
    CABLE
}

@Serializable
data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val originalFileName: String,
    val fileType: AttachmentType = AttachmentType.IMAGE,
    val mimeType: String = "image/jpeg",
    val relativePath: String,
    val thumbnailPath: String? = null,
    val classification: AttachmentClassification = AttachmentClassification.SHAREABLE,
    val pageCount: Int = 1,
    val targetType: AttachmentTargetType? = null,
    val targetId: String? = null,
    val attributionText: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Serializable
enum class AnnotationType {
    TEXT,
    ARROW,
    RECTANGLE,
    CIRCLE,
    HIGHLIGHT_ZONE
}

@Serializable
data class Annotation(
    val id: String = UUID.randomUUID().toString(),
    val areaId: String,
    val type: AnnotationType = AnnotationType.TEXT,
    val x1Ratio: Float,
    val y1Ratio: Float,
    val x2Ratio: Float = x1Ratio,
    val y2Ratio: Float = y1Ratio,
    val label: String = "",
    val colorHex: String = "#FF0000",
    val classification: AttachmentClassification = AttachmentClassification.SHAREABLE
)

@Serializable
enum class PlacementTargetType {
    RACK,
    DEVICE
}

@Serializable
data class FloorplanPlacement(
    val id: String = UUID.randomUUID().toString(),
    val areaId: String,
    val targetType: PlacementTargetType,
    val targetId: String,
    val xRatio: Float,
    val yRatio: Float,
    val labelOverride: String? = null
)

@Serializable
data class Area(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val floor: String? = null,
    val description: String? = null,
    val floorplanAttachmentId: String? = null,
    val floorplanPageIndex: Int = 0
)

@Serializable
data class Site(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String? = null,
    val areas: List<Area> = emptyList(),
)

@Serializable
data class BusinessUnit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val code: String? = null,
    val sites: List<Site> = emptyList(),
    val areas: List<Area> = emptyList(),
    val devices: List<Device> = emptyList(),
)

@Serializable
enum class CredentialType {
    PASSWORD,
    SSH_KEY,
    SNMP_COMMUNITY,
    OTHER,
}

@Serializable
data class Credential(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String? = null,
    val groupName: String? = null,
    val username: String,
    val secret: String,
    val type: CredentialType = CredentialType.PASSWORD,
    val notes: String? = null,
)

@Serializable
enum class CableMedium {
    ETHERNET_COPPER,
    DAC,
    AOC,
    FIBER_OVERALL,
    CONSOLE,
    OTHER,
    UNKNOWN
}

@Serializable
enum class CableOrientation {
    NONE,
    A_TO_B,
    B_TO_A,
    BOTH
}

@Serializable
data class SharedPathSegment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sourceAreaId: String? = null,
    val targetAreaId: String? = null,
    val description: String? = null,
    val capacityMaxCables: Int? = null,
    val notes: String? = null
)

@Serializable
data class Cable(
    val id: String = UUID.randomUUID().toString(),
    val codeOrLabel: String? = null,
    val portAId: String? = null,
    val portBId: String? = null,
    val medium: CableMedium = CableMedium.ETHERNET_COPPER,
    val connectorA: String? = null,
    val connectorB: String? = null,
    val nominalCharacteristics: String? = null,
    val observedSpeed: String? = null,
    val color: String? = null,
    val lengthValue: Double? = null,
    val lengthUnit: String? = "m",
    val orientation: CableOrientation = CableOrientation.NONE,
    val sharedPathSegmentIds: List<String> = emptyList(),
    val observation: Observation? = null,
    val notes: String? = null,
    val deviceAId: String? = null,
    val deviceBId: String? = null,
    val objectTypeId: String? = null,
)

@Serializable
data class PanelMapping(
    val id: String = UUID.randomUUID().toString(),
    val portAId: String,
    val portBId: String? = null,
    val mappingType: String = "CROSS_CONNECT",
    val isUnknownPassage: Boolean = false,
    val notes: String? = null
)

@Serializable
enum class VlanScopeType {
    PROJECT,
    BUSINESS_UNIT,
    SITE,
    DEVICE
}

@Serializable
data class Vlan(
    val id: String = UUID.randomUUID().toString(),
    val vlanId: Int,
    val name: String,
    val scopeType: VlanScopeType = VlanScopeType.PROJECT,
    val scopeTargetId: String? = null,
    val description: String? = null
)

@Serializable
data class Subnet(
    val id: String = UUID.randomUUID().toString(),
    val cidrBlock: String,
    val gatewayIp: String? = null,
    val vlanId: String? = null,
    val name: String? = null,
    val scopeType: VlanScopeType = VlanScopeType.PROJECT,
    val scopeTargetId: String? = null,
    val description: String? = null
)

@Serializable
enum class PortVlanMode {
    ACCESS,
    TRUNK,
    HYBRID,
    UNTAGGED,
    TAGGED,
    UNSPECIFIED
}

@Serializable
data class PortVlanMembership(
    val id: String = UUID.randomUUID().toString(),
    val portId: String,
    val mode: PortVlanMode = PortVlanMode.ACCESS,
    val untaggedVlanId: Int? = null,
    val taggedVlanIds: List<Int> = emptyList(),
    val nativeVlanId: Int? = null,
    val notes: String? = null
)

@Serializable
data class LogicalInterface(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val name: String,
    val ipAddress: String? = null,
    val subnetCidr: String? = null,
    val vlanId: Int? = null,
    val isL3: Boolean = true,
    val macAddress: String? = null,
    val notes: String? = null
)

@Serializable
enum class LagMode {
    LACP,
    STATIC,
    OTHER,
    UNSPECIFIED
}

@Serializable
data class LagGroup(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val name: String,
    val mode: LagMode = LagMode.LACP,
    val memberPortIds: List<String> = emptyList(),
    val notes: String? = null
)

@Serializable
data class DeviceConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val title: String,
    val configText: String? = null,
    val attachmentId: String? = null,
    val capturedEpochMs: Long = System.currentTimeMillis(),
    val notes: String? = null
)

@Serializable
enum class WanVpnType {
    WAN,
    VPN,
    INTERNET,
    OTHER
}

@Serializable
data class WanVpnConnection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: WanVpnType = WanVpnType.WAN,
    val providerOrCarrier: String? = null,
    val bandwidth: String? = null,
    val localEndpointDeviceId: String? = null,
    val localEndpointSiteDescription: String? = null,
    val remoteEndpointDeviceId: String? = null,
    val remoteEndpointSiteDescription: String? = null,
    val underlyingAccessId: String? = null,
    val notes: String? = null
)

@Serializable
data class VideoSurveillanceMapping(
    val id: String = UUID.randomUUID().toString(),
    val cameraDeviceId: String,
    val managerDeviceId: String? = null,
    val externalManagerDescription: String? = null,
    val channelNumber: Int? = null,
    val streamUrl: String? = null,
    val resolution: String? = null,
    val notes: String? = null
)

@Serializable
enum class CustomFieldType {
    STRING,
    NUMBER,
    BOOLEAN,
    DATE
}

@Serializable
data class CustomExtraField(
    val id: String = UUID.randomUUID().toString(),
    val targetType: String,
    val targetId: String,
    val fieldKey: String,
    val fieldValue: String,
    val fieldType: CustomFieldType = CustomFieldType.STRING,
    val classification: AttachmentClassification = AttachmentClassification.SHAREABLE,
    val notes: String? = null
)

@Serializable
enum class PowerFeedType {
    PRIMARY_A,
    SECONDARY_B,
    UPS_BACKUP,
    PDU_DISTRIBUTION,
    MAINS_DIRECT,
    OTHER,
    UNKNOWN
}

@Serializable
data class PowerFeed(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val feedName: String,
    val feedType: PowerFeedType = PowerFeedType.PRIMARY_A,
    val sourceDeviceId: String? = null,
    val sourceOutletDescription: String? = null,
    val voltageVolts: Int? = null,
    val loadVa: Double? = null,
    val loadWatts: Double? = null,
    val observedRuntimeMinutes: Int? = null,
    val observedSource: String? = null,
    val observedEpochMs: Long? = null,
    val notes: String? = null
)

@Serializable
enum class PoeRole {
    PSE_SOURCE,
    PD_SINK,
    PASSIVE_INJECTOR,
    NONE
}

@Serializable
enum class PoeStandard {
    IEEE_802_3AF,
    IEEE_802_3AT,
    IEEE_802_3BT,
    PASSIVE_24V,
    PASSIVE_48V,
    OTHER
}

@Serializable
data class PoeMapping(
    val id: String = UUID.randomUUID().toString(),
    val portId: String,
    val role: PoeRole = PoeRole.PSE_SOURCE,
    val standard: PoeStandard = PoeStandard.IEEE_802_3AT,
    val allocatedPowerWatts: Double? = null,
    val notes: String? = null
)

@Serializable
enum class BadgeCategory {
    VLAN,
    MEDIUM,
    POE,
    UPS_DEPENDENCY,
    COVERAGE,
    OPEN_ISSUE,
    FREE_LABEL
}

@Serializable
data class DocumentBadge(
    val id: String = UUID.randomUUID().toString(),
    val targetType: String, // "DEVICE", "PORT", "RACK", "PROJECT"
    val targetId: String,
    val label: String,
    val category: BadgeCategory = BadgeCategory.FREE_LABEL,
    val isDerived: Boolean = false,
    val notes: String? = null
)

@Serializable
data class TrashItem(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val itemType: String, // "DEVICE", "PORT", "CABLE", "RACK", "AREA", "SITE", "BUSINESS_UNIT", "ATTACHMENT", "CREDENTIAL"
    val itemId: String,
    val displayName: String,
    val serializedJson: String,
    val deletedEpochMs: Long = System.currentTimeMillis(),
    val affectedReferencesSummary: String? = null
)

@Serializable
data class MergeDataChoices(
    val useTechnicalNameFromDuplicate: Boolean = false,
    val usePhysicalLabelFromDuplicate: Boolean = false,
    val useAliasFromDuplicate: Boolean = false,
    val useIpFromDuplicate: Boolean = false,
    val useMacFromDuplicate: Boolean = false,
    val useLocationFromDuplicate: Boolean = false,
    val mergePorts: Boolean = true,
    val mergeCredentials: Boolean = true,
    val mergeConfigurations: Boolean = true,
    val mergePowerFeeds: Boolean = true,
    val mergeExtraFields: Boolean = true
)

@Serializable
data class BatchDeviceChanges(
    val siteId: String? = null,
    val updateSiteId: Boolean = false,
    val areaId: String? = null,
    val updateAreaId: Boolean = false,
    val category: DeviceCategory? = null,
    val updateCategory: Boolean = false,
    val rackId: String? = null,
    val updateRackId: Boolean = false,
    val mountingType: MountingType? = null,
    val updateMountingType: Boolean = false,
    val observationNotes: String? = null,
    val updateObservationNotes: Boolean = false
)

@Serializable
data class BatchEditPreview(
    val targetDeviceIds: List<String>,
    val affectedDeviceNames: List<String>,
    val changesSummary: List<String>,
    val isProhibitedFieldAttempted: Boolean = false
)

@Serializable
data class ExportFilterConfig(
    val selectedBusinessUnitId: String? = null,
    val selectedSiteId: String? = null,
    val selectedAreaId: String? = null,
    val selectedCategory: DeviceCategory? = null,
    val includeConfidential: Boolean = false,
    val reviewRequiredConfirmed: Boolean = true,
    val authorName: String = "Tecnico Operativo",
    val titleOverride: String? = null
)

@Serializable
data class ReportSelection(
    val includeRackCards: Boolean = true,
    val includeInventoryTable: Boolean = true,
    val includeCablingAndPorts: Boolean = true,
    val includeLogicalNetwork: Boolean = true,
    val includePowerAndBadges: Boolean = true,
    val includeNotesAndAttachments: Boolean = true
)

@Serializable
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val createdEpochMs: Long,
    val updatedEpochMs: Long,
    val businessUnits: List<BusinessUnit> = emptyList(),
    val credentials: List<Credential> = emptyList(),
    val racks: List<Rack> = emptyList(),
    val deviceModels: List<DeviceModel> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val annotations: List<Annotation> = emptyList(),
    val floorplanPlacements: List<FloorplanPlacement> = emptyList(),
    val objectTypes: List<ObjectType> = emptyList(),
    val cableRoutes: List<CableRoute> = emptyList(),
    val cables: List<Cable> = emptyList(),
    val sharedPathSegments: List<SharedPathSegment> = emptyList(),
    val panelMappings: List<PanelMapping> = emptyList(),
    val vlans: List<Vlan> = emptyList(),
    val subnets: List<Subnet> = emptyList(),
    val portVlanMemberships: List<PortVlanMembership> = emptyList(),
    val logicalInterfaces: List<LogicalInterface> = emptyList(),
    val lagGroups: List<LagGroup> = emptyList(),
    val deviceConfigurations: List<DeviceConfiguration> = emptyList(),
    val wanVpnConnections: List<WanVpnConnection> = emptyList(),
    val videoSurveillanceMappings: List<VideoSurveillanceMapping> = emptyList(),
    val customExtraFields: List<CustomExtraField> = emptyList(),
    val powerFeeds: List<PowerFeed> = emptyList(),
    val poeMappings: List<PoeMapping> = emptyList(),
    val documentBadges: List<DocumentBadge> = emptyList(),
    val isPasswordProtected: Boolean = false,
)
