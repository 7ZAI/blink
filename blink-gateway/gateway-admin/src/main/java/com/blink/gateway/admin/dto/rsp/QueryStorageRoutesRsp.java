package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.admin.entity.GaRouteDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 配置中心路由响应
 * 从 Redis/Nacos 配置中心查询的路由配置
 *
 * @author binblink
 * @since 2026-04-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryStorageRoutesRsp extends PageDTO<GaRouteDO> {

    @Serial
    private static final long serialVersionUID = 1L;
}
