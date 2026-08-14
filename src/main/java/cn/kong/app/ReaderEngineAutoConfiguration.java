package cn.kong.app;

import io.legado.app.constant.AppConst;
import io.legado.app.help.http.HttpHelperKt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Reader Engine 自动配置类
 * <p>
 * 当宿主项目引入 reader-engine 依赖后，Spring Boot 会自动加载此配置，
 * 预热 JS 引擎 (RhinoScriptEngine) 和 HTTP 客户端 (OkHttp)。
 * <p>
 * 使用方式：
 * <pre>
 * // 1. 从 JSON 创建书源
 * BookSource source = ReaderEngine.parseBookSource(jsonString);
 *
 * // 2. 搜索
 * List&lt;SearchBook&gt; results = ReaderEngine.search(source, "斗破苍穹", 1);
 *
 * // 3. 获取书籍详情
 * Book book = ReaderEngine.getBookInfo(source, results.get(0).getBookUrl());
 *
 * // 4. 获取目录
 * List&lt;BookChapter&gt; chapters = ReaderEngine.getChapterList(source, book);
 *
 * // 5. 获取正文
 * String content = ReaderEngine.getBookContent(source, book, chapters.get(0));
 * </pre>
 */
@Configuration
@ConditionalOnClass({AppConst.class})
public class ReaderEngineAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ReaderEngineAutoConfiguration.class);

    @PostConstruct
    public void init() {
        log.info("Reader Engine 初始化中...");
        // 预热 JS 引擎 (RhinoScriptEngine)
        AppConst.INSTANCE.getSCRIPT_ENGINE();
        // 预热 HTTP 客户端 (OkHttp)
        HttpHelperKt.getOkHttpClient();
        log.info("Reader Engine 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        log.info("Reader Engine 已销毁");
    }
}
