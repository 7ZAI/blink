package com.blink.framework.common.context;


import lombok.Data;

import java.time.LocalDate;

/**
 * 请求上下文 保存常用信息
 * @Author binblink
 */
@Data
public class BlinkRequestContext {
    /**
     * 请求ID
     */
    private String requestId;
    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 组件id
     */
    private String spanId;

    /**
     * 当前登入用户ID
     */
    private String userId;

    /**
     * 当前登入用户名
     */
    private String loginName;

    /**
     * ip
     */
    private String clientIp;

    /**
     * 语言环境
     */
    private String language;

    /**
     * 应用名
     */
    private String appName;

    /**
     * 请求时间
     */
    private LocalDate requestDate;

    /**
     * 渠道
     */
    private String channel;

}
