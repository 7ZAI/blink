package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 简化用户信息（用于弹窗选择）
 *
 * @author binblink
 */
@Data
public class SimpleUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
}