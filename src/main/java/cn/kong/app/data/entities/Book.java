package cn.kong.app.data.entities;

import cn.kong.app.constant.AppConst;
import cn.kong.app.utils.JsonUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 书籍实体
 * 改造自 reader-dev 的 Book.kt
 */
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 书籍 ID（本地生成） */
    private String bookId;

    /** 书名 */
    private String name;

    /** 作者 */
    private String author;

    /** 分类 */
    private String kind;

    /** 自定义分类 */
    private String customCover;

    /** 字数 */
    private String wordCount;

    /** 最近章节 */
    private String lastChapterName;

    /** 简介 */
    private String intro;

    /** 封面 URL */
    private String coverUrl;

    /** 自定义封面 URL */
    private String customCoverUrl;

    /** 详情页 URL */
    private String bookUrl;

    /** 目录页 URL */
    private String tocUrl;

    /** 书源 URL */
    private String origin;

    /** 书源名称 */
    private String originName;

    /** 书源类型 */
    private Integer type = AppConst.BookType.TEXT;

    /** 读音分组 */
    private String group;

    /** 最新章节时间（时间戳） */
    private Long lastCheckTime = 0L;

    /** 最近更新时间（时间戳） */
    private Long lastUpdateTime = 0L;

    /** 总章节数 */
    private Integer totalChapterNum = 0;

    /** 当前阅读章节索引 */
    private Integer durChapterIndex = 0;

    /** 当前阅读章节标题 */
    private String durChapterTitle;

    /** 当前阅读章节位置（百分比） */
    private Integer durChapterPos = 0;

    /** 当前阅读页码 */
    private Integer durChapterPage = 0;

    /** 阅读进度（百分比） */
    private Integer durReadingTime = 0;

    /** 章节列表（JSON 字符串） */
    private String chapterListJson;

    /** 变量（JSON 字符串，用于书源规则中的变量传递） */
    private String variable;

    /** 自定义变量 */
    private String customVariable;

    /** 是否本地书籍 */
    private Boolean local = false;

    /** 章节列表（运行时使用，不序列化到数据库） */
    private transient List<BookChapter> chapterList = new ArrayList<>();

    // ============ 便捷方法 ============

    public static Book fromJson(String json) {
        return JsonUtils.fromJson(json, Book.class);
    }

    public String toJson() {
        return JsonUtils.toJson(this);
    }

    public boolean isLocal() {
        return local != null && local;
    }

    public boolean isTypeText() {
        return type != null && type == AppConst.BookType.TEXT;
    }

    public boolean isTypeImage() {
        return type != null && type == AppConst.BookType.IMAGE;
    }

    public boolean isTypeAudio() {
        return type != null && type == AppConst.BookType.AUDIO;
    }

    // ============ getter / setter ============

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
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

    public String getCustomCover() {
        return customCover;
    }

    public void setCustomCover(String customCover) {
        this.customCover = customCover;
    }

    public String getWordCount() {
        return wordCount;
    }

    public void setWordCount(String wordCount) {
        this.wordCount = wordCount;
    }

    public String getLastChapterName() {
        return lastChapterName;
    }

    public void setLastChapterName(String lastChapterName) {
        this.lastChapterName = lastChapterName;
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

    public String getCustomCoverUrl() {
        return customCoverUrl;
    }

    public void setCustomCoverUrl(String customCoverUrl) {
        this.customCoverUrl = customCoverUrl;
    }

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public String getTocUrl() {
        return tocUrl;
    }

    public void setTocUrl(String tocUrl) {
        this.tocUrl = tocUrl;
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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Long getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(Long lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Integer getTotalChapterNum() {
        return totalChapterNum;
    }

    public void setTotalChapterNum(Integer totalChapterNum) {
        this.totalChapterNum = totalChapterNum;
    }

    public Integer getDurChapterIndex() {
        return durChapterIndex;
    }

    public void setDurChapterIndex(Integer durChapterIndex) {
        this.durChapterIndex = durChapterIndex;
    }

    public String getDurChapterTitle() {
        return durChapterTitle;
    }

    public void setDurChapterTitle(String durChapterTitle) {
        this.durChapterTitle = durChapterTitle;
    }

    public Integer getDurChapterPos() {
        return durChapterPos;
    }

    public void setDurChapterPos(Integer durChapterPos) {
        this.durChapterPos = durChapterPos;
    }

    public Integer getDurChapterPage() {
        return durChapterPage;
    }

    public void setDurChapterPage(Integer durChapterPage) {
        this.durChapterPage = durChapterPage;
    }

    public Integer getDurReadingTime() {
        return durReadingTime;
    }

    public void setDurReadingTime(Integer durReadingTime) {
        this.durReadingTime = durReadingTime;
    }

    public String getChapterListJson() {
        return chapterListJson;
    }

    public void setChapterListJson(String chapterListJson) {
        this.chapterListJson = chapterListJson;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getCustomVariable() {
        return customVariable;
    }

    public void setCustomVariable(String customVariable) {
        this.customVariable = customVariable;
    }

    public Boolean getLocal() {
        return local;
    }

    public void setLocal(Boolean local) {
        this.local = local;
    }

    public List<BookChapter> getChapterList() {
        return chapterList;
    }

    public void setChapterList(List<BookChapter> chapterList) {
        this.chapterList = chapterList;
    }
}
