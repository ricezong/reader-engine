package cn.kong.app.model.webBook;

import cn.kong.app.data.entities.BookChapter;
import cn.kong.app.data.entities.BookSource;
import cn.kong.app.help.js.JsEngine;
import cn.kong.app.help.js.JsExtensions;
import cn.kong.app.model.analyzeRule.AnalyzeRule;
import cn.kong.app.utils.UrlUtil;
import cn.kong.app.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 正文内容解析
 * 改造自 reader-dev 的 BookContent.kt
 * <p>
 * 对于小说书源（bookSourceType=0），提取正文文本段落；
 * 对于漫画源（bookSourceType=2），提取图片 URL 列表。
 * 支持普通 CSS 规则和 JS 脚本规则。
 */
public class BookContent {

    private static final Logger log = LoggerFactory.getLogger(BookContent.class);

    /**
     * 解析正文内容（普通模式）
     */
    public static String parse(String body, BookSource bookSource, BookChapter chapter) {
        return parse(body, bookSource, chapter, null);
    }

    /**
     * 解析正文内容（支持 JS 扩展）
     *
     * @param body       响应体
     * @param bookSource 书源
     * @param chapter    章节
     * @param ext        JS 扩展对象（可为 null）
     * @return 正文内容（小说为文本，漫画为图片 HTML 或 URL 列表）
     */
    public static String parse(String body, BookSource bookSource, BookChapter chapter, JsExtensions ext) {
        if (StringUtils.isEmpty(body) || bookSource == null || chapter == null) {
            return "";
        }
        String contentRule = bookSource.getRuleContent().getContent();
        if (StringUtils.isEmpty(contentRule)) {
            log.warn("书源 [{}] 未配置正文规则", bookSource.getBookSourceName());
            return "";
        }
        try {
            if (ext == null) {
                ext = new JsExtensions(bookSource);
            }
            ext.setResultStr(body);

            String content;

            // 如果正文规则是 JS 脚本
            if (contentRule.startsWith("<js>") || contentRule.startsWith("@js:")) {
                content = parseByJs(body, bookSource, chapter, contentRule, ext);
            } else {
                // 普通 CSS 规则
                AnalyzeRule<String> analyzer = new AnalyzeRule<>(body);
                analyzer.setJsExtensions(ext);
                content = analyzer.getString(contentRule);
                analyzer.release();
            }

            // 应用替换规则（净化正文）
            String replaceRegex = bookSource.getRuleContent().getReplaceRegex();
            if (StringUtils.isNotEmpty(replaceRegex) && StringUtils.isNotEmpty(content)) {
                String regex = replaceRegex;
                String replacement = bookSource.getRuleContent().getReplacement();
                if (regex.startsWith("##")) {
                    regex = regex.substring(2);
                }
                int sepIdx = regex.indexOf("##");
                if (sepIdx >= 0) {
                    replacement = regex.substring(sepIdx + 2);
                    regex = regex.substring(0, sepIdx);
                }
                replacement = StringUtils.defaultIfEmpty(replacement, "");
                try {
                    content = content.replaceAll(regex, replacement);
                } catch (Exception e) {
                    log.warn("正文替换规则执行失败: regex={}, error={}", regex, e.getMessage());
                }
            }

            return content == null ? "" : content;
        } catch (Exception e) {
            log.error("正文内容解析失败: source={}, chapter={}, error={}",
                bookSource.getBookSourceName(), chapter.getTitle(), e.getMessage());
            return "";
        }
    }

    /**
     * 通过 JS 脚本解析正文内容
     * JS 脚本通过 return 或 Put() 输出结果
     */
    private static String parseByJs(String body, BookSource bookSource, BookChapter chapter,
                                    String contentRule, JsExtensions ext) {
        try {
            // 预执行 loginUrl
            String loginUrl = bookSource.getLoginUrl();
            String initJs = "";
            if (StringUtils.isNotEmpty(loginUrl)) {
                initJs = loginUrl.replace("eval(String(source.loginUrl));", "");
            }

            // 提取 JS 脚本
            String js;
            if (contentRule.startsWith("<js>")) {
                js = contentRule.substring(4, contentRule.lastIndexOf("</js>"));
            } else {
                js = contentRule.substring(4);
            }

            // 准备变量
            Map<String, Object> vars = new HashMap<>();
            vars.put("result", body);
            // baseUrl 设为章节 URL（正文 JS 中 java.ajax(baseUrl) 使用）
            String chapterUrl = UrlUtil.absoluteUrl(chapter.getUrl(), bookSource.getBookSourceUrl());
            vars.put("baseUrl", chapterUrl);
            vars.put("title", chapter.getTitle());
            // 注入 book 变量
            Map<String, Object> bookMap = new HashMap<>();
            bookMap.put("name", "");
            bookMap.put("author", "");
            bookMap.put("kind", "");
            bookMap.put("bookUrl", chapterUrl);
            bookMap.put("tocUrl", "");
            vars.put("book", bookMap);

            // 合并 loginUrl + content JS
            String combinedJs = initJs + "\n" + js;

            JsEngine jsEngine = JsEngine.getInstance();
            Object result = jsEngine.eval(combinedJs, vars, ext);

            // 优先使用 Put 输出的结果
            if (ext.getPutResult() != null) {
                return ext.getPutResult().toString();
            }
            return result == null ? "" : result.toString();
        } catch (Exception e) {
            log.error("JS 正文解析失败: source={}, chapter={}, error={}",
                bookSource.getBookSourceName(), chapter.getTitle(), e.getMessage());
            return "";
        }
    }

    /**
     * 解析漫画图片 URL 列表
     */
    public static java.util.List<String> parseImageUrls(String body, BookSource bookSource, BookChapter chapter) {
        return parseImageUrls(body, bookSource, chapter, null);
    }

    /**
     * 解析漫画图片 URL 列表（支持 JS 扩展）
     */
    public static java.util.List<String> parseImageUrls(String body, BookSource bookSource, BookChapter chapter, JsExtensions ext) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (StringUtils.isEmpty(body) || bookSource == null) {
            return result;
        }
        String imageUrlsRule = bookSource.getRuleContent().getImageUrls();
        if (StringUtils.isEmpty(imageUrlsRule)) {
            // 如果没有配置图片 URL 规则，使用正文规则
            String content = parse(body, bookSource, chapter, ext);
            if (StringUtils.isNotEmpty(content)) {
                // 漫画正文可能是 <img src="..."> 格式
                java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
                    "<img[^>]+src=[\"']([^\"']+)[\"']");
                java.util.regex.Matcher matcher = imgPattern.matcher(content);
                while (matcher.find()) {
                    result.add(matcher.group(1));
                }
                // 如果没匹配到 img 标签，按换行分割
                if (result.isEmpty()) {
                    String[] urls = content.split("\\n");
                    for (String url : urls) {
                        if (StringUtils.isNotEmpty(url.trim())) {
                            result.add(url.trim());
                        }
                    }
                }
            }
            return result;
        }
        try {
            if (ext == null) {
                ext = new JsExtensions(bookSource);
            }
            AnalyzeRule<String> analyzer = new AnalyzeRule<>(body);
            analyzer.setJsExtensions(ext);
            java.util.List<String> urls = analyzer.getStringList(imageUrlsRule);
            for (String url : urls) {
                if (StringUtils.isNotEmpty(url)) {
                    result.add(url);
                }
            }
            analyzer.release();
        } catch (Exception e) {
            log.error("漫画图片 URL 解析失败: source={}, chapter={}, error={}",
                bookSource.getBookSourceName(), chapter.getTitle(), e.getMessage());
        }
        return result;
    }
}
