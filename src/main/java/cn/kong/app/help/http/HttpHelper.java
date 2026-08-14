package cn.kong.app.help.http;

import cn.kong.app.constant.AppConst;
import cn.kong.app.utils.StringUtils;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * HTTP 请求封装
 * 改造自 reader-dev 的 HttpHelper.kt
 * <p>
 * 提供同步的 HTTP 请求方法（替代 Vert.x 的异步 WebClient）。
 */
public class HttpHelper {

    private static final Logger log = LoggerFactory.getLogger(HttpHelper.class);

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded; charset=utf-8");
    public static final MediaType TEXT = MediaType.get("text/plain; charset=utf-8");

    private HttpHelper() {
    }

    /**
     * 执行 GET 请求
     */
    public static Response get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * 执行 GET 请求（带请求头）
     */
    public static Response get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).get();
        addHeaders(builder, headers);
        return execute(builder);
    }

    /**
     * 执行 POST 请求（表单）
     */
    public static Response postForm(String url, Map<String, String> formData) throws IOException {
        return postForm(url, formData, null);
    }

    /**
     * 执行 POST 请求（表单，带请求头）
     */
    public static Response postForm(String url, Map<String, String> formData, Map<String, String> headers) throws IOException {
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        if (formData != null) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                bodyBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        Request.Builder builder = new Request.Builder().url(url).post(bodyBuilder.build());
        addHeaders(builder, headers);
        return execute(builder);
    }

    /**
     * 执行 POST 请求（JSON）
     */
    public static Response postJson(String url, String json) throws IOException {
        return postJson(url, json, null);
    }

    /**
     * 执行 POST 请求（JSON，带请求头）
     */
    public static Response postJson(String url, String json, Map<String, String> headers) throws IOException {
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        return execute(builder);
    }

    /**
     * 执行 POST 请求（原始 body）
     */
    public static Response postBody(String url, String body, MediaType mediaType, Map<String, String> headers) throws IOException {
        RequestBody requestBody = RequestBody.create(body, mediaType);
        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        addHeaders(builder, headers);
        return execute(builder);
    }

    /**
     * 执行请求
     */
    private static Response execute(Request.Builder builder) throws IOException {
        // 默认 UA
        if (builder.build().header("User-Agent") == null) {
            builder.header("User-Agent", AppConst.USER_AGENT);
        }
        OkHttpClient client = HttpClientManager.getDefaultClient();
        return client.newCall(builder.build()).execute();
    }

    /**
     * 执行请求（忽略 SSL 证书）
     */
    private static Response executeNoSsl(Request.Builder builder) throws IOException {
        if (builder.build().header("User-Agent") == null) {
            builder.header("User-Agent", AppConst.USER_AGENT);
        }
        OkHttpClient client = HttpClientManager.getNoSslClient();
        return client.newCall(builder.build()).execute();
    }

    /**
     * 执行 GET 请求（忽略 SSL 证书）
     */
    public static Response getNoSsl(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).get();
        addHeaders(builder, headers);
        return executeNoSsl(builder);
    }

    /**
     * 执行 POST 请求（忽略 SSL 证书）
     */
    public static Response postBodyNoSsl(String url, String body, MediaType mediaType, Map<String, String> headers) throws IOException {
        RequestBody requestBody = StringUtils.isEmpty(body) ?
            RequestBody.create("", null) :
            RequestBody.create(body, mediaType == null ? FORM : mediaType);
        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        addHeaders(builder, headers);
        return executeNoSsl(builder);
    }

    /**
     * 添加请求头
     */
    private static void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 获取响应体字符串
     */
    public static String getResponseBody(Response response) throws IOException {
        if (response == null || !response.isSuccessful()) {
            return "";
        }
        ResponseBody body = response.body();
        return body == null ? "" : body.string();
    }

    /**
     * 获取响应体字节流
     */
    public static InputStream getResponseStream(Response response) throws IOException {
        if (response == null || !response.isSuccessful()) {
            return null;
        }
        ResponseBody body = response.body();
        return body == null ? null : body.byteStream();
    }

    /**
     * 获取响应体字节数组
     */
    public static byte[] getResponseBytes(Response response) throws IOException {
        if (response == null || !response.isSuccessful()) {
            return new byte[0];
        }
        ResponseBody body = response.body();
        return body == null ? new byte[0] : body.bytes();
    }

    /**
     * 构建请求头
     */
    public static Headers buildHeaders(Map<String, String> headerMap) {
        if (headerMap == null) {
            return new Headers.Builder().build();
        }
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    /**
     * 关闭响应
     */
    public static void closeResponse(Response response) {
        if (response != null) {
            response.close();
        }
    }
}
