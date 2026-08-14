package com.htmake.reader.init

import java.io.File

/**
 * appCtx 简化实现
 * reader-dev 中这是一个 Android Context 对象，这里简化为临时目录
 */
object appCtx {
    val cacheDir: String
        get() = File(System.getProperty("java.io.tmpdir"), "reader-engine-cache").apply { mkdirs() }.absolutePath
}
