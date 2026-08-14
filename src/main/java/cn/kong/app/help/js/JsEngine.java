package cn.kong.app.help.js;

import cn.kong.app.exception.NoStackTraceException;
import cn.kong.app.utils.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * JavaScript 脚本引擎封装（基于 Mozilla Rhino）
 * 改造自 reader-dev 的 RhinoScriptEngine.kt
 * <p>
 * 用于执行书源规则中嵌入的 JavaScript 脚本，处理复杂的反爬、解密逻辑。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>单例模式，全局共享一个 ContextFactory</li>
 *   <li>每个线程独立 Context（通过 ThreadLocal）</li>
 *   <li>支持设置超时（防止死循环）</li>
 *   <li>支持注入 Java 对象作为 JS 变量</li>
 * </ul>
 */
public class JsEngine {

    private static final Logger log = LoggerFactory.getLogger(JsEngine.class);

    /** JS 执行超时时间（毫秒） */
    private static final int JS_TIMEOUT = 30000;

    /** 全局作用域 */
    private final Scriptable globalScope;

    /** 线程局部 Context */
    private final ThreadLocal<Context> contextThreadLocal = new ThreadLocal<>();

    private JsEngine() {
        // 初始化全局 ContextFactory，设置超时
        ContextFactory factory = new ContextFactory() {
            @Override
            protected boolean hasFeature(Context cx, int featureId) {
                // 启用 ES6 特性
                if (featureId == Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER) {
                    return true;
                }
                return super.hasFeature(cx, featureId);
            }
        };
        Context cx = factory.enterContext();
        try {
            cx.setOptimizationLevel(-1); // 解释执行模式，避免 JIT 内存泄漏
            cx.setLanguageVersion(Context.VERSION_ES6);
            globalScope = cx.initStandardObjects();
        } finally {
            Context.exit();
        }
    }

    /**
     * 单例持有器
     */
    private static class Holder {
        private static final JsEngine INSTANCE = new JsEngine();
    }

    /**
     * 获取单例实例
     */
    public static JsEngine getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 获取当前线程的 Context
     */
    private Context getContext() {
        Context cx = contextThreadLocal.get();
        if (cx == null) {
            cx = Context.enter();
            cx.setOptimizationLevel(-1);
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setInstructionObserverThreshold(JS_TIMEOUT);
            contextThreadLocal.set(cx);
        }
        return cx;
    }

    /**
     * 释放当前线程的 Context
     */
    public void releaseContext() {
        Context cx = contextThreadLocal.get();
        if (cx != null) {
            Context.exit();
            contextThreadLocal.remove();
        }
    }

    /**
     * 执行 JS 脚本
     *
     * @param js 脚本内容
     * @return 执行结果
     */
    public Object eval(String js) {
        return eval(js, null);
    }

    /**
     * 执行 JS 脚本（带变量）
     *
     * @param js        脚本内容
     * @param variables 变量映射
     * @return 执行结果
     */
    public Object eval(String js, Map<String, Object> variables) {
        return eval(js, variables, null);
    }

    /**
     * 执行 JS 脚本（带变量和扩展）
     *
     * @param js         脚本内容
     * @param variables  变量映射
     * @param extensions JS 扩展对象（提供 java.xxx, cache, Get, Put 等方法）
     * @return 执行结果
     */
    public Object eval(String js, Map<String, Object> variables, JsExtensions extensions) {
        if (StringUtils.isEmpty(js)) {
            return null;
        }

        // 替换 JS 脚本中的占位符 {{key}}
        String processedJs = js;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                if (entry.getValue() != null) {
                    processedJs = processedJs.replace("{{" + entry.getKey() + "}}",
                        entry.getValue().toString());
                }
            }
        }

        // 兼容 ES2019 语法：catch{ → catch(e){（Rhino 不支持省略 catch 参数）
        processedJs = processedJs.replaceAll("catch\\s*\\{", "catch(e) {");

        Context cx = getContext();
        try {
            Scriptable scope = cx.newObject(globalScope);
            scope.setPrototype(globalScope);
            scope.setParentScope(null);

            // 注入变量
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    Object jsValue = Context.javaToJS(entry.getValue(), scope);
                    ScriptableObject.putProperty(scope, entry.getKey(), jsValue);
                }
            }

            // 注入 JS 扩展对象
            if (extensions != null) {
                // 注入 java 对象（提供 java.ajax, java.put, java.get 等）
                Object javaObj = Context.javaToJS(extensions, scope);
                ScriptableObject.putProperty(scope, "java", javaObj);

                // 注入 cache 对象
                Object cacheObj = Context.javaToJS(extensions.cache, scope);
                ScriptableObject.putProperty(scope, "cache", cacheObj);

                // 注入 source 对象（书源）
                if (extensions.getSource() != null) {
                    Object sourceObj = Context.javaToJS(extensions.getSource(), scope);
                    ScriptableObject.putProperty(scope, "source", sourceObj);
                }

                // 注入全局函数 Get, Put, login, explore, Num, n, k, name 等
                Object extObj = Context.javaToJS(extensions, scope);
                ScriptableObject.putProperty(scope, "__ext__", extObj);

                // 定义全局函数包装器
                // Get 返回值用 String() 转为 JS 原生字符串，避免 Java String.replace 方法歧义
                String wrapperJs =
                    "var Get = function(key, def) { var v = (def === undefined) ? __ext__.Get(key) : __ext__.Get(key, def); return v == null ? null : String(v); };\n" +
                    "var Put = function(value) { return __ext__.Put(value); };\n" +
                    "var put = function(obj) { return __ext__.put(obj); };\n" +
                    "var login = function(msg) { return __ext__.login(msg); };\n" +
                    "var explore = function(title, url, page, group, isVip) { return __ext__.explore(title, url, page, group, isVip); };\n" +
                    "var Num = function(num) { return __ext__.Num(num); };\n" +
                    "var n = function(count) { return __ext__.n(count); };\n" +
                    "var k = function(num) { return __ext__.k(num); };\n" +
                    "var name = function(num) { return __ext__.name(num); };\n" +
                    "var x = __ext__.x;\n" +
                    "var y = __ext__.y;\n" +
                    // 定义 loginUrl 中使用但未定义的辅助函数
                    "var Checkwait = function(user) { return user; };\n" +
                    "var Data = function() { return '[]'; };\n" +
                    "var Data1 = function() { return '[]'; };\n";
                cx.evaluateString(scope, wrapperJs, "ext-wrapper", 1, null);
            }

            return cx.evaluateString(scope, processedJs, "reader-engine", 1, null);
        } catch (Exception e) {
            log.error("JS 执行失败: {}", e.getMessage());
            throw new NoStackTraceException("JS 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 JS 脚本并返回字符串结果
     */
    public String evalToString(String js) {
        Object result = eval(js);
        return result == null ? "" : result.toString();
    }

    /**
     * 执行 JS 脚本并返回字符串结果（带变量）
     */
    public String evalToString(String js, Map<String, Object> variables) {
        Object result = eval(js, variables);
        return result == null ? "" : result.toString();
    }

    /**
     * 执行 JS 函数
     *
     * @param js        脚本内容（需定义函数）
     * @param functionName 函数名
     * @param args      参数列表
     * @return 执行结果
     */
    public Object evalFunction(String js, String functionName, Object... args) {
        Context cx = getContext();
        try {
            Scriptable scope = cx.newObject(globalScope);
            scope.setPrototype(globalScope);
            scope.setParentScope(null);

            // 先执行脚本定义函数
            cx.evaluateString(scope, js, "reader-engine", 1, null);

            // 调用函数
            Object function = scope.get(functionName, scope);
            if (function == Scriptable.NOT_FOUND) {
                throw new NoStackTraceException("JS 函数不存在: " + functionName);
            }
            return ((org.mozilla.javascript.Function) function).call(cx, scope, scope, args);
        } catch (NoStackTraceException e) {
            throw e;
        } catch (Exception e) {
            log.error("JS 函数执行失败: {}", e.getMessage());
            throw new NoStackTraceException("JS 函数执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注入全局对象（所有 JS 脚本可访问）
     *
     * @param name  对象名
     * @param value Java 对象
     */
    public void putGlobalObject(String name, Object value) {
        Context cx = getContext();
        try {
            Object jsValue = Context.javaToJS(value, globalScope);
            ScriptableObject.putProperty(globalScope, name, jsValue);
        } finally {
            // 不退出，保持全局作用域
        }
    }

    /**
     * 关闭引擎，释放资源
     */
    public void shutdown() {
        try {
            releaseContext();
        } catch (Exception e) {
            log.warn("JsEngine 关闭异常: {}", e.getMessage());
        }
    }
}
