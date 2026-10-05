package com.onlyfield.assetmanager.exchange

/** Later sources override earlier ones without loading their byte arrays. */
object PayloadViews {
    fun merge(vararg sources: Map<String, ByteArray>): Map<String, ByteArray> = object : AbstractMap<String, ByteArray>() {
        override val keys: Set<String> = sources.flatMap { it.keys }.toSet()
        override val size: Int get() = keys.size
        override fun containsKey(key: String) = key in keys
        override fun get(key: String): ByteArray? = sources.lastOrNull { it.containsKey(key) }?.get(key)
        override val entries: Set<Map.Entry<String, ByteArray>> get() = object : AbstractSet<Map.Entry<String, ByteArray>>() {
            override val size: Int get() = keys.size
            override fun iterator(): Iterator<Map.Entry<String, ByteArray>> {
                val names = keys.iterator()
                return object : Iterator<Map.Entry<String, ByteArray>> {
                    override fun hasNext() = names.hasNext()
                    override fun next(): Map.Entry<String, ByteArray> = names.next().let { java.util.AbstractMap.SimpleImmutableEntry(it, getValue(it)) }
                }
            }
        }
    }
}
