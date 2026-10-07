package com.onlyfield.assetmanager.core.edit

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/** Associated records live in the device's existing local trash JSON. */
@Serializable
internal data class DeviceTrashData(
    val credentials: List<Credential> = emptyList(),
    val configurations: List<DeviceConfiguration> = emptyList(),
    val powerFeeds: List<PowerFeed> = emptyList(),
    val extraFields: List<CustomExtraField> = emptyList(),
) {
    fun snapshot(json: Json, device: Device): String {
        val base = json.encodeToJsonElement(Device.serializer(), device).jsonObject
        if (credentials.isEmpty() && configurations.isEmpty() && powerFeeds.isEmpty() && extraFields.isEmpty()) return base.toString()
        return JsonObject(base + ("associatedData" to json.encodeToJsonElement(serializer(), this))).toString()
    }

    fun removeFrom(project: Project): Project {
        val credentialIds = credentials.map(Credential::id).toSet()
        val configIds = configurations.map(DeviceConfiguration::id).toSet()
        val feedIds = powerFeeds.map(PowerFeed::id).toSet()
        val fieldIds = extraFields.map(CustomExtraField::id).toSet()
        return project.copy(credentials = project.credentials.filterNot { it.id in credentialIds },
            deviceConfigurations = project.deviceConfigurations.filterNot { it.id in configIds },
            powerFeeds = project.powerFeeds.filterNot { it.id in feedIds }, customExtraFields = project.customExtraFields.filterNot { it.id in fieldIds })
    }

    fun restore(project: Project, deviceId: String, i18n: Messages): Project {
        check(credentials.all { it.deviceId == deviceId } && configurations.all { it.deviceId == deviceId } &&
            powerFeeds.all { it.deviceId == deviceId || it.sourceDeviceId == deviceId } &&
            extraFields.all { it.targetType == "DEVICE" && it.targetId == deviceId }) { i18n.text("trash.invalidEntry") }
        fun checkIds(restored: List<String>, active: List<String>) {
            check(restored.distinct().size == restored.size) { i18n.text("trash.invalidEntry") }
            check(restored.none { it in active }) { i18n.text("trash.alreadyExists") }
        }
        checkIds(credentials.map(Credential::id), project.credentials.map(Credential::id))
        checkIds(configurations.map(DeviceConfiguration::id), project.deviceConfigurations.map(DeviceConfiguration::id))
        checkIds(powerFeeds.map(PowerFeed::id), project.powerFeeds.map(PowerFeed::id))
        checkIds(extraFields.map(CustomExtraField::id), project.customExtraFields.map(CustomExtraField::id))
        val devices = project.sites.flatMap { it.devices }.map(Device::id).toSet()
        check(powerFeeds.all { it.deviceId in devices && (it.sourceDeviceId == null || it.sourceDeviceId in devices) } &&
            configurations.all { it.attachmentId == null || project.attachments.any { attachment -> attachment.id == it.attachmentId } }) { i18n.text("trash.contextMissing") }
        check(powerFeeds.isEmpty() || !(project.powerFeeds + powerFeeds).hasPowerFeedCycle()) { i18n.text("trash.powerCycle") }
        return project.copy(credentials = project.credentials + credentials, deviceConfigurations = project.deviceConfigurations + configurations,
            powerFeeds = project.powerFeeds + powerFeeds, customExtraFields = project.customExtraFields + extraFields)
    }

    companion object {
        fun capture(project: Project, deviceId: String) = DeviceTrashData(
            project.credentials.filter { it.deviceId == deviceId }, project.deviceConfigurations.filter { it.deviceId == deviceId },
            project.powerFeeds.filter { it.deviceId == deviceId || it.sourceDeviceId == deviceId },
            project.customExtraFields.filter { it.targetType == "DEVICE" && it.targetId == deviceId },
        )

        fun decode(json: Json, text: String): DeviceTrashData = json.parseToJsonElement(text).jsonObject["associatedData"]
            ?.let { json.decodeFromJsonElement(serializer(), it) } ?: DeviceTrashData()
    }
}
