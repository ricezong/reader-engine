package cn.kong.app.data.entities.rule;

import java.io.Serializable;

/**
 * 登录规则
 * 改造自 reader-dev 的 LoginRule.kt
 */
public class LoginRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 登录 URL */
    private String loginUrl;

    /** 登录方式：0=GET，1=POST，2=POST JSON */
    private Integer loginUi;

    /** 登录检测规则 */
    private String check;

    /** 登录请求头 */
    private String header;

    // ============ getter / setter ============

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public Integer getLoginUi() {
        return loginUi;
    }

    public void setLoginUi(Integer loginUi) {
        this.loginUi = loginUi;
    }

    public String getCheck() {
        return check;
    }

    public void setCheck(String check) {
        this.check = check;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
