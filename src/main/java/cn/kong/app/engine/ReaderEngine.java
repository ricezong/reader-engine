package cn.kong.app.engine;

import io.legado.app.data.entities.Book;
import io.legado.app.data.entities.BookChapter;
import io.legado.app.data.entities.BookSource;
import io.legado.app.data.entities.SearchBook;
import io.legado.app.engine.ReaderEngineBridge;

import java.util.List;

/**
 * ReaderEngine - Java 门面接口
 * <p>
 * 提供同步的 Java API，封装 Kotlin 的 WebBook 协程调用。
 * 宿主项目通过此门面类即可完成书源的解析、初始化、搜索、详情、目录、正文操作。
 * <p>
 * 使用方式：
 * <pre>
 * // 1. 从 JSON 创建书源
 * BookSource source = ReaderEngine.parseBookSource(jsonString);
 *
 * // 2. 初始化书源（漫画源必须，小说源可选）
 * ReaderEngine.initSource(source);
 *
 * // 3. 搜索
 * List&lt;SearchBook&gt; results = ReaderEngine.search(source, "斗破苍穹", 1);
 *
 * // 4. 获取书籍详情
 * Book book = ReaderEngine.getBookInfo(source, results.get(0).getBookUrl());
 *
 * // 5. 获取目录
 * List&lt;BookChapter&gt; chapters = ReaderEngine.getChapterList(source, book);
 *
 * // 6. 获取正文
 * String content = ReaderEngine.getBookContent(source, book, chapters.get(0));
 * </pre>
 */
public class ReaderEngine {

    static {
        // 预热 JS 引擎
        io.legado.app.constant.AppConst.INSTANCE.getSCRIPT_ENGINE();
        // 预热 HTTP 客户端
        io.legado.app.help.http.HttpHelperKt.getOkHttpClient();
    }

    // ==================== 书源解析 ====================

    /**
     * 从 JSON 字符串解析单个书源
     *
     * @param json 书源 JSON
     * @return BookSource 对象
     * @throws IllegalArgumentException 如果 JSON 格式错误
     */
    public static BookSource parseBookSource(String json) {
        return ReaderEngineBridge.parseBookSource(json);
    }

    /**
     * 从 JSON 字符串解析书源列表
     *
     * @param json 书源数组 JSON
     * @return BookSource 列表
     * @throws IllegalArgumentException 如果 JSON 格式错误
     */
    public static List<BookSource> parseBookSources(String json) {
        return ReaderEngineBridge.parseBookSources(json);
    }


    // ==================== 书源初始化 ====================

    /**
     * 初始化书源
     * <p>
     * 执行 loginUrl 中的 JS 代码，初始化 variable 等配置。
     * 漫画源必须调用此方法，否则 Get('url') 等函数会返回 null 导致报错。
     * 小说源通常不需要调用此方法。
     * <p>
     * 如果书源已有 variable，不会重复初始化。
     *
     * @param bookSource 书源对象
     * @return 初始化后的书源对象（variable 已设置）
     */
    public static BookSource initSource(BookSource bookSource) {
        return ReaderEngineBridge.initSource(bookSource);
    }

    // ==================== Variable 管理 ====================

    /**
     * 设置书源 variable
     * <p>
     * 用于手动设置书源变量。漫画源的 url、ci0 等配置通过此方法设置。
     * 通常配合 initSource 使用，也可手动设置。
     *
     * @param bookSource 书源对象
     * @param variable   variable JSON 字符串，如 {"url":"https://...","ci0":"0"}
     */
    public static void setVariable(BookSource bookSource, String variable) {
        ReaderEngineBridge.setVariable(bookSource, variable);
    }

    /**
     * 获取书源 variable
     *
     * @param bookSource 书源对象
     * @return variable JSON 字符串，未设置时返回 null
     */
    public static String getVariable(BookSource bookSource) {
        return ReaderEngineBridge.getVariable(bookSource);
    }

    // ==================== JS 执行 ====================

    /**
     * 执行书源 JS 代码
     * <p>
     * 在书源上下文中执行 JS，可访问 source/java/cache/cookie 等对象。
     * jsLib 会自动拼接到 JS 代码前面。
     *
     * @param bookSource 书源对象
     * @param jsStr      JS 代码
     * @return JS 执行结果
     */
    public static Object evalJS(BookSource bookSource, String jsStr) {
        return ReaderEngineBridge.evalJS(bookSource, jsStr);
    }

    // ==================== 搜索 ====================

    /**
     * 搜索书籍
     *
     * @param bookSource 书源
     * @param key        搜索关键词
     * @param page       页码（从 1 开始）
     * @return 搜索结果列表
     */
    public static List<SearchBook> search(BookSource bookSource, String key, Integer page) {
        return ReaderEngineBridge.search(bookSource, key, page);
    }

    /**
     * 搜索书籍（默认第 1 页）
     */
    public static List<SearchBook> search(BookSource bookSource, String key) {
        return search(bookSource, key, 1);
    }

    // ==================== 发现 ====================

    /**
     * 发现书籍
     *
     * @param bookSource 书源
     * @param url        发现页 URL
     * @param page       页码
     * @return 搜索结果列表
     */
    public static List<SearchBook> explore(BookSource bookSource, String url, Integer page) {
        return ReaderEngineBridge.explore(bookSource, url, page);
    }

    /**
     * 发现书籍（默认第 1 页）
     */
    public static List<SearchBook> explore(BookSource bookSource, String url) {
        return explore(bookSource, url, 1);
    }

    // ==================== 书籍详情 ====================

    /**
     * 获取书籍详情
     *
     * @param bookSource 书源
     * @param bookUrl    书籍详情页 URL
     * @return Book 对象（包含详情信息）
     */
    public static Book getBookInfo(BookSource bookSource, String bookUrl) {
        return ReaderEngineBridge.getBookInfo(bookSource, bookUrl, true);
    }

    /**
     * 获取书籍详情
     *
     * @param bookSource 书源
     * @param bookUrl    书籍详情页 URL
     * @param canReName  是否允许书源重命名书籍
     * @return Book 对象
     */
    public static Book getBookInfo(BookSource bookSource, String bookUrl, boolean canReName) {
        return ReaderEngineBridge.getBookInfo(bookSource, bookUrl, canReName);
    }

    /**
     * 获取书籍详情（基于已有的 Book 对象）
     *
     * @param bookSource 书源
     * @param book       已有的 Book 对象
     * @return Book 对象（包含详情信息）
     */
    public static Book getBookInfo(BookSource bookSource, Book book) {
        return ReaderEngineBridge.getBookInfo(bookSource, book, true);
    }

    /**
     * 获取书籍详情（基于已有的 Book 对象）
     *
     * @param bookSource 书源
     * @param book       已有的 Book 对象
     * @param canReName  是否允许书源重命名书籍
     * @return Book 对象
     */
    public static Book getBookInfo(BookSource bookSource, Book book, boolean canReName) {
        return ReaderEngineBridge.getBookInfo(bookSource, book, canReName);
    }

    // ==================== 章节目录 ====================

    /**
     * 获取章节目录
     *
     * @param bookSource 书源
     * @param book       书籍对象（需包含 bookUrl 和 tocUrl）
     * @return 章节列表
     */
    public static List<BookChapter> getChapterList(BookSource bookSource, Book book) {
        return ReaderEngineBridge.getChapterList(bookSource, book);
    }

    // ==================== 章节正文 ====================

    /**
     * 获取章节正文
     *
     * @param bookSource  书源
     * @param book        书籍对象
     * @param bookChapter 章节对象
     * @return 正文内容字符串
     */
    public static String getBookContent(BookSource bookSource, Book book, BookChapter bookChapter) {
        return ReaderEngineBridge.getBookContent(bookSource, book, bookChapter, null);
    }

    /**
     * 获取章节正文（带下一章 URL）
     *
     * @param bookSource    书源
     * @param book          书籍对象
     * @param bookChapter   章节对象
     * @param nextChapterUrl 下一章 URL（用于某些书源正文翻页拼接）
     * @return 正文内容字符串
     */
    public static String getBookContent(BookSource bookSource, Book book, BookChapter bookChapter, String nextChapterUrl) {
        return ReaderEngineBridge.getBookContent(bookSource, book, bookChapter, nextChapterUrl);
    }
}
