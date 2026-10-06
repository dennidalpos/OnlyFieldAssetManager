package com.onlyfield.assetmanager.exchange

import java.io.File

/** Read one local attachment at a time during export. */
class FilePayloadMap(private val files: Map<String, File>) : AbstractMap<String, ByteArray>() {
    override val keys: Set<String> get() = files.keys
    override val size: Int get() = files.size
    override fun containsKey(key: String) = files.containsKey(key)
    override fun get(key: String): ByteArray? = files[key]?.let { file ->
        AttachmentFiles.validateSize(file.length())
        file.inputStream().use { input -> java.io.ByteArrayOutputStream().also { AttachmentFiles.copyBounded(input, it) }.toByteArray() }
    }
    override val entries: Set<Map.Entry<String, ByteArray>> get() = object : AbstractSet<Map.Entry<String, ByteArray>>() {
        override val size: Int get() = files.size
        override fun iterator(): Iterator<Map.Entry<String, ByteArray>> {
            val names = files.keys.iterator()
            return object : Iterator<Map.Entry<String, ByteArray>> {
                override fun hasNext() = names.hasNext()
                override fun next(): Map.Entry<String, ByteArray> = names.next().let { java.util.AbstractMap.SimpleImmutableEntry(it, getValue(it)) }
            }
        }
    }
}
