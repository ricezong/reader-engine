package cn.kong.app.exception;

/**
 * 正文内容为空异常
 */
public class ContentEmptyException extends NoStackTraceException {

    public ContentEmptyException(String message) {
        super(message);
    }
}
