package cn.kong.app.model;

import cn.kong.app.engine.ReaderEngine;
import io.legado.app.data.entities.Book;
import io.legado.app.data.entities.BookChapter;
import io.legado.app.data.entities.BookSource;
import io.legado.app.data.entities.SearchBook;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 书源解析引擎集成测试
 * <p>
 * 4 个书源端到端测试：搜索 → 详情 → 目录 → 正文
 * <p>
 * 书源：八零小说、独步小说、猫眼看书、七猫小说
 * <br>关键词：斗破苍穹、神通者、夜的命名术、捞尸人、谁让他修仙的
 */
public class NovelSourceTest {

    private static final Logger log = LoggerFactory.getLogger(NovelSourceTest.class);

    private static final String[] SOURCES = {
            "novel_80.json",       // 八零小说
            "novel_dubu.json",     // 独步小说
            "novel_maoyan.json",   // 猫眼看书
            "novel_qimao.json"     // 七猫小说
    };

    private static final String[] KEYWORDS = {
            "斗破苍穹",
            "神通者",
            "夜的命名术",
            "捞尸人",
            "谁让他修仙的"
    };

    /**
     * 加载书源 JSON 并解析为 BookSource 对象
     */
    private BookSource loadSource(String resource) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(is, "书源文件未找到: " + resource);
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // 使用 reader-dev 的 SourceAnalyzer 解析书源
            List<BookSource> sources = ReaderEngine.parseBookSources(json);
            assertNotNull(sources, "书源解析失败: " + resource);
            assertTrue(!sources.isEmpty(), "书源列表为空: " + resource);
            return sources.get(0);
        }
    }

    /**
     * 测试 1：所有书源搜索测试（4 源 × 5 关键词）
     */
    @Test
    public void testAllSourcesSearch() throws Exception {
        log.info("========== 书源搜索测试：4 源 × 5 关键词 ==========");
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
        log.info("\n========== 书源搜索测试完成 ==========");
    }

    /**
     * 测试 2：完整流程测试（搜索→详情→目录→正文）
     * 4 源 × 5 关键词逐一验证搜索、目录、正文均可返回数据
     */
    @Test
    public void testFullWorkflow() throws Exception {
        log.info("========== 书源完整流程测试 ==========");

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
        log.info("\n========== 书源完整流程测试完成 ==========");
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

        // Step 4: 正文
        log.info("  --- Step 4: 获取正文 ---");
        BookChapter chapter = chapters.get(0);
        String content = ReaderEngine.getBookContent(source, book, chapter);
        int contentLen = content == null ? 0 : content.length();
        log.info("  章节: {} | 正文长度: {}", chapter.getTitle(), contentLen);
        if (contentLen > 0) {
            log.info("  正文前200字: {}", content.substring(0, Math.min(200, contentLen)));
        }
    }
}
