package io.legado.app.engine

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.model.Debug
import io.legado.app.model.DebugLog
import kotlinx.coroutines.runBlocking

/**
 * Kotlin 桥接层 - 为 Java 门面 ReaderEngine 提供同步调用入口
 *
 * Kotlin suspend 函数在编译后会生成带有 Continuation 参数的字节码,
 * Java 无法直接调用, 需要 Kotlin 层桥接为同步方法.
 */
object ReaderEngineBridge {

    // ==================== 书源解析 ====================

    /**
     * 从 JSON 解析单个书源
     */
    @JvmStatic
    fun parseBookSource(json: String): BookSource {
        return BookSource.fromJson(json).getOrThrow()
    }

    /**
     * 从 JSON 解析书源列表
     */
    @JvmStatic
    fun parseBookSources(json: String): MutableList<BookSource> {
        return BookSource.fromJsonArray(json).getOrThrow()
    }

    // ==================== 书源初始化 ====================

    /**
     * 初始化书源 (同步)
     * 执行 loginUrl 中的 JS 代码, 初始化 variable 等配置.
     * 漫画源必须调用此方法, 否则 Get('url') 等函数会返回 null.
     *
     * @param bookSource 书源对象
     * @return 初始化后的书源对象 (variable 已设置)
     */
    @JvmStatic
    fun initSource(bookSource: BookSource): BookSource {
        // 如果已有 variable, 不重复初始化
        if (!bookSource.getVariable().isNullOrEmpty()) {
            return bookSource
        }
        // 执行 loginUrl 中的 JS, 这会调用 source.setVariable() 设置默认 variable
        bookSource.login()
        return bookSource
    }

    // ==================== 搜索 ====================

    /**
     * 搜索书籍 (同步)
     */
    @JvmStatic
    @JvmOverloads
    fun search(bookSource: BookSource, key: String, page: Int? = 1): List<SearchBook> {
        val webBook = WebBook(bookSource, true, null)
        return runBlocking { webBook.searchBook(key, page) }
    }

    /**
     * 发现书籍 (同步)
     */
    @JvmStatic
    @JvmOverloads
    fun explore(bookSource: BookSource, url: String, page: Int? = 1): List<SearchBook> {
        val webBook = WebBook(bookSource, true, null)
        return runBlocking { webBook.exploreBook(url, page) }
    }

    // ==================== 书籍详情 ====================

    /**
     * 获取书籍详情 (同步, 通过 bookUrl)
     */
    @JvmStatic
    @JvmOverloads
    fun getBookInfo(bookSource: BookSource, bookUrl: String, canReName: Boolean = true): Book {
        val webBook = WebBook(bookSource, true, null)
        return runBlocking { webBook.getBookInfo(bookUrl, canReName) }
    }

    /**
     * 获取书籍详情 (同步, 通过已有 Book 对象)
     */
    @JvmStatic
    @JvmOverloads
    fun getBookInfo(bookSource: BookSource, book: Book, canReName: Boolean = true): Book {
        val webBook = WebBook(bookSource, true, null)
        return runBlocking { webBook.getBookInfo(book, canReName) }
    }

    // ==================== 章节目录 ====================

    /**
     * 获取章节目录 (同步)
     */
    @JvmStatic
    fun getChapterList(bookSource: BookSource, book: Book): List<BookChapter> {
        val webBook = WebBook(bookSource, true, null)
        return runBlocking { webBook.getChapterList(book) }
    }

    // ==================== 章节正文 ====================

    /**
     * 获取章节正文 (同步)
     */
    @JvmStatic
    @JvmOverloads
    fun getBookContent(
        bookSource: BookSource,
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String? = null
    ): String {
        val webBook = WebBook(bookSource, true, null)
        return runBlocking { webBook.getBookContent(book, bookChapter, nextChapterUrl) }
    }

    // ==================== Variable 管理 ====================

    /**
     * 设置书源 variable (同步)
     * 用于手动设置书源变量, 漫画源的 url/ci0 等配置通过此方法设置.
     */
    @JvmStatic
    fun setVariable(bookSource: BookSource, variable: String?) {
        bookSource.setVariable(variable)
    }

    /**
     * 获取书源 variable (同步)
     */
    @JvmStatic
    fun getVariable(bookSource: BookSource): String? {
        return bookSource.getVariable()
    }

    // ==================== JS 执行 ====================

    /**
     * 执行书源 JS 代码 (同步)
     * 在书源上下文中执行 JS, 可访问 source/java/cache/cookie 等对象.
     * jsLib 会自动拼接到 JS 代码前面.
     */
    @JvmStatic
    fun evalJS(bookSource: BookSource, jsStr: String): Any? {
        return bookSource.evalJS(jsStr)
    }
}
