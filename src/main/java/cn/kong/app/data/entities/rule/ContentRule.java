package cn.kong.app.data.entities.rule;

import java.io.Serializable;

/**
 * 正文页规则
 * 改造自 reader-dev 的 ContentRule.kt
 * <p>
 * 对于小说书源（bookSourceType=0），提取正文文本段落；
 * 对于漫画源（bookSourceType=2），提取图片 URL 列表。
 */
public class ContentRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 正文规则 */
    private String content;

    /** 下一页规则（用于分页正文） */
    private String nextContentUrl;

    /** 图片 URL 规则（漫画源用） */
    private String imageStyle;

    /** 替换规则（用于净化正文） */
    private String replaceRegex;

    /** 替换为 */
    private String replacement;

    /** 图片 URL 规则 */
    private String imageUrls;

    // ============ getter / setter ============

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNextContentUrl() {
        return nextContentUrl;
    }

    public void setNextContentUrl(String nextContentUrl) {
        this.nextContentUrl = nextContentUrl;
    }

    public String getImageStyle() {
        return imageStyle;
    }

    public void setImageStyle(String imageStyle) {
        this.imageStyle = imageStyle;
    }

    public String getReplaceRegex() {
        return replaceRegex;
    }

    public void setReplaceRegex(String replaceRegex) {
        this.replaceRegex = replaceRegex;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }
}
