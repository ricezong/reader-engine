package cn.kong.app.constant;

/**
 * 应用常量定义
 * 改造自 reader-dev 的 AppConst.kt
 */
public final class AppConst {

    private AppConst() {
    }

    /**
     * 书源类型
     */
    public static final class BookType {
        public static final int TEXT = 0;   // 文本（小说）
        public static final int AUDIO = 1;   // 音频
        public static final int IMAGE = 2;  // 图片（漫画）

        private BookType() {
        }
    }

    /**
     * 本地存储 ID 前缀
     */
    public static final String LOCAL_TAG = "loc_";

    /**
     * 默认 UA
     */
    public static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * 默认超时时间（毫秒）
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    public static final int DEFAULT_READ_TIMEOUT = 30000;
    public static final int DEFAULT_WRITE_TIMEOUT = 30000;

    /**
     * 默认连接池配置
     */
    public static final int DEFAULT_MAX_IDLE_CONNECTIONS = 20;
    public static final long DEFAULT_KEEP_ALIVE_DURATION = 300000L; // 5分钟
}
