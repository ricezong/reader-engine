package cn.kong.app.help.http;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cookie 存储管理器
 * 改造自 reader-dev 的 CookieStore.kt
 * <p>
 * 按域名存储 Cookie，支持书源网站的会话保持。
 */
public class CookieStore implements CookieJar {

    private final Map<String, List<Cookie>> cookieMap = new ConcurrentHashMap<>();

    private CookieStore() {
    }

    private static class Holder {
        private static final CookieStore INSTANCE = new CookieStore();
    }

    public static CookieStore getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        if (url == null || cookies == null || cookies.isEmpty()) {
            return;
        }
        String host = url.host();
        List<Cookie> existing = cookieMap.computeIfAbsent(host, k -> new CopyOnWriteArrayList<>());
        for (Cookie newCookie : cookies) {
            // 移除同名旧 Cookie
            existing.removeIf(c -> c.name().equals(newCookie.name()));
            existing.add(newCookie);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        if (url == null) {
            return new ArrayList<>();
        }
        String host = url.host();
        List<Cookie> cookies = cookieMap.get(host);
        if (cookies == null) {
            return new ArrayList<>();
        }
        // 过滤过期 Cookie
        List<Cookie> result = new ArrayList<>();
        for (Cookie cookie : cookies) {
            if (cookie.expiresAt() != Long.MAX_VALUE && cookie.expiresAt() < System.currentTimeMillis()) {
                continue;
            }
            result.add(cookie);
        }
        return result;
    }

    /**
     * 获取指定域名的 Cookie 字符串
     */
    public String getCookieString(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            return "";
        }
        List<Cookie> cookies = loadForRequest(httpUrl);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cookies.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(cookies.get(i).name()).append("=").append(cookies.get(i).value());
        }
        return sb.toString();
    }

    /**
     * 清除指定域名的 Cookie
     */
    public void clear(String host) {
        cookieMap.remove(host);
    }

    /**
     * 清除所有 Cookie
     */
    public void clearAll() {
        cookieMap.clear();
    }
}
