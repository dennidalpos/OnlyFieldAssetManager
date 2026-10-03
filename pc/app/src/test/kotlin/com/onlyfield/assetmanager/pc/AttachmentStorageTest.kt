package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AttachmentStorageTest {

    @Test
    fun attachmentFilesSurviveExportAndImportOnAnotherPc() {
        val pcA = DesktopStorageManager(Files.createTempDirectory("pcA").toFile())
        val pcB = DesktopStorageManager(Files.createTempDirectory("pcB").toFile())
        val source = File.createTempFile("planimetria", ".png").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }

        val att = Attachment(name = "Planimetria", originalFileName = source.name, relativePath = "")
        val project = Project(name = "Sede", createdEpochMs = 0, updatedEpochMs = 0,
            attachments = listOf(att.copy(relativePath = AttachmentFiles.entryName(att))))
        pcA.storeAttachmentFile(project.id, att, source)
        assertTrue(pcA.missingAttachments(project).isEmpty())

        val pkgFile = File(pcA.dataDir, "export.ofam")
        pcA.exportPackageToFile(project, pkgFile, password = "pw-123")

        val pkg = pcB.importPackageFromFile(pkgFile, "pw-123").pkg!!
        assertEquals(1, pcB.extractAttachments(pkg))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), pcB.attachmentFile(project.id, att).readBytes())

        source.delete(); pcA.dataDir.deleteRecursively(); pcB.dataDir.deleteRecursively()
    }
}
