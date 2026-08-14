package cn.kong.app.utils;

import java.nio.charset.StandardCharsets;

/**
 * Base64 工具类
 */
public final class Base64 {

    private Base64() {
    }

    private static final java.util.Base64.Encoder ENCODER = java.util.Base64.getEncoder();
    private static final java.util.Base64.Decoder DECODER = java.util.Base64.getDecoder();
    private static final java.util.Base64.Encoder URL_ENCODER = java.util.Base64.getUrlEncoder();
    private static final java.util.Base64.Decoder URL_DECODER = java.util.Base64.getUrlDecoder();

    public static String encode(String str) {
        if (str == null) return "";
        return ENCODER.encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String encode(byte[] bytes) {
        if (bytes == null) return "";
        return ENCODER.encodeToString(bytes);
    }

    public static byte[] decode(String str) {
        if (str == null) return new byte[0];
        return DECODER.decode(str);
    }

    public static String decodeToString(String str) {
        if (str == null) return "";
        return new String(DECODER.decode(str), StandardCharsets.UTF_8);
    }

    public static String urlEncode(String str) {
        if (str == null) return "";
        return URL_ENCODER.encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] urlDecode(String str) {
        if (str == null) return new byte[0];
        return URL_DECODER.decode(str);
    }
}
