package cn.kong.app.help.http;

import cn.kong.app.constant.AppConst;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 客户端管理器
 * 改造自 reader-dev 的 OkHttpUtils.kt
 * <p>
 * 提供全局共享的 OkHttpClient 实例，支持：
 * <ul>
 *   <li>连接池复用</li>
 *   <li>超时配置</li>
 *   <li>忽略 SSL 证书校验（用于书源网站证书问题）</li>
 *   <li>Cookie 管理</li>
 * </ul>
 */
public class HttpClientManager {

    private static OkHttpClient defaultClient;
    private static OkHttpClient noSslClient;

    /**
     * 获取默认 HTTP 客户端
     */
    public static synchronized OkHttpClient getDefaultClient() {
        if (defaultClient == null) {
            defaultClient = createClient(false);
        }
        return defaultClient;
    }

    /**
     * 获取忽略 SSL 证书的 HTTP 客户端
     */
    public static synchronized OkHttpClient getNoSslClient() {
        if (noSslClient == null) {
            noSslClient = createClient(true);
        }
        return noSslClient;
    }

    /**
     * 创建 HTTP 客户端
     *
     * @param ignoreSsl 是否忽略 SSL 证书
     */
    private static OkHttpClient createClient(boolean ignoreSsl) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(AppConst.DEFAULT_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(AppConst.DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(AppConst.DEFAULT_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
            .connectionPool(new ConnectionPool(
                AppConst.DEFAULT_MAX_IDLE_CONNECTIONS,
                AppConst.DEFAULT_KEEP_ALIVE_DURATION,
                TimeUnit.MILLISECONDS
            ))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .cookieJar(CookieStore.getInstance());

        if (ignoreSsl) {
            try {
                X509TrustManager trustManager = new TrustAllManager();
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
                builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
                builder.hostnameVerifier((hostname, session) -> true);
            } catch (Exception e) {
                // 忽略 SSL 配置失败
            }
        }

        return builder.build();
    }

    /**
     * 信任所有证书的 Manager
     */
    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
