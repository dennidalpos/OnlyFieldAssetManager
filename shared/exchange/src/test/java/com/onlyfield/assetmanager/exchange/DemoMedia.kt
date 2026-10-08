package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageOutputStream

/** Small synthetic PNGs; no external downloads or photographs. */
object DemoMedia {
    fun payloads(project: Project): Map<String, ByteArray> = project.attachments.associate { attachment ->
        val image = BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = Color(246, 248, 252)
            g.fillRect(0, 0, image.width, image.height)
            g.color = Color(28, 49, 74)
            g.font = Font(Font.SANS_SERIF, Font.BOLD, 30)
            g.drawString(attachment.name, 32, 45)
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 20)
            g.drawString("DEMO SINTETICA · ${attachment.classification}", 32, 78)
            g.stroke = BasicStroke(3f)
            if (attachment.targetType == AttachmentTargetType.AREA) {
                // Ratios match map placements; room geometry is illustrative only.
                g.color = Color(183, 197, 212)
                g.drawRect(12, 90, 1176, 654)
                for (x in listOf(180, 480, 780)) {
                    g.drawRect(x, 90, 290, 180)
                    g.drawRect(x, 590, 290, 154)
                }
                g.color = Color(225, 233, 242)
                g.fillRect(182, 300, 987, 260)
                g.color = Color(65, 87, 109)
                g.drawString("Corridoio / area tecnica", 490, 440)
                g.drawString("Geometria dimostrativa: zoom, pan, annotazioni e oggetti", 32, 785)
            } else {
                g.color = Color(33, 117, 146)
                g.fillRoundRect(80, 170, 1040, 360, 24, 24)
                g.color = Color.WHITE
                g.font = Font(Font.SANS_SERIF, Font.BOLD, 28)
                g.drawString("${attachment.targetType}: ${targetName(project, attachment)}", 110, 220)
                for (i in 1..8) {
                    val x = 112 + (i - 1) * 120
                    g.drawRect(x, 295, 80, 70)
                    g.drawString(i.toString(), x + 28, 412)
                }
                g.color = Color(28, 49, 74)
                g.font = Font(Font.SANS_SERIF, Font.PLAIN, 22)
                g.drawString("Illustrazione per allegati, anteprima e filtri documentali.", 80, 610)
                g.drawString("Nessuna fotografia o credenziale reale.", 80, 650)
            }
        } finally { g.dispose() }
        val bytes = ByteArrayOutputStream().also { out ->
            MemoryCacheImageOutputStream(out).use { check(ImageIO.write(image, "png", it)) { "PNG writer unavailable" } }
        }.toByteArray()
        attachment.relativePath to bytes
    }

    private fun targetName(project: Project, attachment: Attachment): String = when (attachment.targetType) {
        AttachmentTargetType.PROJECT -> project.name
        AttachmentTargetType.RACK -> project.racks.single { it.id == attachment.targetId }.name
        AttachmentTargetType.DEVICE -> project.sites.flatMap { it.devices }.single { it.id == attachment.targetId }.technicalName
        AttachmentTargetType.PORT -> project.sites.flatMap { it.devices }.flatMap { it.ports }.single { it.id == attachment.targetId }.name
        AttachmentTargetType.CABLE -> project.cables.single { it.id == attachment.targetId }.codeOrLabel.orEmpty()
        else -> error("Unsupported demo target")
    }
}
