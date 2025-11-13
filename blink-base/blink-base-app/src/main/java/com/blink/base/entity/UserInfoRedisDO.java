package com.blink.base.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserInfoRedisDO implements Serializable {

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
}
