package cn.kong.app.help.js;

import cn.hutool.crypto.digest.DigestUtil;
import cn.kong.app.data.entities.BookSource;
import cn.kong.app.help.http.HttpHelper;
import cn.kong.app.utils.StringUtils;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JS 扩展函数集
 * 改造自 reader-dev 的 JsExtensions.kt
 * <p>
 * 提供 Legado 书源 JS 脚本中使用的所有 Java 扩展方法，
 * 通过 Rhino 引擎注入到 JS 执行环境中。
 * <p>
 * 主要方法：
 * - java.ajax(url): 发起 HTTP 请求
 * - java.put(key, value) / java.get(key): 变量存取
 * - cache.get(key) / cache.put(key, value): 缓存存取
 * - java.md5Encode(str) / java.md5Encode16(str): MD5 哈希
 * - java.timeFormat(timestamp): 时间格式化
 * - java.encodeURI(str): URL 编码
 * - java.s2t(str) / java.t2s(str): 繁简转换
 * - java.aesBase64DecodeToString(...): AES 解密
 * - java.getElements(selector): DOM 选择器
 */
public class JsExtensions {

    private static final Logger log = LoggerFactory.getLogger(JsExtensions.class);

    /** 变量存储（java.put/java.get） */
    private final Map<String, Object> variables = new HashMap<>();

    /** 缓存存储（cache.get/cache.put） */
    private final Map<String, String> cacheMap = new HashMap<>();

    /** 书源对象（用于 source.xxx 访问） */
    private BookSource bookSource;

    /** Put 输出（JS 脚本通过 Put() 返回结果） */
    private Object putResult;

    public JsExtensions() {
    }

    public JsExtensions(BookSource bookSource) {
        this.bookSource = bookSource;
    }

    public void setBookSource(BookSource bookSource) {
        this.bookSource = bookSource;
    }

    public Object getPutResult() {
        return putResult;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void reset() {
        variables.clear();
        cacheMap.clear();
        putResult = null;
    }

    // ============ java.xxx 方法 ============

    /**
     * 发起 HTTP 请求，返回响应体字符串
     * 支持 url,{"method":"GET","headers":{...}} 格式
     */
    public String ajax(String urlStr) {
        if (StringUtils.isEmpty(urlStr)) {
            return "";
        }
        try {
            // 解析 URL 和配置
            String url = urlStr.trim();
            Map<String, String> headers = null;
            String method = "GET";
            String body = null;

            int jsonIdx = url.indexOf(",{");
            if (jsonIdx > 0) {
                String jsonConfig = url.substring(jsonIdx + 1);
                url = url.substring(0, jsonIdx).trim();
                try {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(jsonConfig).getAsJsonObject();
                    if (json.has("method")) {
                        method = json.get("method").getAsString().toUpperCase();
                    }
                    if (json.has("body")) {
                        body = json.get("body").getAsString();
                    }
                    if (json.has("headers")) {
                        headers = new HashMap<>();
                        com.google.gson.JsonObject headerJson = json.getAsJsonObject("headers");
                        for (Map.Entry<String, com.google.gson.JsonElement> entry : headerJson.entrySet()) {
                            headers.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                } catch (Exception e) {
                    log.warn("ajax JSON 配置解析失败: {}", e.getMessage());
                }
            }

            Response response = null;
            try {
                if ("POST".equals(method)) {
                    if (StringUtils.isNotEmpty(body)) {
                        response = HttpHelper.postBodyNoSsl(url, body,
                            okhttp3.MediaType.parse("application/x-www-form-urlencoded"), headers);
                    } else {
                        response = HttpHelper.postBodyNoSsl(url, "", null, headers);
                    }
                } else {
                    response = HttpHelper.getNoSsl(url, headers);
                }
                if (!response.isSuccessful()) {
                    log.warn("ajax 请求失败: url={}, code={}", url, response.code());
                    return "";
                }
                String responseBody = HttpHelper.getResponseBody(response);
                log.info("ajax 请求成功: url={}, 响应长度={}", url, responseBody == null ? 0 : responseBody.length());
                return responseBody;
            } finally {
                HttpHelper.closeResponse(response);
            }
        } catch (Exception e) {
            log.error("ajax 请求异常: url={}, error={}", urlStr, e.getMessage());
            return "";
        }
    }

    /**
     * 发起 HTTP 请求，返回响应体字符串（同 ajax）
     */
    public String httpGet(String urlStr) {
        return ajax(urlStr);
    }

    /**
     * 发起 GET 请求，返回响应对象（有 body() 方法）
     * 用于 JS 中 java.get(url, {}).body() 调用
     */
    public HttpResponse get(String urlStr, Map<String, Object> options) {
        String body = ajax(urlStr);
        return new HttpResponse(body);
    }

    /**
     * HTTP 响应包装类，提供 body() 方法
     */
    public static class HttpResponse {
        private final String body;
        public HttpResponse(String body) {
            this.body = body == null ? "" : body;
        }
        public String body() {
            return body;
        }
        @Override
        public String toString() {
            return body;
        }
    }

    /**
     * 发起 POST 请求
     */
    public String post(String urlStr, String body) {
        return ajax(urlStr + ",{\"method\":\"POST\",\"body\":\"" + body + "\"}");
    }

    /**
     * 存储变量
     */
    public void put(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取变量
     */
    public Object get(String key) {
        return variables.get(key);
    }

    /**
     * MD5 加密（32位）
     */
    public String md5Encode(String str) {
        if (str == null) return "";
        return DigestUtil.md5Hex(str);
    }

    /**
     * MD5 加密（16位）
     */
    public String md5Encode16(String str) {
        if (str == null) return "";
        return DigestUtil.md5Hex(str).substring(8, 24);
    }

    /**
     * 时间格式化
     */
    public String timeFormat(Long timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        return sdf.format(new Date(timestamp));
    }

    /**
     * 时间格式化（自定义格式）
     */
    public String timeFormat(Long timestamp, String pattern) {
        if (timestamp == null) return "";
        if (StringUtils.isEmpty(pattern)) pattern = "yyyy/MM/dd HH:mm";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * URL 编码
     */
    public String encodeURI(String str) {
        if (str == null) return "";
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            return str;
        }
    }

    /**
     * URL 编码（指定字符集）
     */
    public String encodeURI(String str, String charset) {
        if (str == null) return "";
        try {
            return URLEncoder.encode(str, charset);
        } catch (Exception e) {
            return str;
        }
    }

    /**
     * Base64 编码
     */
    public String base64Encode(String str) {
        if (str == null) return "";
        return java.util.Base64.getEncoder().encodeToString(str.getBytes(Charset.forName("UTF-8")));
    }

    /**
     * Base64 解码
     */
    public String base64Decode(String str) {
        if (str == null) return "";
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(str);
            return new String(decoded, Charset.forName("UTF-8"));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * AES 解密（Base64 密文 → 明文）
     * 用于解密 variableComment 中的加密 JS 代码
     *
     * Legado 调用顺序：java.aesBase64DecodeToString(cipherText, key, padding, iv)
     *
     * @param cipherText Base64 编码的密文
     * @param key        密钥（原始字符串）
     * @param padding    填充模式（如 AES/ECB/PKCS7Padding）
     * @param iv         初始化向量（可为空）
     */
    public String aesBase64DecodeToString(String cipherText, String key, String padding, String iv) {
        if (StringUtils.isEmpty(cipherText)) return "";
        try {
            // 密钥：直接使用原始字节（Legado 传入的密钥已是合法长度 16/24/32）
            byte[] keyBytes = key.getBytes(Charset.forName("UTF-8"));
            // 如果长度不是 16/24/32，补零到 16 字节
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                byte[] padded = new byte[16];
                System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 16));
                keyBytes = padded;
            }

            // 密文：Base64 解码（NO_WRAP 模式）
            byte[] cipherBytes = java.util.Base64.getDecoder().decode(cipherText);

            // 填充模式：使用参数指定的
            String cipherAlgo = StringUtils.isEmpty(padding) ? "AES/ECB/PKCS7Padding" : padding;

            // 注册 BouncyCastle 提供者
            if (java.security.Security.getProvider("BC") == null) {
                java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            }

            // 使用 BouncyCastle 解密（支持 PKCS7Padding）
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(cipherAlgo, "BC");

            if (cipherAlgo.contains("ECB")) {
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey);
            } else {
                byte[] ivBytes = StringUtils.isEmpty(iv) ? null : iv.getBytes(Charset.forName("UTF-8"));
                if (ivBytes != null && ivBytes.length > 0) {
                    javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(ivBytes);
                    cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, ivSpec);
                } else {
                    cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey);
                }
            }

            byte[] decrypted = cipher.doFinal(cipherBytes);
            String result = new String(decrypted, Charset.forName("UTF-8"));
            // 兼容 ES2019 语法：catch{ → catch(e){（Rhino 不支持省略 catch 参数）
            result = result.replaceAll("catch\\s*\\{", "catch(e) {");
            return result;
        } catch (Exception e) {
            log.error("AES 解密失败: algo={}, error={}", padding, e.getMessage());
            return "";
        }
    }

    /**
     * AES 加密（明文 → Base64 密文）
     */
    public String aesEncode(String key, String plainText, String iv, String padding) {
        if (StringUtils.isEmpty(plainText)) return "";
        try {
            byte[] keyBytes = java.util.Base64.getDecoder().decode(key);
            byte[] ivBytes = StringUtils.isEmpty(iv) ? null : iv.getBytes(Charset.forName("UTF-8"));
            byte[] plainBytes = plainText.getBytes(Charset.forName("UTF-8"));

            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            if (ivBytes != null && ivBytes.length > 0) {
                javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(ivBytes);
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            } else {
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);
            }
            byte[] encrypted = cipher.doFinal(plainBytes);
            return java.util.Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES 加密失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * AES 加密为 Base64 字符串
     * Legado 调用顺序：java.aesEncodeToBase64String(plainText, key, padding, iv)
     */
    public String aesEncodeToBase64String(String plainText, String key, String padding, String iv) {
        if (StringUtils.isEmpty(plainText)) return "";
        try {
            byte[] keyBytes = key.getBytes(Charset.forName("UTF-8"));
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                byte[] padded = new byte[16];
                System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 16));
                keyBytes = padded;
            }
            byte[] plainBytes = plainText.getBytes(Charset.forName("UTF-8"));
            String cipherAlgo = StringUtils.isEmpty(padding) ? "AES/ECB/PKCS7Padding" : padding;

            if (java.security.Security.getProvider("BC") == null) {
                java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            }

            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(cipherAlgo, "BC");

            if (cipherAlgo.contains("ECB")) {
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);
            } else {
                byte[] ivBytes = StringUtils.isEmpty(iv) ? null : iv.getBytes(Charset.forName("UTF-8"));
                if (ivBytes != null && ivBytes.length > 0) {
                    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, new javax.crypto.spec.IvParameterSpec(ivBytes));
                } else {
                    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);
                }
            }
            byte[] encrypted = cipher.doFinal(plainBytes);
            return java.util.Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES 加密失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 繁体转简体（简化实现，实际需要繁简转换库）
     */
    public String t2s(String str) {
        // 简化实现：直接返回原文
        // 完整实现需要引入 quick-chinese-transfer 库
        return str == null ? "" : str;
    }

    /**
     * 简体转繁体
     */
    public String s2t(String str) {
        return str == null ? "" : str;
    }

    /**
     * 获取 DOM 元素列表（CSS 选择器）
     * 用于 JS 中 java.getElements(selector) 调用
     * @param selector CSS 选择器
     * @return Elements 对象（可调用 .select() .attr() .text() 等方法）
     */
    public org.jsoup.select.Elements getElements(String selector) {
        if (StringUtils.isEmpty(selector)) {
            return new org.jsoup.select.Elements();
        }
        try {
            String html = resultStr == null ? "" : resultStr;
            if (StringUtils.isEmpty(html)) {
                return new org.jsoup.select.Elements();
            }
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
            return doc.select(selector);
        } catch (Exception e) {
            log.warn("getElements 失败: selector={}, error={}", selector, e.getMessage());
            return new org.jsoup.select.Elements();
        }
    }

    /**
     * 获取单个 DOM 元素（CSS 选择器）
     * 用于 JS 中 java.getElement(selector) 调用
     * @param selector CSS 选择器
     * @return Element 对象（可调用 .select() .attr() .text() 等方法），找不到返回 null
     */
    public org.jsoup.nodes.Element getElement(String selector) {
        if (StringUtils.isEmpty(selector)) {
            return null;
        }
        try {
            String html = resultStr == null ? "" : resultStr;
            if (StringUtils.isEmpty(html)) {
                return null;
            }
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
            org.jsoup.select.Elements elements = doc.select(selector);
            return elements.isEmpty() ? null : elements.first();
        } catch (Exception e) {
            log.warn("getElement 失败: selector={}, error={}", selector, e.getMessage());
            return null;
        }
    }

    /** 当前结果字符串（HTTP 响应体） */
    private String resultStr;

    /**
     * 设置当前结果字符串
     */
    public void setResultStr(String resultStr) {
        this.resultStr = resultStr;
    }

    /**
     * 日志输出
     */
    public void log(String msg) {
        log.info("[JS] {}", msg);
    }

    /**
     * 长时间 Toast 提示（简化实现，仅日志）
     */
    public void longToast(String msg) {
        log.info("[JS Toast] {}", msg);
    }

    /**
     * 短时间 Toast 提示（简化实现，仅日志）
     */
    public void toast(String msg) {
        log.info("[JS Toast] {}", msg);
    }

    // ============ cache.xxx 方法 ============

    /**
     * 缓存类，提供 cache.get/cache.put 方法
     */
    public CacheProxy cache = new CacheProxy();

    public class CacheProxy {
        public String get(String key) {
            String val = cacheMap.get(key);
            return val == null ? "null" : val;
        }

        /**
         * 获取缓存值，不存在返回默认值
         */
        public String get(String key, String defaultVal) {
            String val = cacheMap.get(key);
            return val == null ? defaultVal : val;
        }

        public void put(String key, String value) {
            cacheMap.put(key, value);
        }

        public void put(String key, String value, long ttl) {
            cacheMap.put(key, value);
        }

        public void delete(String key) {
            cacheMap.remove(key);
        }
    }

    /** 缓存存储 */
    private final Map<String, String> cache_map = new HashMap<>();

    // ============ 全局函数 ============

    /**
     * Get(key) - 获取全局变量
     * 优先从 variables 读取，如果没有则从 source.getVariable() 解析 JSON 读取
     * 对应 JS 中的 Get('url') 等
     */
    public Object Get(String key) {
        Object val = variables.get(key);
        if (val != null) {
            return val;
        }
        // 从 source.getVariable() 解析 JSON 读取
        if (bookSource != null) {
            String varJson = bookSource.getVariable();
            if (varJson != null && !varJson.isEmpty()) {
                try {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(varJson).getAsJsonObject();
                    if (json.has(key)) {
                        return json.get(key).getAsString();
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }
        return null;
    }

    /**
     * Get(key, defaultKey) - 获取变量，不存在则返回默认值
     */
    public Object Get(String key, Object defaultVal) {
        Object val = Get(key);
        return val != null ? val : defaultVal;
    }

    /**
     * get(key, defaultKey) - 获取变量，不存在则从另一个变量取默认值
     */
    public Object get(String key, String defaultKey) {
        Object val = variables.get(key);
        if (val != null) return val;
        return variables.get(defaultKey);
    }

    /**
     * Put(value) - 输出结果（JS 脚本通过 Put() 返回解析结果）
     */
    public void Put(Object value) {
        this.putResult = value;
    }

    /**
     * put(map) - 存储变量映射
     * 支持 Java Map 和 JS 对象（NativeObject）
     */
    public void put(Object obj) {
        if (obj == null) {
            return;
        }
        log.debug("put 接收对象类型: {}, 值: {}", obj.getClass().getName(), obj);
        if (obj instanceof Map) {
            variables.putAll((Map<String, Object>) obj);
        } else if (obj instanceof org.mozilla.javascript.NativeObject) {
            org.mozilla.javascript.NativeObject nativeObj = (org.mozilla.javascript.NativeObject) obj;
            for (Object key : nativeObj.keySet()) {
                Object value = nativeObj.get(key);
                variables.put(key.toString(), jsToJava(value));
            }
        } else if (obj instanceof org.mozilla.javascript.Scriptable) {
            org.mozilla.javascript.Scriptable scriptable = (org.mozilla.javascript.Scriptable) obj;
            Object[] ids = scriptable.getIds();
            for (Object id : ids) {
                String key = id.toString();
                Object value = scriptable.get(key, scriptable);
                if (value != org.mozilla.javascript.Scriptable.NOT_FOUND) {
                    variables.put(key, jsToJava(value));
                }
            }
        } else {
            log.warn("put 不支持的对象类型: {}", obj.getClass().getName());
        }
    }

    /**
     * 将 JS 值转为 Java 类型
     * NativeString → String, NativeNumber → Double, NativeBoolean → Boolean
     */
    private Object jsToJava(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof org.mozilla.javascript.Scriptable) {
            // 尝试转为字符串
            try {
                return org.mozilla.javascript.Context.toString(value);
            } catch (Exception e) {
                return value;
            }
        }
        return value;
    }

    /**
     * login(msg) - 登录提示（简化实现，仅日志）
     */
    public void login(String msg) {
        log.info("[登录提示] {}", msg);
    }

    /**
     * explore(title, url, page, group, isVip) - 构造发现页条目
     */
    public Object explore(String title, String url, int page, double group, boolean isVip) {
        Map<String, Object> item = new HashMap<>();
        item.put("title", title);
        item.put("url", url);
        item.put("page", page);
        item.put("group", group);
        item.put("isVip", isVip);
        return item;
    }

    /**
     * Num(str) - 数字格式化（万、亿）
     */
    public String Num(Object num) {
        if (num == null) return "0";
        try {
            long n = Long.parseLong(num.toString().replaceAll("[^0-9]", ""));
            if (n >= 100000000) {
                return String.format("%.1f亿", n / 100000000.0);
            } else if (n >= 10000) {
                return String.format("%.1f万", n / 10000.0);
            }
            return String.valueOf(n);
        } catch (Exception e) {
            return num.toString();
        }
    }

    /**
     * n(count) - 生成空格字符串
     */
    public String n(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("ㅤ");
        }
        return sb.toString();
    }

    /**
     * k(num) - 生成装饰线
     */
    public String k(int num) {
        String[] lines = {"━", "┅", "┏", "┓", "┗", "┛", "┋", "♦️", "♠", "♣️", "❤️", "📌", "📚", "🔍", "📑", "🗃", "〖", "〗"};
        if (num >= 0 && num < lines.length) {
            return lines[num];
        }
        return "";
    }

    /**
     * name(num) - 获取测试名称
     */
    public String name(int num) {
        String[] names = {"源站测试", "源站更新"};
        if (num >= 0 && num < names.length) {
            return names[num];
        }
        return "";
    }

    /**
     * x - 圆圈数字字符串
     */
    public String x = "⓪①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳";

    /**
     * y - 排序名称数组
     */
    public String[] y = {"默认", "自选"};

    /**
     * 获取书源对象（用于 source.xxx 访问）
     */
    public BookSource getSource() {
        return bookSource;
    }
}
