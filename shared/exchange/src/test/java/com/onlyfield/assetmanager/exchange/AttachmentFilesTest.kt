package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream

class AttachmentFilesTest {

    private val photo = Attachment(name = "Foto rack", originalFileName = "rack A (fronte).jpg", relativePath = "")
    private val project = Project(name = "P", createdEpochMs = 0, updatedEpochMs = 0, attachments = listOf(photo))
    private val content = "JPEG-BYTES-0123456789".toByteArray()

    @Test
    fun attachmentsTravelInsideThePackage() {
        val src = Files.createTempDirectory("att_src").toFile()
        AttachmentFiles.localFile(src, project.id, photo).apply { parentFile.mkdirs(); writeBytes(content) }

        val zip = PackageSerializer.exportPackage(project, AttachmentFiles.collect(project) { AttachmentFiles.localFile(src, project.id, it) })
        val pkg = PackageSerializer.importPackage(zip).pkg!!

        val dst = Files.createTempDirectory("att_dst").toFile()
        assertEquals(1, AttachmentFiles.extract(pkg, dst))
        assertArrayEquals(content, AttachmentFiles.localFile(dst, project.id, photo).readBytes())
        assertTrue(AttachmentFiles.missing(project) { AttachmentFiles.localFile(dst, project.id, it) }.isEmpty())
        src.deleteRecursively(); dst.deleteRecursively()
    }

    @Test
    fun attachmentsOfProtectedProjectsAreEncrypted() {
        val attachments = mapOf(AttachmentFiles.entryName(photo) to content)
        val zip = PackageSerializer.exportPackage(project, attachments, password = "Segreta!1")

        // The raw entry inside the ZIP must not contain the plaintext.
        val raw = ZipInputStream(ByteArrayInputStream(zip)).use { zis ->
            generateSequence { zis.nextEntry }.first { it.name == AttachmentFiles.entryName(photo) }
            zis.readBytes()
        }
        assertFalse(String(raw, Charsets.ISO_8859_1).contains("JPEG-BYTES"))

        val pkg = PackageSerializer.importPackage(zip, password = "Segreta!1").pkg!!
        assertTrue(pkg.manifest.attachmentsEncrypted)
        assertArrayEquals(content, AttachmentFiles.bytesIn(pkg, photo))
        assertNull(PackageSerializer.importPackage(zip, password = "sbagliata").pkg)
    }
}
