package com.onlyfield.assetmanager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.onlyfield.assetmanager.cartography.*
import com.onlyfield.assetmanager.core.i18n.Messages
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class CartographyGridTest {
    private fun png(width: Int, height: Int): ByteArray {
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return try { ByteArrayOutputStream().use { output -> check(image.compress(Bitmap.CompressFormat.PNG, 100, output)); output.toByteArray() } }
        finally { image.recycle() }
    }

    @Test fun borderTilesStayInRangeAndSnapshotRetainsAttribution() {
        val tile = png(256, 256)
        for (zoom in listOf(1, 17)) for (latitude in listOf(-85.0, 85.0)) for (longitude in listOf(-180.0, 180.0)) {
            val urls = mutableListOf<String>()
            val request = MapSnapshotRequest(centerLatitude = latitude, centerLongitude = longitude, zoomLevel = zoom, customAttribution = "Test attribution")
            val result = CartographicMapManager.acquireMapSnapshot(request, Messages()) { url -> urls += url; tile }
            assertEquals(9, urls.size)
            val n = 1 shl zoom
            urls.forEach { url ->
                val segments = java.net.URI(url).path.split('/')
                assertTrue(segments[2].toInt() in 0 until n)
                assertTrue(segments[3].removeSuffix(".png").toInt() in 0 until n)
            }
            assertEquals("Test attribution", result.attributionText)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(result.imageBytes, 0, result.imageBytes.size, bounds)
            assertEquals(768, bounds.outWidth); assertEquals(768, bounds.outHeight)
        }
    }

    @Test fun invalidImagesAndPartialFailuresAbortBeforeReturningASnapshot() {
        val good = png(256, 256)
        for (bytes in listOf(png(512, 256), ByteArray(8), ByteArray(2 * 1024 * 1024 + 1))) {
            var calls = 0
            try {
                CartographicMapManager.acquireMapSnapshot(MapSnapshotRequest(), Messages()) { calls++; bytes }
                fail("Invalid tile accepted")
            } catch (_: OfflineMapException) { assertEquals(1, calls) }
        }
        for (language in listOf("it", "en", "es")) {
            var calls = 0
            val i18n = Messages(Locale.forLanguageTag(language))
            try {
                CartographicMapManager.acquireMapSnapshot(MapSnapshotRequest(), i18n) {
                    calls++
                    if (calls == 5) throw OfflineMapException(i18n.text("map.network.android"))
                    good
                }
                fail("Partial grid returned")
            } catch (e: OfflineMapException) { assertEquals(5, calls); assertEquals(i18n.text("map.network.android"), e.message) }
        }
    }
}
