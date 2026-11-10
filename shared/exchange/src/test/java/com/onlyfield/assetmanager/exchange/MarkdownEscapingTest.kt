package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

class MarkdownEscapingTest {
    private val payload = "user|\\`ticks``\r\n##INJECTED\n<em>html</em>&amp;[link](x)*_~"
    private fun fixture(value: String): Project {
        val rack = Rack(name = value)
        val device = Device(technicalName = value, ipAddress = value, rackId = rack.id, positionU = 1,
            hardware = HardwareSpec(features = listOf(value)), observation = Observation("Test", 1, notes = value))
        val port = Port(deviceId = device.id, name = value, hardware = PortHardware(connector = value, speed = value, opticalModule = value))
        val withPort = device.copy(ports = listOf(port))
        val vlan = Vlan(vlanId = 100, name = value)
        return Project(name = value, description = value, createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(Site(name = value, devices = listOf(withPort))), racks = listOf(rack),
            cables = listOf(Cable(portAId = port.id, codeOrLabel = value, color = value)), vlans = listOf(vlan),
            subnets = listOf(Subnet(cidrBlock = value, gatewayIp = value, vlanId = vlan.id)),
            powerFeeds = listOf(PowerFeed(deviceId = device.id, feedName = value, sourceOutletDescription = value, observedSource = value, notes = value)),
            documentBadges = listOf(DocumentBadge(targetType = "DEVICE", targetId = device.id, label = value)),
            attachments = listOf(Attachment(name = value, originalFileName = value, relativePath = "attachments/test.pdf", attributionText = value)))
    }

    @Test fun userContentCannotIntroduceMarkdownRowsHeadingsOrMarkupInThreeLanguages() {
        val project = fixture(payload)
        val before = project.copy()
        for (language in listOf("it", "en", "es")) {
            val i18n = Messages(Locale.forLanguageTag(language))
            for ((kind, data) in listOf("control" to fixture("user"), "escaped" to project)) {
                val output = ByteArrayOutputStream()
                MarkdownExportManager.exportMarkdownToStream(data, ExportFilterConfig(titleOverride = data.name, authorName = data.name), output, i18n)
                val text = output.toString("UTF-8")
                File("build/reports/markdown-$kind-$language.md").apply { parentFile.mkdirs(); writeText(text) }
                if (kind == "escaped") {
                    assertFalse(text.contains("\r")); assertFalse(text.contains("\n##INJECTED"))
                    assertTrue(text.contains("\\<em\\>html\\</em\\>")); assertFalse(text.contains("**$payload**"))
                    assertTrue(text.contains("user\\|"))
                    val inventory = text.lines().single { it.startsWith("| **user") && it.contains("U1") }
                    assertTrue(inventory.contains("\\|"))
                }
            }
        }
        assertEquals(before, project)
    }

    @Test fun codeSpanBoundariesProduceRendererFixturesWithoutChangingStoredValues() {
        val cases = listOf("|" to "|", "\\|" to "\\|", "|\\" to "|\\", "`" to "`", "``" to "``",
            " `edge` " to " `edge` ", "\r\n|\n" to " | ")
        cases.forEachIndexed { number, (value, expected) ->
            val original = fixture("safe")
            val project = original.copy(sites = original.sites.map { site -> site.copy(devices = site.devices.map { it.copy(ipAddress = value) }) },
                attachments = original.attachments.map { it.copy(originalFileName = value) })
            val output = ByteArrayOutputStream()
            MarkdownExportManager.exportMarkdownToStream(project, ExportFilterConfig(), output)
            File("build/reports/markdown-code-$number.md").writeText(output.toString("UTF-8"))
            File("build/reports/markdown-code-$number.txt").writeText(expected)
            assertEquals(value, project.sites.single().devices.single().ipAddress)
            assertEquals(value, project.attachments.single().originalFileName)
        }
    }
}
