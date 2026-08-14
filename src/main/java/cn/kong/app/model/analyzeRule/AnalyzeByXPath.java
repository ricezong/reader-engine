package cn.kong.app.model.analyzeRule;

import cn.kong.app.utils.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.util.ArrayList;
import java.util.List;

/**
 * XPath 解析器
 * 改造自 reader-dev 的 AnalyzeByXPath.kt
 * <p>
 * 基于 JsoupXpath 实现，支持标准 XPath 1.0 语法。
 */
public class AnalyzeByXPath {

    private JXDocument jxDocument;

    public AnalyzeByXPath() {
    }

    /**
     * 从 HTML 字符串解析
     */
    public AnalyzeByXPath parse(String html) {
        if (StringUtils.isEmpty(html)) {
            this.jxDocument = null;
            return this;
        }
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        this.jxDocument = JXDocument.create(doc);
        return this;
    }

    /**
     * 从 Element 解析
     */
    public AnalyzeByXPath parse(Element element) {
        if (element == null) {
            this.jxDocument = null;
            return this;
        }
        org.jsoup.nodes.Document doc;
        if (element instanceof org.jsoup.nodes.Document) {
            doc = (org.jsoup.nodes.Document) element;
        } else {
            doc = new org.jsoup.nodes.Document("");
            doc.appendChild(element.clone());
        }
        this.jxDocument = JXDocument.create(doc);
        return this;
    }

    /**
     * 获取元素列表
     *
     * @param rule XPath 规则
     */
    public List<Element> getElements(String rule) {
        List<Element> result = new ArrayList<>();
        if (jxDocument == null || StringUtils.isEmpty(rule)) {
            return result;
        }
        try {
            List<JXNode> nodes = jxDocument.selN(rule);
            for (JXNode node : nodes) {
                if (node.isElement()) {
                    result.add(node.asElement());
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return result;
    }

    /**
     * 从元素中提取值
     */
    public static String getValue(Element element, String rule) {
        if (element == null || StringUtils.isEmpty(rule)) {
            return "";
        }
        try {
            org.jsoup.nodes.Document doc;
            if (element instanceof org.jsoup.nodes.Document) {
                doc = (org.jsoup.nodes.Document) element;
            } else {
                doc = new org.jsoup.nodes.Document("");
                doc.appendChild(element.clone());
            }
            JXDocument jxDoc = JXDocument.create(doc);
            List<JXNode> nodes = jxDoc.selN(rule);
            if (nodes.isEmpty()) {
                return "";
            }
            Object value = nodes.get(0).value();
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 从元素列表中提取值列表
     */
    public static List<String> getValues(List<Element> elements, String rule) {
        List<String> result = new ArrayList<>();
        if (elements == null || elements.isEmpty()) {
            return result;
        }
        for (Element element : elements) {
            String value = getValue(element, rule);
            if (StringUtils.isNotEmpty(value)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 获取单值
     */
    public String getString(String rule) {
        if (jxDocument == null || StringUtils.isEmpty(rule)) {
            return "";
        }
        try {
            Object value = jxDocument.selOne(rule);
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
        if (jxDocument == null || StringUtils.isEmpty(rule)) {
            return result;
        }
        try {
            List<Object> values = jxDocument.sel(rule);
            for (Object value : values) {
                if (value != null) {
                    result.add(value.toString());
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return result;
    }

    /**
     * 释放资源
     */
    public void release() {
        jxDocument = null;
    }
}
