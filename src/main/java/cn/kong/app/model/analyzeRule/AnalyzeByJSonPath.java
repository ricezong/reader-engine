package cn.kong.app.model.analyzeRule;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import cn.kong.app.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * JSONPath 解析器
 * 改造自 reader-dev 的 AnalyzeByJSonPath.kt
 * <p>
 * 基于 JsonPath 库实现，支持 $. 开头的 JSONPath 语法。
 */
public class AnalyzeByJSonPath {

    private DocumentContext documentContext;

    public AnalyzeByJSonPath() {
    }

    /**
     * 从 JSON 字符串解析
     */
    public AnalyzeByJSonPath parse(String json) {
        if (StringUtils.isEmpty(json)) {
            this.documentContext = null;
            return this;
        }
        try {
            this.documentContext = JsonPath.parse(json);
        } catch (Exception e) {
            this.documentContext = null;
        }
        return this;
    }

    /**
     * 从对象解析
     */
    public AnalyzeByJSonPath parse(Object obj) {
        if (obj == null) {
            this.documentContext = null;
            return this;
        }
        try {
            this.documentContext = JsonPath.parse(obj);
        } catch (Exception e) {
            this.documentContext = null;
        }
        return this;
    }

    /**
     * 获取 DocumentContext
     */
    public DocumentContext getDocumentContext() {
        return documentContext;
    }

    /**
     * 获取元素列表（JSON 对象/数组）
     *
     * @param rule JSONPath 规则
     */
    public List<Object> getElements(String rule) {
        List<Object> result = new ArrayList<>();
        if (documentContext == null || StringUtils.isEmpty(rule)) {
            return result;
        }
        try {
            Object value = documentContext.read(rule);
            if (value instanceof List) {
                result.addAll((List<?>) value);
            } else if (value != null) {
                result.add(value);
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return result;
    }

    /**
     * 获取单值
     */
    public String getString(String rule) {
        if (documentContext == null || StringUtils.isEmpty(rule)) {
            return "";
        }
        try {
            Object value = documentContext.read(rule);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取多值列表
     */
    public List<String> getStringList(String rule) {
        List<String> result = new ArrayList<>();
        if (documentContext == null || StringUtils.isEmpty(rule)) {
            return result;
        }
        try {
            Object value = documentContext.read(rule);
            if (value instanceof List) {
                List<?> array = (List<?>) value;
                for (Object item : array) {
                    if (item != null) {
                        result.add(item.toString());
                    }
                }
            } else if (value != null) {
                result.add(value.toString());
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return result;
    }

    /**
     * 从对象中提取值
     */
    public static String getValue(Object obj, String rule) {
        if (obj == null || StringUtils.isEmpty(rule)) {
            return "";
        }
        try {
            DocumentContext ctx = JsonPath.parse(obj);
            Object value = ctx.read(rule);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        documentContext = null;
    }
}
