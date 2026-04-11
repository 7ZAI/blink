package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 查询推送历史请求
 *
 * @author binblink
 * @since 2026-04-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryPushLogReq extends Page {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储方式: redis/nacos
     */
    private String storageMode;

    /**
     * 路由分组（Redis模式）
     */
    private String routesGroup;

    /**
     * 推送结果: 0-成功, 1-部分失败, 2-失败
     */
    private Byte pushResult;

    /**
     * 操作人名称
     */
    private String operatorName;
}