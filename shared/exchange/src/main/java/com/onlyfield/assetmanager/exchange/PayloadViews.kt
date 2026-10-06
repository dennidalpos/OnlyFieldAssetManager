package com.onlyfield.assetmanager.exchange

/** Later sources override earlier ones without loading their byte arrays. */
object PayloadViews {
    fun merge(vararg sources: Map<String, ByteArray>): Map<String, ByteArray> = view(sources.flatMap { it.keys }.toSet()) { key ->
        sources.lastOrNull { it.containsKey(key) }?.get(key)
    }

    /** Select by name before reading bytes, including oversized historical orphans. */
    fun select(source: Map<String, ByteArray>, names: Set<String>): Map<String, ByteArray> =
        view(source.keys.intersect(names)) { source[it] }

    private fun view(names: Set<String>, read: (String) -> ByteArray?): Map<String, ByteArray> = object : AbstractMap<String, ByteArray>() {
        override val keys: Set<String> = names
        override val size: Int get() = keys.size
        override fun containsKey(key: String) = key in keys
        override fun get(key: String): ByteArray? = if (key in keys) read(key) else null
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
