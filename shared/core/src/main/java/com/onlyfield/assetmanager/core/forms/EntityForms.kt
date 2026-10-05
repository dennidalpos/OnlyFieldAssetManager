package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.forms.FieldValidators.parseDecimal
import com.onlyfield.assetmanager.core.forms.FieldValidators.parseInt

/** Form states preserve unedited entity fields. */

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
    val siteId: String? = null,
    val areaId: String? = null,
    val rackId: String? = null,
    val positionU: String = "",
    val heightU: String = "1",
    val rackSide: RackSide = RackSide.FRONT,
    val mountingType: MountingType = MountingType.RACK_MOUNT,
    val deviceModelId: String? = null,
    val observationStatus: ObservationStatus = ObservationStatus.TO_VERIFY,
    val notes: String = "",
    val hardware: HardwareSpec = HardwareSpec(),
    val operationalStatus: OperationalStatus = OperationalStatus.IN_SERVICE,
) {
    fun errors(rackHeightU: Int?, i18n: Messages = Messages()): Map<String, String> = buildMap {
        FieldValidators.required(technicalName, i18n.text("text.4f1b2dcbe4ce"), i18n = i18n)?.let { put("technicalName", it) }
        if (siteId == null) put("siteId", i18n.text("text.da384d522c1b"))
        FieldValidators.ipv4(ipAddress, i18n = i18n)?.let { put("ipAddress", it) }
        FieldValidators.mac(macAddress, i18n = i18n)?.let { put("macAddress", it) }
        FieldValidators.int(heightU, min = 1, max = 60, required = true, i18n = i18n)?.let { put("heightU", it) }
        if (rackId != null) {
            val height = parseInt(heightU) ?: 1
            val maxStart = rackHeightU?.let { (it - height + 1).coerceAtLeast(1) }
            FieldValidators.int(positionU, min = 1, max = maxStart, i18n = i18n)?.let { put("positionU", it) }
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
            hardware = hardware,
            operationalStatus = operationalStatus,
        )
    }

    companion object {
        fun from(device: Device?, siteId: String?): DeviceForm = device?.let {
            DeviceForm(
                technicalName = it.technicalName,
                physicalLabel = it.physicalLabel.orEmpty(),
                alias = it.alias.orEmpty(),
                ipAddress = it.ipAddress.orEmpty(),
                macAddress = it.macAddress.orEmpty(),
                serialNumber = it.serialNumber.orEmpty(),
                objectTypeId = it.objectTypeId,
                category = it.category,
                siteId = siteId,
                areaId = it.areaId,
                rackId = it.rackId,
                positionU = it.positionU?.toString().orEmpty(),
                heightU = it.heightU.toString(),
                rackSide = it.rackSide,
                mountingType = it.mountingType,
                deviceModelId = it.deviceModelId,
                observationStatus = it.observation?.status ?: ObservationStatus.TO_VERIFY,
                notes = it.observation?.notes.orEmpty(),
                hardware = it.hardware,
                operationalStatus = it.operationalStatus,
            )
        } ?: DeviceForm(siteId = siteId)
    }
}

data class RackForm(
    val mountingDepthMm: String = "",
    val deviceModelId: String? = null,
    val mountingType: String? = null,
    val name: String = "",
    val heightU: String = "42",
    val numberingDirection: NumberingDirection = NumberingDirection.BOTTOM_TO_TOP,
    val areaId: String? = null,
    val depthMm: String = "",
    val notes: String = "",
) {
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        FieldValidators.required(name, i18n.text("text.5086900635fe"), i18n = i18n)?.let { put("name", it) }
        FieldValidators.int(heightU, min = 1, max = 60, required = true, i18n = i18n)?.let { put("heightU", it) }
        FieldValidators.int(depthMm, min = 100, max = 2000, i18n = i18n)?.let { put("depthMm", it) }
        FieldValidators.int(mountingDepthMm, min = 1, max = 2000, i18n = i18n)?.let { put("mountingDepthMm", it) }
    }

    fun toRack(existing: Rack?): Rack = (existing ?: Rack(name = name.trim())).copy(
        name = name.trim(),
        heightU = parseInt(heightU) ?: existing?.heightU ?: 42,
        numberingDirection = numberingDirection,
        areaId = areaId,
        depthMm = parseInt(depthMm),
        mountingDepthMm = parseInt(mountingDepthMm),
        deviceModelId = deviceModelId,
        mountingType = mountingType,
        notes = notes.orNull(),
    )

    companion object {
        fun from(rack: Rack?) = rack?.let {
            RackForm(name = it.name, heightU = it.heightU.toString(), numberingDirection = it.numberingDirection, areaId = it.areaId, depthMm = it.depthMm?.toString().orEmpty(), notes = it.notes.orEmpty(), mountingDepthMm = it.mountingDepthMm?.toString().orEmpty(), deviceModelId = it.deviceModelId, mountingType = it.mountingType)
        } ?: RackForm()
    }
}

data class CableForm(
    val deviceModelId: String? = null,
    val codeOrLabel: String = "",
    val portAId: String? = null,
    val portBId: String? = null,
    val medium: CableMedium = CableMedium.ETHERNET_COPPER,
    val color: String = "",
    val lengthValue: String = "",
    val notes: String = "",
) {
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        if (portAId != null && portAId == portBId) put("portBId", i18n.text("text.54cba72dee5f"))
        FieldValidators.decimal(lengthValue, min = 0.0, i18n = i18n)?.let { put("lengthValue", it) }
    }

    fun toCable(existing: Cable?): Cable = (existing ?: Cable()).copy(
        codeOrLabel = codeOrLabel.orNull(),
        deviceModelId = deviceModelId,
        portAId = portAId,
        portBId = portBId,
        medium = medium,
        color = color.orNull(),
        lengthValue = parseDecimal(lengthValue),
        notes = notes.orNull(),
    )

    companion object {
        fun from(c: Cable?) = c?.let {
            CableForm(it.deviceModelId, it.codeOrLabel.orEmpty(), it.portAId, it.portBId, it.medium, it.color.orEmpty(), it.lengthValue?.toString().orEmpty(), it.notes.orEmpty())
        } ?: CableForm()
    }
}

data class PanelMappingForm(
    val portAId: String? = null,
    val portBId: String? = null,
    val isUnknownPassage: Boolean = false,
) {
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        if (portAId == null) put("portAId", i18n.text("text.c3f00a679699"))
        if (portAId != null && portAId == portBId) put("portBId", i18n.text("text.7a79a3b345af"))
    }

    fun toMapping(existing: PanelMapping?): PanelMapping {
        val a = requireNotNull(portAId)
        return (existing ?: PanelMapping(portAId = a)).copy(portAId = a, portBId = portBId, isUnknownPassage = isUnknownPassage)
    }

    companion object {
        fun from(m: PanelMapping?) = m?.let { PanelMappingForm(it.portAId, it.portBId, it.isUnknownPassage) } ?: PanelMappingForm()
    }
}

data class VlanForm(
    val vlanId: String = "",
    val name: String = "",
    val scopeType: VlanScopeType = VlanScopeType.PROJECT,
    val scopeTargetId: String? = null,
    val description: String = "",
) {
    fun errors(existingVlanIds: Set<Int>, i18n: Messages = Messages()): Map<String, String> = buildMap {
        val numberError = FieldValidators.int(vlanId, min = 1, max = 4094, required = true, i18n = i18n)
        if (numberError != null) put("vlanId", numberError)
        else if (parseInt(vlanId) in existingVlanIds && scopeType == VlanScopeType.PROJECT) put("vlanId", i18n.text("text.9360f1518699"))
        FieldValidators.required(name, i18n.text("text.5086900635fe"), i18n = i18n)?.let { put("name", it) }
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
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        FieldValidators.cidr(cidrBlock, required = true, i18n = i18n)?.let { put("cidrBlock", it) }
        FieldValidators.ipv4(gatewayIp, i18n = i18n)?.let { put("gatewayIp", it) }
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
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        if (deviceId == null) put("deviceId", i18n.text("text.a723641bd589"))
        FieldValidators.required(name, i18n.text("text.5086900635fe"), i18n = i18n)?.let { put("name", it) }
        FieldValidators.ipv4(ipAddress, i18n = i18n)?.let { put("ipAddress", it) }
        FieldValidators.cidr(subnetCidr, i18n = i18n)?.let { put("subnetCidr", it) }
        FieldValidators.mac(macAddress, i18n = i18n)?.let { put("macAddress", it) }
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
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        FieldValidators.required(name, i18n.text("text.5086900635fe"), i18n = i18n)?.let { put("name", it) }
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
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        if (deviceId == null) put("deviceId", i18n.text("text.a723641bd589"))
        FieldValidators.required(title, i18n.text("text.d8f4eedefb37"), i18n = i18n)?.let { put("title", it) }
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
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        if (deviceId == null) put("deviceId", i18n.text("text.ed0843a576b5"))
        FieldValidators.required(feedName, i18n.text("text.5086900635fe"), i18n = i18n)?.let { put("feedName", it) }
        if (sourceDeviceId != null && sourceDeviceId == deviceId) put("sourceDeviceId", i18n.text("text.5c29f4443952"))
        FieldValidators.int(voltage, min = 1, max = 1000, i18n = i18n)?.let { put("voltage", it) }
        FieldValidators.decimal(loadVa, min = 0.0, i18n = i18n)?.let { put("loadVa", it) }
        FieldValidators.decimal(loadWatts, min = 0.0, i18n = i18n)?.let { put("loadWatts", it) }
        FieldValidators.int(runtimeMinutes, min = 0, i18n = i18n)?.let { put("runtimeMinutes", it) }
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
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        if (portId == null) put("portId", i18n.text("text.4d544da7edd0"))
        FieldValidators.decimal(watts, min = 0.0, max = 100.0, i18n = i18n)?.let { put("watts", it) }
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

/** Badge or extra-field target. */
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
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        FieldValidators.required(label, i18n.text("text.9fe5b72aa900"), i18n = i18n)?.let { put("label", it) }
        if (target.type != "PROJECT" && target.id == null) put("target", i18n.text("text.efc248da0e70"))
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
    fun errors(i18n: Messages = Messages()): Map<String, String> = buildMap {
        FieldValidators.required(key, i18n.text("text.8451f41f283d"), i18n = i18n)?.let { put("key", it) }
        if (target.type != "PROJECT" && target.id == null) put("target", i18n.text("text.efc248da0e70"))
        if (fieldType == CustomFieldType.NUMBER) FieldValidators.decimal(value, i18n = i18n)?.let { put("value", it) }
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
