package cn.kong.app.data.entities;

import java.io.Serializable;

/**
 * 搜索结果实体
 * 改造自 reader-dev 的 SearchBook.kt
 */
public class SearchBook implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 书籍详情页 URL（唯一标识） */
    private String bookUrl;

    /** 书源 URL */
    private String origin;

    /** 书源名称 */
    private String originName;

    /** 书源类型 */
    private Integer type;

    /** 书名 */
    private String name;

    /** 作者 */
    private String author;

    /** 分类 */
    private String kind;

    /** 字数 */
    private String wordCount;

    /** 最近章节 */
    private String lastChapter;

    /** 简介 */
    private String intro;

    /** 封面 URL */
    private String coverUrl;

    /** 详情页 URL */
    private String detailUrl;

    /** 变量 */
    private String variable;

    public SearchBook() {
    }

    public SearchBook(String bookUrl, String origin, String originName) {
        this.bookUrl = bookUrl;
        this.origin = origin;
        this.originName = originName;
    }

    // ============ getter / setter ============

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
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

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }
}
