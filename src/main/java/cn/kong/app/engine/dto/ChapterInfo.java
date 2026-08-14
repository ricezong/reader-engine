package cn.kong.app.engine.dto;

/**
 * 章节信息 - 通用 DTO
 * <p>
 * 不暴露 BookChapter 内部对象，只返回简单的数据结构。
 */
public class ChapterInfo {

    /** 章节标题 */
    private String title;
    /** 章节 URL */
    private String url;
    /** 章节序号（从 0 开始） */
    private int index;

    public ChapterInfo() {}

    public ChapterInfo(String title, String url, int index) {
        this.title = title;
        this.url = url;
        this.index = index;
    }

    // Getters & Setters

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    @Override
    public String toString() {
        return "ChapterInfo{" +
                "index=" + index +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
