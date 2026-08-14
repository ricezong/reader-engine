package cn.kong.app.model;

import com.google.gson.Gson;
import cn.kong.app.data.entities.Book;
import cn.kong.app.data.entities.BookChapter;
import cn.kong.app.data.entities.BookSource;
import cn.kong.app.data.entities.SearchBook;
import cn.kong.app.model.webBook.BookContent;
import cn.kong.app.model.webBook.WebBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 书源解析引擎集成测试
 * <p>
 * 使用「八零小说」书源进行端到端测试：
 * 搜索 → 详情 → 目录 → 正文
 */
public class WebBookIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(WebBookIntegrationTest.class);

    private BookSource bookSource;
    private WebBook webBook;

    @BeforeEach
    public void setUp() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("novel_80.json")) {
            assertNotNull(is, "书源文件未找到");
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            BookSource[] sources = gson.fromJson(json, BookSource[].class);
            assertNotNull(sources, "书源解析失败");
            assertTrue(sources.length > 0, "书源列表为空");
            this.bookSource = sources[0];
            this.webBook = new WebBook(bookSource);
        }
        log.info("书源加载完成: {} ({})", bookSource.getBookSourceName(), bookSource.getBookSourceUrl());
    }

    /**
     * 测试 1：搜索书籍
     */
    @Test
    public void testSearchBook() {
        log.info("========== 测试 1：搜索书籍 ==========");
        List<SearchBook> results = webBook.searchBook("大主宰", 1);
        log.info("搜索结果数量: {}", results.size());
        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            SearchBook sb = results.get(i);
            log.info("  [{}] {} | {} | {}", i + 1, sb.getName(), sb.getAuthor(), sb.getBookUrl());
        }
        assertTrue(results.size() > 0, "搜索结果不应为空");
    }

    /**
     * 测试 2：获取书籍详情
     */
    @Test
    public void testGetBookInfo() {
        log.info("========== 测试 2：获取书籍详情 ==========");
        List<SearchBook> results = webBook.searchBook("大主宰", 1);
        assertTrue(results.size() > 0, "搜索结果不应为空");
        SearchBook searchBook = results.get(0);
        log.info("获取详情: {}", searchBook.getBookUrl());
        Book book = webBook.getBookInfo(searchBook.getBookUrl());
        assertNotNull(book, "书籍详情不应为空");
        log.info("书名: {}", book.getName());
        log.info("作者: {}", book.getAuthor());
        log.info("分类: {}", book.getKind());
        log.info("简介: {}", book.getIntro() == null ? "" :
            (book.getIntro().length() > 100 ? book.getIntro().substring(0, 100) + "..." : book.getIntro()));
        log.info("封面: {}", book.getCoverUrl());
        log.info("目录URL: {}", book.getTocUrl());
    }

    /**
     * 测试 3：获取章节目录
     */
    @Test
    public void testGetChapterList() {
        log.info("========== 测试 3：获取章节目录 ==========");
        List<SearchBook> results = webBook.searchBook("大主宰", 1);
        assertTrue(results.size() > 0, "搜索结果不应为空");
        SearchBook searchBook = results.get(0);
        Book book = webBook.getBookInfo(searchBook.getBookUrl());
        assertNotNull(book, "书籍详情不应为空");
        List<BookChapter> chapters = webBook.getChapterList(book);
        log.info("章节数量: {}", chapters.size());
        for (int i = 0; i < Math.min(chapters.size(), 10); i++) {
            log.info("  [{}] {}", i + 1, chapters.get(i).getTitle());
        }
        assertTrue(chapters.size() > 0, "章节列表不应为空");
    }

    /**
     * 测试 4：获取正文内容
     * 由于章节正文页面 www.qiushu.info 在测试环境网络不可达，
     * 使用本地模拟 HTML 验证正文解析规则
     */
    @Test
    public void testGetBookContent() {
        log.info("========== 测试 4：获取正文内容 ==========");
        // 使用本地模拟 HTML 验证正文解析规则
        String mockContentHtml = "<html><body><div id=\"content\">"
            + "<p>第1章 天帝玉皇决</p>"
            + "<p>天帝玉皇决是一部古老的修仙功法，传说由上古天帝所创。</p>"
            + "<p>主角林凡得到了这部功法，开始了他的修仙之路。</p>"
            + "<p>求书网 www.qiushu.info 提供更多小说下载</p>"
            + "</div></body></html>";

        BookChapter mockChapter = new BookChapter("http://mock/chapter", "第1章 天帝玉皇决");
        String content = BookContent.parse(mockContentHtml, bookSource, mockChapter);
        log.info("正文长度: {}", content == null ? 0 : content.length());
        if (content != null && content.length() > 200) {
            log.info("正文前200字: {}", content.substring(0, 200));
            log.info("正文后100字: {}", content.substring(content.length() - 100));
        } else {
            log.info("正文内容: {}", content);
        }
        assertTrue(content != null && !content.isEmpty(), "正文不应为空");
        assertTrue(!content.contains("求书网"), "正文应过滤掉求书网广告");
    }

    /**
     * 测试 5：完整流程（搜索→详情→目录→正文）
     */
    @Test
    public void testFullWorkflow() {
        log.info("========== 测试 5：完整流程 ==========");

        log.info("\n--- Step 1: 搜索 ---");
        List<SearchBook> results = webBook.searchBook("大主宰", 1);
        log.info("搜索结果: {} 本", results.size());
        assertTrue(results.size() > 0, "搜索结果不应为空");

        log.info("\n--- Step 2: 详情 ---");
        SearchBook searchBook = results.get(0);
        Book book = webBook.getBookInfo(searchBook.getBookUrl());
        assertNotNull(book, "书籍详情不应为空");
        log.info("书名: {} | 作者: {}", book.getName(), book.getAuthor());

        log.info("\n--- Step 3: 目录 ---");
        List<BookChapter> chapters = webBook.getChapterList(book);
        log.info("章节数: {}", chapters.size());
        assertTrue(chapters.size() > 0, "章节列表不应为空");

        log.info("\n--- Step 4: 正文 ---");
        // 由于章节正文页面 www.qiushu.info 在测试环境网络不可达，
        // 使用本地模拟 HTML 验证正文解析规则
        String mockContentHtml = "<html><body><div id=\"content\">"
            + "<p>第1章 天帝玉皇决</p>"
            + "<p>天帝玉皇决是一部古老的修仙功法，传说由上古天帝所创。</p>"
            + "<p>主角林凡得到了这部功法，开始了他的修仙之路。</p>"
            + "<p>求书网 www.qiushu.info 提供更多小说下载</p>"
            + "</div></body></html>";

        BookChapter mockChapter = new BookChapter("http://mock/chapter", "第1章 天帝玉皇决");
        String content = BookContent.parse(mockContentHtml, bookSource, mockChapter);
        log.info("章节: {} | 正文长度: {}", mockChapter.getTitle(), content == null ? 0 : content.length());
        if (content != null && content.length() > 200) {
            log.info("正文前200字: {}", content.substring(0, 200));
        } else {
            log.info("正文内容: {}", content);
        }
        assertTrue(content != null && !content.isEmpty(), "正文不应为空");
        // 验证广告过滤生效
        assertTrue(!content.contains("求书网"), "正文应过滤掉求书网广告");

        log.info("\n========== 完整流程测试通过 ==========");
    }
}
