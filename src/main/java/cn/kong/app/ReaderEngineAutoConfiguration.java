package cn.kong.app;

import cn.kong.app.help.js.JsEngine;
import cn.kong.app.help.http.HttpClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Reader Engine 自动配置类
 * <p>
 * 当宿主项目引入 reader-engine 依赖后，Spring Boot 会自动加载此配置，
 * 初始化 JS 引擎、HTTP 客户端等核心组件。
 * <p>
 * 使用方式：
 * <pre>
 * // 1. 在 pom.xml 中引入依赖
 * &lt;dependency&gt;
 *     &lt;groupId&gt;io.legado&lt;/groupId&gt;
 *     &lt;artifactId&gt;reader-engine&lt;/artifactId&gt;
 *     &lt;version&gt;1.0.0&lt;/version&gt;
 * &lt;/dependency&gt;
 *
 * // 2. 在 Service 中注入 WebBook
 * &#64;Service
 * public class BookService {
 *     public List&lt;SearchBook&gt; search(BookSource source, String key) {
 *         WebBook webBook = new WebBook(source);
 *         return webBook.searchBook(key, 1);
 *     }
 * }
 * </pre>
 */
@Configuration
@ConditionalOnClass({JsEngine.class, HttpClientManager.class})
public class ReaderEngineAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ReaderEngineAutoConfiguration.class);

    @PostConstruct
    public void init() {
        log.info("Reader Engine 初始化中...");
        // 预热 JS 引擎
        JsEngine.getInstance();
        // 预热 HTTP 客户端
        HttpClientManager.getDefaultClient();
        log.info("Reader Engine 初始化完成");
    }

    @Bean
    @ConditionalOnMissingBean
    public JsEngine jsEngine() {
        return JsEngine.getInstance();
    }

    @PreDestroy
    public void destroy() {
        log.info("Reader Engine 销毁中...");
        JsEngine.getInstance().shutdown();
        log.info("Reader Engine 已销毁");
    }
}
