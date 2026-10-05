package com.onlyfield.assetmanager.exchange

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/** Shared import policy; encrypted entries also carry a GCM IV and tag. */
internal data class PackageImportLimits(
    val archiveBytes: Long = 256L * 1024 * 1024,
    val fileBytes: Long = 32L * 1024 * 1024,
    val expandedBytes: Long = 512L * 1024 * 1024,
    val entries: Int = 10_000,
) {
    companion object { const val MAX_KDF_ITERATIONS = 1_000_000; const val GCM_OVERHEAD = 28 }
}

internal class PackageLimitExceeded(val code: String) : IOException(code)

internal class PackageInput(input: InputStream, private val limit: Long) : FilterInputStream(input) {
    var bytesRead = 0L
        private set

    private fun count(bytes: Int) {
        if (bytes > 0) bytesRead += bytes
        if (bytesRead > limit) throw PackageLimitExceeded("PACKAGE_SIZE_LIMIT_EXCEEDED")
    }

    override fun read(): Int = `in`.read().also { if (it >= 0) count(1) }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        `in`.read(buffer, offset, minOf(length.toLong(), (limit - bytesRead + 1).coerceAtLeast(1)).toInt()).also(::count)
}
