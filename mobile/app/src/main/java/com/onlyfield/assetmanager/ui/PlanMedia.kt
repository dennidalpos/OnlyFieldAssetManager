package com.onlyfield.assetmanager.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.IOException
import kotlin.math.max

object PlanMedia {
    fun pageCount(file: File): Int = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { it.pageCount } }
    fun image(file: File, pdf: Boolean, page: Int, maxSide: Int = 2048): ImageBitmap {
        if (pdf) return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                renderer.openPage(page).use { p ->
                    val scale = maxSide.toFloat() / max(p.width, p.height)
                    val bitmap = Bitmap.createBitmap(max(1, (p.width * scale).toInt()), max(1, (p.height * scale).toInt()), Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap.asImageBitmap()
                }
            }
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) throw IOException("Immagine non leggibile")
        var sample = 1
        while (max(options.outWidth, options.outHeight) / sample > maxSide) sample *= 2
        return (BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: throw IOException("Immagine non leggibile")).asImageBitmap()
    }
}
