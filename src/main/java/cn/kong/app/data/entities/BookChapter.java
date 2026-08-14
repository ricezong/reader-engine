package cn.kong.app.data.entities;

import java.io.Serializable;

/**
 * 章节实体
 * 改造自 reader-dev 的 BookChapter.kt
 */
public class BookChapter implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 章节 URL */
    private String url;

    /** 章节标题 */
    private String title;

    /** 章节索引 */
    private Integer index;

    /** 是否 VIP */
    private Boolean isVip = false;

    /** 是否付费 */
    private Boolean isPay = false;

    /** 更新时间 */
    private String updateTime;

    /** 变量 */
    private String variable;

    /** 标题（用于显示） */
    private String displayTitle;

    public BookChapter() {
    }

    public BookChapter(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public BookChapter(String url, String title, Integer index) {
        this.url = url;
        this.title = title;
        this.index = index;
    }

    // ============ getter / setter ============

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Boolean getIsVip() {
        return isVip;
    }

    public void setIsVip(Boolean isVip) {
        this.isVip = isVip;
    }

    public Boolean getIsPay() {
        return isPay;
    }

    public void setIsPay(Boolean isPay) {
        this.isPay = isPay;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }
}
