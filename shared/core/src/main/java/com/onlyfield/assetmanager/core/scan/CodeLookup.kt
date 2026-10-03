package com.onlyfield.assetmanager.core.scan

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.Cable
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Rack

/** What a scanned code (QR, barcode, USB reader) points to. */
sealed interface CodeMatch {
    data class DeviceMatch(val device: Device, val field: String) : CodeMatch
    data class PortMatch(val port: ProjectIndex.PortRef) : CodeMatch
    data class CableMatch(val cable: Cable) : CodeMatch
    data class RackMatch(val rack: Rack) : CodeMatch
    /** An ofam label printed for another project. */
    data class OtherProject(val label: LabelCode) : CodeMatch
    /** Unknown code: the UI can offer it as serial number of a new device. */
    data class NotFound(val code: String) : CodeMatch
}

/** Resolves a scanned code against a project. Exact matches only, case-insensitive. */
object CodeLookup {

    fun find(index: ProjectIndex, rawCode: String, i18n: Messages = Messages()): CodeMatch {
        val code = rawCode.trim()
        if (code.isEmpty()) return CodeMatch.NotFound(code)
        LabelCode.parse(code)?.let { return resolveLabel(index, it, i18n = i18n) }
        fun same(value: String?) = value != null && value.trim().equals(code, ignoreCase = true)

        // Serial first: it is the only field meant to be unique.
        index.devices.firstOrNull { same(it.serialNumber) }?.let { return CodeMatch.DeviceMatch(it, i18n.text("text.488b09e885d9")) }
        index.devices.firstOrNull { same(it.physicalLabel) }?.let { return CodeMatch.DeviceMatch(it, i18n.text("text.9fe5b72aa900")) }
        index.devices.firstOrNull { same(it.alias) }?.let { return CodeMatch.DeviceMatch(it, i18n.text("text.b19e02e9502b")) }
        index.devices.firstOrNull { same(it.technicalName) }?.let { return CodeMatch.DeviceMatch(it, i18n.text("text.4f1b2dcbe4ce")) }
        index.project.cables.firstOrNull { same(it.codeOrLabel) }?.let { return CodeMatch.CableMatch(it) }
        index.ports.firstOrNull { same(it.port.label) }?.let { return CodeMatch.PortMatch(it) }
        return CodeMatch.NotFound(code)
    }

    private fun resolveLabel(index: ProjectIndex, label: LabelCode, i18n: Messages = Messages()): CodeMatch {
        if (label.projectId != index.project.id) return CodeMatch.OtherProject(label)
        val found = when (label.type) {
            LabelCode.Type.DEVICE -> index.device(label.id)?.let { CodeMatch.DeviceMatch(it, i18n.text("text.5003aaa28e13")) }
            LabelCode.Type.RACK -> index.rack(label.id)?.let { CodeMatch.RackMatch(it) }
            LabelCode.Type.CABLE -> index.project.cables.find { it.id == label.id }?.let { CodeMatch.CableMatch(it) }
        }
        return found ?: CodeMatch.NotFound(label.toString())
    }
}
