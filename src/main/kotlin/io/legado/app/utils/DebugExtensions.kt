package io.legado.app.utils

import io.legado.app.model.Debug
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * printOnDebug - 输出调试日志
 */
fun Throwable.printOnDebug() {
    Debug.log(this.stackTraceToString())
}

/**
 * msg - 获取异常的本地化消息
 */
val Throwable.msg: String
    get() = this.localizedMessage ?: this.message ?: "Unknown error"
