package cn.kong.app.engine;

import cn.kong.app.engine.dto.BookDetail;
import cn.kong.app.engine.dto.ChapterContent;
import cn.kong.app.engine.dto.ChapterInfo;
import cn.kong.app.engine.dto.SearchResult;
import cn.kong.app.engine.dto.SourceInfo;
import io.legado.app.data.entities.Book;
import io.legado.app.data.entities.BookChapter;
import io.legado.app.data.entities.BookSource;
import io.legado.app.data.entities.SearchBook;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReaderService - 阅读引擎服务入口
 * <p>
 * 对外提供书源管理、搜索、详情、目录、正文等一站式 API。
 * 用户不需要接触 SearchBook / Book / BookChapter 等内部对象，
 * 只需传入简单参数（关键词、URL、source 简称、序号）即可完成全部操作。
 * <p>
 * 异常处理：底层 {@link ReaderEngine} 已统一封装为 {@link SourceException}，
 * 本层不再重复包装，直接透传。仅在本层独有的逻辑（如 resolveSource、章节序号越界）
 * 处封装异常。
 *
 * <h3>API 总览</h3>
 * <pre>
 * ReaderService service = ReaderService.getInstance();
 *
 * // 1. 列出书源
 * List&lt;SourceInfo&gt; all = service.listAllSources();
 * List&lt;SourceInfo&gt; novels = service.listNovelSources();
 * List&lt;SourceInfo&gt; comics = service.listComicSources();
 *
 * // 2. 搜索
 * List&lt;SearchResult&gt; results = service.search("斗破苍穹");
 *
 * // 3. 按作者搜索
 * List&lt;SearchResult&gt; results = service.searchByAuthor("天蚕土豆");
 *
 * // 4. 获取详情（source 从 SearchResult.getSource() 获取）
 * BookDetail detail = service.getBookDetail(bookUrl, source);
 *
 * // 5. 获取目录
 * List&lt;ChapterInfo&gt; chapters = service.getChapterList(bookUrl, source);
 *
 * // 6. 获取正文（按章节序号）
 * String content = service.getContent(bookUrl, source, 0);
 *
 * // 7. 批量下载正文
 * List&lt;ChapterContent&gt; contents = service.batchDownload(bookUrl, source, 0, 10);
 * </pre>
 *
 * <h3>内置书源简称</h3>
 * <pre>
 * 小说源：novel_1 / novel_2 / novel_3 / novel_4
 * 漫画源：comic_1 / comic_2 / comic_3 / comic_4
 * </pre>
 * 简称即为内置源文件名去掉 .json 后缀，换源时直接替换 JSON 文件内容即可，无需改代码。
 */
public class ReaderService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReaderService.class);

    private static final String BUILTIN_DIR = "/builtin/";

    /** 内置源文件名（文件名前缀即 source 简称） */
    private static final String[] BUILTIN_NOVEL_FILES = {
            "novel_1.json",
            "novel_2.json",
            "novel_3.json",
            "novel_4.json"
    };

    private static final String[] BUILTIN_COMIC_FILES = {
            "comic_1.json",
            "comic_2.json",
            "comic_3.json",
            "comic_4.json"
    };

    private static final int TYPE_NOVEL = 0;
    private static final int TYPE_COMIC = 2;

    private static volatile ReaderService instance;

    /**
     * 书源存储：key = source 简称（文件名前缀，如 novel_1、comic_2），value = BookSource
     */
    private final ConcurrentHashMap<String, BookSource> sourceMap = new ConcurrentHashMap<>();

    private ReaderService() {
        loadBuiltinSources();
    }

    public static ReaderService getInstance() {
        if (instance == null) {
            synchronized (ReaderService.class) {
                if (instance == null) {
                    instance = new ReaderService();
                }
            }
        }
        return instance;
    }

    // ==================== 1. 列出书源 ====================

    /**
     * 列出所有书源
     *
     * @return 书源信息列表
     */
    public List<SourceInfo> listAllSources() {
        List<SourceInfo> list = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            list.add(toSourceInfo(s));
        }
        return list;
    }

    /**
     * 列出所有小说源
     *
     * @return 小说源信息列表
     */
    public List<SourceInfo> listNovelSources() {
        List<SourceInfo> list = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            if (s.getBookSourceType() == TYPE_NOVEL) {
                list.add(toSourceInfo(s));
            }
        }
        return list;
    }

    /**
     * 列出所有漫画源
     *
     * @return 漫画源信息列表
     */
    public List<SourceInfo> listComicSources() {
        List<SourceInfo> list = new ArrayList<>();
        for (BookSource s : sourceMap.values()) {
            if (s.getBookSourceType() == TYPE_COMIC) {
                list.add(toSourceInfo(s));
            }
        }
        return list;
    }

    // ==================== 2. 搜索 ====================

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

    // ==================== 2.1 单源搜索 ====================

    /**
     * 在指定书源中搜索
     *
     * @param keyword 搜索关键词
     * @param source  书源简称（如 novel_1、comic_2 等）
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String keyword, String source) {
        return search(keyword, source, 1);
    }

    /**
     * 在指定书源中搜索（指定页码）
     *
     * @param keyword 搜索关键词
     * @param source  书源简称（如 novel_1、comic_2 等）
     * @param page    页码（从 1 开始）
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String keyword, String source, int page) {
        BookSource src = resolveSource(source);
        ensureSourceReady(src);
        return doSearch(Collections.singletonList(src), keyword, page);
    }

    // ==================== 3. 按作者搜索 ====================

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

    // ==================== 4. 获取详情 ====================

    /**
     * 获取书籍详情
     *
     * @param bookUrl 书籍 URL（从搜索结果 SearchResult.getBookUrl() 获取）
     * @param source  书源简称（从 SearchResult.getSource() 获取，如 novel_1/comic_2 等）
     * @return 书籍详情
     */
    public BookDetail getBookDetail(String bookUrl, String source) {
        BookSource src = resolveSource(source);
        ensureSourceReady(src);
        Book book = ReaderEngine.getBookInfo(src, bookUrl);
        return toBookDetail(book, src);
    }

    // ==================== 5. 获取目录 ====================

    /**
     * 获取章节目录
     *
     * @param bookUrl 书籍 URL
     * @param source  书源简称
     * @return 章节列表
     */
    public List<ChapterInfo> getChapterList(String bookUrl, String source) {
        BookSource src = resolveSource(source);
        ensureSourceReady(src);
        Book book = ReaderEngine.getBookInfo(src, bookUrl);
        List<BookChapter> chapters = ReaderEngine.getChapterList(src, book);
        List<ChapterInfo> result = new ArrayList<>(chapters.size());
        for (BookChapter ch : chapters) {
            result.add(new ChapterInfo(ch.getTitle(), ch.getUrl(), ch.getIndex()));
        }
        return result;
    }

    // ==================== 6. 获取正文 ====================

    /**
     * 获取章节正文（按章节序号）
     *
     * @param bookUrl      书籍 URL
     * @param source        书源简称
     * @param chapterIndex  章节序号（从 0 开始，对应 getChapterList 返回的 index）
     * @return 正文内容字符串
     */
    public String getContent(String bookUrl, String source, int chapterIndex) {
        BookSource src = resolveSource(source);
        ensureSourceReady(src);
        Book book = ReaderEngine.getBookInfo(src, bookUrl);
        List<BookChapter> chapters = ReaderEngine.getChapterList(src, book);
        if (chapterIndex < 0 || chapterIndex >= chapters.size()) {
            throw SourceException.contentError(source, src.getBookSourceName(),
                    "章节序号超出范围: " + chapterIndex + ", 总章节数: " + chapters.size(), null);
        }
        BookChapter chapter = chapters.get(chapterIndex);
        return ReaderEngine.getBookContent(src, book, chapter);
    }

    /**
     * 获取章节正文（按章节 URL）
     *
     * @param bookUrl    书籍 URL
     * @param source     书源简称
     * @param chapterUrl 章节 URL（从 ChapterInfo.getUrl() 获取）
     * @return 正文内容字符串
     */
    public String getContentByUrl(String bookUrl, String source, String chapterUrl) {
        BookSource src = resolveSource(source);
        ensureSourceReady(src);
        Book book = ReaderEngine.getBookInfo(src, bookUrl);
        List<BookChapter> chapters = ReaderEngine.getChapterList(src, book);
        for (BookChapter ch : chapters) {
            if (ch.getUrl().equals(chapterUrl)) {
                return ReaderEngine.getBookContent(src, book, ch);
            }
        }
        throw SourceException.contentError(source, src.getBookSourceName(),
                "找不到章节 URL: " + chapterUrl, null);
    }

    // ==================== 7. 批量下载正文 ====================

    /**
     * 批量下载章节正文（按范围）
     *
     * @param bookUrl    书籍 URL
     * @param source     书源简称
     * @param startIndex 起始章节序号（从 0 开始）
     * @param endIndex   结束章节序号（不包含，即 [startIndex, endIndex)）
     * @return 章节正文列表
     */
    public List<ChapterContent> batchDownload(String bookUrl, String source, int startIndex, int endIndex) {
        return batchDownload(bookUrl, source, startIndex, endIndex, 0);
    }

    /**
     * 批量下载章节正文（按范围，带间隔延迟）
     *
     * @param bookUrl     书籍 URL
     * @param source      书源简称
     * @param startIndex  起始章节序号（从 0 开始）
     * @param endIndex    结束章节序号（不包含）
     * @param delayMs     每章间隔毫秒数（0 表示不延迟）
     * @return 章节正文列表
     */
    public List<ChapterContent> batchDownload(String bookUrl, String source,
                                               int startIndex, int endIndex, long delayMs) {
        BookSource src = resolveSource(source);
        ensureSourceReady(src);
        Book book = ReaderEngine.getBookInfo(src, bookUrl);
        List<BookChapter> chapters = ReaderEngine.getChapterList(src, book);
        if (startIndex < 0 || startIndex >= chapters.size()) {
            throw SourceException.contentError(source, src.getBookSourceName(),
                    "起始章节序号超出范围: " + startIndex, null);
        }
        int end = Math.min(endIndex, chapters.size());
        List<ChapterContent> contents = new ArrayList<>(end - startIndex);
        for (int i = startIndex; i < end; i++) {
            BookChapter chapter = chapters.get(i);
            String content = null;
            try {
                content = ReaderEngine.getBookContent(src, book, chapter);
            } catch (Exception e) {
                log.error("下载章节失败 [{}/{}]: {} | {}",
                        i - startIndex + 1, end - startIndex,
                        chapter.getTitle(), e.getMessage());
            }
            contents.add(new ChapterContent(chapter.getTitle(), chapter.getUrl(),
                    chapter.getIndex(), content));
            log.info("下载章节 [{}/{}]: {} | 长度: {}",
                    i - startIndex + 1, end - startIndex,
                    chapter.getTitle(), content == null ? 0 : content.length());
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
     * @param bookUrl 书籍 URL
     * @param source  书源简称
     * @return 章节正文列表
     */
    public List<ChapterContent> batchDownloadAll(String bookUrl, String source) {
        return batchDownload(bookUrl, source, 0, Integer.MAX_VALUE);
    }

    /**
     * 批量下载并拼接为完整文本
     *
     * @param bookUrl    书籍 URL
     * @param source     书源简称
     * @param startIndex 起始章节序号
     * @param endIndex   结束章节序号（不包含）
     * @param separator  章节分隔符
     * @return 拼接后的完整文本
     */
    public String batchDownloadAsString(String bookUrl, String source,
                                        int startIndex, int endIndex, String separator) {
        List<ChapterContent> contents = batchDownload(bookUrl, source, startIndex, endIndex);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contents.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            if (contents.get(i).getContent() != null) {
                sb.append(contents.get(i).getContent());
            }
        }
        return sb.toString();
    }

    // ==================== 内部方法 ====================

    /**
     * 根据 source 简称解析书源
     */
    private BookSource resolveSource(String source) {
        if (source == null || source.isEmpty()) {
            throw SourceException.sourceNotFound(source, sourceMap.keySet());
        }
        BookSource src = sourceMap.get(source);
        if (src != null) {
            return src;
        }
        throw SourceException.sourceNotFound(source, sourceMap.keySet());
    }

    /**
     * 确保书源就绪：每次请求前重新初始化 variable，
     * 防止 token/cookie 过期导致 JS 执行失败。
     * initSource 内部会先清除旧 variable 再重新执行 login JS。
     */
    private void ensureSourceReady(BookSource src) {
        synchronized (src) {
            try {
                ReaderEngine.initSource(src);
            } catch (Exception e) {
                log.warn("书源 [{}] 重新初始化失败，将使用旧状态继续: {}", src.getBookSourceName(), e.getMessage());
            }
        }
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

    private SourceInfo toSourceInfo(BookSource s) {
        return new SourceInfo(
                findSourceKey(s),
                s.getBookSourceName(),
                s.getBookSourceType(),
                s.getBookSourceType() == TYPE_NOVEL ? "小说" : "漫画"
        );
    }

    private SearchResult toSearchResult(SearchBook sb, BookSource source) {
        SearchResult r = new SearchResult();
        r.setName(sb.getName());
        r.setAuthor(sb.getAuthor());
        r.setBookUrl(sb.getBookUrl());
        r.setSource(findSourceKey(source));
        r.setSourceName(source.getBookSourceName());
        r.setCoverUrl(sb.getCoverUrl());
        r.setIntro(sb.getIntro());
        r.setKind(sb.getKind());
        r.setWordCount(sb.getWordCount());
        r.setLatestChapterTitle(sb.getLatestChapterTitle());
        r.setType(source.getBookSourceType());
        return r;
    }

    private BookDetail toBookDetail(Book book, BookSource source) {
        BookDetail d = new BookDetail();
        d.setName(book.getName());
        d.setAuthor(book.getAuthor());
        d.setBookUrl(book.getBookUrl());
        d.setTocUrl(book.getTocUrl());
        d.setSource(findSourceKey(source));
        d.setSourceName(source.getBookSourceName());
        d.setCoverUrl(book.getCoverUrl());
        d.setIntro(book.getIntro());
        d.setKind(book.getKind());
        d.setWordCount(book.getWordCount());
        d.setLatestChapterTitle(book.getLatestChapterTitle());
        d.setType(source.getBookSourceType());
        return d;
    }

    /**
     * 根据 BookSource 查找 source 简称
     */
    private String findSourceKey(BookSource source) {
        for (java.util.Map.Entry<String, BookSource> entry : sourceMap.entrySet()) {
            if (entry.getValue() == source || entry.getValue().equals(source)) {
                return entry.getKey();
            }
        }
        return source.getBookSourceUrl();
    }

    // ==================== 内置源加载 ====================

    private void loadBuiltinSources() {
        log.info("开始加载内置书源...");
        int count = 0;
        for (String file : BUILTIN_NOVEL_FILES) {
            try {
                BookSource source = loadBuiltinSource(file);
                if (source != null) {
                    String sourceKey = extractSourceKey(file);
                    sourceMap.put(sourceKey, source);
                    count++;
                    log.info("加载内置小说源: {} | 简称: {} | URL: {}",
                            source.getBookSourceName(), sourceKey, source.getBookSourceUrl());
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
                    String sourceKey = extractSourceKey(file);
                    sourceMap.put(sourceKey, source);
                    count++;
                    log.info("加载内置漫画源: {} | 简称: {} | URL: {}",
                            source.getBookSourceName(), sourceKey, source.getBookSourceUrl());
                }
            } catch (Exception e) {
                log.error("加载内置漫画源失败: {}", file, e);
            }
        }
        log.info("内置书源加载完成，共 {} 个", count);
    }

    /**
     * 从文件名提取 source 简称：novel_1.json → novel_1，comic_2.json → comic_2
     */
    private static String extractSourceKey(String filename) {
        if (filename.endsWith(".json")) {
            return filename.substring(0, filename.length() - 5);
        }
        return filename;
    }

    private BookSource loadBuiltinSource(String filename) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(BUILTIN_DIR + filename)) {
            if (is == null) {
                log.warn("内置源文件不存在: {}", filename);
                return null;
            }
            String json = readAllBytes(is);
            List<BookSource> sources = ReaderEngine.parseBookSources(json);
            if (sources != null && !sources.isEmpty()) {
                return sources.get(0);
            }
            return ReaderEngine.parseBookSource(json);
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
