package cn.kong.app.engine;

import java.util.Set;

/**
 * SourceException - 书源操作统一异常封装
 * <p>
 * 参考 FullSourceMatrixTest 中的异常分类模式，将搜索、详情、目录、正文四个环节
 * 的异常统一封装，提供友好的错误提示信息。
 * <p>
 * 异常分类：
 * <ul>
 *   <li>{@link Step#SOURCE_NOT_FOUND} - 找不到书源</li>
 *   <li>{@link Step#SEARCH} - 搜索失败</li>
 *   <li>{@link Step#DETAIL} - 获取详情失败</li>
 *   <li>{@link Step#TOC} - 获取目录失败</li>
 *   <li>{@link Step#CONTENT} - 获取正文失败</li>
 *   <li>{@link Step#INIT} - 书源初始化失败</li>
 *   <li>{@link Step#PARSE} - 书源解析失败</li>
 * </ul>
 */
public class SourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 异常环节 */
    public enum Step {
        SOURCE_NOT_FOUND("找不到书源"),
        SEARCH("搜索"),
        DETAIL("详情"),
        TOC("目录"),
        CONTENT("正文"),
        INIT("初始化"),
        PARSE("解析");

        private final String desc;

        Step(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
    }

    private final Step step;
    private final String source;
    private final String sourceName;

    /**
     * 构造 SourceException
     *
     * @param step       异常环节
     * @param source     书源简称
     * @param sourceName 书源名称
     * @param detail     详细信息（如关键词、bookUrl 等）
     * @param cause      原始异常
     */
    private SourceException(Step step, String source, String sourceName, String detail, Throwable cause) {
        super(formatMessage(step, source, sourceName, detail, cause), cause);
        this.step = step;
        this.source = source;
        this.sourceName = sourceName;
    }

    /**
     * 构造 SourceException（无原始异常）
     */
    private SourceException(Step step, String source, String sourceName, String detail) {
        this(step, source, sourceName, detail, null);
    }

    // ==================== 工厂方法 ====================

    /**
     * 找不到书源
     *
     * @param source       传入的书源简称
     * @param availableKeys 可用的书源简称集合
     * @return SourceException
     */
    public static SourceException sourceNotFound(String source, Set<String> availableKeys) {
        String detail = "可用书源: " + availableKeys;
        return new SourceException(Step.SOURCE_NOT_FOUND, source, "", detail);
    }

    /**
     * 搜索失败
     *
     * @param source     书源简称
     * @param sourceName 书源名称
     * @param keyword    搜索关键词
     * @param cause      原始异常
     * @return SourceException
     */
    public static SourceException searchError(String source, String sourceName, String keyword, Throwable cause) {
        String detail = "关键词: " + keyword;
        return new SourceException(Step.SEARCH, source, sourceName, detail, cause);
    }

    /**
     * 获取详情失败
     *
     * @param source     书源简称
     * @param sourceName 书源名称
     * @param bookUrl    书籍 URL
     * @param cause      原始异常
     * @return SourceException
     */
    public static SourceException detailError(String source, String sourceName, String bookUrl, Throwable cause) {
        String detail = "bookUrl: " + bookUrl;
        return new SourceException(Step.DETAIL, source, sourceName, detail, cause);
    }

    /**
     * 获取目录失败
     *
     * @param source     书源简称
     * @param sourceName 书源名称
     * @param bookUrl    书籍 URL
     * @param cause      原始异常
     * @return SourceException
     */
    public static SourceException tocError(String source, String sourceName, String bookUrl, Throwable cause) {
        String detail = "bookUrl: " + bookUrl;
        return new SourceException(Step.TOC, source, sourceName, detail, cause);
    }

    /**
     * 获取正文失败
     *
     * @param source     书源简称
     * @param sourceName 书源名称
     * @param detail     详细信息（如章节标题、章节 URL 等）
     * @param cause      原始异常（可为 null）
     * @return SourceException
     */
    public static SourceException contentError(String source, String sourceName, String detail, Throwable cause) {
        return new SourceException(Step.CONTENT, source, sourceName, detail, cause);
    }

    /**
     * 书源初始化失败
     *
     * @param source     书源简称
     * @param sourceName 书源名称
     * @param cause      原始异常
     * @return SourceException
     */
    public static SourceException initError(String source, String sourceName, Throwable cause) {
        return new SourceException(Step.INIT, source, sourceName, "", cause);
    }

    /**
     * 书源解析失败
     *
     * @param detail 详细信息
     * @param cause  原始异常
     * @return SourceException
     */
    public static SourceException parseError(String detail, Throwable cause) {
        return new SourceException(Step.PARSE, "", "", detail, cause);
    }

    // ==================== 格式化 ====================

    /**
     * 格式化异常消息，参考 FullSourceMatrixTest 中的提示风格
     * <p>
     * 格式: [环节] ✗ 书源 [简称](名称) 失败: 详细信息
     *       原因: 原始异常消息
     */
    private static String formatMessage(Step step, String source, String sourceName, String detail, Throwable cause) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(step.getDesc()).append("] ✗ ");

        // 书源信息
        if (source != null && !source.isEmpty()) {
            sb.append("书源 [").append(source).append("]");
            if (sourceName != null && !sourceName.isEmpty()) {
                sb.append("(").append(sourceName).append(")");
            }
            sb.append(" ");
        }

        sb.append("失败");

        // 详细信息
        if (detail != null && !detail.isEmpty()) {
            sb.append(": ").append(detail);
        }

        // 原始异常原因
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isEmpty()) {
            sb.append(" | 原因: ").append(cause.getMessage());
        }

        return sb.toString();
    }

    // ==================== Getter ====================

    public Step getStep() {
        return step;
    }

    public String getSource() {
        return source;
    }

    public String getSourceName() {
        return sourceName;
    }
}
