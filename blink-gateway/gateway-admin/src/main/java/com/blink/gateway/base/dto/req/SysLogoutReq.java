package com.blink.gateway.base.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登出请求参数
 * 注：token 和 userId 从 Sa-Token 上下文获取，无需前端传递
 *
 * @Author binblink
 * @Date 2025/8/28
 */
@Data
public class SysLogoutReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（可选，从 Sa-Token 上下文获取）
     */
    private String userId;

    /**
     * Token（可选，从 Sa-Token 上下文获取）
     */
    private String token;
}
