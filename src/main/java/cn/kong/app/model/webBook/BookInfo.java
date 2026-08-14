package cn.kong.app.model.webBook;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import cn.kong.app.data.entities.Book;
import cn.kong.app.data.entities.BookSource;
import cn.kong.app.help.js.JsEngine;
import cn.kong.app.help.js.JsExtensions;
import cn.kong.app.model.analyzeRule.AnalyzeRule;
import cn.kong.app.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 书籍详情解析
 * 改造自 reader-dev 的 BookInfo.kt
 * 支持普通 CSS 规则和 JS 脚本规则（init 规则通过 Put 输出 JSON）
 */
public class BookInfo {

    private static final Logger log = LoggerFactory.getLogger(BookInfo.class);
    private static final Gson gson = new Gson();

    /**
     * 解析书籍详情（普通模式）
     */
    public static Book parse(String body, BookSource bookSource) {
        return parse(body, bookSource, null);
    }

    /**
     * 解析书籍详情（支持 JS 扩展）
     *
     * @param body       响应体
     * @param bookSource 书源
     * @param ext        JS 扩展对象（可为 null）
     * @return 书籍信息
     */
    public static Book parse(String body, BookSource bookSource, JsExtensions ext) {
        if (StringUtils.isEmpty(body) || bookSource == null) {
            return null;
        }
        try {
            if (ext == null) {
                ext = new JsExtensions(bookSource);
            }

            // 设置 resultStr 供 java.getElements 使用
            ext.setResultStr(body);

            // 检查 init 规则是否是 JS 脚本
            String initRule = bookSource.getRuleBookInfo().getInit();
            String putResult = null;

            if (StringUtils.isNotEmpty(initRule) && (initRule.startsWith("<js>") || initRule.startsWith("@js:"))) {
                // 预执行 loginUrl
                String loginUrl = bookSource.getLoginUrl();
                String initJs = "";
                if (StringUtils.isNotEmpty(loginUrl)) {
                    initJs = loginUrl.replace("eval(String(source.loginUrl));", "");
                }

                // 提取 init JS 脚本
                String js;
                if (initRule.startsWith("<js>")) {
                    js = initRule.substring(4, initRule.lastIndexOf("</js>"));
                } else {
                    js = initRule.substring(4);
                }

                // 准备变量
                Map<String, Object> vars = new HashMap<>();
                vars.put("result", body);
                vars.put("baseUrl", bookSource.getBookSourceUrl());
                // 注入 book 变量（JS 中 book['author'] 等访问）
                Map<String, Object> bookMap = new HashMap<>();
                bookMap.put("name", "");
                bookMap.put("author", "");
                bookMap.put("kind", "");
                bookMap.put("word", "");
                bookMap.put("latest", "");
                bookMap.put("intro", "");
                bookMap.put("cover", "");
                bookMap.put("url", "");
                bookMap.put("durl", "");
                bookMap.put("bookUrl", "");
                vars.put("book", bookMap);

                // 合并 loginUrl + init JS
                String combinedJs = initJs + "\n" + js;

                try {
                    JsEngine.getInstance().eval(combinedJs, vars, ext);
                    putResult = ext.getPutResult() == null ? null : ext.getPutResult().toString();
                } catch (Exception e) {
                    log.warn("init JS 执行失败: {}", e.getMessage());
                }
            }

            Book book = new Book();

            // 如果有 Put 结果（JSON），从 JSON 中提取字段
            if (StringUtils.isNotEmpty(putResult)) {
                try {
                    JsonObject json = gson.fromJson(putResult, JsonObject.class);
                    book.setName(getJsonField(json, "name"));
                    book.setAuthor(getJsonField(json, "author"));
                    book.setKind(getJsonField(json, "kind"));
                    book.setWordCount(getJsonField(json, "word"));
                    book.setLastChapterName(getJsonField(json, "latest"));
                    book.setIntro(getJsonField(json, "intro"));
                    book.setCoverUrl(getJsonField(json, "cover"));
                    book.setTocUrl(getJsonField(json, "url"));
                } catch (Exception e) {
                    log.warn("Put JSON 解析失败: {}", e.getMessage());
                }
            }

            // 如果 Put 结果中没有某些字段，用普通规则解析
            AnalyzeRule<String> analyzer = new AnalyzeRule<>(body);
            analyzer.setJsExtensions(ext);
            if (StringUtils.isEmpty(book.getName())) {
                book.setName(analyzer.getString(bookSource.getRuleBookInfo().getName()));
            }
            if (StringUtils.isEmpty(book.getAuthor())) {
                book.setAuthor(analyzer.getString(bookSource.getRuleBookInfo().getAuthor()));
            }
            if (StringUtils.isEmpty(book.getKind())) {
                book.setKind(analyzer.getString(bookSource.getRuleBookInfo().getCategory()));
            }
            if (StringUtils.isEmpty(book.getIntro())) {
                book.setIntro(analyzer.getString(bookSource.getRuleBookInfo().getIntro()));
            }
            if (StringUtils.isEmpty(book.getCoverUrl())) {
                book.setCoverUrl(analyzer.getString(bookSource.getRuleBookInfo().getCoverUrl()));
            }
            if (StringUtils.isEmpty(book.getTocUrl())) {
                book.setTocUrl(analyzer.getString(bookSource.getRuleBookInfo().getTocUrl()));
            }

            // 如果目录 URL 为空，使用详情页 URL
            if (StringUtils.isEmpty(book.getTocUrl())) {
                book.setTocUrl(book.getBookUrl());
            }

            analyzer.release();
            return book;
        } catch (Exception e) {
            log.error("书籍详情解析失败: source={}, error={}",
                bookSource.getBookSourceName(), e.getMessage());
            return null;
        }
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
