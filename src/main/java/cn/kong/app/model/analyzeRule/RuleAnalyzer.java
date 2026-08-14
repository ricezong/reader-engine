package cn.kong.app.model.analyzeRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则分析器
 * 改造自 reader-dev 的 RuleAnalyzer.kt
 * <p>
 * 负责识别规则字符串中的规则类型（CSS / XPath / JSONPath / 正则 / JS），
 * 并将复合规则拆分为规则链。
 * <p>
 * 规则语法说明：
 * <ul>
 *   <li>CSS 选择器：以 @ 或 . 或 # 开头，或直接是标签名</li>
 *   <li>XPath：以 // 或 / 开头</li>
 *   <li>JSONPath：以 $. 开头</li>
 *   <li>正则：以 ## 开头</li>
 *   <li>JS：以 &lt;js&gt; 标签包裹，或 @js: 开头</li>
 * </ul>
 */
public class RuleAnalyzer {

    /** 规则分隔符 @ */
    public static final String RULE_SPLIT_AT = "@";
    /** 规则分隔符 || */
    public static final String RULE_SPLIT_OR = "||";
    /** 规则分隔符 && */
    public static final String RULE_SPLIT_AND = "&&";
    /** 规则分隔符 %% */
    public static final String RULE_SPLIT_PERCENT = "%%";

    /** JS 脚本前缀 */
    public static final String JS_PREFIX = "<js>";
    public static final String JS_SUFFIX = "</js>";
    public static final String JS_PREFIX2 = "@js:";

    /** 正则前缀 */
    public static final String REGEX_PREFIX = "##";

    /** JSONPath 前缀 */
    public static final String JSON_PATH_PREFIX = "$.";

    /** XPath 前缀 */
    public static final String XPATH_PREFIX = "//";
    public static final String XPATH_PREFIX2 = "/";

    /** CSS 属性前缀 */
    public static final String CSS_ATTR_PREFIX = "@";

    private final String ruleStr;
    private int index = 0;

    public RuleAnalyzer(String ruleStr) {
        this.ruleStr = ruleStr == null ? "" : ruleStr;
    }

    /**
     * 判断规则是否为 JSONPath
     */
    public static boolean isJsonPath(String rule) {
        return rule != null && rule.startsWith(JSON_PATH_PREFIX);
    }

    /**
     * 判断规则是否为 XPath
     */
    public static boolean isXPath(String rule) {
        if (rule == null || rule.isEmpty()) {
            return false;
        }
        return rule.startsWith(XPATH_PREFIX) || rule.startsWith(XPATH_PREFIX2);
    }

    /**
     * 判断规则是否为正则
     */
    public static boolean isRegex(String rule) {
        return rule != null && rule.startsWith(REGEX_PREFIX);
    }

    /**
     * 判断规则是否为 JS 脚本
     */
    public static boolean isJs(String rule) {
        if (rule == null || rule.isEmpty()) {
            return false;
        }
        return rule.startsWith(JS_PREFIX) || rule.startsWith(JS_PREFIX2);
    }

    /**
     * 判断规则是否为 CSS 选择器
     */
    public static boolean isCss(String rule) {
        if (rule == null || rule.isEmpty()) {
            return false;
        }
        return !isJsonPath(rule) && !isXPath(rule) && !isRegex(rule) && !isJs(rule);
    }

    /**
     * 提取 JS 脚本内容
     */
    public static String extractJs(String rule) {
        if (rule == null || rule.isEmpty()) {
            return "";
        }
        if (rule.startsWith(JS_PREFIX) && rule.endsWith(JS_SUFFIX)) {
            return rule.substring(JS_PREFIX.length(), rule.length() - JS_SUFFIX.length());
        }
        if (rule.startsWith(JS_PREFIX2)) {
            return rule.substring(JS_PREFIX2.length());
        }
        return rule;
    }

    /**
     * 拆分规则链（按 @ 分隔，但保留 JS 脚本中的 @）
     */
    public List<String> splitRuleChain() {
        List<String> result = new ArrayList<>();
        if (ruleStr.isEmpty()) {
            return result;
        }
        int start = 0;
        boolean inJs = false;
        boolean inQuote = false;
        char quoteChar = 0;
        for (int i = 0; i < ruleStr.length(); i++) {
            char c = ruleStr.charAt(i);
            if (inQuote) {
                if (c == quoteChar) {
                    inQuote = false;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                inQuote = true;
                quoteChar = c;
                continue;
            }
            if (ruleStr.startsWith(JS_PREFIX, i)) {
                inJs = true;
                i += JS_PREFIX.length() - 1;
                continue;
            }
            if (inJs && ruleStr.startsWith(JS_SUFFIX, i)) {
                inJs = false;
                i += JS_SUFFIX.length() - 1;
                continue;
            }
            if (!inJs && c == '@') {
                result.add(ruleStr.substring(start, i));
                start = i + 1;
            }
        }
        result.add(ruleStr.substring(start));
        return result;
    }

    /**
     * 按分隔符拆分规则
     */
    public static List<String> splitBySeparator(String rule, String separator) {
        List<String> result = new ArrayList<>();
        if (rule == null || rule.isEmpty()) {
            return result;
        }
        int idx = rule.indexOf(separator);
        if (idx < 0) {
            result.add(rule);
            return result;
        }
        int start = 0;
        while (idx >= 0) {
            result.add(rule.substring(start, idx));
            start = idx + separator.length();
            idx = rule.indexOf(separator, start);
        }
        result.add(rule.substring(start));
        return result;
    }

    /**
     * 提取 CSS 规则中的属性名
     * 例如：div.content@text 提取出 "text"
     */
    public static String getCssAttr(String rule) {
        if (rule == null || rule.isEmpty()) {
            return "";
        }
        int idx = rule.lastIndexOf(CSS_ATTR_PREFIX);
        if (idx < 0 || idx >= rule.length() - 1) {
            return "";
        }
        return rule.substring(idx + 1);
    }

    /**
     * 移除 CSS 规则中的属性部分
     */
    public static String removeCssAttr(String rule) {
        if (rule == null || rule.isEmpty()) {
            return rule;
        }
        int idx = rule.lastIndexOf(CSS_ATTR_PREFIX);
        if (idx < 0) {
            return rule;
        }
        return rule.substring(0, idx);
    }

    /**
     * 获取规则字符串
     */
    public String getRuleStr() {
        return ruleStr;
    }
}
