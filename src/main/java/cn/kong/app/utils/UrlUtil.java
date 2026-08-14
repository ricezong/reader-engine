package cn.kong.app.utils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * URL 工具类
 * 改造自 reader-dev 的 UrlUtil.kt
 */
public final class UrlUtil {

    private UrlUtil() {
    }

    /**
     * 判断字符串是否为 URL
     */
    public static boolean isUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /**
     * 获取 URL 的根路径，例如 https://www.example.com
     */
    public static String getBaseUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host);
            if (port != -1 && !isDefaultPort(scheme, port)) {
                sb.append(":").append(port);
            }
            return sb.toString();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    /**
     * 拼接相对 URL 为绝对 URL
     *
     * @param relativeUrl 相对 URL
     * @param baseUrl      基础 URL
     */
    public static String absoluteUrl(String relativeUrl, String baseUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) {
            return "";
        }
        if (isUrl(relativeUrl)) {
            return relativeUrl;
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            return relativeUrl;
        }
        try {
            URI base = new URI(baseUrl);
            URI resolved = base.resolve(relativeUrl);
            return resolved.toString();
        } catch (Exception e) {
            // 简单拼接兜底
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String rel = relativeUrl.startsWith("/") ? relativeUrl : "/" + relativeUrl;
            return base + rel;
        }
    }

    /**
     * 判断是否为默认端口
     */
    private static boolean isDefaultPort(String scheme, int port) {
        if ("http".equals(scheme) && port == 80) {
            return true;
        }
        if ("https".equals(scheme) && port == 443) {
            return true;
        }
        return false;
    }

    /**
     * 获取 URL 的 host
     */
    public static String getHost(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    /**
     * 获取 URL 的 path
     */
    public static String getPath(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            return path == null ? "" : path;
        } catch (URISyntaxException e) {
            return "";
        }
    }
}
