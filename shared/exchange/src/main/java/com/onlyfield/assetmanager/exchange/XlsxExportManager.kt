package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.display.toDisplayString

import com.onlyfield.assetmanager.core.i18n.Messages

import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.Project
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes OpenXML XLSX without credentials or formula interpretation. */
object XlsxExportManager {

    fun exportXlsxToStream(
        project: Project,
        filterConfig: ExportFilterConfig,
        outputStream: OutputStream,
        i18n: Messages = Messages()) {
        val filteredDevices = project.businessUnits
            .filter { (filterConfig.selectedBusinessUnitId == null) || (it.id == filterConfig.selectedBusinessUnitId) }
            .flatMap { bu -> bu.devices.filter { device ->
                val siteId = device.siteId ?: bu.sites.find { site -> site.areas.any { it.id == device.areaId } }?.id
                ((filterConfig.selectedSiteId == null) || (siteId == filterConfig.selectedSiteId)) &&
                    ((filterConfig.selectedAreaId == null) || (device.areaId == filterConfig.selectedAreaId))
            } }
            .filter { (filterConfig.selectedCategory == null) || (it.category == filterConfig.selectedCategory) }
            .distinctBy { it.id }

        val filteredDeviceIds = filteredDevices.map { it.id }.toSet()

        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(buildContentTypesXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(buildRelsXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(buildWorkbookXml(i18n).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(buildWorkbookRelsXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(buildStylesXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(buildSheet1Xml(project, filteredDevices, i18n = i18n).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
            zip.write(buildSheet2Xml(project, filteredDeviceIds, i18n = i18n).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet3.xml"))
            zip.write(buildSheet3Xml(project, i18n = i18n).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet4.xml"))
            zip.write(buildSheet4Xml(project, filteredDeviceIds, i18n = i18n).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet5.xml"))
            zip.write(buildSheet5Xml(project, filterConfig, filteredDeviceIds, i18n = i18n).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    private fun escapeXml(value: String?): String {
        if (value == null) return ""
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun cellStr(col: String, row: Int, text: String): String {
        return "<c r=\"$col$row\" t=\"inlineStr\"><is><t>${escapeXml(text)}</t></is></c>"
    }

    private fun cellNum(col: String, row: Int, num: Number?): String {
        if (num == null) return "<c r=\"$col$row\" t=\"inlineStr\"><is><t>-</t></is></c>"
        return "<c r=\"$col$row\"><v>$num</v></c>"
    }

    private fun buildContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet5.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
    }

    private fun buildRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookXml(i18n: Messages): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${escapeXml(i18n.text("text.d68b200f51e2"))}" sheetId="1" r:id="rId1"/>
    <sheet name="${escapeXml(i18n.text("text.48a06cac7f98"))}" sheetId="2" r:id="rId2"/>
    <sheet name="${escapeXml(i18n.text("text.bb4d35db55dd"))}" sheetId="3" r:id="rId3"/>
    <sheet name="${escapeXml(i18n.text("text.9ec7c611be29"))}" sheetId="4" r:id="rId4"/>
    <sheet name="${escapeXml(i18n.text("text.49fc6d4f4848"))}" sheetId="5" r:id="rId5"/>
  </sheets>
</workbook>"""
    }

    private fun buildWorkbookRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet5.xml"/>
  <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1">
    <font><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="1">
    <fill><patternFill patternType="none"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
  </cellXfs>
</styleSheet>"""
    }

    private fun buildSheet1Xml(project: Project, devices: List<com.onlyfield.assetmanager.core.model.Device>, i18n: Messages = Messages()): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf("BU", i18n.text("text.f163aa3f6310"), i18n.text("text.024dc204d7ba"), i18n.text("text.29caae5fe1e7"), i18n.text("text.d5680523de72"), i18n.text("text.b19e02e9502b"), i18n.text("text.ebb396f2d486"), i18n.text("text.8894b359b4e9"), i18n.text("text.54276aa0307f"), "Rack/Posizione", i18n.text("text.90c2d339a9d5"), i18n.text("text.2edfc95a3c46"), i18n.text("text.3b495129c5de"), i18n.text("text.d8da2c49df39")) + listOf(i18n.text("config.width"), i18n.text("config.depth"), i18n.text("config.poeBudget"), i18n.text("config.features"))
        sb.append("<row r=\"1\">")
        headers.forEachIndexed { idx, h ->
            val colLetter = ('A' + idx).toString()
            sb.append(cellStr(colLetter, 1, h))
        }
        sb.append("</row>\n")

        var rowIdx = 2
        for (dev in devices) {
            val buName = project.businessUnits.find { bu -> bu.devices.any { it.id == dev.id } }?.name ?: "-"
            val siteName = project.businessUnits.flatMap { it.sites }.find { it.id == dev.siteId }?.name ?: "-"
            val areaName = project.businessUnits.flatMap { bu -> bu.sites.flatMap { it.areas } + bu.areas }.find { it.id == dev.areaId }?.name ?: "-"
            val rackName = project.racks.find { it.id == dev.rackId }?.name ?: i18n.text("text.3f03be4817b0")
            val rackPos = if (dev.rackId != null) i18n.text("text.cf603098c1b7", rackName, dev.positionU ?: "-", dev.rackSide.toDisplayString(i18n)) else i18n.text("text.3f03be4817b0")
            val modelName = project.deviceModels.find { it.id == dev.deviceModelId }?.name ?: "-"

            sb.append("<row r=\"$rowIdx\">")
            sb.append(cellStr("A", rowIdx, buName))
            sb.append(cellStr("B", rowIdx, siteName))
            sb.append(cellStr("C", rowIdx, areaName))
            sb.append(cellStr("D", rowIdx, dev.technicalName))
            sb.append(cellStr("E", rowIdx, dev.physicalLabel ?: "-"))
            sb.append(cellStr("F", rowIdx, dev.alias ?: "-"))
            sb.append(cellStr("G", rowIdx, dev.ipAddress ?: "-"))
            sb.append(cellStr("H", rowIdx, dev.macAddress ?: "-"))
            sb.append(cellStr("I", rowIdx, dev.category.toDisplayString(i18n)))
            sb.append(cellStr("J", rowIdx, rackPos))
            sb.append(cellStr("K", rowIdx, modelName))
            sb.append(cellNum("L", rowIdx, dev.ports.size))
            sb.append(cellStr("M", rowIdx, (dev.observation?.status ?: com.onlyfield.assetmanager.core.model.ObservationStatus.VERIFIED).toDisplayString(i18n)))
            sb.append(cellStr("N", rowIdx, dev.observation?.notes ?: "-"))
            sb.append(cellNum("O", rowIdx, dev.hardware.widthMm))
            sb.append(cellNum("P", rowIdx, dev.hardware.depthMm))
            sb.append(cellNum("Q", rowIdx, dev.hardware.poeBudgetWatts))
            sb.append(cellStr("R", rowIdx, dev.hardware.features.joinToString(", ")))
            sb.append("</row>\n")
            rowIdx++
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun buildSheet2Xml(project: Project, filteredDeviceIds: Set<String>, i18n: Messages = Messages()): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf(i18n.text("text.9f626fe59e04"), i18n.text("text.f066e83907df"), i18n.text("text.20e26ce71ba4"), i18n.text("text.2de5f6131596"), i18n.text("text.abc0f3511ebf"), i18n.text("text.245eccd84730"), i18n.text("text.13ee8eeebbb8"), i18n.text("text.dcc43f317d0c"), i18n.text("text.55a2c4d86dad"), i18n.text("text.9d3380130243"))
        sb.append("<row r=\"1\">")
        headers.forEachIndexed { idx, h ->
            val colLetter = ('A' + idx).toString()
            sb.append(cellStr(colLetter, 1, h))
        }
        sb.append("</row>\n")

        val allPorts = project.businessUnits.flatMap { it.devices }.flatMap { it.ports }.associateBy { it.id }
        val allDevices = project.businessUnits.flatMap { it.devices }.associateBy { it.id }

        var rowIdx = 2
        for (cable in project.cables) {
            val portA = cable.portAId?.let { allPorts[it] }
            val devA = portA?.let { allDevices[it.deviceId] }

            if (devA != null && !filteredDeviceIds.contains(devA.id)) continue

            val portB = cable.portBId?.let { allPorts[it] }
            val devB = portB?.let { allDevices[it.deviceId] }

            val devBName = when {
                devB != null -> devB.technicalName
                cable.portBId != null -> i18n.text("text.c8bf0487beff", cable.portBId)
                else -> i18n.text("text.230b311f4c2d")
            }

            val portBName = portB?.name ?: (if (cable.portBId != null) i18n.text("text.24f5c3af5736") else i18n.text("text.7235a333bde8"))
            val lenStr = if (cable.lengthValue != null) "${cable.lengthValue} ${cable.lengthUnit ?: "m"}" else "-"

            sb.append("<row r=\"$rowIdx\">")
            sb.append(cellStr("A", rowIdx, cable.codeOrLabel ?: cable.id.take(8)))
            sb.append(cellStr("B", rowIdx, devA?.technicalName ?: "-"))
            sb.append(cellStr("C", rowIdx, portA?.name ?: "-"))
            sb.append(cellStr("D", rowIdx, cable.medium.toDisplayString(i18n)))
            sb.append(cellStr("E", rowIdx, lenStr))
            sb.append(cellStr("F", rowIdx, cable.color ?: "-"))
            sb.append(cellStr("G", rowIdx, devBName))
            sb.append(cellStr("H", rowIdx, portBName))
            sb.append(cellStr("I", rowIdx, (portA?.endpointStatus ?: com.onlyfield.assetmanager.core.model.EndpointStatus.UNKNOWN).toDisplayString(i18n)))
            sb.append(cellStr("J", rowIdx, cable.notes ?: "-"))
            sb.append("</row>\n")
            rowIdx++
        }

        val graph = com.onlyfield.assetmanager.core.model.ConnectionGraph(project)
        val portHeaders = listOf("device", "port", "side", "connector", "speed", "role", "module", "observation", "destination", "passage")
        rowIdx++
        sb.append("<row r=\"$rowIdx\">")
        portHeaders.forEachIndexed { n, key -> sb.append(cellStr(('A' + n).toString(), rowIdx, i18n.text("config.$key"))) }
        sb.append("</row>\n")
        allDevices.values.filter { it.id in filteredDeviceIds }.forEach { device -> device.ports.forEach { port ->
            rowIdx++
            val mapping = project.panelMappings.find { it.portAId == port.id || it.portBId == port.id }
            val cable = project.cables.find { it.portAId == port.id || it.portBId == port.id }
            fun endpoint(id: String?) = id?.let { allPorts[it]?.let { p -> "${allDevices[p.deviceId]?.technicalName} › ${p.name}" } }.orEmpty()
            val values = listOf(device.technicalName, port.name, port.hardware.side?.toDisplayString(i18n).orEmpty(), port.hardware.connector.orEmpty(), port.hardware.speed.orEmpty(),
                listOfNotNull(port.hardware.role, port.hardware.poeStandard?.name).joinToString(" / "), port.hardware.opticalModule.orEmpty(), i18n.text("config.${graph.state(port.id).name.lowercase()}"),
                endpoint(cable?.let { if (it.portAId == port.id) it.portBId else it.portAId }), endpoint(mapping?.let { if (it.portAId == port.id) it.portBId else it.portAId }))
            sb.append("<row r=\"$rowIdx\">")
            values.forEachIndexed { n, value -> sb.append(cellStr(('A' + n).toString(), rowIdx, value)) }
            sb.append("</row>\n")
        } }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun buildSheet3Xml(project: Project, i18n: Messages = Messages()): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf(i18n.text("text.a5216242ff51"), i18n.text("text.0624cb296793"), i18n.text("text.03cbc24f25f2"), i18n.text("text.dc22b79bc7d6"), i18n.text("text.af7ef649048f"), i18n.text("text.5145534513c2"), i18n.text("text.1314f26ce72a"))
        sb.append("<row r=\"1\">")
        headers.forEachIndexed { idx, h ->
            val colLetter = ('A' + idx).toString()
            sb.append(cellStr(colLetter, 1, h))
        }
        sb.append("</row>\n")

        var rowIdx = 2
        for (vlan in project.vlans) {
            val subnetsForVlan = project.subnets.filter { it.vlanId == vlan.id }
            val cidrs = subnetsForVlan.joinToString(", ") { it.cidrBlock }
            val gateways = subnetsForVlan.mapNotNull { it.gatewayIp }.joinToString(", ")

            sb.append("<row r=\"$rowIdx\">")
            sb.append(cellNum("A", rowIdx, vlan.vlanId))
            sb.append(cellStr("B", rowIdx, vlan.name))
            sb.append(cellStr("C", rowIdx, vlan.scopeType.toDisplayString(i18n)))
            sb.append(cellStr("D", rowIdx, cidrs.ifEmpty { "-" }))
            sb.append(cellStr("E", rowIdx, gateways.ifEmpty { "-" }))
            sb.append(cellNum("F", rowIdx, subnetsForVlan.size))
            sb.append(cellStr("G", rowIdx, vlan.description ?: "-"))
            sb.append("</row>\n")
            rowIdx++
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun buildSheet4Xml(project: Project, filteredDeviceIds: Set<String>, i18n: Messages = Messages()): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf(i18n.text("text.cf301d95d32c"), i18n.text("text.f57beb90828a"), i18n.text("text.649eace2ae87"), i18n.text("text.f9876f4c6cfa"), i18n.text("text.12873ee7733c"), i18n.text("text.64f63dbe7bbe"), i18n.text("text.af354b531994"))
        sb.append("<row r=\"1\">")
        headers.forEachIndexed { idx, h ->
            val colLetter = ('A' + idx).toString()
            sb.append(cellStr(colLetter, 1, h))
        }
        sb.append("</row>\n")

        val allDevices = project.businessUnits.flatMap { it.devices }.filter { filteredDeviceIds.contains(it.id) }

        var rowIdx = 2
        for (dev in allDevices) {
            val feeds = project.powerFeeds.filter { it.deviceId == dev.id }
            val feedA = feeds.find { it.feedName == "A" }?.let { "${it.feedType.toDisplayString(i18n)} (${it.sourceOutletDescription ?: "-"})" } ?: "-"
            val feedB = feeds.find { it.feedName == "B" }?.let { "${it.feedType.toDisplayString(i18n)} (${it.sourceOutletDescription ?: "-"})" } ?: "-"

            val va = feeds.mapNotNull { it.loadVa }.sum()
            val w = feeds.mapNotNull { it.loadWatts }.sum()

            val badges = project.documentBadges.filter { it.targetId == dev.id }.joinToString(", ") { it.label }

            val poe = project.poeMappings.filter { poe -> dev.ports.any { it.id == poe.portId } }
                .joinToString("; ") { "${it.role.toDisplayString(i18n)} ${it.standard.toDisplayString(i18n)}" }

            sb.append("<row r=\"$rowIdx\">")
            sb.append(cellStr("A", rowIdx, dev.technicalName))
            sb.append(cellStr("B", rowIdx, if (va > 0) i18n.text("text.1df071e9be6a", va) else "-"))
            sb.append(cellStr("C", rowIdx, if (w > 0) i18n.text("text.cd49315c743a", w) else "-"))
            sb.append(cellStr("D", rowIdx, feedA))
            sb.append(cellStr("E", rowIdx, feedB))
            sb.append(cellStr("F", rowIdx, poe.ifEmpty { "-" }))
            sb.append(cellStr("G", rowIdx, badges.ifEmpty { "-" }))
            sb.append("</row>\n")
            rowIdx++
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun buildSheet5Xml(project: Project, filterConfig: ExportFilterConfig, filteredDeviceIds: Set<String>, i18n: Messages = Messages()): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf(i18n.text("text.486d4aed6f59"), i18n.text("text.83d1dffc1570"), i18n.text("text.57fbd1029ff6"), i18n.text("text.3b495129c5de"), i18n.text("text.b332a1d182ce"))
        sb.append("<row r=\"1\">")
        headers.forEachIndexed { idx, h ->
            val colLetter = ('A' + idx).toString()
            sb.append(cellStr(colLetter, 1, h))
        }
        sb.append("</row>\n")

        var rowIdx = 2

        for (att in project.attachments) {
            if (att.classification == AttachmentClassification.CONFIDENTIAL && !filterConfig.includeConfidential) {
                continue
            }
            val detail = if (!att.attributionText.isNullOrBlank()) i18n.text("text.0e199e27e9e9", att.originalFileName, att.attributionText) else att.originalFileName
            sb.append("<row r=\"$rowIdx\">")
            sb.append(cellStr("A", rowIdx, i18n.text("text.59cc6c3e1526")))
            sb.append(cellStr("B", rowIdx, att.name))
            sb.append(cellStr("C", rowIdx, att.classification.toDisplayString(i18n)))
            sb.append(cellStr("D", rowIdx, "-"))
            sb.append(cellStr("E", rowIdx, detail))
            sb.append("</row>\n")
            rowIdx++
        }

        for (dev in project.businessUnits.flatMap { it.devices }.filter { filteredDeviceIds.contains(it.id) }) {
            val obs = dev.observation
            if (obs != null) {
                sb.append("<row r=\"$rowIdx\">")
                sb.append(cellStr("A", rowIdx, i18n.text("text.cf301d95d32c")))
                sb.append(cellStr("B", rowIdx, dev.technicalName))
                sb.append(cellStr("C", rowIdx, "SHAREABLE"))
                sb.append(cellStr("D", rowIdx, obs.status.toDisplayString(i18n)))
                sb.append(cellStr("E", rowIdx, obs.notes ?: "-"))
                sb.append("</row>\n")
                rowIdx++
            }
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }
}
