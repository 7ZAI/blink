package com.blink.gateway.admin.dto.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 路由推送状态VO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class RoutePushStatusVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 路由ID
     */
    private String routeId;

    /**
     * 路由名称
     */
    private String routeName;

    /**
     * 推送状态
     * 0: 未推送
     * 1: 已推送
     * 2: 推送失败
     */
    private Byte pushStatus;

    /**
     * 推送状态描述
     */
    private String pushStatusDesc;

    /**
     * 最后推送时间
     */
    private LocalDateTime lastPushTime;

    /**
     * 版本号
     */
    private Integer version;
}