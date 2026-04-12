package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 回滚推送请求
 *
 * @author binblink
 * @since 2026-04-11
 */
@Data
public class RollbackPushReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 推送记录ID
     */
    private Long pushId;

    /**
     * 推送模式: broadcast/specified
     */
    private String pushMode;

    /**
     * 目标实例ID列表（指定实例模式）
     */
    private List<String> targetInstanceIds;
}