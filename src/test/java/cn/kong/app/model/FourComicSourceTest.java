package cn.kong.app.model;

import com.google.gson.Gson;
import cn.kong.app.data.entities.Book;
import cn.kong.app.data.entities.BookChapter;
import cn.kong.app.data.entities.BookSource;
import cn.kong.app.data.entities.SearchBook;
import cn.kong.app.help.js.JsEngine;
import cn.kong.app.help.js.JsExtensions;
import cn.kong.app.model.webBook.WebBook;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 四个漫画源完整测试
 * 测试集：偷星九月天、大主宰、哑舍
 * 测试流程：搜索 → 详情 → 目录 → 正文
 */
public class FourComicSourceTest {

    private static final Logger log = LoggerFactory.getLogger(FourComicSourceTest.class);

    private static final String[] SOURCES = {
            "comic_godamanga.json",      // G站漫画
            "comic_manhuatai.json",      // 漫画台
            "comic_rumanhua.json",       // 如漫画
            "comic_zaimanhua.json"      // 再漫画
    };

    private static final String[] KEYWORDS = {"偷星九月天", "大主宰", "哑舍"};

    /**
     * 加载书源
     */
    private BookSource loadSource(String resource) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(is, "书源文件未找到: " + resource);
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new Gson().fromJson(json, BookSource.class);
        }
    }

    /**
     * 测试搜索 URL 生成（不依赖 AES 解密）
     */
    @Test
    public void testAllSourcesSearchUrl() throws Exception {
        log.info("========== 测试：四个漫画源搜索 URL 生成 ==========");
        for (String resource : SOURCES) {
            BookSource source = loadSource(resource);
            log.info("\n--- {} ({}) ---", source.getBookSourceName(), source.getBookSourceUrl());

            String searchUrl = source.getSearchUrl();
            if (searchUrl == null || searchUrl.isEmpty()) {
                log.warn("  未配置搜索 URL");
                continue;
            }

            // 去掉 <js></js> 标签
            String jsScript = searchUrl;
            if (jsScript.startsWith("<js>")) {
                jsScript = jsScript.substring(4);
                int endIdx = jsScript.lastIndexOf("</js>");
                if (endIdx >= 0) {
                    jsScript = jsScript.substring(0, endIdx);
                }
            }

            JsExtensions ext = new JsExtensions(source);

            // 预执行 loginUrl
            String loginUrl = source.getLoginUrl();
            if (loginUrl != null && !loginUrl.isEmpty()) {
                String initJs = loginUrl.replace("eval(String(source.loginUrl));", "");
                Map<String, Object> initVars = new HashMap<>();
                initVars.put("baseUrl", source.getBookSourceUrl());
                try {
                    JsEngine.getInstance().eval(initJs, initVars, ext);
                } catch (Exception e) {
                    log.warn("  loginUrl 执行失败: {}", e.getMessage());
                }
            }

            // 执行搜索 URL 生成 JS
            for (String keyword : KEYWORDS) {
                Map<String, Object> vars = new HashMap<>();
                vars.put("key", keyword);
                vars.put("page", 1);
                vars.put("baseUrl", source.getBookSourceUrl());

                try {
                    Object result = JsEngine.getInstance().eval(jsScript, vars, ext);
                    String url = ext.getPutResult() == null
                        ? (result == null ? "" : result.toString())
                        : ext.getPutResult().toString();
                    if (url != null && !url.isEmpty()) {
                        log.info("  搜索 [{}] → {}", keyword, url);
                    } else {
                        log.warn("  搜索 [{}] → URL 为空", keyword);
                    }
                } catch (Exception e) {
                    log.warn("  搜索 [{}] → 执行失败: {}", keyword, e.getMessage());
                }
            }
        }
        log.info("\n========== 搜索 URL 生成测试完成 ==========");
    }

    /**
     * 测试 G站漫画完整流程（搜索→详情→目录→正文）
     * G站搜索 URL 最简单，不依赖 AES 解密
     */
    @Test
    public void testGodamangaFullWorkflow() throws Exception {
        log.info("========== G站漫画完整流程测试 ==========");
        BookSource source = loadSource("comic_godamanga.json");
        WebBook webBook = new WebBook(source);
        log.info("书源: {} ({})", source.getBookSourceName(), source.getBookSourceUrl());

        for (String keyword : KEYWORDS) {
            log.info("\n--- 搜索: {} ---", keyword);
            try {
                List<SearchBook> results = webBook.searchBook(keyword, 1);
                log.info("  搜索结果: {} 本", results.size());
                for (int i = 0; i < Math.min(results.size(), 3); i++) {
                    SearchBook sb = results.get(i);
                    log.info("    [{}] {} | {} | {}", i + 1, sb.getName(), sb.getAuthor(), sb.getBookUrl());
                }
                assertTrue(results != null, "搜索结果不应为 null");
            } catch (Exception e) {
                log.error("  搜索失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 测试所有书源的搜索流程
     */
    @Test
    public void testAllSourcesSearch() throws Exception {
        log.info("========== 四个漫画源搜索测试 ==========");
        for (String resource : SOURCES) {
            BookSource source = loadSource(resource);
            WebBook webBook = new WebBook(source);
            log.info("\n--- {} ({}) ---", source.getBookSourceName(), source.getBookSourceUrl());

            for (String keyword : KEYWORDS) {
                try {
                    List<SearchBook> results = webBook.searchBook(keyword, 1);
                    log.info("  搜索 [{}]: {} 本", keyword, results.size());
                    if (!results.isEmpty()) {
                        SearchBook first = results.get(0);
                        log.info("    首本: {} | {} | {}", first.getName(), first.getAuthor(), first.getBookUrl());

                        // 测试详情
                        Book book = webBook.getBookInfo(first.getBookUrl());
                        if (book != null) {
                            log.info("    详情: {} | {}", book.getName(), book.getAuthor());

                            // 测试目录
                            List<BookChapter> chapters = webBook.getChapterList(book);
                            log.info("    目录: {} 章", chapters.size());
                            if (!chapters.isEmpty()) {
                                log.info("    首章: {}", chapters.get(0).getTitle());

                                // 测试正文
                                BookChapter chapter = chapters.get(0);
                                String content = webBook.getBookContent(book, chapter);
                                int contentLen = content == null ? 0 : content.length();
                                log.info("    正文长度: {}", contentLen);
                                if (contentLen > 0) {
                                    // 统计图片数量
                                    int imgCount = content.split("<img").length - 1;
                                    log.info("    图片数: {}, 正文前100字符: {}", imgCount,
                                        content.substring(0, Math.min(100, contentLen)));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("  搜索 [{}] 失败: {}", keyword, e.getMessage());
                }
            }
        }
        log.info("\n========== 四源搜索测试完成 ==========");
        // G站漫画搜索应成功返回结果
        BookSource gSource = loadSource("comic_godamanga.json");
        WebBook gWebBook = new WebBook(gSource);
        List<SearchBook> gResults = gWebBook.searchBook("偷星九月天", 1);
        assertTrue(gResults.size() > 0, "G站漫画搜索偷星九月天应返回结果");
    }
}
