package cn.kong.app.data.entities.rule;

import java.io.Serializable;

/**
 * 目录页规则
 * 改造自 reader-dev 的 TocRule.kt
 */
public class TocRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目录列表规则 */
    private String chapterList;

    /** 章节名规则 */
    private String chapterName;

    /** 章节 URL 规则 */
    private String chapterUrl;

    /** 是否 VIP 规则 */
    private String isVip;

    /** 是否付费规则 */
    private String isPay;

    /** 是否更新规则 */
    private String updateTime;

    /** 下一页规则（用于分页目录） */
    private String nextTocUrl;

    // ============ getter / setter ============

    public String getChapterList() {
        return chapterList;
    }

    public void setChapterList(String chapterList) {
        this.chapterList = chapterList;
    }

    public String getChapterName() {
        return chapterName;
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }

    public String getChapterUrl() {
        return chapterUrl;
    }

    public void setChapterUrl(String chapterUrl) {
        this.chapterUrl = chapterUrl;
    }

    public String getIsVip() {
        return isVip;
    }

    public void setIsVip(String isVip) {
        this.isVip = isVip;
    }

    public String getIsPay() {
        return isPay;
    }

    public void setIsPay(String isPay) {
        this.isPay = isPay;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getNextTocUrl() {
        return nextTocUrl;
    }

    public void setNextTocUrl(String nextTocUrl) {
        this.nextTocUrl = nextTocUrl;
    }
}
