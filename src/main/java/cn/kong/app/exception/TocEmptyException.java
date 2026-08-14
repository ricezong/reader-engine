package cn.kong.app.exception;

/**
 * 章节目录为空异常
 */
public class TocEmptyException extends NoStackTraceException {

    public TocEmptyException(String message) {
        super(message);
    }
}
