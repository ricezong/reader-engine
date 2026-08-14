package cn.kong.app.engine;

import io.legado.app.data.entities.Book;
import io.legado.app.data.entities.BookChapter;
import io.legado.app.data.entities.BookSource;
import io.legado.app.data.entities.SearchBook;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BookSourceManager 管理器测试
 * <p>
 * 验证：
 * 1. 内置源自动加载
 * 2. 列出书源/漫画源
 * 3. 搜索小说/漫画
 * 4. 按作者搜索
 * 5. 获取详情
 * 6. 获取目录
 * 7. 获取正文
 * 8. 批量下载正文
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookSourceManagerTest {

    private static final Logger log = LoggerFactory.getLogger(BookSourceManagerTest.class);

    private static BookSourceManager manager;

    @BeforeAll
    static void setUp() {
        manager = BookSourceManager.getInstance();
    }

    // ==================== 1. 内置源加载 & 列出源 ====================

    @Test
    @Order(1)
    @DisplayName("内置源加载：4 小说 + 4 漫画 = 8 个")
    public void testBuiltinSourcesLoaded() {
        log.info("========== 测试 1: 内置源加载 ==========");
        int total = manager.getSourceCount();
        log.info("已加载书源总数: {}", total);
        assertTrue(total >= 8, "内置源至少 8 个");

        List<BookSource> novels = manager.listNovelSources();
        log.info("小说源 ({}):", novels.size());
        for (BookSource s : novels) {
            log.info("  - {} | {} | type={}", s.getBookSourceName(), s.getBookSourceUrl(), s.getBookSourceType());
        }
        assertEquals(4, novels.size(), "小说源应为 4 个");

        List<BookSource> comics = manager.listComicSources();
        log.info("漫画源 ({}):", comics.size());
        for (BookSource s : comics) {
            log.info("  - {} | {} | type={}", s.getBookSourceName(), s.getBookSourceUrl(), s.getBookSourceType());
            // 漫画源应有 variable（initSource 已执行）
            assertNotNull(manager.getSource(s.getBookSourceUrl()).getVariable(),
                    "漫画源 variable 不应为空: " + s.getBookSourceName());
        }
        assertEquals(4, comics.size(), "漫画源应为 4 个");
    }

    // ==================== 2. 搜索小说 ====================

    @Test
    @Order(2)
    @DisplayName("搜索小说：跨所有小说源聚合搜索")
    public void testSearchNovel() {
        log.info("========== 测试 2: 搜索小说 ==========");
        String keyword = "斗破苍穹";
        List<SearchBook> results = manager.searchNovel(keyword);
        log.info("搜索 [{}] 结果: {} 本", keyword, results.size());
        for (int i = 0; i < Math.min(results.size(), 10); i++) {
            SearchBook sb = results.get(i);
            log.info("  [{}] {} | {} | {}", i + 1, sb.getName(), sb.getAuthor(), sb.getOriginName());
        }
        assertTrue(!results.isEmpty(), "搜索结果不应为空");
    }

    // ==================== 3. 搜索漫画 ====================

    @Test
    @Order(3)
    @DisplayName("搜索漫画：跨所有漫画源聚合搜索")
    public void testSearchComic() {
        log.info("========== 测试 3: 搜索漫画 ==========");
        String keyword = "哑舍";
        List<SearchBook> results = manager.searchComic(keyword);
        log.info("搜索 [{}] 结果: {} 本", keyword, results.size());
        for (int i = 0; i < Math.min(results.size(), 10); i++) {
            SearchBook sb = results.get(i);
            log.info("  [{}] {} | {} | {}", i + 1, sb.getName(), sb.getAuthor(), sb.getOriginName());
        }
        assertTrue(!results.isEmpty(), "搜索结果不应为空");
    }

    // ==================== 4. 按作者搜索 ====================

    @Test
    @Order(4)
    @DisplayName("按作者搜索小说")
    public void testSearchNovelByAuthor() {
        log.info("========== 测试 4: 按作者搜索 ==========");
        String author = "天蚕土豆";
        List<SearchBook> results = manager.searchNovelByAuthor(author);
        log.info("按作者 [{}] 搜索结果: {} 本", author, results.size());
        for (int i = 0; i < Math.min(results.size(), 10); i++) {
            SearchBook sb = results.get(i);
            log.info("  [{}] {} | {} | {}", i + 1, sb.getName(), sb.getAuthor(), sb.getOriginName());
        }
        // 结果可能为空（某些源可能搜不到），只验证不报错
    }

    // ==================== 5. 获取详情 ====================

    @Test
    @Order(5)
    @DisplayName("获取书籍详情")
    public void testGetBookInfo() {
        log.info("========== 测试 5: 获取详情 ==========");
        List<SearchBook> results = manager.searchNovel("斗破苍穹");
        assertFalse(results.isEmpty(), "需要先有搜索结果");

        SearchBook sb = results.get(0);
        Book book = manager.getBookInfo(sb);
        assertNotNull(book, "详情不应为空");
        log.info("书名: {} | 作者: {}", book.getName(), book.getAuthor());
        log.info("简介: {}", book.getIntro() == null ? "" :
                (book.getIntro().length() > 100 ? book.getIntro().substring(0, 100) + "..." : book.getIntro()));
        log.info("封面: {}", book.getCoverUrl());
    }

    // ==================== 6. 获取目录 ====================

    @Test
    @Order(6)
    @DisplayName("获取章节目录")
    public void testGetChapterList() {
        log.info("========== 测试 6: 获取目录 ==========");
        List<SearchBook> results = manager.searchNovel("斗破苍穹");
        assertFalse(results.isEmpty());

        Book book = manager.getBookInfo(results.get(0));
        List<BookChapter> chapters = manager.getChapterList(book);
        log.info("章节数: {}", chapters.size());
        assertTrue(!chapters.isEmpty(), "目录不应为空");
        for (int i = 0; i < Math.min(chapters.size(), 5); i++) {
            log.info("  [{}] {}", i + 1, chapters.get(i).getTitle());
        }
    }

    // ==================== 7. 获取正文 ====================

    @Test
    @Order(7)
    @DisplayName("获取章节正文")
    public void testGetBookContent() {
        log.info("========== 测试 7: 获取正文 ==========");
        List<SearchBook> results = manager.searchNovel("斗破苍穹");
        assertFalse(results.isEmpty());

        Book book = manager.getBookInfo(results.get(0));
        List<BookChapter> chapters = manager.getChapterList(book);
        assertFalse(chapters.isEmpty());

        String content = manager.getBookContent(book, chapters.get(0));
        int len = content == null ? 0 : content.length();
        log.info("章节: {} | 正文长度: {}", chapters.get(0).getTitle(), len);
        if (len > 0) {
            log.info("正文前200字: {}", content.substring(0, Math.min(200, len)));
        }
    }

    // ==================== 8. 批量下载正文 ====================

    @Test
    @Order(8)
    @DisplayName("批量下载正文（前 3 章）")
    public void testBatchDownloadContent() {
        log.info("========== 测试 8: 批量下载 ==========");
        List<SearchBook> results = manager.searchNovel("斗破苍穹");
        assertFalse(results.isEmpty());

        Book book = manager.getBookInfo(results.get(0));
        List<BookChapter> chapters = manager.getChapterList(book);
        assertFalse(chapters.isEmpty());

        // 只下载前 3 章
        int count = Math.min(3, chapters.size());
        List<BookChapter> targetChapters = chapters.subList(0, count);
        List<String> contents = manager.batchDownloadContent(book, targetChapters);
        assertEquals(count, contents.size());

        for (int i = 0; i < contents.size(); i++) {
            int len = contents.get(i) == null ? 0 : contents.get(i).length();
            log.info("  章节 [{}]: {} | 长度: {}", i + 1, targetChapters.get(i).getTitle(), len);
        }

        // 测试 Map 形式
        Map<String, String> map = manager.batchDownloadContentAsMap(book, targetChapters);
        log.info("Map 形式下载数量: {}", map.size());
        assertEquals(count, map.size());
    }

    // ==================== 9. 漫画完整流程 ====================

    @Test
    @Order(9)
    @DisplayName("漫画完整流程：搜索→详情→目录→正文")
    public void testComicFullWorkflow() {
        log.info("========== 测试 9: 漫画完整流程 ==========");
        List<SearchBook> results = manager.searchComic("斗破苍穹");
        log.info("搜索结果: {} 本", results.size());
        if (results.isEmpty()) {
            log.warn("无搜索结果，跳过");
            return;
        }

        SearchBook sb = results.get(0);
        log.info("首本: {} | {}", sb.getName(), sb.getAuthor());

        Book book = manager.getBookInfo(sb);
        assertNotNull(book);
        log.info("书名: {} | 作者: {}", book.getName(), book.getAuthor());

        List<BookChapter> chapters = manager.getChapterList(book);
        log.info("章节数: {}", chapters.size());
        if (chapters.isEmpty()) {
            log.warn("目录为空，跳过正文");
            return;
        }

        // 漫画正文为图片列表
        String content = manager.getBookContent(book, chapters.get(0));
        int len = content == null ? 0 : content.length();
        int imgCount = content == null ? 0 : content.split("<img").length - 1;
        log.info("章节: {} | 正文长度: {} | 图片数: {}", chapters.get(0).getTitle(), len, imgCount);
    }

    // ==================== 10. 搜索全部源 ====================

    @Test
    @Order(10)
    @DisplayName("搜索全部源（小说+漫画）")
    public void testSearchAll() {
        log.info("========== 测试 10: 搜索全部源 ==========");
        String keyword = "斗破苍穹";
        List<SearchBook> results = manager.searchAll(keyword);
        log.info("搜索 [{}] 全部结果: {} 本", keyword, results.size());
        // 统计各源结果数
        java.util.Map<String, Integer> stat = new java.util.LinkedHashMap<>();
        for (SearchBook sb : results) {
            stat.merge(sb.getOriginName(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : stat.entrySet()) {
            log.info("  {} : {} 本", entry.getKey(), entry.getValue());
        }
        assertTrue(!results.isEmpty(), "聚合搜索结果不应为空");
    }
}
