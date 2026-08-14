package cn.kong.app.constant;

import java.util.regex.Pattern;

/**
 * 正则表达式常量
 * 改造自 reader-dev 的 AppPattern.kt
 */
public final class AppPattern {

    private AppPattern() {
    }

    public static final Pattern BOOK_URL_PATTERN =
        Pattern.compile("^https?://[\\w-./?%&=#+]+$", Pattern.CASE_INSENSITIVE);

    public static final Pattern URL_PATTERN =
        Pattern.compile("https?://[\\w-./?%&=#+]+", Pattern.CASE_INSENSITIVE);

    public static final Pattern JSON_PATTERN =
        Pattern.compile("^\\s*[\\[{].*[}\\]]\\s*$", Pattern.DOTALL);

    public static final Pattern NUM_PATTERN = Pattern.compile("\\d+");

    public static final Pattern CHINESE_PATTERN =
        Pattern.compile("[\\u4e00-\\u9fa5]");

    public static final Pattern BLANK_LINE_PATTERN =
        Pattern.compile("\\n\\s*\\n");

    public static final Pattern HTML_TAG_PATTERN =
        Pattern.compile("<[^>]+>");
}
