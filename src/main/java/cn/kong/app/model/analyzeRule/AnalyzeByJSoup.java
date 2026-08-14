package cn.kong.app.model.analyzeRule;

import cn.kong.app.utils.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSoup 解析器（CSS 选择器）
 * 改造自 reader-dev 的 AnalyzeByJSoup.kt
 * <p>
 * 支持 Legado 扩展的 CSS 选择器语法：
 * <ul>
 *   <li>id.xxx - 选择 id="xxx" 的元素（等价于 #xxx）</li>
 *   <li>tag.index - 选择第 index+1 个 tag 标签（如 a.1 表示第 2 个 a 标签）</li>
 *   <li>class.xxx - 选择 class="xxx" 的元素（等价于 .xxx）</li>
 *   <li>tag@attr - 提取属性值（如 a@href）</li>
 *   <li>@text / @html / @ownText / @textNodes - 提取文本</li>
 * </ul>
 */
public class AnalyzeByJSoup {

    /** 匹配 tag.index 语法（如 a.1, div.0） */
    private static final Pattern INDEX_PATTERN = Pattern.compile(
        "^([a-zA-Z][\\w-]*)\\.(\\d+)$");

    /** 匹配 id.xxx 语法 */
    private static final Pattern ID_PATTERN = Pattern.compile(
        "^id\\.(.+)$");

    /** 匹配 class.xxx 语法 */
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^class\\.(.+)$");

    private Document document;

    public AnalyzeByJSoup() {
    }

    public AnalyzeByJSoup parse(String html) {
        if (StringUtils.isEmpty(html)) {
            this.document = null;
            return this;
        }
        this.document = Jsoup.parse(html);
        return this;
    }

    public AnalyzeByJSoup parse(Element element) {
        if (element == null) {
            this.document = null;
            return this;
        }
        if (element instanceof Document) {
            this.document = (Document) element;
        } else {
            this.document = new Document("");
            this.document.appendChild(element.clone());
        }
        return this;
    }

    public Document getDocument() {
        return document;
    }

    /**
     * 获取元素列表
     * 支持规则链：id.yulan@li>a 表示 id=yulan 元素下的 li 下的 a 标签
     *
     * @param rule 规则（可能包含 @ 规则链）
     */
    public Elements getElements(String rule) {
        if (document == null || StringUtils.isEmpty(rule)) {
            return new Elements();
        }
        return selectChain(document, rule);
    }

    /**
     * 从指定元素中获取元素列表
     */
    public static Elements getElements(Element element, String rule) {
        if (element == null || StringUtils.isEmpty(rule)) {
            return new Elements();
        }
        return selectChain(element, rule);
    }

    /**
     * 规则链选择（支持 @ 分隔的多级选择器）
     * 例如：id.yulan@li>a 表示 id=yulan → li → a
     */
    private static Elements selectChain(Element root, String rule) {
        // 按 @ 分割规则链
        String[] chainParts = rule.split("@");
        Elements current = new Elements();
        current.add(root);

        for (String part : chainParts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 判断是否为属性（最后一步可能是属性，但 getElements 不提取属性，跳过）
            if (isKnownAttr(trimmed)) {
                continue; // 属性提取不适用于 getElements
            }
            Elements next = new Elements();
            for (Element el : current) {
                next.addAll(selectElements(el, trimmed));
            }
            current = next;
            if (current.isEmpty()) {
                break;
            }
        }
        return current;
    }

    /**
     * 判断是否为已知属性名
     */
    private static boolean isKnownAttr(String str) {
        return str.equals("text") || str.equals("html") || str.equals("outerHtml")
            || str.equals("ownText") || str.equals("textNodes")
            || str.equals("href") || str.equals("src") || str.equals("title")
            || str.equals("alt") || str.equals("value") || str.equals("name")
            || str.equals("class") || str.equals("id") || str.equals("content");
    }

    /**
     * 核心选择方法，支持 Legado 扩展语法
     * 注意：Legado 中 > 等同于后代选择器（空格），不是 CSS 的直接子选择器
     */
    private static Elements selectElements(Element root, String selector) {
        if (StringUtils.isEmpty(selector)) {
            return new Elements();
        }

        // 将 > 替换为空格（Legado 中 > 是后代选择器）
        String normalizedSelector = selector.replace(">", " ").replaceAll("\\s+", " ").trim();

        // 按空格分割为多级选择器（后代选择器）
        String[] parts = normalizedSelector.split("\\s+");
        Elements current = new Elements();
        current.add(root);

        for (String part : parts) {
            if (StringUtils.isEmpty(part)) {
                continue;
            }
            Elements next = new Elements();
            for (Element el : current) {
                next.addAll(selectSingle(el, part));
            }
            current = next;
        }
        return current;
    }

    /**
     * 选择直接子元素（用于 > 选择器）
     */
    private static Elements selectDirectChildren(Element parent, String selector) {
        Elements result = new Elements();
        // 先用 selectSingle 在父元素的所有后代中查找
        Elements matched = selectSingle(parent, selector);
        // 只保留直接子元素
        for (Element el : matched) {
            Element p = el.parent();
            if (p != null && p.equals(parent)) {
                result.add(el);
            }
        }
        return result;
    }

    /**
     * 单级选择器解析
     */
    private static Elements selectSingle(Element root, String selector) {
        Elements result = new Elements();

        // 1. id.xxx 语法
        Matcher idMatcher = ID_PATTERN.matcher(selector);
        if (idMatcher.matches()) {
            String id = idMatcher.group(1);
            Element el = root.getElementById(id);
            if (el != null) {
                result.add(el);
            }
            return result;
        }

        // 2. class.xxx 语法
        Matcher classMatcher = CLASS_PATTERN.matcher(selector);
        if (classMatcher.matches()) {
            String cls = classMatcher.group(1);
            return root.getElementsByClass(cls);
        }

        // 3. tag.index 语法（如 a.1, div.0）- 正索引
        Matcher indexMatcher = INDEX_PATTERN.matcher(selector);
        if (indexMatcher.matches()) {
            String tag = indexMatcher.group(1);
            int idx = Integer.parseInt(indexMatcher.group(2));
            Elements els = root.getElementsByTag(tag);
            if (idx >= 0 && idx < els.size()) {
                result.add(els.get(idx));
            }
            return result;
        }

        // 4. tag.negative-index 语法（如 a.-1 表示倒数第 1 个）
        Pattern negPattern = Pattern.compile("^([a-zA-Z][\\w-]*)\\.(-\\d+)$");
        Matcher negMatcher = negPattern.matcher(selector);
        if (negMatcher.matches()) {
            String tag = negMatcher.group(1);
            int idx = Integer.parseInt(negMatcher.group(2));
            Elements els = root.getElementsByTag(tag);
            int actual = els.size() + idx; // -1 -> size-1
            if (actual >= 0 && actual < els.size()) {
                result.add(els.get(actual));
            }
            return result;
        }

        // 5. 标准 CSS 选择器
        try {
            return root.select(selector);
        } catch (Exception e) {
            return result;
        }
    }

    /**
     * 从元素中提取值
     * 规则可以是：
     *   - 纯属性名：text, href, src 等
     *   - 带 @ 前缀：@text, @href 等
     *   - 选择器@属性：a.0@text
     */
    public static String getValue(Element element, String rule) {
        if (element == null || StringUtils.isEmpty(rule)) {
            return "";
        }
        // 如果规则本身就是已知属性名（无 @），直接提取
        if (isKnownAttr(rule)) {
            return extractAttr(element, rule);
        }
        String attr = RuleAnalyzer.getCssAttr(rule);
        if (StringUtils.isEmpty(attr)) {
            return element.text();
        }
        return extractAttr(element, attr);
    }

    /**
     * 提取指定属性
     */
    private static String extractAttr(Element element, String attr) {
        switch (attr) {
            case "text":
                return element.text();
            case "ownText":
                return element.ownText();
            case "html":
            case "innerHTML":
                return element.html();
            case "outerHtml":
                return element.outerHtml();
            case "src":
                String src = element.attr("abs:src");
                return StringUtils.isNotEmpty(src) ? src : element.absUrl("src");
            case "href":
                String href = element.attr("abs:href");
                if (StringUtils.isNotEmpty(href)) {
                    return href;
                }
                href = element.attr("href");
                if (StringUtils.isNotEmpty(href)) {
                    if (href.startsWith("http")) {
                        return href;
                    }
                    String abs = element.absUrl("href");
                    return StringUtils.isNotEmpty(abs) ? abs : href;
                }
                return element.absUrl("href");
            case "data-src":
                return element.absUrl("data-src");
            case "textNodes":
                StringBuilder sb = new StringBuilder();
                for (TextNode node : element.textNodes()) {
                    String text = node.text().trim();
                    if (text.length() > 0) {
                        sb.append(text).append("\n");
                    }
                }
                return sb.toString().trim();
            default:
                String val = element.attr(attr);
                if (StringUtils.isEmpty(val)) {
                    val = element.absUrl(attr);
                }
                return val;
        }
    }

    /**
     * 从元素列表中提取值列表
     */
    public static List<String> getValues(Elements elements, String rule) {
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

    public String getString(String rule) {
        if (document == null || StringUtils.isEmpty(rule)) {
            return "";
        }
        // 如果规则是纯属性名（如 href, text），直接从 document 提取
        if (isKnownAttr(rule)) {
            return getValue(document, "@" + rule);
        }
        Elements elements = getElements(rule);
        if (elements.isEmpty()) {
            return "";
        }
        return getValue(elements.first(), rule);
    }

    public List<String> getStringList(String rule) {
        if (document == null || StringUtils.isEmpty(rule)) {
            return new ArrayList<>();
        }
        Elements elements = getElements(rule);
        return getValues(elements, rule);
    }

    public void release() {
        document = null;
    }
}
