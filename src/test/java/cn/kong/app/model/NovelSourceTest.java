package cn.kong.app.model;

import com.google.gson.Gson;
import cn.kong.app.data.entities.Book;
import cn.kong.app.data.entities.BookChapter;
import cn.kong.app.data.entities.BookSource;
import cn.kong.app.data.entities.SearchBook;
import cn.kong.app.model.webBook.WebBook;
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
 * 4 个书源 × 5 个关键词端到端测试：
 * 搜索 → 详情 → 目录 → 正文
 * <p>
 * 书源：八零小说、独步小说、猫眼看书、七猫小说
 * 关键词：斗破苍穹、神通者、夜的命名术、捞尸人、谁让他修仙的
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
     * 加载书源（兼容 JSON 数组和单对象两种格式）
     */
    private BookSource loadSource(String resource) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(is, "书源文件未找到: " + resource);
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            // 尝试解析为数组，失败则解析为单对象
            if (json.trim().startsWith("[")) {
                BookSource[] sources = gson.fromJson(json, BookSource[].class);
                assertNotNull(sources, "书源解析失败: " + resource);
                assertTrue(sources.length > 0, "书源列表为空: " + resource);
                return sources[0];
            } else {
                BookSource source = gson.fromJson(json, BookSource.class);
                assertNotNull(source, "书源解析失败: " + resource);
                return source;
            }
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
            WebBook webBook = new WebBook(source);
            log.info("\n━━━━━ {} ({}) ━━━━━", source.getBookSourceName(), source.getBookSourceUrl());

            for (String keyword : KEYWORDS) {
                try {
                    List<SearchBook> results = webBook.searchBook(keyword, 1);
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
     * 对每个书源取第一个关键词「斗破苍穹」进行完整流程验证
     */
    @Test
    public void testFullWorkflow() throws Exception {
        log.info("========== 书源完整流程测试 ==========");
        String testKeyword = "斗破苍穹";

        for (String resource : SOURCES) {
            BookSource source = loadSource(resource);
            WebBook webBook = new WebBook(source);
            log.info("\n━━━━━ {} ({}) ━━━━━", source.getBookSourceName(), source.getBookSourceUrl());

            try {
                // Step 1: 搜索
                log.info("\n  --- Step 1: 搜索 [{}] ---", testKeyword);
                List<SearchBook> results = webBook.searchBook(testKeyword, 1);
                log.info("  搜索结果: {} 本", results.size());
                if (results.isEmpty()) {
                    log.warn("  搜索无结果，跳过后续步骤");
                    continue;
                }
                for (int i = 0; i < Math.min(results.size(), 3); i++) {
                    SearchBook sb = results.get(i);
                    log.info("    [{}] {} | {}", i + 1, sb.getName(), sb.getAuthor());
                }

                // Step 2: 详情
                SearchBook searchBook = results.get(0);
                log.info("\n  --- Step 2: 获取详情 ---");
                Book book = webBook.getBookInfo(searchBook.getBookUrl());
                if (book == null) {
                    log.warn("  详情为空，跳过后续步骤");
                    continue;
                }
                log.info("  书名: {} | 作者: {}", book.getName(), book.getAuthor());
                log.info("  简介: {}", book.getIntro() == null ? "" :
                        (book.getIntro().length() > 100 ? book.getIntro().substring(0, 100) + "..." : book.getIntro()));

                // Step 3: 目录
                log.info("\n  --- Step 3: 获取目录 ---");
                List<BookChapter> chapters = webBook.getChapterList(book);
                log.info("  章节数: {}", chapters.size());
                if (chapters.isEmpty()) {
                    log.warn("  目录为空，跳过正文步骤");
                    continue;
                }
                for (int i = 0; i < Math.min(chapters.size(), 5); i++) {
                    log.info("    [{}] {}", i + 1, chapters.get(i).getTitle());
                }

                // Step 4: 正文
                log.info("\n  --- Step 4: 获取正文 ---");
                BookChapter chapter = chapters.get(0);
                String content = webBook.getBookContent(book, chapter);
                int contentLen = content == null ? 0 : content.length();
                log.info("  章节: {} | 正文长度: {}", chapter.getTitle(), contentLen);
                if (contentLen > 0) {
                    log.info("  正文前200字: {}", content.substring(0, Math.min(200, contentLen)));
                }
            } catch (Exception e) {
                log.error("  完整流程异常: {}", e.getMessage(), e);
            }
        }
        log.info("\n========== 书源完整流程测试完成 ==========");
    }

    /**
     * 测试 3：多关键词完整流程测试
     * 对第一个书源（八零小说）测试所有 5 个关键词的搜索 + 详情 + 目录
     */
    @Test
    public void testMultiKeywordWorkflow() throws Exception {
        log.info("========== 多关键词流程测试（八零小说） ==========");
        BookSource source = loadSource("novel_80.json");
        WebBook webBook = new WebBook(source);
        log.info("书源: {} ({})", source.getBookSourceName(), source.getBookSourceUrl());

        for (String keyword : KEYWORDS) {
            log.info("\n━━━━━ 关键词: {} ━━━━━", keyword);
            try {
                List<SearchBook> results = webBook.searchBook(keyword, 1);
                log.info("  搜索结果: {} 本", results.size());
                if (results.isEmpty()) {
                    log.warn("  无搜索结果");
                    continue;
                }
                SearchBook first = results.get(0);
                log.info("  首本: {} | {}", first.getName(), first.getAuthor());

                Book book = webBook.getBookInfo(first.getBookUrl());
                if (book != null) {
                    log.info("  详情: {} | {}", book.getName(), book.getAuthor());
                    List<BookChapter> chapters = webBook.getChapterList(book);
                    log.info("  目录: {} 章", chapters.size());
                    if (!chapters.isEmpty()) {
                        log.info("  首章: {}", chapters.get(0).getTitle());

                        String content = webBook.getBookContent(book, chapters.get(0));
                        int len = content == null ? 0 : content.length();
                        log.info("  正文长度: {}", len);
                        if (len > 100) {
                            log.info("  正文前100字: {}", content.substring(0, 100));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("  关键词 [{}] 异常: {}", keyword, e.getMessage());
            }
        }
        log.info("\n========== 多关键词流程测试完成 ==========");
    }
}
