package com.onlyfield.assetmanager.exchange

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small payloads stay in RAM; larger ones use authenticated, encrypted scratch files. */
class PackagePayloads(private val parent: File? = null) : AbstractMap<String, ByteArray>(), AutoCloseable {
    private data class Payload(val size: Long, val checksum: String, val bytes: ByteArray?, val file: File?, val iv: ByteArray?)
    private val payloads = linkedMapOf<String, Payload>()
    private var memoryBytes = 0L
    private var directory: File? = null
    private var key: SecretKey? = null
    private var closed = false

    override val size: Int get() = payloads.size
    override val keys: Set<String> get() = payloads.keys
    override fun containsKey(key: String): Boolean = payloads.containsKey(key)
    override fun get(key: String): ByteArray? {
        check(!closed) { "Package payloads are closed" }
        val payload = payloads[key] ?: return null
        return payload.bytes ?: ByteArrayOutputStream(payload.size.toInt()).also { writeTo(key, it) }.toByteArray()
    }
    override val entries: Set<Map.Entry<String, ByteArray>> get() = object : AbstractSet<Map.Entry<String, ByteArray>>() {
        override val size: Int get() = payloads.size
        override fun iterator(): Iterator<Map.Entry<String, ByteArray>> {
            val names = payloads.keys.iterator()
            return object : Iterator<Map.Entry<String, ByteArray>> {
                override fun hasNext() = names.hasNext()
                override fun next(): Map.Entry<String, ByteArray> = names.next().let { java.util.AbstractMap.SimpleImmutableEntry(it, getValue(it)) }
            }
        }
    }

    fun byteSize(name: String): Long = payloads.getValue(name).size
    fun checksum(name: String): String = payloads.getValue(name).checksum

    fun putBytes(name: String, bytes: ByteArray) = write(name) { it.write(bytes) }

    fun copyFrom(source: Map<String, ByteArray>, include: (String) -> Boolean = { true }) {
        for (name in source.keys) {
            if (!include(name)) continue
            if (source is PackagePayloads) write(name) { source.writeTo(name, it) }
            else putBytes(name, source.getValue(name))
        }
    }

    fun writeTo(name: String, output: OutputStream) {
        check(!closed) { "Package payloads are closed" }
        val payload = payloads.getValue(name)
        val bytes = payload.bytes
        if (bytes != null) output.write(bytes)
        else checkNotNull(payload.file).inputStream().buffered().use { input ->
            val decipher = cipher(Cipher.DECRYPT_MODE, checkNotNull(payload.iv))
            val buffer = ByteArray(64 * 1024)
            var read = input.read(buffer)
            while (read >= 0) {
                checkInterrupted()
                decipher.update(buffer, 0, read)?.let { output.write(it) }
                read = input.read(buffer)
            }
            output.write(decipher.doFinal())
        }
    }

    fun decryptPackageEntry(name: String, packageKey: SecretKey) {
        val iv = ByteArray(12)
        var ivBytes = 0
        val decipher = Cipher.getInstance("AES/GCM/NoPadding")
        write(name) { output ->
            val encryptedInput = object : OutputStream() {
                override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    var start = offset
                    var count = length
                    if (ivBytes < iv.size) {
                        val copied = minOf(count, iv.size - ivBytes)
                        bytes.copyInto(iv, ivBytes, start, start + copied)
                        ivBytes += copied
                        start += copied
                        count -= copied
                        if (ivBytes == iv.size) decipher.init(Cipher.DECRYPT_MODE, packageKey, GCMParameterSpec(128, iv))
                    }
                    if (count > 0) decipher.update(bytes, start, count)?.let { output.write(it) }
                }
            }
            writeTo(name, encryptedInput)
            require(ivBytes == iv.size) { "Missing attachment IV" }
            output.write(decipher.doFinal())
        }
    }

    private fun checkInterrupted() {
        if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException("Import cancelled")
    }

    fun write(name: String, block: (OutputStream) -> Unit) {
        check(!closed) { "Package payloads are closed" }
        val digest = MessageDigest.getInstance("SHA-256")
        val memoryLimit = minOf(1024 * 1024L, 8 * 1024 * 1024L - memoryBytes).coerceAtLeast(0)
        var memory: ByteArrayOutputStream? = ByteArrayOutputStream()
        var file: File? = null
        var iv: ByteArray? = null
        var encrypted: OutputStream? = null
        var count = 0L
        val output = object : OutputStream() {
            override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)
            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                checkInterrupted()
                if (memory != null && count + length > memoryLimit) {
                    val root = directory ?: (if (parent == null) Files.createTempDirectory("ofam-import-")
                        else Files.createTempDirectory(parent.toPath(), "ofam-import-")).toFile().also { directory = it }
                    file = Files.createTempFile(root.toPath(), "payload-", ".enc").toFile()
                    iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
                    encrypted = CipherOutputStream(file!!.outputStream().buffered(), cipher(Cipher.ENCRYPT_MODE, iv))
                    memory!!.writeTo(encrypted)
                    memory = null
                }
                (encrypted ?: memory!!).write(bytes, offset, length)
                digest.update(bytes, offset, length)
                count += length
            }
        }
        try {
            block(output)
            encrypted?.close()
            encrypted = null
            val bytes = memory?.toByteArray()
            val payload = Payload(count, digest.digest().joinToString("") { "%02x".format(it) }, bytes, file, iv)
            remove(name)
            payloads[name] = payload
            memoryBytes += bytes?.size ?: 0
            file = null
        } finally {
            try { encrypted?.close() } finally { file?.let { Files.deleteIfExists(it.toPath()) } }
        }
    }

    fun remove(name: String) {
        payloads[name]?.let {
            it.file?.let { file -> Files.deleteIfExists(file.toPath()) }
            memoryBytes -= it.bytes?.size ?: 0
            payloads.remove(name)
        }
    }

    private fun cipher(mode: Int, iv: ByteArray): Cipher {
        val secret = key ?: KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also { key = it }
        return Cipher.getInstance("AES/GCM/NoPadding").apply { init(mode, secret, GCMParameterSpec(128, iv)) }
    }

    override fun close() {
        if (closed) return
        payloads.keys.toList().forEach(::remove)
        directory?.let { Files.deleteIfExists(it.toPath()) }
        key = null
        closed = true
    }
}
