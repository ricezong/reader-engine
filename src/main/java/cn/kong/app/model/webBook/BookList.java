package cn.kong.app.model.webBook;

import cn.kong.app.help.js.JsEngine;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import cn.kong.app.data.entities.BookSource;
import cn.kong.app.data.entities.SearchBook;
import cn.kong.app.help.js.JsExtensions;
import cn.kong.app.model.analyzeRule.AnalyzeRule;
import cn.kong.app.utils.StringUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 书籍列表解析
 * 改造自 reader-dev 的 BookList.kt
 * <p>
 * 负责解析搜索结果列表和发现页列表。
 * 支持普通 CSS/XPath 规则和 JS 脚本规则。
 */
public class BookList {

    private static final Logger log = LoggerFactory.getLogger(BookList.class);
    private static final Gson gson = new Gson();

    /**
     * 解析搜索结果（普通模式）
     */
    public static List<SearchBook> parseSearch(String body, BookSource bookSource) {
        return parseSearch(body, bookSource, null);
    }

    /**
     * 解析搜索结果（支持 JS 扩展）
     *
     * @param body       响应体
     * @param bookSource 书源
     * @param ext        JS 扩展对象（可为 null）
     * @return 搜索结果列表
     */
    public static List<SearchBook> parseSearch(String body, BookSource bookSource, JsExtensions ext) {
        List<SearchBook> result = new ArrayList<>();
        if (StringUtils.isEmpty(body) || bookSource == null) {
            return result;
        }
        String bookListRule = bookSource.getRuleSearch().getBookList();
        if (StringUtils.isEmpty(bookListRule)) {
            log.warn("书源 [{}] 未配置搜索列表规则", bookSource.getBookSourceName());
            return result;
        }
        try {
            // 如果 bookList 规则是 JS 脚本
            if (bookListRule.startsWith("<js>") || bookListRule.startsWith("@js:")) {
                return parseSearchByJs(body, bookSource, bookListRule, ext);
            }

            // 普通 CSS/XPath 规则
            AnalyzeRule<String> analyzer = new AnalyzeRule<>(body);
            if (ext != null) {
                analyzer.setJsExtensions(ext);
            }
            List<Element> elements = analyzer.getElements(bookListRule);
            for (Element element : elements) {
                SearchBook searchBook = parseSearchItem(element, bookSource, ext);
                if (searchBook != null && StringUtils.isNotEmpty(searchBook.getBookUrl())) {
                    result.add(searchBook);
                }
            }
            analyzer.release();
        } catch (Exception e) {
            log.error("搜索结果解析失败: source={}, error={}",
                bookSource.getBookSourceName(), e.getMessage());
        }
        return result;
    }

    /**
     * 通过 JS 脚本解析搜索结果
     * JS 脚本通过 Put(data) 输出 JSON 数组
     */
    private static List<SearchBook> parseSearchByJs(String body, BookSource bookSource, String jsRule, JsExtensions ext) {
        List<SearchBook> result = new ArrayList<>();
        try {
            if (ext == null) {
                ext = new JsExtensions(bookSource);
            }

            // 提取 bookList JS 脚本内容
            String js;
            if (jsRule.startsWith("<js>")) {
                js = jsRule.substring(4, jsRule.lastIndexOf("</js>"));
            } else {
                js = jsRule.substring(4);
            }

            // 预执行 loginUrl 脚本，初始化函数和变量
            String loginUrl = bookSource.getLoginUrl();
            String initJs = "";
            if (StringUtils.isNotEmpty(loginUrl)) {
                initJs = loginUrl.replace("eval(String(source.loginUrl));", "");
            }

            // 准备变量
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("result", body);
            vars.put("baseUrl", bookSource.getBookSourceUrl());

            // 设置 resultStr 供 java.getElements 使用
            ext.setResultStr(body);

            // 合并 loginUrl 和 bookList JS 到同一 scope 执行
            String combinedJs = initJs + "\n" + js;

            JsEngine jsEngine = JsEngine.getInstance();
            jsEngine.eval(combinedJs, vars, ext);

            // 获取 Put 输出的结果
            String putResult = ext.getPutResult() == null ? null : ext.getPutResult().toString();
            if (StringUtils.isEmpty(putResult)) {
                log.warn("书源 [{}] JS bookList 执行后 Put 结果为空", bookSource.getBookSourceName());
                return result;
            }

            // 解析 JSON 数组
            JsonElement jsonElement = JsonParser.parseString(putResult);
            if (jsonElement.isJsonArray()) {
                JsonArray array = jsonElement.getAsJsonArray();
                for (JsonElement item : array) {
                    if (item.isJsonObject()) {
                        SearchBook searchBook = parseSearchItemFromJson(item.getAsJsonObject(), bookSource);
                        if (searchBook != null && StringUtils.isNotEmpty(searchBook.getBookUrl())) {
                            result.add(searchBook);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("JS 搜索结果解析失败: source={}, error={}",
                bookSource.getBookSourceName(), e.getMessage(), e);
        }
        return result;
    }

    /**
     * 从 JSON 对象解析搜索结果
     * JSON 格式：{name, author, kind, word, latest, intro, cover, url}
     */
    private static SearchBook parseSearchItemFromJson(com.google.gson.JsonObject json, BookSource bookSource) {
        SearchBook searchBook = new SearchBook();
        searchBook.setOrigin(bookSource.getBookSourceUrl());
        searchBook.setOriginName(bookSource.getBookSourceName());
        searchBook.setType(bookSource.getBookSourceType());

        if (json.has("name")) searchBook.setName(json.get("name").getAsString());
        if (json.has("author")) searchBook.setAuthor(json.get("author").getAsString());
        if (json.has("kind")) searchBook.setKind(json.get("kind").getAsString());
        if (json.has("word")) searchBook.setWordCount(json.get("word").getAsString());
        if (json.has("latest")) searchBook.setLastChapter(json.get("latest").getAsString());
        if (json.has("intro")) searchBook.setIntro(json.get("intro").getAsString());
        if (json.has("cover")) searchBook.setCoverUrl(json.get("cover").getAsString());
        if (json.has("url")) searchBook.setBookUrl(json.get("url").getAsString());

        return searchBook;
    }

    /**
     * 解析单个搜索结果（CSS 模式）
     */
    private static SearchBook parseSearchItem(Element element, BookSource bookSource, JsExtensions ext) {
        AnalyzeRule<Element> analyzer = new AnalyzeRule<>(element);
        if (ext != null) {
            analyzer.setJsExtensions(ext);
        }
        SearchBook searchBook = new SearchBook();
        searchBook.setOrigin(bookSource.getBookSourceUrl());
        searchBook.setOriginName(bookSource.getBookSourceName());
        searchBook.setType(bookSource.getBookSourceType());

        searchBook.setName(analyzer.getString(bookSource.getRuleSearch().getName()));
        searchBook.setAuthor(analyzer.getString(bookSource.getRuleSearch().getAuthor()));
        searchBook.setKind(analyzer.getString(bookSource.getRuleSearch().getCategory()));
        searchBook.setWordCount(analyzer.getString(bookSource.getRuleSearch().getWordCount()));
        searchBook.setLastChapter(analyzer.getString(bookSource.getRuleSearch().getLastChapter()));
        searchBook.setIntro(analyzer.getString(bookSource.getRuleSearch().getIntro()));
        searchBook.setCoverUrl(analyzer.getString(bookSource.getRuleSearch().getCoverUrl()));
        searchBook.setBookUrl(analyzer.getString(bookSource.getRuleSearch().getBookUrl()));

        analyzer.release();
        return searchBook;
    }
}
