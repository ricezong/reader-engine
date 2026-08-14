package cn.kong.app.exception;

/**
 * 无堆栈异常（用于业务逻辑中断，避免性能开销）
 * 改造自 reader-dev 的 NoStackTraceException.kt
 */
public class NoStackTraceException extends RuntimeException {

    public NoStackTraceException(String message) {
        super(message, null, false, false);
    }

    public NoStackTraceException(String message, Throwable cause) {
        super(message, cause, false, false);
    }
}
