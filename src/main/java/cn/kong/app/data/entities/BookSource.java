package cn.kong.app.data.entities;

import cn.kong.app.constant.AppConst;
import cn.kong.app.data.entities.rule.BookInfoRule;
import cn.kong.app.data.entities.rule.ContentRule;
import cn.kong.app.data.entities.rule.ExploreRule;
import cn.kong.app.data.entities.rule.LoginRule;
import cn.kong.app.data.entities.rule.SearchRule;
import cn.kong.app.data.entities.rule.TocRule;
import cn.kong.app.utils.JsonUtils;

import java.util.Objects;

/**
 * 书源规则实体
 * 改造自 reader-dev 的 BookSource.kt
 * <p>
 * 书源是一份结构化的 JSON 配置文件，使用 CSS 选择器 / XPath / JSONPath 语法提取网页内容，
 * 并支持嵌入 JavaScript 脚本处理复杂的反爬、解密逻辑。
 */
public class BookSource extends BaseSource {

    private static final long serialVersionUID = 1L;

    /** 书源名称 */
    private String bookSourceName;

    /** 书源 URL（唯一标识） */
    private String bookSourceUrl;

    /** 书源分组 */
    private String bookSourceGroup;

    /** 书源类型：0=文本（小说），1=音频，2=图片（漫画） */
    private Integer bookSourceType = AppConst.BookType.TEXT;

    /** 书源注释/说明 */
    private String bookSourceComment;

    /** 是否启用 */
    private Boolean enabled = true;

    /** 是否启用发现页 */
    private Boolean enabledExplore = true;

    /** 最后更新时间（时间戳） */
    private Long lastUpdateTime = 0L;

    /** 响应时间（毫秒，用于排序） */
    private Integer respondTime = 0;

    /** 权重（用于排序） */
    private Integer weight = 0;

    /** 自定义排序 */
    private Integer customOrder;

    /** 搜索 URL（支持 POST/GET，可带 JSON 配置） */
    private String searchUrl;

    /** 发现 URL（多行，格式：名称::url） */
    private String exploreUrl;

    /** 登录 URL */
    private String loginUrl;

    /** 是否启用 Cookie Jar */
    private Boolean enabledCookieJar = false;

    /** 变量注释（加密的 JS 代码，通过 AES 解密后 eval 执行） */
    private String variableComment;

    /** 并发率 */
    private String concurrentRate;

    /** 书籍 URL 正则 */
    private String bookUrlPattern;

    /** 搜索规则 */
    private SearchRule ruleSearch = new SearchRule();

    /** 发现规则 */
    private ExploreRule ruleExplore = new ExploreRule();

    /** 详情页规则 */
    private BookInfoRule ruleBookInfo = new BookInfoRule();

    /** 目录页规则 */
    private TocRule ruleToc = new TocRule();

    /** 正文页规则 */
    private ContentRule ruleContent = new ContentRule();

    /** 登录规则 */
    private LoginRule ruleLogin = new LoginRule();

    // ============ 便捷方法 ============

    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public boolean isTypeText() {
        return bookSourceType != null && bookSourceType == AppConst.BookType.TEXT;
    }

    public boolean isTypeAudio() {
        return bookSourceType != null && bookSourceType == AppConst.BookType.AUDIO;
    }

    public boolean isTypeImage() {
        return bookSourceType != null && bookSourceType == AppConst.BookType.IMAGE;
    }

    /**
     * 从 JSON 字符串反序列化
     */
    public static BookSource fromJson(String json) {
        return JsonUtils.fromJson(json, BookSource.class);
    }

    /**
     * 序列化为 JSON 字符串
     */
    public String toJson() {
        return JsonUtils.toJson(this);
    }

    // ============ getter / setter ============

    public String getBookSourceName() {
        return bookSourceName;
    }

    public void setBookSourceName(String bookSourceName) {
        this.bookSourceName = bookSourceName;
    }

    public String getBookSourceUrl() {
        return bookSourceUrl;
    }

    public void setBookSourceUrl(String bookSourceUrl) {
        this.bookSourceUrl = bookSourceUrl;
    }

    public String getBookSourceGroup() {
        return bookSourceGroup;
    }

    public void setBookSourceGroup(String bookSourceGroup) {
        this.bookSourceGroup = bookSourceGroup;
    }

    public Integer getBookSourceType() {
        return bookSourceType;
    }

    public void setBookSourceType(Integer bookSourceType) {
        this.bookSourceType = bookSourceType;
    }

    public String getBookSourceComment() {
        return bookSourceComment;
    }

    public void setBookSourceComment(String bookSourceComment) {
        this.bookSourceComment = bookSourceComment;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEnabledExplore() {
        return enabledExplore;
    }

    public void setEnabledExplore(Boolean enabledExplore) {
        this.enabledExplore = enabledExplore;
    }

    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Integer getRespondTime() {
        return respondTime;
    }

    public void setRespondTime(Integer respondTime) {
        this.respondTime = respondTime;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getCustomOrder() {
        return customOrder;
    }

    public void setCustomOrder(Integer customOrder) {
        this.customOrder = customOrder;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    public String getExploreUrl() {
        return exploreUrl;
    }

    public void setExploreUrl(String exploreUrl) {
        this.exploreUrl = exploreUrl;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public Boolean getEnabledCookieJar() {
        return enabledCookieJar;
    }

    public void setEnabledCookieJar(Boolean enabledCookieJar) {
        this.enabledCookieJar = enabledCookieJar;
    }

    public String getVariableComment() {
        return variableComment;
    }

    public void setVariableComment(String variableComment) {
        this.variableComment = variableComment;
    }

    public String getConcurrentRate() {
        return concurrentRate;
    }

    public void setConcurrentRate(String concurrentRate) {
        this.concurrentRate = concurrentRate;
    }

    public String getBookUrlPattern() {
        return bookUrlPattern;
    }

    public void setBookUrlPattern(String bookUrlPattern) {
        this.bookUrlPattern = bookUrlPattern;
    }

    public SearchRule getRuleSearch() {
        return ruleSearch;
    }

    public void setRuleSearch(SearchRule ruleSearch) {
        this.ruleSearch = ruleSearch;
    }

    public ExploreRule getRuleExplore() {
        return ruleExplore;
    }

    public void setRuleExplore(ExploreRule ruleExplore) {
        this.ruleExplore = ruleExplore;
    }

    public BookInfoRule getRuleBookInfo() {
        return ruleBookInfo;
    }

    public void setRuleBookInfo(BookInfoRule ruleBookInfo) {
        this.ruleBookInfo = ruleBookInfo;
    }

    public TocRule getRuleToc() {
        return ruleToc;
    }

    public void setRuleToc(TocRule ruleToc) {
        this.ruleToc = ruleToc;
    }

    public ContentRule getRuleContent() {
        return ruleContent;
    }

    public void setRuleContent(ContentRule ruleContent) {
        this.ruleContent = ruleContent;
    }

    public LoginRule getRuleLogin() {
        return ruleLogin;
    }

    public void setRuleLogin(LoginRule ruleLogin) {
        this.ruleLogin = ruleLogin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookSource that = (BookSource) o;
        return Objects.equals(bookSourceUrl, that.bookSourceUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookSourceUrl);
    }

    @Override
    public String toString() {
        return "BookSource{" +
            "bookSourceName='" + bookSourceName + '\'' +
            ", bookSourceUrl='" + bookSourceUrl + '\'' +
            ", bookSourceType=" + bookSourceType +
            '}';
    }

    // ============ JS 脚本中调用的方法 ============

    /** 书源变量（持久化存储，JSON 格式） */
    private String variable;

    /**
     * 获取书源变量（JS 中 source.getVariable() 调用）
     */
    public String getVariable() {
        return variable;
    }

    /**
     * 设置书源变量（JS 中 source.setVariable() 调用）
     */
    public void setVariable(String variable) {
        this.variable = variable;
    }
}
