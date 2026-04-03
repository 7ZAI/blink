package com.blink.framework.common.data;


import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class UserInfoRedisDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 963285923626533164L;


    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 昵称
     */
    private String username;

    /**
     * 登入时间
     */
    private LocalDateTime loginDateTime;

    /**
     * 登入凭证
     */
    private String token;

    /**
     * 用户权限
     */
    private Set<String> permissions;

    /**
     * 超级管理员标志（0否 1是）
     */
    private Integer superFlag;

    /**
     * 角色ID列表
     */
    private List<Integer> roleIds;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getLoginDateTime() {
        return loginDateTime;
    }

    public void setLoginDateTime(LocalDateTime loginDateTime) {
        this.loginDateTime = loginDateTime;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Integer getSuperFlag() {
        return superFlag;
    }

    public void setSuperFlag(Integer superFlag) {
        this.superFlag = superFlag;
    }

    public List<Integer> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Integer> roleIds) {
        this.roleIds = roleIds;
    }

    @Override
    public String toString() {
        return "UserInfoRedisDO{" +
                "userId=" + userId +
                ", loginName='" + loginName + '\'' +
                ", username='" + username + '\'' +
                ", loginDateTime=" + loginDateTime +
                ", token='" + token + '\'' +
                ", permissions=" + permissions +
                ", superFlag=" + superFlag +
                ", roleIds=" + roleIds +
                '}';
    }
}
