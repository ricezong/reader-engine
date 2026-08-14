package cn.kong.app.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 * 改造自 reader-dev 的 GSON.kt
 */
public final class JsonUtils {

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setLenient()
        .create();

    private static final Gson GSON_PRETTY = new GsonBuilder()
        .disableHtmlEscaping()
        .setLenient()
        .setPrettyPrinting()
        .create();

    private JsonUtils() {
    }

    /**
     * 获取默认 Gson 实例
     */
    public static Gson getGson() {
        return GSON;
    }

    /**
     * 获取格式化输出的 Gson 实例
     */
    public static Gson getPrettyGson() {
        return GSON_PRETTY;
    }

    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object src) {
        if (src == null) {
            return null;
        }
        return GSON.toJson(src);
    }

    /**
     * 对象转格式化 JSON 字符串
     */
    public static String toJsonPretty(Object src) {
        if (src == null) {
            return null;
        }
        return GSON_PRETTY.toJson(src);
    }

    /**
     * JSON 字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, classOfT);
    }

    /**
     * JSON 字符串转对象（支持泛型）
     */
    public static <T> T fromJson(String json, Type typeOfT) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, typeOfT);
    }

    /**
     * JSON 字符串转 List
     */
    public static <T> List<T> fromJsonList(String json, Class<T> classOfT) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        Type type = TypeToken.getParameterized(List.class, classOfT).getType();
        return GSON.fromJson(json, type);
    }

    /**
     * JSON 字符串转 Map
     */
    public static <K, V> Map<K, V> fromJsonMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        Type type = TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
        return GSON.fromJson(json, type);
    }

    /**
     * 解析 JSON 字符串为 JsonElement
     */
    public static JsonElement parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JsonParser.parseString(json);
    }

    /**
     * 判断字符串是否为合法 JSON
     */
    public static boolean isValid(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            JsonParser.parseString(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
