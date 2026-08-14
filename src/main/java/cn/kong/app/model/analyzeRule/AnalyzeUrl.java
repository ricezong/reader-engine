package cn.kong.app.model.analyzeRule;

import cn.kong.app.constant.AppConst;
import cn.kong.app.help.js.JsEngine;
import cn.kong.app.utils.StringUtils;
import cn.kong.app.utils.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL 分析器
 * 改造自 reader-dev 的 AnalyzeUrl.kt
 * <p>
 * 负责解析书源 URL 中的复杂参数，包括：
 * <ul>
 *   <li>POST/GET 方法切换（POST: 前缀）</li>
 *   <li>请求头（header 字段）</li>
 *   <li>请求体（body 字段）</li>
 *   <li>JS 脚本（@js: 后缀）</li>
 *   <li>编码（charset 字段）</li>
 *   <li>占位符替换（{key}、{{key}}）</li>
 *   <li>分页（{{page}}）</li>
 * </ul>
 * <p>
 * URL 格式示例：
 * <pre>
 * https://www.example.com/search?q={key}&page={{page}},{"method":"POST","body":"word={key}","charset":"gbk"}
 * </pre>
 */
public class AnalyzeUrl {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeUrl.class);

    /** URL 与配置的分隔符 */
    public static final char URL_CONFIG_SEPARATOR = ',';

    /** JS 脚本前缀 */
    public static final String JS_PREFIX = "@js:";

    /** POST 方法前缀 */
    public static final String POST_PREFIX = "POST:";

    /** GET 方法前缀 */
    public static final String GET_PREFIX = "GET:";

    /** PUT 方法前缀 */
    public static final String PUT_PREFIX = "PUT:";

    /** DELETE 方法前缀 */
    public static final String DELETE_PREFIX = "DELETE:";

    private String url;
    private String method = "GET";
    private String charset = "utf-8";
    private String body;
    private Map<String, String> headers = new HashMap<>();
    private String jsScript;
    private String type;

    private final Map<String, Object> variables = new HashMap<>();

    public AnalyzeUrl(String urlRule) {
        this(urlRule, null, null);
    }

    public AnalyzeUrl(String urlRule, Map<String, Object> variables) {
        this(urlRule, variables, null);
    }

    public AnalyzeUrl(String urlRule, Map<String, Object> variables, String key) {
        if (variables != null) {
            this.variables.putAll(variables);
        }
        if (StringUtils.isNotEmpty(key)) {
            this.variables.put("key", key);
        }
        parse(urlRule);
    }

    /**
     * 解析 URL 规则
     */
    private void parse(String urlRule) {
        if (StringUtils.isEmpty(urlRule)) {
            return;
        }
        String rule = urlRule.trim();

        // 处理 JS 脚本
        int jsIdx = rule.indexOf(JS_PREFIX);
        if (jsIdx >= 0) {
            jsScript = rule.substring(jsIdx + JS_PREFIX.length());
            rule = rule.substring(0, jsIdx).trim();
        }

        // 处理 JSON 配置
        int jsonIdx = rule.indexOf('{');
        if (jsonIdx > 0) {
            String jsonConfig = rule.substring(jsonIdx);
            rule = rule.substring(0, jsonIdx).trim();
            // 移除末尾的逗号分隔符
            if (rule.endsWith(",")) {
                rule = rule.substring(0, rule.length() - 1).trim();
            }
            parseJsonConfig(jsonConfig);
        }

        // 处理方法前缀
        if (rule.startsWith(POST_PREFIX)) {
            method = "POST";
            rule = rule.substring(POST_PREFIX.length()).trim();
        } else if (rule.startsWith(GET_PREFIX)) {
            method = "GET";
            rule = rule.substring(GET_PREFIX.length()).trim();
        } else if (rule.startsWith(PUT_PREFIX)) {
            method = "PUT";
            rule = rule.substring(PUT_PREFIX.length()).trim();
        } else if (rule.startsWith(DELETE_PREFIX)) {
            method = "DELETE";
            rule = rule.substring(DELETE_PREFIX.length()).trim();
        }

        // 替换占位符
        url = replacePlaceholders(rule);

        // 如果是相对 URL，拼接 baseUrl
        Object baseUrlVar = variables.get("baseUrl");
        if (baseUrlVar != null && StringUtils.isNotEmpty(url) && !UrlUtil.isUrl(url)) {
            url = UrlUtil.absoluteUrl(url, baseUrlVar.toString());
        }
    }

    /**
     * 解析 JSON 配置
     */
    private void parseJsonConfig(String jsonConfig) {
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(jsonConfig).getAsJsonObject();
            if (json.has("method")) {
                method = json.get("method").getAsString().toUpperCase();
            }
            if (json.has("charset")) {
                charset = json.get("charset").getAsString();
            }
            if (json.has("body")) {
                body = json.get("body").getAsString();
                // 替换 body 中的占位符
                body = replacePlaceholders(body);
            }
            if (json.has("headers")) {
                com.google.gson.JsonObject headerJson = json.getAsJsonObject("headers");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : headerJson.entrySet()) {
                    headers.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            if (json.has("type")) {
                type = json.get("type").getAsString();
            }
        } catch (Exception e) {
            log.warn("JSON 配置解析失败: {}", e.getMessage());
        }
    }

    /**
     * 替换占位符
     * {key} - URL 编码
     * {{key}} - 不编码
     * {{page}} - 分页
     */
    private String replacePlaceholders(String url) {
        if (StringUtils.isEmpty(url)) {
            return url;
        }
        // 先替换 {{key}}（不编码）
        Pattern doubleBrace = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = doubleBrace.matcher(url);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.getOrDefault(key, "");
            matcher.appendReplacement(sb, value.toString());
        }
        matcher.appendTail(sb);
        url = sb.toString();

        // 再替换 {key}（URL 编码）
        Pattern singleBrace = Pattern.compile("\\{(\\w+)\\}");
        matcher = singleBrace.matcher(url);
        sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.getOrDefault(key, "");
            String encoded = URLEncoder.encode(value.toString(), StandardCharsets.UTF_8);
            matcher.appendReplacement(sb, encoded);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 获取最终 URL（执行 JS 后）
     */
    public String getUrl() {
        if (StringUtils.isNotEmpty(jsScript)) {
            try {
                Map<String, Object> vars = new HashMap<>(variables);
                vars.put("url", url);
                vars.put("baseUrl", variables.getOrDefault("baseUrl", ""));
                Object result = JsEngine.getInstance().eval(jsScript, vars);
                if (result != null) {
                    return result.toString();
                }
            } catch (Exception e) {
                log.warn("URL JS 执行失败: {}", e.getMessage());
            }
        }
        return url;
    }

    /**
     * 获取相对路径 URL（不含域名）
     */
    public String getPathUrl() {
        return UrlUtil.getPath(url);
    }

    /**
     * 获取请求方法
     */
    public String getMethod() {
        return method;
    }

    /**
     * 获取字符编码
     */
    public String getCharset() {
        return charset;
    }

    /**
     * 获取请求体
     */
    public String getBody() {
        return body;
    }

    /**
     * 获取请求头
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * 获取 User-Agent
     */
    public String getUserAgent() {
        return headers.getOrDefault("User-Agent", AppConst.USER_AGENT);
    }

    /**
     * 获取请求类型
     */
    public String getType() {
        return type;
    }

    /**
     * 是否为 POST 请求
     */
    public boolean isPost() {
        return "POST".equalsIgnoreCase(method);
    }

    /**
     * 是否为 GET 请求
     */
    public boolean isGet() {
        return "GET".equalsIgnoreCase(method);
    }

    /**
     * 获取变量
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * 添加变量
     */
    public void putVariable(String key, Object value) {
        variables.put(key, value);
    }
}
