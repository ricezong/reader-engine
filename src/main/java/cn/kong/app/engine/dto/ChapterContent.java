package cn.kong.app.engine.dto;

/**
 * 章节正文 - 通用 DTO
 * <p>
 * 用于批量下载结果，包含章节信息和正文内容。
 */
public class ChapterContent {

    /** 章节标题 */
    private String title;
    /** 章节 URL */
    private String url;
    /** 章节序号（从 0 开始） */
    private int index;
    /** 正文内容 */
    private String content;

    public ChapterContent() {}

    public ChapterContent(String title, String url, int index, String content) {
        this.title = title;
        this.url = url;
        this.index = index;
        this.content = content;
    }

    // Getters & Setters

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public String toString() {
        return "ChapterContent{" +
                "index=" + index +
                ", title='" + title + '\'' +
                ", contentLength=" + (content == null ? 0 : content.length()) +
                '}';
    }
}
