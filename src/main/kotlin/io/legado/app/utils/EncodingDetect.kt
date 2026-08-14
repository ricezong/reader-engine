package io.legado.app.utils

/**
 * 编码检测简化实现
 * 改造自 reader-dev 的 EncodingDetect.kt
 */
object EncodingDetect {

    fun getEncode(byteArray: ByteArray): String {
        return getHtmlEncode(byteArray)
    }

    fun getEncode(file: java.io.File): String {
        val bytes = file.readBytes()
        return getHtmlEncode(bytes)
    }

    fun getHtmlEncode(byteArray: ByteArray): String {
        // 检查 BOM
        if (byteArray.size >= 3) {
            if (byteArray[0] == 0xEF.toByte() && byteArray[1] == 0xBB.toByte() && byteArray[2] == 0xBF.toByte()) {
                return "UTF-8"
            }
        }
        if (byteArray.size >= 4) {
            // UTF-32 BE
            if (byteArray[0] == 0x00.toByte() && byteArray[1] == 0x00.toByte() &&
                byteArray[2] == 0xFE.toByte() && byteArray[3] == 0xFF.toByte()
            ) {
                return "UTF-32BE"
            }
            // UTF-32 LE
            if (byteArray[0] == 0xFF.toByte() && byteArray[1] == 0xFE.toByte() &&
                byteArray[2] == 0x00.toByte() && byteArray[3] == 0x00.toByte()
            ) {
                return "UTF-32LE"
            }
        }
        if (byteArray.size >= 2) {
            // UTF-16 BE
            if (byteArray[0] == 0xFE.toByte() && byteArray[1] == 0xFF.toByte()) {
                return "UTF-16BE"
            }
            // UTF-16 LE
            if (byteArray[0] == 0xFF.toByte() && byteArray[1] == 0xFE.toByte()) {
                return "UTF-16LE"
            }
        }

        // 检查 HTML meta charset
        val head = String(byteArray.copyOfMin(1024), Charsets.ISO_8859_1)
        val charsetPattern = Regex("charset[\"'= ]*([a-zA-Z0-9-]+)", RegexOption.IGNORE_CASE)
        val match = charsetPattern.find(head)
        if (match != null) {
            val charset = match.groupValues[1]
            if (isValidCharset(charset)) {
                return charset
            }
        }

        // 默认 UTF-8
        return "UTF-8"
    }

    private fun ByteArray.copyOfMin(size: Int): ByteArray {
        return if (this.size > size) this.copyOf(size) else this
    }

    private fun isValidCharset(charset: String): Boolean {
        return try {
            java.nio.charset.Charset.forName(charset)
            true
        } catch (e: Exception) {
            false
        }
    }
}
