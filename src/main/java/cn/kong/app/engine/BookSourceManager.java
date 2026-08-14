package cn.kong.app.engine;

import cn.kong.app.engine.dto.BookDetail;
import cn.kong.app.engine.dto.ChapterInfo;
import cn.kong.app.engine.dto.SearchResult;
import io.legado.app.data.entities.Book;
import io.legado.app.data.entities.BookChapter;
import io.legado.app.data.entities.BookSource;
import io.legado.app.data.entities.SearchBook;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BookSourceManager - 书源管理器（通用 API）
 * <p>
 * 用户不需要接触 SearchBook / Book / BookChapter 等内部对象，
 * 只需传入简单参数（关键词、URL、序号）即可完成全部操作。
 *
 * <h3>API 总览</h3>
 * <pre>
 * BookSourceManager manager = BookSourceManager.getInstance();
 *
 * // 1. 导入书源
 * manager.importSources(jsonString);
 *
 * // 2. 列出源
 * List&lt;String&gt; sources = manager.listSourceNames();
 *
 * // 3. 搜索
 * List&lt;SearchResult&gt; results = manager.search("斗破苍穹");
 *
 * // 4. 按作者搜索
 * List&lt;SearchResult&gt; results = manager.searchByAuthor("天蚕土豆");
 *
 * // 5. 获取详情
 * BookDetail detail = manager.getBookDetail(bookUrl, sourceUrl);
 *
 * // 6. 获取目录
 * List&lt;ChapterInfo&gt; chapters = manager.getChapterList(bookUrl, sourceUrl);
 *
 * // 7. 获取正文（按章节序号）
 * String content = manager.getContent(bookUrl, sourceUrl, 0);
 *
 * // 8. 批量下载正文
 * List&lt;String&gt; contents = manager.batchDownload(bookUrl, sourceUrl, 0, 10);
 * </pre>
 */
public class BookSourceManager {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookSourceManager.class);

    private static final String BUILTIN_DIR = "/builtin/";

    private static final String[] BUILTIN_NOVEL_FILES = {
            "novel_80.json",
            "novel_dubu.json",
            "novel_maoyan.json",
            "novel_qimao.json"
    };

    private static final String[] BUILTIN_COMIC_FILES = {
            "comic_godamanga.json",
            "comic_manhuatai.json",
            "comic_rumanhua.json",
            "comic_zaimanhua.json"
    };

    private static final int TYPE_NOVEL = 0;
    private static final int TYPE_COMIC = 2;

    private static volatile BookSourceManager instance;

    /** 书源存储 */
    private final ConcurrentHashMap<String, BookSource> sourceMap = new ConcurrentHashMap<>();

    /**
     * Book 对象缓存，避免重复请求详情。
     * key = sourceUrl + "|" + bookUrl
     */
    private final ConcurrentHashMap<String, Book> bookCache = new ConcurrentHashMap<>();

    private BookSourceManager() {
        loadBuiltinSources();
    }

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

    // ==================== 1. 导入书源 ====================

    /**
     * 从 JSON 字符串导入书源（支持单个或数组格式）
     *
     * @param json 书源 JSON 字符串
     * @return 导入的书源数量
     */
    public int importSources(String json) {
        List<BookSource> sources;
        try {
            sources = ReaderEngine.parseBookSources(json);
        } catch (Exception e) {
            try {
                BookSource source = ReaderEngine.parseBookSource(json);
                sources = new ArrayList<>();
                sources.add(source);
            } catch (Exception e2) {
                throw new IllegalArgumentException("解析书源 JSON 失败", e2);
            }
        }
        for (BookSource source : sources) {
            ReaderEngine.initSource(source);
            sourceMap.put(source.getBookSourceUrl(), source);
            log.info("导入书源: {} ({})", source.getBookSourceName(), source.getBookSourceUrl());
        }
        return sources.size();
    }

    /**
     * 从输入流导入书源
     *
     * @param inputStream 书源 JSON 输入流
     * @return 导入的书源数量
     */
    public int importSources(InputStream inputStream) {
        try {
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return importSources(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("读取书源流失败", e);
        }
    }

    /**
     * 移除指定书源
     *
     * @param sourceUrl 书源 URL
     */
    public void removeSource(String sourceUrl) {
        sourceMap.remove(sourceUrl);
        log.info("移除书源: {}", sourceUrl);
    }

    /**
     * 清除所有书源
     */
    public void clearAllSources() {
        sourceMap.clear();
        bookCache.clear();
        log.info("已清除所有书源");
    }

    /**
     * 重新加载内置源
     */
    public void reloadBuiltinSources() {
        loadBuiltinSources();
    }

    // ==================== 2. 列出书源 ====================

    /**
     * 获取书源数量
     *
     * @return 书源总数
     */
    public int getSourceCount() {
        return sourceMap.size();
    }

    /**
     * 列出所有书源名称
     *
     * @return 书源名称列表
     */
    public List<String> listSourceNames() {
        List<String> names = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            names.add(s.getBookSourceName());
        }
        return names;
    }

    /**
     * 列出所有小说源名称
     *
     * @return 小说源名称列表
     */
    public List<String> listNovelSourceNames() {
        List<String> names = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            if (s.getBookSourceType() == TYPE_NOVEL) {
                names.add(s.getBookSourceName());
            }
        }
        return names;
    }

    /**
     * 列出所有漫画源名称
     *
     * @return 漫画源名称列表
     */
    public List<String> listComicSourceNames() {
        List<String> names = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            if (s.getBookSourceType() == TYPE_COMIC) {
                names.add(s.getBookSourceName());
            }
        }
        return names;
    }

    /**
     * 列出所有书源信息
     *
     * @return 书源信息列表（Map: name, url, type）
     */
    public List<Map<String, Object>> listAllSources() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            Map<String, Object> info = new java.util.LinkedHashMap<>();
            info.put("name", s.getBookSourceName());
            info.put("url", s.getBookSourceUrl());
            info.put("type", s.getBookSourceType());
            info.put("typeDesc", s.getBookSourceType() == TYPE_NOVEL ? "小说" : "漫画");
            list.add(info);
        }
        return list;
    }

    // ==================== 3. 搜索 ====================

    /**
     * 搜索（跨所有源：小说 + 漫画）
     *
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String keyword) {
        return search(keyword, 1);
    }

    /**
     * 搜索（跨所有源，指定页码）
     *
     * @param keyword 搜索关键词
     * @param page    页码（从 1 开始）
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String keyword, int page) {
        return doSearch(new ArrayList<>(sourceMap.values()), keyword, page);
    }

    /**
     * 搜索小说（仅在小说源中搜索）
     *
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    public List<SearchResult> searchNovel(String keyword) {
        return searchNovel(keyword, 1);
    }

    /**
     * 搜索小说（指定页码）
     */
    public List<SearchResult> searchNovel(String keyword, int page) {
        List<BookSource> novels = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            if (s.getBookSourceType() == TYPE_NOVEL) {
                novels.add(s);
            }
        }
        return doSearch(novels, keyword, page);
    }

    /**
     * 搜索漫画（仅在漫画源中搜索）
     *
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    public List<SearchResult> searchComic(String keyword) {
        return searchComic(keyword, 1);
    }

    /**
     * 搜索漫画（指定页码）
     */
    public List<SearchResult> searchComic(String keyword, int page) {
        List<BookSource> comics = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            if (s.getBookSourceType() == TYPE_COMIC) {
                comics.add(s);
            }
        }
        return doSearch(comics, keyword, page);
    }

    // ==================== 4. 按作者搜索 ====================

    /**
     * 按作者搜索（跨所有源）
     *
     * @param author 作者名
     * @return 匹配的搜索结果列表
     */
    public List<SearchResult> searchByAuthor(String author) {
        return searchByAuthor(author, 1);
    }

    /**
     * 按作者搜索（指定页码）
     */
    public List<SearchResult> searchByAuthor(String author, int page) {
        return doSearchByAuthor(new ArrayList<>(sourceMap.values()), author, page);
    }

    /**
     * 按作者搜索小说
     *
     * @param author 作者名
     * @return 匹配的搜索结果列表
     */
    public List<SearchResult> searchNovelByAuthor(String author) {
        List<BookSource> novels = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            if (s.getBookSourceType() == TYPE_NOVEL) {
                novels.add(s);
            }
        }
        return doSearchByAuthor(novels, author, 1);
    }

    /**
     * 按作者搜索漫画
     *
     * @param author 作者名
     * @return 匹配的搜索结果列表
     */
    public List<SearchResult> searchComicByAuthor(String author) {
        List<BookSource> comics = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            if (s.getBookSourceType() == TYPE_COMIC) {
                comics.add(s);
            }
        }
        return doSearchByAuthor(comics, author, 1);
    }

    // ==================== 5. 获取详情 ====================

    /**
     * 获取书籍详情
     *
     * @param bookUrl   书籍 URL（从搜索结果 SearchResult.getBookUrl() 获取）
     * @param sourceUrl 书源 URL（从搜索结果 SearchResult.getSourceUrl() 获取）
     * @return 书籍详情
     */
    public BookDetail getBookDetail(String bookUrl, String sourceUrl) {
        BookSource source = getSource(sourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到书源: " + sourceUrl);
        }
        Book book = ReaderEngine.getBookInfo(source, bookUrl);
        cacheBook(book, sourceUrl);
        return toBookDetail(book, source);
    }

    // ==================== 6. 获取目录 ====================

    /**
     * 获取章节目录
     *
     * @param bookUrl   书籍 URL
     * @param sourceUrl 书源 URL
     * @return 章节列表
     */
    public List<ChapterInfo> getChapterList(String bookUrl, String sourceUrl) {
        BookSource source = getSource(sourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到书源: " + sourceUrl);
        }
        Book book = getOrFetchBook(bookUrl, sourceUrl);
        List<BookChapter> chapters = ReaderEngine.getChapterList(source, book);
        List<ChapterInfo> result = new ArrayList<>(chapters.size());
        for (BookChapter ch : chapters) {
            result.add(new ChapterInfo(ch.getTitle(), ch.getUrl(), ch.getIndex()));
        }
        return result;
    }

    // ==================== 7. 获取正文 ====================

    /**
     * 获取章节正文（按章节序号）
     *
     * @param bookUrl      书籍 URL
     * @param sourceUrl    书源 URL
     * @param chapterIndex 章节序号（从 0 开始，对应 getChapterList 返回的 index）
     * @return 正文内容字符串
     */
    public String getContent(String bookUrl, String sourceUrl, int chapterIndex) {
        BookSource source = getSource(sourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到书源: " + sourceUrl);
        }
        Book book = getOrFetchBook(bookUrl, sourceUrl);
        List<BookChapter> chapters = ReaderEngine.getChapterList(source, book);
        if (chapterIndex < 0 || chapterIndex >= chapters.size()) {
            throw new IndexOutOfBoundsException(
                    "章节序号超出范围: " + chapterIndex + ", 总章节数: " + chapters.size());
        }
        BookChapter chapter = chapters.get(chapterIndex);
        return ReaderEngine.getBookContent(source, book, chapter);
    }

    /**
     * 获取章节正文（按章节 URL）
     *
     * @param bookUrl     书籍 URL
     * @param sourceUrl   书源 URL
     * @param chapterUrl  章节 URL（从 ChapterInfo.getUrl() 获取）
     * @return 正文内容字符串
     */
    public String getContentByUrl(String bookUrl, String sourceUrl, String chapterUrl) {
        BookSource source = getSource(sourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到书源: " + sourceUrl);
        }
        Book book = getOrFetchBook(bookUrl, sourceUrl);
        List<BookChapter> chapters = ReaderEngine.getChapterList(source, book);
        for (BookChapter ch : chapters) {
            if (ch.getUrl().equals(chapterUrl)) {
                return ReaderEngine.getBookContent(source, book, ch);
            }
        }
        throw new IllegalArgumentException("找不到章节 URL: " + chapterUrl);
    }

    // ==================== 8. 批量下载正文 ====================

    /**
     * 批量下载章节正文（按范围）
     *
     * @param bookUrl    书籍 URL
     * @param sourceUrl  书源 URL
     * @param startIndex 起始章节序号（从 0 开始）
     * @param endIndex   结束章节序号（不包含，即 [startIndex, endIndex)）
     * @return 正文内容列表
     */
    public List<String> batchDownload(String bookUrl, String sourceUrl, int startIndex, int endIndex) {
        return batchDownload(bookUrl, sourceUrl, startIndex, endIndex, 0);
    }

    /**
     * 批量下载章节正文（按范围，带间隔延迟）
     *
     * @param bookUrl     书籍 URL
     * @param sourceUrl   书源 URL
     * @param startIndex  起始章节序号（从 0 开始）
     * @param endIndex    结束章节序号（不包含）
     * @param delayMs     每章间隔毫秒数（0 表示不延迟）
     * @return 正文内容列表
     */
    public List<String> batchDownload(String bookUrl, String sourceUrl,
                                       int startIndex, int endIndex, long delayMs) {
        BookSource source = getSource(sourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到书源: " + sourceUrl);
        }
        Book book = getOrFetchBook(bookUrl, sourceUrl);
        List<BookChapter> chapters = ReaderEngine.getChapterList(source, book);
        if (startIndex < 0 || startIndex >= chapters.size()) {
            throw new IndexOutOfBoundsException("起始章节序号超出范围: " + startIndex);
        }
        int end = Math.min(endIndex, chapters.size());
        List<String> contents = new ArrayList<>(end - startIndex);
        for (int i = startIndex; i < end; i++) {
            BookChapter chapter = chapters.get(i);
            try {
                String content = ReaderEngine.getBookContent(source, book, chapter);
                contents.add(content);
                log.info("下载章节 [{}/{}]: {} | 长度: {}",
                        i - startIndex + 1, end - startIndex,
                        chapter.getTitle(), content == null ? 0 : content.length());
            } catch (Exception e) {
                log.error("下载章节失败 [{}/{}]: {} | {}",
                        i - startIndex + 1, end - startIndex,
                        chapter.getTitle(), e.getMessage());
                contents.add(null);
            }
            if (delayMs > 0 && i < end - 1) {
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
     * 批量下载全部章节正文
     *
     * @param bookUrl  书籍 URL
     * @param sourceUrl 书源 URL
     * @return 正文内容列表
     */
    public List<String> batchDownloadAll(String bookUrl, String sourceUrl) {
        return batchDownload(bookUrl, sourceUrl, 0, Integer.MAX_VALUE);
    }

    /**
     * 批量下载并返回 Map（章节标题 → 正文）
     *
     * @param bookUrl    书籍 URL
     * @param sourceUrl  书源 URL
     * @param startIndex 起始章节序号
     * @param endIndex   结束章节序号（不包含）
     * @return Map：章节标题 → 正文内容
     */
    public Map<String, String> batchDownloadAsMap(String bookUrl, String sourceUrl,
                                                    int startIndex, int endIndex) {
        BookSource source = getSource(sourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到书源: " + sourceUrl);
        }
        Book book = getOrFetchBook(bookUrl, sourceUrl);
        List<BookChapter> chapters = ReaderEngine.getChapterList(source, book);
        int end = Math.min(endIndex, chapters.size());
        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (int i = startIndex; i < end; i++) {
            BookChapter ch = chapters.get(i);
            try {
                String content = ReaderEngine.getBookContent(source, book, ch);
                result.put(ch.getTitle(), content);
            } catch (Exception e) {
                result.put(ch.getTitle(), null);
            }
        }
        return result;
    }

    /**
     * 批量下载并拼接为完整文本
     *
     * @param bookUrl    书籍 URL
     * @param sourceUrl  书源 URL
     * @param startIndex 起始章节序号
     * @param endIndex   结束章节序号（不包含）
     * @param separator  章节分隔符
     * @return 拼接后的完整文本
     */
    public String batchDownloadAsString(String bookUrl, String sourceUrl,
                                        int startIndex, int endIndex, String separator) {
        List<String> contents = batchDownload(bookUrl, sourceUrl, startIndex, endIndex);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contents.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            if (contents.get(i) != null) {
                sb.append(contents.get(i));
            }
        }
        return sb.toString();
    }

    // ==================== 内部方法 ====================

    private BookSource getSource(String sourceUrl) {
        return sourceMap.get(sourceUrl);
    }

    /**
     * 获取或缓存 Book 对象（避免重复请求详情页）
     */
    private Book getOrFetchBook(String bookUrl, String sourceUrl) {
        String cacheKey = sourceUrl + "|" + bookUrl;
        Book book = bookCache.get(cacheKey);
        if (book != null) {
            return book;
        }
        BookSource source = getSource(sourceUrl);
        if (source == null) {
            throw new IllegalStateException("找不到书源: " + sourceUrl);
        }
        book = ReaderEngine.getBookInfo(source, bookUrl);
        cacheBook(book, sourceUrl);
        return book;
    }

    private void cacheBook(Book book, String sourceUrl) {
        String cacheKey = sourceUrl + "|" + book.getBookUrl();
        bookCache.put(cacheKey, book);
    }

    /**
     * 聚合搜索
     */
    private List<SearchResult> doSearch(List<BookSource> sources, String keyword, int page) {
        List<SearchResult> allResults = new ArrayList<>();
        for (BookSource source : sources) {
            try {
                List<SearchBook> results = ReaderEngine.search(source, keyword, page);
                for (SearchBook sb : results) {
                    allResults.add(toSearchResult(sb, source));
                }
            } catch (Exception e) {
                log.warn("搜索失败 [{}] in [{}]: {}", keyword, source.getBookSourceName(), e.getMessage());
            }
        }
        return allResults;
    }

    /**
     * 按作者搜索并过滤
     */
    private List<SearchResult> doSearchByAuthor(List<BookSource> sources, String author, int page) {
        List<SearchResult> allResults = new ArrayList<>();
        for (BookSource source : sources) {
            try {
                List<SearchBook> results = ReaderEngine.search(source, author, page);
                for (SearchBook sb : results) {
                    if (sb.getAuthor() != null && sb.getAuthor().contains(author)) {
                        allResults.add(toSearchResult(sb, source));
                    }
                }
            } catch (Exception e) {
                log.warn("按作者搜索失败 [{}] in [{}]: {}", author, source.getBookSourceName(), e.getMessage());
            }
        }
        return allResults;
    }

    // ==================== DTO 转换 ====================

    private static SearchResult toSearchResult(SearchBook sb, BookSource source) {
        SearchResult r = new SearchResult();
        r.setName(sb.getName());
        r.setAuthor(sb.getAuthor());
        r.setBookUrl(sb.getBookUrl());
        r.setSourceUrl(source.getBookSourceUrl());
        r.setSourceName(source.getBookSourceName());
        r.setCoverUrl(sb.getCoverUrl());
        r.setIntro(sb.getIntro());
        r.setKind(sb.getKind());
        r.setWordCount(sb.getWordCount());
        r.setLatestChapterTitle(sb.getLatestChapterTitle());
        r.setType(source.getBookSourceType());
        return r;
    }

    private static BookDetail toBookDetail(Book book, BookSource source) {
        BookDetail d = new BookDetail();
        d.setName(book.getName());
        d.setAuthor(book.getAuthor());
        d.setBookUrl(book.getBookUrl());
        d.setTocUrl(book.getTocUrl());
        d.setSourceUrl(source.getBookSourceUrl());
        d.setSourceName(source.getBookSourceName());
        d.setCoverUrl(book.getCoverUrl());
        d.setIntro(book.getIntro());
        d.setKind(book.getKind());
        d.setWordCount(book.getWordCount());
        d.setLatestChapterTitle(book.getLatestChapterTitle());
        d.setType(source.getBookSourceType());
        return d;
    }

    // ==================== 内置源加载 ====================

    private void loadBuiltinSources() {
        log.info("开始加载内置书源...");
        int count = 0;
        for (String file : BUILTIN_NOVEL_FILES) {
            try {
                BookSource source = loadBuiltinSource(file);
                if (source != null) {
                    sourceMap.put(source.getBookSourceUrl(), source);
                    count++;
                    log.info("加载内置小说源: {} ({})", source.getBookSourceName(), source.getBookSourceUrl());
                }
            } catch (Exception e) {
                log.error("加载内置小说源失败: {}", file, e);
            }
        }
        for (String file : BUILTIN_COMIC_FILES) {
            try {
                BookSource source = loadBuiltinSource(file);
                if (source != null) {
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
            return ReaderEngine.parseBookSource(json);
        }
    }
}
