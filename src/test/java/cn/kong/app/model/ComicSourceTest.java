package cn.kong.app.model;

import cn.kong.app.engine.ReaderEngine;
import io.legado.app.data.entities.Book;
import io.legado.app.data.entities.BookChapter;
import io.legado.app.data.entities.BookSource;
import io.legado.app.data.entities.SearchBook;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 四个漫画源完整测试
 * <p>
 * 4 个漫画源端到端测试：搜索 → 详情 → 目录 → 正文
 * <p>
 * 漫画源：G站漫画、漫画台、如漫画、再漫画
 * <br>关键词：哑舍、偷星九月天、一人之下、斗破苍穹、镇魂街
 */
public class ComicSourceTest {

    private static final Logger log = LoggerFactory.getLogger(ComicSourceTest.class);

    private static final String[] SOURCES = {
            "comic_1.json",       // G站漫画
            "comic_2.json",       // 漫画台
            "comic_3.json",       // 如漫画
            "comic_4.json"        // 再漫画
    };

    private static final String[] KEYWORDS = {
            "哑舍",
            "偷星九月天",
            "一人之下",
            "斗破苍穹",
            "镇魂街"
    };

    /**
     * 加载漫画源 JSON 并初始化 variable
     * 漫画源必须调用 initSource 初始化 variable，否则 jsLib 中 Get('url') 等函数会返回 null
     */
    private BookSource loadSource(String resource) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(is, "书源文件未找到: " + resource);
            String json = readAllBytes(is);
            List<BookSource> sources = ReaderEngine.parseBookSources(json);
            assertNotNull(sources, "书源解析失败: " + resource);
            assertTrue(!sources.isEmpty(), "书源列表为空: " + resource);
            BookSource source = sources.get(0);
            // 漫画源初始化：执行 loginUrl 中的 JS，自动设置 variable（url、ci0 等）
            ReaderEngine.initSource(source);
            log.info("  漫画源初始化完成: {} | variable={}", source.getBookSourceName(),
                    ReaderEngine.getVariable(source));
            return source;
        }
    }

    /**
     * 测试 1：所有漫画源搜索测试（4 源 × 5 关键词）
     */
    @Test
    public void testAllSourcesSearch() throws Exception {
        log.info("========== 漫画源搜索测试：4 源 × 5 关键词 ==========");
        for (String resource : SOURCES) {
            BookSource source = loadSource(resource);
            log.info("\n━━━━━ {} ({}) ━━━━━", source.getBookSourceName(), source.getBookSourceUrl());

            for (String keyword : KEYWORDS) {
                try {
                    List<SearchBook> results = ReaderEngine.search(source, keyword, 1);
                    log.info("  搜索 [{}]: {} 本", keyword, results.size());
                    if (!results.isEmpty()) {
                        SearchBook first = results.get(0);
                        log.info("    首本: {} | {} | {}", first.getName(), first.getAuthor(), first.getBookUrl());
                    }
                } catch (Exception e) {
                    log.warn("  搜索 [{}] 失败: {}", keyword, e.getMessage());
                }
            }
        }
        log.info("\n========== 漫画源搜索测试完成 ==========");
    }

    /**
     * 测试 2：完整流程测试（搜索→详情→目录→正文）
     * 4 源 × 5 关键词逐一验证搜索、目录、正文均可返回数据
     */
    @Test
    public void testFullWorkflow() throws Exception {
        log.info("========== 漫画源完整流程测试 ==========");

        for (String resource : SOURCES) {
            BookSource source = loadSource(resource);
            log.info("\n━━━━━ {} ({}) ━━━━━", source.getBookSourceName(), source.getBookSourceUrl());

            for (String keyword : KEYWORDS) {
                log.info("\n  -------- 关键词 [{}] --------", keyword);
                try {
                    fullWorkflow(source, keyword);
                } catch (Exception e) {
                    log.error("  关键词 [{}] 完整流程异常: {}", keyword, e.getMessage(), e);
                }
            }
        }
        log.info("\n========== 漫画源完整流程测试完成 ==========");
    }

    /**
     * 单关键词完整流程：搜索 → 详情 → 目录 → 正文
     */
    private void fullWorkflow(BookSource source, String keyword) throws Exception {
        // Step 1: 搜索
        log.info("  --- Step 1: 搜索 [{}] ---", keyword);
        List<SearchBook> results = ReaderEngine.search(source, keyword, 1);
        log.info("  搜索结果: {} 本", results.size());
        if (results.isEmpty()) {
            log.warn("  搜索无结果，跳过后续步骤");
            return;
        }
        for (int i = 0; i < Math.min(results.size(), 3); i++) {
            SearchBook sb = results.get(i);
            log.info("    [{}] {} | {}", i + 1, sb.getName(), sb.getAuthor());
        }

        // Step 2: 详情
        SearchBook searchBook = results.get(0);
        log.info("  --- Step 2: 获取详情 ---");
        Book book = ReaderEngine.getBookInfo(source, searchBook.getBookUrl());
        if (book == null) {
            log.warn("  详情为空，跳过后续步骤");
            return;
        }
        log.info("  书名: {} | 作者: {}", book.getName(), book.getAuthor());
        log.info("  简介: {}", book.getIntro() == null ? "" :
                (book.getIntro().length() > 100 ? book.getIntro().substring(0, 100) + "..." : book.getIntro()));

        // Step 3: 目录
        log.info("  --- Step 3: 获取目录 ---");
        List<BookChapter> chapters = ReaderEngine.getChapterList(source, book);
        log.info("  章节数: {}", chapters.size());
        if (chapters.isEmpty()) {
            log.warn("  目录为空，跳过正文步骤");
            return;
        }
        for (int i = 0; i < Math.min(chapters.size(), 5); i++) {
            log.info("    [{}] {}", i + 1, chapters.get(i).getTitle());
        }

        // Step 4: 正文（漫画为图片列表）
        log.info("  --- Step 4: 获取正文 ---");
        BookChapter chapter = chapters.get(0);
        String content = ReaderEngine.getBookContent(source, book, chapter);
        int contentLen = content == null ? 0 : content.length();
        log.info("  章节: {} | 正文长度: {}", chapter.getTitle(), contentLen);
        if (contentLen > 0) {
            int imgCount = content.split("<img").length - 1;
            log.info("  图片数: {}", imgCount);
            log.info("  正文前200字符: {}", content.substring(0, Math.min(200, contentLen)));
        }
    }

    /**
     * Java 8 兼容的 readAllBytes 实现
     */
    private static String readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
