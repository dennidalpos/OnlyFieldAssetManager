package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Cable
import com.onlyfield.assetmanager.core.model.CableMedium
import com.onlyfield.assetmanager.core.model.Credential
import com.onlyfield.assetmanager.core.model.CredentialType
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.DeviceCategory
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Subnet
import com.onlyfield.assetmanager.core.model.Vlan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class DocumentExportTest {

    private fun createSampleProject(): Project {
        val dev1 = Device(
            id = "dev-1",
            technicalName = "SW-CORE-01",
            ipAddress = "192.168.1.1",
            category = DeviceCategory.NETWORK_SWITCH,
            ports = listOf(Port(id = "port-1", deviceId = "dev-1", name = "Ge1/0/1"))
        )

        val dev2 = Device(
            id = "dev-2",
            technicalName = "RTR-EDGE-01",
            ipAddress = "10.0.0.1",
            category = DeviceCategory.CUSTOM,
            ports = listOf(Port(id = "port-2", deviceId = "dev-2", name = "Eth0"))
        )

        val bu = BusinessUnit(
            id = "bu-1",
            name = "Sede Centrale",
            devices = listOf(dev1, dev2)
        )

        val cable = Cable(
            id = "cable-1",
            codeOrLabel = "C-001",
            portAId = "port-1",
            portBId = "port-2",
            medium = CableMedium.ETHERNET_COPPER,
            lengthValue = 15.0,
            lengthUnit = "m"
        )

        val vlan = Vlan(id = "vlan-1", vlanId = 100, name = "MANAGEMENT")
        val subnet = Subnet(id = "sub-1", cidrBlock = "192.168.1.0/24", vlanId = "vlan-1", gatewayIp = "192.168.1.254")

        val cred = Credential(
            id = "cred-1",
            username = "admin_secret",
            secret = "SUPER_SECRET_PASSWORD_123",
            type = CredentialType.PASSWORD
        )

        val publicAtt = Attachment(
            id = "att-1",
            name = "Schema_Pubblico.pdf",
            originalFileName = "Schema_Pubblico.pdf",
            relativePath = "attachments/att-1.pdf",
            classification = AttachmentClassification.SHAREABLE
        )

        val secretAtt = Attachment(
            id = "att-2",
            name = "Password_Backup.txt",
            originalFileName = "Password_Backup.txt",
            relativePath = "attachments/att-2.txt",
            classification = AttachmentClassification.CONFIDENTIAL
        )

        return Project(
            id = "proj-1",
            name = "Progetto Reti Srl",
            createdEpochMs = System.currentTimeMillis(),
            updatedEpochMs = System.currentTimeMillis(),
            businessUnits = listOf(bu),
            credentials = listOf(cred),
            cables = listOf(cable),
            vlans = listOf(vlan),
            subnets = listOf(subnet),
            attachments = listOf(publicAtt, secretAtt)
        )
    }

    @Test
    fun testXlsxExport_structureAndSecurity() {
        val project = createSampleProject()
        val filter = ExportFilterConfig(includeConfidential = false, authorName = "Mario Rossi")

        val baos = ByteArrayOutputStream()
        XlsxExportManager.exportXlsxToStream(project, filter, baos)

        val zipBytes = baos.toByteArray()
        assertTrue("XLSX zip output must not be empty", zipBytes.isNotEmpty())

        val entryNames = mutableSetOf<String>()
        var sheet1Content = ""
        var sheet5Content = ""

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                val bytes = zis.readBytes()
                if (entry.name == "xl/worksheets/sheet1.xml") {
                    sheet1Content = String(bytes, Charsets.UTF_8)
                }
                if (entry.name == "xl/worksheets/sheet5.xml") {
                    sheet5Content = String(bytes, Charsets.UTF_8)
                }
                entry = zis.nextEntry
            }
        }

        assertTrue("XLSX must contain [Content_Types].xml", entryNames.contains("[Content_Types].xml"))
        assertTrue("XLSX must contain xl/workbook.xml", entryNames.contains("xl/workbook.xml"))
        assertTrue("XLSX must contain xl/worksheets/sheet1.xml", entryNames.contains("xl/worksheets/sheet1.xml"))

        // Verify Device SW-CORE-01 is present
        assertTrue("Sheet 1 must contain SW-CORE-01", sheet1Content.contains("SW-CORE-01"))
        // Verify inlineStr formatting
        assertTrue("Sheet 1 must use inlineStr cells", sheet1Content.contains("t=\"inlineStr\""))

        // Verify confidential attachment is excluded when includeConfidential = false
        assertFalse("Confidential attachment must be excluded", sheet5Content.contains("Password_Backup.txt"))

        // Verify zero credential secret leakage
        val fullZipString = String(zipBytes, Charsets.ISO_8859_1)
        assertFalse("XLSX must NOT contain credential password secret", fullZipString.contains("SUPER_SECRET_PASSWORD_123"))
        assertFalse("XLSX must NOT contain credential username", fullZipString.contains("admin_secret"))
    }

    @Test
    fun testMarkdownExport_structureAndSecurity() {
        val project = createSampleProject()
        val filter = ExportFilterConfig(includeConfidential = false, authorName = "Mario Rossi")

        val baos = ByteArrayOutputStream()
        MarkdownExportManager.exportMarkdownToStream(project, filter, baos)

        val mdText = baos.toString("UTF-8")
        assertTrue("Markdown text must not be empty", mdText.isNotEmpty())

        assertTrue("Markdown must contain title", mdText.contains("Documentazione Tecnica — Progetto Reti Srl"))
        assertTrue("Markdown must contain author", mdText.contains("Mario Rossi"))
        assertTrue("Markdown must contain device SW-CORE-01", mdText.contains("SW-CORE-01"))
        assertTrue("Markdown must contain cable C-001", mdText.contains("C-001"))
        assertTrue("Markdown must contain VLAN MANAGEMENT", mdText.contains("MANAGEMENT"))

        // Confidential attachment excluded
        assertFalse("Confidential attachment must be excluded", mdText.contains("Password_Backup.txt"))
        assertTrue("Public attachment must be included", mdText.contains("Schema_Pubblico.pdf"))

        // Zero secret leakage
        assertFalse("Markdown must NOT contain credential password secret", mdText.contains("SUPER_SECRET_PASSWORD_123"))
        assertFalse("Markdown must NOT contain credential username", mdText.contains("admin_secret"))
    }
}
