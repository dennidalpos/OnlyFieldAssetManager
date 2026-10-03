package com.onlyfield.assetmanager.core.display

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.*

/**
 * Read-only lookup over a [Project] that resolves ids to human-readable names,
 * so UIs can offer pickers and never display raw ids.
 */
class ProjectIndex(val project: Project) {

    data class PortRef(val port: Port, val device: Device)

    val devices: List<Device> = project.businessUnits.flatMap { it.devices }
    val areas: List<Area> = project.businessUnits
        .flatMap { bu -> bu.areas + bu.sites.flatMap { it.areas } }
        .distinctBy { it.id }
    val sites: List<Site> = project.businessUnits.flatMap { it.sites }
    val ports: List<PortRef> = devices.flatMap { dev -> dev.ports.map { PortRef(it, dev) } }

    private val deviceById = devices.associateBy { it.id }
    private val portById = ports.associateBy { it.port.id }
    private val areaById = areas.associateBy { it.id }
    private val siteById = sites.associateBy { it.id }
    private val rackById = project.racks.associateBy { it.id }
    private val buById = project.businessUnits.associateBy { it.id }

    fun device(id: String?): Device? = id?.let(deviceById::get)
    fun port(id: String?): PortRef? = id?.let(portById::get)
    fun area(id: String?): Area? = id?.let(areaById::get)
    fun rack(id: String?): Rack? = id?.let(rackById::get)
    fun businessUnitOf(deviceId: String): BusinessUnit? =
        project.businessUnits.find { bu -> bu.devices.any { it.id == deviceId } }

    fun deviceName(id: String?, fallback: String = "—", i18n: Messages = Messages()): String = device(id)?.technicalName ?: fallback
    fun areaName(id: String?, fallback: String = "—"): String = area(id)?.name ?: fallback
    fun rackName(id: String?, fallback: String = "—"): String = rack(id)?.name ?: fallback
    fun siteName(id: String?, fallback: String = "—"): String = id?.let(siteById::get)?.name ?: fallback

    /** "SW-CORE01 › Gi1/0/1", or [fallback] when the port does not exist. */
    fun portLabel(id: String?, fallback: String = "—"): String =
        port(id)?.let { "${it.device.technicalName} › ${it.port.name}" } ?: fallback

    /** Name of an entity referenced by a string target type ("DEVICE", "RACK", "PORT", ...). */
    fun targetLabel(targetType: String, targetId: String?, i18n: Messages = Messages()): String {
        val name = when (targetType.uppercase()) {
            "PROJECT" -> project.name
            "DEVICE" -> device(targetId)?.technicalName
            "RACK" -> rack(targetId)?.name
            "PORT" -> port(targetId)?.let { "${it.device.technicalName} › ${it.port.name}" }
            "CABLE" -> project.cables.find { it.id == targetId }?.codeOrLabel
            "AREA" -> area(targetId)?.name
            "SITE" -> targetId?.let(siteById::get)?.name
            "BUSINESS_UNIT" -> targetId?.let(buById::get)?.name
            else -> null
        }
        return "${EntityTypeLabels.of(targetType, i18n = i18n)}: ${name ?: i18n.text("text.c86fc6dfbd62")}"
    }

    /** Attachments linked to an entity (photos of a device, rack or area). */
    fun attachmentsOf(targetId: String): List<Attachment> = project.attachments.filter { it.targetId == targetId }

    /** "Apparato: SW-01" for a linked attachment, null when it belongs to the whole project. */
    fun attachmentTarget(attachment: Attachment, i18n: Messages = Messages()): String? = attachment.targetType
        ?.takeIf { it != AttachmentTargetType.PROJECT }
        ?.let { targetLabel(it.name, attachment.targetId, i18n = i18n) }

    /** Best-effort name of any entity in the project, used e.g. for validation issues. */
    fun entityName(id: String?, i18n: Messages = Messages()): String? {
        if (id == null) return null
        return device(id)?.technicalName
            ?: port(id)?.let { "${it.device.technicalName} › ${it.port.name}" }
            ?: rack(id)?.name
            ?: area(id)?.name
            ?: siteById[id]?.name
            ?: buById[id]?.name
            ?: project.cables.find { it.id == id }?.let { i18n.text("text.dae31b0efa98", it.codeOrLabel ?: portLabel(it.portAId)) }
            ?: project.vlans.find { it.id == id }?.let { i18n.text("text.b3bc38a1979e", it.vlanId, it.name) }
            ?: project.subnets.find { it.id == id }?.cidrBlock
            ?: project.deviceModels.find { it.id == id }?.name
            ?: project.attachments.find { it.id == id }?.name
            ?: project.powerFeeds.find { it.id == id }?.feedName
    }
}
