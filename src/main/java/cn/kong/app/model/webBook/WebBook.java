package cn.kong.app.model.webBook;

import cn.kong.app.data.entities.Book;
import cn.kong.app.data.entities.BookChapter;
import cn.kong.app.data.entities.BookSource;
import cn.kong.app.data.entities.SearchBook;
import cn.kong.app.help.http.HttpHelper;
import cn.kong.app.help.js.JsEngine;
import cn.kong.app.help.js.JsExtensions;
import cn.kong.app.model.analyzeRule.AnalyzeUrl;
import cn.kong.app.utils.StringUtils;
import cn.kong.app.utils.UrlUtil;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebBook - 书源操作门面
 * 改造自 reader-dev 的 WebBook.kt
 * <p>
 * 这是书源解析引擎对外暴露的核心 API，封装了书源的完整操作流程：
 * <ol>
 *   <li>searchBook - 搜索书籍</li>
 *   <li>getBookInfo - 获取书籍详情</li>
 *   <li>getChapterList - 获取章节目录</li>
 *   <li>getBookContent - 获取正文内容</li>
 * </ol>
 * <p>
 * 替代原 Vert.x 异步实现，改为同步调用（更易于在 Spring Boot 中集成）。
 */
public class WebBook {

    private static final Logger log = LoggerFactory.getLogger(WebBook.class);

    private final BookSource bookSource;

    public WebBook(BookSource bookSource) {
        this.bookSource = bookSource;
    }

    /**
     * 搜索书籍
     *
     * @param query 搜索关键词
     * @param page  页码（从 1 开始）
     * @return 搜索结果列表
     */
    public List<SearchBook> searchBook(String query, int page) {
        if (StringUtils.isEmpty(query)) {
            return new ArrayList<>();
        }
        String searchUrl = bookSource.getSearchUrl();
        if (StringUtils.isEmpty(searchUrl)) {
            log.warn("书源 [{}] 未配置搜索 URL", bookSource.getBookSourceName());
            return new ArrayList<>();
        }

        try {
            // 创建 JS 扩展对象
            JsExtensions ext = new JsExtensions(bookSource);

            // 预执行 loginUrl 脚本，初始化变量和函数
            String loginUrl = bookSource.getLoginUrl();
            if (StringUtils.isNotEmpty(loginUrl)) {
                // 去掉递归的 eval(String(source.loginUrl)) 行，防止无限递归
                String initJs = loginUrl.replace("eval(String(source.loginUrl));", "");
                Map<String, Object> initVars = new HashMap<>();
                initVars.put("baseUrl", bookSource.getBookSourceUrl());
                initVars.put("bookSourceName", bookSource.getBookSourceName());
                try {
                    JsEngine.getInstance().eval(initJs, initVars, ext);
                } catch (Exception e) {
                    log.warn("loginUrl 脚本执行失败: {}", e.getMessage());
                }
            }

            String body;
            // 如果 searchUrl 是 JS 脚本，先执行 JS 获取真实 URL
            if (searchUrl.startsWith("<js>") || searchUrl.startsWith("@js:")) {
                // 去掉 <js></js> 标签
                String jsScript;
                if (searchUrl.startsWith("<js>")) {
                    jsScript = searchUrl.substring(4);
                    int endIdx = jsScript.lastIndexOf("</js>");
                    if (endIdx >= 0) {
                        jsScript = jsScript.substring(0, endIdx);
                    }
                } else {
                    jsScript = searchUrl.substring(4);
                }

                Map<String, Object> vars = new HashMap<>();
                vars.put("key", query);
                vars.put("page", page);
                vars.put("baseUrl", bookSource.getBookSourceUrl());
                vars.put("bookSourceName", bookSource.getBookSourceName());

                JsEngine jsEngine = JsEngine.getInstance();
                Object result = jsEngine.eval(jsScript, vars, ext);

                // 优先使用 Put 输出的结果
                String url = ext.getPutResult() == null ? null : ext.getPutResult().toString();
                if (StringUtils.isEmpty(url) && result != null) {
                    url = result.toString();
                }
                if (StringUtils.isEmpty(url)) {
                    log.warn("书源 [{}] JS 搜索 URL 执行后为空", bookSource.getBookSourceName());
                    return new ArrayList<>();
                }
                log.info("JS 生成的搜索 URL: {}", url);

                // 执行 HTTP 请求
                body = executeUrl(url, ext);
            } else {
                // 普通 URL，使用 AnalyzeUrl 解析
                Map<String, Object> variables = new HashMap<>();
                variables.put("key", query);
                variables.put("page", page);
                variables.put("baseUrl", bookSource.getBookSourceUrl());
                AnalyzeUrl analyzeUrl = new AnalyzeUrl(searchUrl, variables);
                body = executeRequest(analyzeUrl);
            }

            if (StringUtils.isEmpty(body)) {
                return new ArrayList<>();
            }

            // 解析搜索结果（bookList 规则可能是 JS 脚本）
            return BookList.parseSearch(body, bookSource, ext);
        } catch (Exception e) {
            log.error("搜索书籍失败: source={}, query={}, error={}",
                bookSource.getBookSourceName(), query, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 执行 URL 请求（支持带 header 的 URL 格式）
     */
    private String executeUrl(String urlStr, JsExtensions ext) throws IOException {
        if (StringUtils.isEmpty(urlStr)) {
            return "";
        }
        // 解析 URL 中的 header 配置
        // 格式：url,{"headers":{"key":"value"}}
        String url = urlStr;
        Map<String, String> headers = new HashMap<>();
        int jsonIdx = urlStr.indexOf(",{");
        if (jsonIdx > 0) {
            url = urlStr.substring(0, jsonIdx);
            String jsonConfig = urlStr.substring(jsonIdx + 1);
            try {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(jsonConfig).getAsJsonObject();
                if (json.has("headers")) {
                    com.google.gson.JsonObject headerJson = json.getAsJsonObject("headers");
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : headerJson.entrySet()) {
                        headers.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                if (json.has("method")) {
                    String method = json.get("method").getAsString();
                    if ("POST".equalsIgnoreCase(method)) {
                        String body = json.has("body") ? json.get("body").getAsString() : "";
                        okhttp3.Response response = HttpHelper.postBodyNoSsl(url, body, null, headers);
                        String result = HttpHelper.getResponseBody(response);
                        HttpHelper.closeResponse(response);
                        return result;
                    }
                }
            } catch (Exception e) {
                log.warn("URL header 配置解析失败: {}", e.getMessage());
            }
        }
        okhttp3.Response response = HttpHelper.getNoSsl(url, headers);
        String result = HttpHelper.getResponseBody(response);
        HttpHelper.closeResponse(response);
        return result;
    }

    /**
     * 获取书籍详情
     *
     * @param bookUrl 详情页 URL
     * @return 书籍信息
     */
    public Book getBookInfo(String bookUrl) {
        if (StringUtils.isEmpty(bookUrl)) {
            return null;
        }
        String absoluteUrl = UrlUtil.absoluteUrl(bookUrl, bookSource.getBookSourceUrl());

        try {
            // 创建 JS 扩展对象
            JsExtensions ext = new JsExtensions(bookSource);

            // 预执行 loginUrl
            String loginUrl = bookSource.getLoginUrl();
            if (StringUtils.isNotEmpty(loginUrl)) {
                String initJs = loginUrl.replace("eval(String(source.loginUrl));", "");
                Map<String, Object> initVars = new HashMap<>();
                initVars.put("baseUrl", bookSource.getBookSourceUrl());
                try {
                    JsEngine.getInstance().eval(initJs, initVars, ext);
                } catch (Exception e) {
                    log.warn("loginUrl 预执行失败: {}", e.getMessage());
                }
            }

            // 执行 HTTP 请求（支持带 header 的 URL）
            String body = executeUrl(absoluteUrl, ext);
            if (StringUtils.isEmpty(body)) {
                return null;
            }

            Book book = BookInfo.parse(body, bookSource, ext);
            if (book != null) {
                book.setBookUrl(absoluteUrl);
                book.setOrigin(bookSource.getBookSourceUrl());
                book.setOriginName(bookSource.getBookSourceName());
                book.setType(bookSource.getBookSourceType());
            }
            return book;
        } catch (Exception e) {
            log.error("获取书籍详情失败: source={}, url={}, error={}",
                bookSource.getBookSourceName(), bookUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 获取章节目录
     *
     * @param book 书籍
     * @return 章节列表
     */
    public List<BookChapter> getChapterList(Book book) {
        if (book == null || StringUtils.isEmpty(book.getTocUrl())) {
            return new ArrayList<>();
        }
        String tocUrl = UrlUtil.absoluteUrl(book.getTocUrl(), bookSource.getBookSourceUrl());

        try {
            // 创建 JS 扩展对象
            JsExtensions ext = new JsExtensions(bookSource);

            // 预执行 loginUrl
            String loginUrl = bookSource.getLoginUrl();
            if (StringUtils.isNotEmpty(loginUrl)) {
                String initJs = loginUrl.replace("eval(String(source.loginUrl));", "");
                Map<String, Object> initVars = new HashMap<>();
                initVars.put("baseUrl", bookSource.getBookSourceUrl());
                try {
                    JsEngine.getInstance().eval(initJs, initVars, ext);
                } catch (Exception e) {
                    log.warn("loginUrl 预执行失败: {}", e.getMessage());
                }
            }

            // 执行 HTTP 请求
            String body = executeUrl(tocUrl, ext);
            if (StringUtils.isEmpty(body)) {
                return new ArrayList<>();
            }

            List<BookChapter> chapters = BookChapterList.parse(body, bookSource, ext);
            // 设置章节索引
            for (int i = 0; i < chapters.size(); i++) {
                chapters.get(i).setIndex(i);
            }
            return chapters;
        } catch (Exception e) {
            log.error("获取章节目录失败: source={}, url={}, error={}",
                bookSource.getBookSourceName(), book.getTocUrl(), e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取正文内容
     *
     * @param book    书籍
     * @param chapter 章节
     * @return 正文内容（文本或图片 URL 列表）
     */
    public String getBookContent(Book book, BookChapter chapter) {
        if (book == null || chapter == null || StringUtils.isEmpty(chapter.getUrl())) {
            return "";
        }
        String contentUrl = UrlUtil.absoluteUrl(chapter.getUrl(), bookSource.getBookSourceUrl());

        try {
            // 创建 JS 扩展对象
            JsExtensions ext = new JsExtensions(bookSource);

            // 预执行 loginUrl
            String loginUrl = bookSource.getLoginUrl();
            if (StringUtils.isNotEmpty(loginUrl)) {
                String initJs = loginUrl.replace("eval(String(source.loginUrl));", "");
                Map<String, Object> initVars = new HashMap<>();
                initVars.put("baseUrl", bookSource.getBookSourceUrl());
                try {
                    JsEngine.getInstance().eval(initJs, initVars, ext);
                } catch (Exception e) {
                    log.warn("loginUrl 预执行失败: {}", e.getMessage());
                }
            }

            // 执行 HTTP 请求
            String body = executeUrl(contentUrl, ext);
            if (StringUtils.isEmpty(body)) {
                return "";
            }
            return BookContent.parse(body, bookSource, chapter, ext);
        } catch (Exception e) {
            log.error("获取正文内容失败: source={}, url={}, error={}",
                bookSource.getBookSourceName(), chapter.getUrl(), e.getMessage());
            return "";
        }
    }

    /**
     * 执行 HTTP 请求
     */
    private String executeRequest(AnalyzeUrl analyzeUrl) throws IOException {
        String url = analyzeUrl.getUrl();
        if (StringUtils.isEmpty(url)) {
            return "";
        }
        Map<String, String> headers = analyzeUrl.getHeaders();
        Response response = null;
        try {
            if (analyzeUrl.isPost()) {
                String body = analyzeUrl.getBody();
                if (StringUtils.isNotEmpty(body)) {
                    response = HttpHelper.postBody(url, body,
                        okhttp3.MediaType.parse("application/x-www-form-urlencoded"), headers);
                } else {
                    response = HttpHelper.postBody(url, "", null, headers);
                }
            } else {
                response = HttpHelper.get(url, headers);
            }
            if (!response.isSuccessful()) {
                log.warn("HTTP 请求失败: url={}, code={}", url, response.code());
                return "";
            }
            return HttpHelper.getResponseBody(response);
        } finally {
            HttpHelper.closeResponse(response);
        }
    }

    /**
     * 获取书源
     */
    public BookSource getBookSource() {
        return bookSource;
    }
}
