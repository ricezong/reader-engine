package cn.kong.app.data.entities.rule;

import java.io.Serializable;

/**
 * 详情页规则
 * 改造自 reader-dev 的 BookInfoRule.kt
 */
public class BookInfoRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 初始化规则（用于预处理页面） */
    private String init;

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

    /** 目录 URL 规则（如果目录页与详情页不同） */
    private String tocUrl;

    /** 是否分页 */
    private String kind;

    // ============ getter / setter ============

    public String getInit() {
        return init;
    }

    public void setInit(String init) {
        this.init = init;
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

    public String getTocUrl() {
        return tocUrl;
    }

    public void setTocUrl(String tocUrl) {
        this.tocUrl = tocUrl;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
