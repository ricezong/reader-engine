package cn.kong.app.data.entities;

import java.io.Serializable;

/**
 * 源基类
 * 改造自 reader-dev 的 BaseSource.kt
 */
public abstract class BaseSource implements Serializable {

    private static final long serialVersionUID = 1L;

    public String getBookSourceUrl() { return ""; }
    public String getBookSourceName() { return ""; }
    public Integer getBookSourceType() { return 0; }
    public String getConcurrentRate() { return null; }
    public String getHeader() { return null; }
    public String getLoginUrl() { return null; }
    public String getVariableComment() { return null; }
}
