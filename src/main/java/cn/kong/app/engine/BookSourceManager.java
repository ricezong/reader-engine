package cn.kong.app.engine;

import io.legado.app.data.entities.Book;
import io.legado.app.data.entities.BookChapter;
import io.legado.app.data.entities.BookSource;
import io.legado.app.data.entities.SearchBook;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * BookSourceManager - 书源管理器
 * <p>
 * 在 {@link ReaderEngine} 基础上封装的高层 API，提供：
 * <ol>
 *   <li>内置源管理（4 个小说源 + 4 个漫画源，打包在 jar 中）</li>
 *   <li>导入外部书源 / 漫画源</li>
 *   <li>列出所有书源 / 漫画源</li>
 *   <li>聚合搜索（跨所有源搜索小说 / 漫画）</li>
 *   <li>按作者搜索</li>
 *   <li>获取书籍 / 漫画详情</li>
 *   <li>获取章节目录</li>
 *   <li>获取正文</li>
 *   <li>批量下载正文</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 1. 获取管理器实例（自动加载内置源）
 * BookSourceManager manager = BookSourceManager.getInstance();
 *
 * // 2. 列出所有小说源
 * List&lt;BookSource&gt; novelSources = manager.listNovelSources();
 *
 * // 3. 搜索小说
 * List&lt;SearchBook&gt; results = manager.searchNovel("斗破苍穹");
 *
 * // 4. 获取详情
 * Book book = manager.getBookInfo(results.get(0));
 *
 * // 5. 获取目录
 * List&lt;BookChapter&gt; chapters = manager.getChapterList(book);
 *
 * // 6. 批量下载前 10 章
 * List&lt;String&gt; contents = manager.batchDownloadContent(book, chapters.subList(0, 10));
 * </pre>
 *
 * <h3>漫画源使用</h3>
 * <pre>
 * BookSourceManager manager = BookSourceManager.getInstance();
 * List&lt;SearchBook&gt; results = manager.searchComic("哑舍");
 * Book book = manager.getBookInfo(results.get(0));
 * List&lt;BookChapter&gt; chapters = manager.getChapterList(book);
 * List&lt;String&gt; images = manager.batchDownloadContent(book, chapters.subList(0, 5));
 * </pre>
 */
public class BookSourceManager {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookSourceManager.class);

    /** 内置源 JSON 文件目录（classpath 中的路径） */
    private static final String BUILTIN_DIR = "/builtin/";

    /** 内置小说源文件列表 */
    private static final String[] BUILTIN_NOVEL_FILES = {
            "novel_80.json",       // 八零小说
            "novel_dubu.json",     // 独步小说
            "novel_maoyan.json",   // 猫眼看书
            "novel_qimao.json"     // 七猫小说
    };

    /** 内置漫画源文件列表 */
    private static final String[] BUILTIN_COMIC_FILES = {
            "comic_godamanga.json",  // G站漫画
            "comic_manhuatai.json",  // 漫画台
            "comic_rumanhua.json",   // 如漫画
            "comic_zaimanhua.json"   // 再漫画
    };

    /** bookSourceType: 0 = 小说, 2 = 漫画 */
    private static final int TYPE_NOVEL = 0;
    private static final int TYPE_COMIC = 2;

    /** 单例 */
    private static volatile BookSourceManager instance;

    /**
     * 所有已注册的书源，按 bookSourceUrl 去重。
     * key = bookSourceUrl, value = BookSource
     */
    private final ConcurrentHashMap<String, BookSource> sourceMap = new ConcurrentHashMap<>();

    private BookSourceManager() {
        loadBuiltinSources();
    }

    /**
     * 获取 BookSourceManager 单例
     *
     * @return BookSourceManager 实例（已加载内置源）
     */
    public static BookSourceManager getInstance() {
        if (instance == null) {
            synchronized (BookSourceManager.class) {
                if (instance == null) {
                    instance = new BookSourceManager();
                }
            }
        }
        return instance;
    }

    // ==================== 1. 导入书源 / 漫画源 ====================

    /**
     * 从 JSON 字符串导入书源（支持单个或数组格式）
     * <p>
     * 导入后自动调用 {@link ReaderEngine#initSource(BookSource)} 初始化（漫画源必须）。
     * 如果 bookSourceUrl 已存在则覆盖更新。
     *
     * @param json 书源 JSON 字符串（单个对象或数组）
     * @return 导入的书源列表
     */
    public List<BookSource> importSources(String json) {
        List<BookSource> sources;
        try {
            sources = ReaderEngine.parseBookSources(json);
        } catch (Exception e) {
            // 可能是单个书源 JSON
            try {
                BookSource source = ReaderEngine.parseBookSource(json);
                sources = new ArrayList<>();
                sources.add(source);
            } catch (Exception e2) {
                throw new IllegalArgumentException("解析书源 JSON 失败", e2);
            }
        }
        for (BookSource source : sources) {
            // 初始化书源（漫画源会执行 loginUrl JS 设置 variable）
            ReaderEngine.initSource(source);
            sourceMap.put(source.getBookSourceUrl(), source);
            log.info("导入书源: {} ({})", source.getBookSourceName(), source.getBookSourceUrl());
        }
        return sources;
    }

    /**
     * 从输入流导入书源
     *
     * @param inputStream 书源 JSON 输入流
     * @return 导入的书源列表
     */
    public List<BookSource> importSources(InputStream inputStream) {
        try {
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return importSources(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("读取书源流失败", e);
        }
    }

    /**
     * 导入单个书源对象
     *
     * @param source 书源对象
     */
    public void importSource(BookSource source) {
        ReaderEngine.initSource(source);
        sourceMap.put(source.getBookSourceUrl(), source);
        log.info("导入书源: {} ({})", source.getBookSourceName(), source.getBookSourceUrl());
    }

    /**
     * 移除指定书源
     *
     * @param bookSourceUrl 书源 URL
     */
    public void removeSource(String bookSourceUrl) {
        sourceMap.remove(bookSourceUrl);
        log.info("移除书源: {}", bookSourceUrl);
    }

    /**
     * 清除所有书源（内置源也会被清除，可重新调用 {@link #reloadBuiltinSources()} 恢复）
     */
    public void clearAllSources() {
        sourceMap.clear();
        log.info("已清除所有书源");
    }

    /**
     * 重新加载内置源
     */
    public void reloadBuiltinSources() {
        loadBuiltinSources();
    }

    // ==================== 2. 列出所有书源 / 漫画源 ====================

    /**
     * 列出所有已注册的书源
     *
     * @return 所有书源列表
     */
    public List<BookSource> listAllSources() {
        return new ArrayList<>(sourceMap.values());
    }

    /**
     * 列出所有小说源（bookSourceType = 0）
     *
     * @return 小说源列表
     */
    public List<BookSource> listNovelSources() {
        return sourceMap.values().stream()
                .filter(s -> s.getBookSourceType() == TYPE_NOVEL)
                .collect(Collectors.toList());
    }

    /**
     * 列出所有漫画源（bookSourceType = 2）
     *
     * @return 漫画源列表
     */
    public List<BookSource> listComicSources() {
        return sourceMap.values().stream()
                .filter(s -> s.getBookSourceType() == TYPE_COMIC)
                .collect(Collectors.toList());
    }

    /**
     * 根据书源 URL 获取书源
     *
     * @param bookSourceUrl 书源 URL
     * @return 书源对象，不存在返回 null
     */
    public BookSource getSource(String bookSourceUrl) {
        return sourceMap.get(bookSourceUrl);
    }

    /**
     * 获取已注册书源数量
     *
     * @return 书源总数
     */
    public int getSourceCount() {
        return sourceMap.size();
    }

    // ==================== 3. 搜索小说 / 漫画 ====================

    /**
     * 搜索小说（跨所有小说源聚合搜索）
     * <p>
     * 遍历所有已注册的小说源，返回合并后的搜索结果。
     * 每个源搜索失败不会中断其他源的搜索。
     *
     * @param key 搜索关键词
     * @return 聚合搜索结果列表
     */
    public List<SearchBook> searchNovel(String key) {
        return searchNovel(key, 1);
    }

    /**
     * 搜索小说（跨所有小说源聚合搜索，指定页码）
     *
     * @param key  搜索关键词
     * @param page 页码（从 1 开始）
     * @return 聚合搜索结果列表
     */
    public List<SearchBook> searchNovel(String key, int page) {
        return searchSources(listNovelSources(), key, page);
    }

    /**
     * 搜索漫画（跨所有漫画源聚合搜索）
     *
     * @param key 搜索关键词
     * @return 聚合搜索结果列表
     */
    public List<SearchBook> searchComic(String key) {
        return searchComic(key, 1);
    }

    /**
     * 搜索漫画（跨所有漫画源聚合搜索，指定页码）
     *
     * @param key  搜索关键词
     * @param page 页码（从 1 开始）
     * @return 聚合搜索结果列表
     */
    public List<SearchBook> searchComic(String key, int page) {
        return searchSources(listComicSources(), key, page);
    }

    /**
     * 在所有源中搜索（小说+漫画）
     *
     * @param key 搜索关键词
     * @return 聚合搜索结果列表
     */
    public List<SearchBook> searchAll(String key) {
        return searchAll(key, 1);
    }

    /**
     * 在所有源中搜索（小说+漫画），指定页码
     *
     * @param key  搜索关键词
     * @param page 页码
     * @return 聚合搜索结果列表
     */
    public List<SearchBook> searchAll(String key, int page) {
        return searchSources(listAllSources(), key, page);
    }

    /**
     * 在指定书源中搜索
     *
     * @param source 书源
     * @param key    搜索关键词
     * @return 搜索结果
     */
    public List<SearchBook> search(BookSource source, String key) {
        return ReaderEngine.search(source, key, 1);
    }

    /**
     * 在指定书源中搜索，指定页码
     *
     * @param source 书源
     * @param key    搜索关键词
     * @param page   页码
     * @return 搜索结果
     */
    public List<SearchBook> search(BookSource source, String key, int page) {
        return ReaderEngine.search(source, key, page);
    }

    // ==================== 4. 按作者搜索 ====================

    /**
     * 按作者搜索小说（跨所有小说源）
     * <p>
     * 使用作者名作为关键词搜索，然后过滤匹配作者的结果。
     *
     * @param author 作者名
     * @return 匹配的搜索结果列表
     */
    public List<SearchBook> searchNovelByAuthor(String author) {
        return searchByAuthor(listNovelSources(), author);
    }

    /**
     * 按作者搜索漫画（跨所有漫画源）
     *
     * @param author 作者名
     * @return 匹配的搜索结果列表
     */
    public List<SearchBook> searchComicByAuthor(String author) {
        return searchByAuthor(listComicSources(), author);
    }

    /**
     * 按作者搜索所有源（小说+漫画）
     *
     * @param author 作者名
     * @return 匹配的搜索结果列表
     */
    public List<SearchBook> searchAllByAuthor(String author) {
        return searchByAuthor(listAllSources(), author);
    }

    /**
     * 按作者搜索：在指定源列表中搜索并过滤匹配作者的结果
     */
    private List<SearchBook> searchByAuthor(List<BookSource> sources, String author) {
        List<SearchBook> allResults = new ArrayList<>();
        for (BookSource source : sources) {
            try {
                List<SearchBook> results = ReaderEngine.search(source, author, 1);
                for (SearchBook sb : results) {
                    // 模糊匹配作者名
                    if (sb.getAuthor() != null && sb.getAuthor().contains(author)) {
                        allResults.add(sb);
                    }
                }
            } catch (Exception e) {
                log.warn("按作者搜索失败 [{}] in [{}]: {}", author, source.getBookSourceName(), e.getMessage());
            }
        }
        return allResults;
    }

    // ==================== 5. 获取书籍 / 漫画详情 ====================

    /**
     * 获取书籍详情
     * <p>
     * 根据 SearchBook 的 origin（书源 URL）自动查找对应的书源，
     * 然后获取详情。
     *
     * @param searchBook 搜索结果对象
     * @return Book 对象（包含详情信息）
     */
    public Book getBookInfo(SearchBook searchBook) {
        BookSource source = getSource(searchBook.getOrigin());
        if (source == null) {
            throw new IllegalStateException("找不到对应书源: " + searchBook.getOrigin());
        }
        return ReaderEngine.getBookInfo(source, searchBook.getBookUrl());
    }

    /**
     * 获取书籍详情（指定书源）
     *
     * @param bookSourceUrl 书源 URL
     * @param bookUrl       书籍详情页 URL
     * @return Book 对象
     */
    public Book getBookInfo(String bookSourceUrl, String bookUrl) {
        BookSource source = getSource(bookSourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到对应书源: " + bookSourceUrl);
        }
        return ReaderEngine.getBookInfo(source, bookUrl);
    }

    /**
     * 刷新书籍详情（基于已有 Book 对象）
     *
     * @param book 书籍对象（需包含 origin 字段以查找书源）
     * @return 更新后的 Book 对象
     */
    public Book getBookInfo(Book book) {
        BookSource source = getSource(book.getOrigin());
        if (source == null) {
            throw new IllegalStateException("找不到对应书源: " + book.getOrigin());
        }
        return ReaderEngine.getBookInfo(source, book);
    }

    // ==================== 6. 获取章节目录 ====================

    /**
     * 获取章节目录
     * <p>
     * 根据 Book 的 origin 自动查找对应的书源。
     *
     * @param book 书籍对象（需包含 origin 和 bookUrl）
     * @return 章节列表
     */
    public List<BookChapter> getChapterList(Book book) {
        BookSource source = getSource(book.getOrigin());
        if (source == null) {
            throw new IllegalStateException("找不到对应书源: " + book.getOrigin());
        }
        return ReaderEngine.getChapterList(source, book);
    }

    /**
     * 获取章节目录（指定书源）
     *
     * @param bookSourceUrl 书源 URL
     * @param book          书籍对象
     * @return 章节列表
     */
    public List<BookChapter> getChapterList(String bookSourceUrl, Book book) {
        BookSource source = getSource(bookSourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到对应书源: " + bookSourceUrl);
        }
        return ReaderEngine.getChapterList(source, book);
    }

    // ==================== 7. 获取正文 ====================

    /**
     * 获取章节正文
     * <p>
     * 根据 Book 的 origin 自动查找对应的书源。
     *
     * @param book       书籍对象
     * @param chapter    章节对象
     * @return 正文内容字符串
     */
    public String getBookContent(Book book, BookChapter chapter) {
        BookSource source = getSource(book.getOrigin());
        if (source == null) {
            throw new IllegalStateException("找不到对应书源: " + book.getOrigin());
        }
        return ReaderEngine.getBookContent(source, book, chapter);
    }

    /**
     * 获取章节正文（指定书源）
     *
     * @param bookSourceUrl 书源 URL
     * @param book          书籍对象
     * @param chapter       章节对象
     * @return 正文内容字符串
     */
    public String getBookContent(String bookSourceUrl, Book book, BookChapter chapter) {
        BookSource source = getSource(bookSourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到对应书源: " + bookSourceUrl);
        }
        return ReaderEngine.getBookContent(source, book, chapter);
    }

    // ==================== 8. 批量下载正文 ====================

    /**
     * 批量下载章节正文
     * <p>
     * 依次获取每个章节的正文内容，返回章节正文列表。
     * 某个章节下载失败不会中断后续章节，失败章节内容为 null。
     *
     * @param book      书籍对象
     * @param chapters  章节列表
     * @return 正文内容列表（与 chapters 一一对应，失败的为 null）
     */
    public List<String> batchDownloadContent(Book book, List<BookChapter> chapters) {
        return batchDownloadContent(book, chapters, 0);
    }

    /**
     * 批量下载章节正文（带间隔延迟）
     * <p>
     * 每个章节之间暂停 delayMs 毫秒，避免请求过快被屏蔽。
     *
     * @param book      书籍对象
     * @param chapters  章节列表
     * @param delayMs   每章间隔毫秒数（0 表示不延迟）
     * @return 正文内容列表（与 chapters 一一对应，失败的为 null）
     */
    public List<String> batchDownloadContent(Book book, List<BookChapter> chapters, long delayMs) {
        BookSource source = getSource(book.getOrigin());
        if (source == null) {
            throw new IllegalStateException("找不到对应书源: " + book.getOrigin());
        }
        return batchDownloadContent(source, book, chapters, delayMs);
    }

    /**
     * 批量下载章节正文（指定书源）
     *
     * @param bookSource 书源对象
     * @param book       书籍对象
     * @param chapters   章节列表
     * @param delayMs    每章间隔毫秒数
     * @return 正文内容列表
     */
    public List<String> batchDownloadContent(BookSource bookSource, Book book,
                                              List<BookChapter> chapters, long delayMs) {
        List<String> contents = new ArrayList<>(chapters.size());
        for (int i = 0; i < chapters.size(); i++) {
            BookChapter chapter = chapters.get(i);
            try {
                String content = ReaderEngine.getBookContent(bookSource, book, chapter);
                contents.add(content);
                log.info("下载章节 [{}/{}]: {} | 长度: {}", i + 1, chapters.size(),
                        chapter.getTitle(), content == null ? 0 : content.length());
            } catch (Exception e) {
                log.error("下载章节失败 [{}/{}]: {} | {}", i + 1, chapters.size(),
                        chapter.getTitle(), e.getMessage());
                contents.add(null);
            }
            // 间隔延迟
            if (delayMs > 0 && i < chapters.size() - 1) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return contents;
    }

    /**
     * 批量下载章节正文并组装为 Map（章节标题 → 正文内容）
     *
     * @param book      书籍对象
     * @param chapters  章节列表
     * @return Map：章节标题 → 正文内容
     */
    public Map<String, String> batchDownloadContentAsMap(Book book, List<BookChapter> chapters) {
        List<String> contents = batchDownloadContent(book, chapters);
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < chapters.size(); i++) {
            result.put(chapters.get(i).getTitle(), contents.get(i));
        }
        return result;
    }

    /**
     * 批量下载章节正文并拼接到一个字符串
     *
     * @param book      书籍对象
     * @param chapters  章节列表
     * @param separator 章节之间的分隔符（如 "\n\n"）
     * @return 拼接后的完整文本
     */
    public String batchDownloadContentAsString(Book book, List<BookChapter> chapters, String separator) {
        List<String> contents = batchDownloadContent(book, chapters);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contents.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            String content = contents.get(i);
            if (content != null) {
                sb.append(content);
            }
        }
        return sb.toString();
    }

    // ==================== 内置源加载 ====================

    /**
     * 从 classpath 加载内置书源
     */
    private void loadBuiltinSources() {
        log.info("开始加载内置书源...");
        int count = 0;
        // 加载小说源
        for (String file : BUILTIN_NOVEL_FILES) {
            try {
                BookSource source = loadBuiltinSource(file);
                if (source != null) {
                    // 小说源不需要 initSource
                    sourceMap.put(source.getBookSourceUrl(), source);
                    count++;
                    log.info("加载内置小说源: {} ({})", source.getBookSourceName(), source.getBookSourceUrl());
                }
            } catch (Exception e) {
                log.error("加载内置小说源失败: {}", file, e);
            }
        }
        // 加载漫画源
        for (String file : BUILTIN_COMIC_FILES) {
            try {
                BookSource source = loadBuiltinSource(file);
                if (source != null) {
                    // 漫画源需要 initSource 初始化 variable
                    ReaderEngine.initSource(source);
                    sourceMap.put(source.getBookSourceUrl(), source);
                    count++;
                    log.info("加载内置漫画源: {} ({})", source.getBookSourceName(), source.getBookSourceUrl());
                }
            } catch (Exception e) {
                log.error("加载内置漫画源失败: {}", file, e);
            }
        }
        log.info("内置书源加载完成，共 {} 个", count);
    }

    /**
     * 从 classpath 读取单个内置源 JSON 并解析
     */
    private BookSource loadBuiltinSource(String filename) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(BUILTIN_DIR + filename)) {
            if (is == null) {
                log.warn("内置源文件不存在: {}", filename);
                return null;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            List<BookSource> sources = ReaderEngine.parseBookSources(json);
            if (sources != null && !sources.isEmpty()) {
                return sources.get(0);
            }
            // 可能是单个对象格式
            BookSource source = ReaderEngine.parseBookSource(json);
            return source;
        }
    }

    // ==================== 内部工具 ====================

    /**
     * 在指定源列表中聚合搜索
     */
    private List<SearchBook> searchSources(List<BookSource> sources, String key, int page) {
        List<SearchBook> allResults = new ArrayList<>();
        for (BookSource source : sources) {
            try {
                List<SearchBook> results = ReaderEngine.search(source, key, page);
                allResults.addAll(results);
            } catch (Exception e) {
                log.warn("搜索失败 [{}] in [{}]: {}", key, source.getBookSourceName(), e.getMessage());
            }
        }
        return allResults;
    }
}
