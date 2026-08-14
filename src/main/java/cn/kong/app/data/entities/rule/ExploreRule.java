package cn.kong.app.data.entities.rule;

import java.io.Serializable;

/**
 * 发现规则
 * 改造自 reader-dev 的 ExploreRule.kt
 */
public class ExploreRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发现 URL（多行，格式：名称::url） */
    private String exploreUrl;

    /** 列表规则 */
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

    // ============ getter / setter ============

    public String getExploreUrl() {
        return exploreUrl;
    }

    public void setExploreUrl(String exploreUrl) {
        this.exploreUrl = exploreUrl;
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
}
