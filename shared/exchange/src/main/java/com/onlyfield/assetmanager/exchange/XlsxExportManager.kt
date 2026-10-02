package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.Project
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Genera un foglio di calcolo Excel (.xlsx) nativo basato sulla struttura OpenXML.
 * Garantisce zero dipendenze esterne ed esclude esplicitamente i campi segreti.
 * Tutte le celle di testo libero vengono formattate come stringhe esplicite (inlineStr),
 * evitando l'interpretazione indotta di formule (es. =SUM, =CMD).
 */
object XlsxExportManager {

    fun exportXlsxToStream(
        project: Project,
        filterConfig: ExportFilterConfig,
        outputStream: OutputStream
    ) {
        val filteredDevices = project.businessUnits
            .filter { filterConfig.selectedBusinessUnitId == null || it.id == filterConfig.selectedBusinessUnitId }
            .flatMap { bu ->
                bu.sites.filter { filterConfig.selectedSiteId == null || it.id == filterConfig.selectedSiteId }
                    .flatMap { site ->
                        site.areas.filter { filterConfig.selectedAreaId == null || it.id == filterConfig.selectedAreaId }
                            .flatMap { area -> bu.devices.filter { it.areaId == area.id } }
                    } + bu.devices.filter { it.siteId == null && it.areaId == null }
            }
            .filter { filterConfig.selectedCategory == null || it.category == filterConfig.selectedCategory }
            .distinctBy { it.id }

        val filteredDeviceIds = filteredDevices.map { it.id }.toSet()

        ZipOutputStream(outputStream).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(buildContentTypesXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(buildRelsXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 3. xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(buildWorkbookXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 4. xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(buildWorkbookRelsXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 5. xl/styles.xml
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(buildStylesXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 6. xl/worksheets/sheet1.xml (Inventario Apparati)
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(buildSheet1Xml(project, filteredDevices).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 7. xl/worksheets/sheet2.xml (Porte e Cablaggio)
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
            zip.write(buildSheet2Xml(project, filteredDeviceIds).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 8. xl/worksheets/sheet3.xml (Rete Logica e VLAN)
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet3.xml"))
            zip.write(buildSheet3Xml(project).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 9. xl/worksheets/sheet4.xml (Alimentazione e Badge)
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet4.xml"))
            zip.write(buildSheet4Xml(project, filteredDeviceIds).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 10. xl/worksheets/sheet5.xml (Note e Osservazioni)
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet5.xml"))
            zip.write(buildSheet5Xml(project, filterConfig, filteredDeviceIds).toByteArray(Charsets.UTF_8))
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

    private fun buildWorkbookXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Inventario Apparati" sheetId="1" r:id="rId1"/>
    <sheet name="Porte e Cablaggio" sheetId="2" r:id="rId2"/>
    <sheet name="Rete Logica e VLAN" sheetId="3" r:id="rId3"/>
    <sheet name="Alimentazione e Badge" sheetId="4" r:id="rId4"/>
    <sheet name="Note e Osservazioni" sheetId="5" r:id="rId5"/>
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

    private fun buildSheet1Xml(project: Project, devices: List<com.onlyfield.assetmanager.core.model.Device>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf("BU", "Sede", "Area", "Nome Tecnico", "Etichetta Fisica", "Alias", "Indirizzo IP", "Indirizzo MAC", "Categoria", "Rack/Posizione", "Modello", "Porte", "Stato Osservazione", "Note")
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
            val rackName = project.racks.find { it.id == dev.rackId }?.name ?: "Fuori Rack"
            val rackPos = if (dev.rackId != null) "$rackName (U${dev.positionU ?: "-"} ${dev.rackSide.name})" else "Fuori Rack"
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
            sb.append(cellStr("I", rowIdx, dev.category.name))
            sb.append(cellStr("J", rowIdx, rackPos))
            sb.append(cellStr("K", rowIdx, modelName))
            sb.append(cellNum("L", rowIdx, dev.ports.size))
            sb.append(cellStr("M", rowIdx, dev.observation?.status?.name ?: "VERIFIED"))
            sb.append(cellStr("N", rowIdx, dev.observation?.notes ?: "-"))
            sb.append("</row>\n")
            rowIdx++
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun buildSheet2Xml(project: Project, filteredDeviceIds: Set<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf("Cavo ID", "Apparato A", "Porta A", "Mezzo", "Lunghezza", "Orientamento", "Apparato B / Destinazione", "Porta B", "Stato Endpoint", "Note Cablaggio")
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
                cable.portBId != null -> "Fuori Ambito (Porta ${cable.portBId})"
                else -> "Ignoto / Scollegato"
            }

            val portBName = portB?.name ?: (if (cable.portBId != null) "Fuori Ambito" else "Scollegata")
            val lenStr = if (cable.lengthValue != null) "${cable.lengthValue} ${cable.lengthUnit ?: "m"}" else "-"

            sb.append("<row r=\"$rowIdx\">")
            sb.append(cellStr("A", rowIdx, cable.codeOrLabel ?: cable.id.take(8)))
            sb.append(cellStr("B", rowIdx, devA?.technicalName ?: "-"))
            sb.append(cellStr("C", rowIdx, portA?.name ?: "-"))
            sb.append(cellStr("D", rowIdx, cable.medium.name))
            sb.append(cellStr("E", rowIdx, lenStr))
            sb.append(cellStr("F", rowIdx, cable.orientation.name))
            sb.append(cellStr("G", rowIdx, devBName))
            sb.append(cellStr("H", rowIdx, portBName))
            sb.append(cellStr("I", rowIdx, portA?.endpointStatus?.name ?: "UNKNOWN"))
            sb.append(cellStr("J", rowIdx, cable.notes ?: "-"))
            sb.append("</row>\n")
            rowIdx++
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun buildSheet3Xml(project: Project): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf("ID VLAN", "Nome VLAN", "Ambito", "Subnet CIDR", "Gateway L3", "Subnet Associated", "Note Rete Logica")
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
            sb.append(cellStr("C", rowIdx, vlan.scopeType.name))
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

    private fun buildSheet4Xml(project: Project, filteredDeviceIds: Set<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf("Apparato", "Assorbimento (VA)", "Assorbimento (W)", "Feed A", "Feed B", "PoE", "Badge Documentali")
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
            val feedA = feeds.find { it.feedName == "A" }?.let { "${it.feedType.name} (${it.sourceOutletDescription ?: "-"})" } ?: "-"
            val feedB = feeds.find { it.feedName == "B" }?.let { "${it.feedType.name} (${it.sourceOutletDescription ?: "-"})" } ?: "-"

            val va = feeds.mapNotNull { it.loadVa }.sum()
            val w = feeds.mapNotNull { it.loadWatts }.sum()

            val badges = project.documentBadges.filter { it.targetId == dev.id }.joinToString(", ") { it.label }

            val poe = project.poeMappings.filter { poe -> dev.ports.any { it.id == poe.portId } }
                .joinToString("; ") { "${it.role.name} ${it.standard.name}" }

            sb.append("<row r=\"$rowIdx\">")
            sb.append(cellStr("A", rowIdx, dev.technicalName))
            sb.append(cellStr("B", rowIdx, if (va > 0) "$va VA" else "-"))
            sb.append(cellStr("C", rowIdx, if (w > 0) "$w W" else "-"))
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

    private fun buildSheet5Xml(project: Project, filterConfig: ExportFilterConfig, filteredDeviceIds: Set<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")

        val headers = listOf("Tipo Oggetto", "Nome Oggetto", "Classificazione", "Stato Osservazione", "Note / Dettaglio")
        sb.append("<row r=\"1\">")
        headers.forEachIndexed { idx, h ->
            val colLetter = ('A' + idx).toString()
            sb.append(cellStr(colLetter, 1, h))
        }
        sb.append("</row>\n")

        var rowIdx = 2

        // Allegati
        for (att in project.attachments) {
            if (att.classification == AttachmentClassification.CONFIDENTIAL && !filterConfig.includeConfidential) {
                continue
            }
            val detail = if (!att.attributionText.isNullOrBlank()) "${att.originalFileName} | Attribuzione: ${att.attributionText}" else att.originalFileName
            sb.append("<row r=\"$rowIdx\">")
            sb.append(cellStr("A", rowIdx, "Allegato"))
            sb.append(cellStr("B", rowIdx, att.name))
            sb.append(cellStr("C", rowIdx, att.classification.name))
            sb.append(cellStr("D", rowIdx, "-"))
            sb.append(cellStr("E", rowIdx, detail))
            sb.append("</row>\n")
            rowIdx++
        }

        // Apparati con note di osservazione
        for (dev in project.businessUnits.flatMap { it.devices }.filter { filteredDeviceIds.contains(it.id) }) {
            val obs = dev.observation
            if (obs != null) {
                sb.append("<row r=\"$rowIdx\">")
                sb.append(cellStr("A", rowIdx, "Apparato"))
                sb.append(cellStr("B", rowIdx, dev.technicalName))
                sb.append(cellStr("C", rowIdx, "SHAREABLE"))
                sb.append(cellStr("D", rowIdx, obs.status.name))
                sb.append(cellStr("E", rowIdx, obs.notes ?: "-"))
                sb.append("</row>\n")
                rowIdx++
            }
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }
}
