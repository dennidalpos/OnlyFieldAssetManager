package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.i18n.Messages

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.max

object PlanMedia {
    fun pageCount(file: File, i18n: Messages = Messages()): Int = Loader.loadPDF(file).use { document ->
        if (document.isEncrypted) throw IOException(i18n.text("text.7c399be6bf11"))
        document.numberOfPages
    }
    fun validateImage(file: File, i18n: Messages = Messages()) {
        org.jetbrains.skia.Data.makeFromBytes(file.readBytes()).use { data ->
            org.jetbrains.skia.Codec.makeFromData(data).use { codec ->
                require(codec.imageInfo.width > 0 && codec.imageInfo.height > 0) { i18n.text("text.dffbcd08384a") }
            }
        }
    }
    fun image(file: File, pdf: Boolean, page: Int, maxSide: Int = 2048, i18n: Messages = Messages()): ImageBitmap {
        if (pdf) return Loader.loadPDF(file).use { document ->
            if (document.isEncrypted) throw IOException(i18n.text("text.7c399be6bf11"))
            require(page in 0 until document.numberOfPages) { i18n.text("text.e24ef060c152") }
            val box = document.getPage(page).cropBox
            PDFRenderer(document).renderImage(page, maxSide.toFloat() / max(box.width, box.height)).toComposeImageBitmap()
        }
        val image = ImageIO.createImageInputStream(file)?.use { input ->
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) null else {
                val reader = readers.next()
                try {
                    reader.input = input
                    val sample = (max(reader.getWidth(0), reader.getHeight(0)) / maxSide).coerceAtLeast(1)
                    reader.read(0, reader.defaultReadParam.apply { setSourceSubsampling(sample, sample, 0, 0) })
                } finally { reader.dispose() }
            }
        } ?: return org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
        val ratio = (maxSide.toDouble() / max(image.width, image.height)).coerceAtMost(1.0)
        if (ratio == 1.0) return image.toComposeImageBitmap()
        val scaled = java.awt.image.BufferedImage(max(1, (image.width * ratio).toInt()), max(1, (image.height * ratio).toInt()), java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val graphics = scaled.createGraphics()
        try { graphics.drawImage(image, 0, 0, scaled.width, scaled.height, null) } finally { graphics.dispose() }
        return scaled.toComposeImageBitmap()
    }
}
