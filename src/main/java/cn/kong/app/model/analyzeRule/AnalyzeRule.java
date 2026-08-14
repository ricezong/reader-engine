package cn.kong.app.model.analyzeRule;

import cn.kong.app.help.js.JsExtensions;
import cn.kong.app.help.js.JsEngine;
import cn.kong.app.utils.StringUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一规则分析器
 * 改造自 reader-dev 的 AnalyzeRule.kt
 * <p>
 * 这是书源解析引擎的核心类，负责统一调度 CSS 选择器、XPath、JSONPath、正则、JS 五种解析方式。
 * <p>
 * 规则语法识别：
 * <ul>
 *   <li>以 $. 开头 → JSONPath</li>
 *   <li>以 // 或 / 开头 → XPath</li>
 *   <li>以 ## 开头 → 正则</li>
 *   <li>以 &lt;js&gt; 包裹或 @js: 开头 → JS 脚本</li>
 *   <li>其他 → CSS 选择器</li>
 * </ul>
 * <p>
 * 支持规则链：rule1@rule2@rule3，依次应用规则。
 * 支持规则或：rule1||rule2，返回第一个非空结果。
 * 支持规则与：rule1&&rule2，拼接所有结果。
 *
 * @param <T> 解析源类型
 */
public class AnalyzeRule<T> {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeRule.class);

    /** 解析源 */
    private T source;

    /** JS 引擎 */
    private final JsEngine jsEngine = JsEngine.getInstance();

    /** JS 扩展对象（提供 java.ajax, cache, Get, Put 等） */
    private JsExtensions jsExtensions;

    /** 书源变量 */
    private String rule;

    /** 变量映射 */
    private Map<String, Object> variables = new HashMap<>();

    public AnalyzeRule() {
    }

    public AnalyzeRule(T source) {
        this.source = source;
    }

    /**
     * 设置解析源
     */
    public AnalyzeRule<T> setContent(T source) {
        this.source = source;
        return this;
    }

    /**
     * 获取解析源
     */
    public T getContent() {
        return source;
    }

    /**
     * 设置变量
     */
    public AnalyzeRule<T> putVariable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    /**
     * 获取变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 获取所有变量
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * 设置变量映射
     */
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables != null ? variables : new HashMap<>();
    }

    /**
     * 设置 JS 扩展对象
     */
    public void setJsExtensions(JsExtensions jsExtensions) {
        this.jsExtensions = jsExtensions;
    }

    /**
     * 解析单值
     *
     * @param rule 规则
     * @return 解析结果
     */
    public String getString(String rule) {
        return getString(rule, "");
    }

    /**
     * 解析单值（带默认值）
     */
    public String getString(String rule, String def) {
        if (StringUtils.isEmpty(rule)) {
            return def;
        }
        try {
            String result = evalRule(rule);
            return StringUtils.isEmpty(result) ? def : result;
        } catch (Exception e) {
            log.warn("规则解析失败: rule={}, error={}", rule, e.getMessage());
            return def;
        }
    }

    /**
     * 解析单值并去除首尾空白
     */
    public String getStringFirst(String rule) {
        String result = getString(rule);
        return result == null ? "" : result.trim();
    }

    /**
     * 解析多值列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String rule) {
        if (StringUtils.isEmpty(rule)) {
            return new ArrayList<>();
        }
        try {
            Object result = evalRuleList(rule);
            if (result instanceof List) {
                List<String> list = new ArrayList<>();
                for (Object item : (List<Object>) result) {
                    if (item != null) {
                        list.add(item.toString());
                    }
                }
                return list;
            }
            List<String> list = new ArrayList<>();
            if (result != null) {
                list.add(result.toString());
            }
            return list;
        } catch (Exception e) {
            log.warn("规则列表解析失败: rule={}, error={}", rule, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析元素列表
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> getElements(String rule) {
        if (StringUtils.isEmpty(rule)) {
            return new ArrayList<>();
        }
        if (source == null) {
            return new ArrayList<>();
        }
        try {
            RuleType ruleType = detectRuleType(rule);
            switch (ruleType) {
                case CSS:
                    AnalyzeByJSoup jSoup = new AnalyzeByJSoup();
                    if (source instanceof String) {
                        jSoup.parse((String) source);
                    } else if (source instanceof Element) {
                        jSoup.parse((Element) source);
                    }
                    List<Element> elements = jSoup.getElements(rule);
                    jSoup.release();
                    return (List<E>) elements;
                case XPATH:
                    AnalyzeByXPath xPath = new AnalyzeByXPath();
                    if (source instanceof String) {
                        xPath.parse((String) source);
                    } else if (source instanceof Element) {
                        xPath.parse((Element) source);
                    }
                    List<Element> xElements = xPath.getElements(rule);
                    xPath.release();
                    return (List<E>) xElements;
                case JSON_PATH:
                    AnalyzeByJSonPath jsonPath = new AnalyzeByJSonPath();
                    if (source instanceof String) {
                        jsonPath.parse((String) source);
                    } else {
                        jsonPath.parse(source);
                    }
                    List<Object> objects = jsonPath.getElements(rule);
                    jsonPath.release();
                    return (List<E>) objects;
                default:
                    return new ArrayList<>();
            }
        } catch (Exception e) {
            log.warn("元素列表解析失败: rule={}, error={}", rule, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析 URL（带 JS 预处理）
     */
    public String getUrl(String rule) {
        return getString(rule);
    }

    /**
     * 执行规则
     */
    private String evalRule(String rule) {
        // 处理 || 或规则
        if (rule.contains(RuleAnalyzer.RULE_SPLIT_OR)) {
            List<String> rules = RuleAnalyzer.splitBySeparator(rule, RuleAnalyzer.RULE_SPLIT_OR);
            for (String r : rules) {
                String result = evalSingleRule(r.trim());
                if (StringUtils.isNotEmpty(result)) {
                    return result;
                }
            }
            return "";
        }
        // 处理 && 与规则
        if (rule.contains(RuleAnalyzer.RULE_SPLIT_AND)) {
            List<String> rules = RuleAnalyzer.splitBySeparator(rule, RuleAnalyzer.RULE_SPLIT_AND);
            StringBuilder sb = new StringBuilder();
            for (String r : rules) {
                String result = evalSingleRule(r.trim());
                if (StringUtils.isNotEmpty(result)) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(result);
                }
            }
            return sb.toString();
        }
        return evalSingleRule(rule);
    }

    /**
     * 执行单条规则
     */
    private String evalSingleRule(String rule) {
        if (StringUtils.isEmpty(rule)) {
            return "";
        }
        // 处理 JS 脚本
        if (rule.startsWith(RuleAnalyzer.JS_PREFIX) || rule.startsWith(RuleAnalyzer.JS_PREFIX2)) {
            return evalJs(rule);
        }
        // 处理正则
        if (rule.startsWith(RuleAnalyzer.REGEX_PREFIX)) {
            return evalRegex(rule);
        }
        // 处理规则链 @（仅当 @ 后面不是已知 CSS 属性时才拆分）
        int atIdx = rule.indexOf(RuleAnalyzer.RULE_SPLIT_AT);
        if (atIdx > 0 && !rule.startsWith(RuleAnalyzer.RULE_SPLIT_AT)) {
            String afterAt = rule.substring(atIdx + 1);
            if (!isCssAttr(afterAt)) {
                return evalRuleChain(rule);
            }
        }
        // 根据类型分发
        RuleType ruleType = detectRuleType(rule);
        return evalByType(rule, ruleType);
    }

    /**
     * 判断 @ 后面的内容是否为 CSS 属性（而非规则链）
     * CSS 属性：text, html, outerHtml, ownText, textNodes, href, src, title, alt, value, name, class, id 等
     * 注意：## 是正则替换符，不是规则链分隔符
     */
    private boolean isCssAttr(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        // 先移除 ## 后面的正则替换部分
        int regexIdx = str.indexOf("##");
        if (regexIdx >= 0) {
            str = str.substring(0, regexIdx);
        }
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        // 如果包含 @ 或 || 或 && 或 <js>，说明是规则链
        if (str.contains("@") || str.contains("||") || str.contains("&&") || str.contains("<js>")) {
            return false;
        }
        // 已知的 CSS 属性
        return str.equals("text") || str.equals("html") || str.equals("outerHtml")
            || str.equals("ownText") || str.equals("textNodes")
            || str.equals("href") || str.equals("src") || str.equals("title")
            || str.equals("alt") || str.equals("value") || str.equals("name")
            || str.equals("class") || str.equals("id") || str.equals("content")
            || str.equals("data-src") || str.equals("data-original");
    }

    /**
     * 执行规则列表
     */
    private Object evalRuleList(String rule) {
        // 处理 || 或规则
        if (rule.contains(RuleAnalyzer.RULE_SPLIT_OR)) {
            List<String> rules = RuleAnalyzer.splitBySeparator(rule, RuleAnalyzer.RULE_SPLIT_OR);
            for (String r : rules) {
                List<String> result = getStringList(r.trim());
                if (!result.isEmpty()) {
                    return result;
                }
            }
            return new ArrayList<>();
        }
        // 处理 && 与规则
        if (rule.contains(RuleAnalyzer.RULE_SPLIT_AND)) {
            List<String> rules = RuleAnalyzer.splitBySeparator(rule, RuleAnalyzer.RULE_SPLIT_AND);
            List<String> result = new ArrayList<>();
            for (String r : rules) {
                result.addAll(getStringList(r.trim()));
            }
            return result;
        }
        // 单规则
        RuleType ruleType = detectRuleType(rule);
        return getStringListByType(rule, ruleType);
    }

    /**
     * 按类型解析单值
     */
    private String evalByType(String rule, RuleType ruleType) {
        if (source == null) {
            return "";
        }
        switch (ruleType) {
            case CSS:
                return evalCss(rule);
            case XPATH:
                return evalXPath(rule);
            case JSON_PATH:
                return evalJsonPath(rule);
            case REGEX:
                return evalRegex(rule);
            case JS:
                return evalJs(rule);
            default:
                return "";
        }
    }

    /**
     * 按类型解析列表
     */
    private List<String> getStringListByType(String rule, RuleType ruleType) {
        if (source == null) {
            return new ArrayList<>();
        }
        switch (ruleType) {
            case CSS:
                return evalCssList(rule);
            case XPATH:
                return evalXPathList(rule);
            case JSON_PATH:
                return evalJsonPathList(rule);
            default:
                List<String> single = new ArrayList<>();
                String value = evalByType(rule, ruleType);
                if (StringUtils.isNotEmpty(value)) {
                    single.add(value);
                }
                return single;
        }
    }

    /**
     * CSS 选择器解析
     * 支持 ## 正则替换语法：rule##regex##replacement
     */
    private String evalCss(String rule) {
        // 先分离出正则替换部分
        String[] parts = splitRegex(rule);
        String cssRule = parts[0];
        String regex = parts.length > 1 ? parts[1] : null;
        String replacement = parts.length > 2 ? parts[2] : "";

        AnalyzeByJSoup jSoup = new AnalyzeByJSoup();
        try {
            if (source instanceof String) {
                jSoup.parse((String) source);
            } else if (source instanceof Element) {
                jSoup.parse((Element) source);
            } else {
                return "";
            }
            String result = jSoup.getString(cssRule);
            // 应用正则替换
            if (regex != null) {
                result = applyRegex(result, regex, replacement);
            }
            return result;
        } finally {
            jSoup.release();
        }
    }

    /**
     * CSS 选择器列表解析
     * 支持 ## 正则替换语法
     */
    private List<String> evalCssList(String rule) {
        String[] parts = splitRegex(rule);
        String cssRule = parts[0];
        String regex = parts.length > 1 ? parts[1] : null;
        String replacement = parts.length > 2 ? parts[2] : "";

        AnalyzeByJSoup jSoup = new AnalyzeByJSoup();
        try {
            if (source instanceof String) {
                jSoup.parse((String) source);
            } else if (source instanceof Element) {
                jSoup.parse((Element) source);
            } else {
                return new ArrayList<>();
            }
            List<String> result = jSoup.getStringList(cssRule);
            // 应用正则替换
            if (regex != null) {
                List<String> replaced = new ArrayList<>();
                for (String s : result) {
                    replaced.add(applyRegex(s, regex, replacement));
                }
                return replaced;
            }
            return result;
        } finally {
            jSoup.release();
        }
    }

    /**
     * 分离规则中的正则替换部分
     * 例如：a.1@text##\\《|\\》.*  =>  ["a.1@text", "\\《|\\》.*"]
     *       id.content@text##xxx##yyy => ["id.content@text", "xxx", "yyy"]
     */
    private String[] splitRegex(String rule) {
        if (rule == null || !rule.contains("##")) {
            return new String[]{rule};
        }
        // 只在第一个 ## 处分割（CSS 规则部分不应包含 ##）
        int idx = rule.indexOf("##");
        String cssRule = rule.substring(0, idx);
        String regexPart = rule.substring(idx + 2);
        // regexPart 可能是 "regex" 或 "regex##replacement"
        int repIdx = regexPart.indexOf("##");
        if (repIdx >= 0) {
            String regex = regexPart.substring(0, repIdx);
            String replacement = regexPart.substring(repIdx + 2);
            return new String[]{cssRule, regex, replacement};
        }
        return new String[]{cssRule, regexPart, ""};
    }

    /**
     * 应用正则替换
     * 支持格式：regex 或 regex:group
     */
    private String applyRegex(String content, String regex, String replacement) {
        if (content == null || regex == null || regex.isEmpty()) {
            return content;
        }
        try {
            // 处理 group 语法：regex@group
            int group = 0;
            String patternStr = regex;
            int atIdx = regex.lastIndexOf('@');
            if (atIdx >= 0 && atIdx < regex.length() - 1) {
                String groupStr = regex.substring(atIdx + 1);
                try {
                    group = Integer.parseInt(groupStr);
                    patternStr = regex.substring(0, atIdx);
                } catch (NumberFormatException e) {
                    // 不是 group 语法，保持原样
                }
            }
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (group > 0) {
                if (matcher.find()) {
                    return matcher.group(group);
                }
                return content;
            }
            return matcher.replaceAll(replacement == null ? "" : replacement);
        } catch (Exception e) {
            log.warn("正则替换失败: regex={}, error={}", regex, e.getMessage());
            return content;
        }
    }

    /**
     * XPath 解析
     */
    private String evalXPath(String rule) {
        AnalyzeByXPath xPath = new AnalyzeByXPath();
        try {
            if (source instanceof String) {
                xPath.parse((String) source);
            } else if (source instanceof Element) {
                xPath.parse((Element) source);
            } else {
                return "";
            }
            return xPath.getString(rule);
        } finally {
            xPath.release();
        }
    }

    /**
     * XPath 列表解析
     */
    private List<String> evalXPathList(String rule) {
        AnalyzeByXPath xPath = new AnalyzeByXPath();
        try {
            if (source instanceof String) {
                xPath.parse((String) source);
            } else if (source instanceof Element) {
                xPath.parse((Element) source);
            } else {
                return new ArrayList<>();
            }
            return xPath.getStringList(rule);
        } finally {
            xPath.release();
        }
    }

    /**
     * JSONPath 解析
     */
    private String evalJsonPath(String rule) {
        AnalyzeByJSonPath jsonPath = new AnalyzeByJSonPath();
        try {
            if (source instanceof String) {
                jsonPath.parse((String) source);
            } else {
                jsonPath.parse(source);
            }
            return jsonPath.getString(rule);
        } finally {
            jsonPath.release();
        }
    }

    /**
     * JSONPath 列表解析
     */
    private List<String> evalJsonPathList(String rule) {
        AnalyzeByJSonPath jsonPath = new AnalyzeByJSonPath();
        try {
            if (source instanceof String) {
                jsonPath.parse((String) source);
            } else {
                jsonPath.parse(source);
            }
            return jsonPath.getStringList(rule);
        } finally {
            jsonPath.release();
        }
    }

    /**
     * 正则解析
     * 格式：##正则##替换为##取第几个
     */
    private String evalRegex(String rule) {
        if (source == null) {
            return "";
        }
        String content = source instanceof String ? (String) source : source.toString();
        String[] parts = rule.split("##");
        if (parts.length < 2) {
            return content;
        }
        String regex = parts[1];
        String replacement = parts.length > 2 ? parts[2] : "";
        int group = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 0;

        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                if (group == 0) {
                    return matcher.replaceAll(replacement);
                } else {
                    return matcher.group(group);
                }
            }
            return replacement.isEmpty() ? "" : content.replaceAll(regex, replacement);
        } catch (Exception e) {
            log.warn("正则解析失败: regex={}, error={}", regex, e.getMessage());
            return content;
        }
    }

    /**
     * JS 脚本解析
     * 支持 <js>...</js> 和 @js: 前缀
     * 支持 Put() 输出结果
     */
    private String evalJs(String rule) {
        String js;
        if (rule.startsWith(RuleAnalyzer.JS_PREFIX)) {
            js = rule.substring(RuleAnalyzer.JS_PREFIX.length(),
                rule.lastIndexOf(RuleAnalyzer.JS_SUFFIX));
        } else if (rule.startsWith(RuleAnalyzer.JS_PREFIX2)) {
            js = rule.substring(RuleAnalyzer.JS_PREFIX2.length());
        } else {
            return "";
        }
        Map<String, Object> vars = new HashMap<>(variables);
        vars.put("result", source instanceof String ? source : "");
        vars.put("baseUrl", variables.getOrDefault("baseUrl", ""));

        // 创建 JS 扩展对象
        JsExtensions ext = jsExtensions;
        Object result = jsEngine.eval(js, vars, ext);

        // 如果 JS 脚本调用了 Put()，优先返回 Put 的值
        if (ext != null && ext.getPutResult() != null) {
            return ext.getPutResult().toString();
        }
        return result == null ? "" : result.toString();
    }

    /**
     * 规则链解析
     * 支持 Legado 的多级 @ 规则链语法：
     *   id.soft_info_para@h1@text##TXT.*
     *   含义：选择 id=soft_info_para 的元素 → 在其下找 h1 标签 → 提取文本 → 正则替换
     * <p>
     * 规则链中每一步：
     *   - 如果是纯 CSS 选择器（无 @attr），返回 Element 以便继续链式解析
     *   - 如果带 @attr（如 @text, @href），提取属性值返回字符串
     *   - ##regex 是正则替换，作用于当前结果
     */
    @SuppressWarnings("unchecked")
    private String evalRuleChain(String rule) {
        // 先分离出正则替换部分（## 后面的内容）
        String regexPart = null;
        String mainRule = rule;
        int regexIdx = rule.indexOf("##");
        if (regexIdx >= 0) {
            regexPart = rule.substring(regexIdx);
            mainRule = rule.substring(0, regexIdx);
        }

        // 按 @ 分割规则链
        String[] chainParts = mainRule.split(RuleAnalyzer.RULE_SPLIT_AT);
        if (chainParts.length == 0) {
            return "";
        }

        Object currentSource = source;
        String result = "";

        for (int i = 0; i < chainParts.length; i++) {
            String partRule = chainParts[i].trim();
            if (partRule.isEmpty()) {
                continue;
            }

            // 判断当前规则是否为属性提取（text, href 等）
            boolean isAttr = isCssAttr(partRule);

            if (isAttr && currentSource instanceof Element) {
                // 属性提取（需要加 @ 前缀，因为 getValue 依赖 getCssAttr 解析 @）
                result = AnalyzeByJSoup.getValue((Element) currentSource, "@" + partRule);
            } else if (isAttr && currentSource instanceof String) {
                // 如果 source 是字符串但规则是属性，可能是正则替换等
                result = (String) currentSource;
            } else {
                // CSS 选择器规则
                AnalyzeByJSoup jSoup = new AnalyzeByJSoup();
                try {
                    if (currentSource instanceof String) {
                        jSoup.parse((String) currentSource);
                    } else if (currentSource instanceof Element) {
                        jSoup.parse((Element) currentSource);
                    } else {
                        return "";
                    }

                    // 判断是否为最后一步（最后一步通常需要提取值）
                    boolean isLast = (i == chainParts.length - 1) && (regexPart == null);
                    // 如果下一步是属性提取，或者当前是最后一步，提取第一个元素
                    boolean nextIsAttr = (i + 1 < chainParts.length && isCssAttr(chainParts[i + 1].trim()));

                    if (isLast || nextIsAttr) {
                        // 需要返回 Element 以便下一步提取属性
                        org.jsoup.select.Elements elements = jSoup.getElements(partRule);
                        if (elements.isEmpty()) {
                            return "";
                        }
                        currentSource = elements.first();
                        result = elements.first().toString();
                    } else {
                        // 中间步骤，返回 Element
                        org.jsoup.select.Elements elements = jSoup.getElements(partRule);
                        if (elements.isEmpty()) {
                            return "";
                        }
                        currentSource = elements.first();
                        result = elements.first().toString();
                    }
                } finally {
                    jSoup.release();
                }
            }
        }

        // 应用正则替换
        if (regexPart != null && !regexPart.isEmpty()) {
            String[] regexParts = splitRegex(regexPart);
            if (regexParts.length >= 2) {
                result = applyRegex(result, regexParts[1],
                    regexParts.length > 2 ? regexParts[2] : "");
            }
        }

        return result;
    }

    /**
     * 识别规则类型
     */
    private RuleType detectRuleType(String rule) {
        if (StringUtils.isEmpty(rule)) {
            return RuleType.UNKNOWN;
        }
        if (rule.startsWith(RuleAnalyzer.JS_PREFIX) || rule.startsWith(RuleAnalyzer.JS_PREFIX2)) {
            return RuleType.JS;
        }
        if (rule.startsWith(RuleAnalyzer.REGEX_PREFIX)) {
            return RuleType.REGEX;
        }
        if (rule.startsWith(RuleAnalyzer.JSON_PATH_PREFIX)) {
            return RuleType.JSON_PATH;
        }
        if (rule.startsWith(RuleAnalyzer.XPATH_PREFIX) || rule.startsWith(RuleAnalyzer.XPATH_PREFIX2)) {
            return RuleType.XPATH;
        }
        // 判断是否为 JSON
        if (source instanceof String && StringUtils.isJson((String) source)) {
            // 如果 source 是 JSON，且规则不以标签名开头，可能是 JSONPath
            if (!rule.startsWith("div") && !rule.startsWith("span") && !rule.startsWith("a")
                && !rule.startsWith("p") && !rule.startsWith("ul") && !rule.startsWith("li")
                && !rule.startsWith("img") && !rule.startsWith("table")) {
                // 默认按 CSS 处理
            }
        }
        return RuleType.CSS;
    }

    /**
     * 释放资源
     */
    public void release() {
        source = null;
        variables.clear();
    }

    /**
     * 规则类型枚举
     */
    private enum RuleType {
        CSS,
        XPATH,
        JSON_PATH,
        REGEX,
        JS,
        UNKNOWN
    }
}
