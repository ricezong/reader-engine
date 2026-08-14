package cn.kong.app.model.webBook;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cn.kong.app.data.entities.BookChapter;
import cn.kong.app.data.entities.BookSource;
import cn.kong.app.help.js.JsEngine;
import cn.kong.app.help.js.JsExtensions;
import cn.kong.app.model.analyzeRule.AnalyzeByJSoup;
import cn.kong.app.model.analyzeRule.AnalyzeRule;
import cn.kong.app.utils.StringUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 章节目录解析
 * 改造自 reader-dev 的 BookChapterList.kt
 * 支持普通 CSS 规则和 JS 脚本规则（chapterList 规则通过 Put 输出 JSON 数组）
 */
public class BookChapterList {

    private static final Logger log = LoggerFactory.getLogger(BookChapterList.class);
    private static final Gson gson = new Gson();

    /**
     * 解析章节目录（普通模式）
     */
    public static List<BookChapter> parse(String body, BookSource bookSource) {
        return parse(body, bookSource, null);
    }

    /**
     * 解析章节目录（支持 JS 扩展）
     *
     * @param body       响应体
     * @param bookSource 书源
     * @param ext        JS 扩展对象（可为 null）
     * @return 章节列表
     */
    public static List<BookChapter> parse(String body, BookSource bookSource, JsExtensions ext) {
        List<BookChapter> result = new ArrayList<>();
        if (StringUtils.isEmpty(body) || bookSource == null) {
            return result;
        }
        String chapterListRule = bookSource.getRuleToc().getChapterList();
        if (StringUtils.isEmpty(chapterListRule)) {
            log.warn("书源 [{}] 未配置章节列表规则", bookSource.getBookSourceName());
            return result;
        }

        try {
            if (ext == null) {
                ext = new JsExtensions(bookSource);
            }
            ext.setResultStr(body);

            // 如果 chapterList 规则是 JS 脚本
            if (chapterListRule.startsWith("<js>") || chapterListRule.startsWith("@js:")) {
                return parseByJs(body, bookSource, chapterListRule, ext);
            }

            // 普通 CSS 规则
            AnalyzeRule<String> analyzer = new AnalyzeRule<>(body);
            analyzer.setJsExtensions(ext);
            List<Element> elements = analyzer.getElements(chapterListRule);
            log.info("章节列表规则 [{}] 解析到 {} 个元素", chapterListRule, elements.size());
            for (Element element : elements) {
                BookChapter chapter = parseChapter(element, bookSource, ext);
                if (chapter != null && StringUtils.isNotEmpty(chapter.getUrl())) {
                    result.add(chapter);
                }
            }
            analyzer.release();
        } catch (Exception e) {
            log.error("章节目录解析失败: source={}, error={}",
                bookSource.getBookSourceName(), e.getMessage());
        }
        return result;
    }

    /**
     * 通过 JS 脚本解析章节目录
     * JS 脚本通过 Put(data) 输出 JSON 数组
     */
    private static List<BookChapter> parseByJs(String body, BookSource bookSource, String jsRule, JsExtensions ext) {
        List<BookChapter> result = new ArrayList<>();
        try {
            // 预执行 loginUrl
            String loginUrl = bookSource.getLoginUrl();
            String initJs = "";
            if (StringUtils.isNotEmpty(loginUrl)) {
                initJs = loginUrl.replace("eval(String(source.loginUrl));", "");
            }

            // 提取 JS 脚本
            String js;
            if (jsRule.startsWith("<js>")) {
                js = jsRule.substring(4, jsRule.lastIndexOf("</js>"));
            } else {
                js = jsRule.substring(4);
            }

            // 准备变量
            Map<String, Object> vars = new HashMap<>();
            vars.put("result", body);
            vars.put("baseUrl", bookSource.getBookSourceUrl());
            // 注入 book 变量
            Map<String, Object> bookMap = new HashMap<>();
            bookMap.put("name", "");
            bookMap.put("author", "");
            bookMap.put("kind", "");
            bookMap.put("bookUrl", "");
            bookMap.put("tocUrl", "");
            vars.put("book", bookMap);

            // 合并 loginUrl + chapterList JS
            String combinedJs = initJs + "\n" + js;

            JsEngine jsEngine = JsEngine.getInstance();
            jsEngine.eval(combinedJs, vars, ext);

            // 获取 Put 输出的结果
            String putResult = ext.getPutResult() == null ? null : ext.getPutResult().toString();
            if (StringUtils.isEmpty(putResult)) {
                log.warn("书源 [{}] JS chapterList 执行后 Put 结果为空", bookSource.getBookSourceName());
                return result;
            }

            // 解析 JSON 数组
            try {
                JsonArray jsonArray = gson.fromJson(putResult, JsonArray.class);
                log.info("JS chapterList 解析到 {} 个章节", jsonArray.size());
                for (JsonElement elem : jsonArray) {
                    if (elem.isJsonObject()) {
                        JsonObject obj = elem.getAsJsonObject();
                        BookChapter chapter = new BookChapter();
                        chapter.setTitle(getJsonField(obj, "title"));
                        chapter.setUrl(getJsonField(obj, "link"));
                        chapter.setUpdateTime(getJsonField(obj, "time"));
                        if (StringUtils.isNotEmpty(chapter.getUrl())) {
                            result.add(chapter);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("章节 JSON 解析失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("JS 章节目录解析失败: source={}, error={}",
                bookSource.getBookSourceName(), e.getMessage());
        }
        return result;
    }

    /**
     * 解析单个章节（CSS 模式）
     */
    private static BookChapter parseChapter(Element element, BookSource bookSource, JsExtensions ext) {
        BookChapter chapter = new BookChapter();
        String chapterNameRule = bookSource.getRuleToc().getChapterName();
        String chapterUrlRule = bookSource.getRuleToc().getChapterUrl();

        // chapterName 规则可能是 "text" 或 "text##regex"
        if (StringUtils.isNotEmpty(chapterNameRule)) {
            String name = AnalyzeByJSoup.getValue(element, chapterNameRule);
            // 处理 ## 正则替换
            int regexIdx = chapterNameRule.indexOf("##");
            if (regexIdx >= 0) {
                String regexPart = chapterNameRule.substring(regexIdx + 2);
                int repIdx = regexPart.indexOf("##");
                String regex = repIdx >= 0 ? regexPart.substring(0, repIdx) : regexPart;
                String replacement = repIdx >= 0 ? regexPart.substring(repIdx + 2) : "";
                try {
                    name = name.replaceAll(regex, replacement);
                } catch (Exception e) {
                    // 忽略正则错误
                }
            }
            chapter.setTitle(name);
        }

        // chapterUrl 规则通常是 "href"
        if (StringUtils.isNotEmpty(chapterUrlRule)) {
            chapter.setUrl(AnalyzeByJSoup.getValue(element, chapterUrlRule));
        }

        chapter.setUpdateTime(AnalyzeByJSoup.getValue(element,
            StringUtils.defaultIfEmpty(bookSource.getRuleToc().getUpdateTime(), "text")));

        return chapter;
    }

    /**
     * 从 JSON 对象中安全获取字符串字段
     */
    private static String getJsonField(JsonObject json, String field) {
        if (json != null && json.has(field) && !json.get(field).isJsonNull()) {
            return json.get(field).getAsString();
        }
        return "";
    }
}
