package com.blink.framework.common.data;


import java.util.Objects;

/**
 * 动态授权信息域
 */
public class DynAuthData {
    /**
     * 授权码
     **/
    private String authCode;
    /**
     * 授权信息
     **/
    private String authMsg;
    /**
     * 授权等级
     **/
    private String authLvl;

    public String getAuthMsg() {
        return authMsg;
    }

    public void setAuthMsg(String authMsg) {
        this.authMsg = authMsg;
    }

    public String getAuthLvl() {
        return authLvl;
    }

    public void setAuthLvl(String authLvl) {
        this.authLvl = authLvl;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DynAuthData that = (DynAuthData) o;
        return Objects.equals(authCode, that.authCode) &&
                Objects.equals(authMsg, that.authMsg) &&
                Objects.equals(authLvl, that.authLvl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authCode, authMsg, authLvl);
    }
}
