package io.legado.app.utils

import java.io.File
import java.util.zip.ZipInputStream

/**
 * ZipUtils 简化实现
 * 改造自 reader-dev 的 ZipUtils.kt
 */
object ZipUtils {

    fun unzipFile(zipFile: File, unzipFolder: File) {
        if (!unzipFolder.exists()) {
            unzipFolder.mkdirs()
        }
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(unzipFolder, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output ->
                        zis.copyTo(output)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}
