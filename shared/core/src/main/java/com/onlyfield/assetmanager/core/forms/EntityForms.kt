package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.FieldValidators.parseDecimal
import com.onlyfield.assetmanager.core.forms.FieldValidators.parseInt

/*
 * Editable form state for each entity edited on the desktop.
 *
 * Every `toX(existing)` starts from the existing entity and `copy()`s only the fields the form
 * shows, so fields that have no control in the dialog are preserved on save.
 */

private fun String.orNull(): String? = trim().ifBlank { null }

data class DeviceForm(
    val technicalName: String = "",
    val physicalLabel: String = "",
    val alias: String = "",
    val ipAddress: String = "",
    val macAddress: String = "",
    val serialNumber: String = "",
    val objectTypeId: String? = null,
    val category: DeviceCategory = DeviceCategory.NETWORK_SWITCH,
    val businessUnitId: String? = null,
    val areaId: String? = null,
    val rackId: String? = null,
    val positionU: String = "",
    val heightU: String = "1",
    val rackSide: RackSide = RackSide.FRONT,
    val mountingType: MountingType = MountingType.RACK_MOUNT,
    val deviceModelId: String? = null,
    val observationStatus: ObservationStatus = ObservationStatus.TO_VERIFY,
    val notes: String = "",
) {
    fun errors(rackHeightU: Int?): Map<String, String> = buildMap {
        FieldValidators.required(technicalName, "Nome tecnico")?.let { put("technicalName", it) }
        if (businessUnitId == null) put("businessUnitId", "Selezionare la business unit")
        FieldValidators.ipv4(ipAddress)?.let { put("ipAddress", it) }
        FieldValidators.mac(macAddress)?.let { put("macAddress", it) }
        FieldValidators.int(heightU, min = 1, max = 60, required = true)?.let { put("heightU", it) }
        if (rackId != null) {
            val height = parseInt(heightU) ?: 1
            val maxStart = rackHeightU?.let { (it - height + 1).coerceAtLeast(1) }
            FieldValidators.int(positionU, min = 1, max = maxStart)?.let { put("positionU", it) }
        }
    }

    fun toDevice(existing: Device?, source: String, nowMs: Long = System.currentTimeMillis()): Device {
        val base = existing ?: Device(technicalName = technicalName.trim())
        val notesValue = notes.orNull()
        val previous = existing?.observation
        val observation = if (previous != null && previous.notes == notesValue && previous.status == observationStatus) {
            previous
        } else if (previous == null && notesValue == null && observationStatus == ObservationStatus.TO_VERIFY) {
            null
        } else {
            Observation(source = source, timestampEpochMs = nowMs, status = observationStatus, notes = notesValue)
        }
        return base.copy(
            technicalName = technicalName.trim(),
            physicalLabel = physicalLabel.orNull(),
            alias = alias.orNull(),
            ipAddress = ipAddress.orNull(),
            macAddress = macAddress.orNull(),
            serialNumber = serialNumber.orNull(),
            objectTypeId = objectTypeId,
            category = category,
            areaId = areaId,
            rackId = rackId,
            positionU = if (rackId == null) null else parseInt(positionU),
            heightU = parseInt(heightU) ?: base.heightU,
            rackSide = rackSide,
            mountingType = mountingType,
            deviceModelId = deviceModelId,
            observation = observation,
        )
    }

    companion object {
        fun from(device: Device?, businessUnitId: String?): DeviceForm = device?.let {
            DeviceForm(
                technicalName = it.technicalName,
                physicalLabel = it.physicalLabel.orEmpty(),
                alias = it.alias.orEmpty(),
                ipAddress = it.ipAddress.orEmpty(),
                macAddress = it.macAddress.orEmpty(),
                serialNumber = it.serialNumber.orEmpty(),
                objectTypeId = it.objectTypeId,
                category = it.category,
                businessUnitId = businessUnitId,
                areaId = it.areaId,
                rackId = it.rackId,
                positionU = it.positionU?.toString().orEmpty(),
                heightU = it.heightU.toString(),
                rackSide = it.rackSide,
                mountingType = it.mountingType,
                deviceModelId = it.deviceModelId,
                observationStatus = it.observation?.status ?: ObservationStatus.TO_VERIFY,
                notes = it.observation?.notes.orEmpty(),
            )
        } ?: DeviceForm(businessUnitId = businessUnitId)
    }
}

data class RackForm(
    val name: String = "",
    val heightU: String = "42",
    val numberingDirection: NumberingDirection = NumberingDirection.BOTTOM_TO_TOP,
    val areaId: String? = null,
    val depthMm: String = "",
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        FieldValidators.required(name, "Nome")?.let { put("name", it) }
        FieldValidators.int(heightU, min = 1, max = 60, required = true)?.let { put("heightU", it) }
        FieldValidators.int(depthMm, min = 100, max = 2000)?.let { put("depthMm", it) }
    }

    fun toRack(existing: Rack?): Rack = (existing ?: Rack(name = name.trim())).copy(
        name = name.trim(),
        heightU = parseInt(heightU) ?: existing?.heightU ?: 42,
        numberingDirection = numberingDirection,
        areaId = areaId,
        depthMm = parseInt(depthMm),
        notes = notes.orNull(),
    )

    companion object {
        fun from(rack: Rack?) = rack?.let {
            RackForm(it.name, it.heightU.toString(), it.numberingDirection, it.areaId, it.depthMm?.toString().orEmpty(), it.notes.orEmpty())
        } ?: RackForm()
    }
}

data class CableForm(
    val codeOrLabel: String = "",
    val portAId: String? = null,
    val portBId: String? = null,
    val medium: CableMedium = CableMedium.ETHERNET_COPPER,
    val connectorA: String = "",
    val connectorB: String = "",
    val nominalCharacteristics: String = "",
    val observedSpeed: String = "",
    val color: String = "",
    val lengthValue: String = "",
    val orientation: CableOrientation = CableOrientation.NONE,
    val sharedPathSegmentIds: List<String> = emptyList(),
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        if (portAId != null && portAId == portBId) put("portBId", "Le due estremità devono essere porte diverse")
        FieldValidators.decimal(lengthValue, min = 0.0)?.let { put("lengthValue", it) }
    }

    fun toCable(existing: Cable?): Cable = (existing ?: Cable()).copy(
        codeOrLabel = codeOrLabel.orNull(),
        portAId = portAId,
        portBId = portBId,
        medium = medium,
        connectorA = connectorA.orNull(),
        connectorB = connectorB.orNull(),
        nominalCharacteristics = nominalCharacteristics.orNull(),
        observedSpeed = observedSpeed.orNull(),
        color = color.orNull(),
        lengthValue = parseDecimal(lengthValue),
        orientation = orientation,
        sharedPathSegmentIds = sharedPathSegmentIds,
        notes = notes.orNull(),
    )

    companion object {
        fun from(c: Cable?) = c?.let {
            CableForm(
                it.codeOrLabel.orEmpty(), it.portAId, it.portBId, it.medium, it.connectorA.orEmpty(), it.connectorB.orEmpty(),
                it.nominalCharacteristics.orEmpty(), it.observedSpeed.orEmpty(), it.color.orEmpty(),
                it.lengthValue?.toString().orEmpty(), it.orientation, it.sharedPathSegmentIds, it.notes.orEmpty()
            )
        } ?: CableForm()
    }
}

data class SharedPathForm(
    val name: String = "",
    val sourceAreaId: String? = null,
    val targetAreaId: String? = null,
    val description: String = "",
    val capacityMaxCables: String = "",
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        FieldValidators.required(name, "Nome")?.let { put("name", it) }
        FieldValidators.int(capacityMaxCables, min = 1)?.let { put("capacityMaxCables", it) }
    }

    fun toSegment(existing: SharedPathSegment?): SharedPathSegment = (existing ?: SharedPathSegment(name = name.trim())).copy(
        name = name.trim(),
        sourceAreaId = sourceAreaId,
        targetAreaId = targetAreaId,
        description = description.orNull(),
        capacityMaxCables = parseInt(capacityMaxCables),
        notes = notes.orNull(),
    )

    companion object {
        fun from(s: SharedPathSegment?) = s?.let {
            SharedPathForm(it.name, it.sourceAreaId, it.targetAreaId, it.description.orEmpty(), it.capacityMaxCables?.toString().orEmpty(), it.notes.orEmpty())
        } ?: SharedPathForm()
    }
}

data class PanelMappingForm(
    val portAId: String? = null,
    val portBId: String? = null,
    val mappingType: String = "CROSS_CONNECT",
    val isUnknownPassage: Boolean = false,
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        if (portAId == null) put("portAId", "Selezionare la porta A")
        if (portAId != null && portAId == portBId) put("portBId", "Le due porte devono essere diverse")
    }

    fun toMapping(existing: PanelMapping?): PanelMapping {
        val a = requireNotNull(portAId)
        return (existing ?: PanelMapping(portAId = a)).copy(
            portAId = a, portBId = portBId, mappingType = mappingType, isUnknownPassage = isUnknownPassage, notes = notes.orNull()
        )
    }

    companion object {
        fun from(m: PanelMapping?) = m?.let { PanelMappingForm(it.portAId, it.portBId, it.mappingType, it.isUnknownPassage, it.notes.orEmpty()) }
            ?: PanelMappingForm()
    }
}

data class VlanForm(
    val vlanId: String = "",
    val name: String = "",
    val scopeType: VlanScopeType = VlanScopeType.PROJECT,
    val scopeTargetId: String? = null,
    val description: String = "",
) {
    fun errors(existingVlanIds: Set<Int>): Map<String, String> = buildMap {
        val numberError = FieldValidators.int(vlanId, min = 1, max = 4094, required = true)
        if (numberError != null) put("vlanId", numberError)
        else if (parseInt(vlanId) in existingVlanIds && scopeType == VlanScopeType.PROJECT) put("vlanId", "VLAN già definita nel progetto")
        FieldValidators.required(name, "Nome")?.let { put("name", it) }
    }

    fun toVlan(existing: Vlan?): Vlan {
        val number = requireNotNull(parseInt(vlanId))
        return (existing ?: Vlan(vlanId = number, name = name.trim())).copy(
            vlanId = number,
            name = name.trim(),
            scopeType = scopeType,
            scopeTargetId = if (scopeType == VlanScopeType.PROJECT) null else scopeTargetId,
            description = description.orNull(),
        )
    }

    companion object {
        fun from(v: Vlan?) = v?.let { VlanForm(it.vlanId.toString(), it.name, it.scopeType, it.scopeTargetId, it.description.orEmpty()) } ?: VlanForm()
    }
}

data class SubnetForm(
    val cidrBlock: String = "",
    val gatewayIp: String = "",
    val vlanRefId: String? = null,
    val name: String = "",
    val description: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        FieldValidators.cidr(cidrBlock, required = true)?.let { put("cidrBlock", it) }
        FieldValidators.ipv4(gatewayIp)?.let { put("gatewayIp", it) }
    }

    fun toSubnet(existing: Subnet?): Subnet = (existing ?: Subnet(cidrBlock = cidrBlock.trim())).copy(
        cidrBlock = cidrBlock.trim(),
        gatewayIp = gatewayIp.orNull(),
        vlanId = vlanRefId,
        name = name.orNull(),
        description = description.orNull(),
    )

    companion object {
        fun from(s: Subnet?) = s?.let { SubnetForm(it.cidrBlock, it.gatewayIp.orEmpty(), it.vlanId, it.name.orEmpty(), it.description.orEmpty()) }
            ?: SubnetForm()
    }
}

data class LogicalInterfaceForm(
    val deviceId: String? = null,
    val name: String = "",
    val ipAddress: String = "",
    val subnetCidr: String = "",
    val vlanId: Int? = null,
    val isL3: Boolean = true,
    val macAddress: String = "",
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        if (deviceId == null) put("deviceId", "Selezionare l'apparato")
        FieldValidators.required(name, "Nome")?.let { put("name", it) }
        FieldValidators.ipv4(ipAddress)?.let { put("ipAddress", it) }
        FieldValidators.cidr(subnetCidr)?.let { put("subnetCidr", it) }
        FieldValidators.mac(macAddress)?.let { put("macAddress", it) }
    }

    fun toInterface(existing: LogicalInterface?): LogicalInterface {
        val dev = requireNotNull(deviceId)
        return (existing ?: LogicalInterface(deviceId = dev, name = name.trim())).copy(
            deviceId = dev,
            name = name.trim(),
            ipAddress = ipAddress.orNull(),
            subnetCidr = subnetCidr.orNull(),
            vlanId = vlanId,
            isL3 = isL3,
            macAddress = macAddress.orNull(),
            notes = notes.orNull(),
        )
    }

    companion object {
        fun from(i: LogicalInterface?) = i?.let {
            LogicalInterfaceForm(it.deviceId, it.name, it.ipAddress.orEmpty(), it.subnetCidr.orEmpty(), it.vlanId, it.isL3, it.macAddress.orEmpty(), it.notes.orEmpty())
        } ?: LogicalInterfaceForm()
    }
}

data class WanForm(
    val name: String = "",
    val type: WanVpnType = WanVpnType.WAN,
    val provider: String = "",
    val bandwidth: String = "",
    val localDeviceId: String? = null,
    val localSite: String = "",
    val remoteDeviceId: String? = null,
    val remoteSite: String = "",
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        FieldValidators.required(name, "Nome")?.let { put("name", it) }
    }

    fun toConnection(existing: WanVpnConnection?): WanVpnConnection = (existing ?: WanVpnConnection(name = name.trim())).copy(
        name = name.trim(),
        type = type,
        providerOrCarrier = provider.orNull(),
        bandwidth = bandwidth.orNull(),
        localEndpointDeviceId = localDeviceId,
        localEndpointSiteDescription = localSite.orNull(),
        remoteEndpointDeviceId = remoteDeviceId,
        remoteEndpointSiteDescription = remoteSite.orNull(),
        notes = notes.orNull(),
    )

    companion object {
        fun from(w: WanVpnConnection?) = w?.let {
            WanForm(
                it.name, it.type, it.providerOrCarrier.orEmpty(), it.bandwidth.orEmpty(), it.localEndpointDeviceId,
                it.localEndpointSiteDescription.orEmpty(), it.remoteEndpointDeviceId, it.remoteEndpointSiteDescription.orEmpty(), it.notes.orEmpty()
            )
        } ?: WanForm()
    }
}

data class DeviceConfigForm(
    val deviceId: String? = null,
    val title: String = "",
    val configText: String = "",
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        if (deviceId == null) put("deviceId", "Selezionare l'apparato")
        FieldValidators.required(title, "Titolo")?.let { put("title", it) }
    }

    fun toConfig(existing: DeviceConfiguration?): DeviceConfiguration {
        val dev = requireNotNull(deviceId)
        return (existing ?: DeviceConfiguration(deviceId = dev, title = title.trim())).copy(
            deviceId = dev, title = title.trim(), configText = configText.ifBlank { null }, notes = notes.orNull()
        )
    }

    companion object {
        fun from(c: DeviceConfiguration?) = c?.let { DeviceConfigForm(it.deviceId, it.title, it.configText.orEmpty(), it.notes.orEmpty()) }
            ?: DeviceConfigForm()
    }
}

data class PowerFeedForm(
    val deviceId: String? = null,
    val feedName: String = "",
    val feedType: PowerFeedType = PowerFeedType.PRIMARY_A,
    val sourceDeviceId: String? = null,
    val sourceOutlet: String = "",
    val voltage: String = "230",
    val loadVa: String = "",
    val loadWatts: String = "",
    val runtimeMinutes: String = "",
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        if (deviceId == null) put("deviceId", "Selezionare l'apparato alimentato")
        FieldValidators.required(feedName, "Nome")?.let { put("feedName", it) }
        if (sourceDeviceId != null && sourceDeviceId == deviceId) put("sourceDeviceId", "La sorgente non può essere l'apparato stesso")
        FieldValidators.int(voltage, min = 1, max = 1000)?.let { put("voltage", it) }
        FieldValidators.decimal(loadVa, min = 0.0)?.let { put("loadVa", it) }
        FieldValidators.decimal(loadWatts, min = 0.0)?.let { put("loadWatts", it) }
        FieldValidators.int(runtimeMinutes, min = 0)?.let { put("runtimeMinutes", it) }
    }

    fun toFeed(existing: PowerFeed?): PowerFeed {
        val dev = requireNotNull(deviceId)
        return (existing ?: PowerFeed(deviceId = dev, feedName = feedName.trim())).copy(
            deviceId = dev,
            feedName = feedName.trim(),
            feedType = feedType,
            sourceDeviceId = sourceDeviceId,
            sourceOutletDescription = sourceOutlet.orNull(),
            voltageVolts = parseInt(voltage),
            loadVa = parseDecimal(loadVa),
            loadWatts = parseDecimal(loadWatts),
            observedRuntimeMinutes = parseInt(runtimeMinutes),
            notes = notes.orNull(),
        )
    }

    companion object {
        fun from(f: PowerFeed?) = f?.let {
            PowerFeedForm(
                it.deviceId, it.feedName, it.feedType, it.sourceDeviceId, it.sourceOutletDescription.orEmpty(),
                it.voltageVolts?.toString().orEmpty(), it.loadVa?.toString().orEmpty(), it.loadWatts?.toString().orEmpty(),
                it.observedRuntimeMinutes?.toString().orEmpty(), it.notes.orEmpty()
            )
        } ?: PowerFeedForm()
    }
}

data class PoeForm(
    val portId: String? = null,
    val role: PoeRole = PoeRole.PSE_SOURCE,
    val standard: PoeStandard = PoeStandard.IEEE_802_3AT,
    val watts: String = "",
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        if (portId == null) put("portId", "Selezionare la porta")
        FieldValidators.decimal(watts, min = 0.0, max = 100.0)?.let { put("watts", it) }
    }

    fun toMapping(existing: PoeMapping?): PoeMapping {
        val port = requireNotNull(portId)
        return (existing ?: PoeMapping(portId = port)).copy(
            portId = port, role = role, standard = standard, allocatedPowerWatts = parseDecimal(watts), notes = notes.orNull()
        )
    }

    companion object {
        fun from(p: PoeMapping?) = p?.let { PoeForm(it.portId, it.role, it.standard, it.allocatedPowerWatts?.toString().orEmpty(), it.notes.orEmpty()) }
            ?: PoeForm()
    }
}

/** Target of badges and extra fields: one of the [TARGET_TYPES] plus the chosen entity id. */
data class TargetRef(val type: String = "PROJECT", val id: String? = null) {
    companion object {
        val TARGET_TYPES = listOf("PROJECT", "DEVICE", "RACK", "PORT", "AREA")
    }
}

data class BadgeForm(
    val target: TargetRef = TargetRef(),
    val label: String = "",
    val category: BadgeCategory = BadgeCategory.FREE_LABEL,
    val notes: String = "",
) {
    fun errors(): Map<String, String> = buildMap {
        FieldValidators.required(label, "Etichetta")?.let { put("label", it) }
        if (target.type != "PROJECT" && target.id == null) put("target", "Selezionare l'elemento")
    }

    fun toBadge(existing: DocumentBadge?, projectId: String): DocumentBadge {
        val targetId = if (target.type == "PROJECT") projectId else requireNotNull(target.id)
        return (existing ?: DocumentBadge(targetType = target.type, targetId = targetId, label = label.trim())).copy(
            targetType = target.type, targetId = targetId, label = label.trim(), category = category, notes = notes.orNull()
        )
    }

    companion object {
        fun from(b: DocumentBadge?) = b?.let { BadgeForm(TargetRef(it.targetType, it.targetId), it.label, it.category, it.notes.orEmpty()) }
            ?: BadgeForm()
    }
}

data class ExtraFieldForm(
    val target: TargetRef = TargetRef(),
    val key: String = "",
    val value: String = "",
    val fieldType: CustomFieldType = CustomFieldType.STRING,
    val classification: AttachmentClassification = AttachmentClassification.SHAREABLE,
) {
    fun errors(): Map<String, String> = buildMap {
        FieldValidators.required(key, "Nome campo")?.let { put("key", it) }
        if (target.type != "PROJECT" && target.id == null) put("target", "Selezionare l'elemento")
        if (fieldType == CustomFieldType.NUMBER) FieldValidators.decimal(value)?.let { put("value", it) }
    }

    fun toField(existing: CustomExtraField?, projectId: String): CustomExtraField {
        val targetId = if (target.type == "PROJECT") projectId else requireNotNull(target.id)
        return (existing ?: CustomExtraField(targetType = target.type, targetId = targetId, fieldKey = key.trim(), fieldValue = value)).copy(
            targetType = target.type, targetId = targetId, fieldKey = key.trim(), fieldValue = value.trim(),
            fieldType = fieldType, classification = classification
        )
    }

    companion object {
        fun from(f: CustomExtraField?) = f?.let { ExtraFieldForm(TargetRef(it.targetType, it.targetId), it.fieldKey, it.fieldValue, it.fieldType, it.classification) }
            ?: ExtraFieldForm()
    }
}
