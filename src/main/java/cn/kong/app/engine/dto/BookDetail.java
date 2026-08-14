package cn.kong.app.engine.dto;

/**
 * 书籍详情 - 通用 DTO
 * <p>
 * 不暴露 Book 内部对象，只返回简单的数据结构。
 */
public class BookDetail {

    /** 书名 */
    private String name;
    /** 作者 */
    private String author;
    /** 书籍详情页 URL */
    private String bookUrl;
    /** 目录页 URL */
    private String tocUrl;
    /** 书源 URL */
    private String sourceUrl;
    /** 书源名称 */
    private String sourceName;
    /** 封面 URL */
    private String coverUrl;
    /** 简介 */
    private String intro;
    /** 分类标签 */
    private String kind;
    /** 字数 */
    private String wordCount;
    /** 最新章节标题 */
    private String latestChapterTitle;
    /** 类型：0=小说, 2=漫画 */
    private int type;

    public BookDetail() {}

    // Getters & Setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getBookUrl() { return bookUrl; }
    public void setBookUrl(String bookUrl) { this.bookUrl = bookUrl; }

    public String getTocUrl() { return tocUrl; }
    public void setTocUrl(String tocUrl) { this.tocUrl = tocUrl; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getWordCount() { return wordCount; }
    public void setWordCount(String wordCount) { this.wordCount = wordCount; }

    public String getLatestChapterTitle() { return latestChapterTitle; }
    public void setLatestChapterTitle(String latestChapterTitle) { this.latestChapterTitle = latestChapterTitle; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    @Override
    public String toString() {
        return "BookDetail{" +
                "name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", bookUrl='" + bookUrl + '\'' +
                ", sourceName='" + sourceName + '\'' +
                '}';
    }
}
