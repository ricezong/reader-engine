package cn.kong.app.model.analyzeRule;

import java.util.List;

/**
 * 规则分析器接口
 * 改造自 reader-dev 的 IAnalyzeRule.kt
 * <p>
 * 统一 CSS 选择器、XPath、JSONPath、正则四种解析方式的接口。
 */
public interface IAnalyzeRule<T> {

    /**
     * 获取解析源对象
     */
    T getSource();

    /**
     * 设置解析源
     */
    void setSource(T source);

    /**
     * 解析单值
     *
     * @param rule 规则
     * @return 解析结果
     */
    String getString(String rule);

    /**
     * 解析单值（带默认值）
     */
    String getString(String rule, String def);

    /**
     * 解析多值列表
     *
     * @param rule 规则
     * @return 解析结果列表
     */
    List<String> getStringList(String rule);

    /**
     * 解析元素列表（用于列表规则）
     *
     * @param rule 规则
     * @return 元素列表
     */
    List<T> getElements(String rule);

    /**
     * 解析单值并去除前后空白
     */
    String getStringFirst(String rule);

    /**
     * 释放资源
     */
    void release();
}
