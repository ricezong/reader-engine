package io.legado.app.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * ACache 简化实现 - 内存缓存
 * 改造自 reader-dev 的 ACache.kt，移除了磁盘持久化
 */
object ACache {
    private val cacheMap = ConcurrentHashMap<String, ByteArray>()

    fun get(): ACache = this

    fun put(key: String, value: ByteArray, saveTime: Int = 0) {
        cacheMap[key] = value
    }

    fun put(key: String, value: String, saveTime: Int = 0) {
        cacheMap[key] = value.toByteArray()
    }

    fun getAsBinary(key: String): ByteArray? {
        return cacheMap[key]
    }

    fun getAsString(key: String): String? {
        return cacheMap[key]?.toString(Charsets.UTF_8)
    }

    fun remove(key: String) {
        cacheMap.remove(key)
    }
}
