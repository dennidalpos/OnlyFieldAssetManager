package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.PathSchematics
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.ModelValidator
import com.onlyfield.assetmanager.core.validation.ValidationIssue
import com.onlyfield.assetmanager.core.validation.ValidationSeverity

data class DocumentObservation(val label: String, val observation: Observation?)

/** One document scope for all formats; complete physical paths provide external context. */
class DocumentSelection(val source: Project, val filter: ExportFilterConfig) {
    private val limited = filter.selectedSiteId != null || filter.selectedAreaId != null || filter.selectedCategory != null
    private val hierarchy = HierarchyIndex(source)
    private val sites = source.sites.filter { filter.selectedSiteId == null || it.id == filter.selectedSiteId }
    val devices = sites.flatMap { it.devices }.filter {
        (filter.selectedAreaId == null || hierarchy.areaId(ObjectRef(PlacementTargetType.DEVICE, it.id)) == filter.selectedAreaId) &&
            (filter.selectedCategory == null || it.category == filter.selectedCategory)
    }
    val deviceIds = devices.map { it.id }.toSet()
    private val portIds = devices.flatMap { it.ports }.map { it.id }.toSet()
    private val floorsWithDevices = devices.mapNotNull { hierarchy.areaId(ObjectRef(PlacementTargetType.DEVICE, it.id)) }.toSet()
    private val areas = sites.flatMap { it.areas }.filter {
        (filter.selectedAreaId == null || it.id == filter.selectedAreaId) &&
            (filter.selectedCategory == null || it.id in floorsWithDevices)
    }
    private val areaIds = areas.map { it.id }.toSet()
    private val siteIds = sites.filter {
        (filter.selectedAreaId == null || it.areas.any { a -> a.id == filter.selectedAreaId }) &&
            (filter.selectedCategory == null || it.devices.any { d -> d.id in deviceIds })
    }.map { it.id }.toSet()
    private val racks = source.racks.filter {
        val area = hierarchy.areaId(ObjectRef(PlacementTargetType.RACK, it.id))
        (!limited || area in areaIds || devices.any { d -> d.rackId == it.id }) &&
            (filter.selectedCategory == null || devices.any { d -> d.rackId == it.id })
    }
    private val rackIds = racks.map { it.id }.toSet()
    private val ends = CableEnds(source)
    private val cables = source.cables.filter { cable ->
        val a = ends.device(cable, true)?.id
        val b = ends.device(cable, false)?.id
        !limited || ((a in deviceIds || b in deviceIds) && (a == null || a in deviceIds) && (b == null || b in deviceIds))
    }
    private val cableIds = cables.map { it.id }.toSet()

    fun allowed(classification: AttachmentClassification): Boolean = when (classification) {
        AttachmentClassification.SHAREABLE -> true
        AttachmentClassification.CONFIDENTIAL -> filter.includeConfidential
        AttachmentClassification.REVIEW_REQUIRED -> filter.reviewRequiredConfirmed
    }

    private fun target(type: String?, id: String?): Boolean = when (type) {
        null, "PROJECT" -> id == null || id == source.id
        "SITE" -> id in siteIds
        "AREA" -> id in areaIds
        "DEVICE" -> id in deviceIds
        "PORT" -> id in portIds
        "RACK" -> id in rackIds
        "CABLE" -> id in cableIds
        else -> false
    }

    private fun networkScope(type: VlanScopeType, id: String?): Boolean = when (type) {
        VlanScopeType.PROJECT -> true
        VlanScopeType.SITE -> id in siteIds
        VlanScopeType.DEVICE -> id in deviceIds
    }

    private val attachments = source.attachments.filter { allowed(it.classification) && target(it.targetType?.name, it.targetId) }
    private val attachmentIds = attachments.map { it.id }.toSet()
    private val vlans = source.vlans.filter { networkScope(it.scopeType, it.scopeTargetId) }
    private val vlanIds = vlans.map { it.id }.toSet()
    private val refs = deviceIds.map { ObjectRef(PlacementTargetType.DEVICE, it) }.toSet() + rackIds.map { ObjectRef(PlacementTargetType.RACK, it) }

    val project: Project = source.copy(
        sites = sites.filter { it.id in siteIds }.map { site -> site.copy(
            areas = site.areas.filter { it.id in areaIds }.map { area -> area.copy(floorplanAttachmentId = area.floorplanAttachmentId?.takeIf { it in attachmentIds }) },
            devices = site.devices.filter { it.id in deviceIds }.map { d -> d.copy(
                areaId = hierarchy.areaId(ObjectRef(PlacementTargetType.DEVICE, d.id)),
                rackId = d.rackId?.takeIf { it in rackIds },
            ) },
        ) },
        credentials = emptyList(),
        racks = racks.map { it.copy(areaId = hierarchy.areaId(ObjectRef(PlacementTargetType.RACK, it.id))) },
        attachments = attachments,
        annotations = source.annotations.filter { it.areaId in areaIds && allowed(it.classification) },
        floorplanPlacements = source.floorplanPlacements.filter { it.areaId in areaIds && target(it.targetType.name, it.targetId) },
        objectContainments = source.objectContainments.filter { it.parent in refs && it.child in refs },
        cableRoutes = source.cableRoutes.filter { it.areaId in areaIds && it.cableId in cableIds },
        cables = cables,
        panelMappings = source.panelMappings.filter { it.portAId in portIds && (it.portBId == null || it.portBId in portIds) },
        vlans = vlans,
        subnets = source.subnets.filter { networkScope(it.scopeType, it.scopeTargetId) && (it.vlanId == null || it.vlanId in vlanIds) },
        portVlanMemberships = source.portVlanMemberships.filter { it.portId in portIds },
        logicalInterfaces = source.logicalInterfaces.filter { it.deviceId in deviceIds },
        lagGroups = source.lagGroups.filter { it.deviceId in deviceIds },
        deviceConfigurations = source.deviceConfigurations.filter { it.deviceId in deviceIds },
        wanVpnConnections = source.wanVpnConnections.filter { it.localEndpointDeviceId in deviceIds || it.remoteEndpointDeviceId in deviceIds ||
            (filter.selectedSiteId == null && filter.selectedAreaId == null && filter.selectedCategory == null) },
        videoSurveillanceMappings = source.videoSurveillanceMappings.filter { it.cameraDeviceId in deviceIds },
        customExtraFields = source.customExtraFields.filter { allowed(it.classification) && target(it.targetType, it.targetId) },
        powerFeeds = source.powerFeeds.filter { it.deviceId in deviceIds }.map { it.copy(sourceDeviceId = it.sourceDeviceId?.takeIf { id -> id in deviceIds }) },
        poeMappings = source.poeMappings.filter { it.portId in portIds },
        documentBadges = source.documentBadges.filter { target(it.targetType, it.targetId) },
    )

    val paths by lazy { PathSchematics.all(source).filter { path -> path.stations.any { it.device?.id in deviceIds } } }

    val observations: List<DocumentObservation> by lazy {
        devices.flatMap { device -> listOf(DocumentObservation(device.technicalName, device.observation)) +
            device.ports.map { DocumentObservation("${device.technicalName} › ${it.name}", it.observation) } } +
            cables.map { DocumentObservation(it.codeOrLabel ?: it.id, it.observation) }
    }

    /** Validate original references, then retain only warnings belonging to printed sections. */
    fun warnings(i18n: Messages, selection: ReportSelection = ReportSelection()): List<ValidationIssue> {
        val ids = buildSet {
            add(source.id)
            if (selection.includeInventoryTable) addAll(deviceIds)
            if (selection.includeRackCards) { addAll(rackIds); addAll(devices.filter { it.rackId in rackIds }.map { it.id }) }
            if (selection.includeCablingAndPorts) { addAll(portIds); addAll(cableIds); addAll(project.panelMappings.map { it.id }) }
            if (selection.includeLogicalNetwork) {
                addAll(vlanIds); addAll(project.subnets.map { it.id }); addAll(project.portVlanMemberships.map { it.id })
                addAll(project.logicalInterfaces.map { it.id }); addAll(project.lagGroups.map { it.id }); addAll(project.wanVpnConnections.map { it.id })
            }
            if (selection.includePowerAndBadges) {
                addAll(project.powerFeeds.map { it.id }); addAll(project.powerFeeds.map { it.deviceId })
                addAll(project.poeMappings.map { it.id }); addAll(project.documentBadges.map { it.id })
            }
            if (selection.includeNotesAndAttachments) { addAll(attachmentIds); addAll(project.customExtraFields.map { it.id }) }
            if (selection.includeFloorPlans) { addAll(areaIds); addAll(project.floorplanPlacements.map { it.id }); addAll(project.annotations.map { it.id }) }
            if (selection.includePaths || selection.includeTopology) {
                addAll(physicalContext.sites.flatMap { it.devices }.flatMap { it.ports }.map { it.id })
                addAll(physicalContext.cables.map { it.id })
            }
        }
        return ModelValidator.validateProject(source.copy(credentials = emptyList()), i18n).issues.filter {
            val sectionIncluded = when (it.code) {
                "SINGLE_FEED_PARTIAL_COVERAGE_WARNING" -> selection.includePowerAndBadges
                "UNVERIFIED_DEVICE_OBSERVATION" -> selection.includeInventoryTable || selection.includeRackCards
                else -> true
            }
            sectionIncluded && it.severity == ValidationSeverity.DOCUMENTARY_WARNING && (it.targetEntityId == null || it.targetEntityId in ids)
        }
    }

    /** Only selected devices and stations needed by their complete paths. */
    val physicalContext: Project by lazy {
        val contextIds = deviceIds + paths.flatMap { it.stations.mapNotNull { s -> s.device?.id } }
        val contextCables = paths.flatMap { it.segments.map { s -> s.cable.id } }.toSet()
        source.copy(sites = source.sites.map { it.copy(devices = it.devices.filter { d -> d.id in contextIds }) },
            cables = source.cables.filter { it.id in contextCables })
    }
}
