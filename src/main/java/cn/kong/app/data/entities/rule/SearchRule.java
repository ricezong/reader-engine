package cn.kong.app.data.entities.rule;

import java.io.Serializable;

/**
 * 搜索规则
 * 改造自 reader-dev 的 SearchRule.kt
 */
public class SearchRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 搜索地址（支持 {key} 占位符） */
    private String checkKeyWord;

    /** 搜索 URL */
    private String searchUrl;

    /** 列表规则（用于定位搜索结果列表） */
    private String bookList;

    /** 书名规则 */
    private String name;

    /** 作者规则 */
    private String author;

    /** 分类规则 */
    private String category;

    /** 字数规则 */
    private String wordCount;

    /** 最近更新规则 */
    private String lastChapter;

    /** 简介规则 */
    private String intro;

    /** 封面规则 */
    private String coverUrl;

    /** 详情页 URL 规则 */
    private String bookUrl;

    /** 是否分页 */
    private String checkKeyWord2;

    // ============ getter / setter ============

    public String getCheckKeyWord() {
        return checkKeyWord;
    }

    public void setCheckKeyWord(String checkKeyWord) {
        this.checkKeyWord = checkKeyWord;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    public String getBookList() {
        return bookList;
    }

    public void setBookList(String bookList) {
        this.bookList = bookList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getWordCount() {
        return wordCount;
    }

    public void setWordCount(String wordCount) {
        this.wordCount = wordCount;
    }

    public String getLastChapter() {
        return lastChapter;
    }

    public void setLastChapter(String lastChapter) {
        this.lastChapter = lastChapter;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public String getCheckKeyWord2() {
        return checkKeyWord2;
    }

    public void setCheckKeyWord2(String checkKeyWord2) {
        this.checkKeyWord2 = checkKeyWord2;
    }
}
