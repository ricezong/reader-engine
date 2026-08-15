package cn.kong.app.engine;

import cn.kong.app.engine.dto.BookDetail;
import cn.kong.app.engine.dto.ChapterContent;
import cn.kong.app.engine.dto.ChapterInfo;
import cn.kong.app.engine.dto.SearchResult;
import cn.kong.app.engine.dto.SourceInfo;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReaderService 测试（通用 API）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReaderServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ReaderServiceTest.class);

    private static ReaderService service;

    @BeforeAll
    static void setUp() {
        service = ReaderService.getInstance();
    }

    @Test
    @Order(1)
    @DisplayName("内置源加载")
    public void testBuiltinSourcesLoaded() {
        log.info("========== 测试 1: 内置源加载 ==========");

        List<SourceInfo> all = service.listAllSources();
        assertEquals(8, all.size());
        for (SourceInfo s : all) {
            log.info("  source={} | name={} | type={}", s.getSource(), s.getName(), s.getTypeDesc());
        }

        List<SourceInfo> novels = service.listNovelSources();
        assertEquals(4, novels.size());
        log.info("小说源: {}", novels);

        List<SourceInfo> comics = service.listComicSources();
        assertEquals(4, comics.size());
        log.info("漫画源: {}", comics);
    }

    @Test
    @Order(2)
    @DisplayName("搜索小说")
    public void testSearchNovel() {
        log.info("========== 测试 2: 搜索小说 ==========");
        List<SearchResult> results = service.searchNovel("斗破苍穹");
        log.info("结果: {} 本", results.size());
        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            SearchResult r = results.get(i);
            log.info("  [{}] {} | {} | source={}", i + 1, r.getName(), r.getAuthor(), r.getSource());
        }
        assertFalse(results.isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("搜索漫画")
    public void testSearchComic() {
        log.info("========== 测试 3: 搜索漫画 ==========");
        List<SearchResult> results = service.searchComic("哑舍");
        log.info("结果: {} 本", results.size());
        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            SearchResult r = results.get(i);
            log.info("  [{}] {} | {} | source={}", i + 1, r.getName(), r.getAuthor(), r.getSource());
        }
        assertFalse(results.isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("按作者搜索")
    public void testSearchByAuthor() {
        log.info("========== 测试 4: 按作者搜索 ==========");
        List<SearchResult> results = service.searchByAuthor("天蚕土豆");
        log.info("结果: {} 本", results.size());
        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            SearchResult r = results.get(i);
            log.info("  [{}] {} | {}", i + 1, r.getName(), r.getAuthor());
        }
    }

    @Test
    @Order(5)
    @DisplayName("搜索全部源")
    public void testSearchAll() {
        log.info("========== 测试 5: 搜索全部源 ==========");
        List<SearchResult> results = service.search("斗破苍穹");
        log.info("全部结果: {} 本", results.size());
        Map<String, Integer> stat = new java.util.LinkedHashMap<>();
        for (SearchResult r : results) {
            stat.merge(r.getSourceName(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : stat.entrySet()) {
            log.info("  {} : {} 本", e.getKey(), e.getValue());
        }
        assertFalse(results.isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("获取详情")
    public void testGetBookDetail() {
        log.info("========== 测试 6: 获取详情 ==========");
        List<SearchResult> results = service.searchNovel("斗破苍穹");
        assertFalse(results.isEmpty());

        SearchResult r = results.get(0);
        BookDetail detail = service.getBookDetail(r.getBookUrl(), r.getSource());
        assertNotNull(detail);
        log.info("书名: {} | 作者: {}", detail.getName(), detail.getAuthor());
        log.info("简介: {}", detail.getIntro() == null ? "" :
                (detail.getIntro().length() > 100 ? detail.getIntro().substring(0, 100) + "..." : detail.getIntro()));
    }

    @Test
    @Order(7)
    @DisplayName("获取目录")
    public void testGetChapterList() {
        log.info("========== 测试 7: 获取目录 ==========");
        List<SearchResult> results = service.searchNovel("斗破苍穹");
        assertFalse(results.isEmpty());

        SearchResult r = results.get(0);
        List<ChapterInfo> chapters = service.getChapterList(r.getBookUrl(), r.getSource());
        log.info("章节数: {}", chapters.size());
        assertTrue(!chapters.isEmpty());
        for (int i = 0; i < Math.min(chapters.size(), 5); i++) {
            log.info("  [{}] {}", chapters.get(i).getIndex(), chapters.get(i).getTitle());
        }
    }

    @Test
    @Order(8)
    @DisplayName("获取正文")
    public void testGetContent() {
        log.info("========== 测试 8: 获取正文 ==========");
        List<SearchResult> results = service.searchNovel("斗破苍穹");
        assertFalse(results.isEmpty());

        SearchResult r = results.get(0);
        String content = service.getContent(r.getBookUrl(), r.getSource(), 0);
        int len = content == null ? 0 : content.length();
        log.info("第1章正文长度: {}", len);
        if (len > 0) {
            log.info("正文前200字: {}", content.substring(0, Math.min(200, len)));
        }
    }

    @Test
    @Order(9)
    @DisplayName("批量下载（前3章）")
    public void testBatchDownload() {
        log.info("========== 测试 9: 批量下载 ==========");
        List<SearchResult> results = service.searchNovel("斗破苍穹");
        assertFalse(results.isEmpty());

        SearchResult r = results.get(0);
        List<ChapterContent> contents = service.batchDownload(r.getBookUrl(), r.getSource(), 0, 3);
        assertEquals(3, contents.size());
        for (int i = 0; i < contents.size(); i++) {
            int len = contents.get(i).getContent() == null ? 0 : contents.get(i).getContent().length();
            log.info("  第{}章: 长度 {}", i + 1, len);
        }
    }

    @Test
    @Order(10)
    @DisplayName("漫画完整流程")
    public void testComicWorkflow() {
        log.info("========== 测试 10: 漫画完整流程 ==========");
        List<SearchResult> results = service.searchComic("斗破苍穹");
        if (results.isEmpty()) {
            log.warn("无结果，跳过");
            return;
        }
        SearchResult r = results.get(0);
        log.info("首本: {} | {}", r.getName(), r.getAuthor());

        BookDetail detail = service.getBookDetail(r.getBookUrl(), r.getSource());
        assertNotNull(detail);
        log.info("书名: {}", detail.getName());

        List<ChapterInfo> chapters = service.getChapterList(r.getBookUrl(), r.getSource());
        log.info("章节数: {}", chapters.size());
        if (chapters.isEmpty()) return;

        String content = service.getContent(r.getBookUrl(), r.getSource(), 0);
        int len = content == null ? 0 : content.length();
        int imgCount = content == null ? 0 : content.split("<img").length - 1;
        log.info("第1章: 长度 {} | 图片数 {}", len, imgCount);
    }
}
